package com.luaforge.studio.lxclua.plugin.api

import android.content.Context

/**
 * DEX/APK 插件接口，所有的 Java/Kotlin 插件入口类必须实现此接口
 */
interface IPlugin {
    fun onLoad(context: Context, bridge: IPluginBridge)
    fun onUnload()
}