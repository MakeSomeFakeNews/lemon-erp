<template>
  <div>
    <a-alert type="info" show-icon class="block" :message="conf.tip" />

    <a-form layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="conf.warehouseLabel" required>
            <WarehouseSelect
              :value="warehouseId"
              :types="[conf.warehouseType]"
              :placeholder="`请选择${conf.warehouseLabel}`"
              @update:value="onWarehouseChange"
            />
          </a-form-item>
        </a-col>
        <a-col v-if="mode === 'supplier'" :xs="24" :sm="12" :lg="6">
          <a-form-item label="转入仓库" required extra="结算后采购入库到该自有仓库">
            <WarehouseSelect v-model:value="targetWarehouseId" :types="OWN_TYPES" placeholder="请选择自有仓库" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item label="业务日期" required>
            <a-date-picker v-model:value="bizDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="mode === 'supplier' ? 6 : 12">
          <a-form-item label="备注">
            <a-input v-model:value="remark" :maxlength="255" placeholder="选填" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>

    <a-alert v-if="result" type="success" show-icon closable class="block" @close="result = null">
      <template #message>
        结算完成，已生成{{ conf.orderLabel }}
        <a @click="toOrder">查看{{ conf.orderLabel }}</a>
        <span v-if="result.consumeDocId" class="sub-text">，寄售仓已生成寄售消耗出库单</span>
      </template>
    </a-alert>

    <div class="items-head">
      <a-space wrap>
        <span class="section-title">寄售仓库存</span>
        <a-input-search v-model:value="keyword" placeholder="物料编码或名称" allow-clear class="filter" />
        <span class="sub-text">已填 {{ picked.length }} 行，合计 {{ formatQty(pickedQty) }}</span>
      </a-space>
      <a-button v-perm="'scm:consign:settle'" type="primary" :loading="saving" :disabled="!picked.length" @click="submit">
        <template #icon><CheckOutlined /></template>{{ conf.submitLabel }}
      </a-button>
    </div>
    <a-table
      row-key="id"
      size="small"
      class="items-table"
      :columns="columns"
      :data-source="filtered"
      :loading="loading"
      :pagination="{ pageSize: 20, showSizeChanger: false, hideOnSinglePage: true }"
      :scroll="{ x: 1200 }"
      :locale="{ emptyText: warehouseId ? '寄售仓没有库存' : `请先选择${conf.warehouseLabel}` }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'material'">
          <div>{{ record.materialCode }} {{ record.materialName }}</div>
          <div class="sub-text">{{ record.spec }}</div>
          <a-tag v-if="record.serialEnabled === 1" color="purple" :bordered="false">序列号</a-tag>
        </template>
        <template v-else-if="column.key === 'slot'">
          {{ locationText(record.locationCode) }}
          <div v-if="record.batchNo" class="sub-text">批次 {{ record.batchNo }}</div>
        </template>
        <template v-else-if="column.key === 'stock'">
          {{ formatQty(availableQty(record)) }} {{ record.unit }}
          <div v-if="Number(record.frozenQty)" class="sub-text">冻结 {{ formatQty(record.frozenQty) }}</div>
        </template>
        <template v-else-if="column.key === 'qty'">
          <a-input-number
            v-model:value="record.settleQty"
            :min="0"
            :max="availableQty(record)"
            :precision="record.serialEnabled === 1 ? 0 : 4"
            :placeholder="conf.qtyLabel"
            class="full"
          />
        </template>
        <template v-else-if="column.key === 'price'">
          <a-input-number v-model:value="record.price" :min="0" :precision="4" prefix="¥" :placeholder="conf.pricePlaceholder" class="full" />
        </template>
        <template v-else-if="column.key === 'taxRate'">
          <a-input-number v-model:value="record.taxRate" :min="0" :max="100" :precision="2" addon-after="%" placeholder="默认" class="full" />
        </template>
        <template v-else-if="column.key === 'serialNos'">
          <SerialInput
            v-if="record.serialEnabled === 1"
            v-model:value="record.serialNos"
            :source="{ warehouseId, locationId: record.locationId, materialId: record.materialId, batchNo: record.batchNo || undefined }"
            :expected="Number(record.settleQty) || undefined"
            :max-tag-count="3"
            placeholder="选择或扫码"
          />
          <span v-else class="sub-text">-</span>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { CheckOutlined } from '@ant-design/icons-vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import SerialInput from '@/components/wms/SerialInput.vue'
import { pageStock } from '@/api/wms/stock'
import { consignCustomer, consignSupplier } from '@/api/scm/order'
import { availableQty, formatQty, locationText, splitSerials, today } from '@/components/wms/wms'
import { qty4 } from './scm'

/**
 * 寄售结算：mode = customer（客户寄售，售出后生成销售订单并从寄售仓出库）
 * | supplier（供应商寄售 / VMI，领用后生成采购订单、入库到自有仓库，寄售仓做寄售消耗出库）
 */
const props = defineProps({
  mode: { type: String, required: true }
})

