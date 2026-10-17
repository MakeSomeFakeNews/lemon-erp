<template>
  <a-modal v-model:open="visible" :title="$t('mes.wo.reportTitle', { op: `${reportOp?.seq} ${reportOp?.name}` })" :confirm-loading="saving" @ok="submit">
    <a-form layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item :label="$t('mes.wo.reportDate')" required>
            <a-date-picker v-model:value="report.reportDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('mes.wo.actualHours')" required>
            <a-input-number v-model:value="report.hours" :min="0" :precision="2" :addon-after="$t('mes.wo.hoursUnit')" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('mes.wo.goodQty')" required>
            <a-input-number v-model:value="report.goodQty" :min="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('mes.wo.scrapQty')">
            <a-input-number v-model:value="report.scrapQty" :min="0" class="full" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item :label="$t('mes.wo.equipment')" :extra="$t('mes.wo.equipmentExtra')">
            <a-select v-model:value="report.equipmentId" allow-clear show-search option-filter-prop="label" :placeholder="$t('mes.wo.notSpecified')" :options="equipmentChoices" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('mes.wo.mold')" :extra="shotsHint">
            <a-select v-model:value="report.moldId" allow-clear :placeholder="$t('mes.wo.notUsed')" :options="moldChoices" :not-found-content="$t('mes.wo.noMold')" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item :label="$t('mes.wo.operator')" :extra="reportOp?.pieceRate != null ? $t('mes.wo.pieceRateExtra', { v: formatQty(reportOp.pieceRate) }) : $t('mes.wo.operatorDefault')">
        <IdentityUserSelect v-model:value="report.operatorId" :placeholder="$t('mes.wo.self')" allow-clear />
      </a-form-item>
      <a-form-item :label="$t('common.field.remark')">
        <a-input v-model:value="report.remark" :maxlength="255" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import { equipmentOptions, moldsOfWo } from '@/api/mes/base'
import { reportWo } from '@/api/mes/wo'
import { formatQty, today } from '@/components/wms/wms'

/** 工序报工：通过 ref 调用 open(工序)，成功后触发 success */
const props = defineProps({ detail: { type: Object, default: undefined } })
const emit = defineEmits(['success'])
const { t } = useI18n()

const visible = ref(false)
const reportOp = ref()
const saving = ref(false)
const report = reactive({ reportDate: today(), goodQty: undefined, scrapQty: 0, hours: undefined, remark: '', operatorId: undefined,
  equipmentId: undefined, moldId: undefined })
const equipments = ref([])
const molds = ref([])
/** 工序所在工作中心的设备排在前面 */
const equipmentChoices = computed(() => [...equipments.value]
  .sort((a, b) => Number(b.workCenterId === reportOp.value?.workCenterId) - Number(a.workCenterId === reportOp.value?.workCenterId))
  .map((e) => ({ value: e.id, label: `${e.code} ${e.name}` })))
const moldChoices = computed(() => molds.value.map((m) => ({ value: m.id, label: t('mes.wo.moldOption', { code: m.code, name: m.name, n: m.cavities }) })))
const shotsHint = computed(() => {
  const m = molds.value.find((x) => x.id === report.moldId)
  if (!m) return t('mes.wo.moldRequired')
  const shots = Math.ceil((Number(report.goodQty || 0) + Number(report.scrapQty || 0)) / m.cavities)
  return m.designLife ? t('mes.wo.shotsHintLife', { shots, used: m.usedCount + shots, life: m.designLife }) : t('mes.wo.shotsHint', { shots, used: m.usedCount + shots })
})
async function open(op) {
  reportOp.value = op
  const remaining = Math.max(Number(props.detail.qty) - Number(op.goodQty) - Number(op.scrapQty), 0)
  const hours = Math.round(Number(op.stdHours) * remaining * 100) / 100
  const [eqs, ms] = await Promise.all([equipmentOptions(), moldsOfWo(props.detail.id)])
  equipments.value = eqs
  molds.value = ms
  const sameCenter = eqs.filter((e) => e.workCenterId === op.workCenterId)
  Object.assign(report, { reportDate: today(), goodQty: remaining, scrapQty: 0, hours, remark: '', operatorId: undefined,
    equipmentId: ms[0]?.equipmentId ?? (sameCenter.length === 1 ? sameCenter[0].id : undefined), moldId: ms.length === 1 ? ms[0].id : undefined })
  visible.value = true
}
async function submit() {
  saving.value = true
  try {
    await reportWo(props.detail.id, { opId: reportOp.value.id, ...report, operatorId: report.operatorId ?? null,
      equipmentId: report.equipmentId ?? null, moldId: report.moldId ?? null })
    message.success(t('mes.wo.reportSuccess'))
    visible.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.full {
  width: 100%;
}
</style>
