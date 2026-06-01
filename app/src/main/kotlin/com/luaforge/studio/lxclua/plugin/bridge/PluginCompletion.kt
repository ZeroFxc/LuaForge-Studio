package com.luaforge.studio.lxclua.plugin.bridge

import com.luaforge.studio.lxclua.langs.lua.LuaLanguage
import com.luaforge.studio.lxclua.langs.lua.completion.MyIdentifierAutoComplete
import com.luaforge.studio.lxclua.plugin.PluginManager
import com.luaforge.studio.lxclua.plugin.api.IPluginBridgeCompletion
import com.luaforge.studio.lxclua.utils.LogCatcher
import java.util.UUID

/**
 * 插件补全系统实现
 *
 * 允许插件向编辑器补全系统注入自定义补全数据:
 * - 关键字: 注入到语言的关键字列表中
 * - 包函数: 注入到语言的包函数列表中
 * - 变量类型: 注入到变量类型映射中，用于成员补全
 * - 自定义补全提供器: 注册回调函数，在补全时动态返回补全项
 *
 * 所有插件补全数据通过全局注册表管理，在插件卸载时自动清理
 */
class PluginCompletion(private val pluginId: String) : IPluginBridgeCompletion {

    companion object {
        private val pluginKeywords = mutableMapOf<String, MutableSet<String>>()
        private val pluginPackages = mutableMapOf<String, MutableMap<String, MutableList<String>>>()
        private val pluginVariableTypes = mutableMapOf<String, MutableMap<String, String>>()
        private val pluginProviders = mutableMapOf<String, CompletionProviderInfo>()

        data class CompletionProviderInfo(
            val id: String,
            val pluginId: String,
            val language: String,
            val callback: Any
        )

        fun getKeywordsForPlugin(pluginId: String): Set<String> {
            return pluginKeywords[pluginId]?.toSet() ?: emptySet()
        }

        fun getAllKeywords(): Set<String> {
            val all = mutableSetOf<String>()
            pluginKeywords.values.forEach { all.addAll(it) }
            return all
        }

        fun getPackagesForPlugin(pluginId: String): Map<String, List<String>> {
            return pluginPackages[pluginId]?.mapValues { it.value.toList() } ?: emptyMap()
        }

        fun getAllPackages(): Map<String, List<String>> {
            val all = mutableMapOf<String, MutableList<String>>()
            pluginPackages.values.forEach { pluginPkg ->
                pluginPkg.forEach { (pkgName, funcs) ->
                    all.getOrPut(pkgName) { mutableListOf() }.addAll(funcs)
                }
            }
            return all.mapValues { it.value.distinct() }
        }

        fun getVariableTypesForPlugin(pluginId: String): Map<String, String> {
            return pluginVariableTypes[pluginId]?.toMap() ?: emptyMap()
        }

        fun getAllVariableTypes(): Map<String, String> {
            val all = mutableMapOf<String, String>()
            pluginVariableTypes.values.forEach { all.putAll(it) }
            return all
        }

        fun getProvidersForLanguage(language: String): List<CompletionProviderInfo> {
            return pluginProviders.values.filter { it.language == language || it.language == "*" }
        }

        fun removeAllPluginCompletionData(pluginId: String): Int {
            var count = 0
            count += pluginKeywords.remove(pluginId)?.size ?: 0
            count += pluginPackages.remove(pluginId)?.let { pkgMap ->
                pkgMap.values.sumOf { it.size }
            } ?: 0
            count += pluginVariableTypes.remove(pluginId)?.size ?: 0
            val providerKeys = pluginProviders.filter { it.value.pluginId == pluginId }.keys
            providerKeys.forEach { pluginProviders.remove(it) }
            count += providerKeys.size
            return count
        }

        /**
         * 将插件注册的补全数据注入到 MyIdentifierAutoComplete 实例中
         */
        fun injectIntoAutoComplete(autoComplete: MyIdentifierAutoComplete) {
            getAllKeywords().forEach { keyword ->
                autoComplete.addKeyword(keyword)
            }

            getAllPackages().forEach { (pkgName, funcs) ->
                autoComplete.addPackage(pkgName, funcs)
            }

            getAllVariableTypes().forEach { (varName, className) ->
                autoComplete.mmap[varName] = className
            }
        }

        /**
         * 将新添加的补全数据推送到活跃编辑器的 LuaLanguage 实例
         */
        fun pushKeywordToActiveEditor(keyword: String) {
            pushToActiveEditors { lang ->
                lang.addCompletionKeyword(keyword)
            }
        }

        fun pushPackageToActiveEditor(packageName: String, functions: List<String>) {
            pushToActiveEditors { lang ->
                lang.addPackage(packageName, functions)
            }
        }

        fun pushVariableTypeToActiveEditor(variableName: String, className: String) {
            pushToActiveEditors { lang ->
                lang.addCompletionVariableType(variableName, className)
            }
        }

        private fun pushToActiveEditors(action: (LuaLanguage) -> Unit) {
            try {
                PluginManager.activeViewModel?.getActiveEditor()?.let { editor ->
                    val lang = editor.editorLanguage
                    if (lang is LuaLanguage) {
                        action(lang)
                    }
                }
            } catch (e: Exception) {
                LogCatcher.w("PluginCompletion", "推送补全数据到编辑器失败: ${e.message}")
            }
        }
    }

