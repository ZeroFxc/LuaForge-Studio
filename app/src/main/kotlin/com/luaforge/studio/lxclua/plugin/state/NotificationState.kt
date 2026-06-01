package com.luaforge.studio.lxclua.plugin.state

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import java.util.UUID

/**
 * 通知级别枚举
 */
enum class NotificationLevel(val label: String, val priority: Int) {
    SUCCESS("success", 0),
    INFO("info", 1),
    WARN("warn", 2),
    ERROR("error", 3)
}

/**
 * 通知数据类
 * @param id 通知唯一标识
 * @param pluginId 发起通知的插件ID
 * @param title 通知标题
 * @param content 通知内容
 * @param level 通知级别
 * @param group 通知分组（可选，默认根据pluginId分组）
 * @param timestamp 创建时间戳
 * @param dismissed 是否已关闭
 * @param autoDismissMs 自动消失毫秒数（0表示不自动消失）
 */
data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val pluginId: String,
    val title: String,
    val content: String,
    val level: NotificationLevel = NotificationLevel.INFO,
    val group: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    var dismissed: Boolean = false,
    val autoDismissMs: Long = 4000L
)

/**
 * 通知系统状态管理器
 * 
 * 管理 IDE 内通知横幅的显示状态，支持按插件分组、优先级排序和自动消失
 */
object NotificationState {

    /**
     * 当前活跃的通知列表（未关闭的通知）
     */
    val activeNotifications = mutableStateListOf<NotificationItem>()

    /**
     * 通知历史记录（已关闭的通知）
     */
    val notificationHistory = mutableStateListOf<NotificationItem>()

    /**
     * 通知中心面板是否展开
     */
    val isNotificationPanelOpen = mutableStateOf(false)

    /**
     * 未读通知计数
     */
    val unreadCount = mutableStateOf(0)

    /**
     * 最大历史记录数
     */
    private const val MAX_HISTORY = 100

    /**
     * 新增一条通知
     * @param item 通知数据
     */
    fun addNotification(item: NotificationItem) {
        activeNotifications.add(item)
        unreadCount.value = unreadCount.value + 1
    }

    /**
     * 关闭指定通知（移入历史记录）
     * @param notificationId 通知ID
     */
    fun dismissNotification(notificationId: String) {
        val item = activeNotifications.find { it.id == notificationId } ?: return
        item.dismissed = true
        activeNotifications.removeAll { it.id == notificationId }
        notificationHistory.add(0, item)
        trimHistory()
        updateUnreadCount()
    }

    /**
     * 关闭指定插件的所有通知
     * @param pluginId 插件ID
     */
    fun dismissPluginNotifications(pluginId: String) {
        val items = activeNotifications.filter { it.pluginId == pluginId }
        items.forEach { it.dismissed = true }
        notificationHistory.addAll(0, items)
        activeNotifications.removeAll { it.pluginId == pluginId }
        trimHistory()
        updateUnreadCount()
    }

    /**
     * 关闭所有通知
     */
    fun dismissAllNotifications() {
        activeNotifications.forEach { it.dismissed = true }
        notificationHistory.addAll(0, activeNotifications.toList())
        activeNotifications.clear()
        trimHistory()
        unreadCount.value = 0
    }

    /**
     * 清除指定插件的通知历史
     * @param pluginId 插件ID
     */
    fun clearPluginHistory(pluginId: String) {
        notificationHistory.removeAll { it.pluginId == pluginId }
    }

    /**
     * 清除所有通知历史
     */
    fun clearAllHistory() {
        notificationHistory.clear()
        unreadCount.value = 0
    }

    /**
     * 获取指定分组的通知列表
     * @param group 分组名称
     */
    fun getNotificationsByGroup(group: String): List<NotificationItem> {
        return activeNotifications.filter { it.group == group }
    }

    /**
     * 获取指定插件的通知列表
     * @param pluginId 插件ID
     */
    fun getNotificationsByPlugin(pluginId: String): List<NotificationItem> {
        return activeNotifications.filter { it.pluginId == pluginId }
    }

    /**
     * 获取活跃通知数量
     */
    fun getActiveCount(): Int = activeNotifications.size

    /**
     * 获取历史通知数量
     */
    fun getHistoryCount(): Int = notificationHistory.size

    /**
     * 获取指定插件的活跃通知数量
     * @param pluginId 插件ID
     */
    fun getPluginActiveCount(pluginId: String): Int {
        return activeNotifications.count { it.pluginId == pluginId }
    }

    private fun updateUnreadCount() {
        unreadCount.value = activeNotifications.size
    }

    private fun trimHistory() {
        while (notificationHistory.size > MAX_HISTORY) {
            notificationHistory.removeAt(notificationHistory.size - 1)
        }
    }
}