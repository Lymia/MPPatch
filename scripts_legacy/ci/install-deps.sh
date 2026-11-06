#!/usr/bin/env bash

#
# Copyright (c) 2015-2026 Rimi Kanokawa <rimi@rimin.moe>
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.
#

GRAALVM_LINUX_URL="https://download.bell-sw.com/vm/25.0.4.1/bellsoft-liberica-vm-openjdk25.0.4.1+2-25.0.4.1+1-linux-amd64.tar.gz"
GRAALVM_LINUX_DIR="bellsoft-liberica-vm-openjdk25-25.0.4.1"
GRAALVM_LINUX_SHA="66f1fb47df32c7c81e0a1adca5546ddcf208b31311d95638e2fc38e8fa610a21"

LINUXDEPLOY_URL="https://github.com/linuxdeploy/linuxdeploy/releases/download/1-alpha-20251107-1/linuxdeploy-x86_64.AppImage"
LINUXDEPLOY_SHA="c20cd71e3a4e3b80c3483cef793cda3f4e990aca14014d23c544ca3ce1270b4d"

mkdir -pv target/deps target/deps/dl

download_file() {
  if [ ! -f "target/deps/dl/$1" ]; then
    wget "$2" -O "target/deps/dl/$1"
    CUR_SHA256="$(sha256sum "target/deps/dl/$1" | cut -d' ' -f 1 | tr -d '\n')"
    if [ "$CUR_SHA256" != "$3" ]; then
      rm -v "target/deps/dl/$1"
      echo "$1: sha256 mismatch"
      exit 1
    fi
  fi
}

# GraalVM for Linux
if [ ! -d target/deps/graalvm-linux ]; then
  echo "Downloading GraalVM for Linux..."
  download_file "graalvm-linux.tar.gz" "$GRAALVM_LINUX_URL" "$GRAALVM_LINUX_SHA"
  cd target/deps
    tar -xv -f dl/graalvm-linux.tar.gz
    mv -v "$GRAALVM_LINUX_DIR" graalvm-linux
  cd ../..
fi

# linuxdeploy
if [ ! -f target/deps/linuxdeploy ]; then
  echo "Downloading linuxdeploy..."
  download_file "linuxdeploy" "$LINUXDEPLOY_URL" "$LINUXDEPLOY_SHA"
  cp -v target/deps/dl/linuxdeploy target/deps/
  chmod -v +x target/deps/linuxdeploy
fi
