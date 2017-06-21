#!/usr/bin/env bash
path="$(dirname "$0")"
export DYLD_INSERT_LIBRARIES="$path/mppatch_core.so"
exec -a "Civilization Vsub" "$path/Civilization Vsub orig" "$@"
