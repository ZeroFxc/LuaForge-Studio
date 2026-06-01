package com.luaforge.studio.lxclua.plugin

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.google.gson.Gson
import com.luaforge.studio.lxclua.ui.editor.QuickAction
import com.luaforge.studio.lxclua.ui.editor.viewmodel.EditorViewModel
import com.luajava.LuaState
import com.luajava.LuaStateFactory
import com.luajava.LuaFunction
import dalvik.system.DexClassLoader
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
 */
object PluginManager {
    private const val PREFS_NAME = "plugin_settings"
    private const val PREF_ENABLED_PREFIX = "plugin_enabled_"
    
    var appContext: Context? = null
        private set
        
    // 内存中的已扫描插件列表
    val loadedPlugins = mutableStateListOf<LoadedPlugin>()
    
    // 当前活动的编辑器 ViewModel，用于让插件控制代码输入和读取
    var activeViewModel: EditorViewModel? = null
    
    // 当前活动的工程路径，供插件获取
    val currentProjectPath = mutableStateOf<String?>(null)
    
    // 动态注册的快捷功能动作列表
    val pluginQuickActions = mutableStateListOf<QuickAction>()
    private val quickActionsMap = mutableMapOf<String, QuickAction>()
    
    // 插件事件订阅字典
    private val eventListeners = mutableMapOf<String, MutableList<Any>>()

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
                    
                    // 保留已加载插件的运行状态（如果已存在）
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
        
        // 卸载已经被删除的插件
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
     * 加载所有已启用的插件
     */
    fun loadEnabledPlugins() {
        for (plugin in loadedPlugins) {
            if (plugin.enabled && plugin.luaState == null && plugin.dexPlugin == null) {
                try {
                    loadPluginInternal(plugin)
                } catch (e: Exception) {
                    android.util.Log.e("PluginManager", "加载插件失败: ${plugin.manifest.name}", e)
                }
            }
        }
    }

    /**
     * 单个插件加载逻辑
     */
    private fun loadPluginInternal(plugin: LoadedPlugin) {
        val context = appContext ?: return
        val manifest = plugin.manifest
        val type = manifest.type.lowercase(java.util.Locale.getDefault())
        
        if (type == "lua") {
            // 1. 初始化 LuaState 并加载基础库
            val L = LuaStateFactory.newLuaState()
            L.openLibs()
            
            // 2. 绑定全局 studio 桥梁对象
            val bridge = PluginBridgeImpl(manifest.id)
            L.pushJavaObject(bridge)
            L.setGlobal("studio")
            
            // 3. 执行主入口脚本
            val mainFile = File(plugin.directory, manifest.main)
            if (mainFile.exists()) {
                val ok = L.LloadFile(mainFile.absolutePath)
                if (ok == 0) {
                    L.getGlobal("debug")
                    L.getField(-1, "traceback")
                    L.remove(-2)
                    L.insert(-2)
                    val runOk = L.pcall(0, 0, -2)
                    if (runOk != 0) {
                        val error = L.toString(-1)
                        android.util.Log.e("PluginManager", "执行 Lua 插件脚本出错: $error")
                    }
                } else {
                    val error = L.toString(-1)
                    android.util.Log.e("PluginManager", "编译 Lua 插件失败: $error")
                }
            }
            plugin.luaState = L
        } else if (type == "dex" || type == "apk") {
            // 1. 拷贝 DEX/APK 文件到安全缓存区，因为 Android 系统在部分版本上不允许直接加载外部非应用私有目录的 dex
            val pluginFile = File(plugin.directory, manifest.main)
            if (pluginFile.exists()) {
                val cachePluginFile = File(context.codeCacheDir, "${manifest.id}_${manifest.version}.dex")
                if (!cachePluginFile.exists() || cachePluginFile.length() != pluginFile.length()) {
                    pluginFile.copyTo(cachePluginFile, overwrite = true)
                }
                
                // 2. 动态加载类并调用 onLoad 入口
                val classLoader = DexClassLoader(
                    cachePluginFile.absolutePath,
                    context.codeCacheDir.absolutePath,
                    null,
                    context.classLoader
                )
                
                val mainClass = manifest.mainClass
                if (mainClass != null) {
                    val clazz = classLoader.loadClass(mainClass)
                    val instance = clazz.getDeclaredConstructor().newInstance() as? IPlugin
                    if (instance != null) {
                        val bridge = PluginBridgeImpl(manifest.id)
                        instance.onLoad(context, bridge)
                        plugin.dexPlugin = instance
                    }
                } else {
                    android.util.Log.e("PluginManager", "插件未指定 mainClass 入口类: ${manifest.id}")
                }
            }
        }
    }

