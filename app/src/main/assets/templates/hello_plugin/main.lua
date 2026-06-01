-- ============================================================
-- Hello Plugin — LuaForge Studio 入门示例插件
-- 演示了 studio 桥梁 API 的主要用法：
--   - 注册快捷栏按钮
--   - 监听 IDE 事件
--   - 读写编辑器文本
-- ============================================================

-- 1. 检查宿主版本
local version = studio:getVersion()
studio:log("HelloPlugin", "插件已加载，当前 IDE 版本: " .. tostring(version))

-- 2. 在编辑器快捷栏添加一个自定义按钮
studio:addQuickAction("greet", "👋 问好", function()
    local filePath = studio:getActiveFile()
    if filePath then
        studio:toast("当前文件: " .. filePath)
    else
        studio:toast("Hello from HelloPlugin! 🎉")
    end
end)

-- 3. 添加一个"插入注释"按钮
studio:addQuickAction("insert_comment", "# 注释", function()
    studio:insertText("-- TODO: ")
end)

-- 4. 监听文件保存事件
studio:registerEventListener("onFileSave", {
    onEvent = function(filePath)
        studio:log("HelloPlugin", "文件已保存: " .. tostring(filePath))
    end
})

-- 5. 监听文件打开事件
studio:registerEventListener("onFileOpen", {
    onEvent = function(filePath)
        studio:toast("📂 打开文件: " .. tostring(filePath):match("[^/]+$"))
    end
})

studio:log("HelloPlugin", "初始化完成，已注册 2 个快捷按钮 + 2 个事件监听")
