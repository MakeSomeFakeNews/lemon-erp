<template>
  <!-- 运费（与 Web 端销售出库一致）：本公司承担 / 代垫运费（客户承担），付给承运商现付（选付款账户，生成费用支出单）或月结（选承运商，生成应付） -->
  <view class="m-card">
    <view class="m-input">
      <text class="m-input__label">{{ t('wms.sale.freight') }}</text>
      <input :value="amount" type="digit" :placeholder="t('wms.sale.optional')" placeholder-class="l-placeholder" @blur="(e) => changeAmount(e.detail.value)" />
    </view>
    <block v-if="amount > 0">
      <picker :range="bearerLabels" :value="bearerIndex" @change="(e) => (bearer = BEARERS[e.detail.value])">
        <view class="m-input">
          <text class="m-input__label">{{ t('wms.sale.freightBearer') }}</text>
          <text class="m-input__value">{{ bearerLabels[bearerIndex] }}</text>
        </view>
      </picker>
      <picker :range="settleLabels" :value="settleIndex" @change="(e) => (settle = SETTLES[e.detail.value])">
        <view class="m-input">
          <text class="m-input__label">{{ t('wms.sale.freightSettle') }}</text>
          <text class="m-input__value">{{ settleLabels[settleIndex] }}</text>
        </view>
      </picker>
      <picker v-if="settle === FreightSettle.CASH" :range="accounts" range-key="label" :value="accountIndex" @change="(e) => (accountId = accounts[e.detail.value]?.id || null)">
        <view class="m-input">
          <text class="m-input__label">{{ t('wms.sale.freightAccount') }}</text>
          <text class="m-input__value" :class="{ 'l-placeholder': !accountId }">{{ accountLabel || t('common.pleaseSelect') }}</text>
        </view>
      </picker>
      <view class="m-input" @click="carrierOpen = true">
        <text class="m-input__label">{{ t('wms.sale.freightCarrier') }}</text>
        <text class="m-input__value" :class="{ 'l-placeholder': !carrier }">
          {{ carrier ? carrier.title : settle === FreightSettle.MONTHLY ? t('common.pleaseSelect') : t('wms.sale.optional') }}
        </text>
      </view>
      <view class="freight__hint">{{ hint }}</view>
    </block>
    <SearchSheet
      :open="carrierOpen"
      :title="t('wms.sale.freightCarrier')"
      :items="carrierItems"
      :loading="carrierLoading"
      :has-more="carrierHasMore"
      :empty-option="settle === FreightSettle.CASH ? t('wms.sale.freightNoCarrier') : ''"
      @search="searchCarrier"
      @more="searchCarrier(carrierKeyword, true)"
      @select="selectCarrier"
      @close="carrierOpen = false"
    />
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useI18n } from '@/locales'
import { OPTION_PAGE_SIZE, carrierOptions, cashAccounts } from '@/api/wms'
import { FreightBearer, FreightSettle } from '@web/constants/enums'
import SearchSheet from '../components/SearchSheet.vue'

/**
 * 快速开单的运费：通过 ref 调用 check()（返回错误提示，没有为空串）、payload()（请求中的运费字段，没有运费时为空对象）、reset()。
 * 本公司承担现付生成费用支出单（类别「销售运费」，来源为该出库单），月结生成承运商应付；代垫运费另生成客户应收（应收 = 货款 + 代垫运费）
 */
const { t } = useI18n()
const BEARERS = [FreightBearer.COMPANY, FreightBearer.CUSTOMER]
const SETTLES = [FreightSettle.CASH, FreightSettle.MONTHLY]

const amount = ref(0)
const bearer = ref(FreightBearer.COMPANY)
const settle = ref(FreightSettle.CASH)
const accountId = ref(null)
const carrier = ref(null)
const accounts = ref([])

const bearerLabels = computed(() => BEARERS.map((b) => t(`wms.sale.freightBearers.${b}`)))
const bearerIndex = computed(() => Math.max(0, BEARERS.indexOf(bearer.value)))
const settleLabels = computed(() => SETTLES.map((s) => t(`wms.sale.freightSettles.${s}`)))
const settleIndex = computed(() => Math.max(0, SETTLES.indexOf(settle.value)))
const accountIndex = computed(() => Math.max(0, accounts.value.findIndex((a) => a.id === accountId.value)))
const accountLabel = computed(() => accounts.value.find((a) => a.id === accountId.value)?.label)
const hint = computed(() => {
  const cash = settle.value === FreightSettle.CASH
  if (bearer.value === FreightBearer.CUSTOMER) return cash ? t('wms.sale.freightHintCustomerCash') : t('wms.sale.freightHintCustomerMonthly')
  return cash ? t('wms.sale.freightHintCompanyCash') : t('wms.sale.freightHintCompanyMonthly')
})

function changeAmount(v) {
  const n = Number(v)
  amount.value = Number.isFinite(n) && n > 0 ? Math.round(n * 100) / 100 : 0
}

// 有运费时加载付款账户（现金 / 银行末级科目），只有一个时默认选中
watch(amount, async (v) => {
  if (!(v > 0) || accounts.value.length) return
  accounts.value = (await cashAccounts().catch(() => [])) || []
  if (!accountId.value && accounts.value.length === 1) accountId.value = accounts.value[0].id
})

const carrierOpen = ref(false)
const carrierItems = ref([])
const carrierLoading = ref(false)
const carrierHasMore = ref(false)
const carrierKeyword = ref('')
let carrierPage = 1
let carrierSeq = 0
/** 承运商按关键字分页查询，more 为加载下一页 */
async function searchCarrier(keyword, more = false) {
  const seq = ++carrierSeq
  const page = more ? carrierPage + 1 : 1
  if (!more) carrierKeyword.value = keyword
  carrierLoading.value = true
  try {
    const res = await carrierOptions(keyword, page).catch(() => null)
    if (seq !== carrierSeq) return
    const items = (res?.options || []).map((s) => ({ id: s.id, title: `${s.code} ${s.name}` }))
    carrierPage = page
    carrierItems.value = more ? [...carrierItems.value, ...items.filter((it) => !carrierItems.value.some((x) => x.id === it.id))] : items
    carrierHasMore.value = items.length >= OPTION_PAGE_SIZE
  } finally {
    if (seq === carrierSeq) carrierLoading.value = false
  }
}
function selectCarrier(item) {
  carrier.value = item
  carrierOpen.value = false
}

function check() {
  if (!(amount.value > 0)) return ''
  if (settle.value === FreightSettle.CASH && !accountId.value) return t('wms.sale.freightSelectAccount')
  if (settle.value === FreightSettle.MONTHLY && !carrier.value) return t('wms.sale.freightSelectCarrier')
  return ''
}

function payload() {
  if (!(amount.value > 0)) return {}
  return {
    freight: amount.value,
    freightBearer: bearer.value,
    freightSettle: settle.value,
    freightAccountId: settle.value === FreightSettle.CASH ? accountId.value : null,
    carrierSupplierId: carrier.value?.id || null
  }
}

/** 保存后清空运费（账户、付款方式保留，便于连续开单） */
function reset() {
  amount.value = 0
  carrier.value = null
}

defineExpose({ check, payload, reset })
</script>

<style lang="scss">
@import '../mobile.scss';

.freight__hint {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: $l-text-3;
}
</style>
