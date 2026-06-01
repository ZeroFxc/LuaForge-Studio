package com.luaforge.studio.lxclua.plugin.bridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * 剪贴板操作 API
 * 
 * 使用方式: plugin.clipboard.copy("text")
 */
class PluginClipboard(private val context: Context) {
    
    private val clipboardManager by lazy {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    
    /**
     * 复制文本到剪贴板
     */
    fun copy(text: String) {
        val clipData = ClipData.newPlainText("LuaForge Studio", text)
        clipboardManager.setPrimaryClip(clipData)
    }
    
    /**
     * 从剪贴板获取文本
     */
    fun paste(): String? {
        if (clipboardManager.hasPrimaryClip()) {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                return clip.getItemAt(0).text?.toString()
            }
        }
        return null
    }
    
    /**
     * 检查剪贴板是否有内容
     */
    fun hasContent(): Boolean {
        return clipboardManager.hasPrimaryClip()
    }
    
    /**
     * 清空剪贴板
     */
    fun clear() {
        clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
    }
}