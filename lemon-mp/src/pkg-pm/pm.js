/** 项目工时分包的日期工具（yyyy-MM-dd） */

const pad = (n) => String(n).padStart(2, '0')

export function fmt(d) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export function parse(s) {
  const [y, m, d] = s.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function addDays(s, n) {
  const d = parse(s)
  d.setDate(d.getDate() + n)
  return fmt(d)
}

/** 所在周的周一 */
export function monday(s) {
  const d = parse(s)
  return addDays(s, -((d.getDay() + 6) % 7))
}

/** 小时：去掉多余的 0 */
export const hours = (v) => String(Math.round(Number(v || 0) * 10) / 10)

/** 任务树的末级任务（只有末级任务可以填报） */
export function leafTasks(tree, out = []) {
  for (const t of tree || []) {
    if (t.children?.length) leafTasks(t.children, out)
    else out.push(t)
  }
  return out
}
