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

    for filePath in out_release/$platform/*
    do
        if [ -d $filePath ]
        then
          version=`basename $filePath`
          echo " - Version $version for $platform"

          mkdir -p patches/$version/
          cp "out_release/$platform/$version/$fileName" "patches/$version/$version.$extension"
          cp "out_debug/$platform/$version/$fileName" "patches/$version/${version}_debug.$extension"
          strip "patches/$version/$version.$extension"
          echo "ORIGINAL $platform $version.$extension ${version}_debug.$extension" >> "patches/$version/version.mf"

          checksum=$(sha1sum -b "patches/$version/$version.$extension" | cut -f 1 -d ' ')
          mkdir -p patches/$checksum/
          echo "PATCHED $platform $version RELEASE" >> patches/$checksum/version.mf

          checksum=$(sha1sum -b "patches/$version/${version}_debug.$extension" | cut -f 1 -d ' ')
          mkdir -p patches/$checksum/
          echo "PATCHED $platform $version DEBUG" >> patches/$checksum/version.mf
        fi
    done
}

collect win32 "CvGameDatabaseWin32Final Release.dll" dll

rm -rf out_debug out_release
