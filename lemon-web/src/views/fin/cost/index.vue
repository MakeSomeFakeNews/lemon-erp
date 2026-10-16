<template>
  <div class="cost-page">
    <a-card :bordered="false" class="page-card">
      <div class="head">
        <a-space wrap>
          <span>核算期间</span>
          <PeriodPicker v-model:value="period" @update:value="selectPeriod" />
          <a-button v-if="!isOpening" v-perm="'fin:cost:run'" type="primary" :loading="running" @click="run">
            {{ current ? '重新核算' : '核算成本' }}
          </a-button>
          <span v-if="current && !isOpening" v-perm="'fin:cost:run'">
            <a-popconfirm title="撤销核算：删除成本数据和未审核的成本结转凭证，确定？" @confirm="undo">
              <a-button>撤销核算</a-button>
            </a-popconfirm>
          </span>
        </a-space>
        <span class="sub-text">月末一次加权平均，须逐月核算；工单按工时 × 工作中心费率归集人工和制造费用，与实际发生的差异转入主营业务成本</span>
      </div>
      <a-alert v-if="isOpening" type="info" show-icon class="opening-tip" message="本期为存货期初（启用期间的上一期），在「期初余额」中维护，下一期开始核算成本" />
      <a-row :gutter="16" class="stats">
        <a-col :xs="12" :md="5"><a-statistic title="核算状态" :value="isOpening ? '存货期初' : current ? '已核算' : '未核算'" /></a-col>
        <a-col :xs="12" :md="5"><a-statistic title="完工产品成本" :precision="2" prefix="¥" :value="Number(current?.finishedCost || 0)" /></a-col>
        <a-col :xs="12" :md="5"><a-statistic title="销售成本" :precision="2" prefix="¥" :value="Number(current?.salesCost || 0)" /></a-col>
        <a-col :xs="12" :md="5"><a-statistic title="期末在产品" :precision="2" prefix="¥" :value="Number(current?.wipCost || 0)" /></a-col>
        <a-col :xs="12" :md="4">
          <a-statistic title="制造费用差异" :precision="2" prefix="¥" :value="Number(current?.overheadVariance || 0)" />
        </a-col>
      </a-row>
      <div v-if="current && !isOpening" class="sub-text">
        核算人 {{ current.createByName }} {{ current.createTime }} · 成本结转凭证
        <a v-if="current.voucherId" @click="openVoucher(current.voucherId)">{{ current.voucherNo }}</a>
        <span v-else>无</span>
        <DictTag v-if="current.voucherStatus" type="fin_voucher_status" :value="current.voucherStatus" class="ml" />
      </div>
    </a-card>

    <a-card :bordered="false" class="page-card">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="materials" tab="存货收发存">
          <a-input-search v-model:value="keyword" placeholder="物料编码或名称" allow-clear class="filter" @search="materials.search" />
          <a-table
            row-key="id"
            size="small"
            bordered
            :columns="materialColumns"
            :data-source="materials.list.value"
            :loading="materials.loading.value"
            :pagination="materials.pagination"
            :scroll="{ x: 1700 }"
            :locale="{ emptyText: current ? '没有数据' : '本期未核算' }"
            @change="materials.onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'material'">
                <div>{{ record.materialCode }} {{ record.materialName }}</div>
                <div class="sub-text">{{ record.spec }}</div>
              </template>
              <template v-else-if="column.key === 'unitCost'">{{ fmt(record.unitCost, 4) }}</template>
              <template v-else-if="column.qty">
                <div class="num">{{ formatQty(record[column.qty]) === '0' ? '' : formatQty(record[column.qty]) }}</div>
                <div class="num sub-text">{{ moneyOrBlank(record[column.amount]) }}</div>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="wo" tab="工单成本">
          <a-table
            row-key="id"
            size="small"
            bordered
            :columns="woColumns"
            :data-source="wos.list.value"
            :loading="wos.loading.value"
            :pagination="wos.pagination"
            :scroll="{ x: 1300 }"
            @change="wos.onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'wo'">
                <div>{{ record.woNo }}</div>
                <div class="sub-text">{{ record.materialCode }} {{ record.materialName }}</div>
              </template>
              <template v-else-if="column.key === 'hours'">{{ formatQty(record.hours) }}</template>
              <template v-else-if="column.key === 'finishedQty'">{{ formatQty(record.finishedQty) }} {{ record.unit }}</template>
              <template v-else-if="column.key === 'unitCost'">{{ record.unitCost != null ? fmt(record.unitCost, 4) : '-' }}</template>
              <template v-else-if="column.money"><span class="num">{{ moneyOrBlank(record[column.key]) }}</span></template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="variance" tab="成本差异">
          <template v-if="variance">
            <a-alert v-if="!variance.versionId" type="info" show-icon class="opening-tip"
              message="本期没有生效的标准成本版本，请先在「标准成本」中发布（生效期间不晚于本期）" />
            <template v-else>
              <div class="variance-head">
                <span>标准成本版本：{{ variance.versionName }}</span>
                <span class="sub-text">本期完工（最后一次完工入库在本期）或关闭的工单；实际成本为截至本期各期工单成本合计；正数为超支（不利差异）</span>
              </div>
              <a-alert v-if="!variance.costRun" type="warning" show-icon class="opening-tip" message="本期还没有核算成本，实际成本只包含已核算期间的数据" />
              <a-row :gutter="16" class="stats">
                <a-col :xs="12" :md="4"><a-statistic title="实际成本" :precision="2" :value="Number(variance.total.actualTotal)" /></a-col>
                <a-col :xs="12" :md="4"><a-statistic title="标准成本" :precision="2" :value="Number(variance.total.stdTotal)" /></a-col>
                <a-col :xs="12" :md="4">
                  <a-statistic title="总差异" :precision="2" :value="Number(variance.total.totalVariance)" :value-style="varianceStyle(variance.total.totalVariance)" />
                </a-col>
                <a-col :xs="12" :md="4"><a-statistic title="材料差异（价格 / 用量）" :value="`${money(variance.total.materialPrice)} / ${money(variance.total.materialUsage)}`" /></a-col>
                <a-col :xs="12" :md="4"><a-statistic title="人工差异（费率 / 效率）" :value="`${money(variance.total.laborRate)} / ${money(variance.total.laborEfficiency)}`" /></a-col>
                <a-col :xs="12" :md="4">
                  <a-statistic title="制造费用（耗费 / 效率）" :value="`${money(variance.total.overheadSpending)} / ${money(variance.total.overheadEfficiency)}`" />
                </a-col>
              </a-row>
              <a-table
                row-key="woId"
                size="small"
                bordered
                :columns="varianceColumns"
                :data-source="variance.orders"
                :loading="varianceLoading"
                :pagination="{ pageSize: 20, showSizeChanger: false, showTotal: (t) => `共 ${t} 张工单` }"
                :scroll="{ x: 1500 }"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'wo'">
                    <a @click="openComponents(record)">{{ record.woNo }}</a>
                    <DictTag type="mes_wo_status" :value="record.status" class="ml" />
                    <div class="sub-text">{{ record.materialCode }} {{ record.materialName }}</div>
                    <a-tag v-if="!record.hasStd" color="warning">产品没有标准成本</a-tag>
                    <a-tag v-else-if="record.missingStd" color="warning">{{ record.missingStd }} 个子件没有标准成本</a-tag>
                  </template>
                  <template v-else-if="column.key === 'finishedQty'">{{ formatQty(record.finishedQty) }} {{ record.unit }}</template>
                  <template v-else-if="column.key === 'hours'">
                    {{ formatQty(record.hours) }}
                    <div class="sub-text">标准 {{ formatQty(record.stdHours) }}</div>
                  </template>
                  <template v-else-if="column.key === 'totalVariance'">
                    <b :style="varianceStyle(record.totalVariance)">{{ money(record.totalVariance) }}</b>
                  </template>
                  <template v-else-if="column.variance">
                    <span class="num" :style="varianceStyle(record[column.key])">{{ moneyOrBlank(record[column.key]) }}</span>
                  </template>
                  <template v-else-if="column.money"><span class="num">{{ moneyOrBlank(record[column.key]) }}</span></template>
                </template>
              </a-table>
            </template>
          </template>
        </a-tab-pane>
      </a-tabs>
    </a-card>
    <a-modal v-model:open="componentsOpen" :title="`材料差异 - ${componentsWo?.woNo || ''}`" width="min(1000px, 100vw)" :footer="null">
      <div class="sub-text block">
        完工 {{ formatQty(componentsWo?.finishedQty) }}；标准用量 = 完工数量 × 工单单位用量；用量差异 = (实际 - 标准) × 标准成本；价格差异 = 实际金额 - 实际用量 × 标准成本
      </div>
      <a-table row-key="materialId" size="small" :columns="componentColumns" :data-source="components" :pagination="false" :scroll="{ x: 940 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            {{ record.materialCode }} {{ record.materialName }}
            <a-tag v-if="!record.hasStd" color="warning" class="ml">无标准成本</a-tag>
          </template>
          <template v-else-if="column.qty">{{ formatQty(record[column.key]) }} <span class="sub-text">{{ record.unit }}</span></template>
          <template v-else-if="column.key === 'stdPrice'">{{ fmt(record.stdPrice, 4) }}</template>
          <template v-else-if="column.variance">
            <span class="num" :style="varianceStyle(record[column.key])">{{ moneyOrBlank(record[column.key]) }}</span>
          </template>
          <template v-else-if="column.money"><span class="num">{{ moneyOrBlank(record[column.key]) }}</span></template>
        </template>
      </a-table>
    </a-modal>
    <VoucherDetail v-model:open="voucherOpen" :voucher-id="voucherId" @changed="loadRuns" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import VoucherDetail from '@/components/fin/VoucherDetail.vue'
