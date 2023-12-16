$ErrorActionPreference = "Stop"

$URL_RCEDIT = "https://github.com/electron/rcedit/releases/download/v2.0.0/rcedit-x64.exe"

# Install graalvm
scripts/ci/install-graalvm.ps1

# Extract native tarballs
echo "Extracting native tarballs..."
if (Test-Path target/native-bin) {
    rm -Recurse -Force -Verbose target/native-bin
}
New-Item target/native-bin -ItemType Directory -ea 0 -Verbose
cd target/native-bin
tar -xv -f ../../target/mppatch_ci_natives-linux.tar.gz
cd ../..

# Download rcedit if it isn't already downloaded
if (-Not(Test-Path "target/rcedit.exe" -PathType Leaf)) {
    echo "Downloading 'rcedit.exe'..."
    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest -Uri "$URL_RCEDIT" -OutFile "target/rcedit.exe"
}

# Find the current version
$VERSION = "$( sbt "print version" --error )".Trim()
$FILE_VERSION = "$VERSION".Split("-")[0]
$FILE_VERSION = "$FILE_VERSION.$( git rev-list HEAD --count )"

# Build the native-image
echo "Building native-image installer"
if (Test-Path target/native-image) {
    rm -Recurse -Force -Verbose target/native-image
}
sbt nativeImage
target/rcedit.exe "target/native-image/mppatch-installer.exe" `
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
target/rcedit.exe "target/mppatch-installer-stub.exe" `
    --set-version-string "FileDescription" "MPPatch Installer" `
    --set-file-version "$FILE_VERSION" `
    --set-version-string "ProductName" "MPPatch" `
    --set-product-version "$VERSION" `
    --set-version-string "LegalCopyright" "(C) Lymia Kanokawa; available under the MIT License" `
    --set-version-string "OriginalFilename" "MPPatch-Installer_win32_$VERSION.exe" `
    --set-icon "scripts/res/mppatch-installer.ico" `
    --application-manifest "scripts/res/win32-manifest.xml"
Get-Content "target/mppatch-installer-stub.exe", "target/mppatch-installer-data.dat" -Encoding Byte -Read 1024 `
    | Set-Content "target/mppatch-installer.exe" -Encoding Byte

# Create tarball
echo "Creating Windows installer tarball..."
cd target
tar --gzip -cv -f "mppatch_ci_installer-win32.tar.gz" mppatch-installer.exe
cd ..
