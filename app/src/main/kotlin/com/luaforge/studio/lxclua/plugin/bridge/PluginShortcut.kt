package com.luaforge.studio.lxclua.plugin.bridge

import android.view.KeyEvent
import com.luaforge.studio.lxclua.plugin.PluginManager
import com.luaforge.studio.lxclua.plugin.api.IPluginBridgeShortcut
import com.luaforge.studio.lxclua.plugin.data.ShortcutInfo
import com.luajava.LuaFunction
import io.github.rosemoe.sora.event.EditorKeyEvent
import io.github.rosemoe.sora.event.SubscriptionReceipt
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * 插件快捷键绑定系统实现
 * 
 * 使用 sora-editor 的 EditorKeyEvent 事件系统拦截按键，
 * 匹配已注册的快捷键组合后触发插件回调。
 * 
 * 组合键格式: "Ctrl+S", "Ctrl+Shift+Z", "Alt+Enter", "Ctrl+Alt+F" 等
 * 
 * 使用方式:
 * - plugin.shortcut.register("my_key", "Ctrl+S", "保存", callback)
 * - plugin.shortcut.unregister("my_key")
 * - plugin.shortcut.isCombinationTaken("Ctrl+S")
 */
class PluginShortcut(private val pluginId: String) : IPluginBridgeShortcut {

