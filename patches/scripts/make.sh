#!/bin/bash

FLAGS=""
if [ "$1" = "--debug" ]
then
    FLAGS="-O0 -DDEBUG"
else
    FLAGS="-O2 -D_FORTIFY_SOURCE=2"
fi

rm -rf out

echo "Building CvGameDatabaseWin32Final Release.dll for Windows"
mkdir -p out/win32
echo " - Compiling lua51_Win32.dll linking stub"
i686-w64-mingw32-gcc -shared -o out/win32/lua51_Win32.dll win32/lua51_Win32/lua51_Win32.c
echo " - Generating extern_defines_gen.c"
scripts/genExternDefines.py win32/extern_defines.gen > out/win32/extern_defines_gen.c

for dirPath in win32/versions/*
do
    checkSum=$(basename $dirPath)
    echo " - Building patch for version $checkSum"

    outDir="out/win32/$checkSum"
    mkdir -p $outDir

    echo "   - Compiling assembly files"
    nasm $FLAGS -Ox -i $dirPath/ -i common/ -i win32/ -f win32 -o $outDir/as.obj common/as_entry.s
    echo "   - Compiling CvGameDatabaseWin32Final Release.dll"
    i686-w64-mingw32-gcc $FLAGS -DCV_CHECKSUM=$checkSum -flto -g -shared -O2 --std=gnu99 -o "$outDir/CvGameDatabaseWin32Final Release.dll" \
                         -I common -I win32 -I $dirPath common/*.c win32/*.c out/win32/extern_defines_gen.c $outDir/as.obj \
                         -l lua51_Win32 -Wl,-L,"out/win32" -Wl,--enable-stdcall-fixup $* -Wl,-Bstatic \
                         -lssp -fstack-protector -fstack-protector-all -Wl,--dynamicbase,--nxcompat
done

echo "Building mod2dlc_patch.so for Linux"
mkdir -p out/linux
echo " - Generating extern_defines_gen.c"
scripts/genExternDefines.py linux/extern_defines.gen > out/linux/extern_defines_gen.c

for dirPath in linux/versions/*
do
    checkSum=$(basename $dirPath)
    echo " - Building patch for version $checkSum"

    outDir="out/linux/$checkSum"
    mkdir -p $outDir

    echo "   - Compiling assembly files"
    nasm $FLAGS -Ox -i $dirPath/ -i common/ -i linux/ -f elf -o $outDir/as.o common/as_entry.s
    echo "   - Compiling mod2dlc_patch.so"
    gcc -m32 $FLAGS -flto -g -shared -O2 --std=gnu99 -o "$outDir/mod2dlc_patch.so" \
        -I /usr/include/SDL2/ ~/.steam/bin32/libSDL2-2.0.so.0 \
        -I common -I linux -I $dirPath common/*.c linux/*.c out/linux/extern_defines_gen.c $outDir/as.o \
        -ldl -fstack-protector -fstack-protector-all
done



