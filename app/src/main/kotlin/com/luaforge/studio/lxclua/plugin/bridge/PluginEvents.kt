package com.luaforge.studio.lxclua.plugin.bridge

import com.luaforge.studio.lxclua.plugin.state.EventManager

/**
 * 事件监听 API
 * 
 * 使用方式: plugin.events.register("onFileSave", callback)
 */
class PluginEvents(private val pluginId: String) {
    
    /**
     * 注册事件监听器
     */
    fun register(eventName: String, listener: Any) {
        EventManager.registerEventListener(pluginId, eventName, listener)
    }
    
    /**
     * 取消事件监听
     */
    fun unregister(eventName: String, listener: Any) {
        EventManager.unregisterEventListener(eventName, listener)
    }
    
    /**
     * 触发自定义事件
     */
    fun fire(eventName: String, vararg args: Any?) {
        EventManager.fireEvent(eventName, *args)
    }
    
    // ============ 预定义事件常量 ============
    
    // 编辑器事件
    val ON_FILE_OPEN = "onFileOpen"
    val ON_FILE_SAVE = "onFileSave"
    val ON_FILE_CLOSE = "onFileClose"
    val ON_TEXT_CHANGED = "onTextChanged"
    val ON_EDITOR_INIT = "onEditorInit"
    val ON_EDITOR_CLOSE = "onEditorClose"
    
    // 插件生命周期事件
    val ON_PLUGIN_LOADED = "onPluginLoaded"
    val ON_PLUGIN_UNLOADED = "onPluginUnloaded"
    val ON_PLUGIN_ENABLED = "onPluginEnabled"
    val ON_PLUGIN_DISABLED = "onPluginDisabled"
    val ON_ALL_PLUGINS_LOADED = "onAllPluginsLoaded"
    
    // 应用生命周期事件
    val ON_APP_START = "onAppStart"
    val ON_APP_RESUME = "onAppResume"
    val ON_APP_PAUSE = "onAppPause"
    val ON_APP_STOP = "onAppStop"
    
    // 主页项目列表事件
    val ON_PROJECT_LONG_PRESS = "onProjectLongPress"
    val ON_PROJECT_CLICK = "onProjectClick"
    val ON_PROJECT_SWIPE_LEFT = "onProjectSwipeLeft"
    val ON_PROJECT_SWIPE_RIGHT = "onProjectSwipeRight"
    
    // UI 交互事件
    val ON_BACK_PRESSED = "onBackPressed"
}