<template>
  <div class="suggestion-table">
    <div class="filters">
      <a-select v-model:value="query.status" class="filter" :options="statusOptions" :placeholder="$t('ai.cleanse.allStatus')" allow-clear @change="search" />
      <template v-if="!isDuplicate">
        <a-select v-model:value="query.kind" class="filter" :options="kindOptions" @change="search" />
        <a-select v-model:value="query.field" class="filter" :options="fieldOptions" :placeholder="$t('ai.cleanse.allFields')" allow-clear @change="search" />
      </template>
      <a-select v-model:value="query.minConfidence" class="filter" :options="confidenceOptions" :placeholder="$t('ai.cleanse.anyConfidence')" allow-clear @change="search" />
      <a-input-search v-model:value="query.keyword" class="filter-keyword" :placeholder="$t('ai.cleanse.recordKeyword')" allow-clear @search="search" />
      <span class="spacer" />
      <span v-if="canApply" class="batch-actions">
        <span v-if="selectedKeys.length" class="selected-tip">{{ $t('common.table.selected', { n: selectedKeys.length }) }}</span>
        <a-button v-perm="'ai:cleanse:apply'" type="primary" :disabled="!applicableKeys.length" :loading="busy" @click="batchApply">
          <template #icon><CheckOutlined /></template>{{ $t('ai.cleanse.batchApply') }}
        </a-button>
        <a-button v-perm="'ai:cleanse:apply'" :disabled="!selectedKeys.length" :loading="busy" @click="ignore(selectedKeys)">
          <template #icon><StopOutlined /></template>{{ $t('ai.cleanse.batchIgnore') }}
        </a-button>
      </span>
    </div>
    <a-table
      row-key="id"
      size="middle"
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :row-selection="canApply && hasPerm('ai:cleanse:apply') ? selection : null"
      :row-class-name="(r) => (isDuplicate && groupStart(r) ? 'group-start' : '')"
      :scroll="{ x: scrollX, y: '100%' }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'group'">
          <span class="group-no">#{{ record.groupNo }}</span>
        </template>
        <template v-else-if="column.key === 'record'">
          <div class="rec-name">{{ record.recordName }}</div>
          <div class="sub-text">{{ record.recordCode }}</div>
        </template>
        <template v-else-if="column.key === 'keep'">
          <div class="rec-name keep"><CheckCircleFilled class="keep-icon" /> {{ record.keepName }}</div>
          <div class="sub-text">{{ record.keepCode }}</div>
        </template>
        <template v-else-if="column.key === 'field'">
          <a-tag :bordered="false" color="blue">{{ $t('ai.cleanse.field.' + record.field) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'change'">
          <span v-if="record.kind === AiCleanseKind.MISSING" class="missing">
            <ExclamationCircleOutlined /> {{ $t('ai.cleanse.missingValue', { field: $t('ai.cleanse.field.' + record.field) }) }}
          </span>
          <div v-else class="change">
            <span class="old">{{ record.oldValue == null ? $t('ai.cleanse.empty') : displayValue(record, record.oldValue) }}</span>
            <ArrowRightOutlined class="arrow" />
            <span class="new">{{ displayValue(record, record.newValue) }}</span>
          </div>
        </template>
        <template v-else-if="column.key === 'reason'">
          <span v-if="record.reason">{{ record.reason }}</span>
          <span v-else class="sub-text">-</span>
        </template>
        <template v-else-if="column.key === 'confidence'">
          <span v-if="record.confidence != null" class="confidence" :class="confidenceLevel(record.confidence)">{{ record.confidence }}%</span>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tooltip v-if="record.handledTime" :title="$t('ai.cleanse.handledBy', { name: record.handledByName || '-', time: record.handledTime })">
            <a-tag :bordered="false" :color="STATUS_COLOR[record.status]">{{ $t('ai.cleanse.status.' + record.status) }}</a-tag>
          </a-tooltip>
          <a-tag v-else :bordered="false" :color="STATUS_COLOR[record.status]">{{ $t('ai.cleanse.status.' + record.status) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <span v-if="canApply && record.status === AiSuggestionStatus.PENDING" v-perm="'ai:cleanse:apply'" class="table-actions">
            <a-popconfirm
              v-if="isDuplicate"
              :title="$t('ai.cleanse.disableConfirm', { name: record.recordName })"
              @confirm="apply([record.id])"
            >
              <a-button type="link" size="small">{{ $t('ai.cleanse.applyDisable') }}</a-button>
            </a-popconfirm>
            <a-button v-else-if="record.kind !== AiCleanseKind.MISSING" type="link" size="small" @click="apply([record.id])">
              {{ $t('ai.cleanse.apply') }}
            </a-button>
            <a-button type="link" size="small" class="ignore-btn" @click="ignore([record.id])">{{ $t('ai.cleanse.ignore') }}</a-button>
          </span>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import {
  ArrowRightOutlined,
  CheckCircleFilled,
  CheckOutlined,
  ExclamationCircleOutlined,
  StopOutlined
} from '@ant-design/icons-vue'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { AiCleanseField, AiCleanseKind, AiSuggestionStatus } from '@/constants/enums'
import { applyCleanseSuggestions, ignoreCleanseSuggestions, listCleanseSuggestions } from '@/api/ai/cleanse'

/**
 * 一个标签页中的整理建议：疑似重复（按组显示，采纳 = 停用该记录）或规范化建议（原值 → 建议值，含缺失字段提示）
 */
const props = defineProps({
  taskId: { type: Number, required: true },
  kind: { type: String, required: true },
  /** 数据类型的可修改字段 */
  fields: { type: Array, default: () => [] },
  /** 有该数据类型的应用权限 */
  canApply: Boolean,
  /** 分类的取值 → 名称（显示分类建议，已按界面语言翻译） */
  categoryLabels: { type: Object, default: () => ({}) },
  /** 任务进行中的建议数（变化时刷新列表） */
  progress: { type: Number, default: 0 }
})
const emit = defineEmits(['changed'])
const { t } = useI18n()
const hasPerm = (p) => useUserStore().hasPerm(p)

const STATUS_COLOR = {
  [AiSuggestionStatus.PENDING]: 'processing',
  [AiSuggestionStatus.APPLIED]: 'success',
  [AiSuggestionStatus.IGNORED]: 'default'
}
const KIND_ALL = 'ALL'
const isDuplicate = computed(() => props.kind === AiCleanseKind.DUPLICATE)

const query = reactive({
  status: AiSuggestionStatus.PENDING,
  kind: KIND_ALL,
  field: undefined,
  minConfidence: undefined,
  keyword: ''
})
const statusOptions = Object.values(AiSuggestionStatus).map((v) => ({ value: v, label: t('ai.cleanse.status.' + v) }))
const kindOptions = [
  { value: KIND_ALL, label: t('ai.cleanse.kindAll') },
  { value: AiCleanseKind.NORMALIZE, label: t('ai.cleanse.kind.NORMALIZE') },
  { value: AiCleanseKind.MISSING, label: t('ai.cleanse.kind.MISSING') }
]
const fieldOptions = computed(() =>
  (props.fields.length ? props.fields : Object.values(AiCleanseField)).map((v) => ({ value: v, label: t('ai.cleanse.field.' + v) }))
)
const confidenceOptions = [90, 80, 60].map((v) => ({ value: v, label: t('ai.cleanse.confidenceAtLeast', { n: v }) }))

function params(q) {
  const kinds = isDuplicate.value
    ? AiCleanseKind.DUPLICATE
    : q.kind === KIND_ALL
      ? [AiCleanseKind.NORMALIZE, AiCleanseKind.MISSING].join(',')
      : q.kind
  return { kinds, status: q.status, field: q.field, minConfidence: q.minConfidence, keyword: q.keyword || undefined }
}

const { list, loading, pagination, selectedKeys, load, search, onTableChange } = useTable(
  (p) => listCleanseSuggestions(props.taskId, p),
  query,
  { params }
)
pagination.pageSize = 20

const selection = computed(() => ({
  selectedRowKeys: selectedKeys.value,
  onChange: (keys) => (selectedKeys.value = keys),
  getCheckboxProps: (r) => ({ disabled: r.status !== AiSuggestionStatus.PENDING })
}))
/** 选中的建议中可以采纳的（缺失字段提示只能忽略） */
const applicableKeys = computed(() => {
  const byId = new Map(list.value.map((r) => [r.id, r]))
  return selectedKeys.value.filter((id) => byId.get(id) && byId.get(id).kind !== AiCleanseKind.MISSING)
})

const columns = computed(() =>
  isDuplicate.value
    ? [
        { title: t('ai.cleanse.group'), key: 'group', width: 60 },
        { title: t('ai.cleanse.duplicateRecord'), key: 'record', width: 190 },
        { title: t('ai.cleanse.keepRecord'), key: 'keep', width: 190 },
        { title: t('ai.cleanse.reason'), key: 'reason', width: 220 },
        { title: t('ai.cleanse.confidence'), key: 'confidence', width: 100, align: 'center' },
        { title: t('common.field.status'), key: 'status', width: 90 },
        { title: t('common.action.operation'), key: 'action', width: 120 }
      ]
    : [
        { title: t('ai.cleanse.record'), key: 'record', width: 170 },
        { title: t('ai.cleanse.fieldName'), key: 'field', width: 90 },
        { title: t('ai.cleanse.change'), key: 'change', width: 230 },
        { title: t('ai.cleanse.reason'), key: 'reason', width: 180 },
        { title: t('ai.cleanse.confidence'), key: 'confidence', width: 100, align: 'center' },
        { title: t('common.field.status'), key: 'status', width: 90 },
        { title: t('common.action.operation'), key: 'action', width: 120 }
      ]
)

const scrollX = computed(() => columns.value.reduce((n, c) => n + c.width, 0))

/** 任务进行中建议数变化时刷新（保留筛选条件） */
watch(
  () => props.progress,
  () => load()
)

/** 组内第一行（加分隔线） */
function groupStart(r) {
  const i = list.value.indexOf(r)
  return i > 0 && list.value[i - 1].groupNo !== r.groupNo
}

/** 分类建议显示「名称 (取值)」（名称已按界面语言翻译）；不是已有分类的取值时原样显示 */
function displayValue(r, value) {
  if (r.field === AiCleanseField.CATEGORY && props.categoryLabels[value]) {
    return `${props.categoryLabels[value]} (${value})`
  }
  return value
}

function confidenceLevel(n) {
  return n >= 80 ? 'high' : n >= 50 ? 'mid' : 'low'
}

const busy = ref(false)

async function apply(ids) {
  busy.value = true
  try {
    const results = await applyCleanseSuggestions(ids)
    const failed = results.filter((r) => !r.success)
    const ok = results.length - failed.length
    if (!failed.length) {
      message.success(t('ai.cleanse.appliedN', { n: ok }))
    } else {
      const byId = new Map(list.value.map((r) => [r.id, r]))
      Modal.warning({
        title: t('ai.cleanse.applyResult', { ok, failed: failed.length }),
        width: 560,
        content: h(
          'ul',
          { class: 'apply-failures' },
          failed.map((f) => h('li', t('ai.cleanse.failureLine', { name: byId.get(f.id)?.recordName || f.id, message: f.message })))
        )
      })
    }
    selectedKeys.value = []
    await load()
    emit('changed')
  } finally {
    busy.value = false
  }
}

function batchApply() {
  const ids = applicableKeys.value
  Modal.confirm({
    title: t('ai.cleanse.batchApplyConfirm', { n: ids.length }),
    content: isDuplicate.value ? t('ai.cleanse.batchDisableContent') : t('ai.cleanse.batchApplyContent'),
    onOk: () => apply(ids)
  })
}

async function ignore(ids) {
  busy.value = true
  try {
    const n = await ignoreCleanseSuggestions(ids)
    message.success(t('ai.cleanse.ignoredN', { n }))
    selectedKeys.value = []
    await load()
    emit('changed')
  } finally {
    busy.value = false
  }
}

onMounted(load)
defineExpose({ reload: () => load() })
</script>

<style scoped>
.suggestion-table {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.filter {
  width: 130px;
}
.filter-keyword {
  width: 180px;
}
.batch-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}
.spacer {
  flex: 1;
}
.selected-tip {
  color: var(--app-text-secondary, rgba(0, 0, 0, 0.45));
}
.group-no {
  font-weight: 600;
  color: var(--app-primary);
}
.rec-name {
  font-weight: 500;
}
.keep-icon {
  color: #52c41a;
}
.change {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.change .old {
  color: rgba(0, 0, 0, 0.45);
  text-decoration: line-through;
  word-break: break-all;
}
.change .new {
  color: #389e0d;
  font-weight: 500;
  background: #f6ffed;
  border-radius: 4px;
  padding: 0 6px;
  word-break: break-all;
}
.arrow {
  color: rgba(0, 0, 0, 0.35);
  font-size: 12px;
}
.missing {
  color: #d48806;
}
.confidence {
  display: inline-block;
  min-width: 44px;
  border-radius: 10px;
  padding: 0 8px;
  font-size: 12px;
  line-height: 20px;
}
.confidence.high {
  color: #389e0d;
  background: #f6ffed;
}
.confidence.mid {
  color: #d48806;
  background: #fffbe6;
}
.confidence.low {
  color: rgba(0, 0, 0, 0.45);
  background: rgba(0, 0, 0, 0.04);
}
.ignore-btn {
  color: rgba(0, 0, 0, 0.45);
}
:deep(.group-start > td) {
  border-top: 2px solid var(--app-border, #f0f0f0);
}
</style>

<style>
html[data-theme='dark'] .suggestion-table .change .old,
html[data-theme='dark'] .suggestion-table .arrow,
html[data-theme='dark'] .suggestion-table .ignore-btn,
html[data-theme='dark'] .suggestion-table .confidence.low {
  color: rgba(255, 255, 255, 0.45);
}
html[data-theme='dark'] .suggestion-table .change .new,
html[data-theme='dark'] .suggestion-table .confidence.high {
  color: #95de64;
  background: rgba(82, 196, 26, 0.12);
}
html[data-theme='dark'] .suggestion-table .confidence.mid {
  color: #ffd666;
  background: rgba(250, 173, 20, 0.12);
}
html[data-theme='dark'] .suggestion-table .confidence.low {
  background: rgba(255, 255, 255, 0.06);
}
.apply-failures {
  padding-left: 18px;
  margin: 8px 0 0;
  max-height: 320px;
  overflow: auto;
}
</style>
