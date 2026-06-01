package com.luaforge.studio.lxclua.ui.plugin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.luaforge.studio.lxclua.plugin.PluginManager

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
                Text("确定")
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
                Text("确定")
            }
        },
        dismissButton = {
            dialog.onCancel?.let { onCancel ->
                TextButton(onClick = onCancel) {
                    Text("取消")
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
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = dialog.onDismiss) {
                Text("取消")
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
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = dialog.onDismiss) {
                Text("取消")
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
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = dialog.onDismiss) {
                Text("取消")
            }
        }
    )
}
