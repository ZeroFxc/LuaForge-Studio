package com.luaforge.studio.lxclua.ui.plugin

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luaforge.studio.lxclua.plugin.state.NotificationItem
import com.luaforge.studio.lxclua.plugin.state.NotificationState

@Composable
fun PluginNotificationBanner() {
    val notifications = NotificationState.notifications

    if (notifications.isEmpty()) return

    val grouped = notifications.groupBy { it.groupKey ?: it.id }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            grouped.values.forEach { groupItems ->
                val firstItem = groupItems.first()
                val displayCount = groupItems.size

                if (displayCount == 1) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(300)) +
                                expandVertically(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(200)) +
                                shrinkVertically(animationSpec = tween(200))
                    ) {
                        NotificationBannerItem(
                            item = firstItem,
                            onDismiss = { NotificationState.dismissNotification(firstItem.id) }
                        )
                    }
                } else {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(300)) +
                                expandVertically(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(200)) +
                                shrinkVertically(animationSpec = tween(200))
                    ) {
                        NotificationGroupItem(
                            groupKey = firstItem.groupKey ?: firstItem.id,
                            items = groupItems,
                            onDismiss = {
                                groupItems.forEach { NotificationState.dismissNotification(it.id) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotificationBannerItem(
    item: NotificationItem,
    onDismiss: () -> Unit
) {
    val (icon, backgroundColor, textColor, iconTint) = getLevelAppearance(item.level)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onDismiss() },
                    onLongClick = { onDismiss() }
                )
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = item.level,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { onDismiss() },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭",
                    tint = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun NotificationGroupItem(
    groupKey: String,
    items: List<NotificationItem>,
    onDismiss: () -> Unit
) {
    val expanded = remember { mutableStateOf(false) }
    val firstItem = items.first()
    val (icon, backgroundColor, textColor, iconTint) = getLevelAppearance(firstItem.level)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { expanded.value = !expanded.value },
                        onLongClick = { onDismiss() }
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = firstItem.level,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${firstItem.title}（共 ${items.size} 条）",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (expanded.value) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded.value) "收起" else "展开",
                    tint = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { onDismiss() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭全部",
                        tint = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded.value,
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 42.dp, end = 12.dp, bottom = 8.dp)
                ) {
                    items.forEach { subItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "\u2022",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = subItem.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (subItem.message.isNotEmpty()) {
                                    Text(
                                        text = subItem.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textColor.copy(alpha = 0.7f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class LevelAppearance(
    val icon: ImageVector,
    val backgroundColor: Color,
    val textColor: Color,
    val iconTint: Color
)

private fun getLevelAppearance(level: String): LevelAppearance {
    return when (level.lowercase()) {
        "error" -> LevelAppearance(
            icon = Icons.Filled.Error,
            backgroundColor = Color(0xFFFDE8E8),
            textColor = Color(0xFF991B1B),
            iconTint = Color(0xFFDC2626)
        )
        "warning", "warn" -> LevelAppearance(
            icon = Icons.Filled.Warning,
            backgroundColor = Color(0xFFFEF3C7),
            textColor = Color(0xFF92400E),
            iconTint = Color(0xFFF59E0B)
        )
        "success" -> LevelAppearance(
            icon = Icons.Filled.CheckCircle,
            backgroundColor = Color(0xFFD1FAE5),
            textColor = Color(0xFF065F46),
            iconTint = Color(0xFF10B981)
        )
        "info" -> LevelAppearance(
            icon = Icons.Filled.Info,
            backgroundColor = Color(0xFFDBEAFE),
            textColor = Color(0xFF1E40AF),
            iconTint = Color(0xFF3B82F6)
        )
        else -> LevelAppearance(
            icon = Icons.Filled.Notifications,
            backgroundColor = Color(0xFFF3F4F6),
            textColor = Color(0xFF374151),
            iconTint = Color(0xFF6B7280)
        )
    }
}