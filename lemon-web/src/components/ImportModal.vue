<template>
  <a-modal
    :open="open"
    :title="`导入${title}`"
    width="min(640px, 100vw)"
    :confirm-loading="importing"
    ok-text="导入"
    :ok-button-props="{ disabled: !rows.length || !!errors.length }"
    @ok="doImport"
    @cancel="close"
  >
    <a-space direction="vertical" style="width: 100%">
      <a-alert type="info" show-icon>
        <template #message>
          先
          <a @click="downloadTemplate(title, columns, sample)">下载导入模板</a>
          ，按模板填写后上传；带 * 的列必填，任何一行有错误时整批不导入。
        </template>
      </a-alert>
      <a-upload-dragger :file-list="[]" accept=".xlsx" :before-upload="onFile" :disabled="parsing || importing">
        <p class="ant-upload-drag-icon"><InboxOutlined /></p>
        <p class="ant-upload-text">{{ fileName || '点击或拖拽 .xlsx 文件到这里' }}</p>
        <p v-if="rows.length || errors.length" class="ant-upload-hint">
          共 {{ rows.length }} 行<span v-if="errors.length">，{{ errors.length }} 行有错误</span>
        </p>
      </a-upload-dragger>
      <a-alert v-if="result && !result.errors.length" type="success" show-icon :message="`成功导入 ${result.imported} 行`" />
      <a-table
        v-if="errors.length"
        size="small"
        row-key="key"
        :columns="errorColumns"
        :data-source="errors"
        :pagination="errors.length > 8 ? { pageSize: 8, size: 'small' } : false"
      />
    </a-space>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { InboxOutlined } from '@ant-design/icons-vue'
import { useDictStore } from '@/store/dict'
import { downloadTemplate, readExcel } from '@/utils/excel'

/**
 * Excel 导入弹窗。columns 每列：
 * - title 表头、key 字段、required 必填
 * - type：'number' | 'int' | 'date' | 'bool'（是/否）
 * - dict：字典类型，按标签或值转换为值
 * - lookup：async () => Map(名称或编码 → ID)，用于部门、职位等
 * importer：({ rows: [{ rowNum, data }] }) => Promise<{ total, imported, errors }>
 */
const props = defineProps({
  open: Boolean,
  title: { type: String, required: true },
  columns: { type: Array, required: true },
  importer: { type: Function, required: true },
  sample: Object
})
const emit = defineEmits(['update:open', 'success'])

const errorColumns = [
  { title: '行号', dataIndex: 'rowNum', width: 80 },
  { title: '错误', dataIndex: 'message' }
]
const fileName = ref('')
const rows = ref([])
const errors = ref([])
const result = ref(null)
const parsing = ref(false)
const importing = ref(false)

function reset() {
  fileName.value = ''
  rows.value = []
  errors.value = []
  result.value = null
}

function close() {
  reset()
  emit('update:open', false)
}

const BOOL = { 是: 1, 否: 0, Y: 1, N: 0, '1': 1, '0': 0, 启用: 1, 停用: 0 }

function normalizeDate(s) {
  const m = /^(\d{4})[-/.年](\d{1,2})[-/.月](\d{1,2})日?$/.exec(s)
  if (!m) return null
  return `${m[1]}-${m[2].padStart(2, '0')}-${m[3].padStart(2, '0')}`
}

async function convert(list) {
  const dictStore = useDictStore()
  const dicts = {}
  const lookups = {}
  for (const c of props.columns) {
    if (c.dict) dicts[c.dict] = await dictStore.load(c.dict)
    if (c.lookup) lookups[c.key] = await c.lookup()
  }
  const errs = []
  const out = list.map((r) => {
    const data = {}
    const rowErrs = []
    for (const c of props.columns) {
      const v = r[c.key]
      if (v === undefined) {
        if (c.required) rowErrs.push(`${c.title}不能为空`)
        continue
      }
      if (c.dict) {
        const d = dicts[c.dict].find((x) => x.label === v || x.value === v)
        if (d) data[c.key] = d.value
        else rowErrs.push(`${c.title}「${v}」不存在`)
      } else if (c.lookup) {
        const id = lookups[c.key].get(v)
        if (id !== undefined) data[c.key] = id
        else rowErrs.push(`${c.title}「${v}」不存在`)
      } else if (c.type === 'number' || c.type === 'int') {
        const n = Number(v.replace(/,/g, ''))
        if (Number.isNaN(n) || (c.type === 'int' && !Number.isInteger(n))) rowErrs.push(`${c.title}「${v}」不是有效的数字`)
        else data[c.key] = n
      } else if (c.type === 'date') {
        const d = normalizeDate(v)
        if (d) data[c.key] = d
        else rowErrs.push(`${c.title}「${v}」不是有效的日期`)
      } else if (c.type === 'bool') {
        if (v in BOOL) data[c.key] = BOOL[v]
        else rowErrs.push(`${c.title}请填写 是 或 否`)
      } else {
        data[c.key] = v
      }
    }
    if (rowErrs.length) errs.push({ key: r.rowNum, rowNum: r.rowNum, message: rowErrs.join('；') })
    return { rowNum: r.rowNum, data }
  })
  return { out, errs }
}

async function onFile(file) {
  reset()
  fileName.value = file.name
  parsing.value = true
  try {
    const list = await readExcel(file, props.columns)
    if (!list.length) {
      message.warning('文件中没有数据')
      return false
    }
    const { out, errs } = await convert(list)
    rows.value = out
    errors.value = errs
  } catch (e) {
    message.error(e.message || '文件解析失败，请使用导入模板')
  } finally {
    parsing.value = false
  }
  return false
}

async function doImport() {
  importing.value = true
  try {
    const res = await props.importer({ rows: rows.value })
    result.value = res
    errors.value = res.errors.map((e, i) => ({ key: i, ...e }))
    if (!res.errors.length) {
      message.success(`成功导入 ${res.imported} 行`)
      rows.value = []
      emit('success', res)
    }
  } finally {
    importing.value = false
  }
}
</script>
