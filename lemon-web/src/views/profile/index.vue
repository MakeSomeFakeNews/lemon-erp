<template>
  <a-row :gutter="[16, 16]">
    <!-- 个人信息卡 -->
    <a-col :xs="24" :lg="8" :xl="7">
      <a-card :bordered="false" class="page-card info-card">
        <div class="avatar-box">
          <a-upload
            :action="AVATAR_UPLOAD_URL"
            :headers="uploadHeaders"
            name="file"
            accept="image/*"
            :show-upload-list="false"
            :before-upload="beforeUpload"
            @change="onUploadChange"
          >
            <div class="avatar-wrap" :title="$t('profile.changeAvatar')">
              <UserAvatar :src="user.avatar" :name="userStore.displayName" :size="96" />
              <div class="avatar-mask">
                <LoadingOutlined v-if="uploading" />
                <CameraOutlined v-else />
              </div>
            </div>
          </a-upload>
          <h3 class="name">{{ userStore.displayName }}</h3>
          <div class="username">@{{ user.username }}</div>
          <div class="roles">
            <a-tag v-for="r in roleNames" :key="r" color="blue" :bordered="false">{{ r }}</a-tag>
          </div>
        </div>
        <a-divider />
        <ul class="info-list">
          <li><ApartmentOutlined /><span>{{ $t('common.field.dept') }}</span><b>{{ deptName || $t('system.user.unassigned') }}</b></li>
          <li><MailOutlined /><span>{{ $t('common.field.email') }}</span><b>{{ user.email || $t('common.status.unset') }}</b></li>
          <li><PhoneOutlined /><span>{{ $t('profile.mobile') }}</span><b>{{ user.phone || $t('common.status.unset') }}</b></li>
          <li><CalendarOutlined /><span>{{ $t('profile.registerTime') }}</span><b>{{ user.createTime || '-' }}</b></li>
        </ul>
      </a-card>
    </a-col>

    <!-- 资料与密码 -->
    <a-col :xs="24" :lg="16" :xl="17">
      <a-card :bordered="false" class="page-card">
        <a-tabs v-model:active-key="activeTab">
          <a-tab-pane key="info" :tab="$t('profile.basicInfo')">
            <a-form ref="infoRef" :model="info" :rules="infoRules" layout="vertical" class="tab-form">
              <a-form-item :label="$t('profile.nickname')" name="nickname">
                <a-input v-model:value="info.nickname" :placeholder="$t('common.placeholder.inputName', { name: $t('profile.nickname') })" :maxlength="64" />
              </a-form-item>
              <a-form-item :label="$t('common.field.email')" name="email">
                <a-input v-model:value="info.email" :placeholder="$t('common.placeholder.inputName', { name: $t('common.field.email') })" />
              </a-form-item>
              <a-form-item :label="$t('common.field.phone')" name="phone">
                <a-input v-model:value="info.phone" :placeholder="$t('common.placeholder.inputName', { name: $t('common.field.phone') })" />
              </a-form-item>
              <a-button type="primary" :loading="savingInfo" @click="saveInfo">{{ $t('profile.saveInfo') }}</a-button>
            </a-form>
          </a-tab-pane>
          <a-tab-pane key="password" :tab="$t('profile.changePassword')">
            <a-form ref="pwdRef" :model="pwd" :rules="pwdRules" layout="vertical" class="tab-form">
              <a-form-item :label="$t('profile.oldPassword')" name="oldPassword">
                <a-input-password v-model:value="pwd.oldPassword" autocomplete="current-password" />
              </a-form-item>
              <a-form-item :label="$t('system.user.newPassword')" name="newPassword">
                <a-input-password v-model:value="pwd.newPassword" :placeholder="passwordHint()" autocomplete="new-password" />
              </a-form-item>
              <a-form-item :label="$t('profile.confirmPassword')" name="confirm">
                <a-input-password v-model:value="pwd.confirm" autocomplete="new-password" />
              </a-form-item>
              <a-button type="primary" :loading="savingPwd" @click="savePassword">{{ $t('profile.changePassword') }}</a-button>
            </a-form>
          </a-tab-pane>
          <a-tab-pane v-if="imList.length" key="im" :tab="$t('profile.imAccounts')">
            <div class="tab-form im-list">
              <p class="im-tip">{{ $t('profile.imTip') }}</p>
              <div v-for="b in imList" :key="b.platform" class="im-item">
                <ImPlatformIcon :platform="b.platform" :size="40" />
                <div class="im-main">
                  <div class="im-name">{{ imPlatform(b.platform).label }}</div>
                  <div v-if="b.bound" class="im-sub">{{ $t('profile.imBound', { name: b.imName || b.imUserId, time: b.bindTime }) }}</div>
                  <div v-else class="im-sub">{{ $t('system.im.unbound') }}</div>
                </div>
                <a-popconfirm v-if="b.bound" :title="$t('profile.confirmUnbind', { name: imPlatform(b.platform).label })" @confirm="unbindIm(b)">
                  <a-button>{{ $t('common.action.unbind') }}</a-button>
                </a-popconfirm>
                <a-button v-else type="primary" ghost :loading="imBinding === b.platform" @click="bindIm(b)">{{ $t('common.action.bind') }}</a-button>
              </div>
            </div>
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </a-col>
  </a-row>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import {
  ApartmentOutlined,
  CalendarOutlined,
  CameraOutlined,
  LoadingOutlined,
  MailOutlined,
  PhoneOutlined
} from '@ant-design/icons-vue'
import ImPlatformIcon from '@/components/im/ImPlatformIcon.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { getImBindUrl, listMyIm, unbindMyIm } from '@/api/system/im'
import { AVATAR_UPLOAD_URL, getProfile, updatePassword, updateProfile } from '@/api/profile'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getToken } from '@/utils/auth'
import { imPlatform } from '@/utils/im'
import { passwordHint, passwordRules } from '@/utils/password'

