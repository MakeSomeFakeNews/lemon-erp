<template>
  <FormDrawer :drawer="drawer" :create-title="$t('hr.recruit.addCandidate')" :edit-title="$t('hr.recruit.editCandidate')">
    <a-form ref="candidateFormRef" :model="candidateForm" :rules="candidateRules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="24">
          <a-form-item :label="$t('hr.recruit.recruit')" name="recruitId">
            <a-select v-model:value="candidateForm.recruitId" show-search option-filter-prop="label" :options="openOptions" :disabled="drawer.isEdit.value" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('hr.employee.name')" name="name"><a-input v-model:value="candidateForm.name" :maxlength="64" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('hr.employee.gender')" name="gender"><a-radio-group v-model:value="candidateForm.gender" :options="GENDERS" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('common.field.phone')" name="phone"><a-input v-model:value="candidateForm.phone" :maxlength="32" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('common.field.email')" name="email"><a-input v-model:value="candidateForm.email" :maxlength="128" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('hr.employee.education')" name="education"><a-select v-model:value="candidateForm.education" allow-clear :options="toOptions(dict.hr_education)" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="16">
          <a-form-item :label="$t('hr.recruit.school')" name="school"><a-input v-model:value="candidateForm.school" :maxlength="128" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('hr.recruit.workYears')" name="workYears"><a-input-number v-model:value="candidateForm.workYears" :min="0" :max="60" :addon-after="$t('hr.recruit.years')" class="full" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('hr.recruit.source')" name="source"><a-select v-model:value="candidateForm.source" allow-clear :options="toOptions(dict.hr_candidate_source)" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('hr.recruit.expectedSalary')" name="expectedSalary"><a-input-number v-model:value="candidateForm.expectedSalary" :min="0" :precision="0" class="full" /></a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('hr.recruit.resume')" name="resume"><a-textarea v-model:value="candidateForm.resume" :rows="5" :maxlength="2000" :placeholder="$t('hr.recruit.resumePh')" /></a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark"><a-input v-model:value="candidateForm.remark" :maxlength="255" /></a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import FormDrawer from '@/components/FormDrawer.vue'
import { GENDERS } from '@/components/hr/hr'
import { addCandidate, getCandidate, updateCandidate } from '@/api/hr'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'
import { t } from '@/locales'

/** 新增 / 编辑应聘者：openOptions 为招聘中的需求，通过 ref 调用 openCreate(defaults) / openEdit(record)，保存成功后触发 success */
defineProps({ openOptions: { type: Array, default: () => [] } })
const emit = defineEmits(['success'])
const dict = useDict('hr_candidate_source', 'hr_education')

const candidateRules = {
  recruitId: [{ required: true, message: t('hr.recruit.recruitRequired') }],
  name: [{ required: true, whitespace: true, message: t('hr.employee.nameRequired') }],
  email: [{ type: 'email', message: t('common.rule.email') }]
}
const CANDIDATE_FIELDS = ['recruitId', 'name', 'gender', 'phone', 'email', 'education', 'school', 'workYears', 'source', 'expectedSalary',
  'resume', 'remark']
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, ...Object.fromEntries(CANDIDATE_FIELDS.map((f) => [f, undefined])), gender: 'M' }),
  fetch: (r) => getCandidate(r.id),
  create: addCandidate,
  update: updateCandidate,
  toPayload: (f) => Object.fromEntries(CANDIDATE_FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: () => emit('success')
})
const candidateFormRef = drawer.formRef
const candidateForm = drawer.form

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
</style>
