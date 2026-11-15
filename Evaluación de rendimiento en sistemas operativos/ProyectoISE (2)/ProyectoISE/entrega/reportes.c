/***********************************************************************
 * Módulo: cálculo del ISE y reportes por consola
 * Archivo: reportes.c
 * Autor: Nicolas Infante, Juan Quintero, Miguel Castillo, Santiago Gutierrez
 * Contiene: calcular_ise, clasificar_ise, reporte_parcial, reporte_final
 *           y las auxiliares privadas imprimir_promedios, imprimir_ise,
 *           estado_pais y sumar_agentes_pais.
 * Fecha: noviembre 2026
 * Proyecto Sistemas Operativos 2026-30 - Indicador de Sentimiento Económico
 ***********************************************************************/
#include <stdio.h>
#include <string.h>
#include <pthread.h>
#include "reportes.h"

/* Diferencia máxima respecto a 50 para considerar el ISE como "Neutro" */
#define TOLERANCIA_NEUTRO 0.01

/*
 * Variable global (justificada: la comparten todos los hilos que imprimen).
 * Evita que dos reportes de hilos distintos se mezclen en la consola.
 * Es usada por: reporte_parcial y reporte_final.
 */
static pthread_mutex_t mutex_salida = PTHREAD_MUTEX_INITIALIZER;

/* Función: calcular_ise
 * Autor: Nicolas
 * Parámetros de entrada: suma y cuenta, arreglos por sector; ise,
 * apuntador donde se deja el resultado
 * Valor de salida: 0 si se calculó, -1 si ningún sector tiene datos
 * Descripción: ISE = suma(peso * promedio del sector). Si un sector no
 * tiene datos se omite y se divide entre la suma de los pesos usados, de
 * modo que los pesos restantes quedan renormalizados (suman 1). */
int calcular_ise(const double suma[NUM_SECTORES], const long cuenta[NUM_SECTORES],
                 double *ise) {
    double acumulado = 0.0;   /* suma de peso * promedio de cada sector */
    double peso_usado = 0.0;  /* suma de los pesos de sectores con datos */
    int s;

    for (s = 0; s < NUM_SECTORES; s++) {
        if (cuenta[s] > 0) {                         /* sector sin datos: se omite */
            acumulado += PESOS[s] * (suma[s] / (double) cuenta[s]);
            peso_usado += PESOS[s];
        } /* fin if sector con datos */
    } /* fin for sectores */

    if (peso_usado == 0.0) {
        return -1;
    }
    *ise = acumulado / peso_usado;                   /* renormaliza los pesos */
    return 0;
}

/* Función: clasificar_ise
 * Autor: Nicolas
 * Parámetros de entrada: ise, valor del indicador (0 a 100)
 * Valor de salida: cadena constante "Vender", "Comprar" o "Neutro"
 * Descripción: Vender si el ISE supera 50, Comprar si es menor, y Neutro
 * si la diferencia con 50 es menor a TOLERANCIA_NEUTRO (los decimales casi
 * nunca dan exactamente 50). */
const char *clasificar_ise(double ise) {
    double diferencia = ise - 50.0;

    if (diferencia < 0.0) {
        diferencia = -diferencia;                    /* valor absoluto */
    }
    if (diferencia < TOLERANCIA_NEUTRO) {
        return "Neutro";
    }
    return (ise > 50.0) ? "Vender" : "Comprar";
}

/* Función: imprimir_promedios (privada)
 * Autor: Nicolas
 * Parámetros de entrada: suma y cuenta, arreglos por sector
 * Valor de salida: no tiene
 * Descripción: imprime una línea por sector con su promedio y número de
 * encuestas, o "sin datos". El llamador debe tener tomado mutex_salida. */
static void imprimir_promedios(const double suma[NUM_SECTORES],
                               const long cuenta[NUM_SECTORES]) {
    int s;

    for (s = 0; s < NUM_SECTORES; s++) {
        if (cuenta[s] > 0) {
            printf("  %-13s: %6.2f  (%ld encuestas)\n", SECTORES[s],
                   suma[s] / (double) cuenta[s], cuenta[s]);
        } else {
            printf("  %-13s:  sin datos\n", SECTORES[s]);
        } /* fin if sector con datos */
    } /* fin for sectores */
}

/* Función: imprimir_ise (privada)
 * Autor: Nicolas
 * Parámetros de entrada: etiqueta, texto que precede al valor (por
 * ejemplo "ISE global"); suma y cuenta, arreglos por sector
 * Valor de salida: no tiene
 * Descripción: calcula e imprime el ISE con su recomendación, o avisa que
 * no hay datos suficientes. El llamador debe tener tomado mutex_salida. */
