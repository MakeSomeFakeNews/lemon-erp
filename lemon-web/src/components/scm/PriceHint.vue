<template>
  <div v-if="listPrice != null" class="price-hint">
    <span class="sub-text">{{ $t('scm.priceHint.listPrice', { price: symbol + formatPrice(listPrice) }) }}</span>
    <a-tag v-if="discount > 0" color="warning" :bordered="false" class="discount">{{ $t('scm.priceHint.discount', { n: discount }) }}</a-tag>
  </div>
</template>

<script setup>
import { computed } from 'vue'

/** 销售单价与价目价的对比：低于价目价时提示折扣比例 */
const props = defineProps({
  price: [Number, String],
  listPrice: [Number, String],
  /** 币种符号（外币订单时价目价已折算为外币） */
  symbol: { type: String, default: '¥' }
})

const formatPrice = (n) => Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 4 })

/** 折扣 = (价目价 - 单价) / 价目价，保留 1 位小数 */
const discount = computed(() => {
  const list = Number(props.listPrice)
  if (props.price == null || props.price === '' || !list) return 0
  const d = ((list - Number(props.price)) / list) * 100
  return d > 0 ? Math.round(d * 10) / 10 : 0
})
</script>

<style scoped>
.price-hint {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  margin-top: 2px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.discount {
  margin: 0;
}
</style>
