<template>
  <view>
    <view class="m-card">
      <view class="m-title">{{ title }}</view>
      <view v-if="data && (data.spec || data.unit)" class="m-sub">{{ [data.spec, data.unit].filter(Boolean).join(' · ') }}</view>
      <picker :range="warehouseRange" range-key="name" :value="warehouseIndex" @change="changeWarehouse">
        <view class="m-input">
          <text class="m-input__label">{{ t('wms.common.warehouse') }}</text>
          <text class="m-input__value">{{ warehouseRange[warehouseIndex]?.name }}</text>
        </view>
      </picker>
      <view class="m-row hist__dates">
        <picker mode="date" :value="beginDate" :end="endDate" class="hist__date" @change="(e) => changeDate('begin', e.detail.value)">
          <view class="m-input">
            <text class="m-input__label">{{ t('wms.history.from') }}</text>
            <text class="m-input__value">{{ beginDate }}</text>
          </view>
        </picker>
        <picker mode="date" :value="endDate" :start="beginDate" class="hist__date" @change="(e) => changeDate('end', e.detail.value)">
          <view class="m-input">
            <text class="m-input__label">{{ t('wms.history.to') }}</text>
            <text class="m-input__value">{{ endDate }}</text>
          </view>
        </picker>
      </view>
      <view class="m-sub">{{ warehouseId ? t('wms.history.warehouseTip') : t('wms.history.allTip') }}</view>
    </view>

    <block v-if="data">
      <!-- 期初 -->
      <view class="m-card hist__balance">
        <view class="m-row">
          <text class="hist__label">{{ t('wms.history.opening') }} · {{ data.beginDate }}</text>
          <text class="hist__qty">{{ qty(data.openingQty) }}</text>
        </view>
        <view v-if="data.costVisible" class="m-row m-sub">
          <text>{{ t('wms.history.balanceAmount') }}</text>
          <text>{{ money(data.openingAmount) }}</text>
        </view>
        <view v-if="data.costVisible && data.openingEstimated" class="m-sub hist__estimated">{{ t('wms.history.openingEstimated') }}</view>
      </view>
      <view v-if="data.costVisible && hasEstimated" class="m-sub hist__estimated hist__note">{{ t('wms.history.estimatedTip') }}</view>

      <!-- 流水 -->
      <view v-for="r in data.rows" :key="r.id" class="m-card">
        <view class="m-row">
          <text class="m-title">{{ r.docNo || '-' }}</text>
          <l-tag :text="bizTypeText(r)" :color="r.qty > 0 ? 'green' : r.qty < 0 ? 'orange' : 'default'" />
        </view>
        <view class="m-sub">{{ [r.bizDate, r.partnerName, warehouseId ? null : r.warehouseName, r.batchNo && t('wms.common.batchN', { no: r.batchNo }).trim()].filter(Boolean).join(' · ') }}</view>
        <view v-if="r.sourceNo" class="m-sub">{{ t('wms.history.sourceNo', { no: r.sourceNo }) }}</view>
        <view class="hist__nums">
          <view class="hist__num">
            <text class="hist__num-value" :class="r.qty > 0 ? 'hist__in' : 'hist__out'">{{ r.qty > 0 ? '+' : '' }}{{ qty(r.qty) }}</text>
            <text class="hist__num-label">{{ r.qty >= 0 ? t('wms.history.inQty') : t('wms.history.outQty') }}</text>
          </view>
          <view class="hist__num">
            <text class="hist__num-value">{{ qty(r.balanceQty) }}</text>
            <text class="hist__num-label">{{ t('wms.history.balanceQty') }}</text>
          </view>
          <view v-if="data.costVisible" class="hist__num">
            <text class="hist__num-value">{{ money(r.balanceAmount) }}</text>
            <text class="hist__num-label">{{ t('wms.history.balanceAmount') }}</text>
          </view>
        </view>
        <view v-if="data.costVisible && r.costEstimated === 1" class="m-sub hist__estimated">{{ t('wms.history.estimated') }}</view>
        <view v-if="data.costVisible" class="m-row m-sub">
          <text>{{ t('wms.history.bizPrice') }} {{ price(r.bizPrice) }}</text>
          <text>{{ t('wms.history.costPrice') }} {{ price(r.costPrice) }}</text>
          <text>{{ t('wms.history.costAmount') }} {{ money(r.costAmount) }}</text>
        </view>
      </view>
      <l-empty v-if="!data.rows.length" :text="t('wms.history.noFlows')" />

      <!-- 结余 -->
      <view class="m-card hist__balance">
        <view class="m-row">
          <text class="hist__label">{{ t('wms.history.closing') }} · {{ data.endDate }}</text>
          <text class="hist__qty">{{ qty(closing.qty) }}</text>
        </view>
        <view v-if="data.costVisible" class="m-row m-sub">
          <text>{{ t('wms.history.balanceAmount') }}</text>
          <text>{{ money(closing.amount) }}</text>
        </view>
      </view>
    </block>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { StockBizType } from '@web/constants/enums'
