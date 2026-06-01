package com.luaforge.studio.lxclua.plugin.bridge

/**
 * 日志条目，用于搜索结果返回
 */
data class LogEntry(
    val timestamp: String,
    val level: String,
    val tag: String,
    val message: String,
    val lineNumber: Int
)