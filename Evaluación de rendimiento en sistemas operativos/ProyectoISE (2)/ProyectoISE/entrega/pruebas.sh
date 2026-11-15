#!/bin/bash
# pruebas.sh - Pruebas de integración del Proyecto ISE (generador + agentes + servidor)
# Autor: Nicolas
# Uso: ./pruebas.sh        (desde la carpeta con los ejecutables; usa make si faltan)
# Cada caso corre en su propia carpeta temporal y con su propio pipe, así que
# los casos largos (-t 30) se ejecutan en paralelo y todo tarda ~40 s.
# Compara contra el CSV: total de encuestas y ISE (calculado con awk).

# Configuración regional estándar: con un sistema en español (decimales con coma)
# awk interpreta "0.40" como 0 y el ISE esperado sale vacío.
export LC_ALL=C

DIR=$(cd "$(dirname "$0")" && pwd)
TRABAJO=$(mktemp -d /tmp/ise_pruebas.XXXXXX)
TOL=0.011                                  # el ISE se imprime con 2 decimales

[ -x "$DIR/servidorISE" ] || make -C "$DIR" >/dev/null || exit 1

# ise_csv archivo...: ISE esperado de uno o varios CSV (pesos 40/30/20/5/5)
ise_csv() {
    tail -q -n +2 "$@" | awk -F, '
        BEGIN { split("0.40 0.30 0.20 0.05 0.05", w, " ") }
        { s[$5] += $6; c[$5]++ }
        END { a = 0; p = 0
              for (i = 0; i < 5; i++) if (c[i] > 0) { a += w[i+1] * s[i] / c[i]; p += w[i+1] }
              if (p > 0) printf "%.4f\n", a / p }'
}

# cerca A B: 0 si |A-B| < TOL
cerca() { awk -v a="$1" -v b="$2" -v t="$TOL" 'BEGIN { d = a - b; if (d < 0) d = -d; exit !(d < t) }'; }

# resultado FICHERO NOMBRE CONDICION...: registra PASS/FAIL en FICHERO
check() {
    local out=$1 nombre=$2; shift 2
    if "$@"; then echo "  PASS  $nombre" >> "$out"; else echo "  FAIL  $nombre" >> "$out"; fi
}

# caso NOMBRE B A T "PAIS:PCT ..." ESPERADOS: servidor + un agente por país
#   ESPERADOS: países que deben ser aceptados (los demás deben ser rechazados)
caso() {
    local nombre=$1 b=$2 a=$3 t=$4 lista=$5 aceptados=$6
    local d="$TRABAJO/$nombre" out="$TRABAJO/$nombre.res" par pais pct archivos="" total=0
    mkdir -p "$d"; cd "$d" || return
    : > "$out"; echo "[$nombre] -b $b -a $a -t $t  ($lista)" >> "$out"

    for par in $lista; do "$DIR/generadorCSV" -p "${par%%:*}" -e "${par##*:}" >/dev/null; done
    "$DIR/servidorISE" -b "$b" -p "fifo_$nombre" -t "$t" -a "$a" > servidor.log 2>&1 &
    local srv=$!
    while [ ! -p "fifo_$nombre" ]; do sleep 0.1; done

    for par in $lista; do                  # un agente por país, escalonados
        pais=${par%%:*}
        "$DIR/agentePais" -n "encuestas_$pais.csv" -p "fifo_$nombre" > "agente_$pais.log" 2>&1 &
        sleep 1
    done
    wait "$srv"; local rc=$?

    check "$out" "servidor termina con código 0" test $rc -eq 0
    check "$out" "el pipe se eliminó" test ! -e "fifo_$nombre"
    for par in $lista; do
        pais=${par%%:*}
        if [[ " $aceptados " == *" $pais "* ]]; then
            local n=$(( $(wc -l < "encuestas_$pais.csv") - 1 ))
            archivos="$archivos encuestas_$pais.csv"; total=$((total + n))
            check "$out" "$pais: $n encuestas recibidas y finalizó" \
                grep -q "$pais: finalizo .*($n de $n encuestas)" servidor.log
            local ise_p; ise_p=$(awk -v p="$pais" '$0 ~ "REPORTE PARCIAL - " p {f=1} f && /ISE del pais/ {print $4; exit}' servidor.log)
            check "$out" "$pais: ISE parcial = $(ise_csv "encuestas_$pais.csv")" cerca "${ise_p:-x}" "$(ise_csv "encuestas_$pais.csv")"
        else
            check "$out" "$pais: rechazado y sin conexión" \
                bash -c "grep -q 'rechazado' servidor.log && grep -q '$pais: sin conexion' servidor.log"
        fi
    done
    local ise_g; ise_g=$(awk '/ISE global/ {print $3; exit}' servidor.log)
    check "$out" "total global = $total" grep -q "Total de encuestas: $total\$" servidor.log
    check "$out" "ISE global = $(ise_csv $archivos)" cerca "${ise_g:-x}" "$(ise_csv $archivos)"
    check "$out" "no quedan procesos servidor" test -z "$(pgrep -f "servidorISE -b $b -p fifo_$nombre")"
}

