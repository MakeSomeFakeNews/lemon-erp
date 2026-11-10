<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('scm.inquiry.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.scm_inquiry_status)" />
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
          <template #icon><PlusOutlined /></template>{{ $t('scm.inquiry.add') }}
        </a-button>
        <span v-perm="'scm:inquiry:remove'">
          <a-popconfirm
            :title="$t('scm.inquiry.confirmBatchDelete', { n: selectedKeys.length })"
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
        :row-selection="{ ...rowSelection, getCheckboxProps: (r) => ({ disabled: r.status !== InquiryStatus.QUOTING }) }"
        :scroll="{ x: scrollX + 32, y: '100%' }"
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
            <TableActions
              :actions="[
                { type: 'view', onClick: () => openDetail(record) },
                { type: 'edit', show: record.status === InquiryStatus.QUOTING && !record.supplierCount, perm: 'scm:inquiry:edit', onClick: () => form.openEdit(record) },
                { type: 'delete', show: record.status === InquiryStatus.QUOTING, perm: 'scm:inquiry:remove', confirm: $t('scm.inquiry.confirmDelete'), onClick: () => remove([record.id]) }
              ]"
            />
          </template>
        </template>
      </a-table>
    </a-card>

    <InquiryForm ref="form" @success="onSaved" />
    <InquiryDetail ref="detailRef" v-model:open="detailOpen" :inquiry-id="detailId" @edit="onEditFromDetail" @changed="load" />
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
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

import { InquiryStatus } from '@/constants/enums'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
const { t } = useI18n()

const dict = useDict('scm_inquiry_status')

const columns = [
  { title: t('scm.inquiry.inquiryNo'), key: 'inquiryNo', dataIndex: 'inquiryNo', width: 180 },
  { title: t('scm.inquiry.inquiryDate'), dataIndex: 'inquiryDate', width: 110 },
  { title: t('scm.inquiry.deadline'), dataIndex: 'deadline', width: 130 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 100, dict: 'scm_inquiry_status' },
  { title: t('scm.inquiry.itemCount'), dataIndex: 'itemCount', width: 120, align: 'right' },
  { title: t('scm.inquiry.quoteSuppliers'), dataIndex: 'supplierCount', width: 140, align: 'right' },
  { title: t('scm.inquiry.preparedBy'), dataIndex: 'createByName', width: 110 },
  { title: t('scm.inquiry.remark'), dataIndex: 'remark', width: 200, ellipsis: true },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 },
  actionColumn(3, [t('common.action.view')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'scm-inquiry')
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
  message.success(t('common.msg.deleteSuccess'))
  selectedKeys.value = selectedKeys.value.filter((k) => !ids.includes(k))
  load()
}
</script>
