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
        <a-tag v-if="configOf(p.value)?.enabled" color="success" :bordered="false">{{ $t('system.im.enabled') }}</a-tag>
        <a-tag v-else :bordered="false">{{ $t('system.im.notEnabled') }}</a-tag>
      </div>
    </div>

    <a-card :bordered="false" class="page-card table-card">
      <div v-if="loading && !configs.length" class="page-spin"><a-spin /></div>
      <a-tabs v-else v-model:active-key="tab">
        <!-- 应用配置 -->
        <a-tab-pane key="config" :tab="$t('system.im.appConfig')">
          <div class="config-layout">
            <a-form :model="form" layout="vertical" class="config-form" :disabled="!canEdit">
              <a-form-item>
                <a-switch v-model:checked="form.enabled" />
                <span class="switch-label">{{ $t('system.im.enableIntegration', { name: current.label }) }}</span>
              </a-form-item>

              <div class="section-title">{{ $t('system.im.credentials') }}</div>
              <a-row :gutter="16">
                <a-col v-for="f in fields" :key="f.key" :xs="24" :sm="12">
                  <a-form-item :label="f.label" :required="form.enabled">
                    <a-input-password
                      v-if="f.secret"
                      v-model:value="form[f.key]"
                      :placeholder="configOf(platform)?.appSecretSet ? $t('system.im.secretSet') : $t('common.placeholder.input')"
                      autocomplete="new-password"
                    />
                    <a-input v-else v-model:value="form[f.key]" :placeholder="$t('common.placeholder.input')" allow-clear />
                  </a-form-item>
                </a-col>
              </a-row>
              <a-form-item :label="$t('system.im.homeUrl')" :required="form.enabled" :extra="$t('system.im.homeUrlTip')">
                <a-input v-model:value="form.homeUrl" placeholder="https://" allow-clear />
              </a-form-item>
              <div v-if="saved.callbackUrl" class="urls">
                <div class="url-row">
                  <span class="url-label">{{ $t('system.im.callbackUrl') }}</span>
                  <a-typography-text :copyable="{ text: saved.callbackUrl }" class="url">{{ saved.callbackUrl }}</a-typography-text>
                </div>
                <div class="url-row">
                  <span class="url-label">{{ $t('system.im.entryUrl') }}</span>
                  <a-typography-text :copyable="{ text: saved.entryUrl }" class="url">{{ saved.entryUrl }}</a-typography-text>
                </div>
              </div>

              <div class="section-title">{{ $t('system.im.features') }}</div>
              <div class="feature">
                <a-switch v-model:checked="form.loginEnabled" size="small" />
                <div>
                  <div>{{ $t('system.im.loginFeature') }}</div>
                  <div class="sub-text">{{ $t('system.im.loginFeatureTip', { name: current.label }) }}</div>
                </div>
              </div>
              <div class="feature">
                <a-switch v-model:checked="form.messageEnabled" size="small" />
                <div>
                  <div>{{ $t('system.im.messagePush') }}</div>
                  <div class="sub-text">{{ $t('system.im.messagePushTip') }}</div>
                </div>
              </div>
              <div class="feature">
                <a-switch v-model:checked="form.syncCreate" size="small" />
                <div>
                  <div>{{ $t('system.im.syncCreate') }}</div>
                  <div class="sub-text">{{ $t('system.im.syncCreateTip') }}</div>
                </div>
              </div>

              <div class="section-title">{{ $t('system.im.robot') }}</div>
              <a-form-item :label="$t('system.im.webhook')" :extra="$t('system.im.webhookTip', { prefix: ROBOT_PREFIX[platform] })">
                <a-input-password v-model:value="form.robotWebhook" :visibility-toggle="true" :placeholder="$t('system.im.webhookPlaceholder')" />
              </a-form-item>
              <a-form-item v-if="platform !== 'WECOM'" :label="$t('system.im.robotSecret')" :extra="$t('system.im.robotSecretTip')">
                <div class="secret-row">
                  <a-input-password
                    v-model:value="form.robotSecret"
                    :placeholder="configOf(platform)?.robotSecretSet ? $t('system.im.secretSet') : $t('system.im.robotSecretOptional')"
                    autocomplete="new-password"
                  />
                  <a-checkbox v-if="configOf(platform)?.robotSecretSet" v-model:checked="form.clearRobotSecret">{{ $t('system.im.clear') }}</a-checkbox>
                </div>
              </a-form-item>
              <a-form-item :label="$t('system.im.categories')">
                <a-checkbox-group v-model:value="form.categories" :options="dict.sys_message_category.map((d) => ({ label: d.label, value: d.value }))" />
              </a-form-item>

              <div v-if="canEdit" class="actions">
                <a-button type="primary" :loading="saving" @click="save">{{ $t('common.action.save') }}</a-button>
                <a-button :loading="verifying" :disabled="!saved.enabled" @click="verify">{{ $t('system.im.testConnection') }}</a-button>
                <a-button :loading="robotTesting" :disabled="!saved.enabled || !saved.robotWebhook" @click="robotTest">{{ $t('system.im.testRobot') }}</a-button>
              </div>
            </a-form>

            <div class="guide">
              <div class="guide-title"><BulbOutlined /> {{ $t('system.im.guideTitle') }}</div>
              <ol>
                <li v-for="(g, i) in GUIDES[platform]" :key="i">{{ g }}</li>
              </ol>
              <a-alert
                type="info"
                show-icon
                :message="$t('system.im.bindFirst')"
                :description="$t('system.im.bindFirstTip')"
              />
            </div>
          </div>
        </a-tab-pane>

        <!-- 成员绑定 -->
        <a-tab-pane key="members" :tab="$t('system.im.members')">
          <div class="member-bar">
            <a-space wrap>
              <a-popconfirm
                v-if="canSync"
                :title="saved.syncCreate ? $t('system.im.syncConfirmCreate', { name: current.label }) : $t('system.im.syncConfirm', { name: current.label })"
                :disabled="!saved.enabled"
                @confirm="sync"
              >
                <a-button type="primary" :loading="syncing" :disabled="!saved.enabled">
                  <template #icon><SyncOutlined /></template>{{ $t('system.im.syncContacts') }}
                </a-button>
              </a-popconfirm>
              <a-radio-group v-model:value="query.bound" button-style="solid" @change="search">
                <a-radio-button :value="undefined">{{ $t('common.status.all') }}</a-radio-button>
                <a-radio-button :value="true">{{ $t('system.im.bound') }}</a-radio-button>
                <a-radio-button :value="false">{{ $t('system.im.unbound') }}</a-radio-button>
              </a-radio-group>
              <a-input-search v-model:value="query.keyword" :placeholder="$t('system.im.keywordPlaceholder')" allow-clear class="keyword" @search="search" />
            </a-space>
            <span v-if="saved.lastSyncTime" class="sub-text">{{ $t('system.im.lastSync', { time: saved.lastSyncTime, result: saved.lastSyncResult }) }}</span>
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
                  <a-tag color="success" :bordered="false">{{ $t('system.im.bound') }}</a-tag>
                  <span>{{ record.imName || '' }}</span>
                  <span class="sub-text member-id">{{ record.imUserId }}</span>
                </template>
                <a-tag v-else :bordered="false">{{ $t('system.im.unbound') }}</a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <template v-if="canSync">
                  <a v-if="!record.bindingId" @click="openBind(record)">{{ $t('common.action.bind') }}</a>
                  <template v-else>
                    <a @click="openBind(record)">{{ $t('common.action.modify') }}</a>
                    <a-divider type="vertical" />
                    <a-popconfirm :title="$t('system.im.unbindConfirm')" @confirm="unbind(record)">
                      <a class="danger">{{ $t('common.action.unbind') }}</a>
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
    <a-modal v-model:open="bindOpen" :title="$t('system.im.bindTitle', { name: current.label })" :confirm-loading="binding" @ok="submitBind">
      <a-form layout="vertical">
        <a-form-item :label="$t('common.field.user')">{{ $t('system.im.bindUser', { name: bindTarget?.nickname || bindTarget?.username, username: bindTarget?.username }) }}</a-form-item>
        <a-form-item :label="$t('system.im.memberUserId')" required :extra="USERID_HELP[platform]">
          <a-input v-model:value="bindUserId" :placeholder="$t('common.placeholder.input')" :maxlength="64" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 同步结果 -->
    <a-modal v-model:open="resultOpen" :title="$t('system.im.syncDone')" :footer="null" :width="520">
      <template v-if="result">
        <div class="stats">
          <div class="stat"><b>{{ result.memberTotal }}</b><span>{{ $t('system.im.memberTotal') }}</span></div>
          <div class="stat"><b>{{ result.matched }}</b><span>{{ $t('system.im.matched') }}</span></div>
          <div class="stat"><b>{{ result.created }}</b><span>{{ $t('system.im.created') }}</span></div>
          <div class="stat"><b>{{ result.alreadyBound }}</b><span>{{ $t('system.im.alreadyBound') }}</span></div>
        </div>
        <p v-if="result.deptCreated" class="sub-text">{{ $t('system.im.deptCreated', { n: result.deptCreated }) }}</p>
        <a-alert v-if="result.warning" type="warning" show-icon :message="result.warning" class="mb" />
        <template v-if="result.unmatchedCount">
          <div class="unmatched-title">{{ $t('system.im.unmatched', { n: result.unmatchedCount }) }}</div>
          <div class="unmatched">
            <a-tag v-for="n in result.unmatched" :key="n" :bordered="false">{{ n }}</a-tag>
            <span v-if="result.unmatchedCount > result.unmatched.length" class="sub-text">{{ $t('system.im.etc') }}</span>
          </div>
        </template>
      </template>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { BulbOutlined, SyncOutlined } from '@ant-design/icons-vue'
