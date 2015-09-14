-- Copyright (c) 2015 Lymia Alusyia <lymia@lymiahugs.com>
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

_mvmm.loadedModules.moddinghook = true

-- TODO: Completely rewrite this to hook Modding properly and not do this hackjob.

local function iterConcat(iter1, iter2)
    local iter = iter1
    return function(state, current)
        local result = {iter(state, current) }
        if result[1] == nil then
            if iter == iter1 then
                iter = iter2
                return iter(state, current)
            else
                return nil
            end
        end
        return unpack(result)
    end
end

function _mvmm.callEntryPoints(entryPoint)
    print("Multiverse Mod Manager: Running entry point "..entryPoint)
    g_uiAddins = g_uiAddins or {}
    for _, mod in pairs(_mvmm.getMods()) do
        local epList = mod.entryPoints[entryPoint]
        if epList then
            for _, ep in ipairs(epList) do
                print(" - Executing entry point "..ep.name.." from "..mod.name)
                local addinPath = ep.path
                local extension = Path.GetExtension(addinPath)
                local path = string.sub(addinPath, 1, #addinPath - #extension)
                table.insert(g_uiAddins, ContextPtr:LoadNewContext(path));
            end
        end
    end
end

function _mvmm.installModdingHook()
    if not Modding.___mvmm_marker then
        local oldModding = Modding
        Modding = setmetatable({}, {
            __index = function(_, k)
                if k == "GetActivatedModEntryPoints" then
                    return function(entryPoint, ...)
                        local iter = oldModding.GetActivatedModEntryPoints(entryPoint, ...)
                        return function(...)
                            local rv = {iter(...)}
                            if rv[1] == nil then
                                _mvmm.callEntryPoints(entryPoint)
                            end
                            return unpack(rv)
                        end
                    end
                elseif k == "___mvmm_marker" then
                    return true
                else
                    return oldModding[k]
                end
            end
        })
    end
end