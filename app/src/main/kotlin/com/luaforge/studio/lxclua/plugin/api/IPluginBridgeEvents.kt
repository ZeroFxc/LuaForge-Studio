package com.luaforge.studio.lxclua.plugin.api

/**
 * 事件系统相关 API
 */
interface IPluginBridgeEvents {
    /**
     * 注册事件监听器
     * 支持的事件：
     * - onFileOpen: 文件打开时触发，参数: (filePath)
     * - onFileSave: 文件保存时触发，参数: (filePath)
     * - onFileClose: 文件关闭时触发，参数: (filePath)
     * - onTextChanged: 文本变化时触发，参数: (filePath, newContent)
     * - onEditorInit: 编辑器初始化时触发，参数: (projectPath)
     * - onEditorClose: 编辑器关闭时触发，参数: (projectPath)
     * - onProjectLongPress: 主页项目长按时触发，参数: (projectId, projectName, projectPath)
     * - onProjectClick: 主页项目点击时触发，参数: (projectId, projectName, projectPath)
     */
    fun registerEventListener(eventName: String, listener: Any)
    
    /**
     * 取消事件监听
     */
    fun unregisterEventListener(eventName: String, listener: Any)
}