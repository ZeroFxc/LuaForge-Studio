package com.luaforge.studio.lxclua.plugin.bridge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.luaforge.studio.lxclua.plugin.PluginManager
import com.luaforge.studio.lxclua.plugin.state.NotificationItem
import com.luaforge.studio.lxclua.plugin.state.NotificationLevel
import com.luaforge.studio.lxclua.plugin.state.NotificationState
import java.util.UUID

/**
 * 通知系统 API
 * 
 * 使用方式:
 *   plugin.notification.show("标题", "内容", "info")
 *   plugin.notification.info("标题", "内容")
 *   plugin.notification.success("操作成功", "文件已保存")
 *   plugin.notification.warn("警告", "内存不足")
 *   plugin.notification.error("错误", "编译失败")
 *   plugin.notification.clear("pluginId")
 *   plugin.notification.clearAll()
 */
class PluginNotification(
    private val context: Context,
    private val pluginId: String
) {

    private val handler = Handler(Looper.getMainLooper())
    private val channelId = "plugin_notification_$pluginId"

    companion object {
        private const val CHANNEL_NAME = "插件通知"
        private const val CHANNEL_DESC = "来自插件的系统通知"
        private var notificationIdCounter = 1000
    }

    init {
        createNotificationChannel()
    }

    /**
     * 创建 Android 通知渠道（Android 8.0+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "$CHANNEL_NAME - $pluginId",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * 显示通知（IDE 内横幅 + 系统通知栏）
     * @param title 通知标题
     * @param content 通知内容
     * @param level 通知级别: "info" | "success" | "warn" | "error"
     * @param group 通知分组（可选，同一分组的通知会被折叠）
     * @param autoDismissMs 自动消失毫秒数（默认4000，0表示不自动消失）
     * @param pushSystem 是否推送到系统通知栏（默认true）
     * @return 通知ID
     */
    fun show(
        title: String,
        content: String,
        level: String = "info",
        group: String = "",
        autoDismissMs: Long = 4000,
        pushSystem: Boolean = true
    ): String {
        val notificationLevel = parseLevel(level)
        val effectiveGroup = group.ifEmpty { pluginId }
        val notifId = UUID.randomUUID().toString()

        val item = NotificationItem(
            id = notifId,
            pluginId = pluginId,
            title = title,
            content = content,
            level = notificationLevel,
            group = effectiveGroup,
            autoDismissMs = autoDismissMs
        )

        handler.post {
            NotificationState.addNotification(item)
        }

        if (pushSystem) {
            pushSystemNotification(title, content, notificationLevel)
        }

        return notifId
    }

    /**
     * 显示 info 级别通知
     * @param title 通知标题
     * @param content 通知内容
     * @param group 通知分组
     * @return 通知ID
     */
    fun info(title: String, content: String, group: String = ""): String {
        return show(title, content, "info", group)
    }

    /**
     * 显示 success 级别通知
     * @param title 通知标题
     * @param content 通知内容
     * @param group 通知分组
     * @return 通知ID
     */
    fun success(title: String, content: String, group: String = ""): String {
        return show(title, content, "success", group)
    }

    /**
     * 显示 warn 级别通知
     * @param title 通知标题
     * @param content 通知内容
     * @param group 通知分组
     * @return 通知ID
     */
    fun warn(title: String, content: String, group: String = ""): String {
        return show(title, content, "warn", group)
    }

    /**
     * 显示 error 级别通知
     * @param title 通知标题
     * @param content 通知内容
     * @param group 通知分组
     * @return 通知ID
     */
    fun error(title: String, content: String, group: String = ""): String {
        return show(title, content, "error", group, autoDismissMs = 0)
    }

    /**
     * 关闭指定通知
     * @param notificationId 通知ID
     */
    fun dismiss(notificationId: String) {
        handler.post {
            NotificationState.dismissNotification(notificationId)
        }
    }

    /**
     * 关闭当前插件的所有通知
     */
    fun clear() {
        handler.post {
            NotificationState.dismissPluginNotifications(pluginId)
        }
    }

    /**
     * 关闭指定插件的所有通知
     * @param targetPluginId 目标插件ID
     */
    fun clearPlugin(targetPluginId: String) {
        handler.post {
            NotificationState.dismissPluginNotifications(targetPluginId)
        }
    }

    /**
     * 关闭所有活跃通知
     */
    fun clearAll() {
        handler.post {
            NotificationState.dismissAllNotifications()
        }
    }

    /**
     * 清除当前插件的历史通知
     */
    fun clearHistory() {
        NotificationState.clearPluginHistory(pluginId)
    }

    /**
     * 清除所有历史通知
     */
    fun clearAllHistory() {
        NotificationState.clearAllHistory()
    }

    /**
     * 获取当前活跃通知数量
     * @return 活跃通知总数
     */
    fun getActiveCount(): Int {
        return NotificationState.getActiveCount()
    }

    /**
     * 获取当前插件的活跃通知数量
     * @return 当前插件活跃通知数
     */
    fun getMyActiveCount(): Int {
        return NotificationState.getPluginActiveCount(pluginId)
    }

    /**
     * 获取指定插件的活跃通知数量
     * @param targetPluginId 目标插件ID
     * @return 插件活跃通知数
     */
    fun getPluginActiveCount(targetPluginId: String): Int {
        return NotificationState.getPluginActiveCount(targetPluginId)
    }

    /**
     * 获取历史通知数量
     * @return 历史通知总数
     */
    fun getHistoryCount(): Int {
        return NotificationState.getHistoryCount()
    }

    /**
     * 获取指定分组的通知数量
     * @param group 分组名称
     * @return 分组通知数
     */
    fun getGroupCount(group: String): Int {
        return NotificationState.getNotificationsByGroup(group).size
    }

    /**
     * 推送系统通知栏通知
     * @param title 通知标题
     * @param content 通知内容
     * @param level 通知级别
     */
    private fun pushSystemNotification(title: String, content: String, level: NotificationLevel) {
        try {
            val priority = when (level) {
                NotificationLevel.SUCCESS -> NotificationCompat.PRIORITY_DEFAULT
                NotificationLevel.INFO -> NotificationCompat.PRIORITY_DEFAULT
                NotificationLevel.WARN -> NotificationCompat.PRIORITY_HIGH
                NotificationLevel.ERROR -> NotificationCompat.PRIORITY_MAX
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(priority)
                .setAutoCancel(true)
                .setGroup(pluginId)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))

            val notificationId = notificationIdCounter++
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: Exception) {
            android.util.Log.e("PluginNotification", "推送系统通知失败", e)
        }
    }

    /**
     * 解析通知级别字符串
     * @param level 级别字符串
     * @return NotificationLevel 枚举值
     */
    private fun parseLevel(level: String): NotificationLevel {
        return when (level.lowercase()) {
            "success" -> NotificationLevel.SUCCESS
            "warn", "warning" -> NotificationLevel.WARN
            "error" -> NotificationLevel.ERROR
            else -> NotificationLevel.INFO
        }
    }
}