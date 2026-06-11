package com.luaforge.studio.lxclua.plugin.api

import android.webkit.WebView

/**
 * 插件 WebUI 访问 API
 *
 * 提供插件通过 WebView 创建自定义界面的能力。
 * Web 页面通过 window.LXC JavaScript 接口与宿主通信。
 *
 * ## Lua 插件使用方式:
 *
 * ```lua
 * -- 打开插件的 Web 界面
 * plugin.webui.open("index.html")
 *
 * -- 检查 web 文件是否存在
     * if plugin.webui.webFileExists("settings.html") then
     *     plugin.logger.info("WebUI", "settings.html 可用")
     * end
 *
 * -- 向 Web 页面发送消息（宿主→Web）
 * plugin.webui.sendToWeb('{"action": "update", "data": "hello"}')
 * ```
 *
 * ## JavaScript 端 API (window.LXC):
 *
 * ```js
 * // 调用 Lua
 * LXC.callLua("onJsEvent", '{"type": "click"}');
 *
 * // 触发事件
 * LXC.fireEvent("button:save", '{}');
 *
 * // 数据存储
 * LXC.setPluginData("theme", "dark");
 * var theme = LXC.getPluginData("theme");
 *
 * // 系统功能
 * LXC.toast("保存成功");
 * LXC.copyToClipboard("文本内容");
 * LXC.navigateBack();
 * var device = LXC.getDeviceInfo();
 * ```
 */
interface IPluginBridgeWebUI {

    /**
     * 打开插件 Web 界面
     *
     * 启动一个全屏 WebView 页面加载插件的 HTML 界面。
     * 页面文件需放在插件目录的 web/ 子目录下。
     *
     * @param page 页面文件名（相对于 web/ 目录），例如 "index.html"
     * @return 是否成功打开
     */
    fun open(page: String): Boolean

    /**
     * 关闭当前 Web 界面
     *
     * @return 是否成功关闭
     */
    fun close(): Boolean

    /**
     * 检查是否正在显示 Web 界面
     *
     * @return true 表示 WebUI 正在显示
     */
    fun isShowing(): Boolean

    /**
     * 向 WebView 发送消息（宿主 → Web）
     *
     * Web 端通过监听 lxc-message 自定义事件接收:
     * ```js
     * window.addEventListener('lxc-message', function(e) {
     *     console.log('收到宿主消息:', e.detail);
     * });
     * ```
     *
     * @param jsonMessage JSON 格式消息
     */
    fun sendToWeb(jsonMessage: String)

    /**
     * 向 WebView 执行 JavaScript 代码
     *
     * @param jsCode JavaScript 代码字符串
     */
    fun evaluateJs(jsCode: String)

    /**
     * 检查指定 Web 文件是否存在
     *
     * @param filename 文件名（相对于 web/ 目录）
     * @return true 表示文件存在
     */
    fun webFileExists(filename: String): Boolean

    /**
     * 列出 web/ 目录下所有文件
     *
     * @return 文件名数组
     */
    fun listFiles(): Array<String>

    /**
     * 获取 Web 资源根目录路径
     *
     * @return 目录绝对路径，如果不存在返回空字符串
     */
    fun getWebRoot(): String

    /**
     * 获取 WebView 实例引用（由宿主管理，供高级用途）
     *
     * @return WebView 实例，可能为 null
     */
    fun getWebView(): WebView?
}