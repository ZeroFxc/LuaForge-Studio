package com.luaforge.studio.lxclua.plugin.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luaforge.studio.lxclua.R
import com.luaforge.studio.lxclua.plugin.PluginManager
import com.luaforge.studio.lxclua.plugin.bridge.PluginManagerBridge
import com.luaforge.studio.lxclua.plugin.data.PluginInfo
import com.luaforge.studio.lxclua.plugin.data.TagInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 插件管理主界面
 *
 * 支持功能:
 * - 标签页分类过滤（全部/核心/普通/扩展/依赖）
 * - 单个插件卡片（展开/收起详情）
 * - 启用/禁用切换
 * - 卸载插件（含确认对话框）
 * - 导入插件按钮
 * - 长按显示操作菜单
 * - 双击事件处理
 *
 * @param onImportPlugin 导入插件回调
 * @param onOpenPluginWebUI 打开插件的 WebUI 回调（如果有），传入 pluginId
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PluginManagementScreen(
    onImportPlugin: (() -> Unit)? = null,
    onOpenPluginWebUI: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val managerBridge = remember { PluginManagerBridge() }

    // 插件列表（从 PluginManager 实时获取）
    val pluginList = remember {
        derivedStateOf { PluginManager.loadedPlugins.toList() }
    }

    // 获取完整信息列表（using loadedPlugins hashCode 作为 key，确保数据变更时刷新）
    val pluginInfoList = remember(pluginList.value.map { it.manifest.id to it.enabled.value }.hashCode()) {
        managerBridge.getAllPluginInfoList().toList()
    }

    // 选中的标签页
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.plugin_tab_all),
        stringResource(R.string.plugin_tab_core),
        stringResource(R.string.plugin_tab_normal),
        stringResource(R.string.plugin_tab_extension),
        stringResource(R.string.plugin_tab_dependent),
        stringResource(R.string.plugin_tab_disabled)
    )

    // 展开的插件 ID 集合
    var expandedPluginIds by remember { mutableStateOf(setOf<String>()) }

    // 确认卸载对话框
    var uninstallTargetId by remember { mutableStateOf<String?>(null) }

    // 长按操作菜单
    var contextMenuPluginId by remember { mutableStateOf<String?>(null) }

    // 按标签过滤
    val filteredPlugins = remember(pluginInfoList, selectedTab) {
        when (selectedTab) {
            0 -> pluginInfoList // 全部
            1 -> pluginInfoList.filter { it.category.equals("core", ignoreCase = true) }
            2 -> pluginInfoList.filter { it.category.equals("normal", ignoreCase = true) }
            3 -> pluginInfoList.filter { it.category.equals("extension", ignoreCase = true) }
            4 -> pluginInfoList.filter { it.category.equals("dependent", ignoreCase = true) }
            5 -> pluginInfoList.filter { !it.enabled }
            else -> pluginInfoList
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plugin_management_title), style = MaterialTheme.typography.titleLarge) },
                actions = {
                    // 导入插件按钮
                    if (onImportPlugin != null) {
                        IconButton(onClick = onImportPlugin) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.plugin_import_button))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ========== 标签页 ==========
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 8.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    val count = when (index) {
                        0 -> pluginInfoList.size
                        1 -> pluginInfoList.count { it.category.equals("core", ignoreCase = true) }
                        2 -> pluginInfoList.count { it.category.equals("normal", ignoreCase = true) }
                        3 -> pluginInfoList.count { it.category.equals("extension", ignoreCase = true) }
                        4 -> pluginInfoList.count { it.category.equals("dependent", ignoreCase = true) }
                        5 -> pluginInfoList.count { !it.enabled }
                        else -> 0
                    }
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                "$title ($count)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ========== 统计信息栏 ==========
            PluginStatsBar(pluginInfoList)

            // ========== 插件列表 ==========
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filteredPlugins,
                    key = { it.id }
                ) { pluginInfo ->
                    PluginCard(
                        pluginInfo = pluginInfo,
                        isExpanded = pluginInfo.id in expandedPluginIds,
                        onToggleExpand = {
                            expandedPluginIds = if (pluginInfo.id in expandedPluginIds) {
                                expandedPluginIds - pluginInfo.id
                            } else {
                                expandedPluginIds + pluginInfo.id
                            }
                        },
                        onToggleEnabled = { enabled ->
                            managerBridge.togglePlugin(pluginInfo.id, enabled)
                        },
                        onUninstall = {
                            uninstallTargetId = pluginInfo.id
                        },
                        onLongPress = {
                            contextMenuPluginId = pluginInfo.id
                        },
                        onDoubleClick = {
                            // 双击触发重载
                            managerBridge.reloadPlugin(pluginInfo.id)
                        },
                        onOpenWebUI = onOpenPluginWebUI?.let { cb ->
                            { cb(pluginInfo.id) }
                        }
                    )
                }
            }
        }
    }

    // ========== 卸载确认对话框 ==========
    uninstallTargetId?.let { pluginId ->
        val pluginInfo = pluginInfoList.find { it.id == pluginId }
        AlertDialog(
            onDismissRequest = { uninstallTargetId = null },
            title = { Text(stringResource(R.string.plugin_uninstall_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.plugin_uninstall_confirm, pluginInfo?.name ?: pluginId))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.plugin_uninstall_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (pluginInfo != null && pluginInfo.enabled) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.plugin_uninstall_enabled_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        managerBridge.uninstallPlugin(pluginId)
                        uninstallTargetId = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.plugin_uninstall_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { uninstallTargetId = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // ========== 长按操作菜单 ==========
    contextMenuPluginId?.let { pluginId ->
        val pluginInfo = pluginInfoList.find { it.id == pluginId }
        AlertDialog(
            onDismissRequest = { contextMenuPluginId = null },
            title = { Text(stringResource(R.string.plugin_context_menu_title, pluginInfo?.name ?: pluginId)) },
            text = {
                Column {
                    if (pluginInfo != null && pluginInfo.hasWebUI && onOpenPluginWebUI != null) {
                        TextButton(
                            onClick = {
                                onOpenPluginWebUI(pluginId)
                                contextMenuPluginId = null
                            }
                        ) {
                            Icon(Icons.Default.WebAsset, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.plugin_open_webui))
                        }
                    }
                    TextButton(
                        onClick = {
                            managerBridge.reloadPlugin(pluginId)
                            contextMenuPluginId = null
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.plugin_reload))
                    }
                    TextButton(
                        onClick = {
                            managerBridge.exportPluginToZip(context, pluginId, null)
                            contextMenuPluginId = null
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.plugin_export))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { contextMenuPluginId = null }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

// ==================== 辅助组件 ====================

/**
 * 插件统计信息栏
 */
@Composable
private fun PluginStatsBar(pluginInfoList: List<PluginInfo>) {
    val total = pluginInfoList.size
    val enabled = pluginInfoList.count { it.enabled }
    val core = pluginInfoList.count { it.category.equals("core", ignoreCase = true) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(stringResource(R.string.plugin_stats_total), "$total")
            StatItem(stringResource(R.string.plugin_stats_enabled), "$enabled")
            StatItem(stringResource(R.string.plugin_stats_core), "$core")
            StatItem(stringResource(R.string.plugin_stats_disabled), "${total - enabled}")
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * 单个插件卡片
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PluginCard(
    pluginInfo: PluginInfo,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onUninstall: () -> Unit,
    onLongPress: () -> Unit = {},
    onDoubleClick: () -> Unit = {},
    onOpenWebUI: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val categoryColor = when {
        pluginInfo.category.equals("core", ignoreCase = true) -> MaterialTheme.colorScheme.primary
        pluginInfo.category.equals("extension", ignoreCase = true) -> MaterialTheme.colorScheme.tertiary
        pluginInfo.category.equals("dependent", ignoreCase = true) -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    var clickCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .combinedClickable(
                onClick = {
                    // 单击：展开/收起详情
                    clickCount++
                    when {
                        clickCount == 1 -> {
                            scope.launch {
                                delay(300)
                                if (clickCount == 1) {
                                    onToggleExpand()
                                }
                                clickCount = 0
                            }
                        }
                        clickCount >= 2 -> {
                            // 双击：打开 WebUI（如果存在）或 重载插件
                            if (onOpenWebUI != null) {
                                onOpenWebUI.invoke()
                            } else {
                                onDoubleClick()
                            }
                            clickCount = 0
                        }
                    }
                    // 触发插件卡片点击事件，插件可监听 onPluginCardClick 自行处理
                    com.luaforge.studio.lxclua.plugin.state.EventManager.fireEvent(
                        com.luaforge.studio.lxclua.plugin.state.PluginEvents.ON_PLUGIN_CARD_CLICK, pluginInfo.id
                    )
                },
                onLongClick = {
                    onLongPress()
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (pluginInfo.enabled)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // ========== 头部：名称、版本、分类标识、开关 ==========
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 分类指示条
                Surface(
                    color = categoryColor,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .width(3.dp)
                        .height(36.dp)
                ) {}

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = pluginInfo.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // 分类标签
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = pluginInfo.category.uppercase(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(22.dp)
                        )
                    }
                    Text(
                        text = "v${pluginInfo.version} · ${pluginInfo.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 启用/禁用开关
                Switch(
                    checked = pluginInfo.enabled,
                    onCheckedChange = {
                        if (!pluginInfo.isCore) onToggleEnabled(it)
                    },
                    enabled = !pluginInfo.isCore
                )
            }

            // ========== 展开区域 ==========
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // 描述
                    if (pluginInfo.description.isNotEmpty()) {
                        Text(
                            text = pluginInfo.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 详细信息
                    InfoRow(stringResource(R.string.plugin_info_id), pluginInfo.id)
                    InfoRow(stringResource(R.string.plugin_info_type), pluginInfo.type)
                    InfoRow(stringResource(R.string.plugin_info_entry), pluginInfo.main)
                    InfoRow(stringResource(R.string.plugin_info_api_version), "${pluginInfo.apiVersion}")

                    if (pluginInfo.homepage != null) {
                        InfoRow(stringResource(R.string.plugin_info_homepage), pluginInfo.homepage)
                    }

                    // 依赖列表
                    if (pluginInfo.dependencies.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.plugin_deps_title, pluginInfo.dependencies.size),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        pluginInfo.dependencies.forEachIndexed { index, depId ->
                            val depInfo = pluginInfo.dependencyInfos.getOrNull(index)
                            val required = if (depInfo?.required == true)
                                stringResource(R.string.plugin_deps_required)
                            else
                                stringResource(R.string.plugin_deps_optional)
                            Text(
                                text = "  · $depId$required",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 被依赖列表
                    if (pluginInfo.dependents.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.plugin_dependents_title, pluginInfo.dependents.size),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        pluginInfo.dependents.forEach { depId ->
                            Text(
                                text = "  · $depId",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 标签
                    if (pluginInfo.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.plugin_tags_title, pluginInfo.tags.size),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TagFlowRow(tags = pluginInfo.tags)
                    }

                    // 操作按钮行
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        // WebUI 按钮
                        if (pluginInfo.hasWebUI && onOpenWebUI != null) {
                            FilledTonalButton(
                                onClick = { onOpenWebUI.invoke() },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(Icons.Default.WebAsset, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.plugin_card_webui_button))
                            }
                        }

                        // 重载按钮
                        FilledTonalButton(
                            onClick = onDoubleClick,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.plugin_card_reload_button))
                        }

                        // 卸载按钮（核心插件不显示）
                        if (!pluginInfo.isCore) {
                            FilledTonalButton(
                                onClick = onUninstall,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.plugin_card_uninstall_button))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 信息行组件
 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ==================== 标签相关组件 ====================

/** 预设标签颜色调色板 (文本色, 背景色) */
private val TAG_COLORS = listOf(
    Color(0xFF2196F3) to Color(0xFFBBDEFB), // 蓝
    Color(0xFF4CAF50) to Color(0xFFC8E6C9), // 绿
    Color(0xFFFF9800) to Color(0xFFFFE0B2), // 橙
    Color(0xFF9C27B0) to Color(0xFFE1BEE7), // 紫
    Color(0xFFF44336) to Color(0xFFFFCDD2), // 红
    Color(0xFF00BCD4) to Color(0xFFB2EBF2), // 青
    Color(0xFF795548) to Color(0xFFD7CCC8), // 棕
    Color(0xFF607D8B) to Color(0xFFCFD8DC), // 蓝灰
)

/**
 * 标签流式布局
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagFlowRow(tags: Array<TagInfo>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tags.forEach { tag ->
            TagChip(tag = tag)
        }
    }
}

/**
 * 单个标签芯片
 *
 * 支持:
 * - 纯文本（白色/自动颜色背景）
 * - 自定义颜色
 * - 图标 + 文本
 * - 纯图标
 */
@Composable
fun TagChip(tag: TagInfo) {
    val hexColor = tag.color
    val (textColor, bgColor) = if (hexColor != null) {
        val parsed = try {
            android.graphics.Color.parseColor(hexColor)
        } catch (e: Exception) { null }
        if (parsed != null) {
            Color(parsed) to Color(parsed).copy(alpha = 0.15f)
        } else {
            val idx = kotlin.math.abs(tag.name.hashCode()) % TAG_COLORS.size
            TAG_COLORS[idx]
        }
    } else {
        val idx = kotlin.math.abs(tag.name.hashCode()) % TAG_COLORS.size
        TAG_COLORS[idx]
    }

    // 加载图标
    val iconBitmap = tag.getIconAbsolutePath()?.let { path ->
        val file = java.io.File(path)
        if (file.exists()) {
            try {
                android.graphics.BitmapFactory.decodeFile(path)
            } catch (e: Exception) { null }
        } else null
    }

    Row(
        modifier = Modifier
            .background(
                color = bgColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap.asImageBitmap(),
                contentDescription = tag.name,
                modifier = Modifier.size(14.dp)
            )
            if (tag.name.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }

        // 文本
        if (tag.name.isNotEmpty()) {
            Text(
                text = tag.name,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}