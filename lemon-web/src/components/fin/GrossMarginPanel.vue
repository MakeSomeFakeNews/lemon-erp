<template>
  <div>
    <div class="filters">
      <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" @change="load" />
      <a-radio-group v-model:value="groupBy" button-style="solid" @change="load">
        <a-radio-button :value="MarginGroup.MATERIAL">{{ $t('fin.analysis.byMaterial') }}</a-radio-button>
        <a-radio-button :value="MarginGroup.CUSTOMER">{{ $t('fin.analysis.byCustomer') }}</a-radio-button>
        <a-radio-button :value="MarginGroup.MONTH">{{ $t('fin.analysis.byMonth') }}</a-radio-button>
        <a-radio-button :value="DETAIL">{{ $t('fin.analysis.byDetail') }}</a-radio-button>
      </a-radio-group>
      <PartnerSelect v-model:value="customerId" type="customer" allow-clear :placeholder="$t('fin.analysis.allCustomers')" class="filter" />
      <template v-if="groupBy === DETAIL">
        <a-input-search v-model:value="keyword" allow-clear :placeholder="$t('fin.analysis.detailKeyword')" class="filter" @search="load" />
        <a-tooltip :title="$t('fin.analysis.adjustTip')">
          <a-radio-group v-model:value="mode" button-style="solid" @change="load">
            <a-radio-button :value="AdjustMode.BOTTOM">{{ $t('fin.analysis.adjustBottom') }}</a-radio-button>
            <a-radio-button :value="AdjustMode.ALLOCATE">{{ $t('fin.analysis.adjustAllocate') }}</a-radio-button>
          </a-radio-group>
        </a-tooltip>
      </template>
    </div>
    <a-alert type="info" show-icon class="tip" :message="$t('fin.analysis.marginTip')" />
    <a-row :gutter="12" class="cards">
      <a-col v-for="c in cards" :key="c.key" :xs="12" :md="4">
        <a-statistic :title="c.title" :value="c.value" :precision="c.precision" :prefix="c.prefix" :suffix="c.suffix"
          :value-style="{ color: c.neg ? 'var(--status-critical, #cf1322)' : undefined }" />
      </a-col>
    </a-row>
    <a-alert v-if="data.truncated" type="warning" show-icon class="tip" :message="$t('fin.analysis.truncated')" />
    <a-table
      :row-key="rowKey"
      size="middle"
      :columns="columns"
      :data-source="data.rows"
      :loading="loading"
      :pagination="data.rows?.length > 20 ? { pageSize: 20, showTotal: (n) => $t('common.table.total', { total: n }) } : false"
      :scroll="{ x: scrollX }"
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
          <a-tooltip v-if="isDetail && !Number(record.costed)" :title="$t('fin.analysis.refCostTip')">
            <a-tag color="orange" :bordered="false" class="hint">{{ $t('fin.analysis.refCost') }}</a-tag>
          </a-tooltip>
          <a-tooltip v-if="Number(record.missingCost) > 0" :title="$t('fin.analysis.missingCostTip', { n: record.missingCost })">
            <a-tag color="orange" :bordered="false" class="hint">{{ $t('fin.analysis.hasRefCost') }}</a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="ADJUST_KEYS.includes(column.key)">
          <span v-if="Number(record[column.key])">{{ formatMoney(record[column.key]) }}</span>
          <span v-else class="sub-text">-</span>
        </template>
        <template v-else-if="column.money">
          <span :class="{ neg: Number(record[column.key]) < 0 }">{{ formatMoney(record[column.key]) }}</span>
        </template>
        <template v-else-if="column.key === 'margin'">{{ marginText(record.margin) }}</template>
      </template>
      <template v-if="data.rows?.length" #summary>
        <a-table-summary>
          <!-- 调整行（单据明细、汇总到底部）：全部明细的合计，金额为负、成本 0、毛利为负，退货方向相反 -->
          <a-table-summary-row v-for="a in adjustments" :key="a.item" class="adjust-row">
            <a-table-summary-cell v-for="(c, i) in columns" :key="c.key" :index="i" :align="c.align">
              <template v-if="c.key === 'material'">{{ $t(`fin.analysis.adjust.${a.item}`) }}</template>
              <template v-else-if="c.key === 'salesAmount' || c.key === 'profit'">
                <span :class="{ neg: Number(a.amount) < 0 }">{{ formatMoney(a.amount) }}</span>
              </template>
              <template v-else-if="c.key === 'cost'">{{ formatMoney(a.cost) }}</template>
            </a-table-summary-cell>
          </a-table-summary-row>
          <a-table-summary-row class="total-row">
            <a-table-summary-cell v-for="(c, i) in columns" :key="c.key" :index="i" :align="c.align">
              <template v-if="i === 0">{{ $t('common.field.total') }}</template>
              <template v-else-if="c.money || c.key === 'cost' || ADJUST_KEYS.includes(c.key)">
                <span :class="{ neg: Number(total[c.key]) < 0 }">{{ formatMoney(total[c.key]) }}</span>
              </template>
              <template v-else-if="c.key === 'margin'">{{ marginText(total.margin) }}</template>
            </a-table-summary-cell>
          </a-table-summary-row>
        </a-table-summary>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import dayjs from 'dayjs'
