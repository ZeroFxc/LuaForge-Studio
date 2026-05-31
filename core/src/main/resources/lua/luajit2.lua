local LuaJitNatives = luajava.bindClass("party.iroiro.luajava.luajit.LuaJitNatives")

local M = {}

local natives = nil
local L = nil

local function init()
    if natives ~= nil then return end

    natives = LuaJitNatives()
    L = natives.luaL_newstate(0)
    if L == 0 then error("luaL_newstate failed") end

    natives.luaL_openlibs(L)
end


local function stack_to_lua(natives, L, idx)
    local t = natives.lua_type(L, idx)
    if t == 0 then return nil end
    if t == 1 then return natives.lua_toboolean(L, idx) ~= 0 end
    if t == 3 then return natives.lua_tonumber(L, idx) end
    if t == 4 then return natives.lua_tostring(L, idx) end
    if t == 9 then return natives.lua_tointeger(L, idx) end

    if t == 5 then
        local tbl = {}
        natives.lua_pushnil(L)
        while natives.lua_next(L, idx) ~= 0 do
            local k = stack_to_lua(natives, L, -2)
            local v = stack_to_lua(natives, L, -1)
            if k ~= nil then tbl[k] = v end
            natives.lua_pop(L, 1)
        end
        return tbl
    end

    return nil
end


function M.loadstring(code)
    init()
    local r = natives.luaL_loadstring(L, code)
    if r ~= 0 then
        natives.luaL_where(L, 1)
        local w = natives.lua_tostring(L, -1)
        natives.lua_pop(L, 1)
        local err = natives.lua_tostring(L, -1)
        error("load error: " .. w .. " " .. err)
    end

    r = natives.lua_pcall(L, 0, -1, 0)
    if r ~= 0 then
        local err = natives.lua_tostring(L, -1)
        error("run error: " .. err)
    end

    local top = natives.lua_gettop(L)
    local ret = top > 0 and stack_to_lua(natives, L, -1) or nil
    natives.lua_settop(L, 0)
    return ret
end

function M.setglobal(name, value)
    init()
    natives.lua_pushstring(L, name)

    local t = type(value)
    if t == "string" then
        natives.lua_pushstring(L, value)
    elseif t == "number" then
        natives.lua_pushnumber(L, value)
    elseif t == "boolean" then
        natives.lua_pushboolean(L, value and 1 or 0)
    elseif t == "table" then
        natives.lua_createtable(L, 0, 0)
        for k, v in pairs(value) do
            natives.lua_pushstring(L, tostring(k))
            M.setglobal("_tmp", v)
            natives.lua_rawset(L, -3)
        end
    else
        natives.lua_pushnil(L)
    end

    natives.lua_setglobal(L, name)
end


function M.getglobal(name)
    init()
    natives.lua_getglobal(L, name)
    local v = stack_to_lua(natives, L, -1)
    natives.lua_pop(L, 1)
    return v
end

return M
