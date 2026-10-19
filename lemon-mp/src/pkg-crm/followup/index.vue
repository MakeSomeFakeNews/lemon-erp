<template>
  <view>
    <l-segmented v-if="tabs.length > 1" v-model="tab" :options="tabs" @change="current.refresh()" />

    <!-- 今日待跟进：我负责的、下次跟进时间在今天之前的客户 -->
    <template v-if="tab === 'today'">
      <view v-for="c in today.list" :key="c.customerId" class="l-item" @click="openCustomer(c)">
        <view class="l-item__head">
          <text class="l-item__title">{{ c.name }}</text>
          <text v-if="canAdd" class="fu__write" @click.stop="write(c)">{{ t('crm.followup.editTitle') }}</text>
        </view>
        <view class="l-item__desc">{{ t('crm.customer.nextFollow') }} {{ shortTime(c.nextFollowTime) }}</view>
        <view v-if="c.contact || c.phone" class="l-item__desc">{{ [c.contact, c.phone].filter(Boolean).join(' ') }}</view>
      </view>
      <l-empty v-if="today.loaded && !today.list.length" :text="t('crm.followup.noToday')" />
      <l-load-more v-else-if="today.list.length" :status="today.status" />
    </template>

    <!-- 我的跟进记录 -->
    <template v-else>
      <view v-for="f in mine.list" :key="f.id" class="l-item">
        <view class="l-item__head">
          <text class="l-item__title">{{ f.customerName || f.targetName }}</text>
          <l-tag :text="dictText(dict.crm_follow_method, f.method)" :color="dictColor(dict.crm_follow_method, f.method)" />
        </view>
        <view v-if="f.targetType !== FollowupTarget.CUSTOMER" class="l-item__desc">
          {{ t('crm.followup.targetType.' + f.targetType) }} · {{ f.targetName }}
        </view>
        <view class="fu__content">{{ f.content }}</view>
        <FollowupPhotos :followup-id="f.id" />
        <view class="l-item__desc">
          {{ shortTime(f.followTime) }}<text v-if="f.nextTime"> · {{ t('crm.followup.nextAt', { time: shortTime(f.nextTime) }) }}</text>
        </view>
      </view>
      <l-empty v-if="mine.loaded && !mine.list.length" />
      <l-load-more v-else-if="mine.list.length" :status="mine.status" />
    </template>

    <template v-if="canAdd">
      <view class="l-footer-space" />
      <view class="l-footer">
        <button class="l-btn" @click="write()">{{ t('crm.followup.editTitle') }}</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { FollowupTarget } from '@web/constants/enums'
import { pageCustomers, pageFollowups } from '@/api/crm'
import { usePaged } from '@/composables/usePaged'
import { useTitle } from '@/composables/useTitle'
import FollowupPhotos from '../components/FollowupPhotos.vue'
import { dictColor, dictText, useDict } from '@/store/dict'
import { hasPerm } from '@/store/user'
import { shortTime } from '@/utils/format'

const { t } = useI18n()
useTitle('crm.followup.title')
const dict = useDict('crm_follow_method')
const canAdd = hasPerm('crm:followup:add')
// 今日待跟进按客户查询，需要客户列表权限
const tabs = computed(() =>
  [hasPerm('crm:customer:list') && { label: t('crm.followup.today'), value: 'today' }, { label: t('crm.followup.mine'), value: 'mine' }].filter(Boolean)
)
const tab = ref(tabs.value[0].value)
const today = usePaged((p) => pageCustomers({ ...p, scope: 'mine', dueToday: true }))
const mine = usePaged((p) => pageFollowups({ ...p, mine: true }))
const current = computed(() => (tab.value === 'today' ? today : mine))
let shown = false

onLoad(() => current.value.refresh())
// 写完跟进返回时刷新
onShow(() => {
  if (shown) current.value.refresh()
  shown = true
})
onPullDownRefresh(() => current.value.refresh())
onReachBottom(() => current.value.more())

function openCustomer(c) {
  uni.navigateTo({ url: `/pkg-crm/customer/detail?customerId=${c.customerId}` })
}

function write(c) {
  uni.navigateTo({
    url: c
      ? `/pkg-crm/followup/edit?targetType=${FollowupTarget.CUSTOMER}&targetId=${c.customerId}&name=${encodeURIComponent(c.name || '')}`
      : '/pkg-crm/followup/edit'
  })
}
</script>

<style lang="scss">
.fu {
  &__write {
    font-size: 24rpx;
    color: $l-primary;
  }
  &__content {
    margin-top: 10rpx;
    font-size: 26rpx;
    color: $l-text-2;
    line-height: 1.6;
    white-space: pre-wrap;
  }
}
</style>
