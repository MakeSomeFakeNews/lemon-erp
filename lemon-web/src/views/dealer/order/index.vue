<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <span class="sub-text">订单提交后由销售审核，审核前可以撤销</span>
        <template #extra>
          <a-button v-perm="'dealer:order:add'" type="primary" @click="openShop">
            <template #icon><ShoppingCartOutlined /></template>我要订货
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
              优惠 ¥{{ formatMoney(Number(record.discountAmount) + Number(record.rebateAmount)) }}
            </div>
          </template>
          <template v-else-if="column.key === 'qty'">{{ formatQty(record.totalQty) }}</template>
          <template v-else-if="column.key === 'status'"><DictTag type="scm_order_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">查看</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <SubPage :open="shopOpen" title="我要订货" width="min(1100px, 100vw)" @close="shopOpen = false">
      <template #extra>
        <a-button type="primary" :loading="saving" :disabled="!cart.length" @click="submit">提交订单（{{ cart.length }} 种）</a-button>
      </template>
      <div class="toolbar">
        <a-input-search v-model:value="keyword" placeholder="物料编码或名称" allow-clear class="filter-wide" @search="loadCatalog" />
        <span class="sub-text">价格按价目表（起订数量越大价格越低），提交时自动应用促销活动</span>
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
            <div v-for="t in record.tiers" :key="t.minQty" class="sub-text">满 {{ formatQty(t.minQty) }} {{ record.unit }}：¥{{ formatMoney(t.price) }}</div>
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
          <a-form-item label="期望交货日期">
            <a-date-picker v-model:value="order.deliveryDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col v-if="rebateBalance > 0" :xs="24" :sm="8">
          <a-form-item label="使用返利" :extra="`返利余额 ¥${formatMoney(rebateBalance)}`">
            <a-input-number v-model:value="order.rebateAmount" :min="0" :max="Math.min(rebateBalance, cartAmount)" :precision="2" prefix="¥" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item label="备注">
            <a-input v-model:value="order.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
      <div class="total">按价目价合计 <b>¥{{ formatMoney(cartAmount) }}</b><span class="sub-text">（促销优惠以提交后的订单为准）</span></div>
    </SubPage>

    <SubPage :open="detailOpen" :title="detail ? `订单 ${detail.orderNo}` : ''" width="min(900px, 100vw)" @close="detailOpen = false">
      <template v-if="detail && detail.status === 'DRAFT'" #extra>
        <span v-perm="'dealer:order:add'">
          <a-popconfirm title="撤销该订单？" @confirm="cancel"><a-button danger>撤销订单</a-button></a-popconfirm>
        </span>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item label="下单日期">{{ detail.orderDate }}</a-descriptions-item>
          <a-descriptions-item label="期望交货">{{ detail.deliveryDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="状态"><DictTag type="scm_order_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item label="金额">¥{{ formatMoney(detail.totalAmount) }}</a-descriptions-item>
          <a-descriptions-item label="促销优惠">¥{{ formatMoney(detail.discountAmount) }}</a-descriptions-item>
          <a-descriptions-item label="返利抵扣">¥{{ formatMoney(detail.rebateAmount) }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="备注" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false" :scroll="{ x: 760 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">
              {{ record.materialCode }} {{ record.materialName }}
              <a-tag v-if="record.gift" color="magenta" class="ml">赠品</a-tag>
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

/** 经销商门户：按价目表订货（提交后自动应用促销、可使用返利），查看订单进度 */
useDict('scm_order_status')

const columns = [
  { title: '订单号', key: 'orderNo', width: 200 },
  { title: '数量', key: 'qty', width: 100, align: 'right' },
  { title: '金额', key: 'amount', width: 160, align: 'right' },
  { title: '期望交货', dataIndex: 'deliveryDate', width: 120 },
  { title: '状态', key: 'status', width: 110 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'dealer-order')
const { list, loading, size, pagination, load, onTableChange } = useTable(pageDealerOrder, reactive({}))
onMounted(load)

// ---------------------------------------------------------------- 订货
const catalogColumns = [
  { title: '物料', key: 'material' },
  { title: '价格', key: 'price', width: 200 },
  { title: '订货数量', key: 'qty', width: 180 },
  { title: '金额', key: 'amount', width: 120, align: 'right' }
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
    message.success('订单已提交，等待销售审核')
    shopOpen.value = false
    load()
    openDetail(id)
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 订单详情
const itemColumns = [
  { title: '物料', key: 'material' },
  { title: '数量', key: 'qty', width: 110, align: 'right' },
  { title: '单价', key: 'price', width: 150, align: 'right' },
  { title: '金额', key: 'amount', width: 120, align: 'right' },
  { title: '已发货', key: 'done', width: 90, align: 'right' }
]
const detailOpen = ref(false)
const detail = ref()
async function openDetail(id) {
  detail.value = await getDealerOrder(id)
  detailOpen.value = true
}
async function cancel() {
  await cancelDealerOrder(detail.value.id)
  message.success('已撤销')
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
