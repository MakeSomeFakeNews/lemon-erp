<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <div class="filters">
        <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" @change="load" />
        <PartnerSelect
          v-model:value="query.partnerId"
          :type="sales ? 'customer' : 'supplier'"
          allow-clear
          :placeholder="$t(sales ? 'jxc.common.allCustomers' : 'jxc.common.allSuppliers')"
          class="filter"
        />
        <IdentityUserSelect v-if="sales" v-model:value="query.salesmanId" :placeholder="$t('jxc.common.allSalesmen')" class="filter-s" />
        <MaterialCategorySelect v-model="query.category" :placeholder="$t('jxc.common.allCategories')" class="filter-s" />
        <a-select v-model:value="query.brand" :options="toOptions(dict.wms_material_brand)" allow-clear
          :placeholder="$t('jxc.common.allBrands')" class="filter-s" />
        <a-select v-if="sales" v-model:value="query.region" :options="toOptions(dict.scm_region)" allow-clear show-search option-filter-prop="label"
          :placeholder="$t('jxc.common.allRegions')" class="filter-s" />
        <WarehouseSelect v-model:value="query.warehouseId" :placeholder="$t('jxc.common.allWarehouses')" class="filter-s" />
        <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('jxc.common.materialKeyword')" class="filter" @press-enter="load" />
        <a-button type="primary" :loading="loading" @click="load">{{ $t('common.action.query') }}</a-button>
      </div>
      <div class="dims">
        <span class="sub-text">{{ $t('jxc.common.groupBy') }}</span>
        <a-radio-group v-model:value="groupBy" button-style="solid" size="small" @change="load">
          <a-radio-button v-for="d in dims" :key="d" :value="d">{{ $t(`jxc.dim.${d}`) }}</a-radio-button>
        </a-radio-group>
      </div>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <div class="head">
        <div class="kpis">
          <div class="kpi">
            <div class="sub-text">{{ $t('jxc.field.netAmount') }}</div>
            <b>¥{{ money(total.netAmount) }}</b>
          </div>
          <div class="kpi">
            <div class="sub-text">{{ $t('jxc.field.taxedAmount') }}</div>
            <b>¥{{ money(total.taxedAmount) }}</b>
          </div>
          <template v-if="sales">
            <div class="kpi">
              <div class="sub-text">{{ $t('jxc.field.profit') }}</div>
              <b :class="{ neg: Number(total.profit) < 0 }">¥{{ money(total.profit) }}</b>
            </div>
            <div class="kpi">
              <div class="sub-text">{{ $t('jxc.field.margin') }}</div>
              <b>{{ marginText(total.margin) }}</b>
            </div>
            <a-tooltip v-if="Number(total.badDebt) || Number(total.freight)" :title="$t('jxc.common.profitAfterTip')">
              <div class="kpi">
                <div class="sub-text">{{ $t('jxc.field.profitAfterFreight') }}</div>
                <b :class="{ neg: Number(total.profitAfterFreight) < 0 }">¥{{ money(total.profitAfterFreight) }}</b>
              </div>
            </a-tooltip>
          </template>
          <div class="kpi">
            <div class="sub-text">{{ $t('jxc.field.netQty') }}</div>
            <b>{{ qty(total.netQty) }}</b>
          </div>
          <div class="kpi">
            <div class="sub-text">{{ $t('jxc.field.docCount') }}</div>
            <b>{{ total.docCount ?? 0 }}</b>
          </div>
        </div>
        <a-space>
          <a-button @click="openDetail()">{{ $t('jxc.common.allDetail') }}</a-button>
          <a-button :disabled="!data.rows.length" @click="exportSummary">
            <template #icon><DownloadOutlined /></template>{{ $t('jxc.common.exportSummary') }}
          </a-button>
        </a-space>
      </div>
      <div class="sub-text caliber">{{ tip }}</div>
      <a-alert v-if="data.truncated" type="warning" show-icon class="alert" :message="$t('jxc.common.truncated', { n: data.rows.length })" />
      <a-table
        :row-key="(r) => r.groupKey ?? '__none'"
        size="middle"
        :columns="columns"
        :data-source="data.rows"
        :loading="loading"
        :pagination="data.rows.length > 50 ? { pageSize: 50, showSizeChanger: false, showTotal: (n) => $t('common.table.total', { total: n }) } : false"
        :scroll="{ x: sales ? 2030 : 1200, y: '100%' }"
        :custom-row="(r) => ({ onClick: () => openDetail(r), class: 'click-row' })"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'group'">
            <a>{{ groupLabel(groupBy, record, dict) }}</a>
          </template>
          <template v-else-if="column.qty">{{ qty(record[column.key]) }}</template>
          <template v-else-if="column.key === 'cost'">
            {{ money(record.cost) }}
            <a-tooltip v-if="Number(record.missingCost) > 0" :title="$t('jxc.common.missingCostTip', { n: record.missingCost })">
              <a-tag color="orange" :bordered="false" class="hint">{{ $t('jxc.common.hasRefCost') }}</a-tag>
            </a-tooltip>
          </template>
          <template v-else-if="column.money">
            <span :class="{ neg: Number(record[column.key]) < 0 }">{{ money(record[column.key]) }}</span>
          </template>
          <template v-else-if="column.key === 'avgPrice'">{{ price(record.avgPrice) }}</template>
          <template v-else-if="column.key === 'margin'">
            <span :class="{ neg: Number(record.margin) < 0 }">{{ marginText(record.margin) }}</span>
          </template>
        </template>
        <template v-if="data.rows.length" #summary>
          <a-table-summary fixed>
            <a-table-summary-row class="total-row">
              <a-table-summary-cell v-for="(c, i) in columns" :key="c.key" :index="i" :align="c.align">
                <template v-if="c.key === 'group'">{{ $t('jxc.common.total') }}</template>
                <template v-else-if="c.qty">{{ qty(total[c.key]) }}</template>
                <template v-else-if="c.money || c.key === 'cost'">{{ money(total[c.key]) }}</template>
                <template v-else-if="c.key === 'avgPrice'">{{ price(total.avgPrice) }}</template>
                <template v-else-if="c.key === 'margin'">{{ marginText(total.margin) }}</template>
                <template v-else-if="c.key === 'docCount'">{{ total.docCount }}</template>
              </a-table-summary-cell>
            </a-table-summary-row>
          </a-table-summary>
        </template>
      </a-table>
    </a-card>

    <JxcDetail ref="detailRef" :report="report" />
  </div>
