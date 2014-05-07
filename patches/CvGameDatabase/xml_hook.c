/**
    Copyright (C) 2014 Lymia Aluysia <lymiahugs@gmail.com>

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

extern __thiscall bool Database_ExecuteMultiple(void* this, const char* string, size_t length);
extern __thiscall bool Database_LogMessage     (const char* string);

typedef __thiscall bool (*xml_checkLabelRaw_fn)(void* this, const char* string, size_t* tagSize);
typedef __thiscall void (*xml_getContents_fn)  (void* this, char** string_out, size_t* length_out);
static xml_checkLabelRaw_fn xml_checkLabelRaw;
static xml_getContents_fn   xml_getContents;

static bool xml_checkLabel(void* this, const char* string) {
    int length = strlen(string);
    return xml_checkLabelRaw(this, string, &length);
}

__stdcall bool XmlParserHookCore(void* xmlNode, void* connection, int* success) {
    *success = 1;

    if(xml_checkLabel(xmlNode, "__MOD2DLC_PATCH_IGNORE")) {
        return true;
    } else if(xml_checkLabel(xmlNode, "__MOD2DLC_PATCH_RAWSQL")) {
        char* string;
        int   length;
        xml_getContents(xmlNode, &string, &length);

        if(!Database_ExecuteMultiple(connection, string, length)) {
            Database_LogMessage("Failed to execute statement while processing __MOD2DLC_PATCH_RAWSQL tag.");
            *success = 0;
        }
        return true;
    } else return false;
}

__attribute__((constructor(500))) static void installXmlHook() {
    xml_checkLabelRaw = (xml_checkLabelRaw_fn) loadRelativeAddress(xml_check_label_offset);
    xml_getContents   = (xml_getContents_fn)   loadRelativeAddress(xml_get_contents_offset);
}
