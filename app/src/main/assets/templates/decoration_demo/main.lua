--[[
    decoration_demo — 编辑器装饰 API 示例插件
    
    演示 plugin.decoration 的所有功能:
    - 行背景色高亮
    - Gutter（行号区）背景色和图标
    - 多种图标类型 (error, warning, info, bookmark, arrow, star, dot)
    
    使用方法:
    1. 在 LuaForge Studio 中打开此插件
    2. 打开菜单 → 点击"编辑器装饰"菜单项
    3. 选择要测试的装饰类型
--]]

local log = plugin.logger.debug
local TAG = "DecorationDemo"

-- 存储当前激活的装饰行号（用于清除/切换）
local decoratedLines = {}

---- ---- 工具函数 ---- ----

-- 清除所有已设置的装饰
local function clearAll()
    local count = plugin.decoration.clearMyDecorations()
    plugin.sys.toast("已清除 " .. count .. " 个装饰")
    decoratedLines = {}
end

-- 获取当前文件的可见行数（估算）
local function getLineCount()
    local text = plugin.editor.getText()
    if not text then return 30 end
    local count = 0
    for _ in text:gmatch("[^\n]*\n?") do
        count = count + 1
    end
    return math.max(count, 1)
end

---- ---- 装饰操作函数 ---- ----

-- 示例1: 行背景色 — 给偶数行添加浅蓝色背景
function demo_line_background()
    clearAll()
    local lineCount = getLineCount()
    local count = 0
    for i = 0, lineCount - 1, 2 do
        -- 0x1A2196F3 = 10% 透明度的蓝色
        if plugin.decoration.setLineBackground(i, 0x1A2196F3) then
            count = count + 1
        end
    end
    plugin.sys.toast("已为 " .. count .. " 个偶数行设置浅蓝背景")
end

-- 示例2: 行背景色 — 给每第5行添加浅绿色背景（模拟代码块间隔）
function demo_line_background_green()
    clearAll()
    local lineCount = getLineCount()
    local count = 0
    for i = 0, lineCount - 1, 5 do
        -- 0x1A4CAF50 = 10% 透明度的绿色
        if plugin.decoration.setLineBackground(i, 0x1A4CAF50) then
            count = count + 1
        end
    end
    plugin.sys.toast("已为 " .. count .. " 个行设置浅绿背景（每5行）")
end

-- 示例3: 批量设置行背景色
function demo_line_background_batch()
    clearAll()
    local lines = {}
    for i = 3, 8 do
        table.insert(lines, i)
    end
    -- 0x1AFF5722 = 10%透明度的橙色
    local count = plugin.decoration.setLineBackgrounds(lines, 0x1AFF5722)
    plugin.sys.toast("已为第3-8行批量设置橙色背景，成功 " .. count .. " 行")
end

-- 示例4: Gutter 图标 — 模拟代码诊断（错误/警告/信息）
function demo_gutter_icons()
    clearAll()
    local lineCount = getLineCount()
    if lineCount >= 10 then
        plugin.decoration.setGutterIcon(2, "error")
        plugin.decoration.setGutterIcon(5, "warning")
        plugin.decoration.setGutterIcon(8, "info")
        plugin.decoration.setGutterIcon(12, "bookmark")
        plugin.decoration.setGutterIcon(16, "arrow")
        plugin.decoration.setGutterIcon(20, "star")
        plugin.decoration.setGutterIcon(24, "dot")
        plugin.sys.toast("已设置7种不同 Gutter 图标")
    else
        plugin.sys.toast("文件行数太少，请打开一个更长的文件")
    end
end

-- 示例5: Gutter 背景色 — 给行号区域着色
function demo_gutter_background()
    clearAll()
    local lineCount = getLineCount()
    local count = 0
    for i = 0, math.min(lineCount - 1, 20) do
        if i % 2 == 0 then
            -- 0x1AFFCC00 = 10%透明度的黄色
            if plugin.decoration.setGutterBackground(i, 0x1AFFCC00) then
                count = count + 1
            end
        end
    end
    plugin.sys.toast("已为 " .. count .. " 行设置黄色 Gutter 背景")
end

