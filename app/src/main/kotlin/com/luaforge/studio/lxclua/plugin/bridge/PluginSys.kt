package com.luaforge.studio.lxclua.plugin.bridge

import android.content.Context
import com.luaforge.studio.lxclua.plugin.PluginManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 系统级操作 API
 *
 * 使用方式: plugin.sys.log(tag, message)
 *
 * 日志系统：每个插件拥有独立的日志目录，路径为
 *   LXC-LUA/plugins/<pluginId>/logs/YYYY-MM-DD.log
 * 日志通过 plugin.sys.log 写入，自动按日期分文件。
 * 插件也可以通过 listLogFiles / readLogFile 读取自己的日志。
 */
class PluginSys(
    private val context: Context,
    private val pluginId: String
) {

    private val logTimeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val logFileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * 记录一条日志，同时写入 logcat 与插件专属日志文件。
     *
     * @param tag 日志标签（一般为插件名或子模块名）
     * @param message 日志内容
     */
    fun log(tag: String, message: String) {
        // 1) 输出到 logcat（沿用原行为）
        android.util.Log.d(tag, "[$pluginId] $message")

        // 2) 写入按日分文件的专属日志
        try {
            val logDir = ensureLogDir()
            val now = Date()
            val logFile = File(logDir, "${logFileDateFormat.format(now)}.log")
            val timestamp = logTimeFormat.format(now)
            val line = "$timestamp [$tag] $message\n"

            logDir.parentFile?.mkdirs()
            logFile.appendText(line)
        } catch (e: Exception) {
            android.util.Log.e("PluginSys", "写入插件日志失败: $pluginId", e)
        }
    }

    /**
     * 显示 Toast 消息
     */
    fun toast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    /**
     * 显示长 Toast 消息
     */
    fun toastLong(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
    }

    /**
     * 获取 IDE 版本号
     */
    fun getVersion(): String {
        return "1.0.0"
    }

    /**
     * 获取应用数据目录
     */
    fun getDataDir(): String {
        return context.filesDir.absolutePath
    }

    /**
     * 获取当前插件的目录（包含 main.lua / manifest.json）
     */
    fun getPluginDir(): String {
        return PluginManager.getPluginDirectory(context, pluginId)?.absolutePath ?: ""
    }

    /**
     * 获取当前插件的数据目录（用于持久化）
     */
    fun getPluginDataDir(): String {
        val dir = File(context.filesDir, "plugin_data/$pluginId")
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    /**
     * 获取当前插件的日志目录路径
     */
    fun getLogDir(): String {
        return ensureLogDir().absolutePath
    }

    /**
     * 列出当前插件的所有日志文件名（按日期降序，yyyy-MM-dd.log）
     */
    fun listLogFiles(): Array<String> {
        val logDir = ensureLogDir()
        return (logDir.listFiles { f -> f.isFile && f.name.endsWith(".log") }
            ?.map { it.name }
            ?.sortedDescending()
            ?.toTypedArray()
            ?: emptyArray())
    }

    /**
     * 读取指定日志文件的全部内容
     */
    fun readLogFile(filename: String): String {
        val safeName = File(filename).name
        if (safeName != filename || !safeName.endsWith(".log")) {
            return ""
        }
        val logFile = File(ensureLogDir(), safeName)
        if (!logFile.exists() || !logFile.canRead()) {
            return ""
        }
        return try {
            logFile.readText()
        } catch (e: Exception) {
            android.util.Log.e("PluginSys", "读取日志失败: $safeName", e)
            ""
        }
    }

    /**
     * 读取最近一次写入的日志（如果存在）
     */
    fun getLatestLog(): String {
        val files = listLogFiles()
        if (files.isEmpty()) return ""
        return readLogFile(files[0])
    }

    /**
     * 获取指定日志文件的大小（字节）
     */
    fun getLogFileSize(filename: String): Long {
        val safeName = File(filename).name
        val logFile = File(ensureLogDir(), safeName)
        return if (logFile.exists()) logFile.length() else 0L
    }

    /**
     * 清空当前插件的所有日志文件
     */
    fun clearLogs(): Boolean {
        val logDir = ensureLogDir()
        var ok = true
        logDir.listFiles()?.forEach {
            try {
                if (!it.delete()) ok = false
            } catch (e: Exception) {
                ok = false
            }
        }
        return ok
    }

    // ============ 国际化 API ============

    /**
     * 获取当前应用语言环境
     * @return 语言代码，如 "zh", "en", "ja" 等
     */
    fun getAppLanguage(): String {
        return context.resources.configuration.locale.language
    }

    /**
     * 获取当前应用区域
     * @return 区域代码，如 "CN", "US", "JP" 等
     */
    fun getAppRegion(): String {
        return context.resources.configuration.locale.country
    }

    /**
     * 获取完整的语言标签（语言-区域）
     * @return 如 "zh-CN", "en-US" 等
     */
    fun getAppLocale(): String {
        val locale = context.resources.configuration.locale
        return if (locale.country.isNotEmpty()) {
            "${locale.language}-${locale.country}"
        } else {
            locale.language
        }
    }

    /**
     * 根据资源ID获取本地化字符串
     * @param resId 资源ID（如 "app_name"）
     * @return 本地化后的字符串，如果未找到返回 null
     */
    fun getString(resId: String): String? {
        return try {
            val id = context.resources.getIdentifier(resId, "string", context.packageName)
            if (id > 0) {
                context.resources.getString(id)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 根据资源ID获取本地化字符串（带格式化参数）
     * @param resId 资源ID
     * @param args 格式化参数
     * @return 格式化后的字符串，如果未找到返回 null
     */
    fun getString(resId: String, vararg args: Any?): String? {
        return try {
            val id = context.resources.getIdentifier(resId, "string", context.packageName)
            if (id > 0) {
                val format = context.resources.getString(id)
                String.format(format, *args)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查是否为中文环境
     */
    fun isChinese(): Boolean {
        return getAppLanguage() == "zh"
    }

    /**
     * 检查是否为英文环境
     */
    fun isEnglish(): Boolean {
        return getAppLanguage() == "en"
    }

    /**
     * 获取所有可用的应用语言列表
     * @return 语言代码数组
     */
    fun getAvailableLanguages(): Array<String> {
        return arrayOf("zh", "en", "ja", "ko", "fr", "de", "es", "pt", "ru", "ar")
    }

    // ============ 内部辅助 ============

    /**
     * 确保日志目录存在，不存在则创建
     */
    private fun ensureLogDir(): File {
        val baseDir = PluginManager.getPluginsDir(context)
        val logDir = File(baseDir, "$pluginId/logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        return logDir
    }
}
