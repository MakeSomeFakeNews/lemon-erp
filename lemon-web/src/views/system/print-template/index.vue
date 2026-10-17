<template>
  <div class="workspace">
    <!-- 左侧：单据 -->
    <a-card :bordered="false" class="page-card workspace-side">
      <div class="workspace-side-head">{{ $t('print.template.title') }}</div>
      <div class="type-list scroll-hover">
        <div :class="['type-item', { active: current === COMMON_KEY }]" @click="select(COMMON_KEY)">
          <SettingOutlined />
          <span class="type-name">{{ $t('print.template.common') }}</span>
        </div>
        <template v-for="g in groups" :key="g.module">
          <div class="type-group">{{ printLabel(g.module) }}</div>
          <div
            v-for="t in g.types"
            :key="t.key"
            :class="['type-item', { active: current === t.key }]"
            @click="select(t.key)"
          >
            <FileTextOutlined />
            <span class="type-name">{{ printLabel(t.name) }}</span>
            <a-tag v-if="templates[t.key]" color="blue" :bordered="false" class="type-tag">{{ $t('system.config.modified') }}</a-tag>
          </div>
        </template>
        <div class="type-group">{{ $t('print.template.labels') }}</div>
        <div v-for="t in LABEL_TYPES" :key="t.key" :class="['type-item', { active: current === t.key }]" @click="select(t.key)">
          <BarcodeOutlined />
          <span class="type-name">{{ t.name }}</span>
          <a-tag v-if="templates[t.key]" color="blue" :bordered="false" class="type-tag">{{ $t('system.config.modified') }}</a-tag>
        </div>
      </div>
    </a-card>

    <div class="workspace-main">
      <a-card :bordered="false" class="page-card editor-card">
        <!-- 通用设置 -->
        <template v-if="current === COMMON_KEY">
          <div class="editor-head">
            <span class="editor-title">{{ $t('print.template.common') }}</span>
            <span class="spacer" />
            <a-button v-perm="'sys:printTemplate:edit'" type="primary" :loading="saving" :disabled="!dirty" @click="save">{{ $t('common.action.save') }}</a-button>
          </div>
          <a-form layout="vertical" class="common-form">
            <a-form-item :label="$t('print.overlay.company')" :extra="$t('print.template.companyTip')">
              <a-input v-model:value="common.companyName" :maxlength="64" :placeholder="defaultCompanyName()" allow-clear />
            </a-form-item>
          </a-form>
        </template>

        <!-- 标签模板 -->
        <LabelTemplateEditor v-else-if="isLabel(current)" :type-key="current" :saved="templates[current]" @saved="refresh" />

        <!-- 单据模板 -->
        <div v-else-if="config" class="editor">
          <div class="config scroll-hover">
            <div class="editor-head">
              <span class="editor-title">{{ printLabel(type.name) }}</span>
              <a-tag v-if="templates[current]" color="blue" :bordered="false">{{ $t('system.config.modified') }}</a-tag>
              <span class="spacer" />
              <span v-perm="'sys:printTemplate:edit'">
                <a-popconfirm
                  v-if="templates[current]"
                  :title="custom ? $t('print.template.resetWithJrxml') : $t('print.template.resetConfirm')"
                  @confirm="reset"
                >
                  <a-button><template #icon><UndoOutlined /></template>{{ $t('system.config.restoreDefault') }}</a-button>
                </a-popconfirm>
              </span>
              <a-button v-perm="'sys:printTemplate:edit'" type="primary" :loading="saving" :disabled="!dirty" @click="save">{{ $t('common.action.save') }}</a-button>
            </div>

            <div class="section-title">{{ $t('print.template.basic') }}</div>
            <a-form layout="vertical" class="basic">
              <a-row :gutter="12">
                <a-col :span="14">
                  <a-form-item :label="$t('print.overlay.docTitle')">
                    <a-input v-model:value="config.title" :placeholder="printLabel(type.name)" :maxlength="32" allow-clear />
                  </a-form-item>
                </a-col>
                <a-col :span="10">
                  <a-form-item :label="$t('print.template.orientation')">
                    <a-radio-group v-model:value="config.orientation" button-style="solid" size="small">
                      <a-radio-button value="portrait">{{ $t('print.template.portrait') }}</a-radio-button>
                      <a-radio-button value="landscape">{{ $t('print.template.landscape') }}</a-radio-button>
                    </a-radio-group>
                  </a-form-item>
                </a-col>
                <a-col :span="config.paper === 'custom' ? 10 : 24">
                  <a-form-item :label="$t('print.template.paper')">
                    <a-select v-model:value="config.paper" :options="PAPERS" />
                  </a-form-item>
                </a-col>
                <template v-if="config.paper === 'custom'">
                  <a-col :span="7">
                    <a-form-item :label="$t('print.template.paperWidth')">
                      <a-input-number v-model:value="config.paperWidth" :min="50" :max="1000" :precision="0" class="full" />
                    </a-form-item>
                  </a-col>
                  <a-col :span="7">
                    <a-form-item :label="$t('print.template.paperHeight')">
                      <a-input-number v-model:value="config.paperHeight" :min="50" :max="2000" :precision="0" class="full" />
                    </a-form-item>
                  </a-col>
                </template>
                <a-col :span="12">
                  <a-form-item :label="$t('print.template.margin')">
                    <a-input-number v-model:value="config.margin" :min="0" :max="40" :precision="0" class="full">
                      <template #addonAfter>mm</template>
                    </a-input-number>
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item :label="$t('print.template.fontSize')">
                    <a-input-number v-model:value="config.fontSize" :min="6" :max="24" :precision="0" class="full">
                      <template #addonAfter>pt</template>
                    </a-input-number>
                  </a-form-item>
                </a-col>
              </a-row>
              <a-space :size="24">
                <a-checkbox v-if="!type.slip" v-model:checked="config.showCompany">{{ $t('print.template.showCompany') }}</a-checkbox>
                <a-checkbox v-if="type.note" v-model:checked="config.showNote">{{ $t('print.template.showNote') }}</a-checkbox>
              </a-space>
            </a-form>

            <div class="section-title">
              {{ $t('print.template.jrxml') }}
              <span class="section-hint">{{ $t('print.template.jrxmlHint') }}</span>
            </div>
            <div class="jrxml">
              <div class="jrxml-status">
                <a-tag v-if="custom" color="green" :bordered="false">{{ $t('print.template.jrxmlUploaded') }}</a-tag>
                <a-tag v-else :bordered="false">{{ $t('print.template.jrxmlNone') }}</a-tag>
              </div>
              <a-space wrap>
                <a-button size="small" :loading="exporting" @click="downloadDefault">
                  <template #icon><DownloadOutlined /></template>{{ $t('print.template.downloadDefault') }}
                </a-button>
                <a-button v-if="custom" size="small" @click="downloadCustom"><template #icon><DownloadOutlined /></template>{{ $t('print.template.downloadUploaded') }}</a-button>
                <span v-perm="'sys:printTemplate:edit'">
                  <a-upload accept=".jrxml,.xml" :show-upload-list="false" :before-upload="upload">
                    <a-button size="small" :loading="uploading"><template #icon><UploadOutlined /></template>{{ $t('print.template.uploadJrxml') }}</a-button>
                  </a-upload>
                </span>
                <span v-if="custom" v-perm="'sys:printTemplate:edit'">
                  <a-popconfirm :title="$t('print.template.removeJrxmlConfirm')" @confirm="removeCustom">
                    <a-button size="small" danger>{{ $t('common.action.delete') }}</a-button>
                  </a-popconfirm>
                </span>
              </a-space>
              <!-- 名称为中文匹配键，示例中保持中文 -->
              <i18n-t keypath="print.template.jrxmlParams" tag="div" class="jrxml-hint">
                <template #fields><code>{{ jrxmlFieldExample }}</code></template>
                <template #column><code>{{ jrxmlColumnExample }}</code></template>
              </i18n-t>
            </div>

            <div v-if="!type.slip" class="section-title">
              {{ $t('print.template.overlay') }}
              <span class="section-hint">{{ $t('print.template.overlayHint') }}</span>
            </div>
            <div v-if="!type.slip" class="jrxml">
              <a-space wrap>
                <a-switch
                  :checked="!!config.overlay?.enabled"
                  :checked-children="$t('system.im.enabled')"
                  :un-checked-children="$t('system.im.notEnabled')"
                  :disabled="custom"
                  @change="toggleOverlay"
                />
                <a-button size="small" :disabled="custom" @click="overlayOpen = true">
                  <template #icon><DragOutlined /></template>{{ $t('print.template.designOverlay') }}
                </a-button>
                <span v-if="config.overlay" class="section-hint">
                  {{ config.overlay.detail
                    ? $t('print.template.overlayStatsDetail', { n: config.overlay.items?.length || 0, cols: config.overlay.detail.columns.length })
                    : $t('print.template.overlayStats', { n: config.overlay.items?.length || 0 }) }}
                </span>
              </a-space>
              <div class="jrxml-hint">{{ $t('print.template.overlayTip') }}</div>
            </div>
            <OverlayDesigner
              v-model:open="overlayOpen"
              :template-key="current"
              :type="type"
              :layout="overlayLayout"
              :value="config.overlay"
              @confirm="(o) => (config.overlay = o)"
            />

            <a-alert v-if="custom" type="info" show-icon class="custom-tip" :message="$t('print.template.customTip')" />
            <template v-for="sec in sections" :key="sec.key">
              <div class="section-title">
                {{ sec.title }}
                <span class="section-hint">{{ sec.key === 'columns' ? $t('print.template.sectionHintColumns') : $t('print.template.sectionHint') }}</span>
              </div>
              <div class="rows">
                <div v-for="(row, i) in config[sec.key]" :key="row[sec.nameKey]" :class="['row', { hidden: row.hidden }]">
                  <a-checkbox :checked="!row.hidden" @update:checked="(v) => (row.hidden = !v)" />
                  <span class="row-name" :title="printLabel(row[sec.nameKey])">{{ printLabel(row[sec.nameKey]) }}</span>
                  <a-input v-model:value="row.alias" size="small" :placeholder="$t('print.template.alias')" :maxlength="20" class="row-alias" />
                  <a-input-number
                    v-if="sec.key === 'columns'"
                    v-model:value="row.width"
                    size="small"
                    :min="5"
                    :max="200"
                    :placeholder="$t('print.template.auto')"
                    class="row-width"
                  >
                    <template #addonAfter>mm</template>
                  </a-input-number>
                  <span class="row-move">
                    <a-button type="text" size="small" :disabled="i === 0" @click="move(sec.key, i, -1)"><ArrowUpOutlined /></a-button>
                    <a-button type="text" size="small" :disabled="i === config[sec.key].length - 1" @click="move(sec.key, i, 1)">
                      <ArrowDownOutlined />
                    </a-button>
                  </span>
                </div>
              </div>
            </template>
          </div>

          <div class="preview">
            <div class="preview-head">
              <span>{{ $t('print.template.preview') }}</span>
              <a-spin v-if="previewing" size="small" />
              <span class="spacer" />
              <a-button type="link" size="small" :disabled="!previewUrl" @click="zoom"><template #icon><ExpandOutlined /></template>{{ $t('print.template.zoom') }}</a-button>
            </div>
            <iframe v-if="previewUrl" class="preview-frame" :src="`${previewUrl}#toolbar=0&view=FitH`" :title="$t('print.preview.title')" />
            <div v-else class="preview-empty">{{ previewError || $t('print.template.generating') }}</div>
          </div>
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import {
  ArrowDownOutlined,
  ArrowUpOutlined,
  BarcodeOutlined,
  DownloadOutlined,
  ExpandOutlined,
  FileTextOutlined,
  SettingOutlined,
  DragOutlined,
  UndoOutlined,
  UploadOutlined
} from '@ant-design/icons-vue'
import OverlayDesigner from '@/components/print/OverlayDesigner.vue'
import { defaultJrxml, printPdf } from '@/api/system/print'
import { downloadJrxml, removeJrxml, resetPrintTemplate, savePrintTemplate, uploadJrxml } from '@/api/system/printTemplate'
import LabelTemplateEditor from '@/components/label/LabelTemplateEditor.vue'
import { LABEL_TYPES, isLabel } from '@/utils/labels'
import { buildPrintRequest, defaultCompanyName, showPdf } from '@/utils/print'
import {
  COMMON_KEY,
  DEFAULT_FONT_SIZE,
  DEFAULT_MARGIN,
  PAPERS,
  PRINT_TYPES,
  PRINT_TYPE_MAP,
  layoutOf,
  loadTemplates,
  printLabel,
  printName,
  sampleDoc
} from '@/utils/printTemplates'

