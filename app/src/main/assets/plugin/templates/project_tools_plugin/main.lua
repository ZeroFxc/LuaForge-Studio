-- ============================================================
-- 项目工具箱插件 - 项目列表增强功能演示
-- 功能:
--   1. 项目文件统计徽章 - 在每个项目卡片上显示文件数量
--   2. 左滑快捷删除 - 左滑项目卡片弹出删除确认
--   3. 右滑快捷置顶 - 右滑项目卡片切换置顶状态
--   4. 自定义菜单项 - 在项目卡片菜单中添加"查看项目信息"
-- ============================================================

-- 模块:
-- plugin.mainpage: 主页操作（徽章、菜单）
-- plugin.events: 事件监听
-- plugin.config: 配置存储
-- plugin.ui: 对话框
-- plugin.sys: 系统操作
-- plugin.reflect: Java 反射（用于文件统计）

plugin.sys.log("项目工具箱", "插件已加载")

-- ============================================================
-- 辅助函数：统计目录中的文件数量
-- ============================================================
local function countFilesInDir(dirPath)
    local ok, file = pcall(function()
        return plugin.reflect.newInstance("java.io.File", {dirPath})
    end)
    if not ok or not file then
        return 0
    end

    local ok2, exists = pcall(function()
        return plugin.reflect.callMethod(file, "exists", nil)
    end)
    if not ok2 or not exists then
        return 0
    end

    local ok3, isDir = pcall(function()
        return plugin.reflect.callMethod(file, "isDirectory", nil)
    end)
    if not ok3 or not isDir then
        return 0
    end

    local ok4, fileList = pcall(function()
        return plugin.reflect.callMethod(file, "listFiles", nil)
    end)
    if not ok4 or not fileList then
        return 0
    end

    local ok5, length = pcall(function()
        return plugin.reflect.getField(fileList, "length")
    end)
    if not ok5 then
        return 0
    end

    return tonumber(length) or 0
end

-- ============================================================
-- 辅助函数：为所有项目更新徽章
-- ============================================================
local function refreshAllBadges()
    local projectIds = plugin.mainpage.getProjectIds()
    for i = 1, #projectIds do
        local pid = tostring(projectIds[i])
        local projectPath = plugin.mainpage.getProjectPath(pid)
        if projectPath then
            local fileCount = countFilesInDir(projectPath)
            if fileCount > 0 then
                local color = 0xFF4CAF50
                if fileCount > 50 then
                    color = 0xFFFF9800
                end
                if fileCount > 200 then
                    color = 0xFFF44336
                end
                plugin.mainpage.setProjectBadge(pid, fileCount .. " 文件", color)
            end
        end
    end
    plugin.sys.log("项目工具箱", "徽章已刷新")
end

-- ============================================================
-- 添加项目卡片自定义菜单项
-- ============================================================
local projectInfoCallback = {
    onItemClick = function(projectId, projectName, projectPath)
        local fileCount = countFilesInDir(projectPath)
        local info = "项目名称: " .. projectName .. "\n"
        info = info .. "项目ID: " .. projectId .. "\n"
        info = info .. "路径: " .. projectPath .. "\n"
        info = info .. "文件数量: " .. fileCount .. "\n"
        info = info .. "项目总数: " .. plugin.mainpage.getProjectCount()

        plugin.ui.showMessage("项目信息", info)
    end
}

plugin.mainpage.addProjectCardMenuItem("proj_info", "查看项目信息", projectInfoCallback)

-- ============================================================
-- 事件监听：左滑项目卡片 -> 快捷删除确认
-- ============================================================
plugin.events.register("onProjectSwipeLeft", function(projectId, projectName, projectPath)
    plugin.sys.log("项目工具箱", "左滑项目: " .. tostring(projectName))
    plugin.ui.showConfirm("删除项目", "确定要删除项目 \"" .. tostring(projectName) .. "\" 吗？",
        function()
            plugin.sys.toast("已模拟删除: " .. tostring(projectName))
            plugin.mainpage.clearProjectBadge(tostring(projectId))
            plugin.sys.log("项目工具箱", "删除操作已执行: " .. tostring(projectPath))
        end,
        function()
            plugin.sys.toast("已取消删除")
        end
    )
end)

-- ============================================================
-- 事件监听：右滑项目卡片 -> 快捷置顶切换
-- ============================================================
plugin.events.register("onProjectSwipeRight", function(projectId, projectName, projectPath)
    plugin.sys.log("项目工具箱", "右滑项目: " .. tostring(projectName))
    local pinnedKey = "pinned_" .. tostring(projectId)
    local isPinned = plugin.config.getBoolean(pinnedKey, false)
    plugin.config.setBoolean(pinnedKey, not isPinned)
    if not isPinned then
        plugin.sys.toast("已置顶: " .. tostring(projectName))
    else
        plugin.sys.toast("已取消置顶: " .. tostring(projectName))
    end
end)

-- ============================================================
-- 事件监听：所有插件加载完成后刷新徽章
-- ============================================================
plugin.events.register("onAllPluginsLoaded", function()
    plugin.sys.log("项目工具箱", "所有插件已加载，开始刷新项目徽章")
    refreshAllBadges()
end)

-- 添加快捷操作：手动刷新徽章
plugin.menu.addQuickAction("refresh_badges", "刷新徽章", function()
    refreshAllBadges()
    plugin.sys.toast("项目徽章已刷新")
end)

-- 添加快捷操作：清除所有徽章
plugin.menu.addQuickAction("clear_badges", "清除徽章", function()
    plugin.mainpage.clearAllBadges()
    plugin.sys.toast("所有徽章已清除")
end)

plugin.sys.log("项目工具箱", "初始化完成，已注册左滑/右滑事件、自定义菜单项和徽章系统")