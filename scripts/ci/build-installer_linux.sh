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

set -eu

. scripts/ci/install-deps.sh

echo "Gathering facts from SBT..."
sbt "print scalaVersion" # get project files cached, required to wrangle CI
VERSION="$(sbt "print version" --error)"
VERSION="$(echo "$VERSION" | head -n 1 | tr -d '\n')"
APPIMAGE_NAME="MPPatch-Installer_linux_$VERSION.AppImage"

echo "Cleaning up after previous scripts..."
rm -rfv target/dist-build
mkdir -p target/dist-build

echo "Building AppDir for Linux installer..."
mkdir -pv target/dist-build/linux/AppDir

# Build basic directory structure
cd target/dist-build/linux/AppDir
  cp -rv ../../../native-image-linux/* .
  mkdir -pv usr/bin usr/lib usr/share/applications usr/share/icons/hicolor/scalable/apps
  chmod +x mppatch-installer
  mv -v mppatch-installer usr/bin/
  mv -v *.so usr/lib/
  cp -v ../../../../scripts/res/mppatch-installer.desktop usr/share/applications/
  for scale in {8,16,22,24,32,48,64,256}; do
    mkdir -pv usr/share/icons/hicolor/"${scale}x${scale}"/apps
    cp -v ../../../../scripts/res/mppatch-installer-$scale.png usr/share/icons/hicolor/"${scale}x${scale}"/apps/mppatch-installer.png
  done
cd ../../../..

# Build AppImage
echo "Building AppImage..."
cd target/dist-build/linux
  LDAI_COMP=zstd ../../deps/linuxdeploy --appdir AppDir/ --output appimage
cd ../../..
cp -v target/dist-build/linux/MPPatch_Installer-x86_64.AppImage target/"$APPIMAGE_NAME"
