package com.luaforge.studio.lxclua.plugin.bridge

import com.luaforge.studio.lxclua.plugin.PluginManager

/**
 * 符号栏操作 API（供 Lua 插件使用）
 *
 * 使用方式: plugin.symbolBar.getSymbols()
 */
class PluginSymbolBar {

    /**
     * 获取当前符号栏中可用的所有符号
     * @return 符号字符串数组
     */
    fun getSymbols(): Array<String> {
        val baseSymbols = arrayOf(
            "function()", "(", ")", "[", "]", "{", "}", "\"", "=", ":",
            ".", ",", ";", "_", "+", "-", "*", "/", "\\", "%",
            "#", "^", "$", "?", "&", "|", "<", ">", "~", "'"
        )
        val custom = PluginManager.customSymbolBarSymbols.toList()
        return (baseSymbols.toList() + custom).toTypedArray()
    }

    /**
     * 向符号栏添加自定义符号
     * @param symbol 要添加的符号字符串
     * @return 添加成功返回 true，符号已存在或为内置符号返回 false
     */
    fun addSymbol(symbol: String): Boolean {
        val baseSet = setOf(
            "function()", "(", ")", "[", "]", "{", "}", "\"", "=", ":",
            ".", ",", ";", "_", "+", "-", "*", "/", "\\", "%",
            "#", "^", "$", "?", "&", "|", "<", ">", "~", "'"
        )
        if (symbol in baseSet) return false
        if (symbol in PluginManager.customSymbolBarSymbols) return false
        PluginManager.customSymbolBarSymbols.add(symbol)
        return true
    }

    /**
     * 从符号栏移除自定义符号
     * @param symbol 要移除的符号字符串
     * @return 移除成功返回 true，未找到或为内置符号返回 false
     */
    fun removeSymbol(symbol: String): Boolean {
        val baseSet = setOf(
            "function()", "(", ")", "[", "]", "{", "}", "\"", "=", ":",
            ".", ",", ";", "_", "+", "-", "*", "/", "\\", "%",
            "#", "^", "$", "?", "&", "|", "<", ">", "~", "'"
        )
        if (symbol in baseSet) return false
        return PluginManager.customSymbolBarSymbols.remove(symbol)
    }

    /**
     * 清除所有自定义添加的符号，恢复为默认符号列表
     */
    fun clearCustomSymbols() {
        PluginManager.customSymbolBarSymbols.clear()
    }

    /**
     * 获取指定符号的使用频率
     * @param symbol 符号字符串
     * @return 使用次数
     */
    fun getSymbolFrequency(symbol: String): Int {
        return PluginManager.activeViewModel?.symbolFrequencyMap?.get(symbol) ?: 0
    }

    /**
     * 获取所有符号的使用频率映射
     * @return 符号到使用次数的映射（Lua 中为 table）
     */
    fun getAllSymbolFrequencies(): Map<String, Int> {
        return PluginManager.activeViewModel?.symbolFrequencyMap?.toMap() ?: emptyMap()
    }

    /**
     * 增加指定符号的使用频率计数
     * @param symbol 符号字符串
     */
    fun incrementSymbolFrequency(symbol: String) {
        PluginManager.activeViewModel?.incrementSymbolFrequency(symbol)
    }

    /**
     * 向编辑器光标位置插入符号
     * @param symbol 要插入的符号字符串
     */
    fun insertSymbol(symbol: String) {
        PluginManager.activeViewModel?.insertSymbolToCorrectEditor(symbol)
    }

    /**
     * 获取当前选中的类名（短名）
     * @return 选中的类名，无选中时返回 null
     */
    fun getSelectedClassName(): String? {
        return PluginManager.activeViewModel?.selectedClassName
    }

    /**
     * 获取当前选中类名的候选全限定类名列表
     * @return 候选全限定类名数组，无候选时返回 null
     */
    fun getSelectedClassCandidates(): Array<String>? {
        return PluginManager.activeViewModel?.selectedClassCandidates?.toTypedArray()
    }

    /**
     * 设置面板展开状态
     * @param expanded true 展开面板，false 收起面板
     */
    fun setPanelExpanded(expanded: Boolean) {
        val panelState = PluginManager.activePanelState ?: return
        if (expanded) {
            panelState.animateToHeight(panelState.maxHeight * 0.8f)
        } else {
            panelState.animateToHeight(panelState.minHeight)
        }
    }

    /**
     * 获取面板是否处于展开状态
     * @return true 表示面板已展开
     */
    fun isPanelExpanded(): Boolean {
        val panelState = PluginManager.activePanelState ?: return false
        return panelState.isAboveThreshold
    }

    /**
     * 设置面板高度（像素值）
     * @param heightPx 面板高度（像素）
     */
    fun setPanelHeight(heightPx: Float) {
        PluginManager.activePanelState?.updateHeight(heightPx)
    }

    /**
     * 获取当前面板高度（像素值）
     * @return 面板高度（像素）
     */
    fun getPanelHeight(): Float {
        return PluginManager.activePanelState?.height ?: 0f
    }

    /**
     * 获取面板最小高度（像素值）
     * @return 最小高度（像素）
     */
    fun getPanelMinHeight(): Float {
        return PluginManager.activePanelState?.minHeight ?: 0f
    }

    /**
     * 获取面板最大高度（像素值）
     * @return 最大高度（像素）
     */
    fun getPanelMaxHeight(): Float {
        return PluginManager.activePanelState?.maxHeight ?: 0f
    }
}