const CONF = {
  customer: {
    warehouseType: 'CONSIGN_OUT',
    warehouseLabel: '客户寄售仓',
    qtyLabel: '售出数量',
    pricePlaceholder: '空为价目表',
    orderLabel: '销售订单',
    orderPath: '/scm/sales',
    submitLabel: '结算售出',
    tip: '客户寄售：先把货物调拨到客户寄售仓（仍是本公司存货），客户售出后在此结算，生成销售订单并从寄售仓出库（形成应收）。单价为含税单价，不填按销售价目表。'
  },
  supplier: {
    warehouseType: 'CONSIGN_IN',
    warehouseLabel: '供应商寄售仓',
    qtyLabel: '领用数量',
    pricePlaceholder: '空为供货价格',
    orderLabel: '采购订单',
    orderPath: '/scm/purchase',
    submitLabel: '结算领用',
    tip: '供应商寄售（VMI）：供应商的货寄售入库到供应商寄售仓（不计成本），领用后在此结算，生成采购订单、采购入库到自有仓库（形成应付），寄售仓做寄售消耗出库。单价为含税单价，不填按供货价格。'
  }
}
const conf = computed(() => CONF[props.mode])
/** 自有仓库（不含寄售仓） */
const OWN_TYPES = ['NORMAL', 'RAW', 'FINISHED', 'DEFECT']

const columns = [
  { title: '物料', key: 'material', width: 220 },
  { title: '库位 / 批次', key: 'slot', width: 140 },
  { title: '寄售库存', key: 'stock', width: 120, align: 'right' },
  { title: CONF[props.mode].qtyLabel, key: 'qty', width: 130 },
  { title: '含税单价', key: 'price', width: 140 },
  { title: '税率', key: 'taxRate', width: 110 },
  { title: '序列号', key: 'serialNos', width: 260 }
]

const router = useRouter()
const warehouseId = ref()
const targetWarehouseId = ref()
const bizDate = ref(today())
const remark = ref('')
const keyword = ref('')
const stock = ref([])
const loading = ref(false)
const saving = ref(false)
const result = ref(null)

const filtered = computed(() => {
  const k = keyword.value?.trim().toLowerCase()
  if (!k) return stock.value
  return stock.value.filter((s) => `${s.materialCode} ${s.materialName}`.toLowerCase().includes(k))
})
const picked = computed(() => stock.value.filter((s) => Number(s.settleQty) > 0))
const pickedQty = computed(() => qty4(picked.value.reduce((sum, s) => sum + Number(s.settleQty), 0)))

async function loadStock() {
  stock.value = []
  if (!warehouseId.value) return
  loading.value = true
  try {
    const page = await pageStock({ warehouseId: warehouseId.value, current: 1, size: 500 })
    stock.value = page.records
      .filter((s) => availableQty(s) > 0)
      .map((s) => ({ ...s, settleQty: undefined, price: undefined, taxRate: undefined, serialNos: undefined }))
  } finally {
    loading.value = false
  }
}

function onWarehouseChange(id) {
  warehouseId.value = id
  result.value = null
  loadStock()
}

const toOrder = () => router.push({ path: conf.value.orderPath, query: { id: result.value.orderId } })

function validate() {
  if (!warehouseId.value) return `请选择${conf.value.warehouseLabel}`
  if (props.mode === 'supplier' && !targetWarehouseId.value) return '请选择转入仓库'
  if (!bizDate.value) return '请选择业务日期'
  for (const s of picked.value) {
    if (Number(s.settleQty) > availableQty(s)) return `${s.materialCode} 的数量超过寄售库存`
    if (s.serialEnabled === 1 && splitSerials(s.serialNos).length !== Number(s.settleQty)) {
      return `${s.materialCode} 需录入 ${formatQty(s.settleQty)} 个序列号`
    }
  }
  return null
}

function submit() {
  const error = validate()
  if (error) {
    message.warning(error)
    return
  }
  Modal.confirm({
    title: `${conf.value.submitLabel}？`,
    content: `共 ${picked.value.length} 行、数量 ${formatQty(pickedQty.value)}，将生成${conf.value.orderLabel}及出入库单据。`,
    async onOk() {
      saving.value = true
      try {
        const data = {
          warehouseId: warehouseId.value,
          bizDate: bizDate.value,
          remark: remark.value || null,
          items: picked.value.map((s) => ({
            materialId: s.materialId,
            qty: s.settleQty,
            price: s.price ?? null,
            taxRate: s.taxRate ?? null,
            locationId: s.locationId || null,
            batchNo: s.batchNo || null,
            serialNos: s.serialEnabled === 1 ? s.serialNos || null : null
          }))
        }
        result.value =
          props.mode === 'customer'
            ? await consignCustomer(data)
            : await consignSupplier({ ...data, targetWarehouseId: targetWarehouseId.value })
        message.success(`结算成功，已生成${conf.value.orderLabel}`)
        remark.value = ''
        loadStock()
      } finally {
        saving.value = false
      }
    }
  })
}
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.section-title {
  font-weight: 500;
}
.filter {
  width: 220px;
}
.items-table :deep(.ant-table-cell) {
  vertical-align: top;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
