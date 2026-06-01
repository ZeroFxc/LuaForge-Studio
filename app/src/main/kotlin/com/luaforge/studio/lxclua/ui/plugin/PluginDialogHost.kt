package com.luaforge.studio.lxclua.ui.plugin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.luaforge.studio.lxclua.plugin.PluginManager
import com.luaforge.studio.lxclua.R
import java.io.File

/**
 * 插件对话框 Compose 组件
 * 在 MainActivity 中调用此组件来渲染插件触发的对话框
 */
@Composable
fun PluginDialogHost() {
    val dialogState = PluginManager.currentDialog.value
    
    when (val dialog = dialogState) {
        is PluginManager.DialogState.Message -> {
            MessageDialog(dialog)
        }
        is PluginManager.DialogState.Confirm -> {
            ConfirmDialog(dialog)
        }
        is PluginManager.DialogState.Input -> {
            InputDialog(dialog)
        }
        is PluginManager.DialogState.SingleChoice -> {
            SingleChoiceDialog(dialog)
        }
        is PluginManager.DialogState.MultiChoice -> {
            MultiChoiceDialog(dialog)
        }
        is PluginManager.DialogState.FileList -> {
            FileListDialog(dialog)
        }
        is PluginManager.DialogState.ImageDisplay -> {
            ImageDisplayDialog(dialog)
        }
        is PluginManager.DialogState.TextDisplay -> {
            TextDisplayDialog(dialog)
        }
        is PluginManager.DialogState.Checkbox -> {
            CheckboxDialog(dialog)
        }
        null -> {}
    }
}

@Composable
private fun MessageDialog(dialog: PluginManager.DialogState.Message) {
    AlertDialog(
        onDismissRequest = dialog.onDismiss,
        title = { Text(dialog.title) },
        text = { Text(dialog.message) },
        confirmButton = {
            TextButton(onClick = dialog.onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@Composable
private fun ConfirmDialog(dialog: PluginManager.DialogState.Confirm) {
    AlertDialog(
        onDismissRequest = dialog.onDismiss,
        title = { Text(dialog.title) },
        text = { Text(dialog.message) },
        confirmButton = {
            TextButton(onClick = dialog.onConfirm) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            dialog.onCancel?.let { onCancel ->
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}

@Composable
private fun InputDialog(dialog: PluginManager.DialogState.Input) {
    var text by remember { mutableStateOf(dialog.defaultValue) }
    
    AlertDialog(
        onDismissRequest = dialog.onDismiss,
        title = { Text(dialog.title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(dialog.hint) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { dialog.onInput(text) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = dialog.onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun SingleChoiceDialog(dialog: PluginManager.DialogState.SingleChoice) {
    var selectedIndex by remember { mutableIntStateOf(dialog.selectedIndex) }
    
    AlertDialog(
        onDismissRequest = dialog.onDismiss,
        title = { Text(dialog.title) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                dialog.items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedIndex == index,
                                onClick = { selectedIndex = index }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedIndex == index,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(item)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { dialog.onSelect(selectedIndex) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = dialog.onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun MultiChoiceDialog(dialog: PluginManager.DialogState.MultiChoice) {
    val checkedStates = remember { mutableStateListOf(*dialog.checkedItems.toTypedArray()) }
    
    AlertDialog(
        onDismissRequest = dialog.onDismiss,
        title = { Text(dialog.title) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                dialog.items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checkedStates[index],
                            onCheckedChange = { checkedStates[index] = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(item)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { dialog.onConfirm(checkedStates.toBooleanArray()) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = dialog.onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun FileListDialog(dialog: PluginManager.DialogState.FileList) {
    val dir = remember(dialog.directoryPath) { File(dialog.directoryPath) }
    val files = remember(dir) {
        dir.listFiles()
            ?.filter { file ->
                if (dialog.filter != null) {
                    file.name.contains(dialog.filter, ignoreCase = true) ||
                    file.extension.equals(dialog.filter, ignoreCase = true)
                } else true
            }
            ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name })
            ?: emptyList()
    }

    AlertDialog(
        onDismissRequest = dialog.onDismiss,
        title = { Text(dialog.title) },
        text = {
            if (files.isEmpty()) {
                Text(
                    text = stringResource(R.string.plugin_no_files),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    itemsIndexed(files) { _, file ->
                        val icon = if (file.isDirectory) "\uD83D\uDCC1" else "\uD83D\uDCC4"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dialog.onSelect(file.absolutePath) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(icon, style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (!file.isDirectory) {
                                Text(
                                    text = formatFileSize(file.length()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = dialog.onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}

@Composable
private fun ImageDisplayDialog(dialog: PluginManager.DialogState.ImageDisplay) {
    AlertDialog(
        onDismissRequest = dialog.onDismiss,
        title = { Text(dialog.title) },
        text = {
            AsyncImage(
                model = dialog.imagePath,
                contentDescription = dialog.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                contentScale = ContentScale.Fit
            )
        },
        confirmButton = {
            TextButton(onClick = dialog.onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@Composable
private fun TextDisplayDialog(dialog: PluginManager.DialogState.TextDisplay) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = dialog.onDismiss,
        title = { Text(dialog.title) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = dialog.text,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = dialog.onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@Composable
private fun CheckboxDialog(dialog: PluginManager.DialogState.Checkbox) {
    var checked by remember { mutableStateOf(dialog.checked) }

    AlertDialog(
        onDismissRequest = dialog.onDismiss,
        title = { Text(dialog.title) },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { checked = !checked }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { checked = it }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = dialog.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { dialog.onConfirm(checked) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = dialog.onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
