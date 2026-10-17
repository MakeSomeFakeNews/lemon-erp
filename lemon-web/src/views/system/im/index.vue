<template>
  <div class="table-page">
    <!-- 平台 -->
    <div class="platforms">
      <div
        v-for="p in IM_PLATFORMS"
        :key="p.value"
        class="page-card platform"
        :class="{ active: platform === p.value }"
        @click="switchPlatform(p.value)"
      >
        <ImPlatformIcon :platform="p.value" :size="40" />
        <div class="platform-main">
          <div class="platform-name">{{ p.label }}</div>
          <div class="sub-text">{{ summary(p.value) }}</div>
        </div>
        <a-tag v-if="configOf(p.value)?.enabled" color="success" :bordered="false">已启用</a-tag>
        <a-tag v-else :bordered="false">未启用</a-tag>
      </div>
    </div>

    <a-card :bordered="false" class="page-card table-card">
      <div v-if="loading && !configs.length" class="page-spin"><a-spin /></div>
      <a-tabs v-else v-model:active-key="tab">
        <!-- 应用配置 -->
        <a-tab-pane key="config" tab="应用配置">
          <div class="config-layout">
            <a-form :model="form" layout="vertical" class="config-form" :disabled="!canEdit">
              <a-form-item>
                <a-switch v-model:checked="form.enabled" />
                <span class="switch-label">启用{{ current.label }}集成</span>
              </a-form-item>

              <div class="section-title">应用凭证</div>
              <a-row :gutter="16">
                <a-col v-for="f in fields" :key="f.key" :xs="24" :sm="12">
                  <a-form-item :label="f.label" :required="form.enabled">
                    <a-input-password
                      v-if="f.secret"
                      v-model:value="form[f.key]"
                      :placeholder="configOf(platform)?.appSecretSet ? '已设置，留空表示不修改' : '请输入'"
                      autocomplete="new-password"
                    />
                    <a-input v-else v-model:value="form[f.key]" placeholder="请输入" allow-clear />
                  </a-form-item>
                </a-col>
              </a-row>
              <a-form-item label="系统访问地址" :required="form.enabled" extra="外网可访问的前端地址，登录回调和消息中的链接使用，如 https://erp.example.com">
                <a-input v-model:value="form.homeUrl" placeholder="https://" allow-clear />
              </a-form-item>
              <div v-if="saved.callbackUrl" class="urls">
                <div class="url-row">
                  <span class="url-label">回调地址</span>
                  <a-typography-text :copyable="{ text: saved.callbackUrl }" class="url">{{ saved.callbackUrl }}</a-typography-text>
                </div>
                <div class="url-row">
                  <span class="url-label">应用首页</span>
                  <a-typography-text :copyable="{ text: saved.entryUrl }" class="url">{{ saved.entryUrl }}</a-typography-text>
                </div>
              </div>

              <div class="section-title">功能</div>
              <div class="feature">
                <a-switch v-model:checked="form.loginEnabled" size="small" />
                <div>
                  <div>扫码登录与免登</div>
                  <div class="sub-text">登录页显示{{ current.label }}登录；从{{ current.label }}工作台、消息中打开系统时自动登录</div>
                </div>
              </div>
              <div class="feature">
                <a-switch v-model:checked="form.messageEnabled" size="small" />
                <div>
                  <div>消息推送</div>
                  <div class="sub-text">审批待办、到期提醒、预警等站内消息同时推送给已绑定的成员</div>
                </div>
              </div>
              <div class="feature">
                <a-switch v-model:checked="form.syncCreate" size="small" />
                <div>
                  <div>同步通讯录时自动创建部门和账号</div>
                  <div class="sub-text">关闭时只按手机号绑定已有账号；开启后为没有账号的成员开通账号（登录名为手机号，初始密码见参数设置）</div>
                </div>
              </div>

              <div class="section-title">群机器人</div>
              <a-form-item label="Webhook 地址" :extra="`以 ${ROBOT_PREFIX[platform]} 开头，留空不推送`">
                <a-input-password v-model:value="form.robotWebhook" :visibility-toggle="true" placeholder="群设置 → 机器人 → 添加自定义机器人后复制" />
              </a-form-item>
              <a-form-item v-if="platform !== 'WECOM'" label="加签密钥" extra="机器人安全设置选择「加签」时填写">
                <div class="secret-row">
                  <a-input-password
                    v-model:value="form.robotSecret"
                    :placeholder="configOf(platform)?.robotSecretSet ? '已设置，留空表示不修改' : '不加签可不填'"
                    autocomplete="new-password"
                  />
                  <a-checkbox v-if="configOf(platform)?.robotSecretSet" v-model:checked="form.clearRobotSecret">清除</a-checkbox>
                </div>
              </a-form-item>
              <a-form-item label="推送的消息类别">
                <a-checkbox-group v-model:value="form.categories" :options="dict.sys_message_category.map((d) => ({ label: d.label, value: d.value }))" />
              </a-form-item>

              <div v-if="canEdit" class="actions">
                <a-button type="primary" :loading="saving" @click="save">保存</a-button>
                <a-button :loading="verifying" :disabled="!saved.enabled" @click="verify">测试连接</a-button>
                <a-button :loading="robotTesting" :disabled="!saved.enabled || !saved.robotWebhook" @click="robotTest">测试群机器人</a-button>
              </div>
            </a-form>

            <div class="guide">
              <div class="guide-title"><BulbOutlined /> 接入步骤</div>
              <ol>
                <li v-for="(g, i) in GUIDES[platform]" :key="i">{{ g }}</li>
              </ol>
              <a-alert
                type="info"
                show-icon
                message="登录前需要绑定账号"
                description="成员用平台账号登录前，要先绑定系统用户：在「成员绑定」中同步通讯录（按手机号自动绑定），或由成员登录后在个人中心自行绑定。"
              />
            </div>
          </div>
        </a-tab-pane>

        <!-- 成员绑定 -->
        <a-tab-pane key="members" tab="成员绑定">
          <div class="member-bar">
            <a-space wrap>
              <a-popconfirm
                v-if="canSync"
                :title="`从${current.label}读取通讯录，按手机号绑定已有账号${saved.syncCreate ? '，并为没有账号的成员创建部门和账号' : ''}？`"
                :disabled="!saved.enabled"
                @confirm="sync"
              >
                <a-button type="primary" :loading="syncing" :disabled="!saved.enabled">
                  <template #icon><SyncOutlined /></template>同步通讯录
                </a-button>
              </a-popconfirm>
              <a-radio-group v-model:value="query.bound" button-style="solid" @change="search">
                <a-radio-button :value="undefined">全部</a-radio-button>
                <a-radio-button :value="true">已绑定</a-radio-button>
                <a-radio-button :value="false">未绑定</a-radio-button>
              </a-radio-group>
              <a-input-search v-model:value="query.keyword" placeholder="用户名、姓名、手机号、userid" allow-clear class="keyword" @search="search" />
            </a-space>
            <span v-if="saved.lastSyncTime" class="sub-text">最近同步 {{ saved.lastSyncTime }}：{{ saved.lastSyncResult }}</span>
          </div>
          <a-table
            row-key="userId"
            size="middle"
            :columns="columns"
            :data-source="list"
            :loading="tableLoading"
            :pagination="pagination"
            :scroll="{ x: 900, y: '100%' }"
            @change="onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'user'">
                <div>{{ record.nickname || record.username }}</div>
                <div class="sub-text">{{ record.username }}</div>
              </template>
              <template v-else-if="column.key === 'member'">
                <template v-if="record.bindingId">
                  <a-tag color="success" :bordered="false">已绑定</a-tag>
                  <span>{{ record.imName || '' }}</span>
                  <span class="sub-text member-id">{{ record.imUserId }}</span>
                </template>
                <a-tag v-else :bordered="false">未绑定</a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <template v-if="canSync">
                  <a v-if="!record.bindingId" @click="openBind(record)">绑定</a>
                  <template v-else>
                    <a @click="openBind(record)">修改</a>
                    <a-divider type="vertical" />
                    <a-popconfirm title="确定解除绑定？解除后该成员不能再用平台账号登录，也收不到消息推送" @confirm="unbind(record)">
                      <a class="danger">解除绑定</a>
                    </a-popconfirm>
                  </template>
                </template>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 手工绑定 -->
    <a-modal v-model:open="bindOpen" :title="`绑定${current.label}成员`" :confirm-loading="binding" @ok="submitBind">
      <a-form layout="vertical">
        <a-form-item label="用户">{{ bindTarget?.nickname || bindTarget?.username }}（{{ bindTarget?.username }}）</a-form-item>
        <a-form-item label="成员 userid" required :extra="USERID_HELP[platform]">
          <a-input v-model:value="bindUserId" placeholder="请输入" :maxlength="64" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 同步结果 -->
    <a-modal v-model:open="resultOpen" title="同步完成" :footer="null" :width="520">
      <template v-if="result">
        <div class="stats">
          <div class="stat"><b>{{ result.memberTotal }}</b><span>通讯录成员</span></div>
          <div class="stat"><b>{{ result.matched }}</b><span>按手机号绑定</span></div>
          <div class="stat"><b>{{ result.created }}</b><span>新建账号</span></div>
          <div class="stat"><b>{{ result.alreadyBound }}</b><span>之前已绑定</span></div>
        </div>
        <p v-if="result.deptCreated" class="sub-text">新建部门 {{ result.deptCreated }} 个</p>
        <a-alert v-if="result.warning" type="warning" show-icon :message="result.warning" class="mb" />
        <template v-if="result.unmatchedCount">
          <div class="unmatched-title">未绑定 {{ result.unmatchedCount }} 人（手机号未匹配到账号或平台未返回手机号），可在列表中手工绑定：</div>
          <div class="unmatched">
            <a-tag v-for="n in result.unmatched" :key="n" :bordered="false">{{ n }}</a-tag>
            <span v-if="result.unmatchedCount > result.unmatched.length" class="sub-text">等</span>
          </div>
        </template>
      </template>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { BulbOutlined, SyncOutlined } from '@ant-design/icons-vue'
