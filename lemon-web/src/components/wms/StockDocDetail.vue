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
          <a-descriptions-item v-if="kind === 'transfer'" :label="$t('wms.transfer.pricing')">{{ pricingText }}</a-descriptions-item>
          <a-descriptions-item :label="$t('wms.common.bizDate')">{{ detail.bizDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('wms.common.totalQty')">
            <span :class="negativeClass(detail.totalQty)">{{ formatQty(detail.totalQty) }}</span>
          </a-descriptions-item>
          <template v-if="kind === 'transfer' && detail.transferAmount != null">
            <a-descriptions-item :label="$t('wms.transfer.costAmount')">{{ money(detail.costAmount) }}</a-descriptions-item>
            <a-descriptions-item :label="$t('wms.transfer.transferAmount')">{{ money(detail.transferAmount) }}</a-descriptions-item>
            <a-descriptions-item :label="$t('wms.transfer.diffAmount')">{{ money(diffAmount) }}</a-descriptions-item>
          </template>
          <a-descriptions-item v-if="Number(detail.fee)" :label="$t('wms.transfer.fee')">
            <span :class="negativeClass(detail.fee)">¥{{ money(detail.fee) }}</span>
            <DictTag v-if="detail.feeSettle" type="scm_freight_settle" :value="detail.feeSettle" class="fee-tag" />
            <div class="sub-text">{{ [detail.feeCarrierName, detail.feeAccountName].filter(Boolean).join(' · ') }}</div>
            <div v-if="detail.feeIoId" class="sub-text">
              <a v-if="userStore.hasPerm('fin:io:list')" @click="openIo">{{ $t('wms.transfer.ioDoc') }}</a>
              <span v-else>{{ $t('wms.transfer.ioDoc') }}</span>
            </div>
            <div v-else-if="detail.feeBillId" class="sub-text">{{ $t('wms.transfer.carrierBill') }}</div>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.sourceNo" :label="$t('wms.common.sourceNo')">{{ detail.sourceNo }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.partnerName" :label="$t('wms.flow.partner')">{{ detail.partnerName }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.redOf" :label="$t('wms.doc.reversedDoc')">{{ detail.redOfNo || detail.redOf }}</a-descriptions-item>
          <a-descriptions-item :label="$t('wms.doc.prepared')">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.confirmTime" :label="$t('wms.doc.confirmed')">
            {{ detail.confirmByName || '-' }} {{ detail.confirmTime }}
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.receiveTime" :label="$t('wms.doc.received')">
            {{ detail.receiveByName || '-' }} {{ detail.receiveTime }}
            <template v-if="detail.receiveDate"> · {{ $t('wms.doc.receiveDate') }} {{ detail.receiveDate }}</template>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <a-alert
          v-if="detail.status === WmsDocStatus.APPROVING"
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
              <!-- 入库填写的日期，出库等随批次带出登记的生产日期、有效期 -->
              <div v-if="batchDates(record)" class="sub-text">{{ batchDates(record) }}</div>
            </template>
            <template v-else-if="column.key === 'qty'">
              <span :class="negativeClass(record.qty)">{{ formatQty(record.qty) }}</span> {{ record.unit }}
            </template>
            <template v-else-if="column.key === 'costPrice'">{{ record.costPrice == null ? '-' : price(record.costPrice) }}</template>
            <template v-else-if="column.key === 'transferPrice'">{{ record.transferPrice == null ? '-' : price(record.transferPrice) }}</template>
            <template v-else-if="column.key === 'transferAmount'">
              <span :class="negativeClass(record.transferAmount)">{{ record.transferAmount == null ? '-' : money(record.transferAmount) }}</span>
            </template>
            <template v-else-if="column.key === 'serialNos'">
              <SerialTags :value="record.serialNos" />
            </template>
          </template>
        </a-table>
      </template>
    </a-spin>

    <template v-if="showFooter" #footer>
      <div class="drawer-footer">
        <template v-if="detail.status === WmsDocStatus.DRAFT">
          <a-button v-if="canEdit" @click="emit('edit', detail)">
            <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
          </a-button>
          <a-button v-if="canConfirm" type="primary" :loading="confirming" @click="confirm">
            <template #icon><CheckOutlined /></template>{{ $t(`wms.doc.confirmAction.${kind}`) }}
          </a-button>
        </template>
        <ReceiveConfirm v-if="showReceive" :warehouse-name="detail.toWarehouseName" @confirm="receive">
          <a-button type="primary" :loading="acting">
            <template #icon><InboxOutlined /></template>{{ $t('wms.common.receive') }}
          </a-button>
        </ReceiveConfirm>
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
import SerialTags from '@/components/wms/SerialTags.vue'
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import { CheckOutlined, EditOutlined, InboxOutlined, PrinterOutlined, RollbackOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import ReceiveConfirm from './ReceiveConfirm.vue'
import { confirmDoc, getDoc, receiveTransfer, reverseDoc } from '@/api/wms/doc'
import { useUserStore } from '@/store/user'
import { DOC_KINDS, batchDates, canReverse, formatQty, locationText, negativeClass } from './wms'
import { materialColumns, printDocs } from '@/utils/print'
import { printName } from '@/utils/printTemplates'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { money, price } from '@/utils/number'
import { useRouter } from 'vue-router'
import { usePriceLevels } from './priceLevels'

import { TransferPricing, WmsDocStatus } from '@/constants/enums'
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
const router = useRouter()
const { levelName } = usePriceLevels()
const conf = computed(() => DOC_KINDS[props.kind])
const dict = useDict('wms_in_type', 'wms_out_type', 'wms_in_category', 'wms_out_category', 'scm_freight_settle')
const canEdit = computed(() => userStore.hasPerm(`wms:${props.kind}:edit`))
const canConfirm = computed(() => userStore.hasPerm(`wms:${props.kind}:confirm`))
const inTransit = computed(() => detail.value?.inTransit === true || detail.value?.inTransit === 1)
const showReceive = computed(
  () => props.kind === 'transfer' && detail.value?.status === WmsDocStatus.TRANSIT && userStore.hasPerm('wms:transfer:receive')
)
const showReverse = computed(() => !!detail.value && canReverse(detail.value) && userStore.hasPerm(`wms:${props.kind}:reverse`))
const showFooter = computed(
  () => !!detail.value && ((detail.value.status === WmsDocStatus.DRAFT && (canEdit.value || canConfirm.value)) || showReceive.value || showReverse.value)
)

const hasCost = computed(() => (detail.value?.items || []).some((i) => i.costPrice != null))
/** 调拨价：有「查看成本金额」权限时后端才返回 */
const hasTransferPrice = computed(() => props.kind === 'transfer' && (detail.value?.items || []).some((i) => i.transferPrice != null))
/** 调拨价格：同价调拨 / 差价调拨（依据 × 系数） */
const pricingText = computed(() => {
  const d = detail.value
  if (!d?.transferPricing || d.transferPricing === TransferPricing.SAME) return t('wms.transfer.pricings.SAME')
  const basis = d.transferPricing === TransferPricing.PRICE_LEVEL ? levelName(d.priceLevel) : t(`wms.transfer.pricings.${d.transferPricing}`)
  return t('wms.transfer.differentialText', { basis, factor: Number(d.priceFactor ?? 1) })
})
const diffAmount = computed(() => Number(detail.value?.transferAmount || 0) - Number(detail.value?.costAmount || 0))
const openIo = () => router.push({ path: '/fin/income-expense', query: { id: detail.value.feeIoId } })
/** 明细带仓库（如销售出库从多个仓库出库）时显示仓库列 */
const lineWarehouse = computed(() => (detail.value?.items || []).some((i) => i.warehouseId))
const columns = computed(() =>
  [
    { title: t('wms.common.material'), key: 'material', width: 240 },
    lineWarehouse.value ? { title: t('wms.common.warehouse'), dataIndex: 'warehouseName', width: 130 } : null,
    { title: props.kind === 'transfer' ? t('wms.doc.fromLocation') : t('wms.common.location'), key: 'location', width: 120 },
    props.kind === 'transfer' ? { title: t('wms.doc.toLocation'), key: 'toLocation', width: 120 } : null,
    { title: t('wms.common.batch'), key: 'batchNo', width: 180 },
    { title: t('common.field.quantity'), key: 'qty', width: 120, align: 'right' },
    // 成本价：入库填写或业务单据传入的入库成本（有权限时后端才返回）
    hasCost.value
      ? { title: props.kind === 'transfer' ? t('wms.transfer.costPrice') : t('wms.doc.costPrice'), key: 'costPrice', width: 110, align: 'right' }
      : null,
    hasTransferPrice.value ? { title: t('wms.transfer.transferPrice'), key: 'transferPrice', width: 110, align: 'right' } : null,
    hasTransferPrice.value ? { title: t('wms.transfer.transferAmount'), key: 'transferAmount', width: 120, align: 'right' } : null,
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
      [t('wms.common.totalQty'), formatQty(d.totalQty)],
      ...(transfer ? transferPrintFields(d) : [])
    ],
    // 明细列的 id 是打印模板匹配用的原名称
    columns: [
      ...materialColumns(),
      ...(lineWarehouse.value ? [{ id: '仓库', title: t('wms.common.warehouse'), key: 'warehouseName' }] : []), // i18n-ignore
      { id: transfer ? '调出库位' : '库位', title: transfer ? t('wms.doc.fromLocation') : t('wms.common.location'), value: (r) => locationText(r.locationCode) }, // i18n-ignore
      ...(transfer ? [{ id: '调入库位', title: t('wms.doc.toLocation'), value: (r) => locationText(r.toLocationCode) }] : []), // i18n-ignore
      { id: '批次', title: t('wms.common.batch'), key: 'batchNo' }, // i18n-ignore
      { id: '数量', title: t('common.field.quantity'), key: 'qty', value: (r) => formatQty(r.qty), align: 'right' }, // i18n-ignore
      ...(transfer ? transferPrintColumns() : []),
      { id: '序列号', title: t('wms.common.serialNo'), key: 'serialNos' } // i18n-ignore
    ],
    rows: d.items,
    totals: { qty: formatQty(d.totalQty), ...(transfer && d.transferAmount != null ? { transferAmount: money(d.transferAmount) } : {}) },
    note: d.remark,
    signs: [[t('wms.doc.prepared'), d.createByName], [t('wms.doc.confirmed'), d.confirmByName], t('wms.doc.handler')]
  }, `${c.label} ${d.docNo}`, `wms.${props.kind}`)
}

/** 调拨单打印的调拨价格、金额、费用字段（第四项为打印模板的匹配键） */
function transferPrintFields(d) {
  const amount = (v) => (v == null ? '' : money(v))
  return [
    [t('wms.transfer.pricing'), pricingText.value, false, printName('transferPricing')],
    [t('wms.transfer.costAmount'), amount(d.costAmount), false, printName('transferCostAmount')],
    [t('wms.transfer.transferAmount'), amount(d.transferAmount), false, printName('transferAmount')],
    [t('wms.transfer.diffAmount'), d.transferAmount == null ? '' : money(diffAmount.value), false, printName('transferDiff')],
    [t('wms.transfer.fee'), Number(d.fee) ? money(d.fee) : '', false, printName('transferFee')],
    [t('wms.transfer.feeSettle'), findDict(dict.scm_freight_settle, d.feeSettle)?.label ?? '', false, printName('feeSettle')],
    [t('wms.transfer.feeAccount'), d.feeAccountName, false, printName('feeAccount')],
    [t('wms.transfer.carrier'), d.feeCarrierName, false, printName('feeCarrier')]
  ]
}

/** 调拨单打印的价格列（没有「查看成本金额」权限时为空） */
const transferPrintColumns = () => [
  { id: printName('transferCostPrice'), title: t('wms.transfer.costPrice'), value: (r) => (r.costPrice == null ? '' : price(r.costPrice)), align: 'right' },
  { id: printName('transferPrice'), title: t('wms.transfer.transferPrice'), value: (r) => (r.transferPrice == null ? '' : price(r.transferPrice)), align: 'right' },
  { id: printName('transferAmount'), title: t('wms.transfer.transferAmount'), key: 'transferAmount', value: (r) => (r.transferAmount == null ? '' : money(r.transferAmount)), align: 'right' }
]
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
async function receive(receiveDate) {
  acting.value = true
  try {
    await receiveTransfer(props.docId, receiveDate)
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
.fee-tag {
  margin-left: 6px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
