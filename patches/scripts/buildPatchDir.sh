#!/bin/bash

rm -rf out out_debug out_release

scripts/make.sh --debug
mv out out_debug
scripts/make.sh
mv out out_release

echo "Building patch directory..."

rm -rf patches
mkdir patches

collect() {
    platform=$1
    fileName=$2
    extension=$3

    mkdir -p patches/$platform
    for filePath in out_release/$platform/*
    do
        if [ -d $filePath ]
        then
          version=`basename $filePath`
          echo " - Version $version for $platform"

          cp "out_release/$platform/$version/$fileName" "patches/$platform/$version.$extension"
          cp "out_debug/$platform/$version/$fileName" "patches/$platform/${version}_debug.$extension"
          echo "$version.$extension ${version}_debug.$extension" >> "patches/$platform/$version.files"
        fi
    done
}

collect win32 "CvGameDatabaseWin32Final Release.dll" dll
collect linux "mvmm_patch.so" so

rm -rf out_debug out_release