import { costVariance, costVarianceComponents, materialCosts, pageCostRun, runCost, undoCost, woCosts } from '@/api/fin'
import { useTable } from '@/composables/useTable'
import { formatQty } from '@/components/wms/wms'
import { currentPeriod, formatMoney, moneyOrBlank } from '@/components/fin/fin'
import { useDict } from '@/composables/useDict'

const period = ref(currentPeriod())
const runs = ref([])
const current = computed(() => runs.value.find((r) => r.period === period.value))
const isOpening = computed(() => current.value?.kind === 'OPENING')
const tab = ref('materials')
const keyword = ref('')

const fmt = (n, d) => Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: d })

const materialColumns = [
  { title: '物料', key: 'material', width: 220, fixed: 'left' },
  { title: '期初', key: 'open', qty: 'openQty', amount: 'openAmount', width: 120, align: 'right' },
  { title: '采购入库', key: 'purchase', qty: 'purchaseQty', amount: 'purchaseAmount', width: 120, align: 'right' },
  { title: '生产入库', key: 'make', qty: 'makeQty', amount: 'makeAmount', width: 120, align: 'right' },
  { title: '其他入库', key: 'otherIn', qty: 'otherInQty', amount: 'otherInAmount', width: 110, align: 'right' },
  { title: '加权单价', key: 'unitCost', width: 110, align: 'right' },
  { title: '生产领用', key: 'issue', qty: 'issueQty', amount: 'issueAmount', width: 120, align: 'right' },
  { title: '销售出库', key: 'sales', qty: 'salesQty', amount: 'salesAmount', width: 120, align: 'right' },
  { title: '其他出库', key: 'otherOut', qty: 'otherOutQty', amount: 'otherOutAmount', width: 110, align: 'right' },
  { title: '期末', key: 'close', qty: 'closeQty', amount: 'closeAmount', width: 130, align: 'right' }
]
const woColumns = [
  { title: '工单', key: 'wo', width: 240 },
  { title: '期初在产品', key: 'wipOpen', money: true, width: 120, align: 'right' },
  { title: '直接材料', key: 'materialCost', money: true, width: 120, align: 'right' },
  { title: '工时', key: 'hours', width: 80, align: 'right' },
  { title: '直接人工', key: 'laborCost', money: true, width: 110, align: 'right' },
  { title: '制造费用', key: 'overheadCost', money: true, width: 110, align: 'right' },
  { title: '完工数量', key: 'finishedQty', width: 110, align: 'right' },
  { title: '完工成本', key: 'finishedAmount', money: true, width: 120, align: 'right' },
  { title: '单位成本', key: 'unitCost', width: 110, align: 'right' },
  { title: '期末在产品', key: 'wipClose', money: true, width: 120, align: 'right' },
  { title: '关闭损失', key: 'lossAmount', money: true, width: 110, align: 'right' }
]

