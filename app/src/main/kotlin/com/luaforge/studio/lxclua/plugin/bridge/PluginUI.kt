package com.luaforge.studio.lxclua.plugin.bridge

import android.os.Handler
import android.os.Looper
import com.luaforge.studio.lxclua.plugin.PluginManager
import com.luaforge.studio.lxclua.plugin.api.callbacks.OnInputCallback
import com.luaforge.studio.lxclua.plugin.api.callbacks.OnMultiSelectCallback
import com.luaforge.studio.lxclua.plugin.api.callbacks.OnSelectCallback

/**
 * UI 对话框操作 API
 * 
 * 使用方式: plugin.ui.showMessage("title", "message")
 */
class PluginUI {
    
    private val handler = Handler(Looper.getMainLooper())
    
    /**
     * 显示消息对话框
     */
    fun showMessage(title: String, message: String) {
        handler.post {
            PluginManager.currentDialog.value = PluginManager.DialogState.Message(
                title = title,
                message = message,
                onDismiss = { PluginManager.currentDialog.value = null }
            )
        }
    }
    
    /**
     * 显示确认对话框
     */
    fun showConfirm(title: String, message: String, onConfirm: Runnable, onCancel: Runnable?) {
        handler.post {
            PluginManager.currentDialog.value = PluginManager.DialogState.Confirm(
                title = title,
                message = message,
                onConfirm = {
                    PluginManager.currentDialog.value = null
                    onConfirm.run()
                },
                onCancel = onCancel?.let {
                    {
                        PluginManager.currentDialog.value = null
                        it.run()
                    }
                },
                onDismiss = { PluginManager.currentDialog.value = null }
            )
        }
    }
    
    /**
     * 显示输入对话框
     */
    fun showInputDialog(title: String, hint: String, defaultValue: String, onInput: OnInputCallback) {
        handler.post {
            PluginManager.currentDialog.value = PluginManager.DialogState.Input(
                title = title,
                hint = hint,
                defaultValue = defaultValue,
                onInput = { text ->
                    PluginManager.currentDialog.value = null
                    onInput.onInput(text)
                },
                onDismiss = { PluginManager.currentDialog.value = null }
            )
        }
    }
    
    /**
     * 显示单选对话框
     */
    fun showSingleChoiceDialog(title: String, items: Array<String>, selectedIndex: Int, onSelect: OnSelectCallback) {
        handler.post {
            PluginManager.currentDialog.value = PluginManager.DialogState.SingleChoice(
                title = title,
                items = items,
                selectedIndex = selectedIndex,
                onSelect = { index ->
                    PluginManager.currentDialog.value = null
                    onSelect.onSelect(index)
                },
                onDismiss = { PluginManager.currentDialog.value = null }
            )
        }
    }
    
    /**
     * 显示多选对话框
     */
    fun showMultiChoiceDialog(title: String, items: Array<String>, checkedItems: BooleanArray, onConfirm: OnMultiSelectCallback) {
        handler.post {
            PluginManager.currentDialog.value = PluginManager.DialogState.MultiChoice(
                title = title,
                items = items,
                checkedItems = checkedItems.copyOf(),
                onConfirm = { result ->
                    PluginManager.currentDialog.value = null
                    onConfirm.onConfirm(result)
                },
                onDismiss = { PluginManager.currentDialog.value = null }
            )
        }
    }

    /**
     * 显示文件列表对话框
     */
    fun showFileListDialog(title: String, directoryPath: String, filter: String?, onSelect: OnInputCallback) {
        handler.post {
            PluginManager.currentDialog.value = PluginManager.DialogState.FileList(
                title = title,
                directoryPath = directoryPath,
                filter = filter,
                onSelect = { path ->
                    PluginManager.currentDialog.value = null
                    onSelect.onInput(path)
                },
                onDismiss = { PluginManager.currentDialog.value = null }
            )
        }
    }

    /**
     * 显示图片对话框
     */
    fun showImageDialog(title: String, imagePath: String) {
        handler.post {
            PluginManager.currentDialog.value = PluginManager.DialogState.ImageDisplay(
                title = title,
                imagePath = imagePath,
                onDismiss = { PluginManager.currentDialog.value = null }
            )
        }
    }

    /**
     * 显示文本展示对话框
     */
    fun showTextDialog(title: String, text: String) {
        handler.post {
            PluginManager.currentDialog.value = PluginManager.DialogState.TextDisplay(
                title = title,
                text = text,
                onDismiss = { PluginManager.currentDialog.value = null }
            )
        }
    }

    /**
     * 显示复选框对话框
     */
    fun showCheckboxDialog(title: String, message: String, checked: Boolean, onConfirm: OnSelectCallback) {
        handler.post {
            PluginManager.currentDialog.value = PluginManager.DialogState.Checkbox(
                title = title,
                message = message,
                checked = checked,
                onConfirm = { result ->
                    PluginManager.currentDialog.value = null
                    onConfirm.onSelect(if (result) 1 else 0)
                },
                onDismiss = { PluginManager.currentDialog.value = null }
            )
        }
    }

    // ==================== 底部面板扩展 ====================

    /**
     * 添加编辑器底部面板项（从 Lua Table 列表转换）
     * @param key 唯一标识
     * @param title 面板标签标题
     * @param elements Lua Table 列表，每个元素是 Table: {type="text", value="..."} 或 {type="button", id="...", value="..."}
     * @param onEvent 事件回调，当按钮被点击时调用
     */
    fun addBottomPanelItem(pluginId: String, key: String, title: String, elements: List<Map<String, Any?>>, onEvent: Runnable?) {
        val converted = elements.map { raw ->
            PluginManager.BottomPanelElement(
                type = raw["type"] as? String ?: "text",
                id = raw["id"] as? String,
                value = raw["value"] as? String,
                height = (raw["height"] as? Number)?.toFloat() ?: 0f
            )
        }
        handler.post {
            val item = PluginManager.BottomPanelItem(
                pluginId = pluginId,
                key = key,
                title = title,
                elements = converted,
                onEvent = onEvent
            )
            val existingIndex = PluginManager.bottomPanelItems.indexOfFirst { it.key == key }
            if (existingIndex >= 0) {
                PluginManager.bottomPanelItems[existingIndex] = item
            } else {
                PluginManager.bottomPanelItems.add(item)
            }
            if (PluginManager.activeBottomPanelKey.value == null) {
                PluginManager.activeBottomPanelKey.value = key
            }
        }
    }

    /**
     * 移除底部面板项
     */
    fun removeBottomPanelItem(key: String) {
        handler.post {
            PluginManager.bottomPanelItems.removeAll { it.key == key }
            if (PluginManager.activeBottomPanelKey.value == key) {
                PluginManager.activeBottomPanelKey.value = PluginManager.bottomPanelItems.firstOrNull()?.key
            }
        }
    }

    /**
     * 清空插件的底部面板项
     */
    fun clearBottomPanelItems(pluginId: String) {
        handler.post {
            PluginManager.bottomPanelItems.removeAll { it.pluginId == pluginId }
            if (PluginManager.bottomPanelItems.none { it.key == PluginManager.activeBottomPanelKey.value }) {
                PluginManager.activeBottomPanelKey.value = PluginManager.bottomPanelItems.firstOrNull()?.key
            }
        }
    }
}