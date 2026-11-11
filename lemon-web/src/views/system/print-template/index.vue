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
            <a-tag v-if="formatCount(t.key) > 1" :bordered="false" class="type-tag">{{ $t('print.format.count', { n: formatCount(t.key) }) }}</a-tag>
            <a-tag v-else-if="isModified(templates[t.key])" color="blue" :bordered="false" class="type-tag">{{ $t('system.config.modified') }}</a-tag>
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
              <a-tag v-if="isModified(saved)" color="blue" :bordered="false">{{ $t('system.config.modified') }}</a-tag>
              <span class="spacer" />
              <span v-perm="'sys:printTemplate:edit'">
                <a-popconfirm
                  v-if="isModified(saved)"
                  :title="custom ? $t('print.template.resetWithJrxml') : $t('print.template.resetConfirm')"
                  @confirm="reset"
                >
                  <a-button><template #icon><UndoOutlined /></template>{{ $t('system.config.restoreDefault') }}</a-button>
                </a-popconfirm>
              </span>
              <a-button v-perm="'sys:printTemplate:edit'" type="primary" :loading="saving" :disabled="!dirty" @click="save">{{ $t('common.action.save') }}</a-button>
            </div>

            <FormatBar :model-value="currentFormat?.id" :type-key="current" :formats="keyFormats" @update:model-value="selectFormat" @changed="onFormatsChanged" />

            <BasicSettings :config="config" :type="type" />

            <div class="section-title">
              {{ $t('print.template.jrxml') }}
              <span class="section-hint">{{ $t('print.template.jrxmlHint') }}</span>
            </div>
            <JrxmlPanel :type-key="current" :format-id="currentFormat?.id" :custom="custom" :build-request="defaultJrxmlRequest" @changed="refresh" />

            <OverlaySection :config="config" :type="type" :template-key="current" :format-id="currentFormat?.id" :custom="custom" />

            <a-alert v-if="custom" type="info" show-icon class="custom-tip" :message="$t('print.template.customTip')" />
            <SectionRows :config="config" />
          </div>

          <PreviewPane
            :build-request="sampleRequest"
            :watch-key="previewKey"
            :zoom-title="$t('print.template.previewTitle', { name: printLabel(type.name) })"
          />
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { BarcodeOutlined, FileTextOutlined, SettingOutlined, UndoOutlined } from '@ant-design/icons-vue'
import { resetPrintTemplate, savePrintTemplate } from '@/api/system/printTemplate'
import LabelTemplateEditor from '@/components/label/LabelTemplateEditor.vue'
import { LABEL_TYPES, isLabel } from '@/utils/labels'
import { buildPrintRequest, defaultCompanyName } from '@/utils/print'
import { COMMON_KEY, PRINT_TYPES, PRINT_TYPE_MAP, formatConfig, formatsOf, loadFormats, loadTemplates, printLabel, sampleDoc } from '@/utils/printTemplates'
import BasicSettings from './BasicSettings.vue'
import FormatBar from './FormatBar.vue'
import JrxmlPanel from './JrxmlPanel.vue'
import OverlaySection from './OverlaySection.vue'
import PreviewPane from './PreviewPane.vue'
import SectionRows from './SectionRows.vue'
import { buildConfig, toPayload } from './templateConfig'

/**
 * 打印模板设计：按单据配置纸张、标题、表头字段、明细列、签字栏的显示、顺序、名称与列宽，右侧为后端生成的 PDF 预览；
 * 也可以下载默认版式的 JRXML，用 Jaspersoft Studio 修改后上传。
 * 同一单据可以有多份打印格式（FormatBar），页面编辑的是选中的格式（默认为默认格式）。
 * 各区域在同目录子组件中：BasicSettings（基本设置）、JrxmlPanel（JRXML）、OverlaySection（套打）、SectionRows（字段 / 列 / 签字栏）、
 * PreviewPane（预览）；编辑配置的结构见 templateConfig.js，子组件直接修改页面持有的配置对象
 */
const { t } = useI18n()
const groups = computed(() => {
  const map = new Map()
  PRINT_TYPES.forEach((tp) => map.set(tp.module, [...(map.get(tp.module) || []), tp]))
  return [...map].map(([module, types]) => ({ module, types }))
})

