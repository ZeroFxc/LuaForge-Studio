package com.luaforge.studio.lxclua.plugin

import android.content.Context
import android.os.Environment
import com.google.gson.Gson
import com.luaforge.studio.lxclua.plugin.api.IPlugin
import com.luaforge.studio.lxclua.plugin.data.PluginManifest
import com.luaforge.studio.lxclua.plugin.loaders.DexPluginLoader
import com.luaforge.studio.lxclua.plugin.loaders.LuaPluginLoader
import com.luaforge.studio.lxclua.plugin.state.EventManager
import com.luaforge.studio.lxclua.plugin.state.PluginEvents
import com.luaforge.studio.lxclua.plugin.state.UIState
import com.luaforge.studio.lxclua.ui.editor.QuickAction
import com.luaforge.studio.lxclua.ui.editor.viewmodel.EditorViewModel
import com.luajava.LuaState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import java.io.File

/**
 * 已加载插件包装类
 */
data class LoadedPlugin(
    val manifest: PluginManifest,
    val directory: File,
    var enabled: Boolean,
    var luaState: LuaState? = null,
    var dexPlugin: IPlugin? = null
)

/**
 * 插件管理引擎单例
 * 
 * 负责插件的扫描、加载、卸载和管理
 */
object PluginManager {
    private const val PREFS_NAME = "plugin_settings"
    private const val PREF_ENABLED_PREFIX = "plugin_enabled_"
    
    var appContext: Context? = null
        private set
    
    var currentActivity: android.app.Activity? = null
    
    // 内存中的已扫描插件列表
    val loadedPlugins = mutableStateListOf<LoadedPlugin>()
    
    // 当前活动的编辑器 ViewModel
    var activeViewModel: EditorViewModel? = null
    
    // 当前活动的工程路径
    val currentProjectPath = mutableStateOf<String?>(null)
    
    // 动态注册的快捷功能动作列表
    val pluginQuickActions = mutableStateListOf<QuickAction>()
    private val quickActionsMap = mutableMapOf<String, QuickAction>()
    
    // Compose 对话框状态
    sealed class DialogState {
        data class Message(
            val title: String,
            val message: String,
            val onDismiss: () -> Unit
        ) : DialogState()
        
        data class Confirm(
            val title: String,
            val message: String,
            val onConfirm: () -> Unit,
            val onCancel: (() -> Unit)?,
            val onDismiss: () -> Unit
        ) : DialogState()
        
        data class Input(
            val title: String,
            val hint: String,
            val defaultValue: String,
            val onInput: (String) -> Unit,
            val onDismiss: () -> Unit
        ) : DialogState()
        
        data class SingleChoice(
            val title: String,
            val items: Array<String>,
            val selectedIndex: Int,
            val onSelect: (Int) -> Unit,
            val onDismiss: () -> Unit
        ) : DialogState()
        
        data class MultiChoice(
            val title: String,
            val items: Array<String>,
            val checkedItems: BooleanArray,
            val onConfirm: (BooleanArray) -> Unit,
            val onDismiss: () -> Unit
        ) : DialogState()
    }
    
    val currentDialog = mutableStateOf<DialogState?>(null)
    
    /**
     * 获取插件存放根目录
     */
    fun getPluginsDir(context: Context): File {
        val extDir = Environment.getExternalStorageDirectory()
        val mainDir = File(extDir, "LXC-LUA/plugins")
        if (!mainDir.exists()) {
            mainDir.mkdirs()
        }
        return if (mainDir.exists() && mainDir.canWrite()) {
            mainDir
        } else {
            File(context.filesDir, "plugins").apply { mkdirs() }
        }
    }
    
