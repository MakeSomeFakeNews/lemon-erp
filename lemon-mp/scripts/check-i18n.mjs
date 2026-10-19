/**
 * 多语言检查：node scripts/check-i18n.mjs（npm run i18n:check）
 * 1. 语言包：zh-CN 与 en-US 的键是否一致
 * 2. 代码中 t('a.b') / $t('a.b') 使用的字面量键是否在两种语言中都存在
 * 3. 代码中残留的中文（去掉注释后；行内写 i18n-ignore 表示有意保留）
 * pages.json、manifest.json 中只能写一种语言（中文），页面运行时按语言设置标题和底部导航，不检查
 * 有问题时退出码为 1
 */
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join, relative, resolve } from 'node:path'
import { pathToFileURL } from 'node:url'

const ROOT = resolve(import.meta.dirname, '..')
const SRC = join(ROOT, 'src')
const LOCALES = join(SRC, 'locales')
const LANGS = ['zh-CN', 'en-US']
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
    // 语言包是 ESM（export default），以文本方式加载，避免受 package.json 的模块类型影响
    const src = readFileSync(join(LOCALES, lang, f), 'utf8')
    const mod = await import(`data:text/javascript;base64,${Buffer.from(src).toString('base64')}`)
    flatten(mod.default, f.slice(0, -3), keys[lang])
  }
}

const problems = []
for (const [a, b] of [['zh-CN', 'en-US'], ['en-US', 'zh-CN']]) {
  for (const k of keys[a].keys()) {
    if (!keys[b].has(k)) problems.push(`[${b} 缺少] ${k}`)
  }
}

const files = walk(SRC).filter((f) => !f.startsWith(LOCALES + '/'))
const KEY_RE = /(?<![\w.])(?:\$t|t|te)\(\s*['"]([\w-]+(?:\.[\w-]+)+)['"]/g
for (const file of files) {
  const rel = relative(ROOT, file)
  const text = readFileSync(file, 'utf8')
  for (const m of text.matchAll(KEY_RE)) {
    for (const lang of LANGS) {
      if (!keys[lang].has(m[1])) problems.push(`[${lang} 缺少] ${m[1]}（${rel}）`)
    }
  }
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
