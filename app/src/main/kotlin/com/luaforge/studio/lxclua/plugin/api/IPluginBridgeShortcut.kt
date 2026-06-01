package com.luaforge.studio.lxclua.plugin.api

import com.luaforge.studio.lxclua.plugin.data.ShortcutInfo

/**
 * 快捷键绑定系统 API
 * 
 * 插件可通过此接口注册自定义键盘快捷键，
 * 当用户在编辑器中按下对应的组合键时触发回调。
 * 
 * 组合键格式: "Ctrl+S", "Ctrl+Shift+Z", "Alt+Enter", "Ctrl+Alt+F" 等
 * 支持的修饰键: Ctrl, Shift, Alt
 * 支持的主键: A-Z, 0-9, F1-F12, Enter, Delete, Tab, Space, 方向键等
 */
interface IPluginBridgeShortcut {
    
    /**
     * 注册一个快捷键
     * @param key 快捷键唯一标识（插件内唯一）
     * @param combination 组合键字符串，如 "Ctrl+S", "Ctrl+Shift+Z"
     * @param label 显示标签
     * @param description 描述信息
     * @param callback 触发回调（接收组合键字符串作为参数）
     * @param editorOnly 是否仅在编辑器聚焦时生效（默认 true）
     * @return 注册成功返回全局 ID（格式: pluginId/key），失败返回 null
     */
    fun register(
        key: String,
        combination: String,
        label: String,
        description: String,
        callback: Any,
        editorOnly: Boolean
    ): String?
    
    /**
     * 注册一个快捷键（简化版，editorOnly 默认为 true）
     */
    fun register(key: String, combination: String, label: String, callback: Any): String?
    
    /**
     * 注销一个快捷键
     * @param key 快捷键键名
     * @return 是否注销成功
     */
    fun unregister(key: String): Boolean
    
    /**
     * 注销自己的所有快捷键
     * @return 被注销的数量
     */
    fun unregisterAll(): Int
    
    /**
     * 获取自己注册的所有快捷键
     * @return 快捷键信息数组
     */
    fun getMyShortcuts(): Array<ShortcutInfo>
    
    /**
     * 获取指定快捷键的详细信息
     * @param globalId 全局 ID（pluginId/key）或仅 key
     * @return 快捷键信息，不存在返回 null
     */
    fun getShortcut(globalId: String): ShortcutInfo?
    
    /**
     * 获取所有已注册的快捷键（包括其他插件的）
     * @return 快捷键信息数组
     */
    fun getAllShortcuts(): Array<ShortcutInfo>
    
    /**
     * 检查快捷键组合是否已被占用
     * @param combination 组合键字符串
     * @return 占用者信息，未被占用返回 null
     */
    fun isCombinationTaken(combination: String): ShortcutInfo?
    
    /**
     * 获取已注册快捷键总数
     * @return 总数
     */
    fun getShortcutCount(): Int
}