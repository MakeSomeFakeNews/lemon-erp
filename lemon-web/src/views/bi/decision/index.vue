<template>
  <a-card :bordered="false" class="page-card decision">
    <a-tabs v-model:activeKey="tab">
      <!-- 销售预测 -->
      <a-tab-pane key="forecast" tab="销售预测">
        <div class="filters">
          <a-select v-model:value="fc.category" placeholder="全部物料分类" allow-clear :options="toOptions(dict.wms_material_category)" style="width: 160px" />
          <a-radio-group v-model:value="fc.months" button-style="solid">
            <a-radio-button :value="3">3 个月</a-radio-button>
            <a-radio-button :value="6">6 个月</a-radio-button>
            <a-radio-button :value="12">12 个月</a-radio-button>
          </a-radio-group>
          <a-button type="primary" :loading="fc.loading" @click="loadForecast">预测</a-button>
          <span v-if="fc.data" class="sub-text">
            方法：{{ fc.data.method }}；趋势斜率 {{ formatMoney(fc.data.slope) }} 元 / 月（基于数据仓库的月销售额，本月未结束不参与拟合）
          </span>
        </div>
        <a-spin :spinning="fc.loading">
          <a-alert
            v-if="fc.data && historyMonths < 3"
            type="warning"
            show-icon
            class="fc-tip"
            :message="`数据仓库中只有 ${historyMonths} 个完整月份有销售数据，预测需要至少 3 个月的历史，当前结果仅供参考`"
          />
          <EChart v-if="fc.data" :option="forecastOption" height="340px" />
          <a-table
            v-if="fc.data"
            size="small"
            row-key="period"
            :pagination="false"
            :data-source="fc.data.forecast"
            :columns="[{ title: '月份', key: 'period' }, { title: '预测销售额', key: 'amount', align: 'right' }]"
            class="mt"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'period'">{{ periodLabel(record.period) }}</template>
              <template v-else>{{ formatMoney(record.amount) }}</template>
            </template>
          </a-table>
        </a-spin>
      </a-tab-pane>

      <!-- ABC 分析 -->
      <a-tab-pane key="abc" tab="ABC 分析">
        <a-spin :spinning="abc.loading">
          <p class="sub-text">
            近一年按{{ abc.data?.basis || '销售成本' }}降序累计：累计占比 70% 以内为 A 类（重点管控），70%-90% 为 B 类，其余为 C 类
          </p>
          <a-row :gutter="[16, 16]">
            <a-col v-for="s in abc.data?.summary || []" :key="s.class" :xs="24" :sm="8">
              <div class="summary-card" :class="`cls-${s.class}`">
                <div class="summary-title">{{ s.class }} 类物料</div>
                <div class="summary-value">{{ s.count }} 种</div>
                <div class="sub-text">金额 {{ formatMoney(s.value) }}，占 {{ percent(s.share) }}</div>
              </div>
            </a-col>
          </a-row>
          <EChart v-if="abc.data?.rows.length" :option="paretoOption" height="320px" class="mt" />
          <a-table
            size="small"
            row-key="materialId"
            :data-source="abc.data?.rows || []"
            :columns="abcColumns"
            :pagination="{ pageSize: 20, hideOnSinglePage: true }"
            :scroll="{ x: 860 }"
            class="mt"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'material'">{{ record.code }} {{ record.name }}</template>
              <template v-else-if="column.key === 'category'"><DictTag type="wms_material_category" :value="record.category" /></template>
              <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }}</template>
              <template v-else-if="column.key === 'value'">{{ formatMoney(record.value) }}</template>
              <template v-else-if="column.key === 'share'">{{ percent(record.share, 2) }}</template>
              <template v-else-if="column.key === 'cumulative'">{{ percent(record.cumulative, 2) }}</template>
              <template v-else-if="column.key === 'class'">
                <a-tag :color="{ A: 'red', B: 'orange' }[record.class]" :bordered="false">{{ record.class }}</a-tag>
              </template>
            </template>
          </a-table>
        </a-spin>
      </a-tab-pane>

      <!-- 呆滞物料 -->
      <a-tab-pane key="slow" tab="呆滞物料">
        <div class="filters">
          <span>超过</span>
          <a-input-number v-model:value="slow.days" :min="0" :max="3650" addon-after="天" style="width: 140px" />
          <span>没有出库（不含调拨）且有库存</span>
          <a-button type="primary" :loading="slow.loading" @click="loadSlow">查询</a-button>
          <span v-if="slow.list.length" class="sub-text">
            共 {{ slow.list.length }} 种，库存金额 {{ formatMoney(slow.list.reduce((s, r) => s + Number(r.amount || 0), 0)) }}（按最新库存快照）
          </span>
        </div>
        <a-table
          size="small"
          row-key="materialId"
          :loading="slow.loading"
          :data-source="slow.list"
          :columns="slowColumns"
          :pagination="{ pageSize: 20, hideOnSinglePage: true }"
          :scroll="{ x: 900 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">{{ record.code }} {{ record.name }}</template>
            <template v-else-if="column.key === 'category'"><DictTag type="wms_material_category" :value="record.category" /></template>
            <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
            <template v-else-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template>
            <template v-else-if="column.key === 'lastOut'">{{ record.lastOut || '从未出库' }}</template>
            <template v-else-if="column.key === 'idleDays'">
              <a-tag v-if="record.idleDays != null" :color="record.idleDays >= 180 ? 'red' : record.idleDays >= 90 ? 'orange' : 'default'" :bordered="false">
                {{ record.idleDays }} 天
              </a-tag>
            </template>
          </template>
        </a-table>
      </a-tab-pane>

      <!-- 预警 -->
      <a-tab-pane key="alerts" tab="预警">
        <div class="filters">
          <a-button :loading="al.loading" @click="loadAlerts"><template #icon><ReloadOutlined /></template>重新计算</a-button>
          <span class="sub-text">触发 {{ al.list.filter((a) => a.triggered).length }} 条 / 共 {{ al.list.length }} 条启用的规则</span>
        </div>
        <a-row :gutter="[16, 16]">
          <a-col v-for="a in al.list" :key="a.ruleId" :xs="24" :md="12" :xl="8">
            <div class="alert-card" :class="{ on: a.triggered, [`lv-${a.level}`]: a.triggered }">
              <div class="alert-head">
                <span class="alert-name">{{ a.name }}</span>
                <DictTag v-if="a.triggered" type="bi_alert_level" :value="a.level" />
                <a-tag v-else color="green" :bordered="false">正常</a-tag>
              </div>
              <div class="alert-value">{{ fmtValue(a.value, a.unit) }} <small>{{ a.unit }}</small></div>
              <div class="sub-text">{{ a.metricLabel }} {{ OPERATORS[a.operator] }} {{ fmtValue(a.threshold, a.unit) }}{{ a.unit }} 时预警</div>
              <div v-if="a.triggered && a.remark" class="alert-remark">{{ a.remark }}</div>
            </div>
          </a-col>
        </a-row>

        <a-divider orientation="left">预警规则</a-divider>
        <div class="filters">
          <a-button v-perm="'bi:alert:edit'" type="primary" @click="openRule(null)">
            <template #icon><PlusOutlined /></template>新增规则
          </a-button>
        </div>
        <a-table size="small" row-key="id" :data-source="rules" :columns="ruleColumns" :pagination="false" :scroll="{ x: 900 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'metric'">{{ metricLabel(record.metric) }}</template>
            <template v-else-if="column.key === 'condition'">
              {{ OPERATORS[record.operator] }} {{ fmtValue(record.threshold, metricUnit(record.metric)) }} {{ metricUnit(record.metric) }}
            </template>
            <template v-else-if="column.key === 'level'"><DictTag type="bi_alert_level" :value="record.level" /></template>
            <template v-else-if="column.key === 'enabled'">
              <a-tag :color="record.enabled === 1 ? 'green' : 'default'" :bordered="false">{{ record.enabled === 1 ? '启用' : '停用' }}</a-tag>
            </template>
            <template v-else-if="column.key === 'action'">
              <span v-perm="'bi:alert:edit'" class="table-actions">
                <a-button type="link" size="small" @click="openRule(record)"><EditOutlined />编辑</a-button>
                <a-popconfirm :title="`确定删除规则「${record.name}」？`" @confirm="removeRule(record)">
                  <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </template>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>

    <a-modal v-model:open="ruleOpen" :title="ruleForm.id ? '编辑预警规则' : '新增预警规则'" :confirm-loading="ruleSaving" @ok="saveRule">
      <a-form ref="ruleRef" :model="ruleForm" :rules="ruleRules" layout="vertical">
        <a-form-item label="规则名称" name="name">
          <a-input v-model:value="ruleForm.name" :maxlength="64" />
        </a-form-item>
        <a-form-item label="监控指标" name="metric">
          <a-select v-model:value="ruleForm.metric" :options="metrics.map((m) => ({ value: m.key, label: `${m.label}（${m.unit}）` }))" />
        </a-form-item>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="比较" name="operator">
              <a-select v-model:value="ruleForm.operator" :options="Object.entries(OPERATORS).map(([value, label]) => ({ value, label }))" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="阈值" name="threshold">
              <a-input-number v-model:value="ruleForm.threshold" class="full" :addon-after="metricUnit(ruleForm.metric) || undefined" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="级别" name="level">
              <a-select v-model:value="ruleForm.level" :options="toOptions(dict.bi_alert_level)" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="启用">
              <a-switch v-model:checked="ruleForm.enabledBool" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="处理建议" name="remark">
          <a-textarea v-model:value="ruleForm.remark" :rows="2" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import EChart from '@/components/charts/EChart.vue'
