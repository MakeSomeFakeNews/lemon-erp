<template>
  <SubPage
    :open="open"
    :title="detail ? `询价单 ${detail.inquiryNo}` : '询价单'"
    width="min(1200px, 100vw)"
    destroy-on-close
    @close="emit('update:open', false)"
  >
    <template #extra>
      <DictTag v-if="detail" type="scm_inquiry_status" :value="detail.status" />
    </template>

    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="doc-desc">
          <a-descriptions-item label="询价单号">{{ detail.inquiryNo }}</a-descriptions-item>
          <a-descriptions-item label="询价日期">{{ detail.inquiryDate }}</a-descriptions-item>
          <a-descriptions-item label="报价截止">
            {{ detail.deadline || '-' }}
            <a-tag v-if="quoting && detail.deadline && detail.deadline < today()" color="orange" :bordered="false" class="tag-gap">已过截止日</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="制单">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item label="报价供应商">{{ suppliers.length }} 家</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="说明">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <div class="section-head">
          <span class="section-title">比价</span>
          <span class="sub-text">
            每格为含税单价 / 交期，绿色为该物料的最低价{{ quoting ? '；选中的报价为中标报价，默认选最低价' : '' }}
          </span>
        </div>
        <a-table
          row-key="id"
          size="small"
          bordered
          :columns="matrixColumns"
          :data-source="detail.items"
          :pagination="false"
          :scroll="{ x: 520 + suppliers.length * 170 }"
        >
          <template #headerCell="{ column }">
            <template v-if="column.supplier">
              <div class="supplier-head">
                <span>
                  {{ column.supplier.supplierName }}
                  <DictTag v-if="column.supplier.supplierLevel" type="scm_supplier_level" :value="column.supplier.supplierLevel" />
                </span>
                <span v-if="quoting && can('edit')" class="head-actions">
                  <a-tooltip title="修改报价">
                    <a-button type="link" size="small" @click="openQuote(column.supplier)">
                      <template #icon><EditOutlined /></template>
                    </a-button>
                  </a-tooltip>
                  <a-popconfirm title="删除该供应商的全部报价？" @confirm="removeQuote(column.supplier.supplierId)">
                    <a-button type="link" danger size="small">
                      <template #icon><DeleteOutlined /></template>
                    </a-button>
                  </a-popconfirm>
                </span>
              </div>
            </template>
          </template>
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">
              <div>{{ record.materialCode }} {{ record.materialName }}</div>
              <div class="sub-text">{{ record.spec }}</div>
            </template>
            <template v-else-if="column.key === 'qty'">
              <div>{{ formatQty(record.qty) }} {{ record.unit }}</div>
              <div v-if="record.requiredDate" class="sub-text">需 {{ record.requiredDate }}</div>
            </template>
            <template v-else-if="column.supplier">
              <template v-if="quoteOf(record.id, column.supplier.supplierId)">
                <div
                  :class="['quote-cell', { lowest: isLowest(record.id, quoteOf(record.id, column.supplier.supplierId)), clickable: quoting }]"
                  @click="select(record.id, quoteOf(record.id, column.supplier.supplierId))"
                >
                  <a-radio v-if="quoting" :checked="selections[record.id] === quoteOf(record.id, column.supplier.supplierId).id" />
                  <CheckCircleFilled v-else-if="record.selectedQuoteId === quoteOf(record.id, column.supplier.supplierId).id" class="won" />
                  <span>
                    <b>¥{{ formatPrice(quoteOf(record.id, column.supplier.supplierId).price) }}</b>
                    <span class="sub-text"> / {{ quoteOf(record.id, column.supplier.supplierId).leadDays ?? '-' }} 天</span>
                    <div class="sub-text">
                      税率 {{ Number(quoteOf(record.id, column.supplier.supplierId).taxRate || 0) }}%
                      <template v-if="quoteOf(record.id, column.supplier.supplierId).remark">
                        · {{ quoteOf(record.id, column.supplier.supplierId).remark }}
                      </template>
                    </div>
                  </span>
                </div>
              </template>
              <span v-else class="sub-text">未报价</span>
            </template>
            <template v-else-if="column.key === 'order'">
              <a v-if="record.orderId" @click="toOrder(record.orderId)">{{ record.orderNo }}</a>
              <span v-else class="sub-text">-</span>
            </template>
          </template>
        </a-table>
        <a-empty v-if="!suppliers.length" class="empty" description="还没有供应商报价，点击「录入报价」添加" />
      </template>
    </a-spin>

    <template v-if="detail && actions.length" #footer>
      <div class="drawer-footer">
        <a-button v-for="a in actions" :key="a.key" :type="a.primary ? 'primary' : 'default'" :danger="a.danger" @click="a.run">
          <template #icon><component :is="a.icon" /></template>{{ a.label }}
        </a-button>
      </div>
    </template>

    <a-modal v-model:open="quoteOpen" title="录入报价" width="min(900px, 100vw)" :confirm-loading="saving" @ok="submitQuote">
      <a-form layout="vertical">
        <a-form-item label="供应商" required>
          <PartnerSelect
            v-model:value="quoteForm.supplierId"
            type="supplier"
            :label="quoteForm.supplierName"
            :disabled="quoteForm.locked"
            @select="onSupplierSelect"
          />
        </a-form-item>
      </a-form>
      <a-table row-key="itemId" size="small" :columns="quoteColumns" :data-source="quoteForm.lines" :pagination="false" :scroll="{ x: 760 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ formatQty(record.qty) }} {{ record.unit }}</div>
          </template>
          <template v-else-if="column.key === 'price'">
            <a-input-number v-model:value="record.price" :min="0" :precision="4" prefix="¥" placeholder="不报价留空" class="full" />
          </template>
          <template v-else-if="column.key === 'taxRate'">
            <a-input-number v-model:value="record.taxRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
          </template>
          <template v-else-if="column.key === 'leadDays'">
            <a-input-number v-model:value="record.leadDays" :min="0" :max="365" addon-after="天" class="full" />
          </template>
          <template v-else-if="column.key === 'remark'">
            <a-input v-model:value="record.remark" :maxlength="255" />
          </template>
        </template>
      </a-table>
    </a-modal>

    <a-modal v-model:open="finishOpen" title="定标完成" :confirm-loading="saving" @ok="submitFinish">
      <p>
        已选择 {{ selectedCount }} / {{ detail?.items.length || 0 }} 行的中标报价，未选择的明细不定标。定标后询价单不能再修改报价。
      </p>
      <a-form layout="vertical">
        <a-form-item>
          <a-checkbox v-model:checked="finishForm.updatePrice">用中标价更新供应商的供货价格</a-checkbox>
        </a-form-item>
        <a-form-item>
          <a-checkbox v-model:checked="finishForm.createOrder">按中标供应商生成草稿采购订单</a-checkbox>
        </a-form-item>
        <a-form-item v-if="finishForm.createOrder" label="收货仓库">
          <WarehouseSelect v-model:value="finishForm.warehouseId" placeholder="选填，入库时默认" />
        </a-form-item>
      </a-form>
    </a-modal>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { CheckCircleFilled, DeleteOutlined, EditOutlined, FormOutlined, StopOutlined, TrophyOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import PartnerSelect from './PartnerSelect.vue'
import { closeInquiry, deleteInquiryQuote, finishInquiry, getInquiry, saveInquiryQuote } from '@/api/scm/sourcing'
import { useUserStore } from '@/store/user'
import { formatQty, today } from '@/components/wms/wms'

/** 询价单详情：按供应商比价、录入报价、定标（更新供货价格、生成采购订单）、关闭。触发 edit(record)、changed */
const props = defineProps({
  open: Boolean,
  inquiryId: Number
})
const emit = defineEmits(['update:open', 'edit', 'changed'])

const userStore = useUserStore()
const can = (perm) => userStore.hasPerm(`scm:inquiry:${perm}`)
const router = useRouter()

const formatPrice = (n) => Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 4 })

