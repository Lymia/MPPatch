do
    local printFn = print
    local patch = DB.GetMemoryUsage("216f0090-85dd-4061-8371-3d8ba2099a70")
    if patch.__mppatch_marker then printFn = patch.debugPrint end
    patchFn("Content switch.")
end