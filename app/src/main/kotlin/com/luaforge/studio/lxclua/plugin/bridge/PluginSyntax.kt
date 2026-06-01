package com.luaforge.studio.lxclua.plugin.bridge

import com.luaforge.studio.lxclua.plugin.api.IPluginBridgeSyntax
import com.luaforge.studio.lxclua.plugin.data.SyntaxLanguageRules

/**
 * 语法高亮插件扩展实现
 *
 * 允许插件注册自定义语言的语法高亮规则，包括:
 * - 关键词/正则/注释/字符串规则
 * - 代码折叠规则
 * - 文件扩展名关联
 *
 * 所有注册的语法规则通过全局注册表管理，在插件卸载时自动清理
 */
class PluginSyntax(private val pluginId: String) : IPluginBridgeSyntax {

    companion object {
        private val registeredLanguages = mutableMapOf<String, SyntaxLanguageRules>()

        /**
         * 获取指定语言的语法规则
         * @param languageId 语言标识符
         * @return 语法规则对象，未找到返回 null
         */
        fun getRules(languageId: String): SyntaxLanguageRules? {
            return registeredLanguages[languageId]
        }

        /**
         * 获取所有已注册的语法规则
         * @return 语言标识符到语法规则的映射
         */
        fun getAllRules(): Map<String, SyntaxLanguageRules> {
            return registeredLanguages.toMap()
        }

        /**
         * 根据文件扩展名查找匹配的语言规则
         * @param fileExtension 文件扩展名（含点号，如 ".mylang"）
         * @return 匹配的语法规则，未找到返回 null
         */
        fun getRulesByExtension(fileExtension: String): SyntaxLanguageRules? {
            val normalized = fileExtension.lowercase()
            return registeredLanguages.values.firstOrNull { rules ->
                rules.fileExtensions.any { it.lowercase() == normalized }
            }
        }

        /**
         * 根据文件名查找匹配的语言规则
         * @param fileName 文件名（如 "test.mylang"）
         * @return 匹配的语法规则，未找到返回 null
         */
        fun getRulesByFileName(fileName: String): SyntaxLanguageRules? {
            val dotIndex = fileName.lastIndexOf('.')
            if (dotIndex < 0) return null
            val extension = fileName.substring(dotIndex).lowercase()
            return getRulesByExtension(extension)
        }

        /**
         * 移除指定插件注册的所有语言规则
         * @param pluginId 插件 ID
         * @return 被移除的语言规则列表
         */
        fun removePluginLanguages(pluginId: String): List<SyntaxLanguageRules> {
            val removed = registeredLanguages.filter { it.value.pluginId == pluginId }
            removed.keys.forEach { registeredLanguages.remove(it) }
            return removed.values.toList()
        }
    }

    override fun registerLanguage(languageId: String, rules: Map<String, Any>): Boolean {
        val existing = registeredLanguages[languageId]
        if (existing != null && existing.pluginId != pluginId) {
            return false
        }

        val parsed = parseRules(languageId, rules)
        registeredLanguages[languageId] = parsed
        return true
    }

    override fun unregisterLanguage(languageId: String): Boolean {
        val existing = registeredLanguages[languageId] ?: return false
        if (existing.pluginId != pluginId) {
            return false
        }
        registeredLanguages.remove(languageId)
        return true
    }

    override fun getLanguageRules(languageId: String): Map<String, Any>? {
        val rules = registeredLanguages[languageId] ?: return null
        return rulesToMap(rules)
    }

    override fun getAllRegisteredLanguages(): Array<String> {
        return registeredLanguages.keys.toTypedArray()
    }

    override fun clearMyLanguages(): Int {
        val keys = registeredLanguages.filter { it.value.pluginId == pluginId }.keys
        keys.forEach { registeredLanguages.remove(it) }
        return keys.size
    }

    override fun isLanguageRegistered(languageId: String): Boolean {
        return registeredLanguages.containsKey(languageId)
    }

    /**
     * 将 SyntaxLanguageRules 转换为 Map，供 Lua 侧读取
     * @param rules 语法规则对象
     * @return 包含所有规则字段的 Map
     */
    private fun rulesToMap(rules: SyntaxLanguageRules): Map<String, Any> {
        return buildMap {
            put("languageId", rules.languageId)
            put("pluginId", rules.pluginId)
            put("keywords", rules.keywords)
            rules.commentSingleLine?.let { put("commentSingleLine", it) }
            rules.commentMultiLineBegin?.let { put("commentMultiLineBegin", it) }
            rules.commentMultiLineEnd?.let { put("commentMultiLineEnd", it) }
            put("stringSingleQuote", rules.stringSingleQuote)
            put("stringDoubleQuote", rules.stringDoubleQuote)
            put("stringBacktick", rules.stringBacktick)
            put("numbersEnabled", rules.numbersEnabled)
            put("operators", rules.operators)
            put("brackets", rules.brackets)
            put("foldingStartPatterns", rules.foldingStartPatterns)
            put("foldingEndPatterns", rules.foldingEndPatterns)
            put("fileExtensions", rules.fileExtensions)
            put("regexRules", rules.regexRules)
            put("registeredAt", rules.registeredAt)
        }
    }

    /**
     * 从 Lua 传入的 Map 解析出 SyntaxLanguageRules 对象
     * @param languageId 语言标识符
     * @param rules Lua 侧传入的规则 Map
     * @return 解析后的语法规则对象
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseRules(languageId: String, rules: Map<String, Any>): SyntaxLanguageRules {
        return SyntaxLanguageRules(
            languageId = languageId,
            pluginId = pluginId,
            keywords = (rules["keywords"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            commentSingleLine = rules["commentSingleLine"] as? String,
            commentMultiLineBegin = rules["commentMultiLineBegin"] as? String,
            commentMultiLineEnd = rules["commentMultiLineEnd"] as? String,
            stringSingleQuote = rules["stringSingleQuote"] as? Boolean ?: true,
            stringDoubleQuote = rules["stringDoubleQuote"] as? Boolean ?: true,
            stringBacktick = rules["stringBacktick"] as? Boolean ?: false,
            numbersEnabled = rules["numbersEnabled"] as? Boolean ?: true,
            operators = (rules["operators"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            brackets = (rules["brackets"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            foldingStartPatterns = (rules["foldingStartPatterns"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            foldingEndPatterns = (rules["foldingEndPatterns"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            fileExtensions = (rules["fileExtensions"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            regexRules = (rules["regexRules"] as? Map<String, String>) ?: emptyMap(),
        )
    }
}