    /**
     * 单个插件卸载逻辑
     */
    private fun unloadPluginInternal(plugin: LoadedPlugin) {
        val pluginId = plugin.manifest.id
        
        // 1. 回调并释放 DEX 插件
        plugin.dexPlugin?.let {
            try {
                it.onUnload()
            } catch (e: Exception) {
                android.util.Log.e("PluginManager", "卸载 DEX 插件出错: $pluginId", e)
            }
            plugin.dexPlugin = null
        }
        
        // 2. 关闭 LuaState
        plugin.luaState?.let { L ->
            try {
                L.close()
            } catch (e: Exception) {
                android.util.Log.e("PluginManager", "关闭 LuaState 虚拟器出错: $pluginId", e)
            }
            plugin.luaState = null
        }
        
        // 3. 移除该插件动态添加的快捷工具栏动作
        val keysToRemove = quickActionsMap.keys.filter { it.startsWith("${pluginId}_") }
        keysToRemove.forEach { key ->
            quickActionsMap.remove(key)
        }
        updateQuickActions()
        
        // 4. 清理该插件注册的所有事件监听
        eventListeners.values.forEach { list ->
            // 如果是 DEX 加载，我们可以通过反射/类加载器区分，但在 Kotlin 简单的做法是过滤包含包名的监听器或清空
            // 为了安全，在卸载时暂时只允许清理 Lua 监听器以及与特定插件相关的实例
            val iterator = list.iterator()
            while (iterator.hasNext()) {
                val listener = iterator.next()
                if (listener is LuaFunction<*>) {
                    // 对于 Lua 虚拟机已关闭的情况，对应的 LuaFunction 也失效，应当清除
                    iterator.remove()
                } else {
                    // 如果是 DEX 插件的实例，我们根据它的 ClassLoader 判断是否属于该插件的 ClassLoader
                    val classLoader = listener.javaClass.classLoader
                    if (classLoader is DexClassLoader && classLoader.toString().contains(pluginId)) {
                        iterator.remove()
                    }
                }
            }
        }
    }