/**
 * 打印模板设计：按单据配置纸张、标题、表头字段、明细列、签字栏的显示、顺序、名称与列宽，右侧为后端生成的 PDF 预览；
 * 也可以下载默认版式的 JRXML，用 Jaspersoft Studio 修改后上传
 */
const { t } = useI18n()
const groups = computed(() => {
  const map = new Map()
  PRINT_TYPES.forEach((tp) => map.set(tp.module, [...(map.get(tp.module) || []), tp]))
  return [...map].map(([module, types]) => ({ module, types }))
})

const templates = ref({})
const current = ref(PRINT_TYPES[0].key)
const type = computed(() => PRINT_TYPE_MAP[current.value])
const config = ref(null)
const common = ref({ companyName: '' })
const snapshot = ref('')
const saving = ref(false)
const custom = computed(() => !!templates.value[current.value]?.jrxml)

const ALL_SECTIONS = [
  { key: 'fields', title: t('print.overlay.fields'), nameKey: 'label' },
  { key: 'columns', title: t('print.template.columns'), nameKey: 'title' },
  { key: 'signs', title: t('print.overlay.signs'), nameKey: 'label' }
]
/** JRXML 中按中文名称取值的示例 */
const jrxmlFieldExample = `$P{FIELDS}.get("${printName('supplier')}")`
const jrxmlColumnExample = `$F{${printName('materialName')}}`
const sections = computed(() => ALL_SECTIONS.filter((s) => config.value?.[s.key]?.length))

