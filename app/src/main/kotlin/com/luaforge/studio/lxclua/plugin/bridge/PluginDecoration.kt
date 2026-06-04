package com.luaforge.studio.lxclua.plugin.bridge

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import com.luaforge.studio.lxclua.plugin.PluginManager
import com.luaforge.studio.lxclua.plugin.api.IPluginBridgeDecoration
import com.luaforge.studio.lxclua.plugin.data.PluginDecorationEntry
import io.github.rosemoe.sora.lang.styling.color.ConstColor
import io.github.rosemoe.sora.lang.styling.line.LineBackground
import io.github.rosemoe.sora.lang.styling.line.LineGutterBackground
import io.github.rosemoe.sora.lang.styling.line.LineSideIcon
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * 编辑器装饰插件扩展实现
 *
 * 允许插件在编辑器行上添加行背景色、gutter 区域背景色、gutter 图标等视觉装饰。
 * 装饰数据按 (pluginId, filePath) 分组存储，通过 editor.styles.addLineStyle() 渲染。
 */
class PluginDecoration(private val pluginId: String) : IPluginBridgeDecoration {

    companion object {
        /** 全局装饰注册表: key = "pluginId|filePath|line" -> PluginDecorationEntry */
        private val decorationRegistry = mutableMapOf<String, PluginDecorationEntry>()

        // ==================== 装饰事件回调（按插件隔离） ====================
        private val decorationClickCallbacks = mutableMapOf<String, Runnable>()
        private val decorationLongClickCallbacks = mutableMapOf<String, Runnable>()
        private val decorationDoubleClickCallbacks = mutableMapOf<String, Runnable>()
        private val gutterIconClickCallbacks = mutableMapOf<String, Runnable>()

        /** 防重复 post 标记 — 避免快速按键时堆积大量重应用回调 */
        @Volatile
        private var reapplyScheduled = false

        /**
         * 标记重应用已处理，允许下一次 post
         */
        fun markReapplyDone() {
            reapplyScheduled = false
        }

        /**
         * 尝试调度重应用（防抖），返回 true 表示需要执行
         */
        fun tryScheduleReapply(): Boolean {
            if (reapplyScheduled) return false
            reapplyScheduled = true
            return true
        }

        /**
         * 检查指定行是否有装饰
         * @param filePath 文件路径
         * @param line 行号
         */
        fun hasDecorationsAtLine(filePath: String, line: Int): Boolean {
            return decorationRegistry.values.any {
                it.filePath == filePath && it.line == line
            }
        }

        /**
         * 获取指定行上所有装饰的插件 ID 集合
         * @param filePath 文件路径
         * @param line 行号
         * @return 该行有装饰的插件 ID 集合
         */
        private fun getPluginIdsAtLine(filePath: String, line: Int): Set<String> {
            return decorationRegistry.values
                .filter { it.filePath == filePath && it.line == line }
                .map { it.pluginId }
                .toSet()
        }

        /**
         * 触发装饰点击事件 — 仅通知在指定行有装饰的插件
         * @param filePath 当前文件路径
         * @param line 点击的行号
         */
        fun notifyDecorationClick(filePath: String, line: Int) {
            val pluginIds = getPluginIdsAtLine(filePath, line)
            decorationClickCallbacks.forEach { (pluginId, callback) ->
                if (pluginId in pluginIds) callback.run()
            }
        }

        /**
         * 触发装饰长按事件 — 仅通知在指定行有装饰的插件
         * @param filePath 当前文件路径
         * @param line 长按的行号
         */
        fun notifyDecorationLongClick(filePath: String, line: Int) {
            val pluginIds = getPluginIdsAtLine(filePath, line)
            decorationLongClickCallbacks.forEach { (pluginId, callback) ->
                if (pluginId in pluginIds) callback.run()
            }
        }

        /**
         * 触发装饰双击事件 — 仅通知在指定行有装饰的插件
         * @param filePath 当前文件路径
         * @param line 双击的行号
         */
        fun notifyDecorationDoubleClick(filePath: String, line: Int) {
            val pluginIds = getPluginIdsAtLine(filePath, line)
            decorationDoubleClickCallbacks.forEach { (pluginId, callback) ->
                if (pluginId in pluginIds) callback.run()
            }
        }

        /**
         * 触发 gutter 图标点击事件 — 仅通知在指定行有装饰的插件
         * @param filePath 当前文件路径
         * @param line 点击 Gutter 图标的行号
         */
        fun notifyGutterIconClick(filePath: String, line: Int) {
            val pluginIds = getPluginIdsAtLine(filePath, line)
            gutterIconClickCallbacks.forEach { (pluginId, callback) ->
                if (pluginId in pluginIds) callback.run()
            }
        }

        /**
         * 应用所有插件装饰到指定编辑器的 Styles 中
         * @param editor 目标编辑器
         * @param filePath 文件路径
         */
        fun applyToEditor(editor: CodeEditor, filePath: String) {
            val styles = editor.styles ?: return
            val entries = decorationRegistry.values.filter {
                it.filePath == filePath
            }

            for (entry in entries) {
                // 行背景色
                entry.lineBackgroundColor?.let { color ->
                    styles.addLineStyle(
                        LineBackground(entry.line, ConstColor(color))
                    )
                }

                // Gutter 背景色
                entry.gutterBackgroundColor?.let { color ->
                    styles.addLineStyle(
                        LineGutterBackground(entry.line, ConstColor(color))
                    )
                }

                // Gutter 图标
                entry.gutterIconType?.let { iconType ->
                    val drawable = PluginGutterIconDrawable(iconType)
                    styles.addLineStyle(
                        LineSideIcon(entry.line, drawable)
                    )
                }
            }
        }

        /**
         * 清除当前编辑器中所有插件的装饰
         * @param editor 目标编辑器
         * @param filePath 文件路径
         */
        fun clearFromEditor(editor: CodeEditor, filePath: String) {
            val styles = editor.styles ?: return
            val entries = decorationRegistry.values.filter {
                it.filePath == filePath
            }

            for (entry in entries) {
                entry.lineBackgroundColor?.let {
                    styles.eraseLineStyle(entry.line, LineBackground::class.java)
                }
                entry.gutterBackgroundColor?.let {
                    styles.eraseLineStyle(entry.line, LineGutterBackground::class.java)
                }
                entry.gutterIconType?.let {
                    styles.eraseLineStyle(entry.line, LineSideIcon::class.java)
                }
            }
        }

        /**
         * 移除指定插件的所有装饰数据，并立即刷新当前编辑器
         * @param pluginId 插件 ID
         * @return 被移除的装饰数量
         */
        fun removePluginDecorations(pluginId: String): Int {
            val keys = decorationRegistry.filter { it.value.pluginId == pluginId }.keys
            keys.forEach { decorationRegistry.remove(it) }
            // 立即刷新当前编辑器，确保装饰从视图上移除
            val editor = PluginManager.activeViewModel?.getActiveEditor()
            val filePath = PluginManager.activeViewModel?.activeFileState?.file?.absolutePath
            if (editor != null && filePath != null) {
                clearAllPluginStyles(editor)
                applyToEditor(editor, filePath)
                editor.postInvalidate()
            }
            return keys.size
        }

        /**
         * 清理指定插件注册的所有事件回调
         * @param pluginId 插件 ID
         */
        fun removePluginCallbacks(pluginId: String) {
            decorationClickCallbacks.remove(pluginId)
            decorationLongClickCallbacks.remove(pluginId)
            decorationDoubleClickCallbacks.remove(pluginId)
            gutterIconClickCallbacks.remove(pluginId)
        }

        /**
         * 移除指定文件的所有装饰数据（用于文件关闭时清理）
         * @param filePath 文件路径
         * @return 被移除的装饰数量
         */
        fun removeFileDecorations(filePath: String): Int {
            val keys = decorationRegistry.filter { it.value.filePath == filePath }.keys
            keys.forEach { decorationRegistry.remove(it) }
            return keys.size
        }

        /**
         * 清空所有装饰注册表（用于项目切换时清理）
         */
        fun clearAllDecorations() {
            decorationRegistry.clear()
        }

        /**
         * 获取指定插件在指定文件中的所有装饰
         * @param pluginId 插件 ID
         * @param filePath 文件路径
         * @return 装饰列表
         */
        fun getPluginDecorations(pluginId: String, filePath: String): List<PluginDecorationEntry> {
            return decorationRegistry.values.filter {
                it.pluginId == pluginId && it.filePath == filePath
            }
        }

        /** 注册表 key 生成 */
        private fun registryKey(pluginId: String, filePath: String, line: Int): String {
            return "$pluginId|$filePath|$line"
        }

        /**
         * 清除编辑器上所有插件装饰样式（不限行号）
         * 用于文本变更后重新应用装饰，避免因 sora-editor 内部样式移位导致重复
         * @param editor 目标编辑器
         */
        fun clearAllPluginStyles(editor: CodeEditor) {
            val styles = editor.styles ?: return
            val lineCount = editor.text.lineCount
            for (line in 0 until lineCount) {
                styles.eraseLineStyle(line, LineBackground::class.java)
                styles.eraseLineStyle(line, LineGutterBackground::class.java)
                styles.eraseLineStyle(line, LineSideIcon::class.java)
            }
        }
    }

