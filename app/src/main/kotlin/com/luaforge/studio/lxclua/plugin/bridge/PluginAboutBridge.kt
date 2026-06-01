package com.luaforge.studio.lxclua.plugin.bridge

import com.luaforge.studio.lxclua.plugin.state.AboutItem
import com.luaforge.studio.lxclua.plugin.state.AboutSection
import com.luaforge.studio.lxclua.plugin.state.AboutState

/**
 * 关于页面扩展 API
 *
 * 允许插件向"关于"页面添加自定义 section、链接项、回调项、信息项。
 *
 * 使用方式：
 *   plugin.about.addSection("my_section", "插件功能演示", 0)
 *   plugin.about.addLink("homepage", "my_section", "项目主页", "查看源代码", "https://...", "code", 0xFF8D4A5A)
 *   plugin.about.addCallback("action", "my_section", "触发回调", "演示回调", "extension", 0xFF5B8DEF, function()
 *       plugin.sys.toast("回调被触发")
 *   end)
 *   plugin.about.addInfo("hint", "my_section", "提示", "不可点击", "info", 0xFF36618E)
 */
class PluginAboutBridge(private val pluginId: String) {

    companion object {
        const val ACTION_URL = AboutState.ACTION_URL
        const val ACTION_CALLBACK = AboutState.ACTION_CALLBACK
        const val ACTION_INFO = AboutState.ACTION_INFO
    }

    /**
     * 添加一个 section 标题。
     *
     * @param key section 唯一标识（同插件内必须唯一，建议 pluginId_xxx 形式）
     * @param title 显示在 section 顶部的标题
     * @param priority 排序优先级，数字越小越靠前
     */
    fun addSection(key: String, title: String, priority: Int = 100) {
        AboutState.addSection(
            AboutSection(
                key = "${pluginId}_$key",
                pluginId = pluginId,
                title = title,
                priority = priority
            )
        )
    }

    /**
     * 添加一个链接项（点击后用浏览器打开 URL）。
     *
     * @param key 项唯一标识
     * @param sectionKey 所属 section 的 key（注意：使用 addSection 中传入的 key，
     *                   不需要再加 pluginId 前缀，桥接会自动拼接）
     * @param title 主标题
     * @param subtitle 副标题（可空字符串）
     * @param url 目标链接
     * @param iconName Material 图标名（可空，默认 info）
     * @param iconColorArgb 图标圆形背景色 0xAARRGGBB
     */
    fun addLink(
        key: String,
        sectionKey: String,
        title: String,
        subtitle: String,
        url: String,
        iconName: String? = null,
        iconColorArgb: Int = 0xFF5B8DEF.toInt()
    ) {
        AboutState.addItem(
            AboutItem(
                key = "${pluginId}_$key",
                sectionKey = "${pluginId}_$sectionKey",
                pluginId = pluginId,
                title = title,
                subtitle = subtitle,
                iconName = iconName,
                iconColorArgb = iconColorArgb,
                actionType = AboutState.ACTION_URL,
                url = url,
                onClick = null
            )
        )
    }

    /**
     * 添加一个可点击的回调项（点击后调用 onClick 回调）。
     *
     * @param onClick 点击时执行的回调（Lua 端传入 function）
     */
    fun addCallback(
        key: String,
        sectionKey: String,
        title: String,
        subtitle: String,
        iconName: String? = null,
        iconColorArgb: Int = 0xFF8D4A5A.toInt(),
        onClick: Runnable
    ) {
        AboutState.addItem(
            AboutItem(
                key = "${pluginId}_$key",
                sectionKey = "${pluginId}_$sectionKey",
                pluginId = pluginId,
                title = title,
                subtitle = subtitle,
                iconName = iconName,
                iconColorArgb = iconColorArgb,
                actionType = AboutState.ACTION_CALLBACK,
                url = null,
                onClick = {
                    try {
                        onClick.run()
                    } catch (e: Exception) {
                        android.util.Log.e("PluginAboutBridge", "关于页面回调执行失败: ${pluginId}_$key", e)
                    }
                }
            )
        )
    }

    /**
     * 添加一个信息条目（不可点击，仅展示）。
     */
    fun addInfo(
        key: String,
        sectionKey: String,
        title: String,
        subtitle: String,
        iconName: String? = null,
        iconColorArgb: Int = 0xFF36618E.toInt()
    ) {
        AboutState.addItem(
            AboutItem(
                key = "${pluginId}_$key",
                sectionKey = "${pluginId}_$sectionKey",
                pluginId = pluginId,
                title = title,
                subtitle = subtitle,
                iconName = iconName,
                iconColorArgb = iconColorArgb,
                actionType = AboutState.ACTION_INFO,
                url = null,
                onClick = null
            )
        )
    }

    /**
     * 移除 section（同时移除其下所有 item）。
     */
    fun removeSection(key: String) {
        val fullKey = "${pluginId}_$key"
        AboutState.removeSection(fullKey)
        AboutState.pluginItems.removeAll { it.sectionKey == fullKey }
    }

    /**
     * 移除单个 item。
     */
    fun removeItem(key: String) {
        AboutState.removeItem("${pluginId}_$key")
    }

    /**
     * 清除当前插件添加的所有 section 和 item。
     */
    fun clearItems() {
        AboutState.clearPluginItems(pluginId)
    }

    /**
     * 清除所有插件的关于扩展（仅供宿主调用）。
     */
    fun clearAll() {
        AboutState.clearAll()
    }
}