/** 已保存的配置与目录合并：按保存的顺序，目录中新增的项放末尾 */
function merge(names, saved, nameKey, extra = {}) {
  const byName = new Map((saved || []).map((s) => [s[nameKey], s]))
  const ordered = (saved || []).filter((s) => names.includes(s[nameKey]))
  names.filter((n) => !byName.has(n)).forEach((n) => ordered.push({ [nameKey]: n }))
  return ordered.map((s) => ({ [nameKey]: s[nameKey], alias: s.alias || '', hidden: !!s.hidden, ...extra, ...(s.width ? { width: s.width } : {}) }))
}

function buildConfig(tp, saved = {}) {
  return {
    title: saved.title || '',
    orientation: saved.orientation || 'portrait',
    paper: saved.paper || 'A4',
    paperWidth: saved.paperWidth || 210,
    paperHeight: saved.paperHeight || 297,
    margin: saved.margin ?? DEFAULT_MARGIN,
    fontSize: saved.fontSize || DEFAULT_FONT_SIZE,
    showCompany: saved.showCompany !== false,
    showNote: saved.showNote !== false,
    fields: merge(tp.fields, saved.fields, 'label'),
    columns: merge(tp.columns.map((c) => c.title), saved.columns, 'title', { width: null }),
    signs: merge(tp.signs, saved.signs, 'label'),
    overlay: saved.overlay || null
  }
}

