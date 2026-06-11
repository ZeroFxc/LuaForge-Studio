package com.luaforge.studio.lxclua.plugin.data

/**
 * 插件完整信息，聚合 manifest 和运行时状态
 * 供 Lua 端通过 plugin.manager.getPluginInfo(id) 获取
 */
data class PluginInfo(
    /** 插件唯一标识 */
    val id: String,
    /** 显示名称 */
    val name: String,
    /** 版本号 */
    val version: String,
    /** 描述 */
    val description: String,
    /** 作者 */
    val author: String,
    /** 主入口文件（lua）或类名（dex） */
    val main: String,
    /** 插件类型：lua / dex / apk */
    val type: String,
    /** 插件分类：core / normal / dependent / extension */
    val category: String,
    /** API 版本 */
    val apiVersion: Int,
    /** 是否已启用 */
    val enabled: Boolean,
    /** 是否核心插件 */
    val isCore: Boolean,
    /** 插件所在目录路径 */
    val directory: String,
    /** 依赖的插件 ID 列表 */
    val dependencies: Array<String>,
    /** 依赖此插件的其他插件 ID 列表 */
    val dependents: Array<String>,
    /** 依赖详情列表 */
    val dependencyInfos: Array<DependencyInfo>,
    /** 主页/文档链接 */
    val homepage: String?,
    /** 是否有 WebUI 入口（通过 main.lua 检测） */
    val hasWebUI: Boolean,
    /** 插件图标（相对路径或 base64） */
    val icon: String? = null,
    /** 标签列表，解析后的 TagInfo */
    val tags: Array<TagInfo> = emptyArray()
)

/**
 * 依赖信息（用于跨语言传递）
 * 与 PluginManagerBridge.DependencyInfo 不同，此数据类面向跨语言序列化
 */
data class DependencyInfo(
    val pluginId: String,
    val minVersion: String,
    val required: Boolean
)

/**
 * 标签信息（从 manifest.json tags 数组解析）
 *
 * 支持的标签格式:
 * - "标签名"                               → 纯文本标签，颜色自动生成
 * - "#RRGGBB:标签名"                       → 指定颜色标签
 * - "icons/xxx.png|标签名"                 → 带图标标签
 * - "#RRGGBB:icons/xxx.png|标签名"         → 指定颜色 + 图标标签
 * - "icons/xxx.png"                        → 仅图标标签
 */
data class TagInfo(
    /** 标签文本（为空表示仅图标） */
    val name: String = "",
    /** 图标相对路径（相对于插件目录），null 表示无图标 */
    val iconPath: String? = null,
    /** 颜色值（如 #ff6f00），null 表示自动生成 */
    val color: String? = null,
    /** 插件目录路径（用于解析图标绝对路径） */
    val pluginDir: String = ""
) {
    companion object {
        /**
         * 从标签字符串解析 TagInfo 列表
         */
        fun parseAll(tagStrings: List<String>, pluginDir: String): List<TagInfo> {
            return tagStrings.map { parse(it, pluginDir) }
        }

        /**
         * 解析单个标签字符串
         *
         * 格式: [#颜色:]图标路径|标签名
         */
        fun parse(raw: String, pluginDir: String): TagInfo {
            var remaining = raw.trim()
            var color: String? = null
            var iconPath: String? = null

            // 1. 解析颜色前缀 #RRGGBB: 或 RRGGBB:
            val colorRegex = Regex("""^#?([0-9a-fA-F]{6}):""")
            val colorMatch = colorRegex.find(remaining)
            if (colorMatch != null) {
                color = "#" + colorMatch.groupValues[1]
                remaining = remaining.substring(colorMatch.value.length)
            }

            // 2. 解析图标路径和标签名（用 | 分隔）
            val pipeIdx = remaining.lastIndexOf('|')
            if (pipeIdx >= 0) {
                val iconCandidate = remaining.substring(0, pipeIdx).trim()
                val namePart = remaining.substring(pipeIdx + 1).trim()
                // 判断是否像文件路径（含 .png/.svg/.jpg 等）
                if (iconCandidate.contains('.') || iconCandidate.contains('/')) {
                    iconPath = iconCandidate
                    remaining = namePart
                } else {
                    // 不像是图标路径，整个作为标签名
                    remaining = remaining
                }
            }

            // 3. 剩余的是标签名（如果包含 .png 等扩展名且没有 | 分隔 → 纯图标）
            val name = remaining.trim()
            if (name.isEmpty() && iconPath == null) {
                // 空标签 → 当作纯文本
                return TagInfo(name = raw.trim(), pluginDir = pluginDir)
            }
            if (name.contains('.') && iconPath == null) {
                // 可能整个字符串是图标路径
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext in setOf("png", "jpg", "jpeg", "svg", "webp", "gif")) {
                    return TagInfo(name = "", iconPath = name, color = color, pluginDir = pluginDir)
                }
            }

            return TagInfo(
                name = name.ifEmpty { "" },
                iconPath = iconPath,
                color = color,
                pluginDir = pluginDir
            )
        }
    }

    /** 获取图标的绝对路径 */
    fun getIconAbsolutePath(): String? {
        if (iconPath == null || pluginDir.isEmpty()) return null
        return java.io.File(pluginDir, iconPath).absolutePath
    }
}