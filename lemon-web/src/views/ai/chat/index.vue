<template>
  <div class="ai-chat">
    <!-- 未配置 / 未开启时的引导 -->
    <a-card v-if="status && !status.features?.CHAT" :bordered="false" class="page-card guide-card">
      <a-result :title="$t('ai.chat.notAvailable')" :sub-title="status.configured ? $t('ai.chat.disabledDesc') : $t('ai.chat.notConfiguredDesc')">
        <template #icon><RobotOutlined class="guide-icon" /></template>
        <template #extra>
          <a-button v-if="canConfig" type="primary" @click="router.push('/ai/settings')">{{ $t('ai.chat.goSettings') }}</a-button>
          <span v-else class="sub-text">{{ $t('ai.chat.contactAdmin') }}</span>
        </template>
      </a-result>
    </a-card>

    <div v-else-if="status" class="chat-layout page-card">
      <!-- 会话列表：宽屏在左侧，窄屏在抽屉中 -->
      <div class="sider">
        <ConversationList :conversations="conversations" :current-id="currentId" @new="newChat" @select="select" @rename="openRename" @remove="remove" />
      </div>
      <a-drawer v-model:open="listOpen" placement="left" width="min(300px, 85vw)" :title="$t('ai.chat.conversations')">
        <ConversationList
          :conversations="conversations"
          :current-id="currentId"
          @new="newChat(), (listOpen = false)"
          @select="(id) => (select(id), (listOpen = false))"
          @rename="openRename"
          @remove="remove"
        />
      </a-drawer>

      <!-- 对话 -->
      <div class="main">
        <!-- 窄屏：打开会话列表、新建对话 -->
        <div class="mobile-bar">
          <a-button type="text" :aria-label="$t('ai.chat.conversations')" @click="listOpen = true">
            <template #icon><UnorderedListOutlined /></template>
          </a-button>
          <span class="mobile-title">{{ currentTitle }}</span>
          <a-button type="text" :aria-label="$t('ai.chat.newChat')" :disabled="streaming" @click="newChat">
            <template #icon><PlusOutlined /></template>
          </a-button>
        </div>
        <div ref="scrollRef" class="messages" @scroll="onScroll">
          <div class="messages-inner">
            <div v-if="!items.length && !loadingMessages" class="welcome">
              <RobotOutlined class="welcome-icon" />
              <h3>{{ $t('ai.chat.welcome') }}</h3>
              <div class="sub-text">{{ $t('ai.chat.welcomeDesc') }}</div>
              <div class="examples">
                <a-button v-for="q in examples" :key="q" class="example" @click="ask(q)">{{ q }}</a-button>
              </div>
            </div>
            <a-spin v-if="loadingMessages" class="loading" />
            <template v-for="item in items" :key="item.key">
              <div v-if="item.kind === 'user'" class="user-row">
                <div class="user-bubble">{{ item.content }}</div>
              </div>
              <ChatTurn v-else :turn="item" @toggle-data="toggleData" />
            </template>
          </div>
        </div>

        <div class="composer">
          <div class="composer-box">
            <a-textarea
              v-model:value="input"
              :auto-size="{ minRows: 1, maxRows: 6 }"
              :placeholder="$t('ai.chat.placeholder')"
              :maxlength="4000"
              :bordered="false"
              @keydown="onKeydown"
            />
            <a-button v-if="streaming" danger shape="round" @click="stop">
              <template #icon><PauseCircleOutlined /></template>{{ $t('ai.chat.stop') }}
            </a-button>
            <a-button v-else type="primary" shape="round" :disabled="!input.trim()" @click="ask(input)">
              <template #icon><SendOutlined /></template>{{ $t('ai.chat.send') }}
            </a-button>
          </div>
          <div class="disclaimer sub-text">{{ $t('ai.chat.disclaimer') }}</div>
        </div>
      </div>
    </div>

    <a-modal v-model:open="renameOpen" :title="$t('ai.chat.renameTitle')" @ok="submitRename">
      <a-input v-model:value="renameTitle" :maxlength="100" @press-enter="submitRename" />
    </a-modal>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Modal } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import {
  PauseCircleOutlined,
  PlusOutlined,
  RobotOutlined,
  SendOutlined,
  UnorderedListOutlined
} from '@ant-design/icons-vue'
import { getAiStatus } from '@/api/ai/config'
import {
  createConversation,
  deleteConversation,
  getToolData,
  listConversations,
  listMessages,
  renameConversation,
  sendMessage
} from '@/api/ai/chat'
import { useUserStore } from '@/store/user'
import { AiMessageRole } from '@/constants/enums'
import ChatTurn from './ChatTurn.vue'
import ConversationList from './ConversationList.vue'

