-- ============================================================
-- 测试插件 - 设置页面扩展 & 插件详情展开区扩展 API 测试
-- 测试 plugin.settings 和 plugin.detail 两个新 API
-- 演示使用 plugin.config 持久化开关状态
-- ============================================================

plugin.sys.log("Settings Test", "插件已加载，开始注册测试项")

-- 辅助函数：读取配置，带默认值
local function getConfig(key, defaultVal)
    return plugin.config.getBoolean(key, defaultVal)
end

-- 辅助函数：保存配置
local function setConfig(key, value)
    plugin.config.setBoolean(key, value)
end

-- ============================================================
-- 一、设置页面扩展 (plugin.settings)
-- 在系统设置页面底部添加插件专属设置 section
-- 所有开关状态使用 plugin.config 持久化，重启后不丢失
-- ============================================================

-- 1.1 添加 "功能开关" section
plugin.settings.addSection("feature_toggles", "功能开关", 100)

plugin.settings.addSwitch(
    "auto_backup", "feature_toggles",
    "自动备份", "关闭项目时自动备份文件",
    getConfig("cfg_auto_backup", false),
    function(enabled)
        setConfig("cfg_auto_backup", enabled)
        plugin.sys.log("Settings Test", "自动备份 -> " .. tostring(enabled))
        plugin.sys.toast("自动备份: " .. (enabled and "已开启" or "已关闭"))
    end
)

plugin.settings.addSwitch(
    "auto_format", "feature_toggles",
    "自动格式化", "保存文件时自动格式化代码",
    getConfig("cfg_auto_format", true),
    function(enabled)
        setConfig("cfg_auto_format", enabled)
        plugin.sys.log("Settings Test", "自动格式化 -> " .. tostring(enabled))
        plugin.sys.toast("自动格式化: " .. (enabled and "已开启" or "已关闭"))
    end
)

plugin.settings.addSwitch(
    "show_line_numbers", "feature_toggles",
    "显示行号", "编辑器左侧显示行号",
    getConfig("cfg_show_line_numbers", true),
    function(enabled)
        setConfig("cfg_show_line_numbers", enabled)
        plugin.sys.log("Settings Test", "显示行号 -> " .. tostring(enabled))
    end
)

-- 1.2 添加 "高级配置" section，带齿轮按钮
plugin.settings.addSection("advanced_config", "高级配置", 200)

plugin.settings.addButton(
    "open_advanced", "advanced_config",
    "打开高级设置", "配置插件的高级参数",
    function()
        plugin.sys.log("Settings Test", "点击了高级设置按钮")
        local info = "高级配置示例\n\n"
        info = info .. "这里可以打开一个专用的设置页面\n"
        info = info .. "比如：\n"
        info = info .. "  - 配置 API 密钥\n"
        info = info .. "  - 调整性能参数\n"
        info = info .. "  - 管理缓存数据\n"
        info = info .. "  - 自定义 UI 主题\n\n"
        info = info .. "当前这是一个演示对话框，实际应用中可以跳转到自定义页面"
        plugin.ui.showMessage("Advanced Settings", info)
    end
)

plugin.settings.addButton(
    "reset_config", "advanced_config",
    "重置所有配置", "将插件配置恢复为默认值",
    function()
        plugin.sys.log("Settings Test", "点击了重置配置按钮")
        plugin.ui.showConfirm(
            "确认重置",
            "确定要重置所有配置吗？此操作不可撤销。",
            function()
                plugin.config.clear()
                plugin.sys.toast("配置已重置，请重启插件以刷新 UI")
            end,
            function()
                plugin.sys.toast("已取消")
            end
        )
    end
)

-- 1.3 添加 "关于本插件" section
plugin.settings.addSection("about_plugin", "关于本插件", 300)

plugin.settings.addButton(
    "view_about", "about_plugin",
    "查看插件信息", "版本、作者、依赖等详细信息",
    function()
        local info = "Settings & Detail Test 插件\n\n"
        info = info .. "版本: 1.0.0\n"
        info = info .. "作者: Nirithy\n"
        info = info .. "类型: Lua 插件\n\n"
        info = info .. "测试功能:\n"
        info = info .. "  • plugin.settings - 设置页面扩展\n"
        info = info .. "  • plugin.detail - 插件详情展开区扩展\n\n"
        info = info .. "当前配置状态:\n"
        info = info .. "  自动备份: " .. tostring(getConfig("cfg_auto_backup", false)) .. "\n"
        info = info .. "  自动格式化: " .. tostring(getConfig("cfg_auto_format", true)) .. "\n"
        info = info .. "  显示行号: " .. tostring(getConfig("cfg_show_line_numbers", true)) .. "\n"
        info = info .. "  自动运行: " .. tostring(getConfig("cfg_auto_start", false)) .. "\n"
        info = info .. "  调试模式: " .. tostring(getConfig("cfg_debug_mode", false)) .. "\n"
        info = info .. "  使用统计: " .. tostring(getConfig("cfg_telemetry", true))
        plugin.ui.showMessage("About", info)
    end
)


