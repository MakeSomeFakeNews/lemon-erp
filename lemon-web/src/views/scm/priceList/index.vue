<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('scm.priceList.material')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('scm.priceList.materialKeyword')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('scm.priceList.customerLevel')" name="level">
          <a-select v-model:value="query.level" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.scm_customer_level)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :exporter="exportAll"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="'scm:price:edit'" type="primary" @click="openForm()">
          <template #icon><PlusOutlined /></template>{{ $t('scm.priceList.add') }}
        </a-button>
        <a-button @click="controlOpen = true">
          <template #icon><SafetyOutlined /></template>{{ $t('scm.priceList.priceControl') }}
        </a-button>
        <span v-perm="'scm:price:edit'">
          <a-popconfirm
            :title="$t('scm.priceList.confirmDeleteN', { n: selectedKeys.length })"
            :disabled="!selectedKeys.length"
            @confirm="remove(selectedKeys)"
          >
            <a-button danger :disabled="!selectedKeys.length">
              <template #icon><DeleteOutlined /></template>{{ $t('common.action.batchDelete') }}
            </a-button>
          </a-popconfirm>
        </span>
        <a-alert
          type="info"
          show-icon
          :message="$t('scm.priceList.matchTip')"
        />
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        :scroll="{ x: 1200, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ record.spec }}</div>
          </template>
          <template v-else-if="column.key === 'customerLevel'">
            <a-tag v-if="record.customerId" color="purple" :bordered="false">{{ $t('scm.priceList.exclusive') }}</a-tag>
            <span v-if="record.customerId">{{ record.customerName }}</span>
            <DictTag v-else-if="record.customerLevel" type="scm_customer_level" :value="record.customerLevel" />
            <span v-else class="sub-text">{{ $t('scm.priceList.allCustomers') }}</span>
          </template>
          <template v-else-if="column.key === 'minQty'">
            {{ Number(record.minQty) ? `≥ ${formatQty(record.minQty)}` : $t('scm.priceList.unlimited') }}
          </template>
          <template v-else-if="column.key === 'price'">
            <div>¥{{ formatPrice(record.price) }} / {{ record.unit || $t('common.field.unit') }}</div>
            <div class="sub-text">{{ $t('scm.priceList.taxRateOf', { rate: Number(record.taxRate) }) }}</div>
          </template>
          <template v-else-if="column.key === 'validity'">
            <span>{{ validityText(record) }}</span>
            <a-tag v-if="validityTag(record)" :color="validityTag(record).color" :bordered="false" class="tag-gap">
              {{ validityTag(record).text }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <span v-perm="'scm:price:edit'" class="table-actions">
              <a-button type="link" size="small" @click="openForm(record)">
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <a-popconfirm :title="$t('scm.priceList.confirmDelete')" @confirm="remove([record.id])">
                <a-button type="link" danger size="small">
                  <template #icon><DeleteOutlined /></template>{{ $t('common.action.delete') }}
                </a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="formOpen" :title="form.id ? $t('scm.priceList.editTitle') : $t('scm.priceList.add')" :confirm-loading="saving" @ok="submit">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('scm.priceList.material')" name="materialId">
          <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('scm.priceList.customerExclusive')" name="customerId" :extra="$t('scm.priceList.customerExclusiveTip')">
              <PartnerSelect v-model:value="form.customerId" type="customer" :label="form.customerName" allow-clear :placeholder="$t('scm.priceList.notSpecified')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('scm.priceList.customerLevel')" name="customerLevel">
              <a-select
                v-model:value="form.customerLevel"
                :placeholder="$t('scm.priceList.allCustomers')"
                allow-clear
                :disabled="!!form.customerId"
                :options="toOptions(dict.scm_customer_level)"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('scm.priceList.minQty')" name="minQty">
              <a-input-number v-model:value="form.minQty" :min="0" :precision="4" :placeholder="$t('scm.priceList.unlimited')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('scm.priceList.taxPrice')" name="price">
              <a-input-number v-model:value="form.price" :min="0" :precision="4" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('scm.priceList.taxRate')" name="taxRate">
              <a-input-number v-model:value="form.taxRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('scm.priceList.validFrom')" name="validFrom">
              <a-date-picker v-model:value="form.validFrom" value-format="YYYY-MM-DD" :placeholder="$t('scm.priceList.unlimited')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('scm.priceList.validTo')" name="validTo">
              <a-date-picker v-model:value="form.validTo" value-format="YYYY-MM-DD" :placeholder="$t('scm.priceList.unlimited')" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('common.field.remark')" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>
    <PriceControlDrawer v-model:open="controlOpen" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, EditOutlined, PlusOutlined, SafetyOutlined } from '@ant-design/icons-vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import PriceControlDrawer from '@/components/scm/PriceControlDrawer.vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { addPriceList, deletePriceList, pagePriceList, updatePriceList } from '@/api/scm/sourcing'
