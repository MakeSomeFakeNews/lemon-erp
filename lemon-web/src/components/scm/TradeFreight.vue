<template>
  <!--
    出入库单的运费（TradeDocForm 的一部分，直接修改 form）：
    采购入库 供应商代垫 / 本公司承担（生成运费应付单，计入存货成本或当期费用）；
    销售出库 本公司承担 / 代垫运费（客户承担），付给承运商现付（费用支出单）或月结（承运商应付）；
    销售退货 只有客户代垫（红字应收）；采购退货 只有代垫运费（红字应付），付给承运商现付或月结
  -->
  <a-row :gutter="16" class="freight-row">
    <a-col :xs="24" :sm="8" :lg="5">
      <a-form-item :label="$t('scm.freight.label')" name="freight">
        <a-input-number v-model:value="form.freight" :min="0" :precision="2" prefix="¥" class="full" :placeholder="$t('scm.trade.optional')" />
      </a-form-item>
    </a-col>
    <template v-if="Number(form.freight) > 0">
      <a-col :xs="24" :sm="8" :lg="5">
        <a-form-item :label="kind === 'purchaseIn' ? $t('scm.freight.payTo') : $t('scm.freight.bearerLabel')" name="freightBearer">
          <a-select v-if="bearerOptions.length > 1" v-model:value="form.freightBearer" :options="bearerOptions" />
          <div v-else class="fixed-value">{{ bearerOptions[0].label }}</div>
        </a-form-item>
      </a-col>
      <a-col v-if="settled" :xs="24" :sm="8" :lg="4">
        <a-form-item :label="$t('scm.freight.settle')" name="freightSettle">
          <a-radio-group v-model:value="form.freightSettle" :options="settleOptions" option-type="button" />
        </a-form-item>
      </a-col>
      <a-col v-if="settled && form.freightSettle === FreightSettle.CASH" :xs="24" :sm="8" :lg="5">
        <a-form-item :label="$t('scm.freight.account')" name="freightAccountId" :rules="requiredRule($t('scm.freight.selectAccount'))">
          <AccountSelect v-model:value="form.freightAccountId" :roles="['CASH', 'BANK']" :placeholder="$t('scm.freight.selectAccount')" />
        </a-form-item>
      </a-col>
      <a-col v-if="showCarrier" :xs="24" :sm="8" :lg="5">
        <a-form-item
          :label="carrierRequired ? $t('scm.freight.carrier') : $t('scm.freight.carrierOptional')"
          name="carrierSupplierId"
          :rules="carrierRequired ? requiredRule($t('scm.freight.selectCarrier')) : []"
        >
          <PartnerSelect v-model:value="form.carrierSupplierId" type="supplier" carrier allow-clear :label="form.carrierSupplierName" :placeholder="$t('scm.freight.selectCarrier')" />
        </a-form-item>
      </a-col>
      <a-col :span="24">
        <div class="sub-text freight-hint">{{ hint }}</div>
      </a-col>
    </template>
  </a-row>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import PartnerSelect from './PartnerSelect.vue'
import { FREIGHT_BEARERS, freightSettled } from './scm'
import { FreightBearer, FreightSettle, PurchaseFreightCost } from '@/constants/enums'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'

const props = defineProps({
  kind: { type: String, required: true },
  form: { type: Object, required: true },
  /** 业务参数「本公司承担的采购运费计入」 */
  purchaseFreightCost: { type: String, default: PurchaseFreightCost.INVENTORY }
})
const { t } = useI18n()

const requiredRule = (msg) => [{ required: true, message: msg }]
const dict = useDict('scm_freight_settle')
const settleOptions = computed(() => toOptions(dict.scm_freight_settle))
/** 运费承担的名称按单据种类（同一取值在退货中表示往来单位代垫） */
const bearerOptions = computed(() => FREIGHT_BEARERS[props.kind].map((v) => ({ value: v, label: t(`scm.freight.bearers.${props.kind}.${v}`) })))
const settled = computed(() => freightSettled(props.kind))
/** 承运商：采购入库本公司承担时必填；销售出库、采购退货月结时必填，现付时可选（费用支出单的关联单位） */
const showCarrier = computed(() => (props.kind === 'purchaseIn' ? props.form.freightBearer === FreightBearer.COMPANY : settled.value))
const carrierRequired = computed(() => (props.kind === 'purchaseIn' ? true : props.form.freightSettle === FreightSettle.MONTHLY))

const hint = computed(() => {
  const f = props.form
  const cash = f.freightSettle === FreightSettle.CASH
  switch (props.kind) {
    case 'purchaseIn':
      if (f.freightBearer !== FreightBearer.COMPANY) return t('scm.freight.supplierHint')
      return props.purchaseFreightCost === PurchaseFreightCost.EXPENSE ? t('scm.freight.expenseHint') : t('scm.freight.inventoryHint')
    case 'salesOut':
      if (f.freightBearer === FreightBearer.CUSTOMER) return cash ? t('scm.freight.hint.salesOutCustomerCash') : t('scm.freight.hint.salesOutCustomerMonthly')
      return cash ? t('scm.freight.hint.salesOutCompanyCash') : t('scm.freight.hint.salesOutCompanyMonthly')
    case 'salesReturn':
      return t('scm.freight.hint.salesReturn')
    default:
      return cash ? t('scm.freight.hint.purchaseReturnCash') : t('scm.freight.hint.purchaseReturnMonthly')
  }
})
</script>

<style scoped>
.freight-row {
  margin-top: 12px;
}
.full {
  width: 100%;
}
.fixed-value {
  line-height: 32px;
}
.freight-hint {
  margin-top: -16px;
  margin-bottom: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
