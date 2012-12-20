CC = /usr/local/arm/2.95.3/bin/arm-linux-gcc
CCOPTS = -Wall -O2 -mapcs-32 -march=armv4 -mtune=strongarm1100 -fno-common -fno-strict-aliasing -pipe -mshort-load-bytes
JNI_INCLUDE=./include
ARM_LINUX_LIB=./lib
LIB_NAME=libsystemCall.so

all: libsystemCall.so

systemCallImpl.o: systemCallImpl.c moos_utils_SystemCall.h
	$(CC) -fPIC -DPIC $(CCOPTS) -o $@ -c -I$(JNI_INCLUDE) $< 

libsystemCall.so: systemCallImpl.o
	$(CC) $(CCOPTS) -shared -o $@ -L$(ARM_LINUX_LIB) $<

clean:
	rm -f *.o *.so
