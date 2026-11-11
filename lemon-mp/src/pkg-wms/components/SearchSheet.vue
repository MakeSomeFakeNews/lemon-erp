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
          @input="onInput"
          @confirm="searchNow"
        />
        <text class="ss-btn" @click="searchNow">{{ t('common.search') }}</text>
      </view>
      <scroll-view scroll-y class="ss-list" @scrolltolower="onMore">
        <view v-if="emptyOption" class="ss-row" @click="emit('select', null)">
          <text class="ss-name">{{ emptyOption }}</text>
        </view>
        <view v-for="it in items" :key="it.id" class="ss-row" @click="emit('select', it)">
          <text class="ss-name">{{ it.title }}</text>
          <text v-if="it.sub" class="ss-sub">{{ it.sub }}</text>
        </view>
        <view v-if="loading" class="ss-loading l-muted">{{ t('common.loading') }}</view>
        <l-empty v-else-if="!items.length && !emptyOption" />
        <view v-else-if="hasMore" class="ss-loading l-muted" @click="onMore">{{ t('common.moreHint') }}</view>
      </scroll-view>
      <view class="ss-cancel" @click="emit('close')">{{ t('common.cancel') }}</view>
    </view>
  </view>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from '@/locales'

/**
 * 底部搜索选择弹层（客户、业务员、商品等）：输入关键字后由父组件查询并传入 items [{ id, title, sub? }]。
 * 打开时自动触发一次 search('')（关键字为 initial 时用它），输入时防抖触发 search；emptyOption 为第一项的文字（选择后 select(null)，如「默认」）。
 * 分页：hasMore 为 true 时滚动到底部触发 more（父组件按当前关键字加载下一页并追加到 items）
 */
const props = defineProps({
  open: Boolean,
  title: String,
  placeholder: String,
  items: { type: Array, default: () => [] },
  loading: Boolean,
  emptyOption: String,
  /** 打开时的初始关键字 */
  initial: { type: String, default: '' },
  /** 还有下一页 */
  hasMore: Boolean
})
const emit = defineEmits(['search', 'select', 'close', 'more'])
const { t } = useI18n()
const keyword = ref('')
let timer

function searchNow() {
  clearTimeout(timer)
  emit('search', keyword.value.trim())
}

function onInput(e) {
  const value = e?.detail?.value ?? keyword.value
  clearTimeout(timer)
  timer = setTimeout(() => emit('search', String(value).trim()), 400)
}

function onMore() {
  if (props.hasMore && !props.loading) emit('more')
}

onBeforeUnmount(() => clearTimeout(timer))

watch(
  () => props.open,
  (v) => {
    if (!v) return clearTimeout(timer)
    keyword.value = props.initial
    emit('search', props.initial)
  }
)
</script>

<style lang="scss">
@import '../sheet.scss';
</style>
