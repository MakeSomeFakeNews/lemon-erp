<template>
  <view v-if="open" class="ss-mask" @click.self="emit('close')">
    <view class="ss-panel">
      <view v-if="title" class="ss-title">{{ title }}</view>
      <view class="l-search ss-search">
        <input
          v-model="keyword"
          :placeholder="placeholder || t('common.searchPlaceholder')"
          placeholder-class="l-placeholder"
          confirm-type="search"
          focus
          @confirm="emit('search', keyword.trim())"
        />
        <text class="ss-btn" @click="emit('search', keyword.trim())">{{ t('common.search') }}</text>
      </view>
      <scroll-view scroll-y class="ss-list">
        <view v-if="emptyOption" class="ss-row" @click="emit('select', null)">
          <text class="ss-name">{{ emptyOption }}</text>
        </view>
        <view v-for="it in items" :key="it.id" class="ss-row" @click="emit('select', it)">
          <text class="ss-name">{{ it.title }}</text>
          <text v-if="it.sub" class="ss-sub">{{ it.sub }}</text>
        </view>
        <view v-if="loading" class="ss-loading l-muted">{{ t('common.loading') }}</view>
        <l-empty v-else-if="!items.length && !emptyOption" />
      </scroll-view>
      <view class="ss-cancel" @click="emit('close')">{{ t('common.cancel') }}</view>
    </view>
  </view>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useI18n } from '@/locales'

/**
 * 底部搜索选择弹层（客户、业务员、商品等）：输入关键字后由父组件查询并传入 items [{ id, title, sub? }]。
 * 打开时自动触发一次 search('')（关键字为 initial 时用它）；emptyOption 为第一项的文字（选择后 select(null)，如「默认」）
 */
const props = defineProps({
  open: Boolean,
  title: String,
  placeholder: String,
  items: { type: Array, default: () => [] },
  loading: Boolean,
  emptyOption: String,
  /** 打开时的初始关键字 */
  initial: { type: String, default: '' }
})
const emit = defineEmits(['search', 'select', 'close'])
const { t } = useI18n()
const keyword = ref('')

watch(
  () => props.open,
  (v) => {
    if (!v) return
    keyword.value = props.initial
    emit('search', props.initial)
  }
)
</script>

<style lang="scss">
@import '../sheet.scss';
</style>
