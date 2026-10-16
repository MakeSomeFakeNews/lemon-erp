<template>
  <div class="workspace">
    <!-- 左侧：角色列表 -->
    <a-card :bordered="false" class="page-card workspace-side">
      <div class="workspace-side-head">
        <span>角色</span>
        <a-button v-perm="'sys:role:add'" type="link" size="small" @click="openCreate">
          <template #icon><PlusOutlined /></template>新增
        </a-button>
      </div>
      <a-input v-model:value="keyword" placeholder="搜索角色名称或编码" allow-clear>
        <template #prefix><SearchOutlined /></template>
      </a-input>
      <div class="role-list">
        <a-spin :spinning="listLoading">
          <div
            v-for="r in filteredRoles"
            :key="r.id"
            class="role-item"
            :class="{ active: r.id === currentId, disabled: r.status === 0 }"
            @click="select(r.id)"
          >
            <div class="role-item-main">
              <HighlightText :text="r.name" :keyword="keyword" class="role-item-name" />
              <a-tag v-if="isAdminRole(r)" :bordered="false" color="gold" class="role-item-tag">管理员</a-tag>
              <a-tag v-else-if="r.status === 0" :bordered="false" class="role-item-tag">停用</a-tag>
            </div>
            <div class="role-item-sub">
              <span class="role-item-code">{{ r.code }}</span>
              <span><TeamOutlined /> {{ r.userCount }}</span>
            </div>
          </div>
          <a-empty
            v-if="!listLoading && !filteredRoles.length"
            :image="Empty.PRESENTED_IMAGE_SIMPLE"
            :description="keyword ? '没有匹配的角色' : '暂无角色'"
          />
        </a-spin>
      </div>
    </a-card>

    <!-- 右侧：角色详情 -->
    <div class="workspace-main">
      <template v-if="current">
        <a-card :bordered="false" class="page-card">
          <div class="role-header">
            <div class="role-heading">
              <div class="role-title">
                <span>{{ current.name }}</span>
                <a-tag :bordered="false" :color="isAdminRole(current) ? 'gold' : undefined">{{ current.code }}</a-tag>
              </div>
              <div class="role-meta">
                <span><TeamOutlined /> {{ current.userCount }} 名成员</span>
                <a-divider type="vertical" />
                <span>
                  数据权限：
                  <a-tag v-if="isAdminRole(current)" :bordered="false" color="gold">全部数据</a-tag>
                  <DictTag v-else type="sys_data_scope" :value="current.dataScope" />
                </span>
                <template v-if="current.remark">
                  <a-divider type="vertical" />
                  <span class="role-remark">{{ current.remark }}</span>
                </template>
              </div>
            </div>
            <a-space wrap>
              <StatusSwitch
                :value="current.status"
                :name="current.name"
                :api="(s) => changeRoleStatus(current.id, s)"
                :disabled="!can.edit || isAdminRole(current)"
                @changed="refresh"
              />
              <a-tooltip :title="removeTip">
                <a-button v-perm="'sys:role:remove'" danger :disabled="!!removeTip" @click="confirmRemove">
                  <template #icon><DeleteOutlined /></template>删除
                </a-button>
              </a-tooltip>
            </a-space>
          </div>
        </a-card>

        <a-card :bordered="false" class="page-card role-tabs-card">
          <a-spin :spinning="detailLoading">
            <a-tabs v-model:active-key="activeTab">
              <a-tab-pane key="basic">
                <template #tab><TabTitle title="基本信息" :dirty="dirty.basic" /></template>
                <RoleBasicPanel
                  v-if="detail"
                  v-model:dirty="dirty.basic"
                  :detail="detail"
                  :readonly="!can.edit"
                  @saved="refresh"
                />
              </a-tab-pane>
              <a-tab-pane key="menu">
                <template #tab><TabTitle title="菜单权限" :dirty="dirty.menu" /></template>
                <RoleMenuPanel
                  v-if="detail"
                  v-model:dirty="dirty.menu"
                  :detail="detail"
                  :menu-tree="menuTreeData"
                  :readonly="!can.edit"
                  @saved="refresh"
                />
              </a-tab-pane>
              <a-tab-pane key="scope">
                <template #tab><TabTitle title="数据权限" :dirty="dirty.scope" /></template>
                <RoleDataScopePanel
                  v-if="detail"
                  v-model:dirty="dirty.scope"
                  :detail="detail"
                  :dept-tree="deptTreeData"
                  :readonly="!can.dataScope"
                  @saved="refresh"
                />
              </a-tab-pane>
              <a-tab-pane key="users" :tab="`成员（${current.userCount}）`">
                <RoleUserPanel :role="current" :can-assign="canAssignUsers" @changed="loadRoles" />
              </a-tab-pane>
            </a-tabs>
          </a-spin>
        </a-card>
      </template>
      <a-card v-else :bordered="false" class="page-card empty-card">
        <a-empty :description="listLoading ? '加载中…' : '请选择左侧的角色'" />
      </a-card>
    </div>

    <!-- 新增角色：先填基本信息，创建后再分配权限 -->
    <a-modal
      v-model:open="createOpen"
      title="新增角色"
      :confirm-loading="creating"
      ok-text="创建并分配权限"
      destroy-on-close
      @ok="submitCreate"
    >
      <a-form ref="createRef" :model="createForm" :rules="createRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="角色名称" name="name">
              <a-input v-model:value="createForm.name" :maxlength="64" placeholder="如：财务专员" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="角色编码" name="code">
              <a-input v-model:value="createForm.code" :maxlength="64" placeholder="如：finance" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="排序" name="sort">
              <a-input-number v-model:value="createForm.sort" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-textarea v-model:value="createForm.remark" :rows="2" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <a-alert type="info" show-icon message="新角色默认没有菜单权限，数据权限为「本部门数据」，创建后可继续配置。" />
    </a-modal>
  </div>
