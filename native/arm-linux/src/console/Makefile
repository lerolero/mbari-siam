# Makefile for siamcom
# use make CC=arm-linux-gcc 
# to generate binaries for the ARM

CFLAGS = -Os -Wall 
CC = arm-linux-gcc

all: console

console:   console.c
	$(CC) $(CFLAGS) $< -o $@ 


clean:
	rm -f console
