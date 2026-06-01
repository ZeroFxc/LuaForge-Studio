package com.luaforge.studio.lxclua.plugin.api.callbacks

/**
 * 插件事件监听器（Java/Kotlin 接口）
 */
interface IPluginEventListener {
    fun onEvent(vararg args: Any?)
}