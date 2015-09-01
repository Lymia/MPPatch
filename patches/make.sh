#!/bin/bash

FLAGS=""
if [ "$1" = "--debug" ]
then
    FLAGS="-DDEBUG"
fi

patch_files() {
    name=$1
    fileName=$2
    shift; shift

    mkdir out/$name

    echo "Building patches for file $name."
    for filePath in binaries/${name}_*.dll
    do
        echo " - Building patch for $filePath"
        if [ ! -f "$filePath" ]; then
            echo "Could not find $filePath"
            exit 1
        fi
        checkSum=$(sha1sum -b "$filePath" | cut -f 1 -d ' ')
        if [ ! -d $name/versions/$checkSum/ ]; then
            echo "Unknown checksum $checkSum for file $filePath"
            exit 1
        fi
        
        outDir="out/$name/$checkSum"
        mkdir $outDir

        echo "   - Creating symbol dump"
        cp "$filePath" $outDir/${name}_orig_$checkSum.dll
        objdump -x $outDir/${name}_orig_$checkSum.dll |
            grep -A99999999 "Ordinal/Name Pointer" | grep -B99999999 -m 1 "^$" |
            tail -n +2 | sed "s/\s*\[[ 0-9]\+\] /proxy_symbol /g" > $outDir/${name}_symbols.s

        echo "   - Compiling assembly"
        nasm $FLAGS -Ox -f win32 -o $outDir/as_$name.obj $name/versions/$checkSum/as_entry.s
        echo "   - Compiling $fileName"
        i686-w64-mingw32-gcc $FLAGS -g -shared -O2 --std=gnu99 -o "$outDir/$fileName" \
            $name/versions/$checkSum/c_entry.c $outDir/as_$name.obj \
            -Wl,--enable-stdcall-fixup $* -Wl,-Bstatic \
            -lssp -fstack-protector -fstack-protector-all -D_FORTIFY_SOURCE=2 \
            --param ssp-buffer-size=4 -Wl,--dynamicbase,--nxcompat
    done
}

rm -rf out
mkdir out

patch_files CvGameDatabase "CvGameDatabaseWin32Final Release.dll" -Wl,-L,"binaries" -l lua51_Win32 -I /usr/include/lua5.1/
