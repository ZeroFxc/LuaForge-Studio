package com.luaforge.studio.lxclua.plugin.bridge

import android.os.Handler
import android.os.Looper
import com.luaforge.studio.lxclua.plugin.PluginManager

/**
 * 主页项目列表操作 API
 * 
 * 使用方式:
 *   plugin.mainpage.enterMultiSelectMode()
 *   plugin.mainpage.setProjectBadge("projId", "3个文件", 0xFF4CAF50)
 */
class PluginMainPage(private val pluginId: String) {
    
    private val handler = Handler(Looper.getMainLooper())
    
    // ============ 多选模式 ============
    
    fun enterMultiSelectMode() {
        handler.post { PluginManager.isMultiSelectMode.value = true }
    }
    
    fun exitMultiSelectMode() {
        handler.post {
            PluginManager.isMultiSelectMode.value = false
            PluginManager.multiSelectedProjectIds.clear()
        }
    }
    
    fun toggleProjectSelection(projectId: String) {
        handler.post {
            if (projectId in PluginManager.multiSelectedProjectIds) {
                PluginManager.multiSelectedProjectIds.remove(projectId)
            } else {
                PluginManager.multiSelectedProjectIds.add(projectId)
            }
        }
    }
    
    fun selectProject(projectId: String) {
        handler.post {
            if (projectId !in PluginManager.multiSelectedProjectIds) {
                PluginManager.multiSelectedProjectIds.add(projectId)
            }
        }
    }
    
    fun deselectProject(projectId: String) {
        handler.post { PluginManager.multiSelectedProjectIds.remove(projectId) }
    }
    
    fun getSelectedProjectIds(): Array<String> {
        return PluginManager.multiSelectedProjectIds.toTypedArray()
    }
    
    fun getSelectedCount(): Int {
        return PluginManager.multiSelectedProjectIds.size
    }
    
    fun isInMultiSelectMode(): Boolean {
        return PluginManager.isMultiSelectMode.value
    }
    
    fun clearSelection() {
        handler.post { PluginManager.multiSelectedProjectIds.clear() }
    }
    
    // ============ 项目徽章 ============
    
    /**
     * 给项目卡片设置徽章标签
     * @param projectId 项目ID
     * @param text 徽章文字
     * @param color 徽章颜色 (0xAARRGGBB 格式)
     */
    fun setProjectBadge(projectId: String, text: String, color: Long) {
        handler.post {
            PluginManager.projectBadges[projectId] = PluginManager.BadgeInfo(text, color)
        }
    }
    
    /**
     * 清除项目徽章
     */
    fun clearProjectBadge(projectId: String) {
        handler.post { PluginManager.projectBadges.remove(projectId) }
    }
    
    /**
     * 清除所有项目徽章
     */
    fun clearAllBadges() {
        handler.post { PluginManager.projectBadges.clear() }
    }
    
    // ============ 项目卡片上下文菜单扩展 ============
    
    /**
     * 向项目卡片的三点菜单中添加自定义菜单项
     * @param key 菜单项唯一标识
     * @param label 菜单项文字
     * @param onClick 点击回调，参数: (projectId, projectName, projectPath)
     */
    fun addProjectCardMenuItem(key: String, label: String, onClick: ProjectCardMenuCallback) {
        handler.post {
            PluginManager.projectCardMenuItems.add(
                PluginManager.ProjectCardMenuItem(key, label) { pid, pname, ppath ->
                    onClick.onItemClick(pid, pname, ppath)
                }
            )
        }
    }
    
    /**
     * 移除项目卡片菜单项
     */
    fun removeProjectCardMenuItem(key: String) {
        handler.post {
            PluginManager.projectCardMenuItems.removeIf { it.key == key }
        }
    }
    
    /**
     * 清除本插件添加的所有项目卡片菜单项
     */
    fun clearProjectCardMenuItems() {
        handler.post {
            PluginManager.projectCardMenuItems.clear()
        }
    }
    
    // ============ 项目列表数据 ============
    
    /**
     * 获取当前主页项目列表（返回项目ID数组）
     */
    fun getProjectIds(): Array<String> {
        return PluginManager.currentProjectItems.map { it.id }.toTypedArray()
    }
    
    /**
     * 根据ID获取项目名称
     */
    fun getProjectName(projectId: String): String? {
        return PluginManager.currentProjectItems.find { it.id == projectId }?.name
    }
    
    /**
     * 根据ID获取项目路径
     */
    fun getProjectPath(projectId: String): String? {
        return PluginManager.currentProjectItems.find { it.id == projectId }?.path
    }
    
    /**
     * 获取项目列表总数
     */
    fun getProjectCount(): Int {
        return PluginManager.currentProjectItems.size
    }
}

/**
 * 项目卡片菜单项点击回调接口
 * Lua 侧实现 onItemClick(projectId, projectName, projectPath)
 */
interface ProjectCardMenuCallback {
    fun onItemClick(projectId: String, projectName: String, projectPath: String)
}