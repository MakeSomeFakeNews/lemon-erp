<template>
  <div>
    <div class="head">
      <span class="title">{{ kind === 'ar' ? $t('fin.billSettleTable.titleAr') : $t('fin.billSettleTable.titleAp') }}</span>
      <a-space>
        <a-button size="small" :disabled="!bills.length || !(target > 0)" @click="allocate">{{ $t('fin.billSettleTable.allocate') }}</a-button>
        <span>
          {{ $t('fin.billSettleTable.total') }} <b :class="['total', { bad: target > 0 && total !== target }]">¥{{ formatMoney(total) }}</b>
          <template v-if="target > 0"> / {{ $t('fin.billSettleTable.shouldBe', { amount: formatMoney(target) }) }}</template>
        </span>
      </a-space>
    </div>
    <a-table
      row-key="id"
      size="small"
      :columns="columns"
      :data-source="bills"
      :loading="loading"
      :pagination="false"
      :scroll="{ y: 320 }"
      :locale="{ emptyText: partnerId ? $t('fin.billSettleTable.noBills') : $t('fin.billSettleTable.selectPartner') }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'source'">
          <DictTag type="fin_bill_source" :value="record.sourceType" />
          <span class="sub-text"> {{ record.sourceNo || '' }}</span>
        </template>
        <template v-else-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template>
        <template v-else-if="column.key === 'remaining'">{{ formatMoney(remaining(record)) }}</template>
        <template v-else-if="column.key === 'settle'">
          <a-input-number
            v-model:value="record.settle"
            :precision="2"
            :min="Math.min(0, remaining(record))"
            :max="Math.max(0, remaining(record))"
            class="full"
            @change="emitItems"
          />
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { unsettledBills } from '@/api/fin'
import { useDict } from '@/composables/useDict'
import { formatMoney } from './fin'
import { isForeign } from './currency'
import { t } from '@/locales'

/**
 * 选择要核销的应收 / 应付单（票据收票、背书、开票使用）：只列人民币单据，可按目标金额按到期日先后自动分配。
 * v-model 为 [{ billId, amount }]
 */
const props = defineProps({
  kind: { type: String, required: true },
  partnerId: Number,
  /** 应核销的合计（票面金额） */
  target: Number,
  modelValue: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue'])
useDict('fin_bill_source')

const columns = [
  { title: t('fin.billSettleTable.billNo'), dataIndex: 'billNo', width: 150 },
  { title: t('fin.billSettleTable.source'), key: 'source', width: 190 },
  { title: t('fin.billSettleTable.dueDate'), dataIndex: 'dueDate', width: 110 },
  { title: t('common.field.amount'), key: 'amount', width: 120, align: 'right' },
  { title: t('fin.billSettleTable.remaining'), key: 'remaining', width: 120, align: 'right' },
  { title: t('fin.billSettleTable.settle'), key: 'settle', width: 150 }
]
const bills = ref([])
const loading = ref(false)
const remaining = (b) => Math.round((Number(b.amount) - Number(b.settledAmount)) * 100) / 100
const total = computed(() => Math.round(bills.value.reduce((s, b) => s + (Number(b.settle) || 0), 0) * 100) / 100)

function emitItems() {
  emit('update:modelValue', bills.value.filter((b) => Number(b.settle)).map((b) => ({ billId: b.id, amount: Number(b.settle) })))
}

watch(
  () => props.partnerId,
  async (id) => {
    bills.value = []
    emitItems()
    if (!id) return
    loading.value = true
    try {
      bills.value = (await unsettledBills(props.kind, id))
        .filter((b) => !isForeign(b.currency))
        .map((b) => ({ ...b, settle: undefined }))
    } finally {
      loading.value = false
    }
  },
  { immediate: true }
)

/** 先用红字单据冲抵，再按到期日先后分配，直到达到目标金额 */
function allocate() {
  let left = props.target
  bills.value.forEach((b) => (b.settle = undefined))
  bills.value.filter((b) => remaining(b) < 0).forEach((b) => {
    b.settle = remaining(b)
    left -= b.settle
  })
  const positive = bills.value.filter((b) => remaining(b) > 0).sort((a, b) => String(a.dueDate).localeCompare(String(b.dueDate)))
  for (const b of positive) {
    if (left <= 0) break
    b.settle = Math.round(Math.min(left, remaining(b)) * 100) / 100
    left = Math.round((left - b.settle) * 100) / 100
  }
  emitItems()
}

defineExpose({ total })
</script>

<style scoped>
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.title {
  font-weight: 600;
}
.total {
  color: var(--app-primary);
}
.total.bad {
  color: var(--status-critical);
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
