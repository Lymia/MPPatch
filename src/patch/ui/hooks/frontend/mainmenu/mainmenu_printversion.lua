if _mpPatch and _mpPatch.loaded and not _mpPatch.patch.shared.printVersionRan then
    pcall(function()
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
            _mpPrint.debugPrint("Tree Status: ".._mpPatch.version.get("build.treestatus"))
        end
    end)
end