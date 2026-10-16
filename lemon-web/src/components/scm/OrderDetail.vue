<template>
  <SubPage
    :open="open"
    :title="detail ? `${conf.label} ${detail.orderNo}${detail.version > 1 ? ` V${detail.version}` : ''}` : conf.label"
    width="min(1100px, 100vw)"
    destroy-on-close
    @close="emit('update:open', false)"
  >
    <template #extra>
      <a-space>
        <a-button v-if="detail" size="small" @click="print"><template #icon><PrinterOutlined /></template>打印</a-button>
        <DictTag v-if="detail" type="scm_order_status" :value="detail.status" />
      </a-space>
    </template>

    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="doc-desc">
          <a-descriptions-item label="订单号">
            {{ detail.orderNo }}
            <a-tag v-if="detail.version > 1" color="blue" :bordered="false" class="ver-tag">V{{ detail.version }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item :label="conf.partnerLabel">{{ detail.partnerCode }} {{ detail.partnerName }}</a-descriptions-item>
          <a-descriptions-item label="订单日期">{{ detail.orderDate }}</a-descriptions-item>
          <a-descriptions-item label="交货日期">{{ detail.deliveryDate || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="conf.warehouseLabel">{{ detail.warehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="来源"><DictTag type="scm_order_source" :value="detail.source" /></a-descriptions-item>
          <a-descriptions-item label="价税合计">
            <b>{{ symbol }}{{ formatMoney(detail.totalAmount) }}</b>
            <span class="sub-text">（税额 {{ formatMoney(detail.taxAmount) }}）</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="foreign" label="币种 / 汇率">
            {{ detail.currency }} @{{ Number(detail.exchangeRate) }}
            <span class="sub-text">（≈ ¥{{ formatMoney(Number(detail.totalAmount) * Number(detail.exchangeRate || 0)) }}）</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="Number(detail.discountAmount) || Number(detail.rebateAmount)" label="优惠">
            <span v-if="Number(detail.discountAmount)">促销 {{ symbol }}{{ formatMoney(detail.discountAmount) }}</span>
            <span v-if="Number(detail.discountAmount) && Number(detail.rebateAmount)">，</span>
            <span v-if="Number(detail.rebateAmount)">返利抵扣 {{ symbol }}{{ formatMoney(detail.rebateAmount) }}</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.contractNo" label="采购合同">{{ detail.contractNo }}</a-descriptions-item>
          <a-descriptions-item v-if="kind === 'purchase' && detail.approveTime" label="供应商确认">
            <template v-if="detail.supplierConfirm">
              <DictTag type="scm_supplier_confirm" :value="detail.supplierConfirm" />
              <span v-if="detail.promisedDate">承诺交期 {{ detail.promisedDate }}</span>
              <span v-if="detail.confirmRemark" class="sub-text"> {{ detail.confirmRemark }}</span>
            </template>
            <span v-else class="sub-text">未确认</span>
          </a-descriptions-item>
          <a-descriptions-item label="制单">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.approveTime" label="审核">
            {{ detail.approveByName || '-' }} {{ detail.approveTime }}
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="备注">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <a-alert
          v-if="detail.status === 'APPROVING'"
          type="info"
          show-icon
          class="doc-desc"
          message="订单审批中，可在「流程中心 → 我的申请」查看审批进度，审批通过后自动变为已审核"
        />

        <a-tabs v-model:active-key="tab" size="small">
          <a-tab-pane key="items" tab="订单明细">
            <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false" :scroll="{ x: 950 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'material'">
                  <div>{{ record.materialCode }} {{ record.materialName }}</div>
                  <div class="sub-text">{{ record.spec }}</div>
                </template>
                <template v-else-if="column.key === 'qty'">{{ qtyText(record) }}</template>
                <template v-else-if="column.key === 'price'">
                  <div>{{ symbol }}{{ formatMoney(record.price) }}</div>
                  <div v-if="record.gift" class="sub-text"><a-tag color="magenta">赠品</a-tag>{{ record.promotionName }}</div>
                  <div v-else-if="record.origPrice != null" class="sub-text">
                    <del>{{ symbol }}{{ formatMoney(record.origPrice) }}</del> {{ record.promotionName || '优惠分摊' }}
                  </div>
                  <div v-if="hasUnit(record)" class="sub-text">每{{ record.unitName }} {{ symbol }}{{ formatMoney(record.price * record.unitRatio) }}</div>
                  <div class="sub-text">税率 {{ Number(record.taxRate) }}%</div>
                </template>
                <template v-else-if="column.key === 'amount'">{{ symbol }}{{ formatMoney(record.amount) }}</template>
                <template v-else-if="column.key === 'progress'">
                  <a-progress :percent="percent(record)" size="small" :status="percent(record) >= 100 ? 'success' : 'normal'" />
                  <div class="sub-text">
                    {{ kind === 'purchase' ? '已入库' : '已出库' }} {{ formatQty(record.doneQty) }}
                    <template v-if="Number(record.returnQty)">，已退货 {{ formatQty(record.returnQty) }}</template>
                  </div>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane v-for="t in conf.trades" :key="t" :tab="`${TRADE_KINDS[t].label}记录`">
            <a-table row-key="id" size="small" :columns="tradeColumns" :data-source="trades[t]" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'totalQty'">{{ formatQty(record.totalQty) }}</template>
                <template v-else-if="column.key === 'totalAmount'">¥{{ formatMoney(record.totalAmount) }}</template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane v-if="kind === 'sales'" key="reservation" tab="库存预留">
            <SalesReservationPanel
              v-if="tab === 'reservation'"
              :order="detail"
              :editable="['APPROVED', 'PARTIAL'].includes(detail.status) && can('sales:reserve')"
            />
          </a-tab-pane>
          <a-tab-pane v-if="kind === 'sales' && can('delivery:list')" key="delivery" tab="发货通知">
            <a-table row-key="id" size="small" :columns="deliveryColumns" :data-source="deliveries" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'deliveryNo'">
                  <a @click="toDelivery(record.id)">{{ record.deliveryNo }}</a>
                </template>
                <template v-else-if="column.key === 'totalQty'">{{ formatQty(record.totalQty) }}</template>
                <template v-else-if="column.key === 'status'">
                  <DictTag type="scm_delivery_status" :value="record.status" />
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane v-if="kind === 'purchase'" key="outsource" tab="委外用料">
            <OutsourcePanel
              v-if="detail"
              :order-id="detail.id"
              :editable="['APPROVED', 'PARTIAL', 'COMPLETED'].includes(detail.status) && userStore.hasPerm('scm:purchaseIn:add')"
            />
          </a-tab-pane>
          <a-tab-pane key="trace" tab="单据追溯">
            <OrderTrace v-if="detail" :kind="kind" :order-id="detail.id" @voucher="(id) => ((voucherId = id), (voucherOpen = true))" />
          </a-tab-pane>
          <a-tab-pane key="attachment" tab="附件">
            <AttachmentPanel :biz-type="`scm_${kind}`" :biz-id="orderId" :editable="can(`${kind}:edit`)" />
          </a-tab-pane>
          <a-tab-pane key="changes" tab="变更记录">
            <OrderChangeList v-if="tab === 'changes'" :key="changeSeq" :kind="kind" :order-id="detail.id" :currency="detail.currency" />
          </a-tab-pane>
          <a-tab-pane key="history" tab="修改历史">
            <ChangeLogPanel :biz-type="`scm_${kind}`" :biz-id="orderId" />
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-spin>

    <template v-if="detail && actions.length" #footer>
      <div class="drawer-footer">
        <a-button v-for="a in actions" :key="a.key" :type="a.primary ? 'primary' : 'default'" :danger="a.danger" @click="a.run">
          <template #icon><component :is="a.icon" /></template>{{ a.label }}
        </a-button>
      </div>
    </template>
    <VoucherDetail v-model:open="voucherOpen" :voucher-id="voucherId" />
    <OrderChangeDrawer v-model:open="changeOpen" :kind="kind" :order="detail" @success="onChanged" />
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import {
  AuditOutlined,
  DiffOutlined,
  EditOutlined,
  ExportOutlined,
  ImportOutlined,
  PrinterOutlined,
  ProjectOutlined,
  RollbackOutlined,
  SendOutlined,
  StopOutlined,
  UndoOutlined
} from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import VoucherDetail from '@/components/fin/VoucherDetail.vue'
import OrderTrace from './OrderTrace.vue'
import OutsourcePanel from './OutsourcePanel.vue'
import SalesReservationPanel from './SalesReservationPanel.vue'
import ChangeLogPanel from '@/components/ChangeLogPanel.vue'
import OrderChangeDrawer from './OrderChangeDrawer.vue'
import OrderChangeList from './OrderChangeList.vue'
import { currencySymbol, isForeign } from '@/components/fin/currency'
import { approveOrder, closeOrder, getOrder, unapproveOrder, withdrawOrder } from '@/api/scm/order'
import { pageTrade } from '@/api/scm/trade'
import { pageDelivery } from '@/api/scm/planning'
import { useUserStore } from '@/store/user'
import { formatQty } from '@/components/wms/wms'
import { ORDER_KINDS, TRADE_KINDS, formatMoney, netQty } from './scm'
import { materialColumns, printDocs } from '@/utils/print'

/**
 * 订单详情：明细执行进度、入库 / 出库与退货记录，按状态提供审核、撤回、反审核、关闭、入库 / 出库、退货等操作。
 * 触发 edit(order)、trade(tradeKind, order)、changed
 */
const props = defineProps({
  open: Boolean,
  kind: { type: String, required: true },
  orderId: Number
})
const emit = defineEmits(['update:open', 'edit', 'trade', 'changed'])

const userStore = useUserStore()
const conf = computed(() => ORDER_KINDS[props.kind])
const can = (perm) => userStore.hasPerm(`scm:${perm}`)
const router = useRouter()

const itemColumns = [
  { title: '物料', key: 'material', width: 240 },
  { title: '数量', key: 'qty', width: 150, align: 'right' },
  { title: '含税单价', key: 'price', width: 170, align: 'right' },
  { title: '价税合计', key: 'amount', width: 120, align: 'right' },
  { title: '交货日期', dataIndex: 'deliveryDate', width: 110 },
  { title: '执行进度', key: 'progress', width: 200 },
  { title: '备注', dataIndex: 'remark', width: 120, ellipsis: true }
]
const tradeColumns = [
  { title: '单据号', dataIndex: 'docNo', width: 170 },
  { title: '业务日期', dataIndex: 'bizDate', width: 110 },
  { title: '仓库', dataIndex: 'warehouseName', width: 140 },
  { title: '数量', key: 'totalQty', width: 100, align: 'right' },
  { title: '价税合计', key: 'totalAmount', width: 130, align: 'right' },
  { title: '仓库单据', dataIndex: 'wmsDocNo', width: 170 },
  { title: '制单人', dataIndex: 'createByName', width: 100 }
]
const deliveryColumns = [
  { title: '通知单号', key: 'deliveryNo', width: 170 },
  { title: '发货日期', dataIndex: 'shipDate', width: 110 },
  { title: '仓库', dataIndex: 'warehouseName', width: 140 },
  { title: '数量', key: 'totalQty', width: 100, align: 'right' },
  { title: '状态', key: 'status', width: 90 },
  { title: '销售出库单', dataIndex: 'tradeDocNo', width: 170 },
  { title: '制单人', dataIndex: 'createByName', width: 100 }
]

const detail = ref()
const deliveries = ref([])
const voucherOpen = ref(false)
const voucherId = ref()

function print() {
  const d = detail.value
  const c = conf.value
  printDocs({
    title: c.label,
    no: d.orderNo,
    fields: [
      [c.partnerLabel, `${d.partnerCode || ''} ${d.partnerName || ''}`],
      ['订单日期', d.orderDate],
      ['交货日期', d.deliveryDate],
      [c.warehouseLabel, d.warehouseName],
      ['价税合计', `${currencySymbol(d.currency)}${formatMoney(d.totalAmount)}`],
      ['税额', `${currencySymbol(d.currency)}${formatMoney(d.taxAmount)}`],
      ...(isForeign(d.currency) ? [['币种 / 汇率', `${d.currency} @${Number(d.exchangeRate)}`]] : []),
      ...(d.version > 1 ? [['版本', `V${d.version}`]] : [])
    ],
    columns: [
      ...materialColumns(),
      { title: '数量', value: qtyText, align: 'right' },
      { title: '含税单价', value: (r) => formatMoney(r.price), align: 'right' },
      { title: '税率', value: (r) => `${Number(r.taxRate)}%`, align: 'right' },
      { title: '价税合计', key: 'amount', value: (r) => formatMoney(r.amount), align: 'right' },
      { title: '交货日期', key: 'deliveryDate' }
    ],
    rows: d.items,
    totals: { amount: formatMoney(d.totalAmount) },
    note: d.remark,
    signs: [['制单', d.createByName], ['审核', d.approveByName], `${c.partnerLabel}确认`]
  }, `${c.label} ${d.orderNo}`, props.kind === 'purchase' ? 'scm.purchaseOrder' : 'scm.salesOrder')
}
const loading = ref(false)
const tab = ref('items')

const foreign = computed(() => isForeign(detail.value?.currency))
const symbol = computed(() => currencySymbol(detail.value?.currency))

// 订单变更
const changeOpen = ref(false)
const changeSeq = ref(0)
async function onChanged() {
  changeSeq.value++
  tab.value = 'changes'
  await load()
  emit('changed')
}
const trades = reactive({})

/** 按辅助单位下单的明细：「2 箱（24 个）」；数量、单价都按基本单位存储 */
const hasUnit = (i) => !!i.unitName && Number(i.unitRatio) > 0
const qtyText = (i) =>
  hasUnit(i)
    ? `${formatQty(Number(i.qty) / Number(i.unitRatio))} ${i.unitName}（${formatQty(i.qty)} ${i.unit || ''}）`
    : `${formatQty(i.qty)} ${i.unit || ''}`
const percent = (i) => Math.min(Math.round((netQty(i) / Number(i.qty)) * 100), 100)

async function load() {
  loading.value = true
  try {
    detail.value = await getOrder(props.kind, props.orderId)
    await Promise.all([
      ...conf.value.trades.map(async (t) => {
        trades[t] = can(`${t}:list`) ? (await pageTrade(t, { orderId: props.orderId, current: 1, size: 100 })).records : []
      }),
      (async () => {
        deliveries.value =
          props.kind === 'sales' && can('delivery:list') ? (await pageDelivery({ orderId: props.orderId, current: 1, size: 100 })).records : []
      })()
    ])
  } finally {
    loading.value = false
  }
}

watch(
  () => props.open && props.orderId,
  (v) => {
    detail.value = undefined
    tab.value = 'items'
    if (v) load()
  }
)

const toDelivery = (id) => router.push({ path: '/scm/delivery', query: { id } })

function confirmAction(title, content, api, success) {
  Modal.confirm({
    title,
    content,
    async onOk() {
      const res = await api(props.kind, props.orderId)
      message.success(typeof success === 'function' ? success(res) : success)
      await load()
      emit('changed')
    }
  })
}

const actions = computed(() => {
  const d = detail.value
  if (!d) return []
  const k = props.kind
  const [doTrade, returnTrade] = conf.value.trades
  const list = []
  if (d.status === 'DRAFT') {
    if (can(`${k}:edit`)) list.push({ key: 'edit', label: '编辑', icon: EditOutlined, run: () => emit('edit', d) })
    if (can(`${k}:approve`))
      list.push({
        key: 'approve',
        label: '审核',
        icon: AuditOutlined,
        primary: true,
        run: () =>
          confirmAction('审核订单？', '发布了审批流程时将发起审批，否则直接审核通过。', approveOrder, (flow) =>
            flow ? '已提交审批' : '审核成功'
          )
      })
  }
  if (d.status === 'APPROVING' && can(`${k}:approve`)) {
    list.push({
      key: 'withdraw',
      label: '撤回审批',
      icon: UndoOutlined,
      run: () => confirmAction('撤回审批？', '撤销审批流程，订单回到草稿。', withdrawOrder, '已撤回')
    })
  }
  if (d.status === 'APPROVED' && can(`${k}:approve`)) {
    list.push({
      key: 'unapprove',
      label: '反审核',
      icon: UndoOutlined,
      run: () => confirmAction('反审核？', '订单回到草稿，可以修改。', unapproveOrder, '已反审核')
    })
  }
  if (['APPROVED', 'PARTIAL'].includes(d.status) && can(`${k}:close`)) {
    list.push({
      key: 'close',
      label: '关闭',
      icon: StopOutlined,
      danger: true,
      run: () => confirmAction('关闭订单？', `关闭后不能再${k === 'purchase' ? '入库' : '出库'}，已完成部分不受影响。`, closeOrder, '已关闭')
    })
  }
  if (['APPROVED', 'PARTIAL'].includes(d.status) && can(`${k}:change`)) {
    list.push({ key: 'change', label: '变更', icon: DiffOutlined, run: () => (changeOpen.value = true) })
  }
  if (['PARTIAL', 'COMPLETED', 'CLOSED'].includes(d.status) && can(`${returnTrade}:add`)) {
    list.push({ key: 'return', label: TRADE_KINDS[returnTrade].label, icon: RollbackOutlined, run: () => emit('trade', returnTrade, d) })
  }
  if (['APPROVED', 'PARTIAL'].includes(d.status) && can(`${doTrade}:add`)) {
    list.push({
      key: 'trade',
      label: TRADE_KINDS[doTrade].label,
      icon: k === 'purchase' ? ImportOutlined : ExportOutlined,
      primary: true,
      run: () => emit('trade', doTrade, d)
    })
  }
  if (k === 'sales' && ['APPROVED', 'PARTIAL'].includes(d.status) && can('delivery:add')) {
    list.push({
      key: 'delivery',
      label: '发货通知',
      icon: SendOutlined,
      run: () => router.push({ path: '/scm/delivery', query: { orderId: d.id, create: 1 } })
    })
  }
  // 销售订单一键建项目（交付类订单）
  if (k === 'sales' && ['APPROVED', 'PARTIAL', 'COMPLETED'].includes(d.status) && userStore.hasPerm('pm:project:add')) {
    list.push({
      key: 'project',
      label: '创建项目',
      icon: ProjectOutlined,
      run: () =>
        router.push({
          path: '/pm/project',
          query: {
            create: 1,
            salesOrderId: d.id,
            salesOrderNo: d.orderNo,
            customerId: d.partnerId,
            contractAmount: d.totalAmount,
            name: `${d.partnerName || ''} ${d.orderNo}`.trim()
          }
        })
    })
  }
  return list
})

defineExpose({ reload: load })
</script>

<style scoped>
.doc-desc {
  margin-bottom: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.ver-tag {
  margin-left: 4px;
}
</style>
