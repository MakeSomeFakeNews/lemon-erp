<template>
  <SubPage :open="open" :title="detail ? tx('detailTitle', { no: detail.rebateNo }) : ''" width="min(900px, 100vw)" @close="open = false">
    <template v-if="detail" #extra>
      <a-space>
        <template v-if="detail.status === RebateStatus.ACTIVE">
          <span v-perm="perm('add')">
            <a-popconfirm :title="$t('scm.rebate.confirmDelete')" @confirm="remove"><a-button danger>{{ $t('common.action.delete') }}</a-button></a-popconfirm>
          </span>
          <a-button v-perm="perm('add')" @click="emit('edit', detail)">{{ $t('common.action.edit') }}</a-button>
          <span v-perm="perm('settle')">
            <a-popconfirm :title="settleTip" :disabled="!ended" @confirm="settle">
              <a-tooltip :title="ended ? '' : $t('scm.rebate.settleAfterEnd')">
                <a-button type="primary" :disabled="!ended">{{ $t('scm.rebate.settle') }}</a-button>
              </a-tooltip>
            </a-popconfirm>
          </span>
        </template>
        <span v-else v-perm="perm('settle')">
          <a-popconfirm :title="tx('unsettleConfirm')" @confirm="unsettle">
            <a-button>{{ $t('scm.rebate.unsettle') }}</a-button>
          </a-popconfirm>
        </span>
      </a-space>
    </template>
    <template v-if="detail">
      <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
        <a-descriptions-item :label="purchase ? $t('scm.rebate.supplier') : $t('scm.rebate.customer')">{{ purchase ? detail.supplierName : detail.customerName }}</a-descriptions-item>
        <a-descriptions-item :label="$t('scm.rebate.period')">{{ detail.beginDate }} ~ {{ detail.endDate }}</a-descriptions-item>
        <a-descriptions-item :label="$t('common.field.status')"><DictTag type="scm_rebate_status" :value="detail.status" /></a-descriptions-item>
        <a-descriptions-item :label="$t('scm.rebate.settleMode')">
          <span v-if="purchase">{{ $t('scm.rebate.purchaseCreditNote') }}</span>
          <DictTag v-else type="scm_rebate_settle" :value="detail.settleMode" />
        </a-descriptions-item>
        <a-descriptions-item :label="$t('scm.rebate.calcMode')"><DictTag type="scm_rebate_calc" :value="detail.calcMode" /></a-descriptions-item>
        <a-descriptions-item :label="$t('scm.rebate.created')">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
        <template v-if="detail.status === RebateStatus.SETTLED">
          <a-descriptions-item :label="$t('scm.rebate.settled')">{{ detail.settleDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.rebate.rebate')">{{ resultText(detail) }}</a-descriptions-item>
          <a-descriptions-item :label="tx('creditNote')">{{ detail.billNo || '-' }}</a-descriptions-item>
        </template>
        <template v-else-if="calc">
          <a-descriptions-item :label="ended ? $t('scm.rebate.due') : $t('scm.rebate.estimated')">{{ resultText(calc) }}</a-descriptions-item>
          <a-descriptions-item v-if="byItems" :label="tx('baseAmount')">¥{{ formatMoney(calc.salesAmount) }}</a-descriptions-item>
          <a-descriptions-item v-else :label="$t('scm.rebate.nextTier')">{{ nextTierText }}</a-descriptions-item>
          <a-descriptions-item :label="tx('creditNote')">-</a-descriptions-item>
        </template>
        <a-descriptions-item v-if="detail.remark" :label="$t('common.field.description')" :span="3">{{ detail.remark }}</a-descriptions-item>
      </a-descriptions>
      <template v-if="byItems">
        <div class="section-title">{{ $t('scm.rebate.items') }}</div>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'brand'"><DictTag v-if="record.brand" type="wms_material_brand" :value="record.brand" /></template>
            <template v-else-if="column.key === 'category'"><DictTag v-if="record.category" type="wms_material_category" :value="record.category" /></template>
            <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
          </template>
        </a-table>
      </template>
      <template v-else>
        <div class="section-title">{{ $t('scm.rebate.tiers') }}</div>
        <a-table row-key="id" size="small" :columns="tierColumns" :data-source="detail.tiers" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'minAmount'">¥{{ formatMoney(record.minAmount) }}</template>
            <template v-else-if="column.key === 'rate'">{{ Number(record.rate) }}%</template>
            <template v-else-if="column.key === 'reached'">
              <a-tag v-if="reachedTier && reachedTier.id === record.id" color="green">{{ $t('scm.rebate.current') }}</a-tag>
            </template>
          </template>
        </a-table>
      </template>
    </template>
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import { formatMoney } from '@/components/scm/scm'
import { useRebateText } from '@/components/scm/rebate'
import { today } from '@/components/wms/wms'
import { useDict } from '@/composables/useDict'
import { RebateCalcMode, RebateKind, RebateStatus } from '@/constants/enums'

/** 返利协议详情：执行中可修改、删除、结算（期间结束后），已结算可撤销结算 */
const props = defineProps({
  kind: { type: String, required: true },
  api: { type: Object, required: true }
})
const emit = defineEmits(['edit', 'changed'])
const { t } = useI18n()
useDict('scm_rebate_status', 'scm_rebate_settle', 'scm_rebate_calc', 'wms_material_brand', 'wms_material_category')
const purchase = props.kind === RebateKind.PURCHASE
const tx = useRebateText(props.kind)
const perm = (action) => `${purchase ? 'scm:purchaseRebate' : 'scm:rebate'}:${action}`

const tierColumns = [
  { title: tx('reach'), key: 'minAmount' },
  { title: t('scm.rebate.rate'), key: 'rate', width: 120 },
  { title: '', key: 'reached', width: 80 }
]
const itemColumns = [
  { title: t('scm.rebate.itemName'), dataIndex: 'itemName' },
  { title: t('scm.rebate.brand'), key: 'brand', width: 140 },
  { title: t('scm.rebate.category'), key: 'category', width: 140 },
  { title: t('scm.rebate.itemAmount'), key: 'amount', width: 150, align: 'right' }
]

const open = ref(false)
const detail = ref()
const calc = ref()
const byItems = computed(() => detail.value?.calcMode === RebateCalcMode.ITEM)
const ended = computed(() => detail.value && detail.value.endDate < today())
const baseNow = computed(() => Number(detail.value?.status === RebateStatus.SETTLED ? detail.value.salesAmount : calc.value?.salesAmount) || 0)
const reachedTier = computed(() => [...(detail.value?.tiers || [])].filter((x) => baseNow.value >= Number(x.minAmount))
  .sort((a, b) => Number(b.minAmount) - Number(a.minAmount))[0])
const nextTierText = computed(() => {
  const next = [...(detail.value?.tiers || [])].filter((x) => baseNow.value < Number(x.minAmount))
    .sort((a, b) => Number(a.minAmount) - Number(b.minAmount))[0]
  return next ? tx('nextTierText', { amount: formatMoney(Number(next.minAmount) - baseNow.value), rate: Number(next.rate) }) : t('scm.rebate.topTier')
})
/** 返利金额：阶梯比例显示 金额 × 比例，按项目填写只显示合计 */
const resultText = (r) => (r.rate == null
  ? t('scm.rebate.itemsResult', { rebate: formatMoney(r.rebateAmount) })
  : t('scm.rebate.rebateCalc', { rebate: formatMoney(r.rebateAmount), sales: formatMoney(r.salesAmount), rate: Number(r.rate) }))
const settleTip = computed(() => calc.value
  ? tx('settleConfirm', { sales: formatMoney(calc.value.salesAmount), rebate: formatMoney(calc.value.rebateAmount) }) : t('scm.rebate.settleConfirmPlain'))

async function show(id) {
  const r = await props.api.get(id)
  detail.value = r
  calc.value = r.status === RebateStatus.ACTIVE ? await props.api.calc(id) : undefined
  open.value = true
}
function reload() {
  if (open.value && detail.value) show(detail.value.id)
}
async function settle() {
  const res = await props.api.settle(detail.value.id)
  message.success(t('scm.rebate.settledMsg', { amount: formatMoney(res.rebateAmount) }))
  show(detail.value.id)
  emit('changed')
}
async function unsettle() {
  await props.api.unsettle(detail.value.id)
  message.success(t('scm.rebate.unsettledMsg'))
  show(detail.value.id)
  emit('changed')
}
async function remove() {
  await props.api.remove(detail.value.id)
  message.success(t('common.msg.deleted'))
  open.value = false
  emit('changed')
}

defineExpose({ open: show, reload })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.section-title {
  margin: 12px 0 8px;
  font-weight: 600;
}
</style>
