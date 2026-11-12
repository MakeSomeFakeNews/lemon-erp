<template>
  <!-- 调拨单表单明细的价格列（只给有「查看成本金额」权限的人）：调出成本价（草稿显示当前均价，确认时生成）、调拨价、调拨金额 -->
  <template v-if="columnKey === 'outCost'">
    <span>{{ row.currentCost == null ? '-' : price(row.currentCost) }}</span>
    <div class="sub-text">{{ $t('wms.transfer.costPriceTip') }}</div>
  </template>
  <template v-else-if="columnKey === 'transferPrice'">
    <a-input-number
      v-if="differential"
      v-model:value="row.transferPrice"
      :min="0"
      :max="999999999999"
      :precision="4"
      class="full"
      :placeholder="$t('wms.transfer.transferPricePh')"
    />
    <span v-else class="sub-text">{{ $t('wms.transfer.sameAsCost') }}</span>
  </template>
  <span v-else>{{ amount == null ? '-' : money(amount) }}</span>
</template>

<script setup>
import { computed } from 'vue'
import { money, price, round2 } from '@/utils/number'

const props = defineProps({
  /** outCost / transferPrice / transferAmount */
  columnKey: { type: String, required: true },
  row: { type: Object, required: true },
  differential: Boolean
})

/** 调拨金额（预估）：差价调拨按填写的调拨价，不填或同价调拨按当前均价 */
const amount = computed(() => {
  const p = props.differential && props.row.transferPrice != null ? props.row.transferPrice : props.row.currentCost
  return p == null || props.row.qty == null ? null : round2(Number(props.row.qty) * Number(p))
})
</script>

<style scoped>
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
