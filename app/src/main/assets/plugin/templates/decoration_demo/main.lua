--[[
    decoration_demo — 编辑器装饰 API 完整示例插件
    
    演示 plugin.decoration 的所有功能:
    - 行背景色 / Gutter 背景色 / Gutter 图标
    - 装饰分类 (category): breakpoint, highlight, error, warning 等
    - 分类导航: 按类别跳转到下一个/上一个装饰行
    - 事件监听: 点击/长按/双击装饰行 + Gutter 图标点击
    
    使用方法:
    1. 打开任意文件
    2. 点击编辑器顶部工具栏 "..." 按钮打开下拉菜单
    3. 选择【装饰】相关菜单项测试
--]]

local log = plugin.logger.debug
local TAG = "DecorationDemo"

---- ==================== 工具函数 ==================== ----

local function getLineCount()
    local text = plugin.editor.getText()
    if not text then return 30 end
    local count = 0
    for _ in text:gmatch("[^\n]*\n?") do
        count = count + 1
    end
    return math.max(count, 1)
end

local function clearAll()
    local count = plugin.decoration.clearMyDecorations()
    plugin.sys.toast("已清除 " .. count .. " 个装饰")
end

---- ==================== 基础装饰示例 ==================== ----

-- 示例1: 偶数行蓝色背景
function demo_line_background()
    clearAll()
    local lineCount = getLineCount()
    local count = 0
    for i = 0, lineCount - 1, 2 do
        if plugin.decoration.setLineBackground(i, 0x1A2196F3) then
            count = count + 1
        end
    end
    plugin.sys.toast("已为 " .. count .. " 个偶数行设置浅蓝背景")
end

-- 示例2: 每5行绿色背景
function demo_line_background_green()
    clearAll()
    local lineCount = getLineCount()
    local count = 0
    for i = 0, lineCount - 1, 5 do
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
    local count = plugin.decoration.setLineBackgrounds(lines, 0x1AFF5722)
    plugin.sys.toast("已为第3-8行批量设置橙色背景，成功 " .. count .. " 行")
end

-- 示例4: Gutter 七种图标
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

-- 示例5: Gutter 黄色背景
function demo_gutter_background()
    clearAll()
    local lineCount = getLineCount()
    local count = 0
    for i = 0, math.min(lineCount - 1, 20) do
        if i % 2 == 0 then
            if plugin.decoration.setGutterBackground(i, 0x1AFFCC00) then
                count = count + 1
            end
        end
    end
    plugin.sys.toast("已为 " .. count .. " 行设置黄色 Gutter 背景")
end

-- 示例6: 组合装饰
function demo_combined()
    clearAll()
    local lineCount = getLineCount()
    if lineCount >= 15 then
        plugin.decoration.setLineBackground(3, 0x20FF0000)
        plugin.decoration.setGutterIcon(3, "error")
        plugin.decoration.setLineBackground(7, 0x20FFCC00)
        plugin.decoration.setGutterIcon(7, "warning")
        plugin.decoration.setLineBackground(11, 0x202196F3)
        plugin.decoration.setGutterIcon(11, "info")
        plugin.decoration.setLineBackground(15, 0x204CAF50)
        plugin.decoration.setGutterIcon(15, "bookmark")
        plugin.sys.toast("已设置4行组合装饰 (error/warning/info/bookmark)")
    else
        plugin.sys.toast("文件行数不足，请打开至少16行的文件")
    end
end

-- 示例7: 装饰当前光标行
function demo_single_line()
    clearAll()
    local pos = plugin.editor.getCursorPosition()
    if pos and pos[1] then
        local line = pos[1]
        plugin.decoration.setLineBackground(line, 0x30E91E63)
        plugin.decoration.setGutterIcon(line, "star")
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
        if d.category then
            table.insert(parts, "分类=" .. d.category)
        end
        if d.gutterIconType then
            table.insert(parts, "图标=" .. d.gutterIconType)
        end
        if d.lineBackgroundColor then
            table.insert(parts, "背景=" .. d.lineBackgroundColor)
        end
        msg = msg .. "  行" .. d.line .. ": " .. table.concat(parts, ", ") .. "\n"
    end
    plugin.sys.toast(msg)
end

