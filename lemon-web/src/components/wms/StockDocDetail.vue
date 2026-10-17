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
        <a-button v-if="detail" size="small" @click="print"><template #icon><PrinterOutlined /></template>{{ $t('common.action.print') }}</a-button>
        <a-tag v-if="detail?.redOf" color="red" :bordered="false">{{ $t('wms.common.red') }}</a-tag>
        <a-tag v-if="detail?.reversed === 1" :bordered="false">{{ $t('wms.common.reversed') }}</a-tag>
        <DictTag v-if="detail" type="wms_doc_status" :value="detail.status" />
      </a-space>
    </template>

    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="doc-desc">
          <a-descriptions-item :label="$t('wms.common.docNo')">{{ detail.docNo }}</a-descriptions-item>
          <a-descriptions-item v-if="conf.typeDict" :label="conf.typeLabel">
            <DictTag :type="conf.typeDict" :value="detail.bizType" />
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.ioCategory" :label="conf.categoryLabel">
            <DictTag :type="conf.categoryDict" :value="detail.ioCategory" />
          </a-descriptions-item>
          <a-descriptions-item :label="conf.warehouseLabel">{{ detail.warehouseName }}</a-descriptions-item>
          <a-descriptions-item v-if="kind === 'transfer'" :label="$t('wms.doc.toWarehouse')">{{ detail.toWarehouseName }}</a-descriptions-item>
          <a-descriptions-item v-if="kind === 'transfer'" :label="$t('wms.doc.transferMode')">
            {{ inTransit ? $t('wms.doc.inTransitMode') : $t('wms.doc.directTransfer') }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('wms.common.bizDate')">{{ detail.bizDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('wms.common.totalQty')">
            <span :class="negativeClass(detail.totalQty)">{{ formatQty(detail.totalQty) }}</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.sourceNo" :label="$t('wms.common.sourceNo')">{{ detail.sourceNo }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.redOf" :label="$t('wms.doc.reversedDoc')">{{ detail.redOfNo || detail.redOf }}</a-descriptions-item>
          <a-descriptions-item :label="$t('wms.doc.prepared')">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.confirmTime" :label="$t('wms.doc.confirmed')">
            {{ detail.confirmByName || '-' }} {{ detail.confirmTime }}
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.receiveTime" :label="$t('wms.doc.received')">
            {{ detail.receiveByName || '-' }} {{ detail.receiveTime }}
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <a-alert
          v-if="detail.status === 'APPROVING'"
          type="info"
          show-icon
          class="doc-desc"
          :message="$t('wms.doc.approvingTip')"
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
            <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
          </a-button>
          <a-button v-if="canConfirm" type="primary" :loading="confirming" @click="confirm">
            <template #icon><CheckOutlined /></template>{{ $t(`wms.doc.confirmAction.${kind}`) }}
          </a-button>
        </template>
        <a-popconfirm
          v-if="showReceive"
          :title="$t('wms.doc.confirmReceive', { name: detail.toWarehouseName })"
          :description="$t('wms.doc.receiveTip')"
          @confirm="receive"
        >
          <a-button type="primary" :loading="acting">
            <template #icon><InboxOutlined /></template>{{ $t('wms.common.receive') }}
          </a-button>
        </a-popconfirm>
        <a-popconfirm
          v-if="showReverse"
          :title="$t('wms.doc.confirmReverse', { kind: conf.label, no: detail.docNo })"
          :description="$t('wms.doc.reverseTip')"
          ok-type="danger"
          @confirm="reverse"
        >
          <a-button danger :loading="acting">
            <template #icon><RollbackOutlined /></template>{{ $t('wms.common.reverse') }}
          </a-button>
        </a-popconfirm>
      </div>
    </template>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import { CheckOutlined, EditOutlined, InboxOutlined, PrinterOutlined, RollbackOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import { confirmDoc, getDoc, receiveTransfer, reverseDoc } from '@/api/wms/doc'
import { useUserStore } from '@/store/user'
import { DOC_KINDS, canReverse, formatQty, locationText, negativeClass, splitSerials } from './wms'
import { materialColumns, printDocs } from '@/utils/print'
import { printName } from '@/utils/printTemplates'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'

/**
 * 出入库单详情；草稿可以编辑（触发 edit）、确认（更新库存后触发 changed）
 */
const props = defineProps({
  open: Boolean,
  kind: { type: String, required: true },
  docId: Number
})
const emit = defineEmits(['update:open', 'edit', 'changed'])
const { t } = useI18n()

const userStore = useUserStore()
const conf = computed(() => DOC_KINDS[props.kind])
const dict = useDict('wms_in_type', 'wms_out_type', 'wms_in_category', 'wms_out_category')
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
    { title: t('wms.common.material'), key: 'material', width: 240 },
    { title: props.kind === 'transfer' ? t('wms.doc.fromLocation') : t('wms.common.location'), key: 'location', width: 120 },
    props.kind === 'transfer' ? { title: t('wms.doc.toLocation'), key: 'toLocation', width: 120 } : null,
    { title: t('wms.common.batch'), key: 'batchNo', width: 180 },
    { title: t('common.field.quantity'), key: 'qty', width: 120, align: 'right' },
    { title: t('wms.common.serialNo'), key: 'serialNos', width: 260 },
    { title: t('common.field.remark'), dataIndex: 'remark', width: 150, ellipsis: true }
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
      ...(c.typeDict
        ? [
            [c.typeLabel, findDict(dict[c.typeDict], d.bizType)?.label ?? d.bizType, false, printName(props.kind === 'in' ? 'inType' : 'outType')],
            [c.categoryLabel, findDict(dict[c.categoryDict], d.ioCategory)?.label ?? d.ioCategory, false, printName(props.kind === 'in' ? 'inCategory' : 'outCategory')]
          ]
        : []),
      [c.warehouseLabel, d.warehouseName],
      ...(transfer ? [[t('wms.doc.toWarehouse'), d.toWarehouseName]] : []),
      [t('wms.common.bizDate'), d.bizDate],
      [t('wms.common.sourceNo'), d.sourceNo],
      [t('wms.common.totalQty'), formatQty(d.totalQty)]
    ],
    // 明细列的 id 是打印模板匹配用的原名称
    columns: [
      ...materialColumns(),
      { id: transfer ? '调出库位' : '库位', title: transfer ? t('wms.doc.fromLocation') : t('wms.common.location'), value: (r) => locationText(r.locationCode) }, // i18n-ignore
      ...(transfer ? [{ id: '调入库位', title: t('wms.doc.toLocation'), value: (r) => locationText(r.toLocationCode) }] : []), // i18n-ignore
      { id: '批次', title: t('wms.common.batch'), key: 'batchNo' }, // i18n-ignore
      { id: '数量', title: t('common.field.quantity'), key: 'qty', value: (r) => formatQty(r.qty), align: 'right' }, // i18n-ignore
      { id: '序列号', title: t('wms.common.serialNo'), key: 'serialNos' } // i18n-ignore
    ],
    rows: d.items,
    totals: { qty: formatQty(d.totalQty) },
    note: d.remark,
    signs: [[t('wms.doc.prepared'), d.createByName], [t('wms.doc.confirmed'), d.confirmByName], t('wms.doc.handler')]
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
  Modal.confirm({
    title: t(`wms.doc.confirmTitle.${props.kind}`),
    content: t('wms.doc.confirmContent'),
    async onOk() {
      confirming.value = true
      try {
        const flow = await confirmDoc(props.kind, props.docId)
        message.success(flow ? t('wms.doc.submittedApproval') : t(`wms.doc.confirmSuccess.${props.kind}`))
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
    message.success(t('wms.doc.receiveSuccess'))
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
    message.success(t('wms.doc.reverseSuccess'))
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