import {
  abcAnalysis, addAlertRule, alertMetrics, alerts, deleteAlertRule, listAlertRules, salesForecast, slowMoving, updateAlertRule
} from '@/api/bi'
import { formatMoney } from '@/components/scm/scm'
import { formatQty } from '@/components/wms/wms'
import { percent, periodLabel, shortMoney } from '@/components/charts/charts'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'

const OPERATORS = { GT: '大于', GE: '不小于', LT: '小于', LE: '不大于' }
const dict = useDict('wms_material_category', 'bi_alert_level')
const tab = ref('forecast')
const loaded = new Set()

const fmtValue = (v, unit) => (v === null || v === undefined ? '-' : unit === '元' ? formatMoney(v) : Number(v).toLocaleString('zh-CN'))

// ---------------------------------------------------------------- 销售预测

const fc = reactive({ category: undefined, months: 3, data: null, loading: false })

async function loadForecast() {
  fc.loading = true
  try {
    fc.data = await salesForecast({ category: fc.category || undefined, months: fc.months })
  } finally {
    fc.loading = false
  }
}

/** 有销售额的完整月份数 */
const historyMonths = computed(() => {
  const current = Number(dayjs().format('YYYYMM'))
  return (fc.data?.history || []).filter((x) => x.period < current && Number(x.amount) !== 0).length
})

