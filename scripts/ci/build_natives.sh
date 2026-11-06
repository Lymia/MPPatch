#!/usr/bin/env bash
set -eu

echo "Building Linux native binaries..."
scripts/build_natives.sh

echo "Creating Linux native binaries tarball..."
cd target/native-bin
  tar --gzip -cv -f mppatch_ci_natives-linux.tar.gz *
cd ../..
cp -v target/native-bin/mppatch_ci_natives-linux.tar.gz target/