package com.luaforge.studio.lxclua.plugin.api

/**
 * 进度对话框句柄
 */
interface ProgressDialogHandle {
    /**
     * 更新进度
     * @param progress 进度值（0-100）
     */
    fun setProgress(progress: Int)
    
    /**
     * 更新消息
     */
    fun setMessage(message: String)
    
    /**
     * 关闭对话框
     */
    fun dismiss()
    
    /**
     * 检查是否已关闭
     */
    fun isShowing(): Boolean
}