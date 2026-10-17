<template>
  <a-empty v-if="!payslips.length" :description="$t('hr.self.noPayslips')" />
  <a-row v-else :gutter="16">
    <a-col :xs="24" :md="7">
      <div v-for="s in payslips" :key="s.id" class="slip-item" :class="{ active: s.id === slip?.id }" @click="slip = s">
        <b>{{ periodText(s.period) }}</b>
        <span>{{ formatMoney(s.netPay) }}</span>
      </div>
    </a-col>
    <a-col v-if="slip" :xs="24" :md="17">
      <a-descriptions :title="$t('hr.self.payslipTitle', { period: periodText(slip.period) })" bordered size="small" :column="2">
        <a-descriptions-item :label="$t('hr.self.baseSalary')">{{ formatMoney(slip.baseSalary) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('hr.self.allowance')">{{ formatMoney(slip.allowance) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('hr.self.overtimePay')">{{ formatMoney(slip.overtimePay) }} {{ $t('hr.self.hoursParen', { n: Number(slip.overtimeHours) }) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('hr.self.perfBonus')">
          {{ formatMoney(slip.perfBonus) }} <DictTag v-if="slip.perfGrade" type="hr_perf_grade" :value="slip.perfGrade" />
        </a-descriptions-item>
        <a-descriptions-item v-if="Number(slip.pieceWage)" :label="$t('hr.self.pieceWage')">{{ formatMoney(slip.pieceWage) }}</a-descriptions-item>
        <a-descriptions-item v-if="Number(slip.commission)" :label="$t('hr.self.commission')">{{ formatMoney(slip.commission) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('hr.self.otherBonus')">{{ formatMoney(slip.otherBonus) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('hr.self.attendanceDeduct')">
          -{{ formatMoney(slip.attendanceDeduct) }}
          <span class="sub-text">{{ $t('hr.self.deductDetail', { absent: Number(slip.absentDays), late: slip.lateCount }) }}</span>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('hr.self.grossPay')" :span="2"><b>{{ formatMoney(slip.grossPay) }}</b></a-descriptions-item>
        <a-descriptions-item :label="$t('hr.self.socialPersonal')">-{{ formatMoney(slip.socialPersonal) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('hr.self.housingPersonal')">-{{ formatMoney(slip.housingPersonal) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('hr.self.incomeTax')">-{{ formatMoney(slip.incomeTax) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('hr.self.otherDeduct')">-{{ formatMoney(slip.otherDeduct) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('hr.self.netPay')" :span="2"><b class="net">{{ formatMoney(slip.netPay) }}</b></a-descriptions-item>
        <a-descriptions-item :label="$t('hr.self.specialDeduct')">{{ formatMoney(slip.specialDeduct) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('hr.self.cumTaxable')">{{ formatMoney(slip.cumTaxable) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('hr.self.cumTax')">{{ formatMoney(slip.cumTax) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('common.field.status')"><DictTag type="hr_payroll_status" :value="slip.payrollStatus" /></a-descriptions-item>
      </a-descriptions>
    </a-col>
  </a-row>
</template>

<script setup>
import { ref, watch } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { periodText } from '@/components/hr/hr'
import { formatMoney } from '@/components/scm/scm'

/** 员工自助 - 工资条：左侧期间列表，右侧明细（默认最近一期） */
const props = defineProps({ payslips: { type: Array, default: () => [] } })
const slip = ref()
watch(() => props.payslips, (p) => (slip.value = p[0]), { immediate: true })
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.slip-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
}
.slip-item:hover {
  background: var(--app-hover);
}
.slip-item.active {
  background: var(--app-primary-bg);
}
.net {
  color: #1677ff;
  font-size: 16px;
}
</style>
