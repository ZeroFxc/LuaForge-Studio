package com.luaforge.studio.lxclua.plugin.api

/**
 * 代码补全插件扩展 API
 *
 * 允许插件向编辑器补全系统注入自定义补全项:
 * - 关键字补全
 * - 包函数补全
 * - 变量类型映射（用于成员补全）
 * - 自定义补全提供器（回调函数）
 *
 * 使用方式:
 * ```lua
 * plugin.completion.addKeyword("my_function")
 * plugin.completion.addPackage("mypackage", {"func1", "func2"})
 * plugin.completion.addVariableType("myObj", "com.example.MyClass")
 * plugin.completion.registerProvider("lua", function(prefix, language, filePath)
 *     return {
 *         {label = "item1", desc = "Description", commit = "item1", kind = "Keyword"}
 *     }
 * end)
 * ```
 */
interface IPluginBridgeCompletion {

    fun addKeyword(keyword: String): Boolean

    fun addKeywords(keywords: Array<String>): Int

    fun removeKeyword(keyword: String): Boolean

    fun addPackage(packageName: String, functions: Array<String>): Boolean

    fun removePackage(packageName: String): Boolean

    fun addVariableType(variableName: String, className: String): Boolean

    fun removeVariableType(variableName: String): Boolean

    fun registerProvider(language: String, callback: Any): String?

    fun unregisterProvider(providerId: String): Boolean

    fun clearMyKeywords(): Int

    fun clearMyPackages(): Int

    fun clearMyVariableTypes(): Int

    fun clearMyProviders(): Int

    fun clearAll(): Array<Int>
}