import ImPlatformIcon from '@/components/im/ImPlatformIcon.vue'
import { bindImUser, listImBindings, listImConfig, saveImConfig, syncImContacts, testImRobot, unbindImUser, verifyIm } from '@/api/system/im'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { IM_PLATFORMS, imPlatform } from '@/utils/im'

/** 第三方集成：每个租户分别接入自己的钉钉、企业微信、飞书应用 */
const FIELDS = {
  DINGTALK: [
    { key: 'appKey', label: 'Client ID（原 AppKey）' },
    { key: 'appSecret', label: 'Client Secret（原 AppSecret）', secret: true },
    { key: 'agentId', label: 'AgentId' }
  ],
  WECOM: [
    { key: 'corpId', label: '企业ID（CorpID）' },
    { key: 'agentId', label: 'AgentId' },
    { key: 'appSecret', label: '应用 Secret', secret: true }
  ],
  FEISHU: [
    { key: 'appKey', label: 'App ID' },
    { key: 'appSecret', label: 'App Secret', secret: true }
  ]
}
const ROBOT_PREFIX = {
  DINGTALK: 'https://oapi.dingtalk.com/robot/send',
  WECOM: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send',
  FEISHU: 'https://open.feishu.cn/open-apis/bot/v2/hook/'
}
const USERID_HELP = {
  DINGTALK: '钉钉管理后台「通讯录 → 成员管理」中成员详情的 userid',
  WECOM: '企业微信管理后台「通讯录」中成员详情的「账号」',
  FEISHU: '飞书管理后台「组织架构 → 成员与部门」中成员详情的「用户 ID」'
}
const GUIDES = {
  DINGTALK: [
    '在钉钉开发者后台（open-dev.dingtalk.com）创建「企业内部应用」，在「凭证与基础信息」中复制 Client ID、Client Secret，在应用详情中查看 AgentId。',
    '「权限管理」中开通：通讯录部门信息读权限、成员信息读权限、个人手机号信息、根据 unionId 获取成员 userId（通讯录个人信息读权限）。',
    '「安全设置 → 重定向 URL」中填写下方的回调地址。',
    '「网页应用」中把应用首页（PC 端和移动端）设为下方的应用首页地址，成员在工作台点开即自动登录。',
    '发布应用并设置可见范围，再回到这里保存配置、测试连接。'
  ],
  WECOM: [
    '在企业微信管理后台「应用管理 → 自建」中创建应用，复制 AgentId 和 Secret；在「我的企业」页面底部查看企业ID。',
    '在应用中设置「网页授权及 JS-SDK → 可信域名」和「企业微信授权登录 → Web 网页 → 授权回调域」，均填写系统访问地址的域名。',
    '「应用主页」设为下方的应用首页地址；「企业可信 IP」中加入服务器的出口 IP（调用接口需要）。',
    '把成员加入应用的可见范围。2022 年 6 月后新建的应用读取通讯录不返回手机号，此时请在「成员绑定」中手工绑定，或让成员在个人中心自行绑定。',
    '回到这里保存配置、测试连接。'
  ],
  FEISHU: [
    '在飞书开放平台（open.feishu.cn）创建「企业自建应用」，复制 App ID 和 App Secret，并在「添加应用能力」中开启机器人。',
    '「权限管理」中开通：获取用户 user ID、获取通讯录基本信息（以应用身份读取通讯录）、获取用户手机号、获取用户邮箱、以应用的身份发消息。',
    '「安全设置 → 重定向 URL」中添加下方的回调地址。',
    '「网页应用」中把桌面端和移动端主页设为下方的应用首页地址。',
    '设置通讯录权限范围后创建版本并发布，再回到这里保存配置、测试连接。'
  ]
}

