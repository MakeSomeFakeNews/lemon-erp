<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('wms.common.docNo')" name="docNo">
          <a-input v-model:value="query.docNo" :placeholder="$t('common.placeholder.input')" allow-clear />
        </a-form-item>
        <a-form-item v-if="conf.typeDict" :label="conf.typeLabel" name="bizType">
          <a-select v-model:value="query.bizType" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict[conf.typeDict])" />
        </a-form-item>
        <a-form-item v-if="conf.categoryDict" :label="conf.categoryLabel" name="ioCategory">
          <a-select v-model:value="query.ioCategory" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict[conf.categoryDict])" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.wms_doc_status)" />
        </a-form-item>
        <a-form-item :label="$t('wms.common.warehouse')" name="warehouseId">
          <WarehouseSelect v-model:value="query.warehouseId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('wms.common.bizDate')" name="range">
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
        <a-button v-perm="`wms:${kind}:add`" type="primary" @click="form.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('wms.doc.createTitle', { kind: conf.label }) }}
        </a-button>
        <span v-perm="`wms:${kind}:remove`">
          <a-popconfirm
            :title="$t('wms.doc.confirmDeleteDrafts', { n: selectedKeys.length })"
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
        :row-selection="{ ...rowSelection, getCheckboxProps: (r) => ({ disabled: r.status !== WmsDocStatus.DRAFT }) }"
        :scroll="{ x: scrollX + 32, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'docNo'">
            <a @click="openDetail(record)">{{ record.docNo }}</a>
            <a-tag v-if="record.redOf" color="red" :bordered="false" class="doc-tag">{{ $t('wms.common.red') }}</a-tag>
            <a-tag v-if="record.reversed === 1" :bordered="false" class="doc-tag">{{ $t('wms.common.reversed') }}</a-tag>
            <div v-if="record.sourceNo" class="sub-text">{{ $t('wms.doc.sourceN', { no: record.sourceNo }) }}</div>
            <div v-if="record.redOf" class="sub-text">{{ $t('wms.doc.reverseOfN', { no: record.redOfNo || record.redOf }) }}</div>
          </template>
          <template v-else-if="column.key === 'bizType'">
            <DictTag :type="conf.typeDict" :value="record.bizType" />
          </template>
          <template v-else-if="column.key === 'ioCategory'">
            <DictTag v-if="record.ioCategory" :type="conf.categoryDict" :value="record.ioCategory" />
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'warehouse'">
            <span v-if="kind === 'transfer'">{{ record.warehouseName }} <ArrowRightOutlined class="arrow" /> {{ record.toWarehouseName }}</span>
            <WarehouseNames v-else :name="record.warehouseName" :names="record.warehouseNames" />
            <div v-if="kind === 'transfer' && isTrue(record.inTransit)" class="sub-text">{{ $t('wms.doc.inTransitTransfer') }}</div>
          </template>
          <template v-else-if="column.key === 'totalQty'">
            <span :class="negativeClass(record.totalQty)">{{ formatQty(record.totalQty) }}</span>
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
            <TableActions :actions="rowActions(record)">
              <template #receive="{ action, text }">
                <ReceiveConfirm :warehouse-name="record.toWarehouseName" @confirm="(date) => receive(record, date)">
                  <TableActionButton :action="action" :text="text" />
                </ReceiveConfirm>
              </template>
            </TableActions>
          </template>
        </template>
      </a-table>
    </a-card>

    <StockDocForm ref="form" :kind="kind" @success="onSaved" />
    <StockDocDetail ref="detailRef" v-model:open="detailOpen" :kind="kind" :doc-id="detailId" @edit="onEditFromDetail" @changed="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import {
  ArrowRightOutlined,
  CheckOutlined,
  DeleteOutlined,
  InboxOutlined,
  PlusOutlined
} from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import ReceiveConfirm from './ReceiveConfirm.vue'
import StockDocDetail from './StockDocDetail.vue'
import StockDocForm from './StockDocForm.vue'
import WarehouseSelect from './WarehouseSelect.vue'
import WarehouseNames from './WarehouseNames.vue'
import { confirmDoc, deleteDoc, pageDoc, receiveTransfer, reverseDoc } from '@/api/wms/doc'
import { useColumns } from '@/composables/useColumns'
import TableActions from '@/components/common/TableActions.vue'
import TableActionButton from '@/components/common/TableActionButton.vue'
import { actionColumn } from '@/components/common/tableActions'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { DOC_KINDS, canReverse, formatQty, negativeClass, warehouseText } from './wms'
import { useAction } from '@/composables/useAction'

import { WmsDocStatus } from '@/constants/enums'
/**
 * 入库单 / 出库单 / 调拨单列表页，三个页面共用
 */
const props = defineProps({
  kind: { type: String, required: true }
})

const { t } = useI18n()
const conf = DOC_KINDS[props.kind]
const dict = useDict('wms_doc_status', ...(conf.typeDict ? [conf.typeDict, conf.categoryDict] : []))

