-- ============================================================
-- 新API演示插件 — plugin.logger / plugin.assets / plugin.shortcut
-- 全面展示三大新增 API 的用法与最佳实践
-- ============================================================

-- —————— 生命周期日志记录 ——————
plugin.logger.info("新API演示", "插件开始加载")
plugin.logger.debug("新API演示", "插件ID: " .. plugin.getPluginId())
plugin.logger.debug("新API演示", "API版本: " .. tostring(plugin.getApiVersion()))

-- —————— 辅助函数 ——————
local function pname()
    return plugin.manager.getPluginName(plugin.getPluginId()) or plugin.getPluginId()
end

local function logInfo(msg)
    plugin.logger.info("新API演示", msg)
end

local function logWarn(msg)
    plugin.logger.warn("新API演示", msg)
end

local function logError(msg, err)
    plugin.logger.error("新API演示", msg, err)
end

-- —————— 辅助: 格式化文件大小 ——————
local function fmtSize(bytes)
    if bytes < 1024 then return bytes .. " B" end
    if bytes < 1048576 then return string.format("%.2f KB", bytes / 1024) end
    return string.format("%.2f MB", bytes / 1048576)
end

-- ============================================================
-- 第一部分: plugin.logger — 日志系统演示
-- ============================================================

-- 1.1 记录各种级别的日志
plugin.logger.debug("新API演示", "这是一条 DEBUG 日志，用于调试")
plugin.logger.info("新API演示", "这是一条 INFO 日志，表示正常信息")
plugin.logger.warn("新API演示", "这是一条 WARN 日志，表示警告")
plugin.logger.error("新API演示", "这是一条 ERROR 日志，表示错误", "堆栈: 模拟异常")

-- 1.2 快捷操作: 查看日志文件列表
plugin.menu.addQuickAction("log_files", "📋 日志文件列表", function()
    local files = plugin.logger.listLogFiles()
    local logDir = plugin.logger.getLogDir()
    local info = "日志目录:\n" .. logDir .. "\n\n"
    info = info .. "日志文件数: " .. #files .. "\n\n"
    if #files > 0 then
        for i, name in ipairs(files) do
            local size = plugin.logger.getLogFileSize(name)
            info = info .. i .. ". " .. name .. " (" .. fmtSize(size) .. ")\n"
        end
        info = info .. "\n总行数: " .. plugin.logger.getLogLineCount()
    end
    plugin.ui.showMessage("日志文件列表", info)
end)

-- 1.3 快捷操作: 查看最新日志
plugin.menu.addQuickAction("log_latest", "📜 查看最新日志", function()
    local content = plugin.logger.getLatestLog()
    if content and #content > 0 then
        plugin.ui.showTextDialog("最新日志", content)
    else
        plugin.sys.toast("暂无日志内容，试试先执行一些操作")
    end
end)

-- 1.4 快捷操作: 搜索日志
plugin.menu.addQuickAction("log_search", "🔍 搜索日志", function()
    plugin.ui.showInputDialog("搜索日志", "输入关键词（如 ERROR, WARN）", "ERROR", {
        onInput = function(kw)
            local results = plugin.logger.searchLogs(kw, 30)
            if results and #results > 0 then
                local info = "搜索: " .. kw .. "\n"
                info = info .. "匹配: " .. #results .. " 条\n"
                info = info .. "────────────────────\n\n"
                for i, entry in ipairs(results) do
                    info = info .. "[" .. entry.timestamp .. "]"
                    info = info .. " [" .. entry.level .. "]"
                    info = info .. " " .. entry.message .. "\n"
                end
                plugin.ui.showTextDialog("搜索结果", info)
            else
                plugin.sys.toast("未找到包含 '" .. kw .. "' 的日志")
            end
        end
    })
end)

-- 1.5 快捷操作: 日志统计
plugin.menu.addQuickAction("log_stats", "📊 日志统计", function()
    local files = plugin.logger.listLogFiles()
    local lineCount = plugin.logger.getLogLineCount()
    local totalSize = 0
    for _, name in ipairs(files) do
        totalSize = totalSize + plugin.logger.getLogFileSize(name)
    end
    local info = "日志文件数: " .. #files .. "\n"
    info = info .. "日志总行数: " .. lineCount .. "\n"
    info = info .. "日志总大小: " .. fmtSize(totalSize) .. "\n"
    info = info .. "日志目录: " .. plugin.logger.getLogDir()
    plugin.ui.showMessage("日志统计", info)
end)

