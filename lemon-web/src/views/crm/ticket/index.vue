<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="工单" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="工单号、标题、客户或序列号" allow-clear />
        </a-form-item>
        <a-form-item label="类型" name="type">
          <a-select v-model:value="query.type" placeholder="全部" allow-clear :options="toOptions(dict.crm_ticket_type)" />
        </a-form-item>
        <a-form-item label="优先级" name="priority">
          <a-select v-model:value="query.priority" placeholder="全部" allow-clear :options="toOptions(dict.crm_ticket_priority)" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.crm_ticket_status)" />
        </a-form-item>
        <a-form-item label="筛选" name="mine">
          <a-space>
            <a-checkbox v-model:checked="query.mine">指派给我</a-checkbox>
            <a-checkbox v-model:checked="query.overdue">已超时</a-checkbox>
          </a-space>
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'crm:ticket:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>受理工单
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1400, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <a @click="openDetail(record)">{{ record.title }}</a>
            <div class="sub-text">{{ record.ticketNo }}</div>
          </template>
          <template v-else-if="column.key === 'customer'">
            <div>{{ record.customerName }}</div>
            <div v-if="record.contactName" class="sub-text">{{ record.contactName }} {{ record.contactPhone || '' }}</div>
          </template>
          <template v-else-if="column.key === 'type'"><DictTag type="crm_ticket_type" :value="record.type" /></template>
          <template v-else-if="column.key === 'priority'"><DictTag type="crm_ticket_priority" :value="record.priority" /></template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="crm_ticket_status" :value="record.status" />
            <a-rate v-if="record.satisfaction" :value="record.satisfaction" disabled class="rate" />
          </template>
          <template v-else-if="column.key === 'assignee'">
            <span v-if="record.assigneeName">{{ record.assigneeName }}</span>
            <span v-else class="sub-text">未指派</span>
          </template>
          <template v-else-if="column.key === 'due'">
            <template v-if="['OPEN', 'PROCESSING'].includes(record.status)">
              <div v-if="record.overdue" class="danger">{{ overdueText(record.dueTime) }}</div>
              <div v-else>剩余 {{ remaining(record.dueTime) }}</div>
              <div class="sub-text">{{ record.dueTime }}</div>
            </template>
            <template v-else>
              <div :class="record.resolvedTime && record.resolvedTime <= record.dueTime ? 'good' : 'sub-text'">
                {{ record.resolvedTime ? (record.resolvedTime <= record.dueTime ? '按时解决' : '超时解决') : '-' }}
              </div>
              <div class="sub-text">{{ record.resolvedTime }}</div>
            </template>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">
                <template #icon><EyeOutlined /></template>处理
              </a-button>
              <a-button v-if="['OPEN', 'PROCESSING'].includes(record.status)" v-perm="'crm:ticket:handle'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <span v-if="record.status === 'OPEN'" v-perm="'crm:ticket:remove'">
                <a-popconfirm :title="`确定删除工单 ${record.ticketNo}？`" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="受理工单" edit-title="编辑工单" width="min(720px, 100vw)">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="客户" name="customerId">
              <PartnerSelect v-model:value="form.customerId" type="customer" :label="form.customerName" @update:value="onCustomer" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="联系人" name="contactId">
              <a-select v-model:value="form.contactId" allow-clear :options="contactOpts" placeholder="先选择客户" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="类型" name="type">
              <a-select v-model:value="form.type" :options="toOptions(dict.crm_ticket_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="优先级" name="priority" :extra="priorityHint">
              <a-select v-model:value="form.priority" :options="toOptions(dict.crm_ticket_priority)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="受理渠道" name="channel">
              <a-select v-model:value="form.channel" allow-clear :options="toOptions(dict.crm_ticket_channel)" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="标题" name="title"><a-input v-model:value="form.title" :maxlength="128" /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="问题描述" name="description">
              <a-textarea v-model:value="form.description" :rows="4" :maxlength="2000" show-count />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="相关产品" name="materialId">
              <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="产品序列号" name="serialNo">
              <a-input-search v-model:value="form.serialNo" :maxlength="64" enter-button="查询保修" :loading="warrantyLoading" @search="checkWarranty" />
            </a-form-item>
          </a-col>
          <a-col v-if="warranty !== undefined" :span="24">
            <a-alert v-if="warranty === null" type="warning" show-icon class="tip" message="没有找到该序列号的销售出库记录" />
            <a-alert v-else :type="warranty.inWarranty ? 'success' : 'warning'" show-icon class="tip">
              <template #message>
                {{ warranty.inWarranty ? '在保修期内' : '已过保修期' }}：{{ warranty.materialCode }} {{ warranty.materialName }}，
                {{ warranty.salesDate }} 由 {{ warranty.docNo }} 销售给 {{ warranty.customerName }}，保修至 {{ warranty.warrantyEnd }}
                <a v-if="!form.materialId || !form.customerId" @click="applyWarranty">带入客户和产品</a>
              </template>
            </a-alert>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="处理人" name="assigneeId">
              <IdentityUserSelect v-model:value="form.assigneeId" :label="form.assigneeName" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <TicketDetail v-model:open="detailOpen" :ticket-id="detailId" @changed="load" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, EyeOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import TicketDetail from '@/components/crm/TicketDetail.vue'
