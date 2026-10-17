<template>
  <div class="cost-page">
    <a-card :bordered="false" class="page-card">
      <div class="head">
        <a-space wrap>
          <span>{{ $t('fin.cost.period') }}</span>
          <PeriodPicker v-model:value="period" @update:value="selectPeriod" />
          <a-button v-if="!isOpening" v-perm="'fin:cost:run'" type="primary" :loading="running" @click="run">
            {{ current ? $t('fin.cost.rerun') : $t('fin.cost.run') }}
          </a-button>
          <span v-if="current && !isOpening" v-perm="'fin:cost:run'">
            <a-popconfirm :title="$t('fin.cost.confirmUndo')" @confirm="undo">
              <a-button>{{ $t('fin.cost.undo') }}</a-button>
            </a-popconfirm>
          </span>
        </a-space>
        <span class="sub-text">{{ $t('fin.cost.headTip') }}</span>
      </div>
      <a-alert v-if="isOpening" type="info" show-icon class="opening-tip" :message="$t('fin.cost.openingTip')" />
      <a-row :gutter="16" class="stats">
        <a-col :xs="12" :md="5"><a-statistic :title="$t('fin.cost.status')" :value="isOpening ? $t('fin.cost.inventoryOpening') : current ? $t('fin.cost.costed') : $t('fin.cost.notCosted')" /></a-col>
        <a-col :xs="12" :md="5"><a-statistic :title="$t('fin.cost.finishedCost')" :precision="2" prefix="¥" :value="Number(current?.finishedCost || 0)" /></a-col>
        <a-col :xs="12" :md="5"><a-statistic :title="$t('fin.cost.salesCost')" :precision="2" prefix="¥" :value="Number(current?.salesCost || 0)" /></a-col>
        <a-col :xs="12" :md="5"><a-statistic :title="$t('fin.cost.wipClose')" :precision="2" prefix="¥" :value="Number(current?.wipCost || 0)" /></a-col>
        <a-col :xs="12" :md="4">
          <a-statistic :title="$t('fin.cost.overheadVariance')" :precision="2" prefix="¥" :value="Number(current?.overheadVariance || 0)" />
        </a-col>
      </a-row>
      <div v-if="current && !isOpening" class="sub-text">
        {{ $t('fin.cost.runInfo', { name: current.createByName, time: current.createTime }) }}
        <a v-if="current.voucherId" @click="openVoucher(current.voucherId)">{{ current.voucherNo }}</a>
        <span v-else>{{ $t('common.status.none') }}</span>
        <DictTag v-if="current.voucherStatus" type="fin_voucher_status" :value="current.voucherStatus" class="ml" />
      </div>
    </a-card>

    <a-card :bordered="false" class="page-card">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="materials" :tab="$t('fin.cost.materialsTab')">
          <a-input-search v-model:value="keyword" :placeholder="$t('fin.cost.materialKeyword')" allow-clear class="filter" @search="materials.search" />
          <a-table
            row-key="id"
            size="small"
            bordered
            :columns="materialColumns"
            :data-source="materials.list.value"
            :loading="materials.loading.value"
            :pagination="materials.pagination"
            :scroll="{ x: 1700 }"
            :locale="{ emptyText: current ? $t('common.msg.noData') : $t('fin.cost.periodNotCosted') }"
            @change="materials.onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'material'">
                <div>{{ record.materialCode }} {{ record.materialName }}</div>
                <div class="sub-text">{{ record.spec }}</div>
              </template>
              <template v-else-if="column.key === 'unitCost'">
                <div>{{ fmt(record.unitCost, 4) }}</div>
                <div class="sub-text"><DictTag type="wms_cost_method" :value="record.costMethod" /></div>
              </template>
              <template v-else-if="column.qty">
                <div class="num">{{ formatQty(record[column.qty]) === '0' ? '' : formatQty(record[column.qty]) }}</div>
                <div class="num sub-text">{{ moneyOrBlank(record[column.amount]) }}</div>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="wo" :tab="$t('fin.cost.woTab')">
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
        <a-tab-pane key="variance" :tab="$t('fin.cost.varianceTab')">
          <template v-if="variance">
            <a-alert v-if="!variance.versionId" type="info" show-icon class="opening-tip"
              :message="$t('fin.cost.noStdVersion')" />
            <template v-else>
              <div class="variance-head">
                <span>{{ $t('fin.cost.stdVersion', { name: variance.versionName }) }}</span>
                <span class="sub-text">{{ $t('fin.cost.varianceTip') }}</span>
              </div>
              <a-alert v-if="!variance.costRun" type="warning" show-icon class="opening-tip" :message="$t('fin.cost.notCostedWarn')" />
              <a-row :gutter="16" class="stats">
                <a-col :xs="12" :md="4"><a-statistic :title="$t('fin.cost.actualCost')" :precision="2" :value="Number(variance.total.actualTotal)" /></a-col>
                <a-col :xs="12" :md="4"><a-statistic :title="$t('fin.cost.stdCost')" :precision="2" :value="Number(variance.total.stdTotal)" /></a-col>
                <a-col :xs="12" :md="4">
                  <a-statistic :title="$t('fin.cost.totalVariance')" :precision="2" :value="Number(variance.total.totalVariance)" :value-style="varianceStyle(variance.total.totalVariance)" />
                </a-col>
                <a-col :xs="12" :md="4"><a-statistic :title="$t('fin.cost.materialVariance')" :value="`${money(variance.total.materialPrice)} / ${money(variance.total.materialUsage)}`" /></a-col>
                <a-col :xs="12" :md="4"><a-statistic :title="$t('fin.cost.laborVariance')" :value="`${money(variance.total.laborRate)} / ${money(variance.total.laborEfficiency)}`" /></a-col>
                <a-col :xs="12" :md="4">
                  <a-statistic :title="$t('fin.cost.overheadVarianceDetail')" :value="`${money(variance.total.overheadSpending)} / ${money(variance.total.overheadEfficiency)}`" />
                </a-col>
              </a-row>
              <a-table
                row-key="woId"
                size="small"
                bordered
                :columns="varianceColumns"
                :data-source="variance.orders"
                :loading="varianceLoading"
                :pagination="{ pageSize: 20, showSizeChanger: false, showTotal: (n) => $t('fin.cost.totalWos', { n }) }"
                :scroll="{ x: 1500 }"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'wo'">
                    <a @click="openComponents(record)">{{ record.woNo }}</a>
                    <DictTag type="mes_wo_status" :value="record.status" class="ml" />
                    <div class="sub-text">{{ record.materialCode }} {{ record.materialName }}</div>
                    <a-tag v-if="!record.hasStd" color="warning">{{ $t('fin.cost.productNoStd') }}</a-tag>
                    <a-tag v-else-if="record.missingStd" color="warning">{{ $t('fin.cost.missingStd', { n: record.missingStd }) }}</a-tag>
                  </template>
                  <template v-else-if="column.key === 'finishedQty'">{{ formatQty(record.finishedQty) }} {{ record.unit }}</template>
                  <template v-else-if="column.key === 'hours'">
                    {{ formatQty(record.hours) }}
                    <div class="sub-text">{{ $t('fin.cost.stdHoursSub', { n: formatQty(record.stdHours) }) }}</div>
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
    <a-modal v-model:open="componentsOpen" :title="$t('fin.cost.componentsTitle', { no: componentsWo?.woNo || '' })" width="min(1000px, 100vw)" :footer="null">
      <div class="sub-text block">
        {{ $t('fin.cost.componentsTip', { qty: formatQty(componentsWo?.finishedQty) }) }}
      </div>
      <a-table row-key="materialId" size="small" :columns="componentColumns" :data-source="components" :pagination="false" :scroll="{ x: 940 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            {{ record.materialCode }} {{ record.materialName }}
            <a-tag v-if="!record.hasStd" color="warning" class="ml">{{ $t('fin.cost.noStd') }}</a-tag>
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
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const period = ref(currentPeriod())
const runs = ref([])
const current = computed(() => runs.value.find((r) => r.period === period.value))
const isOpening = computed(() => current.value?.kind === 'OPENING')
const tab = ref('materials')
const keyword = ref('')

