<template>
  <a-modal
    :open="open"
    :title="$t('components.import.title', { name: title })"
    width="min(640px, 100vw)"
    :confirm-loading="importing"
    :ok-text="$t('common.action.import')"
    :ok-button-props="{ disabled: !rows.length || !!errors.length || !!mapper }"
    @ok="doImport"
    @cancel="close"
  >
    <a-space direction="vertical" style="width: 100%">
      <a-alert type="info" show-icon>
        <template #message>
          {{ $t('components.import.tipBefore') }}
          <a @click="downloadTemplate(title, columns, sample)">{{ $t('components.import.downloadTemplate') }}</a>
          {{ $t('components.import.tipAfter') }}
        </template>
      </a-alert>
      <!-- 表头与模板不一致：AI 对应字段（数据整理功能开启时） -->
      <AiImportMapper
        v-if="mapper"
        :columns="columns"
        :headers="mapper.headers"
        :samples="mapper.samples"
        @apply="applyMapping"
        @cancel="mapper = null"
      />
      <a-alert v-else-if="mismatch && aiEnabled" type="warning" show-icon>
        <template #message>
          {{ $t('ai.importMap.mismatch') }}
          <a-button type="link" size="small" class="ai-map-btn" @click="openMapper"><RobotOutlined />{{ $t('ai.importMap.button') }}</a-button>
        </template>
      </a-alert>
      <a-upload-dragger v-show="!mapper" :file-list="[]" accept=".xlsx" :before-upload="onFile" :disabled="parsing || importing">
        <p class="ant-upload-drag-icon"><InboxOutlined /></p>
        <p class="ant-upload-text">{{ fileName || $t('components.import.dropHint') }}</p>
        <p v-if="rows.length || errors.length" class="ant-upload-hint">
          {{ errors.length ? $t('components.import.rowsWithErrors', { n: rows.length, e: errors.length }) : $t('components.import.rows', { n: rows.length }) }}
        </p>
      </a-upload-dragger>
      <a-alert v-if="result && !result.errors.length" type="success" show-icon :message="$t('components.import.success', { n: result.imported })" />
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
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { InboxOutlined, RobotOutlined } from '@ant-design/icons-vue'
import { useDictStore } from '@/store/dict'
import { downloadTemplate, readExcel, readExcelHeaders } from '@/utils/excel'
import { getAiStatus } from '@/api/ai/config'
import { useUserStore } from '@/store/user'
import AiImportMapper from '@/components/ai/AiImportMapper.vue'

/**
 * Excel 导入弹窗。columns 每列：
 * - title 表头、key 字段、required 必填
 * - type：'number' | 'int' | 'date' | 'bool'（是/否）
 * - dict：字典类型，按标签或值转换为值
 * - lookup：async () => Map(名称或编码 → ID)，用于部门、职位等
 * importer：({ rows: [{ rowNum, data }] }) => Promise<{ total, imported, errors }>
 * 上传的表头与模板不一致且「数据整理」功能开启时，提供「AI 对应字段」：AI 给出 Excel 列 → 模板字段，用户核对后按此读取
 */
const props = defineProps({
  open: Boolean,
  title: { type: String, required: true },
  columns: { type: Array, required: true },
  importer: { type: Function, required: true },
  sample: Object
})
const emit = defineEmits(['update:open', 'success'])
const { t } = useI18n()

const errorColumns = [
  { title: t('components.import.rowNum'), dataIndex: 'rowNum', width: 80 },
  { title: t('components.import.error'), dataIndex: 'message' }
]
const fileName = ref('')
const rows = ref([])
const errors = ref([])
const result = ref(null)
const parsing = ref(false)
const importing = ref(false)
/** 当前文件（按 AI 对应关系重新读取） */
let currentFile = null
/** 表头与模板不一致：{ headers, samples } */
const mismatch = ref(null)
const mapper = ref(null)
const aiEnabled = ref(false)

function reset() {
  fileName.value = ''
  rows.value = []
  errors.value = []
  result.value = null
  mismatch.value = null
  mapper.value = null
  currentFile = null
}

// 只在弹窗打开时查询一次 AI 是否可用（登录即可调用，不含配置内容）
let aiChecked = false
watch(
  () => props.open,
  (v) => {
    // 「AI 对应字段」需要数据整理的查看权限，并且数据整理功能已开启（用量记在数据整理下）
    if (v && !aiChecked && useUserStore().hasPerm('ai:cleanse:list')) {
      aiChecked = true
      getAiStatus()
        .then((s) => (aiEnabled.value = !!s.features?.CLEANSE))
        .catch(() => {})
    }
  },
  { immediate: true }
)

