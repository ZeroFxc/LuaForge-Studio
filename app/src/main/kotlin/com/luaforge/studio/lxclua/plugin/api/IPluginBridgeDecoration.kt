package com.luaforge.studio.lxclua.plugin.api

/**
 * 编辑器装饰插件 API
 *
 * 允许插件在编辑器行上添加视觉装饰，包括:
 * - 行背景色（错误/警告/自定义高亮）
 * - Gutter 区域背景色（行号区域着色）
 * - Gutter 图标（错误/警告/信息/书签等图标）
 * - 装饰事件监听（点击/长按/双击/gutter图标点击）
 *
 * 使用方式:
 * ```lua
 * -- 设置行背景色为半透明红色
 * plugin.decoration.setLineBackground(line, 0x33FF0000)
 *
 * -- 批量设置行背景色
 * plugin.decoration.setLineBackgrounds({10, 12, 15}, 0x33FF0000)
 *
 * -- 设置 gutter 图标
 * plugin.decoration.setGutterIcon(line, "error")
 *
 * -- 设置 gutter 背景色
 * plugin.decoration.setGutterBackground(line, 0x33FFCC00)
 *
 * -- 监听装饰点击事件
 * plugin.decoration.setOnDecorationClick(function()
 *     local pos = plugin.editor.getCursorPosition()
 *     plugin.sys.toast("点击了装饰行: " .. pos[1])
 * end)
 *
 * -- 移除某行的所有装饰
 * plugin.decoration.removeLineDecoration(line)
 *
 * -- 清除当前插件在当前文件的所有装饰
 * plugin.decoration.clearMyDecorations()
 * ```
 */
interface IPluginBridgeDecoration {

    /**
     * 设置指定行的背景颜色（默认分类）
     * @param line 行号（0-based）
     * @param color ARGB 颜色 int 值（如 0x33FF0000 为半透明红色）
     * @return 设置成功返回 true
     */
    fun setLineBackground(line: Int, color: Int): Boolean

    /**
     * 设置指定行的背景颜色（指定分类）
     * @param line 行号（0-based）
     * @param color ARGB 颜色 int 值
     * @param category 分类标签，如 "breakpoint", "error", "highlight"
     * @return 设置成功返回 true
     */
    fun setLineBackground(line: Int, color: Int, category: String): Boolean

    /**
     * 批量设置多行的背景颜色
     * @param lines 行号数组
     * @param color ARGB 颜色 int 值
     * @return 成功设置的行数
     */
    fun setLineBackgrounds(lines: IntArray, color: Int): Int

    /**
     * 设置指定行的 gutter（行号区域）背景颜色
     * @param line 行号（0-based）
     * @param color ARGB 颜色 int 值
     * @return 设置成功返回 true
     */
    fun setGutterBackground(line: Int, color: Int): Boolean

    /**
     * 设置指定行的 gutter 背景颜色（指定分类）
     * @param line 行号（0-based）
     * @param color ARGB 颜色 int 值
     * @param category 分类标签
     * @return 设置成功返回 true
     */
    fun setGutterBackground(line: Int, color: Int, category: String): Boolean

    /**
     * 设置指定行 gutter 区域的图标
     * @param line 行号（0-based）
     * @param iconType 图标类型: "error", "warning", "info", "bookmark", "arrow", "star", "dot"
     * @return 设置成功返回 true
     */
    fun setGutterIcon(line: Int, iconType: String): Boolean

    /**
     * 设置指定行 gutter 区域的图标（指定分类）
     * @param line 行号（0-based）
     * @param iconType 图标类型
     * @param category 分类标签
     * @return 设置成功返回 true
     */
    fun setGutterIcon(line: Int, iconType: String, category: String): Boolean

    /**
     * 移除指定行的所有装饰
     * @param line 行号（0-based）
     * @return 如果该行有装饰并成功移除返回 true
     */
    fun removeLineDecoration(line: Int): Boolean

    /**
     * 清除当前插件在当前文件中的所有装饰
     * @return 被移除的装饰数量
     */
    fun clearMyDecorations(): Int

    /**
     * 获取当前插件在当前文件中的所有装饰
     * @return 装饰数据列表（每个装饰以 Map 形式返回）
     */
    fun getMyDecorations(): Array<Map<String, Any?>>

    /**
     * 获取文件指定行的所有装饰
     * @param line 行号（0-based）
     * @return 装饰数据列表
     */
    fun getLineDecorations(line: Int): Array<Map<String, Any?>>

    // ==================== 装饰事件监听 ====================

    /**
     * 设置装饰行点击事件回调
     * 当用户点击任意有装饰的行（行背景/gutter背景/gutter图标）时触发
     * @param callback 回调函数（Lua函数，执行时可通过 plugin.editor.getCursorPosition() 获取点击位置）
     */
    fun setOnDecorationClick(callback: Runnable)

    /**
     * 设置装饰行长按事件回调
     * 当用户长按任意有装饰的行时触发
     * @param callback 回调函数
     */
    fun setOnDecorationLongClick(callback: Runnable)

    /**
     * 设置装饰行双击事件回调
     * 当用户双击任意有装饰的行时触发
     * @param callback 回调函数
     */
    fun setOnDecorationDoubleClick(callback: Runnable)

    /**
     * 设置 gutter 图标点击事件回调
     * 当用户点击 gutter 区域的装饰图标时触发
     * @param callback 回调函数
     */
    fun setOnGutterIconClick(callback: Runnable)

    // ==================== 装饰导航 ====================

    /**
     * 跳转到下一个装饰行（所有分类）
     * 从当前光标位置向下查找最近的有装饰的行并跳转
     * @return 跳转到的行号，如果没有则返回 -1
     */
    fun gotoNextDecoration(): Int

    /**
     * 跳转到下一个指定分类的装饰行
     * 从当前光标位置向下查找最近的有指定分类装饰的行并跳转
     * @param category 分类标签，如 "breakpoint", "error", "highlight"
     * @return 跳转到的行号，如果没有则返回 -1
     */
    fun gotoNextDecoration(category: String): Int

    /**
     * 跳转到上一个装饰行（所有分类）
     * 从当前光标位置向上查找最近的有装饰的行并跳转
     * @return 跳转到的行号，如果没有则返回 -1
     */
    fun gotoPreviousDecoration(): Int

    /**
     * 跳转到上一个指定分类的装饰行
     * 从当前光标位置向上查找最近的有指定分类装饰的行并跳转
     * @param category 分类标签
     * @return 跳转到的行号，如果没有则返回 -1
     */
    fun gotoPreviousDecoration(category: String): Int

    /**
     * 获取当前文件所有装饰行的行号列表（所有分类，已排序）
     * @return 行号数组
     */
    fun getDecorationLines(): IntArray

    /**
     * 获取当前文件指定分类的装饰行号列表（已排序）
     * @param category 分类标签
     * @return 行号数组
     */
    fun getDecorationLines(category: String): IntArray
}