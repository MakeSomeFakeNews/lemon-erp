<template>
  <div class="board-page">
    <a-card :bordered="false" class="page-card">
      <div class="bar">
        <a-select
          v-model:value="boardId"
          :options="boardOptions"
          :placeholder="$t('bi.board.select')"
          style="width: 260px"
          :loading="loadingList"
          @change="selectBoard"
        />
        <a-tag v-if="board?.shared === 1" color="blue" :bordered="false">{{ $t('bi.common.shared') }}</a-tag>
        <span v-if="board && !board.mine" class="sub-text">{{ $t('bi.common.sharedBy', { name: board.createByName }) }}</span>
        <span v-if="board?.remark" class="sub-text">{{ board.remark }}</span>
        <div class="bar-actions">
          <a-button v-perm="'bi:board:add'" type="primary" @click="openEditor(null)">
            <template #icon><PlusOutlined /></template>{{ $t('bi.board.create') }}
          </a-button>
          <a-button v-if="board?.mine" v-perm="'bi:board:edit'" @click="openEditor(board)">
            <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
          </a-button>
          <span v-if="board?.mine" v-perm="'bi:board:remove'">
            <a-popconfirm :title="$t('bi.board.confirmDelete', { name: board.name })" @confirm="remove">
              <a-button danger><template #icon><DeleteOutlined /></template></a-button>
            </a-popconfirm>
          </span>
          <a-button :disabled="!board" @click="refreshKey++"><template #icon><ReloadOutlined /></template></a-button>
        </div>
      </div>
    </a-card>

    <a-empty v-if="!loadingList && !boards.length" class="page-card empty" :description="$t('bi.board.emptyHint')">
      <a-space>
        <a-button @click="router.push('/bi/olap')">{{ $t('bi.board.goOlap') }}</a-button>
        <a-button v-perm="'bi:board:add'" type="primary" @click="openEditor(null)">{{ $t('bi.board.create') }}</a-button>
      </a-space>
    </a-empty>

    <a-row v-else-if="board" :gutter="[16, 16]">
      <a-col v-for="(w, i) in widgets" :key="`${refreshKey}-${i}`" :xs="24" :lg="w.span || 12">
        <a-card :bordered="false" class="page-card widget" size="small">
          <template #title>{{ reportName(w.reportId) }}</template>
          <template #extra>
            <a v-if="reportMap[w.reportId]" @click="router.push({ path: '/bi/olap', query: { report: w.reportId } })">{{ $t('bi.board.analyze') }}</a>
          </template>
          <ReportView v-if="reportMap[w.reportId]" :config="reportMap[w.reportId].configObj" :chart="w.chart || undefined" height="280px" />
          <a-empty v-else :description="$t('bi.board.reportGone')" />
        </a-card>
      </a-col>
      <a-col v-if="!widgets.length" :span="24">
        <a-empty class="page-card empty" :description="$t('bi.board.noWidget')" />
      </a-col>
    </a-row>

    <a-drawer v-model:open="editorOpen" :title="form.id ? $t('bi.board.edit') : $t('bi.board.create')" width="min(720px, 100vw)" destroy-on-close>
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="14">
            <a-form-item :label="$t('common.field.name')" required>
              <a-input v-model:value="form.name" :maxlength="64" :placeholder="$t('bi.board.namePlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="10">
            <a-form-item :label="$t('bi.common.shareAll')">
              <a-switch v-model:checked="form.shared" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.description')">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
        <div class="widgets-head">
          <span>{{ $t('bi.board.widgets') }}</span>
          <a-button size="small" @click="form.widgets.push({ reportId: undefined, span: 12, chart: '' })">
            <template #icon><PlusOutlined /></template>{{ $t('bi.board.addWidget') }}
          </a-button>
        </div>
        <a-empty v-if="!form.widgets.length" :image="simpleImage" :description="$t('bi.board.addWidgetHint')" />
        <div v-for="(w, i) in form.widgets" :key="i" class="widget-row">
          <a-select v-model:value="w.reportId" :options="reportOptions" show-search option-filter-prop="label" :placeholder="$t('bi.board.selectReport')" class="w-report" />
          <a-select v-model:value="w.span" :options="SPANS" class="w-span" />
          <a-select v-model:value="w.chart" :options="chartOptions" class="w-chart" />
          <a-button type="text" size="small" :disabled="i === 0" @click="move(i, -1)"><ArrowUpOutlined /></a-button>
          <a-button type="text" size="small" :disabled="i === form.widgets.length - 1" @click="move(i, 1)"><ArrowDownOutlined /></a-button>
          <a-button type="text" danger size="small" @click="form.widgets.splice(i, 1)"><DeleteOutlined /></a-button>
        </div>
      </a-form>
      <template #footer>
        <div class="drawer-footer">
          <a-button @click="editorOpen = false">{{ $t('common.action.cancel') }}</a-button>
          <a-button type="primary" :loading="saving" @click="save">{{ $t('common.action.save') }}</a-button>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Empty, message } from 'ant-design-vue'
