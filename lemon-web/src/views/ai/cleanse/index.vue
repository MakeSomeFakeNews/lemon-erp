<template>
  <div class="ai-cleanse">
    <!-- 未配置 / 未开启时的引导 -->
    <a-card v-if="status && !status.features?.CLEANSE" :bordered="false" class="page-card">
      <a-result :title="$t('ai.cleanse.notAvailable')" :sub-title="status.configured ? $t('ai.cleanse.disabledDesc') : $t('ai.cleanse.notConfiguredDesc')">
        <template #icon><ClearOutlined class="guide-icon" /></template>
        <template #extra>
          <a-button v-if="canConfig" type="primary" @click="router.push('/ai/settings')">{{ $t('ai.chat.goSettings') }}</a-button>
          <span v-else class="sub-text">{{ $t('ai.chat.contactAdmin') }}</span>
        </template>
      </a-result>
    </a-card>

    <div v-else-if="status" class="workspace">
      <!-- 任务列表 -->
      <a-card :bordered="false" class="page-card workspace-side">
        <div class="workspace-side-head">
          <span>{{ $t('ai.cleanse.tasks') }}</span>
          <a-button type="text" size="small" @click="loadTasks">
            <template #icon><ReloadOutlined /></template>
          </a-button>
        </div>
        <div class="task-list">
          <div v-for="item in tasks" :key="item.id" class="task-item" :class="{ active: item.id === currentId }" @click="select(item.id)">
            <div class="task-title">
              <span>{{ item.sourceLabel }}</span>
              <a-tag :bordered="false" :color="TASK_COLOR[item.status]" class="task-tag">{{ $t('ai.cleanse.taskStatus.' + item.status) }}</a-tag>
            </div>
            <div v-if="item.keyword" class="task-keyword">{{ $t('ai.cleanse.keywordShort', { keyword: item.keyword }) }}</div>
            <div class="sub-text">{{ item.createTime }} · {{ item.createByName || '-' }}</div>
          </div>
          <a-empty v-if="!tasks.length" :image="simpleImage" :description="$t('ai.cleanse.noTasks')" />
        </div>
      </a-card>

      <div class="workspace-main">
        <!-- 发起整理 -->
        <a-card :bordered="false" class="page-card">
          <div class="start-row">
            <a-select
              v-model:value="form.sourceKey"
              class="source-select"
              :options="sources.map((s) => ({ value: s.key, label: s.label }))"
              :placeholder="$t('ai.cleanse.selectSource')"
            />
            <a-input v-model:value="form.keyword" class="keyword-input" :maxlength="64" :placeholder="$t('ai.cleanse.keywordPlaceholder')" allow-clear />
            <a-button type="primary" :loading="starting" :disabled="!form.sourceKey" @click="start">
              <template #icon><ThunderboltOutlined /></template>{{ $t('ai.cleanse.start') }}
            </a-button>
            <span class="sub-text start-hint">{{ $t('ai.cleanse.startHint') }}</span>
          </div>
        </a-card>

        <!-- 当前任务：进度与统计 -->
        <a-card v-if="task" :bordered="false" class="page-card">
          <div class="task-head">
            <div class="task-head-title">
              <span class="name">{{ task.sourceLabel }}</span>
              <span v-if="task.keyword" class="sub-text">{{ $t('ai.cleanse.keywordShort', { keyword: task.keyword }) }}</span>
              <a-tag :bordered="false" :color="TASK_COLOR[task.status]">{{ $t('ai.cleanse.taskStatus.' + task.status) }}</a-tag>
            </div>
            <div class="task-head-extra">
              <span class="sub-text">{{ $t('ai.cleanse.startedBy', { name: task.createByName || '-', time: task.createTime }) }}</span>
              <a-popconfirm v-if="running" :title="$t('ai.cleanse.cancelConfirm')" @confirm="cancel">
                <a-button danger size="small">
                  <template #icon><PauseCircleOutlined /></template>{{ $t('ai.cleanse.cancel') }}
                </a-button>
              </a-popconfirm>
            </div>
          </div>
          <div v-if="running" class="progress-row">
            <a-progress :percent="percent" status="active" class="progress" />
            <span class="phase"><LoadingOutlined /> {{ $t('ai.cleanse.phase.' + task.phase) }}</span>
          </div>
          <div class="stats">
            <div class="stat"><div class="stat-num">{{ task.recordCount }}</div><div class="sub-text">{{ $t('ai.cleanse.records') }}</div></div>
            <div class="stat"><div class="stat-num">{{ task.groupCount }}</div><div class="sub-text">{{ $t('ai.cleanse.groups') }}</div></div>
            <div class="stat"><div class="stat-num dup">{{ task.duplicateCount }}</div><div class="sub-text">{{ $t('ai.cleanse.duplicates') }}</div></div>
            <div class="stat"><div class="stat-num norm">{{ task.normalizeCount }}</div><div class="sub-text">{{ $t('ai.cleanse.normalizes') }}</div></div>
            <div class="stat"><div class="stat-num miss">{{ task.missingCount }}</div><div class="sub-text">{{ $t('ai.cleanse.missings') }}</div></div>
            <div class="stat"><div class="stat-num">{{ task.tokens }}</div><div class="sub-text">{{ $t('ai.cleanse.tokens') }}</div></div>
          </div>
          <a-alert v-if="task.errorMessage" type="error" show-icon class="task-alert" :message="task.errorMessage" />
          <a-alert v-if="task.truncated" type="warning" show-icon class="task-alert" :message="$t('ai.cleanse.truncated', { n: task.recordCount })" />
          <a-alert
            v-if="task.failedSteps || task.discardedCount"
            type="info"
            show-icon
            class="task-alert"
            :message="$t('ai.cleanse.quality', { failed: task.failedSteps, discarded: task.discardedCount })"
          />
        </a-card>

        <!-- 建议 -->
        <a-card v-if="task" :bordered="false" class="page-card table-card">
          <a-tabs v-model:active-key="tab" destroy-inactive-tab-pane>
            <a-tab-pane :key="AiCleanseKind.DUPLICATE">
              <template #tab>{{ $t('ai.cleanse.tabDuplicate') }} <a-badge :count="pending(AiCleanseKind.DUPLICATE)" :overflow-count="999" class="tab-badge" /></template>
              <SuggestionTable
                :key="'d' + task.id + '-' + version"
                :task-id="task.id"
                :kind="AiCleanseKind.DUPLICATE"
                :progress="task.duplicateCount"
                :can-apply="!!source?.canApply && !!source?.supportsDisable"
                @changed="refresh"
              />
            </a-tab-pane>
            <a-tab-pane :key="AiCleanseKind.NORMALIZE">
              <template #tab>{{ $t('ai.cleanse.tabNormalize') }} <a-badge :count="pending(AiCleanseKind.NORMALIZE) + pending(AiCleanseKind.MISSING)" :overflow-count="999" class="tab-badge" /></template>
              <SuggestionTable
                :key="'n' + task.id + '-' + version"
                :task-id="task.id"
                :kind="AiCleanseKind.NORMALIZE"
                :progress="task.normalizeCount + task.missingCount"
                :fields="source?.fields || []"
                :category-labels="categoryLabels"
                :can-apply="!!source?.canApply"
                @changed="refresh"
              />
            </a-tab-pane>
          </a-tabs>
        </a-card>
        <a-card v-else :bordered="false" class="page-card empty-card">
          <a-empty :description="$t('ai.cleanse.emptyDesc')">
            <template #image><ClearOutlined class="empty-icon" /></template>
          </a-empty>
        </a-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Empty, message } from 'ant-design-vue'
