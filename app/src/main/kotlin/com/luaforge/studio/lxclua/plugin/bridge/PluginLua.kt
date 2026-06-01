package com.luaforge.studio.lxclua.plugin.bridge

import com.luajava.LuaState
import com.luajava.LuaStateFactory
import java.io.File

/**
 * Lua 脚本执行 API
 * 
 * 使用方式: plugin.lua.execute("return 1+2")
 */
class PluginLua {
    
    /**
     * 执行 Lua 脚本字符串
     */
    fun execute(script: String): Any? {
        return try {
            val l = LuaStateFactory.newLuaState()
            l.openLibs()
            val ok = l.LloadString(script)
            if (ok == 0) {
                l.pcall(0, 1, 0)
                val result = l.toJavaObject(-1)
                l.close()
                result
            } else {
                l.close()
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 执行 Lua 脚本文件
     */
    fun executeFile(scriptPath: String): Any? {
        return try {
            val l = LuaStateFactory.newLuaState()
            l.openLibs()
            val ok = l.LloadFile(scriptPath)
            if (ok == 0) {
                l.pcall(0, 1, 0)
                val result = l.toJavaObject(-1)
                l.close()
                result
            } else {
                l.close()
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 编译 Lua 脚本为字节码
     */
    fun compile(script: String): ByteArray? {
        return try {
            val l = LuaStateFactory.newLuaState()
            l.openLibs()
            val ok = l.LloadString(script)
            if (ok == 0) {
                val bytecode = l.dump(-1)
                l.close()
                bytecode
            } else {
                l.close()
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 执行 Lua 字节码
     */
    fun executeBytecode(bytecode: ByteArray): Any? {
        return try {
            val l = LuaStateFactory.newLuaState()
            l.openLibs()
            l.LloadBuffer(bytecode, "bytecode")
            l.pcall(0, 1, 0)
            val result = l.toJavaObject(-1)
            l.close()
            result
        } catch (e: Exception) {
            null
        }
    }
}