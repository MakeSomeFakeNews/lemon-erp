/**
 * 由后端 Java 枚举生成前端常量 src/constants/enums.js
 *
 *   node scripts/gen-enums.mjs          生成（npm run gen:enums）
 *   node scripts/gen-enums.mjs --check  只检查生成文件是否与后端一致，不一致时退出码为 1（npm run check 中执行）
 *
 * 扫描范围：lemon-common、lemon-framework、lemon-modules 下 src/main/java 中「文件名与枚举同名」的顶层枚举
 * （类内部的私有 / 嵌套枚举不导出）。每个枚举导出一个冻结对象，键和值都是常量名（与 JSON / 数据库中的取值一致）。
 * 用 @JsonValue 自定义 JSON 取值的枚举（如 EnableStatus 输出数字 1 / 0）不按名字生成，跳过；
 * 启用 / 停用在前端用 utils/constants.js 中的 STATUS_ENABLED / STATUS_DISABLED。
 * 命名：使用 Java 简单类名；多个模块有同名枚举时全部加模块前缀（如 WmsDocStatus、MesDocStatus），
 * lemon-common / lemon-framework 的前缀为 Common / Framework。输出按导出名排序，保证结果稳定。
 */
import { existsSync, readdirSync, readFileSync, statSync, writeFileSync } from 'node:fs'
import { join, relative, resolve, sep } from 'node:path'

const WEB = resolve(import.meta.dirname, '..')
const REPO = resolve(WEB, '..')
const OUT = join(WEB, 'src', 'constants', 'enums.js')
const ROOTS = ['lemon-common', 'lemon-framework', 'lemon-modules']

function walk(p, out = []) {
  if (!existsSync(p)) return out
  if (statSync(p).isDirectory()) {
    const name = p.split(sep).pop()
    if (name === 'build' || name === 'test' || name === 'node_modules' || name.startsWith('.')) return out
    for (const f of readdirSync(p).sort()) walk(join(p, f), out)
  } else if (p.endsWith('.java')) out.push(p)
  return out
}

/** 去掉注释，同时保留字符串 / 字符字面量 */
function stripComments(src) {
  let out = ''
  for (let i = 0; i < src.length; i++) {
    const c = src[i]
    if (c === '"' || c === "'") {
      let j = i + 1
      while (j < src.length && src[j] !== c) j += src[j] === '\\' ? 2 : 1
      out += src.slice(i, j + 1)
      i = j
    } else if (c === '/' && src[i + 1] === '/') {
      while (i < src.length && src[i] !== '\n') i++
      out += '\n'
    } else if (c === '/' && src[i + 1] === '*') {
      const end = src.indexOf('*/', i + 2)
      i = end < 0 ? src.length : end + 1
      out += ' '
    } else out += c
  }
  return out
}

/** 枚举体中常量部分：到第一个顶层 ; 或结束的 } 为止，按顶层逗号拆分，取每项开头的标识符 */
function parseConstants(code, start) {
  const names = []
  let depth = 0
  let cur = ''
  for (let i = start; i < code.length; i++) {
    const c = code[i]
    if (c === '"' || c === "'") {
      let j = i + 1
      while (j < code.length && code[j] !== c) j += code[j] === '\\' ? 2 : 1
      cur += ' '
      i = j
      continue
    }
    if (c === '(' || c === '{' || c === '[') depth++
    else if (c === ')' || c === ']') depth--
    else if (c === '}') {
      if (depth === 0) {
        names.push(cur)
        break
      }
      depth--
    } else if (depth === 0 && (c === ',' || c === ';')) {
      names.push(cur)
      cur = ''
      if (c === ';') break
      continue
    }
    if (depth === 0) cur += c
  }
  return names
    .map((s) => s.replace(/@\w+(\([^)]*\))?/g, '').trim().match(/^[A-Za-z_$][\w$]*/)?.[0])
    .filter(Boolean)
}

