<template>
  <view>
    <template v-if="d">
      <view class="nd__head">
        <view class="nd__title">{{ d.materialName }}</view>
        <view class="nd__sub">
          <text>{{ d.ncrNo }}</text>
          <l-tag :text="dictText(dict.qms_ncr_status, d.status)" :color="dictColor(dict.qms_ncr_status, d.status)" />
        </view>
      </view>
      <l-card>
        <l-cell :label="t('mes.ncr.source')" :value="dictText(dict.qms_ncr_source, d.source)" />
        <l-cell v-if="d.inspNo" :label="t('mes.ncr.inspection')" :value="d.inspNo" />
        <l-cell :label="t('mes.common.material')" :value="[d.materialCode, d.spec].filter(Boolean).join(' · ')" />
        <l-cell :label="t('mes.common.batchNo')" :value="d.batchNo" />
        <l-cell :label="t('mes.common.qty')" :value="qty(d.qty) + ' ' + (d.unit || '')" />
        <l-cell v-if="d.woNo" :label="t('mes.common.woNo')" :value="d.woNo" />
        <l-cell v-if="d.supplierName" :label="t('mes.ncr.supplier')" :value="d.supplierName" />
        <l-cell :label="t('mes.ncr.defectType')" :value="dictText(dict.mes_defect_type, d.defectType)" />
        <l-cell :label="t('mes.ncr.description')" :value="d.description" />
        <l-cell :label="t('mes.ncr.createBy')" :value="[d.createByName, shortTime(d.createTime)].filter(Boolean).join(' ')" />
      </l-card>
      <l-card v-if="d.disposition || d.reviewOpinion || d.resultNote">
        <l-cell :label="t('mes.ncr.disposition')" :value="dictText(dict.qms_disposition, d.disposition)" />
        <l-cell v-if="d.responsibility" :label="t('mes.ncr.responsibility')" :value="dictText(dict.qms_responsibility, d.responsibility)" />
        <l-cell v-if="d.reviewOpinion" :label="t('mes.ncr.reviewOpinion')" :value="d.reviewOpinion" />
        <l-cell v-if="d.reworkWoNo" :label="t('mes.ncr.reworkWo')" :value="d.reworkWoNo" />
        <l-cell v-if="d.resultNote" :label="t('mes.ncr.resultNote')" :value="d.resultNote" />
      </l-card>
      <view v-if="d.status === NcrStatus.OPEN" class="nd__tip">{{ t('mes.ncr.reviewOnPc') }}</view>
    </template>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { NcrStatus } from '@web/constants/enums'
import { getNcr } from '@/api/mes'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { qty, shortTime } from '@/utils/format'

/** 不合格品详情（只读）：评审处置在电脑端 */
const { t } = useI18n()
useTitle('mes.ncr.detailTitle')
const dict = useDict('qms_ncr_status', 'qms_ncr_source', 'mes_defect_type', 'qms_disposition', 'qms_responsibility')
const d = ref(null)

onLoad(async (q) => {
  d.value = await getNcr(q.id).catch(() => null)
})
</script>

<style lang="scss">
.nd {
  &__head {
    padding: 32rpx 32rpx 0;
  }
  &__title {
    font-size: 36rpx;
    font-weight: 600;
  }
  &__sub {
    display: flex;
    align-items: center;
    gap: 12rpx;
    margin-top: 8rpx;
    font-size: 26rpx;
    color: $l-text-3;
  }
  &__tip {
    margin: 0 32rpx;
    font-size: 24rpx;
    color: $l-text-3;
  }
}
</style>
