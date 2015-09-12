#!/bin/bash

scripts/buildPatchDir.sh
rm -rfv ../src/moe/lymia/multiverse/data/patches
cp -rv patches ../src/moe/lymia/multiverse/data/patches
