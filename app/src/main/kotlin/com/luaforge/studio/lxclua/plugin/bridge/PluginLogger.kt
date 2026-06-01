package com.luaforge.studio.lxclua.plugin.bridge

import android.content.Context
import com.luaforge.studio.lxclua.plugin.PluginManager
import com.luaforge.studio.lxclua.plugin.api.IPluginBridgeLogger
import com.luaforge.studio.lxclua.utils.LogCatcher
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 插件日志系统 API 实现
 * 
 * 日志存储路径: LXC-LUA/plugins/<pluginId>/logs/YYYY-MM-DD.log
 */
class PluginLogger(
    private val context: Context,
    private val pluginId: String
) : IPluginBridgeLogger {

    private val logTimeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val logFileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun getPluginsDir(): File {
        return PluginManager.getPluginsDir(context)
    }

    private fun ensureLogDir(): File {
        val logDir = File(getPluginsDir(), "$pluginId/logs")
        if (!logDir.exists()) logDir.mkdirs()
        return logDir
    }

    private fun ensureLogDirForPlugin(targetPluginId: String): File? {
        val logDir = File(getPluginsDir(), "$targetPluginId/logs")
        if (!logDir.exists()) return null
        return logDir
    }

    private fun writeLog(level: String, tag: String, message: String, error: String?) {
        val logDir = ensureLogDir()
        val now = Date()
        val logFile = File(logDir, "${logFileDateFormat.format(now)}.log")
        val timestamp = logTimeFormat.format(now)
        val errorSuffix = if (!error.isNullOrEmpty()) " | $error" else ""
        val line = "$timestamp [$level] [$tag] $message$errorSuffix\n"

        try {
            logDir.parentFile?.mkdirs()
            logFile.appendText(line)
        } catch (e: Exception) {
            android.util.Log.e("PluginLogger", "写入日志失败: $pluginId", e)
        }
    }

    override fun debug(tag: String, message: String) {
        LogCatcher.d(tag, "[$pluginId] $message")
        writeLog("DEBUG", tag, message, null)
    }

    override fun info(tag: String, message: String) {
        LogCatcher.i(tag, "[$pluginId] $message")
        writeLog("INFO", tag, message, null)
    }

    override fun warn(tag: String, message: String) {
        LogCatcher.w(tag, "[$pluginId] $message")
        writeLog("WARN", tag, message, null)
    }

    override fun error(tag: String, message: String, error: String?) {
        LogCatcher.e(tag, "[$pluginId] $message", null)
        writeLog("ERROR", tag, message, error)
    }

    override fun getLogDir(): String {
        return ensureLogDir().absolutePath
    }

    override fun listLogFiles(): Array<String> {
        val logDir = ensureLogDir()
        return (logDir.listFiles { f -> f.isFile && f.name.endsWith(".log") }
            ?.map { it.name }
            ?.sortedDescending()
            ?: emptyList()).toTypedArray()
    }

    override fun readLogFile(filename: String): String {
        val safeName = File(filename).name
        if (safeName != filename || !safeName.endsWith(".log")) return ""
        val logFile = File(ensureLogDir(), safeName)
        if (!logFile.exists() || !logFile.canRead()) return ""
        return try {
            logFile.readText()
        } catch (e: Exception) {
            ""
        }
    }

    override fun getLatestLog(): String {
        val files = listLogFiles()
        if (files.isEmpty()) return ""
        return readLogFile(files[0])
    }

    override fun getLogFileSize(filename: String): Long {
        val safeName = File(filename).name
        val logFile = File(ensureLogDir(), safeName)
        return if (logFile.exists()) logFile.length() else 0L
    }

    override fun clearLogs(): Boolean {
        val logDir = ensureLogDir()
        var ok = true
        logDir.listFiles()?.forEach {
            try { if (!it.delete()) ok = false }
            catch (e: Exception) { ok = false }
        }
        return ok
    }

    override fun getPluginLogDir(pluginId: String): String {
        return ensureLogDirForPlugin(pluginId)?.absolutePath ?: ""
    }

    override fun listPluginLogFiles(pluginId: String): Array<String> {
        val logDir = ensureLogDirForPlugin(pluginId) ?: return emptyArray()
        return (logDir.listFiles { f -> f.isFile && f.name.endsWith(".log") }
            ?.map { it.name }
            ?.sortedDescending()
            ?: emptyList()).toTypedArray()
    }

    override fun readPluginLogFile(pluginId: String, filename: String): String {
        val logDir = ensureLogDirForPlugin(pluginId) ?: return ""
        val safeName = File(filename).name
        if (safeName != filename || !safeName.endsWith(".log")) return ""
        val logFile = File(logDir, safeName)
        if (!logFile.exists() || !logFile.canRead()) return ""
        return try {
            logFile.readText()
        } catch (e: Exception) {
            ""
        }
    }

    override fun getPluginLatestLog(pluginId: String): String {
        val files = listPluginLogFiles(pluginId)
        if (files.isEmpty()) return ""
        return readPluginLogFile(pluginId, files[0])
    }

    override fun searchLogs(keyword: String, maxResults: Int): Array<LogEntry> {
        val results = mutableListOf<LogEntry>()
        val files = listLogFiles()
        for (filename in files) {
            if (results.size >= maxResults) break
            try {
                val logFile = File(ensureLogDir(), filename)
                val lines = logFile.readLines()
                for (line in lines) {
                    if (results.size >= maxResults) break
                    if (line.contains(keyword, ignoreCase = true)) {
                        val entry = parseLogLine(line, results.size + 1)
                        if (entry != null) results.add(entry)
                    }
                }
            } catch (e: Exception) {
            }
        }
        return results.toTypedArray()
    }

    override fun searchPluginLogs(pluginId: String, keyword: String, maxResults: Int): Array<LogEntry> {
        val logDir = ensureLogDirForPlugin(pluginId) ?: return emptyArray()
        val results = mutableListOf<LogEntry>()
        val files = (logDir.listFiles { f -> f.isFile && f.name.endsWith(".log") }
            ?.sortedByDescending { it.name }
            ?: emptyList<File>())

        for (logFile in files) {
            if (results.size >= maxResults) break
            try {
                val lines = logFile.readLines()
                for (line in lines) {
                    if (results.size >= maxResults) break
                    if (line.contains(keyword, ignoreCase = true)) {
                        val entry = parseLogLine(line, results.size + 1)
                        if (entry != null) results.add(entry)
                    }
                }
            } catch (e: Exception) {
            }
        }
        return results.toTypedArray()
    }

    override fun getLogLineCount(): Int {
        val files = listLogFiles()
        var count = 0
        for (filename in files) {
            try {
                val logFile = File(ensureLogDir(), filename)
                count += logFile.readLines().size
            } catch (e: Exception) {
            }
        }
        return count
    }

    override fun getPluginLogLineCount(pluginId: String): Int {
        val files = listPluginLogFiles(pluginId)
        val logDir = ensureLogDirForPlugin(pluginId) ?: return 0
        var count = 0
        for (filename in files) {
            try {
                val logFile = File(logDir, filename)
                count += logFile.readLines().size
            } catch (e: Exception) {
            }
        }
        return count
    }

    private fun parseLogLine(line: String, lineNumber: Int): LogEntry? {
        val regex = Regex("""^(\d{2}:\d{2}:\d{2}\.\d{3})\s*\[(\w+)\]\s*\[(.+?)\]\s*(.*)$""")
        val match = regex.find(line) ?: return null
        return LogEntry(
            timestamp = match.groupValues[1],
            level = match.groupValues[2],
            tag = match.groupValues[3],
            message = match.groupValues[4],
            lineNumber = lineNumber
        )
    }
}