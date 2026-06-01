package com.luaforge.studio.lxclua.plugin.bridge

import com.luaforge.studio.lxclua.plugin.state.PluginDetailItem
import com.luaforge.studio.lxclua.plugin.state.PluginDetailState

class PluginDetailBridge(private val pluginId: String) {

    companion object {
        const val TYPE_SWITCH = PluginDetailState.TYPE_SWITCH
        const val TYPE_BUTTON = PluginDetailState.TYPE_BUTTON
        const val TYPE_INFO = PluginDetailState.TYPE_INFO
    }

    fun addSwitch(
        key: String,
        title: String,
        subtitle: String,
        initialValue: Boolean = false,
        onChange: (Boolean) -> Unit
    ) {
        PluginDetailState.addItem(
            PluginDetailItem(
                key = "${pluginId}_$key",
                pluginId = pluginId,
                targetPluginId = pluginId,
                title = title,
                subtitle = subtitle,
                type = TYPE_SWITCH,
                initialValue = initialValue,
                onChange = onChange
            )
        )
    }

    fun addButton(
        key: String,
        title: String,
        subtitle: String,
        onClick: Runnable
    ) {
        PluginDetailState.addItem(
            PluginDetailItem(
                key = "${pluginId}_$key",
                pluginId = pluginId,
                targetPluginId = pluginId,
                title = title,
                subtitle = subtitle,
                type = TYPE_BUTTON,
                onClick = {
                    try {
                        onClick.run()
                    } catch (e: Exception) {
                        android.util.Log.e("PluginDetailBridge", "详情按钮回调执行失败: ${pluginId}_$key", e)
                    }
                }
            )
        )
    }

    fun addInfo(
        key: String,
        title: String,
        subtitle: String
    ) {
        PluginDetailState.addItem(
            PluginDetailItem(
                key = "${pluginId}_$key",
                pluginId = pluginId,
                targetPluginId = pluginId,
                title = title,
                subtitle = subtitle,
                type = TYPE_INFO
            )
        )
    }

    fun removeItem(key: String) {
        PluginDetailState.removeItem("${pluginId}_$key")
    }

    fun clearItems() {
        PluginDetailState.clearPluginItems(pluginId)
    }
}