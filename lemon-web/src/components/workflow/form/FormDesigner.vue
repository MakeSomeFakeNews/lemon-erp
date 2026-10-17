<template>
  <div class="form-designer">
    <div class="form-designer-edit">
      <a-empty v-if="!fields.length" :description="$t('workflow.form.emptyTip')" />
      <div v-for="(f, i) in fields" :key="i" class="field-card">
        <div class="field-index">{{ i + 1 }}</div>
        <div class="field-main">
          <a-row :gutter="[12, 8]">
            <a-col :xs="24" :sm="12">
              <a-input v-model:value="f.label" :maxlength="32" :placeholder="$t('workflow.form.labelPh')" :addon-before="$t('common.field.name')" />
            </a-col>
            <a-col :xs="24" :sm="12">
              <a-input v-model:value="f.field" :maxlength="32" :placeholder="$t('workflow.form.keyPh')" :addon-before="$t('workflow.form.key')" />
            </a-col>
            <a-col :xs="24" :sm="12">
              <a-select v-model:value="f.type" :options="FIELD_TYPES" class="full" />
            </a-col>
            <a-col :xs="24" :sm="12">
              <a-input v-model:value="f.placeholder" :maxlength="64" :placeholder="$t('workflow.form.placeholderPh')" />
            </a-col>
            <a-col v-if="f.type === FormFieldType.SELECT" :span="24">
              <a-select
                v-model:value="f.options"
                mode="tags"
                :placeholder="$t('workflow.form.optionsPh')"
                :token-separators="[',', '，']"
                :open="false"
                class="full"
              />
            </a-col>
          </a-row>
        </div>
        <div class="field-actions">
          <a-checkbox v-model:checked="f.required">{{ $t('workflow.form.required') }}</a-checkbox>
          <a-space :size="0">
            <a-button type="text" size="small" :disabled="i === 0" :title="$t('workflow.form.moveUp')" @click="move(i, -1)">
              <template #icon><ArrowUpOutlined /></template>
            </a-button>
            <a-button type="text" size="small" :disabled="i === fields.length - 1" :title="$t('workflow.form.moveDown')" @click="move(i, 1)">
              <template #icon><ArrowDownOutlined /></template>
            </a-button>
            <a-button type="text" size="small" danger :title="$t('common.action.delete')" @click="fields.splice(i, 1)">
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </a-space>
        </div>
      </div>
      <a-button type="dashed" block @click="fields.push(createField(fields))">
        <template #icon><PlusOutlined /></template>{{ $t('workflow.form.addField') }}
      </a-button>
    </div>

    <a-card size="small" :title="$t('common.action.preview')" :bordered="false" class="form-designer-preview">
      <FormRender v-if="fields.length" :fields="fields" :model="previewModel" />
      <a-empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" :description="$t('workflow.form.noField')" />
    </a-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { Empty } from 'ant-design-vue'
import { ArrowDownOutlined, ArrowUpOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormRender from './FormRender.vue'
import { FIELD_TYPES, createField } from './form'

import { FormFieldType } from '@/constants/enums'
/** 流程表单设计：fields 为 FormField[]，直接修改 */
const props = defineProps({ fields: { type: Array, required: true } })

const previewModel = reactive({})

function move(i, offset) {
  const list = props.fields
  ;[list[i], list[i + offset]] = [list[i + offset], list[i]]
}
</script>

<style scoped>
.form-designer {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.form-designer-edit {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.form-designer-preview {
  width: 360px;
  flex: none;
  position: sticky;
  top: 0;
  background: var(--app-bg);
}
.field-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: var(--app-card);
}
.field-index {
  width: 22px;
  height: 22px;
  flex: none;
  border-radius: 50%;
  background: var(--app-primary-bg);
  color: var(--app-primary);
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 5px;
}
.field-main {
  flex: 1;
  min-width: 0;
}
.field-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  gap: 8px;
  flex: none;
}
.full {
  width: 100%;
}
@media (max-width: 991px) {
  .form-designer {
    flex-direction: column;
  }
  .form-designer-preview {
    width: 100%;
    position: static;
  }
}
</style>
