#!/bin/sh
path="$(dirname "$0")"
export LD_LIBRARY_PATH="$path:$LD_LIBRARY_PATH"
export LD_PRELOAD="mppatch_core.so"
exec -a Civ5XP "$path/Civ5XP.orig" "$@"