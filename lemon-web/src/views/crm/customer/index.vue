<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="客户" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="编码、名称、联系人或电话" allow-clear />
        </a-form-item>
        <a-form-item label="阶段" name="stage">
          <a-select v-model:value="query.stage" placeholder="全部" allow-clear :options="toOptions(dict.crm_customer_stage)" />
        </a-form-item>
        <a-form-item label="行业" name="industry">
          <a-select v-model:value="query.industry" placeholder="全部" allow-clear :options="toOptions(dict.crm_industry)" />
        </a-form-item>
        <a-form-item v-if="query.scope !== 'pool'" label="跟进提醒" name="dueToday">
          <a-checkbox v-model:checked="query.dueToday">今日待跟进</a-checkbox>
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll"
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :columns="columns"
        @refresh="load"
      >
        <a-radio-group v-model:value="query.scope" button-style="solid" @change="onScope">
          <a-radio-button value="mine">我的客户</a-radio-button>
          <a-radio-button value="team">团队客户</a-radio-button>
          <a-radio-button value="pool">公海</a-radio-button>
        </a-radio-group>
        <a-button v-perm="'crm:customer:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>新增客户
        </a-button>
        <a-button v-if="query.scope === 'pool'" v-perm="'crm:customer:claim'" :disabled="!selectedKeys.length" @click="claim(selectedKeys)">
          <template #icon><UserAddOutlined /></template>领取
        </a-button>
        <a-button v-perm="'crm:customer:assign'" :disabled="!selectedKeys.length" @click="openAssign(selectedKeys)">
          <template #icon><SwapOutlined /></template>分配
        </a-button>
        <span v-if="selectedKeys.length" class="selected-tip">
          已选 {{ selectedKeys.length }} 项 <a @click="selectedKeys = []">清空</a>
        </span>
      </TableToolbar>

      <a-table
        row-key="customerId"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        :scroll="{ x: 1400, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="openDetail(record)">{{ record.name }}</a>
            <div class="sub-text">
              {{ record.code }} <DictTag type="scm_customer_level" :value="record.level" />
            </div>
          </template>
          <template v-else-if="column.key === 'stage'">
            <DictTag type="crm_customer_stage" :value="record.stage" />
          </template>
          <template v-else-if="column.key === 'industry'">
            <DictTag v-if="record.industry" type="crm_industry" :value="record.industry" />
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'owner'">
            <span v-if="record.ownerName">{{ record.ownerName }}</span>
            <span v-else class="sub-text">公海</span>
          </template>
          <template v-else-if="column.key === 'contact'">
            <div>{{ record.contact || '-' }}</div>
            <div v-if="record.phone" class="sub-text">{{ record.phone }}</div>
          </template>
          <template v-else-if="column.key === 'opp'">
            <template v-if="record.openOppCount">
              <div>{{ record.openOppCount }} 个</div>
              <div class="sub-text">¥{{ formatMoney(record.openOppAmount) }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'follow'">
            <div>{{ record.lastFollowTime || '未跟进' }}</div>
            <div v-if="record.nextFollowTime" :class="isPast(record.nextFollowTime) ? 'danger' : 'sub-text'">
              下次 {{ record.nextFollowTime }}
            </div>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">
                <template #icon><EyeOutlined /></template>详情
              </a-button>
              <a-button v-if="query.scope === 'pool'" v-perm="'crm:customer:claim'" type="link" size="small" @click="claim([record.customerId])">
                <template #icon><UserAddOutlined /></template>领取
              </a-button>
              <a-button v-else v-perm="'crm:followup:add'" type="link" size="small" @click="openFollow(record)">
                <template #icon><MessageOutlined /></template>写跟进
              </a-button>
              <a-dropdown v-if="moreActions(record).length">
                <a-button type="link" size="small">更多<DownOutlined /></a-button>
                <template #overlay>
                  <a-menu @click="({ key }) => onMore(key, record)">
                    <a-menu-item v-for="a in moreActions(record)" :key="a.key">{{ a.label }}</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增客户" edit-title="编辑客户">
      <a-alert v-if="!drawer.isEdit.value" type="info" show-icon class="tip" message="新增的客户负责人为你本人；填写了联系人时同时建立首要联系人" />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="客户编码" name="code">
              <a-input v-model:value="form.code" :maxlength="32" placeholder="如：C010" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="客户名称" name="name">
              <a-input v-model:value="form.name" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="客户等级" name="level">
              <a-select v-model:value="form.level" allow-clear :options="toOptions(dict.scm_customer_level)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="客户阶段" name="stage">
              <a-select v-model:value="form.stage" :options="toOptions(dict.crm_customer_stage)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="行业" name="industry">
              <a-select v-model:value="form.industry" allow-clear :options="toOptions(dict.crm_industry)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="来源" name="source">
              <a-select v-model:value="form.source" allow-clear :options="toOptions(dict.crm_source)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="联系人" name="contact">
              <a-input v-model:value="form.contact" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="联系电话" name="phone">
              <a-input v-model:value="form.phone" :maxlength="32" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="地区" name="region">
              <a-input v-model:value="form.region" :maxlength="64" placeholder="如：上海" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="网站" name="website">
              <a-input v-model:value="form.website" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="地址" name="address">
              <a-input v-model:value="form.address" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <a-modal v-model:open="assignOpen" title="分配客户" :confirm-loading="assigning" destroy-on-close @ok="submitAssign">
      <p>将选中的 {{ assignIds.length }} 个客户分配给：</p>
      <IdentityUserSelect v-model:value="assignOwner" placeholder="不选择则放回公海" class="full" />
      <div class="sub-text hint">不选择负责人时客户放回公海，其他销售可以领取</div>
    </a-modal>

    <CustomerDetail v-model:open="detailOpen" :customer-id="detailId" @changed="load" />
    <FollowupModal v-model:open="followOpen" target-type="CUSTOMER" :target-id="followId" :title="followTitle" @success="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { DownOutlined, EyeOutlined, MessageOutlined, PlusOutlined, SwapOutlined, UserAddOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import CustomerDetail from '@/components/crm/CustomerDetail.vue'
import FollowupModal from '@/components/crm/FollowupModal.vue'
import { addCrmCustomer, assignCustomers, claimCustomers, getCrmCustomer, pageCrmCustomer, releaseCustomers, updateCrmCustomer } from '@/api/crm'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { formatMoney } from '@/components/scm/scm'
import { isPast } from '@/components/crm/crm'

const route = useRoute()
const userStore = useUserStore()
const dict = useDict('scm_customer_level', 'crm_customer_stage', 'crm_industry', 'crm_source')

const columns = [
  { title: '客户', key: 'name', width: 240 },
  { title: '阶段', key: 'stage', width: 100, dict: 'crm_customer_stage' },
  { title: '行业', key: 'industry', width: 110, dict: 'crm_industry' },
  { title: '负责人', key: 'owner', width: 110 },
  { title: '联系人', key: 'contact', width: 150 },
  { title: '在途商机', key: 'opp', width: 130, align: 'right' },
  { title: '地区', dataIndex: 'region', width: 100, ellipsis: true },
  { title: '最近 / 下次跟进', key: 'follow', width: 210 },
  { title: '操作', key: 'action', width: 230, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'crm-customer')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, stage: undefined, industry: undefined, dueToday: false })
