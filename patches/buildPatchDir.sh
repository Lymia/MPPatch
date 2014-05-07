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
        echo "  <${patch}_$version file=\"patches/${patch}_patch_$version.dll\" cpname=\"${patch}_orig_$version.dll\"/>" >> patches/manifest.xml
    done
}

echo "<?xml version="1.0" encoding="UTF-8"?>" >> patches/manifest.xml
echo "<patches>"                              >> patches/manifest.xml

collect CvGameDatabase "CvGameDatabaseWin32Final Release.dll"

echo "</patches>"                             >> patches/manifest.xml
