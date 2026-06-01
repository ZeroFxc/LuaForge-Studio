package com.luaforge.studio.lxclua.plugin.data

/**
 * 插件注册的资源条目
 * 
 * 插件可以将自己的资源（图片、音频、数据文件等）注册到全局资源表中，
 * 其他插件可以通过 assetRegistry 查询和使用这些资源。
 */
data class RegisteredResource(
    /** 全局唯一的资源标识符，格式: pluginId/resourceKey */
    val id: String,
    /** 注册此资源的插件 ID */
    val pluginId: String,
    /** 资源名称（在插件内唯一） */
    val key: String,
    /** 资源类型: image, audio, font, data, layout, icon, raw 等 */
    val type: String,
    /** 资源的文件绝对路径 */
    val filePath: String,
    /** 资源显示名称 */
    val displayName: String,
    /** 资源描述 */
    val description: String,
    /** 文件扩展名（如 png, mp3, ttf, json） */
    val extension: String,
    /** 文件大小（字节） */
    val size: Long,
    /** 注册时间戳（毫秒） */
    val registeredAt: Long,
    /** 额外的自定义元数据（可选） */
    val metadata: Map<String, String> = emptyMap(),
    /** 是否可被其他插件访问（false 表示仅注册者可用） */
    val isPublic: Boolean = true
)