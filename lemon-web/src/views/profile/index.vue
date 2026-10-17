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
            <div class="avatar-wrap" title="点击更换头像">
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
          <li><ApartmentOutlined /><span>部门</span><b>{{ deptName || '未分配' }}</b></li>
          <li><MailOutlined /><span>邮箱</span><b>{{ user.email || '未设置' }}</b></li>
          <li><PhoneOutlined /><span>手机</span><b>{{ user.phone || '未设置' }}</b></li>
          <li><CalendarOutlined /><span>注册时间</span><b>{{ user.createTime || '-' }}</b></li>
        </ul>
      </a-card>
    </a-col>

    <!-- 资料与密码 -->
    <a-col :xs="24" :lg="16" :xl="17">
      <a-card :bordered="false" class="page-card">
        <a-tabs v-model:active-key="activeTab">
          <a-tab-pane key="info" tab="基本资料">
            <a-form ref="infoRef" :model="info" :rules="infoRules" layout="vertical" class="tab-form">
              <a-form-item label="昵称" name="nickname">
                <a-input v-model:value="info.nickname" placeholder="请输入昵称" :maxlength="64" />
              </a-form-item>
              <a-form-item label="邮箱" name="email">
                <a-input v-model:value="info.email" placeholder="请输入邮箱" />
              </a-form-item>
              <a-form-item label="手机号" name="phone">
                <a-input v-model:value="info.phone" placeholder="请输入手机号" />
              </a-form-item>
              <a-button type="primary" :loading="savingInfo" @click="saveInfo">保存资料</a-button>
            </a-form>
          </a-tab-pane>
          <a-tab-pane key="password" tab="修改密码">
            <a-form ref="pwdRef" :model="pwd" :rules="pwdRules" layout="vertical" class="tab-form">
              <a-form-item label="原密码" name="oldPassword">
                <a-input-password v-model:value="pwd.oldPassword" autocomplete="current-password" />
              </a-form-item>
              <a-form-item label="新密码" name="newPassword">
                <a-input-password v-model:value="pwd.newPassword" :placeholder="passwordHint()" autocomplete="new-password" />
              </a-form-item>
              <a-form-item label="确认新密码" name="confirm">
                <a-input-password v-model:value="pwd.confirm" autocomplete="new-password" />
              </a-form-item>
              <a-button type="primary" :loading="savingPwd" @click="savePassword">修改密码</a-button>
            </a-form>
          </a-tab-pane>
          <a-tab-pane v-if="imList.length" key="im" tab="第三方账号">
            <div class="tab-form im-list">
              <p class="im-tip">绑定后可以用对应平台扫码登录、在平台中免登打开系统，并接收审批待办等消息</p>
              <div v-for="b in imList" :key="b.platform" class="im-item">
                <ImPlatformIcon :platform="b.platform" :size="40" />
                <div class="im-main">
                  <div class="im-name">{{ b.label }}</div>
                  <div v-if="b.bound" class="im-sub">已绑定 {{ b.imName || b.imUserId }} · {{ b.bindTime }}</div>
                  <div v-else class="im-sub">未绑定</div>
                </div>
                <a-popconfirm v-if="b.bound" :title="`确定解除${b.label}绑定？`" @confirm="unbindIm(b)">
                  <a-button>解除绑定</a-button>
                </a-popconfirm>
                <a-button v-else type="primary" ghost :loading="imBinding === b.platform" @click="bindIm(b)">绑定</a-button>
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
import { passwordHint, passwordRules } from '@/utils/password'

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
  message.success('已解除绑定')
  loadIm()
}

// 头像上传
const uploadHeaders = { Authorization: `Bearer ${getToken()}` }
const uploading = ref(false)

function beforeUpload(file) {
  if (!file.type.startsWith('image/')) {
    message.error('只能上传图片')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    message.error('图片不能超过 2MB')
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
      message.success('头像已更新')
    } else {
      message.error(res?.msg || '上传失败')
    }
  } else if (file.status === 'error') {
    message.error(file.response?.msg || '上传失败')
  }
}

// 基本资料
const infoRef = ref()
const savingInfo = ref(false)
const info = reactive({ nickname: '', email: '', phone: '' })
const infoRules = {
  email: [{ type: 'email', message: '邮箱格式不正确' }],
  phone: [{ pattern: /^[0-9+\-\s]{0,20}$/, message: '手机号格式不正确' }]
}

async function saveInfo() {
  await infoRef.value.validate()
  savingInfo.value = true
  try {
    await updateProfile({ ...info })
    Object.assign(user.value, info)
    if (userStore.user) Object.assign(userStore.user, info)
    message.success('资料已保存')
  } finally {
    savingInfo.value = false
  }
}

// 修改密码
const pwdRef = ref()
const savingPwd = ref(false)
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码' }],
  newPassword: passwordRules(),
  confirm: [
    { required: true, message: '请再次输入新密码' },
    { validator: (_, v) => (v === pwd.newPassword ? Promise.resolve() : Promise.reject('两次输入的密码不一致')) }
  ]
}

async function savePassword() {
  await pwdRef.value.validate()
  savingPwd.value = true
  try {
    await updatePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    message.success('密码已修改')
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
