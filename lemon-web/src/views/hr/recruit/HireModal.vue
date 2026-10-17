<template>
  <a-modal v-model:open="visible" :title="$t('hr.recruit.hireTitle', { name: current?.name || '' })" :confirm-loading="hiring" destroy-on-close @ok="submitHire">
    <a-alert type="info" show-icon :message="$t('hr.recruit.hireTip')" class="mb" />
    <a-form ref="hireRef" :model="hireForm" layout="vertical">
      <a-form-item :label="$t('hr.employee.empNo')" name="empNo" :rules="[{ required: true, message: $t('hr.employee.empNoRequired') }, { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: $t('hr.position.codePattern') }]">
        <a-input v-model:value="hireForm.empNo" :maxlength="32" />
      </a-form-item>
      <a-form-item :label="$t('hr.employee.entryDate')" name="entryDate" :rules="[{ required: true, message: $t('hr.employee.entryDateRequired') }]">
        <a-date-picker v-model:value="hireForm.entryDate" value-format="YYYY-MM-DD" class="full" />
      </a-form-item>
      <a-form-item :label="$t('hr.employee.position')" name="positionId" :extra="$t('hr.recruit.hirePositionExtra')"><PositionSelect v-model:value="hireForm.positionId" /></a-form-item>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item :label="$t('hr.employee.empType')" name="empType">
            <a-select v-model:value="hireForm.empType" :options="toOptions(dict.hr_emp_type)" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('hr.recruit.expectedRegularDate')" name="regularDate" :extra="$t('hr.recruit.regularExtra')">
            <a-date-picker v-model:value="hireForm.regularDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item :label="$t('hr.recruit.probationSalary')" name="baseSalary" :extra="$t('hr.recruit.probationSalaryExtra')">
        <a-input-number v-model:value="hireForm.baseSalary" :min="0" :precision="2" :addon-after="$t('hr.employee.perMonth')" class="full" />
      </a-form-item>
      <a-form-item :label="$t('hr.employee.account')">
        <a-radio-group v-model:value="hireForm.accountMode" class="account-mode">
          <a-radio value="none">{{ $t('hr.recruit.accountNone') }}</a-radio>
          <a-radio value="new">{{ $t('hr.recruit.accountNew') }}</a-radio>
          <a-radio value="link">{{ $t('hr.recruit.accountLink') }}</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item
        v-if="hireForm.accountMode === 'new'"
        :label="$t('hr.recruit.username')"
        name="username"
        :extra="$t('hr.recruit.usernameExtra')"
        :rules="[{ required: true, message: $t('hr.recruit.usernameRequired') }, { pattern: /^[A-Za-z0-9_.-]{2,30}$/, message: $t('hr.recruit.usernamePattern') }]"
      >
        <a-input v-model:value="hireForm.username" :maxlength="30" />
      </a-form-item>
      <a-form-item v-if="hireForm.accountMode === 'link'" :label="$t('hr.recruit.existingAccount')" name="userId"><IdentityUserSelect v-model:value="hireForm.userId" /></a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import PositionSelect from '@/components/hr/PositionSelect.vue'
import { hireCandidate } from '@/api/hr'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { t } from '@/locales'

/** 录用应聘者（生成员工档案，可同时开通或关联账号）：通过 ref 调用 open(应聘者)，成功后触发 success */
const emit = defineEmits(['success'])
const dict = useDict('hr_emp_type')

const visible = ref(false)
const hiring = ref(false)
const hireRef = ref()
const current = ref()
const hireForm = reactive({ empNo: '', entryDate: undefined, positionId: undefined, userId: undefined, empType: 'FULL_TIME',
  regularDate: undefined, baseSalary: undefined, accountMode: 'none', username: '' })
function open(record) {
  current.value = record
  Object.assign(hireForm, { empNo: '', entryDate: dayjs().add(7, 'day').format('YYYY-MM-DD'), positionId: undefined, userId: undefined,
    empType: 'FULL_TIME', regularDate: undefined, baseSalary: undefined, accountMode: 'none', username: '' })
  visible.value = true
}
async function submitHire() {
  await hireRef.value.validate()
  hiring.value = true
  try {
    const { accountMode, username, userId, ...rest } = hireForm
    await hireCandidate(current.value.id, {
      ...rest,
      userId: accountMode === 'link' ? userId || null : null,
      username: accountMode === 'new' ? username : null
    })
    message.success(t('hr.recruit.hired'))
    visible.value = false
    emit('success')
  } finally {
    hiring.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.full {
  width: 100%;
}
.mb {
  margin-bottom: 16px;
}
</style>
