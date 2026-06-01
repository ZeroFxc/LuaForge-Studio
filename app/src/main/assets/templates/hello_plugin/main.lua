-- ============================================================
-- Hello Plugin — LuaForge Studio 示例插件
-- 演示了 studio 桥梁 API 的主要用法
-- ============================================================

-- 1. 检查宿主版本
local version = studio.getVersion()
local dataDir = studio.getDataDir()
studio.log("HelloPlugin", "插件已加载，IDE 版本: " .. tostring(version))
studio.log("HelloPlugin", "插件数据目录: " .. tostring(dataDir))

-- 2. 读取配置示例
local greetCount = studio.getConfigInt("greet_count", 0)
studio.setConfigInt("greet_count", greetCount + 1)
studio.log("HelloPlugin", "插件已启动 " .. (greetCount + 1) .. " 次")

-- 3. 在编辑器快捷栏添加按钮
studio.addQuickAction("greet", "👋 问好", function()
    studio.toast("Hello from HelloPlugin! 这是第 " .. studio.getConfigInt("greet_count", 0) .. " 次启动")
end)

-- 4. 插入注释按钮
studio.addQuickAction("insert_comment", "# 注释", function()
    studio.insertText("-- TODO: ")
end)

-- 5. 显示选中文本按钮
studio.addQuickAction("show_selection", "📝 选中文本", function()
    local ok, result = pcall(function()
        return studio.getSelectedText()
    end)
    
    if ok and result and #result > 0 then
        studio.toast("选中: " .. result)
    else
        local ok2, cursor = pcall(function()
            return studio.getCursorPosition()
        end)
        
        if ok2 and cursor then
            studio.toast("光标位置: 行 " .. tostring(cursor[1]) .. ", 列 " .. tostring(cursor[2]))
        else
            studio.toast("请先打开一个文件")
        end
    end
end)

-- 6. 跳转到行按钮
studio.addQuickAction("goto_line", "📍 跳转行", function()
    local callback = {
        onInput = function(text)
            local line = tonumber(text)
            if line then
                studio.gotoLine(line - 1)
                studio.toast("已跳转到第 " .. line .. " 行")
            else
                studio.toast("请输入有效的行号")
            end
        end
    }
    studio.showInputDialog("跳转到行", "请输入行号", "1", callback)
end)

-- 7. 显示对话框按钮
studio.addQuickAction("show_dialog", "💬 对话框", function()
    studio.showConfirm("确认操作", "确定要执行此操作吗？", function()
        studio.toast("你点击了确定")
    end, function()
        studio.toast("你点击了取消")
    end)
end)

-- 8. 复制到剪贴板按钮
studio.addQuickAction("copy_file_path", "📋 复制路径", function()
    local filePath = studio.getActiveFile()
    if filePath then
        studio.copyToClipboard(filePath)
        studio.toast("已复制: " .. filePath)
    else
        studio.toast("没有打开的文件")
    end
end)

-- 9. 文件操作按钮
studio.addQuickAction("file_info", "📁 文件信息", function()
    local filePath = studio.getActiveFile()
    local projectPath = studio.getProjectPath()
    
    if filePath then
        local info = "当前文件: " .. filePath .. "\n"
        info = info .. "项目路径: " .. tostring(projectPath)
        studio.showMessage("文件信息", info)
    else
        studio.toast("没有打开的文件")
    end
end)

-- 10. Java 反射测试 - 加载类
studio.addQuickAction("java_load_class", "☕ 加载类", function()
    local clazz = studio.loadClass("com.luaforge.studio.lxclua.plugin.ReflectionTestClass")
    if clazz then
        studio.toast("类加载成功: " .. tostring(clazz))
    else
        studio.toast("类加载失败")
    end
end)

-- 11. Java 反射测试 - 静态字段
studio.addQuickAction("java_static_field", "☕ 静态字段", function()
    local className = "com.luaforge.studio.lxclua.plugin.ReflectionTestClass"
    
    -- 获取静态字段当前值
    local currentValue = studio.getStaticField(className, "staticField")
    studio.log("HelloPlugin", "静态字段当前值: " .. tostring(currentValue))
    
    -- 切换值
    local newValue
    if tostring(currentValue) == "静态字段初始值" then
        newValue = "从 Lua 修改的值"
    else
        newValue = "静态字段初始值"
    end
    
    -- 设置静态字段
    studio.setStaticField(className, "staticField", newValue)
    
    -- 再次获取确认
    local confirmValue = studio.getStaticField(className, "staticField")
    
    studio.showMessage("静态字段测试", 
        "修改前: " .. tostring(currentValue) .. "\n" ..
        "修改后: " .. tostring(confirmValue) .. "\n\n" ..
        "再次点击会切换回原值")
end)

