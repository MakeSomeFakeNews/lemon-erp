<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="margin" :tab="$t('fin.analysis.margin')">
          <div class="filters">
            <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" @change="loadMargin" />
            <a-radio-group v-model:value="groupBy" button-style="solid" @change="loadMargin">
              <a-radio-button value="MATERIAL">{{ $t('fin.analysis.byMaterial') }}</a-radio-button>
              <a-radio-button value="CUSTOMER">{{ $t('fin.analysis.byCustomer') }}</a-radio-button>
              <a-radio-button value="MONTH">{{ $t('fin.analysis.byMonth') }}</a-radio-button>
              <a-radio-button value="DETAIL">{{ $t('fin.analysis.byDetail') }}</a-radio-button>
            </a-radio-group>
            <PartnerSelect v-model:value="customerId" type="customer" allow-clear :placeholder="$t('fin.analysis.allCustomers')" class="filter" />
            <a-input-search v-if="groupBy === DETAIL" v-model:value="keyword" allow-clear :placeholder="$t('fin.analysis.detailKeyword')" class="filter" @search="loadMargin" />
          </div>
          <a-alert
            type="info"
            show-icon
            class="tip"
            :message="$t('fin.analysis.marginTip')"
          />
          <a-row :gutter="12" class="cards">
            <a-col :xs="12" :md="6">
              <a-statistic :title="$t('fin.analysis.salesRevenue')" :value="Number(margin.total?.revenue || 0)" :precision="2" prefix="¥" />
            </a-col>
            <a-col :xs="12" :md="6">
              <a-statistic :title="$t('fin.analysis.salesCost')" :value="Number(margin.total?.cost || 0)" :precision="2" prefix="¥" />
            </a-col>
            <a-col :xs="12" :md="6">
              <a-statistic :title="$t('fin.analysis.grossProfit')" :value="Number(margin.total?.profit || 0)" :precision="2" prefix="¥"
                :value-style="{ color: Number(margin.total?.profit) < 0 ? '#cf1322' : undefined }" />
            </a-col>
            <a-col :xs="12" :md="6">
              <a-statistic :title="$t('fin.analysis.grossMargin')" :value="margin.total?.margin ?? '-'" :precision="margin.total?.margin == null ? undefined : 2"
                :suffix="margin.total?.margin == null ? '' : '%'" />
            </a-col>
          </a-row>
          <a-alert v-if="margin.truncated" type="warning" show-icon class="tip" :message="$t('fin.analysis.truncated')" />
          <a-table
            :row-key="(r) => (groupBy === DETAIL ? `${r.docId}-${r.materialCode}-${r.qty}-${r.revenue}` : r.groupKey ?? r.groupName)"
            size="middle"
            :columns="marginColumns"
            :data-source="margin.rows"
            :loading="marginLoading"
            :pagination="margin.rows?.length > 20 ? { pageSize: 20, showTotal: (n) => $t('common.table.total', { total: n }) } : false"
            :scroll="{ x: 1000 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'docNo'">
                {{ record.docNo }} <DictTag type="scm_trade_kind" :value="record.kind" />
              </template>
              <template v-else-if="column.key === 'material'">
                {{ record.materialCode }} {{ record.materialName }}<span v-if="record.spec" class="sub-text"> {{ record.spec }}</span>
              </template>
              <template v-else-if="column.key === 'group'">
                {{ groupBy === MarginGroup.MONTH ? periodText(record.groupKey) : record.groupName || '-' }}
              </template>
              <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }}</template>
              <template v-else-if="column.key === 'cost'">
                {{ formatMoney(record.cost) }}
                <a-tooltip v-if="groupBy === DETAIL && !Number(record.costed)" :title="$t('fin.analysis.refCostTip')">
                  <a-tag color="orange" :bordered="false" class="hint">{{ $t('fin.analysis.refCost') }}</a-tag>
                </a-tooltip>
                <a-tooltip v-if="Number(record.missingCost) > 0" :title="$t('fin.analysis.missingCostTip', { n: record.missingCost })">
                  <a-tag color="orange" :bordered="false" class="hint">{{ $t('fin.analysis.hasRefCost') }}</a-tag>
                </a-tooltip>
              </template>
              <template v-else-if="['revenue', 'profit'].includes(column.key)">
                <span :class="{ neg: Number(record[column.key]) < 0 }">{{ formatMoney(record[column.key]) }}</span>
              </template>
              <template v-else-if="column.key === 'margin'">
                <span v-if="record.margin != null" :class="{ neg: Number(record.margin) < 0 }">{{ Number(record.margin).toFixed(2) }}%</span>
                <span v-else class="sub-text">-</span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="inventory" :tab="$t('fin.analysis.inventory')">
          <div class="filters">
            <span>{{ $t('fin.analysis.period') }}</span>
            <PeriodPicker v-model:value="period" @update:value="loadInventory" />
            <span class="sub-text">{{ $t('fin.analysis.inventoryTip') }}</span>
          </div>
          <a-spin :spinning="invLoading">
            <div class="section-title">{{ $t('fin.analysis.accountCheck') }}</div>
            <a-table row-key="code" size="middle" :columns="accountColumns" :data-source="inventory.accounts" :pagination="false" class="block">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'account'">{{ record.code }} {{ record.name }}</template>
                <template v-else-if="column.key === 'glBalance'">{{ formatMoney(record.glBalance) }}</template>
                <template v-else-if="column.key === 'costAmount'">
                  <span v-if="record.costAmount != null">{{ formatMoney(record.costAmount) }}</span>
                  <span v-else class="sub-text">{{ $t('fin.analysis.notInCost') }}</span>
                </template>
                <template v-else-if="column.key === 'diff'">
                  <template v-if="record.diff != null">
                    <span v-if="Math.abs(Number(record.diff)) >= 0.005" class="neg bold">{{ formatMoney(record.diff) }}</span>
                    <a-tag v-else color="green" :bordered="false">{{ $t('fin.analysis.match') }}</a-tag>
                  </template>
                  <span v-else class="sub-text">-</span>
                </template>
              </template>
            </a-table>
            <div class="section-title">
              {{ $t('fin.analysis.qtyDiff') }}
              <a-tag v-if="inventory.period" :color="inventory.qtyDiffs?.length ? 'red' : 'green'" :bordered="false">
                {{ inventory.qtyDiffs?.length ? $t('fin.analysis.nMismatch', { n: inventory.qtyDiffs.length }) : $t('fin.analysis.allMatch') }}
              </a-tag>
            </div>
            <a-table
              row-key="materialId"
              size="middle"
              :columns="qtyColumns"
              :data-source="inventory.qtyDiffs"
              :pagination="inventory.qtyDiffs?.length > 20 ? { pageSize: 20 } : false"
              :locale="{ emptyText: $t('fin.analysis.qtyEmpty') }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'material'">{{ record.code }} {{ record.name }}</template>
                <template v-else-if="column.key === 'costQty'">{{ formatQty(record.costQty) }}</template>
                <template v-else-if="column.key === 'stockQty'">{{ formatQty(record.stockQty) }}</template>
                <template v-else-if="column.key === 'diff'">
                  <span class="neg">{{ formatQty(Number(record.costQty || 0) - Number(record.stockQty || 0)) }}</span>
                </template>
              </template>
            </a-table>
          </a-spin>
        </a-tab-pane>
        <a-tab-pane key="age" :tab="$t('fin.analysis.age')">
          <div class="filters">
            <span>{{ $t('fin.analysis.asOf') }}</span>
            <a-date-picker v-model:value="age.asOf" value-format="YYYY-MM-DD" :allow-clear="false" />
            <WarehouseSelect v-model:value="age.warehouseId" :placeholder="$t('fin.analysis.allWarehouses')" class="filter" />
            <a-select v-model:value="age.category" :options="toOptions(dict.wms_material_category)" allow-clear :placeholder="$t('fin.analysis.allCategories')" class="filter-s" />
            <a-input v-model:value="age.keyword" :placeholder="$t('fin.analysis.materialKeyword')" allow-clear class="filter" />
            <a-tooltip :title="$t('fin.analysis.bucketsTip')">
              <a-input v-model:value="age.buckets" placeholder="30,60,90,180,365" class="filter-s" />
            </a-tooltip>
            <a-button type="primary" :loading="age.loading" @click="loadAge">{{ $t('common.action.query') }}</a-button>
          </div>
          <div class="sub-text block-tip">
            {{ $t('fin.analysis.ageTip') }}
          </div>
          <div v-if="age.result" class="age-summary">
            <div v-for="(b, i) in age.result.buckets" :key="i" class="age-card" :class="{ old: i >= age.result.buckets.length - 2 }">
              <div class="sub-text">{{ b.label }}</div>
              <b>¥{{ formatMoney(age.result.total.ages[i].amount) }}</b>
              <div class="sub-text">{{ percent(age.result.total.ages[i].amount, age.result.total.amount) }}</div>
            </div>
            <div class="age-card total">
              <div class="sub-text">{{ $t('common.field.total') }}</div>
              <b>¥{{ formatMoney(age.result.total.amount) }}</b>
              <div class="sub-text">{{ $t('fin.analysis.nMaterials', { n: age.result.rows.length }) }}</div>
            </div>
          </div>
          <a-table
            row-key="materialId"
            size="small"
            bordered
            :columns="ageColumns"
            :data-source="age.result?.rows || []"
            :loading="age.loading"
            :pagination="{ pageSize: 50, showSizeChanger: false, showTotal: (n) => $t('fin.analysis.totalMaterials', { n }) }"
            :scroll="{ x: 700 + ageColumns.length * 110 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'material'">
                <div>{{ record.code }} {{ record.name }}</div>
                <div class="sub-text">{{ record.spec }}</div>
              </template>
              <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
              <template v-else-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template>
              <template v-else-if="column.ageIndex != null">
                <template v-if="Number(record.ages[column.ageIndex].qty)">
                  <div>{{ formatQty(record.ages[column.ageIndex].qty) }}</div>
                  <div class="sub-text">{{ formatMoney(record.ages[column.ageIndex].amount) }}</div>
                </template>
              </template>
              <template v-else-if="column.key === 'oldestDate'">{{ record.oldestDate || $t('fin.analysis.beforeRecords') }}</template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import dayjs from 'dayjs'
