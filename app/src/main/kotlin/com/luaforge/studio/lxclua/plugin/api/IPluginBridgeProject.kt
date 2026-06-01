package com.luaforge.studio.lxclua.plugin.api

/**
 * 项目操作相关 API
 */
interface IPluginBridgeProject {
    /**
     * 获取当前项目路径
     */
    fun getProjectPath(): String?
    
    /**
     * 读取项目中的文件内容
     */
    fun readFile(relativePath: String): String?
    
    /**
     * 写入文件到项目目录
     */
    fun writeFile(relativePath: String, content: String): Boolean
    
    /**
     * 列出项目目录下的文件
     */
    fun listFiles(relativePath: String): Array<String>?
    
    /**
     * 创建文件
     */
    fun createFile(relativePath: String, content: String): Boolean
    
    /**
     * 删除文件
     */
    fun deleteFile(relativePath: String): Boolean
    
    /**
     * 检查文件是否存在
     */
    fun fileExists(relativePath: String): Boolean
    
    /**
     * 创建目录
     */
    fun createDirectory(relativePath: String): Boolean
}