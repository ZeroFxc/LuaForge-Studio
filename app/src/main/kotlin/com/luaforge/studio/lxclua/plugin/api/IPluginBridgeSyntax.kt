package com.luaforge.studio.lxclua.plugin.api

/**
 * 语法高亮插件扩展 API
 *
 * 允许插件注册自定义语言的语法高亮规则，包括:
 * - 关键词高亮
 * - 注释规则（单行/多行）
 * - 字符串规则（单引号/双引号/反引号）
 * - 数字高亮
 * - 操作符高亮
 * - 括号匹配
 * - 代码折叠规则（起始/结束模式）
 * - 文件扩展名关联
 * - 正则表达式匹配规则
 *
 * 使用方式:
 * ```lua
 * plugin.syntax.registerLanguage("mylang", {
 *     keywords = {"if", "else", "while", "function", "return"},
 *     commentSingleLine = "--",
 *     commentMultiLineBegin = "--[[",
 *     commentMultiLineEnd = "]]",
 *     stringSingleQuote = true,
 *     stringDoubleQuote = true,
 *     numbersEnabled = true,
 *     operators = {"+", "-", "*", "/", "="},
 *     brackets = {"(", ")", "{", "}"},
 *     foldingStartPatterns = {"function", "if", "while", "{"},
 *     foldingEndPatterns = {"end", "}"},
 *     fileExtensions = {".mylang", ".ml"},
 *     regexRules = {["\\b[A-Z][a-z]+\\b"] = "type"}
 * })
 * ```
 */
interface IPluginBridgeSyntax {

    /**
     * 注册自定义语言的语法高亮规则
     * @param languageId 语言唯一标识符，如 "mylang"
     * @param rules 语法规则 Map，包含 keywords, commentSingleLine, commentMultiLineBegin,
     *              commentMultiLineEnd, stringSingleQuote, stringDoubleQuote, stringBacktick,
     *              numbersEnabled, operators, brackets, foldingStartPatterns,
     *              foldingEndPatterns, fileExtensions, regexRules 等字段
     * @return 注册成功返回 true，如果该 languageId 已被其他插件注册则返回 false
     */
    fun registerLanguage(languageId: String, rules: Map<String, Any>): Boolean

    /**
     * 取消注册指定语言的语法高亮规则
     * @param languageId 语言标识符
     * @return 如果该语言属于当前插件且移除成功返回 true，否则返回 false
     */
    fun unregisterLanguage(languageId: String): Boolean

    /**
     * 获取指定语言的语法规则
     * @param languageId 语言标识符
     * @return 规则 Map，若未找到返回 null
     */
    fun getLanguageRules(languageId: String): Map<String, Any>?

    /**
     * 获取所有已注册的语言标识符列表
     * @return 语言标识符数组
     */
    fun getAllRegisteredLanguages(): Array<String>

    /**
     * 清除当前插件注册的所有语言规则
     * @return 被移除的语言数量
     */
    fun clearMyLanguages(): Int

    /**
     * 检查指定语言是否已被注册
     * @param languageId 语言标识符
     * @return 已注册返回 true
     */
    fun isLanguageRegistered(languageId: String): Boolean
}