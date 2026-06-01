package com.luaforge.studio.lxclua.plugin.state

import androidx.compose.runtime.mutableStateListOf

/**
 * UI 状态管理
 * 负责管理插件注册的菜单项、文件树菜单项等 UI 元素
 */
object UIState {
    
    // ==================== 插件菜单项 ====================
    
    sealed class PluginMenuItem {
        abstract val key: String
        abstract val pluginId: String
        
        data class Item(
            override val key: String,
            override val pluginId: String,
            val label: String,
            val onClick: Runnable
        ) : PluginMenuItem()
        
        data class Divider(
            override val key: String,
            override val pluginId: String
        ) : PluginMenuItem()
    }
    
    val pluginMenuItems = mutableStateListOf<PluginMenuItem>()
    
    fun addPluginMenuItem(pluginId: String, key: String, label: String, onClick: Runnable) {
        val globalKey = "${pluginId}_$key"
        val existingIndex = pluginMenuItems.indexOfFirst { it.key == globalKey }
        val newItem = PluginMenuItem.Item(globalKey, pluginId, label, onClick)
        
        if (existingIndex >= 0) {
            pluginMenuItems[existingIndex] = newItem
        } else {
            pluginMenuItems.add(newItem)
        }
    }
    
    fun addPluginMenuDivider(pluginId: String, key: String) {
        val globalKey = "${pluginId}_$key"
        val existingIndex = pluginMenuItems.indexOfFirst { it.key == globalKey }
        val newDivider = PluginMenuItem.Divider(globalKey, pluginId)
        
        if (existingIndex >= 0) {
            pluginMenuItems[existingIndex] = newDivider
        } else {
            pluginMenuItems.add(newDivider)
        }
    }
    
    fun removePluginMenuItem(pluginId: String, key: String) {
        val globalKey = "${pluginId}_$key"
        pluginMenuItems.removeAll { it.key == globalKey }
    }
    
    fun removePluginMenuItems(pluginId: String) {
        pluginMenuItems.removeAll { it.pluginId == pluginId }
    }
    
    // ==================== 文件树菜单项 ====================
    
    sealed class FileTreeMenuItem {
        abstract val key: String
        abstract val pluginId: String
        abstract val filter: String?
        
        data class Item(
            override val key: String,
            override val pluginId: String,
            val label: String,
            val iconName: String?,
            override val filter: String?,
            val onClick: (String, Boolean) -> Unit
        ) : FileTreeMenuItem()
        
        data class Divider(
            override val key: String,
            override val pluginId: String,
            override val filter: String?
        ) : FileTreeMenuItem()
    }
    
    val fileTreeMenuItems = mutableStateListOf<FileTreeMenuItem>()
    
    fun addFileTreeMenuItem(pluginId: String, key: String, label: String, iconName: String?, filter: String?, onClick: (String, Boolean) -> Unit) {
        val globalKey = "${pluginId}_$key"
        val existingIndex = fileTreeMenuItems.indexOfFirst { it.key == globalKey }
        val newItem = FileTreeMenuItem.Item(globalKey, pluginId, label, iconName, filter, onClick)
        
        if (existingIndex >= 0) {
            fileTreeMenuItems[existingIndex] = newItem
        } else {
            fileTreeMenuItems.add(newItem)
        }
    }
    
    fun addFileTreeMenuDivider(pluginId: String, key: String, filter: String?) {
        val globalKey = "${pluginId}_$key"
        val existingIndex = fileTreeMenuItems.indexOfFirst { it.key == globalKey }
        val newDivider = FileTreeMenuItem.Divider(globalKey, pluginId, filter)
        
        if (existingIndex >= 0) {
            fileTreeMenuItems[existingIndex] = newDivider
        } else {
            fileTreeMenuItems.add(newDivider)
        }
    }
    
    fun removeFileTreeMenuItem(pluginId: String, key: String) {
        val globalKey = "${pluginId}_$key"
        fileTreeMenuItems.removeAll { it.key == globalKey }
    }
    
    fun removeFileTreeMenuItems(pluginId: String) {
        fileTreeMenuItems.removeAll { it.pluginId == pluginId }
    }
}
