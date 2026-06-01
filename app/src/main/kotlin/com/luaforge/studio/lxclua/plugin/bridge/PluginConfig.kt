package com.luaforge.studio.lxclua.plugin.bridge

import android.content.Context

/**
 * 配置存储 API
 * 
 * 使用方式: plugin.config.getInt("key", 0)
 */
class PluginConfig(private val context: Context, private val pluginId: String) {
    
    private val prefsName = "plugin_config_$pluginId"
    
    fun getString(key: String, defaultValue: String?): String? {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getString(key, defaultValue)
    }
    
    fun setString(key: String, value: String?) {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString(key, value)
            .apply()
    }
    
    fun getInt(key: String, defaultValue: Int): Int {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getInt(key, defaultValue)
    }
    
    fun setInt(key: String, value: Int) {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putInt(key, value)
            .apply()
    }
    
    fun getLong(key: String, defaultValue: Long): Long {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getLong(key, defaultValue)
    }
    
    fun setLong(key: String, value: Long) {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putLong(key, value)
            .apply()
    }
    
    fun getFloat(key: String, defaultValue: Float): Float {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getFloat(key, defaultValue)
    }
    
    fun setFloat(key: String, value: Float) {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putFloat(key, value)
            .apply()
    }
    
    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .getBoolean(key, defaultValue)
    }
    
    fun setBoolean(key: String, value: Boolean) {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key, value)
            .apply()
    }
    
    fun remove(key: String) {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .remove(key)
            .apply()
    }
    
    fun contains(key: String): Boolean {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .contains(key)
    }
    
    fun clear() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}