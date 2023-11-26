#!/usr/bin/env python3

#  Copyright (c) 2015-2023 Lymia Kanokawa <lymia@lymia.moe>
#
#  Permission is hereby granted, free of charge, to any person obtaining a copy
#  of this software and associated documentation files (the "Software"), to deal
#  in the Software without restriction, including without limitation the rights
#  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
#  copies of the Software, and to permit persons to whom the Software is
#  furnished to do so, subject to the following conditions:
#
#  The above copyright notice and this permission notice shall be included in
#  all copies or substantial portions of the Software.
#
#  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
#  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
#  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
#  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
#  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
#  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
#  THE SOFTWARE.

import subprocess
import sys

prefix = "mppatch_proxy_CvGameDatabase_"

syms = subprocess.check_output(['nm', sys.argv[1]]).decode("utf-8").split("\n")
syms = filter(lambda x: x.strip() != "" and f"_{prefix}?" in x, syms)
syms = map(lambda x: x.split(" ")[2][1+len(prefix):], syms)

print("segment .text")
for sym in syms:
    print(f"global _{sym}")
    print(f"extern _{prefix}{sym}")
    print(f"_{sym}: jmp _{prefix}{sym}")
    print(f"export {sym}")
