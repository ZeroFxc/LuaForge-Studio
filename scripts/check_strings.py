import re
import sys
import xml.etree.ElementTree as ET

path = r'e:\Soft\Proje\LuaForge-Studio-main\app\src\main\res\values\strings.xml'
try:
    tree = ET.parse(path)
    root = tree.getroot()
    names = [s.attrib.get('name','') for s in root.findall('string')]
    plugin_names = [n for n in names if n.startswith('plugin_') or n == 'about_plugin_extensions']
    print('total strings in values/strings.xml:', len(names))
    print('plugin_* + about_plugin_extensions count:', len(plugin_names))
    print('first 5 plugin strings:')
    for n in plugin_names[:5]:
        print('  ', n)
    print('last 5 plugin strings:')
    for n in plugin_names[-5:]:
        print('  ', n)
except ET.ParseError as e:
    print('XML PARSE ERROR:', e)
    sys.exit(2)
