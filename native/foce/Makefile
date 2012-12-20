# 
# Makefile for FOCE native utilities
#  
# NOTE: When you build foceio, set
# ADINC and ADLIB, and move dscud.h
# out of the way (rename it) to ensure
# that the dscud.h version from the 
# A/D library installation
# is used instead of the version from
# SIAM. Otherwise, library mismatch errors
# may occur and analog services will not run.
#

LIB=-L/usr/local/dscud5 -ldscud5 -pthread -lm
INC=-I/usr/local/dscud5

EXECS = foceio

.c.o:
	gcc -o $@ $@.c $(LIB) $(INC)

ADINC=-I/usr/local/dscud5
ADLIB=-L/usr/local/dscud5 -ldscud5 -pthread -lm

all: $(EXECS)

clean:
	rm -f *.o $(EXECS)

foceio: sensoray foceio.c
	gcc -O2 -o $@ $@.c $(ADLIB) $(ADINC) sensoray.o
	sudo chown root $@
	sudo chmod 4755 $@

adtest: adtest.c
	gcc -O2 -o $@ $@.c $(ADLIB) $(ADINC)
	sudo chown root $@
	sudo chmod 4755 $@

temp: temp.c
	gcc -O2 -o $@ $@.c
	sudo chown root $@
	sudo chmod 4755 $@

ioport: ioport.c
	gcc -O2 -o $@ $@.c
	sudo chown root $@
	sudo chmod 4755 $@

relay: relay.c
	gcc -O2 -o $@ $@.c
	sudo chown root $@
	sudo chmod 4755 $@

sensoray: sensoray.c sensoray.h
	gcc -O2 -c -o $@.o $@.c


testSensoray: sensoray testSensoray.c
	gcc -O2 -o $@ $@.c sensoray.o
	sudo chown root $@
	sudo chmod 4755 $@
