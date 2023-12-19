#!/bin/bash

#
# Copyright (c) 2015-2023 Lymia Kanokawa <lymia@lymia.moe>
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

GRAALVM_LINUX_URL="https://download.bell-sw.com/vm/23.1.1/bellsoft-liberica-vm-openjdk21.0.1+12-23.1.1+1-linux-amd64.tar.gz"
GRAALVM_LINUX_DIR="bellsoft-liberica-vm-openjdk21-23.1.1"
GRAALVM_LINUX_SHA="8b3549e3a53cd4bb5e27bf1fa373cf9573874218c55b70681799a68004d088fa"

LINUXDEPLOY_URL="https://github.com/linuxdeploy/linuxdeploy/releases/download/1-alpha-20231026-1/linuxdeploy-x86_64.AppImage"
LINUXDEPLOY_SHA="c242e21f573532c03adc2c356b70055ee0de2ae66e235d086b714e69d2cae529"

mkdir -pv target/deps target/deps/dl || exit 1

download_file() {
  if [ ! -f "target/deps/dl/$1" ]; then
    wget "$2" -O "target/deps/dl/$1" || exit 1
    CUR_SHA256="$(sha256sum "target/deps/dl/$1" | cut -d' ' -f 1 | tr -d '\n')"
    if [ "$CUR_SHA256" != "$3" ]; then
      rm -v "target/deps/dl/$1" || exit 1
      echo "$1: sha256 mismatch"
      exit 1
    fi
  fi
}

# GraalVM for Linux
if [ ! -d target/deps/graalvm-linux ]; then
  echo "Downloading GraalVM for Linux..."
  download_file "graalvm-linux.tar.gz" "$GRAALVM_LINUX_URL" "$GRAALVM_LINUX_SHA" || exit 1
  cd target/deps || exit 1
    tar -xv -f dl/graalvm-linux.tar.gz || exit 1
    mv -v "$GRAALVM_LINUX_DIR" graalvm-linux || exit 1
  cd ../.. || exit 1
fi

# linuxdeploy
if [ ! -f target/deps/linuxdeploy ]; then
  echo "Downloading linuxdeploy..."
  download_file "linuxdeploy" "$LINUXDEPLOY_URL" "$LINUXDEPLOY_SHA" || exit 1
  cp -v target/deps/dl/linuxdeploy target/deps/ || exit 1
  chmod -v +x target/deps/linuxdeploy || exit 1
fi
