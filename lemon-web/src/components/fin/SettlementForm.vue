<template>
  <!-- 新增 / 修改收款、付款：选择往来单位后列出未核销单据，输入本次收付款总额自动核销，或逐笔填写核销金额、折让；可勾选预收预付一起冲抵 -->
  <SubPage :open="visible" :title="editId ? conf.editTitle : conf.newTitle" width="min(1080px, 100vw)" destroy-on-close @close="visible = false">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="conf.partnerLabel" name="partnerId" :rules="[{ required: true, message: $t('common.placeholder.selectName', { name: conf.partnerLabel }) }]">
            <PartnerSelect v-model:value="form.partnerId" :type="conf.partner" :label="partnerLabel" @update:value="loadBills" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="conf.dateLabel" name="settleDate" :rules="[{ required: true, message: $t('fin.settlementPage.dateRequired') }]">
            <a-date-picker v-model:value="form.settleDate" value-format="YYYY-MM-DD" class="full" @change="refreshRate" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('fin.settlementPage.method')" name="method">
            <a-select v-model:value="form.method" :options="methodOptions" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('fin.settlementPage.account')" name="accountId" :rules="[{ required: true, message: $t('fin.settlementPage.accountRequired') }]">
            <AccountSelect v-model:value="form.accountId" :roles="['CASH', 'BANK']" :placeholder="$t('fin.settlementPage.accountPlaceholder')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
        <a-col v-if="foreign" :xs="24" :sm="12">
          <a-form-item :label="$t('fin.settlementPage.rateLabel', { currency: settleCurrency })" required>
            <a-input-number v-model:value="exchangeRate" :min="0" :precision="6" class="full" :placeholder="$t('fin.settlementPage.ratePlaceholder')" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
    <a-alert v-if="foreign" type="info" show-icon class="block" :message="conf.foreignTip(settleCurrency)" />
    <a-alert v-else-if="currencies.length > 1" type="warning" show-icon class="block" :message="conf.multiCurrency(currencies.join(', '))" />

    <!-- 本次收付款总额：默认 = 勾选单据的未核销合计；自动核销按单据日期从早到晚分配（红字先冲抵） -->
    <div class="pay-bar">
      <span class="section-title">{{ conf.thisTotal }}</span>
      <a-input-number v-model:value="payTotal" :min="0" :precision="2" class="pay-input" :prefix="foreign ? currencySymbol(settleCurrency) : '¥'" @change="payTouched = true" />
      <a-tooltip :title="$t('fin.settlementForm.autoTip')">
        <a-button type="primary" ghost :disabled="!checked.length" @click="autoSettle">{{ $t('fin.settlementForm.auto') }}</a-button>
      </a-tooltip>
      <a-button :disabled="!checked.length" @click="fillAll">{{ $t('fin.settlementPage.fillAll') }}</a-button>
      <span class="summary">
        {{ $t('fin.settlementForm.settledTotal') }} <b class="total">{{ foreign ? settleCurrency + ' ' : '¥' }}{{ formatMoney(total) }}</b>
        <template v-if="advanceTotal"> · {{ conf.advanceOffsetLabel }} <b>{{ foreign ? settleCurrency + ' ' : '¥' }}{{ formatMoney(advanceTotal) }}</b> · {{ conf.paidLabel }} <b>{{ foreign ? settleCurrency + ' ' : '¥' }}{{ formatMoney(cash) }}</b></template>
        <template v-if="discountTotal"> · {{ $t('fin.settlementForm.discountTotal') }} <b>{{ foreign ? settleCurrency + ' ' : '¥' }}{{ formatMoney(discountTotal) }}</b></template>
        <template v-if="foreign"> · {{ conf.foreignTotal(settleCurrency, formatMoney(total)) }} <b>¥{{ formatMoney(baseTotal) }}</b></template>
      </span>
    </div>
    <a-alert v-if="cash < 0" type="error" show-icon class="block" :message="$t('fin.settlementForm.advanceTooMuch', { amount: formatMoney(-cash) })" />
    <a-alert v-else-if="diff < 0" type="error" show-icon class="block" :message="$t('fin.settlementForm.short', { amount: formatMoney(-diff) })" />
    <a-alert v-else-if="diff > 0 && advanceTotal" type="error" show-icon class="block" :message="$t('fin.settlementForm.overWithAdvance', { amount: formatMoney(diff) })" />
    <a-alert v-else-if="diff > 0" type="warning" show-icon class="block">
      <template #message>
        {{ foreign ? $t('fin.settlementForm.overForeign', { currency: settleCurrency, amount: formatMoney(diff) }) : $t('fin.settlementForm.over', { amount: formatMoney(diff) }) }}
        <a-checkbox v-model:checked="toAdvance" class="advance-check">{{ conf.toAdvance }}</a-checkbox>
      </template>
    </a-alert>
    <!-- 实收为 0：蓝字与红字单据（冲账、退货）或预收预付正好抵消，不发生资金收付 -->
    <a-alert v-else-if="items.length && cash === 0" type="info" show-icon class="block" :message="advanceTotal ? conf.zeroWithAdvance : conf.zeroTip" />

    <div class="bills-head">
      <span class="section-title">{{ conf.billsTitle }}</span>
      <span class="sub-text">{{ $t('fin.settlementForm.checkedN', { n: checked.length, amount: formatMoney(checkedRemaining) }) }}</span>
    </div>
    <a-table
      row-key="id"
      size="small"
      :columns="billColumns"
      :data-source="bills"
      :loading="billLoading"
      :pagination="false"
      :scroll="{ x: billScrollX, y: 420 }"
      :row-selection="{ selectedRowKeys: checked, onChange: onCheck, getCheckboxProps: (r) => ({ disabled: blocked(r) }) }"
      :locale="{ emptyText: form.partnerId ? $t('fin.settlementPage.noBills') : $t('fin.settlementPage.selectPartnerFirst', { name: conf.partnerLabel }) }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'source'">
          <DictTag type="fin_bill_source" :value="record.sourceType" />
          <SourceDocLink class="sub-text" :type="record.sourceType" :id="record.sourceId" :no="record.sourceNo" :bill-kind="record.kind" :project-id="record.projectId" />
        </template>
        <template v-else-if="column.key === 'amount'">
          <template v-if="isForeign(record.currency)">
            <div>{{ record.currency }} {{ formatMoney(record.amountFc) }}</div>
            <div class="sub-text">¥{{ formatMoney(record.amount) }} @{{ Number(record.exchangeRate) }}</div>
          </template>
          <template v-else>{{ formatMoney(record.amount) }}</template>
        </template>
        <template v-else-if="column.key === 'remaining'">
          {{ isForeign(record.currency) ? `${record.currency} ` : '' }}{{ formatMoney(remaining(record)) }}
        </template>
        <template v-else-if="column.key === 'settle'">
          <a-tooltip :title="blocked(record) ? $t('fin.settlementPage.blocked', { currency: settleCurrency }) : ''">
            <a-input-number
              v-model:value="record.settle"
              :precision="2"
              :min="Math.min(0, remaining(record))"
              :max="Math.max(0, remaining(record))"
              :disabled="!isChecked(record)"
              :prefix="isForeign(record.currency) ? currencySymbol(record.currency) : undefined"
              class="full"
            />
          </a-tooltip>
        </template>
        <template v-else-if="column.key === 'discount'">
          <a-tooltip :title="isForeign(record.currency) ? $t('fin.settlementForm.discountForeign') : ''">
            <a-input-number
              v-model:value="record.discount"
              :precision="2"
              :min="Math.min(0, remaining(record) - nz(record.settle))"
              :max="Math.max(0, remaining(record) - nz(record.settle))"
              :disabled="!isChecked(record)"
              :prefix="isForeign(record.currency) ? currencySymbol(record.currency) : undefined"
              :placeholder="$t('fin.settlementForm.discountPh')"
              class="full"
            />
          </a-tooltip>
        </template>
      </template>
    </a-table>
    <SettlementAdvances v-model:checked="advanceChecked" :rows="advances" :currency="settleCurrency" :need="advanceNeed" :conf="conf" />
    <template #footer>
      <div class="drawer-footer">
        <a-button @click="visible = false">{{ $t('common.action.cancel') }}</a-button>
        <template v-if="draftMode">
          <a-button :loading="saving" :disabled="!canSubmit" @click="submit(false)">{{ $t('fin.settlementForm.saveDraft') }}</a-button>
          <a-button v-perm="`fin:${kind}:confirm`" type="primary" :loading="saving" :disabled="!canSubmit" @click="submit(true)">{{ conf.saveConfirm }}</a-button>
        </template>
        <a-button v-else type="primary" :loading="saving" :disabled="!canSubmit" @click="submit(true)">{{ conf.confirm }}</a-button>
      </div>
    </template>
  </SubPage>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import AccountSelect from './AccountSelect.vue'