import { formatQty, today } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

const { t } = useI18n()
const dict = useDict('scm_customer_level')

const formatPrice = (n) => Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 4 })
const validityText = (r) => (r.validFrom || r.validTo ? `${r.validFrom || t('scm.priceList.unlimited')} ~ ${r.validTo || t('scm.priceList.unlimited')}` : t('scm.priceList.longTerm'))
function validityTag(r) {
  const d = today()
  if (r.validTo && r.validTo < d) return { color: 'red', text: t('scm.priceList.expired') }
  if (r.validFrom && r.validFrom > d) return { color: 'orange', text: t('scm.priceList.notEffective') }
  return null
}

const columns = [
  { title: t('scm.priceList.material'), key: 'material', width: 240, exportValue: (r) => `${r.materialCode} ${r.materialName}` },
  { title: t('scm.priceList.applicableCustomer'), key: 'customerLevel', dataIndex: 'customerLevel', width: 180, dict: 'scm_customer_level' },
  { title: t('scm.priceList.minQty'), key: 'minQty', dataIndex: 'minQty', width: 120, align: 'right' },
  { title: t('scm.priceList.taxPrice'), key: 'price', dataIndex: 'price', width: 160 },
  { title: t('scm.priceList.taxRatePct'), dataIndex: 'taxRate', width: 100, align: 'right' },
  { title: t('scm.priceList.validity'), key: 'validity', width: 240, exportValue: validityText },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 170, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'scm-price-list')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, level: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(
  pagePriceList,
  query
)
onMounted(load)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const controlOpen = ref(false)
const formOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const rules = {
  materialId: [{ required: true, message: t('scm.priceList.materialRequired') }],
  price: [{ required: true, message: t('scm.priceList.priceRequired') }],
  validTo: [
    {
      validator: (_, v) =>
        v && form.value.validFrom && v < form.value.validFrom ? Promise.reject(t('scm.priceList.validToBeforeFrom')) : Promise.resolve()
    }
  ]
}

const num = (v) => (v != null ? Number(v) : undefined)
function openForm(record) {
  form.value = record
    ? {
        ...record,
        materialLabel: `${record.materialCode} ${record.materialName}`,
        customerLevel: record.customerLevel || undefined,
        minQty: Number(record.minQty) ? Number(record.minQty) : undefined,
        price: num(record.price),
        taxRate: num(record.taxRate)
      }
    : { materialId: undefined, customerId: null, customerLevel: undefined, minQty: undefined, price: undefined, taxRate: 13, remark: '' }
  formOpen.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const f = form.value
    const data = {
      materialId: f.materialId,
      customerId: f.customerId || null,
      customerLevel: f.customerId ? null : f.customerLevel || null,
      minQty: f.minQty ?? 0,
      price: f.price,
      taxRate: f.taxRate ?? 0,
      validFrom: f.validFrom || null,
      validTo: f.validTo || null,
      remark: f.remark || null
    }
    f.id ? await updatePriceList(f.id, data) : await addPriceList(data)
    message.success(t('common.msg.saveSuccess'))
    formOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(ids) {
  await deletePriceList(ids)
  message.success(t('common.msg.deleteSuccess'))
  selectedKeys.value = selectedKeys.value.filter((k) => !ids.includes(k))
  load()
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.tag-gap {
  margin-left: 4px;
}
.full {
  width: 100%;
}
</style>
