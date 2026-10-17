<template>
  <template v-if="fields.length">
    <div v-if="title" class="cf-title">{{ title }}</div>
    <a-row :gutter="16">
      <a-col v-for="f in fields" :key="f.fieldKey" :xs="24" :sm="f.fieldType === 'TEXTAREA' ? 24 : span">
        <a-form-item :label="f.label" :required="f.required === 1" :extra="f.remark || undefined">
          <a-input v-if="f.fieldType === 'TEXT'" v-model:value="values[f.fieldKey]" :maxlength="1000" :disabled="disabled" />
          <a-textarea v-else-if="f.fieldType === 'TEXTAREA'" v-model:value="values[f.fieldKey]" :rows="2" :maxlength="1000" :disabled="disabled" />
          <a-input-number
            v-else-if="f.fieldType === 'NUMBER'"
            :value="values[f.fieldKey] === undefined || values[f.fieldKey] === '' ? undefined : Number(values[f.fieldKey])"
            :disabled="disabled"
            class="full"
            @update:value="(v) => (values[f.fieldKey] = v == null ? '' : String(v))"
          />
          <a-date-picker v-else-if="f.fieldType === 'DATE'" v-model:value="values[f.fieldKey]" value-format="YYYY-MM-DD" :disabled="disabled" class="full" />
          <a-select
            v-else-if="f.fieldType === 'SELECT'"
            v-model:value="values[f.fieldKey]"
            :options="optionsOf(f).map((o) => ({ value: o, label: o }))"
            allow-clear
            :disabled="disabled"
          />
          <a-switch
            v-else-if="f.fieldType === 'BOOL'"
            :checked="values[f.fieldKey] === 'true'"
            :disabled="disabled"
            @update:checked="(v) => (values[f.fieldKey] = String(v))"
          />
        </a-form-item>
      </a-col>
    </a-row>
  </template>
</template>

<script setup>
import { optionsOf } from '@/composables/useCustomFields'

/**
 * 自定义字段的输入项：fields 为启用的定义，values 为 { 字段标识: 值 }（直接修改），配合 useCustomFields 使用
 */
defineProps({
  fields: { type: Array, default: () => [] },
  values: { type: Object, required: true },
  title: String,
  /** 每项占的栅格（sm 以上） */
  span: { type: Number, default: 12 },
  disabled: Boolean
})
</script>

<style scoped>
.cf-title {
  font-weight: 500;
  margin: 4px 0 8px;
}
.full {
  width: 100%;
}
</style>