import SettlementAdvances from './SettlementAdvances.vue'
import SourceDocLink from '@/components/common/SourceDocLink.vue'
import { addSettlement, getSettlement, settlementAdvances, unsettledBills, updateSettlement } from '@/api/fin'
import { useDict } from '@/composables/useDict'
import { useBizRules } from '@/composables/useBizRules'
import { toOptions } from '@/utils/dict'
import { nz, round2 } from '@/utils/number'
import { today } from '@/components/wms/wms'
import { allocateSettle, formatMoney, settleAllAmounts } from './fin'
import { BASE_CURRENCY, currencySymbol, fetchRate, isForeign, useCurrencies } from './currency'
import { t } from '@/locales'
import { ApproveStatus } from '@/constants/enums'

/**
 * 新增 / 修改收款单、付款单（SettlementPage 的子组件，往来汇总也用它对某个单位直接收付款），conf 为页面按收付款区分的文字。
 * 业务参数「收付款先存草稿」开启时显示「保存草稿」「保存并确认」，否则保存即生效
 */
const props = defineProps({
  kind: { type: String, required: true },
  conf: { type: Object, required: true }
})
const emit = defineEmits(['saved'])
const { rules } = useBizRules({ 'fin.settlement.draft': false })
const draftMode = computed(() => rules['fin.settlement.draft'] === true)