/** 保存的内容：去掉空的显示名称、列宽 */
function toPayload(cfg) {
  const clean = (rows, nameKey) =>
    rows.map((r) => ({
      [nameKey]: r[nameKey],
      ...(r.alias?.trim() ? { alias: r.alias.trim() } : {}),
      ...(r.hidden ? { hidden: true } : {}),
      ...(r.width ? { width: r.width } : {})
    }))
  return {
    ...(cfg.title?.trim() ? { title: cfg.title.trim() } : {}),
    orientation: cfg.orientation,
    paper: cfg.paper,
    ...(cfg.paper === 'custom' ? { paperWidth: cfg.paperWidth || 210, paperHeight: cfg.paperHeight || 297 } : {}),
    margin: cfg.margin ?? DEFAULT_MARGIN,
    fontSize: cfg.fontSize || DEFAULT_FONT_SIZE,
    showCompany: cfg.showCompany,
    showNote: cfg.showNote,
    fields: clean(cfg.fields, 'label'),
    columns: clean(cfg.columns, 'title'),
    signs: clean(cfg.signs, 'label'),
    ...(cfg.overlay ? { overlay: cfg.overlay } : {})
  }
}

const currentPayload = computed(() =>
  current.value === COMMON_KEY
    ? { companyName: common.value.companyName?.trim() || '' }
    : config.value && toPayload(config.value)
)
const dirty = computed(() => JSON.stringify(currentPayload.value) !== snapshot.value)

