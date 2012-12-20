; @(#) cstart.asm 1.7 02/11/08 14:09:38 @(#) 
;
;  C startup
;
;
; set reset interrupt vector to point to cstart
;
	.pseg reset_vector,abs=0xfffe

	.data cstart

;
; C runtime startup code
;
	.pseg cstartup$code

	.global cstart
	.global __main_returned

	.extern __rominit
	.extern _main
	.extern __max_ram0_
cstart:
	mov #0x5a80,&0x120 ; turn off watchdog timer
	mov #__max_ram0_ + 1,sp ; set stack to max ram address + 1
	call #__rominit ; initialize RAM from ROM
	call #_main     ; run the C program
__main_returned:
	jmp __main_returned ; loop forever if main returns
;
; provide a null initialization descriptor, in case there is no 
; RAM initialization required, so we don't get complaints from
; the linker when trying to link rominit, which will refer
; to the initialization descriptor segment.
;
	.iseg _idesc
	.data 0
	.data 0
	.data 0
