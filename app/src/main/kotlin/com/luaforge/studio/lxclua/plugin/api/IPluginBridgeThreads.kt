package com.luaforge.studio.lxclua.plugin.api

/**
 * 线程工具相关 API
 */
interface IPluginBridgeThreads {
    /**
     * 在主线程执行代码
     */
    fun runOnUIThread(runnable: Runnable)
    
    /**
     * 在后台线程执行代码
     */
    fun runOnBackgroundThread(runnable: Runnable)
}