const userStore = useUserStore()
const canEdit = computed(() => userStore.hasPerm('sys:im:edit'))
const canSync = computed(() => userStore.hasPerm('sys:im:sync'))
const dict = useDict('sys_message_category')

const configs = ref([])
const loading = ref(false)
const platform = ref('DINGTALK')
const tab = ref('config')
const current = computed(() => imPlatform(platform.value))
const fields = computed(() => FIELDS[platform.value])
const configOf = (p) => configs.value.find((c) => c.platform === p)
const saved = computed(() => configOf(platform.value) || {})
const form = reactive({})

function summary(p) {
  const c = configOf(p)
  if (!c?.enabled) return '未配置'
  const parts = []
  if (c.loginEnabled) parts.push('登录')
  if (c.messageEnabled) parts.push('消息推送')
  if (c.robotWebhook) parts.push('群机器人')
  return parts.length ? parts.join(' · ') : '已启用'
}

function resetForm() {
  const c = saved.value
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, {
    platform: platform.value,
    enabled: !!c.enabled,
    corpId: c.corpId,
    appKey: c.appKey,
    agentId: c.agentId,
    appSecret: '',
    homeUrl: c.homeUrl,
    loginEnabled: c.loginEnabled !== false,
    messageEnabled: c.messageEnabled !== false,
    syncCreate: !!c.syncCreate,
    robotWebhook: c.robotWebhook,
    robotSecret: '',
    clearRobotSecret: false,
    categories: c.robotCategories ? c.robotCategories.split(',') : []
  })
}

