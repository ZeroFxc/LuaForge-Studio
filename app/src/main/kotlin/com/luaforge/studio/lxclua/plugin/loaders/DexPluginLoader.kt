package com.luaforge.studio.lxclua.plugin.loaders

import android.content.Context
import com.luaforge.studio.lxclua.plugin.LoadedPlugin
import com.luaforge.studio.lxclua.plugin.api.IPlugin
import com.luaforge.studio.lxclua.plugin.bridge.PluginBridgeImpl
import com.luaforge.studio.lxclua.plugin.state.EventManager
import com.luaforge.studio.lxclua.plugin.state.PluginEvents
import dalvik.system.DexClassLoader
import java.io.File

/**
 * DEX/APK 插件加载器
 */
object DexPluginLoader {
    
    fun load(plugin: LoadedPlugin, context: Context) {
        val manifest = plugin.manifest
        val pluginFile = File(plugin.directory, manifest.main)
        
        if (!pluginFile.exists()) {
            android.util.Log.e("PluginManager", "插件文件不存在: ${pluginFile.absolutePath}")
            return
        }
        
        try {
            // 1. 拷贝 DEX/APK 文件到安全缓存区
            val cachePluginFile = File(context.codeCacheDir, "${manifest.id}_${manifest.version}.dex")
            if (!cachePluginFile.exists() || cachePluginFile.length() != pluginFile.length()) {
                pluginFile.copyTo(cachePluginFile, overwrite = true)
            }
            
            // 2. 动态加载类并调用 onLoad 入口
            val classLoader = DexClassLoader(
                cachePluginFile.absolutePath,
                context.codeCacheDir.absolutePath,
                null,
                javaClass.classLoader
            )
            
            val mainClass = manifest.mainClass 
                ?: throw IllegalStateException("DEX 插件必须指定 mainClass")
            
            val clazz = classLoader.loadClass(mainClass)
            val instance = clazz.getDeclaredConstructor().newInstance() as? IPlugin
            
            if (instance != null) {
                val bridge = PluginBridgeImpl(manifest.id)
                instance.onLoad(context, bridge)
                plugin.dexPlugin = instance
                
                // 触发插件加载完成事件
                EventManager.fireEvent(PluginEvents.ON_PLUGIN_LOADED, manifest.id)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("PluginManager", "加载 DEX 插件失败: ${manifest.name}", e)
        }
    }
}
