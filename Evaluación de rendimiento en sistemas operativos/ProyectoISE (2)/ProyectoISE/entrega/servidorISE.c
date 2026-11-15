
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <poll.h>
#include <pthread.h>
#include <signal.h>
#include <stdatomic.h>
#include <time.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include "comun.h"
#include "buffers.h"
#include "reportes.h"

#define TIEMPO_MIN    30    
#define TIEMPO_MAX    120
#define PASO_POLL_MS  100  

typedef struct {
    int   tam_buffer;
    char *pipe_nom;
    int   segundos;
    int   max_agentes;
} Config;


typedef struct {
    int         fd;                       
    int         tam_buffer;
    int         max_agentes;
    DatosAgente agentes[MAX_AGENTES];     
    int         terminado[MAX_AGENTES];   
    int         n_agentes;
} Contexto;

static atomic_int terminar = 0;          


static void uso(const char *programa) {
    fprintf(stderr, "Uso: %s -b <tamB> -p <pipeNom> -t <30-120> -a <1-%d>\n",
            programa, MAX_AGENTES);
    exit(EXIT_FAILURE);
}

static int texto_a_entero(const char *texto) {
    char *fin;
    errno = 0;
    long valor = strtol(texto, &fin, 10);
    if (*texto == '\0' || *fin != '\0' || errno != 0 || valor < 0 || valor > INT_MAX) {
        return -1;
    }
    return (int) valor;
}

static Config leer_argumentos(int argc, char *argv[]) {
    Config cfg = { -1, NULL, -1, -1 };

    if (argc != 9) {
        uso(argv[0]);
    }
    for (int i = 1; i < argc; i += 2) {
        const char *bandera = argv[i];
        char *valor = argv[i + 1];
        if (strcmp(bandera, "-b") == 0 && cfg.tam_buffer == -1) {
            cfg.tam_buffer = texto_a_entero(valor);
            if (cfg.tam_buffer < 1) {
                fprintf(stderr, "Error: -b debe ser un entero mayor o igual a 1\n");
                uso(argv[0]);
            }
        } else if (strcmp(bandera, "-p") == 0 && cfg.pipe_nom == NULL) {
            cfg.pipe_nom = valor;
        } else if (strcmp(bandera, "-t") == 0 && cfg.segundos == -1) {
            cfg.segundos = texto_a_entero(valor);
            if (cfg.segundos < TIEMPO_MIN || cfg.segundos > TIEMPO_MAX) {
                fprintf(stderr, "Error: -t debe estar entre %d y %d segundos\n",
                        TIEMPO_MIN, TIEMPO_MAX);
                uso(argv[0]);
            }
        } else if (strcmp(bandera, "-a") == 0 && cfg.max_agentes == -1) {
            cfg.max_agentes = texto_a_entero(valor);
            if (cfg.max_agentes < 1 || cfg.max_agentes > MAX_AGENTES) {
                fprintf(stderr, "Error: -a debe estar entre 1 y %d\n", MAX_AGENTES);
                uso(argv[0]);
            }
        } else {
            fprintf(stderr, "Error: bandera desconocida o repetida '%s'\n", bandera);
            uso(argv[0]);
        }
    }
    if (cfg.tam_buffer == -1 || cfg.pipe_nom == NULL ||
        cfg.segundos == -1 || cfg.max_agentes == -1) {
        fprintf(stderr, "Error: faltan banderas (-b, -p, -t y -a son obligatorias)\n");
        uso(argv[0]);
    }
    if (cfg.pipe_nom[0] == '\0') {
        fprintf(stderr, "Error: el nombre del pipe no puede estar vacio\n");
        uso(argv[0]);
    }
    return cfg;
}

static int crear_y_abrir_fifo(const char *nombre) {
    struct stat info;
    if (stat(nombre, &info) == 0) {
        if (!S_ISFIFO(info.st_mode)) {
            fprintf(stderr, "Error: '%s' existe y no es un pipe nominal\n", nombre);
            return -1;
        }
        if (unlink(nombre) == -1) {
            perror("Error al borrar el pipe anterior");
            return -1;
        }
    } else if (errno != ENOENT) {
        perror("Error al consultar el pipe");
        return -1;
    }

    if (mkfifo(nombre, 0666) == -1) {
        perror("Error al crear el pipe");
        return -1;
    }

    int fd = open(nombre, O_RDWR);
    if (fd == -1) {
        perror("Error al abrir el pipe");
        unlink(nombre);
        return -1;
    }
    return fd;
}

