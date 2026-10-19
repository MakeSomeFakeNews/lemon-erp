<template>
  <view class="login">
    <view class="login__brand">
      <image class="login__logo" src="/static/logo.png" mode="aspectFit" />
      <text class="login__name">{{ systemName || t('common.appName') }}</text>
    </view>

    <!-- 绑定了多个租户的账号：选择租户 -->
    <view v-if="mode === 'choose'" class="login__panel">
      <view class="login__subtitle">{{ t('common.login.chooseTenant') }}</view>
      <view v-for="tn in tenants" :key="tn.id" class="login__tenant" @click="choose(tn)">
        <text>{{ tn.name || t('common.login.platformTenant') }}</text>
        <text class="login__arrow">›</text>
      </view>
      <view class="login__link" @click="toBind">{{ t('common.login.otherAccount') }}</view>
    </view>

    <!-- 账号密码：微信未绑定时绑定；H5 调试时直接登录 -->
    <view v-else-if="mode === 'form'" class="login__panel">
      <view class="login__tip">{{ wechat ? t('common.login.bindTip') : t('common.login.h5Tip') }}</view>
      <view v-if="tenantEnabled" class="login__input">
        <input v-model="form.tenantName" :placeholder="t('common.login.tenantPh')" placeholder-class="l-placeholder" />
      </view>
      <view class="login__input">
        <input v-model="form.username" :placeholder="t('common.login.usernamePh')" placeholder-class="l-placeholder" />
      </view>
      <view class="login__input">
        <input v-model="form.password" password :placeholder="t('common.login.passwordPh')" placeholder-class="l-placeholder" />
      </view>
      <view v-if="needCaptcha" class="login__input login__captcha">
        <input v-model="form.captchaCode" :placeholder="t('common.login.captchaPh')" placeholder-class="l-placeholder" maxlength="8" />
        <image v-if="captchaImage" :src="captchaImage" class="login__captcha-img" mode="aspectFit" @click="refreshCaptcha" />
      </view>
      <button class="l-btn login__submit" :loading="busy" :disabled="busy" @click="submit">
        {{ wechat ? t('common.login.submit') : t('common.login.passwordSubmit') }}
      </button>
      <view v-if="wechat && tenants.length" class="login__link" @click="mode = 'choose'">{{ t('common.login.back') }}</view>
    </view>

    <!-- 微信登录 -->
    <view v-else class="login__panel">
      <button class="l-btn login__submit" :loading="busy" :disabled="busy" @click="onWechatTap">{{ t('common.login.wechat') }}</button>
      <view class="login__link" @click="wechatLogin(true)">{{ t('common.login.otherAccount') }}</view>
    </view>

    <view class="login__foot">{{ t('common.login.agreement') }}</view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useI18n } from '@/locales'
import { CaptchaMode } from '@web/constants/enums'
import { getCaptcha, getConfig, miniappBind, miniappChoose, miniappLogin, passwordLogin } from '@/api/auth'
import { afterLogin } from '@/utils/session'
import { requestSubscribe } from '@/utils/subscribe'

const { t } = useI18n()
const TENANT_KEY = 'lemon_mp_tenant'

/** 微信小程序中用微信登录；H5（浏览器调试）中没有 wx.login，直接用账号密码登录 */
let wechat = false
// #ifdef MP-WEIXIN
wechat = true
// #endif

/** wechat 微信登录 / form 账号密码 / choose 选择租户 */
const mode = ref(wechat ? 'wechat' : 'form')
const busy = ref(false)
const key = ref('')
/** 会话标识的获取时间：后端 10 分钟内有效，快过期时提交前重新获取 */
let keyTime = 0
const KEY_TTL = 9 * 60 * 1000
const tenants = ref([])
const systemName = ref('')
const tenantEnabled = ref(false)
const captchaMode = ref(CaptchaMode.OFF)
const failed = ref(false)
const captchaImage = ref('')
const form = reactive({ tenantName: uni.getStorageSync(TENANT_KEY) || '', username: '', password: '', captchaId: '', captchaCode: '' })

const needCaptcha = computed(() => captchaMode.value === CaptchaMode.ALWAYS || (captchaMode.value === CaptchaMode.AFTER_FAILURE && failed.value))

onLoad(async (q) => {
  getConfig()
    .then((c) => {
      systemName.value = c.systemName
      tenantEnabled.value = c.tenantEnabled
      captchaMode.value = c.captchaMode
      if (needCaptcha.value) refreshCaptcha()
    })
    .catch(() => {})
  // 切换账号时不自动登录
  if (wechat && q.switch !== '1') wechatLogin(false)
})