import DictTag from '@/components/DictTag.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import { grossMargin, grossMarginDetail, inventoryCheck, stockAge } from '@/api/fin'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { formatQty, today } from '@/components/wms/wms'
import { currentPeriod, formatMoney, periodText } from '@/components/fin/fin'
import { useI18n } from 'vue-i18n'
import { MarginGroup } from '@/constants/enums'

useDict('scm_trade_kind')
const { t } = useI18n()
const tab = ref('margin')

// ---------------------------------------------------------------- 销售毛利
const range = ref([dayjs().startOf('year').format('YYYY-MM-DD'), today()])
/** 按明细查看：前端模式（走明细接口），不是后端 MarginGroup 的取值 */
const DETAIL = 'DETAIL'
const groupBy = ref(MarginGroup.MATERIAL)
const customerId = ref()
const margin = ref({ rows: [], total: {} })
const marginLoading = ref(false)
const GROUP_TITLES = { MATERIAL: t('fin.analysis.material'), CUSTOMER: t('fin.analysis.customer'), MONTH: t('fin.analysis.month') }
const keyword = ref('')
const DETAIL_COLUMNS = [
  { title: t('common.field.date'), dataIndex: 'bizDate', width: 110 },
  { title: t('fin.analysis.doc'), key: 'docNo', width: 200 },
  { title: t('fin.analysis.customer'), dataIndex: 'customerName', width: 180, ellipsis: true },
  { title: t('fin.analysis.material'), key: 'material', width: 260, ellipsis: true },
  { title: t('common.field.quantity'), key: 'qty', width: 100, align: 'right' },
  { title: t('common.field.unit'), dataIndex: 'unit', width: 70 },
  { title: t('fin.analysis.revenue'), key: 'revenue', width: 130, align: 'right' },
  { title: t('fin.analysis.cost'), key: 'cost', width: 190, align: 'right' },
  { title: t('fin.analysis.grossProfit'), key: 'profit', width: 130, align: 'right' },
  { title: t('fin.analysis.grossMargin'), key: 'margin', width: 120, align: 'right' }
]
const marginColumns = computed(() => groupBy.value === DETAIL ? DETAIL_COLUMNS : [
  { title: GROUP_TITLES[groupBy.value], key: 'group', width: 240 },
  ...(groupBy.value === MarginGroup.MATERIAL ? [{ title: t('fin.analysis.salesQty'), key: 'qty', width: 110, align: 'right' }] : []),
  { title: t('fin.analysis.revenue'), key: 'revenue', width: 140, align: 'right' },
  { title: t('fin.analysis.cost'), key: 'cost', width: 200, align: 'right' },
  { title: t('fin.analysis.grossProfit'), key: 'profit', width: 140, align: 'right' },
  { title: t('fin.analysis.grossMargin'), key: 'margin', width: 120, align: 'right' }
])
let marginSeq = 0
async function loadMargin() {
  const current = ++marginSeq
  marginLoading.value = true
  try {
    const params = { begin: range.value[0], end: range.value[1], customerId: customerId.value ?? undefined }
    const res = groupBy.value === DETAIL
      ? await grossMarginDetail({ ...params, keyword: keyword.value || undefined })
      : await grossMargin({ ...params, groupBy: groupBy.value })
    if (current === marginSeq) margin.value = res
  } finally {
    if (current === marginSeq) marginLoading.value = false
  }
}
watch(customerId, loadMargin)
onMounted(loadMargin)

