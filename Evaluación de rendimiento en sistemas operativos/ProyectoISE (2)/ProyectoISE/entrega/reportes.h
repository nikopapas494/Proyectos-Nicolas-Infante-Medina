/***********************************************************************
 * Módulo: cálculo del ISE y reportes por consola
 * Archivo: reportes.h
 * Autor: Nicolas Infante, Juan Quintero, Miguel Castillo, Santiago Gutierrez
 * Contiene: prototipos del cálculo del ISE, su clasificación y los
 *           reportes parcial (por agente) y final (general).
 * Fecha: noviembre 2026
 * Proyecto Sistemas Operativos 2026-30 - Indicador de Sentimiento Económico
 ***********************************************************************/
#ifndef REPORTES_H
#define REPORTES_H

#include "buffers.h"

/*
 * Calcula el ISE ponderado (pesos de PESOS) a partir de sumas y cantidades
 * por sector. Los sectores sin datos se omiten y los pesos restantes se
 * renormalizan. Devuelve 0 y deja el ISE en *ise; -1 si no hay ningún dato.
 */
int calcular_ise(const double suma[NUM_SECTORES], const long cuenta[NUM_SECTORES],
                 double *ise);

/* Devuelve "Vender" (>50), "Comprar" (<50) o "Neutro" (|ise-50| < 0.01) */
const char *clasificar_ise(double ise);

/* Reporte parcial de un agente (lo llama su consumidor al terminar) */
void reporte_parcial(const DatosAgente *agente);

/*
 * Resumen general con la recomendación; agentes[0..n-1] son los
 * registrados. Llamar DESPUÉS de hacer pthread_join de los consumidores.
 */
void reporte_final(const DatosAgente agentes[], int n, int segundos);

#endif
