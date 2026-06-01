package com.luaforge.studio.lxclua.plugin.api

import com.luaforge.studio.lxclua.plugin.data.RegisteredResource

/**
 * 插件资源注册表 API
 * 
 * 允许插件将自身的资源（图片、音频、字体、数据文件等）注册到全局资源表中。
 * 注册后，其他插件可以通过此接口查询和使用这些资源。
 * 
 * 使用场景示例:
 * - 主题插件注册图标/字体资源，其他插件引用
 * - 工具插件注册数据模板，其他插件加载使用
 * - 插件间通过资源 ID 共享配置文件
 */
interface IPluginBridgeResourceRegistry {
    
    /**
     * 注册一个资源到全局资源表
     * @param key 资源键名（插件内唯一，如 "icon_logo", "theme_dark"）
     * @param type 资源类型: "image", "audio", "font", "data", "layout", "icon", "raw"
     * @param filePath 资源的文件绝对路径
     * @param displayName 显示名称
     * @param description 描述信息
     * @param isPublic 是否允许其他插件访问
     * @param metadata 额外的键值对元数据（可选）
     * @return 注册成功返回资源的全局 ID（格式: pluginId/key），失败返回 null
     */
    fun registerAsset(
        key: String,
        type: String,
        filePath: String,
        displayName: String,
        description: String,
        isPublic: Boolean,
        metadata: Map<String, String>?
    ): String?
    
    /**
     * 注销一个资源
     * @param key 资源键名
     * @return 是否注销成功
     */
    fun unregisterAsset(key: String): Boolean
    
    /**
     * 注销自己的所有资源
     * @return 被注销的资源数量
     */
    fun unregisterAllAssets(): Int
    
    /**
     * 获取自己注册的所有资源
     * @return 资源数组
     */
    fun getMyAssets(): Array<RegisteredResource>
    
    /**
     * 获取指定资源详情
     * @param globalId 全局资源 ID（格式: pluginId/key）或仅 key（查询自己的）
     * @return 资源对象，不存在返回 null
     */
    fun getAsset(globalId: String): RegisteredResource?
    
    /**
     * 获取所有公开资源（其他插件注册的公开资源 + 自己的所有资源）
     * @return 资源数组
     */
    fun getAllPublicAssets(): Array<RegisteredResource>
    
    /**
     * 按类型筛选所有公开资源
     * @param type 资源类型
     * @return 匹配的资源数组
     */
    fun getAssetsByType(type: String): Array<RegisteredResource>
    
    /**
     * 获取指定插件注册的所有公开资源
     * @param pluginId 目标插件 ID
     * @return 资源数组
     */
    fun getPluginAssets(pluginId: String): Array<RegisteredResource>
    
    /**
     * 读取注册资源的二进制内容
     * @param globalId 全局资源 ID
     * @return 文件的字节数组，失败返回 null
     */
    fun readAssetBytes(globalId: String): ByteArray?
    
    /**
     * 读取注册资源的文本内容
     * @param globalId 全局资源 ID
     * @return 文件的文本内容，失败返回 null
     */
    fun readAssetText(globalId: String): String?
    
    /**
     * 检查资源是否存在
     * @param globalId 全局资源 ID
     * @return 存在返回 true
     */
    fun assetExists(globalId: String): Boolean
    
    /**
     * 获取已注册资源总数
     * @return 资源总数
     */
    fun getTotalAssetCount(): Int
    
    /**
     * 获取指定类型的资源数量
     * @param type 资源类型
     * @return 该类型的资源数量
     */
    fun getAssetCountByType(type: String): Int
}