-- 示例6: 组合装饰 — 行背景 + Gutter 图标
function demo_combined()
    clearAll()
    local lineCount = getLineCount()
    if lineCount >= 15 then
        -- 第3行: 错误 — 红色背景 + error 图标
        plugin.decoration.setLineBackground(3, 0x20FF0000)
        plugin.decoration.setGutterIcon(3, "error")
        
        -- 第7行: 警告 — 黄色背景 + warning 图标
        plugin.decoration.setLineBackground(7, 0x20FFCC00)
        plugin.decoration.setGutterIcon(7, "warning")
        
        -- 第11行: 信息 — 蓝色背景 + info 图标  
        plugin.decoration.setLineBackground(11, 0x202196F3)
        plugin.decoration.setGutterIcon(11, "info")
        
        -- 第15行: 书签 — 绿色背景 + bookmark 图标
        plugin.decoration.setLineBackground(15, 0x204CAF50)
        plugin.decoration.setGutterIcon(15, "bookmark")
        
        plugin.sys.toast("已设置4行组合装饰 (error/warning/info/bookmark)")
    else
        plugin.sys.toast("文件行数不足，请打开至少16行的文件")
    end
end

-- 示例7: 单行装饰操作
function demo_single_line()
    clearAll()
    -- 获取光标所在行
    local pos = plugin.editor.getCursorPosition()
    if pos and pos[1] then
        local line = pos[1]
        plugin.decoration.setLineBackground(line, 0x30E91E63)  -- 粉色背景
        plugin.decoration.setGutterIcon(line, "star")            -- 星形图标
        plugin.sys.toast("已装饰当前光标行 " .. line)
    else
        plugin.sys.toast("无法获取光标位置")
    end
end

-- 示例8: 查询当前装饰
function demo_query()
    local decorations = plugin.decoration.getMyDecorations()
    if #decorations == 0 then
        plugin.sys.toast("当前文件没有装饰")
        return
    end
    
    local msg = "当前文件有 " .. #decorations .. " 个装饰:\n"
    for i, d in ipairs(decorations) do
        local parts = {}
        if d.gutterIconType then
            table.insert(parts, "图标=" .. d.gutterIconType)
        end
        if d.lineBackgroundColor then
            table.insert(parts, "背景色=" .. d.lineBackgroundColor)
        end
        if d.gutterBackgroundColor then
            table.insert(parts, "gutter色=" .. d.gutterBackgroundColor)
        end
        msg = msg .. "  行" .. d.line .. ": " .. table.concat(parts, ", ") .. "\n"
    end
    plugin.sys.toast(msg)
end

---- ---- 菜单注册 ---- ----
-- 注册到编辑器工具栏下拉菜单 (点击编辑器顶部工具栏 "..." 按钮可见)

plugin.menu.addMenuDivider("decoration")
plugin.menu.addMenuItem("decoration_line_bg", "【装饰】偶数行蓝色背景", demo_line_background)
plugin.menu.addMenuItem("decoration_line_green", "【装饰】每5行绿色背景", demo_line_background_green)
plugin.menu.addMenuItem("decoration_line_batch", "【装饰】第3-8行橙色批量", demo_line_background_batch)
plugin.menu.addMenuDivider("decoration_div1")
plugin.menu.addMenuItem("decoration_icons", "【装饰】Gutter 七种图标", demo_gutter_icons)
plugin.menu.addMenuItem("decoration_gutter_bg", "【装饰】Gutter 黄色背景", demo_gutter_background)
plugin.menu.addMenuDivider("decoration_div2")
plugin.menu.addMenuItem("decoration_combined", "【装饰】组合装饰(错误/警告等)", demo_combined)
plugin.menu.addMenuItem("decoration_single", "【装饰】装饰当前光标行", demo_single_line)
plugin.menu.addMenuDivider("decoration_div3")
plugin.menu.addMenuItem("decoration_query", "【装饰】查询当前装饰", demo_query)
plugin.menu.addMenuItem("decoration_clear", "【装饰】清除所有装饰", clearAll)

log(TAG, "装饰插件示例已加载")
plugin.sys.toast("装饰插件已就绪！\n打开任意文件 → 点击编辑器工具栏 ... 按钮 → 可见菜单项")