    /**
     * 启用/停用插件
     */
    fun togglePlugin(context: Context, pluginId: String, enabled: Boolean) {
        val index = loadedPlugins.indexOfFirst { it.manifest.id == pluginId }
        if (index != -1) {
            val plugin = loadedPlugins[index]
            plugin.enabled = enabled
            
            // 写入 Preferences 固化状态
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
            
            // 触发列表状态刷新 (使用 copy 创建新引用确保 Compose 可检测变更)
            loadedPlugins[index] = plugin.copy()
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
            
            // 删除目录
            val success = plugin.directory.deleteRecursively()
            
            // 从 Preferences 移除状态
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
            
            // 解压到缓存
            com.luaforge.studio.lxclua.utils.FileUtil.extractZip(zipFile, tempDir)
            
            // 查找 manifest.json 清单文件 (可能包含一层文件夹包裹)
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
            
            // 目标存放路径
            val pluginsDir = getPluginsDir(context)
            val destDir = File(pluginsDir, manifest.id)
            
            // 卸载旧版本插件并清理旧偏好设置
            val oldPlugin = loadedPlugins.find { it.manifest.id == manifest.id }
            if (oldPlugin != null) {
                unloadPluginInternal(oldPlugin)
                loadedPlugins.remove(oldPlugin)
            }
            getPrefs(context).edit()
                .remove(PREF_ENABLED_PREFIX + manifest.id)
                .apply()
            
            if (destDir.exists()) {
                destDir.deleteRecursively()
            }
            
            // 拷贝至 plugins 文件夹
            val sourceDir = manifestFile.parentFile ?: tempDir
            sourceDir.copyRecursively(destDir, overwrite = true)
            tempDir.deleteRecursively()
            
            // 重新扫描并动态加载
            scanPlugins(context)
            val loaded = loadedPlugins.find { it.manifest.id == manifest.id }
            if (loaded != null && loaded.enabled) {
                loadPluginInternal(loaded)
            }
            
            return Result.success(manifest)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * 发送 IDE 事件通知到所有的插件监听器
     */
    fun notifyEvent(eventName: String, vararg args: Any?) {
        val list = eventListeners[eventName] ?: return
        // 复制一份，防止在迭代时有监听器反注册导致 ConcurrentModificationException
        val listCopy = ArrayList(list)
        for (listener in listCopy) {
            try {
                if (listener is IPluginEventListener) {
                    listener.onEvent(*args)
                } else if (listener is LuaFunction<*>) {
                    listener.call(args)
                }
            } catch (e: Exception) {
                android.util.Log.e("PluginManager", "通知事件 $eventName 到监听器 $listener 发生错误", e)
            }
        }
    }

    /**
     * 更新渲染快捷动作的 Compose 列表
     */
    private fun updateQuickActions() {
        pluginQuickActions.clear()
        pluginQuickActions.addAll(quickActionsMap.values)
    }

    /**
     * 桥梁接口具体实现
     */
    private class PluginBridgeImpl(val pluginId: String) : IPluginBridge {
        private val handler = Handler(Looper.getMainLooper())

        override fun toast(message: String) {
            handler.post {
                appContext?.let {
                    android.widget.Toast.makeText(it, message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun log(tag: String, message: String) {
            com.luaforge.studio.lxclua.utils.LogCatcher.d(tag, message)
        }

        override fun getVersion(): String {
            return appContext?.packageManager?.getPackageInfo(appContext!!.packageName, 0)?.versionName ?: "1.0.0"
        }

        override fun getActiveFile(): String? {
            return activeViewModel?.activeFileState?.file?.absolutePath
        }

        override fun getActiveText(): String? {
            return activeViewModel?.activeFileState?.content
        }

        override fun setActiveText(text: String) {
            handler.post {
                activeViewModel?.let { vm ->
                    vm.activeFileState?.onContentChanged(text)
                    vm.getActiveEditor()?.setText(text)
                }
            }
        }

        override fun insertText(text: String) {
            handler.post {
                activeViewModel?.insertSymbolToCorrectEditor(text)
            }
        }

        override fun getProjectPath(): String? {
            return currentProjectPath.value
        }

        override fun addQuickAction(key: String, label: String, onClick: Runnable) {
            val globalKey = "${pluginId}_$key"
            val action = QuickAction(
                labelResId = 0,
                labelString = label,
                key = globalKey,
                onClick = {
                    onClick.run()
                }
            )
            handler.post {
                quickActionsMap[globalKey] = action
                updateQuickActions()
            }
        }

        override fun removeQuickAction(key: String) {
            val globalKey = "${pluginId}_$key"
            handler.post {
                quickActionsMap.remove(globalKey)
                updateQuickActions()
            }
        }

        override fun registerEventListener(eventName: String, listener: Any) {
            val list = eventListeners.getOrPut(eventName) { mutableListOf() }
            if (listener !in list) {
                list.add(listener)
            }
        }
    }
}
