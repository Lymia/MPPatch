#!/bin/sh
path="$(dirname "$0")"
export LD_LIBRARY_PATH="$path:$LD_LIBRARY_PATH"
if [ "$1" -ne "--no-steam" ]; then
    export SteamAppId=8930
fi
export LD_PRELOAD="mppatch_core.so"
exec -a Civ5XP "$path/Civ5XP.orig" "$@"