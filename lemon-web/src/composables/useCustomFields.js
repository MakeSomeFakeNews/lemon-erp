import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { batchCustomValues, customFields, customValues, saveCustomValues } from '@/api/system/customField'

/** 业务类型启用的字段，同一页面共用一次请求 */
const cache = new Map()
function loadFields(bizType) {
  if (!cache.has(bizType)) cache.set(bizType, customFields(bizType).catch(() => []))
  return cache.get(bizType)
}
/** 自定义字段定义修改后清理缓存 */
export function clearCustomFieldCache() {
  cache.clear()
}

/** 下拉选项：逗号或换行分隔 */
export const optionsOf = (f) => (f.options || '').split(/[,，\n]/).map((s) => s.trim()).filter(Boolean)

/** 值的显示文本 */
export function displayValue(f, v) {
  if (v === undefined || v === null || v === '') return ''
  if (f.fieldType === 'BOOL') return v === 'true' ? '是' : '否'
  return v
}

/**
 * 表单中的自定义字段：fields 为启用的定义，values 为 { 字段标识: 值 }；load(bizId) 读取已有的值，save(bizId) 保存
 */
export function useCustomFields(bizType) {
  const fields = ref([])
  const values = ref({})
  loadFields(bizType).then((list) => (fields.value = list))

  async function load(bizId) {
    values.value = {}
    if (!bizId) return
    values.value = (await customValues(bizType, bizId).catch(() => ({}))) || {}
  }

  async function save(bizId) {
    if (!fields.value.length || !bizId) return
    await saveCustomValues(bizType, bizId, values.value)
  }

  /** 必填项没有填写时提示并中止保存（错误带 handled 标记，不再重复提示） */
  function checkRequired(vals) {
    const lack = missing(vals)
    if (!lack.length) return
    message.warning(`请填写${lack.join('、')}`)
    const err = new Error('custom field required')
    err.handled = true
    throw err
  }

  /** 必填但没有填写的字段名称，全部填写时为空 */
  const missing = (vals) => fields.value.filter((f) => f.required === 1 && (vals?.[f.fieldKey] === undefined || vals[f.fieldKey] === null || vals[f.fieldKey] === '')).map((f) => f.label)

  /**
   * 包装 useFormDrawer 的选项：表单中带 cf（{ 字段标识: 值 }，计入未保存修改），打开时读取、保存业务记录后保存值。
   * create 需返回新记录的 ID
   */
  function wrap(opts) {
    const READONLY = ['id', 'createTime', 'updateTime', 'children', 'cf']
    const plain = (f) => Object.fromEntries(Object.entries(f).filter(([k]) => !READONLY.includes(k)))
    return {
      ...opts,
      emptyForm: () => ({ ...opts.emptyForm(), cf: {} }),
      fetch: async (record) => {
        const [detail, vals] = await Promise.all([opts.fetch ? opts.fetch(record) : record, customValues(bizType, record.id).catch(() => ({}))])
        return { ...detail, cf: vals || {} }
      },
      toPayload: (f) => ({ payload: opts.toPayload ? opts.toPayload(f) : plain(f), cf: f.cf || {} }),
      create: async ({ payload, cf }) => {
        checkRequired(cf)
        const id = await opts.create(payload)
        if (fields.value.length && id) await saveCustomValues(bizType, id, cf)
        return id
      },
      update: async (id, { payload, cf }) => {
        checkRequired(cf)
        await opts.update(id, payload)
        if (fields.value.length) await saveCustomValues(bizType, id, cf)
      }
    }
  }

  return { fields, values, load, save, missing, wrap }
}

/**
 * 列表中的自定义字段列：showInList 的字段作为列（key 为 cf:字段标识），列表变化时批量读取当前页的值
 *
 * @param bizType
 * @param list 当前页的记录（ref）
 */
export function useCustomColumns(bizType, list) {
  const fields = ref([])
  const values = ref({})
  loadFields(bizType).then((all) => (fields.value = all.filter((f) => f.showInList === 1)))
  const columns = computed(() => fields.value.map((f) => ({ title: f.label, key: `cf:${f.fieldKey}`, width: 120, ellipsis: true, custom: f })))
  watch(
    () => [fields.value.length, (list.value || []).map((r) => r.id).join(',')],
    async () => {
      const ids = (list.value || []).map((r) => r.id).filter(Boolean)
      if (!fields.value.length || !ids.length) return
      values.value = (await batchCustomValues(bizType, ids).catch(() => ({}))) || {}
    }
  )
  const cell = (record, column) => displayValue(column.custom, values.value[record.id]?.[column.custom.fieldKey])
  return { columns, cell }
}
