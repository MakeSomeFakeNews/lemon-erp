<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="margin" tab="销售毛利">
          <div class="filters">
            <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" @change="loadMargin" />
            <a-radio-group v-model:value="groupBy" button-style="solid" @change="loadMargin">
              <a-radio-button value="MATERIAL">按物料</a-radio-button>
              <a-radio-button value="CUSTOMER">按客户</a-radio-button>
              <a-radio-button value="MONTH">按月份</a-radio-button>
            </a-radio-group>
            <PartnerSelect v-model:value="customerId" type="customer" allow-clear placeholder="全部客户" class="filter" />
          </div>
          <a-alert
            type="info"
            show-icon
            class="tip"
            message="收入按销售出库（扣除退货）的不含税金额；成本按出库当期成本核算的加权平均单价，当期还未核算成本的物料取物料的参考成本价。"
          />
          <a-row :gutter="12" class="cards">
            <a-col :xs="12" :md="6">
              <a-statistic title="销售收入" :value="Number(margin.total?.revenue || 0)" :precision="2" prefix="¥" />
            </a-col>
            <a-col :xs="12" :md="6">
              <a-statistic title="销售成本" :value="Number(margin.total?.cost || 0)" :precision="2" prefix="¥" />
            </a-col>
            <a-col :xs="12" :md="6">
              <a-statistic title="毛利" :value="Number(margin.total?.profit || 0)" :precision="2" prefix="¥"
                :value-style="{ color: Number(margin.total?.profit) < 0 ? '#cf1322' : undefined }" />
            </a-col>
            <a-col :xs="12" :md="6">
              <a-statistic title="毛利率" :value="margin.total?.margin ?? '-'" :precision="margin.total?.margin == null ? undefined : 2"
                :suffix="margin.total?.margin == null ? '' : '%'" />
            </a-col>
          </a-row>
          <a-table
            :row-key="(r) => r.groupKey ?? r.groupName"
            size="middle"
            :columns="marginColumns"
            :data-source="margin.rows"
            :loading="marginLoading"
            :pagination="margin.rows?.length > 20 ? { pageSize: 20, showTotal: (t) => `共 ${t} 条` } : false"
            :scroll="{ x: 1000 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'group'">
                {{ groupBy === 'MONTH' ? periodText(record.groupKey) : record.groupName || '-' }}
              </template>
              <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }}</template>
              <template v-else-if="column.key === 'cost'">
                {{ formatMoney(record.cost) }}
                <a-tooltip v-if="Number(record.missingCost) > 0" :title="`${record.missingCost} 行出库所在期间还未核算成本，按物料的参考成本价计算`">
                  <a-tag color="orange" :bordered="false" class="hint">含参考成本</a-tag>
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

        <a-tab-pane key="inventory" tab="存货对账">
          <div class="filters">
            <span>期间</span>
            <PeriodPicker v-model:value="period" @update:value="loadInventory" />
            <span class="sub-text">总账余额与成本核算期末金额核对；数量差异为成本核算期末数量与截至期末的库存流水不一致的物料</span>
          </div>
          <a-spin :spinning="invLoading">
            <div class="section-title">科目余额核对</div>
            <a-table row-key="code" size="middle" :columns="accountColumns" :data-source="inventory.accounts" :pagination="false" class="block">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'account'">{{ record.code }} {{ record.name }}</template>
                <template v-else-if="column.key === 'glBalance'">{{ formatMoney(record.glBalance) }}</template>
                <template v-else-if="column.key === 'costAmount'">
                  <span v-if="record.costAmount != null">{{ formatMoney(record.costAmount) }}</span>
                  <span v-else class="sub-text">不参与成本核算</span>
                </template>
                <template v-else-if="column.key === 'diff'">
                  <template v-if="record.diff != null">
                    <span v-if="Math.abs(Number(record.diff)) >= 0.005" class="neg bold">{{ formatMoney(record.diff) }}</span>
                    <a-tag v-else color="green" :bordered="false">一致</a-tag>
                  </template>
                  <span v-else class="sub-text">-</span>
                </template>
              </template>
            </a-table>
            <div class="section-title">
              数量差异
              <a-tag v-if="inventory.period" :color="inventory.qtyDiffs?.length ? 'red' : 'green'" :bordered="false">
                {{ inventory.qtyDiffs?.length ? `${inventory.qtyDiffs.length} 种物料不一致` : '全部一致' }}
              </a-tag>
            </div>
            <a-table
              row-key="materialId"
              size="middle"
              :columns="qtyColumns"
              :data-source="inventory.qtyDiffs"
              :pagination="inventory.qtyDiffs?.length > 20 ? { pageSize: 20 } : false"
              :locale="{ emptyText: '成本核算数量与库存流水一致（或本期还未核算成本）' }"
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
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import dayjs from 'dayjs'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import { grossMargin, inventoryCheck } from '@/api/fin'
import { formatQty, today } from '@/components/wms/wms'
import { currentPeriod, formatMoney, periodText } from '@/components/fin/fin'

const tab = ref('margin')

// ---------------------------------------------------------------- 销售毛利
const range = ref([dayjs().startOf('year').format('YYYY-MM-DD'), today()])
const groupBy = ref('MATERIAL')
const customerId = ref()
const margin = ref({ rows: [], total: {} })
const marginLoading = ref(false)
const GROUP_TITLES = { MATERIAL: '物料', CUSTOMER: '客户', MONTH: '月份' }
const marginColumns = computed(() => [
  { title: GROUP_TITLES[groupBy.value], key: 'group', width: 240 },
  ...(groupBy.value === 'MATERIAL' ? [{ title: '销量', key: 'qty', width: 110, align: 'right' }] : []),
  { title: '收入', key: 'revenue', width: 140, align: 'right' },
  { title: '成本', key: 'cost', width: 200, align: 'right' },
  { title: '毛利', key: 'profit', width: 140, align: 'right' },
  { title: '毛利率', key: 'margin', width: 100, align: 'right' }
])
let marginSeq = 0
async function loadMargin() {
  const current = ++marginSeq
  marginLoading.value = true
  try {
    const res = await grossMargin({ begin: range.value[0], end: range.value[1], groupBy: groupBy.value, customerId: customerId.value ?? undefined })
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
  { title: '科目', key: 'account', width: 220 },
  { title: '总账余额', key: 'glBalance', width: 160, align: 'right' },
  { title: '成本核算期末金额', key: 'costAmount', width: 180, align: 'right' },
  { title: '差异', key: 'diff', width: 160, align: 'right' }
]
const qtyColumns = [
  { title: '物料', key: 'material', width: 260 },
  { title: '成本核算期末数量', key: 'costQty', width: 160, align: 'right' },
  { title: '库存流水结存', key: 'stockQty', width: 160, align: 'right' },
  { title: '差异', key: 'diff', width: 140, align: 'right' }
]
async function loadInventory() {
  invLoading.value = true
  try {
    inventory.value = await inventoryCheck(period.value)
  } finally {
    invLoading.value = false
  }
}
watch(tab, (t) => t === 'inventory' && !inventory.value.period && loadInventory())
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
</style>
