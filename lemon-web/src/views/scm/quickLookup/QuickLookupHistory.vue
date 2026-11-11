<template>
  <a-modal v-model:open="visible" :title="$t('pricing.lookup.historyTitle', { name: material?.name || '' })" width="min(900px, 100vw)" :footer="null">
    <a-empty v-if="!kinds.length" :description="$t('pricing.lookup.noKindPermission')" />
    <template v-else>
      <a-segmented v-if="kinds.length > 1" v-model:value="kind" :options="kinds" class="qh-kind" @change="reload" />
      <a-tabs v-model:active-key="tab" size="small">
        <!-- 历史参考：最近 3 次、系统最新 / 最高 / 最低价 -->
        <a-tab-pane key="summary" :tab="$t('pricing.lookup.historyTitle', { name: kindLabel })">
          <a-table :row-key="(r) => r.key" size="small" :columns="summaryColumns" :data-source="summaryRows" :loading="loading" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'price'">{{ record.line ? price(record.line.price) : '-' }}</template>
              <template v-else-if="column.key === 'qty'">{{ record.line ? qty(record.line.qty) : '' }}</template>
              <template v-else-if="column.key === 'bizDate'">{{ record.line?.bizDate || $t('pricing.lookup.noRecord') }}</template>
              <template v-else-if="column.key === 'partnerName'">{{ record.line?.partnerName }}</template>
            </template>
          </a-table>
          <div class="qh-actions">
            <span class="sub-text">{{ $t('pricing.lookup.scope') }}</span>
            <a-button @click="tab = 'trades'">{{ $t('pricing.lookup.moreTrades') }}</a-button>
            <a-button @click="openReference">{{ $t('pricing.lookup.moreContent') }}</a-button>
          </div>
        </a-tab-pane>
        <!-- 更多交易历史：往来明细 -->
        <a-tab-pane key="trades" :tab="$t('pricing.lookup.moreTrades')">
          <div class="qh-filter">
            <a-range-picker v-model:value="trades.range" value-format="YYYY-MM-DD" @change="loadTrades(1)" />
            <a-input-search v-model:value="trades.keyword" :placeholder="$t('pricing.lookup.tradeKeyword')" allow-clear class="qh-keyword" @search="loadTrades(1)" />
          </div>
          <a-table
            :row-key="(r, i) => `${r.docId}-${i}`"
            size="small"
            :columns="tradeColumns"
            :data-source="trades.list"
            :loading="trades.loading"
            :pagination="tradePagination"
            :scroll="{ x: 860, y: 360 }"
            @change="(p) => loadTrades(p.current)"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'price'">{{ price(record.price) }}</template>
              <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }}</template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </template>
    <PriceReference ref="referenceRef" />
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import PriceReference from '@/components/scm/PriceReference.vue'
import { quickLookupSummary, quickLookupTrades } from '@/api/scm/quickLookup'
import { OrderKind } from '@/constants/enums'
import { useUserStore } from '@/store/user'
import { price, qty } from '@/utils/number'

/**
 * 售价 / 进价历史参考：最近 3 次、系统最新价、最高价、最低价（按看得到的出入库单统计，优惠前含税单价）；
 * 更多交易历史（往来明细：日期范围、单位编号 / 名称 / 电话筛选，日期 / 数量 / 单价 / 单位 / 制单 / 复核）；
 * 更多内容打开与销售单明细相同的「价格参考」（报价、销售、进货、客户、供应商，按权限显示）。
 * 只能看售价的人没有「进价」。用法：ref.open(row, kind)
 */
const { t } = useI18n()
const userStore = useUserStore()
const PAGE_SIZE = 20
const visible = ref(false)
const loading = ref(false)
const material = ref()
const kind = ref(OrderKind.SALES)
const tab = ref('summary')
const summary = ref(null)
const referenceRef = ref()
const trades = reactive({ list: [], total: 0, current: 1, loading: false, range: [], keyword: '' })

