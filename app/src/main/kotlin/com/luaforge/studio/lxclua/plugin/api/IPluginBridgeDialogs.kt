package com.luaforge.studio.lxclua.plugin.api

import com.luaforge.studio.lxclua.plugin.api.callbacks.OnInputCallback
import com.luaforge.studio.lxclua.plugin.api.callbacks.OnSelectCallback
import com.luaforge.studio.lxclua.plugin.api.callbacks.OnMultiSelectCallback

/**
 * 对话框相关 API
 */
interface IPluginBridgeDialogs {
    /**
     * 显示消息对话框
     */
    fun showMessage(title: String, message: String)
    
    /**
     * 显示确认对话框
     */
    fun showConfirm(title: String, message: String, onConfirm: Runnable, onCancel: Runnable?)
    
    /**
     * 显示输入对话框
     */
    fun showInputDialog(title: String, hint: String, defaultValue: String, onInput: OnInputCallback)
    
    /**
     * 显示单选列表对话框
     */
    fun showSingleChoiceDialog(title: String, items: Array<String>, selectedIndex: Int, onSelect: OnSelectCallback)
    
    /**
     * 显示多选列表对话框
     */
    fun showMultiChoiceDialog(title: String, items: Array<String>, checkedItems: BooleanArray, onConfirm: OnMultiSelectCallback)
    
    /**
     * 显示进度对话框
     */
    fun showProgressDialog(title: String, message: String): ProgressDialogHandle
}