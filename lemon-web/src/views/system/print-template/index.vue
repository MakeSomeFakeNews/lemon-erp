<template>
  <div class="workspace">
    <!-- 左侧：单据 -->
    <a-card :bordered="false" class="page-card workspace-side">
      <div class="workspace-side-head">打印模板</div>
      <div class="type-list scroll-hover">
        <div :class="['type-item', { active: current === COMMON_KEY }]" @click="select(COMMON_KEY)">
          <SettingOutlined />
          <span class="type-name">通用设置</span>
        </div>
        <template v-for="g in groups" :key="g.module">
          <div class="type-group">{{ g.module }}</div>
          <div
            v-for="t in g.types"
            :key="t.key"
            :class="['type-item', { active: current === t.key }]"
            @click="select(t.key)"
          >
            <FileTextOutlined />
            <span class="type-name">{{ t.name }}</span>
            <a-tag v-if="templates[t.key]" color="blue" :bordered="false" class="type-tag">已修改</a-tag>
          </div>
        </template>
        <div class="type-group">标签</div>
        <div v-for="t in LABEL_TYPES" :key="t.key" :class="['type-item', { active: current === t.key }]" @click="select(t.key)">
          <BarcodeOutlined />
          <span class="type-name">{{ t.name }}</span>
          <a-tag v-if="templates[t.key]" color="blue" :bordered="false" class="type-tag">已修改</a-tag>
        </div>
      </div>
    </a-card>

    <div class="workspace-main">
      <a-card :bordered="false" class="page-card editor-card">
        <!-- 通用设置 -->
        <template v-if="current === COMMON_KEY">
          <div class="editor-head">
            <span class="editor-title">通用设置</span>
            <span class="spacer" />
            <a-button v-perm="'sys:printTemplate:edit'" type="primary" :loading="saving" :disabled="!dirty" @click="save">保存</a-button>
          </div>
          <a-form layout="vertical" class="common-form">
            <a-form-item label="公司名称" extra="显示在各单据的标题上方；为空时使用默认名称">
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
              <span class="editor-title">{{ type.name }}</span>
              <a-tag v-if="templates[current]" color="blue" :bordered="false">已修改</a-tag>
              <span class="spacer" />
              <span v-perm="'sys:printTemplate:edit'">
                <a-popconfirm v-if="templates[current]" title="确定恢复默认版式？" @confirm="reset">
                  <a-button><template #icon><UndoOutlined /></template>恢复默认</a-button>
                </a-popconfirm>
              </span>
              <a-button v-perm="'sys:printTemplate:edit'" type="primary" :loading="saving" :disabled="!dirty" @click="save">保存</a-button>
            </div>

            <div class="section-title">基本设置</div>
            <a-form layout="vertical" class="basic">
              <a-row :gutter="12">
                <a-col :span="14">
                  <a-form-item label="标题">
                    <a-input v-model:value="config.title" :placeholder="type.name" :maxlength="32" allow-clear />
                  </a-form-item>
                </a-col>
                <a-col :span="10">
                  <a-form-item label="纸张方向">
                    <a-radio-group v-model:value="config.orientation" button-style="solid" size="small">
                      <a-radio-button value="portrait">纵向</a-radio-button>
                      <a-radio-button value="landscape">横向</a-radio-button>
                    </a-radio-group>
                  </a-form-item>
                </a-col>
              </a-row>
              <a-space :size="24">
                <a-checkbox v-if="!type.slip" v-model:checked="config.showCompany">显示公司名称</a-checkbox>
                <a-checkbox v-if="type.note" v-model:checked="config.showNote">显示备注</a-checkbox>
              </a-space>
            </a-form>

            <template v-for="sec in sections" :key="sec.key">
              <div class="section-title">
                {{ sec.title }}
                <span class="section-hint">勾选显示，可改名、调整顺序{{ sec.key === 'columns' ? '、设置列宽' : '' }}</span>
              </div>
              <div class="rows">
                <div v-for="(row, i) in config[sec.key]" :key="row[sec.nameKey]" :class="['row', { hidden: row.hidden }]">
                  <a-checkbox :checked="!row.hidden" @update:checked="(v) => (row.hidden = !v)" />
                  <span class="row-name" :title="row[sec.nameKey]">{{ row[sec.nameKey] }}</span>
                  <a-input v-model:value="row.alias" size="small" placeholder="显示名称" :maxlength="20" class="row-alias" />
                  <a-input-number
                    v-if="sec.key === 'columns'"
                    v-model:value="row.width"
                    size="small"
                    :min="5"
                    :max="200"
                    placeholder="自动"
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
              <span>预览（示例数据）</span>
              <a-button type="link" size="small" @click="zoomOpen = true"><template #icon><ExpandOutlined /></template>放大</a-button>
            </div>
            <!-- 按 A4 宽度渲染后整体缩放到预览区宽度，与实际打印的排版一致 -->
            <div ref="previewBox" class="preview-box scroll-hover">
              <div class="preview-page" :style="{ width: `${pageWidth * previewScale}px`, height: `${pageHeight * previewScale}px` }">
                <iframe
                  class="preview-frame"
                  :srcdoc="previewHtml"
                  title="打印预览"
                  :style="{ width: `${pageWidth}px`, height: `${pageHeight}px`, transform: `scale(${previewScale})` }"
                />
              </div>
            </div>
          </div>
        </div>
      </a-card>
    </div>

    <a-modal v-model:open="zoomOpen" :title="`预览 - ${type?.name || ''}`" :footer="null" :width="pageWidth + 48" centered>
      <iframe class="zoom-frame" :srcdoc="previewHtml" title="打印预览" :style="{ width: `${pageWidth}px` }" />
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  ArrowDownOutlined,
  ArrowUpOutlined,
  BarcodeOutlined,
  ExpandOutlined,
  FileTextOutlined,
  SettingOutlined,
  UndoOutlined
} from '@ant-design/icons-vue'
import { resetPrintTemplate, savePrintTemplate } from '@/api/system/printTemplate'
import LabelTemplateEditor from '@/components/label/LabelTemplateEditor.vue'
import { useElementWidth } from '@/composables/useElementWidth'
import { LABEL_TYPES, isLabel } from '@/utils/labels'
import { defaultCompanyName, renderHtml } from '@/utils/print'
import { COMMON_KEY, PRINT_TYPES, PRINT_TYPE_MAP, applyTemplate, loadTemplates, sampleDoc } from '@/utils/printTemplates'

