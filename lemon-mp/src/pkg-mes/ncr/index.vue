<template>
  <view>
    <l-segmented v-model="status" :options="tabs" @change="paged.refresh()" />
    <view class="l-search">
      <input v-model="keyword" :placeholder="t('mes.ncr.searchPh')" placeholder-class="l-placeholder" confirm-type="search" @confirm="paged.refresh()" />
    </view>
    <view v-for="r in paged.list" :key="r.id" class="l-item" @click="open(r)">
      <view class="l-item__head">
        <text class="l-item__title">{{ r.materialName }}</text>
        <l-tag :text="dictText(dict.qms_ncr_status, r.status)" :color="dictColor(dict.qms_ncr_status, r.status)" />
      </view>
      <view class="l-item__desc nc__desc">{{ r.description }}</view>
      <view class="l-item__desc">{{ [r.ncrNo, dictText(dict.qms_ncr_source, r.source), qty(r.qty) + ' ' + (r.unit || '')].filter(Boolean).join(' · ') }}</view>
      <view class="l-item__desc">{{ [r.createByName, shortTime(r.createTime)].filter(Boolean).join(' ') }}</view>
    </view>
    <l-empty v-if="paged.loaded && !paged.list.length" />
    <l-load-more v-else-if="paged.list.length" :status="paged.status" />

    <template v-if="canAdd">
      <view class="l-footer-space" />
      <view class="l-footer">
        <button class="l-btn" @click="add">{{ t('mes.ncr.add') }}</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { NcrStatus } from '@web/constants/enums'
import { pageNcr } from '@/api/mes'
import { usePaged } from '@/composables/usePaged'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { hasPerm } from '@/store/user'
import { qty, shortTime } from '@/utils/format'

/** 不合格品：按状态查看，登记新的不合格；评审处置在电脑端 */
const { t } = useI18n()
useTitle('mes.entry.ncr')
const dict = useDict('qms_ncr_status', 'qms_ncr_source')
const ALL = 'all'
const status = ref(NcrStatus.OPEN)
const keyword = ref('')
const canAdd = computed(() => hasPerm('qms:ncr:add'))
const tabs = computed(() => [NcrStatus.OPEN, NcrStatus.APPROVING, NcrStatus.DONE, ALL].map((v) => ({ value: v, label: t('mes.ncr.tab.' + v) })))
const paged = usePaged((p) => pageNcr({ ...p, status: status.value === ALL ? undefined : status.value, keyword: keyword.value }))

// 登记后返回时刷新
onShow(() => paged.refresh())
onPullDownRefresh(() => paged.refresh())
onReachBottom(() => paged.more())

function open(r) {
  uni.navigateTo({ url: `/pkg-mes/ncr/detail?id=${r.id}` })
}

function add() {
  uni.navigateTo({ url: '/pkg-mes/ncr/add' })
}
</script>

<style lang="scss">
.nc__desc {
  color: $l-text-2;
  font-size: 26rpx;
}
</style>
