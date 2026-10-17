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
import { t } from '@/locales'

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
      { title: t('crm.common.f.contractNo'), dataIndex: 'contractNo', width: 150 },
      { title: t('crm.common.f.contractName'), dataIndex: 'name', width: 180, ellipsis: true },
      { title: t('crm.common.f.signDate'), dataIndex: 'signDate', width: 110 },
      { title: t('crm.common.f.contractAmount'), dataIndex: 'amount', width: 130, align: 'right', money: true },
      { title: t('crm.common.f.received'), dataIndex: 'receivedAmount', width: 130, align: 'right', money: true },
      { title: t('crm.common.f.salesOrder'), dataIndex: 'orderNo', width: 150 },
      { title: t('common.field.status'), dataIndex: 'status', width: 90, dict: 'crm_contract_status' }
    ]
  },
  orders: {
    fetch: (p) => pageOrder('sales', { ...p, partnerId: props.customerId }),
    columns: [
      { title: t('crm.customerTrade.orderNo'), dataIndex: 'orderNo', width: 160 },
      { title: t('crm.customerTrade.orderDate'), dataIndex: 'orderDate', width: 110 },
      { title: t('crm.customerTrade.deliveryDate'), dataIndex: 'deliveryDate', width: 110 },
      { title: t('crm.customerTrade.totalAmount'), dataIndex: 'totalAmount', width: 130, align: 'right', money: true },
      { title: t('common.field.status'), dataIndex: 'status', width: 100, dict: 'scm_order_status' }
    ]
  },
  shipments: {
    fetch: (p) => pageShipment({ ...p, customerId: props.customerId }),
    columns: [
      { title: t('crm.customerTrade.shipNo'), dataIndex: 'shipNo', width: 160 },
      { title: t('crm.customerTrade.outboundNo'), dataIndex: 'tradeDocNo', width: 160 },
      { title: t('crm.customerTrade.carrier'), dataIndex: 'carrier', width: 100, dict: 'scm_carrier' },
      { title: t('crm.customerTrade.trackingNo'), dataIndex: 'trackingNo', width: 150 },
      { title: t('crm.customerTrade.shipDate'), dataIndex: 'shipDate', width: 110 },
      { title: t('common.field.status'), dataIndex: 'status', width: 90, dict: 'scm_shipment_status' }
    ]
  },
  bills: {
    fetch: (p) => pageBill('ar', { ...p, partnerId: props.customerId }),
    columns: [
      { title: t('crm.customerTrade.billNo'), dataIndex: 'billNo', width: 150 },
      { title: t('crm.customerTrade.sourceNo'), dataIndex: 'sourceNo', width: 150 },
      { title: t('crm.customerTrade.billDate'), dataIndex: 'billDate', width: 110 },
      { title: t('crm.customerTrade.dueDate'), dataIndex: 'dueDate', width: 110 },
      { title: t('common.field.amount'), dataIndex: 'amount', width: 120, align: 'right', money: true },
      { title: t('crm.customerTrade.settled'), dataIndex: 'settledAmount', width: 120, align: 'right', money: true },
      { title: t('common.field.status'), dataIndex: 'status', width: 90, dict: 'fin_bill_status' }
    ]
  },
  receipts: {
    fetch: (p) => pageSettlement('receipt', { ...p, partnerId: props.customerId }),
    columns: [
      { title: t('crm.customerTrade.receiptNo'), dataIndex: 'settleNo', width: 160 },
      { title: t('crm.customerTrade.receiptDate'), dataIndex: 'settleDate', width: 110 },
      { title: t('common.field.amount'), dataIndex: 'amount', width: 130, align: 'right', money: true },
      { title: t('crm.customerTrade.account'), dataIndex: 'accountName', width: 130 },
      { title: t('crm.customerTrade.method'), dataIndex: 'method', width: 100, dict: 'fin_settle_method' }
    ]
  },
  projects: {
    fetch: (p) => pageProject({ ...p, customerId: props.customerId }),
    columns: [
      { title: t('crm.customerTrade.projectCode'), dataIndex: 'code', width: 140 },
      { title: t('crm.customerTrade.projectName'), dataIndex: 'name', width: 200 },
      { title: t('crm.customerTrade.manager'), dataIndex: 'managerName', width: 100 },
      { title: t('crm.common.f.contractAmount'), dataIndex: 'contractAmount', width: 130, align: 'right', money: true },
      { title: t('common.field.status'), dataIndex: 'status', width: 100, dict: 'pm_project_status' }
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
