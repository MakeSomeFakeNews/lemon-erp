<template>
  <view>
    <l-scan-input :placeholder="t('mes.equipment.scanPh')" @scan="onScan" />
    <view class="eq__filter">
      <text>{{ t('mes.equipment.dueOnly') }}</text>
      <switch :checked="dueOnly" color="#1677ff" style="transform: scale(0.8)" @change="toggleDue" />
    </view>
    <view v-for="e in paged.list" :key="e.id" class="l-item" @click="open(e.id)">
      <view class="l-item__head">
        <text class="l-item__title">{{ e.name }}</text>
        <l-tag :text="dictText(dict.mes_equipment_status, e.status)" :color="dictColor(dict.mes_equipment_status, e.status)" />
      </view>
      <view class="l-item__desc">{{ [e.code, e.model, e.workCenterName, e.location].filter(Boolean).join(' · ') }}</view>
      <view v-if="e.nextMaintainDate" class="l-item__desc">
        {{ t('mes.equipment.nextMaintain') }} {{ e.nextMaintainDate }}
        <text v-if="dueText(e)" class="eq__due">{{ dueText(e) }}</text>
      </view>
    </view>
    <l-empty v-if="paged.loaded && !paged.list.length" />
    <l-load-more v-else-if="paged.list.length" :status="paged.status" />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { pageEquipment } from '@/api/mes'
import { usePaged } from '@/composables/usePaged'
import { useTitle } from '@/composables/useTitle'
import { vibrate } from '@/composables/useAction'
import { dictColor, dictText, useDict } from '@/store/dict'
import { dueText as due } from '../mes'

/** 设备：扫描设备编码直接进入详情，或按编码、名称搜索；可只看即将到期保养的设备 */
const { t } = useI18n()
useTitle('mes.entry.equipment')
const dict = useDict('mes_equipment_status')
const keyword = ref('')
const dueOnly = ref(false)
const paged = usePaged((p) => pageEquipment({ ...p, keyword: keyword.value, dueDays: dueOnly.value ? 7 : undefined }))

onLoad((q) => {
  dueOnly.value = q.due === '1'
  paged.refresh()
})
onPullDownRefresh(() => paged.refresh())
onReachBottom(() => paged.more())

const dueText = (e) => due(e.nextMaintainDate, t)

function open(id) {
  uni.navigateTo({ url: `/pkg-mes/equipment/detail?id=${id}` })
}

/** 扫码：编码完全一致时直接进入详情，否则按关键字列出 */
async function onScan(code) {
  keyword.value = code
  await paged.refresh()
  const exact = paged.list.find((e) => e.code === code)
  if (exact) {
    vibrate(true)
    open(exact.id)
  } else if (!paged.list.length) {
    vibrate(false)
    uni.showToast({ title: t('mes.equipment.notFound', { code }), icon: 'none' })
  }
}

function toggleDue(e) {
  dueOnly.value = e.detail.value
  paged.refresh()
}
</script>

<style lang="scss">
.eq {
  &__filter {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 8rpx;
    padding: 0 24rpx;
    font-size: 26rpx;
    color: $l-text-2;
  }
  &__due {
    margin-left: 12rpx;
    color: $l-danger;
  }
}
</style>
