package com.luaforge.studio.lxclua.plugin.api

/**
 * 配置存储相关 API
 */
interface IPluginBridgeConfig {
    /**
     * 获取插件配置
     */
    fun getConfig(key: String, defaultValue: String): String
    
    /**
     * 设置插件配置
     */
    fun setConfig(key: String, value: String)
    
    /**
     * 获取插件配置（整数）
     */
    fun getConfigInt(key: String, defaultValue: Int): Int
    
    /**
     * 设置插件配置（整数）
     */
    fun setConfigInt(key: String, value: Int)
    
    /**
     * 获取插件配置（布尔）
     */
    fun getConfigBoolean(key: String, defaultValue: Boolean): Boolean
    
    /**
     * 设置插件配置（布尔）
     */
    fun setConfigBoolean(key: String, value: Boolean)
    
    /**
     * 移除插件配置
     */
    fun removeConfig(key: String)
}