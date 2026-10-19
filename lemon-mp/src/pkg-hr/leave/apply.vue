<template>
  <view>
    <l-card>
      <l-field v-if="isLeave" :label="t('hr.leave.leaveType')" required>
        <picker :range="typeOptions" range-key="label" :value="typeIndex" @change="(e) => (form.type = typeOptions[e.detail.value].value)">
          <view class="l-input" :class="{ 'l-placeholder': !form.type }">{{ typeLabel || t('common.pleaseSelect') }}</view>
        </picker>
        <view v-if="form.type === COMPENSATORY && profile" class="apply__hint">{{ t('hr.leave.compLeft', { n: Number(profile.compensatoryHours || 0) }) }}</view>
      </l-field>
      <l-field v-else :label="t('hr.leave.compensation')">
        <view class="apply__radios">
          <view
            v-for="o in compOptions"
            :key="o.value"
            class="apply__radio"
            :class="{ 'apply__radio--active': form.type === o.value }"
            @click="form.type = o.value"
          >
            {{ o.label }}
          </view>
        </view>
      </l-field>
      <l-field :label="t('hr.leave.startTime')" required>
        <view class="apply__dt">
          <picker mode="date" :value="form.startDate" @change="(e) => change('startDate', e.detail.value)">
            <view class="l-input">{{ form.startDate }}</view>
          </picker>
          <picker mode="time" :value="form.startClock" @change="(e) => change('startClock', e.detail.value)">
            <view class="l-input">{{ form.startClock }}</view>
          </picker>
        </view>
      </l-field>
      <l-field :label="t('hr.leave.endTime')" required>
        <view class="apply__dt">
          <picker mode="date" :value="form.endDate" :start="form.startDate" @change="(e) => change('endDate', e.detail.value)">
            <view class="l-input">{{ form.endDate }}</view>
          </picker>
          <picker mode="time" :value="form.endClock" @change="(e) => change('endClock', e.detail.value)">
            <view class="l-input">{{ form.endClock }}</view>
          </picker>
        </view>
      </l-field>
      <l-field :label="t('hr.leave.duration')" required>
        <input v-model="form.hours" class="l-input" type="digit" :placeholder="t('hr.leave.durationPh')" placeholder-class="l-placeholder" />
        <view class="apply__hint">{{ isLeave ? t('hr.leave.leaveHint') : t('hr.leave.overtimeHint') }}</view>
      </l-field>
      <l-field :label="t('hr.leave.reason')">
        <textarea v-model="form.reason" class="l-textarea" maxlength="255" :placeholder="t('hr.leave.reasonPh')" placeholder-class="l-placeholder" />
      </l-field>
    </l-card>
    <view class="l-footer-space" />
    <view class="l-footer">
      <button class="l-btn" :loading="busy" :disabled="busy" @click="submit">{{ t('common.submit') }}</button>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { LeaveKind, OvertimeComp } from '@web/constants/enums'
import { applyLeave, selfProfile } from '@/api/hr'
import { useDict } from '@/store/dict'
import { today } from '@/utils/format'

const { t } = useI18n()
const dict = useDict('hr_leave_type')
/** 业务字典 hr_leave_type 中程序依赖的内置取值 */
const ANNUAL = 'ANNUAL'
const COMPENSATORY = 'COMPENSATORY'
const PERSONAL = 'PERSONAL'

const kind = ref(LeaveKind.LEAVE)
const isLeave = computed(() => kind.value === LeaveKind.LEAVE)
const profile = ref(null)
const busy = ref(false)
const form = reactive({ type: PERSONAL, startDate: today(1), startClock: '09:00', endDate: today(1), endClock: '18:00', hours: '', reason: '' })

