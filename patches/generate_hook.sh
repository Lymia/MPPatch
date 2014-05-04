#!/bin/sh
echo "bits 32"  >  temp.s
echo "org 0x$1" >> temp.s 
echo "jmp 0x$2" >> temp.s
nasm -o temp temp.s
xxd -p temp
rm temp temp.s