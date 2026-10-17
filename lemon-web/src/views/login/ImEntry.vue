<template>
  <div class="im-page">
    <div class="im-card">
      <template v-if="!error">
        <a-spin size="large" />
        <div class="im-text">正在进入{{ label }}…</div>
      </template>
      <a-result v-else status="warning" title="无法自动登录" :sub-title="error">
        <template #extra>
          <a-button type="primary" @click="router.replace('/login')">使用账号密码登录</a-button>
        </template>
      </a-result>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getImAuthorizeUrl } from '@/api/system/im'
import { getToken, removeToken } from '@/utils/auth'
import { detectImApp, imPlatform } from '@/utils/im'

/**
 * 免登入口：钉钉、企业微信、飞书的应用首页和消息链接指向这里（?platform=&tenant=&redirect=）。
 * 已登录（令牌有效）直接进入目标页面；否则跳到平台授权（客户端内静默授权，浏览器中扫码），授权后由回调页登录
 */
const route = useRoute()
const router = useRouter()
const error = ref('')
const platform = computed(() => route.query.platform)
const label = computed(() => (platform.value ? imPlatform(platform.value).label : '系统'))
const redirect = computed(() => {
  const r = route.query.redirect
  return typeof r === 'string' && r.startsWith('/') && !r.startsWith('//') ? r : '/'
})

/** 本地令牌是否仍有效（直接请求，不经过 request 拦截器的「登录已过期」跳转） */
async function tokenValid() {
  const token = getToken()
  if (!token) return false
  try {
    const res = await fetch('/api/auth/info', { headers: { Authorization: `Bearer ${token}` } })
    return (await res.json()).code === 200
  } catch (e) {
    return false
  }
}

onMounted(async () => {
  if (await tokenValid()) {
    router.replace(redirect.value)
    return
  }
  removeToken()
  if (!platform.value) {
    router.replace({ path: '/login', query: { redirect: redirect.value } })
    return
  }
  try {
    location.href = await getImAuthorizeUrl(platform.value, {
      tenantId: route.query.tenant,
      inApp: detectImApp() === platform.value,
      redirect: redirect.value
    })
  } catch (e) {
    error.value = e?.message || '获取授权地址失败'
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