-- 12. Java 反射测试 - 静态方法
studio.addQuickAction("java_static_method", "☕ 静态方法", function()
    local className = "com.luaforge.studio.lxclua.plugin.ReflectionTestClass"
    
    -- 调用无参静态方法
    local result1 = studio.callStaticMethod(className, "staticMethod", nil)
    studio.log("HelloPlugin", "staticMethod(): " .. tostring(result1))
    
    -- 调用带参静态方法
    local result2 = studio.callStaticMethod(className, "staticMethodWithParams", {10, 20})
    studio.log("HelloPlugin", "staticMethodWithParams(10, 20): " .. tostring(result2))
    
    studio.showMessage("静态方法测试",
        "staticMethod(): " .. tostring(result1) .. "\n" ..
        "staticMethodWithParams(10, 20): " .. tostring(result2))
end)

-- 13. Java 反射测试 - 创建实例
studio.addQuickAction("java_new_instance", "☕ 创建实例", function()
    local className = "com.luaforge.studio.lxclua.plugin.ReflectionTestClass"
    
    -- 无参构造
    local obj1 = studio.newInstance(className, nil)
    studio.log("HelloPlugin", "无参构造: " .. tostring(obj1))
    
    -- 带参构造 (String)
    local obj2 = studio.newInstance(className, {"Lua 传入的初始值"})
    studio.log("HelloPlugin", "String 构造: " .. tostring(obj2))
    
    -- 带参构造 (int, int)
    local obj3 = studio.newInstance(className, {100, 200})
    studio.log("HelloPlugin", "int,int 构造: " .. tostring(obj3))
    
    local info = "无参构造: " .. tostring(obj1) .. "\n"
    info = info .. "String 构造: " .. tostring(obj2) .. "\n"
    info = info .. "int,int 构造: " .. tostring(obj3)
    studio.showMessage("创建实例测试", info)
end)

-- 14. Java 反射测试 - 实例字段
-- 注意：使用全局变量保存实例，这样可以在多次点击之间保持状态
local savedInstance = nil

studio.addQuickAction("java_instance_field", "☕ 实例字段", function()
    local className = "com.luaforge.studio.lxclua.plugin.ReflectionTestClass"
    
    -- 如果实例不存在，创建新实例
    if not savedInstance then
        savedInstance = studio.newInstance(className, {"测试实例"})
        studio.log("HelloPlugin", "创建新实例")
    end
    
    -- 获取实例字段当前值
    local currentValue = studio.getField(savedInstance, "instanceField")
    studio.log("HelloPlugin", "实例字段当前值: " .. tostring(currentValue))
    
    -- 切换值
    local newValue
    if tostring(currentValue) == "测试实例" then
        newValue = "从 Lua 修改"
    elseif tostring(currentValue) == "从 Lua 修改" then
        newValue = "再次修改"
    else
        newValue = "测试实例"
    end
    
    -- 设置实例字段
    studio.setField(savedInstance, "instanceField", newValue)
    
    -- 再次获取确认
    local confirmValue = studio.getField(savedInstance, "instanceField")
    
    studio.showMessage("实例字段测试",
        "当前值: " .. tostring(currentValue) .. "\n" ..
        "修改后: " .. tostring(confirmValue) .. "\n\n" ..
        "再次点击会继续切换值")
end)

-- 15. Java 反射测试 - 实例方法
studio.addQuickAction("java_instance_method", "☕ 实例方法", function()
    local className = "com.luaforge.studio.lxclua.plugin.ReflectionTestClass"
    
    -- 创建实例
    local obj = studio.newInstance(className, {"Hello Lua"})
    
    -- 调用无参实例方法
    local result1 = studio.callMethod(obj, "instanceMethod", nil)
    studio.log("HelloPlugin", "instanceMethod(): " .. tostring(result1))
    
    -- 调用带参实例方法
    local result2 = studio.callMethod(obj, "instanceMethodWithParams", {"测试", 3})
    studio.log("HelloPlugin", "instanceMethodWithParams: " .. tostring(result2))
    
    -- 调用有返回值的方法
    local result3 = studio.callMethod(obj, "increment", nil)
    studio.log("HelloPlugin", "increment(): " .. tostring(result3))
    
    studio.showMessage("实例方法测试",
        "instanceMethod(): " .. tostring(result1) .. "\n" ..
        "instanceMethodWithParams: " .. tostring(result2) .. "\n" ..
        "increment(): " .. tostring(result3))
end)

