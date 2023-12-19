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

$ErrorActionPreference = "Stop"

# Install graalvm
scripts/ci/install-deps.ps1

# Extract native tarballs
echo "Extracting native tarballs..."
if (Test-Path target/native-bin) {
    rm -Recurse -Force -Verbose target/native-bin
}
New-Item target/native-bin -ItemType Directory -ea 0 -Verbose
cd target/native-bin
    tar -xv -f ../../target/mppatch_ci_natives-linux.tar.gz
cd ../..

# Find the current version
$VERSION = "$( sbt "print version" --error )".Trim()
$VERSION = $VERSION.Split(" ")[0].Trim() # fix a weird Github Actions difference
$FILE_VERSION = "$VERSION".Split("-")[0]
$FILE_VERSION = "$FILE_VERSION.$( git rev-list HEAD --count )"
$INSTALLER_NAME = "MPPatch-Installer_win32_$VERSION.exe"

# Build the native-image
echo "Building native-image installer"
if (Test-Path target/native-image-win32) {
    rm -Recurse -Force -Verbose target/native-image-win32
}
sbt nativeImage
target/deps/rcedit.exe "target/native-image-win32/mppatch-installer.exe" `
    --set-version-string "FileDescription" "MPPatch Installer - Native Image Installer" `
    --set-file-version "$FILE_VERSION" `
    --set-version-string "ProductName" "MPPatch" `
    --set-product-version "$VERSION" `
    --set-version-string "LegalCopyright" "(C) Lymia Kanokawa; available under the MIT License" `
    --set-version-string "OriginalFilename" "mppatch-installer.exe" `
    --set-version-string "Comments" "This is the internal installer. It should not be downloaded seperately." `
    --set-icon "scripts/res/mppatch-installer.ico" `
    --application-manifest "scripts/res/win32-manifest.xml"
editbin /SUBSYSTEM:WINDOWS "target/native-image-win32/mppatch-installer.exe"
