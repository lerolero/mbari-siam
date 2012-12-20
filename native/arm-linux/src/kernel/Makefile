#
# Makefile for misc devices that really don't fit anywhere else.
#
# Note! Dependencies are done automagically by 'make dep', which also
# removes any old dependencies. DON'T put your own dependencies here
# unless it's something special (ie not a .c file).
#
# Note 2! The CFLAGS definitions are now inherited from the
# parent makes..
#

O_TARGET := misc.o

obj-m += sa1100spi.o
obj-m += sa1100gpio.o
obj-m += sa1100lddio.o

include $(TOPDIR)/Rules.make

fastdep:
