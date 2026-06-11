--[[
    build_hook_demo — 构建钩子 + 生命周期 + 导航 API 完整示例插件

    演示所有新增插件 API:
    - 构建事件监听: onBuildStart / onBuildFinish / onBuildError
    - 主动触发构建: buildProject() / compileFile() / cancelBuild()
    - 应用生命周期: onAppStart / onAppResume / onAppPause / onAppStop
    - 插件生命周期: onPluginEnabled / onPluginDisabled
    - 应用导航: navigateTo("main"/"new_project"/"editor")

    使用方法:
    1. 打开一个项目
    2. 点击编辑器顶部工具栏 "..." 按钮打开下拉菜单
    3. 选择对应菜单项测试各 API
--]]

local TAG = "BuildHookDemo"

-- 辅助函数：写日志（直接调 plugin.logger.info，不能捕获为局部变量，LuaJava 无法解析）
local function log(msg)
    plugin.logger.info(TAG, msg)
end

---- ==================== 状态变量 ==================== ----

local buildCount = 0          -- 构建次数统计
local compileCount = 0        -- 编译次数统计
local buildEnabled = false    -- 钩子是否已注册
local lifecycleEnabled = false -- 生命周期监听是否已注册
local lcResumeCount = 0       -- 应用切到前台次数
local lcPauseCount = 0        -- 应用切到后台次数

---- ==================== 构建钩子回调 ==================== ----

-- 构建开始回调
local function onBuildStart(projectPath, buildType)
    local typeLabel = (buildType == "project") and "项目构建" or "单文件编译"
    log( "[onBuildStart] " .. typeLabel .. " 开始")
    log( "  路径: " .. projectPath)
    log( "  类型: " .. buildType)
    plugin.sys.toast("钩子触发: " .. typeLabel .. " 开始")
end

-- 构建完成回调
local function onBuildFinish(projectPath, result, success)
    local status = success and "成功" or "失败"
    log( "[onBuildFinish] 构建" .. status)
    log( "  路径: " .. projectPath)
    log( "  结果: " .. tostring(result))
    log( "  成功: " .. tostring(success))

    if success then
        buildCount = buildCount + 1
        plugin.logger.info(TAG, "累计构建成功次数: " .. buildCount)
        plugin.notification.show("构建完成", "构建成功！\n累计: " .. buildCount .. " 次", "success", "build_status")
    else
        plugin.logger.warn(TAG, "构建失败: " .. tostring(result))
        plugin.notification.show("构建失败", tostring(result), "error", "build_status")
    end
end

-- 构建出错回调
local function onBuildError(projectPath, errorMsg, buildType)
    local typeLabel = (buildType == "project") and "项目构建" or "单文件编译"
    log( "[onBuildError] " .. typeLabel .. " 出错")
    log( "  路径: " .. projectPath)
    log( "  错误: " .. tostring(errorMsg))
    plugin.logger.error(TAG, "构建异常: " .. tostring(errorMsg))
end

---- ==================== 钩子注册/注销 ==================== ----

-- 注册构建钩子
function registerBuildHooks()
    if buildEnabled then
        plugin.sys.toast("构建钩子已注册，无需重复注册")
        return
    end

    plugin.events.register(plugin.events.ON_BUILD_START, onBuildStart)
    plugin.events.register(plugin.events.ON_BUILD_FINISH, onBuildFinish)
    plugin.events.register(plugin.events.ON_BUILD_ERROR, onBuildError)

    buildEnabled = true
    log( "构建钩子已注册 (onBuildStart / onBuildFinish / onBuildError)")
    plugin.sys.toast("构建钩子已注册！\n现在触发构建将自动回调")
    plugin.notification.show("钩子就绪", "构建钩子已激活，等待构建事件...", "info", "hook_status")
end

-- 注销构建钩子
function unregisterBuildHooks()
    if not buildEnabled then
        plugin.sys.toast("构建钩子未注册")
        return
    end

    plugin.events.unregister(plugin.events.ON_BUILD_START, onBuildStart)
    plugin.events.unregister(plugin.events.ON_BUILD_FINISH, onBuildFinish)
    plugin.events.unregister(plugin.events.ON_BUILD_ERROR, onBuildError)

    buildEnabled = false
    log( "构建钩子已注销")
    plugin.sys.toast("构建钩子已注销")
    plugin.notification.show("钩子已停用", "构建钩子已注销，不再接收构建事件", "warning", "hook_status")
end

---- ==================== 主动构建/编译 ==================== ----

-- 主动触发项目构建
function triggerBuild()
    local projectPath = plugin.project.getProjectPath()
    if not projectPath then
        plugin.sys.toast("请先打开一个项目")
        plugin.logger.warn(TAG, "无法构建: 未打开项目")
        return
    end

    log( "主动触发项目构建: " .. projectPath)
    plugin.sys.toast("开始构建项目...")

    -- 在后台线程触发构建（避免阻塞 UI）
    plugin.threads.runOnBackgroundThread(function()
        local result = plugin.build.buildProject(projectPath)
        plugin.threads.runOnUIThread(function()
            plugin.sys.toastLong("构建结果: " .. result)
        end)
    end)
end

-- 主动触发单文件编译
function triggerCompile()
    local activeFile = plugin.editor.getActiveFile()
    if not activeFile then
        plugin.sys.toast("请先打开一个 Lua/Aly 文件")
        plugin.logger.warn(TAG, "无法编译: 无活动文件")
        return
    end

    local ext = activeFile:lower():match("%.([^.]+)$")
    if ext ~= "lua" and ext ~= "aly" then
        plugin.sys.toast("只支持编译 .lua 或 .aly 文件")
        return
    end

    log( "主动触发单文件编译: " .. activeFile)
    plugin.sys.toast("开始编译文件...")

    plugin.threads.runOnBackgroundThread(function()
        local result = plugin.build.compileFile(activeFile)
        plugin.threads.runOnUIThread(function()
            plugin.sys.toastLong("编译结果: " .. result)
        end)
    end)
end

