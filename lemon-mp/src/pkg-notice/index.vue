<template>
  <view>
    <l-segmented v-model="tab" :options="tabs" class="nt__seg" @change="paged.refresh()" />
    <view v-for="n in paged.list" :key="n.id" class="l-item" @click="open(n)">
      <view class="l-item__head">
        <view v-if="!n.read" class="nt__dot" />
        <text :class="['l-item__title', { nt__unread: !n.read }]">{{ n.title }}</text>
        <l-tag v-if="n.pinned" :text="t('notice.pinned')" color="red" />
        <l-tag v-if="n.type" :text="dictText(dict.sys_notice_type, n.type)" :color="dictColor(dict.sys_notice_type, n.type)" />
      </view>
      <view class="l-item__desc">{{ [shortTime(n.publishTime), n.publishByName].filter(Boolean).join(' · ') }}</view>
    </view>
    <l-empty v-if="paged.loaded && !paged.list.length" />
    <l-load-more v-else-if="paged.list.length" :status="paged.status" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { pageNotices } from '@/api/notice'
import { usePaged } from '@/composables/usePaged'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { shortTime } from '@/utils/format'

/** 通知公告：发给本人、已发布且未过期的公告，置顶在前；从站内消息进入（?id=）时直接打开该公告 */
const { t } = useI18n()
useTitle('notice.title')
const dict = useDict('sys_notice_type')
const tab = ref('all')
const tabs = computed(() => [
  { label: t('notice.all'), value: 'all' },
  { label: t('notice.unread'), value: 'unread' }
])
const paged = usePaged((p) => pageNotices({ ...p, unread: tab.value === 'unread' }))

const detailUrl = (id) => `/pkg-notice/detail?id=${id}`
const open = (n) => uni.navigateTo({ url: detailUrl(n.id) })

onLoad((query) => {
  if (query?.id) uni.navigateTo({ url: detailUrl(query.id) })
})
// 从详情返回时刷新未读标记
onShow(() => paged.refresh())
onPullDownRefresh(() => paged.refresh())
onReachBottom(() => paged.more())
</script>

<style lang="scss">
.nt {
  &__seg {
    position: sticky;
    top: 0;
    z-index: 5;
  }
  &__dot {
    width: 14rpx;
    height: 14rpx;
    border-radius: 50%;
    background: $l-danger;
  }
  &__unread {
    font-weight: 600;
  }
}
</style>