// ---------------------------------------------------------------- 存货对账
const period = ref(currentPeriod())
const inventory = ref({ accounts: [], qtyDiffs: [] })
const invLoading = ref(false)
const accountColumns = [
  { title: t('fin.analysis.account'), key: 'account', width: 220 },
  { title: t('fin.analysis.glBalance'), key: 'glBalance', width: 160, align: 'right' },
  { title: t('fin.analysis.costAmount'), key: 'costAmount', width: 220, align: 'right' },
  { title: t('fin.analysis.diff'), key: 'diff', width: 160, align: 'right' }
]
const qtyColumns = [
  { title: t('fin.analysis.material'), key: 'material', width: 260 },
  { title: t('fin.analysis.costQty'), key: 'costQty', width: 200, align: 'right' },
  { title: t('fin.analysis.stockQty'), key: 'stockQty', width: 180, align: 'right' },
  { title: t('fin.analysis.diff'), key: 'diff', width: 140, align: 'right' }
]
async function loadInventory() {
  invLoading.value = true
  try {
    inventory.value = await inventoryCheck(period.value)
  } finally {
    invLoading.value = false
  }
}
watch(tab, (t) => {
  if (t === 'inventory' && !inventory.value.period) loadInventory()
  if (t === 'age' && !age.result) loadAge()
})