-- ============================================================
-- 二、插件详情展开区扩展 (plugin.detail)
-- 在插件管理页面自己的卡片展开区添加设置项
-- 同样使用 plugin.config 持久化
-- ============================================================

-- 2.1 开关类（持久化）
plugin.detail.addSwitch(
    "detail_auto_start",
    "打开项目时自动运行", "检测到项目打开时自动执行插件初始化",
    getConfig("cfg_auto_start", false),
    function(enabled)
        setConfig("cfg_auto_start", enabled)
        plugin.sys.log("Settings Test", "auto_start -> " .. tostring(enabled))
        plugin.sys.toast("自动运行: " .. (enabled and "ON" or "OFF"))
    end
)

plugin.detail.addSwitch(
    "detail_debug_mode",
    "调试模式", "启用后输出详细日志到控制台",
    getConfig("cfg_debug_mode", false),
    function(enabled)
        setConfig("cfg_debug_mode", enabled)
        plugin.sys.log("Settings Test", "debug_mode -> " .. tostring(enabled))
        plugin.sys.toast("调试模式: " .. (enabled and "已开启" or "已关闭"))
    end
)

plugin.detail.addSwitch(
    "detail_telemetry",
    "匿名使用统计", "发送匿名使用数据帮助改进插件",
    getConfig("cfg_telemetry", true),
    function(enabled)
        setConfig("cfg_telemetry", enabled)
        plugin.sys.log("Settings Test", "telemetry -> " .. tostring(enabled))
    end
)

-- 2.2 齿轮按钮类
plugin.detail.addButton(
    "detail_config",
    "插件配置", "打开专用配置页面",
    function()
        plugin.sys.log("Settings Test", "点击了详情中的配置按钮")
        plugin.ui.showMessage(
            "Plugin Configuration",
            "这里是插件的专用配置页面\n\n"
            .. "可以在这里设置:\n"
            .. "  • 自定义快捷键\n"
            .. "  • 文件过滤规则\n"
            .. "  • 网络代理设置\n"
            .. "  • 其他高级选项"
        )
    end
)

plugin.detail.addButton(
    "detail_logs",
    "查看日志", "打开本插件的运行日志",
    function()
        plugin.sys.log("Settings Test", "点击了查看日志按钮")
        plugin.ui.showMessage(
            "Plugin Logs",
            "运行日志（演示）\n\n"
            .. "[12:00] 插件已加载\n"
            .. "[12:01] 注册设置页面扩展\n"
            .. "[12:01] 注册详情展开区扩展\n"
            .. "[12:02] 初始化完成\n\n"
            .. "所有功能运行正常 ✓"
        )
    end
)

-- 2.3 只读信息类
plugin.detail.addInfo("detail_version", "当前版本", "v1.0.0 (build 20260101)")
plugin.detail.addInfo("detail_status", "运行状态", "正常 ✓")
plugin.detail.addInfo("detail_api", "API 版本", "v" .. tostring(plugin.getApiVersion()))


-- ============================================================
-- 三、添加快捷操作便于测试
-- ============================================================

plugin.menu.addQuickAction("st_test_info", "Settings Test 信息", function()
    local info = "本插件测试了以下新 API:\n\n"
    info = info .. "【plugin.settings】设置页面扩展\n"
    info = info .. "  - addSection(key, title, priority)\n"
    info = info .. "  - addSwitch(key, section, title, subtitle, initial, callback)\n"
    info = info .. "  - addButton(key, section, title, subtitle, callback)\n"
    info = info .. "  - removeSection(key)\n"
    info = info .. "  - removeItem(key)\n"
    info = info .. "  - clearItems()\n\n"
    info = info .. "【plugin.detail】插件详情展开区扩展\n"
    info = info .. "  - addSwitch(key, title, subtitle, initial, callback)\n"
    info = info .. "  - addButton(key, title, subtitle, callback)\n"
    info = info .. "  - addInfo(key, title, subtitle)\n"
    info = info .. "  - removeItem(key)\n"
    info = info .. "  - clearItems()\n\n"
    info = info .. "如何测试:\n"
    info = info .. "1. 打开 设置 → 滚动到底部查看「功能开关」「高级配置」「关于本插件」\n"
    info = info .. "2. 打开 插件管理 → 展开本插件 → 查看底部「插件设置」区域\n\n"
    info = info .. "配置持久化说明:\n"
    info = info .. "所有开关状态通过 plugin.config 自动保存，重启不丢失\n"
    info = info .. "「重置所有配置」会清除所有配置项"
    plugin.ui.showMessage("Settings & Detail Test", info)
end)

plugin.sys.log("Settings Test", "初始化完成，已注册 3 个 settings section + 6 个 detail 项（含持久化）")