async function loadConfigs() {
  loading.value = true
  try {
    configs.value = await listImConfig()
    resetForm()
  } finally {
    loading.value = false
  }
}

function switchPlatform(p) {
  if (platform.value === p) return
  platform.value = p
  resetForm()
  query.platform = p
  search()
}

const saving = ref(false)
async function save() {
  const { categories, ...data } = form
  saving.value = true
  try {
    await saveImConfig({ ...data, robotCategories: categories.join(',') })
    message.success('已保存')
    await loadConfigs()
  } finally {
    saving.value = false
  }
}

const verifying = ref(false)
async function verify() {
  verifying.value = true
  try {
    message.success(await verifyIm(platform.value), 4)
  } finally {
    verifying.value = false
  }
}

const robotTesting = ref(false)
async function robotTest() {
  robotTesting.value = true
  try {
    await testImRobot(platform.value)
    message.success('已发送，请到群里查看')
  } finally {
    robotTesting.value = false
  }
}

// ---------------------------------------------------------------- 成员绑定
const query = reactive({ platform: platform.value, bound: undefined, keyword: '' })
const { list, loading: tableLoading, pagination, load, search, onTableChange } = useTable(listImBindings, query)
const columns = [
  { title: '用户', key: 'user', width: 180 },
  { title: '部门', dataIndex: 'deptName', width: 160, ellipsis: true },
  { title: '手机号', dataIndex: 'phone', width: 140 },
  { title: '绑定成员', key: 'member', width: 260 },
  { title: '绑定时间', dataIndex: 'bindTime', width: 170 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' }
]

const syncing = ref(false)
const result = ref()
const resultOpen = ref(false)
async function sync() {
  syncing.value = true
  try {
    result.value = await syncImContacts(platform.value)
    resultOpen.value = true
    load()
    configs.value = await listImConfig()
  } finally {
    syncing.value = false
  }
}

const bindOpen = ref(false)
const bindTarget = ref()
const bindUserId = ref('')
const binding = ref(false)
function openBind(record) {
  bindTarget.value = record
  bindUserId.value = record.imUserId || ''
  bindOpen.value = true
}
async function submitBind() {
  if (!bindUserId.value.trim()) {
    message.warning('请输入成员 userid')
    return
  }
  binding.value = true
  try {
    await bindImUser({ userId: bindTarget.value.userId, platform: platform.value, imUserId: bindUserId.value.trim() })
    message.success('已绑定')
    bindOpen.value = false
    load()
  } finally {
    binding.value = false
  }
}
async function unbind(record) {
  await unbindImUser(record.bindingId)
  message.success('已解除绑定')
  load()
}

onMounted(() => {
  loadConfigs()
  load()
})
</script>

<style scoped>
.platforms {
  flex: none;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}
.platforms > .platform {
  margin: 0;
}
.platform {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.platform:hover {
  border-color: var(--app-border);
}
.platform.active {
  border-color: var(--app-primary);
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.12);
}
.platform-main {
  flex: 1;
  min-width: 0;
}
.platform-name {
  font-size: 15px;
  font-weight: 600;
}
.config-layout {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}
.config-form {
  flex: 1;
  min-width: 0;
  max-width: 680px;
}
.switch-label {
  margin-left: 10px;
  font-weight: 500;
}
.section-title {
  font-weight: 600;
  margin: 8px 0 12px;
  padding-left: 8px;
  border-left: 3px solid var(--app-primary);
  line-height: 1.2;
}
.urls {
  margin: -8px 0 20px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--app-hover);
  border: 1px dashed var(--app-border);
}
.url-row {
  display: flex;
  gap: 12px;
  align-items: baseline;
  font-size: 13px;
}
.url-row + .url-row {
  margin-top: 6px;
}
.url-label {
  flex: none;
  color: var(--app-text-3);
}
.url {
  word-break: break-all;
}
.feature {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 14px;
}
.feature :deep(.ant-switch) {
  margin-top: 2px;
}
.secret-row {
  display: flex;
  gap: 12px;
  align-items: center;
}
.actions {
  display: flex;
  gap: 8px;
  padding: 4px 0 8px;
}
.guide {
  width: 360px;
  flex: none;
  position: sticky;
  top: 0;
  padding: 16px 18px;
  border-radius: 10px;
  background: var(--app-hover);
  border: 1px solid var(--app-border);
}
.guide-title {
  font-weight: 600;
  margin-bottom: 8px;
}
.guide ol {
  padding-left: 18px;
  margin: 0 0 14px;
  color: var(--app-text-2);
  font-size: 13px;
  line-height: 1.8;
}
.guide li + li {
  margin-top: 4px;
}
.member-bar {
  flex: none;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 8px 16px;
  margin-bottom: 12px;
}
.keyword {
  width: 240px;
}
.member-id {
  margin-left: 6px;
}
.danger {
  color: var(--ant-color-error, #ff4d4f);
}
.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}
.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 0;
  border-radius: 8px;
  background: var(--app-hover);
}
.stat b {
  font-size: 22px;
}
.stat span {
  font-size: 12px;
  color: var(--app-text-3);
}
.mb {
  margin-bottom: 12px;
}
.unmatched-title {
  margin-bottom: 8px;
}
.unmatched {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 0;
  max-height: 180px;
  overflow-y: auto;
}
.page-spin {
  padding: 48px 0;
  text-align: center;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
@media (max-width: 1100px) {
  .config-layout {
    flex-direction: column-reverse;
  }
  .guide {
    position: static;
    width: 100%;
  }
}
@media (max-width: 767px) {
  .platforms {
    grid-template-columns: 1fr;
  }
}
</style>
