<template>
  <view>
    <l-segmented v-model="scope" :options="scopes" @change="paged.refresh()" />
    <view class="l-search">
      <input v-model="keyword" :placeholder="t('crm.lead.searchPh')" placeholder-class="l-placeholder" confirm-type="search" @confirm="paged.refresh()" />
    </view>
    <view v-for="l in paged.list" :key="l.id" class="l-item" @click="open(l)">
      <view class="l-item__head">
        <text class="l-item__title">{{ l.company }}</text>
        <l-tag :text="dictText(dict.crm_lead_status, l.status)" :color="dictColor(dict.crm_lead_status, l.status)" />
      </view>
      <view class="l-item__desc">{{ [l.contactName, l.phone].filter(Boolean).join(' ') || '-' }}</view>
      <view class="l-item__desc">
        {{ [dictText(dict.crm_source, l.source), l.ownerName, l.lastFollowTime ? t('crm.lead.lastFollow', { time: shortTime(l.lastFollowTime) }) : ''].filter(Boolean).join(' · ') }}
      </view>
    </view>
    <l-empty v-if="paged.loaded && !paged.list.length" />
    <l-load-more v-else-if="paged.list.length" :status="paged.status" />

    <template v-if="canAdd">
      <view class="l-footer-space" />
      <view class="l-footer">
        <button class="l-btn" @click="add">{{ t('crm.lead.add') }}</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { pageLeads } from '@/api/crm'
import { usePaged } from '@/composables/usePaged'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { hasPerm } from '@/store/user'
import { shortTime } from '@/utils/format'

const { t } = useI18n()
useTitle('crm.lead.title')
const dict = useDict('crm_lead_status', 'crm_source')
const canAdd = computed(() => hasPerm('crm:lead:add'))
const scope = ref('mine')
const scopes = computed(() => [
  { label: t('crm.scope.mine'), value: 'mine' },
  { label: t('crm.scope.all'), value: 'all' }
])
const keyword = ref('')
const paged = usePaged((p) => pageLeads({ ...p, mine: scope.value === 'mine', keyword: keyword.value.trim() }))
let shown = false

onLoad(() => paged.refresh())
// 新建、转化后返回时刷新
onShow(() => {
  if (shown) paged.refresh()
  shown = true
})
onPullDownRefresh(() => paged.refresh())
onReachBottom(() => paged.more())

function open(l) {
  uni.navigateTo({ url: `/pkg-crm/lead/detail?id=${l.id}` })
}

function add() {
  uni.navigateTo({ url: '/pkg-crm/lead/edit' })
}
</script>