---- ==================== 分类装饰示例 ====================

-- 示例9: 设置断点（使用 "breakpoint" 分类）
function demo_set_breakpoints()
    clearAll()
    local lineCount = getLineCount()
    -- 在每第4行设置断点装饰
    local count = 0
    for i = 3, math.min(lineCount - 1, 30), 4 do
        plugin.decoration.setGutterIcon(i, "dot", "breakpoint")
        plugin.decoration.setLineBackground(i, 0x30E53935, "breakpoint")
        count = count + 1
    end
    plugin.sys.toast("已设置 " .. count .. " 个断点（分类: breakpoint）\n可使用【跳转断点】菜单项导航")
end

-- 示例10: 设置高亮标记（使用 "highlight" 分类）
function demo_set_highlights()
    -- 不清除断点，只清除本分类
    local lineCount = getLineCount()
    local count = 0
    for i = 1, math.min(lineCount - 1, 30), 6 do
        plugin.decoration.setGutterIcon(i, "arrow", "highlight")
        plugin.decoration.setLineBackground(i, 0x304CAF50, "highlight")
        count = count + 1
    end
    plugin.sys.toast("已设置 " .. count .. " 个高亮标记（分类: highlight）\n断点装饰不受影响")
end

-- 示例11: 混合分类 — 在不同行用不同分类
function demo_mixed_categories()
    clearAll()
    local lineCount = getLineCount()
    if lineCount >= 20 then
        -- breakpoint 分类: 红色背景 + dot 图标
        plugin.decoration.setLineBackground(2, 0x30E53935, "breakpoint")
        plugin.decoration.setGutterIcon(2, "dot", "breakpoint")
        -- error 分类: 红色背景 + error 图标
        plugin.decoration.setLineBackground(5, 0x30FF5722, "error")
        plugin.decoration.setGutterIcon(5, "error", "error")
        -- warning 分类: 黄色背景 + warning 图标
        plugin.decoration.setLineBackground(8, 0x30FFC107, "warning")
        plugin.decoration.setGutterIcon(8, "warning", "warning")
        -- highlight 分类: 蓝色背景 + star 图标
        plugin.decoration.setLineBackground(12, 0x302196F3, "highlight")
        plugin.decoration.setGutterIcon(12, "star", "highlight")
        -- bookmark 分类: 绿色背景 + bookmark 图标
        plugin.decoration.setLineBackground(16, 0x304CAF50, "bookmark")
        plugin.decoration.setGutterIcon(16, "bookmark", "bookmark")
        plugin.sys.toast("已设置5种混合分类:\nbreakpoint/error/warning/highlight/bookmark")
    else
        plugin.sys.toast("文件行数不足，请打开至少20行的文件")
    end
end

---- ==================== 分类导航示例 ====================

-- 示例12: 跳转到下一个装饰行（所有分类）
function demo_goto_next()
    local line = plugin.decoration.gotoNextDecoration()
    if line >= 0 then
        plugin.sys.toast("已跳转到下一个装饰行: " .. line)
    else
        plugin.sys.toast("没有找到更多装饰行")
    end
end

-- 示例13: 跳转到上一个装饰行（所有分类）
function demo_goto_previous()
    local line = plugin.decoration.gotoPreviousDecoration()
    if line >= 0 then
        plugin.sys.toast("已跳转到上一个装饰行: " .. line)
    else
        plugin.sys.toast("没有找到更前的装饰行")
    end
end

-- 示例14: 跳转到下一个断点（仅 "breakpoint" 分类）
function demo_goto_next_breakpoint()
    local line = plugin.decoration.gotoNextDecoration("breakpoint")
    if line >= 0 then
        plugin.sys.toast("已跳转到断点行: " .. line)
    else
        plugin.sys.toast("没有找到更多断点")
    end
end

-- 示例15: 跳转到上一个断点
function demo_goto_prev_breakpoint()
    local line = plugin.decoration.gotoPreviousDecoration("breakpoint")
    if line >= 0 then
        plugin.sys.toast("已跳转到上一个断点: " .. line)
    else
        plugin.sys.toast("找不到上一个断点")
    end
end

