<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <SearchForm :model="query" @search="load" @reset="resetQuery">
        <a-form-item :label="$t('bi.report.report')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('bi.report.reportName')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('bi.common.cube')" name="cube">
          <a-select v-model:value="query.cube" :placeholder="$t('common.status.all')" allow-clear :options="cubeOptions" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <div class="toolbar">
        <a-button v-perm="'bi:report:add'" type="primary" @click="router.push('/bi/olap')">
          <template #icon><PlusOutlined /></template>{{ $t('bi.report.createInOlap') }}
        </a-button>
        <span class="sub-text">{{ $t('bi.report.hint') }}</span>
      </div>
      <a-table
        row-key="id"
        size="middle"
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="{ pageSize: 20, hideOnSinglePage: true }"
        :scroll="{ x: 1000 }"
      >
        <template #emptyText>
          <a-empty :description="$t('bi.report.empty')">
            <a-button type="primary" @click="router.push('/bi/olap')">{{ $t('bi.report.goCreate') }}</a-button>
          </a-empty>
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="preview(record)">{{ record.name }}</a>
            <div v-if="record.remark" class="sub-text">{{ record.remark }}</div>
          </template>
          <template v-else-if="column.key === 'cube'">{{ cubeLabel(record.dataset) }}</template>
          <template v-else-if="column.key === 'config'">
            <span class="sub-text">{{ describe(record) }}</span>
          </template>
          <template v-else-if="column.key === 'shared'">
            <a-tag v-if="record.shared === 1" color="blue" :bordered="false">{{ $t('bi.common.shared') }}</a-tag>
            <a-tag v-else :bordered="false">{{ $t('bi.common.private') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'owner'">
            {{ record.mine ? $t('bi.common.me') : record.createByName }}
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="preview(record)"><EyeOutlined />{{ $t('common.action.preview') }}</a-button>
              <a-button type="link" size="small" @click="router.push({ path: '/bi/olap', query: { report: record.id } })">
                <AppstoreOutlined />{{ $t('bi.board.analyze') }}
              </a-button>
              <a-button v-if="record.mine" v-perm="'bi:report:edit'" type="link" size="small" @click="openEdit(record)">
                <EditOutlined />{{ $t('common.action.edit') }}
              </a-button>
              <span v-if="record.mine" v-perm="'bi:report:remove'">
                <a-popconfirm :title="$t('bi.report.confirmDelete', { name: record.name })" @confirm="remove(record)">
                  <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <SubPage v-model:open="previewOpen" :title="current?.name" width="min(900px, 100vw)" destroy-on-close>
      <template v-if="current">
        <div class="preview-bar">
          <span class="sub-text">{{ describe(current) }}</span>
          <a-segmented v-model:value="previewChart" :options="CHART_TYPES" size="small" />
        </div>
        <ReportView :config="currentConfig" :chart="previewChart" height="380px" />
      </template>
    </SubPage>

    <a-modal v-model:open="editOpen" :title="$t('bi.report.edit')" :confirm-loading="saving" @ok="saveEdit">
      <a-form layout="vertical">
        <a-form-item :label="$t('bi.report.reportName')" required>
          <a-input v-model:value="editForm.name" :maxlength="64" />
        </a-form-item>
        <a-form-item :label="$t('bi.common.defaultChart')">
          <a-radio-group v-model:value="editForm.chart" :options="CHART_TYPES" />
        </a-form-item>
        <a-form-item :label="$t('bi.common.shareAll')">
          <a-switch v-model:checked="editForm.shared" />
        </a-form-item>
        <a-form-item :label="$t('common.field.description')">
          <a-textarea v-model:value="editForm.remark" :rows="2" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { AppstoreOutlined, DeleteOutlined, EditOutlined, EyeOutlined, PlusOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import ReportView from '@/components/bi/ReportView.vue'
import { deleteReport, listReports, updateReport } from '@/api/bi'
import { CHART_TYPES, RANGES, loadCubes } from '@/components/bi/bi'
import { t } from '@/locales'

const router = useRouter()
const cubes = ref([])
const list = ref([])
const loading = ref(false)
const query = reactive({ keyword: undefined, cube: undefined })

const cubeOptions = computed(() => cubes.value.map((c) => ({ value: c.key, label: c.label })))
const cubeLabel = (k) => cubes.value.find((c) => c.key === k)?.label || k

const columns = [
  { title: t('bi.report.report'), key: 'name', width: 240 },
  { title: t('bi.common.cube'), key: 'cube', width: 110 },
  { title: t('bi.report.config'), key: 'config', width: 300 },
  { title: t('bi.report.share'), key: 'shared', width: 90 },
  { title: t('common.field.createBy'), key: 'owner', width: 110 },
  { title: t('common.field.updateTime'), dataIndex: 'updateTime', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: 270, fixed: 'right' }
]

const parse = (r) => {
  try {
    return JSON.parse(r.config)
  } catch {
    return {}
  }
}

/** 配置摘要：行 × 列 · 日期范围 · 图表 */
function describe(r) {
  const cfg = parse(r)
  const cube = cubes.value.find((c) => c.key === cfg.cube)
  const dim = (k) => cube?.dims.find((d) => d.key === k)?.label || k
  const range = cfg.range ? RANGES.find((x) => x.value === cfg.range)?.label : cfg.begin || cfg.end ? `${cfg.begin || ''} ~ ${cfg.end || ''}` : t('common.status.all')
  const chart = CHART_TYPES.find((c) => c.value === cfg.chart)?.label || t('bi.chart.bar')
  const filters = Object.keys(cfg.filters || {}).length
  return [cfg.col ? t('bi.report.byRowCol', { row: dim(cfg.row), col: dim(cfg.col) }) : t('bi.report.byRow', { row: dim(cfg.row) }), range, chart, filters ? t('bi.report.filtersN', { n: filters }) : null].filter(Boolean).join(' · ')
}

async function load() {
  loading.value = true
  try {
    list.value = await listReports({ keyword: query.keyword || undefined, cube: query.cube || undefined })
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.assign(query, { keyword: undefined, cube: undefined })
  load()
}

const previewOpen = ref(false)
const previewChart = ref('bar')
const current = ref(null)
const currentConfig = computed(() => (current.value ? parse(current.value) : {}))

function preview(r) {
  current.value = r
  previewChart.value = parse(r).chart || 'bar'
  previewOpen.value = true
}

const editOpen = ref(false)
const saving = ref(false)
const editForm = reactive({ id: null, name: '', chart: 'bar', shared: false, remark: '', config: {} })

function openEdit(r) {
  const cfg = parse(r)
  Object.assign(editForm, { id: r.id, name: r.name, chart: cfg.chart || 'bar', shared: r.shared === 1, remark: r.remark || '', config: cfg })
  editOpen.value = true
}

async function saveEdit() {
  if (!editForm.name.trim()) {
    message.warning(t('common.rule.required', { name: t('bi.report.reportName') }))
    return
  }
  saving.value = true
  try {
    await updateReport(editForm.id, {
      name: editForm.name.trim(),
      config: { ...editForm.config, chart: editForm.chart },
      shared: editForm.shared ? 1 : 0,
      remark: editForm.remark || null
    })
    message.success(t('common.msg.saveSuccess'))
    editOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(r) {
  await deleteReport(r.id)
  message.success(t('common.msg.deleteSuccess'))
  load()
}

onMounted(async () => {
  cubes.value = await loadCubes()
  load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.preview-bar {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
</style>
