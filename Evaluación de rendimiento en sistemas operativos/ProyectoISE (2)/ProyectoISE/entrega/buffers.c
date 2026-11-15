/***********************************************************************
 * Módulo: buffer circular por agente e hilo consumidor
 * Archivo: buffers.c
 * Autor: Nicolas Infante, Juan Quintero, Miguel Castillo, Santiago Gutierrez
 * Contiene: buffer_iniciar, buffer_destruir, buffer_insertar,
 *           buffer_extraer, hilo_consumidor y las auxiliares privadas
 *           esperar, avisar, bloquear y soltar.
 * Fecha: noviembre 2026
 * Proyecto Sistemas Operativos 2026-30 - Indicador de Sentimiento Económico
 *
 * Sincronización: problema productor-consumidor con tres elementos.
 *   vacios (semáforo, inicia en capacidad): cuenta los espacios libres;
 *       el productor espera aquí antes de escribir.
 *   llenos (semáforo, inicia en 0): cuenta los mensajes disponibles; el
 *       consumidor espera aquí antes de leer.
 *   mutex  (pthread_mutex_t): protege los índices y el arreglo.
 * Terminación: no hace falta "cerrar" el buffer. El consumidor termina al
 * leer un MSG_FIN; el servidor se lo inserta a los agentes que no
 * alcanzaron a enviarlo, así ningún hilo queda bloqueado.
 ***********************************************************************/
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include "buffers.h"
#include "reportes.h"

/* Función: esperar (privada)
 * Autor: Nicolas
 * Parámetros de entrada: s, semáforo sobre el que se hace sem_wait
 * Valor de salida: 0 si se obtuvo el semáforo, -1 si hubo un error real
 * Descripción: sem_wait que se reintenta cuando una señal lo interrumpe
 * (EINTR) y que valida el retorno con perror en cualquier otro error. */
static int esperar(sem_t *s) {
    while (sem_wait(s) == -1) {
        if (errno != EINTR) {
            perror("sem_wait");
            return -1;
        } /* fin if error distinto de EINTR */
    } /* fin while reintento */
    return 0;
}

/* Función: avisar (privada)
 * Autor: Nicolas
 * Parámetros de entrada: s, semáforo sobre el que se hace sem_post
 * Valor de salida: 0 si ok, -1 si falla
 * Descripción: sem_post con validación del retorno. */
static int avisar(sem_t *s) {
    if (sem_post(s) == -1) {
        perror("sem_post");
        return -1;
    }
    return 0;
}

/* Función: bloquear (privada)
 * Autor: Nicolas
 * Parámetros de entrada: m, mutex a tomar
 * Valor de salida: 0 si ok, -1 si falla
 * Descripción: pthread_mutex_lock con validación del retorno (las
 * funciones pthread devuelven el código de error, no usan errno). */
static int bloquear(pthread_mutex_t *m) {
    int err = pthread_mutex_lock(m);

    if (err != 0) {
        fprintf(stderr, "pthread_mutex_lock: %s\n", strerror(err));
        return -1;
    }
    return 0;
}

/* Función: soltar (privada)
 * Autor: Nicolas
 * Parámetros de entrada: m, mutex a liberar
 * Valor de salida: 0 si ok, -1 si falla
 * Descripción: pthread_mutex_unlock con validación del retorno. */
static int soltar(pthread_mutex_t *m) {
    int err = pthread_mutex_unlock(m);

    if (err != 0) {
        fprintf(stderr, "pthread_mutex_unlock: %s\n", strerror(err));
        return -1;
    }
    return 0;
}

/* Función: buffer_iniciar
 * Autor: Nicolas
 * Parámetros de entrada: b, buffer a iniciar; capacidad, número de
 * mensajes que caben (tamB, debe ser >= 1)
 * Valor de salida: 0 si ok, -1 si la capacidad es inválida o falla
 * malloc, sem_init o pthread_mutex_init (libera lo ya reservado)
 * Descripción: reserva el arreglo de mensajes, deja los índices en cero e
 * inicia los semáforos vacios y llenos y el mutex. */
int buffer_iniciar(Buffer *b, int capacidad) {
    int err;

    if (capacidad < 1) {
        fprintf(stderr, "buffer_iniciar: la capacidad debe ser >= 1\n");
        return -1;
    }
    b->datos = malloc((size_t) capacidad * sizeof(Mensaje));
    if (b->datos == NULL) {
        perror("buffer_iniciar: malloc");
        return -1;
    }
    b->capacidad = capacidad;
    b->entrada = 0;
    b->salida = 0;

    /* Si algo falla se destruye lo anterior para no dejar fugas */
    if (sem_init(&b->vacios, 0, (unsigned int) capacidad) == -1) {
        perror("sem_init vacios");
        free(b->datos);
        return -1;
    }
    if (sem_init(&b->llenos, 0, 0) == -1) {
        perror("sem_init llenos");
        sem_destroy(&b->vacios);
        free(b->datos);
        return -1;
    }
    err = pthread_mutex_init(&b->mutex, NULL);
    if (err != 0) {
        fprintf(stderr, "pthread_mutex_init: %s\n", strerror(err));
        sem_destroy(&b->vacios);
        sem_destroy(&b->llenos);
        free(b->datos);
        return -1;
    }
    return 0;
}

