#!/bin/sh
path="$(dirname "$0")"
(echo "set env LD_LIBRARY_PATH=$path:$LD_LIBRARY_PATH"; echo "set env LD_PRELOAD=mppatch_core.so"; cat) | \
    gdb "$path/Civ5XP.orig"