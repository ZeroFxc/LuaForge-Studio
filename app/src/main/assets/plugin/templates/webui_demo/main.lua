--[[
    webui_demo — 插件 WebUI 完整示例

    演示功能:
    - WebView 加载自定义 HTML/CSS/JS 界面
    - JS → Lua 调用 (LXC.callLua)
    - Lua → JS 推送消息 (plugin.webui.sendToWeb)
    - JS → Lua 事件触发 (LXC.fireEvent)
    - 持久化数据存储 (LXC.setPluginData / getPluginData)
    - 设备信息获取 (LXC.getDeviceInfo)
    - 剪贴板/震动等系统能力

    目录结构:
    webui_demo/
      manifest.json
      main.lua          ← 本文件
      web/
        index.html       ← Web 入口
        style.css        ← 样式
        app.js           ← JS 逻辑
--]]

local TAG = "WebUIDemo"

-- 辅助函数：写日志
local function log(msg)
    plugin.logger.info(TAG, msg)
end

-- 辅助函数：显示 Toast
local function toast(msg)
    plugin.sys.toast(msg)
end

-- ==================== JS 调用的 Lua 函数 ====================
-- 注意: 这些必须是全局函数，JS 通过 LXC.callLua("函数名", "参数JSON") 调用
-- json:decode 返回 org.json.JSONObject，使用 obj:optString("key","") 访问字段

-- 内部：安全解码 JSON，诊断错误原因
local function safeDecode(dataJson)
    if json == nil then
        log("[Lua] 致命: json 全局对象为 nil，请检查 LuaPluginLoader 注入")
        return nil, "json 对象未注入"
    end
    -- 使用匿名函数包裹避免 pcall 传参导致的 LuaJava 参数错误
    local ok, result = pcall(function() return json.decode(dataJson) end)
    if not ok then
        log("[Lua] pcall 错误: " .. tostring(result))
        return nil, tostring(result)
    end
    if result == nil then
        log("[Lua] decode 返回 nil, dataJson=" .. tostring(dataJson))
        return nil, "decode 返回 nil"
    end
    return result, nil
end

-- JS 传入字符串，Lua 处理后返回结果
function jsEcho(data)
    local ok, parsed = pcall(function() return json.decode(data) end)
    if ok and parsed ~= nil then
        log("[JS→Lua] jsEcho 收到: " .. tostring(data))
        return "Lua 已收到你的消息: " .. tostring(data)
    else
        log("[JS→Lua] jsEcho 收到(纯文本): " .. tostring(data))
        return "Lua 已收到你的消息: " .. tostring(data)
    end
end

-- JS 请求修改插件配置
function jsSaveConfig(dataJson)
    local config, err = safeDecode(dataJson)
    if config == nil then
        log("[JS→Lua] jsSaveConfig 解析失败: dataJson=" .. tostring(dataJson) .. " err=" .. tostring(err))
        return "error: JSON 解析失败 - " .. tostring(err)
    end

    -- 使用 . 语法调用 Java 方法，避免 Lua : 语法额外传递 self
    local key = config.optString("key", "")
    local value = config.optString("value", "")
    if key ~= "" and value ~= "" then
        plugin.config.setConfig("webui_" .. key, value)
        log("[JS→Lua] 保存配置: " .. key .. " = " .. value)
        toast("配置已保存: " .. key)
        return "ok"
    end
    return "error: 缺少 key 或 value 参数"
end

-- JS 请求读取插件配置
function jsGetConfig(dataJson)
    local ok, config = pcall(function() return json.decode(dataJson) end)
    local key = ""
    if ok and config ~= nil then
        key = config.optString("key", "")
    end
    -- JSON 解析失败时直接用 dataJson 当作 key（兼容纯字符串调用）
    if key == "" then key = dataJson end
    local value = plugin.config.getConfig("webui_" .. key, "")
    log("[JS→Lua] 读取配置: " .. key .. " = " .. value)
    return value
end

-- JS 获取当前计数器值（页面加载时调用，不递增）
function jsGetCounter()
    -- 使用 . 语法调用 Java 方法，避免 : 额外传递 self
    local ok, countStr = pcall(function() return plugin.config.getConfig("webui_counter", "0") end)
    if not ok then
        log("[JS→Lua] jsGetCounter 错误: " .. tostring(countStr))
        return "0"
    end
    local count = tonumber(countStr) or 0
    log("[JS→Lua] 读取计数器: " .. count)
    return tostring(count)
