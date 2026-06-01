package com.luaforge.studio.lxclua.plugin.bridge

import com.luaforge.studio.lxclua.plugin.PluginManager

/**
 * 插件管理 API
 * 
 * 提供插件管理页面所需的基础信息获取接口
 * 不做复杂判断，只提供原始数据
 * 
 * 使用方式: plugin.manager.getPluginVersion("plugin_id")
 */
class PluginManagerBridge {
    
    /**
     * 获取所有已安装插件的 ID 列表
     *
     * 返回 Array 而非 List：LuaJava 在转换 Kotlin List 时可能丢失长度/索引，
     * 改用 Java 数组后 Lua 端可直接用 ipairs 安全遍历。
     */
    fun getPluginIds(): Array<String> {
        return PluginManager.loadedPlugins.map { it.manifest.id }.toTypedArray()
    }
    
    /**
     * 获取插件显示名称
     */
    fun getPluginName(pluginId: String): String? {
        return PluginManager.loadedPlugins.find { it.manifest.id == pluginId }?.manifest?.name
    }
    
    /**
     * 获取插件版本号
     */
    fun getPluginVersion(pluginId: String): String? {
        return PluginManager.loadedPlugins.find { it.manifest.id == pluginId }?.manifest?.version
    }
    
    /**
     * 获取插件描述
     */
    fun getPluginDescription(pluginId: String): String? {
        return PluginManager.loadedPlugins.find { it.manifest.id == pluginId }?.manifest?.description
    }
    
    /**
     * 获取插件作者
     */
    fun getPluginAuthor(pluginId: String): String? {
        return PluginManager.loadedPlugins.find { it.manifest.id == pluginId }?.manifest?.author
    }
    
    /**
     * 获取插件类型（lua/dex/apk）
     */
    fun getPluginType(pluginId: String): String? {
        return PluginManager.loadedPlugins.find { it.manifest.id == pluginId }?.manifest?.type
    }
    
    /**
     * 获取插件分类（core/normal/dependent/extension）
     */
    fun getPluginCategory(pluginId: String): String? {
        return PluginManager.loadedPlugins.find { it.manifest.id == pluginId }?.manifest?.pluginType
    }
    
    /**
     * 检查插件是否存在
     */
    fun pluginExists(pluginId: String): Boolean {
        return PluginManager.loadedPlugins.any { it.manifest.id == pluginId }
    }
    
    /**
     * 检查插件是否已启用
     */
    fun isPluginEnabled(pluginId: String): Boolean {
        return PluginManager.loadedPlugins.find { it.manifest.id == pluginId }?.enabled ?: false
    }
    
    /**
     * 获取插件依赖的插件 ID 列表
     *
     * 返回 Array 而非 List，规避 LuaJava 转换问题。
     */
    fun getPluginDependencies(pluginId: String): Array<String> {
        return PluginManager.loadedPlugins.find { it.manifest.id == pluginId }
            ?.manifest?.dependencies?.map { it.pluginId }?.toTypedArray() ?: emptyArray()
    }
    
    /**
     * 获取插件依赖的详细信息列表
     *
     * 返回 Array 而非 List，规避 LuaJava 转换问题。
     */
    fun getPluginDependenciesInfo(pluginId: String): Array<DependencyInfo> {
        return PluginManager.loadedPlugins.find { it.manifest.id == pluginId }
            ?.manifest?.dependencies?.map { dep ->
                DependencyInfo(
                    pluginId = dep.pluginId,
                    minVersion = dep.minVersion,
                    required = dep.required
                )
            }?.toTypedArray() ?: emptyArray()
    }
    
    /**
     * 获取依赖指定插件的所有插件 ID 列表
     *
     * 返回 Array 而非 List，规避 LuaJava 转换问题。
     */
    fun getDependentPluginIds(pluginId: String): Array<String> {
        return PluginManager.loadedPlugins
            .filter { plugin ->
                plugin.manifest.dependencies.any { it.pluginId == pluginId }
            }
            .map { it.manifest.id }
            .toTypedArray()
    }
    
