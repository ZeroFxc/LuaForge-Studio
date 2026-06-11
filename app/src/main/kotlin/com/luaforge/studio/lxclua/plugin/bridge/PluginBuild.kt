package com.luaforge.studio.lxclua.plugin.bridge

import android.content.Context
import com.luaforge.studio.lxclua.plugin.api.IPluginBridgeBuild
import com.luaforge.studio.lxclua.plugin.state.EventManager
import com.luaforge.studio.lxclua.plugin.state.PluginEvents
import com.luaforge.studio.lxclua.ui.editor.buildProject
import com.luaforge.studio.lxclua.ui.editor.compileCurrentFile
import com.luaforge.studio.lxclua.utils.NonBlockingToastState
import kotlinx.coroutines.runBlocking

/**
 * 构建系统桥梁实现
 *
 * 为插件提供构建/编译能力，并通过事件系统通知所有注册了钩子的插件
 */
class PluginBuild(private val context: Context) : IPluginBridgeBuild {

    companion object {
        /** 全局构建取消标志，由 cancelBuild() 设置，EditorFunctions 检查 */
        @Volatile
        var buildCancelled: Boolean = false

        /** 全局最近构建结果，由 EditorFunctions 和 PluginBuild 共同写入，供所有插件查询 */
        @Volatile
        var lastGlobalResult: String? = null
    }

    /**
     * 构建整个项目（异步执行）
     *
     * 构建在后台线程执行，避免因主线程调用 runBlocking 导致 ANR。
     * 调用后立即返回 "building:构建已启动"。
     * 插件应通过 plugin.events.on("ON_BUILD_FINISH", ...) 监听构建结果，
     * 或轮询 plugin.build.getLastBuildResult()。
     *
     * @param projectPath 项目路径
     * @return 固定返回 "building:构建已启动"，表示构建已投递到后台
     */
    override fun buildProject(projectPath: String): String {
        // 触发构建开始事件（EditorFunctions.buildProject 内部也会触发，此处不重复触发）
        EventManager.fireEvent(PluginEvents.ON_BUILD_START, projectPath, "project")

        // 检查取消标志（插件可能在 onBuildStart 中调用了 cancelBuild）
        if (buildCancelled) {
            buildCancelled = false
            lastGlobalResult = "cancelled:已取消"
            return "cancelled:已取消"
        }

        // 在后台线程执行构建，避免主线程阻塞导致 ANR
        Thread {
            try {
                val buildResult = runBlocking {
                    buildProject(context, projectPath)
                }
                // buildProject 内部已处理 canceled 情况
                if (!buildResult.startsWith("cancelled:")) {
                    val success = !buildResult.startsWith("error:")
                    EventManager.fireEvent(PluginEvents.ON_BUILD_FINISH, projectPath, buildResult, success)
                    if (!success) {
                        EventManager.fireEvent(PluginEvents.ON_BUILD_ERROR, projectPath, buildResult, "project")
                    }
                }
                lastGlobalResult = buildResult
            } catch (e: Exception) {
                val errorMsg = "error: ${e.message}"
                EventManager.fireEvent(PluginEvents.ON_BUILD_FINISH, projectPath, errorMsg, false)
                EventManager.fireEvent(PluginEvents.ON_BUILD_ERROR, projectPath, errorMsg, "project")
                lastGlobalResult = errorMsg
            }
        }.start()

        return "building:构建已启动"
    }

    override fun compileFile(filePath: String): String {
        // 触发编译开始事件
        EventManager.fireEvent(PluginEvents.ON_BUILD_START, filePath, "compile")

        // 检查取消标志（插件可能在 onBuildStart 中调用了 cancelBuild）
        if (buildCancelled) {
            buildCancelled = false
            lastGlobalResult = "cancelled:已取消"
            return "cancelled:已取消"
        }

        val result = try {
            val luaState = com.luajava.LuaStateFactory.newLuaState()
            luaState.openLibs()
            val compileResult = com.luaforge.studio.lxclua.utils.ConsoleUtil.build(luaState, filePath)
            val resultTable = compileResult as? Map<*, *>
            val outputPath = resultTable?.get("path") as? String
            val error = resultTable?.get("error") as? String
            luaState.close()

            if (outputPath != null) {
                EventManager.fireEvent(PluginEvents.ON_BUILD_FINISH, filePath, outputPath, true)
                outputPath
            } else {
                val errorMsg = error ?: "编译失败"
                EventManager.fireEvent(PluginEvents.ON_BUILD_FINISH, filePath, errorMsg, false)
                EventManager.fireEvent(PluginEvents.ON_BUILD_ERROR, filePath, errorMsg, "compile")
                errorMsg
            }
        } catch (e: Exception) {
            val errorMsg = "编译异常: ${e.message}"
            EventManager.fireEvent(PluginEvents.ON_BUILD_FINISH, filePath, errorMsg, false)
            EventManager.fireEvent(PluginEvents.ON_BUILD_ERROR, filePath, errorMsg, "compile")
            errorMsg
        }

        lastGlobalResult = result
        return result
    }

    override fun getLastBuildResult(): String? {
        return lastGlobalResult
    }

    /**
     * 取消当前正在进行的构建
     * 设置全局取消标志，EditorFunctions 在 ON_BUILD_START 事件处理完后检查该标志
     * 该标志仅对紧接的下一次构建生效，构建函数检查后自动重置为 false
     */
    override fun cancelBuild() {
        buildCancelled = true
    }
}