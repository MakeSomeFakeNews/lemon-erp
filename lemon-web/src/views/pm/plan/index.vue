<template>
  <div class="plan-page">
    <a-card :bordered="false" class="page-card">
      <div class="head">
        <ProjectSelect v-model:value="projectId" :allow-clear="false" class="project-select" placeholder="选择项目" @update:value="onProject" />
        <template v-if="project">
          <DictTag type="pm_project_status" :value="project.status" />
          <div class="summary">
            <span class="label">完工进度</span>
            <a-progress :percent="Number(project.progress)" size="small" class="progress" />
            <span class="sub-text">
              计划 {{ project.planStart }} ~ {{ project.planEnd }} · 任务 {{ project.doneTaskCount }} / {{ project.taskCount }}
              <span v-if="project.overdueTaskCount" class="danger">· 逾期 {{ project.overdueTaskCount }}</span>
            </span>
          </div>
        </template>
        <div class="actions">
          <a-radio-group v-model:value="view" button-style="solid" size="small">
            <a-radio-button value="list">列表</a-radio-button>
            <a-radio-button value="gantt">甘特图</a-radio-button>
          </a-radio-group>
          <a-button v-if="project" v-perm="'pm:task:edit'" type="primary" :disabled="!editable" @click="openTask()">
            <template #icon><PlusOutlined /></template>新增任务
          </a-button>
          <a-button type="text" :disabled="!projectId" @click="loadTasks"><template #icon><ReloadOutlined /></template></a-button>
        </div>
      </div>
      <a-alert v-if="project && project.status !== 'ACTIVE'" type="info" show-icon class="tip"
               :message="project.status === 'PLANNING' ? '项目还没有启动，启动后任务负责人才能更新进度' : '项目不在进行中，不能更新任务进度'" />
    </a-card>

    <a-card :bordered="false" class="page-card body-card">
      <a-empty v-if="!projectId" description="请选择项目" />
      <template v-else-if="view === 'list'">
        <a-table
          row-key="id"
          size="middle"
          :columns="columns"
          :data-source="tasks"
          :loading="loading"
          :pagination="false"
          :expanded-row-keys="expanded"
          :scroll="{ x: 1300 }"
          @expand="onExpand"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <FlagFilled v-if="record.milestone === 1" class="milestone" />
              <span :class="{ parent: record.children?.length }">{{ record.name }}</span>
            </template>
            <template v-else-if="column.key === 'dates'">
              <div>{{ record.planStart }} ~ {{ record.planEnd }}</div>
              <div v-if="record.actualEnd" class="sub-text">完成于 {{ record.actualEnd }}</div>
            </template>
            <template v-else-if="column.key === 'hours'">
              {{ Number(record.planHours) }} / <span :class="{ danger: Number(record.actualHours) > Number(record.planHours) && Number(record.planHours) > 0 }">{{ Number(record.actualHours) }}</span>
            </template>
            <template v-else-if="column.key === 'progress'">
              <a-popover v-if="canProgress(record)" trigger="click" title="更新进度" :open="progressFor === record.id"
                         @open-change="(v) => togglePopover(v, record)">
                <template #content>
                  <div class="progress-pop">
                    <a-slider v-model:value="progressValue" :step="5" :marks="{ 0: '0', 50: '50', 100: '100' }" />
                    <div class="pop-actions">
                      <a-button size="small" @click="progressFor = null">取消</a-button>
                      <a-button size="small" type="primary" :loading="progressSaving" @click="saveProgress(record)">保存</a-button>
                    </div>
                  </div>
                </template>
                <a-progress :percent="record.progress" size="small" class="clickable" :status="record.overdue ? 'exception' : undefined" />
              </a-popover>
              <a-progress v-else :percent="record.progress" size="small" :status="record.overdue ? 'exception' : undefined" />
            </template>
            <template v-else-if="column.key === 'status'">
              <DictTag type="pm_task_status" :value="record.status" />
              <a-tag v-if="record.overdue" color="red" :bordered="false">逾期</a-tag>
            </template>
            <template v-else-if="column.key === 'action'">
              <span v-perm="'pm:task:edit'" class="table-actions">
                <a-button type="link" size="small" :disabled="!editable" @click="openTask(null, record)">子任务</a-button>
                <a-button type="link" size="small" :disabled="!editable" @click="openTask(record)"><EditOutlined /></a-button>
                <a-popconfirm :title="`确定删除任务「${record.name}」${record.children?.length ? '及其下级任务' : ''}？`" :disabled="!editable"
                              @confirm="removeTask(record)">
                  <a-button type="link" danger size="small" :disabled="!editable"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </template>
          </template>
        </a-table>
      </template>
      <GanttChart v-else :tasks="tasks" max-height="calc(100vh - 300px)" />
    </a-card>

    <!-- 任务 -->
    <a-drawer :open="taskOpen" :title="taskForm.id ? '编辑任务' : '新增任务'" width="min(640px, 100vw)" destroy-on-close @close="taskOpen = false">
      <a-form ref="taskRef" :model="taskForm" :rules="taskRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="上级任务" name="parentId" extra="上级任务的计划日期、工时、进度由下级任务汇总">
              <a-tree-select v-model:value="taskForm.parentId" :tree-data="parentTree" allow-clear tree-default-expand-all
                             placeholder="顶级任务" :field-names="{ label: 'name', value: 'id', children: 'children' }" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="任务名称" name="name">
              <a-input v-model:value="taskForm.name" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="负责人" name="assigneeId">
              <IdentityUserSelect v-model:value="taskForm.assigneeId" :label="taskForm.assigneeName" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="计划工时" name="planHours">
              <a-input-number v-model:value="taskForm.planHours" :min="0" :precision="1" addon-after="小时" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="12">
            <a-form-item label="计划开始" name="planStart">
              <a-date-picker v-model:value="taskForm.planStart" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="12">
            <a-form-item label="计划完成" name="planEnd">
              <a-date-picker v-model:value="taskForm.planEnd" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="前置任务" name="predecessorId" extra="前置任务完成后才能开始（完成-开始）">
              <a-select v-model:value="taskForm.predecessorId" allow-clear show-search option-filter-prop="label" :options="predecessorOptions"
                        placeholder="无" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="排序" name="sort">
              <a-input-number v-model:value="taskForm.sort" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="里程碑" name="milestone">
              <a-switch v-model:checked="taskForm.milestone" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="说明" name="description">
              <a-textarea v-model:value="taskForm.description" :rows="3" :maxlength="500" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <div class="drawer-footer">
          <a-button @click="taskOpen = false">取消</a-button>
          <a-button type="primary" :loading="taskSaving" @click="saveTask">保存</a-button>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, FlagFilled, PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import GanttChart from '@/components/pm/GanttChart.vue'
