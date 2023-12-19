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

$GRAALVM_WIN32_URL = "https://download.bell-sw.com/vm/23.1.1/bellsoft-liberica-vm-openjdk21.0.1+12-23.1.1+1-windows-amd64.zip"
$GRAALVM_WIN32_DIR = "bellsoft-liberica-vm-openjdk21-23.1.1"
$GRAALVM_WIN32_SHA = "edf9abd89a5da392488517e5135e1ac158f37e44c6c677cd7c784937b515dbdb"

$RCEDIT_URL = "https://github.com/electron/rcedit/releases/download/v2.0.0/rcedit-x64.exe"
$RCEDIT_SHA = "3e7801db1a5edbec91b49a24a094aad776cb4515488ea5a4ca2289c400eade2a"

function Download-Dependency {
    param (
        [string]$Name,
        [string]$Uri,
        [string]$Sha256
    )

    if (-Not(Test-Path "target/deps/dl/$Name" -PathType Container)) {
        $ProgressPreference = 'SilentlyContinue'
        Invoke-WebRequest -Uri "$Uri" -OutFile "target/deps/dl/$Name"
        $FileSha256=$(Get-FileHash "target/deps/dl/$Name" -Algorithm SHA256).Hash
        echo "${Name}: expected - $Sha256, found - $FileSha256"
        if ($Sha256 -ne $FileSha256) {
            Remove-Item "target/deps/dl/$Name"
            throw "${Name}: sha256 mismatch"
        }
    }
}

New-Item "target/deps/dl" -ItemType Directory -ea 0 -Verbose

# Install GraalVM for Windows
if (-Not(Test-Path "target/deps/graalvm-win32" -PathType Container)) {
    echo "Downloading GraalVM for Windows..."
    Download-Dependency -Name "graalvm-win32.zip" -Uri "$GRAALVM_WIN32_URL" -Sha256 "$GRAALVM_WIN32_SHA"
    Expand-Archive -Path "target/deps/dl/graalvm-win32.zip" -DestinationPath "target/deps/graalvm-win32-tmp"
    Move-Item "target/deps/graalvm-win32-tmp/$GRAALVM_WIN32_DIR" "target/deps/graalvm-win32"
    Remove-Item "target/deps/graalvm-win32-tmp"
}

# Install rcedit
if (-Not (Test-Path "target/deps/rcedit.exe" -PathType Leaf)) {
    echo "Downloading rcedit..."
    Download-Dependency -Name "rcedit.exe" -Uri "$RCEDIT_URL" -Sha256 "$RCEDIT_SHA"
    Copy-Item "target/deps/dl/rcedit.exe" "target/deps/rcedit.exe"
}
