<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <div class="head">
        <div>
          <div class="title">{{ $t('bi.etl.title') }}</div>
          <div class="sub-text">
            <template v-if="status.running"><a-badge status="processing" />{{ $t('bi.etl.syncing') }}</template>
            <template v-else-if="status.lastSuccess">{{ $t('bi.etl.lastSuccess', { time: dayjs(status.lastSuccess).format('YYYY-MM-DD HH:mm:ss') }) }}</template>
            <template v-else>{{ $t('bi.etl.never') }}</template>
          </div>
        </div>
        <a-space>
          <a-button @click="loadStatus"><template #icon><ReloadOutlined /></template>{{ $t('common.action.refresh') }}</a-button>
          <a-button v-perm="'bi:etl:run'" type="primary" :loading="running" @click="run">
            <template #icon><SyncOutlined /></template>{{ $t('bi.etl.syncNow') }}
          </a-button>
        </a-space>
      </div>
      <a-row :gutter="[16, 16]">
        <a-col v-for="f in FACTS" :key="f.key" :xs="24" :sm="12" :xl="Math.floor(24 / FACTS.length) || 4">
          <div class="fact">
            <div class="fact-name">{{ f.name }}</div>
            <div class="fact-value">
              {{ formatNumber(status[f.key]) }} <small>{{ $t('bi.etl.rows') }}</small>
              <a-tag v-if="f.key === 'inventory'" :bordered="false" class="snap">{{ $t('bi.etl.snapshots', { n: status.snapshots || 0 }) }}</a-tag>
            </div>
            <div class="sub-text">{{ f.desc }}</div>
          </div>
        </a-col>
      </a-row>
      <a-alert class="info" type="info" show-icon>
        <template #message>
          {{ $t('bi.etl.tip') }}
        </template>
      </a-alert>
    </a-card>

    <a-card :bordered="false" class="page-card table-card" :title="$t('bi.etl.logs')">
      <template #extra>
        <a-select v-model:value="query.status" :placeholder="$t('bi.etl.allStatus')" allow-clear :options="toOptions(dict.bi_etl_status)" style="width: 140px" @change="search" />
      </template>
      <a-table
        row-key="id"
        size="middle"
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1100, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'trigger'">
            <a-tag :bordered="false" :color="record.triggerType === 'MANUAL' ? 'blue' : 'purple'">
              {{ record.triggerType === 'MANUAL' ? $t('bi.etl.manual') : $t('bi.etl.schedule') }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="bi_etl_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'duration'">{{ duration(record) }}</template>
          <template v-else-if="column.key === 'rows'">
            <a-tag v-for="c in parseCounts(record.rowCounts)" :key="c.key" :bordered="false">{{ c.label }} {{ c.value }}</a-tag>
          </template>
          <template v-else-if="column.key === 'message'">
            <span v-if="record.message" class="error">{{ record.message }}</span>
          </template>
          <template v-else-if="column.key === 'user'">{{ record.createByName || (record.triggerType === 'SCHEDULE' ? $t('bi.etl.system') : '-') }}</template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { ReloadOutlined, SyncOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import { etlLogs, etlStatus, runEtl } from '@/api/bi'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { formatNumber } from '@/utils/format'
import { t } from '@/locales'

const FACTS = [
  { key: 'sales', name: t('bi.etl.fact.sales'), short: t('bi.etl.factShort.sales'), desc: t('bi.etl.factDesc.sales') },
  { key: 'purchase', name: t('bi.etl.fact.purchase'), short: t('bi.etl.factShort.purchase'), desc: t('bi.etl.factDesc.purchase') },
  { key: 'inventory', name: t('bi.etl.fact.inventory'), short: t('bi.etl.factShort.inventory'), desc: t('bi.etl.factDesc.inventory') },
  { key: 'production', name: t('bi.etl.fact.production'), short: t('bi.etl.factShort.production'), desc: t('bi.etl.factDesc.production') },
  { key: 'finance', name: t('bi.etl.fact.finance'), short: t('bi.etl.factShort.finance'), desc: t('bi.etl.factDesc.finance') },
  { key: 'hr', name: t('bi.etl.fact.hr'), short: t('bi.etl.factShort.hr'), desc: t('bi.etl.factDesc.hr') },
  { key: 'crm', name: t('bi.etl.fact.crm'), short: t('bi.etl.factShort.crm'), desc: t('bi.etl.factDesc.crm') },
  { key: 'project', name: t('bi.etl.fact.project'), short: t('bi.etl.factShort.project'), desc: t('bi.etl.factDesc.project') }
]
const LABELS = Object.fromEntries(FACTS.map((f) => [f.key, f.short]))

const dict = useDict('bi_etl_status')
const status = ref({})
const running = ref(false)

const columns = [
  { title: t('bi.etl.startTime'), dataIndex: 'startTime', width: 170 },
  { title: t('bi.etl.trigger'), key: 'trigger', width: 100 },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('bi.etl.duration'), key: 'duration', width: 90 },
  { title: t('bi.etl.rowCount'), key: 'rows', width: 360 },
  { title: t('bi.etl.failReason'), key: 'message', ellipsis: true },
  { title: t('bi.etl.operator'), key: 'user', width: 110 }
]

const query = reactive({ status: undefined })
const { list, loading, pagination, load, search, onTableChange } = useTable(etlLogs, query)

function parseCounts(text) {
  if (!text) return []
  return text.split(',').map((kv) => {
    const [key, value] = kv.split('=')
    return { key, label: LABELS[key] || key, value }
  })
}

function duration(r) {
  if (!r.endTime) return '-'
  const ms = dayjs(r.endTime).diff(dayjs(r.startTime))
  return ms < 1000 ? t('bi.etl.lessThanSecond') : t('bi.etl.seconds', { n: (ms / 1000).toFixed(1) })
}

async function loadStatus() {
  status.value = await etlStatus()
}

async function run() {
  running.value = true
  try {
    const log = await runEtl()
    message.success(t('bi.etl.done', { detail: parseCounts(log.rowCounts).map((c) => t('bi.etl.rowsOf', { label: c.label, n: c.value })).join(t('bi.etl.sep')) }))
  } finally {
    running.value = false
    loadStatus()
    load()
  }
}

onMounted(() => {
  loadStatus()
  load()
})
</script>

<style scoped>
.head {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.title {
  font-size: 16px;
  font-weight: 600;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.fact {
  border: 1px solid var(--app-border);
  border-radius: 8px;
  padding: 12px 16px;
  height: 100%;
}
.fact-name {
  color: var(--app-text-2);
}
.fact-value {
  font-size: 22px;
  font-weight: 600;
  margin: 4px 0;
  font-variant-numeric: tabular-nums;
}
.fact-value small {
  font-size: 12px;
  font-weight: normal;
  color: var(--app-text-3);
}
.snap {
  margin-left: 6px;
  font-weight: normal;
}
.info {
  margin-top: 16px;
}
.error {
  color: #f5222d;
}
</style>
