<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="物料" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="物料编码或名称" allow-clear />
        </a-form-item>
        <a-form-item label="客户等级" name="level">
          <a-select v-model:value="query.level" placeholder="全部" allow-clear :options="toOptions(dict.scm_customer_level)" />
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
          <template #icon><PlusOutlined /></template>新增价目
        </a-button>
        <a-button @click="controlOpen = true">
          <template #icon><SafetyOutlined /></template>价格控制
        </a-button>
        <span v-perm="'scm:price:edit'">
          <a-popconfirm
            :title="`确定删除选中的 ${selectedKeys.length} 条价目？`"
            :disabled="!selectedKeys.length"
            @confirm="remove(selectedKeys)"
          >
            <a-button danger :disabled="!selectedKeys.length">
              <template #icon><DeleteOutlined /></template>批量删除
            </a-button>
          </a-popconfirm>
        </span>
        <a-alert
          type="info"
          show-icon
          message="销售订单、报价单选择物料后按客户、数量、日期匹配价目：客户专属价优先，其次指定等级，再其次全部客户，起订数量取满足条件的最高档。"
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
            <a-tag v-if="record.customerId" color="purple" :bordered="false">专属</a-tag>
            <span v-if="record.customerId">{{ record.customerName }}</span>
            <DictTag v-else-if="record.customerLevel" type="scm_customer_level" :value="record.customerLevel" />
            <span v-else class="sub-text">全部客户</span>
          </template>
          <template v-else-if="column.key === 'minQty'">
            {{ Number(record.minQty) ? `≥ ${formatQty(record.minQty)}` : '不限' }}
          </template>
          <template v-else-if="column.key === 'price'">
            <div>¥{{ formatPrice(record.price) }} / {{ record.unit || '单位' }}</div>
            <div class="sub-text">税率 {{ Number(record.taxRate) }}%</div>
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
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <a-popconfirm title="确定删除该价目？" @confirm="remove([record.id])">
                <a-button type="link" danger size="small">
                  <template #icon><DeleteOutlined /></template>删除
                </a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="formOpen" :title="form.id ? '编辑价目' : '新增价目'" :confirm-loading="saving" @ok="submit">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="物料" name="materialId">
          <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="客户专属" name="customerId" extra="指定客户时只适用该客户，优先于客户等级">
              <PartnerSelect v-model:value="form.customerId" type="customer" :label="form.customerName" allow-clear placeholder="不指定" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="客户等级" name="customerLevel">
              <a-select
                v-model:value="form.customerLevel"
                placeholder="全部客户"
                allow-clear
                :disabled="!!form.customerId"
                :options="toOptions(dict.scm_customer_level)"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="起订数量" name="minQty">
              <a-input-number v-model:value="form.minQty" :min="0" :precision="4" placeholder="不限" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="含税单价" name="price">
              <a-input-number v-model:value="form.price" :min="0" :precision="4" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="税率" name="taxRate">
              <a-input-number v-model:value="form.taxRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="有效期起" name="validFrom">
              <a-date-picker v-model:value="form.validFrom" value-format="YYYY-MM-DD" placeholder="不限" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="有效期止" name="validTo">
              <a-date-picker v-model:value="form.validTo" value-format="YYYY-MM-DD" placeholder="不限" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注" name="remark">
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

const dict = useDict('scm_customer_level')

const formatPrice = (n) => Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 4 })
const validityText = (r) => (r.validFrom || r.validTo ? `${r.validFrom || '不限'} ~ ${r.validTo || '不限'}` : '长期有效')
function validityTag(r) {
  const d = today()
  if (r.validTo && r.validTo < d) return { color: 'red', text: '已过期' }
  if (r.validFrom && r.validFrom > d) return { color: 'orange', text: '未生效' }
  return null
}

const columns = [
  { title: '物料', key: 'material', width: 240, exportValue: (r) => `${r.materialCode} ${r.materialName}` },
  { title: '适用客户', key: 'customerLevel', dataIndex: 'customerLevel', width: 180, dict: 'scm_customer_level' },
  { title: '起订数量', key: 'minQty', dataIndex: 'minQty', width: 110, align: 'right' },
  { title: '含税单价', key: 'price', dataIndex: 'price', width: 160 },
  { title: '税率(%)', dataIndex: 'taxRate', width: 80, align: 'right' },
  { title: '有效期', key: 'validity', width: 240, exportValue: validityText },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
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
  materialId: [{ required: true, message: '请选择物料' }],
  price: [{ required: true, message: '请填写单价' }],
  validTo: [
    {
      validator: (_, v) =>
        v && form.value.validFrom && v < form.value.validFrom ? Promise.reject('有效期止不能早于有效期起') : Promise.resolve()
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
    message.success('保存成功')
    formOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(ids) {
  await deletePriceList(ids)
  message.success('删除成功')
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