static void *hilo_temporizador(void *arg) {
    int segundos = *(int *) arg;
    struct timespec inicio, ahora;
    const struct timespec paso = { 0, PASO_POLL_MS * 1000000L };

    if (clock_gettime(CLOCK_MONOTONIC, &inicio) == -1) {
        perror("Error en clock_gettime");
        atomic_store(&terminar, 1);
        return NULL;
    }
    /* Duerme a ratos para poder salir antes si llega Ctrl-C */
    while (!atomic_load(&terminar)) {
        nanosleep(&paso, NULL);
        if (clock_gettime(CLOCK_MONOTONIC, &ahora) == -1) {
            perror("Error en clock_gettime");
            break;
        }
        if (ahora.tv_sec - inicio.tv_sec >= segundos) {
            break;
        }
    }
    atomic_store(&terminar, 1);
    return NULL;
}


static int buscar_agente(const Contexto *ctx, pid_t pid) {
    for (int i = 0; i < ctx->n_agentes; i++) {
        if (ctx->agentes[i].pid == pid) {
            return i;
        }
    }
    return -1;
}

static void registrar_agente(Contexto *ctx, const Mensaje *msg) {
    if (buscar_agente(ctx, msg->pid) != -1) {
        return;   /* registro repetido: se ignora */
    }
    if (ctx->n_agentes == ctx->max_agentes) {
        printf("Agente %.2s (pid %d) rechazado: ya hay %d agentes\n",
               msg->pais, (int) msg->pid, ctx->max_agentes);
        fflush(stdout);
        return;
    }

    DatosAgente *ag = &ctx->agentes[ctx->n_agentes];
    memset(ag, 0, sizeof(DatosAgente));
    ag->pid = msg->pid;
    strncpy(ag->pais, msg->pais, sizeof(ag->pais) - 1);
    ag->esperadas = msg->id_encuesta;

    if (buffer_iniciar(&ag->buffer, ctx->tam_buffer) == -1) {
        fprintf(stderr, "Error: no se pudo crear el buffer del agente %s\n", ag->pais);
        return;
    }
    int err = pthread_create(&ag->hilo, NULL, hilo_consumidor, ag);
    if (err != 0) {
        fprintf(stderr, "Error al crear el hilo consumidor: %s\n", strerror(err));
        buffer_destruir(&ag->buffer);
        return;
    }
    ctx->terminado[ctx->n_agentes] = 0;
    ctx->n_agentes++;

    printf("Agente %s conectado (pid %d), anuncia %d encuestas\n",
           ag->pais, (int) ag->pid, ag->esperadas);
    fflush(stdout);
}

static void procesar_mensaje(Contexto *ctx, const Mensaje *msg) {
    if (msg->tipo == MSG_REGISTRO) {
        registrar_agente(ctx, msg);
        return;
    }

    int pos = buscar_agente(ctx, msg->pid);
    if (pos == -1 || ctx->terminado[pos]) {
        return;   /* agente rechazado o que ya termino */
    }
    DatosAgente *ag = &ctx->agentes[pos];

    if (msg->tipo == MSG_ENCUESTA) {
        if (msg->id_sector < 0 || msg->id_sector >= NUM_SECTORES) {
            return;   /* encuesta invalida: se descarta */
        }
        buffer_insertar(&ag->buffer, msg);
    } else if (msg->tipo == MSG_FIN) {
        ctx->terminado[pos] = 1;
        buffer_insertar(&ag->buffer, msg);   /* el consumidor termina al leerlo */
        printf("Agente %s termino de enviar\n", ag->pais);
        fflush(stdout);
    }
}

static void *hilo_lector(void *arg) {
    Contexto *ctx = arg;
    Mensaje msg;
    char *crudo = (char *) &msg;
    size_t tengo = 0;   /* bytes del mensaje actual ya leidos */

    struct pollfd pfd = { ctx->fd, POLLIN, 0 };

    while (!atomic_load(&terminar)) {
        int r = poll(&pfd, 1, PASO_POLL_MS);
        if (r == -1) {
            if (errno == EINTR) continue;
            perror("Error en poll");
            break;
        }
        if (r == 0) continue;   /* nada que leer: vuelve a mirar el tiempo */

        ssize_t n = read(ctx->fd, crudo + tengo, sizeof(Mensaje) - tengo);
        if (n == -1) {
            if (errno == EINTR || errno == EAGAIN) continue;
            perror("Error al leer del pipe");
            break;
        }
        tengo += (size_t) n;
        if (tengo == sizeof(Mensaje)) {
            procesar_mensaje(ctx, &msg);
            tengo = 0;
        }
    }
    return NULL;
}


