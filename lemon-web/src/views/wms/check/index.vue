<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('wms.common.docNo')" name="docNo">
          <a-input v-model:value="query.docNo" :placeholder="$t('common.placeholder.input')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('wms.common.warehouse')" name="warehouseId">
          <WarehouseSelect v-model:value="query.warehouseId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.wms_doc_status)" />
        </a-form-item>
        <a-form-item :label="$t('wms.checkDoc.date')" name="range">
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
          <template #icon><PlusOutlined /></template>{{ $t('wms.checkDoc.createTitle') }}
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'docNo'">
            <a @click="openDetail(record)">{{ record.docNo }}</a>
            <a-tag v-if="record.freeze === true || record.freeze === 1" color="orange" :bordered="false" class="diff-tag">{{ $t('wms.checkDoc.frozen') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'items'">
            {{ $t('wms.check.lines', { n: record.itemCount }) }}
            <a-tag v-if="record.diffCount" color="orange" :bordered="false" class="diff-tag">{{ $t('wms.check.diffN', { n: record.diffCount }) }}</a-tag>
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
            <TableActions
              :actions="[
                { type: 'view', onClick: () => openDetail(record) },
                { type: 'edit', label: $t('wms.check.count'), show: record.status === WmsDocStatus.DRAFT, perm: 'wms:check:edit', onClick: () => form.openEdit(record) },
                {
                  type: 'delete',
                  show: record.status === WmsDocStatus.DRAFT,
                  perm: 'wms:check:remove',
                  confirm: $t('wms.check.confirmDelete', { no: record.docNo }),
                  onClick: () => remove(record.id)
                }
              ]"
            />
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
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import CheckDetail from '@/components/wms/CheckDetail.vue'
import CheckForm from '@/components/wms/CheckForm.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { deleteCheck, pageCheck } from '@/api/wms/check'
import { useColumns } from '@/composables/useColumns'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { useAction } from '@/composables/useAction'

import { WmsDocStatus } from '@/constants/enums'
const { t } = useI18n()

const dict = useDict('wms_doc_status')

const columns = [
  { title: t('wms.common.docNo'), key: 'docNo', width: 170 },
  { title: t('wms.checkDoc.warehouse'), dataIndex: 'warehouseName', width: 150 },
  { title: t('wms.checkDoc.date'), dataIndex: 'bizDate', width: 110 },
  { title: t('wms.checkDoc.items'), key: 'items', width: 150 },
  { title: t('common.field.status'), key: 'status', width: 90, dict: 'wms_doc_status' },
  { title: t('wms.common.createBy'), dataIndex: 'createByName', width: 100 },
  { title: t('wms.doc.confirmed'), key: 'confirm', width: 160 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  actionColumn(3, [t('common.action.view')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'wms-check')
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

const remove = useAction(deleteCheck, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
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