/** 点击微信登录时顺便申请审批提醒的订阅授权（只能在点击事件中申请） */
function onWechatTap() {
  requestSubscribe()
  wechatLogin(false)
}

function wxCode() {
  return new Promise((resolve, reject) => uni.login({ provider: 'weixin', success: (r) => resolve(r.code), fail: reject }))
}

async function wechatLogin(switchAccount) {
  busy.value = true
  try {
    const res = await miniappLogin(await wxCode(), switchAccount)
    if (res.token) return await afterLogin(res)
    key.value = res.key
    keyTime = Date.now()
    tenants.value = res.tenants || []
    mode.value = tenants.value.length ? 'choose' : 'form'
  } catch {
    // 错误已提示
  } finally {
    busy.value = false
  }
}

async function choose(tn) {
  uni.showLoading({ mask: true })
  try {
    await afterLogin(await miniappChoose(key.value, tn.id))
  } catch {
    // 会话过期时重新走微信登录
    mode.value = 'wechat'
  } finally {
    uni.hideLoading()
  }
}

/** 选择租户页中「使用其他账号登录」：会话标识仍然有效，直接填写账号密码 */
function toBind() {
  mode.value = 'form'
}

async function refreshCaptcha() {
  const c = await getCaptcha()
  form.captchaId = c.captchaId
  form.captchaCode = ''
  captchaImage.value = c.image
}

async function submit() {
  if (tenantEnabled.value && !form.tenantName.trim()) return toast(t('common.required', { name: t('common.login.tenant') }))
  if (!form.username.trim()) return toast(t('common.required', { name: t('common.login.username') }))
  if (!form.password) return toast(t('common.required', { name: t('common.login.password') }))
  if (needCaptcha.value && !form.captchaCode.trim()) return toast(t('common.required', { name: t('common.login.captcha') }))
  const data = { ...form, username: form.username.trim(), tenantName: form.tenantName.trim() }
  if (!needCaptcha.value) Object.assign(data, { captchaId: undefined, captchaCode: undefined })
  busy.value = true
  try {
    if (wechat && Date.now() - keyTime > KEY_TTL) {
      key.value = (await miniappLogin(await wxCode(), true)).key
      keyTime = Date.now()
    }
    const res = wechat ? await miniappBind({ ...data, key: key.value }) : await passwordLogin(data)
    uni.setStorageSync(TENANT_KEY, data.tenantName)
    await afterLogin(res)
  } catch {
    failed.value = true
    if (needCaptcha.value) refreshCaptcha().catch(() => {})
  } finally {
    busy.value = false
  }
}

function toast(title) {
  uni.showToast({ title, icon: 'none' })
}
</script>

<style lang="scss">
.login {
  min-height: 100vh;
  padding: 0 56rpx;
  box-sizing: border-box;
  background: linear-gradient(180deg, #e6f4ff 0%, #fff 45%);
  display: flex;
  flex-direction: column;
  &__brand {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding-top: 220rpx;
    padding-bottom: 72rpx;
  }
  &__logo {
    width: 128rpx;
    height: 128rpx;
  }
  &__name {
    margin-top: 24rpx;
    font-size: 40rpx;
    font-weight: 600;
  }
  &__panel {
    flex: 1;
  }
  &__subtitle {
    font-size: 30rpx;
    font-weight: 600;
    margin-bottom: 24rpx;
  }
  &__tip {
    font-size: 24rpx;
    color: $l-text-3;
    margin-bottom: 32rpx;
    line-height: 1.6;
  }
  &__input {
    display: flex;
    align-items: center;
    height: 96rpx;
    margin-bottom: 24rpx;
    padding: 0 28rpx;
    background: #f5f6f8;
    border-radius: 48rpx;
    input {
      flex: 1;
      font-size: 28rpx;
    }
  }
  &__captcha-img {
    width: 200rpx;
    height: 72rpx;
  }
  &__submit {
    margin-top: 40rpx;
  }
  &__tenant {
    display: flex;
    justify-content: space-between;
    padding: 32rpx 28rpx;
    margin-bottom: 20rpx;
    background: #fff;
    border-radius: $l-radius;
    box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
    font-size: 30rpx;
  }
  &__arrow {
    color: #bfbfbf;
  }
  &__link {
    margin-top: 36rpx;
    text-align: center;
    font-size: 28rpx;
    color: $l-primary;
  }
  &__foot {
    padding: 40rpx 0 calc(40rpx + env(safe-area-inset-bottom));
    text-align: center;
    font-size: 22rpx;
    color: $l-text-3;
  }
}
</style>
