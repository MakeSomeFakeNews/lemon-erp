<template>
  <a-table
    row-key="id"
    size="small"
    :columns="columns"
    :data-source="list"
    :loading="loading"
    :pagination="total > 20 ? { current, pageSize: 20, total, size: 'small', onChange: (p) => ((current = p), load()) } : false"
    :scroll="{ x: 760 }"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.money">{{ record[column.dataIndex] == null ? '-' : '¥' + formatMoney(record[column.dataIndex]) }}</template>
      <DictTag v-else-if="column.dict" :type="column.dict" :value="record[column.dataIndex]" />
    </template>
  </a-table>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { pageOrder } from '@/api/scm/order'
import { pageShipment } from '@/api/scm/shipment'
import { pageBill, pageSettlement } from '@/api/fin'
import { pageProject } from '@/api/pm'
import { pageCrmContract } from '@/api/crm'
import { formatMoney } from '@/components/scm/scm'

/**
 * 客户 360：按客户查询合同、销售订单、发运、应收、收款、项目（复用各模块的列表接口，按各自的权限和数据权限返回）
 */
const props = defineProps({
  customerId: { type: Number, required: true },
  kind: { type: String, required: true }
})

const CONF = {
  contracts: {
    fetch: (p) => pageCrmContract({ ...p, customerId: props.customerId }),
    columns: [
      { title: '合同编号', dataIndex: 'contractNo', width: 150 },
      { title: '合同名称', dataIndex: 'name', width: 180, ellipsis: true },
      { title: '签订日期', dataIndex: 'signDate', width: 110 },
      { title: '合同金额', dataIndex: 'amount', width: 130, align: 'right', money: true },
      { title: '已回款', dataIndex: 'receivedAmount', width: 130, align: 'right', money: true },
      { title: '销售订单', dataIndex: 'orderNo', width: 150 },
      { title: '状态', dataIndex: 'status', width: 90, dict: 'crm_contract_status' }
    ]
  },
  orders: {
    fetch: (p) => pageOrder('sales', { ...p, partnerId: props.customerId }),
    columns: [
      { title: '订单号', dataIndex: 'orderNo', width: 160 },
      { title: '订单日期', dataIndex: 'orderDate', width: 110 },
      { title: '交货日期', dataIndex: 'deliveryDate', width: 110 },
      { title: '价税合计', dataIndex: 'totalAmount', width: 130, align: 'right', money: true },
      { title: '状态', dataIndex: 'status', width: 100, dict: 'scm_order_status' }
    ]
  },
  shipments: {
    fetch: (p) => pageShipment({ ...p, customerId: props.customerId }),
    columns: [
      { title: '发运单', dataIndex: 'shipNo', width: 160 },
      { title: '出库单', dataIndex: 'tradeDocNo', width: 160 },
      { title: '承运商', dataIndex: 'carrier', width: 100, dict: 'scm_carrier' },
      { title: '运单号', dataIndex: 'trackingNo', width: 150 },
      { title: '发货日期', dataIndex: 'shipDate', width: 110 },
      { title: '状态', dataIndex: 'status', width: 90, dict: 'scm_shipment_status' }
    ]
  },
  bills: {
    fetch: (p) => pageBill('ar', { ...p, partnerId: props.customerId }),
    columns: [
      { title: '应收单', dataIndex: 'billNo', width: 150 },
      { title: '来源', dataIndex: 'sourceNo', width: 150 },
      { title: '单据日期', dataIndex: 'billDate', width: 110 },
      { title: '到期日', dataIndex: 'dueDate', width: 110 },
      { title: '金额', dataIndex: 'amount', width: 120, align: 'right', money: true },
      { title: '已收', dataIndex: 'settledAmount', width: 120, align: 'right', money: true },
      { title: '状态', dataIndex: 'status', width: 90, dict: 'fin_bill_status' }
    ]
  },
  receipts: {
    fetch: (p) => pageSettlement('receipt', { ...p, partnerId: props.customerId }),
    columns: [
      { title: '收款单', dataIndex: 'settleNo', width: 160 },
      { title: '收款日期', dataIndex: 'settleDate', width: 110 },
      { title: '金额', dataIndex: 'amount', width: 130, align: 'right', money: true },
      { title: '结算账户', dataIndex: 'accountName', width: 130 },
      { title: '方式', dataIndex: 'method', width: 100, dict: 'fin_settle_method' }
    ]
  },
  projects: {
    fetch: (p) => pageProject({ ...p, customerId: props.customerId }),
    columns: [
      { title: '项目编号', dataIndex: 'code', width: 140 },
      { title: '项目名称', dataIndex: 'name', width: 200 },
      { title: '项目经理', dataIndex: 'managerName', width: 100 },
      { title: '合同金额', dataIndex: 'contractAmount', width: 130, align: 'right', money: true },
      { title: '状态', dataIndex: 'status', width: 100, dict: 'pm_project_status' }
    ]
  }
}

const columns = computed(() => CONF[props.kind].columns)
const list = ref([])
const total = ref(0)
const current = ref(1)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await CONF[props.kind].fetch({ current: current.value, size: 20 })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}
watch(
  () => [props.customerId, props.kind],
  () => {
    current.value = 1
    load()
  },
  { immediate: true }
)
</script>
