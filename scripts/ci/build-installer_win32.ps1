$ErrorActionPreference = "Stop"

$URL_RCEDIT="https://github.com/electron/rcedit/releases/download/v2.0.0/rcedit-x64.exe"

# Install graalvm
scripts/ci/install-graalvm.ps1

# Extract native tarballs
echo "Extracting native tarballs..."
rm -Recurse -Force -Verbose target/native-bin
New-Item target/native-bin -ItemType Directory -ea 0 -Verbose
cd target/native-bin
    tar -xv -f ../../target/mppatch_ci_natives-linux.tar.gz
cd ../..

# Download rcedit if it isn't already downloaded
if (-Not (Test-Path "target/rcedit.exe" -PathType Leaf)) {
    echo "Downloading 'rcedit.exe'..."
    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest -Uri "$URL_RCEDIT" -OutFile "target/rcedit.exe"
}

# Build the native-image
echo "Building native-image installer"
rm -Recurse -Force -Verbose target/native-image
sbt nativeImage
target/rcedit.exe "target/native-image/mppatch-installer.exe" `
    --set-icon "scripts/res/mppatch-installer.ico" `
    --application-manifest "scripts/res/win32-manifest.xml"
editbin /SUBSYSTEM:WINDOWS "target/native-image/mppatch-installer.exe"

# Build NSIS image
echo "Building NSIS installer wrapper"
makensis scripts/res/installer.nsis

# Create tarball
echo "Creating Windows installer tarball..."
cd target
    tar --gzip -cv -f "mppatch_ci_installer-win32.tar.gz" mppatch-installer.exe
cd ..
