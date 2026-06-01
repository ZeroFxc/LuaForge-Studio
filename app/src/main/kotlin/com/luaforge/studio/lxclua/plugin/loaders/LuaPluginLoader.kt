package com.luaforge.studio.lxclua.plugin.loaders

import android.content.Context
import com.luaforge.studio.lxclua.plugin.LoadedPlugin
import com.luaforge.studio.lxclua.plugin.bridge.PluginBridgeImpl
import com.luaforge.studio.lxclua.plugin.state.EventManager
import com.luaforge.studio.lxclua.plugin.state.PluginEvents
import com.luajava.LuaState
import com.luajava.LuaStateFactory
import java.io.File

/**
 * Lua 插件加载器
 */
object LuaPluginLoader {
    
    fun load(plugin: LoadedPlugin, context: Context) {
        // 1. 初始化 LuaState 并加载基础库
        val L = LuaStateFactory.newLuaState()
        L.openLibs()
        
        // 2. 绑定全局 studio 桥梁对象
        val bridge = PluginBridgeImpl(plugin.manifest.id)
        L.pushJavaObject(bridge)
        L.setGlobal("studio")
        
        // 3. 执行主入口脚本
        val mainFile = File(plugin.directory, plugin.manifest.main)
        var loadSuccess = false
        
        if (mainFile.exists()) {
            val ok = L.LloadFile(mainFile.absolutePath)
            if (ok == 0) {
                L.getGlobal("debug")
                L.getField(-1, "traceback")
                L.remove(-2)
                L.insert(-2)
                val runOk = L.pcall(0, 0, -2)
                if (runOk != 0) {
                    val error = L.toString(-1)
                    android.util.Log.e("PluginManager", "执行 Lua 插件脚本出错: $error")
                } else {
                    loadSuccess = true
                }
            } else {
                val error = L.toString(-1)
                android.util.Log.e("PluginManager", "编译 Lua 插件失败: $error")
            }
        }
        
        plugin.luaState = L
        
        // 4. 触发插件加载完成事件
        if (loadSuccess) {
            EventManager.fireEvent(PluginEvents.ON_PLUGIN_LOADED, plugin.manifest.id)
        }
    }
}
