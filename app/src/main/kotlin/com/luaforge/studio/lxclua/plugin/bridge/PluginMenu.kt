package com.luaforge.studio.lxclua.plugin.bridge

import com.luaforge.studio.lxclua.plugin.PluginManager
import com.luaforge.studio.lxclua.plugin.state.UIState
import com.luaforge.studio.lxclua.ui.editor.QuickAction

/**
 * 菜单管理 API
 * 
 * 使用方式: plugin.menu.addQuickAction("id", "label", callback)
 */
class PluginMenu(private val pluginId: String) {
    
    /**
     * 添加编辑器快捷操作按钮
     */
    fun addQuickAction(key: String, label: String, onClick: Runnable) {
        val quickAction = QuickAction(
            key = "${pluginId}_$key",
            labelString = label,
            onClick = { onClick.run() }
        )
        PluginManager.addQuickAction(pluginId, key, quickAction)
    }
    
    /**
     * 移除编辑器快捷操作按钮
     */
    fun removeQuickAction(key: String) {
        PluginManager.removeQuickAction(pluginId, key)
    }
    
    /**
     * 添加顶部菜单项
     */
    fun addMenuItem(key: String, label: String, onClick: Runnable) {
        UIState.addPluginMenuItem(pluginId, key, label, onClick)
    }
    
    /**
     * 移除顶部菜单项
     */
    fun removeMenuItem(key: String) {
        UIState.removePluginMenuItem(pluginId, key)
    }
    
    /**
     * 添加菜单分隔线
     */
    fun addMenuDivider(key: String) {
        UIState.addPluginMenuDivider(pluginId, key)
    }
    
    /**
     * 移除菜单分隔线
     */
    fun removeMenuDivider(key: String) {
        UIState.removePluginMenuItem(pluginId, key)
    }
    
    /**
     * 添加文件树上下文菜单项
     * 
     * @param filter 过滤器: null=全部, ".lua"=指定扩展名, "directory"=仅目录, "file"=仅文件
     */
    fun addFileTreeItem(key: String, label: String, filter: String?, onClick: (String, Boolean) -> Unit) {
        UIState.addFileTreeMenuItem(pluginId, key, label, null, filter, onClick)
    }
    
    /**
     * 移除文件树上下文菜单项
     */
    fun removeFileTreeItem(key: String) {
        UIState.removeFileTreeMenuItem(pluginId, key)
    }
    
    /**
     * 添加文件树菜单分隔线
     */
    fun addFileTreeDivider(key: String, filter: String?) {
        UIState.addFileTreeMenuDivider(pluginId, key, filter)
    }
    
    /**
     * 移除所有该插件注册的菜单项
     */
    fun clearAll() {
        UIState.removePluginMenuItems(pluginId)
        UIState.removeFileTreeMenuItems(pluginId)
        PluginManager.clearQuickActions(pluginId)
    }
}