    /** 获取当前活跃文件路径 */
    private val activeFilePath: String?
        get() = PluginManager.activeViewModel?.activeFileState?.file?.absolutePath

    // ==================== 公开方法 ====================

    override fun setLineBackground(line: Int, color: Int): Boolean {
        val key = registryKey(pluginId, activeFilePath ?: return false, line)
        val existing = decorationRegistry[key]
        return setLineBackground(line, color, existing?.category ?: "default")
    }

    override fun setLineBackground(line: Int, color: Int, category: String): Boolean {
        val filePath = activeFilePath ?: return false
        val key = registryKey(pluginId, filePath, line)
        val existing = decorationRegistry[key]

        decorationRegistry[key] = PluginDecorationEntry(
            pluginId = pluginId,
            filePath = filePath,
            line = line,
            category = category,
            lineBackgroundColor = color,
            gutterBackgroundColor = existing?.gutterBackgroundColor,
            gutterIconType = existing?.gutterIconType
        )
        refreshEditorDecorations(filePath)
        return true
    }

    override fun setLineBackgrounds(lines: IntArray, color: Int): Int {
        var count = 0
        for (line in lines) {
            if (setLineBackground(line, color)) count++
        }
        return count
    }

    override fun setGutterBackground(line: Int, color: Int): Boolean {
        val key = registryKey(pluginId, activeFilePath ?: return false, line)
        val existing = decorationRegistry[key]
        return setGutterBackground(line, color, existing?.category ?: "default")
    }

