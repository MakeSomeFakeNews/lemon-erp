<template>
  <div class="condition-editor">
    <a-alert
      v-if="!fields.length"
      type="info"
      show-icon
      message="表单中还没有字段，可直接填写流程变量名（业务单据发起流程时传入的变量）"
      class="condition-tip"
    />
    <template v-for="(group, gi) in groups" :key="gi">
      <div v-if="gi > 0" class="group-or">或</div>
      <div class="group">
        <div class="group-head">
          <span>条件组 {{ gi + 1 }}<span class="group-hint">（组内条件需同时满足）</span></span>
          <a v-if="groups.length > 1" class="danger-link" @click="groups.splice(gi, 1)">删除条件组</a>
        </div>
        <div v-for="(rule, ri) in group" :key="ri" class="rule">
          <a-select
            v-if="fields.length"
            v-model:value="rule.field"
            placeholder="字段"
            class="rule-field"
            :options="fieldOptions"
            @change="onFieldChange(rule)"
          />
          <a-input v-else v-model:value="rule.field" placeholder="变量名" class="rule-field" />
          <a-select v-model:value="rule.operator" class="rule-op" :options="operatorOptions(fieldOf(rule)?.type)" />
          <a-select
            v-if="fieldOf(rule)?.type === 'SELECT' && rule.operator !== 'IN'"
            v-model:value="rule.value"
            placeholder="值"
            class="rule-value"
            :options="toSelectOptions(fieldOf(rule).options)"
          />
          <a-date-picker
            v-else-if="fieldOf(rule)?.type === 'DATE'"
            v-model:value="rule.value"
            value-format="YYYY-MM-DD"
            placeholder="日期"
            class="rule-value"
          />
          <a-input
            v-else
            v-model:value="rule.value"
            :placeholder="rule.operator === 'IN' ? '多个值用逗号分隔' : '值'"
            class="rule-value"
          />
          <a-button type="text" size="small" danger :disabled="group.length === 1 && groups.length === 1" @click="removeRule(gi, ri)">
            <template #icon><MinusCircleOutlined /></template>
          </a-button>
        </div>
        <a-button type="dashed" size="small" block @click="group.push(emptyRule())">
          <template #icon><PlusOutlined /></template>添加条件（且）
        </a-button>
      </div>
    </template>
    <a-button type="link" class="add-group" @click="groups.push([emptyRule()])">
      <template #icon><PlusOutlined /></template>添加条件组（或）
    </a-button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { emptyRule, operatorOptions, toSelectOptions } from './flow'

/** 条件编辑：条件组之间为「或」，组内条件为「且」。groups 直接修改（ConditionRule[][]） */
const props = defineProps({
  groups: { type: Array, required: true },
  fields: { type: Array, default: () => [] }
})

const fieldOptions = computed(() => props.fields.map((f) => ({ label: `${f.label}（${f.field}）`, value: f.field })))
const fieldOf = (rule) => props.fields.find((f) => f.field === rule.field)

function onFieldChange(rule) {
  const allowed = operatorOptions(fieldOf(rule)?.type).map((o) => o.value)
  if (!allowed.includes(rule.operator)) rule.operator = allowed[0]
  rule.value = ''
}

function removeRule(gi, ri) {
  const group = props.groups[gi]
  group.splice(ri, 1)
  if (!group.length) props.groups.splice(gi, 1)
}
</script>

<style scoped>
.condition-tip {
  margin-bottom: 12px;
}
.group {
  padding: 12px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.group-head {
  display: flex;
  justify-content: space-between;
  font-weight: 500;
}
.group-hint {
  color: var(--app-text-3);
  font-weight: normal;
  font-size: 12px;
}
.danger-link {
  color: #ff4d4f;
  font-weight: normal;
}
.group-or {
  margin: 8px 0;
  text-align: center;
  color: var(--app-text-3);
}
.rule {
  display: flex;
  gap: 6px;
  align-items: center;
}
.rule-field {
  flex: 1.2;
  min-width: 0;
}
.rule-op {
  width: 104px;
  flex: none;
}
.rule-value {
  flex: 1;
  min-width: 0;
}
.add-group {
  padding-left: 0;
  margin-top: 4px;
}
</style>
