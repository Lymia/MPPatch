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

echo "Extracting native tarballs..."
rm -rfv target/native-bin || exit 1
mkdir -v target/native-bin || exit 1
cd target/native-bin || exit 1
  if [ -e ../../mppatch_win32_natives.tar.gz ]; then
    tar -xv -f ../../mppatch_linux_natives.tar.gz
  fi
  if [ -e ../../mppatch_macos_natives.tar.gz ]; then
    tar -xv -f ../../mppatch_linux_natives.tar.gz
  fi
  if [ -e ../../mppatch_linux_natives.tar.gz ]; then
    tar -xv -f ../../mppatch_linux_natives.tar.gz
  fi
cd ../.. || exit 1

echo "Building Linux installer...."
rm -rfv target/native-image || exit 1
sbt nativeImage || exit 1
chmod +x target/native-image/*.so || exit 1

echo "Building assembly jar..."
cp "$(sbt "print assembly" --error || exit 1)" target/native-image/assembly.jar || exit 1

echo "Creating Linux installer tarball..."
cd target/native-image || exit 1
  tar --gzip -cv -f mppatch_linux_installer.tar.gz * || exit 1
cd ../.. || exit 1
cp -v target/native-image/mppatch_linux_installer.tar.gz . || exit 1