const { t } = useI18n()
const userStore = useUserStore()
const user = ref({})
const roleNames = ref([])
const deptName = ref()
const routeTab = useRoute().query.tab
const activeTab = ref(['password', 'im'].includes(routeTab) ? routeTab : 'info')

async function load() {
  const res = await getProfile()
  user.value = res.user
  roleNames.value = res.roleNames
  deptName.value = res.deptName
  Object.assign(info, { nickname: res.user.nickname, email: res.user.email, phone: res.user.phone })
}
onMounted(() => {
  load()
  loadIm()
})

// 第三方账号：当前租户启用的钉钉、企业微信、飞书
const imList = ref([])
const imBinding = ref(null)
async function loadIm() {
  imList.value = await listMyIm().catch(() => [])
}
async function bindIm(b) {
  imBinding.value = b.platform
  try {
    location.href = await getImBindUrl(b.platform)
  } catch (e) {
    imBinding.value = null
  }
}
async function unbindIm(b) {
  await unbindMyIm(b.platform)
  message.success(t('system.im.unboundOk'))
  loadIm()
}

// 头像上传
const uploadHeaders = { Authorization: `Bearer ${getToken()}` }
const uploading = ref(false)

function beforeUpload(file) {
  if (!file.type.startsWith('image/')) {
    message.error(t('profile.imageOnly'))
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    message.error(t('profile.imageTooLarge'))
    return false
  }
  return true
}

function onUploadChange({ file }) {
  if (file.status === 'uploading') {
    uploading.value = true
    return
  }
  uploading.value = false
  if (file.status === 'done') {
    const res = file.response
    if (res?.code === 200) {
      user.value.avatar = res.data
      if (userStore.user) userStore.user.avatar = res.data
      message.success(t('profile.avatarUpdated'))
    } else {
      message.error(res?.msg || t('profile.uploadFailed'))
    }
  } else if (file.status === 'error') {
    message.error(file.response?.msg || t('profile.uploadFailed'))
  }
}

// 基本资料
const infoRef = ref()
const savingInfo = ref(false)
const info = reactive({ nickname: '', email: '', phone: '' })
const infoRules = {
  email: [{ type: 'email', message: t('common.rule.email') }],
  phone: [{ pattern: /^[0-9+\-\s]{0,20}$/, message: t('common.rule.phone') }]
}

async function saveInfo() {
  await infoRef.value.validate()
  savingInfo.value = true
  try {
    await updateProfile({ ...info })
    Object.assign(user.value, info)
    if (userStore.user) Object.assign(userStore.user, info)
    message.success(t('profile.infoSaved'))
  } finally {
    savingInfo.value = false
  }
}

// 修改密码
const pwdRef = ref()
const savingPwd = ref(false)
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: t('profile.inputOldPassword') }],
  newPassword: passwordRules(),
  confirm: [
    { required: true, message: t('profile.inputConfirm') },
    { validator: (_, v) => (v === pwd.newPassword ? Promise.resolve() : Promise.reject(t('profile.mismatch'))) }
  ]
}

async function savePassword() {
  await pwdRef.value.validate()
  savingPwd.value = true
  try {
    await updatePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    message.success(t('profile.passwordChanged'))
    pwdRef.value.resetFields()
  } finally {
    savingPwd.value = false
  }
}
</script>

<style scoped>
.info-card {
  height: 100%;
}
.avatar-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding-top: 8px;
}
.avatar-wrap {
  position: relative;
  border-radius: 50%;
  cursor: pointer;
  overflow: hidden;
}
.avatar-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
  opacity: 0;
  transition: opacity 0.2s;
}
.avatar-wrap:hover .avatar-mask {
  opacity: 1;
}
.name {
  margin: 16px 0 2px;
  font-size: 20px;
  font-weight: 600;
  color: var(--app-text);
}
.username {
  color: var(--app-text-3);
  margin-bottom: 12px;
}
.info-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 14px;
}
.info-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--app-text-3);
}
.info-list b {
  margin-left: auto;
  font-weight: 400;
  color: var(--app-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tab-form {
  max-width: 480px;
  padding-top: 8px;
}
.im-list {
  max-width: 560px;
}
.im-tip {
  color: var(--app-text-3);
  margin-bottom: 16px;
}
.im-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border: 1px solid var(--app-border);
  border-radius: 10px;
}
.im-item + .im-item {
  margin-top: 12px;
}
.im-main {
  flex: 1;
  min-width: 0;
}
.im-name {
  font-weight: 600;
  color: var(--app-text);
}
.im-sub {
  font-size: 12px;
  color: var(--app-text-3);
}
</style>
