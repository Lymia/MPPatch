#!/usr/bin/env bash

if [ "$SteamAppId" = "" ]; then
    echo "Civilization V must be launched from within Steam."
    echo "For debugging purposes, use the Civ5XP.launch script instead."
    exit
fi

path="$(dirname "$0")"
export LD_LIBRARY_PATH="$path:$LD_LIBRARY_PATH"
export LD_PRELOAD="mppatch_core.so $LD_PRELOAD"
exec -a Civ5XP "$path/Civ5XP.orig" "$@"