-- 1.6 快捷操作: 清空日志
plugin.menu.addQuickAction("log_clear", "🗑 清空日志", function()
    plugin.ui.showConfirm("清空日志", "确定要删除所有日志文件吗？", function()
        local ok = plugin.logger.clearLogs()
        plugin.sys.toast(ok and "日志已清空" or "清空失败")
        if ok then logInfo("日志已清空") end
    end, nil)
end)

-- 1.7 快捷操作: 跨插件日志访问
plugin.menu.addQuickAction("log_cross", "🔗 查看其他插件日志", function()
    local plugins = plugin.manager.getPluginIds()
    local items = {}
    local idList = {}
    for i, pid in ipairs(plugins) do
        if pid ~= plugin.getPluginId() then
            local name = plugin.manager.getPluginName(pid) or pid
            table.insert(items, name .. " (" .. pid .. ")")
            table.insert(idList, pid)
        end
    end

    if #items == 0 then
        plugin.sys.toast("没有其他已安装的插件")
        return
    end

    plugin.ui.showSingleChoiceDialog("选择要查看日志的插件", items, 0, {
        onSelect = function(index)
            local targetId = idList[index + 1]
            local targetName = plugin.manager.getPluginName(targetId) or targetId
            local files = plugin.logger.listPluginLogFiles(targetId)
            local logDir = plugin.logger.getPluginLogDir(targetId)

            if files and #files > 0 then
                local content = plugin.logger.readPluginLogFile(targetId, files[1])
                local lineCount = plugin.logger.getPluginLogLineCount(targetId)
                local info = "插件: " .. targetName .. " (" .. targetId .. ")\n"
                info = info .. "日志文件: " .. files[1] .. "\n"
                info = info .. "总行数: " .. lineCount .. "\n"
                info = info .. "────────────────────\n\n"
                info = info .. content
                plugin.ui.showTextDialog("插件日志: " .. targetName, info)
            else
                plugin.sys.toast("插件 " .. targetName .. " 暂无日志文件\n日志目录: " .. logDir)
            end
        end
    })
end)

-- ============================================================
-- 第二部分: plugin.assets — 资源注册表演示
-- ============================================================

-- 2.1 自动注册当前项目入口文件为资源（如果项目已打开）
local function autoRegisterEntryFile()
    local projectPath = plugin.project.getPath()
    if not projectPath then return false end

    local entryFile = projectPath .. "/main.lua"
    local f = io.open(entryFile, "r")
    if not f then return false end
    f:close()

    local result = plugin.assets.registerAsset(
        "project_entry",
        "data",
        entryFile,
        "项目入口文件",
        "当前打开项目的 main.lua 入口文件",
        true,
        { auto_registered = "true", file_type = "lua" }
    )
    if result then
        logInfo("自动注册资源: " .. result)
        return true
    end
    return false
end

local entryRegistered = autoRegisterEntryFile()

-- 2.2 快捷操作: 注册资源
plugin.menu.addQuickAction("asset_reg", "📦 注册资源", function()
    plugin.ui.showInputDialog("注册资源", "输入资源文件路径", "", {
        onInput = function(path)
            local f = io.open(path, "r")
            if not f then
                plugin.sys.toast("文件不存在: " .. path)
                return
            end
            f:close()

            -- 根据扩展名推断类型
            local ext = path:match("%.([^%.]+)$") or "raw"
            local typeMap = {
                png = "image", jpg = "image", jpeg = "image", gif = "image", bmp = "image",
                mp3 = "audio", wav = "audio", ogg = "audio", flac = "audio",
                ttf = "font", otf = "font",
                json = "data", xml = "data", csv = "data",
                lua = "data", txt = "data"
            }
            local rtype = typeMap[ext:lower()] or "raw"

            local filename = path:match("[^/\\]+$") or "unknown"
            local result = plugin.assets.registerAsset(
                "user_reg_" .. os.time(),
                rtype,
                path,
                filename,
                "用户手动注册的资源",
                true,
                { extension = ext, original_path = path }
            )
            if result then
                logInfo("用户注册资源: " .. result)
                plugin.sys.toast("资源注册成功: " .. result)
            else
                plugin.sys.toast("资源注册失败")
            end
        end
    })
end)

