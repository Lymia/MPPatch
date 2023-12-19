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

. scripts/ci/install-deps.sh

echo "Gathering facts from SBT..."
sbt "print scalaVersion" || exit 1 # get project files cached, required to wrangle CI
VERSION="$(sbt "print version" --error || exit 1)"
VERSION="$(echo "$VERSION" | head -n 1 | tr -d '\n')"
APPIMAGE_NAME="MPPatch-Installer_linux_$VERSION.AppImage"

echo "Cleaning up after previous scripts..."
rm -rfv target/dist-build || exit 1
mkdir -p target/dist-build || exit 1

echo "Building AppDir for Linux installer..."
mkdir -pv target/dist-build/linux/AppDir || exit 1

# Build basic directory structure
cd target/dist-build/linux/AppDir || exit 1
  cp -rv ../../../native-image-linux/* . || exit 1
  mkdir -pv usr/bin usr/lib usr/share/applications usr/share/icons/hicolor/scalable/apps || exit 1
  mv -v mppatch-installer usr/bin/ || exit 1
  mv -v *.so usr/lib/ || exit 1
  cp -v ../../../../scripts/res/mppatch-installer.desktop usr/share/applications/ || exit 1
  cp -v ../../../../scripts/res/mppatch-installer.svg usr/share/icons/hicolor/scalable/apps/ || exit 1
  for scale in {8,16,22,24,32,48,64,256}; do
    mkdir -pv usr/share/icons/hicolor/"${scale}x${scale}"/apps || exit 1
    cp -v ../../../../scripts/res/mppatch-installer-$scale.png usr/share/icons/hicolor/"${scale}x${scale}"/apps/mppatch-installer.png || exit 1
  done
cd ../../../.. || exit 1

# Build AppImage
echo "Building AppImage..."
cd target/dist-build/linux || exit 1
  LDAI_COMP=xz ../../deps/linuxdeploy --appdir AppDir/ --output appimage || exit 1
cd ../../.. || exit 1
cp -v target/dist-build/linux/MPPatch_Installer-x86_64.AppImage target/"$APPIMAGE_NAME" || exit 1
