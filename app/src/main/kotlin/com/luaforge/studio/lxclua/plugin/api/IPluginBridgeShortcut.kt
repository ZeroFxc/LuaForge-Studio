package com.luaforge.studio.lxclua.plugin.api

import com.luaforge.studio.lxclua.plugin.data.ShortcutInfo

/**
 * 快捷键绑定系统 API
 * 
 * 组合键格式: "Ctrl+S", "Ctrl+Shift+Z", "Alt+Enter", "Ctrl+Alt+F" 等
 * 支持的修饰键: Ctrl, Shift, Alt
 * 支持的主键: A-Z, 0-9, F1-F12, Enter, Delete, Tab, Space, 方向键等
 */
interface IPluginBridgeShortcut {
    
    fun register(
        key: String,
        combination: String,
        label: String,
        description: String,
        callback: Any,
        editorOnly: Boolean
    ): String?
    
    fun register(key: String, combination: String, label: String, callback: Any): String?
    
    fun register(key: String, combination: String, label: String, callback: Any, editorOnly: Boolean): String?
    
    fun unregister(key: String): Boolean
    
    fun unregisterAll(): Int
    
    fun getMyShortcuts(): Array<ShortcutInfo>
    
    fun getShortcut(globalId: String): ShortcutInfo?
    
    fun getAllShortcuts(): Array<ShortcutInfo>
    
    fun isCombinationTaken(combination: String): ShortcutInfo?
    
    fun getShortcutCount(): Int
}