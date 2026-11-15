/*
 * generadorCSV.c
 * Genera el fichero de encuestas de un pais (encuestas_XX.csv).
 *
 * Uso:  ./generadorCSV -p <ES|FR|IT> -e <porcentaje>
 *       (las banderas pueden ir en cualquier orden)
 *
 * Proyecto Sistemas Operativos 2026-30 - Indicador de Sentimiento Economico
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include "comun.h"

/* Muestra como se usa el programa y termina con error */
static void uso(const char *programa) {
    fprintf(stderr, "Uso: %s -p <ES|FR|IT> -e <porcentaje 1-100>\n", programa);
    exit(EXIT_FAILURE);
}

/* Devuelve la posicion del pais en PAISES, o -1 si no existe */
static int buscar_pais(const char *pais) {
    for (int i = 0; i < NUM_PAISES; i++) {
        if (strcmp(pais, PAISES[i]) == 0) {
            return i;
        }
    }
    return -1;
}

/* Convierte texto a entero; devuelve -1 si el texto no es un numero valido */
static int texto_a_entero(const char *texto) {
    char *fin;
    long valor = strtol(texto, &fin, 10);
    if (*texto == '\0' || *fin != '\0') {
        return -1;
    }
    return (int) valor;
}

int main(int argc, char *argv[]) {
    char *pais = NULL;
    int porcentaje = -1;

    /* 1. Leer los argumentos: siempre van en parejas bandera-valor */
    if (argc != 5) {
        uso(argv[0]);
    }
    for (int i = 1; i < argc; i += 2) {
        if (strcmp(argv[i], "-p") == 0) {
            pais = argv[i + 1];
        } else if (strcmp(argv[i], "-e") == 0) {
            porcentaje = texto_a_entero(argv[i + 1]);
        } else {
            fprintf(stderr, "Error: bandera desconocida '%s'\n", argv[i]);
            uso(argv[0]);
        }
    }

    /* 2. Validar los valores */
    if (pais == NULL) {
        fprintf(stderr, "Error: falta el pais (-p)\n");
        uso(argv[0]);
    }
    int id_pais = buscar_pais(pais);
    if (id_pais == -1) {
        fprintf(stderr, "Error: pais '%s' no valido, debe ser ES, FR o IT\n", pais);
        uso(argv[0]);
    }
    if (porcentaje < 1 || porcentaje > 100) {
        fprintf(stderr, "Error: el porcentaje debe ser un entero entre 1 y 100\n");
        uso(argv[0]);
    }

    /* 3. Calcular cuantas encuestas van por sector (redondeando) */
    int cantidad[NUM_SECTORES];
    int total = 0;
    for (int s = 0; s < NUM_SECTORES; s++) {
        cantidad[s] = (int) (MAXIMOS[id_pais][s] * porcentaje / 100.0 + 0.5);
        total += cantidad[s];
    }

    /* 4. Crear el fichero */
    char nombre[64];
    snprintf(nombre, sizeof(nombre), "encuestas_%s.csv", pais);

    FILE *f = fopen(nombre, "w");
    if (f == NULL) {
        perror("Error al crear el fichero");
        return EXIT_FAILURE;
    }

    printf("Generando fichero %s para pais %s con %d%% de encuestas\n",
           nombre, pais, porcentaje);

    /* 5. Escribir encabezado y encuestas */
    if (fprintf(f, "ID_Encuesta,ID_Pais,Pais,Sector,ID_Sector,Sentimiento\n") < 0) {
        perror("Error al escribir en el fichero");
        fclose(f);
        return EXIT_FAILURE;
    }

    srand((unsigned int) time(NULL));   /* semilla para los aleatorios */
    int id_encuesta = 1;

    for (int s = 0; s < NUM_SECTORES; s++) {
        for (int k = 0; k < cantidad[s]; k++) {
            double sentimiento = (double) rand() / RAND_MAX * 100.0;
            if (fprintf(f, "%d,%d,%s,%s,%d,%.2f\n", id_encuesta, id_pais, pais,
                        SECTORES[s], s, sentimiento) < 0) {
                perror("Error al escribir en el fichero");
                fclose(f);
                return EXIT_FAILURE;
            }
            id_encuesta++;
        }
    }

    if (fclose(f) != 0) {
        perror("Error al cerrar el fichero");
        return EXIT_FAILURE;
    }

    /* 6. Mostrar el resumen */
    printf("Máximos por sector:\n");
    for (int s = 0; s < NUM_SECTORES; s++) {
        printf("  %-13s: %d/%d encuestas (%d%% del máximo)\n",
               SECTORES[s], cantidad[s], MAXIMOS[id_pais][s], porcentaje);
    }
    printf("\nTotal encuestas: %d\n", total);

    return EXIT_SUCCESS;
}
