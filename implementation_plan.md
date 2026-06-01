# LuaForge-Studio 插件系统设计方案 (更新版)

本项目是基于 Android 平台的 Lua 应用开发 IDE。为了最大化其扩展性，我们设计并实现一个完整的插件系统，同时支持：
1. **Lua 插件**：直接加载执行的 Lua 脚本（适合轻量开发、动态调试，使用 `luajava` 与主程序互通）。
2. **DEX 插件 / APK 插件**：通过 `DexClassLoader` 动态加载编译好的 `.dex` 或 `.apk` 文件中的类（适合高性能扩展、引入第三方 Java/Kotlin 依赖的复杂插件）。

---

## 方案设计

### 1. 插件存放目录与结构
所有插件都存放在外部存储 `/storage/emulated/0/LXC-LUA/plugins/` 文件夹下。每个插件都拥有独立的子文件夹，其标准结构如下：
```text
/storage/emulated/0/LXC-LUA/plugins/
  └── my_plugin/
      ├── manifest.json   # 插件元数据（清单文件）
      ├── plugin.dex      # (如果是 DEX/APK 插件) 编译后的二进制字节码
      ├── main.lua        # (如果是 Lua 插件) 主入口脚本
      └── icon.png        # (可选) 插件图标
```

`manifest.json` 示例（Lua 插件）：
```json
{
  "id": "com.example.lua_helper",
  "name": "Lua 助手",
  "version": "1.0.0",
  "description": "基于 Lua 编写的演示插件",
  "author": "Developer",
  "type": "lua",
  "main": "main.lua",
  "apiVersion": 1
}
```

`manifest.json` 示例（DEX/APK 插件）：
```json
{
  "id": "com.example.java_addon",
  "name": "Java 高级工具箱",
  "version": "1.0.0",
  "description": "基于 DEX/APK 动态加载的 Kotlin/Java 插件",
  "author": "Developer",
  "type": "dex",
  "main": "plugin.dex",
  "mainClass": "com.example.java_addon.MyPluginEntry",
  "apiVersion": 1
}
```

### 2. 插件统一接口与 API 桥梁
为了让 Lua 和 DEX/APK 插件采用统一的方式访问主程序功能，我们定义了一个 Host 桥梁接口 `IPluginBridge` 以及插件入口接口 `IPlugin`。

#### Unified Interface: `IPlugin`
DEX/APK 插件的 `mainClass` 必须实现该接口：
```kotlin
package com.luaforge.studio.lxclua.plugin

import android.content.Context

interface IPlugin {
    fun onLoad(context: Context, bridge: IPluginBridge)
    fun onUnload()
}
```

#### Bridge Interface: `IPluginBridge`
```kotlin
package com.luaforge.studio.lxclua.plugin

interface IPluginBridge {
    fun toast(message: String)
    fun log(tag: String, message: String)
    fun getVersion(): String
    fun getActiveFile(): String?
    fun getActiveText(): String?
    fun setActiveText(text: String)
    fun insertText(text: String)
    fun getProjectPath(): String?
    
    // 动态注册编辑器快捷工具栏动作
    fun addQuickAction(key: String, label: String, onClick: Runnable)
    fun removeQuickAction(key: String)
    
    // 监听 IDE 事件 (如 "onFileOpen", "onFileSave", "onTextChanged")
    fun registerEventListener(eventName: String, listener: IPluginEventListener)
}

interface IPluginEventListener {
    fun onEvent(eventData: String) // 传递 JSON 字符串类型的事件数据
}
```

对于 Lua 插件，我们将通过 `luajava` 暴露相同的 `IPluginBridge` 实例作为全局变量 `studio`，Lua 可以使用冒号语法直接调用接口方法，并直接传递 Lua 函数作为回调。

---

### 3. 插件管理器 (PluginManager)
提供 `PluginManager.kt` 单例类，作为插件的核心驱动引擎，具有以下功能：
- **插件发现与加载**：
  - 扫描 `plugins/` 目录，解析 `manifest.json`。
  - 对于 **Lua 插件**：创建一个独立的 `LuaState` 环境，将 `IPluginBridge` 实例作为全局变量 `studio` 绑定，并执行主入口 Lua 文件。
  - 对于 **DEX / APK 插件**：在应用的缓存区中拷贝插件的 `.dex` 或 `.apk` 文件，使用 `DexClassLoader` 实例化对应的 `mainClass`，传入 `IPluginBridge` 完成加载。
- **配置持久化**：使用 `Preferences` 记录插件的开启/关闭状态。
- **生命周期与事件中心**：管理 IDE 的事件监听器（例如文件打开、文件保存、文本改变），并在相应时刻回调 Lua/DEX 插件中注册的事件回调。
- **自定义快捷功能**：合并收集插件注册的自定义 `QuickAction`，动态注入到编辑器的快捷工具栏中。
- **插件安装与删除**：支持导入 `.zip` 格式的插件包并解包安装；支持彻底删除插件。