const annualLeft = computed(() => (profile.value ? Math.max(0, +(profile.value.annualDays - Number(profile.value.annualUsedDays)).toFixed(1)) : 0))
const typeOptions = computed(() =>
  dict.hr_leave_type.map((d) => ({
    value: d.value,
    label: d.value === ANNUAL && profile.value ? t('hr.leave.annualOption', { label: d.label, n: annualLeft.value }) : d.label
  }))
)
const typeIndex = computed(() => Math.max(0, typeOptions.value.findIndex((o) => o.value === form.type)))
const typeLabel = computed(() => typeOptions.value.find((o) => o.value === form.type)?.label)
const compOptions = computed(() => [
  { value: OvertimeComp.PAY, label: t('hr.leave.payOvertime') },
  { value: OvertimeComp.TO_LEAVE, label: t('hr.leave.toLeave') }
])

onLoad((q) => {
  kind.value = q.kind === LeaveKind.OVERTIME ? LeaveKind.OVERTIME : LeaveKind.LEAVE
  uni.setNavigationBarTitle({ title: t(isLeave.value ? 'hr.title.applyLeave' : 'hr.title.applyOvertime') })
  if (!isLeave.value) Object.assign(form, { type: OvertimeComp.PAY, startClock: '18:30', endClock: '20:30' })
  suggestHours()
  selfProfile()
    .then((p) => (profile.value = p))
    .catch(() => {})
})

const toDate = (d, c) => new Date(`${d.replace(/-/g, '/')} ${c}:00`)

function change(field, value) {
  form[field] = value
  if (field === 'startDate' && form.endDate < value) form.endDate = value
  suggestHours()
}

/** 请假：同一天按时间差（跨午休扣 1 小时，最多 8 小时），跨天按工作日 × 8 小时估算；加班：按起止时间差 */
function suggestHours() {
  const start = toDate(form.startDate, form.startClock)
  const end = toDate(form.endDate, form.endClock)
  if (!(end > start)) return
  const minutes = (end - start) / 60000
  if (!isLeave.value) {
    form.hours = String(Math.round((minutes / 60) * 2) / 2)
    return
  }
  if (form.startDate === form.endDate) {
    const lunch = start.getHours() < 13 && end.getHours() >= 13 ? 60 : 0
    form.hours = String(Math.min(8, Math.max(0.5, Math.round(((minutes - lunch) / 60) * 2) / 2)))
    return
  }
  let days = 0
  const d = new Date(start.getFullYear(), start.getMonth(), start.getDate())
  const last = new Date(end.getFullYear(), end.getMonth(), end.getDate())
  for (; d <= last; d.setDate(d.getDate() + 1)) {
    if (d.getDay() !== 0 && d.getDay() !== 6) days++
  }
  form.hours = String(days * 8)
}

async function submit() {
  if (isLeave.value && !form.type) return toast(t('common.selectRequired', { name: t('hr.leave.leaveType') }))
  if (!(toDate(form.endDate, form.endClock) > toDate(form.startDate, form.startClock))) return toast(t('hr.leave.endBeforeStart'))
  const hours = Number(form.hours)
  if (!(hours >= 0.5) || Math.round(hours * 2) !== hours * 2) return toast(t('hr.leave.durationInvalid'))
  busy.value = true
  try {
    await applyLeave({
      kind: kind.value,
      type: form.type || null,
      startTime: `${form.startDate} ${form.startClock}:00`,
      endTime: `${form.endDate} ${form.endClock}:00`,
      hours,
      reason: form.reason.trim() || null
    })
  } catch {
    return
  } finally {
    busy.value = false
  }
  uni.showToast({ title: t('hr.leave.submitted'), icon: 'none' })
  setTimeout(() => uni.navigateBack(), 1000)
}

function toast(title) {
  uni.showToast({ title, icon: 'none' })
}
</script>

<style lang="scss">
.apply {
  &__hint {
    margin-top: 8rpx;
    font-size: 22rpx;
    color: $l-text-3;
    line-height: 1.5;
  }
  &__dt {
    display: flex;
    gap: 24rpx;
    picker {
      flex: 1;
    }
  }
  &__radios {
    display: flex;
    gap: 20rpx;
  }
  &__radio {
    padding: 12rpx 32rpx;
    border-radius: 32rpx;
    border: 1rpx solid #d9d9d9;
    font-size: 26rpx;
    color: $l-text-2;
    &--active {
      color: $l-primary;
      border-color: $l-primary;
      background: #e6f4ff;
    }
  }
}
</style>