const forecastOption = computed(() => {
  const h = fc.data.history
  const f = fc.data.forecast
  const periods = [...h.map((x) => x.period), ...f.map((x) => x.period).filter((p) => !h.some((x) => x.period === p))]
  const current = Number(dayjs().format('YYYYMM'))
  const histMap = Object.fromEntries(h.filter((x) => x.period < current).map((x) => [x.period, Number(x.amount)]))
  const fcMap = Object.fromEntries(f.map((x) => [x.period, Number(x.amount)]))
  // 预测线从最后一个完整月份接上
  const lastFull = h.filter((x) => x.period < current).at(-1)
  if (lastFull) fcMap[lastFull.period] = Number(lastFull.amount)
  return {
    tooltip: { trigger: 'axis', valueFormatter: (v) => (v == null ? '-' : `¥ ${shortMoney(v)}`) },
    legend: { top: 0 },
    grid: { left: 16, right: 24, top: 36, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: periods.map(periodLabel) },
    yAxis: { type: 'value', axisLabel: { formatter: shortMoney } },
    series: [
      {
        name: '实际销售额', type: 'line', smooth: true, data: periods.map((p) => histMap[p] ?? null),
        markLine: { symbol: 'none', silent: true, label: { formatter: '本月' }, lineStyle: { type: 'dotted' }, data: [{ xAxis: periodLabel(current) }] }
      },
      { name: '预测', type: 'line', smooth: true, lineStyle: { type: 'dashed' }, data: periods.map((p) => fcMap[p] ?? null) }
    ]
  }
})