const dict = useDict('fin_settle_method', 'fin_bill_source')
// 票据结算由票据业务（收票、背书、开票）自动生成，手工收付款时显示为禁用并说明
const methodOptions = computed(() =>
  toOptions(dict.fin_settle_method).map((o) => (o.value !== 'NOTE' ? o : { ...o, label: t('fin.settlementPage.noteMethodHint', { label: o.label }), disabled: true }))
)

const billColumns = [
  { title: t('fin.settlementPage.no'), dataIndex: 'billNo', width: 150 },
  { title: t('fin.settlementPage.source'), key: 'source', width: 190 },
  { title: t('fin.settlementPage.billDate'), dataIndex: 'billDate', width: 105 },
  { title: t('fin.settlementPage.dueDate'), dataIndex: 'dueDate', width: 105 },
  { title: t('common.field.amount'), key: 'amount', width: 130, align: 'right' },
  { title: t('fin.settlementPage.remaining'), key: 'remaining', width: 120, align: 'right' },
  { title: t('fin.settlementPage.settle'), key: 'settle', width: 140 },
  { title: t('fin.settlementForm.discount'), key: 'discount', width: 120 }
]
const billScrollX = billColumns.reduce((sum, c) => sum + c.width, 0) + 40
const visible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const bills = ref([])
const billLoading = ref(false)
/** 勾选的单据（参与本次收付款） */
const checked = ref([])
/** 本次收付款总额：没有手工改过时 = 勾选单据的未核销合计 */
const payTotal = ref(0)
const payTouched = ref(false)
const toAdvance = ref(false)
/** 修改的单据 ID（草稿、已拒绝的付款），新增为空 */
const editId = ref()
const partnerLabel = ref()