    private fun getPrefs(context: Context) = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * 初始化插件管理器
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        scanPlugins(context)
        loadEnabledPlugins()
    }
    
    /**
     * 扫描所有的插件目录，解析元数据
     */
    fun scanPlugins(context: Context) {
        val pluginsDir = getPluginsDir(context)
        val dirs = pluginsDir.listFiles { file -> file.isDirectory } ?: emptyArray()
        val manifestList = mutableListOf<LoadedPlugin>()
        val prefs = getPrefs(context)
        
        for (dir in dirs) {
            val manifestFile = File(dir, "manifest.json")
            if (manifestFile.exists()) {
                try {
                    val content = manifestFile.readText()
                    val manifest = Gson().fromJson(content, PluginManifest::class.java)
                    val enabled = prefs.getBoolean(PREF_ENABLED_PREFIX + manifest.id, true)
                    
                    // 保留已加载插件的运行状态
                    val existing = loadedPlugins.find { it.manifest.id == manifest.id }
                    if (existing != null) {
                        manifestList.add(existing.copy(manifest = manifest, directory = dir, enabled = enabled))
                    } else {
                        manifestList.add(LoadedPlugin(manifest, dir, enabled))
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PluginManager", "解析插件元数据失败: ${dir.absolutePath}", e)
                }
            }
        }
        
        // 卸载已删除的插件
        val currentIds = manifestList.map { it.manifest.id }.toSet()
        loadedPlugins.forEach {
            if (it.manifest.id !in currentIds) {
                unloadPluginInternal(it)
            }
        }
        
        loadedPlugins.clear()
        loadedPlugins.addAll(manifestList)
    }
    
    /**
     * 加载所有已启用的插件（带依赖检查）
     */
    fun loadEnabledPlugins() {
        // 先加载核心插件
        for (plugin in loadedPlugins) {
            if (plugin.enabled && isCorePlugin(plugin) && plugin.luaState == null && plugin.dexPlugin == null) {
                try {
                    loadPluginInternal(plugin)
                } catch (e: Exception) {
                    android.util.Log.e("PluginManager", "加载核心插件失败: ${plugin.manifest.name}", e)
                }
            }
        }
        
        // 然后加载普通插件（按依赖顺序）
        for (plugin in loadedPlugins) {
            if (plugin.enabled && !isCorePlugin(plugin) && plugin.luaState == null && plugin.dexPlugin == null) {
                try {
                    loadPluginInternal(plugin)
                } catch (e: Exception) {
                    android.util.Log.e("PluginManager", "加载插件失败: ${plugin.manifest.name}", e)
                }
            }
        }
        
        // 触发所有插件加载完成事件
        EventManager.fireEvent(PluginEvents.ON_ALL_PLUGINS_LOADED)
    }
    
    /**
     * 检查是否为核心插件
     */
    private fun isCorePlugin(plugin: LoadedPlugin): Boolean {
        return plugin.manifest.pluginType.equals("core", ignoreCase = true)
    }
    
    /**
     * 检查插件依赖是否满足
     * @return Pair(是否满足, 不满足的原因)
     */
    fun checkDependencies(plugin: LoadedPlugin): Pair<Boolean, String> {
        val manifest = plugin.manifest
        
        for (dep in manifest.dependencies) {
            val depPlugin = loadedPlugins.find { it.manifest.id == dep.pluginId }
            
            if (depPlugin == null) {
                if (dep.required) {
                    return Pair(false, "缺少必需依赖: ${dep.pluginId}")
                }
                continue
            }
            
            // 检查版本要求
            if (!isVersionSatisfied(depPlugin.manifest.version, dep.minVersion)) {
                if (dep.required) {
                    return Pair(false, "依赖 ${dep.pluginId} 版本要求 ${dep.minVersion}，当前版本 ${depPlugin.manifest.version}")
                }
            }
            
            // 检查依赖是否已启用
            if (!depPlugin.enabled) {
                if (dep.required) {
                    return Pair(false, "必需依赖 ${dep.pluginId} 未启用")
                }
            }
        }
        
        return Pair(true, "依赖检查通过")
    }
    
    /**
     * 版本号比较
     * @return 当前版本是否满足最低版本要求
     */
    private fun isVersionSatisfied(currentVersion: String, minVersion: String): Boolean {
        val currentParts = currentVersion.split(".")
        val minParts = minVersion.split(".")
        
        for (i in 0 until maxOf(currentParts.size, minParts.size)) {
            val current = currentParts.getOrNull(i)?.toIntOrNull() ?: 0
            val min = minParts.getOrNull(i)?.toIntOrNull() ?: 0
            
            if (current > min) return true
            if (current < min) return false
        }
        return true
    }
    
    /**
     * 单个插件加载逻辑（带依赖检查）
     */
    private fun loadPluginInternal(plugin: LoadedPlugin) {
        val context = appContext ?: return
        val manifest = plugin.manifest
        
        // 检查依赖
        val (depsOk, depsReason) = checkDependencies(plugin)
        if (!depsOk) {
            android.util.Log.w("PluginManager", "插件 ${manifest.name} 依赖检查失败: $depsReason，跳过加载")
            return
        }
        
        val type = manifest.type.lowercase(java.util.Locale.getDefault())
        
        try {
            if (type == "lua") {
                LuaPluginLoader.load(plugin, context)
            } else if (type == "dex" || type == "apk") {
                DexPluginLoader.load(plugin, context)
            }
        } catch (e: Exception) {
            android.util.Log.e("PluginManager", "加载插件失败: ${manifest.name}", e)
        }
    }
    
    /**
     * 单个插件卸载逻辑
     */
    private fun unloadPluginInternal(plugin: LoadedPlugin) {
        val pluginId = plugin.manifest.id
        
        try {
            // 清理 Lua 状态
            plugin.luaState?.close()
            plugin.luaState = null
            
            // 调用插件的卸载回调
            plugin.dexPlugin?.onUnload()
            plugin.dexPlugin = null
            
            // 清理该插件注册的 UI 元素
            removePluginUiElements(pluginId)
            
            // 移除该插件的所有事件监听器
            EventManager.removePluginListeners(pluginId)
            
            // 触发插件卸载事件
            EventManager.fireEvent(PluginEvents.ON_PLUGIN_UNLOADED, pluginId)
        } catch (e: Exception) {
            android.util.Log.e("PluginManager", "卸载插件失败: ${plugin.manifest.name}", e)
        }
    }
    
    /**
     * 移除插件注册的所有 UI 元素
     */
    private fun removePluginUiElements(pluginId: String) {
        // 移除快捷操作
        quickActionsMap.keys.removeAll { it.startsWith("${pluginId}_") }
        updateQuickActions()
        
        // 移除菜单项
        UIState.removePluginMenuItems(pluginId)
        
        // 移除文件树菜单项
        UIState.removeFileTreeMenuItems(pluginId)
    }
    
    /**
     * 更新快捷操作列表
     */
    fun updateQuickActions() {
        pluginQuickActions.clear()
        pluginQuickActions.addAll(quickActionsMap.values)
    }
    
    /**
     * 添加快捷操作
     */
    fun addQuickAction(pluginId: String, key: String, action: QuickAction) {
        val globalKey = "${pluginId}_$key"
        quickActionsMap[globalKey] = action
        updateQuickActions()
    }
    
    /**
     * 移除快捷操作
     */
    fun removeQuickAction(pluginId: String, key: String) {
        val globalKey = "${pluginId}_$key"
        quickActionsMap.remove(globalKey)
        updateQuickActions()
    }
    
    /**
     * 清除插件的所有快捷操作
     */
    fun clearQuickActions(pluginId: String) {
        quickActionsMap.keys.removeAll { it.startsWith("${pluginId}_") }
        updateQuickActions()
    }
    
    /**
     * 启用/禁用插件
     */
    fun setPluginEnabled(context: Context, pluginId: String, enabled: Boolean) {
        val plugin = loadedPlugins.find { it.manifest.id == pluginId }
        if (plugin != null) {
            plugin.enabled = enabled
            getPrefs(context).edit()
                .putBoolean(PREF_ENABLED_PREFIX + pluginId, enabled)
                .apply()
            
            if (enabled) {
                loadPluginInternal(plugin)
            } else {
                unloadPluginInternal(plugin)
            }
        }
    }
    
    /**
     * 触发事件
     */
    fun fireEvent(eventName: String, vararg args: Any?) {
        EventManager.fireEvent(eventName, *args)
    }
    
    /**
     * 通知事件（与 fireEvent 功能相同，为了兼容旧代码）
     */
    fun notifyEvent(eventName: String, vararg args: Any?) {
        EventManager.fireEvent(eventName, *args)
    }
    
    /**
     * 动态切换插件启用状态
     */
    fun togglePlugin(context: Context, pluginId: String, enabled: Boolean) {
        val index = loadedPlugins.indexOfFirst { it.manifest.id == pluginId }
        if (index != -1) {
            val plugin = loadedPlugins[index]
            
            getPrefs(context).edit()
                .putBoolean(PREF_ENABLED_PREFIX + pluginId, enabled)
                .apply()
                
            if (enabled) {
                try {
                    loadPluginInternal(plugin)
                } catch (e: Exception) {
                    android.util.Log.e("PluginManager", "动态启用插件失败: $pluginId", e)
                }
            } else {
                unloadPluginInternal(plugin)
            }
            
            val updatedPlugin = plugin.copy(enabled = enabled)
            loadedPlugins[index] = updatedPlugin
        }
    }
    
    /**
     * 彻底删除插件
     */
    fun deletePlugin(context: Context, pluginId: String): Boolean {
        val index = loadedPlugins.indexOfFirst { it.manifest.id == pluginId }
        if (index != -1) {
            val plugin = loadedPlugins[index]
            unloadPluginInternal(plugin)
            
            val success = plugin.directory.deleteRecursively()
            
            getPrefs(context).edit()
                .remove(PREF_ENABLED_PREFIX + pluginId)
                .apply()
                
            scanPlugins(context)
            return success
        }
        return false
    }
    
    /**
     * 从 Zip 文件解压并安装插件
     */
    fun installPluginFromZip(context: Context, zipFile: File): Result<PluginManifest> {
        try {
            val tempDir = File(context.cacheDir, "temp_plugin_extract_${System.currentTimeMillis()}")
            tempDir.mkdirs()
            
            com.luaforge.studio.lxclua.utils.FileUtil.extractZip(zipFile, tempDir)
            
            var manifestFile = File(tempDir, "manifest.json")
            if (!manifestFile.exists()) {
                val children = tempDir.listFiles()
                val nestedDir = children?.find { it.isDirectory && File(it, "manifest.json").exists() }
                if (nestedDir != null) {
                    manifestFile = File(nestedDir, "manifest.json")
                } else {
                    return Result.failure(Exception("ZIP 包内未找到 manifest.json"))
                }
            }
            
            val content = manifestFile.readText()
            val manifest = Gson().fromJson(content, PluginManifest::class.java)
            
            val pluginsDir = getPluginsDir(context)
            val destDir = File(pluginsDir, manifest.id)
            
            if (destDir.exists()) {
                togglePlugin(context, manifest.id, false)
                getPrefs(context).edit().remove(PREF_ENABLED_PREFIX + manifest.id).apply()
            }
            
            destDir.deleteRecursively()
            val sourceDir = manifestFile.parentFile ?: tempDir
            sourceDir.copyRecursively(destDir, overwrite = true)
            tempDir.deleteRecursively()
            
            scanPlugins(context)
            
            return Result.success(manifest)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
