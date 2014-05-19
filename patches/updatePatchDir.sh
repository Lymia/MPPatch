#!/bin/bash

./buildPatchDir.sh
rm -rfv ../src/com/lymiahugs/mod2dlc/data/patches
cp -rv patches ../src/com/lymiahugs/mod2dlc/data/patches