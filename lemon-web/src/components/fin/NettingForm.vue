<template>
  <!-- 新建应收应付对冲（issue #95）：选择客户（须有关联的供应商），勾选两边的单据并填写核销金额，应收合计 = 应付合计 -->
  <a-modal v-model:open="visible" :title="$t('fin.netting.createTitle')" width="min(1000px, 100vw)" :confirm-loading="saving" destroy-on-close @ok="submit">
    <a-alert type="info" show-icon class="block" :message="$t('fin.netting.formTip')" />
    <a-row :gutter="16">
      <a-col :xs="24" :sm="10">
        <a-form-item :label="$t('fin.netting.customer')" required>
          <PartnerSelect v-model:value="form.customerId" type="customer" @update:value="loadCandidates" />
        </a-form-item>
      </a-col>
      <a-col :xs="24" :sm="6">
        <a-form-item :label="$t('fin.netting.date')" required>
          <a-date-picker v-model:value="form.nettingDate" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
      </a-col>
      <a-col :xs="24" :sm="8">
        <a-form-item :label="$t('common.field.remark')">
          <a-input v-model:value="form.remark" :maxlength="255" />
        </a-form-item>
      </a-col>
    </a-row>

    <a-spin :spinning="loading">
      <template v-if="cand">
        <div class="bar">
          <span>{{ $t('fin.netting.linkedSupplier') }}：<b>{{ cand.supplierCode }} {{ cand.supplierName }}</b></span>
          <a-button size="small" @click="autoAllocate">{{ $t('fin.netting.auto') }}</a-button>
        </div>
        <a-row :gutter="16">
          <a-col v-for="side in sides" :key="side.key" :xs="24" :lg="12">
            <div class="side-title">{{ side.title }}</div>
            <a-table row-key="id" size="small" :columns="columns" :data-source="side.rows" :pagination="false" :scroll="{ y: 300 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'bill'">
                  <div>{{ record.billNo }}</div>
                  <div class="sub-text">{{ record.billDate }} <DictTag type="fin_bill_source" :value="record.sourceType" /></div>
                </template>
                <template v-else-if="column.key === 'remaining'">
                  <span :class="{ neg: remaining(record) < 0 }">{{ formatMoney(remaining(record)) }}</span>
                </template>
                <template v-else-if="column.key === 'amount'">
                  <a-input-number
                    v-model:value="record.nettingAmount"
                    :min="Math.min(0, remaining(record))"
                    :max="Math.max(0, remaining(record))"
                    :precision="2"
                    size="small"
                    class="full"
                  />
                </template>
              </template>
            </a-table>
            <div class="total">{{ $t('fin.netting.sideTotal') }} <b>¥{{ formatMoney(side.total) }}</b></div>
          </a-col>
        </a-row>
        <div class="summary">
          <span>{{ $t('fin.netting.amount') }} <b>¥{{ formatMoney(Math.min(arTotal, apTotal)) }}</b></span>
          <span v-if="arTotal !== apTotal" class="neg">{{ $t('fin.netting.diff', { amount: formatMoney(round2(arTotal - apTotal)) }) }}</span>
        </div>
      </template>
    </a-spin>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { addNetting, nettingCandidates } from '@/api/fin'
import { round2 } from '@/utils/number'
import { today } from '@/components/wms/wms'
import { formatMoney } from './fin'
import { t } from '@/locales'

const emit = defineEmits(['saved'])

const visible = ref(false)
const saving = ref(false)
const loading = ref(false)
const form = reactive({ customerId: undefined, nettingDate: undefined, remark: '' })
const cand = ref(null)
// 行：未核销的单据，nettingAmount 为填写的核销金额（与余额同号）
const receivables = ref([])
const payables = ref([])

const columns = [
  { title: t('fin.netting.bill'), key: 'bill' },
  { title: t('fin.netting.remaining'), key: 'remaining', width: 120, align: 'right' },
  { title: t('fin.netting.nettingAmount'), key: 'amount', width: 140 }
]
const remaining = (b) => round2(Number(b.amount) - Number(b.settledAmount))
const total = (rows) => round2(rows.reduce((s, r) => s + (Number(r.nettingAmount) || 0), 0))
const arTotal = computed(() => total(receivables.value))
const apTotal = computed(() => total(payables.value))
const sides = computed(() => [
  { key: 'ar', title: t('fin.netting.receivables'), rows: receivables.value, total: arTotal.value },
  { key: 'ap', title: t('fin.netting.payables'), rows: payables.value, total: apTotal.value }
])

/** customerId：从往来汇总等处直接带入客户 */
function open(customerId) {
  Object.assign(form, { customerId, nettingDate: today(), remark: '' })
  cand.value = null
  receivables.value = []
  payables.value = []
  visible.value = true
  if (customerId) loadCandidates(customerId)
}

async function loadCandidates(customerId) {
  cand.value = null
  receivables.value = []
  payables.value = []
  if (!customerId) return
  loading.value = true
  try {
    const res = await nettingCandidates(customerId)
    cand.value = res
    receivables.value = res.receivables.map((b) => ({ ...b, nettingAmount: undefined }))
    payables.value = res.payables.map((b) => ({ ...b, nettingAmount: undefined }))
  } finally {
    loading.value = false
  }
}

/** 自动分配：红字单据全部冲抵，其余按日期先后核销，两边都核销到较小一方的余额合计 */
function autoAllocate() {
  const openTotal = (rows) => round2(rows.reduce((s, r) => s + remaining(r), 0))
  const target = Math.min(openTotal(receivables.value), openTotal(payables.value))
  if (target <= 0) {
    message.warning(t('fin.netting.nothingToNet'))
    return
  }
  allocate(receivables.value, target)
  allocate(payables.value, target)
}
function allocate(rows, target) {
  let left = target
  rows.forEach((r) => {
    r.nettingAmount = remaining(r) < 0 ? remaining(r) : undefined
    if (r.nettingAmount) left = round2(left - r.nettingAmount)
  })
  rows.forEach((r) => {
    const rem = remaining(r)
    if (rem > 0 && left > 0) {
      r.nettingAmount = Math.min(rem, left)
      left = round2(left - r.nettingAmount)
    }
  })
}

async function submit() {
  if (!form.customerId || !form.nettingDate) {
    message.warning(t('fin.netting.required'))
    return
  }
  const items = [...receivables.value, ...payables.value]
    .filter((r) => Number(r.nettingAmount))
    .map((r) => ({ billId: r.id, amount: Number(r.nettingAmount) }))
  if (!items.length || arTotal.value <= 0 || arTotal.value !== apTotal.value) {
    message.warning(t('fin.netting.notBalanced'))
    return
  }
  saving.value = true
  try {
    await addNetting({ customerId: form.customerId, nettingDate: form.nettingDate, remark: form.remark || null, items })
    message.success(t('common.msg.saveSuccess'))
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.side-title {
  margin-bottom: 6px;
  font-weight: 500;
}
.total {
  margin-top: 6px;
  text-align: right;
}
.summary {
  display: flex;
  gap: 24px;
  justify-content: flex-end;
  margin-top: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.neg {
  color: #cf1322;
}
</style>
