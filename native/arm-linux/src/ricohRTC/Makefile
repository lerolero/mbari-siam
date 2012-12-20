CC = arm-linux-gcc
CCOPTS = -s -Wall -I. -march=armv4 -mtune=strongarm1100 -pipe -mshort-load-bytes -Wstrict-prototypes

all: ricohRTC

ricohRTC: ricohRTC.c ricohRTC.h
	$(CC) $(CCOPTS) -o $@ $<

test: test.c ricohRTC.h
	$(CC) $(CCOPTS) -o $@ $<