    companion object {
        private val shortcuts = mutableMapOf<String, ShortcutInfo>()
        private val callbacks = mutableMapOf<String, Any>()
        private var currentReceipt: SubscriptionReceipt<EditorKeyEvent>? = null
        private var subscribedEditor: CodeEditor? = null

        private val keyNameMap: Map<String, Int> = mapOf(
            "A" to KeyEvent.KEYCODE_A, "B" to KeyEvent.KEYCODE_B, "C" to KeyEvent.KEYCODE_C,
            "D" to KeyEvent.KEYCODE_D, "E" to KeyEvent.KEYCODE_E, "F" to KeyEvent.KEYCODE_F,
            "G" to KeyEvent.KEYCODE_G, "H" to KeyEvent.KEYCODE_H, "I" to KeyEvent.KEYCODE_I,
            "J" to KeyEvent.KEYCODE_J, "K" to KeyEvent.KEYCODE_K, "L" to KeyEvent.KEYCODE_L,
            "M" to KeyEvent.KEYCODE_M, "N" to KeyEvent.KEYCODE_N, "O" to KeyEvent.KEYCODE_O,
            "P" to KeyEvent.KEYCODE_P, "Q" to KeyEvent.KEYCODE_Q, "R" to KeyEvent.KEYCODE_R,
            "S" to KeyEvent.KEYCODE_S, "T" to KeyEvent.KEYCODE_T, "U" to KeyEvent.KEYCODE_U,
            "V" to KeyEvent.KEYCODE_V, "W" to KeyEvent.KEYCODE_W, "X" to KeyEvent.KEYCODE_X,
            "Y" to KeyEvent.KEYCODE_Y, "Z" to KeyEvent.KEYCODE_Z,
            "0" to KeyEvent.KEYCODE_0, "1" to KeyEvent.KEYCODE_1, "2" to KeyEvent.KEYCODE_2,
            "3" to KeyEvent.KEYCODE_3, "4" to KeyEvent.KEYCODE_4, "5" to KeyEvent.KEYCODE_5,
            "6" to KeyEvent.KEYCODE_6, "7" to KeyEvent.KEYCODE_7, "8" to KeyEvent.KEYCODE_8,
            "9" to KeyEvent.KEYCODE_9,
            "F1" to KeyEvent.KEYCODE_F1, "F2" to KeyEvent.KEYCODE_F2, "F3" to KeyEvent.KEYCODE_F3,
            "F4" to KeyEvent.KEYCODE_F4, "F5" to KeyEvent.KEYCODE_F5, "F6" to KeyEvent.KEYCODE_F6,
            "F7" to KeyEvent.KEYCODE_F7, "F8" to KeyEvent.KEYCODE_F8, "F9" to KeyEvent.KEYCODE_F9,
            "F10" to KeyEvent.KEYCODE_F10, "F11" to KeyEvent.KEYCODE_F11, "F12" to KeyEvent.KEYCODE_F12,
            "ENTER" to KeyEvent.KEYCODE_ENTER, "DELETE" to KeyEvent.KEYCODE_DEL,
            "TAB" to KeyEvent.KEYCODE_TAB, "SPACE" to KeyEvent.KEYCODE_SPACE,
            "ESCAPE" to KeyEvent.KEYCODE_ESCAPE, "BACKSPACE" to KeyEvent.KEYCODE_DEL,
            "UP" to KeyEvent.KEYCODE_DPAD_UP, "DOWN" to KeyEvent.KEYCODE_DPAD_DOWN,
            "LEFT" to KeyEvent.KEYCODE_DPAD_LEFT, "RIGHT" to KeyEvent.KEYCODE_DPAD_RIGHT,
            "HOME" to KeyEvent.KEYCODE_MOVE_HOME, "END" to KeyEvent.KEYCODE_MOVE_END,
            "PAGE_UP" to KeyEvent.KEYCODE_PAGE_UP, "PAGE_DOWN" to KeyEvent.KEYCODE_PAGE_DOWN,
            "INSERT" to KeyEvent.KEYCODE_INSERT,
            "PERIOD" to KeyEvent.KEYCODE_PERIOD, "COMMA" to KeyEvent.KEYCODE_COMMA,
            "SLASH" to KeyEvent.KEYCODE_SLASH, "BACKSLASH" to KeyEvent.KEYCODE_BACKSLASH,
            "SEMICOLON" to KeyEvent.KEYCODE_SEMICOLON, "EQUALS" to KeyEvent.KEYCODE_EQUALS,
            "MINUS" to KeyEvent.KEYCODE_MINUS, "LEFT_BRACKET" to KeyEvent.KEYCODE_LEFT_BRACKET,
            "RIGHT_BRACKET" to KeyEvent.KEYCODE_RIGHT_BRACKET,
            "GRAVE" to KeyEvent.KEYCODE_GRAVE, "APOSTROPHE" to KeyEvent.KEYCODE_APOSTROPHE
        )

        fun parseCombination(combination: String): Pair<Int, Int>? {
            val upper = combination.uppercase().trim()
            if (upper.isEmpty()) return null

            var modifiers = 0
            var remaining = upper

            while (true) {
                when {
                    remaining.startsWith("CTRL+") -> {
                        modifiers = modifiers or 1
                        remaining = remaining.removePrefix("CTRL+")
                    }
                    remaining.startsWith("SHIFT+") -> {
                        modifiers = modifiers or 2
                        remaining = remaining.removePrefix("SHIFT+")
                    }
                    remaining.startsWith("ALT+") -> {
                        modifiers = modifiers or 4
                        remaining = remaining.removePrefix("ALT+")
                    }
                    else -> break
                }
            }

            if (remaining.isEmpty()) return null

            val keyCode = keyNameMap[remaining] ?: return null

            return Pair(modifiers, keyCode)
        }

        private fun matchesEvent(event: EditorKeyEvent, shortcut: ShortcutInfo): Boolean {
            if (event.eventType != EditorKeyEvent.Type.DOWN) return false
            if (event.keyCode != shortcut.keyCode) return false

            val ctrlOk = ((shortcut.modifiers and 1) != 0) == event.isCtrlPressed
            val shiftOk = ((shortcut.modifiers and 2) != 0) == event.isShiftPressed
            val altOk = ((shortcut.modifiers and 4) != 0) == event.isAltPressed

            return ctrlOk && shiftOk && altOk
        }

        fun removeAllPluginShortcuts(pluginId: String): Int {
            val keysToRemove = shortcuts.filter { it.value.pluginId == pluginId }.keys
            keysToRemove.forEach { key ->
                shortcuts.remove(key)
                callbacks.remove("$pluginId/$key")
            }
            return keysToRemove.size
        }

        private fun ensureSubscribed() {
            val editor = PluginManager.activeViewModel?.getActiveEditor()
            if (editor == null) {
                currentReceipt?.unsubscribe()
                currentReceipt = null
                subscribedEditor = null
                return
            }

            if (editor === subscribedEditor && subscribedEditor != null) return

            currentReceipt?.unsubscribe()
            currentReceipt = null
            subscribedEditor = null

            currentReceipt = editor.subscribeEvent(EditorKeyEvent::class.java) { event, _ ->
                handleKeyEvent(event)
            }
            subscribedEditor = editor
        }

        private fun handleKeyEvent(event: EditorKeyEvent) {
            for ((_, shortcut) in shortcuts) {
                if (matchesEvent(event, shortcut)) {
                    val cb = callbacks[shortcut.id]
                    if (cb != null) {
                        try {
                            when (cb) {
                                is LuaFunction<*> -> cb.call(shortcut.combination)
                                is Runnable -> cb.run()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("PluginShortcut", "触发快捷键回调失败: ${shortcut.id}", e)
                        }
                    }
                    event.markAsConsumed()
                    return
                }
            }
        }
    }

    private fun makeGlobalId(key: String): String = "$pluginId/$key"

    override fun register(
        key: String,
        combination: String,
        label: String,
        description: String,
        callback: Any,
        editorOnly: Boolean
    ): String? {
        val (modifiers, keyCode) = Companion.parseCombination(combination) ?: return null

        val globalId = makeGlobalId(key)
        val info = ShortcutInfo(
            id = globalId,
            pluginId = pluginId,
            key = key,
            combination = combination,
            label = label,
            description = description,
            modifiers = modifiers,
            keyCode = keyCode,
            editorOnly = editorOnly,
            registeredAt = System.currentTimeMillis()
        )

        shortcuts[globalId] = info
        callbacks[globalId] = callback

        ensureSubscribed()

        return globalId
    }

    override fun register(key: String, combination: String, label: String, callback: Any): String? {
        return register(key, combination, label, "", callback, true)
    }

    override fun unregister(key: String): Boolean {
        val globalId = makeGlobalId(key)
        callbacks.remove(globalId)
        return shortcuts.remove(globalId) != null
    }

    override fun unregisterAll(): Int {
        return Companion.removeAllPluginShortcuts(pluginId)
    }

    override fun getMyShortcuts(): Array<ShortcutInfo> {
        return shortcuts.values.filter { it.pluginId == pluginId }.toTypedArray()
    }

    override fun getShortcut(globalId: String): ShortcutInfo? {
        return if (globalId.contains("/")) {
            shortcuts[globalId]
        } else {
            shortcuts[makeGlobalId(globalId)]
        }
    }

    override fun getAllShortcuts(): Array<ShortcutInfo> {
        return shortcuts.values.toTypedArray()
    }

    override fun isCombinationTaken(combination: String): ShortcutInfo? {
        val parsed = Companion.parseCombination(combination) ?: return null
        val (mods, keyCode) = parsed
        return shortcuts.values.find { it.modifiers == mods && it.keyCode == keyCode }
    }

    override fun getShortcutCount(): Int {
        return shortcuts.size
    }
}