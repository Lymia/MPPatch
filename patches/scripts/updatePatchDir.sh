#!/bin/bash

scripts/buildPatchDir.sh
rm -rfv ../src/main/resources/moe/lymia/multiverse/res/patches
cp -rv patches ../src/main/resources/moe/lymia/multiverse/res/patches
