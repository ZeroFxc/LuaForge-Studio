package com.luaforge.studio.lxclua.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.luaforge.studio.lxclua.R

/**
 * 构建结果弹窗展示的三种结果状态
 */
enum class BuildResultType {
    /** 构建成功，APK 已生成 */
    SUCCESS,
    /** 构建已被取消 */
    CANCELLED,
    /** 构建失败 */
    ERROR
}

/**
 * 构建结果弹窗
 *
 * 统一管理构建成功、失败、取消三种状态的展示：
 * - 成功：显示 APK 路径、安装按钮
 * - 取消：显示"构建已取消"提示，禁用安装操作
 * - 失败：显示错误信息，禁用安装操作
 *
 * @param showDialog 是否显示弹窗
 * @param resultType 构建结果类型
 * @param resultMessage 结果描述文本（成功时为 APK 路径，取消/失败时为提示信息）
 * @param onDismiss 关闭弹窗回调
 * @param onInstall 安装 APK 回调（仅在 SUCCESS 状态下可用）
 */
@Composable
fun BuildResultDialog(
    showDialog: Boolean,
    resultType: BuildResultType,
    resultMessage: String?,
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!showDialog) return

    val isSuccess = resultType == BuildResultType.SUCCESS

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (resultType) {
                    BuildResultType.SUCCESS -> stringResource(R.string.code_editor_build_success)
                    BuildResultType.CANCELLED -> stringResource(R.string.code_editor_build_cancelled)
                    BuildResultType.ERROR -> stringResource(R.string.editor_build_failed_title)
                }
            )
        },
        text = {
            Column {
                when (resultType) {
                    BuildResultType.SUCCESS -> {
                        Text(
                            text = stringResource(R.string.code_editor_apk_built),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = resultMessage ?: stringResource(R.string.editor_unknown_path),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.code_editor_install_prompt),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    BuildResultType.CANCELLED -> {
                        Text(
                            text = stringResource(R.string.build_cancelled_message),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (!resultMessage.isNullOrEmpty() && resultMessage != "已取消") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = resultMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    BuildResultType.ERROR -> {
                        Text(
                            text = stringResource(R.string.build_error_message),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (!resultMessage.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = resultMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isSuccess) {
                TextButton(onClick = onInstall) {
                    Text(
                        text = stringResource(R.string.code_editor_install),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (isSuccess) stringResource(R.string.cancel)
                    else stringResource(R.string.close)
                )
            }
        }
    )
}

/**
 * 兼容旧接口的 InstallApkDialog
 * 仅用于构建成功场景，新代码建议直接使用 BuildResultDialog
 */
@Composable
fun InstallApkDialog(
    showInstallDialog: Boolean,
    apkFilePath: String?,
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    BuildResultDialog(
        showDialog = showInstallDialog,
        resultType = BuildResultType.SUCCESS,
        resultMessage = apkFilePath,
        onDismiss = onDismiss,
        onInstall = onInstall,
        modifier = modifier
    )
}