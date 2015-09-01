#!/bin/sh
objdump -x "$1" | grep -A99999999 "Ordinal/Name Pointer" | grep -B99999999 -m 1 "^$" | tail -n +2 | sed "s/\s*\[[ 0-9]\+\] //g"
