#!/usr/bin/env bash
path="$(dirname "$0")"

export LD_LIBRARY_PATH="$path:$LD_LIBRARY_PATH"
export SteamAppId=8930

echo "Please run: set env LD_PRELOAD=mppatch_core.so"
gdb "$path/Civ5XP.orig"