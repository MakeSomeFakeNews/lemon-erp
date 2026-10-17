<template>
  <div class="im-page">
    <div class="im-card">
      <template v-if="!error">
        <a-spin size="large" />
        <div class="im-text">{{ bindMode ? '正在绑定…' : '正在登录…' }}</div>
      </template>
      <a-result v-else status="warning" :title="bindMode ? '绑定失败' : '登录失败'" :sub-title="error">
        <template #extra>
          <a-button type="primary" @click="back">{{ getToken() ? '返回个人中心' : '返回登录' }}</a-button>
        </template>
      </a-result>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { imCallback } from '@/api/system/im'
import { useUserStore } from '@/store/user'
import { getToken, setToken } from '@/utils/auth'
import { imPlatform } from '@/utils/im'

/**
 * 钉钉、企业微信、飞书授权后跳回的页面：把授权码交给后端，登录（保存令牌）或完成个人中心的账号绑定。
 * 钉钉的授权码参数为 authCode，其他平台为 code
 */
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const error = ref('')
/** 已登录时视为绑定（登录流程不会带着令牌进入） */
const bindMode = ref(!!getToken())

function back() {
  router.replace(getToken() ? { path: '/profile', query: { tab: 'im' } } : '/login')
}

onMounted(async () => {
  const code = route.query.authCode || route.query.code
  const state = route.query.state
  if (!code || !state) {
    error.value = '没有收到授权码，可能是取消了授权'
    return
  }
  try {
    const res = await imCallback({ code, state })
    const label = imPlatform(res.platform).label
    if (res.bind) {
      message.success(`已绑定${label}账号`)
      router.replace({ path: '/profile', query: { tab: 'im' } })
      return
    }
    // 重新加载当前用户的菜单和路由
    userStore.reset()
    setToken(res.token)
    userStore.token = res.token
    if (res.passwordExpired) {
      message.warning('当前密码为管理员设置的初始密码或已过期，请先修改密码', 4)
      router.replace({ path: '/profile', query: { tab: 'password' } })
    } else {
      message.success('登录成功')
      router.replace(res.redirect || '/')
    }
  } catch (e) {
    error.value = e?.message || '授权失败'
  }
})
</script>

<style scoped>
.im-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: var(--app-bg);
}
.im-card {
  width: 100%;
  max-width: 480px;
  padding: 40px 24px;
  border-radius: 16px;
  background: var(--app-card);
  box-shadow: var(--app-shadow);
  text-align: center;
}
.im-text {
  margin-top: 16px;
  color: var(--app-text-2);
}
</style>