    private fun makeGlobalId(key: String): String = "$pluginId/$key"

    override fun addKeyword(keyword: String): Boolean {
        val keywords = pluginKeywords.getOrPut(pluginId) { mutableSetOf() }
        val added = keywords.add(keyword)
        if (added) {
            pushKeywordToActiveEditor(keyword)
        }
        return added
    }

    override fun addKeywords(keywords: Array<String>): Int {
        var count = 0
        keywords.forEach { keyword ->
            if (addKeyword(keyword)) count++
        }
        return count
    }

    override fun removeKeyword(keyword: String): Boolean {
        val keywords = pluginKeywords[pluginId] ?: return false
        return keywords.remove(keyword)
    }

    override fun addPackage(packageName: String, functions: Array<String>): Boolean {
        val packages = pluginPackages.getOrPut(pluginId) { mutableMapOf() }
        val funcList = packages.getOrPut(packageName) { mutableListOf() }
        var added = false
        for (func in functions) {
            if (!funcList.contains(func)) {
                funcList.add(func)
                added = true
            }
        }
        if (added) {
            pushPackageToActiveEditor(packageName, funcList.toList())
        }
        return added
    }

    override fun removePackage(packageName: String): Boolean {
        val packages = pluginPackages[pluginId] ?: return false
        return packages.remove(packageName) != null
    }

    override fun addVariableType(variableName: String, className: String): Boolean {
        val varTypes = pluginVariableTypes.getOrPut(pluginId) { mutableMapOf() }
        val existing = varTypes[variableName]
        varTypes[variableName] = className
        val changed = existing != className
        if (changed) {
            pushVariableTypeToActiveEditor(variableName, className)
        }
        return changed
    }

    override fun removeVariableType(variableName: String): Boolean {
        val varTypes = pluginVariableTypes[pluginId] ?: return false
        return varTypes.remove(variableName) != null
    }

    override fun registerProvider(language: String, callback: Any): String? {
        val providerId = makeGlobalId(UUID.randomUUID().toString().take(8))
        val info = CompletionProviderInfo(
            id = providerId,
            pluginId = pluginId,
            language = language,
            callback = callback
        )
        pluginProviders[providerId] = info
        return providerId
    }

    override fun unregisterProvider(providerId: String): Boolean {
        return pluginProviders.remove(providerId) != null
    }

    override fun clearMyKeywords(): Int {
        val keywords = pluginKeywords.remove(pluginId) ?: return 0
        return keywords.size
    }

    override fun clearMyPackages(): Int {
        val packages = pluginPackages.remove(pluginId) ?: return 0
        return packages.size
    }

    override fun clearMyVariableTypes(): Int {
        val varTypes = pluginVariableTypes.remove(pluginId) ?: return 0
        return varTypes.size
    }

    override fun clearMyProviders(): Int {
        val keys = pluginProviders.filter { it.value.pluginId == pluginId }.keys
        keys.forEach { pluginProviders.remove(it) }
        return keys.size
    }

    override fun clearAll(): Array<Int> {
        return arrayOf(
            clearMyKeywords(),
            clearMyPackages(),
            clearMyVariableTypes(),
            clearMyProviders()
        )
    }
}