import ProjectSelect from '@/components/pm/ProjectSelect.vue'
import { addTask, deleteTask, getProject, projectOptions, taskTree, updateTask, updateTaskProgress } from '@/api/pm'
import { useUserStore } from '@/store/user'
import { load as loadStorage, save as saveStorage } from '@/utils/storage'
import { flattenTasks, today } from '@/components/pm/pm'

const STORAGE_KEY = 'pm-plan-project'
const userStore = useUserStore()

const projectId = ref(loadStorage(STORAGE_KEY, undefined) || undefined)
const project = ref(null)
const tasks = ref([])
const loading = ref(false)
const view = ref('list')
const expanded = ref([])

const editable = computed(() => project.value && !['FINISHED', 'CLOSED'].includes(project.value.status))

const columns = [
  { title: '任务', key: 'name', width: 280, fixed: 'left' },
  { title: '负责人', dataIndex: 'assigneeName', width: 100 },
  { title: '计划日期', key: 'dates', width: 210 },
  { title: '计划 / 实际工时', key: 'hours', width: 130 },
  { title: '进度', key: 'progress', width: 170 },
  { title: '状态', key: 'status', width: 140 },
  { title: '前置任务', dataIndex: 'predecessorName', width: 140, ellipsis: true },
  { title: '操作', key: 'action', width: 170, fixed: 'right' }
]

async function onProject(id) {
  saveStorage(STORAGE_KEY, id)
  await loadTasks()
}

async function loadTasks() {
  if (!projectId.value) return
  loading.value = true
  try {
    const [p, t] = await Promise.all([getProject(projectId.value), taskTree(projectId.value)])
    project.value = p
    tasks.value = t
    expanded.value = flattenTasks(t).filter((x) => x.children?.length).map((x) => x.id)
  } catch {
    // 项目已删除等：清除记住的选择
    projectId.value = undefined
    project.value = null
    tasks.value = []
    saveStorage(STORAGE_KEY, undefined)
  } finally {
    loading.value = false
  }
}
onMounted(async () => {
  // 没有记住的项目时默认选中第一个进行中的项目
  if (!projectId.value) {
    const list = await projectOptions().catch(() => [])
    projectId.value = (list.find((p) => p.status === 'ACTIVE') || list[0])?.id
  }
  await loadTasks()
})

