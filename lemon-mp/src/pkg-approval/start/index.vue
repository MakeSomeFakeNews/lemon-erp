<template>
  <view>
    <view class="l-search">
      <input v-model="keyword" :placeholder="t('approval.start.searchPh')" placeholder-class="l-placeholder" confirm-type="search" />
    </view>
    <l-card v-for="g in groups" :key="g.value" :title="g.label">
      <view v-for="m in g.items" :key="m.modelKey" class="st__item" @click="open(m)">
        <view class="st__icon">{{ (m.name || '').slice(0, 1) }}</view>
        <view class="st__text">
          <view class="st__name">{{ m.name }}</view>
          <view class="st__remark">{{ m.remark || t('approval.start.clickToStart') }}</view>
        </view>
        <text class="st__arrow">›</text>
      </view>
    </l-card>
    <l-empty v-if="loaded && !groups.length" :text="keyword.trim() ? t('approval.start.noMatch') : t('approval.start.empty')" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { listStartModels } from '@/api/workflow'
import { useTitle } from '@/composables/useTitle'
import { useDict } from '@/store/dict'
import { ensureLogin } from '@/utils/session'

const { t } = useI18n()
useTitle('approval.start.title')
const dict = useDict('wf_category')
const models = ref([])
const keyword = ref('')
const loaded = ref(false)

/** 按分类分组，分类顺序与字典一致，未分类的放最后（与 Web 端一致） */
const groups = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const filtered = models.value.filter((m) => !kw || (m.name || '').toLowerCase().includes(kw))
  const result = dict.wf_category.map((d) => ({ value: d.value, label: d.label, items: [] }))
  const other = { value: '__other', label: t('approval.start.other'), items: [] }
  filtered.forEach((m) => (result.find((g) => g.value === m.category) || other).items.push(m))
  return [...result, other].filter((g) => g.items.length)
})

async function load() {
  try {
    models.value = (await listStartModels()) || []
  } catch {
    // 错误已提示
  } finally {
    loaded.value = true
    uni.stopPullDownRefresh()
  }
}

onLoad(async () => {
  if (await ensureLogin()) load()
})
onPullDownRefresh(load)

function open(m) {
  uni.navigateTo({ url: `/pkg-approval/start/form?key=${encodeURIComponent(m.modelKey)}` })
}
</script>

<style lang="scss">
.st {
  &__item {
    display: flex;
    align-items: center;
    gap: 20rpx;
    padding: 20rpx 0;
    border-bottom: 1rpx solid $l-border;
    &:last-child {
      border-bottom: none;
    }
  }
  &__icon {
    flex: none;
    width: 72rpx;
    height: 72rpx;
    border-radius: 18rpx;
    line-height: 72rpx;
    text-align: center;
    font-size: 30rpx;
    font-weight: 600;
    color: $l-primary;
    background: #e6f4ff;
  }
  &__text {
    flex: 1;
    min-width: 0;
  }
  &__name {
    font-size: 30rpx;
    font-weight: 500;
  }
  &__remark {
    margin-top: 6rpx;
    font-size: 24rpx;
    color: $l-text-3;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
  &__arrow {
    color: #bfbfbf;
    font-size: 32rpx;
  }
}
</style>
