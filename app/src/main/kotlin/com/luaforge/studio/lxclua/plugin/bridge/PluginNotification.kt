package com.luaforge.studio.lxclua.plugin.bridge

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.luaforge.studio.lxclua.R
import com.luaforge.studio.lxclua.plugin.PluginManager
import com.luaforge.studio.lxclua.plugin.state.NotificationItem
import com.luaforge.studio.lxclua.plugin.state.NotificationState

class PluginNotification(
    private val context: Context,
    private val pluginId: String
) {

    companion object {
        private const val CHANNEL_ID_PLUGIN = "plugin_notifications"
        private var channelsInitialized = false

        @Synchronized
        fun ensureNotificationChannel(context: Context) {
            if (channelsInitialized) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val pluginChannel = NotificationChannel(
                CHANNEL_ID_PLUGIN,
                context.getString(R.string.plugin_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.plugin_notification_channel_desc)
                enableVibration(true)
            }
            manager.createNotificationChannel(pluginChannel)

            val infoChannel = NotificationChannel(
                "${CHANNEL_ID_PLUGIN}_info",
                context.getString(R.string.plugin_notification_info_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.plugin_notification_info_channel_desc)
            }
            manager.createNotificationChannel(infoChannel)

            val warnChannel = NotificationChannel(
                "${CHANNEL_ID_PLUGIN}_warning",
                context.getString(R.string.plugin_notification_warning_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.plugin_notification_warning_channel_desc)
            }
            manager.createNotificationChannel(warnChannel)

            val errorChannel = NotificationChannel(
                "${CHANNEL_ID_PLUGIN}_error",
                context.getString(R.string.plugin_notification_error_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.plugin_notification_error_channel_desc)
            }
            manager.createNotificationChannel(errorChannel)

            val successChannel = NotificationChannel(
                "${CHANNEL_ID_PLUGIN}_success",
                context.getString(R.string.plugin_notification_success_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.plugin_notification_success_channel_desc)
            }
            manager.createNotificationChannel(successChannel)

            channelsInitialized = true
        }

        private fun getChannelIdForLevel(level: String): String {
            return when (level.lowercase()) {
                "warning", "warn" -> "${CHANNEL_ID_PLUGIN}_warning"
                "error" -> "${CHANNEL_ID_PLUGIN}_error"
                "success" -> "${CHANNEL_ID_PLUGIN}_success"
                else -> "${CHANNEL_ID_PLUGIN}_info"
            }
        }

        private fun getLevelIconRes(level: String): Int {
            return when (level.lowercase()) {
                "warning", "warn" -> android.R.drawable.ic_dialog_alert
                "error" -> android.R.drawable.ic_dialog_alert
                "success" -> android.R.drawable.ic_dialog_info
                else -> android.R.drawable.ic_dialog_info
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    private var lastSystemNotificationId = 1000

    fun show(title: String, message: String, level: String) {
        show(title, message, level, null, 0)
    }

    fun show(title: String, message: String, level: String, groupKey: String?) {
        show(title, message, level, groupKey, 0)
    }

    fun show(title: String, message: String, level: String, groupKey: String?, priority: Int): NotificationItem {
        val item = NotificationState.addNotification(
            pluginId = pluginId,
            title = title,
            message = message,
            level = level,
            groupKey = groupKey,
            priority = priority
        )
        return item
    }

    fun showSystem(title: String, message: String, level: String) {
        handler.post {
            ensureNotificationChannel(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    PluginManager.currentActivity?.let { activity ->
                        androidx.core.app.ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            9001
                        )
                    }
                    return@post
                }
            }

            val channelId = getChannelIdForLevel(level)
            val notifId = lastSystemNotificationId++

            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val pendingIntent = if (intent != null) {
                PendingIntent.getActivity(
                    context, notifId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                null
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(getLevelIconRes(level))
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_STATUS)

            val notifManager = NotificationManagerCompat.from(context)
            notifManager.notify(notifId, builder.build())
        }
    }

    fun clear() {
        NotificationState.clearPlugin(pluginId)
    }

    fun clearGroup(groupKey: String?) {
        if (groupKey != null) {
            NotificationState.clearGroup(groupKey)
        }
    }

    fun dismiss(id: String) {
        NotificationState.dismissNotification(id)
    }

    fun getCount(): Int {
        return NotificationState.getActiveNotifications()
            .count { it.pluginId == pluginId }
    }

    fun clearAll() {
        handler.post {
            NotificationState.clearAll()
        }
    }
}