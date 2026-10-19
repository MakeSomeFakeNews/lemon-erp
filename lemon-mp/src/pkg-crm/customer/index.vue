<template>
  <view>
    <l-segmented v-model="scope" :options="scopes" @change="paged.refresh()" />
    <view class="l-search">
      <input v-model="keyword" :placeholder="t('crm.customer.searchPh')" placeholder-class="l-placeholder" confirm-type="search" @confirm="paged.refresh()" />
    </view>
    <view v-for="c in paged.list" :key="c.customerId" class="l-item" @click="open(c)">
      <view class="l-item__head">
        <text class="l-item__title">{{ c.name }}</text>
        <l-tag v-if="c.stage" :text="dictText(dict.crm_customer_stage, c.stage)" :color="dictColor(dict.crm_customer_stage, c.stage)" />
      </view>
      <view class="l-item__desc">
        {{ [c.code, dictText(dict.scm_customer_level, c.level), c.ownerName].filter(Boolean).join(' · ') }}
      </view>
      <view v-if="c.contact || c.phone" class="l-item__desc">{{ [c.contact, c.phone].filter(Boolean).join(' ') }}</view>
      <view v-if="c.nextFollowTime" class="l-item__desc">{{ t('crm.customer.nextFollow') }} {{ shortTime(c.nextFollowTime) }}</view>
    </view>
    <l-empty v-if="paged.loaded && !paged.list.length" />
    <l-load-more v-else-if="paged.list.length" :status="paged.status" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { pageCustomers } from '@/api/crm'
import { usePaged } from '@/composables/usePaged'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { shortTime } from '@/utils/format'

const { t } = useI18n()
useTitle('crm.customer.title')
const dict = useDict('crm_customer_stage', 'scm_customer_level')
const scope = ref('mine')
const keyword = ref('')
const scopes = computed(() => [
  { label: t('crm.scope.mine'), value: 'mine' },
  { label: t('crm.scope.team'), value: 'team' }
])
const paged = usePaged((p) => pageCustomers({ ...p, scope: scope.value, keyword: keyword.value.trim() }))

onLoad(() => paged.refresh())
onPullDownRefresh(() => paged.refresh())
onReachBottom(() => paged.more())

function open(c) {
  uni.navigateTo({ url: `/pkg-crm/customer/detail?customerId=${c.customerId}` })
}
</script>