// ---------------------------------------------------------------- ABC

const abc = reactive({ data: null, loading: false })
const abcColumns = [
  { title: '物料', key: 'material', width: 240 },
  { title: '分类', key: 'category', width: 100 },
  { title: '数量', key: 'qty', width: 110, align: 'right' },
  { title: '金额', key: 'value', width: 140, align: 'right' },
  { title: '占比', key: 'share', width: 90, align: 'right' },
  { title: '累计占比', key: 'cumulative', width: 100, align: 'right' },
  { title: '类别', key: 'class', width: 80 }
]

async function loadAbc() {
  abc.loading = true
  try {
    abc.data = await abcAnalysis()
  } finally {
    abc.loading = false
  }
}

const paretoOption = computed(() => {
  const rows = abc.data.rows.slice(0, 30)
  return {
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { left: 16, right: 16, top: 36, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: rows.map((r) => r.code), axisLabel: { rotate: 30, hideOverlap: true } },
    yAxis: [
      { type: 'value', axisLabel: { formatter: shortMoney } },
      { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } }
    ],
    series: [
      {
        name: abc.data.basis, type: 'bar', barMaxWidth: 24,
        data: rows.map((r) => ({ value: Number(r.value), itemStyle: { color: { A: '#f5222d', B: '#fa8c16' }[r.class] || '#8c8c8c' } }))
      },
      { name: '累计占比', type: 'line', yAxisIndex: 1, smooth: true, data: rows.map((r) => Number(r.cumulative)) }
    ]
  }
})

// ---------------------------------------------------------------- 呆滞

const slow = reactive({ days: 90, list: [], loading: false })
const slowColumns = [
  { title: '物料', key: 'material', width: 240 },
  { title: '分类', key: 'category', width: 100 },
  { title: '库存数量', key: 'qty', width: 120, align: 'right' },
  { title: '库存金额', key: 'amount', width: 130, align: 'right' },
  { title: '最近出库', key: 'lastOut', width: 170 },
  { title: '呆滞天数', key: 'idleDays', width: 100 }
]

async function loadSlow() {
  slow.loading = true
  try {
    slow.list = await slowMoving(slow.days ?? 90)
  } finally {
    slow.loading = false
  }
}

// ---------------------------------------------------------------- 预警

const al = reactive({ list: [], loading: false })
const rules = ref([])
const metrics = ref([])
const metricLabel = (k) => metrics.value.find((m) => m.key === k)?.label || k
const metricUnit = (k) => metrics.value.find((m) => m.key === k)?.unit || ''
const ruleColumns = [
  { title: '规则', dataIndex: 'name', width: 180 },
  { title: '监控指标', key: 'metric', width: 220 },
  { title: '预警条件', key: 'condition', width: 160 },
  { title: '级别', key: 'level', width: 80 },
  { title: '状态', key: 'enabled', width: 80 },
  { title: '处理建议', dataIndex: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 140, fixed: 'right' }
]

