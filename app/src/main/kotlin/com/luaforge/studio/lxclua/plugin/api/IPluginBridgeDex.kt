package com.luaforge.studio.lxclua.plugin.api

import com.luaforge.studio.lxclua.plugin.api.callbacks.DexLoadCallback

/**
 * DEX/类加载相关 API
 */
interface IPluginBridgeDex {
    /**
     * 加载 DEX 文件
     * @param dexPath DEX 文件路径
     * @return 是否加载成功
     */
    fun loadDex(dexPath: String): Boolean
    
    /**
     * 从网络下载并加载 DEX
     * @param url DEX 文件下载地址
     * @param callback 加载结果回调
     */
    fun loadDexFromUrl(url: String, callback: DexLoadCallback)
    
    /**
     * 获取已加载的 DEX 类加载器
     * @return 类加载器
     */
    fun getPluginClassLoader(): ClassLoader?
}