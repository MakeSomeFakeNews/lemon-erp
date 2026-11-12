<template>
  <!-- 引用订单 / 原单：明细按优惠前单价显示（不改单价），订单优惠在合计处整单扣减（退货即扣回原单的优惠），金额以保存后的单据为准；
       采购入库可按供应商实际给的优惠修改本单的订单优惠 -->
  <div v-if="form.items.length && (refDiscount > 0 || discountEditable)" class="ref-discount">
    <span>{{ $t('scm.wholeDiscount.total') }} <b>¥{{ formatMoney(total) }}</b></span>
    <span class="discount-cell">
      <a-tooltip :title="discountHint">
        <span class="dashed">{{ returns ? $t('scm.wholeDiscount.returnShare') : $t('scm.wholeDiscount.orderShare') }}</span>
      </a-tooltip>
      <template v-if="discountEditable">
        <a-input-number
          :value="docDiscount"
          :min="0"
          :max="total"
          :precision="2"
          prefix="−¥"
          size="small"
          class="discount-input"
          @update:value="(v) => (form.orderDiscount = v ?? 0)"
        />
        <a v-if="form.orderDiscount != null" class="reset-link" @click="form.orderDiscount = undefined">{{ $t('scm.wholeDiscount.orderDiscountReset') }}</a>
      </template>
      <b v-else>−¥{{ formatMoney(refDiscount) }}</b>
    </span>
    <span>{{ returns ? $t('scm.wholeDiscount.refundAfter') : $t('scm.wholeDiscount.after') }} <b class="after">¥{{ formatMoney(afterAmount) }}</b></span>
  </div>
  <div v-if="discountChanged" class="discount-warn">{{ $t('scm.wholeDiscount.orderDiscountChanged', { amount: formatMoney(refDiscount) }) }}</div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatMoney, lineAmount } from './scm'
import { round2 } from '@/utils/number'

/**
 * 引用订单的出入库单合计处的整单优惠：订单 / 原单按优惠前单价合计，再扣减分摊的整单优惠；
 * 引用订单的采购入库可修改本单的订单优惠（form.orderDiscount，为空按订单）
 */
const props = defineProps({
  kind: { type: String, required: true },
  /** 表单：items、orderDiscount（直接修改） */
  form: { type: Object, required: true },
  /** 明细合计（优惠前单价） */
  total: { type: Number, required: true },
  returns: Boolean
})
const { t } = useI18n()

/** 按成交价（已扣除整单优惠分摊）估算的金额，与优惠前合计的差额即订单 / 原单的整单优惠分摊 */
const netAmount = computed(() => round2(props.form.items.reduce((sum, i) => sum + lineAmount(i.qty, i.price), 0)))
const refDiscount = computed(() => round2(props.total - netAmount.value))
const discountEditable = computed(() => props.kind === 'purchaseIn')
const docDiscount = computed(() => (discountEditable.value && props.form.orderDiscount != null ? Number(props.form.orderDiscount) : refDiscount.value))
const afterAmount = computed(() => round2(props.total - docDiscount.value))
const discountChanged = computed(() => discountEditable.value && props.form.orderDiscount != null && round2(props.form.orderDiscount) !== refDiscount.value)
const discountHint = computed(() =>
  props.returns
    ? t('scm.wholeDiscount.returnShareHint')
    : t(props.kind === 'purchaseIn' ? 'scm.wholeDiscount.orderSharePurchaseHint' : 'scm.wholeDiscount.orderShareHint')
)
</script>

<style scoped>
.ref-discount {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px 24px;
  margin-top: 12px;
}
.ref-discount .dashed {
  border-bottom: 1px dashed var(--app-text-3);
  cursor: help;
}
.discount-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.discount-input {
  width: 140px;
}
.reset-link {
  font-size: 12px;
}
.discount-warn {
  margin-top: 4px;
  text-align: right;
  color: var(--ant-color-warning, #faad14);
  font-size: 12px;
}
.ref-discount .after {
  font-size: 16px;
  color: var(--ant-color-primary, #1677ff);
}
</style>