const materials = useTable((p) => materialCosts(period.value, { ...p, keyword: keyword.value || undefined }), reactive({}))
const wos = useTable((p) => woCosts(period.value, p), reactive({}))

async function loadRuns() {
  runs.value = (await pageCostRun({ current: 1, size: 120 })).records
}

function selectPeriod() {
  materials.search()
  wos.search()
  if (tab.value === 'variance') loadVariance()
}

// ---------------------------------------------------------------- 成本差异
useDict('mes_wo_status')
const money = (n) => formatMoney(Number(n || 0))
/** 正数为超支（不利差异）标红，负数为节约标绿 */
const varianceStyle = (n) => (Number(n) > 0 ? { color: 'var(--ant-color-error, #ff4d4f)' } : Number(n) < 0 ? { color: 'var(--ant-color-success, #52c41a)' } : {})
const varianceColumns = [
  { title: '工单', key: 'wo', width: 240, fixed: 'left' },
  { title: '完工数量', key: 'finishedQty', width: 100, align: 'right' },
  { title: '工时', key: 'hours', width: 90, align: 'right' },
  { title: '标准成本', key: 'stdTotal', money: true, width: 110, align: 'right' },
  { title: '实际成本', key: 'actualTotal', money: true, width: 110, align: 'right' },
  { title: '总差异', key: 'totalVariance', width: 110, align: 'right' },
  { title: '材料价格', key: 'materialPrice', variance: true, width: 100, align: 'right' },
  { title: '材料用量', key: 'materialUsage', variance: true, width: 100, align: 'right' },
  { title: '人工费率', key: 'laborRate', variance: true, width: 100, align: 'right' },
  { title: '人工效率', key: 'laborEfficiency', variance: true, width: 100, align: 'right' },
  { title: '费用耗费', key: 'overheadSpending', variance: true, width: 100, align: 'right' },
  { title: '费用效率', key: 'overheadEfficiency', variance: true, width: 100, align: 'right' },
  { title: '其中关闭损失', key: 'lossAmount', money: true, width: 110, align: 'right' }
]
const variance = ref()
const varianceLoading = ref(false)
async function loadVariance() {
  varianceLoading.value = true
  try {
    variance.value = await costVariance(period.value)
  } finally {
    varianceLoading.value = false
  }
}
watch(tab, (t) => t === 'variance' && loadVariance())

