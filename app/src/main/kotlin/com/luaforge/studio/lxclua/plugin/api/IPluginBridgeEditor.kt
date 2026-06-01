package com.luaforge.studio.lxclua.plugin.api

/**
 * 编辑器操作相关 API
 */
interface IPluginBridgeEditor {
    /**
     * 获取当前活动文件路径
     */
    fun getActiveFile(): String?
    
    /**
     * 获取当前活动文件内容
     */
    fun getActiveText(): String?
    
    /**
     * 设置当前活动文件内容
     */
    fun setActiveText(text: String)
    
    /**
     * 在当前光标位置插入文本
     */
    fun insertText(text: String)
    
    /**
     * 获取当前选中的文本
     */
    fun getSelectedText(): String?
    
    /**
     * 设置编辑器选区
     */
    fun setSelection(start: Int, end: Int)
    
    /**
     * 获取当前光标位置
     * @return 返回 int[2]，[0]为行号，[1]为列号
     */
    fun getCursorPosition(): IntArray?
    
    /**
     * 跳转到指定行
     * @param line 行号（从0开始）
     */
    fun gotoLine(line: Int)
    
    /**
     * 跳转到指定位置
     */
    fun gotoPosition(line: Int, column: Int)
    
    /**
     * 获取所有打开的文件路径
     */
    fun getOpenFiles(): Array<String>?
    
    /**
     * 关闭当前文件
     */
    fun closeActiveFile()
    
    /**
     * 打开指定文件
     */
    fun openFile(filePath: String)
    
    /**
     * 保存当前文件
     */
    fun saveActiveFile()
    
    /**
     * 保存所有文件
     */
    fun saveAllFiles()
    
    /**
     * 撤销
     */
    fun undo()
    
    /**
     * 重做
     */
    fun redo()
    
    /**
     * 查找文本
     */
    fun findText(query: String, caseSensitive: Boolean, regex: Boolean)
    
    /**
     * 替换文本
     */
    fun replaceText(query: String, replacement: String, replaceAll: Boolean)
}