    override fun setGutterBackground(line: Int, color: Int, category: String): Boolean {
        val filePath = activeFilePath ?: return false
        val key = registryKey(pluginId, filePath, line)
        val existing = decorationRegistry[key]

        decorationRegistry[key] = PluginDecorationEntry(
            pluginId = pluginId,
            filePath = filePath,
            line = line,
            category = category,
            lineBackgroundColor = existing?.lineBackgroundColor,
            gutterBackgroundColor = color,
            gutterIconType = existing?.gutterIconType
        )
        refreshEditorDecorations(filePath)
        return true
    }

    override fun setGutterIcon(line: Int, iconType: String): Boolean {
        val key = registryKey(pluginId, activeFilePath ?: return false, line)
        val existing = decorationRegistry[key]
        return setGutterIcon(line, iconType, existing?.category ?: "default")
    }

    override fun setGutterIcon(line: Int, iconType: String, category: String): Boolean {
        if (iconType !in PluginDecorationEntry.SUPPORTED_ICON_TYPES) return false
        val filePath = activeFilePath ?: return false
        val key = registryKey(pluginId, filePath, line)
        val existing = decorationRegistry[key]

        decorationRegistry[key] = PluginDecorationEntry(
            pluginId = pluginId,
            filePath = filePath,
            line = line,
            category = category,
            lineBackgroundColor = existing?.lineBackgroundColor,
            gutterBackgroundColor = existing?.gutterBackgroundColor,
            gutterIconType = iconType
        )
        refreshEditorDecorations(filePath)
        return true
    }

