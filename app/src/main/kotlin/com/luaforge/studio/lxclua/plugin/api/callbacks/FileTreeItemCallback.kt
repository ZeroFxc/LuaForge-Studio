package com.luaforge.studio.lxclua.plugin.api.callbacks

/**
 * 文件树菜单项回调接口
 */
interface FileTreeItemCallback {
    /**
     * 当菜单项被点击时调用
     * @param filePath 被操作的文件路径
     * @param isDirectory 是否为目录
     */
    fun onItemClick(filePath: String, isDirectory: Boolean)
}