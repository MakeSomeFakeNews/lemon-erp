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
                <template #icon><EyeOutlined /></template>{{ $t('common.action.view') }}
              </a-button>
              <a-button v-if="record.status === InquiryStatus.QUOTING && !record.supplierCount" v-perm="'scm:inquiry:edit'" type="link" size="small" @click="form.openEdit(record)">
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <span v-if="record.status === InquiryStatus.QUOTING" v-perm="'scm:inquiry:remove'">
                <a-popconfirm :title="$t('scm.inquiry.confirmDelete')" @confirm="remove([record.id])">
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

    <InquiryForm ref="form" @success="onSaved" />
    <InquiryDetail ref="detailRef" v-model:open="detailOpen" :inquiry-id="detailId" @edit="onEditFromDetail" @changed="load" />
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
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

import { InquiryStatus } from '@/constants/enums'
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
  { title: t('common.action.operation'), key: 'action', width: 200, fixed: 'right' }
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
  message.success(t('common.msg.deleteSuccess'))
  selectedKeys.value = selectedKeys.value.filter((k) => !ids.includes(k))
  load()
}
</script>
