package com.luaforge.studio.lxclua.plugin.api

import com.luaforge.studio.lxclua.plugin.bridge.LogEntry

/**
 * 插件日志系统 API
 * 
 * 插件可通过此接口：
 * - 记录自己的日志
 * - 读取自己的日志文件
 * - 读取其他插件的日志（跨插件日志访问）
 * - 获取全局应用日志
 */
interface IPluginBridgeLogger {
    
    fun debug(tag: String, message: String)
    
    fun info(tag: String, message: String)
    
    fun warn(tag: String, message: String)
    
    fun error(tag: String, message: String, error: String?)
    
    fun getLogDir(): String
    
    fun listLogFiles(): Array<String>
    
    fun readLogFile(filename: String): String
    
    fun getLatestLog(): String
    
    fun getLogFileSize(filename: String): Long
    
    fun clearLogs(): Boolean

    fun getPluginLogDir(pluginId: String): String
    
    fun listPluginLogFiles(pluginId: String): Array<String>
    
    fun readPluginLogFile(pluginId: String, filename: String): String
    
    fun getPluginLatestLog(pluginId: String): String
    
    fun searchLogs(keyword: String, maxResults: Int): Array<LogEntry>
    
    fun searchPluginLogs(pluginId: String, keyword: String, maxResults: Int): Array<LogEntry>
    
    fun getLogLineCount(): Int
    
    fun getPluginLogLineCount(pluginId: String): Int
}