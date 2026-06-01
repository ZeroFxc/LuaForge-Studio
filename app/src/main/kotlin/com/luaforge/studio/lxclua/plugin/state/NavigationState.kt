package com.luaforge.studio.lxclua.plugin.state

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 侧滑栏菜单项数据类
 */
data class SidebarItem(
    val pluginId: String,
    val key: String,
    val label: String,
    val group: String,
    val iconName: String?,
    val onClick: Runnable
)

/**
 * 导航状态管理
 * 负责管理侧滑栏菜单项和页面导航
 */
object NavigationState {
    
    // 插件注册的侧滑栏菜单项（Compose 响应式列表，UI 直接读取以触发重组）
    val pluginSidebarItems = mutableStateListOf<SidebarItem>()
    
    // 侧滑栏状态回调
    private val sidebarStateListeners = CopyOnWriteArrayList<(Boolean) -> Unit>()
    
    // 当前侧滑栏状态（Compose 响应式，供 UI 直接观察）
    val sidebarOpen = mutableStateOf(false)
    
    // 当前导航页面
    private var currentPage = "editor"
    
    /**
     * 添加侧滑栏菜单项
     */
    fun addSidebarItem(pluginId: String, key: String, label: String, group: String, iconName: String?, onClick: Runnable) {
        val fullKey = "$pluginId:$key"
        
        // 移除已存在的同名项
        removeSidebarItem(pluginId, key)
        
        pluginSidebarItems.add(
            SidebarItem(
                pluginId = pluginId,
                key = fullKey,
                label = label,
                group = group,
                iconName = iconName,
                onClick = onClick
            )
        )
    }
    
    /**
     * 移除侧滑栏菜单项
     */
    fun removeSidebarItem(pluginId: String, key: String) {
        val fullKey = "$pluginId:$key"
        pluginSidebarItems.removeAll { it.pluginId == pluginId && it.key == fullKey }
    }
    
    /**
     * 清除指定插件的所有侧滑栏菜单项
     */
    fun clearPluginSidebarItems(pluginId: String) {
        pluginSidebarItems.removeAll { it.pluginId == pluginId }
    }
    
    /**
     * 获取所有插件注册的侧滑栏菜单项
     */
    fun getPluginSidebarItems(): List<SidebarItem> {
        return pluginSidebarItems.toList()
    }
    
    /**
     * 按分组获取侧滑栏菜单项
     */
    fun getSidebarItemsByGroup(group: String): List<SidebarItem> {
        return pluginSidebarItems.filter { it.group == group }
    }
    
    /**
     * 打开侧滑栏
     */
    fun openSidebar() {
        sidebarOpen.value = true
        notifySidebarStateChanged(true)
    }
    
    /**
     * 关闭侧滑栏
     */
    fun closeSidebar() {
        sidebarOpen.value = false
        notifySidebarStateChanged(false)
    }
    
    /**
     * 切换侧滑栏状态
     */
    fun toggleSidebar() {
        if (sidebarOpen.value) {
            closeSidebar()
        } else {
            openSidebar()
        }
    }
    
    /**
     * 检查侧滑栏是否打开
     */
    fun isSidebarOpen(): Boolean {
        return sidebarOpen.value
    }
    
    /**
     * 注册侧滑栏状态监听器
     */
    fun addSidebarStateListener(listener: (Boolean) -> Unit) {
        sidebarStateListeners.add(listener)
    }
    
    /**
     * 取消注册侧滑栏状态监听器
     */
    fun removeSidebarStateListener(listener: (Boolean) -> Unit) {
        sidebarStateListeners.remove(listener)
    }
    
    /**
     * 通知侧滑栏状态变化
     */
    private fun notifySidebarStateChanged(isOpen: Boolean) {
        sidebarStateListeners.forEach { it.invoke(isOpen) }
    }
    
    /**
     * 导航到指定页面
     */
    fun navigateTo(pageId: String) {
        currentPage = pageId
    }
    
    /**
     * 获取当前页面
     */
    fun getCurrentPage(): String {
        return currentPage
    }
    
    /**
     * 返回上一页
     */
    fun goBack() {
        currentPage = "editor"
    }
    
    /**
     * 清理指定插件的所有资源
     */
    fun cleanupPluginResources(pluginId: String) {
        clearPluginSidebarItems(pluginId)
    }
}
