package com.luaforge.studio.lxclua.plugin.bridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.luaforge.studio.lxclua.plugin.PluginManager
import com.luaforge.studio.lxclua.plugin.api.IPluginBridge
import com.luaforge.studio.lxclua.plugin.api.callbacks.*
import com.luaforge.studio.lxclua.plugin.data.FileInfo
import com.luaforge.studio.lxclua.plugin.state.EventManager
import com.luaforge.studio.lxclua.plugin.state.UIState
import com.luaforge.studio.lxclua.ui.editor.QuickAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.Executors

/**
 * 插件桥梁实现类
 * 
 * 实现了 IPluginBridge 接口，为插件提供访问宿主功能的能力
 */
class PluginBridgeImpl(val pluginId: String) : IPluginBridge {
    
    private val handler = Handler(Looper.getMainLooper())
    private val backgroundExecutor = Executors.newCachedThreadPool()
    private val httpClient = OkHttpClient()
    
    private val pluginDataDir: File by lazy {
        File(PluginManager.appContext?.filesDir, "plugins_data/$pluginId").apply { mkdirs() }
    }
    
    private val pluginPrefs by lazy {
        PluginManager.appContext?.getSharedPreferences("plugin_prefs_$pluginId", Context.MODE_PRIVATE)!!
    }
    
    // ==================== 基础功能 ====================
    
