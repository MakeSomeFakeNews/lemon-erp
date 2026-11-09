<template>
  <a-drawer :open="open" :title="$t('pricing.customerPrice.title', { name: customer?.name || '' })" width="min(880px, 100vw)" destroy-on-close @close="open = false">
    <a-alert type="info" show-icon class="tip" :message="$t('pricing.customerPrice.tip')" />
    <a-space class="tip">
      <a-button v-if="editable" size="small" @click="importOpen = true"><ImportOutlined />{{ $t('common.action.import') }}</a-button>
      <a-button size="small" :disabled="!form.items.length" @click="exportRows"><ExportOutlined />{{ $t('common.action.export') }}</a-button>
    </a-space>
    <a-spin :spinning="loading">
      <a-form ref="formRef" :model="form">
        <LineItemsTable :items="form.items" :columns="columns" :empty-text="$t('pricing.customerPrice.empty')">
          <template #cell="{ column, record, index }">
            <template v-if="column.key === 'material'">
              <a-form-item v-if="editable" :name="['items', index, 'materialId']" :rules="[{ required: true, message: $t('pricing.customerPrice.selectMaterial') }]" class="cell-item">
                <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => (record.materialLabel = m.label)" />
              </a-form-item>
              <span v-else>{{ record.materialLabel }}</span>
            </template>
            <template v-else-if="column.key === 'price'">
              <a-form-item v-if="editable" :name="['items', index, 'price']" :rules="[{ required: true, message: $t('pricing.customerPrice.inputPrice') }]" class="cell-item">
                <a-input-number v-model:value="record.price" :min="0" :precision="4" prefix="¥" class="full" />
              </a-form-item>
              <span v-else>{{ price(record.price) }}</span>
            </template>
            <template v-else-if="column.key === 'taxRate'">
              <a-input-number v-if="editable" v-model:value="record.taxRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
              <span v-else>{{ record.taxRate != null ? `${record.taxRate}%` : '-' }}</span>
            </template>
            <template v-else-if="column.key === 'remark'">
              <a-input v-if="editable" v-model:value="record.remark" :maxlength="255" />
              <span v-else>{{ record.remark }}</span>
            </template>
          </template>
        </LineItemsTable>
      </a-form>
      <a-button v-if="editable" type="dashed" block class="add-row" @click="lines.addRow()">
        <template #icon><PlusOutlined /></template>{{ $t('pricing.customerPrice.add') }}
      </a-button>
    </a-spin>
    <template v-if="editable" #footer>
      <a-space class="footer">
        <a-button @click="open = false">{{ $t('common.action.cancel') }}</a-button>
        <a-button type="primary" :loading="saving" @click="save">{{ $t('common.action.save') }}</a-button>
      </a-space>
    </template>
    <ImportModal
      v-model:open="importOpen"
      :title="$t('pricing.customerPrice.importTitle')"
      :columns="importColumns"
      :importer="importRows"
      :sample="priceImportSample"
      @success="reload"
    />
  </a-drawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { ExportOutlined, ImportOutlined, PlusOutlined } from '@ant-design/icons-vue'
import ImportModal from '@/components/ImportModal.vue'
import { CUSTOMER_PRICE_COLUMNS, exportCustomerPriceRows, priceImportSample } from './customerPrices'
import LineItemsTable from '@/components/common/LineItemsTable.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { customerPrices, importCustomerPrices, saveCustomerPrices } from '@/api/scm/pricing'
import { useLineItems } from '@/composables/useLineItems'
import { useUserStore } from '@/store/user'
import { price } from '@/utils/number'

/** 客户执行价：客户 × 物料的含税单价（基本单位），开单默认价最优先。open(customer) 打开 */
const { t } = useI18n()
const userStore = useUserStore()
const editable = computed(() => userStore.hasPerm('scm:customer:edit'))
const open = ref(false)
const loading = ref(false)
const saving = ref(false)
const customer = ref()
const formRef = ref()
const form = ref({ items: [] })
const lines = useLineItems(() => form.value.items, { newRow: () => ({ materialId: undefined, price: undefined, taxRate: undefined, remark: '' }) })

const columns = computed(() => [
  { title: t('pricing.customerPrice.material'), key: 'material', width: 300 },
  { title: t('pricing.customerPrice.price'), key: 'price', width: 150 },
  { title: t('pricing.customerPrice.taxRate'), key: 'taxRate', width: 130 },
  { title: t('pricing.customerPrice.remark'), key: 'remark', width: 180 },
  ...(editable.value ? [{ title: '', key: 'action', width: 50, fixed: 'right' }] : [])
])

async function show(record) {
  customer.value = record
  open.value = true
  await reload()
}

// Excel 导入导出（只含当前客户，导入按物料新增或更新，直接保存；导入后重新读取，未保存的修改会丢弃）
const importOpen = ref(false)
const importColumns = CUSTOMER_PRICE_COLUMNS(false)
const importRows = (data) =>
  importCustomerPrices({ rows: data.rows.map((r) => ({ ...r, data: { ...r.data, customerCode: customer.value.code } })) })
const exportRows = () =>
  exportCustomerPriceRows({ customerId: customer.value.id }, t('pricing.customerPrice.exportOneName', { name: customer.value.name }))

async function reload() {
  form.value = { items: [] }
  loading.value = true
  try {
    const list = await customerPrices(record.id)
    form.value.items = list.map((p) => lines.withKey({
      ...p,
      materialLabel: `${p.materialCode} ${p.materialName}`,
      price: Number(p.price),
      taxRate: p.taxRate != null ? Number(p.taxRate) : undefined
    }))
  } finally {
    loading.value = false
  }
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    await saveCustomerPrices(customer.value.id, form.value.items.map((i) => ({
      materialId: i.materialId,
      price: i.price,
      taxRate: i.taxRate ?? null,
      remark: i.remark || null
    })))
    message.success(t('pricing.customerPrice.saved'))
    open.value = false
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.full {
  width: 100%;
}
.cell-item {
  margin-bottom: 0;
}
.add-row {
  margin-top: 8px;
}
.footer {
  display: flex;
  justify-content: flex-end;
}
</style>
