package com.luaforge.studio.lxclua.plugin.data

/**
 * 文件信息类
 */
class FileInfo(
    /**
     * 文件完整路径
     */
    val path: String,
    
    /**
     * 文件名（含扩展名）
     */
    val name: String,
    
    /**
     * 文件扩展名（不含点，如 "lua"）
     */
    val extension: String,
    
    /**
     * 是否为目录
     */
    val isDirectory: Boolean,
    
    /**
     * 文件大小（字节）
     */
    val size: Long,
    
    /**
     * 最后修改时间（毫秒时间戳）
     */
    val lastModified: Long,
    
    /**
     * 文件所在目录路径
     */
    val parentPath: String,
    
    /**
     * 文件名（不含扩展名）
     */
    val nameWithoutExtension: String
)