<template>
  <SubPage
    :open="open"
    :title="detail ? `${conf.label} ${detail.docNo}` : conf.label"
    width="min(1100px, 100vw)"
    destroy-on-close
    @close="emit('update:open', false)"
  >
    <template #extra>
      <a-space>
        <a-button v-if="detail" size="small" @click="print"><template #icon><PrinterOutlined /></template>打印</a-button>
        <a-tag v-if="detail?.redOf" color="red" :bordered="false">红字</a-tag>
        <a-tag v-if="detail?.reversed === 1" :bordered="false">已冲销</a-tag>
        <DictTag v-if="detail" type="wms_doc_status" :value="detail.status" />
      </a-space>
    </template>

    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="doc-desc">
          <a-descriptions-item label="单据号">{{ detail.docNo }}</a-descriptions-item>
          <a-descriptions-item v-if="conf.typeDict" :label="conf.typeLabel">
            <DictTag :type="conf.typeDict" :value="detail.bizType" />
          </a-descriptions-item>
          <a-descriptions-item :label="conf.warehouseLabel">{{ detail.warehouseName }}</a-descriptions-item>
          <a-descriptions-item v-if="kind === 'transfer'" label="调入仓库">{{ detail.toWarehouseName }}</a-descriptions-item>
          <a-descriptions-item v-if="kind === 'transfer'" label="调拨方式">
            {{ inTransit ? '在途调拨（调入仓库收货后入库）' : '直接调拨' }}
          </a-descriptions-item>
          <a-descriptions-item label="业务日期">{{ detail.bizDate }}</a-descriptions-item>
          <a-descriptions-item label="合计数量">
            <span :class="negativeClass(detail.totalQty)">{{ formatQty(detail.totalQty) }}</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.sourceNo" label="来源单号">{{ detail.sourceNo }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.redOf" label="冲销原单">{{ detail.redOfNo || detail.redOf }}</a-descriptions-item>
          <a-descriptions-item label="制单">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.confirmTime" label="确认">
            {{ detail.confirmByName || '-' }} {{ detail.confirmTime }}
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.receiveTime" label="收货">
            {{ detail.receiveByName || '-' }} {{ detail.receiveTime }}
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="备注">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <a-alert
          v-if="detail.status === 'APPROVING'"
          type="info"
          show-icon
          class="doc-desc"
          message="单据审批中，可在「流程中心 → 我的申请」查看审批进度，审批通过后更新库存"
        />

        <a-table
          row-key="id"
          size="small"
          :columns="columns"
          :data-source="detail.items"
          :pagination="false"
          :scroll="{ x: 900 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">
              <div>{{ record.materialCode }} {{ record.materialName }}</div>
              <div class="sub-text">{{ record.spec }}</div>
            </template>
            <template v-else-if="column.key === 'location'">{{ locationText(record.locationCode) }}</template>
            <template v-else-if="column.key === 'toLocation'">{{ locationText(record.toLocationCode) }}</template>
            <template v-else-if="column.key === 'batchNo'">
              <span>{{ record.batchNo || '-' }}</span>
              <div v-if="record.expiryDate" class="sub-text">
                {{ record.productionDate || '?' }} ~ {{ record.expiryDate }}
              </div>
            </template>
            <template v-else-if="column.key === 'qty'">
              <span :class="negativeClass(record.qty)">{{ formatQty(record.qty) }}</span> {{ record.unit }}
            </template>
            <template v-else-if="column.key === 'serialNos'">
              <template v-if="record.serialNos">
                <a-tag v-for="sn in splitSerials(record.serialNos)" :key="sn" class="serial-tag">{{ sn }}</a-tag>
              </template>
              <span v-else>-</span>
            </template>
          </template>
        </a-table>
      </template>
    </a-spin>

    <template v-if="showFooter" #footer>
      <div class="drawer-footer">
        <template v-if="detail.status === 'DRAFT'">
          <a-button v-if="canEdit" @click="emit('edit', detail)">
            <template #icon><EditOutlined /></template>编辑
          </a-button>
          <a-button v-if="canConfirm" type="primary" :loading="confirming" @click="confirm">
            <template #icon><CheckOutlined /></template>确认{{ kind === 'in' ? '入库' : kind === 'out' ? '出库' : '调拨' }}
          </a-button>
        </template>
        <a-popconfirm
          v-if="showReceive"
          :title="`确认调入仓库「${detail.toWarehouseName}」已收货？`"
          description="收货后按明细增加调入仓库的库存"
          @confirm="receive"
        >
          <a-button type="primary" :loading="acting">
            <template #icon><InboxOutlined /></template>收货
          </a-button>
        </a-popconfirm>
        <a-popconfirm
          v-if="showReverse"
          :title="`确定冲销${conf.label} ${detail.docNo}？`"
          description="生成红字单据，库存反向变动"
          ok-type="danger"
          @confirm="reverse"
        >
          <a-button danger :loading="acting">
            <template #icon><RollbackOutlined /></template>冲销
          </a-button>
        </a-popconfirm>
      </div>
    </template>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, ref, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { CheckOutlined, EditOutlined, InboxOutlined, PrinterOutlined, RollbackOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import { confirmDoc, getDoc, receiveTransfer, reverseDoc } from '@/api/wms/doc'
import { useUserStore } from '@/store/user'
import { DOC_KINDS, canReverse, formatQty, locationText, negativeClass, splitSerials } from './wms'
import { materialColumns, printDocs } from '@/utils/print'

/**
 * 出入库单详情；草稿可以编辑（触发 edit）、确认（更新库存后触发 changed）
 */
const props = defineProps({
  open: Boolean,
  kind: { type: String, required: true },
  docId: Number
})
const emit = defineEmits(['update:open', 'edit', 'changed'])

const userStore = useUserStore()
const conf = computed(() => DOC_KINDS[props.kind])
const canEdit = computed(() => userStore.hasPerm(`wms:${props.kind}:edit`))
const canConfirm = computed(() => userStore.hasPerm(`wms:${props.kind}:confirm`))
const inTransit = computed(() => detail.value?.inTransit === true || detail.value?.inTransit === 1)
const showReceive = computed(
  () => props.kind === 'transfer' && detail.value?.status === 'TRANSIT' && userStore.hasPerm('wms:transfer:receive')
)
const showReverse = computed(() => !!detail.value && canReverse(detail.value) && userStore.hasPerm(`wms:${props.kind}:reverse`))
const showFooter = computed(
  () => !!detail.value && ((detail.value.status === 'DRAFT' && (canEdit.value || canConfirm.value)) || showReceive.value || showReverse.value)
)

const columns = computed(() =>
  [
    { title: '物料', key: 'material', width: 240 },
    { title: props.kind === 'transfer' ? '调出库位' : '库位', key: 'location', width: 110 },
    props.kind === 'transfer' ? { title: '调入库位', key: 'toLocation', width: 110 } : null,
    { title: '批次', key: 'batchNo', width: 180 },
    { title: '数量', key: 'qty', width: 120, align: 'right' },
    { title: '序列号', key: 'serialNos', width: 260 },
    { title: '备注', dataIndex: 'remark', width: 150, ellipsis: true }
  ].filter(Boolean)
)

const detail = ref()

function print() {
  const d = detail.value
  const c = conf.value
  const transfer = props.kind === 'transfer'
  printDocs({
    title: c.label,
    no: d.docNo,
    fields: [
      [c.warehouseLabel, d.warehouseName],
      ...(transfer ? [['调入仓库', d.toWarehouseName]] : []),
      ['业务日期', d.bizDate],
      ['来源单号', d.sourceNo],
      ['合计数量', formatQty(d.totalQty)]
    ],
    columns: [
      ...materialColumns(),
      { title: transfer ? '调出库位' : '库位', value: (r) => locationText(r.locationCode) },
      ...(transfer ? [{ title: '调入库位', value: (r) => locationText(r.toLocationCode) }] : []),
      { title: '批次', key: 'batchNo' },
      { title: '数量', key: 'qty', value: (r) => formatQty(r.qty), align: 'right' },
      { title: '序列号', key: 'serialNos' }
    ],
    rows: d.items,
    totals: { qty: formatQty(d.totalQty) },
    note: d.remark,
    signs: [['制单', d.createByName], ['确认', d.confirmByName], '经手人']
  }, `${c.label} ${d.docNo}`, `wms.${props.kind}`)
}
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    detail.value = await getDoc(props.kind, props.docId)
  } finally {
    loading.value = false
  }
}

