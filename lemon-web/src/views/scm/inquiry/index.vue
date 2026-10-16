<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="关键字" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="询价单号或说明" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.scm_inquiry_status)" />
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
        <a-button v-perm="'scm:inquiry:add'" type="primary" @click="form.openCreate()">
          <template #icon><PlusOutlined /></template>新增询价单
        </a-button>
        <span v-perm="'scm:inquiry:remove'">
          <a-popconfirm
            :title="`确定删除选中的 ${selectedKeys.length} 张询价单？`"
            :disabled="!selectedKeys.length"
            @confirm="remove(selectedKeys)"
          >
            <a-button danger :disabled="!selectedKeys.length">
              <template #icon><DeleteOutlined /></template>批量删除
            </a-button>
          </a-popconfirm>
        </span>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="{ ...rowSelection, getCheckboxProps: (r) => ({ disabled: r.status !== 'QUOTING' }) }"
        :scroll="{ x: 1200, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'inquiryNo'">
            <a @click="openDetail(record)">{{ record.inquiryNo }}</a>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="scm_inquiry_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">
                <template #icon><EyeOutlined /></template>查看
              </a-button>
              <a-button v-if="record.status === 'QUOTING' && !record.supplierCount" v-perm="'scm:inquiry:edit'" type="link" size="small" @click="form.openEdit(record)">
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <span v-if="record.status === 'QUOTING'" v-perm="'scm:inquiry:remove'">
                <a-popconfirm title="确定删除该询价单？" @confirm="remove([record.id])">
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

    <InquiryForm ref="form" @success="onSaved" />
    <InquiryDetail ref="detailRef" v-model:open="detailOpen" :inquiry-id="detailId" @edit="onEditFromDetail" @changed="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, EyeOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import InquiryDetail from '@/components/scm/InquiryDetail.vue'
import InquiryForm from '@/components/scm/InquiryForm.vue'
import { deleteInquiry, pageInquiry } from '@/api/scm/sourcing'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

const dict = useDict('scm_inquiry_status')

const columns = [
  { title: '询价单号', key: 'inquiryNo', dataIndex: 'inquiryNo', width: 180 },
  { title: '询价日期', dataIndex: 'inquiryDate', width: 110 },
  { title: '报价截止', dataIndex: 'deadline', width: 110 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100, dict: 'scm_inquiry_status' },
  { title: '物料行数', dataIndex: 'itemCount', width: 90, align: 'right' },
  { title: '报价供应商', dataIndex: 'supplierCount', width: 100, align: 'right' },
  { title: '制单人', dataIndex: 'createByName', width: 100 },
  { title: '说明', dataIndex: 'remark', width: 200, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'scm-inquiry')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(
  pageInquiry,
  query
)
onMounted(load)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const form = ref()
const detailRef = ref()
const detailOpen = ref(false)
const detailId = ref()

function openDetail(record) {
  detailId.value = record.id
  detailOpen.value = true
}

function onEditFromDetail(record) {
  form.value.openEdit(record)
}

/** 保存后打开详情，方便接着录入报价 */
function onSaved(id) {
  load()
  // 从详情进入的编辑：返回详情并刷新
  if (detailOpen.value && (!id || id === detailId.value)) detailRef.value?.reload()
  else if (id) openDetail({ id })
}

async function remove(ids) {
  await deleteInquiry(ids)
  message.success('删除成功')
  selectedKeys.value = selectedKeys.value.filter((k) => !ids.includes(k))
  load()
}
</script>
