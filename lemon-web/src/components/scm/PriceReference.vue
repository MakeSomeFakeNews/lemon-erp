<template>
  <a-modal :open="visible" :title="$t('pricing.reference.title', { name: materialName || '-' })" width="min(1000px, 100vw)" :footer="null" @cancel="visible = false">
    <div class="ref-filter">
      <a-input
        v-model:value="query.keyword"
        allow-clear
        :placeholder="$t('pricing.reference.partnerPlaceholder')"
        class="ref-keyword"
        @press-enter="load"
      />
      <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" class="ref-range" />
      <a-checkbox v-if="partnerId" v-model:checked="query.onlyPartner">{{ $t('pricing.reference.onlyPartner') }}</a-checkbox>
      <a-button type="primary" :loading="loading" @click="load">{{ $t('common.action.search') }}</a-button>
    </div>
    <a-spin :spinning="loading">
      <a-empty v-if="data && !tabs.length" :description="$t('pricing.reference.noPermission')" />
      <a-tabs v-else v-model:active-key="activeTab" size="small">
        <a-tab-pane v-for="tab in tabs" :key="tab.key" :tab="`${tab.label} (${data[tab.key].length})`">
          <a-table
            :row-key="(r, i) => `${tab.key}-${i}`"
            size="small"
            :columns="tab.columns"
            :data-source="data[tab.key]"
            :pagination="false"
            :scroll="{ x: tab.scrollX, y: 360 }"
            :locale="{ emptyText: $t('pricing.reference.empty') }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="PRICE_KEYS.includes(column.key)">
                <a v-if="pickable && record[column.key] != null" :title="$t('pricing.reference.usePrice')" @click="pick(record[column.key])">{{ price(record[column.key]) }}</a>
                <span v-else>{{ record[column.key] == null ? '-' : price(record[column.key]) }}</span>
              </template>
              <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }}</template>
              <template v-else-if="column.key === 'amount'">{{ money(record.amount) }}</template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
      <div class="sub-text">{{ $t('pricing.reference.note') }}</div>
      <div v-if="COMPARE_KEYS.includes(activeTab)" class="sub-text">{{ $t('pricing.reference.compareScope') }}</div>
    </a-spin>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { priceReference } from '@/api/scm/pricing'
import { money, price, qty } from '@/utils/number'
import { OrderKind } from '@/constants/enums'

/**
 * 价格参考弹窗：报价历史、销售历史、进货历史（日期 单号 单位编号 单位名称 数量 单价 金额 备注）、进货数量（按供应商汇总）、
 * 供商对比、客户对比（价格记忆）。没有权限的分区后端不返回，不显示对应标签页。价格均为含税、基本单位、本位币。
 * 报价、出入库历史按数据权限；供商 / 客户对比有「全部数据」权限时取价格记忆，否则只按有权查看的出入库单统计（只列出这些单据的往来单位）。
 * 用法：ref.open({ materialId, materialName, partnerId, kind })，kind 为 SALES / PURCHASE（决定默认打开的标签页）；
 * 传了 partnerId 时默认只看该往来单位。监听 @pick(price) 时单价可点击，选中后关闭弹窗
 */
const emit = defineEmits(['pick'])
const COMPARE_KEYS = ['customerCompare', 'supplierCompare']
defineProps({
  /** 单价是否可点击选用（父组件监听了 pick 时传 true） */
  pickable: Boolean
})

const { t } = useI18n()
const visible = ref(false)
const loading = ref(false)
const data = ref()
const activeTab = ref()
const materialId = ref()
const materialName = ref()
const partnerId = ref()
const kind = ref(OrderKind.SALES)
const query = reactive({ keyword: '', range: [], onlyPartner: true })

const PRICE_KEYS = ['price', 'avgPrice', 'lastPrice', 'minPrice', 'maxPrice']

