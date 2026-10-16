<template>
  <SubPage
    :open="open"
    :title="detail?.name || '流程详情'"
    width="min(860px, 100vw)"
    destroy-on-close
    class="instance-drawer"
    @close="emit('update:open', false)"
  >
    <template #extra>
      <DictTag v-if="detail" type="wf_status" :value="detail.status" />
    </template>

    <a-spin :spinning="loading">
      <template v-if="detail">
        <div class="instance-meta">
          <span><UserOutlined /> {{ detail.startUserName }}</span>
          <span>流程：{{ detail.processName }}</span>
          <span>发起：{{ detail.startTime }}</span>
          <span v-if="detail.endTime">结束：{{ detail.endTime }}</span>
        </div>

        <a-tabs v-model:active-key="activeTab">
          <a-tab-pane key="detail" tab="审批详情">
            <template v-if="detail.business?.length">
              <!-- 业务单据发起的流程：展示发起时的业务信息（金额、天数等），字典值显示为标签 -->
              <div class="section-title">业务信息</div>
              <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" bordered size="small">
                <a-descriptions-item v-for="b in detail.business" :key="b.field" :label="b.label">
                  <DictTag v-if="b.dictType" :type="b.dictType" :value="String(b.value)" />
                  <template v-else>{{ formatBusinessValue(b) }}</template>
                </a-descriptions-item>
              </a-descriptions>
            </template>
            <template v-else>
              <div class="section-title">表单信息</div>
              <FormView :fields="detail.form || []" :data="detail.formData || {}" />
            </template>

            <div class="section-title">审批记录</div>
            <a-timeline class="record-timeline">
              <a-timeline-item v-for="(r, i) in detail.records" :key="i" :color="ACTIONS[r.action]?.color || 'gray'">
                <template v-if="r.action === 'PENDING'" #dot><ClockCircleOutlined class="pending-dot" /></template>
                <div class="record-head">
                  <span class="record-node">{{ r.nodeName }}</span>
                  <span class="record-time">{{ r.time || '' }}</span>
                </div>
                <div class="record-user">
                  {{ r.userName || '-' }}
                  <a-tag :color="ACTIONS[r.action]?.tag" :bordered="false">{{ ACTIONS[r.action]?.label || r.action }}</a-tag>
                </div>
                <div v-if="r.comment" class="record-comment">{{ r.comment }}</div>
              </a-timeline-item>
            </a-timeline>
          </a-tab-pane>
          <a-tab-pane key="flow" tab="流程图">
            <div class="flow-box">
              <FlowDesigner
                v-if="detail.flow"
                :model-value="detail.flow"
                :fields="detail.form || []"
                :node-status="detail.nodeStatus || {}"
                readonly
              />
              <a-empty v-else description="无流程图" />
            </div>
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-spin>

    <template v-if="detail && (canHandle || detail.cancelable)" #footer>
      <div class="drawer-footer">
        <a-button v-if="detail.cancelable" danger @click="openAction('cancel')">
          <template #icon><RollbackOutlined /></template>撤回
        </a-button>
        <template v-if="canHandle">
          <a-button @click="openAction('transfer')">
            <template #icon><SwapOutlined /></template>转交
          </a-button>
          <a-button danger @click="openAction('reject')">
            <template #icon><CloseOutlined /></template>拒绝
          </a-button>
          <a-button type="primary" @click="openAction('approve')">
            <template #icon><CheckOutlined /></template>同意
          </a-button>
        </template>
      </div>
    </template>

    <!-- 同意 / 拒绝 / 转交 / 撤回 -->
    <a-modal
      v-model:open="actionOpen"
      :title="ACTION_FORMS[action]?.title"
      :confirm-loading="submitting"
      :ok-text="ACTION_FORMS[action]?.okText"
      :ok-button-props="{ danger: action === 'reject' || action === 'cancel' }"
      destroy-on-close
      @ok="submitAction"
    >
      <a-form ref="actionRef" :model="actionForm" layout="vertical">
        <a-form-item
          v-if="action === 'transfer'"
          label="转交给"
          name="user"
          :rules="[{ required: true, message: '请选择转交人' }]"
        >
          <div class="transfer-user">
            <span v-if="actionForm.user">{{ actionForm.user.nickname || actionForm.user.username }}</span>
            <span v-else class="sub-text">未选择</span>
            <a-button size="small" @click="pickerOpen = true">选择</a-button>
          </div>
        </a-form-item>
        <a-form-item
          :label="ACTION_FORMS[action]?.label"
          name="comment"
          :rules="action === 'reject' ? [{ required: true, whitespace: true, message: '请填写拒绝理由' }] : []"
        >
          <a-textarea
            v-model:value="actionForm.comment"
            :rows="4"
            :maxlength="500"
            show-count
            :placeholder="ACTION_FORMS[action]?.placeholder"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <UserPicker
      v-model:open="pickerOpen"
      title="选择转交人"
      :multiple="false"
      :fetcher="pageIdentityUsers"
      :disabled-row="(r) => r.id === userStore.user?.id"
      disabled-text="自己"
      :submit="(ids, rows) => (actionForm.user = rows[0])"
    />
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  CheckOutlined,
  ClockCircleOutlined,
  CloseOutlined,
  RollbackOutlined,
  SwapOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import UserPicker from '@/components/user/UserPicker.vue'
