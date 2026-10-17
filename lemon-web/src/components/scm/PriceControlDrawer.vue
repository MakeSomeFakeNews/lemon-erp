<template>
  <a-drawer :open="open" :title="$t('scm.priceControl.title')" width="min(900px, 100vw)" @close="emit('update:open', false)">
    <a-alert
      type="info"
      show-icon
      class="tip"
      :message="$t('scm.priceControl.tip')"
    />
    <div class="toolbar">
      <a-input-search v-model:value="keyword" :placeholder="$t('scm.priceControl.keywordPh')" allow-clear class="search" @search="search" />
      <a-button v-perm="'scm:price:edit'" type="primary" @click="openForm()">
        <template #icon><PlusOutlined /></template>{{ $t('common.action.add') }}
      </a-button>
    </div>
    <a-table row-key="id" size="small" :columns="columns" :data-source="list" :loading="loading" :pagination="pagination" @change="onTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'material'">
          <div>{{ record.materialCode }} {{ record.materialName }}</div>
          <div class="sub-text">{{ record.spec }}</div>
        </template>
        <template v-else-if="column.key === 'minSalePrice'">{{ record.minSalePrice != null ? `¥${Number(record.minSalePrice)}` : '-' }}</template>
        <template v-else-if="column.key === 'maxPurchasePrice'">{{ record.maxPurchasePrice != null ? `¥${Number(record.maxPurchasePrice)}` : '-' }}</template>
        <template v-else-if="column.key === 'controlMode'"><DictTag type="scm_price_control_mode" :value="record.controlMode" /></template>
        <template v-else-if="column.key === 'action'">
          <span v-perm="'scm:price:edit'" class="table-actions">
            <a-button type="link" size="small" @click="openForm(record)">{{ $t('common.action.edit') }}</a-button>
            <a-popconfirm :title="$t('scm.priceControl.confirmDelete')" @confirm="remove(record.id)">
              <a-button type="link" danger size="small">{{ $t('common.action.delete') }}</a-button>
            </a-popconfirm>
          </span>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="formOpen" :title="form.id ? $t('scm.priceControl.editTitle') : $t('scm.priceControl.addTitle')" :confirm-loading="saving" @ok="submit">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-form-item :label="$t('scm.priceControl.material')" name="materialId" :rules="[{ required: true, message: $t('scm.priceControl.selectMaterial') }]">
          <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" :disabled="!!form.id" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('scm.priceControl.minSalePriceTax')" name="minSalePrice">
              <a-input-number v-model:value="form.minSalePrice" :min="0" :precision="4" prefix="¥" :placeholder="$t('scm.priceControl.noControl')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('scm.priceControl.maxPurchasePriceTax')" name="maxPurchasePrice">
              <a-input-number v-model:value="form.maxPurchasePrice" :min="0" :precision="4" prefix="¥" :placeholder="$t('scm.priceControl.noControl')" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('scm.priceControl.controlMode')" name="controlMode">
          <a-radio-group v-model:value="form.controlMode" button-style="solid">
            <a-radio-button value="WARN">{{ $t('scm.priceControl.warn') }}</a-radio-button>
            <a-radio-button value="BLOCK">{{ $t('scm.priceControl.block') }}</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('common.field.remark')" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-drawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { addPriceControl, deletePriceControl, pagePriceControl, updatePriceControl } from '@/api/scm/sourcing'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'

const { t } = useI18n()

/** 价格控制：物料的最低销售价、最高采购价及控制方式 */
const props = defineProps({ open: Boolean })
const emit = defineEmits(['update:open'])
useDict('scm_price_control_mode')

const columns = [
  { title: t('scm.priceControl.material'), key: 'material', width: 240 },
  { title: t('scm.priceControl.minSalePrice'), key: 'minSalePrice', width: 140, align: 'right' },
  { title: t('scm.priceControl.maxPurchasePrice'), key: 'maxPurchasePrice', width: 150, align: 'right' },
  { title: t('scm.priceControl.controlMode'), key: 'controlMode', width: 120 },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 120 }
]
const keyword = ref('')
const query = reactive({ keyword: undefined })
const { list, loading, pagination, load, search: doSearch, onTableChange } = useTable(pagePriceControl, query)
function search() {
  query.keyword = keyword.value?.trim() || undefined
  doSearch()
}
watch(
  () => props.open,
  (v) => v && load()
)

const formOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
function openForm(record) {
  form.value = record
    ? {
        ...record,
        materialLabel: `${record.materialCode} ${record.materialName}`,
        minSalePrice: record.minSalePrice != null ? Number(record.minSalePrice) : undefined,
        maxPurchasePrice: record.maxPurchasePrice != null ? Number(record.maxPurchasePrice) : undefined
      }
    : { materialId: undefined, minSalePrice: undefined, maxPurchasePrice: undefined, controlMode: 'WARN', remark: '' }
  formOpen.value = true
}
async function submit() {
  await formRef.value.validate()
  const f = form.value
  if (f.minSalePrice == null && f.maxPurchasePrice == null) {
    message.warning(t('scm.priceControl.atLeastOne'))
    return
  }
  saving.value = true
  try {
    const data = {
      materialId: f.materialId,
      minSalePrice: f.minSalePrice ?? null,
      maxPurchasePrice: f.maxPurchasePrice ?? null,
      controlMode: f.controlMode,
      remark: f.remark || null
    }
    f.id ? await updatePriceControl(f.id, data) : await addPriceControl(data)
    message.success(t('common.msg.saveSuccess'))
    formOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function remove(id) {
  await deletePriceControl(id)
  message.success(t('common.msg.deleteSuccess'))
  load()
}
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.search {
  width: 260px;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
