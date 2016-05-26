/**
    Copyright (C) 2015-2016 Lymia Aluysia <lymiahugs@gmail.com>

    Permission is hereby granted, free of charge, to any person obtaining a copy of
    this software and associated documentation files (the "Software"), to deal in
    the Software without restriction, including without limitation the rights to
    use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
    of the Software, and to permit persons to whom the Software is furnished to do
    so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
*/

#include <stdlib.h>
#include <stdbool.h>
#include <string.h>

#include "c_rt.h"
#include "c_defines.h"
#include "extern_defines.h"
#include "version.h"
#include "base64.h"

static bool checkXmlNodeTag(class_XmlNode* xmlNode, const char* name) {
  size_t len = strlen(name);
  return XmlNode_NameMatches(xmlNode, name, &len);  
}

// Main body
bool xml_init = false;
extern ASM_ENTRY bool XmlParserHookCore(class_XmlNode* xmlNode, class_Database* connection, int* success) __asm__("cif_XmlParserHookCore");
ASM_ENTRY bool XmlParserHookCore(class_XmlNode* xmlNode, class_Database* connection, int* success) {
    *success = 1;

    if(!xml_init) {
        debug_print("Initialized XmlParserHook!");
        Database_LogMessage(connection, patchMarkerString);
        xml_init = true;
    }

    if(checkXmlNodeTag(xmlNode, "__MVMM_PATCH_IGNORE")) {
        return true;
    } else if(checkXmlNodeTag(xmlNode, "__MVMM_PATCH_RAWSQL")) {
        char*  string;
        size_t length;
        XmlNode_GetValUtf8(xmlNode, &string, &length);

        size_t targetLength = base64OutputSize(length);
        char* tmpString = malloc(targetLength);
        memset(tmpString, 0, targetLength);
        decodeBase64(string, tmpString, length, targetLength);

        debug_print("Executing XML-encapsulated SQL:\n%s", tmpString);

        if(!Database_ExecuteMultiple(connection, tmpString, strlen(tmpString))) {
            Database_LogMessage(connection, "Failed to execute statement while processing __MVMM_PATCH_RAWSQL tag.");
            *success = 0;
        }

        free(tmpString);

        return true;
    } else {
        return false;
    }
}

extern void XmlParserHook() __asm__("cif_XmlParserHook");
UnpatchData* XmlParserPatch;
__attribute__((constructor(500))) static void installXmlHook() {
    XmlParserPatch = doPatch(XmlParserHook_offset, XmlParserHook, "XmlParserHook");
}
__attribute__((destructor(500))) static void destroyXmlHook() {
    unpatch(XmlParserPatch);
}
