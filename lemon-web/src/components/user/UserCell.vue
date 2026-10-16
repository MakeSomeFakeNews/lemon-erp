<template>
  <div class="user-cell">
    <UserAvatar :src="user.avatar" :name="user.nickname || user.username" size="small" />
    <div class="user-cell-name">
      <span>{{ user.username }}</span>
      <span v-if="user.nickname" class="user-cell-sub">{{ user.nickname }}</span>
    </div>
  </div>
</template>

<script setup>
import UserAvatar from '@/components/UserAvatar.vue'

/** 表格中的用户单元格：头像 + 用户名 + 昵称 */
defineProps({
  user: { type: Object, required: true }
})
</script>

<style scoped>
.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.user-cell-name {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
  min-width: 0;
}
.user-cell-sub {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