/** 常量前的 javadoc 第一句，作为行尾注释 */
function constantDocs(src) {
  const docs = {}
  const re = /\/\*\*((?:(?!\*\/)[\s\S])*)\*\/\s*(?:@\w+(?:\([^)]*\))?\s*)*([A-Z][A-Z0-9_]*)\s*[(,;{\n]/g
  let m
  while ((m = re.exec(src))) {
    const text = m[1].replace(/^\s*\*\s?/gm, '').replace(/\s+/g, ' ').trim()
    if (text && !text.startsWith('@')) docs[m[2]] = text.split(/[：:；;。，,（(]/)[0].trim()
  }
  return docs
}

function moduleOf(file) {
  const rel = relative(REPO, file).split(sep)
  if (rel[0] === 'lemon-modules') return rel[1].replace(/^lemon-/, '')
  return rel[0].replace(/^lemon-/, '')
}

function cap(s) {
  return s.charAt(0).toUpperCase() + s.slice(1)
}

export function collect() {
  const list = []
  for (const root of ROOTS) {
    for (const file of walk(join(REPO, root))) {
      if (!file.includes(`${sep}src${sep}main${sep}java${sep}`)) continue
      const simple = file.split(sep).pop().slice(0, -5)
      const raw = readFileSync(file, 'utf8')
      if (!new RegExp(`\\benum\\s+${simple}\\b`).test(raw)) continue
      const code = stripComments(raw)
      // JSON 取值不是常量名（@JsonValue），按名字生成会与接口数据不一致
      if (/@JsonValue\b/.test(code)) continue
      const m = new RegExp(`\\benum\\s+${simple}\\b[^{]*\\{`).exec(code)
      if (!m) continue
      const pkg = /package\s+([\w.]+)\s*;/.exec(code)?.[1]
      const head = raw.slice(0, raw.search(new RegExp(`\\benum\\s+${simple}\\b`)))
      const classDoc = [...head.matchAll(/\/\*\*([\s\S]*?)\*\//g)].pop()?.[1] ?? ''
      const dict = /字典\s*`?([a-z][a-z0-9]*_[a-z0-9_]+)/.exec(classDoc)?.[1]
      list.push({
        simple,
        module: moduleOf(file),
        fqcn: pkg ? `${pkg}.${simple}` : simple,
        dict,
        constants: parseConstants(code, m.index + m[0].length),
        docs: constantDocs(raw),
      })
    }
  }
  const count = {}
  for (const e of list) count[e.simple] = (count[e.simple] ?? 0) + 1
  for (const e of list) e.name = count[e.simple] > 1 ? cap(e.module) + e.simple : e.simple
  const names = new Set()
  for (const e of list) {
    if (names.has(e.name)) throw new Error(`枚举导出名重复：${e.name}`)
    names.add(e.name)
  }
  return list.filter((e) => e.constants.length).sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0))
}

export function render(list) {
  const lines = [
    '/**',
    ' * 后端枚举常量 —— 由 scripts/gen-enums.mjs 生成，请勿手工修改；后端新增或修改枚举后运行 npm run gen:enums',
    ' *',
    ' * 来源：lemon-common、lemon-framework、lemon-modules 中文件名与枚举同名的顶层 Java 枚举，键和值都是枚举常量名。',
    ' * 命名：Java 简单类名；多个模块有同名枚举时加模块前缀（如 WmsDocStatus、MesDocStatus）。',
    ' * 用法：import { WorkOrderStatus } from \'@/constants/enums\'；record.status === WorkOrderStatus.DRAFT',
    ' */',
    '',
  ]
  for (const e of list) {
    lines.push(`/** ${e.fqcn}${e.dict ? `（字典 ${e.dict}）` : ''} */`)
    lines.push(`export const ${e.name} = Object.freeze({`)
    for (const c of e.constants) {
      const doc = e.docs[c]
      lines.push(`  ${c}: '${c}',${doc ? ` // ${doc}` : ''}`)
    }
    lines.push('})')
    lines.push('')
  }
  return lines.join('\n')
}

const isMain = process.argv[1] && resolve(process.argv[1]) === resolve(import.meta.filename)
if (isMain) {
  const content = render(collect())
  if (process.argv.includes('--check')) {
    const current = existsSync(OUT) ? readFileSync(OUT, 'utf8') : ''
    if (current !== content) {
      console.error('src/constants/enums.js 与后端枚举不一致，请运行 npm run gen:enums')
      process.exit(1)
    }
    console.log('枚举常量与后端一致')
  } else {
    writeFileSync(OUT, content)
    console.log(`已生成 ${relative(WEB, OUT)}`)
  }
}
