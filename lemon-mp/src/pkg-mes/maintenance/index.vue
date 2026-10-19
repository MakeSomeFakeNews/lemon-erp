<template>
  <view>
    <l-segmented v-model="status" :options="tabs" @change="paged.refresh()" />
    <view class="l-search">
      <input v-model="keyword" :placeholder="t('mes.maintenance.searchPh')" placeholder-class="l-placeholder" confirm-type="search" @confirm="paged.refresh()" />
    </view>
    <view v-for="m in paged.list" :key="m.id" class="l-item" @click="open(m)">
      <view class="l-item__head">
        <text class="l-item__title">{{ m.equipmentName }}</text>
        <l-tag :text="dictText(dict.mes_maintain_type, m.type)" :color="m.type === MaintenanceType.REPAIR ? 'red' : 'blue'" />
      </view>
      <view class="l-item__desc mt__content">{{ m.content }}</view>
      <view class="l-item__desc">{{ [m.orderNo, m.equipmentCode].filter(Boolean).join(' · ') }}</view>
      <view class="l-item__desc">
        <text v-if="m.status === MaintenanceStatus.PENDING">{{ t('mes.maintenance.planDate') }} {{ m.planDate || '-' }} · {{ m.createByName }}</text>
        <text v-else-if="m.status === MaintenanceStatus.IN_PROGRESS">{{ t('mes.maintenance.startTime') }} {{ shortTime(m.startTime) }} · {{ m.handler || '-' }}</text>
        <text v-else>{{ t('mes.maintenance.finishTime') }} {{ shortTime(m.finishTime) }} · {{ m.handler || '-' }}</text>
      </view>
    </view>
    <l-empty v-if="paged.loaded && !paged.list.length" />
    <l-load-more v-else-if="paged.list.length" :status="paged.status" />

    <template v-if="canAdd">
      <view class="l-footer-space" />
      <view class="l-footer">
        <button class="l-btn" @click="add">{{ t('mes.maintenance.addTitle') }}</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { MaintenanceStatus, MaintenanceType } from '@web/constants/enums'
import { pageMaintenance } from '@/api/mes'
import { usePaged } from '@/composables/usePaged'
import { useTitle } from '@/composables/useTitle'
import { dictText, useDict } from '@/store/dict'
import { hasPerm } from '@/store/user'
import { shortTime } from '@/utils/format'

/** 保养维修任务：待处理 / 处理中 / 已完成 */
const { t } = useI18n()
useTitle('mes.entry.maintenance')
const dict = useDict('mes_maintain_type')
const status = ref(MaintenanceStatus.PENDING)
const keyword = ref('')
const canAdd = computed(() => hasPerm('mes:maintain:add'))
const tabs = computed(() =>
  [MaintenanceStatus.PENDING, MaintenanceStatus.IN_PROGRESS, MaintenanceStatus.DONE].map((v) => ({ value: v, label: t('mes.maintenance.tab.' + v) }))
)
const paged = usePaged((p) => pageMaintenance({ ...p, status: status.value, keyword: keyword.value }))

onLoad((q) => {
  if (q.status && tabs.value.some((x) => x.value === q.status)) status.value = q.status
})
// 处理后返回时刷新
onShow(() => paged.refresh())
onPullDownRefresh(() => paged.refresh())
onReachBottom(() => paged.more())

function open(m) {
  uni.navigateTo({ url: `/pkg-mes/maintenance/detail?id=${m.id}&no=${encodeURIComponent(m.orderNo)}` })
}

function add() {
  uni.navigateTo({ url: '/pkg-mes/maintenance/add' })
}
</script>

<style lang="scss">
.mt__content {
  color: $l-text-2;
  font-size: 26rpx;
}
</style>