-- 示例16: 跳转到下一个高亮（仅 "highlight" 分类）
function demo_goto_next_highlight()
    local line = plugin.decoration.gotoNextDecoration("highlight")
    if line >= 0 then
        plugin.sys.toast("已跳转到高亮行: " .. line)
    else
        plugin.sys.toast("没有找到更多高亮标记")
    end
end

-- 示例17: 列出所有装饰行（按分类）
function demo_list_decorations()
    local allLines = plugin.decoration.getDecorationLines()
    local breakpointLines = plugin.decoration.getDecorationLines("breakpoint")
    local highlightLines = plugin.decoration.getDecorationLines("highlight")
    
    local msg = "当前文件装饰统计:\n"
    msg = msg .. "  合计: " .. #allLines .. " 行\n"
    msg = msg .. "  breakpoint: " .. #breakpointLines .. " 处 (行 "
    for i, l in ipairs(breakpointLines) do
        msg = msg .. l .. (i < #breakpointLines and "," or "")
    end
    msg = msg .. ")\n  highlight: " .. #highlightLines .. " 处"
    plugin.sys.toast(msg)
end

---- ==================== 事件监听示例 ====================

-- 注册装饰事件监听
local function registerEvents()
    -- 点击装饰行事件
    plugin.decoration.setOnDecorationClick(function()
        local pos = plugin.editor.getCursorPosition()
        plugin.sys.toast("点击了装饰行: " .. (pos and pos[1] or "?"))
    end)
    
    -- 长按装饰行事件
    plugin.decoration.setOnDecorationLongClick(function()
        local pos = plugin.editor.getCursorPosition()
        plugin.sys.toast("长按了装饰行: " .. (pos and pos[1] or "?"))
    end)
    
    -- 双击装饰行事件
    plugin.decoration.setOnDecorationDoubleClick(function()
        local pos = plugin.editor.getCursorPosition()
        plugin.sys.toast("双击了装饰行: " .. (pos and pos[1] or "?"))
    end)
    
    -- Gutter 图标点击事件
    plugin.decoration.setOnGutterIconClick(function()
        plugin.sys.toast("点击了 Gutter 装饰图标")
    end)
    
    plugin.sys.toast("装饰事件已注册\n点击/长按/双击任意装饰行测试")
end

---- ==================== 菜单注册 ====================
-- 点击编辑器顶部工具栏 "..." 按钮可见菜单项

-- 基础装饰
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

-- 分类装饰
plugin.menu.addMenuDivider("decoration_cat")
plugin.menu.addMenuItem("deco_breakpoints", "【分类】设置断点(每4行)", demo_set_breakpoints)
plugin.menu.addMenuItem("deco_highlights", "【分类】设置高亮(每6行)", demo_set_highlights)
plugin.menu.addMenuItem("deco_mixed", "【分类】混合5种分类", demo_mixed_categories)

-- 分类导航
plugin.menu.addMenuDivider("decoration_nav")
plugin.menu.addMenuItem("deco_goto_next", "【导航】跳转下一个装饰行", demo_goto_next)
plugin.menu.addMenuItem("deco_goto_prev", "【导航】跳转上一个装饰行", demo_goto_previous)
plugin.menu.addMenuItem("deco_goto_next_bp", "【导航】跳转下一个断点", demo_goto_next_breakpoint)
plugin.menu.addMenuItem("deco_goto_prev_bp", "【导航】跳转上一个断点", demo_goto_prev_breakpoint)
plugin.menu.addMenuItem("deco_goto_next_hl", "【导航】跳转下一个高亮", demo_goto_next_highlight)
plugin.menu.addMenuItem("deco_list", "【导航】列出装饰统计", demo_list_decorations)

-- 事件+查询
plugin.menu.addMenuDivider("decoration_event")
plugin.menu.addMenuItem("deco_events", "【事件】注册装饰事件监听", registerEvents)
plugin.menu.addMenuItem("decoration_query", "【事件】查询当前装饰", demo_query)
plugin.menu.addMenuItem("decoration_clear", "【事件】清除所有装饰", clearAll)

log(TAG, "装饰插件示例已加载（含分类/导航/事件）")
plugin.sys.toast("装饰插件已就绪！\n打开文件 → 工具栏 ... →【装饰/分类/导航/事件】菜单项")