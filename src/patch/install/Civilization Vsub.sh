#!/usr/bin/env bash

if [ "$SteamAppId" = "" ]; then
    echo "Civilization V must be launched from within Steam."
    echo "For debugging purposes, use the Launch Civilization V script instead."
    exit
fi

path="$(dirname "$0")"
export DYLD_INSERT_LIBRARIES="$path/mppatch_core.dylib"
exec -a "Civilization Vsub" "$path/Civilization Vsub orig" "$@"