</template>

<script setup>
import MaterialCategorySelect from '@/components/wms/MaterialCategorySelect.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import JxcDetail from './JxcDetail.vue'
import { jxcSummary } from '@/api/bi/jxc'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { money, price, qty } from '@/utils/number'
import { writeExcel } from '@/utils/excel'
import { JxcDimension } from '@/constants/enums'
import { DICT_TYPES, PURCHASE_DIMS, SALES_DIMS, groupLabel, marginText, rangePresets, thisMonth } from './jxc'
import { t } from '@/locales'

/**
 * 销售报表 / 进货报表：按维度汇总出入库单（净额 = 出库 − 退货），点击汇总行查看明细，明细中点击单据号查看单据
 * - report：sales | purchase
 */
const props = defineProps({
  report: { type: String, required: true }
})

const sales = computed(() => props.report === 'sales')
const dict = useDict(...DICT_TYPES)
const dims = computed(() => (sales.value ? SALES_DIMS : PURCHASE_DIMS))
const presets = rangePresets()
const range = ref(thisMonth())
const groupBy = ref(sales.value ? JxcDimension.CUSTOMER : JxcDimension.SUPPLIER)
const query = reactive({ partnerId: undefined, salesmanId: undefined, category: undefined, brand: undefined, region: undefined, warehouseId: undefined, keyword: '' })
const data = ref({ rows: [], total: {}, truncated: false })
const total = computed(() => data.value.total || {})
const loading = ref(false)
const tip = computed(() => t(sales.value ? 'jxc.sales.tip' : 'jxc.purchase.tip'))

