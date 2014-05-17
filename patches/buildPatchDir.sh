#!/bin/bash

./make.sh

echo "Building patch directory..."

rm -rf patches
mkdir patches

collect() {
    patch=$1
    fileName=$2

    for filePath in out/$patch/*
    do
        version=`basename $filePath`
        echo " - Version $version of $patch"

        cp "out/$patch/$version/$fileName" "patches/${patch}_patch_$version.dll"
        strip "patches/${patch}_patch_$version.dll"

        checkSum=$(sha1sum -b "patches/${patch}_patch_$version.dll" | cut -f 1 -d ' ')
        echo "$version ${patch}_patch_$version.dll ${patch}_orig_$version.dll" >> patches/${patch}_versions.mf
    done
}

collect CvGameDatabase "CvGameDatabaseWin32Final Release.dll"