import { addTicket, deleteTicket, getTicket, listContacts, pageTicket, ticketWarranty, updateTicket } from '@/api/crm'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { overdueText, remaining } from '@/components/crm/crm'

const dict = useDict('crm_ticket_type', 'crm_ticket_priority', 'crm_ticket_status', 'crm_ticket_channel')

const columns = [
  { title: '工单', key: 'title', width: 240 },
  { title: '客户', key: 'customer', width: 200 },
  { title: '类型', key: 'type', width: 90, dict: 'crm_ticket_type' },
  { title: '优先级', key: 'priority', width: 80, dict: 'crm_ticket_priority' },
  { title: '状态', key: 'status', width: 140, dict: 'crm_ticket_status' },
  { title: '处理人', key: 'assignee', width: 100 },
  { title: '处理时限', key: 'due', width: 170 },
  { title: '受理时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'crm-ticket')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, type: undefined, priority: undefined, status: undefined, mine: false, overdue: false })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageTicket, query, {
  params: (q) => ({ ...q, overdue: q.overdue || undefined })
})

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
onMounted(load)

// ---------------------------------------------------------------- 受理 / 编辑

const contactOpts = ref([])
async function loadContacts(customerId) {
  contactOpts.value = customerId ? (await listContacts(customerId)).map((c) => ({ value: c.id, label: c.phone ? `${c.name} ${c.phone}` : c.name })) : []
}
function onCustomer(id) {
  form.value.contactId = undefined
  loadContacts(id)
}

const priorityHint = computed(() => dict.crm_ticket_priority.find((d) => d.value === form.value.priority)?.remark || '')

const rules = {
  customerId: [{ required: true, message: '请选择客户' }],
  type: [{ required: true, message: '请选择类型' }],
  priority: [{ required: true, message: '请选择优先级' }],
  title: [{ required: true, whitespace: true, message: '请输入标题' }]
}
const FIELDS = ['customerId', 'contactId', 'type', 'priority', 'channel', 'title', 'description', 'materialId', 'serialNo', 'assigneeId']
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    ...Object.fromEntries(FIELDS.map((f) => [f, undefined])),
    type: 'REPAIR',
    priority: 'MEDIUM',
    channel: 'PHONE'
  }),
  beforeOpen: () => {
    contactOpts.value = []
    warranty.value = undefined
  },
  fetch: async (record) => {
    const d = await getTicket(record.id)
    loadContacts(d.customerId)
    return { ...d, materialLabel: d.materialCode ? `${d.materialCode} ${d.materialName}` : undefined }
  },
  create: addTicket,
  update: updateTicket,
  toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: load
})
const { formRef, form } = drawer

// 按序列号追溯销售出库与保修
const warranty = ref(undefined)
const warrantyLoading = ref(false)
async function checkWarranty() {
  if (!form.value.serialNo?.trim()) {
    message.warning('请输入序列号')
    return
  }
  warrantyLoading.value = true
  try {
    warranty.value = (await ticketWarranty(form.value.serialNo.trim())) || null
  } finally {
    warrantyLoading.value = false
  }
}
function applyWarranty() {
  const w = warranty.value
  if (!form.value.customerId) {
    form.value.customerId = w.customerId
    form.value.customerName = w.customerName
    loadContacts(w.customerId)
  }
  if (!form.value.materialId) {
    form.value.materialId = w.materialId
    form.value.materialLabel = `${w.materialCode} ${w.materialName}`
  }
}

async function remove(id) {
  await deleteTicket(id)
  message.success('删除成功')
  load()
}

const detailOpen = ref(false)
const detailId = ref()
function openDetail(record) {
  detailId.value = record.id
  detailOpen.value = true
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.danger {
  color: #f5222d;
}
.good {
  color: #52c41a;
}
.tip {
  margin-bottom: 16px;
}
.rate {
  display: block;
  font-size: 12px;
}
</style>