import { ArrowDownOutlined, ArrowUpOutlined, DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import ReportView from '@/components/bi/ReportView.vue'
import { addBoard, deleteBoard, getBoard, listBoards, listReports, updateBoard } from '@/api/bi'
import { CHART_TYPES } from '@/components/bi/bi'
import { t } from '@/locales'

const STORAGE_KEY = 'lemon-bi-board'
const SPANS = [
  { value: 8, label: t('bi.board.span3') },
  { value: 12, label: t('bi.board.span2') },
  { value: 16, label: t('bi.board.span23') },
  { value: 24, label: t('bi.board.spanFull') }
]
const chartOptions = [{ value: '', label: t('bi.board.defaultChart') }, ...CHART_TYPES]

const router = useRouter()
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const boards = ref([])
const reports = ref([])
const boardId = ref()
const board = ref(null)
const loadingList = ref(false)
const refreshKey = ref(0)

const boardOptions = computed(() => boards.value.map((b) => ({ value: b.id, label: b.mine ? b.name : t('bi.common.withOwner', { name: b.name, owner: b.createByName }) })))
const reportOptions = computed(() => reports.value.map((r) => ({ value: r.id, label: r.mine ? r.name : t('bi.common.withOwner', { name: r.name, owner: r.createByName }) })))
const reportMap = computed(() => Object.fromEntries(reports.value.map((r) => {
  let configObj = {}
  try {
    configObj = JSON.parse(r.config)
  } catch {
    // 配置损坏时显示为空
  }
  return [r.id, { ...r, configObj }]
})))
const reportName = (id) => reportMap.value[id]?.name || t('bi.board.reportUnavailable')
const widgets = computed(() => {
  try {
    return JSON.parse(board.value?.widgets || '[]')
  } catch {
    return []
  }
})

function remember(id) {
  try {
    if (id) localStorage.setItem(STORAGE_KEY, String(id))
    else localStorage.removeItem(STORAGE_KEY)
  } catch {
    // 浏览器禁用存储时忽略
  }
}

async function selectBoard(id) {
  boardId.value = id
  board.value = id ? await getBoard(id) : null
  remember(id)
}

async function loadAll(prefer) {
  loadingList.value = true
  try {
    ;[boards.value, reports.value] = await Promise.all([listBoards(), listReports()])
  } finally {
    loadingList.value = false
  }
  let saved
  try {
    saved = Number(localStorage.getItem(STORAGE_KEY))
  } catch {
    saved = undefined
  }
  const id = [prefer, saved, boards.value[0]?.id].find((x) => x && boards.value.some((b) => b.id === x))
  await selectBoard(id)
}

const editorOpen = ref(false)
const saving = ref(false)
const form = reactive({ id: null, name: '', shared: false, remark: '', widgets: [] })

function openEditor(b) {
  Object.assign(form, b
    ? { id: b.id, name: b.name, shared: b.shared === 1, remark: b.remark || '', widgets: widgets.value.map((w) => ({ chart: '', ...w })) }
    : { id: null, name: '', shared: false, remark: '', widgets: [] })
  editorOpen.value = true
}

function move(i, d) {
  const [w] = form.widgets.splice(i, 1)
  form.widgets.splice(i + d, 0, w)
}

async function save() {
  if (!form.name.trim()) return message.warning(t('common.rule.required', { name: t('common.field.name') }))
  if (form.widgets.some((w) => !w.reportId)) return message.warning(t('bi.board.selectReportEach'))
  saving.value = true
  try {
    const data = {
      name: form.name.trim(),
      shared: form.shared ? 1 : 0,
      remark: form.remark || null,
      widgets: form.widgets.map((w) => ({ reportId: w.reportId, span: w.span, chart: w.chart || null }))
    }
    const id = form.id ? (await updateBoard(form.id, data), form.id) : await addBoard(data)
    message.success(t('common.msg.saveSuccess'))
    editorOpen.value = false
    await loadAll(id)
    refreshKey.value++
  } finally {
    saving.value = false
  }
}

async function remove() {
  await deleteBoard(board.value.id)
  message.success(t('common.msg.deleteSuccess'))
  remember(null)
  loadAll()
}

onMounted(() => loadAll())
</script>

<style scoped>
.board-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.board-page .page-card + .page-card {
  margin-top: 0;
}
.bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.bar-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.empty {
  padding: 40px 0;
}
.widget {
  height: 100%;
}
.widgets-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 8px 0 12px;
  font-weight: 500;
}
.widget-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.w-report {
  flex: 1;
  min-width: 200px;
}
.w-span {
  width: 110px;
}
.w-chart {
  width: 150px;
}
</style>
