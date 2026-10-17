<template>
  <a-form ref="formRef" :model="model" layout="vertical">
    <a-form-item
      v-for="f in fields"
      :key="f.field"
      :label="f.label"
      :name="f.field"
      :rules="f.required ? [{ required: true, message: f.type === 'SELECT' || f.type === 'DATE' ? $t('common.rule.requiredSelect', { name: f.label }) : $t('common.rule.required', { name: f.label }) }] : []"
    >
      <a-input-number
        v-if="f.type === 'NUMBER'"
        v-model:value="model[f.field]"
        :placeholder="f.placeholder || $t('workflow.form.inputNumber')"
        class="full"
      />
      <a-date-picker
        v-else-if="f.type === 'DATE'"
        v-model:value="model[f.field]"
        value-format="YYYY-MM-DD"
        :placeholder="f.placeholder || $t('workflow.form.selectDate')"
        class="full"
      />
      <a-select
        v-else-if="f.type === 'SELECT'"
        v-model:value="model[f.field]"
        :placeholder="f.placeholder || $t('common.placeholder.select')"
        :options="(f.options || []).map((o) => ({ label: o, value: o }))"
        allow-clear
      />
      <a-textarea
        v-else-if="f.type === 'TEXTAREA'"
        v-model:value="model[f.field]"
        :placeholder="f.placeholder || $t('common.placeholder.input')"
        :rows="4"
        :maxlength="2000"
        show-count
      />
      <a-input v-else v-model:value="model[f.field]" :placeholder="f.placeholder || $t('common.placeholder.input')" :maxlength="200" />
    </a-form-item>
  </a-form>
</template>

<script setup>
import { ref } from 'vue'

/** 按 FormField[] 渲染表单，填写的值写入 model（reactive 对象） */
defineProps({
  fields: { type: Array, default: () => [] },
  model: { type: Object, required: true }
})

const formRef = ref()
defineExpose({ validate: () => formRef.value.validate() })
</script>

<style scoped>
.full {
  width: 100%;
}
</style>
