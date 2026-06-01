package com.luaforge.studio.lxclua.plugin.api

/**
 * Lua 脚本执行相关 API
 */
interface IPluginBridgeLua {
    /**
     * 执行 Lua 脚本字符串
     * @param script Lua 脚本内容
     * @return 执行结果
     */
    fun executeLua(script: String): Any?
    
    /**
     * 执行 Lua 脚本文件
     * @param scriptPath 脚本文件路径
     * @return 执行结果
     */
    fun executeLuaFile(scriptPath: String): Any?
    
    /**
     * 编译 Lua 脚本为字节码
     * @param script Lua 脚本内容
     * @return 字节码数据
     */
    fun compileLua(script: String): ByteArray?
    
    /**
     * 执行 Lua 字节码
     * @param bytecode 字节码数据
     * @return 执行结果
     */
    fun executeLuaBytecode(bytecode: ByteArray): Any?
}