const fmt = (n, d) => Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: d })

const materialColumns = [
  { title: t('fin.cost.material'), key: 'material', width: 220, fixed: 'left' },
  { title: t('fin.cost.opening'), key: 'open', qty: 'openQty', amount: 'openAmount', width: 120, align: 'right' },
  { title: t('fin.cost.purchaseIn'), key: 'purchase', qty: 'purchaseQty', amount: 'purchaseAmount', width: 120, align: 'right' },
  { title: t('fin.cost.makeIn'), key: 'make', qty: 'makeQty', amount: 'makeAmount', width: 120, align: 'right' },
  { title: t('fin.cost.otherIn'), key: 'otherIn', qty: 'otherInQty', amount: 'otherInAmount', width: 120, align: 'right' },
  { title: t('fin.cost.outUnitCost'), key: 'unitCost', width: 150, align: 'right' },
  { title: t('fin.cost.issue'), key: 'issue', qty: 'issueQty', amount: 'issueAmount', width: 120, align: 'right' },
  { title: t('fin.cost.salesOut'), key: 'sales', qty: 'salesQty', amount: 'salesAmount', width: 120, align: 'right' },
  { title: t('fin.cost.otherOut'), key: 'otherOut', qty: 'otherOutQty', amount: 'otherOutAmount', width: 120, align: 'right' },
  { title: t('fin.cost.closing'), key: 'close', qty: 'closeQty', amount: 'closeAmount', width: 130, align: 'right' }
]
const woColumns = [
  { title: t('fin.cost.wo'), key: 'wo', width: 240 },
  { title: t('fin.cost.wipOpen'), key: 'wipOpen', money: true, width: 130, align: 'right' },
  { title: t('fin.cost.directMaterial'), key: 'materialCost', money: true, width: 130, align: 'right' },
  { title: t('fin.cost.hours'), key: 'hours', width: 80, align: 'right' },
  { title: t('fin.cost.directLabor'), key: 'laborCost', money: true, width: 120, align: 'right' },
  { title: t('fin.cost.overhead'), key: 'overheadCost', money: true, width: 120, align: 'right' },
  { title: t('fin.cost.finishedQty'), key: 'finishedQty', width: 120, align: 'right' },
  { title: t('fin.cost.finishedAmount'), key: 'finishedAmount', money: true, width: 130, align: 'right' },
  { title: t('fin.cost.unitCost'), key: 'unitCost', width: 110, align: 'right' },
  { title: t('fin.cost.wipClose'), key: 'wipClose', money: true, width: 130, align: 'right' },
  { title: t('fin.cost.closeLoss'), key: 'lossAmount', money: true, width: 120, align: 'right' }
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
  { title: t('fin.cost.wo'), key: 'wo', width: 240, fixed: 'left' },
  { title: t('fin.cost.finishedQty'), key: 'finishedQty', width: 120, align: 'right' },
  { title: t('fin.cost.hours'), key: 'hours', width: 100, align: 'right' },
  { title: t('fin.cost.stdCost'), key: 'stdTotal', money: true, width: 120, align: 'right' },
  { title: t('fin.cost.actualCost'), key: 'actualTotal', money: true, width: 120, align: 'right' },
  { title: t('fin.cost.totalVariance'), key: 'totalVariance', width: 120, align: 'right' },
  { title: t('fin.cost.materialPrice'), key: 'materialPrice', variance: true, width: 120, align: 'right' },
  { title: t('fin.cost.materialUsage'), key: 'materialUsage', variance: true, width: 120, align: 'right' },
  { title: t('fin.cost.laborRate'), key: 'laborRate', variance: true, width: 110, align: 'right' },
  { title: t('fin.cost.laborEfficiency'), key: 'laborEfficiency', variance: true, width: 130, align: 'right' },
  { title: t('fin.cost.overheadSpending'), key: 'overheadSpending', variance: true, width: 140, align: 'right' },
  { title: t('fin.cost.overheadEfficiency'), key: 'overheadEfficiency', variance: true, width: 140, align: 'right' },
  { title: t('fin.cost.ofWhichCloseLoss'), key: 'lossAmount', money: true, width: 150, align: 'right' }
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
  { title: t('fin.cost.component'), key: 'material' },
  { title: t('fin.cost.unitQty'), key: 'unitQty', qty: true, width: 100, align: 'right' },
  { title: t('fin.cost.stdQty'), key: 'stdQty', qty: true, width: 110, align: 'right' },
  { title: t('fin.cost.actualQty'), key: 'actualQty', qty: true, width: 110, align: 'right' },
  { title: t('fin.cost.stdCost'), key: 'stdPrice', width: 120, align: 'right' },
  { title: t('fin.cost.actualAmount'), key: 'actualAmount', money: true, width: 120, align: 'right' },
  { title: t('fin.cost.usageVariance'), key: 'usageVariance', variance: true, width: 130, align: 'right' },
  { title: t('fin.cost.priceVariance'), key: 'priceVariance', variance: true, width: 120, align: 'right' }
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
    title: current.value ? t('fin.cost.confirmRerun', { period: period.value }) : t('fin.cost.confirmRun', { period: period.value }),
    content: t('fin.cost.confirmRunContent'),
    async onOk() {
      running.value = true
      try {
        const res = await runCost(period.value)
        if (res.zeroCostMaterials.length) {
          Modal.warning({
            title: t('fin.cost.runDone', { n: res.materialCount }),
            content: t('fin.cost.zeroCostWarn', { list: res.zeroCostMaterials.join(t('fin.cost.sep')) })
          })
        } else {
          message.success(t('fin.cost.runDone', { n: res.materialCount }))
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
  message.success(t('fin.cost.undone'))
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