const templates = ref({})
const formats = ref([])
const current = ref(PRINT_TYPES[0].key)
const type = computed(() => PRINT_TYPE_MAP[current.value])
/** 当前单据的打印格式；选中的格式（为空或已删除时为默认格式，没有保存过格式时为空） */
const keyFormats = computed(() => formatsOf(formats.value, current.value))
const formatId = ref(undefined)
const currentFormat = computed(() => keyFormats.value.find((f) => f.id === formatId.value) || keyFormats.value.find((f) => f.isDefault))
/** 选中格式已保存的配置（带 jrxml 标记）；没有保存过时为默认格式（标签、通用设置）的配置 */
const saved = computed(() => (currentFormat.value ? formatConfig(currentFormat.value) : templates.value[current.value]))
const formatCount = (key) => formatsOf(formats.value, key).length
/** 配置有内容（只保存了空配置的格式视为未修改） */
const isModified = (cfg) => !!cfg && Object.keys(cfg).length > 0
const config = ref(null)
const common = ref({ companyName: '' })
const snapshot = ref('')
const saving = ref(false)
const custom = computed(() => !!saved.value?.jrxml)

const currentPayload = computed(() =>
  current.value === COMMON_KEY
    ? { companyName: common.value.companyName?.trim() || '' }
    : config.value && toPayload(config.value)
)
const dirty = computed(() => JSON.stringify(currentPayload.value) !== snapshot.value)

function select(key, fid = undefined) {
  current.value = key
  formatId.value = fid
  if (isLabel(key)) {
    config.value = null
    snapshot.value = ''
    return
  }
  if (key === COMMON_KEY) {
    common.value = { companyName: templates.value[COMMON_KEY]?.companyName || '' }
  } else {
    config.value = buildConfig(PRINT_TYPE_MAP[key], saved.value)
  }
  snapshot.value = JSON.stringify(currentPayload.value)
}

/** 切换要编辑的格式（未保存的修改放弃） */
function selectFormat(id) {
  select(current.value, id)
}

/** 新增、修改、删除格式后重新加载，选中指定的格式 */
async function onFormatsChanged(id) {
  await refresh()
  select(current.value, id)
}

// ---------------------------------------------------------------- 预览（后端生成 PDF）

/** 用当前（未保存的）配置和示例数据生成的打印请求；已上传 JRXML 时按 JRXML；通用设置、标签不预览 */
function sampleRequest() {
  if (!config.value || current.value === COMMON_KEY) return null
  const tpl = { ...toPayload(config.value), ...(custom.value ? { jrxml: true } : {}) }
  return buildPrintRequest([sampleDoc(type.value)], printLabel(type.value.name), current.value, templates.value, tpl, currentFormat.value?.id)
}

/** 下载默认模板：按当前配置生成（已上传 JRXML 时也按配置） */
function defaultJrxmlRequest() {
  return buildPrintRequest([sampleDoc(type.value)], printLabel(type.value.name), current.value, templates.value, toPayload(config.value))
}

/** 配置变化后预览重新生成 */
const previewKey = computed(() => [current.value, currentFormat.value?.id, JSON.stringify(currentPayload.value), custom.value, templates.value[COMMON_KEY]?.companyName])

// ---------------------------------------------------------------- 保存

async function refresh() {
  const [all, list] = await Promise.all([loadTemplates(true), loadFormats(true)])
  templates.value = { ...all }
  formats.value = list
}

async function save() {
  saving.value = true
  try {
    await savePrintTemplate(current.value, currentPayload.value, current.value === COMMON_KEY ? undefined : currentFormat.value?.id)
    await refresh()
    snapshot.value = JSON.stringify(currentPayload.value)
    message.success(t('print.template.saved'))
  } finally {
    saving.value = false
  }
}

/** 恢复默认版式：只有一份格式时删除（恢复为未修改），有多份时只清空选中格式的配置、JRXML 和套打底图 */
async function reset() {
  const fid = keyFormats.value.length > 1 ? currentFormat.value?.id : undefined
  await resetPrintTemplate(current.value, fid)
  await refresh()
  select(current.value, fid)
  message.success(t('print.template.resetDone'))
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
