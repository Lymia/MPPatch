#!/usr/bin/env bash
# This shell script analyzes a Windows binary to extract a hash of its .text segment.

set -eu

TEMP="$(mktemp -d --suffix=.mppatch)"
echo "Temp dir: $TEMP"

for i in "$@"; do
    objcopy --dump-section .text="$TEMP/$(basename -- "$i").text" "$i"
    sha256sum "$TEMP/$(basename -- "$i").text"
done

rm -rf "$TEMP"