/* Función: buffer_destruir
 * Autor: Nicolas
 * Parámetros de entrada: b, buffer a destruir
 * Valor de salida: no tiene
 * Descripción: destruye semáforos y mutex y libera el arreglo. Debe
 * llamarse solo cuando ningún hilo use ya el buffer. */
void buffer_destruir(Buffer *b) {
    sem_destroy(&b->vacios);
    sem_destroy(&b->llenos);
    pthread_mutex_destroy(&b->mutex);
    free(b->datos);
    b->datos = NULL;
}

/* Función: buffer_insertar
 * Autor: Nicolas
 * Parámetros de entrada: b, buffer destino; msg, mensaje a guardar
 * Valor de salida: 0 si se guardó, -1 si falló un semáforo o el mutex
 * Descripción: (productor) espera un espacio libre, entra a la sección
 * crítica, copia el mensaje en la posición de entrada, avanza el índice de
 * forma circular y avisa que hay un mensaje más. */
int buffer_insertar(Buffer *b, const Mensaje *msg) {
    if (esperar(&b->vacios) == -1) {
        return -1;
    }
    if (bloquear(&b->mutex) == -1) {
        return -1;
    }
    b->datos[b->entrada] = *msg;
    b->entrada = (b->entrada + 1) % b->capacidad;    /* avance circular */
    if (soltar(&b->mutex) == -1) {
        return -1;
    }
    return avisar(&b->llenos);
}

/* Función: buffer_extraer
 * Autor: Nicolas
 * Parámetros de entrada: b, buffer origen; msg, donde se copia el mensaje
 * Valor de salida: 0 si se leyó un mensaje, -1 si falló un semáforo o el
 * mutex
 * Descripción: (consumidor) espera un mensaje disponible, entra a la
 * sección crítica, lo copia desde la posición de salida, avanza el índice
 * de forma circular y avisa que hay un espacio libre más. */
int buffer_extraer(Buffer *b, Mensaje *msg) {
    if (esperar(&b->llenos) == -1) {
        return -1;
    }
    if (bloquear(&b->mutex) == -1) {
        return -1;
    }
    *msg = b->datos[b->salida];
    b->salida = (b->salida + 1) % b->capacidad;      /* avance circular */
    if (soltar(&b->mutex) == -1) {
        return -1;
    }
    return avisar(&b->vacios);
}

/* Función: acumular (privada)
 * Autor: Nicolas
 * Parámetros de entrada: ag, agente dueño del acumulado; msg, encuesta
 * Valor de salida: no tiene
 * Descripción: suma el sentimiento de la encuesta al sector que le
 * corresponde. Las encuestas con sector inválido se descartan. */
static void acumular(DatosAgente *ag, const Mensaje *msg) {
    if (msg->id_sector < 0 || msg->id_sector >= NUM_SECTORES) {
        fprintf(stderr, "[%s] sector invalido (%d), encuesta descartada\n",
                ag->pais, msg->id_sector);
    } else {
        ag->recibidas++;
        ag->cuenta[msg->id_sector]++;
        ag->suma[msg->id_sector] += msg->sentimiento;
    } /* fin if sector válido */
}

/* Función: hilo_consumidor
 * Autor: Nicolas
 * Parámetros de entrada: arg, apuntador a un DatosAgente
 * Valor de salida: NULL
 * Descripción: saca mensajes del buffer del agente y acumula el
 * sentimiento por sector. Termina al leer MSG_FIN. Si el fin es real
 * (el agente terminó de enviar) imprime el reporte parcial; si lo puso el
 * servidor porque se acabó el tiempo (interrumpido = 1) no lo imprime,
 * porque el agente aparecerá como incompleto en el resumen general. Si
 * buffer_extraer falla, marca el agente como interrumpido y termina. */
void *hilo_consumidor(void *arg) {
    DatosAgente *ag = (DatosAgente *) arg;
    Mensaje msg;
    int seguir = 1;    /* 0 cuando llega MSG_FIN o hay un error */

    while (seguir) {
        if (buffer_extraer(&ag->buffer, &msg) == -1) {
            ag->interrumpido = 1;
            seguir = 0;
        } else if (msg.tipo == MSG_FIN) {
            seguir = 0;
            if (!ag->interrumpido) {
                reporte_parcial(ag);
            }
        } else if (msg.tipo == MSG_ENCUESTA) {
            acumular(ag, &msg);
        } /* fin if tipo de mensaje */
    } /* fin while consumo */
    return NULL;
}
