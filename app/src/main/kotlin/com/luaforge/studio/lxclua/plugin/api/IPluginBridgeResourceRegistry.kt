package com.luaforge.studio.lxclua.plugin.api

import com.luaforge.studio.lxclua.plugin.data.RegisteredResource

/**
 * 插件资源注册表 API
 */
interface IPluginBridgeResourceRegistry {
    
    fun registerAsset(
        key: String,
        type: String,
        filePath: String,
        displayName: String,
        description: String,
        isPublic: Boolean,
        metadata: Map<String, String>?
    ): String?
    
    fun unregisterAsset(key: String): Boolean
    
    fun unregisterAllAssets(): Int
    
    fun getMyAssets(): Array<RegisteredResource>
    
    fun getAsset(globalId: String): RegisteredResource?
    
    fun getAllPublicAssets(): Array<RegisteredResource>
    
    fun getAssetsByType(type: String): Array<RegisteredResource>
    
    fun getPluginAssets(pluginId: String): Array<RegisteredResource>
    
    fun readAssetBytes(globalId: String): ByteArray?
    
    fun readAssetText(globalId: String): String?
    
    fun assetExists(globalId: String): Boolean
    
    fun getTotalAssetCount(): Int
    
    fun getAssetCountByType(type: String): Int
}