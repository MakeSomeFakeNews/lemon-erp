<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="线索" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="编号、公司、联系人或电话" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.crm_lead_status)" />
        </a-form-item>
        <a-form-item label="来源" name="source">
          <a-select v-model:value="query.source" placeholder="全部" allow-clear :options="toOptions(dict.crm_source)" />
        </a-form-item>
        <a-form-item label="市场活动" name="campaignId">
          <a-select v-model:value="query.campaignId" placeholder="全部" allow-clear :options="campaignOpts" option-filter-prop="label" show-search />
        </a-form-item>
        <a-form-item label="负责人" name="mine">
          <a-checkbox v-model:checked="query.mine">只看我的</a-checkbox>
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'crm:lead:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>新增线索
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1450, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'company'">
            <div>{{ record.company }}</div>
            <div class="sub-text">{{ record.leadNo }}</div>
          </template>
          <template v-else-if="column.key === 'contact'">
            <div>{{ record.contactName || '-' }}<span v-if="record.title" class="sub-text"> · {{ record.title }}</span></div>
            <div v-if="record.phone" class="sub-text">{{ record.phone }}</div>
          </template>
          <template v-else-if="column.key === 'source'">
            <DictTag v-if="record.source" type="crm_source" :value="record.source" />
            <span v-else class="sub-text">-</span>
            <div v-if="record.campaignName" class="sub-text">{{ record.campaignName }}</div>
          </template>
          <template v-else-if="column.key === 'industry'">
            <DictTag v-if="record.industry" type="crm_industry" :value="record.industry" />
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="crm_lead_status" :value="record.status" />
            <div v-if="record.status === 'CONVERTED'" class="sub-text">
              <router-link :to="{ path: '/crm/customer', query: { scope: 'team', customerId: record.customerId } }">{{ record.customerName }}</router-link>
            </div>
            <div v-else-if="record.status === 'INVALID' && record.invalidReason" class="sub-text">{{ record.invalidReason }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <span v-if="open(record)" class="table-actions">
              <a-button v-perm="'crm:followup:add'" type="link" size="small" @click="openFollow(record)">
                <template #icon><MessageOutlined /></template>跟进
              </a-button>
              <a-button v-perm="'crm:lead:convert'" type="link" size="small" @click="openConvert(record)">
                <template #icon><SwapRightOutlined /></template>转化
              </a-button>
              <a-dropdown v-if="userStore.hasPerm(['crm:lead:edit', 'crm:lead:remove'])">
                <a-button type="link" size="small">更多<DownOutlined /></a-button>
                <template #overlay>
                  <a-menu @click="({ key }) => onMore(key, record)">
                    <a-menu-item v-if="userStore.hasPerm('crm:lead:edit')" key="edit">编辑</a-menu-item>
                    <a-menu-item v-if="userStore.hasPerm('crm:lead:edit')" key="invalid">标记无效</a-menu-item>
                    <a-menu-item v-if="userStore.hasPerm('crm:lead:remove')" key="remove" danger>删除</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </span>
            <span v-else-if="record.status === 'INVALID'" v-perm="'crm:lead:remove'">
              <a-popconfirm :title="`确定删除线索「${record.company}」？`" @confirm="remove(record.id)">
                <a-button type="link" danger size="small"><template #icon><DeleteOutlined /></template>删除</a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增线索" edit-title="编辑线索">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="公司名称" name="company">
              <a-input v-model:value="form.company" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="联系人" name="contactName"><a-input v-model:value="form.contactName" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="职务" name="title"><a-input v-model:value="form.title" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="电话" name="phone"><a-input v-model:value="form.phone" :maxlength="32" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="邮箱" name="email"><a-input v-model:value="form.email" :maxlength="128" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="来源" name="source">
              <a-select v-model:value="form.source" allow-clear :options="toOptions(dict.crm_source)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="市场活动" name="campaignId">
              <a-select v-model:value="form.campaignId" allow-clear show-search option-filter-prop="label" :options="campaignOpts" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="行业" name="industry">
              <a-select v-model:value="form.industry" allow-clear :options="toOptions(dict.crm_industry)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="地区" name="region"><a-input v-model:value="form.region" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="负责人" name="ownerId" extra="不选择时为你本人">
              <IdentityUserSelect v-model:value="form.ownerId" :label="form.ownerName" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="需求描述" name="requirement">
              <a-textarea v-model:value="form.requirement" :rows="3" :maxlength="500" show-count />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <a-modal v-model:open="convertOpen" :title="`转化线索：${convertTarget?.company || ''}`" :confirm-loading="converting" destroy-on-close @ok="submitConvert">
      <a-alert type="info" show-icon class="tip" message="生成客户（意向客户，负责人沿用线索负责人）和首要联系人，可以同时生成商机" />
      <a-form ref="convertRef" :model="convertForm" layout="vertical">
        <a-form-item
          label="客户编码"
          name="customerCode"
          :rules="[{ required: true, message: '请输入客户编码' }, { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: '只能包含字母、数字、下划线和短横线' }]"
        >
          <a-input v-model:value="convertForm.customerCode" :maxlength="32" placeholder="如：C010" />
        </a-form-item>
        <a-form-item>
          <a-checkbox v-model:checked="convertForm.createOpportunity">同时生成商机</a-checkbox>
        </a-form-item>
        <template v-if="convertForm.createOpportunity">
          <a-form-item label="商机名称" name="opportunityName">
            <a-input v-model:value="convertForm.opportunityName" :maxlength="128" />
          </a-form-item>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="预计金额" name="amount">
                <a-input-number v-model:value="convertForm.amount" :min="0" :precision="2" prefix="¥" class="full" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="预计成交日期" name="expectedDate">
                <a-date-picker v-model:value="convertForm.expectedDate" value-format="YYYY-MM-DD" class="full" />
              </a-form-item>
            </a-col>
          </a-row>
        </template>
      </a-form>
    </a-modal>

    <a-modal v-model:open="invalidOpen" :title="`标记无效：${invalidTarget?.company || ''}`" :confirm-loading="invaliding" @ok="submitInvalid">
      <a-textarea v-model:value="invalidReason" :rows="3" :maxlength="255" placeholder="无效原因，如：联系方式错误、没有需求" />
    </a-modal>

    <FollowupModal v-model:open="followOpen" target-type="LEAD" :target-id="followTarget?.id" :title="`跟进线索：${followTarget?.company || ''}`" @success="load" />
  </div>
</template>

<script setup>
import { h, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { DeleteOutlined, DownOutlined, MessageOutlined, PlusOutlined, SwapRightOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import FollowupModal from '@/components/crm/FollowupModal.vue'
import { addLead, campaignOptions, convertLead, deleteLead, getLead, invalidLead, pageLead, updateLead } from '@/api/crm'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const dict = useDict('crm_lead_status', 'crm_source', 'crm_industry')

const columns = [
  { title: '公司', key: 'company', width: 220 },
  { title: '联系人', key: 'contact', width: 170 },
  { title: '来源', key: 'source', width: 150, dict: 'crm_source' },
  { title: '行业', key: 'industry', width: 110, dict: 'crm_industry' },
  { title: '负责人', dataIndex: 'ownerName', width: 100 },
  { title: '状态', key: 'status', width: 140, dict: 'crm_lead_status' },
  { title: '最近跟进', dataIndex: 'lastFollowTime', width: 160 },
  { title: '需求', dataIndex: 'requirement', width: 180, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 210, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'crm-lead')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, status: undefined, source: undefined, campaignId: undefined, mine: false })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageLead, query)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const campaignOpts = ref([])
onMounted(async () => {
  if (route.query.campaignId) query.campaignId = Number(route.query.campaignId)
  load()
  campaignOpts.value = (await campaignOptions()).map((c) => ({ value: c.id, label: c.name }))
})

const open = (r) => r.status === 'NEW' || r.status === 'FOLLOWING'

function onMore(key, record) {
  if (key === 'edit') drawer.openEdit(record)
  else if (key === 'invalid') openInvalid(record)
  else if (key === 'remove') {
    Modal.confirm({ title: `确定删除线索「${record.company}」？`, okType: 'danger', onOk: () => remove(record.id) })
  }
}

// ---------------------------------------------------------------- 新增 / 编辑

const rules = {
  company: [{ required: true, whitespace: true, message: '请输入公司名称' }],
  email: [{ type: 'email', message: '邮箱格式不正确' }]
}
const FIELDS = ['company', 'contactName', 'title', 'phone', 'email', 'industry', 'source', 'campaignId', 'region', 'requirement', 'ownerId']
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, ownerName: undefined, ...Object.fromEntries(FIELDS.map((f) => [f, undefined])) }),
  fetch: (record) => getLead(record.id),
  create: addLead,
  update: updateLead,
  toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: load
})
const { formRef, form } = drawer