// ---------------------------------------------------------------- 库龄分析
const dict = useDict('wms_material_category')
const age = reactive({ asOf: today(), warehouseId: undefined, category: undefined, keyword: '', buckets: '', loading: false, result: null })
const ageColumns = computed(() => [
  { title: t('fin.analysis.material'), key: 'material', width: 240, fixed: 'left' },
  { title: t('fin.analysis.balanceQty'), key: 'qty', width: 130, align: 'right' },
  { title: t('fin.analysis.balanceAmount'), key: 'amount', width: 140, align: 'right' },
  ...(age.result?.buckets || []).map((b, i) => ({ title: b.label, key: `age${i}`, ageIndex: i, width: 110, align: 'right' })),
  { title: t('fin.analysis.oldestDate'), key: 'oldestDate', width: 140 }
])
const percent = (part, all) => (Number(all) ? `${((Number(part) / Number(all)) * 100).toFixed(1)}%` : '-')
async function loadAge() {
  age.loading = true
  try {
    age.result = await stockAge({
      asOf: age.asOf,
      warehouseId: age.warehouseId || undefined,
      category: age.category || undefined,
      keyword: age.keyword?.trim() || undefined,
      buckets: age.buckets?.trim() || undefined
    })
  } finally {
    age.loading = false
  }
}
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.filter {
  width: 240px;
}
.tip {
  margin-bottom: 12px;
}
.cards {
  margin-bottom: 16px;
}
.section-title {
  font-weight: 500;
  margin: 8px 0;
}
.block {
  margin-bottom: 16px;
}
.hint {
  margin-left: 6px;
  margin-right: 0;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.neg {
  color: #cf1322;
}
.bold {
  font-weight: 600;
}
.block-tip {
  margin-bottom: 12px;
}
.age-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.age-card {
  min-width: 130px;
  padding: 10px 14px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
}
.age-card.old b {
  color: var(--status-critical);
}
.age-card.total {
  background: var(--app-primary-bg);
}
.filter-s {
  width: 150px;
}
</style>
