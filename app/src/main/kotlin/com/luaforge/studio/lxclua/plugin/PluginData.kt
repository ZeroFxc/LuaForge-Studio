package com.luaforge.studio.lxclua.plugin

import android.content.Context

/**
 * 插件类型
 */
enum class PluginType {
    LUA,
    DEX,
    APK
}

/**
 * 插件元数据（从 manifest.json 解析）
 */
data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String = "",
    val author: String = "",
    val type: String,             // "lua", "dex", "apk"
    val main: String,             // 入口文件，例如 "main.lua" 或 "plugin.dex"
    val mainClass: String? = null, // DEX/APK 插件的入口类名
    val apiVersion: Int = 1
)

/**
 * DEX/APK 插件接口，所有的 Java/Kotlin 插件入口类必须实现此接口
 */
interface IPlugin {
    fun onLoad(context: Context, bridge: IPluginBridge)
    fun onUnload()
}

/**
 * 提供给插件调用的 Host API 桥梁
 */
interface IPluginBridge {
    fun toast(message: String)
    fun log(tag: String, message: String)
    fun getVersion(): String
    fun getActiveFile(): String?
    fun getActiveText(): String?
    fun setActiveText(text: String)
    fun insertText(text: String)
    fun getProjectPath(): String?
    
    // 动态在编辑器的快捷栏中添加/移除动作
    fun addQuickAction(key: String, label: String, onClick: Runnable)
    fun removeQuickAction(key: String)
    
    // 订阅 IDE 各种事件，listener 可以是 IPluginEventListener，也可以是 Lua 中的 function (即 LuaFunction)
    fun registerEventListener(eventName: String, listener: Any)
}

/**
 * 插件事件监听器（Java/Kotlin 接口）
 */
interface IPluginEventListener {
    fun onEvent(vararg args: Any?)
}