function select(key) {
  current.value = key
  if (isLabel(key)) {
    config.value = null
    snapshot.value = ''
    return
  }
  if (key === COMMON_KEY) {
    common.value = { companyName: templates.value[COMMON_KEY]?.companyName || '' }
  } else {
    config.value = buildConfig(PRINT_TYPE_MAP[key], templates.value[key])
  }
  snapshot.value = JSON.stringify(currentPayload.value)
}

// ---------------------------------------------------------------- 套打
const overlayOpen = ref(false)
const overlayLayout = computed(() => layoutOf(toPayload(config.value)))
function toggleOverlay(on) {
  if (on && !config.value.overlay?.items?.length && !config.value.overlay?.detail) {
    overlayOpen.value = true
    return
  }
  config.value.overlay = { ...(config.value.overlay || { items: [], detail: null, offsetX: 0, offsetY: 0 }), enabled: on }
}

function move(key, i, delta) {
  const rows = config.value[key]
  const [row] = rows.splice(i, 1)
  rows.splice(i + delta, 0, row)
}

// ---------------------------------------------------------------- 预览（后端生成 PDF）

/** 用当前（未保存的）配置和示例数据生成的打印请求；已上传 JRXML 时按 JRXML */
function sampleRequest() {
  const tpl = { ...toPayload(config.value), ...(custom.value ? { jrxml: true } : {}) }
  return buildPrintRequest([sampleDoc(type.value)], printLabel(type.value.name), current.value, templates.value, tpl)
}

const previewUrl = ref('')
const previewing = ref(false)
const previewError = ref('')
let previewBlob = null
let previewTimer = null
let previewSeq = 0

async function renderPreview() {
  if (!config.value || current.value === COMMON_KEY) return
  const seq = ++previewSeq
  previewing.value = true
  try {
    const blob = await printPdf(sampleRequest())
    if (seq !== previewSeq) return
    if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
    previewBlob = blob
    previewUrl.value = URL.createObjectURL(blob)
    previewError.value = ''
  } catch (e) {
    if (seq === previewSeq) previewError.value = e?.message || t('print.template.previewFailed')
  } finally {
    if (seq === previewSeq) previewing.value = false
  }
}

// 配置变化后稍等再生成，避免输入时频繁请求
watch(
  () => [current.value, JSON.stringify(currentPayload.value), custom.value, templates.value[COMMON_KEY]?.companyName],
  () => {
    clearTimeout(previewTimer)
    previewTimer = setTimeout(renderPreview, 500)
  }
)

function zoom() {
  if (previewBlob) showPdf(previewBlob, t('print.template.previewTitle', { name: printLabel(type.value.name) }))
}

onBeforeUnmount(() => {
  clearTimeout(previewTimer)
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
})

// ---------------------------------------------------------------- JRXML

function saveBlob(blob, name) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = name
  a.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

