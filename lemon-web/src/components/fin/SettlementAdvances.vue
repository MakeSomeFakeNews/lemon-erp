<template>
  <!-- 收付款中冲抵预收 / 预付款（SettlementForm 的子组件）：勾选该单位未冲抵完的预收预付并填写冲抵金额，只能选与核销单据同币种的 -->
  <div v-if="rows.length" class="advances">
    <div class="advances-head">
      <span class="section-title">{{ conf.advanceTitle }}</span>
      <span class="sub-text">{{ conf.advanceTip }}</span>
    </div>
    <a-table
      row-key="id"
      size="small"
      :columns="columns"
      :data-source="rows"
      :pagination="false"
      :scroll="{ x: 640 }"
      :row-selection="{ selectedRowKeys: checked, onChange: onCheck, getCheckboxProps: (r) => ({ disabled: blocked(r) }) }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'remaining'">
          {{ isForeign(record.currency) ? `${record.currency} ` : '' }}{{ formatMoney(record.remaining) }}
        </template>
        <template v-else-if="column.key === 'offset'">
          <a-tooltip :title="blocked(record) ? $t('fin.settlementForm.advanceBlocked', { currency }) : ''">
            <a-input-number
              v-model:value="record.offset"
              :precision="2"
              :min="0"
              :max="record.remaining"
              :disabled="!checked.includes(record.id)"
              :prefix="isForeign(record.currency) ? currencySymbol(record.currency) : undefined"
              class="full"
            />
          </a-tooltip>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { t } from '@/locales'
import { round2 } from '@/utils/number'
import { formatMoney } from './fin'
import { BASE_CURRENCY, currencySymbol, isForeign } from './currency'

/**
 * rows：[{ id, advanceNo, advanceDate, currency, remaining（原币未冲抵）, offset（本次冲抵） }]，由父组件维护；
 * checked：勾选的预收预付 ID；currency：本次核销的币种；need：勾选时默认冲抵的上限（未分配的核销余额）
 */
const props = defineProps({
  rows: { type: Array, required: true },
  checked: { type: Array, required: true },
  currency: { type: String, default: BASE_CURRENCY },
  need: { type: Number, default: 0 },
  conf: { type: Object, required: true }
})
const emit = defineEmits(['update:checked'])

const columns = [
  { title: t('fin.settlementForm.advanceNo'), dataIndex: 'advanceNo', width: 150 },
  { title: t('common.field.date'), dataIndex: 'advanceDate', width: 110 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  { title: t('fin.settlementForm.advanceRemaining'), key: 'remaining', width: 130, align: 'right' },
  { title: t('fin.settlementForm.advanceOffset'), key: 'offset', width: 150 }
]

const blocked = (r) => (r.currency || BASE_CURRENCY) !== props.currency

/** 勾选时默认冲抵 min(余额, 尚未冲抵的核销余额)，取消勾选清空 */
function onCheck(keys) {
  let need = props.need
  props.rows.forEach((r) => {
    if (!keys.includes(r.id)) {
      r.offset = undefined
    } else if (!props.checked.includes(r.id)) {
      r.offset = round2(Math.max(0, Math.min(r.remaining, need)))
      need -= r.offset
    }
  })
  emit('update:checked', keys)
}
</script>

<style scoped>
.advances {
  margin-top: 16px;
}
.advances-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin: 8px 0;
}
.section-title {
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
</style>
