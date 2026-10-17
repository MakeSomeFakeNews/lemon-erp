<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <span class="sub-text">{{ $t('dealer.order.tip') }}</span>
        <template #extra>
          <a-button v-perm="'dealer:order:add'" type="primary" @click="openShop">
            <template #icon><ShoppingCartOutlined /></template>{{ $t('dealer.order.shop') }}
          </a-button>
        </template>
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 900, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'orderNo'">
            <a @click="openDetail(record.id)">{{ record.orderNo }}</a>
            <div class="sub-text">{{ record.orderDate }}</div>
          </template>
          <template v-else-if="column.key === 'amount'">
            ¥{{ formatMoney(record.totalAmount) }}
            <div v-if="Number(record.discountAmount) || Number(record.rebateAmount)" class="sub-text">
              {{ $t('dealer.order.discount', { amount: formatMoney(Number(record.discountAmount) + Number(record.rebateAmount)) }) }}
            </div>
          </template>
          <template v-else-if="column.key === 'qty'">{{ formatQty(record.totalQty) }}</template>
          <template v-else-if="column.key === 'status'"><DictTag type="scm_order_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">{{ $t('common.action.view') }}</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <SubPage :open="shopOpen" :title="$t('dealer.order.shop')" width="min(1100px, 100vw)" @close="shopOpen = false">
      <template #extra>
        <a-button type="primary" :loading="saving" :disabled="!cart.length" @click="submit">{{ $t('dealer.order.submitOrder', { n: cart.length }) }}</a-button>
      </template>
      <div class="toolbar">
        <a-input-search v-model:value="keyword" :placeholder="$t('dealer.order.searchPlaceholder')" allow-clear class="filter-wide" @search="loadCatalog" />
        <span class="sub-text">{{ $t('dealer.order.priceTip') }}</span>
      </div>
      <a-table row-key="materialId" size="small" :columns="catalogColumns" :data-source="catalog" :loading="catalogLoading" :pagination="false"
        :scroll="{ x: 860, y: 420 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            {{ record.materialCode }} {{ record.materialName }}
            <div class="sub-text">{{ record.spec }}</div>
          </template>
          <template v-else-if="column.key === 'price'">
            ¥{{ formatMoney(record.price) }} / {{ record.unit }}
            <div v-for="t in record.tiers" :key="t.minQty" class="sub-text">{{ $t('dealer.order.tierPrice', { qty: formatQty(t.minQty), unit: record.unit, price: formatMoney(t.price) }) }}</div>
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-input-number v-model:value="qty[record.materialId]" :min="0" :addon-after="record.unit" class="full" />
          </template>
          <template v-else-if="column.key === 'amount'">
            <span v-if="qty[record.materialId]">¥{{ formatMoney(lineAmount(record)) }}</span>
          </template>
        </template>
      </a-table>
      <a-row :gutter="16" class="order-form">
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('dealer.order.expectedDelivery')">
            <a-date-picker v-model:value="order.deliveryDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col v-if="rebateBalance > 0" :xs="24" :sm="8">
          <a-form-item :label="$t('dealer.order.useRebate')" :extra="$t('dealer.order.rebateBalance', { amount: formatMoney(rebateBalance) })">
            <a-input-number v-model:value="order.rebateAmount" :min="0" :max="Math.min(rebateBalance, cartAmount)" :precision="2" prefix="¥" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('common.field.remark')">
            <a-input v-model:value="order.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
      <div class="total">{{ $t('dealer.order.totalByList') }} <b>¥{{ formatMoney(cartAmount) }}</b><span class="sub-text">{{ $t('dealer.order.totalTip') }}</span></div>
    </SubPage>

    <SubPage :open="detailOpen" :title="detail ? $t('dealer.order.detailTitle', { no: detail.orderNo }) : ''" width="min(900px, 100vw)" @close="detailOpen = false">
      <template v-if="detail && detail.status === 'DRAFT'" #extra>
        <span v-perm="'dealer:order:add'">
          <a-popconfirm :title="$t('dealer.order.confirmCancel')" @confirm="cancel"><a-button danger>{{ $t('dealer.order.cancelOrder') }}</a-button></a-popconfirm>
        </span>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('dealer.order.orderDate')">{{ detail.orderDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('dealer.order.expected')">{{ detail.deliveryDate || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="scm_order_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.amount')">¥{{ formatMoney(detail.totalAmount) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('dealer.order.promoDiscount')">¥{{ formatMoney(detail.discountAmount) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('dealer.order.rebateDeduct')">¥{{ formatMoney(detail.rebateAmount) }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false" :scroll="{ x: 760 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">
              {{ record.materialCode }} {{ record.materialName }}
              <a-tag v-if="record.gift" color="magenta" class="ml">{{ $t('dealer.order.gift') }}</a-tag>
            </template>
            <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
            <template v-else-if="column.key === 'price'">
              ¥{{ formatMoney(record.price) }}
              <div v-if="record.origPrice != null" class="sub-text"><del>¥{{ formatMoney(record.origPrice) }}</del> {{ record.promotionName || '' }}</div>
            </template>
            <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
            <template v-else-if="column.key === 'done'">{{ formatQty(record.doneQty) }}</template>
          </template>
        </a-table>
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ShoppingCartOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { cancelDealerOrder, dealerCatalog, dealerRebateBalance, getDealerOrder, pageDealerOrder, placeDealerOrder } from '@/api/scm/dealer'
import { formatMoney } from '@/components/scm/scm'
import { formatQty } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'

const { t } = useI18n()

/** 经销商门户：按价目表订货（提交后自动应用促销、可使用返利），查看订单进度 */
useDict('scm_order_status')

const columns = [
  { title: t('dealer.order.orderNo'), key: 'orderNo', width: 200 },
  { title: t('common.field.quantity'), key: 'qty', width: 100, align: 'right' },
  { title: t('common.field.amount'), key: 'amount', width: 160, align: 'right' },
  { title: t('dealer.order.expected'), dataIndex: 'deliveryDate', width: 150 },
  { title: t('common.field.status'), key: 'status', width: 110 },
  { title: t('common.action.operation'), key: 'action', width: 90, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'dealer-order')
const { list, loading, size, pagination, load, onTableChange } = useTable(pageDealerOrder, reactive({}))
onMounted(load)

// ---------------------------------------------------------------- 订货
const catalogColumns = [
  { title: t('dealer.order.material'), key: 'material' },
  { title: t('dealer.order.price'), key: 'price', width: 200 },
  { title: t('dealer.order.orderQty'), key: 'qty', width: 180 },
  { title: t('common.field.amount'), key: 'amount', width: 120, align: 'right' }
]
const shopOpen = ref(false)
const keyword = ref('')
const catalog = ref([])
const catalogLoading = ref(false)
const qty = reactive({})
const order = reactive({ deliveryDate: undefined, remark: '', rebateAmount: undefined })
const rebateBalance = ref(0)
const saving = ref(false)
/** 按起订数量取单价：满足数量的最高一档 */
function unitPrice(item, q) {
  const tier = [...(item.tiers || [])].filter((t) => q >= Number(t.minQty)).sort((a, b) => Number(b.minQty) - Number(a.minQty))[0]
  return Number(tier ? tier.price : item.price)
}
const lineAmount = (item) => unitPrice(item, Number(qty[item.materialId] || 0)) * Number(qty[item.materialId] || 0)
const cart = computed(() => catalog.value.filter((c) => Number(qty[c.materialId]) > 0))
const cartAmount = computed(() => cart.value.reduce((s, c) => s + lineAmount(c), 0))
async function loadCatalog() {
  catalogLoading.value = true
  try {
    catalog.value = await dealerCatalog(keyword.value || undefined)
  } finally {
    catalogLoading.value = false
  }
}
async function openShop() {
  Object.keys(qty).forEach((k) => delete qty[k])
  Object.assign(order, { deliveryDate: undefined, remark: '', rebateAmount: undefined })
  keyword.value = ''
  shopOpen.value = true
  const [, balance] = await Promise.all([loadCatalog(), dealerRebateBalance()])
  rebateBalance.value = Number(balance) || 0
}
async function submit() {
  saving.value = true
  try {
    const id = await placeDealerOrder({
      deliveryDate: order.deliveryDate || null, remark: order.remark || null, rebateAmount: order.rebateAmount || null,
      items: cart.value.map((c) => ({ materialId: c.materialId, qty: qty[c.materialId] }))
    })
    message.success(t('dealer.order.submitted'))
    shopOpen.value = false
    load()
    openDetail(id)
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 订单详情
const itemColumns = [
  { title: t('dealer.order.material'), key: 'material' },
  { title: t('common.field.quantity'), key: 'qty', width: 110, align: 'right' },
  { title: t('common.field.price'), key: 'price', width: 150, align: 'right' },
  { title: t('common.field.amount'), key: 'amount', width: 120, align: 'right' },
  { title: t('dealer.order.shipped'), key: 'done', width: 100, align: 'right' }
]
const detailOpen = ref(false)
const detail = ref()
async function openDetail(id) {
  detail.value = await getDealerOrder(id)
  detailOpen.value = true
}
async function cancel() {
  await cancelDealerOrder(detail.value.id)
  message.success(t('dealer.order.canceled'))
  detailOpen.value = false
  load()
}
</script>

<style scoped>
.filter-wide {
  width: 240px;
}
.full {
  width: 100%;
}
.block {
  margin-bottom: 16px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.order-form {
  margin-top: 16px;
}
.total {
  text-align: right;
}
.ml {
  margin-left: 6px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
