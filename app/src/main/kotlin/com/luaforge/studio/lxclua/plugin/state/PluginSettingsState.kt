package com.luaforge.studio.lxclua.plugin.state

import androidx.compose.runtime.mutableStateListOf

data class PluginSettingsSection(
    val key: String,
    val pluginId: String,
    val title: String,
    val priority: Int = 100
)

data class PluginSettingsItem(
    val key: String,
    val sectionKey: String,
    val pluginId: String,
    val title: String,
    val subtitle: String,
    val type: String,
    val initialValue: Boolean = false,
    val onChange: ((Boolean) -> Unit)? = null,
    val onClick: (() -> Unit)? = null
)

object PluginSettingsState {

    const val TYPE_SWITCH = "switch"
    const val TYPE_BUTTON = "button"

    val pluginSections = mutableStateListOf<PluginSettingsSection>()
    val pluginItems = mutableStateListOf<PluginSettingsItem>()

    fun addSection(section: PluginSettingsSection) {
        val existingIndex = pluginSections.indexOfFirst { it.key == section.key }
        if (existingIndex >= 0) {
            pluginSections[existingIndex] = section
        } else {
            pluginSections.add(section)
        }
    }

    fun addItem(item: PluginSettingsItem) {
        val existingIndex = pluginItems.indexOfFirst { it.key == item.key }
        if (existingIndex >= 0) {
            pluginItems[existingIndex] = item
        } else {
            pluginItems.add(item)
        }
    }

    fun removeSection(key: String) {
        pluginSections.removeAll { it.key == key }
    }

    fun removeItem(key: String) {
        pluginItems.removeAll { it.key == key }
    }

    fun clearPluginItems(pluginId: String) {
        pluginSections.removeAll { it.pluginId == pluginId }
        pluginItems.removeAll { it.pluginId == pluginId }
    }

    fun clearAll() {
        pluginSections.clear()
        pluginItems.clear()
    }

    fun getSortedSections(): List<PluginSettingsSection> {
        return pluginSections.sortedWith(compareBy({ it.priority }, { it.key }))
    }

    fun getItemsBySection(sectionKey: String): List<PluginSettingsItem> {
        return pluginItems.filter { it.sectionKey == sectionKey }
    }
}