const componentColumns = [
  { title: '子件', key: 'material' },
  { title: '单位用量', key: 'unitQty', qty: true, width: 100, align: 'right' },
  { title: '标准用量', key: 'stdQty', qty: true, width: 110, align: 'right' },
  { title: '实际用量', key: 'actualQty', qty: true, width: 110, align: 'right' },
  { title: '标准成本', key: 'stdPrice', width: 100, align: 'right' },
  { title: '实际金额', key: 'actualAmount', money: true, width: 110, align: 'right' },
  { title: '用量差异', key: 'usageVariance', variance: true, width: 100, align: 'right' },
  { title: '价格差异', key: 'priceVariance', variance: true, width: 100, align: 'right' }
]
const componentsOpen = ref(false)
const componentsWo = ref()
const components = ref([])
async function openComponents(record) {
  componentsWo.value = record
  components.value = await costVarianceComponents(period.value, record.woId)
  componentsOpen.value = true
}

onMounted(async () => {
  await loadRuns()
  selectPeriod()
})

const running = ref(false)
function run() {
  Modal.confirm({
    title: `${current.value ? '重新核算' : '核算'} ${period.value} 期间成本？`,
    content: '按本期仓库流水、采购金额、工单领料与报工计算存货单价和产品成本，并生成成本结转凭证（未审核）。',
    async onOk() {
      running.value = true
      try {
        const res = await runCost(period.value)
        if (res.zeroCostMaterials.length) {
          Modal.warning({
            title: `核算完成，共 ${res.materialCount} 个物料`,
            content: `以下物料有出库但单价为 0（没有期初、采购或生产金额），请在「期初余额」中补录存货期初后重新核算：${res.zeroCostMaterials.join('、')}`
          })
        } else {
          message.success(`核算完成，共 ${res.materialCount} 个物料`)
        }
        await loadRuns()
        selectPeriod()
      } finally {
        running.value = false
      }
    }
  })
}

async function undo() {
  await undoCost(period.value)
  message.success('已撤销')
  await loadRuns()
  selectPeriod()
}

const voucherOpen = ref(false)
const voucherId = ref()
function openVoucher(id) {
  voucherId.value = id
  voucherOpen.value = true
}
</script>

<style scoped>
.opening-tip {
  margin: 12px 0;
}
.cost-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}
.stats {
  margin-bottom: 12px;
}
.filter {
  width: 240px;
  margin-bottom: 12px;
}
.num {
  font-variant-numeric: tabular-nums;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.ml {
  margin-left: 6px;
}
.block {
  margin-bottom: 12px;
}
.variance-head {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
</style>
