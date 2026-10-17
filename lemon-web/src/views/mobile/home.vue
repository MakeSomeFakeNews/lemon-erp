<template>
  <div>
    <div class="m-user">
      <div>
        <div class="m-name">{{ userStore.displayName }}</div>
        <div class="sub-text">{{ userStore.tenantName || '' }}</div>
      </div>
      <a-button size="small" @click="router.push('/')">{{ $t('mobile.home.backToAdmin') }}</a-button>
    </div>
    <div v-if="pages.length" class="m-tiles">
      <div v-for="p in pages" :key="p.path" class="m-tile" @click="router.push(p.path)">
        <MenuIcon :name="p.icon" class="m-tile-icon" />
        <div>{{ p.title }}</div>
      </div>
    </div>
    <a-empty v-else :description="$t('mobile.home.empty')" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import MenuIcon from '@/components/MenuIcon.vue'
import { MOBILE_PATH } from '@/router'
import { useUserStore } from '@/store/user'
import { flattenPages } from '@/utils/menu'
import { joinPath } from '@/utils/path'

/** 移动作业首页：按「移动作业」模块下有权限的页面显示功能宫格 */
const router = useRouter()
const userStore = useUserStore()
const pages = computed(() => {
  const module = (userStore.menus || []).find((m) => joinPath('', m.path) === MOBILE_PATH)
  return module ? flattenPages(module.children || [], MOBILE_PATH) : []
})
</script>

<style scoped>
.m-user {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 12px;
  background: var(--app-card);
  border-radius: 8px;
}
.m-name {
  font-size: 16px;
  font-weight: 500;
}
.m-tiles {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.m-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 24px 8px;
  background: var(--app-card);
  border-radius: 8px;
  font-size: 15px;
  cursor: pointer;
}
.m-tile:active {
  background: var(--app-hover);
}
.m-tile-icon {
  font-size: 32px;
  color: var(--app-primary);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
