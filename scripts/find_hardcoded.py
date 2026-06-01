import re
import os

base = r'e:\Soft\Proje\LuaForge-Studio-main\app\src\main\kotlin\com\luaforge\studio\lxclua'

target_files = [
    'ui/about/AboutScreen.kt',
    'ui/plugin/PluginScreen.kt',
    'ui/plugin/PluginLogScreen.kt',
    'MainActivity.kt',
    'files/FileTree.kt',
]

for f in target_files:
    path = os.path.join(base, f)
    if not os.path.exists(path):
        print(f'NOT FOUND: {f}')
        continue
    lines = open(path, encoding='utf-8').readlines()
    for i, line in enumerate(lines, 1):
        # 找包含中文的硬编码字符串（排除 import/注释）
        has_cn = any('\u4e00' <= c <= '\u9fff' for c in line)
        if has_cn:
            # 跳过注释行
            stripped = line.strip()
            if stripped.startswith('//') or stripped.startswith('*') or stripped.startswith('/*'):
                continue
            # 跳过 stringResource / R.string / context.getString 调用
            if 'R.string.' in line or 'stringResource' in line or 'context.getString' in line:
                continue
            # 跳过 data class / 变量声明（非 UI 字符串）
            if stripped.startswith('data class') or stripped.startswith('@Suppress'):
                continue
            print(f'{f}:{i}: {stripped[:120]}')