/** AI 问数：左侧本人的会话，右侧对话（SSE 流式回答，工具调用过程与数据表格） */
const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const canConfig = computed(() => userStore.hasPerm('ai:config:list'))
const examples = [t('ai.chat.example1'), t('ai.chat.example2'), t('ai.chat.example3'), t('ai.chat.example4')]

const status = ref(null)
const conversations = ref([])
const currentId = ref(null)
const items = ref([])
const loadingMessages = ref(false)
const input = ref('')
const streaming = ref(false)
let controller = null
// 窄屏的会话列表抽屉
const listOpen = ref(false)
const currentTitle = computed(() => {
  if (!currentId.value) return t('ai.chat.newChat')
  return conversations.value.find((c) => c.id === currentId.value)?.title || t('ai.chat.untitled')
})

async function loadConversations() {
  conversations.value = await listConversations()
}

function newTurn(key, streamingTurn = false) {
  return reactive({ kind: 'assistant', key, content: '', steps: [], streaming: streamingTurn, error: null, cancelled: false, tokens: null })
}

/** 消息历史 → 显示项：连续的模型 / 工具消息合并为一次回答，工具消息为查询过程中的一步（数据按需加载） */
function buildItems(messages) {
  const out = []
  let turn = null
  for (const m of messages) {
    if (m.role === AiMessageRole.USER) {
      out.push({ kind: 'user', key: `m${m.id}`, content: m.content })
      turn = null
      continue
    }
    if (!turn) {
      turn = newTurn(`t${m.id}`)
      out.push(turn)
    }
    if (m.role === AiMessageRole.TOOL) {
      turn.steps.push(reactive({ key: `s${m.id}`, tool: m.toolName, label: m.toolLabel, arguments: m.content, messageId: m.id, open: false }))
    } else {
      if (m.content) turn.content += (turn.content ? '\n\n' : '') + m.content
      if (!m.hasToolCalls) turn.tokens = m.tokens
    }
  }
  return out
}

async function select(id) {
  if (streaming.value || id === currentId.value) return
  currentId.value = id
  items.value = []
  loadingMessages.value = true
  try {
    items.value = buildItems(await listMessages(id))
    scrollToBottom(true)
  } finally {
    loadingMessages.value = false
  }
}

function newChat() {
  if (streaming.value) return
  currentId.value = null
  items.value = []
}

async function toggleData(step) {
  step.open = !step.open
  if (!step.open || step.loaded) return
  step.loading = true
  try {
    const d = await getToolData(step.messageId)
    Object.assign(step, { data: d.data, total: d.total, truncated: d.truncated, error: d.error, loaded: true })
  } finally {
    step.loading = false
  }
}

// ---------------------------------------------------------------- 发送与流式回答

function onKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
    e.preventDefault()
    if (!streaming.value && input.value.trim()) ask(input.value)
  }
}

async function ask(text) {
  const content = text.trim()
  if (!content || streaming.value) return
  streaming.value = true
  try {
    if (!currentId.value) {
      const c = await createConversation()
      currentId.value = c.id
    }
    input.value = ''
    items.value.push({ kind: 'user', key: `u${Date.now()}`, content })
    const turn = newTurn(`a${Date.now()}`, true)
    items.value.push(turn)
    scrollToBottom(true)
    controller = new AbortController()
    try {
      await sendMessage(currentId.value, content, { onEvent: (name, data) => onEvent(turn, name, data) }, controller.signal)
    } catch {
      // 请求失败（未开启、超过上限等）已提示：去掉这次的提问和空回答
      if (!turn.content && !turn.steps.length) items.value.splice(items.value.length - 2, 2)
      input.value = input.value || content
    }
    if (controller?.signal.aborted) turn.cancelled = true
    turn.streaming = false
    turn.steps.forEach((s) => (s.running = false))
    loadConversations()
  } finally {
    streaming.value = false
    controller = null
  }
}

