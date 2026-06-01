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
    
    /**
     * 记录 DEBUG 级别日志
     * @param tag 日志标签
     * @param message 日志内容
     */
    fun debug(tag: String, message: String)
    
    /**
     * 记录 INFO 级别日志
     * @param tag 日志标签
     * @param message 日志内容
     */
    fun info(tag: String, message: String)
    
    /**
     * 记录 WARN 级别日志
     * @param tag 日志标签
     * @param message 日志内容
     */
    fun warn(tag: String, message: String)
    
    /**
     * 记录 ERROR 级别日志
     * @param tag 日志标签
     * @param message 日志内容
     * @param error 异常信息（可选）
     */
    fun error(tag: String, message: String, error: String?)
    
    /**
     * 获取自己的日志目录路径
     * @return 日志目录绝对路径
     */
    fun getLogDir(): String
    
    /**
     * 列出自己的所有日志文件名（按日期降序）
     * @return 日志文件名数组，如 ["2026-06-02.log", "2026-06-01.log"]
     */
    fun listLogFiles(): Array<String>
    
    /**
     * 读取自己指定日志文件的全部内容
     * @param filename 日志文件名（如 "2026-06-02.log"）
     * @return 日志内容，文件不存在或读取失败返回空字符串
     */
    fun readLogFile(filename: String): String
    
    /**
     * 获取自己最近的日志内容
     * @return 最新日志文件的全部内容，无日志返回空字符串
     */
    fun getLatestLog(): String
    
    /**
     * 获取自己指定日志文件的大小（字节）
     * @param filename 日志文件名
     * @return 文件大小，文件不存在返回 0
     */
    fun getLogFileSize(filename: String): Long
    
    /**
     * 清空自己的所有日志文件
     * @return 是否全部清空成功
     */
    fun clearLogs(): Boolean

    /**
     * 获取指定插件的日志目录路径
     * @param pluginId 目标插件 ID
     * @return 目标插件的日志目录绝对路径，插件不存在返回空字符串
     */
    fun getPluginLogDir(pluginId: String): String
    
    /**
     * 列出指定插件的所有日志文件名
     * @param pluginId 目标插件 ID
     * @return 日志文件名数组
     */
    fun listPluginLogFiles(pluginId: String): Array<String>
    
    /**
     * 读取指定插件的日志文件内容
     * @param pluginId 目标插件 ID
     * @param filename 日志文件名
     * @return 日志内容
     */
    fun readPluginLogFile(pluginId: String, filename: String): String
    
    /**
     * 获取指定插件最近的日志内容
     * @param pluginId 目标插件 ID
     * @return 最新日志内容
     */
    fun getPluginLatestLog(pluginId: String): String
    
    /**
     * 搜索自己的日志（支持关键词筛选）
     * @param keyword 搜索关键词
     * @param maxResults 最大返回条数
     * @return 匹配的日志条目数组
     */
    fun searchLogs(keyword: String, maxResults: Int): Array<LogEntry>
    
    /**
     * 搜索指定插件的日志
     * @param pluginId 目标插件 ID
     * @param keyword 搜索关键词
     * @param maxResults 最大返回条数
     * @return 匹配的日志条目数组
     */
    fun searchPluginLogs(pluginId: String, keyword: String, maxResults: Int): Array<LogEntry>
    
    /**
     * 获取自己的日志总行数
     * @return 总行数
     */
    fun getLogLineCount(): Int
    
    /**
     * 获取指定插件的日志总行数
     * @param pluginId 目标插件 ID
     * @return 总行数
     */
    fun getPluginLogLineCount(pluginId: String): Int
}