static void manejar_senal(int sig) {
    (void) sig;
    atomic_store(&terminar, 1);
}


static int ejecutar_servidor(const Config *cfg) {
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_handler = manejar_senal;
    sigemptyset(&sa.sa_mask);
    if (sigaction(SIGINT, &sa, NULL) == -1 || sigaction(SIGTERM, &sa, NULL) == -1) {
        perror("Error al configurar las senales");
        return EXIT_FAILURE;
    }

    int fd = crear_y_abrir_fifo(cfg->pipe_nom);
    if (fd == -1) {
        return EXIT_FAILURE;
    }

    Contexto *ctx = calloc(1, sizeof(Contexto));
    if (ctx == NULL) {
        perror("Error al reservar memoria");
        close(fd);
        unlink(cfg->pipe_nom);
        return EXIT_FAILURE;
    }
    ctx->fd          = fd;
    ctx->tam_buffer  = cfg->tam_buffer;
    ctx->max_agentes = cfg->max_agentes;

    printf("\n==================================================\n");
    printf("            SERVIDOR ISE - CONTROL PRINCIPAL\n");
    printf("==================================================\n");
    printf("Pipe: %s | Buffer: %d | Tiempo: %d s | Agentes max: %d\n",
           cfg->pipe_nom, cfg->tam_buffer, cfg->segundos, cfg->max_agentes);
    printf("Esperando agentes...\n");
    fflush(stdout);

    int estado = EXIT_SUCCESS;
    int segundos = cfg->segundos;
    pthread_t temporizador, lector;

    int err = pthread_create(&temporizador, NULL, hilo_temporizador, &segundos);
    if (err != 0) {
        fprintf(stderr, "Error al crear el hilo temporizador: %s\n", strerror(err));
        estado = EXIT_FAILURE;
    } else {
        err = pthread_create(&lector, NULL, hilo_lector, ctx);
        if (err != 0) {
            fprintf(stderr, "Error al crear el hilo lector: %s\n", strerror(err));
            atomic_store(&terminar, 1);
            estado = EXIT_FAILURE;
        } else {
            pthread_join(lector, NULL);
        }
        atomic_store(&terminar, 1);
        pthread_join(temporizador, NULL);
    }

    printf("\nTiempo terminado. Cerrando agentes...\n");
    fflush(stdout);

    for (int i = 0; i < ctx->n_agentes; i++) {
        DatosAgente *ag = &ctx->agentes[i];
        if (!ctx->terminado[i]) {
            Mensaje fin;
            memset(&fin, 0, sizeof(fin));
            fin.tipo = MSG_FIN;
            fin.pid = ag->pid;
            fin.id_sector = -1;
            ag->interrumpido = 1;
            buffer_insertar(&ag->buffer, &fin);
        }
    }
    for (int i = 0; i < ctx->n_agentes; i++) {
        pthread_join(ctx->agentes[i].hilo, NULL);
    }

    reporte_final(ctx->agentes, ctx->n_agentes, cfg->segundos);

    for (int i = 0; i < ctx->n_agentes; i++) {
        buffer_destruir(&ctx->agentes[i].buffer);
    }
    free(ctx);
    if (close(fd) == -1) {
        perror("Error al cerrar el pipe");
        estado = EXIT_FAILURE;
    }
    if (unlink(cfg->pipe_nom) == -1) {
        perror("Error al borrar el pipe");
        estado = EXIT_FAILURE;
    }
    return estado;
}


int main(int argc, char *argv[]) {
    Config cfg = leer_argumentos(argc, argv);

    pid_t hijo = fork();
    if (hijo == -1) {
        perror("Error en fork");
        return EXIT_FAILURE;
    }
    if (hijo == 0) {
        return ejecutar_servidor(&cfg);
    }

    /* Padre: ignora Ctrl-C para esperar a que el hijo termine de limpiar */
    signal(SIGINT, SIG_IGN);

    int estado;
    pid_t r;
    do {
        r = waitpid(hijo, &estado, 0);
    } while (r == -1 && errno == EINTR);
    if (r == -1) {
        perror("Error en waitpid");
        return EXIT_FAILURE;
    }

    if (WIFEXITED(estado) && WEXITSTATUS(estado) == EXIT_SUCCESS) {
        printf("Servidor ISE finalizado satisfactoriamente.\n");
        return EXIT_SUCCESS;
    }
    fprintf(stderr, "El servidor ISE termino con errores.\n");
    unlink(cfg.pipe_nom);   /* por si el hijo murio sin borrarlo */
    return EXIT_FAILURE;
}
