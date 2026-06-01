package com.luaforge.studio.lxclua.ui.plugin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.luaforge.studio.lxclua.plugin.state.NotificationItem
import com.luaforge.studio.lxclua.plugin.state.NotificationLevel
import com.luaforge.studio.lxclua.plugin.state.NotificationState
import kotlinx.coroutines.delay

/**
 * 通知横幅宿主组件
 * 
 * 在应用顶层渲染活跃通知横幅和通知中心入口按钮
 */
@Composable
fun NotificationBannerHost() {
    val activeNotifications = NotificationState.activeNotifications
    val isPanelOpen = NotificationState.isNotificationPanelOpen.value
    val unreadCount = NotificationState.unreadCount.value

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            activeNotifications.take(3).forEach { notification ->
                NotificationBannerItem(
                    item = notification,
                    onDismiss = {
                        NotificationState.dismissNotification(notification.id)
                    }
                )
            }
        }

        if (isPanelOpen) {
            NotificationPanel(
                onDismiss = {
                    NotificationState.isNotificationPanelOpen.value = false
                }
            )
        }
    }
}

/**
 * 通知中心入口按钮
 */
@Composable
fun NotificationCenterButton(
    modifier: Modifier = Modifier
) {
    val unreadCount = NotificationState.unreadCount.value

    IconButton(
        onClick = {
            NotificationState.isNotificationPanelOpen.value =
                !NotificationState.isNotificationPanelOpen.value
        },
        modifier = modifier
    ) {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString()
                        )
                    }
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "通知中心"
            )
        }
    }
}

/**
 * 单条通知横幅
 * @param item 通知数据
 * @param onDismiss 关闭回调
 */
@Composable
fun NotificationBannerItem(
    item: NotificationItem,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(item.id) {
        if (item.autoDismissMs > 0) {
            delay(item.autoDismissMs)
            visible = false
            delay(300)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(300)),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(300))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = getLevelBackgroundColor(item.level)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = getLevelIcon(item.level),
                    contentDescription = item.level.label,
                    tint = getLevelIconColor(item.level),
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = getLevelTextColor(item.level),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.content.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = getLevelTextColor(item.level).copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        modifier = Modifier.size(16.dp),
                        tint = getLevelTextColor(item.level).copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * 通知中心面板（Popup 形式）
 * @param onDismiss 关闭面板回调
 */
@Composable
fun NotificationPanel(
    onDismiss: () -> Unit
) {
    val activeNotifications = NotificationState.activeNotifications
    val history = NotificationState.notificationHistory

    var showHistory by remember { mutableStateOf(false) }

    Popup(
        alignment = Alignment.TopEnd,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .width(340.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showHistory) "通知历史" else "通知中心",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        TextButton(
                            onClick = { showHistory = !showHistory }
                        ) {
                            Text(
                                text = if (showHistory) "活跃" else "历史",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        if (!showHistory && activeNotifications.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    NotificationState.dismissAllNotifications()
                                }
                            ) {
                                Text(
                                    text = "全部清除",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        if (showHistory && history.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    NotificationState.clearAllHistory()
                                }
                            ) {
                                Text(
                                    text = "清空历史",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                if (showHistory) {
                    if (history.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无历史通知",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(400.dp)
                        ) {
                            items(history.take(50)) { item ->
                                NotificationHistoryItem(item = item)
                                HorizontalDivider()
                            }
                        }
                    }
                } else {
                    if (activeNotifications.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无活跃通知",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(400.dp)
                        ) {
                            items(activeNotifications.toList()) { item ->
                                NotificationPanelItem(
                                    item = item,
                                    onDismiss = {
                                        NotificationState.dismissNotification(item.id)
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 通知面板中的单条通知项
 * @param item 通知数据
 * @param onDismiss 关闭回调
 */
@Composable
private fun NotificationPanelItem(
    item: NotificationItem,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDismiss() }
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = getLevelIcon(item.level),
            contentDescription = item.level.label,
            tint = getLevelIconColor(item.level),
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatTimestamp(item.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * 历史通知项
 * @param item 通知数据
 */
@Composable
private fun NotificationHistoryItem(item: NotificationItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = getLevelIcon(item.level),
            contentDescription = item.level.label,
            tint = getLevelIconColor(item.level).copy(alpha = 0.5f),
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (item.content.isNotEmpty()) {
                Text(
                    text = item.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = formatTimestamp(item.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * 获取通知级别对应图标
 * @param level 通知级别
 * @return ImageVector 图标
 */
private fun getLevelIcon(level: NotificationLevel): ImageVector {
    return when (level) {
        NotificationLevel.SUCCESS -> Icons.Default.CheckCircle
        NotificationLevel.INFO -> Icons.Default.Info
        NotificationLevel.WARN -> Icons.Default.Warning
        NotificationLevel.ERROR -> Icons.Default.Error
    }
}

/**
 * 获取通知级别对应图标颜色
 * @param level 通知级别
 * @return Color 颜色
 */
private fun getLevelIconColor(level: NotificationLevel): Color {
    return when (level) {
        NotificationLevel.SUCCESS -> Color(0xFF4CAF50)
        NotificationLevel.INFO -> Color(0xFF2196F3)
        NotificationLevel.WARN -> Color(0xFFFF9800)
        NotificationLevel.ERROR -> Color(0xFFF44336)
    }
}

/**
 * 获取通知级别对应文字颜色
 * @param level 通知级别
 * @return Color 颜色
 */
private fun getLevelTextColor(level: NotificationLevel): Color {
    return when (level) {
        NotificationLevel.SUCCESS -> Color(0xFF1B5E20)
        NotificationLevel.INFO -> Color(0xFF0D47A1)
        NotificationLevel.WARN -> Color(0xFFE65100)
        NotificationLevel.ERROR -> Color(0xFFB71C1C)
    }
}

/**
 * 获取通知级别对应背景颜色
 * @param level 通知级别
 * @return Color 颜色
 */
private fun getLevelBackgroundColor(level: NotificationLevel): Color {
    return when (level) {
        NotificationLevel.SUCCESS -> Color(0xFFE8F5E9)
        NotificationLevel.INFO -> Color(0xFFE3F2FD)
        NotificationLevel.WARN -> Color(0xFFFFF3E0)
        NotificationLevel.ERROR -> Color(0xFFFFEBEE)
    }
}

/**
 * 格式化时间戳为可读字符串
 * @param timestamp 毫秒时间戳
 * @return 格式化后的时间字符串
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
        else -> {
            val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }
}