async function loadAlerts() {
  al.loading = true
  try {
    ;[al.list, rules.value, metrics.value] = await Promise.all([alerts(), listAlertRules(), alertMetrics()])
  } finally {
    al.loading = false
  }
}

const ruleOpen = ref(false)
const ruleSaving = ref(false)
const ruleRef = ref()
const ruleForm = reactive({ id: null, name: '', metric: undefined, operator: 'GT', threshold: 0, level: 'WARN', enabledBool: true, remark: '' })
const ruleRules = {
  name: [{ required: true, whitespace: true, message: '请输入规则名称' }],
  metric: [{ required: true, message: '请选择监控指标' }],
  operator: [{ required: true, message: '请选择比较方式' }],
  threshold: [{ required: true, message: '请填写阈值' }],
  level: [{ required: true, message: '请选择级别' }]
}

function openRule(r) {
  Object.assign(ruleForm, r
    ? { id: r.id, name: r.name, metric: r.metric, operator: r.operator, threshold: Number(r.threshold), level: r.level, enabledBool: r.enabled === 1, remark: r.remark || '' }
    : { id: null, name: '', metric: undefined, operator: 'GT', threshold: 0, level: 'WARN', enabledBool: true, remark: '' })
  ruleOpen.value = true
}

async function saveRule() {
  try {
    await ruleRef.value.validate()
  } catch {
    return
  }
  ruleSaving.value = true
  try {
    const { id, enabledBool, ...rest } = ruleForm
    const data = { ...rest, enabled: enabledBool ? 1 : 0, remark: rest.remark || null }
    id ? await updateAlertRule(id, data) : await addAlertRule(data)
    message.success('保存成功')
    ruleOpen.value = false
    loadAlerts()
  } finally {
    ruleSaving.value = false
  }
}

async function removeRule(r) {
  await deleteAlertRule(r.id)
  message.success('删除成功')
  loadAlerts()
}

// 切换到某个标签页时才加载
const LOADERS = { forecast: loadForecast, abc: loadAbc, slow: loadSlow, alerts: loadAlerts }
watch(tab, (t) => {
  if (!loaded.has(t)) {
    loaded.add(t)
    LOADERS[t]()
  }
})
onMounted(() => {
  loaded.add(tab.value)
  LOADERS[tab.value]()
})
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.mt {
  margin-top: 16px;
}
.full {
  width: 100%;
}
.summary-card {
  border: 1px solid var(--app-border);
  border-left-width: 4px;
  border-radius: 8px;
  padding: 12px 16px;
}
.cls-A {
  border-left-color: #f5222d;
}
.cls-B {
  border-left-color: #fa8c16;
}
.cls-C {
  border-left-color: #8c8c8c;
}
.summary-title {
  color: var(--app-text-2);
}
.summary-value {
  font-size: 22px;
  font-weight: 600;
  margin: 4px 0;
}
.alert-card {
  border: 1px solid var(--app-border);
  border-radius: 8px;
  padding: 14px 16px;
  height: 100%;
}
.alert-card.on {
  border-left-width: 4px;
}
.alert-card.lv-DANGER {
  border-left-color: #f5222d;
}
.alert-card.lv-WARN {
  border-left-color: #fa8c16;
}
.alert-card.lv-INFO {
  border-left-color: #1677ff;
}
.alert-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.alert-name {
  font-weight: 500;
}
.alert-value {
  font-size: 22px;
  font-weight: 600;
  margin: 6px 0 2px;
  font-variant-numeric: tabular-nums;
}
.alert-value small {
  font-size: 12px;
  font-weight: normal;
  color: var(--app-text-3);
}
.alert-remark {
  margin-top: 6px;
  font-size: 12px;
  color: var(--app-text-2);
}
.fc-tip {
  margin-bottom: 12px;
}
</style>
