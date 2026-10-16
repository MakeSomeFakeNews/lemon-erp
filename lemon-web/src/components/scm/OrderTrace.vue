<template>
  <a-spin :spinning="loading">
    <a-empty v-if="!loading && !sections.length" description="暂无下游单据" />
    <div v-for="s in sections" :key="s.key" class="trace-section">
      <div class="trace-title">{{ s.title }} <span class="sub-text">（{{ s.rows.length }}）</span></div>
      <a-table row-key="id" size="small" :columns="s.columns" :data-source="s.rows" :pagination="false" :scroll="{ x: 700 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.money">{{ record[column.dataIndex] == null ? '-' : '¥' + formatMoney(record[column.dataIndex]) }}</template>
          <DictTag v-else-if="column.dict" :type="column.dict" :value="record[column.dataIndex]" />
          <a v-else-if="column.voucher && record.voucherId" @click="emit('voucher', record.voucherId)">{{ record.voucherNo }}</a>
        </template>
      </a-table>
    </div>
  </a-spin>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { orderTrace } from '@/api/scm/order'
import { formatMoney } from './scm'

/** 单据追溯：订单的出入库、应收应付、收付款、发运、来料检验、生产计划、项目（按权限返回） */
const props = defineProps({
  kind: { type: String, required: true },
  orderId: { type: Number, required: true }
})
const emit = defineEmits(['voucher'])

const CONF = {
  tradeDocs: {
    title: '出入库 / 退货',
    columns: [
      { title: '单据号', dataIndex: 'docNo', width: 160 },
      { title: '类型', dataIndex: 'kind', width: 100, dict: 'scm_trade_kind' },
      { title: '业务日期', dataIndex: 'bizDate', width: 110 },
      { title: '金额', dataIndex: 'totalAmount', width: 120, money: true },
      { title: '仓库单据', dataIndex: 'wmsDocNo', width: 160 }
    ]
  },
  bills: {
    title: '应收 / 应付',
    columns: [
      { title: '单号', dataIndex: 'billNo', width: 150 },
      { title: '来源', dataIndex: 'sourceNo', width: 150 },
      { title: '日期', dataIndex: 'billDate', width: 110 },
      { title: '金额', dataIndex: 'amount', width: 120, money: true },
      { title: '已核销', dataIndex: 'settledAmount', width: 120, money: true },
      { title: '状态', dataIndex: 'status', width: 90, dict: 'fin_bill_status' },
      { title: '凭证', dataIndex: 'voucherNo', width: 100, voucher: true }
    ]
  },
  settlements: {
    title: '收款 / 付款',
    columns: [
      { title: '单号', dataIndex: 'settleNo', width: 160 },
      { title: '日期', dataIndex: 'settleDate', width: 110 },
      { title: '核销本订单', dataIndex: 'amount', width: 130, money: true },
      { title: '状态', dataIndex: 'status', width: 90, dict: 'sys_approve_status' },
      { title: '凭证', dataIndex: 'voucherNo', width: 100, voucher: true }
    ]
  },
  shipments: {
    title: '物流发运',
    columns: [
      { title: '发运单', dataIndex: 'shipNo', width: 160 },
      { title: '出库单', dataIndex: 'tradeDocNo', width: 160 },
      { title: '承运商', dataIndex: 'carrier', width: 100, dict: 'scm_carrier' },
      { title: '运单号', dataIndex: 'trackingNo', width: 150 },
      { title: '状态', dataIndex: 'status', width: 90, dict: 'scm_shipment_status' }
    ]
  },
  inspections: {
    title: '来料检验',
    columns: [
      { title: '检验单', dataIndex: 'inspNo', width: 150 },
      { title: '入库单', dataIndex: 'sourceNo', width: 150 },
      { title: '物料', dataIndex: 'materialName', width: 160 },
      { title: '送检', dataIndex: 'inspectQty', width: 90 },
      { title: '结论', dataIndex: 'result', width: 100, dict: 'mes_qc_result' },
      { title: '日期', dataIndex: 'inspectDate', width: 110 }
    ]
  },
  plans: {
    title: '生产计划',
    columns: [
      { title: '计划号', dataIndex: 'planNo', width: 150 },
      { title: '产品', dataIndex: 'materialName', width: 160 },
      { title: '数量', dataIndex: 'qty', width: 90 },
      { title: '需求日期', dataIndex: 'dueDate', width: 110 },
      { title: '工单数', dataIndex: 'woCount', width: 80 },
      { title: '已完工', dataIndex: 'finishedQty', width: 90 },
      { title: '状态', dataIndex: 'status', width: 90, dict: 'mes_plan_status' }
    ]
  },
  projects: {
    title: '项目',
    columns: [
      { title: '项目编号', dataIndex: 'code', width: 140 },
      { title: '项目名称', dataIndex: 'name', width: 200 },
      { title: '进度', dataIndex: 'progress', width: 90 },
      { title: '状态', dataIndex: 'status', width: 100, dict: 'pm_project_status' }
    ]
  }
}

const data = ref({})
const loading = ref(false)
const sections = computed(() =>
  Object.entries(data.value)
    .filter(([key, rows]) => CONF[key] && rows?.length)
    .map(([key, rows]) => ({ key, rows, ...CONF[key] }))
)

async function load() {
  loading.value = true
  try {
    data.value = await orderTrace(props.kind, props.orderId)
  } finally {
    loading.value = false
  }
}
watch(() => [props.kind, props.orderId], load, { immediate: true })
</script>

<style scoped>
.trace-section {
  margin-bottom: 16px;
}
.trace-title {
  font-weight: 500;
  margin-bottom: 6px;
}
</style>