/** 未核销余额：外币单据为原币 */
const remaining = (b) =>
  isForeign(b.currency)
    ? round2(Number(b.amountFc || 0) - Number(b.settledFc || 0))
    : round2(Number(b.amount) - Number(b.settledAmount))
const isChecked = (b) => checked.value.includes(b.id)
const checkedBills = computed(() => bills.value.filter(isChecked))
const checkedRemaining = computed(() => round2(checkedBills.value.reduce((s, b) => s + remaining(b), 0)))
/** 核销合计（实收实付，外币为原币）、折让合计 */
const total = computed(() => round2(checkedBills.value.reduce((s, b) => s + nz(b.settle), 0)))
const discountTotal = computed(() => round2(checkedBills.value.reduce((s, b) => s + nz(b.discount), 0)))
// ---------------------------------------------------------------- 冲抵预收预付
/** 该单位未冲抵完的预收 / 预付款：[{ id, advanceNo, advanceDate, currency, remaining, offset }] */
const advances = ref([])
const advanceChecked = ref([])
const advanceTotal = computed(() => round2(advances.value.filter((a) => advanceChecked.value.includes(a.id)).reduce((s, a) => s + nz(a.offset), 0)))
/** 勾选预收预付时默认冲抵的上限：勾选单据的未核销合计中还没有被冲抵的部分 */
const advanceNeed = computed(() => round2(checkedRemaining.value - advanceTotal.value))
/** 实收实付 = 核销合计 − 冲抵预收预付 */
const cash = computed(() => round2(total.value - advanceTotal.value))
/** 本次收付款 - 实收实付：大于 0 为多收 / 多付，小于 0 为核销超过收付款 */
const diff = computed(() => round2(nz(payTotal.value) - cash.value))

watch([checkedRemaining, advanceTotal], ([v, a]) => {
  if (!payTouched.value) payTotal.value = round2(Math.max(0, v - a))
})

// ---------------------------------------------------------------- 外币核销
useCurrencies()
const curOf = (b) => b.currency || BASE_CURRENCY
const currencies = computed(() => [...new Set(bills.value.map(curOf))])
/** 本次核销的币种：勾选单据的币种；只有一种币种的单据时即为该币种 */
const settleCurrency = computed(() => {
  const picked = checkedBills.value[0]
  if (picked) return curOf(picked)
  return currencies.value.length === 1 ? currencies.value[0] : BASE_CURRENCY
})
const foreign = computed(() => isForeign(settleCurrency.value))
/** 已勾选了其他币种的单据时不能再勾选 */
const blocked = (b) => checkedBills.value.length > 0 && curOf(b) !== settleCurrency.value
const exchangeRate = ref()
/** 实收 / 实付本位币 = 原币合计 × 汇率 */
const baseTotal = computed(() => round2(total.value * (Number(exchangeRate.value) || 0)))

let rateSeq = 0
async function refreshRate() {
  if (!isForeign(settleCurrency.value)) return
  const seq = ++rateSeq
  const rate = await fetchRate(settleCurrency.value, form.value.settleDate)
  if (seq === rateSeq && rate != null) exchangeRate.value = rate
}
let keepRate = false
watch(settleCurrency, (c, old) => {
  if (c === old) return
  // 修改单据时保留原汇率
  if (keepRate) {
    keepRate = false
    return
  }
  exchangeRate.value = undefined
  refreshRate()
})
watch(settleCurrency, () => {
  // 只能冲抵同币种的预收预付
  advances.value.forEach((a) => {
    if ((a.currency || BASE_CURRENCY) !== settleCurrency.value) a.offset = undefined
  })
  advanceChecked.value = advanceChecked.value.filter((id) => advances.value.some((a) => a.id === id && (a.currency || BASE_CURRENCY) === settleCurrency.value))
})

