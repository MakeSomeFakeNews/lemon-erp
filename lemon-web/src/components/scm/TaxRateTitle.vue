<template>
  <span class="tax-title">
    {{ title }}
    <a-popover v-model:open="open" trigger="click" placement="bottom" :title="$t('scm.taxRate.batch')">
      <template #content>
        <div class="batch">
          <a-input-number v-model:value="rate" :min="0" :max="100" :precision="2" addon-after="%" size="small" class="rate-input" />
          <a-button type="primary" size="small" :disabled="rate == null" @click="apply">{{ $t('scm.taxRate.applyAll') }}</a-button>
        </div>
        <div v-if="quick.length" class="quick">
          <a-tag v-for="r in quick" :key="r" class="quick-tag" @click="rate = r">{{ r }}%</a-tag>
        </div>
      </template>
      <a-tooltip :title="$t('scm.taxRate.batchTip')">
        <EditOutlined class="trigger" @click.stop />
      </a-tooltip>
    </a-popover>
  </span>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { EditOutlined } from '@ant-design/icons-vue'
import { kindRate, useTaxRates } from './taxRate'

/**
 * 明细表税率列的列头：点击图标统一设置全部明细的税率（不使用发票类型时的批量方式），触发 apply(rate)。
 * 快捷选项为默认税率和发票类型字典中的各税率
 */
defineProps({
  title: { type: String, required: true }
})
const emit = defineEmits(['apply'])

const tax = useTaxRates()
const open = ref(false)
const rate = ref()
watch(open, (v) => v && rate.value == null && (rate.value = tax.defaultRate.value))

const quick = computed(() => {
  const set = new Set([tax.defaultRate.value, ...tax.kinds.value.map(kindRate).filter((r) => r != null)])
  return [...set].sort((a, b) => b - a)
})

function apply() {
  emit('apply', rate.value)
  open.value = false
}
</script>

<style scoped>
.trigger {
  margin-left: 4px;
  color: var(--app-text-3);
  cursor: pointer;
}
.trigger:hover {
  color: var(--ant-color-primary, #1677ff);
}
.batch {
  display: flex;
  gap: 8px;
}
.rate-input {
  width: 120px;
}
.quick {
  margin-top: 8px;
  max-width: 260px;
}
.quick-tag {
  cursor: pointer;
  margin-bottom: 4px;
}
</style>
