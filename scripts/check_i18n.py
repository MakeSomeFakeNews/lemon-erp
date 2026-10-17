#!/usr/bin/env python3
"""
后端国际化检查：python3 scripts/check_i18n.py [模块目录…]（默认全部 lemon-* 模块）

资源包：各模块 src/main/resources/i18n/<模块>.properties（默认，简体中文）和 <模块>_en.properties（英文），
键为 <模块>.<分类>.<名称>（error 业务异常、valid 校验、text 返回前端的文字、label 名称、enum 枚举）。
检查：
  [资源包]   中英文的键不一致、占位符不一致、英文中有中文、单引号未转义（MessageFormat 中单引号要写成两个）
  [缺少键]   代码中 new BizException("键")、Messages.get("键")、校验注解 message = "{键}" 使用的键不存在
  [中文]     Java 代码中残留的中文字符串（注释、日志除外）：返回前端的文字改为消息键；
             存入数据库的内容（站内消息、操作日志标题、单据摘要等）有意保留中文的，在行内写 // i18n-ignore
  [过渡]     仍在使用 Messages.legacyText（迁移用的过渡方法，应改为消息键）
"""
import glob, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CJK = re.compile(r'[一-鿿]')
LIT = r'"((?:[^"\\\n]|\\.)*)"'
KEY_USES = [re.compile(r'new\s+BizException\s*\(\s*(?:[\w.]+\s*,\s*)?' + LIT), re.compile(r'Messages\.get\s*\(\s*(?:[\w.()]+\s*,\s*)?' + LIT),
            re.compile(r'[(,]\s*message\s*=\s*"\{([\w.-]+)\}"')]
KEY_FORMAT = re.compile(r'^[a-z]+\.[a-z]+\.[A-Za-z0-9._]+$')


def load_props(path):
    props, cont, key = {}, '', None
    for raw in open(path, encoding='utf-8').read().split('\n'):
        line = cont + raw.lstrip() if cont else raw.strip()
        cont = ''
        if not line or line.startswith('#') or line.startswith('!'):
            continue
        if line.endswith('\\') and not line.endswith('\\\\'):
            cont = line[:-1]
            continue
        k, _, v = line.partition('=')
        props[k.strip()] = v.encode('raw_unicode_escape').decode('unicode_escape') if '\\u' in v else v.replace('\\n', '\n').replace('\\\\', '\\')
    return props


def strip_comments(text):
    out, i, n = [], 0, len(text)
    lit = re.compile(LIT)
    while i < n:
        if text.startswith('/*', i):
            j = text.find('*/', i + 2)
            j = n if j < 0 else j + 2
            out.append(re.sub(r'[^\n]', ' ', text[i:j]))
            i = j
        elif text.startswith('//', i):
            j = text.find('\n', i)
            j = n if j < 0 else j
            out.append(' ' * (j - i))
            i = j
        elif text[i] == '"':
            m = lit.match(text, i)
            j = m.end() if m else i + 1
            out.append(text[i:j])
            i = j
        elif text[i] == "'":
            m = re.compile(r"'(?:[^'\\\n]|\\.)*'").match(text, i)
            j = m.end() if m else i + 1
            out.append(text[i:j])
            i = j
        else:
            out.append(text[i])
            i += 1
    return ''.join(out)


def placeholders(s):
    return sorted(set(re.findall(r'\{\d+\}', s)))


def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    problems = []
    base, en = {}, {}
    for f in sorted(glob.glob(os.path.join(ROOT, '**/src/main/resources/i18n/*.properties'), recursive=True)):
        if '/build/' in f:
            continue
        name = os.path.basename(f)[:-len('.properties')]
        target = en if name.endswith('_en') else base if '_' not in name else None
        if target is None:
            continue
        props = load_props(f)
        rel = os.path.relpath(f, ROOT)
        for k, v in props.items():
            if not KEY_FORMAT.match(k):
                problems.append(f'[资源包] {rel}: 键格式应为 <模块>.<分类>.<名称>：{k}')
            if k in target and target[k][1] != v:
                problems.append(f'[资源包] 键重复且内容不同：{k}（{rel}，{target[k][0]}）')
            if re.search(r"(?<!')'(?!')", v.replace("'{'", '').replace("'}'", '')):
                problems.append(f'[资源包] {rel}: 单引号要写成两个：{k}={v}')
            target[k] = (rel, v)
    for k in sorted(set(base) | set(en)):
        if k not in en:
            problems.append(f'[资源包] 缺少英文：{k}（{base[k][0]}）')
        elif k not in base:
            problems.append(f'[资源包] 缺少中文：{k}（{en[k][0]}）')
        else:
            if placeholders(base[k][1]) != placeholders(en[k][1]):
                problems.append(f'[资源包] 占位符不一致：{k}：{base[k][1]} → {en[k][1]}')
            if CJK.search(en[k][1]):
                problems.append(f'[资源包] 英文中有中文：{k}={en[k][1]}')

    dirs = [os.path.abspath(a) for a in args] or sorted(glob.glob(os.path.join(ROOT, 'lemon-*'))) + sorted(glob.glob(os.path.join(ROOT, 'lemon-modules/lemon-*')))
    for d in dirs:
        for f in sorted(glob.glob(os.path.join(d, '**/src/main/java/**/*.java'), recursive=True)):
            rel = os.path.relpath(f, ROOT)
            raw = open(f, encoding='utf-8').read()
            text = strip_comments(raw)
            raw_lines = raw.split('\n')
            for regex in KEY_USES:
                for m in regex.finditer(text):
                    key = m.group(1)
                    if KEY_FORMAT.match(key) and key not in base:
                        problems.append(f'[缺少键] {rel}:{text.count(chr(10), 0, m.start()) + 1}: {key}')
            for i, line in enumerate(text.split('\n'), 1):
                if 'i18n-ignore' in raw_lines[i - 1]:
                    continue
                if 'Messages.legacyText(' in line:
                    problems.append(f'[过渡] {rel}:{i}: {raw_lines[i - 1].strip()[:110]}')
                if re.search(r'\blog\.(trace|debug|info|warn|error)\s*\(', line):
                    continue
                for m in re.finditer(LIT, line):
                    if CJK.search(m.group(1)):
                        problems.append(f'[中文] {rel}:{i}: {m.group(1)[:100]}')
    for p in problems:
        print(p)
    print(f'\n共 {len(problems)} 个问题' if problems else '后端国际化检查通过')
    sys.exit(1 if problems else 0)


if __name__ == '__main__':
    main()
