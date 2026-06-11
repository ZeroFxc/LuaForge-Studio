-- ============================================================
-- 通知系统演示插件 — plugin.notification
-- 全面展示通知系统的所有功能
-- ============================================================

-- ============ 辅助函数 ============

local function fmtNum(n)
    return n
end

local function pname()
    return plugin.manager.getPluginName(plugin.getPluginId()) or plugin.getPluginId()
end

-- ============ 生命周期日志 ============

plugin.logger.info("通知演示", "插件开始加载")
plugin.logger.info("通知演示", "插件ID: " .. plugin.getPluginId())
plugin.logger.info("通知演示", "API版本: " .. tostring(plugin.getApiVersion()))

-- ============================================================
-- 第一部分: 基础通知 — 不同级别的通知演示
-- ============================================================

-- 1.1 发送一条 info 通知
plugin.menu.addQuickAction("notif_info", "📘 信息通知", function()
    plugin.notification.show("信息通知", "这是一条 info 级别的通知消息", "info")
    plugin.sys.toast("已发送 info 通知")
end)

-- 1.2 发送一条 success 通知
plugin.menu.addQuickAction("notif_success", "✅ 成功通知", function()
    plugin.notification.show("编译成功", "项目「" .. pname() .. "」已成功构建", "success")
    plugin.sys.toast("已发送 success 通知")
end)

-- 1.3 发送一条 warning 通知
plugin.menu.addQuickAction("notif_warning", "⚠ 警告通知", function()
    plugin.notification.show("内存告警", "当前内存使用率超过 80%，建议关闭不必要的文件", "warning")
    plugin.sys.toast("已发送 warning 通知")
end)

-- 1.4 发送一条 error 通知
plugin.menu.addQuickAction("notif_error", "❌ 错误通知", function()
    plugin.notification.show("语法错误", "第 42 行: 缺少 end 关键字", "error")
    plugin.sys.toast("已发送 error 通知")
end)

-- ============================================================
-- 第二部分: 通知分组 — 同组通知折叠展示
-- ============================================================

-- 2.1 发送多条同组通知（会折叠）
plugin.menu.addQuickAction("notif_group", "🗂 分组通知（3条）", function()
    plugin.notification.show("语法检查", "第 10 行: 变量未声明", "error", "syntax_check")
    plugin.notification.show("语法检查", "第 25 行: 缺少括号", "error", "syntax_check")
    plugin.notification.show("语法检查", "第 58 行: 类型不匹配", "warning", "syntax_check")
    plugin.sys.toast("已发送 3 条「语法检查」分组通知")
end)

-- 2.2 发送另一组通知
plugin.menu.addQuickAction("notif_group2", "🗂 构建分组（3条）", function()
    plugin.notification.show("构建日志", "正在编译 Lua 模块...", "info", "build_log")
    plugin.notification.show("构建日志", "正在打包资源文件...", "info", "build_log")
    plugin.notification.show("构建日志", "构建完成", "success", "build_log")
    plugin.sys.toast("已发送 3 条「构建日志」分组通知")
end)

-- 2.3 清除指定分组
plugin.menu.addQuickAction("notif_clear_group", "🗑 清除分组", function()
    local groups = {"syntax_check", "build_log", "config_check", "perf_monitor"}
    plugin.ui.showSingleChoiceDialog("选择要清除的分组", groups, 0, {
        onSelect = function(index)
            local g = groups[index + 1]
            plugin.notification.clearGroup(g)
            plugin.sys.toast("已清除分组: " .. g)
        end
    })
end)

-- ============================================================
-- 第三部分: 通知优先级 — 高优先级通知优先显示
-- ============================================================

-- 3.1 发送一条普通优先级通知
plugin.menu.addQuickAction("notif_pri_low", "🔹 低优先级通知", function()
    plugin.notification.show("普通提醒", "自动保存已启用", "info", nil, 0)
    plugin.sys.toast("已发送低优先级通知 (priority=0)")
end)

-- 3.2 发送一条中等优先级通知
plugin.menu.addQuickAction("notif_pri_mid", "🔶 中优先级通知", function()
    plugin.notification.show("配置变更", "检测到项目设置被修改，重启后生效", "warning", nil, 5)
    plugin.sys.toast("已发送中优先级通知 (priority=5)")
end)

