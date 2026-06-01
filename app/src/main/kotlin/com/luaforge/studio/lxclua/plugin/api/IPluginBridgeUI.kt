package com.luaforge.studio.lxclua.plugin.api

import com.luaforge.studio.lxclua.plugin.api.callbacks.FileTreeItemCallback
import com.luaforge.studio.lxclua.plugin.data.FileInfo

/**
 * UI 操作相关 API（快捷动作、菜单、文件树）
 */
interface IPluginBridgeUI {
    // ==================== 快捷操作 ====================
    
    /**
     * 在编辑器快捷栏添加按钮
     */
    fun addQuickAction(key: String, label: String, onClick: Runnable)
    
    /**
     * 移除快捷栏按钮
     */
    fun removeQuickAction(key: String)
    
    /**
     * 清除所有快捷栏按钮
     */
    fun clearQuickActions()
    
    // ==================== 菜单操作 ====================
    
    /**
     * 在编辑器右上角菜单中添加菜单项
     */
    fun addMenuItem(key: String, label: String, onClick: Runnable)
    
    /**
     * 在菜单中添加分隔线
     */
    fun addMenuDivider(key: String)
    
    /**
     * 移除菜单项
     */
    fun removeMenuItem(key: String)
    
    /**
     * 清除所有插件添加的菜单项
     */
    fun clearMenuItems()
    
    // ==================== 文件树操作 ====================
    
    /**
     * 注册文件树上下文菜单项
     */
    fun addFileTreeMenuItem(key: String, label: String, filter: String?, onClick: FileTreeItemCallback)
    
    /**
     * 注册文件树上下文菜单项（带图标）
     */
    fun addFileTreeMenuItem(key: String, label: String, iconName: String?, filter: String?, onClick: FileTreeItemCallback)
    
    /**
     * 注册文件树上下文菜单分隔线
     */
    fun addFileTreeMenuDivider(key: String, filter: String?)
    
    /**
     * 移除已注册的文件树菜单项
     */
    fun removeFileTreeMenuItem(key: String)
    
    /**
     * 清除所有插件注册的文件树菜单项
     */
    fun clearFileTreeMenuItems()
    
    /**
     * 获取文件信息
     */
    fun getFileInfo(filePath: String): FileInfo?
}