</template>

<script setup>
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { Empty, Modal, message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined, SearchOutlined, TeamOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import HighlightText from '@/components/tree/HighlightText.vue'
import RoleBasicPanel from './components/RoleBasicPanel.vue'
import RoleDataScopePanel from './components/RoleDataScopePanel.vue'
import RoleMenuPanel from './components/RoleMenuPanel.vue'
import RoleUserPanel from './components/RoleUserPanel.vue'
import { ADMIN_ROLE, ROLE_CODE_RULES } from './components/role'
import { addRole, changeRoleStatus, deleteRole, getRole, listRoles } from '@/api/system/role'
import { menuTree } from '@/api/system/menu'
import { deptTree } from '@/api/system/dept'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'

useDict('sys_data_scope', 'sys_common_status')
const userStore = useUserStore()
const can = computed(() => ({
  edit: userStore.hasPerm('sys:role:edit'),
  dataScope: userStore.hasPerm('sys:role:dataScope'),
  assignUser: userStore.hasPerm('sys:role:assignUser')
}))
const isAdminRole = (role) => role?.code === ADMIN_ROLE
// 只有超级管理员能增减超级管理员角色的成员
const canAssignUsers = computed(
  () => can.value.assignUser && (!isAdminRole(current.value) || userStore.isAdmin)
)

/** 标签页标题，有未保存的修改时显示提示点 */
const TabTitle = (props) => h('span', [props.title, props.dirty ? h('span', { class: 'dirty-dot' }) : null])
TabTitle.props = ['title', 'dirty']

// ---------- 角色列表 ----------
const roles = ref([])
const listLoading = ref(false)
const keyword = ref('')
const currentId = ref()
const current = computed(() => roles.value.find((r) => r.id === currentId.value))
const filteredRoles = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return kw ? roles.value.filter((r) => r.name.toLowerCase().includes(kw) || r.code.toLowerCase().includes(kw)) : roles.value
})

async function loadRoles() {
  listLoading.value = true
  try {
    roles.value = await listRoles()
    if (!current.value) currentId.value = roles.value[0]?.id
  } finally {
    listLoading.value = false
  }
}

// ---------- 角色详情 ----------
const detail = ref()
const detailLoading = ref(false)
const activeTab = ref('basic')
const dirty = reactive({ basic: false, menu: false, scope: false })
const anyDirty = computed(() => dirty.basic || dirty.menu || dirty.scope)

async function loadDetail() {
  if (currentId.value == null) {
    detail.value = undefined
    return
  }
  const id = currentId.value
  detailLoading.value = true
  try {
    const data = await getRole(id)
    // 快速切换角色时忽略过期的响应
    if (id === currentId.value) detail.value = data
  } finally {
    if (id === currentId.value) detailLoading.value = false
  }
}
watch(currentId, loadDetail)

