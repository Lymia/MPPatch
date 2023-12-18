#!/bin/bash

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

rm -vf scripts/res/mppatch-installer-*.png scripts/res/mppatch-installer.ico || exit 1

for i in {16,20,24,30,32,36,40,48,60,64,72,80,96,256}; do
  resvg -w $i -h $i scripts/res/mppatch-installer.svg scripts/res/mppatch-installer-$i.png || exit 1
done

convert scripts/res/mppatch-installer-{16,20,24,30,32,36,40,48,60,64,72,80,96,256}.png scripts/res/mppatch-installer.ico || exit 1

for i in {8,22,512}; do
  resvg -w $i -h $i scripts/res/mppatch-installer.svg scripts/res/mppatch-installer-$i.png || exit 1
done

for i in {20,30,36,40,60,72,80,96}; do
  rm -v scripts/res/mppatch-installer-$i.png
done