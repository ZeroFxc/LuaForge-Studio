package com.luaforge.studio.lxclua.plugin.api

import com.luaforge.studio.lxclua.plugin.api.callbacks.HttpCallback

/**
 * 网络请求相关 API
 */
interface IPluginBridgeNetwork {
    /**
     * HTTP GET 请求
     * @param url 请求地址
     * @param headers 请求头（可选）
     * @param callback 回调函数
     */
    fun httpGet(url: String, headers: Map<String, String>?, callback: HttpCallback)
    
    /**
     * HTTP POST 请求
     * @param url 请求地址
     * @param body 请求体
     * @param headers 请求头（可选）
     * @param callback 回调函数
     */
    fun httpPost(url: String, body: String, headers: Map<String, String>?, callback: HttpCallback)
}