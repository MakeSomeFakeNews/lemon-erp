<template>
  <div class="form-designer">
    <div class="form-designer-edit">
      <a-empty v-if="!fields.length" description="还没有字段，点击下方按钮添加" />
      <div v-for="(f, i) in fields" :key="i" class="field-card">
        <div class="field-index">{{ i + 1 }}</div>
        <div class="field-main">
          <a-row :gutter="[12, 8]">
            <a-col :xs="24" :sm="12">
              <a-input v-model:value="f.label" :maxlength="32" placeholder="字段名称，如：采购金额" addon-before="名称" />
            </a-col>
            <a-col :xs="24" :sm="12">
              <a-input v-model:value="f.field" :maxlength="32" placeholder="英文标识，如：days" addon-before="标识" />
            </a-col>
            <a-col :xs="24" :sm="12">
              <a-select v-model:value="f.type" :options="FIELD_TYPES" class="full" />
            </a-col>
            <a-col :xs="24" :sm="12">
              <a-input v-model:value="f.placeholder" :maxlength="64" placeholder="输入提示（可选）" />
            </a-col>
            <a-col v-if="f.type === 'SELECT'" :span="24">
              <a-select
                v-model:value="f.options"
                mode="tags"
                placeholder="输入选项后回车，如：年假、事假"
                :token-separators="[',', '，']"
                :open="false"
                class="full"
              />
            </a-col>
          </a-row>
        </div>
        <div class="field-actions">
          <a-checkbox v-model:checked="f.required">必填</a-checkbox>
          <a-space :size="0">
            <a-button type="text" size="small" :disabled="i === 0" title="上移" @click="move(i, -1)">
              <template #icon><ArrowUpOutlined /></template>
            </a-button>
            <a-button type="text" size="small" :disabled="i === fields.length - 1" title="下移" @click="move(i, 1)">
              <template #icon><ArrowDownOutlined /></template>
            </a-button>
            <a-button type="text" size="small" danger title="删除" @click="fields.splice(i, 1)">
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </a-space>
        </div>
      </div>
      <a-button type="dashed" block @click="fields.push(createField(fields))">
        <template #icon><PlusOutlined /></template>添加字段
      </a-button>
    </div>

    <a-card size="small" title="预览" :bordered="false" class="form-designer-preview">
      <FormRender v-if="fields.length" :fields="fields" :model="previewModel" />
      <a-empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" description="暂无字段" />
    </a-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { Empty } from 'ant-design-vue'
import { ArrowDownOutlined, ArrowUpOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormRender from './FormRender.vue'
import { FIELD_TYPES, createField } from './form'

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
