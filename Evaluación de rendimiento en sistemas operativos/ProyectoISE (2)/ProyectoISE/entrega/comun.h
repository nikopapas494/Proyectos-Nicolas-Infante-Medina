/*
 * comun.h
 * Definiciones compartidas por generadorCSV, agentePais y servidorISE.
 * Proyecto Sistemas Operativos 2026-30 - Indicador de Sentimiento Economico
 */
#ifndef COMUN_H
#define COMUN_H

#include <sys/types.h>

#define MAX_AGENTES   4
#define NUM_SECTORES  5
#define NUM_PAISES    3

/* Tipos de mensaje que viajan por el pipe nominal */
#define MSG_REGISTRO  1   /* el agente se presenta ante el servidor */
#define MSG_ENCUESTA  2   /* una encuesta */
#define MSG_FIN       3   /* el agente termino de enviar */

typedef struct {
    int    tipo;          /* MSG_REGISTRO, MSG_ENCUESTA o MSG_FIN */
    pid_t  pid;           /* identifica a cada agente */
    char   pais[3];       /* "ES", "FR" o "IT" */
    int    id_encuesta;
    int    id_sector;     /* 0 a 4 */
    double sentimiento;   /* 0.00 a 100.00 */
} Mensaje;

/* La posicion en cada arreglo es el ID (ID_Pais, ID_Sector) */
static const char *const PAISES[NUM_PAISES] = {"ES", "FR", "IT"};
static const char *const SECTORES[NUM_SECTORES] =
    {"Industria", "Servicios", "Consumidores", "Comercio", "Construccion"};
static const double PESOS[NUM_SECTORES] = {0.40, 0.30, 0.20, 0.05, 0.05};

/* Tabla 1: maximo de encuestas por pais (fila) y sector (columna) */
static const int MAXIMOS[NUM_PAISES][NUM_SECTORES] = {
    {1760, 1430, 2020, 1080,   290},   /* ES */
    {3850, 4240, 1670, 2860, 24020},   /* FR */
    {5440, 3100, 2000, 1560,  1000}    /* IT */
};

#endif
