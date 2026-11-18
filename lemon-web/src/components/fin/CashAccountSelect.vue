<template>
  <a-select
    :value="value ?? undefined"
    show-search
    :allow-clear="allowClear"
    :placeholder="placeholder"
    :options="options"
    :loading="loading"
    :disabled="disabled"
    option-filter-prop="label"
    :dropdown-match-select-width="false"
    @change="(v) => emit('update:value', v ?? null)"
  >
    <template #option="{ label, code, subject, inactive }">
      <span :class="{ inactive }">{{ label }}</span>
      <span class="code">{{ code }} {{ subject }}</span>
    </template>
  </a-select>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { cashAccountOptions } from '@/api/fin'
import { cached } from './fin'
import { t } from '@/locales'
import { CashAccountType } from '@/constants/enums'

/**
 * 资金账户选择（收付款、其他收支、报销借款付款、账户调拨、运费现付等的结算账户）：列出「资金账户」中启用的账户名称（如 招行基本户、微信零钱），
 * 值为账户的入账科目 ID（单据中保存的值）。已停用的账户不能选，只在当前值是它时显示（已有单据）。
 * all：查询条件用，停用的账户也能选；bank：只列出银行类账户（不含现金，银行对账）
 */
const props = defineProps({
  value: Number,
  all: Boolean,
  bank: Boolean,
  placeholder: { type: String, default: () => t('fin.cashAccountSelect.placeholder') },
  allowClear: Boolean,
  disabled: Boolean
})
const emit = defineEmits(['update:value'])

const list = ref([])
const loading = ref(false)
const options = computed(() =>
  list.value
    .filter((a) => (props.all || a.usable || a.accountId === props.value) && (!props.bank || a.type !== CashAccountType.CASH))
    .map((a) => ({
      value: a.accountId,
      label: a.usable ? a.name : t('fin.cashAccountSelect.disabledName', { name: a.name }),
      code: a.accountCode,
      subject: a.accountName,
      inactive: !a.usable,
      disabled: !props.all && !a.usable
    }))
)

onMounted(async () => {
  loading.value = true
  try {
    list.value = await cached('cash-accounts', cashAccountOptions)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.code {
  margin-left: 8px;
  color: var(--app-text-3);
  font-size: 12px;
}
.inactive {
  color: var(--app-text-3);
}
</style>
