#!/bin/bash

CIV5_PATH="/cygdrive/c/Program Files (x86)/Steam/steamapps/common/Sid Meier's Civilization V/"
WIN_PATH="/cygdrive/c/Windows/SysWOW64"
patch_files() {
    name=$1
    fileName=$2
    shift; shift

    mkdir out/$name

    for filePath in binaries/${name}_*.dll
    do
        if [ ! -f "$filePath" ]; then
            echo "Could not find $filePath"
            exit 1
        fi
        checkSum=$(sha1sum -b "$filePath" | cut -f 1 -d ' ')
        if [ ! -d $name/versions/$checkSum/ ]; then
            echo "Unknown checksum $checkSum for file $filePath"
            exit 1
        fi
        mkdir out/$name/$checkSum

        cp "$filePath" out/$name/$checkSum/${name}_orig_$checkSum.dll
        objdump -x out/$name/$checkSum/${name}_orig_$checkSum.dll |
            grep -A99999999 "Ordinal/Name Pointer" | grep -B99999999 -m 1 "^$" |
            tail -n +2 | sed "s/\s*\[[ 0-9]\+\] /proxy_symbol /g" > out/$name/$checkSum/${name}_symbols.s

        nasm -Ox -f win32 -o out/$name/$checkSum/as_$name.obj $name/versions/$checkSum/as_entry.s
        i686-w64-mingw32-gcc -g -shared -O2 --std=gnu99 -o "out/$name/$checkSum/$fileName" \
            $name/versions/$checkSum/c_entry.c out/$name/$checkSum/as_$name.obj \
            -Wl,--enable-stdcall-fixup -Wl,-L,"$CIV5_PATH" $* \
            -Wl,-Bstatic -lssp -fstack-protector -fstack-protector-all -D_FORTIFY_SOURCE=2 \
            --param ssp-buffer-size=4 -Wl,--dynamicbase,--nxcompat
    done
}

rm -rf out
mkdir out

patch_files CvGameDatabase "CvGameDatabaseWin32Final Release.dll" -l lua51_Win32