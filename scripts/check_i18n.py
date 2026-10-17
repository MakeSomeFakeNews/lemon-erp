#!/usr/bin/env python3
"""
后端多语言检查：python3 scripts/check_i18n.py [模块目录…]（默认全部 lemon-* 模块）

译文文件：<模块>/src/main/resources/i18n/<名称>.en-US.json，内容为「中文模板 → 英文」（所有模块的文件合并使用）。
检查（只看 src/main/java，跳过注释和日志）：
  [缺少译文]   BizException / I18n.t 的中文模板、校验注解 message 没有英文译文
  [拼接]       BizException / I18n.t 的消息用 + 拼接，应改为 {0} 占位符加参数
  [其他中文]   其他含中文的字符串字面量（加 --all 时列出），需要判断是否返回给前端显示：
               是则在输出处用 I18n.t("…") 并登记译文；否则（日志、存入数据库的业务数据、SQL）可忽略，
               行内写 // i18n-ignore 表示有意保留
  [多余译文]   json 中有但代码中已不存在的模板（加 --unused 时列出，仅供参考）
"""
import glob, json, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CJK = re.compile(r'[一-鿿]')
LIT = r'"((?:[^"\\\n]|\\.)*)"'
LIT_RE = re.compile(LIT)
BIZ_RE = re.compile(r'new\s+BizException\s*\(\s*(?:[\w.]+\s*,\s*)?' + LIT + r'(\s*\+)?')
T_RE = re.compile(r'I18n\.t\s*\(\s*(?:[\w.()]+\s*,\s*)?' + LIT + r'(\s*\+)?')
MSG_RE = re.compile(r'\bmessage\s*=\s*' + LIT)


def unescape(s):
    return re.sub(r'\\(u[0-9a-fA-F]{4}|.)', lambda m: chr(int(m.group(1)[1:], 16)) if m.group(1)[0] == 'u' and len(m.group(1)) == 5
                  else {'n': '\n', 't': '\t', '"': '"', "'": "'", '\\': '\\'}.get(m.group(1), m.group(1)), s)


def strip_comments(text):
    out, i, n = [], 0, len(text)
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
            m = re.compile(LIT).match(text, i)
            if m:
                out.append(m.group(0))
                i = m.end()
            else:
                out.append(text[i])
                i += 1
        elif text[i] == "'":
            m = re.compile(r"'(?:[^'\\\n]|\\.)*'").match(text, i)
            j = m.end() if m else i + 1
            out.append(text[i:j])
            i = j
        else:
            out.append(text[i])
            i += 1
    return ''.join(out)


def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    show_all = '--all' in sys.argv
    show_unused = '--unused' in sys.argv
    trans = {}
    for f in glob.glob(os.path.join(ROOT, '**/src/main/resources/i18n/*.en-US.json'), recursive=True):
        if '/build/' in f:
            continue
        try:
            trans.update(json.load(open(f, encoding='utf-8')))
        except json.JSONDecodeError as e:
            print(f'[json 格式错误] {os.path.relpath(f, ROOT)}: {e}')
            sys.exit(1)
    dirs = [os.path.abspath(a) for a in args] or sorted(glob.glob(os.path.join(ROOT, 'lemon-*'))) + sorted(glob.glob(os.path.join(ROOT, 'lemon-modules/lemon-*')))
    problems, used = [], set()
    for d in dirs:
        for f in glob.glob(os.path.join(d, '**/src/main/java/**/*.java'), recursive=True):
            rel = os.path.relpath(f, ROOT)
            raw = open(f, encoding='utf-8').read()
            text = strip_comments(raw)
            raw_lines = raw.split('\n')
            covered = set()
            for regex in (BIZ_RE, T_RE, MSG_RE):
                for m in regex.finditer(text):
                    line = text.count('\n', 0, m.start()) + 1
                    lit = unescape(m.group(1))
                    covered.add((line, m.group(1)))
                    if regex is not MSG_RE and m.lastindex >= 2 and m.group(2):
                        problems.append(f'[拼接] {rel}:{line}: "{lit}" + …')
                        continue
                    if CJK.search(lit):
                        used.add(lit)
                        if lit not in trans:
                            problems.append(f'[缺少译文] {rel}:{line}: {lit}')
            if show_all:
                for i, line_text in enumerate(text.split('\n'), 1):
                    if re.search(r'\blog\.(trace|debug|info|warn|error)\s*\(', line_text) or 'i18n-ignore' in raw_lines[i - 1]:
                        continue
                    for m in LIT_RE.finditer(line_text):
                        if CJK.search(m.group(1)) and (i, m.group(1)) not in covered and unescape(m.group(1)) not in trans:
                            problems.append(f'[其他中文] {rel}:{i}: {unescape(m.group(1))[:100]}')
    if show_unused and not args:
        for k in sorted(set(trans) - used):
            problems.append(f'[多余译文] {k}')
    for p in problems:
        print(p)
    print(f'\n共 {len(problems)} 个问题' if problems else '后端多语言检查通过')
    sys.exit(1 if any(not p.startswith('[多余译文]') for p in problems) else 0)


if __name__ == '__main__':
    main()