/** 与后端 ScmPricingService.canViewSales / canViewPurchase 一致，只决定显示哪些分段 */
const kinds = computed(() =>
  [
    userStore.hasPerm(['scm:sales:list', 'scm:salesOut:list', 'scm:quotation:list']) ? { value: OrderKind.SALES, label: t('pricing.lookup.sales') } : null,
    userStore.hasPerm(['scm:purchase:list', 'scm:purchaseIn:list']) ? { value: OrderKind.PURCHASE, label: t('pricing.lookup.purchase') } : null
  ].filter(Boolean)
)
const kindLabel = computed(() => kinds.value.find((k) => k.value === kind.value)?.label || '')

const summaryColumns = computed(() => [
  { title: t('pricing.lookup.item'), dataIndex: 'title', key: 'title', width: 120 },
  { title: t('pricing.lookup.unitPrice'), key: 'price', width: 110, align: 'right' },
  { title: t('pricing.lookup.date'), key: 'bizDate', width: 110 },
  { title: t('pricing.lookup.partner'), key: 'partnerName', width: 200, ellipsis: true },
  { title: t('pricing.lookup.qty'), key: 'qty', width: 90, align: 'right' }
])
const summaryRows = computed(() => {
  const s = summary.value || {}
  const recent = s.recent || []
  return [
    ...[0, 1, 2].map((i) => ({ key: `r${i}`, title: t('pricing.lookup.recent', { n: i + 1 }), line: recent[i] })),
    { key: 'latest', title: t('pricing.lookup.latest'), line: s.latest },
    { key: 'max', title: t('pricing.lookup.max'), line: s.max },
    { key: 'min', title: t('pricing.lookup.min'), line: s.min }
  ]
})

const tradeColumns = computed(() => [
  { title: t('pricing.lookup.date'), dataIndex: 'bizDate', key: 'bizDate', width: 105 },
  { title: t('pricing.lookup.qty'), key: 'qty', width: 90, align: 'right' },
  { title: t('pricing.lookup.unitPrice'), key: 'price', width: 100, align: 'right' },
  { title: t('pricing.lookup.partnerName'), dataIndex: 'partnerName', key: 'partnerName', width: 180, ellipsis: true },
  { title: t('pricing.lookup.partnerCode'), dataIndex: 'partnerCode', key: 'partnerCode', width: 110 },
  { title: t('pricing.lookup.docNo'), dataIndex: 'docNo', key: 'docNo', width: 150 },
  { title: t('pricing.lookup.prepared'), dataIndex: 'createByName', key: 'createByName', width: 90 },
  { title: t('pricing.lookup.reviewed'), dataIndex: 'postByName', key: 'postByName', width: 90 }
])
const tradePagination = computed(() => ({ current: trades.current, pageSize: PAGE_SIZE, total: trades.total, showSizeChanger: false, size: 'small' }))

let seq = 0
async function loadSummary() {
  const current = ++seq
  loading.value = true
  summary.value = null
  try {
    const res = await quickLookupSummary(material.value.materialId, kind.value)
    if (current === seq) summary.value = res
  } finally {
    if (current === seq) loading.value = false
  }
}

let tradeSeq = 0
async function loadTrades(page) {
  const current = ++tradeSeq
  const [start, end] = trades.range || []
  trades.loading = true
  try {
    const res = await quickLookupTrades(material.value.materialId, {
      kind: kind.value,
      keyword: trades.keyword || undefined,
      start: start || undefined,
      end: end || undefined,
      current: page,
      size: PAGE_SIZE
    })
    if (current !== tradeSeq) return
    trades.list = res?.records || []
    trades.total = res?.total || 0
    trades.current = page
  } finally {
    if (current === tradeSeq) trades.loading = false
  }
}

function reload() {
  loadSummary()
  loadTrades(1)
}

function openReference() {
  referenceRef.value.open({ materialId: material.value.materialId, materialName: material.value.name, kind: kind.value })
}

function open(m, k) {
  material.value = m
  const allowed = kinds.value.map((o) => o.value)
  kind.value = allowed.includes(k) ? k : allowed[0] || OrderKind.SALES
  tab.value = 'summary'
  Object.assign(trades, { list: [], total: 0, current: 1, range: [], keyword: '' })
  visible.value = true
  if (allowed.length) reload()
}

defineExpose({ open })
</script>

<style scoped>
.qh-kind {
  margin-bottom: 8px;
}
.qh-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
.qh-actions .sub-text {
  flex: 1;
  min-width: 200px;
}
.qh-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.qh-keyword {
  width: 240px;
  max-width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
