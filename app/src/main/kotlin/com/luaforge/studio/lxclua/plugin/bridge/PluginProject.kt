package com.luaforge.studio.lxclua.plugin.bridge

import com.luaforge.studio.lxclua.plugin.PluginManager
import com.luaforge.studio.lxclua.plugin.api.IPluginBridgeProject
import com.luaforge.studio.lxclua.plugin.data.FileInfo
import java.io.File

/**
 * 项目操作 API
 * 
 * 使用方式: plugin.project.readFile("path")
 */
class PluginProject : IPluginBridgeProject {
    
    /**
     * 获取当前项目路径（兼容旧名）
     */
    fun getPath(): String? {
        return PluginManager.currentProjectPath.value
    }

    /**
     * 获取当前项目路径
     */
    override fun getProjectPath(): String? {
        return PluginManager.currentProjectPath.value
    }
    
    /**
     * 读取项目中的文件
     */
    override fun readFile(relativePath: String): String? {
        val projectPath = PluginManager.currentProjectPath.value ?: return null
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
    
    /**
     * 写入文件（覆盖）
     */
    override fun writeFile(relativePath: String, content: String): Boolean {
        return try {
            val projectPath = PluginManager.currentProjectPath.value ?: return false
            val file = File(projectPath, relativePath)
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 创建新文件
     */
    override fun createFile(relativePath: String, content: String): Boolean {
        return try {
            val projectPath = PluginManager.currentProjectPath.value ?: return false
            val file = File(projectPath, relativePath)
            if (file.exists()) return false
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 删除文件/目录
     */
    override fun deleteFile(relativePath: String): Boolean {
        return try {
            val projectPath = PluginManager.currentProjectPath.value ?: return false
            File(projectPath, relativePath).deleteRecursively()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查文件是否存在
     */
    override fun fileExists(relativePath: String): Boolean {
        return try {
            val projectPath = PluginManager.currentProjectPath.value ?: return false
            File(projectPath, relativePath).exists()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 创建目录
     */
    fun createDir(relativePath: String): Boolean {
        return createDirectory(relativePath)
    }

    /**
     * 创建目录
     */
    override fun createDirectory(relativePath: String): Boolean {
        return try {
            val projectPath = PluginManager.currentProjectPath.value ?: return false
            File(projectPath, relativePath).mkdirs()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 列出项目目录下的文件和子目录
     */
    override fun listFiles(relativePath: String): Array<String>? {
        return try {
            val projectPath = PluginManager.currentProjectPath.value ?: return null
            val dir = if (relativePath.isEmpty()) File(projectPath) else File(projectPath, relativePath)
            dir.list()?.toList()?.toTypedArray()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 获取文件信息
     */
    fun getFileInfo(filePath: String): FileInfo? {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                val extension = file.extension.takeIf { it.isNotEmpty() } ?: ""
                val nameWithoutExtension = if (extension.isNotEmpty()) {
                    file.name.substring(0, file.name.length - extension.length - 1)
                } else {
                    file.name
                }
                FileInfo(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    extension = extension,
                    size = if (file.isFile) file.length() else 0,
                    lastModified = file.lastModified(),
                    parentPath = file.parent ?: "",
                    nameWithoutExtension = nameWithoutExtension
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}