#!/bin/sh

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

GRAALVM_LINUX="https://download.bell-sw.com/vm/23.1.1/bellsoft-liberica-vm-openjdk21.0.1+12-23.1.1+1-linux-amd64.tar.gz"
GRAALVM_LINUX_DIR="bellsoft-liberica-vm-openjdk21-23.1.1"

if [ ! -d target/graalvm-linux ]; then
  wget $GRAALVM_LINUX -O target/graalvm-linux.tar.gz || exit 1
  cd target/ || exit 1
    tar -xv -f graalvm-linux.tar.gz || exit 1
    mv -v $GRAALVM_LINUX_DIR graalvm-linux || exit 1
    rm -rf graalvm-linux.tar.gz || exit 1
  cd .. || exit 1
fi
