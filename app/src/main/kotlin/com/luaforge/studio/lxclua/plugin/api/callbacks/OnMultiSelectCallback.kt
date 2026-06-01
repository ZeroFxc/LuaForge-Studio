package com.luaforge.studio.lxclua.plugin.api.callbacks

interface OnMultiSelectCallback {
    fun onConfirm(checkedItems: BooleanArray)
}