const historyColumns = (partnerTitle) => [
  { title: t('pricing.reference.date'), dataIndex: 'bizDate', key: 'bizDate', width: 105 },
  { title: t('pricing.reference.docNo'), dataIndex: 'docNo', key: 'docNo', width: 150 },
  { title: t('pricing.reference.partnerCode', { name: partnerTitle }), dataIndex: 'partnerCode', key: 'partnerCode', width: 110 },
  { title: t('pricing.reference.partnerName', { name: partnerTitle }), dataIndex: 'partnerName', key: 'partnerName', width: 180, ellipsis: true },
  { title: t('pricing.reference.qty'), dataIndex: 'qty', key: 'qty', width: 100, align: 'right' },
  { title: t('pricing.reference.price'), dataIndex: 'price', key: 'price', width: 100, align: 'right' },
  { title: t('pricing.reference.amount'), dataIndex: 'amount', key: 'amount', width: 110, align: 'right' },
  { title: t('pricing.reference.remark'), dataIndex: 'remark', key: 'remark', width: 160, ellipsis: true }
]
const memoryColumns = (partnerTitle) => [
  { title: t('pricing.reference.partnerCode', { name: partnerTitle }), dataIndex: 'partnerCode', key: 'partnerCode', width: 110 },
  { title: t('pricing.reference.partnerName', { name: partnerTitle }), dataIndex: 'partnerName', key: 'partnerName', width: 180, ellipsis: true },
  { title: t('pricing.reference.lastDate'), dataIndex: 'lastDate', key: 'lastDate', width: 105 },
  { title: t('pricing.reference.lastPrice'), dataIndex: 'lastPrice', key: 'lastPrice', width: 100, align: 'right' },
  { title: t('pricing.reference.minDate'), dataIndex: 'minDate', key: 'minDate', width: 105 },
  { title: t('pricing.reference.minPrice'), dataIndex: 'minPrice', key: 'minPrice', width: 100, align: 'right' },
  { title: t('pricing.reference.maxDate'), dataIndex: 'maxDate', key: 'maxDate', width: 105 },
  { title: t('pricing.reference.maxPrice'), dataIndex: 'maxPrice', key: 'maxPrice', width: 100, align: 'right' }
]
const customer = t('pricing.reference.customer')
const supplier = t('pricing.reference.supplier')
const withX = (tab) => ({ ...tab, scrollX: tab.columns.reduce((s, c) => s + c.width, 0) })
const SALES_TABS = [
  withX({ key: 'sales', label: t('pricing.reference.sales'), columns: historyColumns(customer) }),
  withX({ key: 'quotations', label: t('pricing.reference.quotations'), columns: historyColumns(customer) }),
  withX({ key: 'customerCompare', label: t('pricing.reference.customerCompare'), columns: memoryColumns(customer) })
]
const PURCHASE_TABS = [
  withX({ key: 'purchases', label: t('pricing.reference.purchases'), columns: historyColumns(supplier) }),
  withX({
    key: 'purchaseQty',
    label: t('pricing.reference.purchaseQty'),
    columns: [
      { title: t('pricing.reference.partnerCode', { name: supplier }), dataIndex: 'partnerCode', key: 'partnerCode', width: 110 },
      { title: t('pricing.reference.partnerName', { name: supplier }), dataIndex: 'partnerName', key: 'partnerName', width: 200, ellipsis: true },
      { title: t('pricing.reference.times'), dataIndex: 'times', key: 'times', width: 80, align: 'right' },
      { title: t('pricing.reference.qty'), dataIndex: 'qty', key: 'qty', width: 110, align: 'right' },
      { title: t('pricing.reference.amount'), dataIndex: 'amount', key: 'amount', width: 120, align: 'right' },
      { title: t('pricing.reference.avgPrice'), dataIndex: 'avgPrice', key: 'avgPrice', width: 100, align: 'right' },
      { title: t('pricing.reference.lastDate'), dataIndex: 'lastDate', key: 'lastDate', width: 105 }
    ]
  }),
  withX({ key: 'supplierCompare', label: t('pricing.reference.supplierCompare'), columns: memoryColumns(supplier) })
]

/** 有权限的分区：当前单据方向的排在前面 */
const tabs = computed(() => {
  if (!data.value) return []
  const order = kind.value === OrderKind.PURCHASE ? [...PURCHASE_TABS, ...SALES_TABS] : [...SALES_TABS, ...PURCHASE_TABS]
  return order.filter((tab) => Array.isArray(data.value[tab.key]))
})

let seq = 0
async function load() {
  if (!materialId.value) return
  const current = ++seq
  loading.value = true
  try {
    const [start, end] = query.range || []
    const res = await priceReference({
      materialId: materialId.value,
      partnerId: partnerId.value && query.onlyPartner ? partnerId.value : undefined,
      kind: kind.value,
      keyword: query.keyword || undefined,
      start: start || undefined,
      end: end || undefined
    })
    if (current !== seq) return
    data.value = res
    if (!tabs.value.some((tab) => tab.key === activeTab.value)) activeTab.value = tabs.value[0]?.key
  } finally {
    if (current === seq) loading.value = false
  }
}

function pick(value) {
  emit('pick', Number(value))
  visible.value = false
}

function open(options = {}) {
  materialId.value = options.materialId
  materialName.value = options.materialName
  partnerId.value = options.partnerId || undefined
  kind.value = String(options.kind || OrderKind.SALES).toUpperCase()
  query.keyword = ''
  query.range = []
  query.onlyPartner = true
  data.value = undefined
  activeTab.value = kind.value === OrderKind.PURCHASE ? 'purchases' : 'sales'
  visible.value = true
  load()
}

defineExpose({ open })
</script>

<style scoped>
.ref-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.ref-keyword {
  width: 220px;
  max-width: 100%;
}
.ref-range {
  width: 260px;
  max-width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
  margin-top: 8px;
}
</style>
