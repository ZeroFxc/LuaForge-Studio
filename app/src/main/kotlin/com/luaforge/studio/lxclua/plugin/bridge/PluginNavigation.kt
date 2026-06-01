package com.luaforge.studio.lxclua.plugin.bridge

import com.luaforge.studio.lxclua.plugin.state.NavigationState

/**
 * 导航与侧滑栏扩展 API
 * 
 * 允许插件向应用侧滑栏添加自定义菜单项
 * 
 * 使用方式: plugin.nav.addSidebarItem("my_item", "我的功能", "settings", callback)
 */
class PluginNavigation(private val pluginId: String) {
    
    /**
     * 预定义的侧滑栏菜单项类型
     */
    companion object {
        const val TYPE_PROJECT = "project"
        const val TYPE_SETTINGS = "settings"
        const val TYPE_ABOUT = "about"
        const val TYPE_PLUGINS = "plugins"
        const val TYPE_CUSTOM = "custom"
    }
    
    /**
     * 向侧滑栏添加菜单项
     * @param key 菜单项唯一标识
     * @param label 显示文本
     * @param group 分组 (project/settings/about/plugins/custom)
     * @param iconName 图标名称 (可选)
     * @param onClick 点击回调
     */
    fun addSidebarItem(key: String, label: String, group: String = TYPE_CUSTOM, iconName: String? = null, onClick: Runnable) {
        NavigationState.addSidebarItem(pluginId, key, label, group, iconName, onClick)
    }
    
    /**
     * 从侧滑栏移除菜单项
     * @param key 菜单项唯一标识
     */
    fun removeSidebarItem(key: String) {
        NavigationState.removeSidebarItem(pluginId, key)
    }
    
    /**
     * 清除当前插件添加的所有侧滑栏菜单项
     */
    fun clearSidebarItems() {
        NavigationState.clearPluginSidebarItems(pluginId)
    }
    
    /**
     * 打开侧滑栏
     */
    fun openSidebar() {
        NavigationState.openSidebar()
    }
    
    /**
     * 关闭侧滑栏
     */
    fun closeSidebar() {
        NavigationState.closeSidebar()
    }
    
    /**
     * 切换侧滑栏状态
     */
    fun toggleSidebar() {
        NavigationState.toggleSidebar()
    }
    
    /**
     * 检查侧滑栏是否打开
     */
    fun isSidebarOpen(): Boolean {
        return NavigationState.isSidebarOpen()
    }
    
    /**
     * 获取所有侧滑栏菜单项分组
     */
    fun getSidebarGroups(): Array<String> {
        return arrayOf(TYPE_PROJECT, TYPE_SETTINGS, TYPE_ABOUT, TYPE_PLUGINS, TYPE_CUSTOM)
    }
    
    /**
     * 导航到指定页面
     * @param pageId 页面标识
     */
    fun navigateTo(pageId: String) {
        NavigationState.navigateTo(pageId)
    }
    
    /**
     * 返回上一页
     */
    fun goBack() {
        NavigationState.goBack()
    }
}
