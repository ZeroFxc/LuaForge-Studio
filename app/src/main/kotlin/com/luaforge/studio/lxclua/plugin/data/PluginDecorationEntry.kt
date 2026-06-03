package com.luaforge.studio.lxclua.plugin.data

/**
 * 编辑器行装饰数据
 *
 * 用于插件在编辑器行上添加视觉标记（行背景色、gutter 区域背景色、gutter 图标）
 *
 * 使用方式:
 * ```lua
 * plugin.decoration.setLineBackground(line, 0x33FF0000)     -- 红色半透明行背景
 * plugin.decoration.setGutterIcon(line, "error")             -- 错误图标
 * plugin.decoration.removeLineDecoration(line)                -- 移除该行所有装饰
 * ```
 */
data class PluginDecorationEntry(
    /** 装饰所属的插件 ID */
    val pluginId: String,
    /** 目标文件路径 */
    val filePath: String,
    /** 行号（0-based） */
    val line: Int,
    /** 装饰分类（"default", "breakpoint", "error", "warning", "bookmark" 等），用于分类导航 */
    val category: String = "default",
    /** 行背景颜色（ARGB int），null 表示不设置 */
    val lineBackgroundColor: Int? = null,
    /** Gutter 区域背景颜色（ARGB int），null 表示不设置 */
    val gutterBackgroundColor: Int? = null,
    /** Gutter 图标类型（"error", "warning", "info", "bookmark", "arrow", "star"），null 表示不设置 */
    val gutterIconType: String? = null,
    /** 创建时间戳 */
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 构建用于 Lua 侧返回的 Map 表示
     */
    fun toMap(): Map<String, Any?> {
        return buildMap {
            put("pluginId", pluginId)
            put("filePath", filePath)
            put("line", line)
            put("category", category)
            lineBackgroundColor?.let { put("lineBackgroundColor", String.format("#%08X", it)) }
            gutterBackgroundColor?.let { put("gutterBackgroundColor", String.format("#%08X", it)) }
            gutterIconType?.let { put("gutterIconType", it) }
        }
    }

    companion object {
        /** 支持的图标类型列表 */
        val SUPPORTED_ICON_TYPES = setOf(
            "error", "warning", "info", "bookmark", "arrow", "star", "dot"
        )
    }
}