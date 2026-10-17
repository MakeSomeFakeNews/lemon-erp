/**
 * 多语言检查：node scripts/check-i18n.mjs [目录或文件…]（默认 src）
 * 1. 语言包：zh-CN 与 en-US 的键是否一致（menu、dict 只有译文，不检查）
 * 2. 代码中 t('a.b') / $t('a.b') / tt('a.b') 使用的键是否在 zh-CN、en-US 中都存在（只检查字面量键）
 * 3. 代码中残留的中文（去掉注释和 console 输出后；行内写 i18n-ignore 表示有意保留，如中文大写金额）
 * 有问题时退出码为 1
 */
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join, relative, resolve } from 'node:path'
import { pathToFileURL } from 'node:url'

const ROOT = resolve(import.meta.dirname, '..')
const SRC = join(ROOT, 'src')
const LOCALES = join(SRC, 'locales')
const LANGS = ['zh-CN', 'en-US']
const DATA_FILES = new Set(['menu', 'dict'])
const CJK = /[一-鿿]/

function walk(p, out = []) {
  if (statSync(p).isDirectory()) {
    for (const f of readdirSync(p)) walk(join(p, f), out)
  } else if (/\.(vue|js)$/.test(p)) out.push(p)
  return out
}

function flatten(obj, prefix, out) {
  for (const [k, v] of Object.entries(obj)) {
    const key = prefix ? `${prefix}.${k}` : k
    if (v && typeof v === 'object') flatten(v, key, out)
    else out.set(key, v)
  }
  return out
}

const keys = {}
for (const lang of LANGS) {
  keys[lang] = new Map()
  for (const f of readdirSync(join(LOCALES, lang))) {
    if (!f.endsWith('.js')) continue
    const name = f.slice(0, -3)
    const mod = await import(pathToFileURL(join(LOCALES, lang, f)).href)
    flatten(mod.default, name, keys[lang])
  }
}

const problems = []
for (const [a, b] of [['zh-CN', 'en-US'], ['en-US', 'zh-CN']]) {
  for (const k of keys[a].keys()) {
    if (!DATA_FILES.has(k.split('.')[0]) && !keys[b].has(k)) problems.push(`[${b} 缺少] ${k}`)
  }
}
for (const lang of LANGS) {
  for (const [k, v] of keys[lang]) {
    if (typeof v === 'string' && /(^|[^{'])[@|$]/.test(v.replace(/\{'[@|${}]'\}/g, ''))) {
      problems.push(`[${lang} 特殊字符] ${k}: ${v}（@ | $ 要写成 {'@'}）`)
    }
  }
}

const targets = process.argv.slice(2).length ? process.argv.slice(2).map((p) => resolve(p)) : [SRC]
const files = targets.flatMap((p) => walk(p)).filter((f) => !f.startsWith(LOCALES + '/'))
const KEY_RE = /(?<![\w.])(?:\$t|t|tt|te)\(\s*['"]([\w-]+(?:\.[\w-]+)+)['"]/g
for (const file of files) {
  const rel = relative(ROOT, file)
  const text = readFileSync(file, 'utf8')
  for (const m of text.matchAll(KEY_RE)) {
    const key = m[1]
    const ns = key.split('.')[0]
    for (const lang of LANGS) {
      if (DATA_FILES.has(ns)) continue
      if (!keys[lang].has(key)) problems.push(`[${lang} 缺少] ${key}（${rel}）`)
    }
  }
  // 去掉注释后检查残留中文
  const stripped = text
    .replace(/<!--[\s\S]*?-->/g, (s) => s.replace(/[^\n]/g, ' '))
    .replace(/\/\*[\s\S]*?\*\//g, (s) => s.replace(/[^\n]/g, ' '))
  stripped.split('\n').forEach((line, i) => {
    const code = line.replace(/(^|[^:'"`\\])\/\/.*$/, '$1')
    if (CJK.test(code) && !/console\.\w+\(/.test(code) && !text.split('\n')[i].includes('i18n-ignore')) {
      problems.push(`[中文] ${rel}:${i + 1}: ${code.trim().slice(0, 120)}`)
    }
  })
}

const uniq = [...new Set(problems)]
uniq.forEach((p) => console.log(p))
console.log(uniq.length ? `\n共 ${uniq.length} 个问题` : '多语言检查通过')
process.exit(uniq.length ? 1 : 0)
