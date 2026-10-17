<template>
  <div class="login-page">
    <!-- 背景：柔和光斑 + 柠檬切片装饰 -->
    <div class="bg" aria-hidden="true">
      <span class="glow glow-a" />
      <span class="glow glow-b" />
      <span class="glow glow-c" />
      <svg class="slice slice-lg" viewBox="0 0 200 200">
        <circle cx="100" cy="100" r="96" fill="#ffd43b" />
        <circle cx="100" cy="100" r="84" fill="#fff9db" />
        <circle cx="100" cy="100" r="74" fill="#ffe066" />
        <g stroke="#fff9db" stroke-width="7" stroke-linecap="round">
          <path d="M100 26v148M26 100h148M47.7 47.7l104.6 104.6M152.3 47.7L47.7 152.3" />
        </g>
        <circle cx="100" cy="100" r="11" fill="#fff9db" />
      </svg>
      <svg class="slice slice-sm" viewBox="0 0 200 200">
        <circle cx="100" cy="100" r="96" fill="#94d82d" />
        <circle cx="100" cy="100" r="84" fill="#f4fce3" />
        <circle cx="100" cy="100" r="74" fill="#c0eb75" />
        <g stroke="#f4fce3" stroke-width="7" stroke-linecap="round">
          <path d="M100 26v148M26 100h148M47.7 47.7l104.6 104.6M152.3 47.7L47.7 152.3" />
        </g>
        <circle cx="100" cy="100" r="11" fill="#f4fce3" />
      </svg>
    </div>

    <div class="top-actions">
      <LangSwitch btn-class="top-btn" show-label />
      <a-tooltip :title="appStore.dark ? $t('layout.toLight') : $t('layout.toDark')">
        <a-button type="text" shape="circle" class="top-btn" @click="appStore.toggleDark()">
          <template #icon>
            <ThemeIcon :dark="appStore.dark" />
          </template>
        </a-button>
      </a-tooltip>
    </div>

    <main class="container">
      <!-- 品牌区（窄屏隐藏） -->
      <section class="brand">
        <div class="brand-logo">
          <AppLogo :size="48" />
          <div>
            <div class="brand-name">{{ configStore.systemName }}</div>
            <div class="brand-en">LEMON ERP</div>
          </div>
        </div>
        <h1 class="brand-title">{{ $t('login.brandLine1') }}<br />{{ $t('login.brandLine2') }}<span class="highlight">{{ $t('login.brandHighlight') }}</span></h1>
        <p class="brand-desc">{{ $t('login.brandDesc') }}</p>
        <div class="modules">
          <div v-for="m in modules" :key="m.name" class="module">
            <span class="module-icon" :style="{ color: m.color, background: `${m.color}1a` }"><component :is="m.icon" /></span>
            <span class="module-name">{{ m.name }}</span>
          </div>
        </div>
        <div class="brand-tags">
          <span>{{ $t('login.tagWorkflow') }}</span>
          <span>{{ $t('login.tagDataPerm') }}</span>
          <span>{{ $t('login.tagCurrency') }}</span>
          <span>{{ $t('login.tagCost') }}</span>
          <span>{{ $t('login.tagAnalytics') }}</span>
        </div>
      </section>

      <!-- 登录表单 -->
      <section class="card">
        <div class="mobile-logo">
          <AppLogo :size="36" />
          <span>{{ configStore.systemName }}</span>
        </div>
        <h2 class="form-title">{{ $t('login.welcome') }} <span class="wave">👋</span></h2>
        <p class="form-subtitle">{{ $t('login.subtitle', { name: configStore.systemName }) }}</p>

        <a-form :model="form" :rules="rules" layout="vertical" size="large" @finish="handleLogin">
          <a-form-item v-if="tenantEnabled" :label="$t('login.tenant')" name="tenantName">
            <a-input v-model:value="form.tenantName" :placeholder="$t('login.inputTenant')" autocomplete="organization">
              <template #prefix><ShopOutlined class="input-icon" /></template>
            </a-input>
          </a-form-item>
          <a-form-item :label="$t('login.username')" name="username">
            <a-input v-model:value="form.username" :placeholder="$t('login.inputUsername')" autocomplete="username">
              <template #prefix><UserOutlined class="input-icon" /></template>
            </a-input>
          </a-form-item>
          <a-form-item :label="$t('login.password')" name="password">
            <a-input-password v-model:value="form.password" :placeholder="$t('login.inputPassword')" autocomplete="current-password">
              <template #prefix><LockOutlined class="input-icon" /></template>
            </a-input-password>
          </a-form-item>
          <a-form-item v-if="needCaptcha" :label="$t('login.captcha')" name="captchaCode">
            <div class="captcha-row">
              <a-input v-model:value="form.captchaCode" :placeholder="$t('login.inputCaptcha')" autocomplete="off" :maxlength="8">
                <template #prefix><SafetyOutlined class="input-icon" /></template>
              </a-input>
              <img v-if="captchaImage" :src="captchaImage" class="captcha-img" :alt="$t('login.captcha')" :title="$t('login.refreshCaptcha')" @click="refreshCaptcha" />
            </div>
          </a-form-item>
          <div class="form-row">
            <a-checkbox v-model:checked="remember">{{ tenantEnabled ? $t('login.rememberTenant') : $t('login.rememberUsername') }}</a-checkbox>
          </div>
          <a-button type="primary" html-type="submit" block :loading="loading" class="submit-btn">
            {{ loading ? $t('login.loggingIn') : $t('login.login') }}
          </a-button>
        </a-form>

        <template v-if="imProviders.length">
          <div class="im-divider"><span>{{ $t('login.otherMethods') }}</span></div>
          <div class="im-logins">
            <a-tooltip v-for="p in imProviders" :key="p.platform" :title="$t('login.imLogin', { name: imPlatform(p.platform).label })">
              <button type="button" class="im-login" :disabled="!!imLoading" @click="imLogin(p.platform)">
                <a-spin v-if="imLoading === p.platform" size="small" />
                <ImPlatformIcon v-else :platform="p.platform" :size="34" />
                <span>{{ imPlatform(p.platform).label }}</span>
              </button>
            </a-tooltip>
          </div>
        </template>

        <div v-if="configStore.showDemoAccount" class="tip">
          <span class="tip-dot" />
          {{ $t('login.demoAccount') }}<template v-if="tenantEnabled">{{ $t('login.demoTenantPrefix') }}<b>{{ DEMO_TENANT }}</b>{{ $t('login.demoTenantSuffix') }}</template>
          <a class="demo-account" @click="fillDemo('admin')"><b>admin</b> / <b>admin123</b></a>{{ $t('login.demoAdmin') }}<a class="demo-account" @click="fillDemo('test')"><b>test</b> / <b>admin123</b></a>{{ $t('login.demoReadonly') }}
        </div>
        <div v-if="configStore.demoMode" class="tip">
          <span class="tip-dot" />
          {{ $t('layout.demoTip') }}
        </div>
      </section>
    </main>

    <footer class="copyright">© {{ year }} {{ configStore.systemName }} · Spring Boot · Vue 3 · Ant Design Vue</footer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import {
  AccountBookOutlined,
  ApartmentOutlined,
  BarChartOutlined,
  DatabaseOutlined,
  HeartOutlined,
  IdcardOutlined,
  LockOutlined,
  ProjectOutlined,
  SafetyOutlined,
  ShopOutlined,
  ShoppingCartOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import { getCaptcha } from '@/api/auth'
import { getImAuthorizeUrl, listImProviders } from '@/api/system/im'
import AppLogo from '@/components/AppLogo.vue'
import ImPlatformIcon from '@/components/im/ImPlatformIcon.vue'
import LangSwitch from '@/components/LangSwitch.vue'
import ThemeIcon from '@/components/ThemeIcon.vue'
import { useAppStore } from '@/store/app'
import { useConfigStore } from '@/store/config'
import { useUserStore } from '@/store/user'
import { imPlatform } from '@/utils/im'
import { load, remove, save } from '@/utils/storage'

import { CaptchaMode } from '@/constants/enums'
const REMEMBER_KEY = 'lemon_erp_remember_username'
const REMEMBER_TENANT_KEY = 'lemon_erp_remember_tenant'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()
const year = new Date().getFullYear()

const modules = [
  { icon: ShoppingCartOutlined, name: t('login.modScm'), color: '#1677ff' },
  { icon: ApartmentOutlined, name: t('login.modMes'), color: '#fa8c16' },
  { icon: DatabaseOutlined, name: t('login.modWms'), color: '#13c2c2' },
  { icon: AccountBookOutlined, name: t('login.modFin'), color: '#faad14' },
  { icon: IdcardOutlined, name: t('login.modHr'), color: '#eb2f96' },
  { icon: HeartOutlined, name: t('login.modCrm'), color: '#f5222d' },
  { icon: ProjectOutlined, name: t('login.modPm'), color: '#722ed1' },
  { icon: BarChartOutlined, name: t('login.modBi'), color: '#52c41a' }
]

const rememberedName = load(REMEMBER_KEY, '')
const remember = ref(!!rememberedName)
const loading = ref(false)
const form = reactive({ tenantName: load(REMEMBER_TENANT_KEY, ''), username: rememberedName, password: '', captchaId: undefined, captchaCode: '' })
const rules = {
  tenantName: [{ required: true, whitespace: true, message: t('login.inputTenant') }],
  username: [{ required: true, message: t('login.inputUsername') }],
  password: [{ required: true, message: t('login.inputPassword') }],
  captchaCode: [{ required: true, whitespace: true, message: t('login.inputCaptcha') }]
}

const configStore = useConfigStore()
/** SaaS 模式下登录需要填写租户 */
const tenantEnabled = computed(() => configStore.tenantEnabled)

// ---------------------------------------------------------------- 验证码
/** 「密码输错后」模式：本次登录失败过才显示验证码（后端按账号的失败记录判断，失败后要求验证码时同样显示） */
const failed = ref(false)
const needCaptcha = computed(() => configStore.captchaMode === CaptchaMode.ALWAYS || (configStore.captchaMode === CaptchaMode.AFTER_FAILURE && failed.value))
const captchaImage = ref('')
async function refreshCaptcha() {
  const c = await getCaptcha()
  form.captchaId = c.captchaId
  form.captchaCode = ''
  captchaImage.value = c.image
}
// ---------------------------------------------------------------- 演示账号
const DEMO_TENANT = '柠檬科技' // i18n-ignore 内置演示租户的名称（数据）
const DEMO_PASSWORD = 'admin123'
/** 点击演示账号填入表单；演示模式下默认填好超级管理员，直接点登录即可 */
function fillDemo(username) {
  Object.assign(form, { username, password: DEMO_PASSWORD })
  if (tenantEnabled.value) form.tenantName = DEMO_TENANT
}

// ---------------------------------------------------------------- 第三方登录（钉钉、企业微信、飞书）
/** SaaS 模式下按填写的租户显示该租户启用的登录方式 */
const imProviders = ref([])
const imLoading = ref(null)
let providerTimer
let providerSeq = 0
async function loadProviders() {
  const seq = ++providerSeq
  const name = form.tenantName?.trim()
  if (tenantEnabled.value && !name) {
    imProviders.value = []
    return
  }
  const list = await listImProviders(tenantEnabled.value ? name : undefined).catch(() => [])
  if (seq === providerSeq) imProviders.value = list
}
watch(
  () => form.tenantName,
  () => {
    clearTimeout(providerTimer)
    providerTimer = setTimeout(loadProviders, 400)
  }
)
async function imLogin(platform) {
  imLoading.value = platform
  try {
    location.href = await getImAuthorizeUrl(platform, {
      tenantName: tenantEnabled.value ? form.tenantName?.trim() : undefined,
      redirect: route.query.redirect
    })
  } catch (e) {
    imLoading.value = null
  }
}

onMounted(async () => {
  await configStore.load(true)
  if (configStore.demoMode && !form.password) fillDemo(form.username === 'test' ? 'test' : 'admin')
  if (needCaptcha.value) refreshCaptcha()
  loadProviders()
})

async function handleLogin() {
  loading.value = true
  try {
    const data = { ...form }
    if (!needCaptcha.value) Object.assign(data, { captchaId: undefined, captchaCode: undefined })
    const mustChange = await userStore.login(data)
    if (remember.value) {
      save(REMEMBER_KEY, form.username)
      if (tenantEnabled.value) save(REMEMBER_TENANT_KEY, form.tenantName)
    } else {
      remove(REMEMBER_KEY)
      remove(REMEMBER_TENANT_KEY)
    }
    if (mustChange) {
      message.warning(t('login.mustChange'), 4)
      router.replace({ path: '/profile', query: { tab: 'password' } })
    } else {
      message.success(t('login.success'))
      router.replace(route.query.redirect || '/')
    }
  } catch (e) {
    // 登录失败后验证码作废，需要验证码时换一张
    failed.value = true
    if (needCaptcha.value) refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  --login-bg: #fffdf3;
  --login-card: rgba(255, 255, 255, 0.86);
  --login-card-border: rgba(255, 255, 255, 0.9);
  --login-module: rgba(255, 255, 255, 0.72);
  --login-shadow: 0 24px 60px -20px rgba(120, 90, 0, 0.25), 0 8px 24px -12px rgba(0, 0, 0, 0.08);
  --login-glow-opacity: 1;
  --login-slice-opacity: 0.9;
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--login-bg);
}

/* 背景 */
.bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: var(--login-glow-opacity);
}
.glow-a {
  width: 520px;
  height: 520px;
  top: -160px;
  left: -120px;
  background: rgba(255, 212, 59, 0.45);
}
.glow-b {
  width: 460px;
  height: 460px;
  right: -140px;
  bottom: -160px;
  background: rgba(148, 216, 45, 0.35);
}
.glow-c {
  width: 360px;
  height: 360px;
  top: 30%;
  left: 40%;
  background: rgba(22, 119, 255, 0.12);
}
.slice {
  position: absolute;
  opacity: var(--login-slice-opacity);
}
.slice-lg {
  width: 380px;
  top: -120px;
  right: 30%;
  transform: rotate(18deg);
}
.slice-sm {
  width: 180px;
  bottom: 48px;
  left: -50px;
  transform: rotate(-20deg);
}