async function remove(id) {
  await deleteLead(id)
  message.success('删除成功')
  load()
}

// ---------------------------------------------------------------- 转化

const convertOpen = ref(false)
const convertTarget = ref(null)
const convertRef = ref()
const convertForm = ref({})
const converting = ref(false)

function openConvert(record) {
  convertTarget.value = record
  convertForm.value = { customerCode: '', createOpportunity: true, opportunityName: `${record.company} 商机`, amount: undefined, expectedDate: undefined }
  convertOpen.value = true
}

async function submitConvert() {
  try {
    await convertRef.value.validate()
  } catch {
    return
  }
  converting.value = true
  try {
    const res = await convertLead(convertTarget.value.id, convertForm.value)
    convertOpen.value = false
    load()
    Modal.success({
      title: '线索已转化',
      content: h('div', [
        h('p', `已生成客户 ${convertForm.value.customerCode}${res.opportunityId ? '，并生成了商机' : ''}。`)
      ]),
      okText: '查看客户',
      onOk: () => router.push({ path: '/crm/customer', query: { scope: 'team', customerId: res.customerId } })
    })
  } finally {
    converting.value = false
  }
}

// ---------------------------------------------------------------- 无效、跟进

const invalidOpen = ref(false)
const invalidTarget = ref(null)
const invalidReason = ref('')
const invaliding = ref(false)

function openInvalid(record) {
  invalidTarget.value = record
  invalidReason.value = ''
  invalidOpen.value = true
}

async function submitInvalid() {
  invaliding.value = true
  try {
    await invalidLead(invalidTarget.value.id, invalidReason.value || null)
    message.success('已标记为无效')
    invalidOpen.value = false
    load()
  } finally {
    invaliding.value = false
  }
}

const followOpen = ref(false)
const followTarget = ref(null)
function openFollow(record) {
  followTarget.value = record
  followOpen.value = true
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
.tip {
  margin-bottom: 16px;
}
</style>
