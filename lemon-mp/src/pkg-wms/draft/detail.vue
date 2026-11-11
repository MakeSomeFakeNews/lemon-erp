<template>
  <view>
    <block v-if="draft">
      <view class="l-item">
        <view class="l-item__head">
          <text class="l-item__title">{{ draft.docNo }}</text>
          <l-tag :text="t('trade.draft.status')" color="orange" />
        </view>
        <l-cell :label="t(`trade.partner.${kind}`)" :value="draft.partnerName" />
        <l-cell :label="t('wms.common.warehouse')" :value="draft.warehouseName" />
        <l-cell :label="t('trade.draft.bizDate')" :value="draft.bizDate" />
        <l-cell v-if="draft.orderNo" :label="t('trade.draft.orderNo')" :value="draft.orderNo" />
        <l-cell v-if="draft.invoiceKind" :label="t('wms.sale.invoiceKind')" :value="dictText(dict.scm_invoice_kind, draft.invoiceKind)" />
        <l-cell v-if="Number(draft.wholeDiscount)" :label="t('wms.sale.wholeDiscount')" :value="money(draft.wholeDiscount)" />
        <l-cell v-if="Number(draft.freight)" :label="t('wms.sale.freight')" :value="money(draft.freight)" />
        <l-cell :label="t('trade.draft.totalAmount')" :value="money(draft.totalAmount)" />
        <l-cell :label="t('trade.draft.createBy')" :value="draft.createByName" />
        <l-cell v-if="draft.remark" :label="t('common.remark')" :value="draft.remark" />
      </view>

      <view class="draft__section">{{ t('trade.draft.items', { n: (draft.items || []).length }) }}</view>
      <view v-for="(r, i) in draft.items" :key="r.id || i" class="l-item">
        <view class="l-item__head">
          <text class="l-item__title">
            <text v-if="r.gift === 1" class="draft__gift">{{ t('wms.sale.gift') }}</text>{{ r.materialCode }} {{ r.materialName }}
          </text>
          <text class="draft__line">{{ money(lineAmount(r)) }}</text>
        </view>
        <view v-if="r.spec" class="l-item__desc">{{ r.spec }}</view>
        <view class="l-item__desc">
          {{ t('trade.draft.qtyPrice', { qty: qty(r.qty), unit: r.unit || '', price: r.price == null ? '-' : price(r.price) }) }}{{ r.unitName ? ` (${r.unitName})` : '' }}
        </view>
        <view v-if="r.locationCode || r.batchNo" class="l-item__desc">{{ [r.locationCode, r.batchNo].filter(Boolean).join(' · ') }}</view>
        <view v-if="r.serialNos" class="l-item__desc">{{ r.serialNos }}</view>
      </view>

      <view class="l-footer-space" />
      <view v-if="canDelete || canPost" class="l-footer">
        <button v-if="canDelete" class="l-btn l-btn--default" @click="remove">{{ t('common.delete') }}</button>
        <button v-if="canPost" class="l-btn" @click="post">{{ t('trade.draft.post') }}</button>
      </view>
    </block>
    <l-empty v-else-if="loaded" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { deleteTradeDraft, getTradeDraft, postTradeDraft } from '@/api/trade'
import { runAction } from '@/composables/useAction'
import { useTitle } from '@/composables/useTitle'
import { dictText, useDict } from '@/store/dict'
import { hasPerm } from '@/store/user'
import { money, price, qty } from '@/utils/format'
import { askPrint } from '../print'

/**
 * 草稿详情：表头和明细（单价为含税、基本单位，审核后以出入库单为准）；
 * 审核记账（scm:{kind}:post）：出入库、生成应收应付，成功后可打印 / 分享 PDF；删除（scm:{kind}:add）
 */
const { t } = useI18n()
useTitle('trade.draft.title')
const dict = useDict('scm_invoice_kind')
const kind = ref('salesOut')
const id = ref(null)
const draft = ref(null)
const loaded = ref(false)

const canPost = computed(() => hasPerm(`scm:${kind.value}:post`))
const canDelete = computed(() => hasPerm(`scm:${kind.value}:add`))
const lineAmount = (r) => (r.gift === 1 ? 0 : Math.round((Number(r.qty) || 0) * (Number(r.price) || 0) * 100) / 100)

onLoad(async (q) => {
  kind.value = q.kind === 'purchaseIn' ? 'purchaseIn' : 'salesOut'
  id.value = Number(q.id)
  try {
    draft.value = await getTradeDraft(kind.value, id.value)
  } finally {
    loaded.value = true
  }
})

async function post() {
  let docId = null
  const ok = await runAction(
    async () => {
      docId = await postTradeDraft(kind.value, id.value)
    },
    { confirm: t('trade.draft.postConfirm', { no: draft.value.docNo }), success: t('trade.save.posted') }
  )
  if (!ok) return
  if (docId) askPrint(kind.value, docId)
  // 审核后草稿已删除：返回列表（列表 onShow 刷新）
  setTimeout(() => uni.navigateBack(), docId ? 0 : 800)
}

async function remove() {
  const ok = await runAction(() => deleteTradeDraft(kind.value, id.value), { confirm: t('trade.draft.deleteConfirm', { no: draft.value.docNo }) })
  if (ok) setTimeout(() => uni.navigateBack(), 800)
}
</script>

<style lang="scss">
.draft {
  &__section {
    margin: 24rpx 32rpx 0;
    font-size: 26rpx;
    color: $l-text-3;
  }
  &__line {
    flex-shrink: 0;
    font-size: 28rpx;
    font-weight: 600;
  }
  &__gift {
    margin-right: 8rpx;
    padding: 2rpx 12rpx;
    border-radius: 6rpx;
    background: $l-success;
    color: #fff;
    font-size: 22rpx;
  }
}
</style>
