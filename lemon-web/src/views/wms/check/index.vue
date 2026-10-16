<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="单据号" name="docNo">
          <a-input v-model:value="query.docNo" placeholder="请输入单据号" allow-clear />
        </a-form-item>
        <a-form-item label="仓库" name="warehouseId">
          <WarehouseSelect v-model:value="query.warehouseId" placeholder="全部" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.wms_doc_status)" />
        </a-form-item>
        <a-form-item label="盘点日期" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" style="width: 100%" />
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
        <a-button v-perm="'wms:check:add'" type="primary" @click="form.openCreate()">
          <template #icon><PlusOutlined /></template>新增盘点单
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
          <template v-if="column.key === 'docNo'">
            <a @click="openDetail(record)">{{ record.docNo }}</a>
            <a-tag v-if="record.freeze === true || record.freeze === 1" color="orange" :bordered="false" class="diff-tag">冻结</a-tag>
          </template>
          <template v-else-if="column.key === 'items'">
            {{ record.itemCount }} 行
            <a-tag v-if="record.diffCount" color="orange" :bordered="false" class="diff-tag">差异 {{ record.diffCount }}</a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="wms_doc_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'confirm'">
            <template v-if="record.confirmTime">
              <div>{{ record.confirmByName || '-' }}</div>
              <div class="sub-text">{{ record.confirmTime }}</div>
            </template>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">
                <template #icon><EyeOutlined /></template>查看
              </a-button>
              <template v-if="record.status === 'DRAFT'">
                <a-button v-perm="'wms:check:edit'" type="link" size="small" @click="form.openEdit(record)">
                  <template #icon><EditOutlined /></template>盘点
                </a-button>
                <span v-perm="'wms:check:remove'">
                  <a-popconfirm :title="`确定删除盘点单 ${record.docNo}？`" @confirm="remove(record.id)">
                    <a-button type="link" danger size="small">
                      <template #icon><DeleteOutlined /></template>删除
                    </a-button>
                  </a-popconfirm>
                </span>
              </template>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <CheckForm ref="form" @success="onSaved" />
    <CheckDetail ref="detailRef" v-model:open="detailOpen" :check-id="detailId" @edit="onEditFromDetail" @changed="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, EyeOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import CheckDetail from '@/components/wms/CheckDetail.vue'
import CheckForm from '@/components/wms/CheckForm.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { deleteCheck, pageCheck } from '@/api/wms/check'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

const dict = useDict('wms_doc_status')

const columns = [
  { title: '单据号', key: 'docNo', width: 170 },
  { title: '盘点仓库', dataIndex: 'warehouseName', width: 140 },
  { title: '盘点日期', dataIndex: 'bizDate', width: 110 },
  { title: '明细', key: 'items', width: 150 },
  { title: '状态', key: 'status', width: 90, dict: 'wms_doc_status' },
  { title: '制单人', dataIndex: 'createByName', width: 100 },
  { title: '确认', key: 'confirm', width: 160 },
  { title: '备注', dataIndex: 'remark', width: 160, ellipsis: true },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'wms-check')
const showSearch = ref(true)

const emptyQuery = () => ({ docNo: undefined, warehouseId: undefined, status: undefined, range: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageCheck, query, {
  params: ({ range, ...rest }) => ({ ...rest, beginDate: range?.[0], endDate: range?.[1] })
})

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

onMounted(load)

const form = ref()
const detailRef = ref()
const detailOpen = ref(false)
const detailId = ref()
function openDetail(record) {
  detailId.value = record.id
  detailOpen.value = true
}
function onSaved() {
  load()
  if (detailOpen.value) detailRef.value?.reload()
}
function onEditFromDetail(record) {
  form.value.openEdit(record)
}

async function remove(id) {
  await deleteCheck(id)
  message.success('删除成功')
  load()
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.diff-tag {
  margin-left: 4px;
}
</style>
