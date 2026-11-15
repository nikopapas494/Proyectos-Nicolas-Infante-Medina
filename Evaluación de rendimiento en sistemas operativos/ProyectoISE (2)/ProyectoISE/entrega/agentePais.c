#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <signal.h>
#include <unistd.h>
#include <sys/stat.h>
#include "comun.h"

#define TAM_LINEA       256   /* longitud maxima de una linea del CSV       */
#define LOTE_PROGRESO   500   /* cada cuantas encuestas se muestra progreso */
#define MAX_ESPERA_PIPE 10    /* segundos maximos esperando a que exista    */

/* Una encuesta leida del CSV (solo lo que necesita el servidor) */
typedef struct {
    int    id_encuesta;
    int    id_sector;
    double sentimiento;
} Encuesta;

/* Muestra como se usa el programa y termina con error */
static void uso(const char *programa) {
    fprintf(stderr, "Uso: %s -n <encuestas_XX.csv> -p <pipeNom>\n", programa);
    exit(EXIT_FAILURE);
}

/* Devuelve 1 si la linea solo tiene espacios o saltos de linea */
static int linea_vacia(const char *linea) {
    for (; *linea != '\0'; linea++) {
        if (*linea != ' ' && *linea != '\t' && *linea != '\r' && *linea != '\n') {
            return 0;
        }
    }
    return 1;
}

/* Devuelve 1 si el codigo de pais esta en PAISES */
static int pais_valido(const char *pais) {
    for (int i = 0; i < NUM_PAISES; i++) {
        if (strcmp(pais, PAISES[i]) == 0) {
            return 1;
        }
    }
    return 0;
}

static Encuesta *leer_encuestas(const char *archivo, char pais[3], int *total) {
    char linea[TAM_LINEA];

    FILE *f = fopen(archivo, "r");
    if (f == NULL) {
        perror("Error al abrir el fichero de encuestas");
        return NULL;
    }

    /* 1. Saltar el encabezado */
    if (fgets(linea, sizeof(linea), f) == NULL) {
        fprintf(stderr, "Error: el fichero '%s' esta vacio\n", archivo);
        fclose(f);
        return NULL;
    }

    /* 2. Contar las encuestas */
    int cantidad = 0;
    while (fgets(linea, sizeof(linea), f) != NULL) {
        if (!linea_vacia(linea)) {
            cantidad++;
        }
    }
    if (ferror(f)) {
        perror("Error al leer el fichero de encuestas");
        fclose(f);
        return NULL;
    }
    if (cantidad == 0) {
        fprintf(stderr, "Error: el fichero '%s' no tiene encuestas\n", archivo);
        fclose(f);
        return NULL;
    }

    /* 3. Reservar memoria para todas las encuestas */
    Encuesta *encuestas = malloc((size_t) cantidad * sizeof(Encuesta));
    if (encuestas == NULL) {
        perror("Error al reservar memoria para las encuestas");
        fclose(f);
        return NULL;
    }

    /* 4. Volver al inicio, saltar encabezado y guardar cada encuesta */
    rewind(f);
    if (fgets(linea, sizeof(linea), f) == NULL) {
        perror("Error al releer el fichero de encuestas");
        free(encuestas);
        fclose(f);
        return NULL;
    }

    int n = 0;
    int num_linea = 1;
    pais[0] = '\0';

    while (n < cantidad && fgets(linea, sizeof(linea), f) != NULL) {
        num_linea++;
        if (linea_vacia(linea)) {
            continue;
        }

        /* Formato: ID_Encuesta,ID_Pais,Pais,Sector,ID_Sector,Sentimiento */
        int id_enc, id_pais, id_sector;
        char pais_linea[3], sector[32];
        double sentimiento;
        int campos = sscanf(linea, "%d,%d,%2[^,],%31[^,],%d,%lf",
                            &id_enc, &id_pais, pais_linea, sector,
                            &id_sector, &sentimiento);

        if (campos != 6 || !pais_valido(pais_linea) ||
            id_sector < 0 || id_sector >= NUM_SECTORES ||
            strcmp(sector, SECTORES[id_sector]) != 0 ||
            sentimiento < 0.0 || sentimiento > 100.0) {
            fprintf(stderr, "Error: linea %d mal formada en '%s'\n",
                    num_linea, archivo);
            free(encuestas);
            fclose(f);
            return NULL;
        }

        /* Todas las encuestas del fichero deben ser del mismo pais */
        if (pais[0] == '\0') {
            strcpy(pais, pais_linea);
        } else if (strcmp(pais, pais_linea) != 0) {
            fprintf(stderr, "Error: linea %d es del pais %s, se esperaba %s\n",
                    num_linea, pais_linea, pais);
            free(encuestas);
            fclose(f);
            return NULL;
        }

        encuestas[n].id_encuesta = id_enc;
        encuestas[n].id_sector   = id_sector;
        encuestas[n].sentimiento = sentimiento;
        n++;
    }

    if (fclose(f) != 0) {
        perror("Error al cerrar el fichero de encuestas");
        free(encuestas);
        return NULL;
    }

    *total = n;
    return encuestas;
}

static int abrir_pipe(const char *nombre) {
    struct stat info;
    int segundos = 0;

    while (stat(nombre, &info) == -1) {
        if (errno != ENOENT) {
            perror("Error al consultar el pipe");
            return -1;
        }
        if (segundos == MAX_ESPERA_PIPE) {
            fprintf(stderr, "Error: el pipe '%s' no existe. "
                            "Primero debe iniciarse el servidor ISE\n", nombre);
            return -1;
        }
        if (segundos == 0) {
            printf("Esperando a que el servidor cree el pipe '%s'...\n", nombre);
            fflush(stdout);
        }
        sleep(1);
        segundos++;
    }

    if (!S_ISFIFO(info.st_mode)) {
        fprintf(stderr, "Error: '%s' existe pero no es un pipe nominal\n", nombre);
        return -1;
    }

    int fd;
    do {
        fd = open(nombre, O_WRONLY);
    } while (fd == -1 && errno == EINTR);

    if (fd == -1) {
        perror("Error al abrir el pipe");
    }
    return fd;
}

