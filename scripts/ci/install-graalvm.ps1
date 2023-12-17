$ErrorActionPreference = "Stop"

$GRAALVM_WIN32 = "https://download.bell-sw.com/vm/23.1.1/bellsoft-liberica-vm-openjdk21.0.1+12-23.1.1+1-windows-amd64.zip"
$GRAALVM_WIN32_DIR = "bellsoft-liberica-vm-openjdk21-23.1.1"

# Install graalvm
if (-Not(Test-Path "target/graalvm-win32" -PathType Container)) {
    echo "Downloading graalvm..."
    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest -Uri "$GRAALVM_WIN32" -OutFile "target/graalvm-win32.zip"
    Expand-Archive -Path "target/graalvm-win32.zip" -DestinationPath "target/"
    mv "target/$GRAALVM_WIN32_DIR" "target/graalvm-win32"
    rm "target/graalvm-win32.zip"
}
