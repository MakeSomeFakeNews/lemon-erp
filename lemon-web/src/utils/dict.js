/**
 * 字典数据转为 select / radio 的 options。
 * 字典 value 为字符串，状态类字段为数字时传 numeric = true
 */
export function toOptions(list = [], numeric = false) {
  return list.map((d) => ({ label: d.label, value: numeric ? Number(d.value) : d.value }))
}

export function findDict(list = [], value) {
  if (value === null || value === undefined) return undefined
  return list.find((d) => d.value === String(value))
}
