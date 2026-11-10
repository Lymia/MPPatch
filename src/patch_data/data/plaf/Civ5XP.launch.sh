#!/usr/bin/env bash

set -eu

path="$(dirname "$0")"
cd "$path"
bash ~/.steam/root/ubuntu12_32/steam-runtime/run.sh bash Civ5XP.launch.helper "$@"
