package com.luaforge.studio.lxclua.plugin.api

import com.luaforge.studio.lxclua.plugin.data.PluginResources

/**
 * 动态资源相关 API
 */
interface IPluginBridgeResources {
    /**
     * 加载插件资源 APK
     * @param apkPath APK 文件路径
     * @return 资源包对象，用于访问资源
     */
    fun loadResources(apkPath: String): PluginResources?
    
    /**
     * 获取插件资源中的字符串
     * @param resources 资源包对象
     * @param resName 资源名称
     * @return 字符串值
     */
    fun getResourceString(resources: PluginResources, resName: String): String?
    
    /**
     * 获取插件资源中的 Drawable
     * @param resources 资源包对象
     * @param resName 资源名称
     * @return Drawable 对象
     */
    fun getResourceDrawable(resources: PluginResources, resName: String): android.graphics.drawable.Drawable?
    
    /**
     * 获取插件资源中的颜色值
     * @param resources 资源包对象
     * @param resName 资源名称
     * @return 颜色值
     */
    fun getResourceColor(resources: PluginResources, resName: String): Int
    
    /**
     * 获取插件资源 ID
     * @param resources 资源包对象
     * @param resName 资源名称
     * @param resType 资源类型 (string, drawable, color, layout 等)
     * @return 资源 ID，找不到返回 0
     */
    fun getResourceId(resources: PluginResources, resName: String, resType: String): Int
}