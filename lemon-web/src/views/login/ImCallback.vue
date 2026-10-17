<template>
  <div class="im-page">
    <div class="im-card">
      <template v-if="!error">
        <a-spin size="large" />
        <div class="im-text">{{ bindMode ? $t('login.im.binding') : $t('login.im.loggingIn') }}</div>
      </template>
      <a-result v-else status="warning" :title="bindMode ? $t('login.im.bindFailed') : $t('login.im.loginFailed')" :sub-title="error">
        <template #extra>
          <a-button type="primary" @click="back">{{ getToken() ? $t('login.im.backProfile') : $t('login.im.backLogin') }}</a-button>
        </template>
      </a-result>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { imCallback } from '@/api/system/im'
import { useUserStore } from '@/store/user'
import { getToken, setToken } from '@/utils/auth'
import { imPlatform } from '@/utils/im'

/**
 * 钉钉、企业微信、飞书授权后跳回的页面：把授权码交给后端，登录（保存令牌）或完成个人中心的账号绑定。
 * 钉钉的授权码参数为 authCode，其他平台为 code
 */
const { t } = useI18n()
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
    error.value = t('login.im.noCode')
    return
  }
  try {
    const res = await imCallback({ code, state })
    const label = imPlatform(res.platform).label
    if (res.bind) {
      message.success(t('login.im.bound', { name: label }))
      router.replace({ path: '/profile', query: { tab: 'im' } })
      return
    }
    // 重新加载当前用户的菜单和路由
    userStore.reset()
    setToken(res.token)
    userStore.token = res.token
    if (res.passwordExpired) {
      message.warning(t('login.mustChange'), 4)
      router.replace({ path: '/profile', query: { tab: 'password' } })
    } else {
      message.success(t('login.success'))
      router.replace(res.redirect || '/')
    }
  } catch (e) {
    error.value = e?.message || t('login.im.authFailed')
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
