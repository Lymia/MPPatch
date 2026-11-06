#!/usr/bin/env bash

set -eu
SCRIPT_PATH="$(dirname "$(realpath "${BASH_SOURCE[0]}")")"/..
cd "$SCRIPT_PATH"

# Clean old files
rm -rfv target/native-bin ||:
mkdir -pv target/native-bin target/native-bin/plaf_{linux,win32}

# Build the Rust native hook
cargo build --target i686-unknown-linux-gnu --release -p mpp_hooklib
cargo build --target i686-win7-windows-gnu --release -p mpp_hooklib
cp -v target/i686-unknown-linux-gnu/release/libmpp_hooklib.so target/native-bin/plaf_linux/
cp -v target/i686-win7-windows-gnu/release/mpp_hooklib.dll target/native-bin/plaf_win32/

# Build the LuaJIT package
build_luajit() {
    cd contrib/luajit

    TARGET_FLAGS="--target=$1 -Os -fstack-protector -fstack-protector-strong -D_FORTIFY_SOURCE=2 $2"
    shift 2
    make clean
    make --trace -j"$(nproc --all)" \
        HOST_CC="clang -m32" STATIC_CC="clang" DYNAMIC_CC="clang -fPIC" TARGET_LD="clang" \
        TARGET_FLAGS="$TARGET_FLAGS" \
        "$@"

    cd "$SCRIPT_PATH"
}

LINUX_FLAGS=""
WIN32_FLAGS="-fuse-ld=lld -static-libgcc -Wl,--start-group -lmsvcr90 -Wno-unused-command-line-argument -Wl,-Bstatic -lssp -Wl,--dynamicbase,--nxcompat"

build_luajit "i686-unknown-linux-gnu" "$LINUX_FLAGS"
cp contrib/luajit/src/libluajit.so target/native-bin/plaf_linux/
build_luajit "i686-win7-windows-gnu" "$WIN32_FLAGS" CROSS="i686-w64-mingw32-" TARGET_SYS="Windows"
cp contrib/luajit/src/lua51.dll target/native-bin/plaf_win32/
