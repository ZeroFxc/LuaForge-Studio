/**
 * WebUI 示例插件 — JavaScript 逻辑
 *
 * 演示 window.LXC API 的所有功能:
 *   LXC.log(msg)              — 输出到 Logcat
 *   LXC.toast(msg)            — 短 Toast
 *   LXC.toastLong(msg)        — 长 Toast
 *   LXC.callLua(fn, argsJson) — 调用 Lua 全局函数
 *   LXC.fireEvent(name, data) — 触发 Lua 事件
 *   LXC.getPluginData(key)    — 读持久化数据（按插件隔离）
 *   LXC.setPluginData(k, v)   — 写持久化数据（按插件隔离）
 *   LXC.removePluginData(key) — 删持久化数据
 *   LXC.clearPluginData()     — 清空持久化数据
 *   LXC.getAllPluginDataKeys()— 列出所有持久化数据键名
 *   LXC.getPluginId()         — 获取插件 ID
 *   LXC.getDeviceInfo()       — 获取设备信息 JSON
 *   LXC.getBatteryInfo()      — 获取电池信息 JSON
 *   LXC.getNetworkType()      — 获取网络类型
 *   LXC.getAppVersion()       — 获取应用版本
 *   LXC.getStoragePath()      — 获取存储路径
 *   LXC.openUrl(url)          — 在浏览器中打开 URL
 *   LXC.shareText(text)       — 系统分享
 *   LXC.vibrate(ms)           — 震动
 *   LXC.copyToClipboard(text) — 复制到剪贴板
 *   LXC.getClipboard()        — 读取剪贴板内容
 *   LXC.getTimestamp()        — 获取当前时间戳 (ms)
 *   LXC.getSystemProperty(k)  — 读取 Java 系统属性 (如 os.version)
 *   LXC.getThemeMode()        — 获取系统深色模式 ("dark"/"light")
 *   LXC.sendNotification(t,s) — 发送系统通知
 *   LXC.execShell(cmd)        — 执行 Shell 命令并返回输出
 *   LXC.navigateBack()        — 返回上一页
 *
 * 监听 Lua → JS 消息:
 *   window.addEventListener('lxc-message', function(e) {
 *       // e.detail 即 Json 消息体
 *   });
 */

'use strict';

// ==================== 初始化 ====================

/** 初始化主题：根据系统深色/浅色模式设置 data-theme 属性 */
function applyTheme() {
    try {
        var mode = LXC.getThemeMode();
        document.documentElement.setAttribute('data-theme', mode);
        LXC.log('主题模式: ' + mode);
    } catch (e) {
        document.documentElement.setAttribute('data-theme', 'light');
    }
}

document.addEventListener('DOMContentLoaded', function () {
    LXC.log('WebUI index.html 已加载');
    LXC.toast('WebUI 已就绪');

    // 应用系统主题
    applyTheme();

    // 加载设备信息
    refreshDeviceInfo();

    // 加载已保存的计数器值
    loadCounter();
});

// ==================== 设备信息 ====================

function refreshDeviceInfo() {
    var infoJson = LXC.getDeviceInfo();
    var info;
    try {
        info = JSON.parse(infoJson);
    } catch (e) {
        info = {};
    }

    var container = document.getElementById('device-info');
    if (!container) return;

    var fields = [
        ['型号', info.model],
        ['制造商', info.manufacturer],
        ['系统版本', 'Android ' + info.versionRelease],
        ['SDK', info.sdkVersion],
        ['语言', info.language],
        ['分辨率', info.screenWidth + '×' + info.screenHeight],
        ['密度', info.density],
        ['插件ID', LXC.getPluginId()]
    ];

    container.innerHTML = fields.map(function (f) {
        return '<div class="di-key">' + f[0] + '</div><div class="di-value">' + (f[1] || '—') + '</div>';
    }).join('');
}

// ==================== 计数器初始化 ====================

/** 页面加载时从 Lua 读取已存储的计数器值并显示 */
function loadCounter() {
    var result = LXC.callLua('jsGetCounter', '');
    var display = document.getElementById('counter-display');
    if (display && result && !isNaN(result)) {
        display.textContent = result;
        LXC.log('计数器初始化: ' + result);
    }
}

// ==================== JS → Lua 调用 ====================

/** Echo 测试：JS 发送文本到 Lua，Lua 原样返回 */
function doEcho() {
    var input = document.getElementById('echo-input');
    var text = input.value.trim();
    if (!text) {
        LXC.toast('请输入内容');
        return;
    }
    var result = LXC.callLua('jsEcho', JSON.stringify(text));
    document.getElementById('echo-result').textContent = 'Lua 返回: ' + result;
    LXC.log('Echo 结果: ' + result);
}

/** 保存配置到 Lua */
function doSaveConfig() {
    var key = document.getElementById('config-key').value.trim();
    var value = document.getElementById('config-value').value.trim();
    if (!key) { LXC.toast('请输入键名'); return; }

    var data = JSON.stringify({ key: key, value: value });
    var result = LXC.callLua('jsSaveConfig', data);
    document.getElementById('config-result').textContent = '结果: ' + result;
    if (result === 'ok') {
        LXC.toast('配置已保存');
    }
}

