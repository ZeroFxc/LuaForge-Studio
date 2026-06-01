package com.luaforge.studio.lxclua.plugin.bridge

import android.content.Context

/**
 * 系统级操作 API
 * 
 * 使用方式: plugin.sys.log(tag, message)
 */
class PluginSys(private val context: Context) {
    
    /**
     * 记录日志
     */
    fun log(tag: String, message: String) {
        android.util.Log.d(tag, message)
    }
    
    /**
     * 显示 Toast 消息
     */
    fun toast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 显示长 Toast 消息
     */
    fun toastLong(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
    }
    
    /**
     * 获取 IDE 版本号
     */
    fun getVersion(): String {
        return "1.0.0"
    }
    
    /**
     * 获取应用数据目录
     */
    fun getDataDir(): String {
        return context.filesDir.absolutePath
    }
    
    /**
     * 获取包名
     */
    fun getPackageName(): String {
        return context.packageName
    }
    
    /**
     * 获取设备唯一标识
     */
    fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
    }
    
    /**
     * 获取当前时间戳（毫秒）
     */
    fun getTimeMillis(): Long {
        return System.currentTimeMillis()
    }
    
    /**
     * 获取当前时间戳（秒）
     */
    fun getTimeSeconds(): Long {
        return System.currentTimeMillis() / 1000
    }
    
    // ============ 国际化 API ============
    
    /**
     * 获取当前应用语言环境
     * @return 语言代码，如 "zh", "en", "ja" 等
     */
    fun getAppLanguage(): String {
        return context.resources.configuration.locale.language
    }
    
    /**
     * 获取当前应用区域
     * @return 区域代码，如 "CN", "US", "JP" 等
     */
    fun getAppRegion(): String {
        return context.resources.configuration.locale.country
    }
    
    /**
     * 获取完整的语言标签（语言-区域）
     * @return 如 "zh-CN", "en-US" 等
     */
    fun getAppLocale(): String {
        val locale = context.resources.configuration.locale
        return if (locale.country.isNotEmpty()) {
            "${locale.language}-${locale.country}"
        } else {
            locale.language
        }
    }
    
    /**
     * 根据资源ID获取本地化字符串
     * @param resId 资源ID（如 "app_name"）
     * @return 本地化后的字符串，如果未找到返回 null
     */
    fun getString(resId: String): String? {
        return try {
            val id = context.resources.getIdentifier(resId, "string", context.packageName)
            if (id > 0) {
                context.resources.getString(id)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 根据资源ID获取本地化字符串（带格式化参数）
     * @param resId 资源ID
     * @param args 格式化参数
     * @return 格式化后的本地化字符串
     */
    fun getString(resId: String, vararg args: Any?): String? {
        return try {
            val id = context.resources.getIdentifier(resId, "string", context.packageName)
            if (id > 0) {
                val format = context.resources.getString(id)
                String.format(format, *args)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 检查是否为中文环境
     */
    fun isChinese(): Boolean {
        return getAppLanguage() == "zh"
    }
    
    /**
     * 检查是否为英文环境
     */
    fun isEnglish(): Boolean {
        return getAppLanguage() == "en"
    }
    
    /**
     * 获取所有可用的应用语言列表
     * @return 语言代码数组
     */
    fun getAvailableLanguages(): Array<String> {
        return arrayOf("zh", "en", "ja", "ko", "fr", "de", "es", "pt", "ru", "ar")
    }
}