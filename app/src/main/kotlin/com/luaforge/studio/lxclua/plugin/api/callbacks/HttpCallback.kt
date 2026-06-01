package com.luaforge.studio.lxclua.plugin.api.callbacks

interface HttpCallback {
    fun onResult(success: Boolean, response: String?, error: String?)
}