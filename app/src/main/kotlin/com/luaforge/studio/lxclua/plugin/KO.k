package com.luaforge.studio.lxclua.plugin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.google.gson.Gson
import com.luaforge.studio.lxclua.ui.editor.QuickAction
import com.luaforge.studio.lxclua.ui.editor.viewmodel.EditorViewModel
import com.luajava.LuaState
import com.luajava.LuaStateFactory
import com.luajava.LuaFunction
import com.luajava.LuaTable
import dalvik.system.DexClassLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.Executors

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
    
    var currentActivity: android.app.Activity? = null
        
    // 内存中的已扫描插件列表
    val loadedPlugins = mutableStateListOf<LoadedPlugin>()
    
    // 当前活动的编辑器 ViewModel，用于让插件控制代码输入和读取
    var activeViewModel: EditorViewModel? = null
    
    // 当前活动的工程路径，供插件获取
    val currentProjectPath = mutableStateOf<String?>(null)
    
    // 动态注册的快捷功能动作列表
    val pluginQuickActions = mutableStateListOf<QuickAction>()
    private val quickActionsMap = mutableMapOf<String, QuickAction>()
    
    // 插件菜单项
    sealed class PluginMenuItem {
        abstract val key: String
        abstract val pluginId: String
        
        data class Item(
            override val key: String,
            override val pluginId: String,
            val label: String,
            val onClick: Runnable
        ) : PluginMenuItem()
        
        data class Divider(
            override val key: String,
            override val pluginId: String
        ) : PluginMenuItem()
    }
    
    val pluginMenuItems = mutableStateListOf<PluginMenuItem>()
    
    // ==================== 文件树菜单项 ====================
    
    /**
     * 文件树上下文菜单项
     */
    sealed class FileTreeMenuItem {
        abstract val key: String
        abstract val pluginId: String
        abstract val filter: String?  // 过滤器：扩展名如 ".lua"，或 "directory"，或 "file"
        
        data class Item(
            override val key: String,
            override val pluginId: String,
            val label: String,
            val iconName: String?,  // 图标名称
            override val filter: String?,
            val onClick: (String, Boolean) -> Unit  // (filePath, isDirectory)
        ) : FileTreeMenuItem()
        
        data class Divider(
            override val key: String,
            override val pluginId: String,
            override val filter: String?
        ) : FileTreeMenuItem()
    }
    
    /**
     * 全局的文件树菜单项列表（由所有插件注册）
     */
    val fileTreeMenuItems = mutableStateListOf<FileTreeMenuItem>()
    
    // 插件事件订阅字典
    private val eventListeners = mutableMapOf<String, MutableList<Any>>()
    
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
            
            // 创建新的插件对象并替换，确保 Compose 检测到变化
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
        val listCopy = ArrayList(list)
        for (listener in listCopy) {
            try {
                when (listener) {
                    is IPluginEventListener -> {
                        listener.onEvent(*args)
                    }
                    is LuaFunction<*> -> {
                        listener.call(*args)
                    }
                    is LuaTable<*, *> -> {
                        val onEvent = listener["onEvent"]
                        if (onEvent is LuaFunction<*>) {
                            onEvent.call(*args)
                        }
                    }
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
        private val backgroundExecutor = Executors.newCachedThreadPool()
        private val httpClient = OkHttpClient()
        
        private val pluginDataDir: File by lazy {
            File(appContext?.filesDir, "plugins_data/$pluginId").apply { mkdirs() }
        }
        
        private val pluginPrefs by lazy {
            appContext?.getSharedPreferences("plugin_prefs_$pluginId", Context.MODE_PRIVATE)!!
        }

        override fun toast(message: String) {
            handler.post {
                appContext?.let {
                    android.widget.Toast.makeText(it, message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        override fun toastLong(message: String) {
            handler.post {
                appContext?.let {
                    android.widget.Toast.makeText(it, message, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }

        override fun log(tag: String, message: String) {
            com.luaforge.studio.lxclua.utils.LogCatcher.d(tag, message)
        }

        override fun getVersion(): String {
            return appContext?.packageManager?.getPackageInfo(appContext!!.packageName, 0)?.versionName ?: "1.0.0"
        }
        
        override fun getDataDir(): String {
            return pluginDataDir.absolutePath
        }
        
        override fun getContext(): Context {
            return appContext!!
        }
        
        // ==================== UI 对话框 ====================
        
        override fun showMessage(title: String, message: String) {
            handler.post {
                currentDialog.value = DialogState.Message(
                    title = title,
                    message = message,
                    onDismiss = { currentDialog.value = null }
                )
            }
        }
        
        override fun showConfirm(title: String, message: String, onConfirm: Runnable, onCancel: Runnable?) {
            handler.post {
                currentDialog.value = DialogState.Confirm(
                    title = title,
                    message = message,
                    onConfirm = {
                        currentDialog.value = null
                        onConfirm.run()
                    },
                    onCancel = onCancel?.let { 
                        { 
                            currentDialog.value = null
                            it.run() 
                        } 
                    },
                    onDismiss = { currentDialog.value = null }
                )
            }
        }
        
        override fun showInputDialog(title: String, hint: String, defaultValue: String, onInput: OnInputCallback) {
            handler.post {
                currentDialog.value = DialogState.Input(
                    title = title,
                    hint = hint,
                    defaultValue = defaultValue,
                    onInput = { text ->
                        currentDialog.value = null
                        onInput.onInput(text)
                    },
                    onDismiss = { currentDialog.value = null }
                )
            }
        }
        
        override fun showSingleChoiceDialog(title: String, items: Array<String>, selectedIndex: Int, onSelect: OnSelectCallback) {
            handler.post {
                currentDialog.value = DialogState.SingleChoice(
                    title = title,
                    items = items,
                    selectedIndex = selectedIndex,
                    onSelect = { index ->
                        currentDialog.value = null
                        onSelect.onSelect(index)
                    },
                    onDismiss = { currentDialog.value = null }
                )
            }
        }
        
        override fun showMultiChoiceDialog(title: String, items: Array<String>, checkedItems: BooleanArray, onConfirm: OnMultiSelectCallback) {
            handler.post {
                currentDialog.value = DialogState.MultiChoice(
                    title = title,
                    items = items,
                    checkedItems = checkedItems.copyOf(),
                    onConfirm = { result ->
                        currentDialog.value = null
                        onConfirm.onConfirm(result)
                    },
                    onDismiss = { currentDialog.value = null }
                )
            }
        }
        
        override fun showProgressDialog(title: String, message: String): ProgressDialogHandle {
            val handle = ProgressDialogHandleImpl()
            handler.post {
                val dialog = AlertDialog.Builder(appContext!!)
                    .setTitle(title)
                    .setMessage(message)
                    .setCancelable(false)
                    .create()
                handle.dialog = dialog
                dialog.show()
            }
            return handle
        }
        
        private class ProgressDialogHandleImpl : ProgressDialogHandle {
            var dialog: AlertDialog? = null
            
            override fun setProgress(progress: Int) {
                // 标准 AlertDialog 不支持进度条，这里只是更新消息
            }
            
            override fun setMessage(message: String) {
                dialog?.setMessage(message)
            }
            
            override fun dismiss() {
                dialog?.dismiss()
            }
            
            override fun isShowing(): Boolean {
                return dialog?.isShowing ?: false
            }
        }

        // ==================== 编辑器操作 ====================
        
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
        
        override fun getSelectedText(): String? {
            val editor = activeViewModel?.getActiveEditor() ?: return null
            val cursor = editor.cursor
            val leftIndex = cursor.left
            val rightIndex = cursor.right
            return if (leftIndex != rightIndex) {
                editor.text.substring(leftIndex, rightIndex)
            } else {
                ""
            }
        }
        
        override fun setSelection(start: Int, end: Int) {
            handler.post {
                val editor = activeViewModel?.getActiveEditor() ?: return@post
                val startPos = editor.text.indexer.getCharPosition(start)
                val endPos = editor.text.indexer.getCharPosition(end)
                editor.setSelectionRegion(
                    startPos.line, startPos.column,
                    endPos.line, endPos.column
                )
            }
        }
        
        override fun getCursorPosition(): IntArray? {
            val editor = activeViewModel?.getActiveEditor() ?: return null
            val cursor = editor.cursor
            return intArrayOf(cursor.leftLine, cursor.leftColumn)
        }
        
        override fun gotoLine(line: Int) {
            handler.post {
                val editor = activeViewModel?.getActiveEditor() ?: return@post
                editor.jumpToLine(line.coerceAtLeast(0))
            }
        }
        
        override fun gotoPosition(line: Int, column: Int) {
            handler.post {
                val editor = activeViewModel?.getActiveEditor() ?: return@post
                editor.setSelectionRegion(line, column, line, column)
            }
        }
        
        override fun getOpenFiles(): Array<String>? {
            return activeViewModel?.openFiles?.map { it.file.absolutePath }?.toTypedArray()
        }
        
        override fun closeActiveFile() {
            handler.post {
                val index = activeViewModel?.activeFileIndex ?: -1
                if (index >= 0) {
                    activeViewModel?.closeFile(index)
                }
            }
        }
        
        override fun openFile(filePath: String) {
            handler.post {
                val file = File(filePath)
                if (file.exists()) {
                    activeViewModel?.openFile(file)
                }
            }
        }
        
        override fun saveActiveFile() {
            kotlinx.coroutines.GlobalScope.launch {
                activeViewModel?.saveCurrentFileSilently()
            }
        }
        
        override fun saveAllFiles() {
            kotlinx.coroutines.GlobalScope.launch {
                activeViewModel?.saveAllFilesSilently()
            }
        }
        
        override fun undo() {
            handler.post {
                activeViewModel?.getActiveEditor()?.undo()
            }
        }
        
        override fun redo() {
            handler.post {
                activeViewModel?.getActiveEditor()?.redo()
            }
        }
        
        override fun findText(query: String, caseSensitive: Boolean, regex: Boolean) {
            handler.post {
                val editor = activeViewModel?.getActiveEditor() ?: return@post
                val options = io.github.rosemoe.sora.widget.EditorSearcher.SearchOptions(!caseSensitive, regex)
                editor.searcher.search(query, options)
            }
        }
        
        override fun replaceText(query: String, replacement: String, replaceAll: Boolean) {
            handler.post {
                val editor = activeViewModel?.getActiveEditor() ?: return@post
                if (replaceAll) {
                    val content = editor.text.toString()
                    val newContent = content.replace(query, replacement)
                    editor.setText(newContent)
                } else {
                    val cursor = editor.cursor
                    val selectedText = editor.text.substring(cursor.left, cursor.right)
                    if (selectedText == query) {
                        editor.insertText(replacement, 0)
                    }
                }
            }
        }

        // ==================== 项目操作 ====================
        
        override fun getProjectPath(): String? {
            return currentProjectPath.value
        }
        
        override fun readFile(relativePath: String): String? {
            val projectPath = currentProjectPath.value ?: return null
            val file = File(projectPath, relativePath)
            return if (file.exists() && file.isFile) {
                try {
                    file.readText()
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
        
        override fun writeFile(relativePath: String, content: String): Boolean {
            val projectPath = currentProjectPath.value ?: return false
            val file = File(projectPath, relativePath)
            return try {
                file.parentFile?.mkdirs()
                file.writeText(content)
                true
            } catch (e: Exception) {
                false
            }
        }
        
        override fun listFiles(relativePath: String): Array<String>? {
            val projectPath = currentProjectPath.value ?: return null
            val dir = File(projectPath, relativePath)
            return if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.map { it.name }?.toTypedArray()
            } else {
                null
            }
        }
        
        override fun createFile(relativePath: String, content: String): Boolean {
            val projectPath = currentProjectPath.value ?: return false
            val file = File(projectPath, relativePath)
            return try {
                file.parentFile?.mkdirs()
                file.writeText(content)
                true
            } catch (e: Exception) {
                false
            }
        }
        
        override fun deleteFile(relativePath: String): Boolean {
            val projectPath = currentProjectPath.value ?: return false
            val file = File(projectPath, relativePath)
            return try {
                file.delete()
            } catch (e: Exception) {
                false
            }
        }
        
        override fun fileExists(relativePath: String): Boolean {
            val projectPath = currentProjectPath.value ?: return false
            return File(projectPath, relativePath).exists()
        }
        
        override fun createDirectory(relativePath: String): Boolean {
            val projectPath = currentProjectPath.value ?: return false
            val dir = File(projectPath, relativePath)
            return try {
                dir.mkdirs()
                true
            } catch (e: Exception) {
                false
            }
        }

        // ==================== 快捷操作 ====================

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
        
        override fun clearQuickActions() {
            handler.post {
                val keysToRemove = quickActionsMap.keys.filter { it.startsWith("${pluginId}_") }
                keysToRemove.forEach { quickActionsMap.remove(it) }
                updateQuickActions()
            }
        }
        
        // ==================== 菜单操作 ====================
        
        override fun addMenuItem(key: String, label: String, onClick: Runnable) {
            handler.post {
                val globalKey = "${pluginId}_$key"
                val existingIndex = pluginMenuItems.indexOfFirst { it.key == globalKey }
                val newItem = PluginMenuItem.Item(globalKey, pluginId, label, onClick)
                
                if (existingIndex >= 0) {
                    pluginMenuItems[existingIndex] = newItem
                } else {
                    pluginMenuItems.add(newItem)
                }
            }
        }
        
        override fun addMenuDivider(key: String) {
            handler.post {
                val globalKey = "${pluginId}_$key"
                val existingIndex = pluginMenuItems.indexOfFirst { it.key == globalKey }
                val newDivider = PluginMenuItem.Divider(globalKey, pluginId)
                
                if (existingIndex >= 0) {
                    pluginMenuItems[existingIndex] = newDivider
                } else {
                    pluginMenuItems.add(newDivider)
                }
            }
        }
        
        override fun removeMenuItem(key: String) {
            handler.post {
                val globalKey = "${pluginId}_$key"
                pluginMenuItems.removeAll { it.key == globalKey }
            }
        }
        
        override fun clearMenuItems() {
            handler.post {
                pluginMenuItems.removeAll { it.pluginId == pluginId }
            }
        }
        
        // ==================== 文件树操作 ====================
        
        override fun addFileTreeMenuItem(key: String, label: String, filter: String?, onClick: FileTreeItemCallback) {
            addFileTreeMenuItem(key, label, null, filter, onClick)
        }
        
        override fun addFileTreeMenuItem(key: String, label: String, iconName: String?, filter: String?, onClick: FileTreeItemCallback) {
            handler.post {
                val globalKey = "${pluginId}_$key"
                val existingIndex = fileTreeMenuItems.indexOfFirst { it.key == globalKey }
                val newItem = FileTreeMenuItem.Item(
                    key = globalKey,
                    pluginId = pluginId,
                    label = label,
                    iconName = iconName,
                    filter = filter,
                    onClick = onClick::onItemClick
                )
                
                if (existingIndex >= 0) {
                    fileTreeMenuItems[existingIndex] = newItem
                } else {
                    fileTreeMenuItems.add(newItem)
                }
            }
        }
        
        override fun addFileTreeMenuDivider(key: String, filter: String?) {
            handler.post {
                val globalKey = "${pluginId}_$key"
                val existingIndex = fileTreeMenuItems.indexOfFirst { it.key == globalKey }
                val newDivider = FileTreeMenuItem.Divider(
                    key = globalKey,
                    pluginId = pluginId,
                    filter = filter
                )
                
                if (existingIndex >= 0) {
                    fileTreeMenuItems[existingIndex] = newDivider
                } else {
                    fileTreeMenuItems.add(newDivider)
                }
            }
        }
        
        override fun removeFileTreeMenuItem(key: String) {
            handler.post {
                val globalKey = "${pluginId}_$key"
                fileTreeMenuItems.removeAll { it.key == globalKey }
            }
        }
        
        override fun clearFileTreeMenuItems() {
            handler.post {
                fileTreeMenuItems.removeAll { it.pluginId == pluginId }
            }
        }
        
        override fun getFileInfo(filePath: String): FileInfo? {
            val file = File(filePath)
            if (!file.exists()) {
                return null
            }
            
            val name = file.name
            val extension = name.substringAfterLast('.', "").lowercase()
            val nameWithoutExtension = name.substringBeforeLast('.', name)
            
            return FileInfo(
                path = file.absolutePath,
                name = name,
                extension = extension,
                isDirectory = file.isDirectory,
                size = if (file.isDirectory) 0 else file.length(),
                lastModified = file.lastModified(),
                parentPath = file.parent ?: "",
                nameWithoutExtension = nameWithoutExtension
            )
        }
        
        // ==================== 剪贴板 ====================
        
        override fun copyToClipboard(text: String) {
            handler.post {
                val clipboard = appContext?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = ClipData.newPlainText("text", text)
                clipboard?.setPrimaryClip(clip)
            }
        }
        
        override fun getClipboardText(): String? {
            val clipboard = appContext?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboard?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                return clip.getItemAt(0).text?.toString()
            }
            return null
        }
        
        // ==================== 配置存储 ====================
        
        override fun getConfig(key: String, defaultValue: String): String {
            return pluginPrefs.getString("${pluginId}_$key", defaultValue) ?: defaultValue
        }
        
        override fun setConfig(key: String, value: String) {
            pluginPrefs.edit().putString("${pluginId}_$key", value).apply()
        }
        
        override fun getConfigInt(key: String, defaultValue: Int): Int {
            return pluginPrefs.getInt("${pluginId}_$key", defaultValue)
        }
        
        override fun setConfigInt(key: String, value: Int) {
            pluginPrefs.edit().putInt("${pluginId}_$key", value).apply()
        }
        
        override fun getConfigBoolean(key: String, defaultValue: Boolean): Boolean {
            return pluginPrefs.getBoolean("${pluginId}_$key", defaultValue)
        }
        
        override fun setConfigBoolean(key: String, value: Boolean) {
            pluginPrefs.edit().putBoolean("${pluginId}_$key", value).apply()
        }
        
        override fun removeConfig(key: String) {
            pluginPrefs.edit().remove("${pluginId}_$key").apply()
        }

        // ==================== 事件系统 ====================

        override fun registerEventListener(eventName: String, listener: Any) {
            val list = eventListeners.getOrPut(eventName) { mutableListOf() }
            if (listener !in list) {
                list.add(listener)
            }
        }
        
        override fun unregisterEventListener(eventName: String, listener: Any) {
            eventListeners[eventName]?.remove(listener)
        }
        
        // ==================== 线程工具 ====================
        
        override fun runOnUIThread(runnable: Runnable) {
            handler.post(runnable)
        }
        
        override fun runOnBackgroundThread(runnable: Runnable) {
            backgroundExecutor.execute(runnable)
        }
        
        // ==================== 网络请求 ====================
        
        override fun httpGet(url: String, headers: Map<String, String>?, callback: HttpCallback) {
            backgroundExecutor.execute {
                try {
                    val requestBuilder = Request.Builder().url(url)
                    headers?.forEach { (key, value) -> requestBuilder.addHeader(key, value) }
                    val response = httpClient.newCall(requestBuilder.build()).execute()
                    val body = response.body?.string()
                    handler.post { callback.onResult(response.isSuccessful, body, null) }
                } catch (e: Exception) {
                    handler.post { callback.onResult(false, null, e.message) }
                }
            }
        }
        
        override fun httpPost(url: String, body: String, headers: Map<String, String>?, callback: HttpCallback) {
            backgroundExecutor.execute {
                try {
                    val requestBody = body.toRequestBody()
                    val requestBuilder = Request.Builder().url(url).post(requestBody)
                    headers?.forEach { (key, value) -> requestBuilder.addHeader(key, value) }
                    val response = httpClient.newCall(requestBuilder.build()).execute()
                    val responseBody = response.body?.string()
                    handler.post { callback.onResult(response.isSuccessful, responseBody, null) }
                } catch (e: Exception) {
                    handler.post { callback.onResult(false, null, e.message) }
                }
            }
        }
        
        // ==================== Java 反射调用 ====================
        
        private var pluginClassLoader: DexClassLoader? = null
        
        override fun loadClass(className: String): Class<*>? {
            return try {
                pluginClassLoader?.loadClass(className) ?: Class.forName(className)
            } catch (e: Exception) {
                null
            }
        }
        
        override fun newInstance(className: String, args: Array<Any?>?): Any? {
            return try {
                val clazz = loadClass(className) ?: return null
                val argArray = convertLuaTableToArray(args) ?: emptyArray()
                val constructor = findBestConstructor(clazz, argArray)
                constructor.isAccessible = true
                val convertedArgs = convertArgs(constructor.parameterTypes, argArray)
                constructor.newInstance(*convertedArgs)
            } catch (e: Exception) {
                android.util.Log.e("PluginBridge", "newInstance failed: $className", e)
                null
            }
        }
        
        override fun callStaticMethod(className: String, methodName: String, args: Array<Any?>?): Any? {
            return try {
                val clazz = loadClass(className) ?: return null
                val argArray = convertLuaTableToArray(args) ?: emptyArray()
                val method = findBestMethod(clazz, methodName, argArray)
                method.isAccessible = true
                val convertedArgs = convertArgs(method.parameterTypes, argArray)
                method.invoke(null, *convertedArgs)
            } catch (e: Exception) {
                android.util.Log.e("PluginBridge", "callStaticMethod failed: $className.$methodName", e)
                null
            }
        }
        
        override fun callMethod(obj: Any, methodName: String, args: Array<Any?>?): Any? {
            return try {
                // 调试日志
                android.util.Log.d("PluginBridge", "callMethod: $methodName, args=${args?.toList()}, argsType=${args?.map { it?.javaClass?.simpleName }}")
                val argArray = convertLuaTableToArray(args) ?: emptyArray()
                android.util.Log.d("PluginBridge", "converted args: ${argArray.toList()}, types: ${argArray.map { it?.javaClass?.simpleName }}")
                val method = findBestMethod(obj.javaClass, methodName, argArray)
                method.isAccessible = true
                val convertedArgs = convertArgs(method.parameterTypes, argArray)
                method.invoke(obj, *convertedArgs)
            } catch (e: Exception) {
                android.util.Log.e("PluginBridge", "callMethod failed: $methodName", e)
                null
            }
        }
        
        private fun convertLuaTableToArray(args: Array<Any?>?): Array<Any?>? {
            if (args == null) return null
            
            // 如果只有一个参数且是 LuaTable，将其展开为数组
            if (args.size == 1 && args[0] is LuaTable<*, *>) {
                val table = args[0] as LuaTable<*, *>
                val list = mutableListOf<Any?>()
                val len = table.length()
                for (i in 1..len) {
                    list.add(table[i])
                }
                return list.toTypedArray()
            }
            
            // 否则检查每个元素是否需要转换
            return args.map { arg ->
                when (arg) {
                    is LuaTable<*, *> -> {
                        val list = mutableListOf<Any?>()
                        val len = arg.length()
                        for (i in 1..len) {
                            list.add(arg[i])
                        }
                        list.toTypedArray()
                    }
                    else -> arg
                }
            }.toTypedArray()
        }
        
        private fun convertArgs(paramTypes: Array<Class<*>>, args: Array<Any?>): Array<Any?> {
            return Array(args.size) { i ->
                val arg = args[i]
                val paramType = paramTypes[i]
                convertValue(arg, paramType)
            }
        }
        
        private fun convertValue(value: Any?, targetType: Class<*>): Any? {
            if (value == null) return null
            if (targetType.isInstance(value)) return value
            
            return when (targetType) {
                Int::class.java, Int::class.javaPrimitiveType -> (value as? Number)?.toInt()
                Long::class.java, Long::class.javaPrimitiveType -> (value as? Number)?.toLong()
                Float::class.java, Float::class.javaPrimitiveType -> (value as? Number)?.toFloat()
                Double::class.java, Double::class.javaPrimitiveType -> (value as? Number)?.toDouble()
                Short::class.java, Short::class.javaPrimitiveType -> (value as? Number)?.toShort()
                Byte::class.java, Byte::class.javaPrimitiveType -> (value as? Number)?.toByte()
                Boolean::class.java, Boolean::class.javaPrimitiveType -> value as? Boolean
                else -> value
            }
        }
        
        private fun findBestConstructor(clazz: Class<*>, args: Array<Any?>): java.lang.reflect.Constructor<*> {
            val constructors = clazz.declaredConstructors
            return constructors.firstOrNull { isCompatible(it.parameterTypes, args) }
                ?: throw NoSuchMethodException("No compatible constructor found for ${clazz.name}")
        }
        
        private fun findBestMethod(clazz: Class<*>, methodName: String, args: Array<Any?>): java.lang.reflect.Method {
            val methods = clazz.methods.filter { it.name == methodName }
            return methods.firstOrNull { isCompatible(it.parameterTypes, args) }
                ?: throw NoSuchMethodException("No compatible method $methodName found for ${clazz.name}")
        }
        
        private fun isCompatible(paramTypes: Array<Class<*>>, args: Array<Any?>): Boolean {
            if (paramTypes.size != args.size) return false
            for (i in paramTypes.indices) {
                val paramType = paramTypes[i]
                val arg = args[i]
                if (arg == null) {
                    if (paramType.isPrimitive) return false
                } else {
                    val argType = arg.javaClass
                    if (!isTypeCompatible(paramType, argType)) return false
                }
            }
            return true
        }
        
        private fun isTypeCompatible(paramType: Class<*>, argType: Class<*>): Boolean {
            if (paramType == argType) return true
            if (paramType.isPrimitive) {
                val argTypeName = argType.name
                val result = when (paramType) {
                    Int::class.javaPrimitiveType -> 
                        argTypeName == "java.lang.Integer" || 
                        argTypeName == "java.lang.Long" || 
                        argTypeName == "java.lang.Double" || 
                        argTypeName == "java.lang.Float"
                    Long::class.javaPrimitiveType -> 
                        argTypeName == "java.lang.Long" || 
                        argTypeName == "java.lang.Integer"
                    Float::class.javaPrimitiveType -> 
                        argTypeName == "java.lang.Float" || 
                        argTypeName == "java.lang.Double" || 
                        argTypeName == "java.lang.Integer" || 
                        argTypeName == "java.lang.Long"
                    Double::class.javaPrimitiveType -> 
                        argTypeName == "java.lang.Double" || 
                        argTypeName == "java.lang.Float" || 
                        argTypeName == "java.lang.Integer" || 
                        argTypeName == "java.lang.Long"
                    Boolean::class.javaPrimitiveType -> 
                        argTypeName == "java.lang.Boolean"
                    Short::class.javaPrimitiveType -> 
                        argTypeName == "java.lang.Short" || 
                        argTypeName == "java.lang.Integer" || 
                        argTypeName == "java.lang.Long"
                    Byte::class.javaPrimitiveType -> 
                        argTypeName == "java.lang.Byte" || 
                        argTypeName == "java.lang.Integer" || 
                        argTypeName == "java.lang.Long"
                    Char::class.javaPrimitiveType -> 
                        argTypeName == "java.lang.Character" || 
                        argTypeName == "java.lang.Integer"
                    else -> false
                }
                if (!result) {
                    android.util.Log.d("PluginBridge", "isTypeCompatible failed: paramType=${paramType.name} (primitive=${paramType.isPrimitive}), argType=${argTypeName} (primitive=${argType.isPrimitive})")
                }
                return result
            }
            return paramType.isAssignableFrom(argType)
        }
        
        override fun getStaticField(className: String, fieldName: String): Any? {
            return try {
                val clazz = loadClass(className) ?: return null
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                field.get(null)
            } catch (e: Exception) {
                null
            }
        }
        
        override fun setStaticField(className: String, fieldName: String, value: Any?) {
            try {
                val clazz = loadClass(className) ?: return
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                field.set(null, value)
            } catch (e: Exception) {
                android.util.Log.e("PluginBridge", "setStaticField failed: $className.$fieldName", e)
            }
        }
        
        override fun getField(obj: Any, fieldName: String): Any? {
            return try {
                val field = obj.javaClass.getDeclaredField(fieldName)
                field.isAccessible = true
                field.get(obj)
            } catch (e: Exception) {
                null
            }
        }
        
        override fun setField(obj: Any, fieldName: String, value: Any?) {
            try {
                val field = obj.javaClass.getDeclaredField(fieldName)
                field.isAccessible = true
                field.set(obj, value)
            } catch (e: Exception) {
                android.util.Log.e("PluginBridge", "setField failed: $fieldName", e)
            }
        }
        
        // ==================== 热修复/类加载 ====================
        
        override fun loadDex(dexPath: String): Boolean {
            return try {
                val dexFile = File(dexPath)
                if (!dexFile.exists()) return false
                
                val optDir = File(pluginDataDir, "dex_opt")
                optDir.mkdirs()
                
                val parentLoader = appContext?.classLoader ?: return false
                pluginClassLoader = DexClassLoader(dexPath, optDir.absolutePath, null, parentLoader)
                true
            } catch (e: Exception) {
                android.util.Log.e("PluginBridge", "loadDex failed", e)
                false
            }
        }
        
        override fun loadDexFromUrl(url: String, callback: DexLoadCallback) {
            backgroundExecutor.execute {
                try {
                    val dexFile = File(pluginDataDir, "hotfix_${System.currentTimeMillis()}.dex")
                    httpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                        if (!response.isSuccessful) {
                            handler.post { callback.onLoad(false, "HTTP ${response.code}") }
                            return@execute
                        }
                        response.body?.byteStream()?.use { input ->
                            dexFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    
                    val success = loadDex(dexFile.absolutePath)
                    handler.post { callback.onLoad(success, if (success) null else "加载 DEX 失败") }
                } catch (e: Exception) {
                    handler.post { callback.onLoad(false, e.message) }
                }
            }
        }
        
        override fun getPluginClassLoader(): ClassLoader? {
            return pluginClassLoader ?: appContext?.classLoader
        }
        
        // ==================== 动态资源 ====================
        
        override fun loadResources(apkPath: String): PluginResources? {
            return try {
                val assetManager = android.content.res.AssetManager::class.java.newInstance()
                val addAssetPath = assetManager.javaClass.getDeclaredMethod("addAssetPath", String::class.java)
                addAssetPath.isAccessible = true
                addAssetPath.invoke(assetManager, apkPath)
                
                val displayMetrics = appContext?.resources?.displayMetrics
                val config = appContext?.resources?.configuration
                val resources = android.content.res.Resources(assetManager, displayMetrics, config)
                
                val pm = appContext?.packageManager
                val pkgInfo = pm?.getPackageArchiveInfo(apkPath, 0)
                
                PluginResources(assetManager, resources, pkgInfo?.packageName ?: "")
            } catch (e: Exception) {
                android.util.Log.e("PluginBridge", "loadResources failed", e)
                null
            }
        }
        
        override fun getResourceString(resources: PluginResources, resName: String): String? {
            return try {
                val resId = resources.resources.getIdentifier(resName, "string", resources.packageName)
                if (resId != 0) resources.resources.getString(resId) else null
            } catch (e: Exception) {
                null
            }
        }
        
        override fun getResourceDrawable(resources: PluginResources, resName: String): android.graphics.drawable.Drawable? {
            return try {
                val resId = resources.resources.getIdentifier(resName, "drawable", resources.packageName)
                if (resId != 0) resources.resources.getDrawable(resId, null) else null
            } catch (e: Exception) {
                null
            }
        }
        
        override fun getResourceColor(resources: PluginResources, resName: String): Int {
            return try {
                val resId = resources.resources.getIdentifier(resName, "color", resources.packageName)
                if (resId != 0) resources.resources.getColor(resId, null) else 0
            } catch (e: Exception) {
                0
            }
        }
        
        override fun getResourceId(resources: PluginResources, resName: String, resType: String): Int {
            return try {
                resources.resources.getIdentifier(resName, resType, resources.packageName)
            } catch (e: Exception) {
                0
            }
        }
        
        // ==================== 脚本执行 ====================
        
        override fun executeLua(script: String): Any? {
            return try {
                val luaState = loadedPlugins.find { it.manifest.id == pluginId }?.luaState ?: return null
                luaState.LdoString(script)
                if (luaState.getTop() > 0) {
                    val result = luaState.toJavaObject(-1)
                    luaState.pop(1)
                    result
                } else {
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("PluginBridge", "executeLua failed", e)
                null
            }
        }
        
        override fun executeLuaFile(scriptPath: String): Any? {
            return try {
                val file = File(scriptPath)
                if (!file.exists()) return null
                executeLua(file.readText())
            } catch (e: Exception) {
                null
            }
        }
        
        override fun compileLua(script: String): ByteArray? {
            return try {
                val luaState = loadedPlugins.find { it.manifest.id == pluginId }?.luaState ?: return null
                luaState.LdoString(script)
                val result = luaState.dump(-1)
                luaState.pop(1)
                result
            } catch (e: Exception) {
                null
            }
        }
        
        override fun executeLuaBytecode(bytecode: ByteArray): Any? {
            return try {
                val luaState = loadedPlugins.find { it.manifest.id == pluginId }?.luaState ?: return null
                val loadResult = luaState.LloadBuffer(bytecode, "bytecode")
                if (loadResult == 0) {
                    luaState.pcall(0, 1, 0)
                    if (luaState.getTop() > 0) {
                        val result = luaState.toJavaObject(-1)
                        luaState.pop(1)
                        result
                    } else {
                        null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
