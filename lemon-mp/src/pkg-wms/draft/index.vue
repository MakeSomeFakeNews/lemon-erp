<template>
  <view>
    <l-segmented v-if="kinds.length > 1" v-model="kind" :options="kinds" @change="paged.refresh()" />
    <view class="l-search">
      <input v-model="keyword" :placeholder="t('trade.draft.searchPh')" placeholder-class="l-placeholder" confirm-type="search" @confirm="paged.refresh()" />
    </view>
    <view v-for="d in paged.list" :key="d.id" class="l-item" @click="open(d)">
      <view class="l-item__head">
        <text class="l-item__title">{{ d.docNo }}</text>
        <text class="draft__amount">{{ money(d.totalAmount) }}</text>
      </view>
      <view class="l-item__desc">{{ [d.partnerName, d.warehouseName].filter(Boolean).join(' · ') }}</view>
      <view class="l-item__desc">{{ [d.bizDate, d.createByName].filter(Boolean).join(' · ') }}</view>
    </view>
    <l-empty v-if="paged.loaded && !paged.list.length" :text="t('trade.draft.empty')" />
    <l-load-more v-else-if="paged.list.length" :status="paged.status" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { pageTradeDraft } from '@/api/trade'
import { usePaged } from '@/composables/usePaged'
import { useTitle } from '@/composables/useTitle'
import { hasPerm } from '@/store/user'
import { money } from '@/utils/format'

/**
 * 单据草稿：销售出库、采购入库的草稿（业务参数「进销存单据先存草稿」开启时手机开单、Web 端保存的草稿），
 * 按出入库单的数据权限只列出看得到的；点击进入详情审核记账或删除。分段按列表权限显示
 */
const { t } = useI18n()
useTitle('trade.entry.draft')
const kinds = computed(() =>
  ['salesOut', 'purchaseIn'].filter((k) => hasPerm(`scm:${k}:list`)).map((k) => ({ label: t(`trade.kind.${k}`), value: k }))
)
const kind = ref(kinds.value[0]?.value || 'salesOut')
const keyword = ref('')
const paged = usePaged((p) => pageTradeDraft(kind.value, { ...p, docNo: keyword.value.trim() || undefined }))
let shown = false

onLoad((q) => {
  if (q?.kind && kinds.value.some((k) => k.value === q.kind)) kind.value = q.kind
  paged.refresh()
})
// 审核、删除后返回时刷新
onShow(() => {
  if (shown) paged.refresh()
  shown = true
})
onPullDownRefresh(() => paged.refresh())
onReachBottom(() => paged.more())

function open(d) {
  uni.navigateTo({ url: `/pkg-wms/draft/detail?kind=${kind.value}&id=${d.id}` })
}
</script>

<style lang="scss">
.draft__amount {
  flex-shrink: 0;
  font-size: 30rpx;
  font-weight: 600;
  color: $l-primary;
}
</style>