-- 2.3 快捷操作: 查看我的资源
plugin.menu.addQuickAction("asset_my", "📋 查看我的资源", function()
    local assets = plugin.assets.getMyAssets()
    local total = plugin.assets.getTotalAssetCount()
    if assets and #assets > 0 then
        local info = "我的资源: " .. #assets .. " 个（全局共 " .. total .. " 个）\n\n"
        for i, a in ipairs(assets) do
            local icon = a.isPublic and "🌐" or "🔒"
            info = info .. i .. ". " .. icon .. " " .. a.id .. "\n"
            info = info .. "   类型: " .. a.type .. " | 名称: " .. a.displayName .. "\n"
            info = info .. "   大小: " .. fmtSize(a.size) .. " | 扩展名: ." .. a.extension .. "\n"
            info = info .. "   描述: " .. a.description .. "\n\n"
        end
        plugin.ui.showTextDialog("我的资源", info)
    else
        plugin.sys.toast("你还没有注册任何资源（全局共 " .. total .. " 个）")
    end
end)

-- 2.4 快捷操作: 查看所有公开资源
plugin.menu.addQuickAction("asset_all", "🌐 查看所有公开资源", function()
    local all = plugin.assets.getAllPublicAssets()
    local total = plugin.assets.getTotalAssetCount()
    if all and #all > 0 then
        local info = "公开资源: " .. #all .. " 个（全局共 " .. total .. " 个）\n\n"
        for i, a in ipairs(all) do
            local owner = plugin.manager.getPluginName(a.pluginId) or a.pluginId
            info = info .. i .. ". [" .. owner .. "] " .. a.id .. "\n"
            info = info .. "   类型: " .. a.type .. " | " .. a.displayName .. "\n"
            info = info .. "   大小: " .. fmtSize(a.size) .. " | 公开: " .. (a.isPublic and "是" or "否") .. "\n\n"
        end
        plugin.ui.showTextDialog("公开资源列表", info)
    else
        plugin.sys.toast("暂无公开资源（全局共 " .. total .. " 个）")
    end
end)

-- 2.5 快捷操作: 按类型筛选
plugin.menu.addQuickAction("asset_type", "🏷 按类型筛选资源", function()
    local types = {"image", "audio", "font", "data", "layout", "icon", "raw"}
    plugin.ui.showSingleChoiceDialog("选择资源类型", types, 0, {
        onSelect = function(index)
            local t = types[index + 1]
            local assets = plugin.assets.getAssetsByType(t)
            local count = plugin.assets.getAssetCountByType(t)
            if assets and #assets > 0 then
                local info = "类型: " .. t .. " (" .. #assets .. " / " .. count .. " 个)\n\n"
                for i, a in ipairs(assets) do
                    info = info .. i .. ". " .. a.id .. " - " .. a.displayName .. "\n"
                end
                plugin.ui.showTextDialog("类型筛选", info)
            else
                plugin.sys.toast("没有 " .. t .. " 类型的资源（共 " .. count .. " 个）")
            end
        end
    })
end)

