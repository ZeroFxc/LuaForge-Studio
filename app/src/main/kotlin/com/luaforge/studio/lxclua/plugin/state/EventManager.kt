package com.luaforge.studio.lxclua.plugin.state

import com.luaforge.studio.lxclua.plugin.api.callbacks.IPluginEventListener
import com.luajava.LuaFunction
import com.luajava.LuaTable

/**
 * 插件生命周期事件常量
 */
object PluginEvents {
    // 编辑器相关事件
    const val ON_FILE_OPEN = "onFileOpen"
    const val ON_FILE_SAVE = "onFileSave"
    const val ON_FILE_CLOSE = "onFileClose"
    const val ON_TEXT_CHANGED = "onTextChanged"
    const val ON_EDITOR_INIT = "onEditorInit"
    const val ON_EDITOR_CLOSE = "onEditorClose"
    
    // 插件生命周期事件
    const val ON_PLUGIN_LOADED = "onPluginLoaded"
    const val ON_PLUGIN_UNLOADED = "onPluginUnloaded"
    const val ON_PLUGIN_ENABLED = "onPluginEnabled"
    const val ON_PLUGIN_DISABLED = "onPluginDisabled"
    const val ON_ALL_PLUGINS_LOADED = "onAllPluginsLoaded"
    
    // 应用生命周期事件
    const val ON_APP_START = "onAppStart"
    const val ON_APP_RESUME = "onAppResume"
    const val ON_APP_PAUSE = "onAppPause"
    const val ON_APP_STOP = "onAppStop"
    
    // 主页项目列表事件
    const val ON_PROJECT_LONG_PRESS = "onProjectLongPress"
    const val ON_PROJECT_CLICK = "onProjectClick"
    const val ON_PROJECT_SWIPE_LEFT = "onProjectSwipeLeft"
    const val ON_PROJECT_SWIPE_RIGHT = "onProjectSwipeRight"
    
    // UI 交互事件
    const val ON_BACK_PRESSED = "onBackPressed"

    // 构建事件
    /** 构建开始，参数: (projectPath: String, buildType: String) buildType 为 "project" 或 "compile" */
    const val ON_BUILD_START = "onBuildStart"
    /** 构建完成，参数: (projectPath: String, result: String, success: Boolean) */
    const val ON_BUILD_FINISH = "onBuildFinish"
    /** 构建出错，参数: (projectPath: String, errorMessage: String, buildType: String) */
    const val ON_BUILD_ERROR = "onBuildError"

    // 依赖事件
    /** 依赖缺失，启用插件时必需依赖不满足，参数: (pluginId: String, missingDepsCsv: String) */
    const val ON_DEPENDENCY_MISSING = "onDependencyMissing"

    // 属性变更事件
    /** 插件属性变更，updatePluginName/Description 等成功后，参数: (pluginId: String, changedFieldsJson: String) */
    const val ON_PLUGIN_PROPERTY_CHANGED = "onPluginPropertyChanged"

    // 安装事件
    /** 安装版本冲突，参数: (pluginId, existingVersion, newVersion, isUpdate, isDowngrade) */
    const val ON_INSTALL_VERSION_CONFLICT = "onInstallVersionConflict"

    // 插件卡片交互事件
    /** 插件管理页面中卡片被单击，参数: (pluginId: String) */
    const val ON_PLUGIN_CARD_CLICK = "onPluginCardClick"
}

/**
 * 带插件ID的监听器包装
 */
data class ListenerEntry(
    val pluginId: String,
    val listener: Any
)

/**
 * 事件管理器
 * 负责管理插件的事件订阅和触发
 */
object EventManager {
    
    // 存储结构: eventName -> List<ListenerEntry>
    private val eventListeners = mutableMapOf<String, MutableList<ListenerEntry>>()
    
    /**
     * 注册事件监听器
     * @param pluginId 插件ID，用于卸载时清理
     * @param eventName 事件名称
     * @param listener 监听器对象（支持 IPluginEventListener, LuaFunction, LuaTable）
     */
    fun registerEventListener(pluginId: String, eventName: String, listener: Any) {
        eventListeners.getOrPut(eventName) { mutableListOf() }.add(ListenerEntry(pluginId, listener))
    }
    
    /**
     * 简化版注册（兼容旧代码）
     */
    fun registerEventListener(eventName: String, listener: Any) {
        eventListeners.getOrPut(eventName) { mutableListOf() }.add(ListenerEntry("", listener))
    }
    
    /**
     * 取消事件监听
     */
    fun unregisterEventListener(eventName: String, listener: Any) {
        eventListeners[eventName]?.removeIf { it.listener === listener }
    }
    
    /**
     * 触发事件
     */
    fun fireEvent(eventName: String, vararg args: Any?) {
        val entries = eventListeners[eventName]?.toList() ?: return
        
        for (entry in entries) {
            try {
                when (val listener = entry.listener) {
                    is LuaFunction<*> -> {
                        listener.call(*args)
                    }
                    is LuaTable<*, *> -> {
                        val onEvent = listener["onEvent"]
                        if (onEvent is LuaFunction<*>) {
                            onEvent.call(*args)
                        }
                    }
                    is IPluginEventListener -> {
                        listener.onEvent(*args)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("EventManager", "通知事件 $eventName 到监听器 ${entry.pluginId} 发生错误", e)
            }
        }
    }
    
    /**
     * 移除指定插件的所有事件监听器
     */
    fun removePluginListeners(pluginId: String) {
        eventListeners.values.forEach { entries ->
            entries.removeIf { it.pluginId == pluginId }
        }
    }
    
    /**
     * 获取指定事件的监听器数量
     */
    fun getListenerCount(eventName: String): Int {
        return eventListeners[eventName]?.size ?: 0
    }
    
    /**
     * 清空所有事件监听器
     */
    fun clearAllListeners() {
        eventListeners.clear()
    }
}
