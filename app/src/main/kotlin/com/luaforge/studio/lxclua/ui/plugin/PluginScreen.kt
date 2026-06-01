package com.luaforge.studio.lxclua.ui.plugin

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luaforge.studio.lxclua.plugin.PluginManager
import com.luaforge.studio.lxclua.plugin.LoadedPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PluginScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val plugins = PluginManager.loadedPlugins

    var showDeleteConfirmDialog by remember { mutableStateOf<LoadedPlugin?>(null) }
    var isInstalling by remember { mutableStateOf(false) }

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isInstalling = true
            scope.launch(Dispatchers.IO) {
                val tempZip = copyUriToCache(context, uri)
                if (tempZip != null && tempZip.exists()) {
                    val result = PluginManager.installPluginFromZip(context, tempZip)
                    withContext(Dispatchers.Main) {
                        isInstalling = false
                        tempZip.delete()
                        if (result.isSuccess) {
                            Toast.makeText(context, "插件 ${result.getOrNull()?.name} 安装成功！", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "安装失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isInstalling = false
                        Toast.makeText(context, "读取 ZIP 文件失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (plugins.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "未发现任何插件",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 16.sp
                    )
                    TextButton(onClick = { zipPickerLauncher.launch("application/zip") }) {
                        Text("点击导入 .zip 插件包")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(plugins, key = { "${it.manifest.id}_${it.enabled}" }) { plugin ->
                    PluginItemCard(
                        key = plugin.manifest.id,
                        plugin = plugin,
                        onToggle = { enabled ->
                            PluginManager.togglePlugin(context, plugin.manifest.id, enabled)
                        },
                        onDelete = {
                            showDeleteConfirmDialog = plugin
                        }
                    )
                }
            }
        }

        if (isInstalling) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("正在安装插件，请稍候...")
                    }
                }
            }
        }

        showDeleteConfirmDialog?.let { plugin ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = null },
                title = { Text("确认删除插件") },
                text = { Text("确定要彻底删除插件 [${plugin.manifest.name}] 吗？此操作无法撤销。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            PluginManager.deletePlugin(context, plugin.manifest.id)
                            showDeleteConfirmDialog = null
                            Toast.makeText(context, "插件已删除", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = null }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
fun PluginItemCard(
    key: String,
    plugin: LoadedPlugin,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = plugin.manifest.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        val typeText = plugin.manifest.type.uppercase()
                        val badgeColor = when (plugin.manifest.type.lowercase()) {
                            "lua" -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                        val badgeTextColor = when (plugin.manifest.type.lowercase()) {
                            "lua" -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = typeText,
                                fontSize = 10.sp,
                                color = badgeTextColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "v${plugin.manifest.version} · 作者: ${plugin.manifest.author}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = plugin.enabled,
                        onCheckedChange = onToggle
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除插件",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (plugin.manifest.description.isNotEmpty()) {
                Text(
                    text = plugin.manifest.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun copyUriToCache(context: Context, uri: Uri): File? {
    return try {
        val cacheFile = File(context.cacheDir, "plugin_import_${System.currentTimeMillis()}.zip")
        context.contentResolver.openInputStream(uri)?.use { input ->
            cacheFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        cacheFile
    } catch (e: Exception) {
        null
    }
}
