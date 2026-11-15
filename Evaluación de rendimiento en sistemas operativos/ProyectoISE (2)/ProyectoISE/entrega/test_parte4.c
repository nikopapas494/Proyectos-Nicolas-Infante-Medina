/***********************************************************************
 * Módulo: pruebas unitarias de buffers.c y reportes.c
 * Archivo: test_parte4.c
 * Autor: Nicolas Infante, Juan Quintero, Miguel Castillo, Santiago Gutierrez
 * Contiene: prueba_calculo, prueba_buffer, prueba_interrumpido,
 *           prueba_reporte_final, el productor de prueba y main. Prueba
 *           los buffers y el cálculo del ISE sin servidor ni pipe.
 * Fecha: noviembre 2026
 * Compilar y ejecutar: make test
 ***********************************************************************/
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <pthread.h>
#include "buffers.h"
#include "reportes.h"

/* Contador de pruebas fallidas (global justificada: lo usa la macro CHECK) */
static int fallos = 0;

/* Macro CHECK: imprime "ok" o "FALLA" según cond y cuenta los fallos */
#define CHECK(cond, msg) do { if (cond) printf("  ok   %s\n", msg); \
    else { printf("  FALLA %s\n", msg); fallos++; } } while (0)

/* Argumento del hilo productor de prueba */
typedef struct {
    Buffer *b;     /* buffer donde inserta */
    int     n;     /* encuestas a insertar (más un MSG_FIN al final) */
} ArgProd;

/* Función: prueba_calculo
 * Autor: Nicolas
 * Parámetros de entrada: no tiene
 * Valor de salida: no tiene
 * Descripción: verifica calcular_ise y clasificar_ise con datos fijos:
 * ISE ponderado, los tres resultados posibles, renormalización de pesos
 * cuando faltan sectores y el caso sin datos. */
static void prueba_calculo(void) {
    double suma[NUM_SECTORES] = {80, 60, 40, 20, 10};
    long cuenta[NUM_SECTORES] = {1, 1, 1, 1, 1};
    double s2[NUM_SECTORES] = {80, 60, 0, 0, 0};
    long c2[NUM_SECTORES] = {1, 1, 0, 0, 0};
    double s3[NUM_SECTORES] = {0};
    long c3[NUM_SECTORES] = {0};
    double ise;

    printf("[calculo]\n");
    /* 0.4*80 + 0.3*60 + 0.2*40 + 0.05*20 + 0.05*10 = 32+18+8+1+0.5 = 59.5 */
    CHECK(calcular_ise(suma, cuenta, &ise) == 0 && fabs(ise - 59.5) < 1e-9, "ISE ponderado = 59.5");
    CHECK(strcmp(clasificar_ise(59.5), "Vender") == 0, "59.5 -> Vender");
    CHECK(strcmp(clasificar_ise(40.0), "Comprar") == 0, "40 -> Comprar");
    CHECK(strcmp(clasificar_ise(50.004), "Neutro") == 0, "50.004 -> Neutro");
    CHECK(strcmp(clasificar_ise(49.996), "Neutro") == 0, "49.996 -> Neutro");
    /* Solo industria (80) y servicios (60): (0.4*80+0.3*60)/0.7 */
    CHECK(calcular_ise(s2, c2, &ise) == 0 && fabs(ise - 50.0 / 0.7) < 1e-9, "renormaliza sectores sin datos");
    CHECK(calcular_ise(s3, c3, &ise) == -1, "sin datos -> -1");
}

/* Función: productor
 * Autor: Nicolas
 * Parámetros de entrada: p, apuntador a un ArgProd
 * Valor de salida: NULL
 * Descripción: hilo que inserta n encuestas (sectores en rotación y
 * sentimiento i % 101) y al final un MSG_FIN. */
static void *productor(void *p) {
    ArgProd *a = (ArgProd *) p;
    Mensaje msg;
    int i;

    memset(&msg, 0, sizeof(msg));
    msg.tipo = MSG_ENCUESTA;
    for (i = 0; i < a->n; i++) {
        msg.id_encuesta = i + 1;
        msg.id_sector = i % NUM_SECTORES;
        msg.sentimiento = i % 101;
        buffer_insertar(a->b, &msg);
    } /* fin for encuestas */
    msg.tipo = MSG_FIN;
    buffer_insertar(a->b, &msg);
    return NULL;
}

/* Función: prueba_buffer
 * Autor: Nicolas
 * Parámetros de entrada: tam, capacidad del buffer; n, encuestas a enviar
 * Valor de salida: no tiene
 * Descripción: lanza un productor y un consumidor sobre un buffer de
 * tamaño tam y verifica que se cuenten las n encuestas, que la suma de
 * sentimientos coincida y que el agente no quede como interrumpido. */