function onEvent(turn, name, data) {
  if (name === 'delta') {
    turn.content += data.text
  } else if (name === 'status') {
    turn.steps.push(reactive({ key: `s${turn.steps.length}`, tool: data.tool, status: data.text, running: true, open: false }))
  } else if (name === 'data') {
    const step = [...turn.steps].reverse().find((s) => s.running && s.tool === data.tool)
    const fields = {
      label: data.label,
      arguments: data.arguments,
      messageId: data.messageId,
      data: data.data,
      total: data.total,
      truncated: data.truncated,
      error: data.error,
      running: false,
      loaded: true,
      open: !data.error
    }
    if (step) Object.assign(step, fields)
    else turn.steps.push(reactive({ key: `s${turn.steps.length}`, tool: data.tool, ...fields }))
  } else if (name === 'done') {
    turn.tokens = (data.usage?.promptTokens || 0) + (data.usage?.completionTokens || 0)
    turn.estimated = data.usage?.estimated
    turn.cancelled = data.cancelled
  } else if (name === 'error') {
    turn.error = data.message
  }
  scrollToBottom()
}

function stop() {
  controller?.abort()
}

// ---------------------------------------------------------------- 滚动：在底部附近时跟随新内容

const scrollRef = ref()
let stick = true
function onScroll() {
  const el = scrollRef.value
  if (el) stick = el.scrollHeight - el.scrollTop - el.clientHeight < 80
}
function scrollToBottom(force = false) {
  if (!force && !stick) return
  nextTick(() => {
    const el = scrollRef.value
    if (el) el.scrollTop = el.scrollHeight
    stick = true
  })
}

// ---------------------------------------------------------------- 会话管理

const renameOpen = ref(false)
const renameTitle = ref('')
let renameTarget = null
function openRename(c) {
  renameTarget = c
  renameTitle.value = c.title || ''
  renameOpen.value = true
}
async function submitRename() {
  await renameConversation(renameTarget.id, renameTitle.value)
  renameOpen.value = false
  loadConversations()
}

function remove(c) {
  Modal.confirm({
    title: t('ai.chat.deleteConfirm'),
    okType: 'danger',
    onOk: async () => {
      await deleteConversation(c.id)
      if (c.id === currentId.value) newChat()
      loadConversations()
    }
  })
}

onMounted(async () => {
  status.value = await getAiStatus()
  if (status.value.features?.CHAT) loadConversations()
})
onBeforeUnmount(() => controller?.abort())
</script>

<style scoped>
.ai-chat {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.guide-card {
  flex: 1;
  margin: 0;
}
.guide-icon {
  font-size: 64px;
  color: var(--app-primary);
}
.chat-layout {
  flex: 1;
  background: var(--app-card);
  border-radius: 8px;
  min-height: 0;
  display: flex;
  margin: 0;
  overflow: hidden;
  padding: 0;
}
.sider {
  width: 260px;
  flex: none;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--app-border);
  padding: 12px 8px;
}
.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.mobile-bar {
  display: none;
}
.messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 24px 24px 8px;
}
.messages-inner {
  max-width: 880px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.loading {
  align-self: center;
  margin-top: 48px;
}
.welcome {
  text-align: center;
  padding-top: 8vh;
}
.welcome-icon {
  font-size: 48px;
  color: var(--app-primary);
}
.welcome h3 {
  margin: 12px 0 4px;
  font-size: 20px;
}
.examples {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.example {
  height: auto;
  white-space: normal;
  text-align: left;
  padding: 10px 14px;
}
.user-row {
  display: flex;
  justify-content: flex-end;
}
.user-bubble {
  max-width: 75%;
  padding: 8px 14px;
  border-radius: 12px 12px 2px 12px;
  background: var(--app-primary);
  color: #fff;
  white-space: pre-wrap;
  word-break: break-word;
}
.composer {
  flex: none;
  padding: 8px 24px 12px;
}
.composer-box {
  max-width: 880px;
  margin: 0 auto;
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 8px 8px 8px 4px;
  border: 1px solid var(--app-border);
  border-radius: 12px;
  background: var(--app-card);
  box-shadow: var(--app-shadow);
}
.composer-box:focus-within {
  border-color: var(--app-primary);
}
.disclaimer {
  max-width: 880px;
  margin: 6px auto 0;
  text-align: center;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
@media (max-width: 767px) {
  .sider {
    display: none;
  }
  .mobile-bar {
    flex: none;
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 6px 8px;
    border-bottom: 1px solid var(--app-border);
  }
  .mobile-title {
    flex: 1;
    min-width: 0;
    text-align: center;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .examples {
    grid-template-columns: 1fr;
  }
  .messages,
  .composer {
    padding-left: 12px;
    padding-right: 12px;
  }
}
</style>
