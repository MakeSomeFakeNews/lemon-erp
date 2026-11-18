<template>
  <!-- 余额调节表 -->
  <a-modal v-model:open="open" :title="$t('fin.bank.reconcileTitle')" width="min(900px, 100vw)" :footer="null">
    <div class="filters">
      <span>{{ $t('fin.bank.asOf') }}</span>
      <a-date-picker v-model:value="asOf" value-format="YYYY-MM-DD" :allow-clear="false" />
      <span>{{ $t('fin.bank.bankBalance') }}</span>
      <a-input-number v-model:value="bankBalance" :precision="2" :placeholder="$t('fin.bank.bankBalancePh')" class="balance" />
      <a-button type="primary" :loading="loading" @click="load">{{ $t('fin.bank.calculate') }}</a-button>
    </div>
    <a-spin :spinning="loading">
      <template v-if="rec">
        <div class="report-title">{{ $t('fin.bank.reconcileTitle') }}</div>
        <div class="report-sub">
          {{ $t('fin.bank.reportSub', { account: accountName, date: rec.asOf }) }}
          <a-tag :color="rec.balanced ? 'green' : 'red'" :bordered="false" class="check">
            {{ rec.balanced ? $t('fin.bank.balanced') : $t('fin.bank.unbalanced') }}
          </a-tag>
        </div>
        <table class="rec-table">
          <thead>
            <tr>
              <th>{{ $t('fin.bank.book') }}</th>
              <th class="num">{{ $t('common.field.amount') }}</th>
              <th>{{ $t('fin.bank.statementTitle') }}</th>
              <th class="num">{{ $t('common.field.amount') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>{{ $t('fin.bank.bookBalance') }}</td>
              <td class="num">{{ formatMoney(rec.bookBalance) }}</td>
              <td>{{ $t('fin.bank.bankBalance') }}</td>
              <td class="num">{{ formatMoney(rec.bankBalance) }}</td>
            </tr>
            <tr>
              <td>{{ $t('fin.bank.bankIn') }}</td>
              <td class="num">{{ formatMoney(rec.bankIn) }}</td>
              <td>{{ $t('fin.bank.bookIn') }}</td>
              <td class="num">{{ formatMoney(rec.bookIn) }}</td>
            </tr>
            <tr>
              <td>{{ $t('fin.bank.bankOut') }}</td>
              <td class="num">{{ formatMoney(rec.bankOut) }}</td>
              <td>{{ $t('fin.bank.bookOut') }}</td>
              <td class="num">{{ formatMoney(rec.bookOut) }}</td>
            </tr>
          </tbody>
          <tfoot>
            <tr>
              <td>{{ $t('fin.bank.adjusted') }}</td>
              <td :class="['num', { neg: !rec.balanced }]">{{ formatMoney(rec.adjustedBook) }}</td>
              <td>{{ $t('fin.bank.adjusted') }}</td>
              <td :class="['num', { neg: !rec.balanced }]">{{ formatMoney(rec.adjustedBank) }}</td>
            </tr>
          </tfoot>
        </table>
        <a-row :gutter="12" class="rec-detail">
          <a-col :xs="24" :md="12">
            <div class="section-title">{{ $t('fin.bank.bankOnly', { n: rec.bankOnly.length }) }}</div>
            <a-table :row-key="(r) => (r.opening ? 'o' : 's') + r.id" size="small" :columns="bankOnlyColumns" :data-source="rec.bankOnly" :pagination="false" :scroll="{ y: 240 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'amount'">
                  <span :class="['num', { neg: Number(record.amount) < 0 }]">{{ formatMoney(record.amount) }}</span>
                </template>
                <template v-else-if="column.key === 'summary'">
                  <a-tag v-if="record.opening" :bordered="false" color="orange">{{ $t('fin.bank.opening') }}</a-tag>{{ record.summary }}
                </template>
              </template>
            </a-table>
          </a-col>
          <a-col :xs="24" :md="12">
            <div class="section-title">{{ $t('fin.bank.bookOnly', { n: rec.bookOnly.length }) }}</div>
            <a-table :row-key="(r) => (r.openingItemId ? 'o' + r.openingItemId : 'e' + r.entryId)" size="small" :columns="bookOnlyColumns" :data-source="rec.bookOnly" :pagination="false" :scroll="{ y: 240 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'voucher'">
                  <a-tag v-if="record.openingItemId" :bordered="false" color="orange">{{ $t('fin.bank.opening') }}</a-tag>
                  <template v-else>{{ record.voucherNo }}</template>
                  <div class="sub-text">{{ record.voucherDate }}</div>
                </template>
                <template v-else-if="column.key === 'amount'">
                  <span :class="['num', { neg: Number(record.amount) < 0 }]">{{ formatMoney(record.amount) }}</span>
                </template>
              </template>
            </a-table>
          </a-col>
        </a-row>
      </template>
    </a-spin>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { bankReconcile } from '@/api/fin'
import { formatMoney } from '@/components/fin/fin'
import { today } from '@/components/wms/wms'

/** 银行存款余额调节表：未勾对的流水、凭证分录和期初未达账 */
const props = defineProps({
  accountId: Number,
  accountName: String
})
const { t } = useI18n()
const open = ref(false)
const loading = ref(false)
const asOf = ref(today())
const bankBalance = ref()
const rec = ref()
const bankOnlyColumns = [
  { title: t('common.field.date'), dataIndex: 'txnDate', width: 100 },
  { title: t('common.field.amount'), key: 'amount', width: 110, align: 'right' },
  { title: t('fin.bank.summary'), key: 'summary', ellipsis: true }
]
const bookOnlyColumns = [
  { title: t('fin.bank.voucher'), key: 'voucher', width: 110 },
  { title: t('common.field.amount'), key: 'amount', width: 110, align: 'right' },
  { title: t('fin.bank.summary'), dataIndex: 'summary', ellipsis: true }
]

async function load() {
  loading.value = true
  try {
    rec.value = await bankReconcile({ accountId: props.accountId, asOf: asOf.value, bankBalance: bankBalance.value ?? undefined })
  } finally {
    loading.value = false
  }
}

/** 打开并按截止日期计算 */
function show(date) {
  asOf.value = date || today()
  rec.value = undefined
  open.value = true
  load()
}
defineExpose({ open: show })
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.balance {
  width: 200px;
}
.report-title {
  text-align: center;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 2px;
}
.report-sub {
  text-align: center;
  color: var(--app-text-3);
  margin: 6px 0 12px;
}
.check {
  margin-left: 8px;
}
.rec-table {
  width: 100%;
  border-collapse: collapse;
}
.rec-table th,
.rec-table td {
  border: 1px solid var(--app-border, #d9d9d9);
  padding: 8px 10px;
}
.rec-table th {
  background: var(--app-bg-2, #fafafa);
  font-weight: 500;
  text-align: left;
}
.rec-table th.num {
  text-align: right;
}
.rec-table tfoot td {
  font-weight: 600;
}
.rec-detail {
  margin-top: 16px;
}
.section-title {
  font-weight: 500;
  margin: 8px 0;
}
.num {
  font-variant-numeric: tabular-nums;
  text-align: right;
}
.neg {
  color: #cf1322;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
