package com.luaforge.studio.lxclua.plugin.bridge

import android.os.Handler
import android.os.Looper
import com.luaforge.studio.lxclua.plugin.api.IPluginBridgeThreads
import java.util.concurrent.Executors

/**
 * 线程工具 API
 * 
 * 使用方式: plugin.threads.runOnUIThread(callback)
 */
class PluginThreads : IPluginBridgeThreads {

    private val bgExecutor = Executors.newCachedThreadPool()

    override fun runOnUIThread(runnable: Runnable) {
        Handler(Looper.getMainLooper()).post(runnable)
    }

    override fun runOnBackgroundThread(runnable: Runnable) {
        bgExecutor.execute(runnable)
    }
}