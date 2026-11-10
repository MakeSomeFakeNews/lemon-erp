<template>
  <a-modal v-model:open="open" :title="$t('fin.asset.disposeTitle', { name: target?.name || '' })" :confirm-loading="disposing" @ok="submit">
    <a-alert
      type="info"
      show-icon
      class="dispose-tip"
      :message="$t('fin.asset.disposeTip')"
    />
    <a-form layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item :label="$t('fin.asset.disposeDate')" required>
            <a-date-picker v-model:value="form.date" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('fin.asset.settleAccount')" :extra="form.income || form.expense ? '' : $t('fin.asset.settleAccountExtra')">
            <AccountSelect v-model:value="form.settleAccountId" :roles="['CASH', 'BANK']" :placeholder="$t('fin.asset.settleAccountPh')" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('fin.asset.disposeIncome')">
            <a-input-number v-model:value="form.income" :min="0" :precision="2" :addon-after="$t('fin.asset.yuan')" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('fin.asset.disposeExpense')">
            <a-input-number v-model:value="form.expense" :min="0" :precision="2" :addon-after="$t('fin.asset.yuan')" class="full" />
          </a-form-item>
        </a-col>
      </a-row>
      <div v-if="target" class="sub-text">
        {{ $t(net >= 0 ? 'fin.asset.disposeLoss' : 'fin.asset.disposeGain', { net: formatMoney(target.netValue), amount: formatMoney(Math.abs(net)) }) }}
      </div>
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import { disposeAsset } from '@/api/fin'
import { today } from '@/components/wms/wms'
import { formatMoney } from '@/components/fin/fin'

/** 处置资产：生成处置凭证，完成后触发 done */
const emit = defineEmits(['done'])
const { t } = useI18n()
const open = ref(false)
const disposing = ref(false)
const target = ref()
const form = reactive({ date: undefined, income: undefined, expense: undefined, settleAccountId: undefined, remark: '' })
const net = computed(() => {
  const r = target.value
  if (!r) return 0
  return Number(r.netValue) + (Number(form.expense) || 0) - (Number(form.income) || 0)
})

function show(record) {
  target.value = record
  Object.assign(form, { date: today(), income: undefined, expense: undefined, settleAccountId: undefined, remark: '' })
  open.value = true
}

async function submit() {
  if ((form.income || form.expense) && !form.settleAccountId) {
    message.warning(t('fin.asset.needSettleAccount'))
    return
  }
  disposing.value = true
  try {
    await disposeAsset(target.value.id, { ...form })
    message.success(t('fin.asset.disposed'))
    open.value = false
    emit('done')
  } finally {
    disposing.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.dispose-tip {
  margin-bottom: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
</style>
