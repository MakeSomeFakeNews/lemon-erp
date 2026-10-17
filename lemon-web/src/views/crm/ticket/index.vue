<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('crm.common.f.ticket')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('crm.ticket.kwPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.type')" name="type">
          <a-select v-model:value="query.type" :placeholder="$t('crm.common.all')" allow-clear :options="toOptions(dict.crm_ticket_type)" />
        </a-form-item>
        <a-form-item :label="$t('crm.common.f.priority')" name="priority">
          <a-select v-model:value="query.priority" :placeholder="$t('crm.common.all')" allow-clear :options="toOptions(dict.crm_ticket_priority)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('crm.common.all')" allow-clear :options="toOptions(dict.crm_ticket_status)" />
        </a-form-item>
        <a-form-item :label="$t('crm.ticket.filter')" name="mine">
          <a-space>
            <a-checkbox v-model:checked="query.mine">{{ $t('crm.ticket.assignedToMe') }}</a-checkbox>
            <a-checkbox v-model:checked="query.overdue">{{ $t('crm.ticket.overdue') }}</a-checkbox>
          </a-space>
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'crm:ticket:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('crm.ticket.add') }}
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
            <span v-else class="sub-text">{{ $t('crm.common.unassigned') }}</span>
          </template>
          <template v-else-if="column.key === 'due'">
            <template v-if="[TicketStatus.OPEN, TicketStatus.PROCESSING].includes(record.status)">
              <div v-if="record.overdue" class="danger">{{ overdueText(record.dueTime) }}</div>
              <div v-else>{{ $t('crm.ticket.remain', { time: remaining(record.dueTime) }) }}</div>
              <div class="sub-text">{{ record.dueTime }}</div>
            </template>
            <template v-else>
              <div :class="record.resolvedTime && record.resolvedTime <= record.dueTime ? 'good' : 'sub-text'">
                {{ record.resolvedTime ? (record.resolvedTime <= record.dueTime ? $t('crm.ticket.onTime') : $t('crm.ticket.late')) : '-' }}
              </div>
              <div class="sub-text">{{ record.resolvedTime }}</div>
            </template>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">
                <template #icon><EyeOutlined /></template>{{ $t('crm.ticket.handle') }}
              </a-button>
              <a-button v-if="[TicketStatus.OPEN, TicketStatus.PROCESSING].includes(record.status)" v-perm="'crm:ticket:handle'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <span v-if="record.status === TicketStatus.OPEN" v-perm="'crm:ticket:remove'">
                <a-popconfirm :title="$t('crm.ticket.confirmDelete', { no: record.ticketNo })" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('crm.ticket.add')" :edit-title="$t('crm.ticket.editTitle')" width="min(720px, 100vw)">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('crm.common.f.customer')" name="customerId">
              <PartnerSelect v-model:value="form.customerId" type="customer" :label="form.customerName" @update:value="onCustomer" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('crm.common.f.contact')" name="contactId">
              <a-select v-model:value="form.contactId" allow-clear :options="contactOpts" :placeholder="$t('crm.opportunity.contactPh')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('common.field.type')" name="type">
              <a-select v-model:value="form.type" :options="toOptions(dict.crm_ticket_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('crm.common.f.priority')" name="priority" :extra="priorityHint">
              <a-select v-model:value="form.priority" :options="toOptions(dict.crm_ticket_priority)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('crm.common.f.channel')" name="channel">
              <a-select v-model:value="form.channel" allow-clear :options="toOptions(dict.crm_ticket_channel)" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('crm.ticket.title')" name="title"><a-input v-model:value="form.title" :maxlength="128" /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('crm.common.f.description')" name="description">
              <a-textarea v-model:value="form.description" :rows="4" :maxlength="2000" show-count />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('crm.common.f.product')" name="materialId">
              <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('crm.ticket.serialNo')" name="serialNo">
              <a-input-search v-model:value="form.serialNo" :maxlength="64" :enter-button="$t('crm.ticket.checkWarranty')" :loading="warrantyLoading" @search="checkWarranty" />
            </a-form-item>
          </a-col>
          <a-col v-if="warranty !== undefined" :span="24">
            <a-alert v-if="warranty === null" type="warning" show-icon class="tip" :message="$t('crm.ticket.noWarranty')" />
            <a-alert v-else :type="warranty.inWarranty ? 'success' : 'warning'" show-icon class="tip">
              <template #message>
                {{ $t('crm.ticket.warrantyInfo', { status: warranty.inWarranty ? $t('crm.ticket.inWarranty') : $t('crm.ticket.outWarranty'), material: `${warranty.materialCode} ${warranty.materialName}`, date: warranty.salesDate, docNo: warranty.docNo, customer: warranty.customerName, end: warranty.warrantyEnd }) }}
                <a v-if="!form.materialId || !form.customerId" @click="applyWarranty">{{ $t('crm.ticket.applyWarranty') }}</a>
              </template>
            </a-alert>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('crm.common.f.assignee')" name="assigneeId">
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
import { useI18n } from 'vue-i18n'
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
import { useAction } from '@/composables/useAction'

import { TicketStatus } from '@/constants/enums'
const { t } = useI18n()
const dict = useDict('crm_ticket_type', 'crm_ticket_priority', 'crm_ticket_status', 'crm_ticket_channel')

const columns = [
  { title: t('crm.common.f.ticket'), key: 'title', width: 240 },
  { title: t('crm.common.f.customer'), key: 'customer', width: 200 },
  { title: t('common.field.type'), key: 'type', width: 100, dict: 'crm_ticket_type' },
  { title: t('crm.common.f.priority'), key: 'priority', width: 90, dict: 'crm_ticket_priority' },
  { title: t('common.field.status'), key: 'status', width: 140, dict: 'crm_ticket_status' },
  { title: t('crm.common.f.assignee'), key: 'assignee', width: 110 },
  { title: t('crm.ticket.col.due'), key: 'due', width: 170 },
  { title: t('crm.common.f.acceptTime'), dataIndex: 'createTime', width: 160 },
  { title: t('common.action.operation'), key: 'action', width: 180, fixed: 'right' }
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
  customerId: [{ required: true, message: t('crm.common.customerRequired') }],
  type: [{ required: true, message: t('crm.ticket.typeRequired') }],
  priority: [{ required: true, message: t('crm.ticket.priorityRequired') }],
  title: [{ required: true, whitespace: true, message: t('crm.ticket.titleRequired') }]
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
    message.warning(t('crm.ticket.serialRequired'))
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

const remove = useAction(deleteTicket, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

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
