#!/usr/bin/env python3

#  Copyright (c) 2015-2026 Rimi Kanokawa <rimi@rimin.moe>
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

import json
import sys

ceg_file = sys.argv[1]
ceg_json = json.loads(open(ceg_file).read())

stub_functions = []
for function in ceg_json["ConstantOrStolen"]:
    assert len(function) == 1
    key = list(function)[0]
    data = function[key]
    assert(key == data["Prologue"])
    assert(not "IsFunction" in data)
    assert(data["Type"] == 2 or data["Type"] == 3)

    stub_functions.append((int(key, 16), int(data["Value"], 16)))
if "Init" in ceg_json:
    stub_functions.append((int(ceg_json["Init"], 16), 1))
for offset in ceg_json["Integrity"]:
    stub_functions.append((int(offset, 16), 1))
for offset in ceg_json["RegisterThreads"]:
    stub_functions.append((int(offset, 16), 1))
if "Terminate" in ceg_json:
    stub_functions.append((int(ceg_json["Terminate"], 16), 1))
for offset in ceg_json["TestSecret"]:
    stub_functions.append((int(offset, 16), 1))

print("CegPatches {")
print("  stub_functions: &[")
for stub in sorted(stub_functions):
    print(f"    ({hex(stub[0])}, {stub[1]}),")
print("  ],")
print("}")