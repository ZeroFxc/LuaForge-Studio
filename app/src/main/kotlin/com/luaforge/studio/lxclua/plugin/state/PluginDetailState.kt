package com.luaforge.studio.lxclua.plugin.state

import androidx.compose.runtime.mutableStateListOf

data class PluginDetailItem(
    val key: String,
    val pluginId: String,
    val targetPluginId: String,
    val title: String,
    val subtitle: String,
    val type: String,
    val initialValue: Boolean = false,
    val onChange: ((Boolean) -> Unit)? = null,
    val onClick: (() -> Unit)? = null
)

object PluginDetailState {

    const val TYPE_SWITCH = "switch"
    const val TYPE_BUTTON = "button"
    const val TYPE_INFO = "info"

    val pluginItems = mutableStateListOf<PluginDetailItem>()

    fun addItem(item: PluginDetailItem) {
        val existingIndex = pluginItems.indexOfFirst { it.key == item.key }
        if (existingIndex >= 0) {
            pluginItems[existingIndex] = item
        } else {
            pluginItems.add(item)
        }
    }

    fun removeItem(key: String) {
        pluginItems.removeAll { it.key == key }
    }

    fun clearPluginItems(pluginId: String) {
        pluginItems.removeAll { it.pluginId == pluginId }
    }

    fun clearAll() {
        pluginItems.clear()
    }

    fun getItemsForPlugin(targetPluginId: String): List<PluginDetailItem> {
        return pluginItems.filter { it.targetPluginId == targetPluginId }
    }
}