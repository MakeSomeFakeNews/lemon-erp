<template>
  <div class="whole-discount">
    <span class="item">{{ $t('scm.wholeDiscount.total') }} <b>{{ symbol }}{{ money(total) }}</b></span>
    <span class="item">
      <a-tooltip :title="$t('scm.wholeDiscount.hint')">
        <span class="label">{{ $t('scm.wholeDiscount.label') }}</span>
      </a-tooltip>
      <a-input-number
        :value="value"
        :min="0"
        :max="maxDiscount"
        :precision="2"
        :prefix="symbol"
        :disabled="disabled"
        class="input"
        @update:value="update"
      />
      <a-dropdown v-if="!disabled" :trigger="['click']">
        <a class="round-link">{{ $t('scm.wholeDiscount.round') }} <DownOutlined /></a>
        <template #overlay>
          <a-menu @click="({ key }) => roundDown(key)">
            <a-menu-item key="0.1">{{ $t('scm.wholeDiscount.roundFen') }}</a-menu-item>
            <a-menu-item key="1">{{ $t('scm.wholeDiscount.roundJiao') }}</a-menu-item>
            <a-menu-item key="10">{{ $t('scm.wholeDiscount.roundYuan') }}</a-menu-item>
            <a-menu-item v-if="Number(value) > 0" key="clear">{{ $t('scm.wholeDiscount.clear') }}</a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </span>
    <span class="item">{{ $t('scm.wholeDiscount.after') }} <b class="after">{{ symbol }}{{ money(after) }}</b></span>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { DownOutlined } from '@ant-design/icons-vue'
import { money, nz, round2 } from '@/utils/number'

/**
 * 整单优惠（含税，抹零也用它）：显示合计、优惠输入、优惠后合计，「抹零」按优惠后合计向下取整到角 / 元 / 十元，差额累加到优惠。
 * 分摊由后端在保存时按行金额比例完成（先促销、再返利、最后整单优惠），这里只录入金额。
 * 用法：<WholeDiscountBar v-model:value="form.wholeDiscount" :total="合计" symbol="¥" />
 */
const props = defineProps({
  /** 优惠前合计（已扣除促销、返利） */
  total: { type: Number, default: 0 },
  value: Number,
  symbol: { type: String, default: '¥' },
  disabled: Boolean
})
const emit = defineEmits(['update:value'])

const maxDiscount = computed(() => Math.max(round2(props.total), 0))
const after = computed(() => round2(nz(props.total) - nz(props.value)))

const update = (v) => emit('update:value', v == null || v === 0 ? undefined : v)

/** 抹零：优惠后合计向下取整到 unit（0.1 / 1 / 10），差额加到优惠 */
function roundDown(key) {
  if (key === 'clear') return update(undefined)
  const unit = Number(key)
  // 先转为分的整数，避免浮点误差
  const cents = Math.round(after.value * 100)
  const unitCents = Math.round(unit * 100)
  if (cents <= 0) return
  const off = cents % unitCents
  update(round2(nz(props.value) + off / 100))
}
</script>

<style scoped>
.whole-discount {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px 24px;
  margin-top: 12px;
}
.item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.label {
  border-bottom: 1px dashed var(--app-text-3);
  cursor: help;
}
.input {
  width: 140px;
}
.round-link {
  white-space: nowrap;
}
.after {
  font-size: 16px;
  color: var(--ant-color-primary, #1677ff);
}
</style>