-- 16. Java 反射测试 - 综合测试
studio.addQuickAction("java_full_test", "☕ 完整测试", function()
    local className = "com.luaforge.studio.lxclua.plugin.ReflectionTestClass"
    local results = {}
    
    -- 1. 加载类
    local clazz = studio.loadClass(className)
    table.insert(results, "1. 加载类: " .. (clazz and "成功" or "失败"))
    
    -- 2. 静态字段操作
    local staticValue = studio.getStaticField(className, "staticField")
    studio.setStaticField(className, "staticField", "Lua 修改的静态值")
    local newStaticValue = studio.getStaticField(className, "staticField")
    table.insert(results, "2. 静态字段: " .. tostring(staticValue) .. " -> " .. tostring(newStaticValue))
    
    -- 3. 静态方法调用
    local staticResult = studio.callStaticMethod(className, "staticMethodWithParams", {5, 3})
    table.insert(results, "3. 静态方法(5+3): " .. tostring(staticResult))
    
    -- 4. 创建实例
    local obj = studio.newInstance(className, {"Lua 创建的对象"})
    table.insert(results, "4. 创建实例: " .. tostring(obj))
    
    -- 5. 实例字段操作
    local fieldValue = studio.getField(obj, "instanceField")
    studio.setField(obj, "instanceField", "Lua 修改的实例字段")
    local newFieldValue = studio.getField(obj, "instanceField")
    table.insert(results, "5. 实例字段: " .. tostring(fieldValue) .. " -> " .. tostring(newFieldValue))
    
    -- 6. 实例方法调用
    local methodResult = studio.callMethod(obj, "instanceMethodWithParams", {"Lua", 2})
    table.insert(results, "6. 实例方法: " .. tostring(methodResult))
    
    -- 恢复静态字段
    studio.setStaticField(className, "staticField", "静态字段初始值")
    
    studio.showMessage("Java 反射完整测试", table.concat(results, "\n"))
end)

-- 17. 执行 Lua 脚本示例
studio.addQuickAction("exec_lua", "🔧 执行Lua", function()
    local result = studio.executeLua("return 1 + 2 + 3")
    studio.toast("执行结果: " .. tostring(result))
end)

-- 18. 添加菜单项示例
studio.addMenuItem("hello_menu", "👋 来自插件的消息", function()
    studio.showMessage("Hello Plugin", "这是从插件菜单项触发的消息！")
end)

studio.addMenuItem("current_file", "📄 显示当前文件", function()
    local file = studio.getActiveFile()
    if file then
        studio.toast("当前文件: " .. file)
    else
        studio.toast("没有打开的文件")
    end
end)

studio.addMenuDivider("divider1")

studio.addMenuItem("insert_date", "📅 插入日期", function()
    -- Date.toString() 是实例方法，需要先创建实例
    local date = studio.newInstance("java.util.Date", nil)
    local dateStr = studio.callMethod(date, "toString", nil)
    studio.insertText("-- " .. tostring(dateStr))
end)

-- 19. 监听文件保存事件
studio.registerEventListener("onFileSave", function(filePath)
    studio.log("HelloPlugin", "文件已保存: " .. tostring(filePath))
end)

-- 20. 监听文件打开事件
studio.registerEventListener("onFileOpen", function(filePath)
    local fileName = tostring(filePath):match("[^/]+$") or tostring(filePath)
    studio.toast("📂 打开文件: " .. fileName)
end)

-- 21. 文件树上下文菜单示例 - 对所有文件显示的"查看文件信息"
studio.addFileTreeMenuItem("ft_file_info", "📋 查看文件信息", nil, function(filePath, isDirectory)
    local fileInfo = studio.getFileInfo(filePath)
    if fileInfo then
        local info = "名称: " .. tostring(fileInfo.name) .. "\n"
        info = info .. "路径: " .. tostring(fileInfo.path) .. "\n"
        info = info .. "类型: " .. (fileInfo.isDirectory and "目录" or "文件") .. "\n"
        if not fileInfo.isDirectory then
            info = info .. "扩展名: ." .. tostring(fileInfo.extension) .. "\n"
            info = info .. "大小: " .. tostring(fileInfo.size) .. " 字节\n"
            info = info .. "修改: " .. os.date("%Y-%m-%d %H:%M:%S", fileInfo.lastModified / 1000) .. "\n"
        end
        info = info .. "父目录: " .. tostring(fileInfo.parentPath)
        studio.showMessage("文件信息", info)
    else
        studio.toast("文件不存在")
    end
end)

-- 22. 文件树上下文菜单示例 - 仅对 Lua 文件显示的"运行 Lua"
studio.addFileTreeMenuItem("ft_run_lua", "▶️ 运行 Lua", ".lua", function(filePath, isDirectory)
    studio.toast("正在运行: " .. tostring(filePath))
    local result = studio.executeLuaFile(filePath)
    studio.log("HelloPlugin", "Lua 执行结果: " .. tostring(result))
end)