const exporting = ref(false)
async function downloadDefault() {
  exporting.value = true
  try {
    // 按当前配置生成，已上传 JRXML 时也下载按配置生成的默认版式
    const req = buildPrintRequest([sampleDoc(type.value)], printLabel(type.value.name), current.value, templates.value, toPayload(config.value))
    saveBlob(await defaultJrxml(req), `${current.value}.jrxml`)
  } finally {
    exporting.value = false
  }
}

async function downloadCustom() {
  saveBlob(await downloadJrxml(current.value), `${current.value}.jrxml`)
}

const uploading = ref(false)
async function upload(file) {
  uploading.value = true
  try {
    await uploadJrxml(current.value, file)
    await refresh()
    message.success(t('print.template.uploaded'))
  } finally {
    uploading.value = false
  }
  return false
}

async function removeCustom() {
  await removeJrxml(current.value)
  await refresh()
  message.success(t('print.template.removed'))
}

// ---------------------------------------------------------------- 保存

async function refresh() {
  templates.value = { ...(await loadTemplates(true)) }
}

async function save() {
  saving.value = true
  try {
    await savePrintTemplate(current.value, currentPayload.value)
    await refresh()
    snapshot.value = JSON.stringify(currentPayload.value)
    message.success(t('print.template.saved'))
  } finally {
    saving.value = false
  }
}

async function reset() {
  await resetPrintTemplate(current.value)
  await refresh()
  select(current.value)
  message.success(t('print.template.resetDone'))
}

onMounted(async () => {
  await refresh()
  select(current.value)
  renderPreview()
})
</script>

<style scoped>
.type-list {
  flex: 1;
  min-height: 0;
  margin: 0 -4px;
}
.type-group {
  margin: 12px 8px 4px;
  font-size: 12px;
  color: var(--app-text-3);
}
.type-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  color: var(--app-text-2);
}
.type-item:hover {
  background: var(--app-hover);
}
.type-item.active {
  color: var(--app-primary);
  background: var(--app-primary-bg);
}
.type-name {
  flex: 1;
  min-width: 0;
}
.type-tag {
  margin: 0;
  font-size: 11px;
  line-height: 18px;
}
.editor-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.editor-card :deep(> .ant-card-body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.editor {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 20px;
}
.config {
  width: 440px;
  flex: none;
  padding-right: 8px;
}
.editor-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.editor-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text);
}
.spacer {
  flex: 1;
}
.section-title {
  margin: 16px 0 8px;
  font-weight: 600;
  color: var(--app-text);
}
.section-hint {
  margin-left: 8px;
  font-weight: 400;
  font-size: 12px;
  color: var(--app-text-3);
}
.basic :deep(.ant-form-item) {
  margin-bottom: 12px;
}
.rows {
  border: 1px solid var(--app-border);
  border-radius: 8px;
}
.row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
}
.row + .row {
  border-top: 1px solid var(--app-border);
}
.row.hidden .row-name {
  color: var(--app-text-3);
  text-decoration: line-through;
}
.row-name {
  width: 96px;
  flex: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.row-alias {
  flex: 1;
  min-width: 0;
}
.row-width {
  width: 104px;
  flex: none;
}
.row-move {
  flex: none;
  display: flex;
}
.preview {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  background: var(--app-hover);
  padding: 8px;
}
.preview-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--app-text-3);
  margin-bottom: 6px;
}
.preview-frame {
  flex: 1;
  min-height: 480px;
  width: 100%;
  border: 0;
  border-radius: 4px;
  background: #fff;
}
.preview-empty {
  flex: 1;
  min-height: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--app-text-3);
}
.full {
  width: 100%;
}
.jrxml {
  border: 1px solid var(--app-border);
  border-radius: 8px;
  padding: 10px 12px;
}
.jrxml-status {
  margin-bottom: 8px;
}
.jrxml-hint {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--app-text-3);
}
.jrxml-hint code {
  font-size: 12px;
}
.custom-tip {
  margin-top: 16px;
}
.common-form {
  max-width: 480px;
}
@media (max-width: 1280px) {
  .editor {
    flex-direction: column;
  }
  .config {
    width: auto;
  }
}
</style>