    /**
     * 获取插件的依赖要求版本
     * @return 返回该插件要求依赖插件的版本，如果未指定依赖则返回 null
     */
    fun getRequiredDependencyVersion(pluginId: String, dependencyId: String): String? {
        return PluginManager.loadedPlugins.find { it.manifest.id == pluginId }
            ?.manifest?.dependencies?.find { it.pluginId == dependencyId }?.minVersion
    }
    
    /**
     * 检查插件是否需要指定依赖
     */
    fun requiresDependency(pluginId: String, dependencyId: String): Boolean {
        return PluginManager.loadedPlugins.find { it.manifest.id == pluginId }
            ?.manifest?.dependencies?.any { it.pluginId == dependencyId } ?: false
    }
    
    /**
     * 检查依赖是否为必需的
     */
    fun isDependencyRequired(pluginId: String, dependencyId: String): Boolean {
        return PluginManager.loadedPlugins.find { it.manifest.id == pluginId }
            ?.manifest?.dependencies?.find { it.pluginId == dependencyId }?.required ?: false
    }
    
    /**
     * 获取插件数量
     */
    fun getPluginCount(): Int {
        return PluginManager.loadedPlugins.size
    }
    
    /**
     * 获取已启用插件数量
     */
    fun getEnabledPluginCount(): Int {
        return PluginManager.loadedPlugins.count { it.enabled }
    }
    
    /**
     * 获取插件主页/文档链接
     */
    fun getPluginHomepage(pluginId: String): String? {
        return PluginManager.loadedPlugins.find { it.manifest.id == pluginId }?.manifest?.homepage
    }
    
    /**
     * 启用指定插件
     * @param pluginId 插件ID
     */
    fun enablePlugin(pluginId: String) {
        PluginManager.appContext?.let {
            PluginManager.setPluginEnabled(it, pluginId, true)
        }
    }
    
    /**
     * 禁用指定插件
     * @param pluginId 插件ID
     */
    fun disablePlugin(pluginId: String) {
        PluginManager.appContext?.let {
            PluginManager.setPluginEnabled(it, pluginId, false)
        }
    }
    
    /**
     * 切换插件启用状态
     * @param pluginId 插件ID
     * @param enabled 是否启用
     */
    fun setPluginEnabled(pluginId: String, enabled: Boolean) {
        PluginManager.appContext?.let {
            PluginManager.setPluginEnabled(it, pluginId, enabled)
        }
    }
    
    /**
     * 动态切换插件状态（立即生效）
     * @param pluginId 插件ID
     * @param enabled 是否启用
     */
    fun togglePlugin(pluginId: String, enabled: Boolean) {
        PluginManager.appContext?.let {
            PluginManager.togglePlugin(it, pluginId, enabled)
        }
    }
    
    /**
     * 删除指定插件
     * @param pluginId 插件ID
     * @return 是否删除成功
     */
    fun deletePlugin(pluginId: String): Boolean {
        return PluginManager.appContext?.let {
            PluginManager.deletePlugin(it, pluginId)
        } ?: false
    }
    
    /**
     * 检查插件是否为核心插件（无法禁用）
     * @param pluginId 插件ID
     */
    fun isCorePlugin(pluginId: String): Boolean {
        return PluginManager.loadedPlugins.find { it.manifest.id == pluginId }
            ?.manifest?.pluginType.equals("core", ignoreCase = true)
    }
    
    /**
     * 获取插件的分类标签
     * @param pluginId 插件ID
     */
    fun getPluginCategoryLabel(pluginId: String): String {
        val category = getPluginCategory(pluginId) ?: "normal"
        return when (category.lowercase()) {
            "core" -> "核心"
            "normal" -> "普通"
            "dependent" -> "附属"
            "extension" -> "扩展"
            else -> category
        }
    }
    
    /**
     * 依赖信息数据类
     */
    data class DependencyInfo(
        val pluginId: String,
        val minVersion: String,
        val required: Boolean
    )
}