import FlowDesigner from './designer/FlowDesigner.vue'
import FormView from './form/FormView.vue'
import { useDictStore } from '@/store/dict'
import { pageIdentityUsers } from '@/api/workflow/identity'
import { cancelInstance, getInstance } from '@/api/workflow/instance'
import { approveTask, rejectTask, transferTask } from '@/api/workflow/task'
import { useUserStore } from '@/store/user'

/**
 * 流程实例详情：表单、审批记录、流程进度；当前用户有待办时可同意 / 拒绝 / 转交，发起人可撤回
 */
const props = defineProps({
  open: Boolean,
  instanceId: String
})
const emit = defineEmits(['update:open', 'changed'])
const userStore = useUserStore()

const ACTIONS = {
  START: { label: '发起', color: 'blue', tag: 'blue' },
  APPROVE: { label: '同意', color: 'green', tag: 'green' },
  REJECT: { label: '拒绝', color: 'red', tag: 'red' },
  TRANSFER: { label: '转交', color: 'blue', tag: 'blue' },
  CANCEL: { label: '撤回', color: 'gray', tag: 'default' },
  TERMINATE: { label: '终止', color: 'red', tag: 'red' },
  CC: { label: '抄送', color: 'purple', tag: 'purple' },
  PENDING: { label: '待审批', color: 'gray', tag: 'orange' }
}

const ACTION_FORMS = {
  approve: { title: '同意', okText: '同意', label: '审批意见', placeholder: '选填' },
  reject: { title: '拒绝', okText: '拒绝', label: '拒绝理由', placeholder: '请填写拒绝理由' },
  transfer: { title: '转交', okText: '转交', label: '转交说明', placeholder: '选填' },
  cancel: { title: '撤回申请', okText: '撤回', label: '撤回原因', placeholder: '选填，撤回后流程结束' }
}

const loading = ref(false)
const detail = ref(null)
const activeTab = ref('detail')
const canHandle = computed(() => !!detail.value?.todoTaskId && userStore.hasPerm('wf:task:todo'))

// 忽略过期响应：快速切换实例时只采用最后一次请求
let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    const data = await getInstance(props.instanceId)
    if (current === seq) detail.value = data
  } finally {
    if (current === seq) loading.value = false
  }
}

watch(
  () => [props.open, props.instanceId],
  ([open, id]) => {
    if (!open || !id) return
    detail.value = null
    activeTab.value = 'detail'
    load()
  },
  { immediate: true }
)

// 操作
const actionOpen = ref(false)
const action = ref('approve')
const actionRef = ref()
const actionForm = reactive({ comment: '', user: null })
const submitting = ref(false)
const pickerOpen = ref(false)

function openAction(type) {
  action.value = type
  actionForm.comment = ''
  actionForm.user = null
  actionOpen.value = true
}

async function submitAction() {
  await actionRef.value.validate()
  const taskId = detail.value.todoTaskId
  const comment = actionForm.comment?.trim() || undefined
  submitting.value = true
  try {
    if (action.value === 'approve') await approveTask(taskId, comment)
    if (action.value === 'reject') await rejectTask(taskId, comment)
    if (action.value === 'transfer') await transferTask(taskId, actionForm.user.id, comment)
    if (action.value === 'cancel') await cancelInstance(detail.value.id, comment)
    message.success(`已${ACTION_FORMS[action.value].okText}`)
    actionOpen.value = false
    emit('changed')
    // 处理完成后流程可能已结束或流转给他人，重新加载展示最新进度
    load()
  } finally {
    submitting.value = false
  }
}

// 业务变量中的字典（请假类型、凭证来源等）
const dictStore = useDictStore()
watch(
  () => detail.value?.business,
  (list) => (list || []).forEach((b) => b.dictType && dictStore.load(b.dictType).catch(() => {}))
)
const formatBusinessValue = (b) =>
  b.type === 'NUMBER' && typeof b.value === 'number' ? b.value.toLocaleString('zh-CN', { maximumFractionDigits: 2 }) : b.value
</script>

<style scoped>
.instance-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  color: var(--app-text-2);
  font-size: 13px;
}
.section-title {
  margin: 8px 0 12px;
  font-weight: 600;
}
.section-title:not(:first-child) {
  margin-top: 24px;
}
.record-timeline {
  padding-top: 4px;
}
.record-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.record-node {
  font-weight: 500;
}
.record-time {
  color: var(--app-text-3);
  font-size: 12px;
}
.record-user {
  margin-top: 4px;
  color: var(--app-text-2);
}
.record-user :deep(.ant-tag) {
  margin-left: 6px;
}
.record-comment {
  margin-top: 6px;
  padding: 8px 12px;
  background: var(--app-hover);
  border-radius: 6px;
  white-space: pre-wrap;
  word-break: break-all;
}
.pending-dot {
  color: #fa8c16;
}
.flow-box {
  height: calc(100vh - 260px);
  min-height: 400px;
}
.transfer-user {
  display: flex;
  align-items: center;
  gap: 12px;
}
.sub-text {
  color: var(--app-text-3);
}
</style>