static void imprimir_ise(const char *etiqueta, const double suma[NUM_SECTORES],
                         const long cuenta[NUM_SECTORES]) {
    double ise;   /* resultado de calcular_ise */

    if (calcular_ise(suma, cuenta, &ise) == 0) {
        printf("  %s: %.2f -> Recomendacion: %s\n", etiqueta, ise, clasificar_ise(ise));
    } else {
        printf("  %s: sin datos suficientes -> sin recomendacion\n", etiqueta);
    } /* fin if hay datos */
}

/* Función: reporte_parcial
 * Autor: Nicolas
 * Parámetros de entrada: agente, datos del agente que acaba de terminar
 * Valor de salida: no tiene
 * Descripción: imprime encuestas recibidas (frente a las anunciadas),
 * promedio por sector, ISE del país y recomendación. Toma mutex_salida
 * para que el reporte salga completo aunque otros hilos impriman a la vez. */
void reporte_parcial(const DatosAgente *agente) {
    pthread_mutex_lock(&mutex_salida);
    printf("\n=== REPORTE PARCIAL - %s ===\n", agente->pais);
    printf("  Encuestas recibidas: %ld de %d anunciadas\n",
           agente->recibidas, agente->esperadas);
    imprimir_promedios(agente->suma, agente->cuenta);
    imprimir_ise("ISE del pais", agente->suma, agente->cuenta);
    printf("============================\n");
    fflush(stdout);
    pthread_mutex_unlock(&mutex_salida);
}

/* Función: sumar_agentes_pais (privada)
 * Autor: Nicolas
 * Parámetros de entrada: agentes y n, arreglo de agentes registrados;
 * pais, sigla a buscar; recibidas y esperadas, donde se devuelven los
 * totales del país
 * Valor de salida: estado del país: 0 sin conexión, 1 incompleto,
 * 2 finalizó
 * Descripción: recorre los agentes de ese país (puede haber más de uno)
 * y suma sus encuestas recibidas y anunciadas. El país está finalizado
 * solo si todos sus agentes enviaron su fin real. */
static int sumar_agentes_pais(const DatosAgente agentes[], int n, const char *pais,
                              long *recibidas, long *esperadas) {
    int i;
    int hay_agentes = 0;
    int incompleto = 0;

    *recibidas = 0;
    *esperadas = 0;
    for (i = 0; i < n; i++) {
        if (strcmp(agentes[i].pais, pais) == 0) {
            hay_agentes = 1;
            *recibidas += agentes[i].recibidas;
            *esperadas += agentes[i].esperadas;
            if (agentes[i].interrumpido) {
                incompleto = 1;
            }
        } /* fin if agente del país */
    } /* fin for agentes */

    if (!hay_agentes) {
        return 0;
    }
    return incompleto ? 1 : 2;
}

/* Función: reporte_final
 * Autor: Nicolas
 * Parámetros de entrada: agentes y n, arreglo con los n agentes
 * registrados; segundos, tiempo configurado con -t
 * Valor de salida: no tiene
 * Descripción: imprime el estado de cada país (finalizó, incompleto o sin
 * conexión), el total de encuestas, los promedios globales (calculados
 * sobre las encuestas de todos los países juntos), el ISE global y su
 * recomendación. Debe llamarse después del pthread_join de los
 * consumidores para no leer datos que aún se estén escribiendo. */
void reporte_final(const DatosAgente agentes[], int n, int segundos) {
    static const char *const NOMBRES_ESTADO[] = {"sin conexion", "incompleto", "finalizo"};
    double suma[NUM_SECTORES] = {0};      /* suma global por sector */
    long cuenta[NUM_SECTORES] = {0};      /* cantidad global por sector */
    long total = 0;                       /* encuestas de todos los países */
    long recibidas, esperadas;            /* totales de un país */
    int estado;
    int i, s;

    /* Los promedios globales se calculan sobre todas las encuestas juntas */
    for (i = 0; i < n; i++) {
        for (s = 0; s < NUM_SECTORES; s++) {
            suma[s] += agentes[i].suma[s];
            cuenta[s] += agentes[i].cuenta[s];
        } /* fin for sectores */
        total += agentes[i].recibidas;
    } /* fin for agentes */

    pthread_mutex_lock(&mutex_salida);
    printf("\n######## RESUMEN GENERAL ISE (%d s) ########\n", segundos);
    for (i = 0; i < NUM_PAISES; i++) {
        estado = sumar_agentes_pais(agentes, n, PAISES[i], &recibidas, &esperadas);
        printf("  %s: %-12s (%ld de %ld encuestas)\n", PAISES[i],
               NOMBRES_ESTADO[estado], recibidas, esperadas);
    } /* fin for países */
    printf("  Total de encuestas: %ld\n", total);
    printf("  Promedios globales:\n");
    imprimir_promedios(suma, cuenta);
    imprimir_ise("ISE global", suma, cuenta);
    printf("############################################\n");
    fflush(stdout);
    pthread_mutex_unlock(&mutex_salida);
}
