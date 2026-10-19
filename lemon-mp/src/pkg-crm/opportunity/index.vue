<template>
  <view>
    <l-segmented v-model="scope" :options="scopes" @change="paged.refresh()" />
    <view class="l-search">
      <input v-model="keyword" :placeholder="t('crm.opportunity.searchPh')" placeholder-class="l-placeholder" confirm-type="search" @confirm="paged.refresh()" />
    </view>
    <view v-for="o in paged.list" :key="o.id" class="l-item" @click="actions(o)">
      <view class="l-item__head">
        <text class="l-item__title">{{ o.name }}</text>
        <l-tag :text="dictText(dict.crm_opp_stage, o.stage)" :color="dictColor(dict.crm_opp_stage, o.stage)" />
      </view>
      <view class="l-item__desc">{{ [o.oppNo, o.customerName, o.ownerName].filter(Boolean).join(' · ') }}</view>
      <view class="op__row">
        <text class="op__amount">{{ money(o.amount) }}</text>
        <text class="l-muted op__meta">
          {{ t('crm.opportunity.expectedDate') }} {{ o.expectedDate || '-' }}<text v-if="o.probability !== null && o.probability !== undefined"> · {{ t('crm.opportunity.probability', { n: o.probability }) }}</text>
        </text>
      </view>
      <view v-if="o.salesOrderNo" class="l-item__desc">{{ t('crm.opportunity.salesOrder', { no: o.salesOrderNo }) }}</view>
    </view>
    <l-empty v-if="paged.loaded && !paged.list.length" />
    <l-load-more v-else-if="paged.list.length" :status="paged.status" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { FollowupTarget, OpportunityStage } from '@web/constants/enums'
import { changeOpportunityStage, pageOpportunities } from '@/api/crm'
import { usePaged } from '@/composables/usePaged'
import { runAction } from '@/composables/useAction'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { hasPerm } from '@/store/user'
import { money } from '@/utils/format'

const { t } = useI18n()
useTitle('crm.opportunity.title')
const dict = useDict('crm_opp_stage')
const scope = ref('mine')
const keyword = ref('')
const scopes = computed(() => [
  { label: t('crm.scope.mine'), value: 'mine' },
  { label: t('crm.scope.all'), value: 'all' }
])
const paged = usePaged((p) => pageOpportunities({ ...p, mine: scope.value === 'mine', keyword: keyword.value.trim() }))

onLoad(() => paged.refresh())
onPullDownRefresh(() => paged.refresh())
onReachBottom(() => paged.more())

/** 点击商机：推进阶段（赢单后不能再改）、写跟进、查看客户 */
function actions(o) {
  const list = [
    hasPerm('crm:opp:edit') && o.stage !== OpportunityStage.WON && { label: t('crm.opportunity.changeStage'), run: () => chooseStage(o) },
    hasPerm('crm:followup:add') && {
      label: t('crm.followup.editTitle'),
      run: () =>
        uni.navigateTo({ url: `/pkg-crm/followup/edit?targetType=${FollowupTarget.OPPORTUNITY}&targetId=${o.id}&name=${encodeURIComponent(o.name || '')}` })
    },
    hasPerm('crm:customer:list') && o.customerId && {
      label: t('crm.customer.detailTitle'),
      run: () => uni.navigateTo({ url: `/pkg-crm/customer/detail?customerId=${o.customerId}` })
    }
  ].filter(Boolean)
  if (!list.length) return
  uni.showActionSheet({ itemList: list.map((x) => x.label), success: (r) => list[r.tapIndex].run() })
}

function chooseStage(o) {
  const stages = dict.crm_opp_stage.filter((d) => d.value !== o.stage)
  if (!stages.length) return
  uni.showActionSheet({
    itemList: stages.map((d) => d.label),
    success: (r) => {
      const stage = stages[r.tapIndex].value
      if (stage === OpportunityStage.LOST) askLostReason(o)
      else save(o, { stage })
    }
  })
}

function askLostReason(o) {
  uni.showModal({
    title: t('crm.opportunity.lostReason'),
    editable: true,
    placeholderText: t('crm.opportunity.lostReasonPh'),
    confirmText: t('common.ok'),
    cancelText: t('common.cancel'),
    success: (r) => {
      if (!r.confirm) return
      const reason = (r.content || '').trim()
      if (!reason) return uni.showToast({ title: t('crm.opportunity.lostReasonPh'), icon: 'none' })
      save(o, { stage: OpportunityStage.LOST, lostReason: reason })
    }
  })
}

function save(o, data) {
  runAction(() => changeOpportunityStage(o.id, data), { success: t('crm.opportunity.stageChanged'), then: () => paged.refresh() })
}
</script>

<style lang="scss">
.op {
  &__row {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin-top: 12rpx;
  }
  &__amount {
    font-size: 32rpx;
    font-weight: 600;
    color: $l-text;
  }
  &__meta {
    font-size: 24rpx;
  }
}
</style>
