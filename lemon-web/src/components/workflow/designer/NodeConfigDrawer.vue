<template>
  <a-drawer :open="open" :title="title" width="min(560px, 100vw)" destroy-on-close @close="emit('update:open', false)">
    <a-form v-if="draft" layout="vertical">
      <a-form-item :label="isBranch ? '分支名称' : '节点名称'" required>
        <a-input v-model:value="draft.name" :maxlength="32" placeholder="请输入名称" />
      </a-form-item>

      <!-- 审批人 / 抄送人 -->
      <template v-if="isAssigneeNode">
        <a-form-item :label="isCc ? '抄送人' : '审批人'" required>
          <a-radio-group v-model:value="draft.assignee.type" :options="assigneeTypeOptions" @change="onTypeChange" />
        </a-form-item>
        <a-form-item v-if="draft.assignee.type === 'USER'" label="选择成员">
          <div class="user-tags">
            <a-tag v-for="(id, i) in draft.assignee.ids" :key="id" closable @close.prevent="removeUser(i)">
              {{ draft.assignee.names[i] || id }}
            </a-tag>
            <a-button size="small" type="dashed" @click="pickerOpen = true">
              <template #icon><PlusOutlined /></template>选择成员
            </a-button>
          </div>
        </a-form-item>
        <a-form-item v-else-if="draft.assignee.type === 'ROLE'" label="选择角色" :extra="isCc ? '' : '拥有该角色的所有启用用户'">
          <a-select
            v-model:value="draft.assignee.ids"
            mode="multiple"
            placeholder="请选择角色"
            :options="roleOptions"
            option-filter-prop="label"
          />
        </a-form-item>
        <template v-else-if="draft.assignee.type === 'DEPT_LEADER'">
          <a-form-item label="主管级别" extra="部门负责人在「部门管理」中设置；该级部门没有负责人或负责人是发起人自己时，自动向上一级查找">
            <a-select v-model:value="draft.assignee.level" :options="LEADER_LEVELS" />
          </a-form-item>
        </template>
        <a-alert v-else type="info" show-icon :message="isCc ? '抄送给发起人自己' : '由发起人自己审批（常用于确认、补充信息）'" />

        <template v-if="!isCc">
          <a-form-item v-if="!isSingleAssignee(draft.assignee.type)" label="多人审批方式">
            <a-radio-group v-model:value="draft.approveMode" class="vertical-radio">
              <a-radio v-for="m in APPROVE_MODES" :key="m.value" :value="m.value">
                {{ m.label }}<span class="radio-desc">（{{ m.desc }}）</span>
              </a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="审批人为空时">
            <a-radio-group v-model:value="draft.emptyAction" :options="EMPTY_ACTIONS" />
          </a-form-item>
        </template>
      </template>

      <!-- 条件分支 -->
      <a-form-item v-if="isConditionBranch" label="条件" required>
        <ConditionEditor :groups="draft.conditions" :fields="fields" />
      </a-form-item>
    </a-form>

    <template #footer>
      <div class="drawer-footer">
        <a-button @click="emit('update:open', false)">取消</a-button>
        <a-button type="primary" @click="confirm">确定</a-button>
      </div>
    </template>

    <UserPicker
      v-model:open="pickerOpen"
      title="选择成员"
      :fetcher="pageIdentityUsers"
      :disabled-row="(r) => draft.assignee.ids.includes(r.id)"
      disabled-text="已选择"
      :submit="addUsers"
    />
  </a-drawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import UserPicker from '@/components/user/UserPicker.vue'
import ConditionEditor from './ConditionEditor.vue'
import { pageIdentityUsers } from '@/api/workflow/identity'
import {
  APPROVE_MODES,
  ASSIGNEE_TYPES,
  EMPTY_ACTIONS,
  LEADER_LEVELS,
  NODE_TYPES,
  emptyRule,
  isSingleAssignee,
  requiresIds
} from './flow'

/**
 * 节点 / 分支配置。target：{ node, branch? }，编辑副本，确定后通过 ok 事件交给设计器写回
 */
const props = defineProps({
  open: Boolean,
  target: Object,
  fields: { type: Array, default: () => [] },
  roles: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:open', 'ok'])

const draft = ref(null)
const pickerOpen = ref(false)

const isBranch = computed(() => !!props.target?.branch)
const isConditionBranch = computed(() => isBranch.value && props.target.node.type === NODE_TYPES.CONDITION)
const isCc = computed(() => !isBranch.value && props.target?.node.type === NODE_TYPES.CC)
const isAssigneeNode = computed(
  () => !isBranch.value && [NODE_TYPES.APPROVER, NODE_TYPES.CC].includes(props.target?.node.type)
)
const title = computed(() => {
  if (isConditionBranch.value) return '条件设置'
  if (isBranch.value) return '分支设置'
  return isCc.value ? '抄送人设置' : '审批人设置'
})
const assigneeTypeOptions = ASSIGNEE_TYPES.map(({ label, value }) => ({ label, value }))
const roleOptions = computed(() => props.roles.map((r) => ({ label: r.name, value: r.id })))

watch(
  () => props.open,
  (open) => {
    if (!open || !props.target) return
    const source = props.target.branch || props.target.node
    draft.value = JSON.parse(JSON.stringify(source))
    if (isConditionBranch.value && !draft.value.conditions?.length) draft.value.conditions = [[emptyRule()]]
  },
  { immediate: true }
)

function onTypeChange() {
  const a = draft.value.assignee
  a.ids = []
  a.names = []
  if (a.type === 'DEPT_LEADER') a.level = a.level || 1
  else delete a.level
}

function removeUser(i) {
  draft.value.assignee.ids.splice(i, 1)
  draft.value.assignee.names.splice(i, 1)
}

function addUsers(ids, rows) {
  const a = draft.value.assignee
  rows.forEach((r) => {
    if (a.ids.includes(r.id)) return
    a.ids.push(r.id)
    a.names.push(r.nickname || r.username)
  })
}

function confirm() {
  const d = draft.value
  if (!d.name?.trim()) return message.warning('请输入名称')
  d.name = d.name.trim()
  if (isAssigneeNode.value && d.assignee.type === 'ROLE') {
    d.assignee.names = d.assignee.ids.map((id) => props.roles.find((r) => r.id === id)?.name || String(id))
  }
  if (isAssigneeNode.value && !requiresIds(d.assignee.type)) {
    d.assignee.ids = []
    d.assignee.names = []
  }
  emit('ok', d)
  emit('update:open', false)
}
</script>

<style scoped>
.user-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 0;
  align-items: center;
}
.vertical-radio {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.radio-desc {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
