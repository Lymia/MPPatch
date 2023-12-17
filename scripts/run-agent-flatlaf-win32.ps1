scripts/ci/install-graalvm.ps1

$ProgressPreference = 'SilentlyContinue'
iwr -Uri "https://download.formdev.com/files/flatlaf/flatlaf-demo-3.2.5.jar" -OutFile "target/flatlaf-demo-3.2.5.jar"
target/graalvm-win32/bin/java.exe `
  -agentlib:native-image-agent=config-merge-dir=scripts/native-image-config/common-flatlaf-win32 `
  -jar target/flatlaf-demo-3.2.5.jar