    override fun toast(message: String) {
        handler.post {
            PluginManager.appContext?.let {
                android.widget.Toast.makeText(it, message, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun toastLong(message: String) {
        handler.post {
            PluginManager.appContext?.let {
                android.widget.Toast.makeText(it, message, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun log(tag: String, message: String) {
        com.luaforge.studio.lxclua.utils.LogCatcher.d(tag, message)
    }
    
    override fun getVersion(): String {
        return PluginManager.appContext?.packageManager?.getPackageInfo(
            PluginManager.appContext!!.packageName, 0
        )?.versionName ?: "1.0.0"
    }
    
    override fun getDataDir(): String {
        return pluginDataDir.absolutePath
    }
    
    override fun getContext(): Context {
        return PluginManager.appContext!!
    }
    
    // ==================== 快捷操作 ====================
    
    override fun addQuickAction(key: String, label: String, onClick: Runnable) {
        val globalKey = "${pluginId}_$key"
        val action = QuickAction(
            labelResId = 0,
            labelString = label,
            key = globalKey,
            onClick = { onClick.run() }
        )
        PluginManager.addQuickAction(pluginId, key, action)
    }
    
    override fun removeQuickAction(key: String) {
        PluginManager.removeQuickAction(pluginId, key)
    }
    
    override fun clearQuickActions() {
        PluginManager.clearQuickActions(pluginId)
    }
    
    // ==================== 菜单操作 ====================
    
    override fun addMenuItem(key: String, label: String, onClick: Runnable) {
        UIState.addPluginMenuItem(pluginId, key, label, onClick)
    }
    
    override fun addMenuDivider(key: String) {
        UIState.addPluginMenuDivider(pluginId, key)
    }
    
    override fun removeMenuItem(key: String) {
        UIState.removePluginMenuItem(pluginId, key)
    }
    
    override fun clearMenuItems() {
        UIState.removePluginMenuItems(pluginId)
    }
    
    // ==================== 文件树操作 ====================
    
    override fun addFileTreeMenuItem(key: String, label: String, filter: String?, onClick: FileTreeItemCallback) {
        addFileTreeMenuItem(key, label, null, filter, onClick)
    }
    
    override fun addFileTreeMenuItem(key: String, label: String, iconName: String?, filter: String?, onClick: FileTreeItemCallback) {
        UIState.addFileTreeMenuItem(pluginId, key, label, iconName, filter, onClick::onItemClick)
    }
    
    override fun addFileTreeMenuDivider(key: String, filter: String?) {
        UIState.addFileTreeMenuDivider(pluginId, key, filter)
    }
    
    override fun removeFileTreeMenuItem(key: String) {
        UIState.removeFileTreeMenuItem(pluginId, key)
    }
    
    override fun clearFileTreeMenuItems() {
        UIState.removeFileTreeMenuItems(pluginId)
    }
    
    override fun getFileInfo(filePath: String): FileInfo? {
        val file = File(filePath)
        if (!file.exists()) return null
        
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
        val clipboard = PluginManager.appContext?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("text", text)
        clipboard?.setPrimaryClip(clip)
    }
    
    override fun getClipboardText(): String? {
        val clipboard = PluginManager.appContext?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            return clip.getItemAt(0).text?.toString()
        }
        return null
    }
    
    // ==================== 配置存储 ====================
    
    override fun getConfig(key: String, defaultValue: String): String {
        return pluginPrefs.getString(key, defaultValue) ?: defaultValue
    }
    
    override fun setConfig(key: String, value: String) {
        pluginPrefs.edit().putString(key, value).apply()
    }
    
    override fun getConfigInt(key: String, defaultValue: Int): Int {
        return pluginPrefs.getInt(key, defaultValue)
    }
    
    override fun setConfigInt(key: String, value: Int) {
        pluginPrefs.edit().putInt(key, value).apply()
    }
    
    override fun getConfigBoolean(key: String, defaultValue: Boolean): Boolean {
        return pluginPrefs.getBoolean(key, defaultValue)
    }
    
    override fun setConfigBoolean(key: String, value: Boolean) {
        pluginPrefs.edit().putBoolean(key, value).apply()
    }
    
    override fun removeConfig(key: String) {
        pluginPrefs.edit().remove(key).apply()
    }
    
    // ==================== 事件系统 ====================
    
    override fun registerEventListener(eventName: String, listener: Any) {
        EventManager.registerEventListener(eventName, listener)
    }
    
    override fun unregisterEventListener(eventName: String, listener: Any) {
        EventManager.unregisterEventListener(eventName, listener)
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
                val requestBuilder = Request.Builder().url(url).get()
                headers?.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
                
                val response = httpClient.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful) {
                    callback.onResult(true, responseBody, null)
                } else {
                    callback.onResult(false, responseBody, "HTTP ${response.code}")
                }
            } catch (e: Exception) {
                callback.onResult(false, null, e.message)
            }
        }
    }
    
    override fun httpPost(url: String, body: String, headers: Map<String, String>?, callback: HttpCallback) {
        backgroundExecutor.execute {
            try {
                val requestBuilder = Request.Builder().url(url)
                    .post(body.toRequestBody())
                headers?.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
                
                val response = httpClient.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful) {
                    callback.onResult(true, responseBody, null)
                } else {
                    callback.onResult(false, responseBody, "HTTP ${response.code}")
                }
            } catch (e: Exception) {
                callback.onResult(false, null, e.message)
            }
        }
    }
    
    // ==================== 编辑器操作（简化实现） ====================
    
    override fun getActiveFile(): String? {
        return PluginManager.activeViewModel?.activeFileState?.file?.absolutePath
    }
    
    override fun getActiveText(): String? {
        return PluginManager.activeViewModel?.activeFileState?.content
    }
    
    override fun setActiveText(text: String) {
        handler.post {
            PluginManager.activeViewModel?.let { vm ->
                vm.activeFileState?.onContentChanged(text)
                vm.activeFileState?.let { state ->
                    state.content = text
                }
            }
        }
    }
    
    override fun insertText(text: String) {
        handler.post {
            PluginManager.activeViewModel?.insertSymbolToCorrectEditor(text)
        }
    }
    
    override fun getSelectedText(): String? {
        val editor = PluginManager.activeViewModel?.getActiveEditor() ?: return null
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
            val editor = PluginManager.activeViewModel?.getActiveEditor() ?: return@post
            val startPos = editor.text.indexer.getCharPosition(start)
            val endPos = editor.text.indexer.getCharPosition(end)
            editor.setSelectionRegion(
                startPos.line, startPos.column,
                endPos.line, endPos.column
            )
        }
    }
    
    override fun getCursorPosition(): IntArray? {
        val editor = PluginManager.activeViewModel?.getActiveEditor() ?: return null
        val cursor = editor.cursor
        return intArrayOf(cursor.leftLine, cursor.leftColumn)
    }
    
    override fun gotoLine(line: Int) {
        handler.post {
            val editor = PluginManager.activeViewModel?.getActiveEditor() ?: return@post
            editor.jumpToLine(line.coerceAtLeast(0))
        }
    }
    
    override fun gotoPosition(line: Int, column: Int) {
        handler.post {
            val editor = PluginManager.activeViewModel?.getActiveEditor() ?: return@post
            editor.setSelectionRegion(line, column, line, column)
        }
    }
    
    override fun getOpenFiles(): Array<String>? {
        return PluginManager.activeViewModel?.openFiles?.map { it.file.absolutePath }?.toTypedArray()
    }
    
    override fun closeActiveFile() {
        handler.post {
            val index = PluginManager.activeViewModel?.activeFileIndex ?: -1
            if (index >= 0) {
                PluginManager.activeViewModel?.closeFile(index)
            }
        }
    }
    
    override fun openFile(filePath: String) {
        handler.post {
            val file = File(filePath)
            if (file.exists()) {
                PluginManager.activeViewModel?.openFile(file)
            }
        }
    }
    
    override fun saveActiveFile() {
        kotlinx.coroutines.GlobalScope.launch {
            PluginManager.activeViewModel?.saveCurrentFileSilently()
        }
    }
    
    override fun saveAllFiles() {
        kotlinx.coroutines.GlobalScope.launch {
            PluginManager.activeViewModel?.saveAllFilesSilently()
        }
    }
    
    override fun undo() {
        handler.post {
            PluginManager.activeViewModel?.getActiveEditor()?.undo()
        }
    }
    
    override fun redo() {
        handler.post {
            PluginManager.activeViewModel?.getActiveEditor()?.redo()
        }
    }
    
    override fun findText(query: String, caseSensitive: Boolean, regex: Boolean) {
        handler.post {
            val editor = PluginManager.activeViewModel?.getActiveEditor() ?: return@post
            val options = io.github.rosemoe.sora.widget.EditorSearcher.SearchOptions(!caseSensitive, regex)
            editor.searcher.search(query, options)
        }
    }
    
    override fun replaceText(query: String, replacement: String, replaceAll: Boolean) {
        handler.post {
            val editor = PluginManager.activeViewModel?.getActiveEditor() ?: return@post
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
        return PluginManager.currentProjectPath.value
    }
    
    override fun readFile(relativePath: String): String? {
        return try {
            val projectPath = PluginManager.currentProjectPath.value ?: return null
            File(projectPath, relativePath).readText()
        } catch (e: Exception) {
            null
        }
    }
    
    override fun writeFile(relativePath: String, content: String): Boolean {
        return try {
            val projectPath = PluginManager.currentProjectPath.value ?: return false
            File(projectPath, relativePath).writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun listFiles(relativePath: String): Array<String>? {
        return try {
            val projectPath = PluginManager.currentProjectPath.value ?: return null
            File(projectPath, relativePath).list()
        } catch (e: Exception) {
            null
        }
    }
    
    override fun createFile(relativePath: String, content: String): Boolean {
        return try {
            val projectPath = PluginManager.currentProjectPath.value ?: return false
            File(projectPath, relativePath).apply {
                parentFile?.mkdirs()
                writeText(content)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun deleteFile(relativePath: String): Boolean {
        return try {
            val projectPath = PluginManager.currentProjectPath.value ?: return false
            File(projectPath, relativePath).deleteRecursively()
        } catch (e: Exception) {
            false
        }
    }
    
    override fun fileExists(relativePath: String): Boolean {
        return try {
            val projectPath = PluginManager.currentProjectPath.value ?: return false
            File(projectPath, relativePath).exists()
        } catch (e: Exception) {
            false
        }
    }
    
    override fun createDirectory(relativePath: String): Boolean {
        return try {
            val projectPath = PluginManager.currentProjectPath.value ?: return false
            File(projectPath, relativePath).mkdirs()
        } catch (e: Exception) {
            false
        }
    }
    
    // ==================== UI 对话框 ====================
    
    override fun showMessage(title: String, message: String) {
        handler.post {
            PluginManager.currentDialog.value = PluginManager.DialogState.Message(
                title = title,
                message = message,
                onDismiss = { PluginManager.currentDialog.value = null }
            )
        }
    }
    
    override fun showConfirm(title: String, message: String, onConfirm: Runnable, onCancel: Runnable?) {
        handler.post {
            PluginManager.currentDialog.value = PluginManager.DialogState.Confirm(
                title = title,
                message = message,
                onConfirm = {
                    PluginManager.currentDialog.value = null
                    onConfirm.run()
                },
                onCancel = onCancel?.let {
                    {
                        PluginManager.currentDialog.value = null
                        it.run()
                    }
                },
                onDismiss = { PluginManager.currentDialog.value = null }
            )
        }
    }
    
    override fun showInputDialog(title: String, hint: String, defaultValue: String, onInput: OnInputCallback) {
        handler.post {
            PluginManager.currentDialog.value = PluginManager.DialogState.Input(
                title = title,
                hint = hint,
                defaultValue = defaultValue,
                onInput = { text ->
                    PluginManager.currentDialog.value = null
                    onInput.onInput(text)
                },
                onDismiss = { PluginManager.currentDialog.value = null }
            )
        }
    }
    
    override fun showSingleChoiceDialog(title: String, items: Array<String>, selectedIndex: Int, onSelect: OnSelectCallback) {
        handler.post {
            PluginManager.currentDialog.value = PluginManager.DialogState.SingleChoice(
                title = title,
                items = items,
                selectedIndex = selectedIndex,
                onSelect = { index ->
                    PluginManager.currentDialog.value = null
                    onSelect.onSelect(index)
                },
                onDismiss = { PluginManager.currentDialog.value = null }
            )
        }
    }
    
    override fun showMultiChoiceDialog(title: String, items: Array<String>, checkedItems: BooleanArray, onConfirm: OnMultiSelectCallback) {
        handler.post {
            PluginManager.currentDialog.value = PluginManager.DialogState.MultiChoice(
                title = title,
                items = items,
                checkedItems = checkedItems.copyOf(),
                onConfirm = { result ->
                    PluginManager.currentDialog.value = null
                    onConfirm.onConfirm(result)
                },
                onDismiss = { PluginManager.currentDialog.value = null }
            )
        }
    }
    
    override fun showProgressDialog(title: String, message: String): com.luaforge.studio.lxclua.plugin.api.ProgressDialogHandle {
        return object : com.luaforge.studio.lxclua.plugin.api.ProgressDialogHandle {
            override fun setProgress(progress: Int) {}
            override fun setMessage(message: String) {}
            override fun dismiss() {}
            override fun isShowing(): Boolean = false
        }
    }
    
    // ==================== Java 反射调用 ====================
    
    override fun loadClass(className: String): Class<*>? {
        return try {
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            null
        }
    }
    
    override fun newInstance(className: String, args: Array<Any?>?): Any? {
        return try {
            val clazz = Class.forName(className)
            if (args.isNullOrEmpty()) {
                clazz.getDeclaredConstructor().newInstance()
            } else {
                val argTypes = args.map { it?.javaClass ?: Any::class.java }.toTypedArray()
                val constructor = clazz.getDeclaredConstructor(*argTypes)
                constructor.isAccessible = true
                constructor.newInstance(*args)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun callStaticMethod(className: String, methodName: String, args: Array<Any?>?): Any? {
        return try {
            val clazz = Class.forName(className)
            val argArray = args ?: emptyArray()
            val methods = clazz.methods.filter { it.name == methodName }
            for (method in methods) {
                val paramTypes = method.parameterTypes
                if (paramTypes.size == argArray.size) {
                    val convertedArgs = convertArgs(paramTypes, argArray)
                    if (convertedArgs != null) {
                        return method.invoke(null, *convertedArgs)
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
    
    override fun callMethod(obj: Any, methodName: String, args: Array<Any?>?): Any? {
        return try {
            val argArray = args ?: emptyArray()
            val methods = obj.javaClass.methods.filter { it.name == methodName }
            for (method in methods) {
                val paramTypes = method.parameterTypes
                if (paramTypes.size == argArray.size) {
                    val convertedArgs = convertArgs(paramTypes, argArray)
                    if (convertedArgs != null) {
                        return method.invoke(obj, *convertedArgs)
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun convertArgs(paramTypes: Array<Class<*>>, args: Array<Any?>): Array<Any?>? {
        return try {
            Array(args.size) { i ->
                val arg = args[i]
                val paramType = paramTypes[i]
                if (arg == null) null
                else if (paramType.isInstance(arg)) arg
                else convertValue(arg, paramType)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun convertValue(value: Any, targetType: Class<*>): Any? {
        return when (targetType) {
            Int::class.java, Int::class.javaPrimitiveType -> (value as? Number)?.toInt()
            Long::class.java, Long::class.javaPrimitiveType -> (value as? Number)?.toLong()
            Float::class.java, Float::class.javaPrimitiveType -> (value as? Number)?.toFloat()
            Double::class.java, Double::class.javaPrimitiveType -> (value as? Number)?.toDouble()
            Boolean::class.java, Boolean::class.javaPrimitiveType -> value as? Boolean
            else -> value
        }
    }
    
    override fun getStaticField(className: String, fieldName: String): Any? {
        return try {
            val clazz = Class.forName(className)
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            field.get(null)
        } catch (e: Exception) {
            null
        }
    }
    
    override fun setStaticField(className: String, fieldName: String, value: Any?) {
        try {
            val clazz = Class.forName(className)
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(null, value)
        } catch (e: Exception) {
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
        }
    }
    
    // ==================== DEX 加载 ====================
    
    override fun loadDex(dexPath: String): Boolean {
        return try {
            val classLoader = dalvik.system.DexClassLoader(
                dexPath,
                PluginManager.appContext?.codeCacheDir?.absolutePath,
                null,
                javaClass.classLoader
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun loadDexFromUrl(url: String, callback: DexLoadCallback) {
        backgroundExecutor.execute {
            try {
                val response = httpClient.newCall(Request.Builder().url(url).get().build()).execute()
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null) {
                        val dexFile = File(PluginManager.appContext?.cacheDir, "temp.dex")
                        dexFile.writeBytes(bytes)
                        loadDex(dexFile.absolutePath)
                        callback.onLoad(true, null)
                    } else {
                        callback.onLoad(false, "下载失败")
                    }
                } else {
                    callback.onLoad(false, "HTTP ${response.code}")
                }
            } catch (e: Exception) {
                callback.onLoad(false, e.message)
            }
        }
    }
    
    override fun getPluginClassLoader(): ClassLoader? {
        return javaClass.classLoader
    }
    
    // ==================== 动态资源 ====================
    
    override fun loadResources(apkPath: String): com.luaforge.studio.lxclua.plugin.data.PluginResources? {
        return try {
            val assetManager = android.content.res.AssetManager::class.java.newInstance()
            val addAssetPath = android.content.res.AssetManager::class.java.getMethod("addAssetPath", String::class.java)
            addAssetPath.invoke(assetManager, apkPath)
            
            val resources = PluginManager.appContext?.resources
            val config = resources?.configuration
            val metrics = resources?.displayMetrics
            
            com.luaforge.studio.lxclua.plugin.data.PluginResources(
                assetManager,
                android.content.res.Resources(assetManager, metrics, config),
                "plugin"
            )
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getResourceString(resources: com.luaforge.studio.lxclua.plugin.data.PluginResources, resName: String): String? {
        return try {
            val resId = resources.resources.getIdentifier(resName, "string", resources.packageName)
            if (resId > 0) resources.resources.getString(resId) else null
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getResourceDrawable(resources: com.luaforge.studio.lxclua.plugin.data.PluginResources, resName: String): android.graphics.drawable.Drawable? {
        return try {
            val resId = resources.resources.getIdentifier(resName, "drawable", resources.packageName)
            if (resId > 0) resources.resources.getDrawable(resId, null) else null
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getResourceColor(resources: com.luaforge.studio.lxclua.plugin.data.PluginResources, resName: String): Int {
        return try {
            val resId = resources.resources.getIdentifier(resName, "color", resources.packageName)
            if (resId > 0) resources.resources.getColor(resId, null) else 0
        } catch (e: Exception) {
            0
        }
    }
    
    override fun getResourceId(resources: com.luaforge.studio.lxclua.plugin.data.PluginResources, resName: String, resType: String): Int {
        return resources.resources.getIdentifier(resName, resType, resources.packageName)
    }
    
    // ==================== Lua 脚本执行 ====================
    
    override fun executeLua(script: String): Any? {
        return try {
            val l = com.luajava.LuaStateFactory.newLuaState()
            l.openLibs()
            val ok = l.LloadString(script)
            if (ok == 0) {
                l.pcall(0, 1, 0)
                val result = l.toJavaObject(-1)
                l.close()
                result
            } else {
                l.close()
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun executeLuaFile(scriptPath: String): Any? {
        return try {
            val l = com.luajava.LuaStateFactory.newLuaState()
            l.openLibs()
            val ok = l.LloadFile(scriptPath)
            if (ok == 0) {
                l.pcall(0, 1, 0)
                val result = l.toJavaObject(-1)
                l.close()
                result
            } else {
                l.close()
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun compileLua(script: String): ByteArray? {
        return try {
            val l = com.luajava.LuaStateFactory.newLuaState()
            l.openLibs()
            val ok = l.LloadString(script)
            if (ok == 0) {
                val bytecode = l.dump(-1)
                l.close()
                bytecode
            } else {
                l.close()
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun executeLuaBytecode(bytecode: ByteArray): Any? {
        return try {
            val l = com.luajava.LuaStateFactory.newLuaState()
            l.openLibs()
            l.LloadBuffer(bytecode, "bytecode")
            l.pcall(0, 1, 0)
            val result = l.toJavaObject(-1)
            l.close()
            result
        } catch (e: Exception) {
            null
        }
    }
}
