package com.luaforge.studio.lxclua.plugin.bridge

import com.luaforge.studio.lxclua.plugin.PluginManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

/**
 * 编辑器操作 API
 * 
 * 使用方式: plugin.editor.getText()
 */
class PluginEditor {
    
    /**
     * 获取当前活动文件路径
     */
    fun getActiveFile(): String? {
        return PluginManager.activeViewModel?.activeFileState?.file?.absolutePath
    }
    
    /**
     * 获取当前文件内容
     */
    fun getText(): String? {
        return PluginManager.activeViewModel?.activeFileState?.content
    }
    
    /**
     * 设置当前文件内容
     */
    fun setText(text: String) {
        PluginManager.activeViewModel?.let { vm ->
            vm.activeFileState?.onContentChanged(text)
            vm.activeFileState?.let { state ->
                state.content = text
            }
        }
    }
    
    /**
     * 在光标位置插入文本
     */
    fun insertText(text: String) {
        PluginManager.activeViewModel?.insertSymbolToCorrectEditor(text)
    }
    
    /**
     * 获取选中的文本
     */
    fun getSelectedText(): String? {
        val editor = PluginManager.activeViewModel?.getActiveEditor() ?: return null
        val cursor = editor.cursor
        val leftIndex = cursor.left
        val rightIndex = cursor.right
        return if (leftIndex != rightIndex) {
            editor.text.substring(leftIndex, rightIndex)
        } else {
            ""
        }
    }
    
    /**
     * 获取光标位置 [行, 列]
     */
    fun getCursorPosition(): IntArray? {
        val editor = PluginManager.activeViewModel?.getActiveEditor() ?: return null
        val cursor = editor.cursor
        return intArrayOf(cursor.leftLine, cursor.leftColumn)
    }
    
    /**
     * 跳转到指定行
     */
    fun gotoLine(line: Int) {
        PluginManager.activeViewModel?.getActiveEditor()?.jumpToLine(line.coerceAtLeast(0))
    }
    
    /**
     * 跳转到指定位置
     */
    fun gotoPosition(line: Int, column: Int) {
        PluginManager.activeViewModel?.getActiveEditor()?.setSelectionRegion(line, column, line, column)
    }
    
    /**
     * 打开文件
     */
    fun openFile(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            PluginManager.activeViewModel?.openFile(file)
        }
    }
    
    /**
     * 保存当前文件
     */
    fun saveFile() {
        kotlinx.coroutines.GlobalScope.launch {
            PluginManager.activeViewModel?.saveCurrentFileSilently()
        }
    }
    
    /**
     * 保存所有文件
     */
    fun saveAll() {
        kotlinx.coroutines.GlobalScope.launch {
            PluginManager.activeViewModel?.saveAllFilesSilently()
        }
    }
    
    /**
     * 撤销操作
     */
    fun undo() {
        PluginManager.activeViewModel?.getActiveEditor()?.undo()
    }
    
    /**
     * 重做操作
     */
    fun redo() {
        PluginManager.activeViewModel?.getActiveEditor()?.redo()
    }
    
    /**
     * 获取所有打开的文件路径
     */
    fun getOpenFiles(): Array<String>? {
        return PluginManager.activeViewModel?.openFiles?.map { it.file.absolutePath }?.toTypedArray()
    }
    
    /**
     * 关闭当前文件
     */
    fun closeFile() {
        val index = PluginManager.activeViewModel?.activeFileIndex ?: -1
        if (index >= 0) {
            PluginManager.activeViewModel?.closeFile(index)
        }
    }
    
    /**
     * 获取当前文件的语言类型（根据扩展名判断）
     * @return 语言标识，如 "lua", "java", "kotlin", "xml", "json", "txt" 等
     */
    fun getLanguage(): String? {
        val filePath = getActiveFile() ?: return null
        val extension = filePath.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "lua" -> "lua"
            "java" -> "java"
            "kt", "kts" -> "kotlin"
            "xml" -> "xml"
            "json" -> "json"
            "txt" -> "txt"
            "md" -> "markdown"
            "js" -> "javascript"
            "ts" -> "typescript"
            "py" -> "python"
            "cpp", "hpp", "cc", "hh" -> "cpp"
            "c", "h" -> "c"
            "cs" -> "csharp"
            "go" -> "go"
            "rs" -> "rust"
            "swift" -> "swift"
            "dart" -> "dart"
            "html" -> "html"
            "css" -> "css"
            "gradle" -> "gradle"
            "properties" -> "properties"
            "yml", "yaml" -> "yaml"
            else -> extension.ifEmpty { "unknown" }
        }
    }
    
    /**
     * 获取当前文件的扩展名（不含点）
     */
    fun getFileExtension(): String? {
        val filePath = getActiveFile() ?: return null
        return filePath.substringAfterLast('.', "").ifEmpty { null }
    }
    
    /**
     * 检查当前文件是否为指定语言
     * @param language 语言标识
     * @return 是否匹配
     */
    fun isLanguage(language: String): Boolean {
        return getLanguage()?.equals(language, ignoreCase = true) ?: false
    }
    
    /**
     * 获取所有支持的语言列表
     */
    fun getSupportedLanguages(): Array<String> {
        return arrayOf(
            "lua", "java", "kotlin", "xml", "json", "txt", "markdown",
            "javascript", "typescript", "python", "cpp", "c", "csharp",
            "go", "rust", "swift", "dart", "html", "css", "gradle",
            "properties", "yaml"
        )
    }
}