static int enviar_mensaje(int fd, const Mensaje *msg) {
    ssize_t escritos;
    do {
        escritos = write(fd, msg, sizeof(Mensaje));
    } while (escritos == -1 && errno == EINTR);

    if (escritos != (ssize_t) sizeof(Mensaje)) {
        if (escritos == -1 && errno == EPIPE) {
            fprintf(stderr, "\nError: el servidor cerro el pipe "
                            "(posiblemente se acabo su tiempo)\n");
        } else {
            perror("\nError al escribir en el pipe");
        }
        return -1;
    }
    return 0;
}

/* Prepara un mensaje de control (registro o fin) */
static void armar_control(Mensaje *msg, int tipo, const char *pais, int cantidad) {
    memset(msg, 0, sizeof(Mensaje));
    msg->tipo        = tipo;
    msg->pid         = getpid();
    strcpy(msg->pais, pais);
    msg->id_encuesta = cantidad;
    msg->id_sector   = -1;
}

int main(int argc, char *argv[]) {
    char *archivo = NULL;
    char *pipe_nom = NULL;

    /* El mensaje cabe en un write atomico del pipe */
    _Static_assert(sizeof(Mensaje) <= PIPE_BUF, "Mensaje mas grande que PIPE_BUF");

    /* 1. Leer los argumentos: siempre van en parejas bandera-valor */
    if (argc != 5) {
        uso(argv[0]);
    }
    for (int i = 1; i < argc; i += 2) {
        if (strcmp(argv[i], "-n") == 0) {
            archivo = argv[i + 1];
        } else if (strcmp(argv[i], "-p") == 0) {
            pipe_nom = argv[i + 1];
        } else {
            fprintf(stderr, "Error: bandera desconocida '%s'\n", argv[i]);
            uso(argv[0]);
        }
    }
    if (archivo == NULL) {
        fprintf(stderr, "Error: falta el fichero de encuestas (-n)\n");
        uso(argv[0]);
    }
    if (pipe_nom == NULL) {
        fprintf(stderr, "Error: falta el pipe nominal (-p)\n");
        uso(argv[0]);
    }

    /* Si el servidor cierra el pipe, write() devuelve EPIPE en vez de
       matar el proceso con SIGPIPE; asi podemos avisar y liberar todo. */
    if (signal(SIGPIPE, SIG_IGN) == SIG_ERR) {
        perror("Error al configurar SIGPIPE");
        return EXIT_FAILURE;
    }

    /* 2. Leer las encuestas del CSV a memoria */
    char pais[3];
    int total = 0;
    Encuesta *encuestas = leer_encuestas(archivo, pais, &total);
    if (encuestas == NULL) {
        return EXIT_FAILURE;
    }

    printf("\n==================================================\n");
    printf("          AGENTE - ENVIO DE ENCUESTAS\n");
    printf("==================================================\n\n");
    printf("País: %s\n", pais);
    printf("Archivo: %s\n", archivo);
    printf("Pipe: %s\n", pipe_nom);
    printf("Leídas %d encuestas.\n", total);

    /* 3. Conectar con el servidor */
    printf("Conectando al proceso principal...\n");
    fflush(stdout);
    int fd = abrir_pipe(pipe_nom);
    if (fd == -1) {
        free(encuestas);
        return EXIT_FAILURE;
    }
    printf("Conectado.\n");

    /* 4. Presentarse ante el servidor */
    Mensaje msg;
    armar_control(&msg, MSG_REGISTRO, pais, total);
    if (enviar_mensaje(fd, &msg) == -1) {
        close(fd);
        free(encuestas);
        return EXIT_FAILURE;
    }

    /* 5. Enviar las encuestas mostrando el progreso por lotes */
    printf("Enviando...\n");
    fflush(stdout);

    memset(&msg, 0, sizeof(Mensaje));
    msg.tipo = MSG_ENCUESTA;
    msg.pid  = getpid();
    strcpy(msg.pais, pais);

    int enviadas = 0;
    for (int i = 0; i < total; i++) {
        msg.id_encuesta = encuestas[i].id_encuesta;
        msg.id_sector   = encuestas[i].id_sector;
        msg.sentimiento = encuestas[i].sentimiento;

        if (enviar_mensaje(fd, &msg) == -1) {
            fprintf(stderr, "Se alcanzaron a enviar %d/%d encuestas.\n",
                    enviadas, total);
            close(fd);
            free(encuestas);
            return EXIT_FAILURE;
        }
        enviadas++;

        if (enviadas % LOTE_PROGRESO == 0) {
            printf("  %d/%d\n", enviadas, total);
            fflush(stdout);
        }
    }
    printf("\nEnviadas %d/%d encuestas.\n", enviadas, total);

    /* 6. Avisar al servidor que este agente termino */
    armar_control(&msg, MSG_FIN, pais, enviadas);
    if (enviar_mensaje(fd, &msg) == -1) {
        close(fd);
        free(encuestas);
        return EXIT_FAILURE;
    }
    printf("Mensaje de finalización enviado.\n");

    /* 7. Liberar recursos y devolver la terminal */
    free(encuestas);
    if (close(fd) == -1) {
        perror("Error al cerrar el pipe");
        return EXIT_FAILURE;
    }

    printf("Envío %s finalizado satisfactoriamente!!.\n\n", pais);
    return EXIT_SUCCESS;
}