end

-- JS 触发计数递增
function jsIncrementCounter(dataJson)
    local ok, countStr = pcall(function() return plugin.config.getConfig("webui_counter", "0") end)
    if not ok then
        log("[JS→Lua] jsIncrementCounter getConfig 错误: " .. tostring(countStr))
        return "0"
    end
    local count = tonumber(countStr) or 0
    count = count + 1
    local ok2 = pcall(function() plugin.config.setConfig("webui_counter", tostring(count)) end)
    if not ok2 then
        log("[JS→Lua] jsIncrementCounter setConfig 错误")
    end
    log("[JS→Lua] 计数器递增: " .. count)
    toast("计数器: " .. count)
    return tostring(count)
end

-- ==================== 事件监听 ====================

-- 监听插件管理页面卡片点击：打开 WebUI
plugin.events.register("onPluginCardClick", function(pluginId)
    if pluginId == "com.example.webui_demo" then
        local ok = plugin.webui.open("index.html")
        if ok then
            log("通过卡片点击打开 WebUI")
        end
    end
end)
-- WebUI 会通过 LXC.fireEvent 触发，Lua 端通过 EventManager 接收

-- 监听 WebUI 发来的自定义事件
plugin.events.register("WEBUI_webui:submit", function(pluginId, dataJson)
    log("[WebUI事件] 收到 webui:submit 事件: " .. tostring(dataJson))
    toast("WebUI 提交了表单数据")
end)

plugin.events.register("WEBUI_webui:reset", function(pluginId, dataJson)
    log("[WebUI事件] 收到 webui:reset 事件")
    toast("WebUI 请求重置")

    -- 向 Web 推送重置命令
    plugin.webui.sendToWeb('{"action":"reset","timestamp":"' .. os.date("%H:%M:%S") .. '"}')
end)

-- ==================== 菜单注册 ====================

-- 打开 WebUI 主界面
plugin.menu.addMenuItem("webui_open", "【WebUI】打开 Web 界面", function()
    local ok = plugin.webui.open("index.html")
    if ok then
        log("WebUI 已打开")
        toast("WebUI 已打开！")
    else
        toast("WebUI 打开失败，检查 web/index.html 是否存在")
    end
end)

-- 向 WebUI 发送消息
plugin.menu.addMenuDivider("webui_div1")
plugin.menu.addMenuItem("webui_push", "【WebUI】向 Web 推送消息", function()
    if plugin.webui.isShowing() then
        local msg = '{"action":"push","from":"Lua", "time":"' .. os.date("%H:%M:%S") .. '", "count":' .. math.random(10, 99) .. '}'
        plugin.webui.sendToWeb(msg)
        toast("已向 Web 推送消息")
        log("发送 Web 消息: " .. msg)
    else
        toast("请先打开 WebUI 界面")
    end
end)

-- 向 WebUI 执行 JS
plugin.menu.addMenuItem("webui_js", "【WebUI】执行 JavaScript", function()
    if plugin.webui.isShowing() then
        local js = "document.body.style.backgroundColor = '" ..
            ({"#ffebee","#e8f5e9","#e3f2fd","#fff3e0","#f3e5f5"})[math.random(1,5)] ..
            "'; LXC.toast('背景色已变');"
        plugin.webui.evaluateJs(js)
        toast("已执行 JS 指令")
    else
        toast("请先打开 WebUI 界面")
    end
end)

-- 重置计数器
plugin.menu.addMenuItem("webui_reset", "【WebUI】重置计数器", function()
    plugin.config.setConfig("webui_counter", "0")
    if plugin.webui.isShowing() then
        plugin.webui.sendToWeb('{"action":"reset","counter":0}')
    end
    toast("计数器已重置")
end)

-- ==================== 初始化 ====================

log("WebUI 示例插件已加载")
toast("WebUI 插件已就绪！\n工具栏 ... → 【WebUI】打开 Web 界面")

-- 提示: 
-- 1. 点击菜单【WebUI】打开 Web 界面 即可看到 HTML 页面
-- 2. 在 Web 页面中点击按钮测试 JS→Lua 调用
-- 3. 点击菜单【WebUI】推送消息测试 Lua→JS 通信