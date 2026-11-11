<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="margin" :tab="$t('fin.analysis.margin')">
          <GrossMarginPanel />
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
            <MaterialCategorySelect v-model="age.category" :placeholder="$t('fin.analysis.allCategories')" class="filter-s" />
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
import MaterialCategorySelect from '@/components/wms/MaterialCategorySelect.vue'
import { computed, reactive, ref, watch } from 'vue'
import GrossMarginPanel from '@/components/fin/GrossMarginPanel.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import { inventoryCheck, stockAge } from '@/api/fin'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { useDict } from '@/composables/useDict'
import { formatQty, today } from '@/components/wms/wms'
import { currentPeriod, formatMoney } from '@/components/fin/fin'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const tab = ref('margin')

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