function reset() {
  bills.value = []
  checked.value = []
  advances.value = []
  advanceChecked.value = []
  exchangeRate.value = undefined
  payTouched.value = false
  payTotal.value = 0
  toAdvance.value = false
}

/**
 * 打开表单：不传参数为新增；{ partnerId, partnerName } 为对该单位新增（往来汇总）；{ id } 为修改草稿 / 已拒绝的付款单
 */
async function open(opts = {}) {
  reset()
  editId.value = opts.id
  partnerLabel.value = opts.partnerName
  form.value = { partnerId: opts.partnerId, settleDate: today(), method: 'TRANSFER', accountId: undefined, remark: '' }
  visible.value = true
  if (opts.id) {
    await loadForEdit(opts.id)
  } else if (opts.partnerId) {
    await loadBills(opts.partnerId)
  }
}

async function loadForEdit(id) {
  const d = await getSettlement(props.kind, id)
  partnerLabel.value = d.partnerName
  form.value = { partnerId: d.partnerId, settleDate: d.settleDate, method: d.method, accountId: d.accountId, remark: d.remark || '' }
  keepRate = isForeign(d.currency)
  await loadBills(d.partnerId, d)
  if (isForeign(d.currency)) exchangeRate.value = Number(d.exchangeRate)
  payTotal.value = round2(Number(d.amountFc || 0) + Number(d.advanceAmount || 0))
  payTouched.value = true
  toAdvance.value = Number(d.advanceAmount) > 0
}

/** 列出未核销单据和可冲抵的预收预付；修改时按原单据回填核销金额、折让和冲抵金额 */
async function loadBills(partnerId, draft) {
  reset()
  if (!partnerId) return
  billLoading.value = true
  try {
    const [list, open] = await Promise.all([unsettledBills(props.kind === 'receipt' ? 'ar' : 'ap', partnerId), settlementAdvances(props.kind, partnerId)])
    bills.value = list.map((b) => ({ ...b, settle: undefined, discount: undefined }))
    advances.value = open.map((a) => ({ ...a, remaining: round2(Number(a.amountFc || 0) - Number(a.usedFc || 0)), offset: undefined }))
    if (draft) {
      const byBill = new Map(draft.items.map((i) => [i.billId, i]))
      bills.value.forEach((b) => {
        const i = byBill.get(b.id)
        if (!i) return
        b.settle = round2(Number(i.amountFc ?? i.amount) + Number(i.advanceOffset || 0))
        b.discount = Number(i.discountFc ?? i.discount) || undefined
      })
      checked.value = bills.value.filter((b) => byBill.has(b.id)).map((b) => b.id)
      const byAdvance = new Map((draft.advances || []).map((a) => [a.advanceId, a]))
      advances.value.forEach((a) => (a.offset = byAdvance.has(a.id) ? Number(byAdvance.get(a.id).amount) : undefined))
      advanceChecked.value = advances.value.filter((a) => byAdvance.has(a.id)).map((a) => a.id)
      return
    }
    // 默认勾选与第一张单据同币种的全部单据（只有一种币种时即全部）
    const cur = bills.value.length ? curOf(bills.value[0]) : BASE_CURRENCY
    checked.value = bills.value.filter((b) => curOf(b) === cur).map((b) => b.id)
  } finally {
    billLoading.value = false
  }
}

function onCheck(keys) {
  checked.value = keys
  // 取消勾选的单据不参与核销
  bills.value.forEach((b) => {
    if (!keys.includes(b.id)) {
      b.settle = undefined
      b.discount = undefined
    }
  })
}

/** 自动核销：把本次收付款总额 + 冲抵的预收预付按单据日期从早到晚分配到勾选的单据（红字先冲抵），已填的折让保留 */
function autoSettle() {
  const { amounts } = allocateSettle(checkedBills.value, round2(nz(payTotal.value) + advanceTotal.value), (b) => round2(remaining(b) - nz(b.discount)))
  // 核销合计为 0（蓝字与红字正好抵消）时，未分配到的单据核销为 0 也要保留，否则没有核销明细
  checkedBills.value.forEach((b) => (b.settle = amounts.has(b.id) ? amounts.get(b.id) : undefined))
}

