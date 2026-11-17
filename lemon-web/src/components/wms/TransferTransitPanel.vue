<template>
  <!-- 在途调拨详情下方：验收记录（调拨验收单）、差异及处理、补发单 -->
  <div class="transit-panel">
    <a-spin :spinning="loading">
      <div class="section-title">{{ $t('wms.transit.receipts', { n: data.receipts.length }) }}</div>
      <a-table
        row-key="id"
        size="small"
        :columns="receiptColumns"
        :data-source="data.receipts"
        :pagination="false"
        :scroll="{ x: 1000 }"
        :locale="{ emptyText: $t('wms.transit.noReceipts') }"
      >
        <template #expandedRowRender="{ record }">
          <a-table row-key="id" size="small" :columns="lineColumns" :data-source="record.items" :pagination="false">
            <template #bodyCell="{ column, record: line }">
              <template v-if="column.key === 'material'">
                {{ line.materialCode }} {{ line.materialName }}
                <span class="sub-text">{{ line.spec }}</span>
              </template>
              <template v-else-if="column.key === 'qty'">{{ formatQty(line.qty) }} {{ line.unit }}</template>
              <template v-else-if="column.key === 'toLocation'">{{ locationText(line.toLocationCode) }}</template>
              <template v-else-if="column.key === 'serialNos'"><SerialTags :value="line.serialNos" /></template>
            </template>
          </a-table>
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'qty'">{{ formatQty(record.totalQty) }}</template>
          <template v-else-if="column.key === 'diffQty'">
            <span :class="{ 'warn-text': Number(record.diffQty) }">{{ Number(record.diffQty) ? formatQty(record.diffQty) : '-' }}</span>
          </template>
          <template v-else-if="column.key === 'logistics'">{{ [record.logisticsNo, record.plateNo].filter(Boolean).join(' · ') || '-' }}</template>
          <template v-else-if="column.key === 'by'">
            <div>{{ record.createByName || '-' }}</div>
            <div class="sub-text">{{ record.createTime }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="[{ type: 'print', show: record.items.length > 0, onClick: () => printReceipt(record) }]" />
          </template>
        </template>
      </a-table>

      <div class="section-title">
        {{ $t('wms.transit.diffs', { n: data.diffs.length }) }}
        <a-tag v-if="pendingCount" color="red" :bordered="false">{{ $t('wms.transit.pendingN', { n: pendingCount }) }}</a-tag>
      </div>
      <a-table
        row-key="id"
        size="small"
        :columns="diffColumns"
        :data-source="data.diffs"
        :pagination="false"
        :scroll="{ x: 1200 }"
        :locale="{ emptyText: $t('wms.transit.noDiffs') }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ [record.spec, record.batchNo && $t('wms.transit.batchN', { no: record.batchNo })].filter(Boolean).join(' · ') }}</div>
          </template>
          <template v-else-if="column.key === 'diffType'">
            <DictTag type="wms_transfer_diff_type" :value="record.diffType" />
            <div v-if="record.receiptNo" class="sub-text">{{ record.receiptNo }}</div>
          </template>
          <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
          <template v-else-if="column.key === 'serialNos'"><SerialTags :value="record.serialNos" /></template>
          <template v-else-if="column.key === 'resolution'">
            <a-tag v-if="!record.resolution" color="orange" :bordered="false">{{ $t('wms.transit.pending') }}</a-tag>
            <template v-else>
              <DictTag type="wms_transfer_resolution" :value="record.resolution" />
              <div class="sub-text">{{ [record.resolveDate, record.resolveByName].filter(Boolean).join(' · ') }}</div>
              <div v-if="record.refDocNo" class="sub-text">
                <a @click="openRef(record)">{{ record.refDocNo }}</a>
              </div>
              <div v-if="record.resolveRemark" class="sub-text">{{ record.resolveRemark }}</div>
            </template>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="diffActions(record)" />
          </template>
        </template>
      </a-table>

      <template v-if="data.supplements.length">
        <div class="section-title">{{ $t('wms.transit.supplements') }}</div>
        <a-space wrap>
          <a-tag v-for="s in data.supplements" :key="s.id" class="supplement" @click="openTransfer(s.id)">
            {{ s.docNo }} · {{ s.bizDate }} · {{ formatQty(s.totalQty) }}
            <DictTag type="wms_doc_status" :value="s.status" />
          </a-tag>
        </a-space>
      </template>
    </a-spin>
    <TransferResolveModal ref="resolveRef" @done="onResolved" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ToolOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import SerialTags from './SerialTags.vue'
import TransferResolveModal from './TransferResolveModal.vue'
import { getTransferTransit } from '@/api/wms/doc'
import { materialColumns, printDocs } from '@/utils/print'
import { printName } from '@/utils/printTemplates'
import { formatQty, locationText } from './wms'

import { WmsDocStatus } from '@/constants/enums'
/**
 * 在途调拨的验收记录与差异处理。doc 为调拨单详情；处理差异后触发 changed（详情重新加载），点击补发单触发 open（调拨单 ID）
 */
const props = defineProps({ doc: { type: Object, required: true } })
const emit = defineEmits(['changed', 'open'])
const { t } = useI18n()
const router = useRouter()
const receiving = computed(() => [WmsDocStatus.TRANSIT, WmsDocStatus.PARTIAL, WmsDocStatus.EXCEPTION].includes(props.doc.status))

const loading = ref(false)
const data = ref({ receipts: [], diffs: [], supplements: [], availableSerials: {} })
const pendingCount = computed(() => data.value.diffs.filter((d) => !d.resolution).length)
const resolveRef = ref()

const receiptColumns = computed(() => [
  { title: t('wms.transit.receiptNo'), dataIndex: 'receiptNo', width: 160 },
  { title: t('wms.transit.receiveDate'), dataIndex: 'bizDate', width: 110 },
  { title: t('wms.transit.receivedQty'), key: 'qty', width: 100, align: 'right' },
  { title: t('wms.transit.diffQty'), key: 'diffQty', width: 100, align: 'right' },
  { title: t('wms.transit.logistics'), key: 'logistics', width: 200 },
  { title: t('wms.transit.receiver'), key: 'by', width: 170 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  actionColumn(1, [t('common.action.print')])
])
const lineColumns = computed(() => [
  { title: t('wms.common.material'), key: 'material', width: 260 },
  { title: t('wms.common.batch'), dataIndex: 'batchNo', width: 140 },
  { title: t('wms.doc.toLocation'), key: 'toLocation', width: 120 },
  { title: t('wms.transit.receivedQty'), key: 'qty', width: 110, align: 'right' },
  { title: t('wms.common.serialNo'), key: 'serialNos', width: 260 }
])
const diffColumns = computed(() => [
  { title: t('wms.common.material'), key: 'material', width: 240 },
  { title: t('wms.transit.diffType'), key: 'diffType', width: 150 },
  { title: t('wms.transit.diffQty'), key: 'qty', width: 100, align: 'right' },
  { title: t('wms.common.serialNo'), key: 'serialNos', width: 200 },
  { title: t('wms.transit.diffRemark'), dataIndex: 'remark', width: 160, ellipsis: true },
  { title: t('wms.transit.resolution'), key: 'resolution', width: 200 },
  actionColumn(1, [t('wms.transit.resolve')])
])

/** 待处理的差异：处理（需要差异处理权限，单据未完结） */
const diffActions = (record) => [
  {
    key: 'resolve',
    label: t('wms.transit.resolve'),
    icon: ToolOutlined,
    show: !record.resolution && receiving.value,
    perm: 'wms:transfer:resolve',
    onClick: () => resolveRef.value.openDiff(record)
  }
]

async function load() {
  loading.value = true
  try {
    data.value = await getTransferTransit(props.doc.id)
  } finally {
    loading.value = false
  }
}

watch(
  () => props.doc && `${props.doc.id}-${props.doc.status}-${props.doc.updateTime}`,
  (v) => v && load(),
  { immediate: true }
)

function onResolved() {
  emit('changed')
}

/** 处理生成的单据：报废出库单、多发补调的调拨单 */
function openRef(record) {
  if (record.refDocKind === 'OUT') {
    router.push({ path: '/wms/out', query: { id: record.refDocId } })
  } else {
    openTransfer(record.refDocId)
  }
}

const openTransfer = (id) => emit('open', id)

/** 打印调拨验收单（明细为本次验收入库的数量；第四项为打印模板的匹配键） */
function printReceipt(r) {
  const d = props.doc
  const n = (id) => printName(id)
  printDocs(
    {
      title: n('transferReceipt'),
      no: r.receiptNo,
      fields: [
        [t('wms.transit.transferNo'), d.docNo, false, n('transferDocNo')],
        [t('wms.doc.kind.fromWarehouse'), d.warehouseName, false, n('outWarehouse')],
        [t('wms.doc.toWarehouse'), d.toWarehouseName, false, n('inWarehouse')],
        [t('wms.transit.receiveDate'), r.bizDate, false, n('receiveDate')],
        [t('wms.transit.logisticsNo'), r.logisticsNo, false, n('logisticsNo')],
        [t('wms.transit.plateNo'), r.plateNo, false, n('plateNo')],
        [t('wms.transit.receivedQty'), formatQty(r.totalQty), false, n('receivedQty')],
        [t('wms.transit.diffQty'), Number(r.diffQty) ? formatQty(r.diffQty) : '', false, n('diffQty')]
      ],
      columns: [
        ...materialColumns(),
        { id: n('batch'), title: t('wms.common.batch'), key: 'batchNo' },
        { id: n('inLocation'), title: t('wms.doc.toLocation'), value: (row) => locationText(row.toLocationCode) },
        { id: n('receivedQty'), title: t('wms.transit.receivedQty'), key: 'qty', value: (row) => formatQty(row.qty), align: 'right' },
        { id: n('serialNo'), title: t('wms.common.serialNo'), key: 'serialNos' }
      ],
      rows: r.items,
      totals: { qty: formatQty(r.totalQty) },
      note: r.remark,
      signs: [
        [t('wms.transit.receiver'), r.createByName, n('receiver')],
        [t('wms.transit.deliverer'), '', n('deliverer')],
        [t('wms.transit.reviewer'), '', n('reviewer')]
      ]
    },
    `${n('transferReceipt')} ${r.receiptNo}`,
    'wms.transferReceipt'
  )
}

defineExpose({ reload: load })
</script>

<style scoped>
.transit-panel {
  margin-top: 16px;
}
.section-title {
  font-weight: 500;
  margin: 16px 0 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.warn-text {
  color: var(--status-warning);
}
.supplement {
  cursor: pointer;
}
</style>
