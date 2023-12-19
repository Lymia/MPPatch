#!/usr/bin/env bash
path="$(dirname "$0")"

export LD_LIBRARY_PATH="$path:$LD_LIBRARY_PATH"
export SteamAppId=8930

export LD_PRELOAD="mppatch_core.so $LD_PRELOAD"

exec -a Civ5XP "$path/Civ5XP.orig" "$@"