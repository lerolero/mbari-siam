# Makefile for siamcom
# use make CC=arm-linux-gcc 
# to generate binaries for the ARM

CFLAGS = -Os -Wall 
#CC = arm-linux-gcc
CC = gcc

all: commandRunner

commandRunner:   commandRunner.c
	$(CC) $(CFLAGS) $< -o $@ 


clean:
	rm -f commandRunner
