<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="仓库" name="warehouseId">
          <WarehouseSelect v-model:value="query.warehouseId" placeholder="全部" @update:value="search" />
        </a-form-item>
        <a-form-item label="库位" name="keyword">
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
        <a-button v-perm="'wms:location:add'" type="primary" @click="drawer.openCreate({ warehouseId: query.warehouseId })">
          <template #icon><PlusOutlined /></template>新增库位
        </a-button>
        <span v-perm="'wms:location:remove'">
          <a-popconfirm
            :title="`确定删除选中的 ${selectedKeys.length} 个库位？`"
            :disabled="!selectedKeys.length"
            @confirm="remove(selectedKeys)"
          >
            <a-button danger :disabled="!selectedKeys.length">
              <template #icon><DeleteOutlined /></template>批量删除
            </a-button>
          </a-popconfirm>
        </span>
        <a-button :disabled="!selectedKeys.length" @click="openLabels">
          <template #icon><BarcodeOutlined /></template>打印库位标签{{ selectedKeys.length ? `（${selectedKeys.length}）` : '' }}
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
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <span v-perm="'wms:location:remove'">
                <a-popconfirm :title="`确定删除库位「${record.code}」？`" @confirm="remove([record.id])">
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

    <FormDrawer :drawer="drawer" create-title="新增库位" edit-title="编辑库位">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="所属仓库" name="warehouseId" :extra="drawer.isEdit.value ? '库位所属仓库不能修改' : ''">
              <WarehouseSelect v-model:value="form.warehouseId" :allow-clear="false" :disabled="drawer.isEdit.value" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="库位编码" name="code">
              <a-input v-model:value="form.code" :maxlength="32" placeholder="如：A-01-02（区-排-层）" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="库位名称" name="name">
              <a-input v-model:value="form.name" :maxlength="64" placeholder="选填" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="类型" name="locType" extra="上架推荐空库位只用存储位、拣货位；不良品位的库存不参与拣货建议">
              <a-select v-model:value="form.locType" :options="toOptions(dict.wms_location_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="区域" name="zone">
              <a-input v-model:value="form.zone" :maxlength="32" placeholder="如 A 区、冷藏区" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="排序" name="sort" extra="拣货路径按 区域 → 排序 → 编码">
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
    <LabelPrintModal v-model:open="labelOpen" type-key="label.location" :items="labelItems" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
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

const route = useRoute()
const userStore = useUserStore()
const dict = useDict('sys_common_status', 'wms_location_type')

const columns = [
  { title: '仓库', dataIndex: 'warehouseName', width: 160 },
  { title: '库位编码', dataIndex: 'code', width: 140 },
  { title: '库位名称', dataIndex: 'name', width: 160 },
  { title: '区域', dataIndex: 'zone', width: 100 },
  { title: '类型', key: 'type', width: 100 },
  { title: '排序', dataIndex: 'sort', width: 70 },
  { title: '状态', key: 'status', width: 90 },
  { title: '备注', dataIndex: 'remark', width: 160, ellipsis: true },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
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
  warehouseId: [{ required: true, message: '请选择所属仓库' }],
  code: [
    { required: true, message: '请输入库位编码' },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: '只能包含字母、数字、下划线和短横线' }
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
  message.success('删除成功')
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
