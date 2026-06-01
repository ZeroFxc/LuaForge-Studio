package com.luaforge.studio.lxclua.plugin.bridge

import android.content.Context
import com.luaforge.studio.lxclua.plugin.LoadedPlugin
import com.luajava.LuaState
import com.luajava.LuaStateFactory
import java.io.File

/**
 * 插件 API 主入口类
 * 
 * 提供分类化的 API 访问方式:
 * - plugin.sys: 系统操作
 * - plugin.editor: 编辑器操作
 * - plugin.project: 项目操作
 * - plugin.ui: 对话框操作
 * - plugin.events: 事件监听
 * - plugin.config: 配置存储
 * - plugin.reflect: Java 反射
 * - plugin.lua: Lua 脚本执行
 * - plugin.menu: 菜单管理
 * - plugin.clipboard: 剪贴板操作
 * - plugin.http: 网络请求
 * - plugin.manager: 插件管理
 * - plugin.nav: 导航与侧滑栏
 */
class PluginBridge(private val context: Context, private val pluginId: String) {
    
    // ============ 子模块入口 ============
    
    /** 系统操作 API */
    val sys = PluginSys(context)
    
    /** 编辑器操作 API */
    val editor = PluginEditor()
    
    /** 项目操作 API */
    val project = PluginProject()
    
    /** UI 对话框 API */
    val ui = PluginUI()
    
    /** 事件监听 API */
    val events = PluginEvents(pluginId)
    
    /** 配置存储 API */
    val config = PluginConfig(context, pluginId)
    
    /** Java 反射 API */
    val reflect = PluginReflect()
    
    /** Lua 脚本执行 API */
    val lua = PluginLua()
    
    /** 菜单管理 API */
    val menu = PluginMenu(pluginId)
    
    /** 剪贴板操作 API */
    val clipboard = PluginClipboard(context)
    
    /** 网络请求 API */
    val http = PluginHttp()
    
    /** 插件管理 API */
    val manager = PluginManagerBridge()
    
    /** 导航与侧滑栏 API */
    val nav = PluginNavigation(pluginId)
    
    // ============ 版本信息 ============
    
    /**
     * 获取 API 版本号
     */
    fun getApiVersion(): Int {
        return 1
    }
    
    /**
     * 获取插件自身 ID
     */
    fun getPluginId(): String {
        return pluginId
    }
}