static void prueba_buffer(int tam, int n) {
    char texto[96];
    DatosAgente ag;
    ArgProd ap = { &ag.buffer, n };
    pthread_t consumidor, prod;
    double esperado = 0, obtenido = 0;
    int i;

    memset(&ag, 0, sizeof(ag));
    strcpy(ag.pais, "ES");
    buffer_iniciar(&ag.buffer, tam);
    pthread_create(&consumidor, NULL, hilo_consumidor, &ag);
    pthread_create(&prod, NULL, productor, &ap);
    pthread_join(prod, NULL);
    pthread_join(consumidor, NULL);

    for (i = 0; i < n; i++) {
        esperado += (i % 101);
    }
    for (i = 0; i < NUM_SECTORES; i++) {
        obtenido += ag.suma[i];
    }
    snprintf(texto, sizeof texto, "buffer %d: %d encuestas contadas, suma correcta, no interrumpido", tam, n);
    CHECK(ag.recibidas == n && fabs(esperado - obtenido) < 1e-6 && !ag.interrumpido, texto);
    buffer_destruir(&ag.buffer);
}

/* Función: prueba_interrumpido
 * Autor: Nicolas
 * Parámetros de entrada: no tiene
 * Valor de salida: no tiene
 * Descripción: simula lo que hace el servidor al vencer -t con un agente a
 * medias: marca interrumpido, inserta un MSG_FIN y verifica que el
 * consumidor procese lo pendiente y termine sin bloquearse. También
 * verifica que buffer_iniciar rechace una capacidad inválida. */
static void prueba_interrumpido(void) {
    DatosAgente ag;
    Mensaje msg;
    pthread_t consumidor;
    int i;

    printf("[interrumpido]\n");
    CHECK(buffer_iniciar(&ag.buffer, 0) == -1, "buffer_iniciar rechaza capacidad 0");

    memset(&ag, 0, sizeof(ag));
    strcpy(ag.pais, "FR");
    buffer_iniciar(&ag.buffer, 20);
    memset(&msg, 0, sizeof(msg));
    msg.tipo = MSG_ENCUESTA;
    msg.sentimiento = 70.0;
    for (i = 0; i < 10; i++) {
        buffer_insertar(&ag.buffer, &msg);              /* quedan 10 sin consumir */
    }
    ag.interrumpido = 1;
    msg.tipo = MSG_FIN;
    buffer_insertar(&ag.buffer, &msg);

    pthread_create(&consumidor, NULL, hilo_consumidor, &ag);
    pthread_join(consumidor, NULL);                     /* si se bloquea, timeout lo corta */
    CHECK(ag.recibidas == 10 && ag.interrumpido == 1, "procesa lo pendiente y termina como interrumpido");
    buffer_destruir(&ag.buffer);
}

/* Función: prueba_reporte_final
 * Autor: Nicolas
 * Parámetros de entrada: no tiene
 * Valor de salida: no tiene
 * Descripción: imprime un reporte parcial y un resumen general de ejemplo
 * (un país finalizado, uno incompleto y uno sin conexión) para revisarlos
 * a ojo. */
static void prueba_reporte_final(void) {
    DatosAgente ag[2];

    printf("[reportes de ejemplo]\n");
    memset(ag, 0, sizeof(ag));
    strcpy(ag[0].pais, "FR");
    ag[0].esperadas = 3;
    ag[0].recibidas = 3;
    ag[0].cuenta[0] = 1; ag[0].suma[0] = 70;
    ag[0].cuenta[1] = 1; ag[0].suma[1] = 55;
    ag[0].cuenta[4] = 1; ag[0].suma[4] = 30;
    strcpy(ag[1].pais, "ES");
    ag[1].esperadas = 100;
    ag[1].recibidas = 1;
    ag[1].interrumpido = 1;
    ag[1].cuenta[2] = 1; ag[1].suma[2] = 40;

    reporte_parcial(&ag[0]);
    reporte_final(ag, 2, 30);
}

/* Función: main
 * Autor: Nicolas
 * Parámetros de entrada: no tiene
 * Valor de salida: 0 si todas las pruebas pasan, 1 si alguna falla
 * Descripción: corre todas las pruebas e imprime el resultado. */
int main(void) {
    prueba_calculo();
    printf("[buffers]\n");
    prueba_buffer(1, 5000);      /* máxima contención */
    prueba_buffer(2, 5000);
    prueba_buffer(100, 5000);    /* buffer grande */
    prueba_interrumpido();
    prueba_reporte_final();

    printf("\n%s\n", fallos ? "HAY FALLAS" : "TODO OK");
    return fallos ? 1 : 0;
}
