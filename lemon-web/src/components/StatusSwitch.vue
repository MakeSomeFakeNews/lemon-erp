<template>
  <a-switch
    :checked="value === 1"
    :loading="loading"
    :disabled="disabled"
    :checked-children="$t('common.status.enabled')"
    :un-checked-children="$t('common.status.disabled')"
    @click="toggle"
  />
</template>

<script setup>
import { ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'

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
const { t } = useI18n()
const loading = ref(false)

function toggle() {
  const next = props.value === 1 ? 0 : 1
  const enable = next === 1
  Modal.confirm({
    title: enable ? t('components.status.confirmEnable') : t('components.status.confirmDisable'),
    content: enable ? t('components.status.enableContent', { name: props.name }) : t('components.status.disableContent', { name: props.name }),
    okType: next === 1 ? 'primary' : 'danger',
    async onOk() {
      loading.value = true
      try {
        await props.api(next)
        message.success(enable ? t('components.status.enabled') : t('components.status.disabled'))
        emit('changed', next)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>
