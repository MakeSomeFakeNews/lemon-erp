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
              :placeholder="$t('common.placeholder.selectName', { name: conf.warehouseLabel })"
              @update:value="onWarehouseChange"
            />
          </a-form-item>
        </a-col>
        <a-col v-if="mode === 'supplier'" :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.consign.targetWarehouse')" required :extra="$t('scm.consign.targetWarehouseExtra')">
            <WarehouseSelect v-model:value="targetWarehouseId" :types="OWN_TYPES" :placeholder="$t('scm.consign.selectOwnWarehouse')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.consign.bizDate')" required>
            <a-date-picker v-model:value="bizDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="mode === 'supplier' ? 6 : 12">
          <a-form-item :label="$t('common.field.remark')">
            <a-input v-model:value="remark" :maxlength="255" :placeholder="$t('scm.consign.optional')" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>

    <a-alert v-if="result" type="success" show-icon closable class="block" @close="result = null">
      <template #message>
        {{ $t('scm.consign.done', { order: conf.orderLabel }) }}
        <a @click="toOrder">{{ $t('scm.consign.viewOrder', { order: conf.orderLabel }) }}</a>
        <span v-if="result.consumeDocId" class="sub-text">{{ $t('scm.consign.consumeDone') }}</span>
      </template>
    </a-alert>

    <div class="items-head">
      <a-space wrap>
        <span class="section-title">{{ $t('scm.consign.stockTitle') }}</span>
        <a-input-search v-model:value="keyword" :placeholder="$t('scm.consign.materialKeyword')" allow-clear class="filter" />
        <span class="sub-text">{{ $t('scm.consign.picked', { n: picked.length, qty: formatQty(pickedQty) }) }}</span>
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
      :locale="{ emptyText: warehouseId ? $t('scm.consign.noStock') : $t('scm.consign.selectFirst', { name: conf.warehouseLabel }) }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'material'">
          <div>{{ record.materialCode }} {{ record.materialName }}</div>
          <div class="sub-text">{{ record.spec }}</div>
          <a-tag v-if="record.serialEnabled === 1" color="purple" :bordered="false">{{ $t('scm.consign.serialNo') }}</a-tag>
        </template>
        <template v-else-if="column.key === 'slot'">
          {{ locationText(record.locationCode) }}
          <div v-if="record.batchNo" class="sub-text">{{ $t('scm.consign.batch', { no: record.batchNo }) }}</div>
        </template>
        <template v-else-if="column.key === 'stock'">
          {{ formatQty(availableQty(record)) }} {{ record.unit }}
          <div v-if="Number(record.frozenQty)" class="sub-text">{{ $t('scm.consign.frozen', { qty: formatQty(record.frozenQty) }) }}</div>
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
          <a-input-number v-model:value="record.taxRate" :min="0" :max="100" :precision="2" addon-after="%" :placeholder="$t('scm.consign.default')" class="full" />
        </template>
        <template v-else-if="column.key === 'serialNos'">
          <SerialInput
            v-if="record.serialEnabled === 1"
            v-model:value="record.serialNos"
            :source="{ warehouseId, locationId: record.locationId, materialId: record.materialId, batchNo: record.batchNo || undefined }"
            :expected="Number(record.settleQty) || undefined"
            :max-tag-count="3"
            :placeholder="$t('scm.consign.selectOrScan')"
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
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import { CheckOutlined } from '@ant-design/icons-vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import SerialInput from '@/components/wms/SerialInput.vue'
import { pageStock } from '@/api/wms/stock'
import { consignCustomer, consignSupplier } from '@/api/scm/order'
import { availableQty, formatQty, locationText, splitSerials, today } from '@/components/wms/wms'
import { qty4 } from './scm'

import { WarehouseType } from '@/constants/enums'
/**
 * 寄售结算：mode = customer（客户寄售，售出后生成销售订单并从寄售仓出库）
 * | supplier（供应商寄售 / VMI，领用后生成采购订单、入库到自有仓库，寄售仓做寄售消耗出库）
 */
const props = defineProps({
  mode: { type: String, required: true }
})

const { t } = useI18n()
const CONF = {
  customer: {
    warehouseType: WarehouseType.CONSIGN_OUT,
    warehouseLabel: t('scm.consign.customerWarehouse'),
    qtyLabel: t('scm.consign.soldQty'),
    pricePlaceholder: t('scm.consign.pricePriceList'),
    orderLabel: t('scm.consign.salesOrder'),
    orderPath: '/scm/sales',
    submitLabel: t('scm.consign.settleSold'),
    tip: t('scm.consign.customerTip')
  },
  supplier: {
    warehouseType: WarehouseType.CONSIGN_IN,
    warehouseLabel: t('scm.consign.supplierWarehouse'),
    qtyLabel: t('scm.consign.usedQty'),
    pricePlaceholder: t('scm.consign.priceSupply'),
    orderLabel: t('scm.consign.purchaseOrder'),
    orderPath: '/scm/purchase',
    submitLabel: t('scm.consign.settleUsed'),
    tip: t('scm.consign.supplierTip')
  }
}
const conf = computed(() => CONF[props.mode])
/** 自有仓库（不含寄售仓） */
const OWN_TYPES = [WarehouseType.NORMAL, WarehouseType.RAW, WarehouseType.FINISHED, WarehouseType.DEFECT]

const columns = [
  { title: t('scm.consign.material'), key: 'material', width: 220 },
  { title: t('scm.consign.slot'), key: 'slot', width: 150 },
  { title: t('scm.consign.consignStock'), key: 'stock', width: 130, align: 'right' },
  { title: CONF[props.mode].qtyLabel, key: 'qty', width: 130 },
  { title: t('scm.consign.taxPrice'), key: 'price', width: 140 },
  { title: t('scm.consign.taxRate'), key: 'taxRate', width: 110 },
  { title: t('scm.consign.serialNo'), key: 'serialNos', width: 260 }
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
  if (!warehouseId.value) return t('common.placeholder.selectName', { name: conf.value.warehouseLabel })
  if (props.mode === 'supplier' && !targetWarehouseId.value) return t('scm.consign.selectTarget')
  if (!bizDate.value) return t('scm.consign.selectBizDate')
  for (const s of picked.value) {
    if (Number(s.settleQty) > availableQty(s)) return t('scm.consign.overStock', { code: s.materialCode })
    if (s.serialEnabled === 1 && splitSerials(s.serialNos).length !== Number(s.settleQty)) {
      return t('scm.consign.needSerials', { code: s.materialCode, n: formatQty(s.settleQty) })
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
    title: t('scm.consign.confirmTitle', { action: conf.value.submitLabel }),
    content: t('scm.consign.confirmContent', { n: picked.value.length, qty: formatQty(pickedQty.value), order: conf.value.orderLabel }),
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
        message.success(t('scm.consign.success', { order: conf.value.orderLabel }))
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
