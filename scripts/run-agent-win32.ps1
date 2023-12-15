scripts/ci/install-graalvm.ps1

$JAR_NAME="$(sbt "print assembly" --error)".Trim()

if (Test-Path target/native-image-config-temp) {
  rm -Recurse -Force -Verbose target/native-image-config-temp
}
if (Test-Path scripts/native-image-config/win32) {
  rm -Recurse -Force -Verbose scripts/native-image-config/win32
}
mkdir scripts/native-image-config/win32

target/graalvm-win32/bin/java.exe `
  -agentlib:native-image-agent=config-output-dir=target/native-image-config-temp `
  -jar "$JAR_NAME" "@nativeImageGenerateConfig" "9e3c6db9-2a2f-4a22-9eb5-fba1a710449c"
python scripts/python/merge-configs.py win32
