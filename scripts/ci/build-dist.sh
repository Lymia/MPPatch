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

LINUXDEPLOY_DL="https://github.com/linuxdeploy/linuxdeploy/releases/download/1-alpha-20231026-1/linuxdeploy-x86_64.AppImage"

echo "Cleaning up after previous distribution scripts..."
rm -rfv target/dist target/dist-build || exit 1
mkdir -p target/dist target/dist-build || exit 1

echo "Downloading tools..."
wget -O target/dist-build/linuxdeploy "$LINUXDEPLOY_DL" || exit 1
chmod +x target/dist-build/linuxdeploy || exit 1

echo "Gathering facts from SBT..."
sbt "print scalaVersion" || exit 1 # get results cached, required to wrangle CI
VERSION="$(sbt "print version" --error || exit 1)"
VERSION="$(echo "$VERSION" | head -n 1 | tr -d '\n')"
echo "VERSION=$VERSION"

echo "Building AppDir for Linux installer..."
mkdir -pv target/dist-build/linux/AppDir || exit 1

# Build basic directory structure
cd target/dist-build/linux/AppDir || exit 1
  tar -xv -f ../../../../target/mppatch_ci_installer-linux.tar.gz || exit 1
  mv -v assembly.jar ../.. || exit 1
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
  LDAI_COMP=xz ../linuxdeploy --appdir AppDir/ --output appimage || exit 1
cd ../../.. || exit 1

echo "Extracting Windows installer..."
cd target/dist-build || exit 1
  tar -xv -f ../../target/mppatch_ci_installer-win32.tar.gz || exit 1
cd ../.. || exit 1

# Copy to dist directory
cp -v target/dist-build/mppatch-installer.exe target/dist/MPPatch_Installer-win32-$VERSION.exe || exit 1
cp -v target/dist-build/linux/MPPatch_Installer-x86_64.AppImage target/dist/MPPatch_Installer-linux-$VERSION.AppImage || exit 1
cp -v target/dist-build/assembly.jar target/dist/MPPatch_Installer-universal-$VERSION.jar || exit 1
