<template>
  <a-drawer :open="open" :title="row?.name" width="min(860px, 100vw)" destroy-on-close @close="emit('update:open', false)">
    <template v-if="row">
      <a-descriptions :column="1" size="small" bordered class="block">
        <a-descriptions-item v-if="row.lineNo" label="行次">{{ row.lineNo }}</a-descriptions-item>
        <a-descriptions-item label="取数">{{ row.formula || '-' }}</a-descriptions-item>
        <a-descriptions-item v-for="(c, i) in columns" :key="c" :label="c">
          <span class="num bold">{{ formatMoney(row.values?.[i]) }}</span>
        </a-descriptions-item>
      </a-descriptions>

      <template v-if="row.accounts">
        <div class="section-title">构成科目 <span class="sub-text">点击科目查看明细账（{{ rangeText }}）</span></div>
        <a-table
          row-key="accountId"
          size="small"
          bordered
          :columns="accountColumns"
          :data-source="row.accounts"
          :pagination="false"
          :locale="{ emptyText: '没有余额或发生额的科目' }"
          :row-class-name="(r) => (r.accountId === ledgerAccount?.accountId ? 'selected-row' : '')"
          class="block"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'account'">
              <a @click="openLedger(record)">{{ record.code }}</a> {{ record.name }}
            </template>
            <template v-else-if="column.index != null">
              <span :class="['num', { neg: Number(record.values[column.index]) < 0 }]">{{ formatMoney(record.values[column.index]) }}</span>
            </template>
          </template>
        </a-table>
      </template>

      <template v-if="ledgerAccount">
        <div class="section-title">明细账：{{ ledgerAccount.code }} {{ ledgerAccount.name }}</div>
        <a-table
          :row-key="(r, i) => i"
          size="small"
          bordered
          :columns="ledgerColumns"
          :data-source="ledgerRows"
          :loading="ledgerLoading"
          :pagination="false"
          :row-class-name="(r) => (r.type !== 'ENTRY' ? 'total-row' : '')"
          :scroll="{ x: 760 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'voucherNo'">
              <a v-if="record.voucherId" @click="openVoucher(record.voucherId)">{{ record.voucherNo }}</a>
            </template>
            <template v-else-if="['debit', 'credit'].includes(column.key)">
              <span class="num">{{ moneyOrBlank(record[column.key]) }}</span>
            </template>
            <template v-else-if="column.key === 'balance'">
              <span class="num">{{ formatMoney(record.balance) }}</span>
            </template>
          </template>
        </a-table>
      </template>
    </template>
    <VoucherDetail v-model:open="voucherOpen" :voucher-id="voucherId" />
  </a-drawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import VoucherDetail from '@/components/fin/VoucherDetail.vue'
import { ledger } from '@/api/fin'
import { formatMoney, moneyOrBlank, periodText } from '@/components/fin/fin'

/**
 * 报表数字追溯：报表行的取数公式、构成科目（各列金额），点击科目查看明细账，再点凭证号查看凭证
 * - row：报表行（lineNo、name、formula、values、accounts）
 * - columns：金额列名称
 * - begin / end：明细账的期间范围
 */
const props = defineProps({
  open: Boolean,
  row: Object,
  columns: { type: Array, default: () => [] },
  begin: Number,
  end: Number
})
const emit = defineEmits(['update:open'])

const rangeText = computed(() => (props.begin === props.end ? periodText(props.end) : `${periodText(props.begin)} 至 ${periodText(props.end)}`))
const accountColumns = computed(() => [
  { title: '科目', key: 'account' },
  ...props.columns.map((c, i) => ({ title: c, key: `v${i}`, index: i, width: 150, align: 'right' }))
])
const ledgerColumns = [
  { title: '日期', dataIndex: 'entryDate', width: 110 },
  { title: '凭证号', key: 'voucherNo', width: 140 },
  { title: '摘要', dataIndex: 'summary', ellipsis: true },
  { title: '借方', key: 'debit', width: 120, align: 'right' },
  { title: '贷方', key: 'credit', width: 120, align: 'right' },
  { title: '方向', dataIndex: 'side', width: 60, align: 'center' },
  { title: '余额', key: 'balance', width: 130, align: 'right' }
]

const ledgerAccount = ref()
const ledgerRows = ref([])
const ledgerLoading = ref(false)
async function openLedger(account) {
  ledgerAccount.value = account
  ledgerLoading.value = true
  try {
    ledgerRows.value = await ledger({ accountId: account.accountId, begin: props.begin, end: props.end })
  } finally {
    ledgerLoading.value = false
  }
}
watch(
  () => props.row,
  () => {
    ledgerAccount.value = undefined
    ledgerRows.value = []
  }
)

const voucherOpen = ref(false)
const voucherId = ref()
function openVoucher(id) {
  voucherId.value = id
  voucherOpen.value = true
}
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.section-title {
  font-weight: 500;
  margin-bottom: 8px;
}
.num {
  font-variant-numeric: tabular-nums;
}
.bold {
  font-weight: 600;
}
.neg {
  color: var(--status-critical);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
  font-weight: 400;
  margin-left: 6px;
}
:deep(.total-row) td {
  font-weight: 500;
  background: var(--app-bg-2, #fafafa);
}
:deep(.selected-row) td {
  background: var(--app-primary-bg, #e6f4ff);
}
</style>