/** 全部核销：勾选的蓝字单据按未核销余额（扣除折让）全部核销，红字单据冲抵不超过蓝字合计；本次收付款总额同步为实收实付 */
function fillAll() {
  const amounts = settleAllAmounts(checkedBills.value, (b) => round2(remaining(b) - nz(b.discount)))
  checkedBills.value.forEach((b) => (b.settle = amounts.get(b.id) || undefined))
  payTotal.value = Math.max(0, cash.value)
  payTouched.value = false
  if (currencies.value.length > 1) message.info(props.conf.filledTip(settleCurrency.value))
}

// 核销合计为 0 时（蓝字与红字正好抵消）核销金额都不为 0；有折让时核销金额可为 0
const items = computed(() =>
  checkedBills.value
    .filter((b) => nz(b.settle) || nz(b.discount))
    .map((b) => ({ billId: b.id, amount: nz(b.settle), discount: nz(b.discount) || null }))
)
const advanceItems = computed(() =>
  advances.value.filter((a) => advanceChecked.value.includes(a.id) && nz(a.offset) > 0).map((a) => ({ advanceId: a.id, amount: nz(a.offset) }))
)
/** 金额是否可以保存：核销合计、实收实付不为负数，本次金额不小于实收实付，多收须转预收（冲抵预收预付时不能多收） */
const amountsValid = computed(
  () =>
    items.value.length > 0 &&
    total.value >= 0 &&
    cash.value >= 0 &&
    diff.value >= 0 &&
    (diff.value === 0 || (toAdvance.value && !advanceTotal.value))
)
// 还没有填写核销金额时也可以提交：提交时按本次金额自动核销（如欠款与冲账正好抵消、本次金额为 0）
const canSubmit = computed(
  () => checked.value.length > 0 && (items.value.length === 0 || amountsValid.value) && !(foreign.value && !(Number(exchangeRate.value) > 0))
)

/** @param {boolean} confirm 未启用草稿时始终为 true（保存即生效）；启用时 false 保存草稿、true 保存并确认 */
async function submit(confirm) {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (foreign.value && !(Number(exchangeRate.value) > 0)) {
    message.warning(t('fin.settlementPage.rateRequired'))
    return
  }
  if (!items.value.length) autoSettle()
  if (!amountsValid.value) {
    message.warning(items.value.length ? t('fin.settlementForm.checkAmounts') : t('fin.settlementForm.noItems'))
    return
  }
  saving.value = true
  try {
    const data = {
      kind: props.kind.toUpperCase(),
      ...form.value,
      items: items.value,
      exchangeRate: foreign.value ? exchangeRate.value : null,
      totalAmount: nz(payTotal.value),
      toAdvance: diff.value > 0 && toAdvance.value,
      advances: advanceItems.value,
      confirm
    }
    const id = editId.value ? (await updateSettlement(props.kind, editId.value, data), editId.value) : await addSettlement(props.kind, data)
    const saved = await getSettlement(props.kind, id).catch(() => null)
    message.success(
      saved?.status === ApproveStatus.APPROVING ? props.conf.submitted : saved?.status === ApproveStatus.DRAFT ? t('fin.settlementForm.draftSaved') : props.conf.success
    )
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.pay-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 12px;
  padding: 12px;
  margin-bottom: 12px;
  border-radius: 8px;
  background: var(--app-hover);
}
.pay-input {
  width: 180px;
}
.summary {
  margin-left: auto;
}
.bills-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0;
}
.section-title {
  font-weight: 500;
}
.total {
  font-size: 16px;
  color: var(--ant-color-primary, #1677ff);
}
.advance-check {
  margin-left: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
.block {
  margin-bottom: 12px;
}
</style>
