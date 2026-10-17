<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('wms.common.warehouse')" name="warehouseId">
          <WarehouseSelect v-model:value="query.warehouseId" :placeholder="$t('common.placeholder.all')" @update:value="search" />
        </a-form-item>
        <a-form-item :label="$t('wms.common.location')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('wms.flow.codeOrName')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" />
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
        <a-button v-perm="'wms:location:add'" type="primary" @click="drawer.openCreate({ warehouseId: query.warehouseId })">
          <template #icon><PlusOutlined /></template>{{ $t('wms.location.createTitle') }}
        </a-button>
        <span v-perm="'wms:location:remove'">
          <a-popconfirm
            :title="$t('wms.location.confirmDeleteN', { n: selectedKeys.length })"
            :disabled="!selectedKeys.length"
            @confirm="remove(selectedKeys)"
          >
            <a-button danger :disabled="!selectedKeys.length">
              <template #icon><DeleteOutlined /></template>{{ $t('common.action.batchDelete') }}
            </a-button>
          </a-popconfirm>
        </span>
        <a-button :disabled="!selectedKeys.length" @click="openLabels">
          <template #icon><BarcodeOutlined /></template>{{ $t('wms.location.printLabel') }}{{ selectedKeys.length ? ` (${selectedKeys.length})` : '' }}
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        :scroll="{ x: 1100, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <DictTag type="wms_location_type" :value="record.locType || 'STORAGE'" />
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch
              :value="record.status"
              :name="record.code"
              :api="(s) => changeLocationStatus(record.id, s)"
              :disabled="!userStore.hasPerm('wms:location:edit')"
              @changed="(s) => (record.status = s)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-perm="'wms:location:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <span v-perm="'wms:location:remove'">
                <a-popconfirm :title="$t('wms.location.confirmDelete', { code: record.code })" @confirm="remove([record.id])">
                  <a-button type="link" danger size="small">
                    <template #icon><DeleteOutlined /></template>{{ $t('common.action.delete') }}
                  </a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('wms.location.createTitle')" :edit-title="$t('wms.location.editTitle')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item :label="$t('wms.location.warehouse')" name="warehouseId" :extra="drawer.isEdit.value ? $t('wms.location.warehouseFixed') : ''">
              <WarehouseSelect v-model:value="form.warehouseId" :allow-clear="false" :disabled="drawer.isEdit.value" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('wms.location.code')" name="code">
              <a-input v-model:value="form.code" :maxlength="32" :placeholder="$t('wms.location.codePh')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('wms.location.name')" name="name">
              <a-input v-model:value="form.name" :maxlength="64" :placeholder="$t('wms.common.optional')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.type')" name="locType" :extra="$t('wms.location.typeTip')">
              <a-select v-model:value="form.locType" :options="toOptions(dict.wms_location_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('wms.location.zone')" name="zone">
              <a-input v-model:value="form.zone" :maxlength="32" :placeholder="$t('wms.location.zonePh')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.sort')" name="sort" :extra="$t('wms.location.sortTip')">
              <a-input-number v-model:value="form.sort" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-textarea v-model:value="form.remark" :rows="3" :maxlength="255" show-count />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>
    <LabelPrintModal v-model:open="labelOpen" type-key="label.location" :items="labelItems" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { BarcodeOutlined, DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import LabelPrintModal from '@/components/label/LabelPrintModal.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { addLocation, changeLocationStatus, deleteLocation, getLocation, pageLocation, updateLocation } from '@/api/wms/location'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'

const { t } = useI18n()

const route = useRoute()
const userStore = useUserStore()
const dict = useDict('sys_common_status', 'wms_location_type')

const columns = [
  { title: t('wms.common.warehouse'), dataIndex: 'warehouseName', width: 160 },
  { title: t('wms.location.code'), dataIndex: 'code', width: 140 },
  { title: t('wms.location.name'), dataIndex: 'name', width: 160 },
  { title: t('wms.location.zone'), dataIndex: 'zone', width: 100 },
  { title: t('common.field.type'), key: 'type', width: 100 },
  { title: t('common.field.sort'), dataIndex: 'sort', width: 70 },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 150, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'wms-location')
const showSearch = ref(true)

// 从仓库管理「库位」进入时带上仓库
const initialWarehouse = route.query.warehouseId ? Number(route.query.warehouseId) : undefined
const query = reactive({ warehouseId: initialWarehouse, keyword: undefined, status: undefined })
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(pageLocation, query)

function resetQuery() {
  Object.assign(query, { warehouseId: undefined, keyword: undefined, status: undefined })
  search()
}

onMounted(load)

const rules = {
  warehouseId: [{ required: true, message: t('wms.location.selectWarehouse') }],
  code: [
    { required: true, message: t('wms.location.inputCode') },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: t('wms.location.codeRule') }
  ]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, warehouseId: undefined, code: '', name: '', zone: '', locType: 'STORAGE', sort: 0, remark: '' }),
  fetch: (record) => getLocation(record.id),
  create: addLocation,
  update: updateLocation,
  toPayload: ({ warehouseId, code, name, zone, locType, sort, remark }) => ({ warehouseId, code, name, zone: zone || null, locType, sort, remark }),
  onSuccess: load
})
const { formRef, form } = drawer

async function remove(ids) {
  await deleteLocation(ids)
  message.success(t('common.msg.deleteSuccess'))
  selectedKeys.value = selectedKeys.value.filter((k) => !ids.includes(k))
  load()
}

// 条码标签：选中的行（当前页）
const labelOpen = ref(false)
const labelItems = ref([])
function openLabels() {
  labelItems.value = list.value.filter((r) => selectedKeys.value.includes(r.id)).map((r) => ({ code: r.code, name: r.name, zone: r.zone, warehouse: r.warehouseName }))
  labelOpen.value = true
}
</script>

<style scoped>
.full {
  width: 100%;
}
</style>