/**
 * 打印模板设计：按单据配置标题、表头字段、明细列、签字栏的显示、顺序、名称与列宽，右侧实时预览
 */
const groups = computed(() => {
  const map = new Map()
  PRINT_TYPES.forEach((t) => map.set(t.module, [...(map.get(t.module) || []), t]))
  return [...map].map(([module, types]) => ({ module, types }))
})

const templates = ref({})
const current = ref(PRINT_TYPES[0].key)
const type = computed(() => PRINT_TYPE_MAP[current.value])
const config = ref(null)
const common = ref({ companyName: '' })
const snapshot = ref('')
const saving = ref(false)

const ALL_SECTIONS = [
  { key: 'fields', title: '表头字段', nameKey: 'label' },
  { key: 'columns', title: '明细列', nameKey: 'title' },
  { key: 'signs', title: '签字栏', nameKey: 'label' }
]
const sections = computed(() => ALL_SECTIONS.filter((s) => config.value?.[s.key]?.length))

/** 已保存的配置与目录合并：按保存的顺序，目录中新增的项放末尾 */
function merge(names, saved, nameKey, extra = {}) {
  const byName = new Map((saved || []).map((s) => [s[nameKey], s]))
  const ordered = (saved || []).filter((s) => names.includes(s[nameKey]))
  names.filter((n) => !byName.has(n)).forEach((n) => ordered.push({ [nameKey]: n }))
  return ordered.map((s) => ({ [nameKey]: s[nameKey], alias: s.alias || '', hidden: !!s.hidden, ...extra, ...(s.width ? { width: s.width } : {}) }))
}

function buildConfig(t, saved = {}) {
  return {
    title: saved.title || '',
    orientation: saved.orientation || 'portrait',
    showCompany: saved.showCompany !== false,
    showNote: saved.showNote !== false,
    fields: merge(t.fields, saved.fields, 'label'),
    columns: merge(t.columns.map((c) => c.title), saved.columns, 'title', { width: null }),
    signs: merge(t.signs, saved.signs, 'label')
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
    showCompany: cfg.showCompany,
    showNote: cfg.showNote,
    fields: clean(cfg.fields, 'label'),
    columns: clean(cfg.columns, 'title'),
    signs: clean(cfg.signs, 'label')
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

function move(key, i, delta) {
  const rows = config.value[key]
  const [row] = rows.splice(i, 1)
  rows.splice(i + delta, 0, row)
}

// A4 纸 96dpi 下的像素尺寸（210mm × 297mm）
const A4 = { width: 794, height: 1123 }
const previewBox = ref()
const zoomOpen = ref(false)
const boxWidth = useElementWidth(previewBox)
const landscape = computed(() => config.value?.orientation === 'landscape')
const pageWidth = computed(() => (landscape.value ? A4.height : A4.width))
const pageHeight = computed(() => (landscape.value ? A4.width : A4.height))
const previewScale = computed(() => (boxWidth.value ? Math.min(1, boxWidth.value / pageWidth.value) : 0.5))

const previewHtml = computed(() => {
  if (!config.value || current.value === COMMON_KEY) return ''
  const doc = applyTemplate(sampleDoc(type.value), toPayload(config.value), templates.value[COMMON_KEY])
  return renderHtml([doc], type.value.name)
})

async function refresh() {
  templates.value = { ...(await loadTemplates(true)) }
}

async function save() {
  saving.value = true
  try {
    await savePrintTemplate(current.value, currentPayload.value)
    await refresh()
    snapshot.value = JSON.stringify(currentPayload.value)
    message.success('已保存，之后打印即按新版式')
  } finally {
    saving.value = false
  }
}

async function reset() {
  await resetPrintTemplate(current.value)
  await refresh()
  select(current.value)
  message.success('已恢复默认版式')
}

onMounted(async () => {
  await refresh()
  select(current.value)
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
  justify-content: space-between;
  font-size: 12px;
  color: var(--app-text-3);
  margin-bottom: 6px;
}
.zoom-frame {
  display: block;
  height: 75vh;
  border: 1px solid var(--app-border);
  background: #fff;
}
.preview-box {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
}
.preview-page {
  overflow: hidden;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.12);
}
.preview-frame {
  display: block;
  border: 0;
  transform-origin: top left;
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
