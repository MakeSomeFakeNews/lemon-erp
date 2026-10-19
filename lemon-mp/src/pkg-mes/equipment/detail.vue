<template>
  <view v-if="eq">
    <view class="eqd__head">
      <view class="eqd__name">{{ eq.name }}</view>
      <view class="eqd__sub">
        <text>{{ eq.code }}</text>
        <l-tag :text="dictText(dict.mes_equipment_status, eq.status)" :color="dictColor(dict.mes_equipment_status, eq.status)" />
      </view>
    </view>
    <l-card>
      <l-cell :label="t('mes.equipment.model')" :value="eq.model" />
      <l-cell :label="t('mes.equipment.workCenter')" :value="eq.workCenterName" />
      <l-cell :label="t('mes.equipment.location')" :value="eq.location" />
      <l-cell :label="t('mes.equipment.purchaseDate')" :value="eq.purchaseDate" />
      <l-cell :label="t('mes.equipment.cycle')" :value="eq.maintainCycleDays ? t('mes.equipment.cycleDays', { n: eq.maintainCycleDays }) : ''" />
      <l-cell :label="t('mes.equipment.lastMaintain')" :value="eq.lastMaintainDate" />
      <l-cell :label="t('mes.equipment.nextMaintain')">
        <text>{{ eq.nextMaintainDate || '-' }}</text>
        <text v-if="due" class="eqd__due">{{ due }}</text>
      </l-cell>
      <l-cell v-if="eq.remark" :label="t('common.remark')" :value="eq.remark" />
    </l-card>

    <l-card v-if="canListMaintain" :title="t('mes.equipment.records')">
      <view v-if="!records.length" class="l-muted eqd__none">{{ t('mes.equipment.noRecords') }}</view>
      <view v-for="m in records" :key="m.id" class="eqd__rec" @click="openRecord(m)">
        <view class="eqd__rec-head">
          <text class="eqd__rec-title">{{ dictText(dict.mes_maintain_type, m.type) }} · {{ m.orderNo }}</text>
          <l-tag :text="dictText(dict.mes_maintain_status, m.status)" :color="dictColor(dict.mes_maintain_status, m.status)" />
        </view>
        <view class="eqd__rec-desc">{{ m.content }}</view>
        <view class="eqd__rec-desc l-muted">{{ shortTime(m.finishTime || m.startTime || m.createTime) }} {{ m.handler || m.createByName || '' }}</view>
      </view>
    </l-card>

    <template v-if="canAdd && eq.status !== EquipmentStatus.SCRAPPED">
      <view class="l-footer-space" />
      <view class="l-footer">
        <button class="l-btn l-btn--ghost" @click="add(MaintenanceType.MAINTAIN)">{{ t('mes.equipment.maintain') }}</button>
        <button class="l-btn l-btn--danger" @click="add(MaintenanceType.REPAIR)">{{ t('mes.equipment.report') }}</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { EquipmentStatus, MaintenanceType } from '@web/constants/enums'
import { getEquipment, pageMaintenance } from '@/api/mes'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { hasPerm } from '@/store/user'
import { shortTime } from '@/utils/format'
import { dueText } from '../mes'

/** 设备详情：台账信息、最近的保养维修记录；可报修、登记保养 */
const { t } = useI18n()
useTitle('mes.entry.equipment')
const dict = useDict('mes_equipment_status', 'mes_maintain_type', 'mes_maintain_status')
const id = ref(null)
const eq = ref(null)
const records = ref([])
const canListMaintain = computed(() => hasPerm('mes:maintain:list'))
const canAdd = computed(() => hasPerm('mes:maintain:add'))
const due = computed(() => (eq.value ? dueText(eq.value.nextMaintainDate, t) : ''))

onLoad((q) => {
  id.value = q.id
})
// 报修、处理后返回时刷新
onShow(() => id.value && load())

async function load() {
  eq.value = await getEquipment(id.value).catch(() => null)
  if (canListMaintain.value) {
    const page = await pageMaintenance({ equipmentId: id.value, current: 1, size: 10 }).catch(() => null)
    records.value = page?.records || []
  }
}

function add(type) {
  uni.navigateTo({ url: `/pkg-mes/maintenance/add?equipmentId=${id.value}&type=${type}` })
}

function openRecord(m) {
  uni.navigateTo({ url: `/pkg-mes/maintenance/detail?id=${m.id}&no=${encodeURIComponent(m.orderNo)}` })
}
</script>

<style lang="scss">
.eqd {
  &__head {
    padding: 32rpx 32rpx 0;
  }
  &__name {
    font-size: 38rpx;
    font-weight: 600;
  }
  &__sub {
    display: flex;
    align-items: center;
    gap: 16rpx;
    margin-top: 8rpx;
    font-size: 26rpx;
    color: $l-text-3;
  }
  &__due {
    margin-left: 12rpx;
    color: $l-danger;
  }
  &__none {
    padding: 8rpx 0 16rpx;
    font-size: 26rpx;
  }
  &__rec {
    padding: 20rpx 0;
    border-bottom: 1rpx solid $l-border;
    &:last-child {
      border-bottom: none;
    }
  }
  &__rec-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16rpx;
  }
  &__rec-title {
    font-size: 28rpx;
    font-weight: 500;
  }
  &__rec-desc {
    margin-top: 8rpx;
    font-size: 24rpx;
    color: $l-text-2;
  }
}
</style>
