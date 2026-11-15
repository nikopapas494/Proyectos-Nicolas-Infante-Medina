# Makefile - Proyecto Sistemas Operativos 2026-30 (ISE)
CC     = gcc
CFLAGS = -Wall -Wextra -pthread

all: generadorCSV agentePais servidorISE

generadorCSV: generadorCSV.c comun.h
	$(CC) $(CFLAGS) -o generadorCSV generadorCSV.c

agentePais: agentePais.c comun.h
	$(CC) $(CFLAGS) -o agentePais agentePais.c

servidorISE: servidorISE.c buffers.c reportes.c comun.h buffers.h reportes.h
	$(CC) $(CFLAGS) -o servidorISE servidorISE.c buffers.c reportes.c

# Pruebas unitarias de buffers y reportes (-lm por fabs/math.h en el test)
test_parte4: test_parte4.c buffers.c reportes.c comun.h buffers.h reportes.h
	$(CC) $(CFLAGS) -o test_parte4 test_parte4.c buffers.c reportes.c -lm

# make test: pruebas unitarias; ./pruebas.sh: pruebas de integración (~30 s)
test: test_parte4
	timeout 60 ./test_parte4

clean:
	rm -f generadorCSV agentePais servidorISE test_parte4 encuestas_*.csv

.PHONY: all clean test