const columns = computed(() => {
  const s = sales.value
  return [
    { title: t(`jxc.dim.${groupBy.value}`), key: 'group', width: 220, fixed: 'left', ellipsis: true },
    { title: t(s ? 'jxc.field.outQty' : 'jxc.field.inQty'), key: 'bizQty', width: 110, align: 'right', qty: true },
    { title: t('jxc.field.returnQty'), key: 'returnQty', width: 100, align: 'right', qty: true },
    { title: t('jxc.field.netQty'), key: 'netQty', width: 100, align: 'right', qty: true },
    { title: t(s ? 'jxc.field.outAmount' : 'jxc.field.inAmount'), key: 'bizAmount', width: 130, align: 'right', money: true },
    { title: t('jxc.field.returnAmount'), key: 'returnAmount', width: 120, align: 'right', money: true },
    { title: t('jxc.field.netAmount'), key: 'netAmount', width: 130, align: 'right', money: true },
    { title: t('jxc.field.taxedAmount'), key: 'taxedAmount', width: 130, align: 'right', money: true },
    ...(s
      ? [
          { title: t('jxc.field.cost'), key: 'cost', width: 170, align: 'right' },
          { title: t('jxc.field.profit'), key: 'profit', width: 120, align: 'right', money: true },
          { title: t('jxc.field.margin'), key: 'margin', width: 90, align: 'right' },
          // 参考口径：坏账核销、本公司承担的发运运费及扣除后的毛利（财务报表中分别计入营业外支出、销售费用）
          { title: t('jxc.field.badDebt'), key: 'badDebt', width: 110, align: 'right', money: true },
          { title: t('jxc.field.profitAfterBadDebt'), key: 'profitAfterBadDebt', width: 130, align: 'right', money: true },
          { title: t('jxc.field.freight'), key: 'freight', width: 110, align: 'right', money: true },
          { title: t('jxc.field.profitAfterFreight'), key: 'profitAfterFreight', width: 150, align: 'right', money: true }
        ]
      : []),
    { title: t('jxc.field.avgPrice'), key: 'avgPrice', width: 100, align: 'right' },
    { title: t('jxc.field.docCount'), key: 'docCount', dataIndex: 'docCount', width: 80, align: 'right' }
  ]
})

/** 查询条件（不含汇总维度），去掉空值 */
function params() {
  const p = { begin: range.value[0], end: range.value[1], ...query, keyword: query.keyword?.trim() }
  return Object.fromEntries(Object.entries(p).filter(([, v]) => v !== undefined && v !== null && v !== ''))
}

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    const res = await jxcSummary(props.report, { ...params(), groupBy: groupBy.value })
    if (current === seq) data.value = res
  } finally {
    if (current === seq) loading.value = false
  }
}

const detailRef = ref()
function openDetail(row) {
  if (!row) {
    detailRef.value.open({ params: params() })
    return
  }
  detailRef.value.open({ params: params(), dim: groupBy.value, key: row.groupKey, label: groupLabel(groupBy.value, row, dict) })
}

function exportSummary() {
  const cols = columns.value
  const cell = (c, r) => {
    if (c.key === 'group') return groupLabel(groupBy.value, r, dict)
    const v = r[c.key]
    return v === null || v === undefined ? '' : Number(v)
  }
  const rows = data.value.rows.map((r) => cols.map((c) => cell(c, r)))
  rows.push(cols.map((c) => (c.key === 'group' ? t('jxc.common.total') : cell(c, total.value))))
  const name = `${t(sales.value ? 'jxc.sales.title' : 'jxc.purchase.title')}_${t(`jxc.dim.${groupBy.value}`)}_${range.value[0]}_${range.value[1]}`
  writeExcel(name, cols.map((c) => ({ title: c.title, width: c.key === 'group' ? 30 : 14 })), rows, { withStamp: false })
}

onMounted(load)
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
}
.filter {
  width: 220px;
}
.filter-s {
  width: 150px;
}
.dims {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}
.head {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.kpis {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 28px;
}
.kpi b {
  font-size: 18px;
}
.caliber {
  margin-bottom: 12px;
  line-height: 1.6;
}
.alert {
  margin-bottom: 12px;
}
.hint {
  margin-left: 6px;
  margin-right: 0;
}
.neg {
  color: var(--status-critical, #cf1322);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
:deep(.click-row) {
  cursor: pointer;
}
:deep(.total-row) {
  font-weight: 600;
}
</style>
