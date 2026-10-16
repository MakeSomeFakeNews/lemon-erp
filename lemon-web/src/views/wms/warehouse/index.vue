<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="仓库" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="编码或名称" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.sys_common_status, true)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll"
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="'wms:warehouse:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>新增仓库
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1200, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div>{{ record.name }}</div>
            <div class="sub-text">{{ record.code }}</div>
          </template>
          <template v-else-if="column.key === 'type'">
            <DictTag type="wms_warehouse_type" :value="record.type" />
            <div v-if="CONSIGN_PARTNER[record.type] && record.partnerId" class="sub-text">{{ partnerText(record) }}</div>
          </template>
          <template v-else-if="column.key === 'contact'">
            <div>{{ record.contact || '-' }}</div>
            <div v-if="record.phone" class="sub-text">{{ record.phone }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch
              :value="record.status"
              :name="record.name"
              :api="(s) => changeWarehouseStatus(record.id, s)"
              :disabled="!userStore.hasPerm('wms:warehouse:edit')"
              @changed="(s) => (record.status = s)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-perm="'wms:location:list'" type="link" size="small" @click="toLocations(record)">
                <template #icon><EnvironmentOutlined /></template>库位
              </a-button>
              <a-button v-perm="'wms:warehouse:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <span v-perm="'wms:warehouse:remove'">
                <a-popconfirm :title="`确定删除仓库「${record.name}」及其库位？`" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small">
                    <template #icon><DeleteOutlined /></template>删除
                  </a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增仓库" edit-title="编辑仓库">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="仓库编码" name="code">
              <a-input v-model:value="form.code" :maxlength="32" placeholder="如：WH01" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="仓库名称" name="name">
              <a-input v-model:value="form.name" :maxlength="64" placeholder="如：总部成品仓" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="仓库类型" name="type">
              <template #extra>
                <a-tooltip v-if="CONSIGN_PARTNER[form.type]" title="寄售仓不计入可用库存；供应商寄售仓的库存属于供应商，不计入存货成本">
                  <span class="type-hint"><InfoCircleOutlined /> 寄售仓不计入可用库存</span>
                </a-tooltip>
                <a-tooltip v-else title="不良品仓的库存不计入 MRP、齐套、可承诺量">
                  <span class="type-hint"><InfoCircleOutlined /> 不良品仓的库存不参与计划</span>
                </a-tooltip>
              </template>
              <a-select v-model:value="form.type" :options="toOptions(dict.wms_warehouse_type)" @change="form.partnerId = undefined" />
            </a-form-item>
          </a-col>
          <a-col v-if="CONSIGN_PARTNER[form.type]" :xs="24" :sm="12">
            <a-form-item :label="CONSIGN_PARTNER[form.type].label" name="partnerId">
              <PartnerSelect
                :key="form.type"
                v-model:value="form.partnerId"
                :type="CONSIGN_PARTNER[form.type].type"
                :label="partnerText(form)"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="联系人" name="contact">
              <a-input v-model:value="form.contact" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="联系电话" name="phone">
              <a-input v-model:value="form.phone" :maxlength="32" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="地址" name="address">
              <a-input v-model:value="form.address" :maxlength="255" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="排序" name="sort">
              <a-input-number v-model:value="form.sort" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-textarea v-model:value="form.remark" :rows="3" :maxlength="255" show-count />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, EnvironmentOutlined, InfoCircleOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { partnerOptions } from '@/api/scm/partner'
import { addWarehouse, changeWarehouseStatus, deleteWarehouse, getWarehouse, pageWarehouse, updateWarehouse } from '@/api/wms/warehouse'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'

const router = useRouter()
const userStore = useUserStore()
const dict = useDict('sys_common_status', 'wms_warehouse_type')

const columns = [
  { title: '仓库', key: 'name', width: 200 },
  { title: '类型', key: 'type', width: 150, dict: 'wms_warehouse_type' },
  { title: '地址', dataIndex: 'address', width: 240, ellipsis: true },
  { title: '联系人', key: 'contact', width: 140 },
  { title: '排序', dataIndex: 'sort', width: 70 },
  { title: '状态', key: 'status', width: 90 },
  { title: '备注', dataIndex: 'remark', width: 160, ellipsis: true },
  { title: '操作', key: 'action', width: 220, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'wms-warehouse')
const showSearch = ref(true)

const query = reactive({ keyword: undefined, status: undefined })
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageWarehouse, query)

function resetQuery() {
  Object.assign(query, { keyword: undefined, status: undefined })
  search()
}

/** 寄售仓的往来单位：客户寄售仓 → 客户，供应商寄售仓 → 供应商 */
const CONSIGN_PARTNER = {
  CONSIGN_OUT: { type: 'customer', label: '寄售客户' },
  CONSIGN_IN: { type: 'supplier', label: '寄售供应商' }
}
// 往来单位名称：列表只返回 partnerId，按客户 / 供应商下拉选项映射名称
const partnerNames = reactive({ customer: new Map(), supplier: new Map() })
const partnerText = (w) => {
  const c = CONSIGN_PARTNER[w.type]
  if (!c || !w.partnerId) return undefined
  return partnerNames[c.type].get(w.partnerId) || `#${w.partnerId}`
}
async function loadPartnerNames() {
  await Promise.all(
    ['customer', 'supplier'].map(async (type) => {
      const list = await partnerOptions(type).catch(() => [])
      partnerNames[type] = new Map(list.map((p) => [p.id, `${p.code} ${p.name}`]))
    })
  )
}

onMounted(() => {
  load()
  loadPartnerNames()
})

const rules = {
  code: [
    { required: true, message: '请输入仓库编码' },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: '只能包含字母、数字、下划线和短横线' }
  ],
  name: [{ required: true, whitespace: true, message: '请输入仓库名称' }],
  partnerId: [{ required: true, message: '请选择寄售仓的往来单位' }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, code: '', name: '', type: 'NORMAL', partnerId: undefined, contact: '', phone: '', address: '', sort: 0, remark: '' }),
  fetch: (record) => getWarehouse(record.id),
  create: addWarehouse,
  update: updateWarehouse,
  toPayload: ({ code, name, type, partnerId, contact, phone, address, sort, remark }) => ({
    code,
    name,
    type: type || 'NORMAL',
    partnerId: CONSIGN_PARTNER[type] ? partnerId : null,
    contact,
    phone,
    address,
    sort,
    remark
  }),
  onSuccess: load
})
const { formRef, form } = drawer

const toLocations = (record) => router.push({ path: '/wms/location', query: { warehouseId: record.id } })

async function remove(id) {
  await deleteWarehouse(id)
  message.success('删除成功')
  load()
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
.type-hint {
  cursor: help;
}
</style>
