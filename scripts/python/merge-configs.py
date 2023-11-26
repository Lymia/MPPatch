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

import json
import sys

platform = sys.argv[1]


def merge_jni_config(file_a, file_b, output):
    json_a = json.loads(open(file_a).read())
    json_b = json.loads(open(file_b).read())

    names = {}
    for line in json_a:
        names[line["name"]] = line
    for line in json_b:
        if line["name"] in names:
            names[line["name"]] = merge_jni_line(names[line["name"]], line)
        else:
            names[line["name"]] = line

    for name in names:
        if is_swing_class(names[name]):
            del names[name]["methods"]
            names[name]["allDeclaredMethods"] = True
        if names[name]["name"] == "java.awt.event.KeyEvent":
            del names[name]["fields"]
            names[name]["allPublicFields"] = True
        if names[name]["name"] == "sun.java2d.loops.SurfaceType":
            del names[name]["fields"]
            names[name]["allPublicFields"] = True

    print(names.keys())
    names = list(names.values())
    names.sort(key=lambda x: x["name"])
    open(output, "w").write(json.dumps(names, indent=2))


def is_swing_class(file):
    if "methods" in file:
        for method in file["methods"]:
            if method["name"] == "createUI": return True
            if method["name"] == "coalesceEvents": return True
            if method["name"] == "loadActionMap": return True
    return False


def merge_jni_line(line_a, line_b):
    if not "fields" in line_a:
        line_a["fields"] = []
    if not "fields" in line_b:
        line_b["fields"] = []
    if not "methods" in line_a:
        line_a["methods"] = []
    if not "methods" in line_b:
        line_b["methods"] = []

    for field in line_b["fields"]:
        if not field in line_a["fields"]:
            line_a["fields"].append(field)
    for method in line_b["methods"]:
        if not method in line_a["methods"]:
            line_a["methods"].append(field)

    line_a["fields"].sort(key=lambda x: x["name"])
    line_a["methods"].sort(key=lambda x: x["name"])

    if len(line_a["fields"]) == 0:
        del line_a["fields"]
    if len(line_a["methods"]) == 0:
        del line_a["methods"]

    return line_a


def merge_resource_config(file_a, file_b, output):
    json_a = json.loads(open(file_a).read())
    json_b = json.loads(open(file_b).read())

    patterns = set([])
    for pattern in json_a["resources"]["includes"] + json_b["resources"]["includes"]:
        pattern = pattern["pattern"]
        if not pattern.startswith("\\Qcom/formdev/flatlaf/") and not pattern.startswith("\\Qmoe/lymia/mppatch/"):
            patterns.add(pattern)
    patterns.add("com/formdev/flatlaf/.*")
    patterns.add("moe/lymia/mppatch/.*")

    new_json = {"resources": {"includes": []}, "bundles": []}
    for pattern in patterns:
        new_json["resources"]["includes"].append({"pattern": pattern})
    new_json["resources"]["includes"].sort(key=lambda x: x["pattern"])

    if "bundles" in json_a:
        for bundle in json_a["bundles"]:
            new_json["bundles"].append(bundle)
    if "bundles" in json_b:
        for bundle in json_b["bundles"]:
            if not bundle in new_json["bundles"]:
                new_json["bundles"].append(bundle)

    print(new_json)
    open(output, "w").write(json.dumps(new_json, indent=2))


merge_jni_config(f"scripts/native-image-config/common-flatlaf-{platform}/jni-config.json",
                 "target/native-image-config-temp/jni-config.json",
                 f"scripts/native-image-config/{platform}/jni-config.json")
merge_jni_config(f"scripts/native-image-config/common-flatlaf-{platform}/reflect-config.json",
                 "target/native-image-config-temp/reflect-config.json",
                 f"scripts/native-image-config/{platform}/reflect-config.json")
merge_resource_config(f"scripts/native-image-config/common-flatlaf-{platform}/resource-config.json",
                      "target/native-image-config-temp/resource-config.json",
                      f"scripts/native-image-config/{platform}/resource-config.json")
