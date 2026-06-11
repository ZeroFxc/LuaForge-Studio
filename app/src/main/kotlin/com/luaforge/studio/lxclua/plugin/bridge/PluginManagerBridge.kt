package com.luaforge.studio.lxclua.plugin.bridge

import android.content.Context
import com.luaforge.studio.lxclua.R
import com.luaforge.studio.lxclua.plugin.PluginManager
import java.io.File

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
        return PluginManager.loadedPlugins.find { it.manifest.id == pluginId }?.enabled?.value ?: false
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
        return PluginManager.loadedPlugins.count { it.enabled.value }
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
        val context = PluginManager.appContext ?: return category
        return when (category.lowercase()) {
            "core" -> context.getString(R.string.plugin_category_core)
            "normal" -> context.getString(R.string.plugin_category_normal)
            "dependent" -> context.getString(R.string.plugin_category_dependent)
            "extension" -> context.getString(R.string.plugin_category_extension)
            else -> category
        }
    }

    // ==================== 综合信息获取 ====================

    /**
     * 获取插件的完整信息（聚合 manifest + 运行时状态）
     * @param pluginId 插件ID
     * @return PluginInfo 对象，未找到返回 null
     */
    fun getPluginInfo(pluginId: String): com.luaforge.studio.lxclua.plugin.data.PluginInfo? {
        val plugin = PluginManager.loadedPlugins.find { it.manifest.id == pluginId } ?: return null
        val manifest = plugin.manifest

        // 检测是否有 WebUI：检查 main.lua 中是否包含 webview 或 html 调用
        val hasWebUI = run {
            try {
                val mainFile = java.io.File(plugin.directory, manifest.main)
                mainFile.exists() && mainFile.readText().contains("webview", ignoreCase = true)
            } catch (e: Exception) { false }
        }

        return com.luaforge.studio.lxclua.plugin.data.PluginInfo(
            id = manifest.id,
            name = manifest.name,
            version = manifest.version,
            description = manifest.description,
            author = manifest.author,
            main = manifest.main,
            type = manifest.type,
            category = manifest.pluginType,
            apiVersion = manifest.apiVersion,
            enabled = plugin.enabled.value,
            isCore = manifest.pluginType.equals("core", ignoreCase = true),
            directory = plugin.directory.absolutePath,
            dependencies = manifest.dependencies.map { it.pluginId }.toTypedArray(),
            dependents = getDependentPluginIds(pluginId),
            dependencyInfos = manifest.dependencies.map { dep ->
                com.luaforge.studio.lxclua.plugin.data.DependencyInfo(
                    pluginId = dep.pluginId,
                    minVersion = dep.minVersion,
                    required = dep.required
                )
            }.toTypedArray(),
            homepage = manifest.homepage,
            hasWebUI = hasWebUI,
            icon = manifest.icon,
            tags = com.luaforge.studio.lxclua.plugin.data.TagInfo
                .parseAll(manifest.tags, plugin.directory.absolutePath)
                .toTypedArray()
        )
    }

    /**
     * 获取所有已安装插件的完整信息列表
     * @return PluginInfo 数组
     */
    fun getAllPluginInfoList(): Array<com.luaforge.studio.lxclua.plugin.data.PluginInfo> {
        return PluginManager.loadedPlugins.mapNotNull { plugin ->
            getPluginInfo(plugin.manifest.id)
        }.toTypedArray()
    }

    /**
     * 获取所有已安装插件的 ID 列表并按分类过滤
     * @param category 分类过滤（core/normal/dependent/extension），为空则返回全部
     * @return 插件 ID 数组
     */
    fun getPluginIdsByCategory(category: String): Array<String> {
        return PluginManager.loadedPlugins
            .filter { category.isEmpty() || it.manifest.pluginType.equals(category, ignoreCase = true) }
            .map { it.manifest.id }
            .toTypedArray()
    }

    // ==================== 插件生命周期管理 ====================

    /**
     * 彻底卸载插件（删除目录 + 清理数据 + 清理 UI 注册 + 清理 SharedPreferences）
     * @param pluginId 插件ID
     * @return 是否卸载成功
     */
    fun uninstallPlugin(pluginId: String): Boolean {
        val context = PluginManager.appContext ?: return false
        return PluginManager.deletePlugin(context, pluginId)
    }

    /**
     * 重载指定插件（不删除目录，仅重新加载代码）
     * @param pluginId 插件ID
     * @return 是否重载成功
     */
    fun reloadPlugin(pluginId: String): Boolean {
        val context = PluginManager.appContext ?: return false
        val plugin = PluginManager.loadedPlugins.find { it.manifest.id == pluginId } ?: return false
        try {
            // 先卸载再重新加载
            PluginManager.unloadPluginInternal(plugin)
            PluginManager.loadPluginInternal(plugin)
        } catch (e: Exception) {
            android.util.Log.e("PluginManagerBridge", "重载插件失败: $pluginId", e)
            return false
        }
        return true
    }

    /**
     * 重载所有已启用插件
     * @return 成功重载的插件数量
     */
    fun reloadAllPlugins(): Int {
        val enabledIds = PluginManager.loadedPlugins
            .filter { it.enabled.value }
            .map { it.manifest.id }
        var count = 0
        for (id in enabledIds) {
            if (reloadPlugin(id)) count++
        }
        return count
    }

    // ==================== 依赖管理 ====================

    /**
     * 检查指定插件的所有依赖是否已满足（依赖插件已安装且已启用）
     * @param pluginId 插件ID
     * @return 缺失的依赖 ID 数组，为空表示所有依赖已满足
     */
    fun checkDependencies(pluginId: String): Array<String> {
        val plugin = PluginManager.loadedPlugins.find { it.manifest.id == pluginId } ?: return emptyArray()
        val missing = mutableListOf<String>()
        for (dep in plugin.manifest.dependencies) {
            val depPlugin = PluginManager.loadedPlugins.find { it.manifest.id == dep.pluginId }
            if (depPlugin == null || !depPlugin.enabled.value) {
                missing.add(dep.pluginId)
            }
        }
        return missing.toTypedArray()
    }

    /**
     * 获取插件的所有依赖详情（用于构建依赖树）
     * @param pluginId 插件ID
     * @return DependencyInfo 数组
     */
    fun getDependencyTree(pluginId: String): Array<com.luaforge.studio.lxclua.plugin.data.DependencyInfo> {
        val visited = mutableSetOf<String>()
        val result = mutableListOf<com.luaforge.studio.lxclua.plugin.data.DependencyInfo>()
        fun collectDeps(id: String) {
            if (id in visited) return
            visited.add(id)
            val plugin = PluginManager.loadedPlugins.find { it.manifest.id == id } ?: return
            for (dep in plugin.manifest.dependencies) {
                result.add(com.luaforge.studio.lxclua.plugin.data.DependencyInfo(
                    pluginId = dep.pluginId,
                    minVersion = dep.minVersion,
                    required = dep.required
                ))
                collectDeps(dep.pluginId)
            }
        }
        collectDeps(pluginId)
        return result.toTypedArray()
    }

    // ==================== 导入导出 ====================

    /**
     * 导出插件为 ZIP 文件
     * @param context 上下文
     * @param pluginId 插件ID
     * @param destZipFile 目标 ZIP 文件路径，为 null 时自动生成到 Downloads 目录
     * @return 是否导出成功
     */
    fun exportPluginToZip(context: Context, pluginId: String, destZipFile: File?): Boolean {
        val target = destZipFile ?: run {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            File(downloadsDir, "${pluginId}_export.zip")
        }
        return PluginManager.exportPluginToZip(context, pluginId, target)
    }

    /**
     * 导出插件到指定目录
     * @param context 上下文
     * @param pluginId 插件ID
     * @param destDir 目标目录
     * @return 是否导出成功
     */
    fun exportPluginToDirectory(context: Context, pluginId: String, destDir: String): Boolean {
        return PluginManager.exportPluginToDirectory(context, pluginId, File(destDir))
    }

    /**
     * 从 ZIP 文件安装插件
     * @param zipPath ZIP 文件路径
     * @return 安装结果描述
     */
    fun installPluginFromZip(zipPath: String): String {
        val context = PluginManager.appContext ?: return "应用上下文不可用"
        val zipFile = File(zipPath)
        if (!zipFile.exists()) return "ZIP 文件不存在: $zipPath"
        val result = PluginManager.installPluginFromZip(context, zipFile)
        return result.fold(
            onSuccess = { manifest -> "插件 ${manifest.name} 安装成功" },
            onFailure = { e -> "安装失败: ${e.message}" }
        )
    }

    /**
     * 从目录安装插件
     * @param dirPath 目录路径
     * @return 安装结果描述
     */
    fun installPluginFromDirectory(dirPath: String): String {
        val context = PluginManager.appContext ?: return "应用上下文不可用"
        val sourceDir = File(dirPath)
        if (!sourceDir.exists() || !sourceDir.isDirectory) return "目录不存在: $dirPath"
        val result = PluginManager.installPluginFromDirectory(context, sourceDir)
        return result.fold(
            onSuccess = { manifest -> "插件 ${manifest.name} 导入成功" },
            onFailure = { e -> "导入失败: ${e.message}" }
        )
    }

    /**
     * 依赖信息数据类（兼容旧接口）
     */
    data class DependencyInfo(
        val pluginId: String,
        val minVersion: String,
        val required: Boolean
    )

    // ==================== 依赖缺失检查 ====================

    /**
     * 获取插件的缺失依赖 ID 列表（仅必需依赖）
     * @param pluginId 插件ID
     * @return 缺失的依赖 ID 数组
     */
    fun getMissingDependencies(pluginId: String): Array<String> {
        return PluginManager.getMissingDependencies(pluginId).toTypedArray()
    }

    /**
     * 检查插件的完整依赖状态
     * @param pluginId 插件ID
     * @return 每个依赖的详细状态（JSON 字符串数组格式）
     */
    fun getDependencyStatusList(pluginId: String): Array<String> {
        return PluginManager.checkDependencyStatus(pluginId).map { status ->
            """{"pluginId":"${status.pluginId}","required":${status.required},"""" +
            """"exists":${status.exists},"isEnabled":${status.isEnabled},"""" +
            """"installedVersion":${status.installedVersion?.let { "\"$it\"" } ?: "null"},"""" +
            """"minVersion":"${status.minVersion}","versionMatch":${status.versionMatch}}"""
        }.toTypedArray()
    }

    // ==================== 安装版本预检 ====================

    /**
     * 安装前版本预检
     * @param manifestJson manifest.json 的 JSON 字符串
     * @return 预检结果 JSON 字符串，供插件/UI层判断是否弹窗
     */
    fun preCheckInstall(manifestJson: String): String {
        return try {
            val manifest = com.google.gson.Gson().fromJson(manifestJson, com.luaforge.studio.lxclua.plugin.data.PluginManifest::class.java)
            val check = PluginManager.preCheckInstall(manifest)
            """{"alreadyExists":${check.alreadyExists},"existingVersion":${check.existingVersion?.let { "\"$it\"" } ?: "null"},"""" +
            """"newVersion":"${check.newVersion}","isSameVersion":${check.isSameVersion},"""" +
            """"isUpdate":${check.isUpdate},"isDowngrade":${check.isDowngrade}}"""
        } catch (e: Exception) {
            """{"error":"${e.message}"}"""
        }
    }

    // ==================== 属性动态修改 ====================

    /**
     * 更新插件名称（持久化到 manifest.json + 重新扫描）
     */
    fun updatePluginName(pluginId: String, newName: String): Boolean {
        val context = PluginManager.appContext ?: return false
        return PluginManager.updatePluginName(context, pluginId, newName)
    }

    /**
     * 更新插件描述
     */
    fun updatePluginDescription(pluginId: String, newDescription: String): Boolean {
        val context = PluginManager.appContext ?: return false
        return PluginManager.updatePluginDescription(context, pluginId, newDescription)
    }

    /**
     * 更新插件作者
     */
    fun updatePluginAuthor(pluginId: String, newAuthor: String): Boolean {
        val context = PluginManager.appContext ?: return false
        return PluginManager.updatePluginAuthor(context, pluginId, newAuthor)
    }

    /**
     * 更新插件主页
     */
    fun updatePluginHomepage(pluginId: String, newHomepage: String?): Boolean {
        val context = PluginManager.appContext ?: return false
        return PluginManager.updatePluginHomepage(context, pluginId, newHomepage)
    }

    /**
     * 更新插件标签列表
     */
    fun updatePluginTags(pluginId: String, newTagsJson: String): Boolean {
        val context = PluginManager.appContext ?: return false
        return try {
            val tags = com.google.gson.Gson().fromJson(newTagsJson, Array<String>::class.java)
            PluginManager.updatePluginTags(context, pluginId, tags.toList())
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 比较两个版本号
     * @return 1: v1 > v2, 0: v1 == v2, -1: v1 < v2
     */
    fun compareVersions(v1: String, v2: String): Int {
        return PluginManager.compareVersions(v1, v2)
    }
}