import ImPlatformIcon from '@/components/im/ImPlatformIcon.vue'
import { bindImUser, listImBindings, listImConfig, saveImConfig, syncImContacts, testImRobot, unbindImUser, verifyIm } from '@/api/system/im'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { IM_PLATFORMS, imPlatform } from '@/utils/im'

/** 第三方集成：每个租户分别接入自己的钉钉、企业微信、飞书应用 */
const { t } = useI18n()
const FIELDS = {
  DINGTALK: [
    { key: 'appKey', label: t('system.im.dingAppKey') },
    { key: 'appSecret', label: t('system.im.dingAppSecret'), secret: true },
    { key: 'agentId', label: 'AgentId' }
  ],
  WECOM: [
    { key: 'corpId', label: t('system.im.corpId') },
    { key: 'agentId', label: 'AgentId' },
    { key: 'appSecret', label: t('system.im.appSecret'), secret: true }
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
  DINGTALK: t('system.im.userIdHelpDingtalk'),
  WECOM: t('system.im.userIdHelpWecom'),
  FEISHU: t('system.im.userIdHelpFeishu')
}
const GUIDES = {
  DINGTALK: [
    t('system.im.guide.DINGTALK1'),
    t('system.im.guide.DINGTALK2'),
    t('system.im.guide.DINGTALK3'),
    t('system.im.guide.DINGTALK4'),
    t('system.im.guide.DINGTALK5')
  ],
  WECOM: [
    t('system.im.guide.WECOM1'),
    t('system.im.guide.WECOM2'),
    t('system.im.guide.WECOM3'),
    t('system.im.guide.WECOM4'),
    t('system.im.guide.WECOM5')
  ],
  FEISHU: [
    t('system.im.guide.FEISHU1'),
    t('system.im.guide.FEISHU2'),
    t('system.im.guide.FEISHU3'),
    t('system.im.guide.FEISHU4'),
    t('system.im.guide.FEISHU5')
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
  if (!c?.enabled) return t('system.im.notConfigured')
  const parts = []
  if (c.loginEnabled) parts.push(t('system.im.login'))
  if (c.messageEnabled) parts.push(t('system.im.messagePush'))
  if (c.robotWebhook) parts.push(t('system.im.robot'))
  return parts.length ? parts.join(' · ') : t('system.im.enabled')
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
    message.success(t('common.msg.saved'))
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
    message.success(t('system.im.robotSent'))
  } finally {
    robotTesting.value = false
  }
}

// ---------------------------------------------------------------- 成员绑定
const query = reactive({ platform: platform.value, bound: undefined, keyword: '' })
const { list, loading: tableLoading, pagination, load, search, onTableChange } = useTable(listImBindings, query)
const columns = [
  { title: t('common.field.user'), key: 'user', width: 180 },
  { title: t('common.field.dept'), dataIndex: 'deptName', width: 160, ellipsis: true },
  { title: t('common.field.phone'), dataIndex: 'phone', width: 140 },
  { title: t('system.im.boundMember'), key: 'member', width: 260 },
  { title: t('system.im.bindTime'), dataIndex: 'bindTime', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: 160, fixed: 'right' }
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
    message.warning(t('system.im.inputUserId'))
    return
  }
  binding.value = true
  try {
    await bindImUser({ userId: bindTarget.value.userId, platform: platform.value, imUserId: bindUserId.value.trim() })
    message.success(t('system.im.boundOk'))
    bindOpen.value = false
    load()
  } finally {
    binding.value = false
  }
}
async function unbind(record) {
  await unbindImUser(record.bindingId)
  message.success(t('system.im.unboundOk'))
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
