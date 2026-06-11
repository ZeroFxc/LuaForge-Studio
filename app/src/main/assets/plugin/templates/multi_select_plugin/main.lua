-- ============================================================
-- 项目多选插件 - 主页项目列表多选模式
-- 长按项目进入多选模式，点击切换选中，再次长按已选项目进行批量操作
-- 返回键可退出多选模式
-- ============================================================

-- 插件模块:
-- plugin.events: 事件监听
-- plugin.mainpage: 主页项目列表操作（多选模式控制）
-- plugin.ui: 对话框操作
-- plugin.sys: 系统操作（日志、Toast）
-- plugin.config: 配置存储

plugin.sys.log("项目多选插件", "插件已加载")

-- ============================================================
-- 返回键回调函数引用（保存引用以便取消注册）
-- ============================================================
local backPressedCallback = nil

-- ============================================================
-- 注册返回键监听，进入多选模式后可通过返回键退出
-- ============================================================
local function registerBackPressedListener()
    if backPressedCallback == nil then
        backPressedCallback = function()
            plugin.sys.log("项目多选插件", "返回键按下，退出多选模式")
            plugin.mainpage.exitMultiSelectMode()
            plugin.sys.toast("已退出多选模式")
            unregisterBackPressedListener()
        end
        plugin.events.register("onBackPressed", backPressedCallback)
        plugin.sys.log("项目多选插件", "已注册返回键监听")
    end
end

-- ============================================================
-- 取消返回键监听
-- ============================================================
local function unregisterBackPressedListener()
    if backPressedCallback ~= nil then
        plugin.events.unregister("onBackPressed", backPressedCallback)
        backPressedCallback = nil
        plugin.sys.log("项目多选插件", "已取消返回键监听")
    end
end

-- ============================================================
-- 退出多选模式的封装函数（同时取消返回键监听）
-- ============================================================
local function exitMultiSelectMode()
    plugin.mainpage.exitMultiSelectMode()
    unregisterBackPressedListener()
end

-- ============================================================
-- 进入多选模式的封装函数（同时注册返回键监听）
-- ============================================================
local function enterMultiSelectMode()
    plugin.mainpage.enterMultiSelectMode()
    registerBackPressedListener()
end

-- 添加一个快捷操作，用于手动退出多选模式
plugin.menu.addQuickAction("exit_multi_select", "退出多选", function()
    exitMultiSelectMode()
    plugin.sys.toast("已退出多选模式")
end)

-- ============================================================
-- 事件监听：项目长按
-- 参数: projectId, projectName, projectPath
-- ============================================================
plugin.events.register("onProjectLongPress", function(projectId, projectName, projectPath)
    plugin.sys.log("项目多选插件", "长按项目: " .. tostring(projectId) .. " - " .. tostring(projectName))

    if not plugin.mainpage.isInMultiSelectMode() then
        -- 进入多选模式，选中当前项目
        enterMultiSelectMode()
        plugin.mainpage.selectProject(tostring(projectId))
        plugin.sys.toast("已进入多选模式，选中: " .. tostring(projectName))
    else
        -- 已在多选模式中，判断当前项目是否被选中
        local selectedIds = plugin.mainpage.getSelectedProjectIds()
        local isSelected = false
        for i = 1, #selectedIds do
            if tostring(selectedIds[i]) == tostring(projectId) then
                isSelected = true
                break
            end
        end

        if isSelected then
            -- 长按已选中的项目 -> 弹出批量操作对话框
            local selectedCount = plugin.mainpage.getSelectedCount()
            local info = "已选中 " .. selectedCount .. " 个项目:\n"
            for i = 1, #selectedIds do
                info = info .. "  - " .. tostring(selectedIds[i]) .. "\n"
            end

            plugin.ui.showConfirm("批量操作", info .. "\n是否对选中的项目执行批量操作？",
                function()
                    -- 确定：执行批量操作
                    plugin.sys.log("项目多选插件", "批量操作确认，选中项目数: " .. selectedCount)
                    plugin.sys.toast("执行批量操作: " .. selectedCount .. " 个项目")

                    -- 这里可以扩展具体的批量操作逻辑
                    -- 例如：批量删除、批量导出、批量分享等
                    plugin.ui.showMessage("批量操作完成", "共处理了 " .. selectedCount .. " 个项目")

                    -- 操作完成后退出多选模式
                    exitMultiSelectMode()
                end,
                function()
                    -- 取消：停留在多选模式
                    plugin.sys.toast("已取消批量操作")
                end
            )
        else
            -- 长按未选中的项目 -> 选中它
            plugin.mainpage.selectProject(tostring(projectId))
            plugin.sys.toast("已选中: " .. tostring(projectName) .. " (共 " .. plugin.mainpage.getSelectedCount() .. " 个)")
        end
    end
end)

-- ============================================================
-- 事件监听：项目点击
-- 在多选模式下，点击切换选中状态
-- ============================================================
plugin.events.register("onProjectClick", function(projectId, projectName, projectPath)
    plugin.sys.log("项目多选插件", "点击项目: " .. tostring(projectId) .. " - " .. tostring(projectName))

    if plugin.mainpage.isInMultiSelectMode() then
        plugin.mainpage.toggleProjectSelection(tostring(projectId))
        local totalSelected = plugin.mainpage.getSelectedCount()
        if totalSelected > 0 then
            plugin.sys.toast("已选中 " .. totalSelected .. " 个项目")
        else
            -- 所有项目都已取消选中，退出多选模式
            exitMultiSelectMode()
            plugin.sys.toast("已退出多选模式")
        end
    end
end)

plugin.sys.log("项目多选插件", "初始化完成，已注册长按多选、点击切换和返回键退出事件")