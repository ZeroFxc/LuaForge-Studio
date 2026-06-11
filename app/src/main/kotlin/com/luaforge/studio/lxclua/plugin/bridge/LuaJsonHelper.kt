package com.luaforge.studio.lxclua.plugin.bridge

import org.json.JSONArray
import org.json.JSONObject

/**
 * Lua JSON 辅助工具 — 作为 json 全局对象注入到每个插件的 Lua 环境
 *
 * 提供 json.decode(jsonStr) 返回 org.json.JSONObject，
 * Lua 端通过 Java 方法调用访问字段:
 * ```lua
 * local config = json.decode('{"key":"value"}')
 * local value = config:getString("key")  -- "value"
 * local num = config:optInt("count", 0)  -- 带默认值
 * ```
 *
 * 数组使用 org.json.JSONArray:
 * ```lua
 * local arr = json.decodeArray('[1,2,3]')
 * local first = arr:getInt(0)
 * ```
 */
class LuaJsonHelper {

    companion object {
        private const val TAG = "LuaJson"
    }

    /**
     * 解析 JSON 字符串为 JSONObject
     * Lua 调用: json:decode('{"key":"value"}')
     *
     * @param jsonStr JSON 格式字符串
     * @return org.json.JSONObject 实例，解析失败返回 null
     */
    fun decode(jsonStr: String): JSONObject? {
        return try {
            JSONObject(jsonStr)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "JSON 对象解析失败: ${e.message}")
            null
        }
    }

    /**
     * 解析 JSON 数组字符串为 JSONArray
     * Lua 调用: json:decodeArray('[1,2,3]')
     *
     * @param jsonStr JSON 数组格式字符串
     * @return org.json.JSONArray 实例，解析失败返回 null
     */
    fun decodeArray(jsonStr: String): JSONArray? {
        return try {
            JSONArray(jsonStr)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "JSON 数组解析失败: ${e.message}")
            null
        }
    }
}