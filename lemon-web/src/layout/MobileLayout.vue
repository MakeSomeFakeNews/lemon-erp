<template>
  <div class="mobile-layout">
    <header class="m-header">
      <a-button v-if="!isHome" type="text" class="m-nav" @click="back"><LeftOutlined /></a-button>
      <span v-else class="m-nav" />
      <div class="m-title">{{ route.meta.title }}</div>
      <a-button v-if="!isHome" type="text" class="m-nav" @click="router.push(MOBILE_PATH)"><AppstoreOutlined /></a-button>
      <span v-else class="m-nav" />
    </header>
    <main class="m-body">
      <router-view />
    </main>
    <Watermark />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AppstoreOutlined, LeftOutlined } from '@ant-design/icons-vue'
import Watermark from '@/components/Watermark.vue'
import { MOBILE_PATH } from '@/router'

/** 移动作业（手机 / PDA）布局：顶部标题栏 + 全屏内容，没有后台的导航和标签页 */
const route = useRoute()
const router = useRouter()
const isHome = computed(() => route.path === MOBILE_PATH)
const back = () => (window.history.state?.back ? router.back() : router.push(MOBILE_PATH))
</script>

<style scoped>
.mobile-layout {
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  background: var(--app-bg);
}
.m-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  height: 48px;
  padding: 0 4px;
  background: var(--app-primary);
  color: #fff;
}
.m-title {
  flex: 1;
  text-align: center;
  font-size: 17px;
  font-weight: 500;
}
.m-nav {
  width: 44px;
  color: #fff !important;
  font-size: 18px;
}
.m-body {
  flex: 1;
  padding: 12px;
  max-width: 640px;
  width: 100%;
  margin: 0 auto;
}
</style>
