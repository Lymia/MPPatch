#!/bin/bash

rm -rf out out_debug out_release

./make.sh --debug
mv out out_debug
./make.sh
mv out out_release

echo "Building patch directory..."

rm -rf patches
mkdir patches

collect() {
    patch=$1
    fileName=$2

    for filePath in out_release/$patch/*
    do
        version=`basename $filePath`
        echo " - Version $version of $patch"

        cp "out_release/$patch/$version/$fileName" "patches/${patch}_patch_$version.dll"
        cp "out_debug/$patch/$version/$fileName" "patches/${patch}_patch_debug_$version.dll"
        strip "patches/${patch}_patch_$version.dll"

        checkSum=$(sha1sum -b "patches/${patch}_patch_$version.dll" | cut -f 1 -d ' ')
        echo "$version ${patch}_patch_$version.dll ${patch}_patch_debug_$version.dll ${patch}_orig_$version.dll" >> patches/${patch}_versions.mf
    done
}

collect CvGameDatabase "CvGameDatabaseWin32Final Release.dll"

rm -rf out_debug out_release