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

import glob
import json
import os
import shutil
import sys

source_path = "res/noceg"
target_path = "src/patch/mppatch-core/src/data"

if os.path.exists(target_path):
    shutil.rmtree(target_path)
    os.makedirs(target_path)

for ceg_file in glob.glob(f"{source_path}/*.json"):
    print(f"Processing {ceg_file}...")

    ceg_json = json.loads(open(ceg_file).read())

    static_functions = []
    relative_functions = []
    stub_functions = []

    for function in ceg_json["ConstantOrStolen"]:
        assert len(function) == 1
        key = list(function)[0]
        data = function[key]
        assert(key == data["Prologue"])
        assert(data["Type"] == 2 or data["Type"] == 3)

        if data["IsAddress"]:
            relative_functions.append((int(key, 16), int(data["Value"], 16)))
        else:
            static_functions.append((int(key, 16), int(data["Value"], 16)))

    if "Init" in ceg_json:
        stub_functions.append(int(ceg_json["Init"], 16))
    for offset in ceg_json["Integrity"]:
        stub_functions.append(int(offset, 16))
    for offset in ceg_json["RegisterThreads"]:
        stub_functions.append(int(offset, 16))
    if "Terminate" in ceg_json:
        stub_functions.append(int(ceg_json["Terminate"], 16))
    for offset in ceg_json["TestSecret"]:
        stub_functions.append(int(offset, 16))

    string = ""
    string += "CegPatches {\n"
    string += "  static_functions: &[\n"
    for stub in sorted(static_functions):
        string += f"    ({hex(stub[0])}, {hex(stub[1])}),\n"
    string += "  ],\n"
    string += "  relative_functions: &[\n"
    for stub in sorted(relative_functions):
        string += f"    ({hex(stub[0])}, {hex(stub[1])}),\n"
    string += "  ],\n"
    string += "  stub_functions: &[\n"
    for stub in sorted(stub_functions):
        string += f"    {hex(stub)},\n"
    string += "  ],\n"
    string += "}\n"

    basename = ceg_file.split("/")[-1].replace(".json", ".inc")
    open(f"{target_path}/{basename}", "w").write(string)