-- 3.3 发送一条高优先级通知
plugin.menu.addQuickAction("notif_pri_high", "🔴 高优先级通知", function()
    plugin.notification.show("许可证过期", "您的许可证将在 3 天后过期，请及时续期", "error", nil, 10)
    plugin.sys.toast("已发送高优先级通知 (priority=10)")
end)

-- 3.4 混合优先级演示
plugin.menu.addQuickAction("notif_pri_mix", "🎯 混合优先级演示", function()
    plugin.notification.show("低优先级", "普通日志消息", "info", "priority_demo", 0)
    plugin.notification.show("高优先级", "关键警告消息", "error", "priority_demo", 10)
    plugin.notification.show("中优先级", "常规提醒消息", "warning", "priority_demo", 5)
    plugin.notification.show("最高优先级", "紧急错误消息", "error", "priority_demo", 20)
    plugin.sys.toast("已发送 4 条不同优先级通知（高优先级在前）")
end)

-- ============================================================
-- 第四部分: 系统通知栏推送
-- ============================================================

-- 4.1 推送系统通知（info）
plugin.menu.addQuickAction("notif_sys_info", "📱 系统通知·信息", function()
    plugin.notification.showSystem("LuaForge Studio", "项目编译完成，共 0 个错误", "info")
    plugin.sys.toast("系统通知已发送 (info)")
end)

-- 4.2 推送系统通知（success）
plugin.menu.addQuickAction("notif_sys_success", "📱 系统通知·成功", function()
    plugin.notification.showSystem("导出成功", "项目已导出到 /sdcard/output/", "success")
    plugin.sys.toast("系统通知已发送 (success)")
end)

-- 4.3 推送系统通知（warning）
plugin.menu.addQuickAction("notif_sys_warning", "📱 系统通知·警告", function()
    plugin.notification.showSystem("磁盘空间不足", "剩余空间小于 500MB，请清理无用文件", "warning")
    plugin.sys.toast("系统通知已发送 (warning)")
end)

-- 4.4 推送系统通知（error）
plugin.menu.addQuickAction("notif_sys_error", "📱 系统通知·错误", function()
    plugin.notification.showSystem("运行错误", "Lua 脚本执行失败: attempt to call nil value", "error")
    plugin.sys.toast("系统通知已发送 (error)")
end)

-- 4.5 同时发送 IDE 内通知 + 系统通知
plugin.menu.addQuickAction("notif_dual", "📱+📘 双通知推送", function()
    plugin.notification.show("双通知演示", "这是一条 IDE 内通知", "info", "dual_demo")
    plugin.notification.showSystem("双通知演示", "这是一条系统通知栏推送", "info")
    plugin.sys.toast("IDE 内通知 + 系统通知 已同时发送")
end)

-- ============================================================
-- 第五部分: 通知管理
-- ============================================================

-- 5.1 查看通知数量
plugin.menu.addQuickAction("notif_count", "📊 查看通知数量", function()
    local count = plugin.notification.getCount()
    plugin.ui.showMessage("通知统计", "当前插件「" .. pname() .. "」\n活跃通知数: " .. count .. " 条")
end)

-- 5.2 清除当前插件的所有通知
plugin.menu.addQuickAction("notif_clear", "🗑 清除本插件通知", function()
    plugin.notification.clear()
    plugin.sys.toast("已清除本插件所有通知")
end)

-- 5.3 清除所有插件的通知
plugin.menu.addQuickAction("notif_clear_all", "🗑 清除全部通知", function()
    plugin.ui.showConfirm("清除全部通知", "确定要清除所有插件的通知吗？", function()
        plugin.notification.clearAll()
        plugin.sys.toast("已清除所有通知")
    end, nil)
end)

-- ============================================================
-- 第六部分: 综合演示
-- ============================================================

-- 6.1 模拟一次性通知场景
plugin.menu.addQuickAction("notif_one_shot", "🎬 一次性通知（手动关闭）", function()
    local item = plugin.notification.show("一次性通知", "点击此通知即可关闭它", "info", "temp")
    plugin.sys.toast("已发送一次性通知，点击横幅即可关闭")
end)

-- 6.2 完整的编译流程演示
plugin.menu.addQuickAction("notif_build_flow", "🔧 模拟编译流程", function()
    plugin.notification.show("开始编译", "正在初始化编译环境...", "info", "build_flow", 0)
    plugin.notification.show("编译中", "正在编译 Lua 模块...", "info", "build_flow", 1)
    plugin.notification.show("编译中", "正在链接资源文件...", "info", "build_flow", 2)
    plugin.notification.show("编译完成", "编译成功! 输出: build/output.lua", "success", "build_flow", 3)
    plugin.notification.showSystem("编译完成", "项目构建成功", "success")
    plugin.sys.toast("模拟编译流程完成，查看通知横幅")
end)