# caso_incompleto: un agente que muere a la mitad (sin MSG_FIN) debe salir "incompleto"
caso_incompleto() {
    local nombre=incompleto d="$TRABAJO/incompleto" out="$TRABAJO/incompleto.res"
    mkdir -p "$d"; cd "$d" || return
    : > "$out"; echo "[$nombre] agente que muere sin enviar FIN, -t 30" >> "$out"
    if ! command -v python3 >/dev/null; then echo "  SKIP  falta python3" >> "$out"; return; fi
    "$DIR/servidorISE" -b 5 -p fifo_inc -t 30 -a 1 > servidor.log 2>&1 &
    local srv=$!
    while [ ! -p fifo_inc ]; do sleep 0.1; done
    python3 - <<'PY'
import os, struct, random
fd = os.open("fifo_inc", os.O_WRONLY); pid = os.getpid()
def msg(tipo, enc, sec, sent):      # mismo layout que Mensaje (32 bytes)
    os.write(fd, struct.pack("<ii3sxiixxxxd", tipo, pid, b"IT", enc, sec, sent))
msg(1, 1000, -1, 0.0)               # anuncia 1000 encuestas
for i in range(300): msg(2, i + 1, i % 5, 80.0)
os.close(fd)                        # muere sin enviar FIN
PY
    wait "$srv"
    check "$out" "IT: incompleto (300 de 1000)" grep -q "IT: incompleto .*(300 de 1000 encuestas)" servidor.log
    check "$out" "ISE global = 80.00 -> Vender" grep -q "ISE global: 80.00 -> Recomendacion: Vender" servidor.log
    check "$out" "no se imprimió reporte parcial" bash -c "! grep -q 'REPORTE PARCIAL' servidor.log"
    check "$out" "el pipe se eliminó" test ! -e fifo_inc
}

# caso_errores: argumentos inválidos, servidor apagado y CSV inexistente
caso_errores() {
    local out="$TRABAJO/errores.res" d="$TRABAJO/errores"
    mkdir -p "$d"; cd "$d" || return
    : > "$out"; echo "[errores] validaciones" >> "$out"
    check "$out" "generador rechaza país XX"        bash -c "! $DIR/generadorCSV -p XX -e 10 >/dev/null 2>&1"
    check "$out" "generador rechaza porcentaje 0"   bash -c "! $DIR/generadorCSV -p ES -e 0 >/dev/null 2>&1"
    check "$out" "generador acepta flags invertidos" bash -c "$DIR/generadorCSV -e 5 -p ES >/dev/null"
    check "$out" "servidor rechaza -t 10"           bash -c "! $DIR/servidorISE -b 1 -p f -t 10 -a 1 >/dev/null 2>&1"
    check "$out" "servidor rechaza -a 5"            bash -c "! $DIR/servidorISE -b 1 -p f -t 30 -a 5 >/dev/null 2>&1"
    check "$out" "servidor rechaza -b 0"            bash -c "! $DIR/servidorISE -b 0 -p f -t 30 -a 1 >/dev/null 2>&1"
    check "$out" "agente con CSV inexistente falla" bash -c "! $DIR/agentePais -n no_existe.csv -p f >/dev/null 2>&1"
    check "$out" "agente sin servidor falla"        bash -c "! $DIR/agentePais -n encuestas_ES.csv -p no_hay_fifo >/dev/null 2>&1"
}

caso basico   10  3 30 "ES:20 FR:5 IT:10" "ES FR IT" &
caso rechazo   1  2 30 "ES:10 FR:10 IT:10" "ES FR" &
caso buffer1   1  3 30 "FR:5 ES:5 IT:5"   "FR ES IT" &
caso grande  500  3 30 "IT:50 FR:20 ES:50" "IT FR ES" &
caso_incompleto &
caso_errores &
wait

for r in "$TRABAJO"/*.res; do cat "$r"; done
echo; echo "Resultados completos en $TRABAJO"
if grep -q "FAIL" "$TRABAJO"/*.res; then echo "HAY FALLAS"; exit 1; fi
echo "TODO OK"
