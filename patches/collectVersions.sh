#!/bin/bash

CIV5_PATH="/cygdrive/c/Program Files (x86)/Steam/steamapps/common/Sid Meier's Civilization V/"
collect() {
    name=$1
    fileName=$2
    shift; shift

    filePath="$CIV5_PATH$fileName"

    if [ ! -f "$filePath" ]; then
        echo "Could not find $filePath"
        exit 1
    fi
    checkSum=$(sha1sum -b "$filePath" | cut -f 1 -d ' ')

    cp "$filePath" binaries/${name}_$checkSum.dll
    echo "Collected local $fileName with checksum $checkSum."
}

collect CvGameDatabase "CvGameDatabaseWin32Final Release.dll"