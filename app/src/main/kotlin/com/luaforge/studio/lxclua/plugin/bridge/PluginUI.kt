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
}