import DictTag from '@/components/DictTag.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { grossMargin, grossMarginDetail } from '@/api/fin'
import { useDict } from '@/composables/useDict'
import { useAdjustMode } from '@/composables/useAdjustMode'
import { formatQty, today } from '@/components/wms/wms'
import { formatMoney, periodText } from '@/components/fin/fin'
import { AdjustMode, MarginGroup } from '@/constants/enums'
import { t } from '@/locales'

/**
 * 经营分析 - 销售毛利：按物料、客户、月份汇总，或按单据明细。口径（issue #93）：货款 − 整单优惠 = 销售收入；
 * 销售金额 = 销售收入 − 本公司运费 − 收款折让 − 坏账核销；毛利 = 销售金额 − 成本。
 * 单据明细的调整项可汇总到底部（行上为优惠前的货款，底部按类别列出调整行）或分摊到各行，合计相同
 */
useDict('scm_trade_kind')

/** 按明细查看：前端模式（走明细接口），不是后端 MarginGroup 的取值 */
const DETAIL = 'DETAIL'
/** 调整项列：没有金额时显示 - */
const ADJUST_KEYS = ['discount', 'freight', 'settleDiscount', 'badDebt']

const range = ref([dayjs().startOf('year').format('YYYY-MM-DD'), today()])
const groupBy = ref(MarginGroup.MATERIAL)
const customerId = ref()
const keyword = ref('')
const mode = useAdjustMode()
const data = ref({ rows: [], total: {} })
const loading = ref(false)
const isDetail = computed(() => groupBy.value === DETAIL)
const bottom = computed(() => mode.value === AdjustMode.BOTTOM)
const total = computed(() => data.value.total || {})
const adjustments = computed(() => (isDetail.value ? data.value.adjustments || [] : []))

const marginText = (v) => (v === null || v === undefined ? '-' : `${Number(v).toFixed(2)}%`)
const rowKey = (r) => (isDetail.value ? `${r.docId}-${r.materialCode}-${r.qty}-${r.revenue}` : r.groupKey ?? r.groupName)

const cards = computed(() => {
  const x = total.value
  const yuan = (key, titleKey) => ({ key, title: t(titleKey), value: Number(x[key] || 0), precision: 2, prefix: '¥', neg: Number(x[key]) < 0 })
  const list = [yuan('revenue', 'fin.analysis.salesRevenue'), yuan('salesAmount', 'fin.analysis.salesAmount'), yuan('cost', 'fin.analysis.salesCost'),
    yuan('profit', 'fin.analysis.grossProfit'),
    { key: 'margin', title: t('fin.analysis.grossMargin'), value: x.margin ?? '-', precision: x.margin == null ? undefined : 2,
      suffix: x.margin == null ? '' : '%' }]
  // 有调整项时列出（整单优惠、本公司运费、收款折让、坏账核销）
  for (const k of ADJUST_KEYS) {
    if (Number(x[k])) list.push(yuan(k, `fin.analysis.${k}`))
  }
  return list
})

