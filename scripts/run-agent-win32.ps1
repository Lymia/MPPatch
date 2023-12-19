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

scripts/ci/install-deps.ps1

$JAR_NAME = "$( sbt "print assembly" --error )".Trim()

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