.top-actions {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 4px;
}

.container {
  position: relative;
  z-index: 1;
  flex: 1;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 48px 32px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 64px;
}

/* 品牌区 */
.brand {
  flex: 1;
  max-width: 560px;
}
.brand-logo {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 40px;
}
.brand-name {
  font-size: 24px;
  font-weight: 700;
  color: var(--app-text);
  line-height: 1.2;
}
.brand-en {
  font-size: 12px;
  letter-spacing: 3px;
  color: var(--app-text-3);
}
.brand-title {
  font-size: 40px;
  line-height: 1.3;
  font-weight: 700;
  color: var(--app-text);
  margin: 0 0 16px;
}
.highlight {
  position: relative;
  white-space: nowrap;
  background: linear-gradient(transparent 62%, rgba(255, 212, 59, 0.7) 62%);
}
.brand-desc {
  font-size: 15px;
  line-height: 1.8;
  color: var(--app-text-2);
  margin: 0 0 32px;
}
.modules {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.module {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 14px 6px;
  border-radius: 14px;
  background: var(--login-module);
  border: 1px solid var(--login-card-border);
  backdrop-filter: blur(6px);
  transition: transform 0.2s;
}
.module:hover {
  transform: translateY(-2px);
}
.module-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 19px;
}
.module-name {
  font-size: 13px;
  color: var(--app-text-2);
}
.brand-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 24px;
}
.brand-tags span {
  font-size: 12px;
  padding: 3px 12px;
  border-radius: 999px;
  color: #ad6800;
  background: rgba(255, 212, 59, 0.22);
}