/** 有模板中没有的表头，且有模板字段在表头中找不到 */
async function detectMismatch(file) {
  try {
    const { headers, samples } = await readExcelHeaders(file)
    const titles = new Set(props.columns.map((c) => c.title))
    const unknown = headers.some((h) => !titles.has(h))
    const absent = props.columns.some((c) => !headers.includes(c.title))
    mismatch.value = unknown && absent ? { headers, samples } : null
  } catch {
    mismatch.value = null
  }
}

function openMapper() {
  mapper.value = mismatch.value
}

/** 按对应关系（字段 → 表头）重新读取 */
async function applyMapping(headerMap) {
  mapper.value = null
  parsing.value = true
  try {
    const list = await readExcel(currentFile, props.columns, headerMap)
    if (!list.length) {
      message.warning(t('components.import.noData'))
      return
    }
    const { out, errs } = await convert(list)
    rows.value = out
    errors.value = errs
    mismatch.value = null
    message.success(t('ai.importMap.applied'))
  } catch (e) {
    message.error(e.message || t('components.import.parseFailed'))
  } finally {
    parsing.value = false
  }
}

function close() {
  reset()
  emit('update:open', false)
}

// 中英文的「是 / 否」都可以，英文不区分大小写
const BOOL = { 是: 1, 否: 0, y: 1, n: 0, '1': 1, '0': 0, 启用: 1, 停用: 0, yes: 1, no: 0, true: 1, false: 0, enabled: 1, disabled: 0 } // i18n-ignore
function boolOf(v) {
  const k = String(v).trim().toLowerCase()
  return Object.hasOwn(BOOL, k) ? BOOL[k] : undefined
}

function normalizeDate(s) {
  const m = /^(\d{4})[-/.年](\d{1,2})[-/.月](\d{1,2})日?$/.exec(s) // i18n-ignore 兼容「2026年1月2日」
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
        if (c.required) rowErrs.push(t('components.import.required', { name: c.title }))
        continue
      }
      if (c.dict) {
        const d = dicts[c.dict].find((x) => x.label === v || x.value === v)
        if (d) data[c.key] = d.value
        else rowErrs.push(t('components.import.notFound', { name: c.title, value: v }))
      } else if (c.lookup) {
        const id = lookups[c.key].get(v)
        if (id !== undefined) data[c.key] = id
        else rowErrs.push(t('components.import.notFound', { name: c.title, value: v }))
      } else if (c.type === 'number' || c.type === 'int') {
        const n = Number(v.replace(/,/g, ''))
        if (Number.isNaN(n) || (c.type === 'int' && !Number.isInteger(n))) rowErrs.push(t('components.import.invalidNumber', { name: c.title, value: v }))
        else data[c.key] = n
      } else if (c.type === 'date') {
        const d = normalizeDate(v)
        if (d) data[c.key] = d
        else rowErrs.push(t('components.import.invalidDate', { name: c.title, value: v }))
      } else if (c.type === 'bool') {
        if (boolOf(v) !== undefined) data[c.key] = boolOf(v)
        else rowErrs.push(t('components.import.invalidBool', { name: c.title }))
      } else {
        data[c.key] = v
      }
    }
    if (rowErrs.length) errs.push({ key: r.rowNum, rowNum: r.rowNum, message: rowErrs.join(t('components.import.sep')) })
    return { rowNum: r.rowNum, data }
  })
  return { out, errs }
}

async function onFile(file) {
  reset()
  fileName.value = file.name
  currentFile = file
  parsing.value = true
  await detectMismatch(file)
  try {
    const list = await readExcel(file, props.columns)
    if (!list.length) {
      message.warning(t('components.import.noData'))
      return false
    }
    const { out, errs } = await convert(list)
    rows.value = out
    errors.value = errs
  } catch (e) {
    // 表头与模板不一致且可以用 AI 对应字段时，由上方的提示引导，不再弹出缺少列的错误
    if (!(mismatch.value && aiEnabled.value)) message.error(e.message || t('components.import.parseFailed'))
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
      message.success(t('components.import.success', { n: res.imported }))
      rows.value = []
      emit('success', res)
    }
  } finally {
    importing.value = false
  }
}
</script>

<style scoped>
.ai-map-btn {
  padding: 0 4px;
}
</style>
