import { t } from '@/locales'

import { FormFieldType } from '@/constants/enums'
/** 流程表单字段（与后端 FormField 对应） */

export const FIELD_TYPES = [
  { label: t('workflow.fieldType.input'), value: 'INPUT' },
  { label: t('workflow.fieldType.textarea'), value: 'TEXTAREA' },
  { label: t('workflow.fieldType.number'), value: 'NUMBER' },
  { label: t('workflow.fieldType.date'), value: 'DATE' },
  { label: t('workflow.fieldType.select'), value: 'SELECT' }
]

const KEY_PATTERN = /^[A-Za-z][A-Za-z0-9_]{0,31}$/
const RESERVED_KEYS = ['initiator', 'assignee', 'loopCounter', 'execution', 'task']

export const isReservedKey = (key) => RESERVED_KEYS.includes(key) || /^(nrOf|wf)/.test(key)

export function createField(fields) {
  let i = fields.length + 1
  while (fields.some((f) => f.field === `field${i}`)) i++
  return { field: `field${i}`, label: t('workflow.form.fieldN', { n: i }), type: FormFieldType.INPUT, required: false, placeholder: '', options: [] }
}

/** 返回错误提示列表 */
export function validateFields(fields = []) {
  const errors = []
  const keys = new Set()
  fields.forEach((f, i) => {
    const name = f.label?.trim() || t('workflow.form.nthField', { n: i + 1 })
    if (!f.label?.trim()) errors.push(t('workflow.form.nthFieldNoName', { n: i + 1 }))
    if (!KEY_PATTERN.test(f.field || '')) errors.push(t('workflow.form.keyInvalid', { name }))
    else if (isReservedKey(f.field)) errors.push(t('workflow.form.keyReserved', { name, key: f.field }))
    else if (keys.has(f.field)) errors.push(t('workflow.form.keyDuplicate', { key: f.field }))
    keys.add(f.field)
    if (f.type === FormFieldType.SELECT && !f.options?.length) errors.push(t('workflow.form.needOption', { name }))
  })
  return errors
}

/** 提交前整理：非下拉字段不保留 options */
export const normalizeFields = (fields = []) =>
  fields.map((f) => ({ ...f, options: f.type === FormFieldType.SELECT ? f.options || [] : undefined }))
