<template>
  <FormDrawer :drawer="drawer" :create-title="$t('hr.recruit.addRecruit')" :edit-title="$t('hr.recruit.editRecruit')">
    <a-form ref="recruitFormRef" :model="recruitForm" :rules="recruitRules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="24">
          <a-form-item :label="$t('hr.recruit.title')" name="title"><a-input v-model:value="recruitForm.title" :maxlength="128" :placeholder="$t('hr.recruit.titlePh')" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('hr.recruit.dept')" name="deptId"><DeptSelect v-model:value="recruitForm.deptId" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('hr.employee.position')" name="positionId"><PositionSelect v-model:value="recruitForm.positionId" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('hr.recruit.headcount')" name="headcount"><a-input-number v-model:value="recruitForm.headcount" :min="1" :max="999" class="full" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('hr.recruit.salaryRange')" name="salaryRange"><a-input v-model:value="recruitForm.salaryRange" :maxlength="64" :placeholder="$t('hr.recruit.salaryRangePh')" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('hr.recruit.deadline')" name="deadline"><a-date-picker v-model:value="recruitForm.deadline" value-format="YYYY-MM-DD" class="full" /></a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('hr.recruit.reason')" name="reason"><a-input v-model:value="recruitForm.reason" :maxlength="255" :placeholder="$t('hr.recruit.reasonPh')" /></a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('hr.recruit.requirement')" name="requirement"><a-textarea v-model:value="recruitForm.requirement" :rows="4" :maxlength="1000" /></a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import FormDrawer from '@/components/FormDrawer.vue'
import DeptSelect from '@/components/fin/DeptSelect.vue'
import PositionSelect from '@/components/hr/PositionSelect.vue'
import { addRecruit, getRecruit, updateRecruit } from '@/api/hr'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { t } from '@/locales'

/** 新增 / 编辑招聘需求：通过 ref 调用 openCreate() / openEdit(record)，保存成功后触发 success */
const emit = defineEmits(['success'])

const recruitRules = {
  title: [{ required: true, whitespace: true, message: t('hr.recruit.titleRequired') }],
  deptId: [{ required: true, message: t('hr.recruit.deptRequired') }],
  headcount: [{ required: true, message: t('hr.recruit.headcountRequired') }]
}
const RECRUIT_FIELDS = ['title', 'deptId', 'positionId', 'headcount', 'salaryRange', 'requirement', 'reason', 'deadline']
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, ...Object.fromEntries(RECRUIT_FIELDS.map((f) => [f, undefined])), headcount: 1 }),
  fetch: (r) => getRecruit(r.id),
  create: addRecruit,
  update: updateRecruit,
  toPayload: (f) => Object.fromEntries(RECRUIT_FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: () => emit('success')
})
const recruitFormRef = drawer.formRef
const recruitForm = drawer.form

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
</style>
