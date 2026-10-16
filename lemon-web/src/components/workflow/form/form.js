/** 流程表单字段（与后端 FormField 对应） */

export const FIELD_TYPES = [
  { label: '单行文本', value: 'INPUT' },
  { label: '多行文本', value: 'TEXTAREA' },
  { label: '数字', value: 'NUMBER' },
  { label: '日期', value: 'DATE' },
  { label: '下拉选择', value: 'SELECT' }
]

const KEY_PATTERN = /^[A-Za-z][A-Za-z0-9_]{0,31}$/
const RESERVED_KEYS = ['initiator', 'assignee', 'loopCounter', 'execution', 'task']

export const isReservedKey = (key) => RESERVED_KEYS.includes(key) || /^(nrOf|wf)/.test(key)

export function createField(fields) {
  let i = fields.length + 1
  while (fields.some((f) => f.field === `field${i}`)) i++
  return { field: `field${i}`, label: `字段${i}`, type: 'INPUT', required: false, placeholder: '', options: [] }
}

/** 返回错误提示列表 */
export function validateFields(fields = []) {
  const errors = []
  const keys = new Set()
  fields.forEach((f, i) => {
    const name = f.label?.trim() || `第 ${i + 1} 个字段`
    if (!f.label?.trim()) errors.push(`第 ${i + 1} 个字段未填写名称`)
    if (!KEY_PATTERN.test(f.field || '')) errors.push(`「${name}」的字段标识需以字母开头，只含字母、数字、下划线，最多 32 位`)
    else if (isReservedKey(f.field)) errors.push(`「${name}」的字段标识「${f.field}」为系统保留字`)
    else if (keys.has(f.field)) errors.push(`字段标识「${f.field}」重复`)
    keys.add(f.field)
    if (f.type === 'SELECT' && !f.options?.length) errors.push(`「${name}」至少需要一个选项`)
  })
  return errors
}

/** 提交前整理：非下拉字段不保留 options */
export const normalizeFields = (fields = []) =>
  fields.map((f) => ({ ...f, options: f.type === 'SELECT' ? f.options || [] : undefined }))
