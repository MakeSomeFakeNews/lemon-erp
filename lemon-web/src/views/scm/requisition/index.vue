<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('scm.requisition.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.scm_requisition_status)" />
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
        <a-button v-perm="'scm:requisition:add'" type="primary" @click="form.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('scm.requisition.add') }}
        </a-button>
        <span v-perm="'scm:requisition:remove'">
          <a-popconfirm
            :title="$t('scm.requisition.confirmBatchDelete', { n: selectedKeys.length })"
            :disabled="!selectedKeys.length"
            @confirm="remove(selectedKeys)"
          >
            <a-button danger :disabled="!selectedKeys.length">
              <template #icon><DeleteOutlined /></template>{{ $t('common.action.batchDelete') }}
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
        :row-selection="{ ...rowSelection, getCheckboxProps: (r) => ({ disabled: r.status !== RequisitionStatus.DRAFT }) }"
        :scroll="{ x: scrollX + 32, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'reqNo'">
            <a @click="openDetail(record)">{{ record.reqNo }}</a>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="scm_requisition_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[
                { type: 'view', onClick: () => openDetail(record) },
                { type: 'edit', show: record.status === RequisitionStatus.DRAFT, perm: 'scm:requisition:edit', onClick: () => form.openEdit(record) },
                { type: 'delete', show: record.status === RequisitionStatus.DRAFT, perm: 'scm:requisition:remove', confirm: $t('scm.requisition.confirmDelete'), onClick: () => remove([record.id]) }
              ]"
            />
          </template>
        </template>
      </a-table>
    </a-card>

    <RequisitionForm ref="form" @success="onSaved" />
    <RequisitionDetail ref="detailRef" v-model:open="detailOpen" :req-id="detailId" @edit="onEditFromDetail" @changed="load" />
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import RequisitionDetail from '@/components/scm/RequisitionDetail.vue'
import RequisitionForm from '@/components/scm/RequisitionForm.vue'
import { deleteRequisition, pageRequisition } from '@/api/scm/sourcing'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

import { RequisitionStatus } from '@/constants/enums'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
const { t } = useI18n()

const dict = useDict('scm_requisition_status')

const columns = [
  { title: t('scm.requisition.reqNo'), key: 'reqNo', dataIndex: 'reqNo', width: 180 },
  { title: t('scm.requisition.reqDate'), dataIndex: 'reqDate', width: 140 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 100, dict: 'scm_requisition_status' },
  { title: t('scm.requisition.itemCount'), dataIndex: 'itemCount', width: 90, align: 'right' },
  { title: t('scm.requisition.dept'), dataIndex: 'deptName', width: 160, ellipsis: true },
  { title: t('scm.requisition.preparedBy'), dataIndex: 'createByName', width: 110 },
  { title: t('scm.requisition.approveTime'), dataIndex: 'approveTime', width: 170 },
  { title: t('scm.requisition.remark'), dataIndex: 'remark', width: 200, ellipsis: true },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 },
  actionColumn(3, [t('common.action.view')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'scm-requisition')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(
  pageRequisition,
  query
)
const route = useRoute()
const router = useRouter()
onMounted(() => {
  load()
  // 从补货建议等跳转过来时（?id=请购单ID）直接打开详情
  const id = Number(route.query.id)
  if (id) {
    openDetail({ id })
    router.replace({ query: {} })
  }
})

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

/** 保存后打开详情，方便接着提交 */
function onSaved(id) {
  load()
  // 从详情进入的编辑：返回详情并刷新
  if (detailOpen.value && (!id || id === detailId.value)) detailRef.value?.reload()
  else if (id) openDetail({ id })
}

async function remove(ids) {
  await deleteRequisition(ids)
  message.success(t('common.msg.deleteSuccess'))
  selectedKeys.value = selectedKeys.value.filter((k) => !ids.includes(k))
  load()
}
</script>
