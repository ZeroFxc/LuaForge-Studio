package com.luaforge.studio.lxclua.plugin.data

/**
 * 插件注册的快捷键信息
 */
data class ShortcutInfo(
    /** 快捷键唯一标识，格式: pluginId/key */
    val id: String,
    /** 注册此快捷键的插件 ID */
    val pluginId: String,
    /** 快捷键键名（插件内唯一） */
    val key: String,
    /** 组合键字符串，如 "Ctrl+S", "Ctrl+Shift+Z" */
    val combination: String,
    /** 显示标签 */
    val label: String,
    /** 快捷键描述 */
    val description: String,
    /** 修饰键掩码: 1=Ctrl, 2=Shift, 4=Alt */
    val modifiers: Int,
    /** 主键的 Android KeyEvent 键码 */
    val keyCode: Int,
    /** 是否仅在编辑器聚焦时生效 */
    val editorOnly: Boolean,
    /** 注册时间戳 */
    val registeredAt: Long
)