-- 23. 文件树上下文菜单示例 - 仅对目录显示的"新建文件"
studio.addFileTreeMenuItem("ft_new_file_in_dir", "📄 新建文件", "directory", function(filePath, isDirectory)
    local callback = {
        onInput = function(text)
            if text and #text > 0 then
                local newFilePath = filePath .. "/" .. text
                local success = studio.createFile(newFilePath, "-- 新建文件\n")
                if success then
                    studio.toast("已创建: " .. text)
                else
                    studio.toast("创建失败")
                end
            end
        end
    }
    studio.showInputDialog("新建文件", "请输入文件名", "new_file.lua", callback)
end)

-- 24. 文件树上下文菜单示例 - 仅对图片文件显示的"预览图片"
studio.addFileTreeMenuItem("ft_preview_image", "🖼️ 预览图片", ".png", function(filePath, isDirectory)
    studio.toast("预览图片: " .. tostring(filePath))
end)

-- 25. 文件树上下文菜单示例 - 带图标的菜单项
studio.addFileTreeMenuItem("ft_copy_name", "📋 复制文件名", "file", function(filePath, isDirectory)
    local fileInfo = studio.getFileInfo(filePath)
    if fileInfo then
        studio.copyToClipboard(fileInfo.name)
        studio.toast("已复制: " .. fileInfo.name)
    end
end)

-- 26. 文件树上下文菜单分隔线示例
studio.addFileTreeMenuDivider("ft_divider1", nil)

-- 27. 文件树上下文菜单示例 - 对所有文件显示的"复制相对路径"
studio.addFileTreeMenuItem("ft_copy_relative", "📋 复制相对路径", nil, function(filePath, isDirectory)
    local projectPath = studio.getProjectPath()
    if projectPath and filePath:find(projectPath, 1, true) == 1 then
        local relativePath = filePath:sub(#projectPath + 2)
        studio.copyToClipboard(relativePath)
        studio.toast("已复制相对路径: " .. relativePath)
    else
        studio.copyToClipboard(filePath)
        studio.toast("已复制路径")
    end
end)

-- 28. 监听插件生命周期事件 - 插件加载完成
studio.registerEventListener("onPluginLoaded", function(pluginId)
    studio.log("HelloPlugin", "插件已加载: " .. tostring(pluginId))
    if pluginId ~= "hello_plugin" then
        studio.toast("📦 插件已加载: " .. pluginId)
    end
end)

-- 29. 监听插件生命周期事件 - 插件卸载
studio.registerEventListener("onPluginUnloaded", function(pluginId)
    studio.log("HelloPlugin", "插件已卸载: " .. tostring(pluginId))
    studio.toast("📤 插件已卸载: " .. pluginId)
end)

-- 30. 监听所有插件加载完成事件
studio.registerEventListener("onAllPluginsLoaded", function()
    studio.log("HelloPlugin", "所有插件加载完成")
end)

-- 31. 监听应用启动事件
studio.registerEventListener("onAppStart", function()
    studio.log("HelloPlugin", "应用已启动")
end)

-- 32. 测试插件依赖检查
studio.addQuickAction("test_dependencies", "🔗 检查依赖", function()
    -- 这里演示如何检查其他插件的依赖状态
    -- 实际使用时，可以在 manifest.json 中声明依赖
    studio.showMessage("依赖检查", 
        "插件依赖系统已启用\n\n" ..
        "- 支持必需依赖和可选依赖\n" ..
        "- 支持版本号检查\n" ..
        "- 自动按依赖顺序加载")
end)

-- 33. 测试插件类型信息
studio.addQuickAction("plugin_info", "📋 插件信息", function()
    local info = "插件 ID: hello_plugin\n" ..
                 "版本: 1.0.0\n" ..
                 "类型: Lua 插件\n" ..
                 "API 版本: 1\n\n" ..
                 "支持的功能:\n" ..
                 "- Java 反射调用\n" ..
                 "- 编辑器控制\n" ..
                 "- 文件操作\n" ..
                 "- UI 对话框\n" ..
                 "- 事件监听"
    studio.showMessage("Hello Plugin 信息", info)
end)

-- 34. 测试编辑器初始化事件
studio.registerEventListener("onEditorInit", function(projectPath)
    studio.log("HelloPlugin", "编辑器已初始化，项目路径: " .. tostring(projectPath))
end)

-- 35. 测试编辑器关闭事件
studio.registerEventListener("onEditorClose", function(projectPath)
    studio.log("HelloPlugin", "编辑器已关闭，项目路径: " .. tostring(projectPath))
end)

studio.log("HelloPlugin", "初始化完成，已注册 19 个快捷按钮 + 3 个菜单项 + 7 个文件树菜单项 + 6 个事件监听")
