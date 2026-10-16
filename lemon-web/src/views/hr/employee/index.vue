<template>
  <div class="workspace">
    <a-card v-show="showDept" :bordered="false" class="page-card workspace-side">
      <DeptTree v-model="query.deptId" :tree="deptList" :loading="deptLoading" @change="search" />
    </a-card>

    <div class="table-page workspace-main">
      <a-row :gutter="12" class="stats">
        <a-col v-for="s in statCards" :key="s.label" :xs="12" :md="6">
          <a-card :bordered="false" class="page-card stat-card">
            <div class="stat-label">{{ s.label }}</div>
            <div class="stat-value" :style="{ color: s.color }">{{ s.value }}</div>
          </a-card>
        </a-col>
      </a-row>

      <a-card v-show="showSearch" :bordered="false" class="page-card">
        <SearchForm :model="query" @search="search" @reset="resetQuery">
          <a-form-item label="员工" name="keyword">
            <a-input v-model:value="query.keyword" placeholder="工号、姓名或手机号" allow-clear />
          </a-form-item>
          <a-form-item label="状态" name="status">
            <a-select v-model:value="query.status" placeholder="在职（不含离职）" allow-clear :options="toOptions(dict.hr_emp_status)" />
          </a-form-item>
          <a-form-item label="职位" name="positionId">
            <PositionSelect v-model:value="query.positionId" placeholder="全部" />
          </a-form-item>
          <a-form-item label="用工类型" name="empType">
            <a-select v-model:value="query.empType" placeholder="全部" allow-clear :options="toOptions(dict.hr_emp_type)" />
          </a-form-item>
        </SearchForm>
      </a-card>

      <a-card :bordered="false" class="page-card table-card">
        <TableToolbar
          :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="reload">
          <a-button v-perm="'hr:employee:add'" type="primary" @click="drawer.openCreate({ deptId: query.deptId })">
            <template #icon><PlusOutlined /></template>办理入职
          </a-button>
          <a-button v-perm="'hr:employee:add'" @click="importOpen = true">
            <template #icon><ImportOutlined /></template>导入
          </a-button>
          <template #extra>
            <a-tooltip :title="showDept ? '隐藏部门' : '显示部门'">
              <a-button type="text" @click="showDept = !showDept">
                <template #icon><MenuFoldOutlined v-if="showDept" /><MenuUnfoldOutlined v-else /></template>
              </a-button>
            </a-tooltip>
          </template>
        </TableToolbar>

        <a-table
          row-key="id"
          :size="size"
          :columns="visibleColumns"
          :data-source="list"
          :loading="loading"
          :pagination="pagination"
          :scroll="{ x: 1200, y: '100%' }"
          @change="onTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <a @click="openDetail(record)">{{ record.name }}</a>
              <div class="sub-text">{{ record.empNo }} · {{ genderText(record.gender) }}</div>
            </template>
            <template v-else-if="column.key === 'dept'">
              <div>{{ record.deptName || '-' }}</div>
              <div class="sub-text">{{ record.positionName || '未设置职位' }}</div>
            </template>
            <template v-else-if="column.key === 'empType'">
              <DictTag type="hr_emp_type" :value="record.empType" />
            </template>
            <template v-else-if="column.key === 'status'">
              <DictTag type="hr_emp_status" :value="record.status" />
              <div v-if="record.status === 'PROBATION'" class="sub-text">转正 {{ record.regularDate || '-' }}</div>
              <div v-else-if="record.status === 'LEFT'" class="sub-text">离职 {{ record.leaveDate }}</div>
            </template>
            <template v-else-if="column.key === 'account'">
              <span v-if="record.username">{{ record.username }}</span>
              <span v-else class="sub-text">未关联</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <span class="table-actions">
                <a-button v-perm="'hr:employee:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                  <template #icon><EditOutlined /></template>编辑
                </a-button>
                <a-dropdown v-if="record.status !== 'LEFT' && userStore.hasPerm('hr:employee:change')">
                  <a-button type="link" size="small">异动<DownOutlined /></a-button>
                  <template #overlay>
                    <a-menu @click="({ key }) => openChange(record, key)">
                      <a-menu-item v-for="t in changeTypes(record)" :key="t.value" :danger="t.value === 'LEAVE'">{{ t.label }}</a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
                <span v-perm="'hr:employee:remove'">
                  <a-popconfirm :title="`确定删除员工「${record.name}」？`" @confirm="remove(record.id)">
                    <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                  </a-popconfirm>
                </span>
              </span>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <!-- 入职 / 编辑 -->
    <FormDrawer page :drawer="drawer" create-title="办理入职" edit-title="编辑员工档案" width="min(760px, 100vw)">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-divider orientation="left" plain>基本信息</a-divider>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="8">
            <a-form-item label="工号" name="empNo"><a-input v-model:value="form.empNo" :maxlength="32" placeholder="如：E0101" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="姓名" name="name"><a-input v-model:value="form.name" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="性别" name="gender">
              <a-radio-group v-model:value="form.gender" :options="GENDERS" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="出生日期" name="birthDate">
              <a-date-picker v-model:value="form.birthDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="16">
            <a-form-item label="身份证号" name="idCard"><a-input v-model:value="form.idCard" :maxlength="18" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="手机号" name="phone"><a-input v-model:value="form.phone" :maxlength="32" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="邮箱" name="email"><a-input v-model:value="form.email" :maxlength="128" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="学历" name="education">
              <a-select v-model:value="form.education" allow-clear :options="toOptions(dict.hr_education)" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-divider orientation="left" plain>任职信息</a-divider>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="部门" name="deptId" :extra="isEdit ? '部门、职位通过「异动 - 调岗」调整' : undefined">
              <DeptSelect v-model:value="form.deptId" :disabled="isEdit" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="职位" name="positionId">
              <PositionSelect v-model:value="form.positionId" :disabled="isEdit" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="用工类型" name="empType">
              <a-select v-model:value="form.empType" :options="toOptions(dict.hr_emp_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="入职日期" name="entryDate">
              <a-date-picker v-model:value="form.entryDate" value-format="YYYY-MM-DD" class="full" :disabled="isEdit" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="参加工作日期" name="workStartDate" extra="法定年假按累计工作年限计算">
              <a-date-picker v-model:value="form.workStartDate" value-format="YYYY-MM-DD" class="full" placeholder="默认入职日期" />
            </a-form-item>
          </a-col>
          <a-col v-if="!isEdit" :xs="24" :sm="8">
            <a-form-item label="入职状态" name="status">
              <a-radio-group v-model:value="form.status">
                <a-radio-button value="PROBATION">试用期</a-radio-button>
                <a-radio-button value="REGULAR">直接转正</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col v-if="form.status === 'PROBATION'" :xs="24" :sm="8">
            <a-form-item label="计划转正日期" name="regularDate" extra="不填为入职日期 + 3 个月">
              <a-date-picker v-model:value="form.regularDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="班次" name="shiftId" extra="不选使用默认班次">
              <a-select v-model:value="form.shiftId" allow-clear :options="shiftOptions" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="登录账号" name="userId" extra="关联后可以使用员工自助">
              <IdentityUserSelect v-model:value="form.userId" :label="form.username" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-divider orientation="left" plain>其他</a-divider>
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="住址" name="address"><a-input v-model:value="form.address" :maxlength="255" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="紧急联系人" name="emergencyContact"><a-input v-model:value="form.emergencyContact" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="紧急联系电话" name="emergencyPhone"><a-input v-model:value="form.emergencyPhone" :maxlength="32" /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark"><a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <!-- 人事异动 -->
    <a-modal
      v-model:open="changeOpen"
      :title="`${changeLabel}：${changeTarget?.name || ''}`"
      :confirm-loading="changing"
      destroy-on-close
      @ok="submitChange"
    >
      <a-form ref="changeRef" :model="changeForm" layout="vertical">
        <a-form-item label="生效日期" name="changeDate" :rules="[{ required: true, message: '请选择生效日期' }]">
          <a-date-picker v-model:value="changeForm.changeDate" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <template v-if="changeForm.type === 'TRANSFER'">
          <a-form-item label="调入部门" name="deptId" :rules="[{ required: true, message: '请选择调入部门' }]">
            <DeptSelect v-model:value="changeForm.deptId" />
          </a-form-item>
          <a-form-item label="新职位" name="positionId">
            <PositionSelect v-model:value="changeForm.positionId" />
          </a-form-item>
        </template>
        <a-form-item v-if="changeForm.type === 'SALARY'" label="调整后基本工资" name="salary" :rules="[{ required: true, message: '请填写基本工资' }]">
          <a-input-number v-model:value="changeForm.salary" :min="0" :precision="2" addon-after="元/月" class="full" />
        </a-form-item>
        <a-form-item :label="changeForm.type === 'LEAVE' ? '离职原因' : '说明'" name="reason">
          <a-textarea v-model:value="changeForm.reason" :rows="2" :maxlength="255" />
        </a-form-item>
        <a-form-item v-if="changeForm.type === 'LEAVE'" label="工作交接人" name="handoverUserId" extra="负责的客户、线索、商机、服务工单、项目、未完成任务和审批待办移交给交接人；不选时客户退回公海">
          <IdentityUserSelect v-model:value="changeForm.handoverUserId" />
        </a-form-item>
        <a-alert v-if="changeForm.type === 'LEAVE'" type="warning" show-icon message="离职后员工不再参与考勤和工资核算，关联的登录账号会被停用并强制下线。" />
        <a-alert v-if="changeForm.type === 'TRANSFER'" type="info" show-icon message="调岗后关联的登录账号同步调整部门，部门负责人审批和数据权限按新部门生效。" />
      </a-form>
    </a-modal>

    <!-- 详情 -->
    <SubPage v-model:open="detailOpen" :title="detail?.name" width="min(760px, 100vw)" destroy-on-close>
      <template v-if="detail">
        <a-descriptions bordered size="small" :column="{ xs: 1, sm: 2 }">
          <a-descriptions-item label="工号">{{ detail.empNo }}</a-descriptions-item>
          <a-descriptions-item label="状态"><DictTag type="hr_emp_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item label="部门">{{ detail.deptName }}</a-descriptions-item>
          <a-descriptions-item label="职位">{{ detail.positionName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="性别">{{ genderText(detail.gender) }}</a-descriptions-item>
          <a-descriptions-item label="出生日期">{{ detail.birthDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="身份证号">{{ detail.idCard || '-' }}</a-descriptions-item>
          <a-descriptions-item label="学历"><DictTag type="hr_education" :value="detail.education" /></a-descriptions-item>
          <a-descriptions-item label="手机号">{{ detail.phone || '-' }}</a-descriptions-item>
          <a-descriptions-item label="邮箱">{{ detail.email || '-' }}</a-descriptions-item>
          <a-descriptions-item label="入职日期">{{ detail.entryDate }}</a-descriptions-item>
          <a-descriptions-item label="转正日期">{{ detail.regularDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="用工类型"><DictTag type="hr_emp_type" :value="detail.empType" /></a-descriptions-item>
          <a-descriptions-item label="班次">{{ detail.shiftName || '默认班次' }}</a-descriptions-item>
          <a-descriptions-item label="登录账号">{{ detail.username || '未关联' }}</a-descriptions-item>
          <a-descriptions-item label="紧急联系人">{{ detail.emergencyContact || '-' }} {{ detail.emergencyPhone }}</a-descriptions-item>
          <a-descriptions-item label="住址" :span="2">{{ detail.address || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.leaveDate" label="离职" :span="2">{{ detail.leaveDate }} {{ detail.leaveReason }}</a-descriptions-item>
        </a-descriptions>
        <a-tabs class="detail-tabs">
          <a-tab-pane key="changes" tab="异动记录">
            <a-timeline>
              <a-timeline-item v-for="c in changes" :key="c.id" :color="c.type === 'LEAVE' ? 'gray' : c.type === 'REGULAR' ? 'green' : 'blue'">
                <div>
                  <DictTag type="hr_change_type" :value="c.type" /> {{ c.changeDate }}
                  <DictTag v-if="c.status && c.status !== 'DONE'" type="sys_approve_status" :value="c.status" />
                </div>
                <div class="sub-text">{{ changeDesc(c) }}</div>
              </a-timeline-item>
            </a-timeline>
          </a-tab-pane>
          <a-tab-pane key="contract" tab="劳动合同">
            <a-table row-key="id" size="small" :columns="contractColumns" :data-source="contracts" :pagination="false" :scroll="{ x: 640 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'contractType'"><DictTag type="hr_contract_type" :value="record.contractType" /></template>
                <template v-else-if="column.key === 'term'">{{ record.startDate }} ~ {{ record.endDate || '无固定期限' }}</template>
                <template v-else-if="column.key === 'status'">
                  <DictTag type="hr_contract_status" :value="record.status" />
                  <div v-if="record.status === 'ACTIVE' && record.daysLeft != null && record.daysLeft <= 30" class="warn">
                    {{ record.daysLeft < 0 ? `已过期 ${-record.daysLeft} 天` : `${record.daysLeft} 天后到期` }}
                  </div>
                  <div v-else-if="record.terminateDate" class="sub-text">{{ record.terminateDate }} {{ record.terminateReason }}</div>
                </template>
              </template>
            </a-table>
            <div v-if="userStore.hasPerm('hr:contract:list')" class="sub-text contract-link">
              <router-link to="/hr/contract">到「劳动合同」中签订、续签或终止</router-link>
            </div>
          </a-tab-pane>
          <a-tab-pane key="training" tab="培训记录">
            <a-table row-key="id" size="small" :columns="trainingColumns" :data-source="trainings" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'attended'">
                  <a-tag :color="record.attended ? 'green' : 'default'" :bordered="false">{{ record.attended ? '出勤' : '未出勤' }}</a-tag>
                </template>
                <template v-else-if="column.key === 'passed'">
                  <span v-if="record.passed === 1" class="ok">通过</span>
                  <span v-else-if="record.passed === 0" class="sub-text">未通过</span>
                  <span v-else class="sub-text">-</span>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="attachment" tab="附件">
            <AttachmentPanel biz-type="hr_employee" :biz-id="detail.id" :editable="userStore.hasPerm('hr:employee:edit')" />
          </a-tab-pane>
          <a-tab-pane key="history" tab="变更记录">
            <ChangeLogPanel biz-type="hr_employee" :biz-id="detail.id" />
          </a-tab-pane>
        </a-tabs>
      </template>
    </SubPage>
    <ImportModal
      v-model:open="importOpen"
      title="员工档案"
      :columns="importColumns"
      :importer="importEmployee"
      :sample="{ empNo: 'E0001', name: '王五', gender: '男', deptId: '研发部', entryDate: '2026-01-05' }"
      @success="search"
    />
  </div>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, DownOutlined, EditOutlined, ImportOutlined, MenuFoldOutlined, MenuUnfoldOutlined, PlusOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import DictTag from '@/components/DictTag.vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import ChangeLogPanel from '@/components/ChangeLogPanel.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import ImportModal from '@/components/ImportModal.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DeptTree from '@/components/tree/DeptTree.vue'
import DeptSelect from '@/components/fin/DeptSelect.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import PositionSelect from '@/components/hr/PositionSelect.vue'
import { CHANGE_TYPES, GENDERS, genderText } from '@/components/hr/hr'
import {
  addEmployee,
  pageContract,
  changeEmployee,
  deleteEmployee,
  employeeChanges,
  employeeStats,
  employeeTrainings,
  getEmployee,
  importEmployee,
  listShift,
  pageEmployee,
  positionOptions,
  updateEmployee
} from '@/api/hr'
import { deptTree } from '@/api/system/dept'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'

const userStore = useUserStore()
const dict = useDict('hr_emp_status', 'hr_emp_type', 'hr_education', 'hr_change_type', 'sys_approve_status', 'hr_contract_type', 'hr_contract_status')
const showDept = ref(true)
const showSearch = ref(true)

const importOpen = ref(false)
/** 部门树展开为 名称 → ID */
const flatMap = (tree, map = new Map()) => {
  ;(tree || []).forEach((d) => {
    map.set(d.name, d.id)
    flatMap(d.children, map)
  })
  return map
}
const importColumns = [
  { title: '工号', key: 'empNo', required: true },
  { title: '姓名', key: 'name', required: true },
  { title: '性别', key: 'gender', lookup: async () => new Map([['男', 'M'], ['女', 'F'], ['M', 'M'], ['F', 'F']]) },
  { title: '出生日期', key: 'birthDate', type: 'date' },
  { title: '身份证号', key: 'idCard' },
  { title: '手机号', key: 'phone' },
  { title: '邮箱', key: 'email' },
  { title: '学历', key: 'education', dict: 'hr_education' },
  { title: '部门', key: 'deptId', required: true, lookup: async () => flatMap(await deptTree()) },
  { title: '职位', key: 'positionId', lookup: async () => new Map((await positionOptions()).map((p) => [p.name, p.id])) },
  { title: '用工类型', key: 'empType', dict: 'hr_emp_type' },
  { title: '状态', key: 'status', dict: 'hr_emp_status' },
  { title: '入职日期', key: 'entryDate', type: 'date', required: true },
  { title: '计划转正日期', key: 'regularDate', type: 'date' },
  { title: '参加工作日期', key: 'workStartDate', type: 'date' },
  { title: '住址', key: 'address' },
  { title: '紧急联系人', key: 'emergencyContact' },
  { title: '紧急联系电话', key: 'emergencyPhone' },
  { title: '备注', key: 'remark' }
]

const columns = [
  { title: '员工', key: 'name', width: 160, fixed: 'left' },
  { title: '部门 / 职位', key: 'dept', width: 180 },
  { title: '用工类型', key: 'empType', width: 100, dict: 'hr_emp_type' },
  { title: '状态', key: 'status', width: 130, dict: 'hr_emp_status' },
  { title: '入职日期', dataIndex: 'entryDate', width: 110 },
  { title: '手机号', dataIndex: 'phone', width: 130 },
  { title: '学历', dataIndex: 'education', width: 90, customRender: ({ text }) => dict.hr_education.find((d) => d.value === text)?.label || '-' },
  { title: '登录账号', key: 'account', width: 120 },
  { title: '操作', key: 'action', width: 190, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'hr-employee')

const emptyQuery = () => ({ keyword: undefined, status: undefined, positionId: undefined, empType: undefined, deptId: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageEmployee, query)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

// 部门树
const deptList = ref([])
const deptLoading = ref(false)
async function loadDept() {
  deptLoading.value = true
  try {
    deptList.value = await deptTree()
  } finally {
    deptLoading.value = false
  }
}

// 概况
const stats = ref({})
const statCards = computed(() => [
  { label: '在职员工', value: stats.value.active ?? '-' },
  { label: '试用期', value: stats.value.probation ?? '-', color: '#fa8c16' },
  { label: '本月入职', value: stats.value.joinedThisMonth ?? '-', color: '#52c41a' },
  { label: '本月离职', value: stats.value.leftThisMonth ?? '-', color: '#8c8c8c' }
])
async function loadStats() {
  stats.value = await employeeStats()
}

function reload() {
  load()
  loadStats()
}

const shiftOptions = ref([])
onMounted(async () => {
  load()
  loadDept()
  loadStats()
  const shifts = await listShift().catch(() => [])
  shiftOptions.value = shifts.map((s) => ({ value: s.id, label: `${s.name}（${s.startTime}-${s.endTime}）${s.isDefault ? ' 默认' : ''}` }))
})

// 入职 / 编辑
const FIELDS = ['empNo', 'name', 'gender', 'birthDate', 'idCard', 'phone', 'email', 'education', 'deptId', 'positionId', 'userId',
  'shiftId', 'empType', 'status', 'entryDate', 'regularDate', 'address', 'emergencyContact', 'emergencyPhone', 'remark', 'workStartDate']
const rules = {
  empNo: [
    { required: true, message: '请输入工号' },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: '只能包含字母、数字、下划线和短横线' }
  ],
  name: [{ required: true, whitespace: true, message: '请输入姓名' }],
  deptId: [{ required: true, message: '请选择部门' }],
  entryDate: [{ required: true, message: '请选择入职日期' }],
  idCard: [{ pattern: /^$|^\d{17}[\dXx]$/, message: '身份证号格式不正确' }],
  email: [{ type: 'email', message: '邮箱格式不正确' }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, ...Object.fromEntries(FIELDS.map((f) => [f, undefined])), gender: 'M', empType: 'FULL_TIME',
    status: 'PROBATION', entryDate: dayjs().format('YYYY-MM-DD'), username: undefined }),
  fetch: (record) => getEmployee(record.id),
  create: addEmployee,
  update: updateEmployee,
  toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: reload
})
const { formRef, form, isEdit } = drawer

async function remove(id) {
  await deleteEmployee(id)
  message.success('删除成功')
  reload()
}

// 人事异动
const changeOpen = ref(false)
const changing = ref(false)
const changeTarget = ref()
const changeRef = ref()
const changeForm = reactive({ type: undefined, changeDate: undefined, deptId: undefined, positionId: undefined, salary: undefined, reason: '', handoverUserId: undefined })
const changeLabel = computed(() => CHANGE_TYPES.find((t) => t.value === changeForm.type)?.label || '异动')
const changeTypes = (record) => CHANGE_TYPES.filter((t) => t.value !== 'REGULAR' || record.status === 'PROBATION')

function openChange(record, type) {
  changeTarget.value = record
  Object.assign(changeForm, { type, changeDate: dayjs().format('YYYY-MM-DD'), deptId: record.deptId, positionId: record.positionId,
    salary: undefined, reason: '', handoverUserId: undefined })
  changeOpen.value = true
}

async function submitChange() {
  await changeRef.value.validate()
  changing.value = true
  try {
    const submitted = await changeEmployee(changeTarget.value.id, { ...changeForm })
    message.success(submitted ? `${changeLabel.value}已提交审批，审批通过后生效` : `${changeLabel.value}已办理`)
    changeOpen.value = false
    reload()
  } finally {
    changing.value = false
  }
}

// 详情
const detailOpen = ref(false)
const detail = ref()
const changes = ref([])
const trainings = ref([])
const trainingColumns = [
  { title: '培训', dataIndex: 'trainingTitle' },
  { title: '日期', dataIndex: 'startDate', width: 110 },
  { title: '课时', dataIndex: 'hours', width: 70 },
  { title: '出勤', key: 'attended', width: 80 },
  { title: '成绩', dataIndex: 'score', width: 70 },
  { title: '结果', key: 'passed', width: 80 }
]

const contracts = ref([])
const contractColumns = [
  { title: '合同编号', dataIndex: 'contractNo', width: 150 },
  { title: '类型', key: 'contractType', width: 130 },
  { title: '期限', key: 'term', width: 200 },
  { title: '状态', key: 'status', width: 150 }
]

async function openDetail(record) {
  detailOpen.value = true
  detail.value = null
  contracts.value = []
  const [d, c, t, ct] = await Promise.all([
    getEmployee(record.id),
    employeeChanges(record.id),
    employeeTrainings(record.id),
    pageContract({ employeeId: record.id, current: 1, size: 100 }).catch(() => ({ records: [] }))
  ])
  detail.value = d
  changes.value = c
  trainings.value = t
  contracts.value = ct.records
}

function changeDesc(c) {
  switch (c.type) {
    case 'ENTRY':
      return `入职 ${c.toDeptName || ''} ${c.toPositionName || ''}`
    case 'TRANSFER':
      return `${c.fromDeptName || '-'} / ${c.fromPositionName || '-'} → ${c.toDeptName || '-'} / ${c.toPositionName || '-'}`
    case 'SALARY':
      return `基本工资 ${c.fromSalary ?? '-'} → ${c.toSalary ?? '-'}`
    default:
      return c.reason || ''
  }
}
</script>

<style scoped>
.stats {
  margin-bottom: 12px;
}
.stat-card :deep(.ant-card-body) {
  padding: 12px 16px;
}
.stat-label {
  color: var(--app-text-3);
  font-size: 12px;
}
.stat-value {
  font-size: 22px;
  font-weight: 600;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
.detail-tabs {
  margin-top: 16px;
}
.ok {
  color: #52c41a;
}
.warn {
  color: #fa8c16;
  font-size: 12px;
}
.contract-link {
  margin-top: 8px;
}
</style>
