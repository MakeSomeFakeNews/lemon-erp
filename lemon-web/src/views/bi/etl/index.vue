<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <div class="head">
        <div>
          <div class="title">数据仓库</div>
          <div class="sub-text">
            <template v-if="status.running"><a-badge status="processing" />正在同步…</template>
            <template v-else-if="status.lastSuccess">最近一次成功同步：{{ dayjs(status.lastSuccess).format('YYYY-MM-DD HH:mm:ss') }}</template>
            <template v-else>尚未同步</template>
          </div>
        </div>
        <a-space>
          <a-button @click="loadStatus"><template #icon><ReloadOutlined /></template>刷新</a-button>
          <a-button v-perm="'bi:etl:run'" type="primary" :loading="running" @click="run">
            <template #icon><SyncOutlined /></template>立即同步
          </a-button>
        </a-space>
      </div>
      <a-row :gutter="[16, 16]">
        <a-col v-for="f in FACTS" :key="f.key" :xs="24" :sm="12" :xl="Math.floor(24 / FACTS.length) || 4">
          <div class="fact">
            <div class="fact-name">{{ f.name }}</div>
            <div class="fact-value">
              {{ formatNumber(status[f.key]) }} <small>行</small>
              <a-tag v-if="f.key === 'inventory'" :bordered="false" class="snap">{{ status.snapshots || 0 }} 天快照</a-tag>
            </div>
            <div class="sub-text">{{ f.desc }}</div>
          </div>
        </a-col>
      </a-row>
      <a-alert class="info" type="info" show-icon>
        <template #message>
          每天凌晨 1:30 自动同步（配置项 lemon.bi.etl-cron，设为 "-" 关闭定时同步），也可以手动「立即同步」。
          经营驾驶舱、多维分析、自定义报表和决策支持读取数据仓库，业务数据变化后同步才会反映到分析中；
          每次同步全量重建销售、采购、生产、财务事实表，并保存当天的库存快照（历史快照保留，用于库存趋势分析）。
        </template>
      </a-alert>
    </a-card>

    <a-card :bordered="false" class="page-card table-card" title="同步日志">
      <template #extra>
        <a-select v-model:value="query.status" placeholder="全部状态" allow-clear :options="toOptions(dict.bi_etl_status)" style="width: 120px" @change="search" />
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
              {{ record.triggerType === 'MANUAL' ? '手动' : '定时' }}
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
          <template v-else-if="column.key === 'user'">{{ record.createByName || (record.triggerType === 'SCHEDULE' ? '系统' : '-') }}</template>
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

const FACTS = [
  { key: 'sales', name: '销售事实', desc: '销售出库 / 退货明细，含成本与毛利（按存货单位成本）' },
  { key: 'purchase', name: '采购事实', desc: '采购入库 / 退货明细，不含税金额' },
  { key: 'inventory', name: '库存快照', desc: '每天按仓库、物料汇总的库存数量与金额' },
  { key: 'production', name: '生产事实', desc: '已下达的工单：计划、完工、报废、工时、是否延期' },
  { key: 'finance', name: '财务事实', desc: '已记账凭证按期间、科目、部门汇总（不含结转损益）' },
  { key: 'hr', name: '人力事实', desc: '最近 24 个月按部门、岗位类别的月末人数、入职、离职、工资与人工成本' },
  { key: 'crm', name: '客户经营事实', desc: '按月份、负责人的新增线索、跟进、新增商机、赢单与输单' },
  { key: 'project', name: '项目事实', desc: '按月份、项目的已审批工时与人工成本、费用、月结确认的收入与结转成本' }
]
const LABELS = Object.fromEntries(FACTS.map((f) => [f.key, f.name.replace('事实', '')]))

const dict = useDict('bi_etl_status')
const status = ref({})
const running = ref(false)

const columns = [
  { title: '开始时间', dataIndex: 'startTime', width: 170 },
  { title: '触发方式', key: 'trigger', width: 90 },
  { title: '状态', key: 'status', width: 90 },
  { title: '耗时', key: 'duration', width: 90 },
  { title: '行数', key: 'rows', width: 360 },
  { title: '失败原因', key: 'message', ellipsis: true },
  { title: '操作人', key: 'user', width: 110 }
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
  return ms < 1000 ? '< 1 秒' : `${(ms / 1000).toFixed(1)} 秒`
}

async function loadStatus() {
  status.value = await etlStatus()
}

async function run() {
  running.value = true
  try {
    const log = await runEtl()
    message.success(`同步完成：${parseCounts(log.rowCounts).map((c) => `${c.label} ${c.value} 行`).join('，')}`)
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
