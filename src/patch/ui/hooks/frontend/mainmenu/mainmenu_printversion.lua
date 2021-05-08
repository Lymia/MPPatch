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

if _mpPatch and _mpPatch.loaded and not _mpPatch.patch.shared.printVersionRan then
    _mpPatch.patch.shared.printVersionRan = true

    local tostring = _mpPatch.patch.globals.tostring
    _mpPatch.debugPrint("MPPatch version ".._mpPatch.versionString)

    local shortRev  = _mpPatch.version.get("mppatch.version.commit"):sub(0, 8)
    local timestr   = _mpPatch.version.get("build.timestr")
    local buildUser = _mpPatch.version.get("build.user")
    _mpPatch.debugPrint("Revision "..shortRev..", built on "..timestr.." by "..buildUser)
    if _mpPatch.patch.luajit_version then
        _mpPatch.debugPrint("Running on ".._mpPatch.patch.luajit_version..".")
    else
        _mpPatch.debugPrint("Running on ".._VERSION..".")
    end
    if not _mpPatch.version.getBoolean("mppatch.version.clean") then
        _mpPatch.debugPrint("Tree Status: ".._mpPatch.version.get("build.treestatus"))
    end
end