function onExpand(open, record) {
  expanded.value = open ? [...expanded.value, record.id] : expanded.value.filter((k) => k !== record.id)
}

// ---------------------------------------------------------------- 进度

const progressFor = ref(null)
const progressValue = ref(0)
const progressSaving = ref(false)

/** 末级任务，项目进行中，负责人本人或有任务维护权限 */
const canProgress = (t) =>
  project.value?.status === 'ACTIVE' && !t.children?.length &&
  (t.assigneeId === userStore.user?.id || userStore.hasPerm('pm:task:edit'))

function togglePopover(open, record) {
  progressFor.value = open ? record.id : null
  if (open) progressValue.value = record.progress
}

async function saveProgress(record) {
  progressSaving.value = true
  try {
    await updateTaskProgress(record.id, progressValue.value)
    message.success('进度已更新')
    progressFor.value = null
    loadTasks()
  } finally {
    progressSaving.value = false
  }
}

// ---------------------------------------------------------------- 任务

const taskOpen = ref(false)
const taskSaving = ref(false)
const taskRef = ref()
const taskForm = ref({})

const taskRules = {
  name: [{ required: true, whitespace: true, message: '请输入任务名称' }],
  planStart: [{ required: true, message: '请选择计划开始日期' }],
  planEnd: [{ required: true, message: '请选择计划完成日期' }]
}

/** 上级任务可选范围：排除自己和自己的下级 */
const parentTree = computed(() => {
  const strip = (list) =>
    (list || []).filter((t) => t.id !== taskForm.value.id).map((t) => ({ id: t.id, name: t.name, children: strip(t.children) }))
  return strip(tasks.value)
})

const predecessorOptions = computed(() =>
  flattenTasks(tasks.value)
    .filter((t) => t.id !== taskForm.value.id)
    .map((t) => ({ value: t.id, label: `${'　'.repeat(t.level)}${t.name}（${t.planEnd} 完成）` }))
)

function openTask(record, parent) {
  if (record) {
    taskForm.value = {
      id: record.id, parentId: record.parentId || undefined, name: record.name, assigneeId: record.assigneeId,
      assigneeName: record.assigneeName, planStart: record.planStart, planEnd: record.planEnd, planHours: Number(record.planHours),
      milestone: record.milestone === 1, predecessorId: record.predecessorId || undefined, sort: record.sort, description: record.description
    }
  } else {
    taskForm.value = {
      id: undefined, parentId: parent?.id, name: '', assigneeId: undefined, planStart: parent?.planStart || project.value?.planStart || today(),
      planEnd: parent?.planEnd || project.value?.planEnd || today(), planHours: 8, milestone: false, predecessorId: undefined, sort: 0,
      description: undefined
    }
  }
  taskOpen.value = true
}

async function saveTask() {
  try {
    await taskRef.value.validate()
  } catch {
    return
  }
  taskSaving.value = true
  try {
    const f = taskForm.value
    const payload = {
      projectId: projectId.value, parentId: f.parentId || 0, name: f.name, assigneeId: f.assigneeId || null, planStart: f.planStart,
      planEnd: f.planEnd, planHours: f.planHours ?? 0, milestone: f.milestone ? 1 : 0, predecessorId: f.predecessorId || null,
      sort: f.sort ?? 0, description: f.description || null
    }
    f.id ? await updateTask(f.id, payload) : await addTask(payload)
    message.success('保存成功')
    taskOpen.value = false
    loadTasks()
  } finally {
    taskSaving.value = false
  }
}

async function removeTask(record) {
  await deleteTask(record.id)
  message.success('删除成功')
  loadTasks()
}
</script>

<style scoped>
.plan-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.plan-page > .page-card + .page-card {
  margin-top: 0;
}
.head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}
.project-select {
  width: 300px;
}
.summary {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.summary .label {
  color: var(--app-text-2);
}
.progress {
  width: 160px;
  margin: 0;
}
.actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}
.tip {
  margin-top: 12px;
}
.milestone {
  color: #faad14;
  margin-right: 4px;
}
.parent {
  font-weight: 600;
}
.clickable {
  cursor: pointer;
}
.progress-pop {
  width: 240px;
}
.pop-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.danger {
  color: #f5222d;
}
.full {
  width: 100%;
}
</style>