import { ClearOutlined, LoadingOutlined, PauseCircleOutlined, ReloadOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/store/user'
import { tt } from '@/locales'
import { AiCleanseKind, AiCleanseTaskStatus, AiSuggestionStatus } from '@/constants/enums'
import { getAiStatus } from '@/api/ai/config'
import { cancelCleanse, getCleanseTask, listCleanseSources, listCleanseTasks, startCleanse } from '@/api/ai/cleanse'
import SuggestionTable from './SuggestionTable.vue'

const POLL_MS = 2000
const TASK_COLOR = {
  [AiCleanseTaskStatus.RUNNING]: 'processing',
  [AiCleanseTaskStatus.COMPLETED]: 'success',
  [AiCleanseTaskStatus.FAILED]: 'error',
  [AiCleanseTaskStatus.CANCELLED]: 'default'
}

const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()
const canConfig = computed(() => userStore.hasPerm('ai:config:list'))
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE

const status = ref(null)
const sources = ref([])
const tasks = ref([])
const currentId = ref(null)
const task = ref(null)
const stats = ref([])
const tab = ref(AiCleanseKind.DUPLICATE)
/** 任务结束时递增，重建建议表格 */
const version = ref(0)
const form = reactive({ sourceKey: undefined, keyword: '' })
const starting = ref(false)
let timer = null

const running = computed(() => task.value?.status === AiCleanseTaskStatus.RUNNING)
const source = computed(() => sources.value.find((s) => s.key === task.value?.sourceKey))

/** 数据类型的分类所用的字典（分类名称按界面语言翻译，见 locales/<语言>/dict.js） */
const CATEGORY_DICTS = { 'wms.material': 'wms_material_category' }
/** 分类的取值 → 名称：有前端译文时用译文，否则（租户自建的分类等）用数据库中的名称 */
const categoryLabels = computed(() => {
  const dict = CATEGORY_DICTS[source.value?.key]
  const map = source.value?.categories || {}
  return dict ? Object.fromEntries(Object.entries(map).map(([v, name]) => [v, tt(`dict.${dict}.${v}`, name)])) : map
})
const percent = computed(() => {
  const total = task.value?.totalSteps || 0
  return total ? Math.min(99, Math.round((task.value.doneSteps * 100) / total)) : 0
})

function pending(kind) {
  return stats.value.filter((s) => s.kind === kind && s.status === AiSuggestionStatus.PENDING).reduce((n, s) => n + s.cnt, 0)
}

async function loadTasks() {
  const res = await listCleanseTasks({ current: 1, size: 30 })
  tasks.value = res.records
}

async function loadTask(id) {
  const res = await getCleanseTask(id)
  if (currentId.value !== id) return
  const wasRunning = running.value
  task.value = res.task
  stats.value = res.stats
  if (wasRunning && !running.value) {
    version.value++
    loadTasks()
  }
  schedule()
}

function schedule() {
  clearTimeout(timer)
  if (running.value) {
    timer = setTimeout(() => currentId.value && loadTask(currentId.value), POLL_MS)
  }
}

function select(id) {
  if (currentId.value === id) return
  currentId.value = id
  task.value = null
  stats.value = []
  tab.value = AiCleanseKind.DUPLICATE
  loadTask(id)
}

async function start() {
  starting.value = true
  try {
    const created = await startCleanse({ sourceKey: form.sourceKey, keyword: form.keyword?.trim() || undefined })
    message.success(t('ai.cleanse.started'))
    await loadTasks()
    currentId.value = null
    select(created.id)
  } finally {
    starting.value = false
  }
}

async function cancel() {
  await cancelCleanse(task.value.id)
  message.success(t('ai.cleanse.cancelled'))
  version.value++
  await loadTask(task.value.id)
  loadTasks()
}

/** 采纳 / 忽略后刷新统计 */
async function refresh() {
  const res = await getCleanseTask(task.value.id)
  stats.value = res.stats
}

onMounted(async () => {
  status.value = await getAiStatus()
  if (!status.value.features?.CLEANSE) return
  sources.value = await listCleanseSources()
  await loadTasks()
  form.sourceKey = sources.value.find((s) => s.key === tasks.value[0]?.sourceKey)?.key ?? sources.value[0]?.key
  if (tasks.value.length) select(tasks.value[0].id)
})
onBeforeUnmount(() => clearTimeout(timer))
</script>

<style scoped>
.ai-cleanse {
  height: 100%;
}
.guide-icon,
.empty-icon {
  font-size: 56px;
  color: var(--app-primary);
}
.empty-icon {
  font-size: 48px;
  opacity: 0.6;
}
.task-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.task-item {
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.task-item:hover {
  background: rgba(0, 0, 0, 0.03);
}
.task-item.active {
  background: var(--app-primary-bg, #e6f4ff);
}
.task-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 500;
}
.task-tag {
  margin-right: 0;
}
.task-keyword {
  font-size: 12px;
  color: var(--app-primary);
}
.start-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.source-select {
  width: 160px;
}
.keyword-input {
  width: 240px;
}
.start-hint {
  margin-left: 8px;
}
.task-head {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.task-head-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.task-head-title .name {
  font-size: 16px;
  font-weight: 600;
}
.task-head-extra {
  display: flex;
  align-items: center;
  gap: 12px;
}
.progress-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 12px;
}
.progress {
  flex: 1;
  margin: 0;
}
.phase {
  white-space: nowrap;
  color: var(--app-primary);
}
.stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 40px;
  margin-top: 14px;
}
.stat-num {
  font-size: 22px;
  font-weight: 600;
  line-height: 30px;
}
.stat-num.dup {
  color: #cf1322;
}
.stat-num.norm {
  color: var(--app-primary);
}
.stat-num.miss {
  color: #d48806;
}
.task-alert {
  margin-top: 12px;
}
.tab-badge :deep(.ant-badge-count) {
  box-shadow: none;
  background: var(--app-primary);
}
.empty-card {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>

<style>
html[data-theme='dark'] .ai-cleanse .task-item:hover {
  background: rgba(255, 255, 255, 0.06);
}
html[data-theme='dark'] .ai-cleanse .task-item.active {
  background: rgba(22, 119, 255, 0.18);
}
</style>