-- 6.3 错误处理流程演示
plugin.menu.addQuickAction("notif_error_flow", "💥 模拟错误处理", function()
    plugin.notification.show("运行错误", "尝试执行 main.lua", "info", "error_flow", 0)
    plugin.notification.show("错误详情", "第 42 行: variable 'foo' is not declared", "error", "error_flow", 5)
    plugin.notification.show("修复建议", "请在第 41 行之前添加 'local foo = nil'", "warning", "error_flow", 3)
    plugin.notification.showSystem("运行出错", "main.lua 第 42 行发生错误", "error")
    plugin.sys.toast("模拟错误处理流程完成")
end)

-- 6.4 配置变更通知
plugin.menu.addQuickAction("notif_config_change", "⚙ 配置变更通知", function()
    plugin.notification.show("配置已更新", "主题设置已保存", "success", "config", 0)
    plugin.notification.show("配置已更新", "编辑器字体大小已更改为 14px", "info", "config", 1)
    plugin.notification.show("配置已更新", "自动保存间隔已设置为 30 秒", "info", "config", 2)
    plugin.notification.show("配置已更新", "需要重启以应用语言设置", "warning", "config", 5)
    plugin.sys.toast("配置变更通知已发送")
end)

-- ============================================================
-- 第七部分: 通知信息展示
-- ============================================================

-- 7.1 显示通知 API 使用说明
plugin.menu.addQuickAction("notif_help", "❓ 通知 API 说明", function()
    local info = [[
=== plugin.notification API 说明 ===

【IDE 内通知横幅】
plugin.notification.show(title, message, level)
plugin.notification.show(title, message, level, groupKey)
plugin.notification.show(title, message, level, groupKey, priority)

参数:
  title    - 通知标题
  message  - 通知内容
  level    - 通知级别: "info" | "success" | "warning" | "error"
  groupKey - 分组键（可选），同组通知会折叠
  priority - 优先级（可选），数字越大越靠前

【系统通知栏推送】
plugin.notification.showSystem(title, message, level)

【通知管理】
plugin.notification.clear()          -- 清除本插件通知
plugin.notification.clearGroup(key)  -- 清除指定分组
plugin.notification.clearAll()       -- 清除所有通知
plugin.notification.dismiss(id)      -- 关闭单条通知
plugin.notification.getCount()       -- 获取通知数量

【通知级别】
info     - 信息（蓝色）
success  - 成功（绿色）
warning  - 警告（橙色）
error    - 错误（红色）
]]
    plugin.ui.showTextDialog("通知 API 使用说明", info)
end)

-- 7.2 显示本插件信息
plugin.menu.addQuickAction("notif_plugin_info", "ℹ 插件信息", function()
    local count = plugin.notification.getCount()
    local info = "=== 通知系统演示插件 ===\n\n"
    info = info .. "插件ID: " .. plugin.getPluginId() .. "\n"
    info = info .. "插件名称: " .. pname() .. "\n"
    info = info .. "API版本: " .. tostring(plugin.getApiVersion()) .. "\n"
    info = info .. "当前通知数: " .. count .. " 条\n\n"
    info = info .. "功能列表:\n"
    info = info .. "  - 4 种通知级别 (info/success/warning/error)\n"
    info = info .. "  - 通知分组与折叠\n"
    info = info .. "  - 通知优先级排序\n"
    info = info .. "  - 系统通知栏推送\n"
    info = info .. "  - 通知管理 (清除/关闭/统计)\n"
    info = info .. "  - 编译流程模拟\n"
    info = info .. "  - 错误处理流程模拟\n"
    plugin.ui.showMessage("插件信息", info)
end)

-- ============================================================
-- 初始化完成
-- ============================================================

-- 启动时发送一条欢迎通知
plugin.notification.show("通知系统已就绪", "「" .. pname() .. "」插件已加载，通知系统正常工作", "success", "system", 0)

plugin.logger.info("通知演示", "插件初始化完成，已注册 20 个快捷操作")
plugin.sys.toast("通知系统演示插件已加载 ✓")