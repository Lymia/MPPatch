-- Copyright (c) 2015-2017 Lymia Alusyia <lymia@lymiahugs.com>
--
-- Permission is hereby granted, free of charge, to any person obtaining a copy
-- of this software and associated documentation files (the "Software"), to deal
-- in the Software without restriction, including without limitation the rights
-- to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
-- copies of the Software, and to permit persons to whom the Software is
-- furnished to do so, subject to the following conditions:
--
-- The above copyright notice and this permission notice shall be included in
-- all copies or substantial portions of the Software.
--
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
-- IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
-- FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
-- AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
-- LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
-- OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
-- THE SOFTWARE.

local success, error = pcall(function()
    local versionString
    if not _mpPatch then
        versionString = Locale.Lookup("TXT_KEY_MPPATCH_UNKNOWN_FAILURE")
    elseif not _mpPatch.loaded and _mpPatch.status.binaryLoadFailed then
        versionString = Locale.Lookup("TXT_KEY_MPPATCH_BINARY_NOT_PATCHED")
    elseif not _mpPatch.loaded then
        versionString = Locale.Lookup("TXT_KEY_MPPATCH_UNKNOWN_FAILURE")
    else
        versionString = "MpPatch v".._mpPatch.versionString
    end
    Controls.VersionNumber:SetText(Controls.VersionNumber:GetText().." -- "..versionString)
end)

if not success then
    print("Failed to add version to main menu: "..tostring(error))
end