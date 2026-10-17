<template>
  <a-spin :spinning="loading">
    <a-empty v-if="!loading && !sections.length" :description="$t('scm.orderTrace.empty')" />
    <div v-for="s in sections" :key="s.key" class="trace-section">
      <div class="trace-title">{{ s.title }} <span class="sub-text">{{ $t('scm.orderTrace.count', { n: s.rows.length }) }}</span></div>
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
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import { orderTrace } from '@/api/scm/order'
import { formatMoney } from './scm'

/** 单据追溯：订单的出入库、应收应付、收付款、发运、来料检验、生产计划、项目（按权限返回） */
const props = defineProps({
  kind: { type: String, required: true },
  orderId: { type: Number, required: true }
})
const emit = defineEmits(['voucher'])
const { t } = useI18n()

const CONF = {
  tradeDocs: {
    title: t('scm.orderTrace.tradeDocs'),
    columns: [
      { title: t('scm.orderTrace.docNo'), dataIndex: 'docNo', width: 160 },
      { title: t('scm.orderTrace.type'), dataIndex: 'kind', width: 100, dict: 'scm_trade_kind' },
      { title: t('scm.orderTrace.bizDate'), dataIndex: 'bizDate', width: 110 },
      { title: t('scm.orderTrace.amount'), dataIndex: 'totalAmount', width: 120, money: true },
      { title: t('scm.orderTrace.wmsDoc'), dataIndex: 'wmsDocNo', width: 160 }
    ]
  },
  bills: {
    title: t('scm.orderTrace.bills'),
    columns: [
      { title: t('scm.orderTrace.no'), dataIndex: 'billNo', width: 150 },
      { title: t('scm.orderTrace.source'), dataIndex: 'sourceNo', width: 150 },
      { title: t('scm.orderTrace.date'), dataIndex: 'billDate', width: 110 },
      { title: t('scm.orderTrace.amount'), dataIndex: 'amount', width: 120, money: true },
      { title: t('scm.orderTrace.settled'), dataIndex: 'settledAmount', width: 120, money: true },
      { title: t('scm.orderTrace.status'), dataIndex: 'status', width: 90, dict: 'fin_bill_status' },
      { title: t('scm.orderTrace.voucher'), dataIndex: 'voucherNo', width: 100, voucher: true }
    ]
  },
  settlements: {
    title: t('scm.orderTrace.settlements'),
    columns: [
      { title: t('scm.orderTrace.no'), dataIndex: 'settleNo', width: 160 },
      { title: t('scm.orderTrace.date'), dataIndex: 'settleDate', width: 110 },
      { title: t('scm.orderTrace.settledOrder'), dataIndex: 'amount', width: 170, money: true },
      { title: t('scm.orderTrace.status'), dataIndex: 'status', width: 90, dict: 'sys_approve_status' },
      { title: t('scm.orderTrace.voucher'), dataIndex: 'voucherNo', width: 100, voucher: true }
    ]
  },
  shipments: {
    title: t('scm.orderTrace.shipments'),
    columns: [
      { title: t('scm.orderTrace.shipNo'), dataIndex: 'shipNo', width: 160 },
      { title: t('scm.orderTrace.outDoc'), dataIndex: 'tradeDocNo', width: 160 },
      { title: t('scm.orderTrace.carrier'), dataIndex: 'carrier', width: 100, dict: 'scm_carrier' },
      { title: t('scm.orderTrace.trackingNo'), dataIndex: 'trackingNo', width: 150 },
      { title: t('scm.orderTrace.status'), dataIndex: 'status', width: 90, dict: 'scm_shipment_status' }
    ]
  },
  inspections: {
    title: t('scm.orderTrace.inspections'),
    columns: [
      { title: t('scm.orderTrace.inspNo'), dataIndex: 'inspNo', width: 150 },
      { title: t('scm.orderTrace.inDoc'), dataIndex: 'sourceNo', width: 150 },
      { title: t('scm.orderTrace.material'), dataIndex: 'materialName', width: 160 },
      { title: t('scm.orderTrace.inspectQty'), dataIndex: 'inspectQty', width: 90 },
      { title: t('scm.orderTrace.result'), dataIndex: 'result', width: 100, dict: 'mes_qc_result' },
      { title: t('scm.orderTrace.date'), dataIndex: 'inspectDate', width: 110 }
    ]
  },
  plans: {
    title: t('scm.orderTrace.plans'),
    columns: [
      { title: t('scm.orderTrace.planNo'), dataIndex: 'planNo', width: 150 },
      { title: t('scm.orderTrace.product'), dataIndex: 'materialName', width: 160 },
      { title: t('scm.orderTrace.qty'), dataIndex: 'qty', width: 90 },
      { title: t('scm.orderTrace.dueDate'), dataIndex: 'dueDate', width: 110 },
      { title: t('scm.orderTrace.woCount'), dataIndex: 'woCount', width: 80 },
      { title: t('scm.orderTrace.finished'), dataIndex: 'finishedQty', width: 90 },
      { title: t('scm.orderTrace.status'), dataIndex: 'status', width: 90, dict: 'mes_plan_status' }
    ]
  },
  projects: {
    title: t('scm.orderTrace.projects'),
    columns: [
      { title: t('scm.orderTrace.projectCode'), dataIndex: 'code', width: 140 },
      { title: t('scm.orderTrace.projectName'), dataIndex: 'name', width: 200 },
      { title: t('scm.orderTrace.progress'), dataIndex: 'progress', width: 90 },
      { title: t('scm.orderTrace.status'), dataIndex: 'status', width: 100, dict: 'pm_project_status' }
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
