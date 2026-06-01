package com.luaforge.studio.lxclua.plugin.state

import androidx.compose.runtime.mutableStateListOf

/**
 * 关于页面 section（分组标题）
 *
 * @param key 唯一标识（建议由插件 ID + 计数器组成）
 * @param pluginId 所属插件
 * @param title 标题文本
 * @param priority 排序优先级（数字越小越靠前）
 */
data class AboutSection(
    val key: String,
    val pluginId: String,
    val title: String,
    val priority: Int = 0
)

/**
 * 关于页面项（链接 / 回调 / 信息）
 *
 * @param key 唯一标识
 * @param sectionKey 所属 section 的 key
 * @param pluginId 所属插件
 * @param title 主标题
 * @param subtitle 副标题
 * @param iconName Material 图标名（如 "info"、"code"、"book"）
 * @param iconColorArgb 图标背景色，0xAARRGGBB
 * @param actionType "url" 打开链接 | "callback" 触发回调 | "info" 不可点击
 * @param url 当 actionType="url" 时使用
 * @param onClick 当 actionType="callback" 时使用
 */
data class AboutItem(
    val key: String,
    val sectionKey: String,
    val pluginId: String,
    val title: String,
    val subtitle: String,
    val iconName: String?,
    val iconColorArgb: Int,
    val actionType: String,
    val url: String? = null,
    val onClick: (() -> Unit)? = null
)

/**
 * 关于页面插件扩展状态
 *
 * 由 PluginAboutBridge 调用以注册 section / item，
 * AboutScreen 直接读取 pluginSections / pluginItems 以触发 Compose 重组。
 */
object AboutState {

    const val ACTION_URL = "url"
    const val ACTION_CALLBACK = "callback"
    const val ACTION_INFO = "info"

    /** 插件注册的 section 列表 */
    val pluginSections = mutableStateListOf<AboutSection>()

    /** 插件注册的 item 列表 */
    val pluginItems = mutableStateListOf<AboutItem>()

    /**
     * 添加一个 section。
     * 同 key 的 section 不会重复添加；后注册的同 key section 会覆盖前一个。
     */
    fun addSection(section: AboutSection) {
        val existingIndex = pluginSections.indexOfFirst { it.key == section.key }
        if (existingIndex >= 0) {
            pluginSections[existingIndex] = section
        } else {
            pluginSections.add(section)
        }
    }

    /**
     * 添加一个 item。
     * 同 key 的 item 不会重复添加；后注册的同 key item 会覆盖前一个。
     */
    fun addItem(item: AboutItem) {
        val existingIndex = pluginItems.indexOfFirst { it.key == item.key }
        if (existingIndex >= 0) {
            pluginItems[existingIndex] = item
        } else {
            pluginItems.add(item)
        }
    }

    /**
     * 移除指定 section（不连带移除 item，item 仍保留但不会显示）
     */
    fun removeSection(key: String) {
        pluginSections.removeAll { it.key == key }
    }

    /**
     * 移除指定 item
     */
    fun removeItem(key: String) {
        pluginItems.removeAll { it.key == key }
    }

    /**
     * 清除指定插件的所有 section 与 item
     */
    fun clearPluginItems(pluginId: String) {
        pluginSections.removeAll { it.pluginId == pluginId }
        pluginItems.removeAll { it.pluginId == pluginId }
    }

    /**
     * 清除所有插件的 section 与 item
     */
    fun clearAll() {
        pluginSections.clear()
        pluginItems.clear()
    }

    /**
     * 获取排序后的 section 列表（按 priority 升序，再按注册顺序）
     */
    fun getSortedSections(): List<AboutSection> {
        return pluginSections.sortedWith(compareBy({ it.priority }, { it.key }))
    }

    /**
     * 获取指定 section 下的所有 item
     */
    fun getItemsBySection(sectionKey: String): List<AboutItem> {
        return pluginItems.filter { it.sectionKey == sectionKey }
    }
}