    override fun removeLineDecoration(line: Int): Boolean {
        val filePath = activeFilePath ?: return false
        val key = registryKey(pluginId, filePath, line)
        if (!decorationRegistry.containsKey(key)) return false
        decorationRegistry.remove(key)
        // 清除所有插件样式（sora-editor 可能已移位）后重新应用剩余装饰
        val editor = PluginManager.activeViewModel?.getActiveEditor()
        if (editor != null) {
            clearAllPluginStyles(editor)
            applyToEditor(editor, filePath)
            editor.postInvalidate()
        }
        return true
    }

    override fun clearMyDecorations(): Int {
        val keys = decorationRegistry.filter {
            it.value.pluginId == pluginId
        }.keys
        keys.forEach { decorationRegistry.remove(it) }
        // 清除所有插件样式（sora-editor 可能已移位）后重新应用其他插件装饰
        val editor = PluginManager.activeViewModel?.getActiveEditor()
        if (editor != null) {
            clearAllPluginStyles(editor)
            val filePath = activeFilePath
            if (filePath != null) {
                applyToEditor(editor, filePath)
            }
            editor.postInvalidate()
        }
        return keys.size
    }

    override fun getMyDecorations(): Array<Map<String, Any?>> {
        val filePath = activeFilePath ?: return emptyArray()
        return decorationRegistry.values
            .filter { it.pluginId == pluginId && it.filePath == filePath }
            .map { it.toMap() }
            .toTypedArray()
    }

    override fun getLineDecorations(line: Int): Array<Map<String, Any?>> {
        val filePath = activeFilePath ?: return emptyArray()
        val key = registryKey(pluginId, filePath, line)
        val entry = decorationRegistry[key] ?: return emptyArray()
        return arrayOf(entry.toMap())
    }

    // ==================== 装饰事件监听 ====================

    override fun setOnDecorationClick(callback: Runnable) {
        decorationClickCallbacks[pluginId] = callback
    }

    override fun setOnDecorationLongClick(callback: Runnable) {
        decorationLongClickCallbacks[pluginId] = callback
    }

    override fun setOnDecorationDoubleClick(callback: Runnable) {
        decorationDoubleClickCallbacks[pluginId] = callback
    }

    override fun setOnGutterIconClick(callback: Runnable) {
        gutterIconClickCallbacks[pluginId] = callback
    }

    // ==================== 装饰导航 ====================

    override fun gotoNextDecoration(): Int {
        return gotoNextDecorationInternal { true }
    }

    override fun gotoNextDecoration(category: String): Int {
        return gotoNextDecorationInternal { it.category == category }
    }

    private inline fun gotoNextDecorationInternal(filter: (PluginDecorationEntry) -> Boolean): Int {
        val editor = PluginManager.activeViewModel?.getActiveEditor() ?: return -1
        val filePath = activeFilePath ?: return -1
        val currentLine = editor.cursor.leftLine
        val lines = decorationRegistry.values
            .filter { it.filePath == filePath && filter(it) }
            .map { it.line }
            .distinct()
            .sorted()
        val next = lines.firstOrNull { it > currentLine } ?: return -1
        editor.jumpToLine(next)
        editor.setSelection(next, 0)
        return next
    }

    override fun gotoPreviousDecoration(): Int {
        return gotoPreviousDecorationInternal { true }
    }

    override fun gotoPreviousDecoration(category: String): Int {
        return gotoPreviousDecorationInternal { it.category == category }
    }

