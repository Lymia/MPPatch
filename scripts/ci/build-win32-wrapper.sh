#!/usr/bin/env bash

#
# Copyright (c) 2015-2026 Rimi Kanokawa <rimi@rimin.moe>
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

set -eu

scripts/python/make-win32-wrapper-asm.py \
  target/i686-win7-windows-gnu/release/mpp_hooklib.dll \
  >target/i686-win7-windows-gnu/release/mpp_hooklib_wrapper.s
nasm -f win32 \
  target/i686-win7-windows-gnu/release/mpp_hooklib_wrapper.s \
  -o target/i686-win7-windows-gnu/release/mpp_hooklib_wrapper.o
clang -fuse-ld=lld -target i686-win7-windows-gnu -shared \
  target/i686-win7-windows-gnu/release/mpp_hooklib_wrapper.o \
  target/i686-win7-windows-gnu/release/mpp_hooklib.dll \
  -o target/i686-win7-windows-gnu/release/mpp_hooklib_wrapper.dll
strip target/i686-win7-windows-gnu/release/mpp_hooklib_wrapper.dll
