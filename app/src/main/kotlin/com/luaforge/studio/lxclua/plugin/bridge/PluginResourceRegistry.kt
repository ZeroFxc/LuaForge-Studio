package com.luaforge.studio.lxclua.plugin.bridge

import com.luaforge.studio.lxclua.plugin.api.IPluginBridgeResourceRegistry
import com.luaforge.studio.lxclua.plugin.data.RegisteredResource
import java.io.File

/**
 * 插件资源注册表实现
 * 
 * 使用方式:
 * - plugin.assets.register(key, type, path, name, desc, public, meta)  注册资源
 * - plugin.assets.unregister(key)       注销资源
 * - plugin.assets.getMyAssets()         获取自己的资源
 * - plugin.assets.getAllPublicAssets()  获取所有公开资源
 * - plugin.assets.getAssetsByType(type) 按类型筛选
 * - plugin.assets.readAssetBytes(id)    读取资源二进制
 * - plugin.assets.readAssetText(id)     读取资源文本
 * 
 * 全局资源 ID 格式: pluginId/resourceKey
 * 
 * 资源注册表是全局单例的，所有插件共享同一张表。
 */
class PluginResourceRegistry(private val pluginId: String) : IPluginBridgeResourceRegistry {

    companion object {
        private val registry = mutableMapOf<String, RegisteredResource>()

        fun removeAllPluginAssets(pluginId: String): Int {
            val keysToRemove = registry.filter { it.value.pluginId == pluginId }.keys
            keysToRemove.forEach { registry.remove(it) }
            return keysToRemove.size
        }

        fun getResource(globalId: String): RegisteredResource? {
            return registry[globalId]
        }
    }

    private fun makeGlobalId(key: String): String = "$pluginId/$key"

    override fun registerAsset(
        key: String,
        type: String,
        filePath: String,
        displayName: String,
        description: String,
        isPublic: Boolean,
        metadata: Map<String, String>?
    ): String? {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) return null

        val globalId = makeGlobalId(key)
        val resource = RegisteredResource(
            id = globalId,
            pluginId = pluginId,
            key = key,
            type = type.lowercase(),
            filePath = file.absolutePath,
            displayName = displayName,
            description = description,
            extension = file.extension.lowercase().ifEmpty { type.lowercase() },
            size = file.length(),
            registeredAt = System.currentTimeMillis(),
            metadata = metadata ?: emptyMap(),
            isPublic = isPublic
        )
        registry[globalId] = resource
        return globalId
    }

    override fun unregisterAsset(key: String): Boolean {
        return registry.remove(makeGlobalId(key)) != null
    }

    override fun unregisterAllAssets(): Int {
        return Companion.removeAllPluginAssets(pluginId)
    }

    override fun getMyAssets(): Array<RegisteredResource> {
        return registry.values.filter { it.pluginId == pluginId }.toTypedArray()
    }

    override fun getAsset(globalId: String): RegisteredResource? {
        return if (globalId.contains("/")) {
            registry[globalId]
        } else {
            registry[makeGlobalId(globalId)]
        }
    }

    override fun getAllPublicAssets(): Array<RegisteredResource> {
        return registry.values.filter {
            it.isPublic || it.pluginId == pluginId
        }.toTypedArray()
    }

    override fun getAssetsByType(type: String): Array<RegisteredResource> {
        val lowerType = type.lowercase()
        return registry.values.filter {
            (it.isPublic || it.pluginId == pluginId) && it.type == lowerType
        }.toTypedArray()
    }

    override fun getPluginAssets(pluginId: String): Array<RegisteredResource> {
        return registry.values.filter {
            it.pluginId == pluginId && (it.isPublic || pluginId == this.pluginId)
        }.toTypedArray()
    }

    override fun readAssetBytes(globalId: String): ByteArray? {
        val resolved = resolveGlobalId(globalId) ?: return null
        return try {
            File(resolved.filePath).readBytes()
        } catch (e: Exception) {
            null
        }
    }

    override fun readAssetText(globalId: String): String? {
        val resolved = resolveGlobalId(globalId) ?: return null
        return try {
            File(resolved.filePath).readText()
        } catch (e: Exception) {
            null
        }
    }

    override fun assetExists(globalId: String): Boolean {
        val resolved = resolveGlobalId(globalId) ?: return false
        return File(resolved.filePath).exists()
    }

    override fun getTotalAssetCount(): Int {
        return registry.size
    }

    override fun getAssetCountByType(type: String): Int {
        val lowerType = type.lowercase()
        return registry.values.count { it.type == lowerType }
    }

    private fun resolveGlobalId(globalId: String): RegisteredResource? {
        return if (globalId.contains("/")) {
            registry[globalId]
        } else {
            registry[makeGlobalId(globalId)]
        }
    }
}