<template>
  <view v-if="visible" class="ss-mask" @click.self="visible = false">
    <view class="ss-panel ls-panel">
      <view class="ss-title">{{ t('lookup.setting.title') }}</view>
      <view class="ls-tip l-muted">{{ t('lookup.setting.tip') }}</view>
      <scroll-view scroll-y class="ss-list">
        <view v-for="g in groups" :key="g.key">
          <view class="ls-group">{{ g.title }}</view>
          <view
            v-for="o in g.options"
            :key="o.type"
            class="ss-row ls-row"
            :class="{ 'ls-row--disabled': !o.allowed, 'ls-row--active': o.type === current }"
            @click="choose(o)"
          >
            <text class="ss-name">{{ o.label }}</text>
            <text v-if="!o.allowed" class="ls-flag">{{ t('lookup.setting.noPermission') }}</text>
            <text v-else-if="o.type === current" class="ls-check">✓</text>
          </view>
        </view>
      </scroll-view>
      <view class="ss-cancel" @click="visible = false">{{ t('common.cancel') }}</view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from '@/locales'
import { QuickPrice } from '@web/constants/enums'
import { saveLookupSetting } from '@/api/lookup'

/**
 * 「查询价格」设置：按进货类 / 成本 / 销售类分组列出后端返回的可选价格（停用的价格列不列出），没有权限的置灰不可选；
 * 选择后保存到个人设置 quickLookup（与 Web 端共用），emit change(type)。用法：ref.open(options, current)
 */
const emit = defineEmits(['change'])
const { t } = useI18n()
const visible = ref(false)
const options = ref([])
const current = ref(null)

const GROUPS = [
  { key: 'PURCHASE', title: 'lookup.setting.purchase' },
  { key: 'COST', title: 'lookup.setting.cost' },
  { key: 'SALES', title: 'lookup.setting.sales' },
  { key: null, title: 'lookup.setting.none' }
]
const groups = computed(() =>
  GROUPS.map((g) => ({ key: g.key || 'none', title: t(g.title), options: options.value.filter((o) => (o.side || null) === g.key) })).filter(
    (g) => g.options.length
  )
)

async function choose(o) {
  if (!o.allowed || o.type === current.value) return
  await saveLookupSetting({ price: o.type })
  current.value = o.type
  uni.showToast({ title: t('lookup.setting.saved'), icon: 'none' })
  visible.value = false
  emit('change', o.type)
}

defineExpose({
  open(list, value) {
    options.value = list || []
    current.value = value || QuickPrice.LAST_SALES
    visible.value = true
  }
})
</script>

<style lang="scss">
@import '../sheet.scss';

.ls-panel {
  height: 70vh;
}
.ls-tip {
  padding: 8rpx 32rpx 16rpx;
  font-size: 24rpx;
}
.ls-group {
  padding: 16rpx 32rpx 12rpx;
  font-size: 26rpx;
  font-weight: 600;
  color: $l-text-2;
}
.ls-row {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  border: 2rpx solid transparent;
  &--active {
    border-color: $l-primary;
  }
  &--disabled .ss-name {
    color: $l-text-3;
  }
}
.ls-flag {
  font-size: 24rpx;
  color: $l-text-3;
}
.ls-check {
  color: $l-primary;
  font-weight: 600;
}
</style>