-- 2.6 快捷操作: 读取资源内容
plugin.menu.addQuickAction("asset_read", "📖 读取资源内容", function()
    local assets = plugin.assets.getMyAssets()
    if not assets or #assets == 0 then
        plugin.sys.toast("请先注册一个资源再读取")
        return
    end

    local items = {}
    for i, a in ipairs(assets) do
        table.insert(items, a.id .. "  [" .. a.type .. "]  " .. a.displayName)
    end

    plugin.ui.showSingleChoiceDialog("选择要读取的资源", items, 0, {
        onSelect = function(index)
            local a = assets[index + 1]
            if not plugin.assets.assetExists(a.id) then
                plugin.sys.toast("资源文件已不存在: " .. a.filePath)
                return
            end

            local text = plugin.assets.readAssetText(a.id)
            if text then
                local preview = text
                if #text > 2000 then
                    preview = text:sub(1, 2000) .. "\n\n... (内容过长已截断，共 " .. #text .. " 字符)"
                end
                plugin.ui.showTextDialog("资源: " .. a.id, preview)
            else
                local bytes = plugin.assets.readAssetBytes(a.id)
                if bytes then
                    plugin.sys.toast("二进制资源: " .. a.id .. " (" .. fmtSize(#bytes) .. " 字节)")
                else
                    plugin.sys.toast("无法读取资源: " .. a.id)
                end
            end
        end
    })
end)

-- 2.7 快捷操作: 注销资源
plugin.menu.addQuickAction("asset_unreg", "🗑 注销资源", function()
    local assets = plugin.assets.getMyAssets()
    if not assets or #assets == 0 then
        plugin.sys.toast("没有可注销的资源")
        return
    end

    local items = {}
    for i, a in ipairs(assets) do
        table.insert(items, a.id .. " - " .. a.displayName)
    end

    plugin.ui.showSingleChoiceDialog("选择要注销的资源", items, 0, {
        onSelect = function(index)
            local a = assets[index + 1]
            local ok = plugin.assets.unregisterAsset(a.key)
            plugin.sys.toast(ok and "已注销: " .. a.id or "注销失败")
            if ok then logInfo("注销资源: " .. a.id) end
        end
    })
end)

-- 2.8 快捷操作: 注销全部资源
plugin.menu.addQuickAction("asset_unregall", "🗑 注销全部资源", function()
    local assets = plugin.assets.getMyAssets()
    local n = assets and #assets or 0
    if n == 0 then
        plugin.sys.toast("没有可注销的资源")
        return
    end
    plugin.ui.showConfirm("注销全部资源", "确定要注销全部 " .. n .. " 个资源吗？", function()
        local removed = plugin.assets.unregisterAllAssets()
        plugin.sys.toast("已注销 " .. removed .. " 个资源")
        logInfo("注销全部资源: " .. removed .. " 个")
        entryRegistered = false
    end, nil)
end)

-- ============================================================
-- 第三部分: plugin.shortcut — 快捷键绑定演示
-- ============================================================

-- 3.1 注册: Ctrl+Shift+F → 格式化代码（冲突检测后注册）
local taken = plugin.shortcut.isCombinationTaken("Ctrl+Shift+F")
if taken then
    logWarn("Ctrl+Shift+F 已被 [" .. taken.pluginId .. "] 占用 (" .. taken.label .. ")，跳过注册")
else
    local r = plugin.shortcut.register(
        "format_code",
        "Ctrl+Shift+F",
        "格式化代码",
        "按下 Ctrl+Shift+F 格式化当前编辑器文件",
        function(combo)
            plugin.editor.saveActiveFile()
            plugin.sys.toast("Ctrl+Shift+F → 已保存当前文件")
            logInfo("快捷键触发: " .. combo)
        end,
        true
    )
    if r then logInfo("快捷键注册成功: " .. r) end
end

-- 3.2 注册: Ctrl+D → 复制选中文本
plugin.shortcut.register(
    "duplicate_line",
    "Ctrl+D",
    "复制选中文本",
    "按下 Ctrl+D 复制当前选中的文本并插入",
    function(combo)
        local text = plugin.editor.getSelectedText()
        if text and #text > 0 then
            plugin.editor.insertText(text)
            plugin.sys.toast("Ctrl+D → 已复制选中文本")
        else
            plugin.sys.toast("Ctrl+D → 请先选中文本")
        end
        logInfo("快捷键触发: " .. combo)
    end,
    true
)

-- 3.3 注册: Alt+G → 跳转到行
plugin.shortcut.register(
    "goto_line",
    "Alt+G",
    "跳转到行",
    "按下 Alt+G 弹出对话框输入行号跳转",
    function(combo)
        plugin.ui.showInputDialog("跳转到行", "请输入行号", "1", {
            onInput = function(text)
                local line = tonumber(text)
                if line and line > 0 then
                    plugin.editor.gotoLine(line - 1)
                    plugin.sys.toast("Alt+G → 已跳转到第 " .. line .. " 行")
                    logInfo("快捷键跳转: 第 " .. line .. " 行")
                end
            end
        })
    end,
    true
)

-- 3.4 注册: Ctrl+Shift+L → 查看日志（无冲突检测的简化版）
plugin.shortcut.register(
    "view_log",
    "Ctrl+Shift+L",
    "查看日志",
    "按下 Ctrl+Shift+L 快速查看最新日志",
    function(combo)
        local content = plugin.logger.getLatestLog()
        if content and #content > 0 then
            plugin.ui.showTextDialog("最新日志 (Ctrl+Shift+L)", content)
        else
            plugin.sys.toast("暂无日志")
        end
    end,
    true
)

-- 3.5 快捷操作: 查看我的快捷键
plugin.menu.addQuickAction("shortcut_my", "⌨ 查看我的快捷键", function()
    local shortcuts = plugin.shortcut.getMyShortcuts()
    local total = plugin.shortcut.getShortcutCount()
    if shortcuts and #shortcuts > 0 then
        local info = "我的快捷键: " .. #shortcuts .. " 个（全局共 " .. total .. " 个）\n\n"
        for i, sc in ipairs(shortcuts) do
            info = info .. i .. ". " .. sc.combination .. " → " .. sc.label .. "\n"
            if sc.description ~= "" then
                info = info .. "   " .. sc.description .. "\n"
            end
            info = info .. "   仅编辑器: " .. (sc.editorOnly and "是" or "否") .. "\n\n"
        end
        plugin.ui.showMessage("我的快捷键", info)
    else
        plugin.sys.toast("未注册任何快捷键")
    end
end)

-- 3.6 快捷操作: 查看所有快捷键
plugin.menu.addQuickAction("shortcut_all", "⌨ 查看全部快捷键", function()
    local all = plugin.shortcut.getAllShortcuts()
    local count = plugin.shortcut.getShortcutCount()
    if all and #all > 0 then
        local info = "全部快捷键 (" .. count .. " 个):\n\n"
        for i, sc in ipairs(all) do
            local owner = plugin.manager.getPluginName(sc.pluginId) or sc.pluginId
            info = info .. i .. ". [" .. owner .. "] " .. sc.combination .. " → " .. sc.label .. "\n"
        end
        plugin.ui.showTextDialog("全部快捷键", info)
    else
        plugin.sys.toast("暂无已注册的快捷键")
    end
end)

-- 3.7 快捷操作: 冲突检测
plugin.menu.addQuickAction("shortcut_conf", "⚠ 检测快捷键冲突", function()
    plugin.ui.showInputDialog("冲突检测", "输入组合键", "Ctrl+S", {
        onInput = function(text)
            local taken = plugin.shortcut.isCombinationTaken(text)
            if taken then
                local owner = plugin.manager.getPluginName(taken.pluginId) or taken.pluginId
                plugin.ui.showMessage("冲突检测",
                    "组合键 " .. text .. " 已被占用:\n\n" ..
                    "注册者: " .. owner .. " (" .. taken.pluginId .. ")\n" ..
                    "用途: " .. taken.label .. "\n" ..
                    "描述: " .. taken.description .. "\n" ..
                    "ID: " .. taken.id)
            else
                plugin.sys.toast("组合键 " .. text .. " 未被占用，可以注册")
            end
        end
    })
end)

-- 3.8 快捷操作: 注销快捷键
plugin.menu.addQuickAction("shortcut_unreg", "🗑 注销快捷键", function()
    local shortcuts = plugin.shortcut.getMyShortcuts()
    if not shortcuts or #shortcuts == 0 then
        plugin.sys.toast("没有可注销的快捷键")
        return
    end

    local items = {}
    for i, sc in ipairs(shortcuts) do
        table.insert(items, sc.combination .. " - " .. sc.label)
    end

    plugin.ui.showSingleChoiceDialog("选择要注销的快捷键", items, 0, {
        onSelect = function(index)
            local sc = shortcuts[index + 1]
            local ok = plugin.shortcut.unregister(sc.key)
            plugin.sys.toast(ok and "已注销: " .. sc.combination or "注销失败")
            if ok then logInfo("注销快捷键: " .. sc.combination) end
        end
    })
end)

-- 3.9 快捷操作: 重新注册全部快捷键
plugin.menu.addQuickAction("shortcut_rereg", "🔄 重新注册快捷键", function()
    plugin.shortcut.unregisterAll()
    plugin.shortcut.register("format_code", "Ctrl+Shift+F", "格式化代码", "格式化当前文件", function(combo)
        plugin.editor.saveActiveFile()
        plugin.sys.toast("格式化代码!")
    end, true)
    plugin.shortcut.register("duplicate_line", "Ctrl+D", "复制选中文本", "复制并插入文本", function(combo)
        local t = plugin.editor.getSelectedText()
        if t and #t > 0 then plugin.editor.insertText(t) end
    end, true)
    plugin.shortcut.register("goto_line", "Alt+G", "跳转到行", "输入行号跳转", function(combo)
        plugin.ui.showInputDialog("跳转", "行号", "1", {
            onInput = function(txt)
                local n = tonumber(txt)
                if n then plugin.editor.gotoLine(n - 1) end
            end
        })
    end, true)
    plugin.shortcut.register("view_log", "Ctrl+Shift+L", "查看日志", "快速查看最新日志", function(combo)
        local c = plugin.logger.getLatestLog()
        if c and #c > 0 then plugin.ui.showTextDialog("日志", c) end
    end, true)
    plugin.sys.toast("已重新注册 4 个快捷键")
    logInfo("重新注册全部快捷键")
end)

-- ============================================================
-- 第四部分: 综合演示 — 跨 API 组合使用
-- ============================================================

-- 4.1 组合: 注册资源 + 记录日志
plugin.menu.addQuickAction("combo_log_asset", "🔗 组合: 资源→日志", function()
    local assets = plugin.assets.getMyAssets()
    if not assets or #assets == 0 then
        plugin.sys.toast("请先注册资源")
        return
    end
    local count = #assets
    logInfo("资源统计: 共 " .. count .. " 个资源")
    for i, a in ipairs(assets) do
        logInfo("资源 #" .. i .. ": " .. a.id .. " (" .. fmtSize(a.size) .. ")")
    end
    plugin.sys.toast("已将 " .. count .. " 个资源信息写入日志")
end)

-- 4.2 组合: 快捷键触发资源读取
plugin.menu.addQuickAction("combo_shortcut_asset", "🔗 组合: 快捷键→资源", function()
    plugin.shortcut.register(
        "combo_read_asset",
        "Ctrl+Shift+R",
        "读取第一个资源",
        "按下 Ctrl+Shift+R 读取第一个已注册资源的内容",
        function(combo)
            local assets = plugin.assets.getMyAssets()
            if not assets or #assets == 0 then
                plugin.sys.toast("没有已注册的资源")
                logWarn("Ctrl+Shift+R 触发但无资源可读")
                return
            end
            local a = assets[1]
            local text = plugin.assets.readAssetText(a.id)
            if text then
                plugin.ui.showTextDialog("快捷键读取: " .. a.id, text:sub(1, 500))
                logInfo("Ctrl+Shift+R 读取资源: " .. a.id)
            else
                plugin.sys.toast("无法读取: " .. a.id)
            end
        end,
        true
    )
    plugin.sys.toast("已注册 Ctrl+Shift+R → 读取第一个资源")
    logInfo("注册组合快捷键: Ctrl+Shift+R")
end)

-- 4.3 组合: 日志→资源注册（从日志中提取信息注册资源）
plugin.menu.addQuickAction("combo_log_to_asset", "🔗 组合: 日志→资源", function()
    local results = plugin.logger.searchLogs("注册", 10)
    if results and #results > 0 then
        local info = "从日志中搜索到 " .. #results .. " 条与「注册」相关的记录\n\n"
        for i, entry in ipairs(results) do
            info = info .. "[" .. entry.timestamp .. "] " .. entry.message .. "\n"
        end
        plugin.ui.showTextDialog("日志→资源 分析", info)
        logInfo("组合操作: 日志→资源 分析完成")
    else
        plugin.sys.toast("日志中暂无「注册」相关记录")
    end
end)

-- 4.4 快捷操作: 查看本插件信息
plugin.menu.addQuickAction("combo_info", "ℹ 插件信息", function()
    local assets = plugin.assets.getMyAssets()
    local shortcuts = plugin.shortcut.getMyShortcuts()
    local logFiles = plugin.logger.listLogFiles()
    local logLines = plugin.logger.getLogLineCount()

    local info = "=== 新API演示插件 ===\n\n"
    info = info .. "插件ID: " .. plugin.getPluginId() .. "\n"
    info = info .. "API版本: " .. tostring(plugin.getApiVersion()) .. "\n"
    info = info .. "\n【日志系统】\n"
    info = info .. "  日志文件: " .. #logFiles .. " 个\n"
    info = info .. "  日志行数: " .. logLines .. " 行\n"
    info = info .. "  日志目录: " .. plugin.logger.getLogDir() .. "\n"
    info = info .. "\n【资源注册表】\n"
    info = info .. "  已注册资源: " .. #assets .. " 个\n"
    local entered = entryRegistered and "是" or "否"
    info = info .. "  自动注册入口: " .. entered .. "\n"
    info = info .. "\n【快捷键】\n"
    info = info .. "  已注册快捷键: " .. #shortcuts .. " 个\n"
    for i, sc in ipairs(shortcuts) do
        info = info .. "  " .. sc.combination .. " → " .. sc.label .. "\n"
    end
    info = info .. "\n【全局资源】\n"
    info = info .. "  全局资源总数: " .. plugin.assets.getTotalAssetCount() .. " 个\n"
    info = info .. "  全局快捷键总数: " .. plugin.shortcut.getShortcutCount() .. " 个\n"
    info = info .. "\n【通知系统】\n"
    info = info .. "  活跃通知: " .. plugin.notification.getActiveCount() .. " 条\n"
    info = info .. "  本插件通知: " .. plugin.notification.getMyActiveCount() .. " 条\n"
    info = info .. "  历史通知: " .. plugin.notification.getHistoryCount() .. " 条\n"

    plugin.ui.showMessage("插件信息", info)
end)

-- ============================================================
-- 第五部分: plugin.notification — 通知系统演示
-- ============================================================

-- 5.1 快捷操作: 发送 info 通知
plugin.menu.addQuickAction("notif_info", "🔔 发送Info通知", function()
    plugin.ui.showInputDialog("Info通知", "输入通知内容", "这是一条信息通知", {
        onInput = function(text)
            plugin.notification.show("信息通知", text, "info")
            plugin.sys.toast("已发送 info 通知")
        end
    })
end)

-- 5.2 快捷操作: 发送 success 通知
plugin.menu.addQuickAction("notif_success", "✅ 发送成功通知", function()
    plugin.notification.success("操作成功", "文件已成功保存到磁盘")
    plugin.logger.info("新API演示", "发送成功通知")
end)

-- 5.3 快捷操作: 发送 warn 通知
plugin.menu.addQuickAction("notif_warn", "⚠ 发送警告通知", function()
    plugin.notification.warn("内存不足", "可用内存低于 50MB，建议关闭未使用的文件")
    plugin.logger.warn("新API演示", "发送警告通知")
end)

-- 5.4 快捷操作: 发送 error 通知
plugin.menu.addQuickAction("notif_error", "❌ 发送错误通知", function()
    plugin.notification.error("编译失败", "main.lua 第 42 行: 语法错误，缺少 end")
    plugin.logger.error("新API演示", "发送错误通知", "模拟编译错误")
end)

-- 5.5 快捷操作: 分组通知演示
plugin.menu.addQuickAction("notif_group", "📂 分组通知演示", function()
    plugin.notification.info("构建系统", "开始编译项目...", "build")
    plugin.notification.success("构建系统", "编译完成", "build")
    plugin.notification.info("代码检查", "开始静态分析...", "lint")
    plugin.notification.warn("代码检查", "发现 3 个潜在问题", "lint")
    plugin.sys.toast("已发送 4 条分组通知 (build + lint)")
    plugin.logger.info("新API演示", "发送分组通知: build 组 2 条 + lint 组 2 条")
end)

-- 5.6 快捷操作: 不自动消失的通知
plugin.menu.addQuickAction("notif_sticky", "📌 置顶通知(不消失)", function()
    plugin.ui.showInputDialog("置顶通知", "输入通知内容", "请及时处理此问题", {
        onInput = function(text)
            plugin.notification.show("重要提醒", text, "warn", "important", 0)
            plugin.sys.toast("已发送置顶通知（不会自动消失）")
        end
    })
end)

-- 5.7 快捷操作: 仅IDE内通知（不推送系统栏）
plugin.menu.addQuickAction("notif_ideonly", "💻 仅IDE内通知", function()
    plugin.notification.show("IDE内部通知", "此通知仅在IDE内显示，不会推送到系统通知栏", "info", "internal", 3000, false)
    plugin.sys.toast("已发送仅IDE内通知")
end)

-- 5.8 快捷操作: 查看通知统计
plugin.menu.addQuickAction("notif_stats", "📊 通知统计", function()
    local active = plugin.notification.getActiveCount()
    local myActive = plugin.notification.getMyActiveCount()
    local history = plugin.notification.getHistoryCount()

    local info = "=== 通知系统统计 ===\n\n"
    info = info .. "活跃通知总数: " .. active .. "\n"
    info = info .. "本插件活跃通知: " .. myActive .. "\n"
    info = info .. "历史通知总数: " .. history .. "\n\n"
    info = info .. "各分组通知数:\n"
    info = info .. "  build 组: " .. plugin.notification.getGroupCount("build") .. "\n"
    info = info .. "  lint 组: " .. plugin.notification.getGroupCount("lint") .. "\n"
    info = info .. "  important 组: " .. plugin.notification.getGroupCount("important") .. "\n"

    plugin.ui.showMessage("通知统计", info)
end)

-- 5.9 快捷操作: 清除本插件通知
plugin.menu.addQuickAction("notif_clear", "🗑 清除本插件通知", function()
    local count = plugin.notification.getMyActiveCount()
    plugin.notification.clear()
    plugin.sys.toast("已清除 " .. count .. " 条通知")
    plugin.logger.info("新API演示", "清除本插件通知: " .. count .. " 条")
end)

-- 5.10 快捷操作: 清除所有通知
plugin.menu.addQuickAction("notif_clearall", "🗑 清除所有通知", function()
    local count = plugin.notification.getActiveCount()
    plugin.ui.showConfirm("清除所有通知", "确定要清除全部 " .. count .. " 条活跃通知吗？", function()
        plugin.notification.clearAll()
        plugin.sys.toast("已清除所有通知")
        plugin.logger.info("新API演示", "清除所有通知")
    end, nil)
end)

-- 5.11 快捷操作: 清除历史通知
plugin.menu.addQuickAction("notif_clearhistory", "🧹 清除历史通知", function()
    local count = plugin.notification.getHistoryCount()
    plugin.notification.clearAllHistory()
    plugin.sys.toast("已清除 " .. count .. " 条历史通知")
    plugin.logger.info("新API演示", "清除历史通知: " .. count .. " 条")
end)

-- 5.12 快捷操作: 完整通知流程演示
plugin.menu.addQuickAction("notif_demo", "🎬 完整通知流程演示", function()
    plugin.notification.info("流程演示", "步骤 1/4: 开始处理任务...", "demo")
    plugin.logger.info("新API演示", "演示流程开始")

    plugin.notification.success("流程演示", "步骤 2/4: 数据加载完成", "demo")
    plugin.notification.warn("流程演示", "步骤 3/4: 检测到异常配置，已自动修正", "demo")
    plugin.notification.error("流程演示", "步骤 4/4: 部分文件处理失败，请检查日志", "demo")

    plugin.sys.toast("完整通知流程演示完成，请查看通知中心")
    plugin.logger.info("新API演示", "演示流程完成，共发送 4 条通知")
end)

-- ============================================================
-- 初始化完成
-- ============================================================
plugin.logger.info("新API演示", "插件初始化完成")
plugin.logger.info("新API演示", "已注册 4 个快捷键 + " .. (entryRegistered and "1 个自动资源" or "0 个资源") .. " + 29 个快捷操作菜单 (含通知系统 12 个)")
plugin.sys.toast("新API演示插件已加载 ✓")