---

## 拟定修改及新建文件

### [Component Name] 插件内核与管理器

#### [NEW] [PluginData.kt](file:///e:/Soft/Proje/LuaForge-Studio-main/app/src/main/kotlin/com/luaforge/studio/lxclua/plugin/PluginData.kt)
包含：
- `PluginType` 枚举（LUA, DEX, APK）。
- `PluginManifest` 实体数据类。
- `IPlugin` 插件入口接口。
- `IPluginBridge` 和 `IPluginEventListener` 接口定义。

#### [NEW] [PluginManager.kt](file:///e:/Soft/Proje/LuaForge-Studio-main/app/src/main/kotlin/com/luaforge/studio/lxclua/plugin/PluginManager.kt)
实现插件的主引擎加载与卸载逻辑，提供基于 `DexClassLoader` 运行 DEX/APK 的逻辑，以及基于 `LuaStateFactory` 运行 Lua 脚本的环境。

---

### [Component Name] 插件管理 UI

#### [NEW] [PluginScreen.kt](file:///e:/Soft/Proje/LuaForge-Studio-main/app/src/main/kotlin/com/luaforge/studio/lxclua/ui/plugin/PluginScreen.kt)
设计 Compose 界面：
- 展示插件列表，按类型（Lua / DEX / APK）以及加载状态（已加载、未启用）进行分类和标记。
- 提供 Toggle 开关控制插件使能。
- 提供右上角“安装插件”按钮（触发 zip 文件导入）。
- 支持点击卸载删除。

#### [MODIFY] [MainActivity.kt](file:///e:/Soft/Proje/LuaForge-Studio-main/app/src/main/kotlin/com/luaforge/studio/lxclua/MainActivity.kt)
- 在 App 初始化或 `MainActivity.onCreate` 时调用 `PluginManager.init(this)`。
- 修改抽屉菜单 `MainContentType`，新增 `PLUGINS` 项，在侧边栏显示“插件管理”入口。
- 将 `PluginScreen` 集成进主页 Crossfade 切换中。

---

### [Component Name] 编辑器与工具栏适配

#### [MODIFY] [QuickActionToolbar.kt](file:///e:/Soft/Proje/LuaForge-Studio-main/app/src/main/kotlin/com/luaforge/studio/lxclua/ui/editor/QuickActionToolbar.kt)
- 扩展 `QuickAction`，使其支持除了 `R.string` 资源 ID 之外的自定义字符串 Label。

#### [MODIFY] [CodeEditScreen.kt](file:///e:/Soft/Proje/LuaForge-Studio-main/app/src/main/kotlin/com/luaforge/studio/lxclua/ui/editor/CodeEditScreen.kt)
- 合并系统默认的 `quickActions` 与 `PluginManager` 动态添加的快捷动作。
- 在文件打开、保存等关键交互位置触发 `PluginManager.notifyEvent` 派发事件。

#### [MODIFY] [EditorViewModel.kt](file:///e:/Soft/Proje/LuaForge-Studio-main/app/src/main/kotlin/com/luaforge/studio/lxclua/ui/editor/viewmodel/EditorViewModel.kt)
- 在编辑器内容改变的 ContentListener 中，触发事件派发，以支持 `onTextChanged` 插件监听。

---

## 验证方案

### 自动化与手动测试
1. **Lua 插件加载测试**：
   - 释出并加载 `HelloPlugin`，验证 `studio` API 功能是否正常。
2. **DEX/APK 插件加载测试**：
   - 编写一个打包好的 `plugin.dex` / `plugin.apk` 测试插件，验证 `DexClassLoader` 是否成功解析类并调用 `onLoad`，能否同样实现 Toast 弹窗和添加快捷按钮的功能。
3. **安全与资源清理测试**：
   - 停用插件时调用 `onUnload()`，验证动态添加的快捷按钮是否同步被移除，绑定的事件监听器是否被正确注销。

## 待探讨的问题

> [!NOTE]
> 1. **主线程执行**：为了保证 UI 操作安全（如操作编辑器文字、显示 Toast），插件的加载与接口调用将统一派发在主线程执行。如果插件需要进行网络或高强度耗时计算，请插件开发者在插件内自行开启子线程或协程处理。
> 2. **DEX 编译链**：DEX/APK 插件需要预先使用 Android SDK 的 D8/DX 编译生成，后续我们可以考虑在 IDE 内置一个 DEX 编译器，这样甚至能支持在 IDE 里直接编译编写 Java/Kotlin 插件。目前阶段，插件需要在电脑上编译完成后导入到手机运行。
