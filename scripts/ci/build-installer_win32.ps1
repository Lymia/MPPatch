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
echo "VERSION=$VERSION"
echo "FILE_VERSION=$FILE_VERSION"
echo "INSTALLER_NAME=$INSTALLER_NAME"

# Build the native-image
echo "Building native-image installer"
if (Test-Path target/native-image) {
    rm -Recurse -Force -Verbose target/native-image
}
sbt nativeImage
target/deps/rcedit.exe "target/native-image/mppatch-installer.exe" `
    --set-version-string "FileDescription" "MPPatch Installer - Native Image Installer" `
    --set-file-version "$FILE_VERSION" `
    --set-version-string "ProductName" "MPPatch" `
    --set-product-version "$VERSION" `
    --set-version-string "LegalCopyright" "(C) Lymia Kanokawa; available under the MIT License" `
    --set-version-string "OriginalFilename" "mppatch-installer.exe" `
    --set-version-string "Comments" "This is the internal installer. It should not be downloaded seperately." `
    --set-icon "scripts/res/mppatch-installer.ico" `
    --application-manifest "scripts/res/win32-manifest.xml"
editbin /SUBSYSTEM:WINDOWS "target/native-image/mppatch-installer.exe"

# Build NSIS image
echo "Building NSIS installer wrapper"
makensis scripts/res/installer.nsh

# Extract NSIS resources partition and run rcedit
[byte[]]$bytes = [System.IO.File]::ReadAllBytes("target/mppatch-installer-unmodified.exe")
[byte[]]$signature = 4, 0, 0, 0, 0xEF, 0xBE, 0xAD, 0xDE, 0x4E, 0x75, 0x6C, 0x6C
$nsisLocation = 0
for ($i = 0; $i -lt ($bytes.Count - $signature.Count); $i++) {
    if ($i % 100000 -eq 0) {
        echo "Searching for NSIS signature... $i/$( $bytes.Count )"
    }
    if ( [Linq.Enumerable]::SequenceEqual([byte[]]@($bytes[$i..($i + $signature.Count - 1)]), $signature)) {
        echo "Found NSIS signature at 0x$($i.ToString("X") )"
        $nsisLocation = $i
        break
    }
}
if ($nsisLocation -eq 0) {
    echo "NSIS signature not found?"
    exit 1
}

echo "Writing NSIS split resources..."
[System.IO.File]::WriteAllBytes("target/mppatch-installer-stub.exe", @($bytes[0..($nsisLocation - 1)]))
[System.IO.File]::WriteAllBytes("target/mppatch-installer-data.dat", @($bytes[$nsisLocation..($bytes.Count - 1)]))

echo "Building final installer..."
target/deps/rcedit.exe "target/mppatch-installer-stub.exe" `
    --set-version-string "FileDescription" "MPPatch Installer" `
    --set-file-version "$FILE_VERSION" `
    --set-version-string "ProductName" "MPPatch" `
    --set-product-version "$VERSION" `
    --set-version-string "LegalCopyright" "(C) Lymia Kanokawa; available under the MIT License" `
    --set-version-string "OriginalFilename" "$INSTALLER_NAME" `
    --set-icon "scripts/res/mppatch-installer.ico" `
    --application-manifest "scripts/res/win32-manifest.xml"
cmd /c copy /b "target\mppatch-installer-stub.exe" + "target\mppatch-installer-data.dat" "target\$INSTALLER_NAME"
