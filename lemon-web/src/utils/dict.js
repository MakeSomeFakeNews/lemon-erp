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

/** 字典的 options，当前值不在其中（取值已停用或删除）时追加一项，避免下拉框显示为键值或被清空 */
export function optionsWithCurrent(list = [], current) {
  const opts = toOptions(list)
  if (current === null || current === undefined || current === '' || opts.some((o) => o.value === current)) return opts
  return [...opts, { label: String(current), value: current }]
}
