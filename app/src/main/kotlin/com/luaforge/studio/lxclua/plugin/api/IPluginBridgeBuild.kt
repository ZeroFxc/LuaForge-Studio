package com.luaforge.studio.lxclua.plugin.api

/**
 * 构建系统相关 API
 *
 * 提供插件对 LXC-LUA 构建流程的访问能力，包括：
 * - 触发项目构建（生成 APK）
 * - 触发单文件编译
 * - 通过事件系统注册构建钩子（onBuildStart / onBuildFinish / onBuildError）
 *
 * 构建钩子使用方式（Lua 插件）:
 * ```lua
 * -- 构建开始前回调
 * plugin.events.register(plugin.events.ON_BUILD_START, function(projectPath, buildType)
 *     plugin.logger.info("BuildHook", "开始构建: " .. projectPath .. " 类型: " .. buildType)
 * end)
 *
 * -- 构建完成后回调
 * plugin.events.register(plugin.events.ON_BUILD_FINISH, function(projectPath, result, success)
 *     if success then
 *         plugin.sys.toast("构建成功: " .. result)
 *     else
 *         plugin.sys.toast("构建失败: " .. result)
 *     end
 * end)
 *
 * -- 构建出错回调
 * plugin.events.register(plugin.events.ON_BUILD_ERROR, function(projectPath, errorMsg, buildType)
 *     plugin.logger.error("BuildHook", "构建出错: " .. errorMsg)
 * end)
 * ```
 */
interface IPluginBridgeBuild {

    /**
     * 触发项目构建（生成 APK）
     * 会依次触发 ON_BUILD_START -> 构建过程 -> ON_BUILD_FINISH/ON_BUILD_ERROR 事件
     *
     * @param projectPath 项目根目录路径
     * @return 构建结果字符串（成功返回 APK 路径，失败返回 "error: ..."）
     */
    fun buildProject(projectPath: String): String

    /**
     * 编译单个 Lua/Aly 文件
     * 会依次触发 ON_BUILD_START -> 编译过程 -> ON_BUILD_FINISH/ON_BUILD_ERROR 事件
     *
     * @param filePath 要编译的文件路径
     * @return 编译结果字符串（成功返回编译输出路径，失败返回错误信息）
     */
    fun compileFile(filePath: String): String

    /**
     * 获取最近一次构建/编译的输出结果
     *
     * @return 构建结果字符串，若从未构建过返回 null
     */
    fun getLastBuildResult(): String?

    /**
     * 取消当前正在进行的构建（在 ON_BUILD_START 回调中调用有效）
     *
     * 典型用法: 在 onBuildStart 事件回调中根据条件取消
     * ```lua
     * plugin.events.register(plugin.events.ON_BUILD_START, function(projectPath, buildType)
     *     if someCondition then
     *         plugin.build.cancelBuild()
     *         return
     *     end
     * end)
     * ```
     */
    fun cancelBuild()
}