const detail = ref()
const loading = ref(false)
const quoting = computed(() => detail.value?.status === 'QUOTING')

/** 报价过的供应商（按首次出现顺序） */
const suppliers = computed(() => {
  const map = new Map()
  for (const q of detail.value?.quotes || []) {
    if (!map.has(q.supplierId)) map.set(q.supplierId, { supplierId: q.supplierId, supplierName: q.supplierName, supplierLevel: q.supplierLevel })
  }
  return [...map.values()]
})

/** itemId → supplierId → 报价 */
const quoteMap = computed(() => {
  const map = new Map()
  for (const q of detail.value?.quotes || []) {
    if (!map.has(q.itemId)) map.set(q.itemId, new Map())
    map.get(q.itemId).set(q.supplierId, q)
  }
  return map
})
const quoteOf = (itemId, supplierId) => quoteMap.value.get(itemId)?.get(supplierId)

/** itemId → 最低价报价 */
const lowestMap = computed(() => {
  const map = new Map()
  for (const [itemId, quotes] of quoteMap.value) {
    let best
    for (const q of quotes.values()) {
      if (!best || Number(q.price) < Number(best.price)) best = q
    }
    if (best) map.set(itemId, best)
  }
  return map
})
const isLowest = (itemId, quote) => Number(lowestMap.value.get(itemId)?.price) === Number(quote.price)

