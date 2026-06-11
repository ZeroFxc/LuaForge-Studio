package com.luaforge.studio.lxclua.plugin.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.luaforge.studio.lxclua.R
import com.luaforge.studio.lxclua.plugin.PluginManager
import com.luaforge.studio.lxclua.plugin.bridge.PluginWebUIBridge
import java.io.File

/**
 * 插件 WebUI 界面组件
 *
 * 在独立页面中嵌入 WebView，加载插件的 HTML/CSS/JS 界面。
 * 通过 LXC JavaScript 接口实现 Web ↔ 宿主双向通信。
 *
 * 特性:
 * - WebView 内导航回退（先回退 WebView 历史，再关闭页面）
 * - 页面加载进度条
 * - 加载错误时显示重试按钮
 * - 系统返回键拦截（BackHandler）
 * - 响应式布局填满整个可用空间
 *
 * @param pluginId 插件 ID
 * @param page Web 页面文件名（相对于插件目录下 web/ 子目录），默认 index.html
 * @param title 页面标题，为 null 时使用插件的 manifest.name
 * @param onBack 返回按钮 / 系统返回键回调
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginWebUIScreen(
    pluginId: String,
    page: String = "index.html",
    title: String? = null,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    // 查找 web 文件
    val plugin = remember(pluginId) {
        PluginManager.loadedPlugins.find { it.manifest.id == pluginId }
    }
    val webDir = plugin?.let { File(it.directory, "web") }
    val pageFile = webDir?.let { File(it, page) }
    val displayTitle = title ?: plugin?.manifest?.name ?: "插件界面"

    // 页面加载状态
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBackInWebView by remember { mutableStateOf(false) }

    // 预取字符串资源，避免在非 Composable 上下文（WebViewClient 回调）中调用 stringResource
    val unknownErrorText = stringResource(R.string.webui_unknown_error)

    // 拦截系统返回键：优先回退 WebView 历史
    BackHandler(enabled = canGoBackInWebView) {
        webViewRef?.goBack()
    }
    // 无法回退 WebView 历史时，关闭 WebUI（不限制 loading/error 状态，确保始终可返回）
    BackHandler(enabled = !canGoBackInWebView) {
        onBack()
    }

    // 页面关闭时清理 WebView
    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (canGoBackInWebView) {
                            webViewRef?.goBack()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    // 刷新按钮
                    if (loadError != null) {
                        IconButton(onClick = {
                            loadError = null
                            webViewRef?.reload()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.refresh)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // 文件不存在或插件未找到
                pageFile == null || !pageFile.exists() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.plugin_no_webui, page),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                // 加载错误
                loadError != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.webui_load_error),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        loadError?.let { error ->
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                                textAlign = TextAlign.Center,
                                maxLines = 4
                            )
                        }
                        TextButton(
                            onClick = {
                                loadError = null
                                webViewRef?.reload()
                            },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(stringResource(R.string.webui_retry))
                        }
                    }
                }
                // 文件存在：始终创建 WebView（不等待加载状态），避免初始 isLoading=true 导致
                // WebView 永不被创建的死循环；加载指示器覆盖在 WebView 上方
                else -> {
                    val bridge = remember { PluginWebUIBridge(pluginId) }
                    bridge.onBackRequested = onBack

                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )

                                android.util.Log.d("PluginWebUI[$pluginId]", "WebView factory 创建，准备加载: ${pageFile.absolutePath}")

                                // WebView 基础设置
                                with(settings) {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    allowFileAccess = true
                                    allowContentAccess = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    setSupportZoom(true)
                                    // 允许跨域加载 file:// 资源（Android 7.0+ 的 Strict Origin Policy 问题）
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                                        allowUniversalAccessFromFileURLs = true
                                        allowFileAccessFromFileURLs = true
                                    }
                                    // cookie
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    }
                                    // 响应式适配
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                                        setLayoutAlgorithm(android.webkit.WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING)
                                    }
                                }

                                // JS 桥接接口
                                addJavascriptInterface(bridge.JsApiBridge(), "LXC")

                                // 将 WebView 附着到桥接器，启用宿主→Web 消息发送
                                bridge.attachWebView(this)

                                // 页面加载状态监听
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        android.util.Log.d("PluginWebUI[$pluginId]", "onPageStarted: $url")
                                        isLoading = true
                                        loadError = null
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        android.util.Log.d("PluginWebUI[$pluginId]", "onPageFinished: $url, progress=100")
                                        isLoading = false
                                        loadProgress = 100
                                        canGoBackInWebView = view?.canGoBack() ?: false
                                    }

                                    @Suppress("DEPRECATION")
                                    override fun onReceivedError(
                                        view: WebView?,
                                        errorCode: Int,
                                        description: String?,
                                        failingUrl: String?
                                    ) {
                                        super.onReceivedError(view, errorCode, description, failingUrl)
                                        android.util.Log.e("PluginWebUI[$pluginId]", "onReceivedError(deprecated): code=$errorCode, desc=$description, url=$failingUrl")
                                        isLoading = false
                                        loadError = "错误码 $errorCode: ${description ?: "未知错误"}"
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        val errorCode = error?.errorCode ?: -1
                                        val errorDesc = error?.description?.toString() ?: unknownErrorText
                                        val url = request?.url?.toString() ?: "unknown"
                                        android.util.Log.e("PluginWebUI[$pluginId]", "onReceivedError: code=$errorCode, desc=$errorDesc, url=$url, isMainFrame=${request?.isForMainFrame}")
                                        // 仅主框架错误才阻止显示（子资源错误如 CSS/JS 404 不阻塞页面）
                                        if (request?.isForMainFrame == true) {
                                            isLoading = false
                                            loadError = errorDesc
                                        }
                                    }

                                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                        super.doUpdateVisitedHistory(view, url, isReload)
                                        canGoBackInWebView = view?.canGoBack() ?: false
                                    }
                                }

                                // 加载进度监听
                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        super.onProgressChanged(view, newProgress)
                                        loadProgress = newProgress
                                        android.util.Log.d("PluginWebUI[$pluginId]", "onProgressChanged: $newProgress%")
                                        if (newProgress == 100) {
                                            isLoading = false
                                        }
                                    }

                                    override fun onReceivedTitle(view: WebView?, pageTitle: String?) {
                                        super.onReceivedTitle(view, pageTitle)
                                        android.util.Log.d("PluginWebUI[$pluginId]", "onReceivedTitle: $pageTitle")
                                    }
                                }

                                android.util.Log.d("PluginWebUI[$pluginId]", "开始 loadUrl: file://${pageFile.absolutePath}")
                                loadUrl("file://${pageFile.absolutePath}")
                            }.also { webViewRef = it }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // 加载指示器覆盖在 WebView 上方
                    if (isLoading && loadError == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (loadProgress > 0 && loadProgress < 100) {
                                    Text(
                                        text = "$loadProgress%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}