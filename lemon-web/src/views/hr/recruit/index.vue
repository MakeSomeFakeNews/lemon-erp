<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <div class="head">
        <a-segmented v-model:value="tab" :options="[{ label: '招聘需求', value: 'recruit' }, { label: '应聘者', value: 'candidate' }]" />
        <template v-if="tab === 'recruit'">
          <a-input v-model:value="rq.keyword" placeholder="单号或岗位" allow-clear class="w200" @press-enter="rSearch" />
          <a-select v-model:value="rq.status" placeholder="全部状态" allow-clear class="w140" :options="toOptions(dict.hr_recruit_status)" @change="rSearch" />
          <a-button type="primary" @click="rSearch">查询</a-button>
        </template>
        <template v-else>
          <a-select
            v-model:value="cq.recruitId"
            placeholder="全部招聘需求"
            allow-clear
            show-search
            option-filter-prop="label"
            class="w240"
            :options="recruitOptions"
            @change="cSearch"
          />
          <a-input v-model:value="cq.keyword" placeholder="姓名、手机号或院校" allow-clear class="w200" @press-enter="cSearch" />
          <a-button type="primary" @click="cSearch">查询</a-button>
        </template>
      </div>
      <div v-if="tab === 'candidate'" class="stages">
        <a-tag
          v-for="s in [{ value: undefined, label: '全部' }, ...dict.hr_candidate_stage]"
          :key="s.value || 'all'"
          :color="cq.stage === s.value ? 'blue' : 'default'"
          class="stage-tag"
          @click="setStage(s.value)"
        >
          {{ s.label }}
        </a-tag>
      </div>
    </a-card>

    <!-- 招聘需求 -->
    <a-card v-show="tab === 'recruit'" :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="rSize" :columns="recruitColumns" @refresh="rLoad">
        <a-button v-perm="'hr:recruit:add'" type="primary" @click="recruitDrawer.openCreate()">
          <template #icon><PlusOutlined /></template>新增招聘需求
        </a-button>
      </TableToolbar>
      <a-table
        row-key="id"
        :size="rSize"
        :columns="recruitColumns"
        :data-source="rList"
        :loading="rLoading"
        :pagination="rPagination"
        :scroll="{ x: 1200, y: '100%' }"
        @change="rChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <div>{{ record.title }}</div>
            <div class="sub-text">{{ record.recruitNo }}</div>
          </template>
          <template v-else-if="column.key === 'dept'">
            <div>{{ record.deptName }}</div>
            <div class="sub-text">{{ record.positionName || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'progress'">
            <a-progress :percent="Math.round((record.hiredCount / record.headcount) * 100)" size="small" :format="() => `${record.hiredCount}/${record.headcount}`" />
          </template>
          <template v-else-if="column.key === 'candidates'">
            <a @click="viewCandidates(record)">{{ record.candidateCount }} 人</a>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="hr_recruit_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <template v-if="record.status === 'DRAFT'">
                <a-button v-perm="'hr:recruit:approve'" type="link" size="small" @click="approve(record)">审核</a-button>
                <a-button v-perm="'hr:recruit:edit'" type="link" size="small" @click="recruitDrawer.openEdit(record)">编辑</a-button>
                <span v-perm="'hr:recruit:remove'">
                  <a-popconfirm :title="`确定删除「${record.title}」？`" @confirm="removeRecruit(record.id)">
                    <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                  </a-popconfirm>
                </span>
              </template>
              <span v-else-if="record.status === 'APPROVING'" v-perm="'hr:recruit:approve'">
                <a-popconfirm title="撤回审批？" @confirm="withdraw(record.id)">
                  <a-button type="link" size="small">撤回</a-button>
                </a-popconfirm>
              </span>
              <template v-else-if="record.status === 'OPEN'">
                <a-button v-perm="'hr:recruit:candidate'" type="link" size="small" @click="candidateDrawer.openCreate({ recruitId: record.id })">
                  录入简历
                </a-button>
                <span v-perm="'hr:recruit:edit'">
                  <a-popconfirm title="关闭后不再录入应聘者，确定关闭？" @confirm="close(record.id)">
                    <a-button type="link" size="small">关闭</a-button>
                  </a-popconfirm>
                </span>
              </template>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 应聘者 -->
    <a-card v-show="tab === 'candidate'" :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="cSize" :columns="candidateColumns" @refresh="cLoad">
        <a-button v-perm="'hr:recruit:candidate'" type="primary" @click="candidateDrawer.openCreate({ recruitId: cq.recruitId })">
          <template #icon><PlusOutlined /></template>录入简历
        </a-button>
      </TableToolbar>
      <a-table
        row-key="id"
        :size="cSize"
        :columns="candidateColumns"
        :data-source="cList"
        :loading="cLoading"
        :pagination="cPagination"
        :scroll="{ x: 1250, y: '100%' }"
        @change="cChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="openInterviews(record)">{{ record.name }}</a>
            <div class="sub-text">{{ genderText(record.gender) }} · {{ record.phone || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'recruit'">
            <div>{{ record.recruitTitle }}</div>
            <div class="sub-text">{{ record.recruitNo }}</div>
          </template>
          <template v-else-if="column.key === 'education'">
            <div>{{ dictLabel(dict.hr_education, record.education) }} {{ record.school || '' }}</div>
            <div class="sub-text">{{ record.workYears != null ? `${record.workYears} 年经验` : '' }}</div>
          </template>
          <template v-else-if="column.key === 'source'">
            <DictTag type="hr_candidate_source" :value="record.source" />
          </template>
          <template v-else-if="column.key === 'expectedSalary'">
            {{ record.expectedSalary ? formatMoney(record.expectedSalary) : '-' }}
          </template>
          <template v-else-if="column.key === 'stage'">
            <DictTag type="hr_candidate_stage" :value="record.stage" />
            <div v-if="record.empNo" class="sub-text">工号 {{ record.empNo }}</div>
          </template>
          <template v-else-if="column.key === 'interview'">
            <span v-if="record.interviewCount">
              {{ record.interviewCount }} 轮 <DictTag type="hr_interview_result" :value="record.lastResult" />
            </span>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openInterviews(record)">面试</a-button>
              <template v-if="['NEW', 'INTERVIEW', 'OFFER'].includes(record.stage)">
                <span v-if="record.stage === 'INTERVIEW'" v-perm="'hr:recruit:candidate'">
                  <a-popconfirm title="确定向该应聘者发出 Offer？" @confirm="offer(record.id)">
                    <a-button type="link" size="small">发 Offer</a-button>
                  </a-popconfirm>
                </span>
                <a-button v-if="record.stage === 'OFFER'" v-perm="'hr:recruit:hire'" type="link" size="small" @click="openHire(record)">录用</a-button>
                <a-dropdown>
                  <a-button type="link" size="small">更多<DownOutlined /></a-button>
                  <template #overlay>
                    <a-menu @click="({ key }) => onMore(key, record)">
                      <a-menu-item key="edit" :disabled="!userStore.hasPerm('hr:recruit:candidate')">编辑</a-menu-item>
                      <a-menu-item key="reject" danger :disabled="!userStore.hasPerm('hr:recruit:candidate')">淘汰</a-menu-item>
                      <a-menu-item key="delete" danger :disabled="!userStore.hasPerm('hr:recruit:candidate')">删除</a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </template>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 招聘需求表单 -->
    <FormDrawer :drawer="recruitDrawer" create-title="新增招聘需求" edit-title="编辑招聘需求">
      <a-form ref="recruitFormRef" :model="recruitForm" :rules="recruitRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="岗位名称" name="title"><a-input v-model:value="recruitForm.title" :maxlength="128" placeholder="如：嵌入式软件工程师" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="用人部门" name="deptId"><DeptSelect v-model:value="recruitForm.deptId" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="职位" name="positionId"><PositionSelect v-model:value="recruitForm.positionId" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="招聘人数" name="headcount"><a-input-number v-model:value="recruitForm.headcount" :min="1" :max="999" class="full" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="薪资范围" name="salaryRange"><a-input v-model:value="recruitForm.salaryRange" :maxlength="64" placeholder="如：15-25K" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="期望到岗" name="deadline"><a-date-picker v-model:value="recruitForm.deadline" value-format="YYYY-MM-DD" class="full" /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="招聘原因" name="reason"><a-input v-model:value="recruitForm.reason" :maxlength="255" placeholder="新增编制 / 离职补缺 / 业务扩展" /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="任职要求" name="requirement"><a-textarea v-model:value="recruitForm.requirement" :rows="4" :maxlength="1000" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <!-- 应聘者表单 -->
    <FormDrawer :drawer="candidateDrawer" create-title="录入简历" edit-title="编辑应聘者">
      <a-form ref="candidateFormRef" :model="candidateForm" :rules="candidateRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="招聘需求" name="recruitId">
              <a-select v-model:value="candidateForm.recruitId" show-search option-filter-prop="label" :options="openOptions" :disabled="candidateDrawer.isEdit.value" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="姓名" name="name"><a-input v-model:value="candidateForm.name" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="性别" name="gender"><a-radio-group v-model:value="candidateForm.gender" :options="GENDERS" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="手机号" name="phone"><a-input v-model:value="candidateForm.phone" :maxlength="32" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="邮箱" name="email"><a-input v-model:value="candidateForm.email" :maxlength="128" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="学历" name="education"><a-select v-model:value="candidateForm.education" allow-clear :options="toOptions(dict.hr_education)" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="16">
            <a-form-item label="毕业院校" name="school"><a-input v-model:value="candidateForm.school" :maxlength="128" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="工作年限" name="workYears"><a-input-number v-model:value="candidateForm.workYears" :min="0" :max="60" addon-after="年" class="full" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="简历来源" name="source"><a-select v-model:value="candidateForm.source" allow-clear :options="toOptions(dict.hr_candidate_source)" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="期望月薪" name="expectedSalary"><a-input-number v-model:value="candidateForm.expectedSalary" :min="0" :precision="0" class="full" /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="简历摘要" name="resume"><a-textarea v-model:value="candidateForm.resume" :rows="5" :maxlength="2000" placeholder="工作经历、项目经验、技能" /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark"><a-input v-model:value="candidateForm.remark" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <!-- 面试 -->
    <a-drawer v-model:open="interviewOpen" :title="`面试记录 - ${current?.name || ''}`" width="min(720px, 100vw)" destroy-on-close>
      <template v-if="current">
        <a-descriptions size="small" :column="2" bordered>
          <a-descriptions-item label="应聘岗位">{{ current.recruitTitle }}</a-descriptions-item>
          <a-descriptions-item label="阶段"><DictTag type="hr_candidate_stage" :value="current.stage" /></a-descriptions-item>
          <a-descriptions-item label="学历">{{ dictLabel(dict.hr_education, current.education) }} {{ current.school || '' }}</a-descriptions-item>
          <a-descriptions-item label="期望月薪">{{ current.expectedSalary ? formatMoney(current.expectedSalary) : '-' }}</a-descriptions-item>
          <a-descriptions-item label="简历摘要" :span="2"><div class="pre">{{ resumeText || '-' }}</div></a-descriptions-item>
        </a-descriptions>
        <div class="section-head"><span>简历附件</span></div>
        <AttachmentPanel biz-type="hr_candidate" :biz-id="current.id" :editable="userStore.hasPerm('hr:recruit:candidate')" />
        <div class="section-head">
          <span>面试安排</span>
          <a-button
            v-if="['NEW', 'INTERVIEW'].includes(current.stage)"
            v-perm="'hr:recruit:candidate'"
            size="small"
            type="primary"
            @click="openInterviewForm()"
          >
            安排面试
          </a-button>
        </div>
        <a-empty v-if="!interviews.length" description="还没有面试记录" />
        <a-timeline v-else>
          <a-timeline-item v-for="i in interviews" :key="i.id" :color="i.result === 'PASS' ? 'green' : i.result === 'FAIL' ? 'red' : 'blue'">
            <div class="iv-head">
              第 {{ i.round }} 轮 · {{ i.interviewTime }} · {{ methodText(i.method) }} · 面试官 {{ i.interviewerName || '-' }}
              <DictTag type="hr_interview_result" :value="i.result" />
              <span v-if="i.score != null" class="sub-text">{{ i.score }} 分</span>
              <a v-if="['NEW', 'INTERVIEW'].includes(current.stage) && userStore.hasPerm('hr:recruit:candidate')" @click="openInterviewForm(i)">填写结果</a>
            </div>
            <div v-if="i.feedback" class="sub-text">{{ i.feedback }}</div>
          </a-timeline-item>
        </a-timeline>
      </template>
    </a-drawer>

    <a-modal v-model:open="ivOpen" :title="ivForm.id ? '面试结果' : '安排面试'" :confirm-loading="ivSaving" destroy-on-close @ok="saveInterview">
      <a-form ref="ivRef" :model="ivForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="面试时间" name="interviewTime" :rules="[{ required: true, message: '请选择面试时间' }]">
              <a-date-picker v-model:value="ivForm.interviewTime" show-time value-format="YYYY-MM-DD HH:mm:ss" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="方式" name="method"><a-select v-model:value="ivForm.method" :options="INTERVIEW_METHODS" /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="面试官" name="interviewerId"><IdentityUserSelect v-model:value="ivForm.interviewerId" :label="ivForm.interviewerName" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="结果" name="result">
              <a-radio-group v-model:value="ivForm.result" :options="toOptions(dict.hr_interview_result)" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="评分" name="score"><a-input-number v-model:value="ivForm.score" :min="0" :max="100" addon-after="分" class="full" /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="面试评价" name="feedback"><a-textarea v-model:value="ivForm.feedback" :rows="3" :maxlength="1000" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 录用 -->
    <a-modal v-model:open="hireOpen" :title="`录用入职：${current?.name || ''}`" :confirm-loading="hiring" destroy-on-close @ok="submitHire">
      <a-alert type="info" show-icon message="录用后生成试用期员工档案（部门为用人部门），招满后招聘需求自动关闭。" class="mb" />
      <a-form ref="hireRef" :model="hireForm" layout="vertical">
        <a-form-item label="工号" name="empNo" :rules="[{ required: true, message: '请填写工号' }, { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: '只能包含字母、数字、下划线和短横线' }]">
          <a-input v-model:value="hireForm.empNo" :maxlength="32" />
        </a-form-item>
        <a-form-item label="入职日期" name="entryDate" :rules="[{ required: true, message: '请选择入职日期' }]">
          <a-date-picker v-model:value="hireForm.entryDate" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item label="职位" name="positionId" extra="不选使用招聘需求的职位"><PositionSelect v-model:value="hireForm.positionId" /></a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="用工类型" name="empType">
              <a-select v-model:value="hireForm.empType" :options="toOptions(dict.hr_emp_type)" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="预计转正日期" name="regularDate" extra="默认入职 3 个月后">
              <a-date-picker v-model:value="hireForm.regularDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="试用期基本工资" name="baseSalary" extra="填写后同时建立薪资档案（社保、公积金基数默认为基本工资）">
          <a-input-number v-model:value="hireForm.baseSalary" :min="0" :precision="2" addon-after="元/月" class="full" />
        </a-form-item>
        <a-form-item label="登录账号">
          <a-radio-group v-model:value="hireForm.accountMode" class="account-mode">
            <a-radio value="none">暂不开通</a-radio>
            <a-radio value="new">开通新账号</a-radio>
            <a-radio value="link">关联已有账号</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item
          v-if="hireForm.accountMode === 'new'"
          label="登录名"
          name="username"
          extra="初始密码为系统配置的默认密码，首次登录后请修改"
          :rules="[{ required: true, message: '请填写登录名' }, { pattern: /^[A-Za-z0-9_.-]{2,30}$/, message: '2-30 位字母、数字、下划线、点或短横线' }]"
        >
          <a-input v-model:value="hireForm.username" :maxlength="30" />
        </a-form-item>
        <a-form-item v-if="hireForm.accountMode === 'link'" label="已有账号" name="userId"><IdentityUserSelect v-model:value="hireForm.userId" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { h, onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { DeleteOutlined, DownOutlined, PlusOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import DictTag from '@/components/DictTag.vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DeptSelect from '@/components/fin/DeptSelect.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import PositionSelect from '@/components/hr/PositionSelect.vue'
import { GENDERS, INTERVIEW_METHODS, genderText } from '@/components/hr/hr'
import { formatMoney } from '@/components/scm/scm'
import {
  addCandidate,
  addInterview,
  addRecruit,
  approveRecruit,
  candidateInterviews,
  closeRecruit,
  deleteCandidate,
  deleteRecruit,
  getCandidate,
  getRecruit,
  hireCandidate,
  offerCandidate,
  openRecruits,
  pageCandidate,
  pageRecruit,
  rejectCandidate,
  updateCandidate,
  updateInterview,
  updateRecruit,
  withdrawRecruit
} from '@/api/hr'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { findDict, toOptions } from '@/utils/dict'

const userStore = useUserStore()
const dict = useDict('hr_recruit_status', 'hr_candidate_stage', 'hr_candidate_source', 'hr_interview_result', 'hr_education', 'hr_emp_type')
const dictLabel = (list, v) => findDict(list, v)?.label || ''
const methodText = (m) => INTERVIEW_METHODS.find((x) => x.value === m)?.label || '-'
const tab = ref('recruit')

// ---------------------------------------------------------------- 招聘需求
const recruitColumns = [
  { title: '岗位', key: 'title', width: 220, fixed: 'left' },
  { title: '用人部门 / 职位', key: 'dept', width: 170 },
  { title: '招聘进度', key: 'progress', width: 150 },
  { title: '应聘者', key: 'candidates', width: 80 },
  { title: '薪资范围', dataIndex: 'salaryRange', width: 110 },
  { title: '期望到岗', dataIndex: 'deadline', width: 110 },
  { title: '状态', key: 'status', width: 90 },
  { title: '申请人', dataIndex: 'createByName', width: 100 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]
const rq = reactive({ keyword: undefined, status: undefined })
const { list: rList, loading: rLoading, size: rSize, pagination: rPagination, load: rLoad, search: rSearch, onTableChange: rChange } =
  useTable(pageRecruit, rq)

const recruitRules = {
  title: [{ required: true, whitespace: true, message: '请输入岗位名称' }],
  deptId: [{ required: true, message: '请选择用人部门' }],
  headcount: [{ required: true, message: '请填写招聘人数' }]
}
const RECRUIT_FIELDS = ['title', 'deptId', 'positionId', 'headcount', 'salaryRange', 'requirement', 'reason', 'deadline']
const recruitDrawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, ...Object.fromEntries(RECRUIT_FIELDS.map((f) => [f, undefined])), headcount: 1 }),
  fetch: (r) => getRecruit(r.id),
  create: addRecruit,
  update: updateRecruit,
  toPayload: (f) => Object.fromEntries(RECRUIT_FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: () => {
    rLoad()
    loadRecruitOptions()
  }
})
const recruitFormRef = recruitDrawer.formRef
const recruitForm = recruitDrawer.form

async function approve(record) {
  const flow = await approveRecruit(record.id)
  message.success(flow ? '已提交审批，审批通过后开始招聘' : '审核通过，开始招聘')
  rLoad()
  loadRecruitOptions()
}
async function withdraw(id) {
  await withdrawRecruit(id)
  message.success('已撤回')
  rLoad()
}
async function close(id) {
  await closeRecruit(id)
  message.success('已关闭')
  rLoad()
  loadRecruitOptions()
}
async function removeRecruit(id) {
  await deleteRecruit(id)
  message.success('删除成功')
  rLoad()
}
function viewCandidates(record) {
  cq.recruitId = record.id
  tab.value = 'candidate'
  cSearch()
}

// 招聘需求下拉：全部（筛选）与招聘中（录入简历）
const recruitOptions = ref([])
const openOptions = ref([])
async function loadRecruitOptions() {
  const [all, open] = await Promise.all([pageRecruit({ current: 1, size: 200 }), openRecruits()])
  recruitOptions.value = all.records.map((r) => ({ value: r.id, label: `${r.recruitNo} ${r.title}` }))
  openOptions.value = open.map((r) => ({ value: r.id, label: `${r.recruitNo} ${r.title}` }))
}

// ---------------------------------------------------------------- 应聘者
const candidateColumns = [
  { title: '姓名', key: 'name', width: 150, fixed: 'left' },
  { title: '应聘岗位', key: 'recruit', width: 190 },
  { title: '学历 / 经验', key: 'education', width: 200 },
  { title: '来源', key: 'source', width: 100 },
  { title: '期望月薪', key: 'expectedSalary', width: 110, align: 'right' },
  { title: '面试', key: 'interview', width: 140 },
  { title: '阶段', key: 'stage', width: 110 },
  { title: '录入时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]
const cq = reactive({ recruitId: undefined, stage: undefined, keyword: undefined })
const { list: cList, loading: cLoading, size: cSize, pagination: cPagination, load: cLoad, search: cSearch, onTableChange: cChange } =
  useTable(pageCandidate, cq)
function setStage(stage) {
  cq.stage = stage
  cSearch()
}

const candidateRules = {
  recruitId: [{ required: true, message: '请选择招聘需求' }],
  name: [{ required: true, whitespace: true, message: '请输入姓名' }],
  email: [{ type: 'email', message: '邮箱格式不正确' }]
}
const CANDIDATE_FIELDS = ['recruitId', 'name', 'gender', 'phone', 'email', 'education', 'school', 'workYears', 'source', 'expectedSalary',
  'resume', 'remark']
const candidateDrawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, ...Object.fromEntries(CANDIDATE_FIELDS.map((f) => [f, undefined])), gender: 'M' }),
  fetch: (r) => getCandidate(r.id),
  create: addCandidate,
  update: updateCandidate,
  toPayload: (f) => Object.fromEntries(CANDIDATE_FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: () => {
    cLoad()
    rLoad()
  }
})
const candidateFormRef = candidateDrawer.formRef
const candidateForm = candidateDrawer.form

async function offer(id) {
  await offerCandidate(id)
  message.success('已发 Offer')
  cLoad()
}

function onMore(key, record) {
  if (key === 'edit') {
    candidateDrawer.openEdit(record)
  } else if (key === 'reject') {
    let reason = ''
    Modal.confirm({
      title: `淘汰「${record.name}」`,
      content: () => h('textarea', { class: 'ant-input', rows: 2, placeholder: '淘汰原因（选填）', onInput: (e) => (reason = e.target.value) }),
      okType: 'danger',
      async onOk() {
        await rejectCandidate(record.id, reason)
        message.success('已淘汰')
        cLoad()
      }
    })
  } else if (key === 'delete') {
    Modal.confirm({
      title: `删除应聘者「${record.name}」？`,
      okType: 'danger',
      async onOk() {
        await deleteCandidate(record.id)
        message.success('删除成功')
        cLoad()
        rLoad()
      }
    })
  }
}

// 面试
const interviewOpen = ref(false)
const current = ref()
const interviews = ref([])
const resumeText = ref('')
async function openInterviews(record) {
  current.value = record
  interviewOpen.value = true
  const [c, list] = await Promise.all([getCandidate(record.id), candidateInterviews(record.id)])
  current.value = { ...record, ...c }
  resumeText.value = c.resume
  interviews.value = list
}

const ivOpen = ref(false)
const ivSaving = ref(false)
const ivRef = ref()
const ivForm = reactive({})
function openInterviewForm(i) {
  Object.assign(ivForm, i
    ? { id: i.id, interviewTime: i.interviewTime, method: i.method, interviewerId: i.interviewerId, interviewerName: i.interviewerName,
        result: i.result, score: i.score, feedback: i.feedback }
    : { id: undefined, interviewTime: dayjs().add(1, 'day').hour(10).minute(0).second(0).format('YYYY-MM-DD HH:mm:ss'),
        method: 'ONSITE', interviewerId: userStore.user?.id, interviewerName: userStore.displayName, result: 'PENDING', score: undefined,
        feedback: '' })
  ivOpen.value = true
}
async function saveInterview() {
  await ivRef.value.validate()
  ivSaving.value = true
  try {
    const { id, interviewerName, ...data } = ivForm
    if (id) await updateInterview(id, data)
    else await addInterview(current.value.id, data)
    message.success('已保存')
    ivOpen.value = false
    await openInterviews(current.value)
    cLoad()
  } finally {
    ivSaving.value = false
  }
}

// 录用
const hireOpen = ref(false)
const hiring = ref(false)
const hireRef = ref()
const hireForm = reactive({ empNo: '', entryDate: undefined, positionId: undefined, userId: undefined, empType: 'FULL_TIME',
  regularDate: undefined, baseSalary: undefined, accountMode: 'none', username: '' })
function openHire(record) {
  current.value = record
  Object.assign(hireForm, { empNo: '', entryDate: dayjs().add(7, 'day').format('YYYY-MM-DD'), positionId: undefined, userId: undefined,
    empType: 'FULL_TIME', regularDate: undefined, baseSalary: undefined, accountMode: 'none', username: '' })
  hireOpen.value = true
}
async function submitHire() {
  await hireRef.value.validate()
  hiring.value = true
  try {
    const { accountMode, username, userId, ...rest } = hireForm
    await hireCandidate(current.value.id, {
      ...rest,
      userId: accountMode === 'link' ? userId || null : null,
      username: accountMode === 'new' ? username : null
    })
    message.success('已录用，员工档案已生成')
    hireOpen.value = false
    cLoad()
    rLoad()
    loadRecruitOptions()
  } finally {
    hiring.value = false
  }
}

onMounted(() => {
  rLoad()
  cLoad()
  loadRecruitOptions()
})
</script>

<style scoped>
.head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}
.stages {
  margin-top: 12px;
}
.stage-tag {
  cursor: pointer;
}
.w140 {
  width: 140px;
}
.w200 {
  width: 200px;
}
.w240 {
  width: 260px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
.mb {
  margin-bottom: 16px;
}
.pre {
  white-space: pre-wrap;
}
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 20px 0 12px;
  font-weight: 600;
}
.iv-head {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
</style>
