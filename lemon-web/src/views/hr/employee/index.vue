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
          <a-form-item :label="$t('hr.employee.employee')" name="keyword">
            <a-input v-model:value="query.keyword" :placeholder="$t('hr.employee.keywordPh')" allow-clear />
          </a-form-item>
          <a-form-item :label="$t('common.field.status')" name="status">
            <a-select v-model:value="query.status" :placeholder="$t('hr.employee.statusPh')" allow-clear :options="toOptions(dict.hr_emp_status)" />
          </a-form-item>
          <a-form-item :label="$t('hr.employee.position')" name="positionId">
            <PositionSelect v-model:value="query.positionId" :placeholder="$t('common.placeholder.all')" />
          </a-form-item>
          <a-form-item :label="$t('hr.employee.empType')" name="empType">
            <a-select v-model:value="query.empType" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.hr_emp_type)" />
          </a-form-item>
        </SearchForm>
      </a-card>

      <a-card :bordered="false" class="page-card table-card">
        <TableToolbar
          :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="reload">
          <a-button v-perm="'hr:employee:add'" type="primary" @click="drawer.openCreate({ deptId: query.deptId })">
            <template #icon><PlusOutlined /></template>{{ $t('hr.employee.onboard') }}
          </a-button>
          <a-button v-perm="'hr:employee:add'" @click="importOpen = true">
            <template #icon><ImportOutlined /></template>{{ $t('common.action.import') }}
          </a-button>
          <template #extra>
            <a-tooltip :title="showDept ? $t('hr.employee.hideDept') : $t('hr.employee.showDept')">
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
              <div class="sub-text">{{ record.positionName || $t('hr.employee.noPosition') }}</div>
            </template>
            <template v-else-if="column.key === 'empType'">
              <DictTag type="hr_emp_type" :value="record.empType" />
            </template>
            <template v-else-if="column.key === 'status'">
              <DictTag type="hr_emp_status" :value="record.status" />
              <div v-if="record.status === EmployeeStatus.PROBATION" class="sub-text">{{ $t('hr.employee.regularOn', { date: record.regularDate || '-' }) }}</div>
              <div v-else-if="record.status === EmployeeStatus.LEFT" class="sub-text">{{ $t('hr.employee.leftOn', { date: record.leaveDate }) }}</div>
            </template>
            <template v-else-if="column.key === 'account'">
              <span v-if="record.username">{{ record.username }}</span>
              <span v-else class="sub-text">{{ $t('hr.employee.unlinked') }}</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <span class="table-actions">
                <a-button v-perm="'hr:employee:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                  <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
                </a-button>
                <a-dropdown v-if="record.status !== EmployeeStatus.LEFT && userStore.hasPerm('hr:employee:change')">
                  <a-button type="link" size="small">{{ $t('hr.employee.change') }}<DownOutlined /></a-button>
                  <template #overlay>
                    <a-menu @click="({ key }) => openChange(record, key)">
                      <a-menu-item v-for="t in changeTypes(record)" :key="t.value" :danger="t.value === ChangeType.LEAVE">{{ t.label }}</a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
                <span v-perm="'hr:employee:remove'">
                  <a-popconfirm :title="$t('hr.employee.confirmDelete', { name: record.name })" @confirm="remove(record.id)">
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
    <FormDrawer page :drawer="drawer" :create-title="$t('hr.employee.onboard')" :edit-title="$t('hr.employee.editTitle')" width="min(760px, 100vw)">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-divider orientation="left" plain>{{ $t('hr.employee.basicInfo') }}</a-divider>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('hr.employee.empNo')" name="empNo"><a-input v-model:value="form.empNo" :maxlength="32" :placeholder="$t('hr.employee.empNoPh')" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('hr.employee.name')" name="name"><a-input v-model:value="form.name" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('hr.employee.gender')" name="gender">
              <a-radio-group v-model:value="form.gender" :options="GENDERS" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('hr.employee.birthDate')" name="birthDate">
              <a-date-picker v-model:value="form.birthDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="16">
            <a-form-item :label="$t('hr.employee.idCard')" name="idCard"><a-input v-model:value="form.idCard" :maxlength="18" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('common.field.phone')" name="phone"><a-input v-model:value="form.phone" :maxlength="32" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('common.field.email')" name="email"><a-input v-model:value="form.email" :maxlength="128" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('hr.employee.education')" name="education">
              <a-select v-model:value="form.education" allow-clear :options="toOptions(dict.hr_education)" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-divider orientation="left" plain>{{ $t('hr.employee.jobInfo') }}</a-divider>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.dept')" name="deptId" :extra="isEdit ? $t('hr.employee.deptExtra') : undefined">
              <DeptSelect v-model:value="form.deptId" :disabled="isEdit" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.employee.position')" name="positionId">
              <PositionSelect v-model:value="form.positionId" :disabled="isEdit" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('hr.employee.empType')" name="empType">
              <a-select v-model:value="form.empType" :options="toOptions(dict.hr_emp_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('hr.employee.entryDate')" name="entryDate">
              <a-date-picker v-model:value="form.entryDate" value-format="YYYY-MM-DD" class="full" :disabled="isEdit" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('hr.employee.workStartDate')" name="workStartDate" :extra="$t('hr.employee.workStartExtra')">
              <a-date-picker v-model:value="form.workStartDate" value-format="YYYY-MM-DD" class="full" :placeholder="$t('hr.employee.workStartPh')" />
            </a-form-item>
          </a-col>
          <a-col v-if="!isEdit" :xs="24" :sm="8">
            <a-form-item :label="$t('hr.employee.entryStatus')" name="status">
              <a-radio-group v-model:value="form.status">
                <a-radio-button value="PROBATION">{{ $t('hr.employee.probation') }}</a-radio-button>
                <a-radio-button value="REGULAR">{{ $t('hr.employee.directRegular') }}</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col v-if="form.status === EmployeeStatus.PROBATION" :xs="24" :sm="8">
            <a-form-item :label="$t('hr.employee.regularDate')" name="regularDate" :extra="$t('hr.employee.regularExtra')">
              <a-date-picker v-model:value="form.regularDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('hr.employee.shift')" name="shiftId" :extra="$t('hr.employee.shiftExtra')">
              <a-select v-model:value="form.shiftId" allow-clear :options="shiftOptions" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('hr.employee.account')" name="userId" :extra="$t('hr.employee.userExtra')">
              <IdentityUserSelect v-model:value="form.userId" :label="form.username" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-divider orientation="left" plain>{{ $t('hr.employee.other') }}</a-divider>
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item :label="$t('hr.employee.address')" name="address"><a-input v-model:value="form.address" :maxlength="255" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.employee.emergencyContact')" name="emergencyContact"><a-input v-model:value="form.emergencyContact" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.employee.emergencyPhone')" name="emergencyPhone"><a-input v-model:value="form.emergencyPhone" :maxlength="32" /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark"><a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <!-- 人事异动 -->
    <a-modal
      v-model:open="changeOpen"
      :title="$t('hr.employee.changeTitle', { type: changeLabel, name: changeTarget?.name || '' })"
      :confirm-loading="changing"
      destroy-on-close
      @ok="submitChange"
    >
      <a-form ref="changeRef" :model="changeForm" layout="vertical">
        <a-form-item :label="$t('hr.employee.effectiveDate')" name="changeDate" :rules="[{ required: true, message: $t('hr.employee.effectiveDateRequired') }]">
          <a-date-picker v-model:value="changeForm.changeDate" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <template v-if="changeForm.type === ChangeType.TRANSFER">
          <a-form-item :label="$t('hr.employee.toDept')" name="deptId" :rules="[{ required: true, message: $t('hr.employee.toDeptRequired') }]">
            <DeptSelect v-model:value="changeForm.deptId" />
          </a-form-item>
          <a-form-item :label="$t('hr.employee.newPosition')" name="positionId">
            <PositionSelect v-model:value="changeForm.positionId" />
          </a-form-item>
        </template>
        <a-form-item v-if="changeForm.type === ChangeType.SALARY" :label="$t('hr.employee.newSalary')" name="salary" :rules="[{ required: true, message: $t('hr.employee.salaryRequired') }]">
          <a-input-number v-model:value="changeForm.salary" :min="0" :precision="2" :addon-after="$t('hr.employee.perMonth')" class="full" />
        </a-form-item>
        <a-form-item :label="changeForm.type === ChangeType.LEAVE ? $t('hr.employee.leaveReason') : $t('common.field.description')" name="reason">
          <a-textarea v-model:value="changeForm.reason" :rows="2" :maxlength="255" />
        </a-form-item>
        <a-form-item v-if="changeForm.type === ChangeType.LEAVE" :label="$t('hr.employee.handover')" name="handoverUserId" :extra="$t('hr.employee.handoverExtra')">
          <IdentityUserSelect v-model:value="changeForm.handoverUserId" />
        </a-form-item>
        <a-alert v-if="changeForm.type === ChangeType.LEAVE" type="warning" show-icon :message="$t('hr.employee.leaveTip')" />
        <a-alert v-if="changeForm.type === ChangeType.TRANSFER" type="info" show-icon :message="$t('hr.employee.transferTip')" />
      </a-form>
    </a-modal>

    <!-- 详情 -->
    <SubPage v-model:open="detailOpen" :title="detail?.name" width="min(760px, 100vw)" destroy-on-close>
      <template v-if="detail">
        <a-descriptions bordered size="small" :column="{ xs: 1, sm: 2 }">
          <a-descriptions-item :label="$t('hr.employee.empNo')">{{ detail.empNo }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="hr_emp_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.dept')">{{ detail.deptName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('hr.employee.position')">{{ detail.positionName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('hr.employee.gender')">{{ genderText(detail.gender) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('hr.employee.birthDate')">{{ detail.birthDate || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('hr.employee.idCard')">{{ detail.idCard || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('hr.employee.education')"><DictTag type="hr_education" :value="detail.education" /></a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.phone')">{{ detail.phone || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.email')">{{ detail.email || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('hr.employee.entryDate')">{{ detail.entryDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('hr.employee.regularDateActual')">{{ detail.regularDate || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('hr.employee.empType')"><DictTag type="hr_emp_type" :value="detail.empType" /></a-descriptions-item>
          <a-descriptions-item :label="$t('hr.employee.shift')">{{ detail.shiftName || $t('hr.employee.defaultShift') }}</a-descriptions-item>
          <a-descriptions-item :label="$t('hr.employee.account')">{{ detail.username || $t('hr.employee.unlinked') }}</a-descriptions-item>
          <a-descriptions-item :label="$t('hr.employee.emergencyContact')">{{ detail.emergencyContact || '-' }} {{ detail.emergencyPhone }}</a-descriptions-item>
          <a-descriptions-item :label="$t('hr.employee.address')" :span="2">{{ detail.address || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.leaveDate" :label="$t('hr.employee.left')" :span="2">{{ detail.leaveDate }} {{ detail.leaveReason }}</a-descriptions-item>
        </a-descriptions>
        <a-tabs class="detail-tabs">
          <a-tab-pane key="changes" :tab="$t('hr.employee.changeHistory')">
            <a-timeline>
              <a-timeline-item v-for="c in changes" :key="c.id" :color="c.type === ChangeType.LEAVE ? 'gray' : c.type === ChangeType.REGULAR ? 'green' : 'blue'">
                <div>
                  <DictTag type="hr_change_type" :value="c.type" /> {{ c.changeDate }}
                  <DictTag v-if="c.status && c.status !== ApproveStatus.DONE" type="sys_approve_status" :value="c.status" />
                </div>
                <div class="sub-text">{{ changeDesc(c) }}</div>
              </a-timeline-item>
            </a-timeline>
          </a-tab-pane>
          <a-tab-pane key="contract" :tab="$t('hr.employee.contracts')">
            <a-table row-key="id" size="small" :columns="contractColumns" :data-source="contracts" :pagination="false" :scroll="{ x: 640 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'contractType'"><DictTag type="hr_contract_type" :value="record.contractType" /></template>
                <template v-else-if="column.key === 'term'">{{ record.startDate }} ~ {{ record.endDate || $t('hr.contract.openEnded') }}</template>
                <template v-else-if="column.key === 'status'">
                  <DictTag type="hr_contract_status" :value="record.status" />
                  <div v-if="record.status === LaborContractStatus.ACTIVE && record.daysLeft != null && record.daysLeft <= 30" class="warn">
                    {{ record.daysLeft < 0 ? $t('hr.contract.overdueDays', { n: -record.daysLeft }) : $t('hr.employee.expiresIn', { n: record.daysLeft }) }}
                  </div>
                  <div v-else-if="record.terminateDate" class="sub-text">{{ record.terminateDate }} {{ record.terminateReason }}</div>
                </template>
              </template>
            </a-table>
            <div v-if="userStore.hasPerm('hr:contract:list')" class="sub-text contract-link">
              <router-link to="/hr/contract">{{ $t('hr.employee.contractLink') }}</router-link>
            </div>
          </a-tab-pane>
          <a-tab-pane key="training" :tab="$t('hr.employee.trainings')">
            <a-table row-key="id" size="small" :columns="trainingColumns" :data-source="trainings" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'attended'">
                  <a-tag :color="record.attended ? 'green' : 'default'" :bordered="false">{{ record.attended ? $t('hr.employee.attended') : $t('hr.employee.absent') }}</a-tag>
                </template>
                <template v-else-if="column.key === 'passed'">
                  <span v-if="record.passed === 1" class="ok">{{ $t('hr.employee.passed') }}</span>
                  <span v-else-if="record.passed === 0" class="sub-text">{{ $t('hr.employee.failed') }}</span>
                  <span v-else class="sub-text">-</span>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="attachment" :tab="$t('hr.employee.attachments')">
            <AttachmentPanel biz-type="hr_employee" :biz-id="detail.id" :editable="userStore.hasPerm('hr:employee:edit')" />
          </a-tab-pane>
          <a-tab-pane key="history" :tab="$t('hr.employee.history')">
            <ChangeLogPanel biz-type="hr_employee" :biz-id="detail.id" />
          </a-tab-pane>
        </a-tabs>
      </template>
    </SubPage>
    <ImportModal
      v-model:open="importOpen"
      :title="$t('hr.employee.importTitle')"
      :columns="importColumns"
      :importer="importEmployee"
      :sample="{ empNo: 'E0001', name: $t('hr.employee.sampleName'), gender: $t('hr.common.male'), deptId: $t('hr.employee.sampleDept'), entryDate: '2026-01-05' }"
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
import { t } from '@/locales'
import { useAction } from '@/composables/useAction'

import { ApproveStatus, ChangeType, EmployeeStatus, LaborContractStatus } from '@/constants/enums'
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
  { title: t('hr.employee.empNo'), key: 'empNo', required: true },
  { title: t('hr.employee.name'), key: 'name', required: true },
  { title: t('hr.employee.gender'), key: 'gender', lookup: async () => new Map([['男', 'M'], ['女', 'F'], ['M', 'M'], ['F', 'F'], [t('hr.common.male'), 'M'], [t('hr.common.female'), 'F']]) }, // i18n-ignore 兼容中文模板
  { title: t('hr.employee.birthDate'), key: 'birthDate', type: 'date' },
  { title: t('hr.employee.idCard'), key: 'idCard' },
  { title: t('common.field.phone'), key: 'phone' },
  { title: t('common.field.email'), key: 'email' },
  { title: t('hr.employee.education'), key: 'education', dict: 'hr_education' },
  { title: t('common.field.dept'), key: 'deptId', required: true, lookup: async () => flatMap(await deptTree()) },
  { title: t('hr.employee.position'), key: 'positionId', lookup: async () => new Map((await positionOptions()).map((p) => [p.name, p.id])) },
  { title: t('hr.employee.empType'), key: 'empType', dict: 'hr_emp_type' },
  { title: t('common.field.status'), key: 'status', dict: 'hr_emp_status' },
  { title: t('hr.employee.entryDate'), key: 'entryDate', type: 'date', required: true },
  { title: t('hr.employee.regularDate'), key: 'regularDate', type: 'date' },
  { title: t('hr.employee.workStartDate'), key: 'workStartDate', type: 'date' },
  { title: t('hr.employee.address'), key: 'address' },
  { title: t('hr.employee.emergencyContact'), key: 'emergencyContact' },
  { title: t('hr.employee.emergencyPhone'), key: 'emergencyPhone' },
  { title: t('common.field.remark'), key: 'remark' }
]

const columns = [
  { title: t('hr.employee.employee'), key: 'name', width: 160, fixed: 'left' },
  { title: t('hr.employee.deptPosition'), key: 'dept', width: 180 },
  { title: t('hr.employee.empType'), key: 'empType', width: 130, dict: 'hr_emp_type' },
  { title: t('common.field.status'), key: 'status', width: 130, dict: 'hr_emp_status' },
  { title: t('hr.employee.entryDate'), dataIndex: 'entryDate', width: 110 },
  { title: t('common.field.phone'), dataIndex: 'phone', width: 130 },
  { title: t('hr.employee.education'), dataIndex: 'education', width: 100, customRender: ({ text }) => dict.hr_education.find((d) => d.value === text)?.label || '-' },
  { title: t('hr.employee.account'), key: 'account', width: 130 },
  { title: t('common.action.operation'), key: 'action', width: 210, fixed: 'right' }
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
  { label: t('hr.employee.statActive'), value: stats.value.active ?? '-' },
  { label: t('hr.employee.probation'), value: stats.value.probation ?? '-', color: '#fa8c16' },
  { label: t('hr.employee.statJoined'), value: stats.value.joinedThisMonth ?? '-', color: '#52c41a' },
  { label: t('hr.employee.statLeft'), value: stats.value.leftThisMonth ?? '-', color: '#8c8c8c' }
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
  shiftOptions.value = shifts.map((s) => ({ value: s.id, label: t(s.isDefault ? 'hr.employee.shiftOptionDefault' : 'hr.employee.shiftOption', { name: s.name, start: s.startTime, end: s.endTime }) }))
})

// 入职 / 编辑
const FIELDS = ['empNo', 'name', 'gender', 'birthDate', 'idCard', 'phone', 'email', 'education', 'deptId', 'positionId', 'userId',
  'shiftId', 'empType', 'status', 'entryDate', 'regularDate', 'address', 'emergencyContact', 'emergencyPhone', 'remark', 'workStartDate']
const rules = {
  empNo: [
    { required: true, message: t('hr.employee.empNoRequired') },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: t('hr.position.codePattern') }
  ],
  name: [{ required: true, whitespace: true, message: t('hr.employee.nameRequired') }],
  deptId: [{ required: true, message: t('hr.employee.deptRequired') }],
  entryDate: [{ required: true, message: t('hr.employee.entryDateRequired') }],
  idCard: [{ pattern: /^$|^\d{17}[\dXx]$/, message: t('hr.employee.idCardInvalid') }],
  email: [{ type: 'email', message: t('common.rule.email') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, ...Object.fromEntries(FIELDS.map((f) => [f, undefined])), gender: 'M', empType: 'FULL_TIME',
    status: EmployeeStatus.PROBATION, entryDate: dayjs().format('YYYY-MM-DD'), username: undefined }),
  fetch: (record) => getEmployee(record.id),
  create: addEmployee,
  update: updateEmployee,
  toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: reload
})
const { formRef, form, isEdit } = drawer

const remove = useAction(deleteEmployee, { successKey: 'common.msg.deleteSuccess', reload: () => reload() })

// 人事异动
const changeOpen = ref(false)
const changing = ref(false)
const changeTarget = ref()
const changeRef = ref()
const changeForm = reactive({ type: undefined, changeDate: undefined, deptId: undefined, positionId: undefined, salary: undefined, reason: '', handoverUserId: undefined })
const changeLabel = computed(() => CHANGE_TYPES.find((c) => c.value === changeForm.type)?.label || t('hr.employee.change'))
const changeTypes = (record) => CHANGE_TYPES.filter((c) => c.value !== ChangeType.REGULAR || record.status === EmployeeStatus.PROBATION)

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
    message.success(submitted ? t('hr.employee.changeSubmitted', { type: changeLabel.value }) : t('hr.employee.changeDone', { type: changeLabel.value }))
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
  { title: t('hr.employee.training'), dataIndex: 'trainingTitle' },
  { title: t('common.field.date'), dataIndex: 'startDate', width: 110 },
  { title: t('hr.employee.hours'), dataIndex: 'hours', width: 80 },
  { title: t('hr.employee.attendance'), key: 'attended', width: 110 },
  { title: t('hr.employee.score'), dataIndex: 'score', width: 80 },
  { title: t('hr.employee.result'), key: 'passed', width: 80 }
]

const contracts = ref([])
const contractColumns = [
  { title: t('hr.contract.contractNo'), dataIndex: 'contractNo', width: 150 },
  { title: t('common.field.type'), key: 'contractType', width: 130 },
  { title: t('hr.contract.term'), key: 'term', width: 200 },
  { title: t('common.field.status'), key: 'status', width: 150 }
]

async function openDetail(record) {
  detailOpen.value = true
  detail.value = null
  contracts.value = []
  const [d, c, tr, ct] = await Promise.all([
    getEmployee(record.id),
    employeeChanges(record.id),
    employeeTrainings(record.id),
    pageContract({ employeeId: record.id, current: 1, size: 100 }).catch(() => ({ records: [] }))
  ])
  detail.value = d
  changes.value = c
  trainings.value = tr
  contracts.value = ct.records
}

function changeDesc(c) {
  switch (c.type) {
    case ChangeType.ENTRY:
      return t('hr.employee.entryDesc', { dept: c.toDeptName || '', position: c.toPositionName || '' })
    case ChangeType.TRANSFER:
      return `${c.fromDeptName || '-'} / ${c.fromPositionName || '-'} → ${c.toDeptName || '-'} / ${c.toPositionName || '-'}`
    case ChangeType.SALARY:
      return t('hr.employee.salaryDesc', { from: c.fromSalary ?? '-', to: c.toSalary ?? '-' })
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
