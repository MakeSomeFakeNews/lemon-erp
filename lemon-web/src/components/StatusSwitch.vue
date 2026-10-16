<template>
  <a-switch
    :checked="value === 1"
    :loading="loading"
    :disabled="disabled"
    checked-children="启用"
    un-checked-children="停用"
    @click="toggle"
  />
</template>

<script setup>
import { ref } from 'vue'
import { Modal, message } from 'ant-design-vue'

/**
 * 表格内状态开关：切换前确认，调用 api(newStatus) 成功后通知父组件更新
 */
const props = defineProps({
  value: Number,
  name: { type: String, default: '' },
  api: { type: Function, required: true },
  disabled: Boolean
})
const emit = defineEmits(['changed'])
const loading = ref(false)

function toggle() {
  const next = props.value === 1 ? 0 : 1
  const action = next === 1 ? '启用' : '停用'
  Modal.confirm({
    title: `确认${action}`,
    content: `确定要${action}「${props.name}」吗？`,
    okType: next === 1 ? 'primary' : 'danger',
    async onOk() {
      loading.value = true
      try {
        await props.api(next)
        message.success(`已${action}`)
        emit('changed', next)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>
