#!/bin/sh
path="`dirname "$0"`"
export LD_PRELOAD="$path/libmppatch.so"
exec -a Civ5XP "$path/Civ5XP.orig" $*