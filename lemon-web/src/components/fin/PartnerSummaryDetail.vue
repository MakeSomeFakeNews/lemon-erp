<template>
  <!-- 往来汇总展开的单位明细：未核销的应收应付单（含红字）、未冲抵的预收预付、最近的收付款（按权限显示） -->
  <a-tabs v-model:active-key="tab" size="small" class="detail-tabs">
    <a-tab-pane key="bills" :tab="tx.bills">
      <a-table row-key="id" size="small" :columns="billColumns" :data-source="bills" :loading="loading.bills" :pagination="false" :scroll="{ x: 860, y: 320 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'source'">
            <DictTag type="fin_bill_source" :value="record.sourceType" />
            <SourceDocLink class="sub-text" :type="record.sourceType" :id="record.sourceId" :no="record.sourceNo" :bill-kind="record.kind" :project-id="record.projectId" />
          </template>
          <template v-else-if="column.key === 'amount'">
            <span :class="{ neg: Number(record.amount) < 0 }">{{ formatMoney(record.amount) }}</span>
            <div v-if="isForeign(record.currency)" class="sub-text">{{ record.currency }} {{ formatMoney(record.amountFc) }}</div>
            <div v-if="Number(record.discountAmount)" class="sub-text">
              {{ $t('fin.billPage.discountN', { amount: formatMoney(record.discountAmount) }) }} · {{ $t('fin.partnerSummary.netN', { amount: formatMoney(record.netAmount) }) }}
            </div>
          </template>
          <template v-else-if="column.key === 'remaining'">
            <b :class="{ neg: remaining(record) < 0 }">{{ formatMoney(remaining(record)) }}</b>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="fin_bill_status" :value="record.status" /></template>
        </template>
      </a-table>
    </a-tab-pane>
    <a-tab-pane v-if="canAdvance" key="advances" :tab="tx.advances">
      <a-table row-key="id" size="small" :columns="advanceColumns" :data-source="advances" :loading="loading.advances" :pagination="false" :scroll="{ x: 700, y: 320 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            {{ isForeign(record.currency) ? `${record.currency} ${formatMoney(record.amountFc)}` : formatMoney(record.amount) }}
          </template>
          <template v-else-if="column.key === 'remaining'">
            <b>{{ isForeign(record.currency) ? `${record.currency} ` : '' }}{{ formatMoney(Number(record.amountFc || 0) - Number(record.usedFc || 0)) }}</b>
          </template>
        </template>
      </a-table>
    </a-tab-pane>
    <a-tab-pane v-if="canSettle" key="settlements" :tab="tx.settlements">
      <a-table row-key="id" size="small" :columns="settleColumns" :data-source="settlements" :loading="loading.settlements" :pagination="false" :scroll="{ x: 700, y: 320 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            {{ formatMoney(record.amount) }}
            <div v-if="Number(record.discountAmount)" class="sub-text">{{ $t('fin.settlementPage.discountN', { amount: formatMoney(record.discountAmount) }) }}</div>
            <div v-if="Number(record.advanceOffset)" class="sub-text">{{ settleConf.advanceOffsetLabel }} {{ formatMoney(record.advanceOffset) }}</div>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="sys_approve_status" :value="record.status" /></template>
        </template>
      </a-table>
      <div class="sub-text more">{{ $t('fin.partnerSummary.recentTip', { n: SETTLE_LIMIT }) }}</div>
    </a-tab-pane>
  </a-tabs>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import DictTag from '@/components/DictTag.vue'
import SourceDocLink from '@/components/common/SourceDocLink.vue'
import { pageAdvance, pageSettlement, unsettledBills } from '@/api/fin'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { round2 } from '@/utils/number'
import { formatMoney } from './fin'
import { isForeign } from './currency'
import { settlementConf } from './settlementConf'
import { t } from '@/locales'

/** kind = ar | ap，partnerId 为往来单位 */
const props = defineProps({
  kind: { type: String, required: true },
  partnerId: { type: Number, required: true },
  conf: { type: Object, required: true }
})
useDict('fin_bill_source', 'fin_bill_status', 'sys_approve_status')
const SETTLE_LIMIT = 20

const ar = props.kind === 'ar'
const settleKind = ar ? 'receipt' : 'payment'
const settleConf = settlementConf(settleKind)
const tx = {
  bills: t(ar ? 'fin.partnerSummary.ar.billTab' : 'fin.partnerSummary.ap.billTab'),
  advances: t(ar ? 'fin.partnerSummary.ar.advanceTab' : 'fin.partnerSummary.ap.advanceTab'),
  settlements: t(ar ? 'fin.partnerSummary.ar.settleTab' : 'fin.partnerSummary.ap.settleTab')
}
const userStore = useUserStore()
const canAdvance = computed(() => userStore.hasPerm('fin:advance:list'))
const canSettle = computed(() => userStore.hasPerm(`fin:${settleKind}:list`))

const billColumns = [
  { title: t('fin.billPage.billNo'), dataIndex: 'billNo', width: 150 },
  { title: t('fin.billPage.source'), key: 'source', width: 200 },
  { title: t('fin.billPage.billDate'), dataIndex: 'billDate', width: 110 },
  { title: t('fin.billPage.dueDate'), dataIndex: 'dueDate', width: 110 },
  { title: t('common.field.amount'), key: 'amount', width: 130, align: 'right' },
  { title: t('fin.billPage.unsettledBalance'), key: 'remaining', width: 130, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 100 }
]
const advanceColumns = [
  { title: t('fin.settlementForm.advanceNo'), dataIndex: 'advanceNo', width: 150 },
  { title: t('common.field.date'), dataIndex: 'advanceDate', width: 110 },
  { title: t('common.field.amount'), key: 'amount', width: 140, align: 'right' },
  { title: t('fin.settlementForm.advanceRemaining'), key: 'remaining', width: 140, align: 'right' },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true }
]
const settleColumns = [
  { title: t('fin.settlementPage.no'), dataIndex: 'settleNo', width: 150 },
  { title: t('common.field.date'), dataIndex: 'settleDate', width: 110 },
  { title: settleConf.paidLabel, key: 'amount', width: 150, align: 'right' },
  { title: t('fin.settlementPage.account'), dataIndex: 'accountName', width: 140, ellipsis: true },
  { title: t('common.field.status'), key: 'status', width: 100 }
]

const tab = ref('bills')
const bills = ref([])
const advances = ref([])
const settlements = ref([])
const loading = reactive({ bills: false, advances: false, settlements: false })
const remaining = (b) => round2(Number(b.amount) - Number(b.settledAmount))

async function fetch(key, loader) {
  loading[key] = true
  try {
    return await loader()
  } finally {
    loading[key] = false
  }
}

onMounted(async () => {
  const tasks = [fetch('bills', () => unsettledBills(props.kind, props.partnerId)).then((r) => (bills.value = r))]
  if (canAdvance.value) {
    tasks.push(
      fetch('advances', () => pageAdvance({ kind: settleKind.toUpperCase(), partnerId: props.partnerId, onlyOpen: true, size: 100 })).then(
        (r) => (advances.value = r.records)
      )
    )
  }
  if (canSettle.value) {
    tasks.push(fetch('settlements', () => pageSettlement(settleKind, { partnerId: props.partnerId, size: SETTLE_LIMIT })).then((r) => (settlements.value = r.records)))
  }
  await Promise.allSettled(tasks)
})
</script>

<style scoped>
.detail-tabs {
  padding: 0 8px 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.more {
  margin-top: 6px;
}
.neg {
  color: #cf1322;
}
</style>
