/***********************************************************************
 * Módulo: buffer circular por agente e hilo consumidor
 * Archivo: buffers.h
 * Autor: Nicolas Infante, Juan Quintero, Miguel Castillo, Santiago Gutierrez
 * Contiene: tipos Buffer y DatosAgente, y los prototipos de las
 *           funciones del buffer (productor-consumidor con semáforos
 *           POSIX) y del hilo consumidor.
 * Fecha: noviembre 2026
 * Proyecto Sistemas Operativos 2026-30 - Indicador de Sentimiento Económico
 *
 * Cómo lo usa servidorISE.c, por cada agente aceptado:
 *   1. buffer_iniciar(&ag->buffer, tamB).
 *   2. pthread_create(&ag->hilo, NULL, hilo_consumidor, ag).
 *   3. El hilo lector llama buffer_insertar() con cada MSG_ENCUESTA y con
 *      el MSG_FIN del agente (el consumidor termina al leerlo).
 *   4. Al vencer -t, a los agentes que no terminaron se les marca
 *      interrumpido = 1 y se les inserta un MSG_FIN; luego pthread_join de
 *      todos los consumidores, reporte_final() y buffer_destruir().
 ***********************************************************************/
#ifndef BUFFERS_H
#define BUFFERS_H

#include <pthread.h>
#include <semaphore.h>
#include "comun.h"

/* Buffer circular de capacidad fija, uno por agente */
typedef struct {
    Mensaje        *datos;      /* arreglo circular de 'capacidad' mensajes */
    int             capacidad;  /* tamaño del buffer (tamB) */
    int             entrada;    /* siguiente posicion donde insertar */
    int             salida;     /* siguiente posicion de donde extraer */
    sem_t           vacios;     /* espacios libres, inicia en capacidad */
    sem_t           llenos;     /* espacios ocupados, inicia en 0 */
    pthread_mutex_t mutex;      /* protege entrada/salida/datos */
} Buffer;

/* Todo lo que el servidor sabe de un agente */
typedef struct {
    Buffer    buffer;
    pthread_t hilo;                  /* hilo consumidor de este agente */
    pid_t     pid;
    char      pais[3];
    int       esperadas;             /* total anunciado en MSG_REGISTRO */
    int       interrumpido;          /* 1 si no llego su MSG_FIN real */
    long      recibidas;             /* encuestas que proceso el consumidor */
    long      cuenta[NUM_SECTORES];  /* encuestas por sector */
    double    suma[NUM_SECTORES];    /* suma de sentimientos por sector */
} DatosAgente;

/* Las funciones devuelven 0 si todo va bien y -1 si hay error */

/* Reserva el arreglo e inicia semáforos y mutex. Error si capacidad < 1 */
int  buffer_iniciar(Buffer *b, int capacidad);

/* Destruye semáforos y mutex y libera el arreglo (ya sin hilos usándolo) */
void buffer_destruir(Buffer *b);

/* Productor: copia el mensaje en el buffer; bloquea si está lleno */
int  buffer_insertar(Buffer *b, const Mensaje *msg);

/* Consumidor: saca el mensaje más antiguo; bloquea si está vacío */
int  buffer_extraer(Buffer *b, Mensaje *msg);

/* Cuerpo del hilo consumidor; arg es un DatosAgente* */
void *hilo_consumidor(void *arg);

#endif
