-- Get the path of the currently executing Lua file using some very evil magic.
local currentExecutingPath
do
    local luaFile = ("").dump(function() end)
    local luaFile_expectedHeader = "\27\76\117\97\81\0\1\4\4\4\8\0"
    local luaFile_header = luaFile:sub(1, #luaFile_expectedHeader)
    assert(luaFile_header == luaFile_expectedHeader, "Unexpected Lua bytecode format!")
    local function decode_u32(string, i)
        return string:byte(i+0) * 0x00000001 +
                string:byte(i+1) * 0x00000100 +
                string:byte(i+2) * 0x00010000 +
                string:byte(i+3) * 0x01000000
    end
    local str_len = decode_u32(luaFile, 13)
    currentExecutingPath = luaFile:sub(17, 17+str_len)
end