const matrixColumns = computed(() => [
  { title: '物料', key: 'material', width: 220, fixed: 'left' },
  { title: '数量', key: 'qty', width: 120, align: 'right' },
  ...suppliers.value.map((s) => ({ key: `s-${s.supplierId}`, supplier: s, width: 170 })),
  { title: '采购订单', key: 'order', width: 170 }
])

const quoteColumns = [
  { title: '物料', key: 'material', width: 220 },
  { title: '含税单价', key: 'price', width: 150 },
  { title: '税率', key: 'taxRate', width: 110 },
  { title: '交期', key: 'leadDays', width: 110 },
  { title: '备注', key: 'remark', width: 160 }
]

// 中标选择：itemId → quoteId
const selections = reactive({})
const selectedCount = computed(() => Object.values(selections).filter(Boolean).length)

function initSelections() {
  Object.keys(selections).forEach((k) => delete selections[k])
  for (const item of detail.value?.items || []) {
    const id = item.selectedQuoteId || lowestMap.value.get(item.id)?.id
    if (id) selections[item.id] = id
  }
}

function select(itemId, quote) {
  if (quoting.value) selections[itemId] = quote.id
}

async function load() {
  loading.value = true
  try {
    detail.value = await getInquiry(props.inquiryId)
    initSelections()
  } finally {
    loading.value = false
  }
}

watch(
  () => props.open && props.inquiryId,
  (v) => {
    detail.value = undefined
    if (v) load()
  }
)

async function afterChange(msg) {
  message.success(msg)
  await load()
  emit('changed')
}

function toOrder(id) {
  router.push({ path: '/scm/purchase', query: { id } })
}

// 录入报价
const quoteOpen = ref(false)
const saving = ref(false)
const quoteForm = reactive({ supplierId: undefined, supplierName: undefined, locked: false, lines: [] })

function fillLines(supplierId) {
  quoteForm.lines = detail.value.items.map((item) => {
    const q = supplierId ? quoteOf(item.id, supplierId) : undefined
    return {
      itemId: item.id,
      materialCode: item.materialCode,
      materialName: item.materialName,
      qty: item.qty,
      unit: item.unit,
      price: q ? Number(q.price) : undefined,
      taxRate: q?.taxRate != null ? Number(q.taxRate) : 13,
      leadDays: q?.leadDays ?? undefined,
      remark: q?.remark || ''
    }
  })
}