/** 计数器 +1 */
function doIncrement() {
    var result = LXC.callLua('jsIncrementCounter', '');
    var display = document.getElementById('counter-display');
    if (display && result && !isNaN(result)) {
        display.textContent = result;
    }
}

// ==================== 事件触发 ====================

/** 触发 Lua 事件（通过 EventManager） */
function fireLuaEvent(name) {
    var data = JSON.stringify({
        eventName: name,
        time: new Date().toLocaleTimeString()
    });
    LXC.fireEvent(name, data);
    LXC.toast('已触发事件: ' + name);
    LXC.log('触发事件: ' + name + ' -> ' + data);
}

// ==================== 系统功能 ====================

function doToast() {
    LXC.toast('你好！来自 WebUI 的问候');
}

function doLongToast() {
    LXC.toastLong('这是一条较长的提示信息，来自 WebUI 插件');
}

function doCopy(text) {
    LXC.copyToClipboard(text);
}

function doVibrate() {
    LXC.vibrate(50);
}

function doSaveData() {
    LXC.setPluginData('html_theme', 'dark');
    LXC.setPluginData('html_font_size', '16');
    LXC.setPluginData('html_last_access', new Date().toLocaleString());
    document.getElementById('storage-result').textContent = '已保存 3 个键值对';
    LXC.toast('数据已保存');
}

function doLoadData() {
    var theme = LXC.getPluginData('html_theme');
    var fontSize = LXC.getPluginData('html_font_size');
    var lastAccess = LXC.getPluginData('html_last_access');
    document.getElementById('storage-result').textContent =
        'theme=' + (theme || '无') + ', fontSize=' + (fontSize || '无') + ', lastAccess=' + (lastAccess || '无');
    LXC.log('读取数据: ' + theme + ', ' + fontSize + ', ' + lastAccess);
}

// ==================== Lua → JS 消息监听 ====================

window.addEventListener('lxc-message', function (e) {
    var detail = e.detail;
    var msgLog = document.getElementById('lua-messages');
    if (!msgLog) return;

    var now = new Date().toLocaleTimeString();
    var action = (detail && detail.action) ? detail.action : '未知';
    var content = JSON.stringify(detail, null, 0);

    var item = document.createElement('div');
    item.className = 'msg-item';
    item.innerHTML =
        '<div class="msg-time">' + now + ' &middot; ' + action + '</div>' +
        '<div class="msg-content">' + escapeHtml(content) + '</div>';
    msgLog.insertBefore(item, msgLog.firstChild);

    // 处理特定消息
    if (detail && detail.action === 'reset') {
        document.getElementById('counter-display').textContent = '0';
        LXC.toast('计数器已重置 (来自Lua)');
    }
});

// ==================== 工具函数 ====================

function clearMessages() {
    document.getElementById('lua-messages').innerHTML = '';
    LXC.toast('日志已清空');
}

function escapeHtml(str) {
    var div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// ==================== 新增 API 演示 ====================

function demoGetBattery() {
    var info = LXC.getBatteryInfo();
    try {
        var batt = JSON.parse(info);
        var text = '电量: ' + batt.level + '%  |  ' + batt.status + '  |  充电: ' + batt.charging;
        document.getElementById('sys-info-result').textContent = text;
        LXC.toast(text);
    } catch (e) {
        document.getElementById('sys-info-result').textContent = '获取失败';
    }
}

function demoGetNetwork() {
    var type = LXC.getNetworkType();
    document.getElementById('sys-info-result').textContent = '网络类型: ' + type;
}

function demoGetAppVersion() {
    var ver = LXC.getAppVersion();
    document.getElementById('sys-info-result').textContent = 'App版本: ' + ver;
}

function demoOpenUrl() {
    LXC.openUrl('https://github.com');
}

function demoShare() {
    LXC.shareText('来自 LXC WebUI 插件的分享测试！');
}

function demoGetAllKeys() {
    var keysJson = LXC.getAllPluginDataKeys();
    document.getElementById('storage-result').textContent = '所有键: ' + keysJson;
}

function demoGetTheme() {
    var mode = LXC.getThemeMode();
    document.getElementById('sys-info-result').textContent = '系统主题: ' + mode;
    // 同时刷新页面主题
    document.documentElement.setAttribute('data-theme', mode);
}

function demoGetTimestamp() {
    var ts = LXC.getTimestamp();
    var date = new Date(parseInt(ts));
    document.getElementById('sys-info-result').textContent =
        '时间戳: ' + ts + ' (' + date.toLocaleString() + ')';
}

function demoGetClipboard() {
    var text = LXC.getClipboard();
    document.getElementById('ext-result').textContent = text
        ? '剪贴板: ' + text
        : '剪贴板为空或读取失败';
}

function demoSendNotification() {
    LXC.sendNotification('WebUI 测试', '这是一条来自插件的测试通知');
    document.getElementById('ext-result').textContent = '通知已发送，请查看通知栏';
}

function demoGetSystemProp() {
    var props = ['os.version', 'os.name', 'java.vm.version'];
    var results = props.map(function (k) {
        return k + '=' + LXC.getSystemProperty(k);
    }).join(' | ');
    document.getElementById('ext-result').textContent = results;
}

function demoExecShell() {
    var output = LXC.execShell('echo "Hello from shell" && date');
    document.getElementById('ext-result').textContent = 'Shell 输出: ' + output;
}

LXC.log('app.js 初始化完成');