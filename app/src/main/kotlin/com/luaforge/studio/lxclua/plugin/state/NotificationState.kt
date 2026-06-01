package com.luaforge.studio.lxclua.plugin.state

import androidx.compose.runtime.mutableStateListOf
import java.util.UUID

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val pluginId: String,
    val title: String,
    val message: String,
    val level: String = "info",
    val groupKey: String? = null,
    val priority: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

object NotificationState {

    val notifications = mutableStateListOf<NotificationItem>()

    fun addNotification(
        pluginId: String,
        title: String,
        message: String,
        level: String,
        groupKey: String?,
        priority: Int
    ): NotificationItem {
        val item = NotificationItem(
            pluginId = pluginId,
            title = title,
            message = message,
            level = level,
            groupKey = groupKey,
            priority = priority
        )
        val sortedIndex = notifications.indexOfFirst { it.priority < priority }
        if (sortedIndex >= 0) {
            notifications.add(sortedIndex, item)
        } else {
            notifications.add(item)
        }
        return item
    }

    fun dismissNotification(id: String) {
        notifications.removeAll { it.id == id }
    }

    fun clearAll() {
        notifications.clear()
    }

    fun clearGroup(groupKey: String) {
        notifications.removeAll { it.groupKey == groupKey }
    }

    fun clearPlugin(pluginId: String) {
        notifications.removeAll { it.pluginId == pluginId }
    }

    fun getActiveNotifications(): List<NotificationItem> {
        return notifications.toList()
    }

    fun getGroupedActiveNotifications(): Map<String?, List<NotificationItem>> {
        return notifications.groupBy { it.groupKey }
    }
}