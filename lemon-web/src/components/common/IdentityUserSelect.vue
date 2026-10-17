<template>
  <UserSelect :value="value" :label="label" :placeholder="placeholder" :fetcher="pageIdentityUsers" @update:value="(v) => emit('update:value', v)" />
</template>

<script setup>
import { t } from '@/locales'
import UserSelect from '@/components/user/UserSelect.vue'
import { pageIdentityUsers } from '@/api/workflow/identity'

/**
 * 选择用户（负责人、处理人、项目成员等）：使用登录即可访问的用户搜索接口，不要求用户管理权限
 */
defineProps({
  value: Number,
  label: String,
  placeholder: { type: String, default: () => t('components.user.searchPlaceholder') }
})
const emit = defineEmits(['update:value'])
</script>
