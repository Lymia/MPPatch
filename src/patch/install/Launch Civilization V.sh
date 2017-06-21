#!/usr/bin/env bash

export SteamAppId=8930
path="$(dirname "$0")"
export DYLD_INSERT_LIBRARIES="$path/mppatch_core.dylib"
exec -a "Civilization Vsub" "$path/Civilization Vsub orig" "$@"