-- 编译当前项目所有 Lua 文件
function compileAllLuaFiles()
    local projectPath = plugin.project.getProjectPath()
    if not projectPath then
        plugin.sys.toast("请先打开一个项目")
        return
    end

    plugin.threads.runOnBackgroundThread(function()
        -- 递归收集所有 .lua 文件
        local function collectLuaFiles(dir)
            local files = plugin.project.listFiles(dir) or {}
            local luaFiles = {}
            for _, name in ipairs(files) do
                local path = dir .. "/" .. name
                if name:match("%.lua$") then
                    table.insert(luaFiles, path)
                elseif name:match("%.aly$") then
                    table.insert(luaFiles, path)
                elseif not name:match("%.") then
                    -- 可能是目录，递归
                    local subFiles = collectLuaFiles(path)
                    for _, sf in ipairs(subFiles) do
                        table.insert(luaFiles, sf)
                    end
                end
            end
            return luaFiles
        end

        local luaFiles = collectLuaFiles("")
        log( "找到 " .. #luaFiles .. " 个 Lua/Aly 文件")

        plugin.threads.runOnUIThread(function()
            plugin.sys.toast("找到 " .. #luaFiles .. " 个文件，开始批量编译...")
        end)

        local successCount = 0
        local failCount = 0
        for _, file in ipairs(luaFiles) do
            local result = plugin.build.compileFile(projectPath .. "/" .. file)
            if result and not result:match("error") and not result:match("失败") then
                successCount = successCount + 1
            else
                failCount = failCount + 1
            end
        end

        local msg = "批量编译完成: 成功 " .. successCount .. ", 失败 " .. failCount
        log( msg)
        plugin.threads.runOnUIThread(function()
            plugin.sys.toast(msg)
            plugin.notification.show("批量编译", msg,
                (failCount > 0) and "warning" or "success", "batch_compile")
        end)
    end)
end

---- ==================== 查询/统计 ==================== ----

-- 查看最近构建结果
function showLastResult()
    local result = plugin.build.getLastBuildResult()
    if result then
        plugin.ui.showTextDialog("最近构建结果", result)
    else
        plugin.sys.toast("暂无构建记录")
    end
end

-- 查看构建统计
function showBuildStats()
    local msg = "=== 构建统计 ===\n\n"
    msg = msg .. "钩子状态: " .. (buildEnabled and "已注册" or "未注册") .. "\n"
    msg = msg .. "累计构建成功: " .. buildCount .. " 次\n"
    msg = msg .. "累计编译: " .. compileCount .. " 次\n\n"

    local lastResult = plugin.build.getLastBuildResult()
    if lastResult then
        msg = msg .. "最近结果: " .. lastResult .. "\n"
    else
        msg = msg .. "最近结果: 暂无\n"
    end

    plugin.ui.showMessage("构建统计", msg)
end

-- 重置统计
function resetStats()
    buildCount = 0
    compileCount = 0
    plugin.sys.toast("统计已重置")
end

---- ==================== 模拟场景 ==================== ----

-- 模拟"保存后自动编译"场景
function demoAutoCompileOnSave()
    if not buildEnabled then
        registerBuildHooks()
    end

    plugin.events.register(plugin.events.ON_FILE_SAVE, function(filePath)
        local ext = filePath:lower():match("%.([^.]+)$")
        if ext == "lua" or ext == "aly" then
            log( "[自动编译] 检测到文件保存: " .. filePath)
            plugin.threads.runOnBackgroundThread(function()
                plugin.build.compileFile(filePath)
            end)
        end
    end)

    log( "已启用「保存后自动编译」模式")
    plugin.sys.toast("保存后自动编译已启用！\n保存 .lua/.aly 文件将自动编译")
    plugin.notification.show("自动编译", "保存 .lua/.aly 文件时自动触发编译", "info", "auto_compile")
end

-- 模拟"构建前自动备份"场景
function demoAutoBackupBeforeBuild()
    if not buildEnabled then
        registerBuildHooks()
    end

    plugin.events.register(plugin.events.ON_BUILD_START, function(projectPath, buildType)
        if buildType == "project" then
            log( "[构建前备份] 准备备份项目: " .. projectPath)
            -- 实际备份逻辑（示例）
            local backupDir = plugin.sys.getDataDir() .. "/backups"
            local timestamp = os.date("%Y%m%d_%H%M%S")
            log( "[构建前备份] 备份目录: " .. backupDir .. "/" .. timestamp)
            plugin.sys.toast("构建前自动备份已触发")
        end
    end)

    log( "已启用「构建前自动备份」模式")
    plugin.sys.toast("构建前自动备份已启用！\n构建项目时将自动备份")
    plugin.notification.show("自动备份", "构建项目前自动触发备份", "info", "auto_backup")
end

---- ==================== 构建取消场景 ==================== ----

-- 模拟"条件性阻止构建"场景
function demoCancelBuild()
    if not buildEnabled then
        registerBuildHooks()
    end

    -- 注册 onBuildStart 钩子，在特定条件下取消构建
    plugin.events.register(plugin.events.ON_BUILD_START, function(projectPath, buildType)
        -- 示例：如果构建次数超过阈值就阻止
        if buildCount >= 999 then
            log( "[取消构建] 超出构建次数阈值，取消构建")
            plugin.build.cancelBuild()
            plugin.sys.toast("构建已取消 (超出次数限制)")
            return
        end
    end)

    log( "已启用「条件性阻止构建」模式")
    plugin.sys.toast("条件性阻止构建已启用！\n构建超过 999 次后自动阻止")
    plugin.notification.show("构建防护", "构建次数达上限时自动取消", "warning", "cancel_build")
end

-- 直接测试取消功能（下次构建会被取消一次）
function demoCancelNextBuild()
    plugin.events.register(plugin.events.ON_BUILD_START, function(projectPath, buildType)
        plugin.build.cancelBuild()
        plugin.sys.toast("构建已手动取消！")
    end)
    plugin.sys.toast("下次构建将被自动取消")
    plugin.notification.show("取消模式", "下次构建触发时自动取消", "warning", "cancel_mode")
end

---- ==================== 生命周期事件监听 ==================== ----

-- 注册生命周期监听
function registerLifecycleListeners()
    if lifecycleEnabled then
        plugin.sys.toast("生命周期监听已注册")
        return
    end

    plugin.events.register(plugin.events.ON_APP_START, function()
        log( "[生命周期] 应用启动")
        plugin.notification.show("应用状态", "应用已启动", "info", "lifecycle")
    end)

    plugin.events.register(plugin.events.ON_APP_RESUME, function()
        lcResumeCount = lcResumeCount + 1
        log( "[生命周期] 应用切到前台 (第" .. lcResumeCount .. "次)")
    end)

    plugin.events.register(plugin.events.ON_APP_PAUSE, function()
        lcPauseCount = lcPauseCount + 1
        log( "[生命周期] 应用切到后台 (第" .. lcPauseCount .. "次)")
    end)

    plugin.events.register(plugin.events.ON_APP_STOP, function()
        log( "[生命周期] 应用即将销毁")
    end)

    plugin.events.register(plugin.events.ON_PLUGIN_ENABLED, function(pluginId)
        log( "[生命周期] 插件被启用: " .. tostring(pluginId))
    end)

    plugin.events.register(plugin.events.ON_PLUGIN_DISABLED, function(pluginId)
        log( "[生命周期] 插件被禁用: " .. tostring(pluginId))
    end)

    lifecycleEnabled = true
    log( "生命周期监听已注册")
    plugin.sys.toast("生命周期监听已注册！\n前后台切换将记录日志")
end

-- 注销生命周期监听
function unregisterLifecycleListeners()
    if not lifecycleEnabled then
        plugin.sys.toast("生命周期监听未注册")
        return
    end
    -- 生命周期事件用一次性注册，卸载由系统自动清理
    lifecycleEnabled = false
    log( "生命周期监听已注销")
    plugin.sys.toast("生命周期监听已注销（重启插件后重新注册）")
end

-- 查看生命周期统计
function showLifecycleStats()
    local msg = "=== 生命周期统计 ===\n\n"
    msg = msg .. "监听状态: " .. (lifecycleEnabled and "已注册" or "未注册") .. "\n"
    msg = msg .. "切到前台次数: " .. lcResumeCount .. "\n"
    msg = msg .. "切到后台次数: " .. lcPauseCount .. "\n"
    msg = msg .. "累计构建成功: " .. buildCount .. " 次\n"
    msg = msg .. "累计编译成功: " .. compileCount .. " 次\n"
    plugin.ui.showMessage("统计面板", msg)
end

---- ==================== 应用导航 ==================== ----

-- 导航到主页
function navToMain()
    plugin.nav.navigateTo("main")
    log( "[导航] 切换到主页面")
end

-- 导航到新建项目
function navToNewProject()
    plugin.nav.navigateTo("new_project")
    log( "[导航] 切换到新建项目页面")
end

-- 导航到编辑器
function navToEditor()
    plugin.nav.navigateTo("editor")
    log( "[导航] 切换到编辑器页面")
end

-- 从编辑器返回主页
function navGoBack()
    plugin.nav.goBack()
    log( "[导航] 返回上一页")
end

-- 打开/关闭侧滑栏
function toggleSidebar()
    if plugin.nav.isSidebarOpen() then
        plugin.nav.closeSidebar()
        plugin.sys.toast("侧滑栏已关闭")
    else
        plugin.nav.openSidebar()
        plugin.sys.toast("侧滑栏已打开")
    end
end

---- ==================== 菜单注册 ====================
-- 点击编辑器顶部工具栏 "..." 按钮可见菜单项

-- 钩子注册/注销
plugin.menu.addMenuDivider("build_hook")
plugin.menu.addMenuItem("build_hook_register", "【构建钩子】注册钩子", registerBuildHooks)
plugin.menu.addMenuItem("build_hook_unregister", "【构建钩子】注销钩子", unregisterBuildHooks)

-- 主动触发
plugin.menu.addMenuDivider("build_hook_div1")
plugin.menu.addMenuItem("build_trigger_project", "【构建】触发项目构建", triggerBuild)
plugin.menu.addMenuItem("build_trigger_file", "【构建】编译当前文件", triggerCompile)
plugin.menu.addMenuItem("build_trigger_all", "【构建】批量编译所有Lua", compileAllLuaFiles)

-- 查询/统计
plugin.menu.addMenuDivider("build_hook_div2")
plugin.menu.addMenuItem("build_last_result", "【查询】最近构建结果", showLastResult)
plugin.menu.addMenuItem("build_stats", "【查询】构建统计", showBuildStats)
plugin.menu.addMenuItem("build_reset_stats", "【查询】重置统计", resetStats)

-- 场景演示
plugin.menu.addMenuDivider("build_hook_div3")
plugin.menu.addMenuItem("build_scenario_autocompile", "【场景】保存后自动编译", demoAutoCompileOnSave)
plugin.menu.addMenuItem("build_scenario_autobackup", "【场景】构建前自动备份", demoAutoBackupBeforeBuild)
plugin.menu.addMenuItem("build_scenario_cancel", "【场景】取消下次构建", demoCancelNextBuild)
plugin.menu.addMenuItem("build_scenario_threshold", "【场景】条件性阻止构建", demoCancelBuild)

-- 生命周期
plugin.menu.addMenuDivider("lifecycle_div")
plugin.menu.addMenuItem("lifecycle_register", "【生命周期】注册监听", registerLifecycleListeners)
plugin.menu.addMenuItem("lifecycle_unregister", "【生命周期】注销监听", unregisterLifecycleListeners)
plugin.menu.addMenuItem("lifecycle_stats", "【生命周期】查看统计", showLifecycleStats)

-- 导航
plugin.menu.addMenuDivider("nav_div")
plugin.menu.addMenuItem("nav_main", "【导航】回到主页", navToMain)
plugin.menu.addMenuItem("nav_new_project", "【导航】新建项目", navToNewProject)
plugin.menu.addMenuItem("nav_editor", "【导航】编辑器", navToEditor)
plugin.menu.addMenuItem("nav_goback", "【导航】返回上一页", navGoBack)
plugin.menu.addMenuItem("nav_sidebar", "【导航】切换侧滑栏", toggleSidebar)

-- API 说明
plugin.menu.addMenuDivider("build_hook_div4")
plugin.menu.addMenuItem("build_help", "【构建钩子】API 说明", function()
    local info = [[
=== 完整插件 API 说明 ===

【构建钩子事件】
plugin.events.ON_BUILD_START   → (projectPath, buildType)
plugin.events.ON_BUILD_FINISH  → (projectPath, result, success)
plugin.events.ON_BUILD_ERROR   → (projectPath, errorMsg, buildType)

【主动构建】
plugin.build.buildProject(projectPath) → 构建 APK
plugin.build.compileFile(filePath)     → 编译单文件
plugin.build.cancelBuild()             → 取消当前构建
plugin.build.getLastBuildResult()      → 最近构建结果

【应用生命周期事件】
plugin.events.ON_APP_START    → 应用启动
plugin.events.ON_APP_RESUME   → 切到前台
plugin.events.ON_APP_PAUSE    → 切到后台
plugin.events.ON_APP_STOP     → 应用销毁

【插件生命周期事件】
plugin.events.ON_PLUGIN_ENABLED  → (pluginId)
plugin.events.ON_PLUGIN_DISABLED → (pluginId)

【应用导航】
plugin.nav.navigateTo("main")       → 主页
plugin.nav.navigateTo("new_project") → 新建项目
plugin.nav.navigateTo("editor")     → 编辑器
plugin.nav.goBack()                 → 返回上一页

【侧滑栏】
plugin.nav.openSidebar()
plugin.nav.closeSidebar()
plugin.nav.toggleSidebar()

【典型用例】
1. 构建钩子: 自动备份/安装/日志
2. cancelBuild: 条件性阻止构建
3. 生命周期: 前台时自动刷新,后台时暂停任务
4. 导航: 插件内部跳转不同页面
]]
    plugin.ui.showTextDialog("构建钩子 API 说明", info)
end)

---- ==================== 初始化 ==================== ----

-- 启动时自动注册构建钩子
registerBuildHooks()

log( "构建钩子示例插件已加载")
plugin.sys.toast("构建钩子插件已就绪！\n钩子已自动注册，开放项目 → 工具栏 ... → 测试")