const query = reactive({ scope: 'mine', ...emptyQuery() })
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(pageCrmCustomer, query, {
  params: (q) => ({ ...q, dueToday: q.scope !== 'pool' && q.dueToday ? true : undefined })
})

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

function onScope() {
  selectedKeys.value = []
  search()
}

onMounted(() => {
  if (route.query.scope) query.scope = route.query.scope
  load()
  if (route.query.customerId) openDetail({ customerId: Number(route.query.customerId) })
})

const isOwner = (r) => r.ownerId && r.ownerId === userStore.user?.id

function moreActions(record) {
  if (query.scope === 'pool') return userStore.hasPerm('crm:customer:assign') ? [{ key: 'assign', label: '分配' }] : []
  const actions = []
  if (isOwner(record) || userStore.hasPerm('crm:customer:edit')) actions.push({ key: 'edit', label: '编辑' })
  if (userStore.hasPerm('crm:customer:assign')) actions.push({ key: 'assign', label: '分配 / 转移' })
  if (isOwner(record) || userStore.hasPerm('crm:customer:assign')) actions.push({ key: 'release', label: '放回公海' })
  return actions
}

function onMore(key, record) {
  if (key === 'edit') drawer.openEdit(record)
  else if (key === 'assign') openAssign([record.customerId])
  else if (key === 'release') release(record)
}

// ---------------------------------------------------------------- 新增 / 编辑

const rules = {
  code: [
    { required: true, message: '请输入客户编码' },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: '只能包含字母、数字、下划线和短横线' }
  ],
  name: [{ required: true, whitespace: true, message: '请输入客户名称' }]
}
const FIELDS = ['code', 'name', 'level', 'contact', 'phone', 'address', 'industry', 'source', 'stage', 'region', 'website']
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, ...Object.fromEntries(FIELDS.map((f) => [f, undefined])), stage: 'POTENTIAL' }),
  fetch: async (record) => {
    const d = await getCrmCustomer(record.customerId)
    return { ...Object.fromEntries(FIELDS.map((f) => [f, d[f] ?? undefined])), id: d.customerId }
  },
  create: addCrmCustomer,
  update: updateCrmCustomer,
  toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: load
})
const { formRef, form } = drawer

// ---------------------------------------------------------------- 分配、领取、放回公海

const assignOpen = ref(false)
const assignIds = ref([])
const assignOwner = ref(null)
const assigning = ref(false)

function openAssign(ids) {
  assignIds.value = [...ids]
  assignOwner.value = null
  assignOpen.value = true
}

async function submitAssign() {
  assigning.value = true
  try {
    await assignCustomers(assignIds.value, assignOwner.value)
    message.success(assignOwner.value ? '分配成功' : '已放回公海')
    assignOpen.value = false
    selectedKeys.value = []
    load()
  } finally {
    assigning.value = false
  }
}

async function claim(ids) {
  const count = await claimCustomers(ids)
  if (count === ids.length) message.success(`已领取 ${count} 个客户`)
  else message.warning(`领取 ${count} 个，${ids.length - count} 个已被其他人领取`)
  selectedKeys.value = []
  load()
}

async function release(record) {
  await releaseCustomers([record.customerId])
  message.success(`「${record.name}」已放回公海`)
  load()
}

// ---------------------------------------------------------------- 详情、跟进

const detailOpen = ref(false)
const detailId = ref()
function openDetail(record) {
  detailId.value = record.customerId
  detailOpen.value = true
}

const followOpen = ref(false)
const followId = ref()
const followTitle = ref('')
function openFollow(record) {
  followId.value = record.customerId
  followTitle.value = `写跟进 - ${record.name}`
  followOpen.value = true
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.danger {
  color: #f5222d;
  font-size: 12px;
}
.full {
  width: 100%;
}
.tip {
  margin-bottom: 16px;
}
.hint {
  margin-top: 8px;
}
</style>
