<template>
  <view>
    <NoEmployee v-if="profile && !profile.employee" />
    <template v-else-if="profile">
      <l-empty v-if="loaded && !list.length" :text="t('hr.payslip.empty')" />
      <view v-for="s in list" :key="s.id" class="slip">
        <view class="slip__head" @click="toggle(s.id)">
          <view>
            <view class="slip__period">{{ periodText(s.period) }}</view>
            <view class="slip__label">{{ t('hr.payslip.netPay') }}</view>
          </view>
          <view class="slip__right">
            <text class="slip__net">{{ money(s.netPay) }}</text>
            <text class="slip__arrow" :class="{ 'slip__arrow--open': openId === s.id }">›</text>
          </view>
        </view>
        <view v-if="openId === s.id" class="slip__body">
          <view class="slip__section">{{ t('hr.payslip.income') }}</view>
          <l-cell :label="t('hr.payslip.baseSalary')" :value="money(s.baseSalary)" />
          <l-cell :label="t('hr.payslip.allowance')" :value="money(s.allowance)" />
          <l-cell :label="t('hr.payslip.overtimePay')">
            <text>{{ money(s.overtimePay) }}</text>
            <view v-if="Number(s.overtimeHours)" class="slip__note">{{ t('hr.payslip.overtimeHours', { n: Number(s.overtimeHours) }) }}</view>
          </l-cell>
          <l-cell :label="t('hr.payslip.perfBonus')">
            <text>{{ money(s.perfBonus) }} </text>
            <l-tag v-if="s.perfGrade" :text="dictText(dict.hr_perf_grade, s.perfGrade)" :color="dictColor(dict.hr_perf_grade, s.perfGrade)" />
          </l-cell>
          <l-cell v-if="Number(s.pieceWage)" :label="t('hr.payslip.pieceWage')" :value="money(s.pieceWage)" />
          <l-cell v-if="Number(s.commission)" :label="t('hr.payslip.commission')" :value="money(s.commission)" />
          <l-cell :label="t('hr.payslip.otherBonus')" :value="money(s.otherBonus)" />
          <l-cell :label="t('hr.payslip.attendanceDeduct')">
            <text>-{{ money(s.attendanceDeduct) }}</text>
            <view class="slip__note">{{ t('hr.payslip.deductDetail', { absent: Number(s.absentDays || 0), late: s.lateCount || 0 }) }}</view>
          </l-cell>
          <l-cell :label="t('hr.payslip.grossPay')">
            <text class="slip__strong">{{ money(s.grossPay) }}</text>
          </l-cell>

          <view class="slip__section">{{ t('hr.payslip.deduction') }}</view>
          <l-cell :label="t('hr.payslip.socialPersonal')" :value="'-' + money(s.socialPersonal)" />
          <l-cell :label="t('hr.payslip.housingPersonal')" :value="'-' + money(s.housingPersonal)" />
          <l-cell :label="t('hr.payslip.incomeTax')" :value="'-' + money(s.incomeTax)" />
          <l-cell :label="t('hr.payslip.otherDeduct')" :value="'-' + money(s.otherDeduct)" />
          <l-cell :label="t('hr.payslip.netPay')">
            <text class="slip__strong slip__primary">{{ money(s.netPay) }}</text>
          </l-cell>

          <view class="slip__section">{{ t('hr.payslip.tax') }}</view>
          <l-cell :label="t('hr.payslip.specialDeduct')" :value="money(s.specialDeduct)" />
          <l-cell :label="t('hr.payslip.cumTaxable')" :value="money(s.cumTaxable)" />
          <l-cell :label="t('hr.payslip.cumTax')" :value="money(s.cumTax)" />
          <l-cell :label="t('hr.payslip.status')">
            <l-tag :text="dictText(dict.hr_payroll_status, s.payrollStatus)" :color="dictColor(dict.hr_payroll_status, s.payrollStatus)" />
          </l-cell>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { selfPayslips, selfProfile } from '@/api/hr'
import { useTitle } from '@/composables/useTitle'
import { dictColor, dictText, useDict } from '@/store/dict'
import { money } from '@/utils/format'
import NoEmployee from '../components/NoEmployee.vue'
import { periodText } from '../hr'

const { t } = useI18n()
useTitle('hr.title.payslip')
const dict = useDict('hr_perf_grade', 'hr_payroll_status')
const profile = ref(null)
const list = ref([])
const loaded = ref(false)
const openId = ref(null)

onLoad(async () => {
  try {
    profile.value = await selfProfile()
    if (profile.value.employee) {
      list.value = (await selfPayslips()) || []
      // 默认展开最近一期
      openId.value = list.value[0]?.id ?? null
    }
  } catch {
    // 错误已提示
  } finally {
    loaded.value = true
  }
})

function toggle(id) {
  openId.value = openId.value === id ? null : id
}
</script>

<style lang="scss">
.slip {
  margin: 20rpx 24rpx;
  background: #fff;
  border-radius: $l-radius;
  overflow: hidden;
  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 28rpx;
  }
  &__period {
    font-size: 30rpx;
    font-weight: 600;
  }
  &__label {
    margin-top: 6rpx;
    font-size: 22rpx;
    color: $l-text-3;
  }
  &__right {
    display: flex;
    align-items: center;
    gap: 16rpx;
  }
  &__net {
    font-size: 36rpx;
    font-weight: 600;
    color: $l-primary;
  }
  &__arrow {
    font-size: 36rpx;
    color: #bfbfbf;
    transition: transform 0.2s;
    &--open {
      transform: rotate(90deg);
    }
  }
  &__body {
    padding: 0 28rpx 16rpx;
    border-top: 1rpx solid $l-border;
  }
  &__section {
    padding: 24rpx 0 4rpx;
    font-size: 24rpx;
    font-weight: 600;
    color: $l-text-2;
  }
  &__note {
    font-size: 22rpx;
    color: $l-text-3;
  }
  &__strong {
    font-weight: 600;
  }
  &__primary {
    color: $l-primary;
  }
}
</style>