function openQuote(supplier) {
  quoteForm.supplierId = supplier?.supplierId
  quoteForm.supplierName = supplier?.supplierName
  quoteForm.locked = !!supplier
  fillLines(supplier?.supplierId)
  quoteOpen.value = true
}

function onSupplierSelect(partner) {
  quoteForm.supplierName = `${partner.code} ${partner.name}`
  fillLines(partner.id)
}

async function submitQuote() {
  if (!quoteForm.supplierId) return message.warning('请选择供应商')
  const lines = quoteForm.lines
    .filter((l) => l.price != null)
    .map((l) => ({ itemId: l.itemId, price: l.price, taxRate: l.taxRate ?? 0, leadDays: l.leadDays ?? null, remark: l.remark || null }))
  if (!lines.length) return message.warning('请至少填写一行报价')
  saving.value = true
  try {
    await saveInquiryQuote(props.inquiryId, { supplierId: quoteForm.supplierId, lines })
    quoteOpen.value = false
    await afterChange('报价已保存')
  } finally {
    saving.value = false
  }
}

async function removeQuote(supplierId) {
  await deleteInquiryQuote(props.inquiryId, supplierId)
  await afterChange('已删除报价')
}

// 定标
const finishOpen = ref(false)
const finishForm = reactive({ updatePrice: true, createOrder: true, warehouseId: undefined })

function openFinish() {
  if (!selectedCount.value) return message.warning('请在比价表中选择中标报价')
  Object.assign(finishForm, { updatePrice: true, createOrder: true, warehouseId: undefined })
  finishOpen.value = true
}

async function submitFinish() {
  saving.value = true
  try {
    const selected = Object.fromEntries(Object.entries(selections).filter(([, v]) => v))
    const orderIds = await finishInquiry(props.inquiryId, {
      selections: selected,
      updatePrice: finishForm.updatePrice,
      createOrder: finishForm.createOrder,
      warehouseId: finishForm.createOrder ? finishForm.warehouseId || null : null
    })
    finishOpen.value = false
    await afterChange(orderIds?.length ? `已定标，生成 ${orderIds.length} 张采购订单` : '已定标')
  } finally {
    saving.value = false
  }
}

const actions = computed(() => {
  const d = detail.value
  if (!d || !quoting.value || !can('edit')) return []
  const list = []
  if (!d.quotes?.length) list.push({ key: 'edit', label: '编辑', icon: EditOutlined, run: () => emit('edit', d) })
  list.push({
    key: 'close',
    label: '关闭',
    icon: StopOutlined,
    danger: true,
    run: () =>
      Modal.confirm({
        title: '关闭询价单？',
        content: '关闭后不能再录入报价和定标。',
        async onOk() {
          await closeInquiry(props.inquiryId)
          await afterChange('已关闭')
        }
      })
  })
  list.push({ key: 'quote', label: '录入报价', icon: FormOutlined, run: () => openQuote() })
  if (d.quotes?.length) list.push({ key: 'finish', label: '定标完成', icon: TrophyOutlined, primary: true, run: openFinish })
  return list
})

defineExpose({ reload: load })
</script>

<style scoped>
.doc-desc {
  margin-bottom: 16px;
}
.section-head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}
.section-title {
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.tag-gap {
  margin-left: 4px;
}
.supplier-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
}
.head-actions {
  flex-shrink: 0;
  white-space: nowrap;
}
.quote-cell {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  margin: -4px;
  padding: 4px;
  border-radius: 4px;
}
.quote-cell.clickable {
  cursor: pointer;
}
.quote-cell.lowest {
  background: rgba(82, 196, 26, 0.12);
}
.quote-cell.lowest b {
  color: #52c41a;
}
.won {
  margin-top: 3px;
  color: #52c41a;
}
.empty {
  margin-top: 16px;
}
.full {
  width: 100%;
}
</style>