// 导出（exportColumns / exportValue）：单号下显示的来源单号、调拨的调出 / 调入仓库拆成单独的列，数量为原始数值，见 useTable
const columns = [
  {
    title: t('wms.common.docNo'),
    key: 'docNo',
    width: 170,
    exportColumns: [
      {
        title: t('wms.common.docNo'),
        exportValue: (r) => [r.docNo, r.redOf && t('wms.common.red'), r.reversed === 1 && t('wms.common.reversed')].filter(Boolean).join(' ')
      },
      { title: t('wms.common.sourceNo'), exportValue: (r) => r.sourceNo || (r.redOf ? t('wms.doc.reverseOfN', { no: r.redOfNo || r.redOf }) : '') }
    ]
  },
  conf.typeDict ? { title: conf.typeLabel, key: 'bizType', width: 120, dict: conf.typeDict } : null,
  conf.categoryDict ? { title: conf.categoryLabel, key: 'ioCategory', width: 110, dict: conf.categoryDict } : null,
  props.kind === 'transfer'
    ? {
        title: t('wms.doc.fromTo'),
        key: 'warehouse',
        width: 240,
        exportColumns: [
          { title: t('wms.doc.kind.fromWarehouse'), dataIndex: 'warehouseName' },
          { title: t('wms.doc.toWarehouse'), dataIndex: 'toWarehouseName' },
          { title: t('wms.doc.transferMode'), exportValue: (r) => (isTrue(r.inTransit) ? t('wms.doc.inTransitTransfer') : t('wms.doc.directTransfer')) }
        ]
      }
    : { title: t('wms.common.warehouse'), key: 'warehouse', width: 140, exportValue: warehouseText },
  { title: t('wms.common.bizDate'), dataIndex: 'bizDate', width: 120 },
  { title: t('wms.common.totalQty'), key: 'totalQty', width: 110, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 100, dict: 'wms_doc_status' },
  { title: t('wms.common.createBy'), dataIndex: 'createByName', width: 110 },
  {
    title: t('wms.doc.confirmed'),
    key: 'confirm',
    width: 160,
    exportValue: (r) => (r.confirmTime ? [r.confirmByName, r.confirmTime].filter(Boolean).join(' ') : '')
  },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 },
  // 草稿：查看、编辑、确认、删除；在途调拨：查看、收货；已确认：查看、冲销
  actionColumn(4, [t('common.action.view')])
].filter(Boolean)
const { checked, visibleColumns, scrollX } = useColumns(columns, `wms-${props.kind}`)
const showSearch = ref(true)

const emptyQuery = () => ({ docNo: undefined, bizType: undefined, ioCategory: undefined, status: undefined, warehouseId: undefined, range: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(
  (params) => pageDoc(props.kind, params),
  query,
  { params: ({ range, ...rest }) => ({ ...rest, beginDate: range?.[0], endDate: range?.[1] }) }
)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

onMounted(load)

const form = ref()

// 详情
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

// 行操作
const rowActions = (record) => {
  const draft = record.status === WmsDocStatus.DRAFT
  return [
    { type: 'view', onClick: () => openDetail(record) },
    { type: 'edit', show: draft, perm: `wms:${props.kind}:edit`, onClick: () => form.value.openEdit(record) },
    {
      key: 'confirm',
      label: t('wms.doc.confirm'),
      icon: CheckOutlined,
      show: draft,
      perm: `wms:${props.kind}:confirm`,
      confirm: { title: t('wms.doc.confirmDoc', { kind: conf.label, no: record.docNo }), description: t('wms.doc.confirmContent') },
      onClick: () => confirm(record)
    },
    {
      type: 'delete',
      show: draft,
      perm: `wms:${props.kind}:remove`,
      confirm: t('wms.doc.confirmDeleteDoc', { kind: conf.label, no: record.docNo }),
      onClick: () => remove([record.id])
    },
    {
      key: 'receive',
      slot: 'receive',
      label: t('wms.common.receive'),
      icon: InboxOutlined,
      show: props.kind === 'transfer' && record.status === WmsDocStatus.TRANSIT,
      perm: 'wms:transfer:receive'
    },
    {
      type: 'reverse',
      label: t('wms.common.reverse'),
      show: canReverse(record),
      perm: `wms:${props.kind}:reverse`,
      confirm: { title: t('wms.doc.confirmReverse', { kind: conf.label, no: record.docNo }), description: t('wms.doc.reverseTip'), okType: 'danger' },
      onClick: () => reverse(record)
    }
  ]
}

async function confirm(record) {
  const flow = await confirmDoc(props.kind, record.id)
  message.success(flow ? t('wms.doc.submittedApproval') : t('wms.doc.confirmOk'))
  load()
}

const isTrue = (v) => v === true || v === 1

const receive = useAction((record, date) => receiveTransfer(record.id, date), { successKey: 'wms.doc.receiveSuccess', reload: () => load() })

const reverse = useAction((record) => reverseDoc(props.kind, record.id), { successKey: 'wms.doc.reverseSuccess', reload: () => load() })

async function remove(ids) {
  await deleteDoc(props.kind, ids)
  message.success(t('common.msg.deleteSuccess'))
  selectedKeys.value = selectedKeys.value.filter((k) => !ids.includes(k))
  load()
}
</script>

<style scoped>
.doc-tag {
  margin-left: 6px;
}
.qty-negative {
  color: var(--status-critical);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.arrow {
  color: var(--app-text-3);
  font-size: 12px;
  margin: 0 2px;
}
</style>