    private inline fun gotoPreviousDecorationInternal(filter: (PluginDecorationEntry) -> Boolean): Int {
        val editor = PluginManager.activeViewModel?.getActiveEditor() ?: return -1
        val filePath = activeFilePath ?: return -1
        val currentLine = editor.cursor.leftLine
        val lines = decorationRegistry.values
            .filter { it.filePath == filePath && filter(it) }
            .map { it.line }
            .distinct()
            .sortedDescending()
        val prev = lines.firstOrNull { it < currentLine } ?: return -1
        editor.jumpToLine(prev)
        editor.setSelection(prev, 0)
        return prev
    }

    override fun getDecorationLines(): IntArray {
        return getDecorationLinesInternal { true }
    }

    override fun getDecorationLines(category: String): IntArray {
        return getDecorationLinesInternal { it.category == category }
    }

    private inline fun getDecorationLinesInternal(filter: (PluginDecorationEntry) -> Boolean): IntArray {
        val filePath = activeFilePath ?: return intArrayOf()
        return decorationRegistry.values
            .filter { it.filePath == filePath && filter(it) }
            .map { it.line }
            .distinct()
            .sorted()
            .toIntArray()
    }

    // ==================== 编辑器刷新 ====================

    /** 刷新指定文件编辑器上的装饰（全量清除后重应用，避免 sora-editor 移位导致重复） */
    private fun refreshEditorDecorations(filePath: String) {
        val editor = PluginManager.activeViewModel?.getActiveEditor() ?: return
        val activeFile = activeFilePath
        if (activeFile == filePath) {
            clearAllPluginStyles(editor)
            applyToEditor(editor, filePath)
            editor.postInvalidate()
        }
    }

    /** 刷新所有编辑器 */
    private fun refreshAllEditors() {
        val editor = PluginManager.activeViewModel?.getActiveEditor() ?: return
        val filePath = activeFilePath ?: return
        clearFromEditor(editor, filePath)
        applyToEditor(editor, filePath)
        editor.postInvalidate()
    }
}

/**
 * 插件 Gutter 图标 Drawable
 *
 * 根据图标类型绘制简单的几何图形（圆形、三角形、星形等），
 * 使用预设颜色区分不同类型
 */
class PluginGutterIconDrawable(private val iconType: String) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = when (iconType) {
            "error" -> Color.RED
            "warning" -> Color.parseColor("#FFCC00")
            "info" -> Color.parseColor("#2196F3")
            "bookmark" -> Color.parseColor("#4CAF50")
            "arrow" -> Color.parseColor("#FF9800")
            "star" -> Color.parseColor("#E91E63")
            "dot" -> Color.parseColor("#9E9E9E")
            else -> Color.GRAY
        }
    }

    override fun draw(canvas: Canvas) {
        val cx = bounds.exactCenterX()
        val cy = bounds.exactCenterY()
        val radius = (Math.min(bounds.width(), bounds.height()) / 2f) * 0.45f

        when (iconType) {
            "dot", "error", "warning", "info", "bookmark" -> {
                // 圆形图标
                canvas.drawCircle(cx, cy, radius, paint)
            }
            "arrow" -> {
                // 三角形箭头
                val path = android.graphics.Path()
                path.moveTo(cx, cy - radius)
                path.lineTo(cx + radius, cy + radius)
                path.lineTo(cx - radius, cy + radius)
                path.close()
                canvas.drawPath(path, paint)
            }
            "star" -> {
                // 五角星
                drawStar(canvas, cx, cy, radius)
            }
        }
    }

    /** 绘制五角星 */
    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val path = android.graphics.Path()
        val innerRadius = radius * 0.382f
        for (i in 0 until 10) {
            val angle = (Math.PI / 2 * -1) + (Math.PI / 5 * i)
            val r = if (i % 2 == 0) radius else innerRadius
            val x = cx + (r * Math.cos(angle)).toFloat()
            val y = cy + (r * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}