import { stockHistory, warehouseOptions } from '@/api/wms'
import { useTitle } from '@/composables/useTitle'
import { dictText, useDict } from '@/store/dict'
import { money, price, qty, today } from '@/utils/format'

/**
 * 经营历史（与 Web 端 components/wms/StockHistory.vue 相同的接口 /wms/stock/history）：物料的期初余额、每条库存流水、期末结余。
 * 不选仓库为全公司（不含调拨和供应商寄售仓）；有「查看成本金额」权限时显示业务单价、成本价、金额和结余金额。
 * 升级前的历史流水成本是回填的近似值，按后端的 costEstimated / openingEstimated 标示。参数：materialId、name（显示用）
 */
const { t } = useI18n()
useTitle('wms.history.title')
const dict = useDict('wms_biz_type', 'wms_in_type', 'wms_out_type')

const materialId = ref(null)
const name = ref('')
const warehouses = ref([])
const warehouseId = ref(null)
const beginDate = ref(today().slice(0, 8) + '01')
const endDate = ref(today())
const data = ref(null)

const title = computed(() => (data.value ? `${data.value.materialCode} ${data.value.materialName}` : name.value))
const warehouseRange = computed(() => [{ id: null, name: t('wms.history.allWarehouses') }, ...warehouses.value])
const warehouseIndex = computed(() => Math.max(0, warehouseRange.value.findIndex((w) => w.id === warehouseId.value)))
/** 期末结余：最后一条流水的结余，没有流水时为期初 */
const closing = computed(() => {
  const rows = data.value?.rows || []
  const last = rows[rows.length - 1]
  return last ? { qty: last.balanceQty, amount: last.balanceAmount } : { qty: data.value?.openingQty, amount: data.value?.openingAmount }
})

/** 升级前的历史流水：成本是回填的近似值（costEstimated 为 1） */
const hasEstimated = computed(() => (data.value?.rows || []).some((r) => r.costEstimated === 1))

const bizTypeText = (r) =>
  r.docBizType
    ? dictText(r.bizType === StockBizType.IN ? dict.wms_in_type : dict.wms_out_type, r.docBizType)
    : dictText(dict.wms_biz_type, r.bizType)

onLoad((q) => {
  materialId.value = Number(q.materialId)
  name.value = decodeURIComponent(q.name || '')
  if (q.warehouseId) warehouseId.value = Number(q.warehouseId)
  warehouseOptions().then((list) => (warehouses.value = list || [])).catch(() => {})
  load()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})

async function load() {
  uni.showLoading({ mask: true })
  try {
    const res = await stockHistory({ materialId: materialId.value, warehouseId: warehouseId.value, beginDate: beginDate.value, endDate: endDate.value })
    data.value = { ...res, rows: (res.rows || []).map((r) => ({ ...r, qty: Number(r.qty) })) }
  } catch {
    // 错误已提示
  } finally {
    uni.hideLoading()
  }
}

function changeWarehouse(e) {
  warehouseId.value = warehouseRange.value[e.detail.value]?.id ?? null
  load()
}

function changeDate(which, value) {
  if (which === 'begin') beginDate.value = value
  else endDate.value = value
  load()
}
</script>

<style lang="scss">
@import '../mobile.scss';

.hist {
  &__dates {
    gap: 16rpx;
  }
  &__date {
    flex: 1;
    min-width: 0;
  }
  &__balance {
    background: #f0f5ff;
  }
  &__label {
    font-size: 28rpx;
    font-weight: 600;
  }
  &__qty {
    font-size: 32rpx;
    font-weight: 600;
  }
  &__nums {
    display: flex;
    margin: 16rpx 0 4rpx;
  }
  &__num {
    flex: 1;
    display: flex;
    flex-direction: column;
  }
  &__num-value {
    font-size: 30rpx;
    font-weight: 600;
  }
  &__num-label {
    margin-top: 4rpx;
    font-size: 22rpx;
    color: $l-text-3;
  }
  &__in {
    color: $l-success;
  }
  &__out {
    color: #fa8c16;
  }
  &__estimated {
    color: #d48806;
  }
  &__note {
    margin: 0 24rpx;
  }
}
</style>