/* 登录卡片 */
.card {
  width: 480px;
  flex-shrink: 0;
  padding: 48px 48px 36px;
  border-radius: 24px;
  background: var(--login-card);
  border: 1px solid var(--login-card-border);
  box-shadow: var(--login-shadow);
  backdrop-filter: blur(16px);
}
.mobile-logo {
  display: none;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 700;
  color: var(--app-text);
  margin-bottom: 28px;
}
.form-title {
  font-size: 26px;
  font-weight: 700;
  margin: 0 0 6px;
  color: var(--app-text);
}
.wave {
  display: inline-block;
  transform-origin: 70% 70%;
  animation: wave 2.4s ease-in-out 0.6s 2;
}
@keyframes wave {
  0%, 60%, 100% { transform: rotate(0); }
  10%, 30% { transform: rotate(14deg); }
  20%, 40% { transform: rotate(-8deg); }
}
.form-subtitle {
  color: var(--app-text-3);
  margin-bottom: 28px;
}
.input-icon {
  color: var(--app-text-3);
}
.captcha-row {
  display: flex;
  gap: 10px;
}
.captcha-img {
  flex: none;
  width: 120px;
  height: 40px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid var(--login-card-border);
}
.form-row {
  display: flex;
  justify-content: space-between;
  margin: -4px 0 20px;
}
.submit-btn {
  height: 46px;
  font-size: 16px;
  border-radius: 10px;
}
.im-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 24px 0 16px;
  font-size: 12px;
  color: var(--app-text-3);
}
.im-divider::before,
.im-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--app-border);
}
.im-logins {
  display: flex;
  justify-content: center;
  gap: 28px;
}
.im-login {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-width: 56px;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 12px;
  color: var(--app-text-2);
  transition: transform 0.2s;
}
.im-login:hover:not(:disabled) {
  transform: translateY(-2px);
}
.im-login:disabled {
  cursor: default;
}
.im-login :deep(.ant-spin) {
  height: 34px;
  display: flex;
  align-items: center;
}
.tip {
  margin-top: 22px;
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--app-text-2);
  background: rgba(255, 212, 59, 0.14);
}
.demo-account {
  color: inherit;
  cursor: pointer;
}
.demo-account:hover b {
  color: var(--ant-color-primary, #1677ff);
}
.tip + .tip {
  margin-top: 8px;
}
.tip-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 6px;
  border-radius: 50%;
  vertical-align: middle;
  background: #fab005;
}
.copyright {
  position: relative;
  z-index: 1;
  text-align: center;
  color: var(--app-text-3);
  font-size: 12px;
  padding: 0 16px 20px;
}

@media (max-width: 960px) {
  .brand {
    display: none;
  }
  .container {
    justify-content: center;
    padding: 72px 16px 24px;
  }
  .card {
    width: 100%;
    max-width: 480px;
    padding: 36px 28px 28px;
  }
  .mobile-logo {
    display: flex;
  }
}
</style>

<style>
/* 暗色主题（不能放在 scoped 中：需要匹配根元素的 data-theme） */
html[data-theme='dark'] .login-page {
  --login-bg: #121212;
  --login-card: rgba(28, 28, 30, 0.86);
  --login-card-border: rgba(255, 255, 255, 0.06);
  --login-module: rgba(255, 255, 255, 0.04);
  --login-shadow: 0 24px 60px -20px rgba(0, 0, 0, 0.6);
  --login-glow-opacity: 0.35;
  --login-slice-opacity: 0.18;
}
html[data-theme='dark'] .login-page .highlight {
  background: linear-gradient(transparent 70%, rgba(255, 212, 59, 0.35) 70%);
}
html[data-theme='dark'] .login-page .brand-tags span {
  color: #ffd666;
  background: rgba(255, 212, 59, 0.1);
}
</style>
