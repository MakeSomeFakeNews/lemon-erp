<template>
  <view>
    <l-segmented v-model="type" :options="tabs" @change="paged.refresh()" />
    <l-scan-input :placeholder="t('mes.inspection.searchPh')" @scan="onScan" />
    <view v-if="keyword" class="iq__keyword">
      <text>{{ keyword }}</text>
      <text class="iq__clear" @click="clearKeyword">×</text>
    </view>
    <view v-if="draftOnly" class="iq__keyword">
      <text>{{ t('mes.inspection.draftOnly') }}</text>
      <text class="iq__clear" @click="clearDraft">×</text>
    </view>
    <view v-for="r in paged.list" :key="r.id" class="l-item" @click="open(r)">
      <view class="l-item__head">
        <text class="l-item__title">{{ r.materialName }}</text>
        <l-tag v-if="r.status !== InspectionStatus.CONFIRMED" :text="t('mes.inspection.draft')" color="orange" />
        <l-tag v-else :text="dictText(dict.mes_qc_result, r.result)" :color="dictColor(dict.mes_qc_result, r.result)" />
      </view>
      <view class="l-item__desc">{{ dictText(dict.mes_qc_type, r.type) }} · {{ r.inspNo }}</view>
      <view class="l-item__desc">{{ [r.materialCode, r.batchNo, r.sourceNo, r.supplierName].filter(Boolean).join(' · ') }}</view>
      <view class="l-item__desc">
        {{ t('mes.inspection.qtyLine', { inspect: qty(r.inspectQty), sample: qty(r.sampleQty), defect: qty(r.defectQty) }) }} · {{ r.inspectDate }}
      </view>
    </view>
    <l-empty v-if="paged.loaded && !paged.list.length" />
    <l-load-more v-else-if="paged.list.length" :status="paged.status" />

    <template v-if="canAdd">
      <view class="l-footer-space" />
      <view class="l-footer">
        <button class="l-btn" @click="add">{{ t('mes.inspection.add') }}</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { InspectionStatus } from '@web/constants/enums'
import { pageInspection } from '@/api/mes'
import { usePaged } from '@/composables/usePaged'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { hasPerm } from '@/store/user'
import { qty } from '@/utils/format'

/** 质量检验：按类型查看检验单（草稿为待检），扫描或输入单号、来源单号、物料查找；新增检验 */
const { t } = useI18n()
useTitle('mes.entry.inspection')
const dict = useDict('mes_qc_type', 'mes_qc_result')
const ALL = ''
const type = ref(ALL)
const keyword = ref('')
/** 只看草稿（待确认）：首页待办「检验单待确认」带 status=DRAFT */
const draftOnly = ref(false)
const canAdd = computed(() => hasPerm('mes:qc:add'))
const tabs = computed(() => [{ value: ALL, label: t('common.all') }, ...dict.mes_qc_type.map((d) => ({ value: d.value, label: d.label }))])
const paged = usePaged((p) => pageInspection({ ...p, type: type.value || undefined, status: draftOnly.value ? InspectionStatus.DRAFT : undefined, keyword: keyword.value }))

onLoad((q) => {
  draftOnly.value = q?.status === InspectionStatus.DRAFT
})
// 录入、确认后返回时刷新
onShow(() => paged.refresh())
onPullDownRefresh(() => paged.refresh())
onReachBottom(() => paged.more())

function onScan(code) {
  keyword.value = code
  paged.refresh()
}

function clearDraft() {
  draftOnly.value = false
  paged.refresh()
}

function clearKeyword() {
  keyword.value = ''
  paged.refresh()
}

function open(r) {
  uni.navigateTo({ url: `/pkg-mes/inspection/edit?id=${r.id}` })
}

function add() {
  uni.navigateTo({ url: `/pkg-mes/inspection/edit${type.value ? '?type=' + type.value : ''}` })
}
</script>

<style lang="scss">
.iq {
  &__keyword {
    display: flex;
    align-items: center;
    gap: 12rpx;
    margin: -8rpx 24rpx 8rpx;
    font-size: 24rpx;
    color: $l-text-2;
  }
  &__clear {
    padding: 0 12rpx;
    font-size: 32rpx;
    color: $l-text-3;
  }
}
</style>
