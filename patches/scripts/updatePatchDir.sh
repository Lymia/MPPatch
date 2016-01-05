#!/bin/bash

scripts/buildPatchDir.sh
rm -rfv ../src/main/resources/moe/lymia/multiverse/data/patches
cp -rv patches ../src/main/resources/moe/lymia/multiverse/data/patches