watch(
  () => props.open && props.docId,
  (v) => {
    detail.value = undefined
    if (v) load()
  }
)

const confirming = ref(false)
function confirm() {
  const action = { in: '入库', out: '出库', transfer: '调拨' }[props.kind]
  Modal.confirm({
    title: `确认${action}？`,
    content: '确认后按明细更新库存，单据不能再修改；发布了出入库审批流程时先提交审批。',
    async onOk() {
      confirming.value = true
      try {
        const flow = await confirmDoc(props.kind, props.docId)
        message.success(flow ? '已提交出入库审批，审批通过后更新库存' : `${action}成功`)
        await load()
        emit('changed')
      } finally {
        confirming.value = false
      }
    }
  })
}

const acting = ref(false)
async function receive() {
  acting.value = true
  try {
    await receiveTransfer(props.docId)
    message.success('收货成功')
    await load()
    emit('changed')
  } finally {
    acting.value = false
  }
}

async function reverse() {
  acting.value = true
  try {
    await reverseDoc(props.kind, props.docId)
    message.success('冲销成功，已生成红字单据')
    await load()
    emit('changed')
  } finally {
    acting.value = false
  }
}

defineExpose({ reload: load })
</script>

<style scoped>
.qty-negative {
  color: var(--status-critical);
}
.doc-desc {
  margin-bottom: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.serial-tag {
  margin: 2px 4px 2px 0;
}
</style>
