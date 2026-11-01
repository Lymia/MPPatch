#!/usr/bin/env bash

export LD_LIBRARY_PATH="$path:$LD_LIBRARY_PATH"
export SteamAppId=8930
export LD_PRELOAD="mpp_hooklib.so $LD_PRELOAD"

exec -a Civ5XP "$path/Civ5XP.orig" "$@"