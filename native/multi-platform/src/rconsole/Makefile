# Makefile for siamcom
# use make CC=arm-linux-gcc 
# to generate binaries for the ARM

CFLAGS = -Os -Wall 
#CC = arm-linux-gcc
CC = gcc

all: rconsole

rconsole:   rconsole.c
	$(CC) $(CFLAGS) $< -o $@ 


clean:
	rm -f rconsole
