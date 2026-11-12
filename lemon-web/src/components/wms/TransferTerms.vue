<template>
  <!--
    调拨单表头的调拨价格与调拨费用（StockDocForm 的一部分，直接修改 form）：
    调拨价格 同价调拨 / 差价调拨（依据价 × 系数，修改时批量更新明细的调拨价，明细可再逐行修改）；
    调拨费用 本公司承担，现付生成费用支出单、月结生成承运商应付单，计入当期费用
  -->
  <a-row :gutter="16">
    <a-col :xs="24" :sm="12" :lg="6">
      <a-form-item :label="$t('wms.transfer.pricing')" name="transferPricing" :extra="$t('wms.transfer.pricingTip')">
        <a-select :value="pricingValue" :options="pricingOptions" @change="onPricingChange" />
      </a-form-item>
    </a-col>
    <a-col v-if="differential" :xs="24" :sm="12" :lg="4">
      <a-form-item :label="$t('wms.transfer.factor')" name="priceFactor">
        <a-input-number v-model:value="form.priceFactor" :min="0.0001" :max="999999" :precision="4" :step="0.1" class="full" @change="recalc()" />
      </a-form-item>
    </a-col>
    <a-col :xs="24" :sm="12" :lg="4">
      <a-form-item :label="$t('wms.transfer.fee')" name="fee">
        <a-input-number v-model:value="form.fee" :min="0" :precision="2" prefix="¥" class="full" :placeholder="$t('wms.common.optional')" />
      </a-form-item>
    </a-col>
    <template v-if="Number(form.fee) > 0">
      <a-col :xs="24" :sm="12" :lg="4">
        <a-form-item :label="$t('wms.transfer.feeSettle')" name="feeSettle">
          <a-radio-group v-model:value="form.feeSettle" :options="settleOptions" option-type="button" />
        </a-form-item>
      </a-col>
      <a-col v-if="cash" :xs="24" :sm="12" :lg="6">
        <a-form-item :label="$t('wms.transfer.feeAccount')" name="feeAccountId" :rules="[{ required: true, message: $t('wms.transfer.selectAccount') }]">
          <AccountSelect v-model:value="form.feeAccountId" :roles="['CASH', 'BANK']" :placeholder="$t('wms.transfer.selectAccount')" />
        </a-form-item>
      </a-col>
      <a-col :xs="24" :sm="12" :lg="6">
        <a-form-item
          :label="cash ? $t('wms.transfer.carrierOptional') : $t('wms.transfer.carrier')"
          name="feeCarrierId"
          :rules="cash ? [] : [{ required: true, message: $t('wms.transfer.selectCarrier') }]"
        >
          <PartnerSelect v-model:value="form.feeCarrierId" type="supplier" carrier allow-clear :label="form.feeCarrierName" :placeholder="$t('wms.transfer.selectCarrier')" />
        </a-form-item>
      </a-col>
      <a-col :span="24">
        <div class="sub-text fee-hint">{{ cash ? $t('wms.transfer.feeHintCash') : $t('wms.transfer.feeHintMonthly') }}</div>
      </a-col>
    </template>
  </a-row>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { usePriceLevels } from './priceLevels'
import { transferPrices } from '@/api/wms/doc'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { TransferFeeSettle, TransferPricing } from '@/constants/enums'

const props = defineProps({
  form: { type: Object, required: true }
})
const { t } = useI18n()
const userStore = useUserStore()
const { enabledLevels } = usePriceLevels()
const dict = useDict('scm_freight_settle')
const settleOptions = computed(() => toOptions(dict.scm_freight_settle))
const cash = computed(() => props.form.feeSettle !== TransferFeeSettle.MONTHLY)

/** 调拨价、调出成本只给有「查看成本金额」权限的人（同价调拨的调拨价就是成本）；没有权限时由后端按依据 × 系数计算 */
const canCost = computed(() => userStore.hasPerm('wms:stock:cost'))
const differential = computed(() => !!props.form.transferPricing && props.form.transferPricing !== TransferPricing.SAME)

// 价格列在下拉中展开为「零售价」「批发价1」…（只列启用的），取值 PRICE_LEVEL:<列>
const LEVEL = `${TransferPricing.PRICE_LEVEL}:`
const pricingOptions = computed(() => [
  ...[TransferPricing.SAME, TransferPricing.REF_COST, TransferPricing.AVG_COST, TransferPricing.MIN_SALE].map((v) => ({
    value: v,
    label: t(`wms.transfer.pricings.${v}`)
  })),
  ...enabledLevels.value.map((l) => ({ value: `${LEVEL}${l.level}`, label: l.name }))
])
const pricingValue = computed(() =>
  props.form.transferPricing === TransferPricing.PRICE_LEVEL ? `${LEVEL}${props.form.priceLevel}` : props.form.transferPricing || TransferPricing.SAME
)

function onPricingChange(v) {
  const level = String(v).startsWith(LEVEL)
  props.form.transferPricing = level ? TransferPricing.PRICE_LEVEL : v
  props.form.priceLevel = level ? Number(String(v).slice(LEVEL.length)) : null
  if (props.form.priceFactor == null) props.form.priceFactor = 1
  recalc()
}

/**
 * 按 依据价 × 系数 批量更新明细的调拨价（同价调拨清空，确认时等于调出成本价）
 *
 * @param rows 只更新这些行（如新选择的库存），默认全部明细
 */
async function recalc(rows = props.form.items) {
  if (!differential.value) {
    rows.forEach((r) => (r.transferPrice = null))
    return
  }
  if (!canCost.value || !rows.length || !(Number(props.form.priceFactor) > 0)) return
  const prices = await transferPrices({
    pricing: props.form.transferPricing,
    priceLevel: props.form.priceLevel ?? undefined,
    factor: props.form.priceFactor,
    materialIds: [...new Set(rows.map((r) => r.materialId))]
  })
  let missing = 0
  rows.forEach((r) => {
    const p = prices[r.materialId]
    r.transferPrice = p == null ? null : Number(p)
    if (p == null) missing++
  })
  if (missing) message.info(t('wms.transfer.noBasis', { n: missing }))
}

defineExpose({ recalc })
</script>

<style scoped>
.full {
  width: 100%;
}
.fee-hint {
  margin-top: -16px;
  margin-bottom: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
