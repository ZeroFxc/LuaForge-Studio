package com.luaforge.studio.lxclua.plugin.api.callbacks

interface DexLoadCallback {
    fun onLoad(success: Boolean, error: String?)
}