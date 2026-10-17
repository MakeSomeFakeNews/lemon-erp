<template>
  <a-modal v-model:open="visible" :title="applyForm.kind === LeaveKind.OVERTIME ? $t('hr.self.applyOvertime') : $t('hr.self.applyLeave')" :confirm-loading="saving" destroy-on-close @ok="apply">
    <a-form ref="applyRef" :model="applyForm" layout="vertical">
      <a-form-item v-if="applyForm.kind === LeaveKind.LEAVE" :label="$t('hr.self.leaveType')" name="type" :rules="[{ required: true, message: $t('hr.self.leaveTypeRequired') }]"
        :extra="applyForm.type === 'COMPENSATORY' ? $t('hr.self.compLeft', { n: Number(profile.compensatoryHours) }) : ''">
        <a-select v-model:value="applyForm.type" :options="leaveTypeOptions" />
      </a-form-item>
      <a-form-item v-else :label="$t('hr.self.compensation')" name="type">
        <a-radio-group v-model:value="applyForm.type">
          <a-radio :value="OvertimeComp.PAY">{{ $t('hr.self.payOvertime') }}</a-radio>
          <a-radio :value="OvertimeComp.TO_LEAVE">{{ $t('hr.self.toLeave') }}</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item :label="$t('hr.self.time')" name="range" :rules="[{ required: true, message: $t('hr.self.rangeRequired') }]">
        <a-range-picker
          v-model:value="applyForm.range"
          show-time
          :minute-step="30"
          format="YYYY-MM-DD HH:mm"
          value-format="YYYY-MM-DD HH:mm:ss"
          class="full"
          @change="suggestHours"
        />
      </a-form-item>
      <a-form-item :label="$t('hr.self.duration')" name="hours" :rules="[{ required: true, message: $t('hr.self.durationRequired') }]" :extra="hoursHint">
        <a-input-number v-model:value="applyForm.hours" :min="0.5" :step="0.5" :precision="1" :addon-after="$t('hr.self.hoursUnit')" class="full" />
      </a-form-item>
      <a-form-item :label="$t('hr.self.reason')" name="reason">
        <a-textarea v-model:value="applyForm.reason" :rows="3" :maxlength="255" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { applyLeave } from '@/api/hr'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { t } from '@/locales'
import { LeaveKind, OvertimeComp } from '@/constants/enums'

/** 请假 / 加班申请：通过 ref 调用 open(LEAVE / OVERTIME)，提交后触发 success */
const props = defineProps({
  profile: { type: Object, default: undefined },
  annualLeft: { type: Number, default: 0 }
})
const emit = defineEmits(['success'])
const dict = useDict('hr_leave_type')

const visible = ref(false)
const saving = ref(false)
const applyRef = ref()
const applyForm = reactive({ kind: LeaveKind.LEAVE, type: undefined, range: undefined, hours: undefined, reason: '' })
const leaveTypeOptions = computed(() => toOptions(dict.hr_leave_type).map((o) => ({
  ...o, label: o.value === 'ANNUAL' && props.profile ? t('hr.self.annualOption', { label: o.label, n: props.annualLeft }) : o.label
})))
const hoursHint = computed(() => (applyForm.kind === LeaveKind.LEAVE ? t('hr.self.leaveHint') : t('hr.self.overtimeHint')))
function open(kind) {
  const start = dayjs().add(1, 'day').hour(kind === LeaveKind.LEAVE ? 9 : 18).minute(kind === LeaveKind.LEAVE ? 0 : 30).second(0)
  const end = kind === LeaveKind.LEAVE ? start.hour(18) : start.add(2, 'hour')
  Object.assign(applyForm, { kind, type: kind === LeaveKind.LEAVE ? 'PERSONAL' : OvertimeComp.PAY,
    range: [start.format('YYYY-MM-DD HH:mm:ss'), end.format('YYYY-MM-DD HH:mm:ss')], hours: undefined, reason: '' })
  suggestHours()
  visible.value = true
}
/** 请假：按日期内的工作日（周一至周五）× 8 小时估算；加班：按起止时间差 */
function suggestHours() {
  const [s, e] = applyForm.range || []
  if (!s || !e) return
  const start = dayjs(s)
  const end = dayjs(e)
  if (!end.isAfter(start)) return
  if (applyForm.kind === LeaveKind.OVERTIME) {
    applyForm.hours = Math.round((end.diff(start, 'minute') / 60) * 2) / 2
    return
  }
  if (start.isSame(end, 'day')) {
    const minutes = end.diff(start, 'minute') - (start.hour() < 13 && end.hour() >= 13 ? 60 : 0)
    applyForm.hours = Math.min(8, Math.max(0.5, Math.round((minutes / 60) * 2) / 2))
    return
  }
  let days = 0
  for (let d = start.startOf('day'); !d.isAfter(end, 'day'); d = d.add(1, 'day')) {
    if (d.day() !== 0 && d.day() !== 6) days++
  }
  applyForm.hours = days * 8
}
async function apply() {
  await applyRef.value.validate()
  saving.value = true
  try {
    await applyLeave({ kind: applyForm.kind, type: applyForm.type || null, startTime: applyForm.range[0], endTime: applyForm.range[1],
      hours: applyForm.hours, reason: applyForm.reason || null })
    message.success(t('hr.self.submitted'))
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
