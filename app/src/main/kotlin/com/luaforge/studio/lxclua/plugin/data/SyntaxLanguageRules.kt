package com.luaforge.studio.lxclua.plugin.data

/**
 * 插件注册的语法高亮语言规则
 *
 * 定义了自定义语言的高亮规则，包括关键词、注释、字符串、数字、
 * 操作符、括号、代码折叠模式和正则匹配规则
 *
 * 使用方式:
 * ```lua
 * plugin.syntax.registerLanguage("mylang", {
 *     keywords = {"if", "else", "while", "function", "return", "local"},
 *     commentSingleLine = "--",
 *     commentMultiLineBegin = "--[[",
 *     commentMultiLineEnd = "]]",
 *     stringSingleQuote = true,
 *     stringDoubleQuote = true,
 *     numbersEnabled = true,
 *     operators = {"+", "-", "*", "/", "=", "==", "~="},
 *     brackets = {"(", ")", "{", "}", "[", "]"},
 *     foldingStartPatterns = {"function", "if", "while", "do", "{"},
 *     foldingEndPatterns = {"end", "}"},
 *     fileExtensions = {".mylang", ".ml"},
 *     regexRules = {["\\b[A-Z][a-z]+\\b"] = "type", ["\\d+"] = "number"}
 * })
 * ```
 */
data class SyntaxLanguageRules(
    /** 语言唯一标识符 */
    val languageId: String,
    /** 注册此语言的插件 ID */
    val pluginId: String,
    /** 关键词列表，作为关键字高亮 */
    val keywords: List<String> = emptyList(),
    /** 单行注释起始符号，如 "--", "//", "#" */
    val commentSingleLine: String? = null,
    /** 多行注释起始符号，如 "--[[" */
    val commentMultiLineBegin: String? = null,
    /** 多行注释结束符号，如 "]]" */
    val commentMultiLineEnd: String? = null,
    /** 是否高亮单引号字符串 */
    val stringSingleQuote: Boolean = true,
    /** 是否高亮双引号字符串 */
    val stringDoubleQuote: Boolean = true,
    /** 是否高亮反引号字符串 */
    val stringBacktick: Boolean = false,
    /** 是否高亮数字字面量 */
    val numbersEnabled: Boolean = true,
    /** 操作符列表 */
    val operators: List<String> = emptyList(),
    /** 括号配对列表，用于括号匹配高亮 */
    val brackets: List<String> = emptyList(),
    /** 代码折叠起始模式列表，匹配到这些关键词/符号时创建折叠区域起点 */
    val foldingStartPatterns: List<String> = emptyList(),
    /** 代码折叠结束模式列表，匹配到这些关键词/符号时结束折叠区域 */
    val foldingEndPatterns: List<String> = emptyList(),
    /** 该语言关联的文件扩展名列表，如 {".mylang", ".ml"} */
    val fileExtensions: List<String> = emptyList(),
    /** 正则匹配规则，key 为正则表达式字符串，value 为高亮类型名称（如 "number", "type", "keyword" 等） */
    val regexRules: Map<String, String> = emptyMap(),
    /** 注册时间戳（毫秒） */
    val registeredAt: Long = System.currentTimeMillis()
)