/** 保存后刷新列表（名称、状态、用户数）和详情 */
async function refresh() {
  await Promise.all([loadRoles(), loadDetail()])
}

/** 有未保存的修改时确认是否放弃 */
function confirmDiscard() {
  if (!anyDirty.value) return Promise.resolve(true)
  return new Promise((resolve) =>
    Modal.confirm({
      title: '放弃未保存的修改？',
      content: '当前角色有未保存的修改，切换后将会丢失。',
      okText: '放弃修改',
      okType: 'danger',
      cancelText: '继续编辑',
      onOk: () => resolve(true),
      onCancel: () => resolve(false)
    })
  )
}

async function select(id) {
  if (id === currentId.value || !(await confirmDiscard())) return
  currentId.value = id
}

// ---------- 删除 ----------
const removeTip = computed(() => {
  if (isAdminRole(current.value)) return '管理员角色不能删除'
  if (current.value?.userCount) return '角色下还有成员，请先在「成员」中移除'
  return ''
})

function confirmRemove() {
  Modal.confirm({
    title: `确定删除角色「${current.value.name}」？`,
    okType: 'danger',
    async onOk() {
      await deleteRole([current.value.id])
      message.success('删除成功')
      Object.assign(dirty, { basic: false, menu: false, scope: false })
      currentId.value = undefined
      await loadRoles()
    }
  })
}

// ---------- 新增 ----------
const createOpen = ref(false)
const creating = ref(false)
const createRef = ref()
const createForm = reactive({ name: '', code: '', sort: 0, remark: '' })
const createRules = {
  name: [{ required: true, message: '请输入角色名称' }],
  code: ROLE_CODE_RULES
}

async function openCreate() {
  if (!(await confirmDiscard())) return
  Object.assign(createForm, { name: '', code: '', sort: roles.value.length, remark: '' })
  createOpen.value = true
}

async function submitCreate() {
  await createRef.value.validate()
  creating.value = true
  try {
    await addRole({ ...createForm, menuIds: [] })
    message.success('角色已创建，请继续分配菜单权限')
    createOpen.value = false
    Object.assign(dirty, { basic: false, menu: false, scope: false })
    roles.value = await listRoles()
    currentId.value = roles.value.find((r) => r.code === createForm.code)?.id
    activeTab.value = 'menu'
  } finally {
    creating.value = false
  }
}

// ---------- 权限分配用到的树 ----------
const menuTreeData = ref([])
const deptTreeData = ref([])

onMounted(async () => {
  await loadRoles()
  ;[menuTreeData.value, deptTreeData.value] = await Promise.all([menuTree(), deptTree()])
})
</script>

<style scoped>
.role-list {
  flex: 1;
  min-height: 0;
  overflow: auto;
  margin: 0 -4px;
  padding: 0 4px;
}
.role-item {
  padding: 8px 10px;
  margin-bottom: 4px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: background 0.2s, border-color 0.2s;
}
.role-item:hover {
  background: var(--app-hover);
}
.role-item.active {
  background: var(--app-primary-bg);
  border-color: var(--app-primary);
}
.role-item.disabled .role-item-name {
  color: var(--app-text-3);
}
.role-item-main {
  display: flex;
  align-items: center;
  gap: 6px;
}
.role-item-name {
  flex: 1;
  min-width: 0;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.role-item-tag {
  margin: 0;
  font-size: 11px;
  line-height: 16px;
  padding: 0 4px;
}
.role-item-sub {
  display: flex;
  justify-content: space-between;
  margin-top: 2px;
  font-size: 12px;
  color: var(--app-text-3);
}
.role-item-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}
.role-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.role-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
}
.role-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  margin-top: 6px;
  color: var(--app-text-2);
  font-size: 13px;
}
.role-remark {
  color: var(--app-text-3);
}
.role-tabs-card {
  flex: 1;
  min-height: 0;
  overflow: auto;
}
.role-tabs-card :deep(.ant-tabs-nav) {
  margin-bottom: 16px;
}
:deep(.dirty-dot) {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-left: 4px;
  vertical-align: top;
  border-radius: 50%;
  background: #fa8c16;
}
.empty-card {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
