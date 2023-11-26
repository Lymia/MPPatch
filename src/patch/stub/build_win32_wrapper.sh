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

src/patch/stub/make_win32_wrapper.py \
  src/patch/mppatch-core/target/i686-pc-windows-gnu/release/mppatch_core.dll \
  >src/patch/mppatch-core/target/i686-pc-windows-gnu/release/mppatch_core_wrapper.s || exit 1
nasm -f win32 \
  src/patch/mppatch-core/target/i686-pc-windows-gnu/release/mppatch_core_wrapper.s \
  -o src/patch/mppatch-core/target/i686-pc-windows-gnu/release/mppatch_core_wrapper.o || exit 1
clang -target i686-pc-windows-gnu -shared \
  src/patch/mppatch-core/target/i686-pc-windows-gnu/release/mppatch_core_wrapper.o \
  src/patch/mppatch-core/target/i686-pc-windows-gnu/release/mppatch_core.dll \
  -o src/patch/mppatch-core/target/i686-pc-windows-gnu/release/mppatch_core_wrapper.dll || exit 1
strip src/patch/mppatch-core/target/i686-pc-windows-gnu/release/mppatch_core_wrapper.dll || exit 1
