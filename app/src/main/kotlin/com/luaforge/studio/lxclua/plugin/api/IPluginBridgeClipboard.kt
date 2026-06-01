package com.luaforge.studio.lxclua.plugin.api

/**
 * 剪贴板相关 API
 */
interface IPluginBridgeClipboard {
    /**
     * 复制文本到剪贴板
     */
    fun copyToClipboard(text: String)
    
    /**
     * 从剪贴板获取文本
     */
    fun getClipboardText(): String?
}