const money = (key, titleKey, width = 130) => ({ title: t(titleKey), key, width, align: 'right', money: true })
/** 完整口径的金额列：货款、整单优惠、销售收入、运费、折让、坏账、销售金额、成本、毛利、毛利率 */
const fullColumns = () => [
  money('goodsAmount', 'fin.analysis.goodsAmount'),
  { title: t('fin.analysis.discount'), key: 'discount', width: 110, align: 'right' },
  money('revenue', 'fin.analysis.revenue'),
  { title: t('fin.analysis.freight'), key: 'freight', width: 110, align: 'right' },
  { title: t('fin.analysis.settleDiscount'), key: 'settleDiscount', width: 110, align: 'right' },
  { title: t('fin.analysis.badDebt'), key: 'badDebt', width: 110, align: 'right' },
  money('salesAmount', 'fin.analysis.salesAmount'),
  { title: t('fin.analysis.cost'), key: 'cost', width: 190, align: 'right' },
  money('profit', 'fin.analysis.grossProfit'),
  { title: t('fin.analysis.grossMargin'), key: 'margin', width: 100, align: 'right' }
]
const GROUP_TITLES = { MATERIAL: 'fin.analysis.material', CUSTOMER: 'fin.analysis.customer', MONTH: 'fin.analysis.month' }

const columns = computed(() => {
  if (!isDetail.value) {
    return [
      { title: t(GROUP_TITLES[groupBy.value]), key: 'group', width: 240 },
      ...(groupBy.value === MarginGroup.MATERIAL ? [{ title: t('fin.analysis.salesQty'), key: 'qty', width: 110, align: 'right' }] : []),
      ...fullColumns()
    ]
  }
  const head = [
    { title: t('common.field.date'), dataIndex: 'bizDate', key: 'bizDate', width: 110 },
    { title: t('fin.analysis.doc'), key: 'docNo', width: 200 },
    { title: t('fin.analysis.customer'), dataIndex: 'customerName', key: 'customerName', width: 180, ellipsis: true },
    { title: t('fin.analysis.material'), key: 'material', width: 260, ellipsis: true },
    { title: t('common.field.quantity'), key: 'qty', width: 100, align: 'right' },
    { title: t('common.field.unit'), dataIndex: 'unit', key: 'unit', width: 70 }
  ]
  // 汇总到底部：行上的销售金额即优惠前的货款，调整项在底部
  return bottom.value
    ? [...head, money('salesAmount', 'fin.analysis.salesAmount'), { title: t('fin.analysis.cost'), key: 'cost', width: 190, align: 'right' },
        money('profit', 'fin.analysis.grossProfit'), { title: t('fin.analysis.grossMargin'), key: 'margin', width: 100, align: 'right' }]
    : [...head, ...fullColumns()]
})
const scrollX = computed(() => columns.value.reduce((s, c) => s + (c.width || 0), 0))

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    const params = { begin: range.value[0], end: range.value[1], customerId: customerId.value ?? undefined }
    const res = isDetail.value
      ? await grossMarginDetail({ ...params, keyword: keyword.value || undefined, adjustMode: mode.value })
      : await grossMargin({ ...params, groupBy: groupBy.value })
    if (current === seq) data.value = res
  } finally {
    if (current === seq) loading.value = false
  }
}
watch(customerId, load)
onMounted(load)
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
.hint {
  margin-left: 6px;
  margin-right: 0;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.neg {
  color: var(--status-critical, #cf1322);
}
:deep(.adjust-row) {
  color: var(--app-text-2);
}
:deep(.total-row) {
  font-weight: 600;
}
</style>
