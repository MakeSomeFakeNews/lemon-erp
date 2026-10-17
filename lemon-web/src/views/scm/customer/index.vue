<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('scm.customer.customer')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('scm.customer.codeOrName')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('scm.customer.level')" name="level">
          <a-select v-model:value="query.level" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.scm_customer_level)" />
        </a-form-item>
        <a-form-item :label="$t('scm.customer.channel')" name="channel">
          <a-select v-model:value="query.channel" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.scm_channel)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" />
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
        <a-button v-perm="'scm:customer:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('scm.customer.add') }}
        </a-button>
        <a-button v-perm="'scm:customer:add'" @click="importOpen = true">
          <template #icon><ImportOutlined /></template>{{ $t('common.action.import') }}
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="tableColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1590, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div>{{ record.name }}</div>
            <div class="sub-text">{{ record.code }}</div>
          </template>
          <template v-else-if="column.key === 'level'">
            <DictTag type="scm_customer_level" :value="record.level" />
          </template>
          <template v-else-if="column.key === 'channel'">
            <DictTag type="scm_channel" :value="record.channel || 'DIRECT'" />
            <div v-if="record.region" class="sub-text">{{ record.region }}</div>
            <div v-if="record.ownerName" class="sub-text">{{ $t('scm.customer.ownerOf', { name: record.ownerName }) }}</div>
          </template>
          <template v-else-if="column.key === 'contact'">
            <div>{{ record.contact || '-' }}</div>
            <div v-if="record.phone" class="sub-text">{{ record.phone }}</div>
          </template>
          <template v-else-if="column.key === 'creditLimit'">{{ record.creditLimit != null ? '¥' + formatMoney(record.creditLimit) : $t('scm.customer.unlimited') }}</template>
          <template v-else-if="column.key === 'paymentDays'">{{ record.paymentDays ? $t('scm.customer.days', { n: record.paymentDays }) : $t('scm.customer.cash') }}</template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch
              :value="record.status"
              :name="record.name"
              :api="(s) => changePartnerStatus('customer', record.id, s)"
              :disabled="!userStore.hasPerm('scm:customer:edit')"
              @changed="(s) => (record.status = s)"
            />
          </template>
          <template v-else-if="column.custom">{{ cfCell(record, column) }}</template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openExtras(record)">
                <template #icon><PaperClipOutlined /></template>{{ $t('scm.customer.extras') }}
              </a-button>
              <a-button v-perm="'scm:customer:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <a-button v-if="record.channel === SalesChannel.DEALER" v-perm="'scm:customer:portal'" type="link" size="small" @click="openPortal(record)">
                {{ $t('scm.customer.portal') }}
              </a-button>
              <span v-perm="'scm:customer:remove'">
                <a-popconfirm :title="$t('scm.customer.confirmDelete', { name: record.name })" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small">
                    <template #icon><DeleteOutlined /></template>{{ $t('common.action.delete') }}
                  </a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('scm.customer.add')" :edit-title="$t('scm.customer.edit')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.customer.code')" name="code">
              <a-input v-model:value="form.code" :maxlength="32" :placeholder="$t('scm.customer.codePlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.customer.name')" name="name">
              <a-input v-model:value="form.name" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.customer.level')" name="level">
              <a-select v-model:value="form.level" allow-clear :options="toOptions(dict.scm_customer_level)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.customer.salesChannel')" name="channel" :extra="$t('scm.customer.channelTip')">
              <a-radio-group v-model:value="form.channel">
                <a-radio v-for="o in dict.scm_channel" :key="o.value" :value="o.value">{{ o.label }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.customer.owner')" name="ownerId" :extra="$t('scm.customer.ownerTip')">
              <IdentityUserSelect v-model:value="form.ownerId" :label="form.ownerName" />
            </a-form-item>
          </a-col>
          <a-col v-if="form.channel === SalesChannel.DEALER" :span="24">
            <a-form-item :label="$t('scm.customer.region')" name="region">
              <a-input v-model:value="form.region" :maxlength="128" :placeholder="$t('scm.customer.regionPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.customer.paymentDays')" name="paymentDays" :extra="$t('scm.customer.paymentDaysTip')">
              <a-input-number v-model:value="form.paymentDays" :min="0" :max="365" :addon-after="$t('scm.customer.day')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.customer.contact')" name="contact">
              <a-input v-model:value="form.contact" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.customer.phone')" name="phone">
              <a-input v-model:value="form.phone" :maxlength="32" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.customer.creditLimit')" name="creditLimit" :extra="$t('scm.customer.creditLimitTip')">
              <a-input-number v-model:value="form.creditLimit" :min="0" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.customer.taxNo')" name="taxNo">
              <a-input v-model:value="form.taxNo" :maxlength="32" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('scm.customer.shipAddress')" name="address">
              <a-input v-model:value="form.address" :maxlength="255" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" show-count />
            </a-form-item>
          </a-col>
        </a-row>
        <CustomFieldsForm :fields="cfFields" :values="form.cf || {}" :title="$t('scm.customer.extInfo')" />
      </a-form>
    </FormDrawer>

    <ImportModal
      v-model:open="importOpen"
      :title="$t('scm.customer.customer')"
      :columns="importColumns"
      :importer="(data) => importPartner('customer', data)"
      :sample="{ code: 'C0001', name: $t('scm.customer.sampleName'), contact: $t('scm.customer.sampleContact'), phone: '13800000000', paymentDays: 30 }"
      @success="search"
    />
    <RecordExtrasModal
      v-model:open="extrasOpen"
      :title="extrasTitle"
      biz-type="scm_customer"
      :biz-id="extrasId"
      :editable="userStore.hasPerm('scm:customer:edit')"
    />
    <PortalAccountDrawer v-model:open="portalOpen" kind="customer" :partner="portalCustomer" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, EditOutlined, ImportOutlined, PaperClipOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import ImportModal from '@/components/ImportModal.vue'
import RecordExtrasModal from '@/components/RecordExtrasModal.vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import PortalAccountDrawer from '@/components/scm/PortalAccountDrawer.vue'
import { addPartner, changePartnerStatus, deletePartner, getPartner, pagePartner, updatePartner, importPartner } from '@/api/scm/partner'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { formatMoney } from '@/components/scm/scm'
import CustomFieldsForm from '@/components/common/CustomFieldsForm.vue'
import { useCustomColumns, useCustomFields } from '@/composables/useCustomFields'
import { useAction } from '@/composables/useAction'

import { SalesChannel } from '@/constants/enums'
const { t } = useI18n()
const userStore = useUserStore()
const dict = useDict('sys_common_status', 'scm_customer_level', 'scm_channel')

// 经销商门户账号
const portalOpen = ref(false)
const portalCustomer = ref()
function openPortal(record) {
  portalCustomer.value = record
  portalOpen.value = true
}

const importOpen = ref(false)
// 附件、变更记录
const extrasOpen = ref(false)
const extrasId = ref()
const extrasTitle = ref('')
function openExtras(record) {
  extrasId.value = record.id
  extrasTitle.value = `${record.code} ${record.name}`
  extrasOpen.value = true
}
const importColumns = [
  { title: t('scm.customer.code'), key: 'code', required: true },
  { title: t('scm.customer.name'), key: 'name', required: true },
  { title: t('scm.customer.customerLevel'), key: 'level', dict: 'scm_customer_level' },
  { title: t('scm.customer.contact'), key: 'contact' },
  { title: t('scm.customer.phone'), key: 'phone' },
  { title: t('scm.customer.shipAddress'), key: 'address' },
  { title: t('scm.customer.taxNo'), key: 'taxNo' },
  { title: t('scm.customer.creditLimit'), key: 'creditLimit', type: 'number' },
  { title: t('scm.customer.paymentDaysCol'), key: 'paymentDays', type: 'int' },
  { title: t('common.field.remark'), key: 'remark' }
]

const columns = [
  { title: t('scm.customer.customer'), key: 'name', width: 240 },
  { title: t('scm.customer.level'), key: 'level', width: 100, dict: 'scm_customer_level' },
  { title: t('scm.customer.channel'), key: 'channel', width: 150, dict: 'scm_channel' },
  { title: t('scm.customer.contact'), key: 'contact', width: 150 },
  { title: t('scm.customer.creditLimit'), key: 'creditLimit', width: 130, align: 'right' },
  { title: t('scm.customer.paymentDays'), key: 'paymentDays', width: 110 },
  { title: t('scm.customer.address'), dataIndex: 'address', width: 220, ellipsis: true },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 150, ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 340, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'scm-customer')
const showSearch = ref(true)

const query = reactive({ keyword: undefined, level: undefined, status: undefined, channel: undefined })
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable((p) => pagePartner('customer', p), query)
// 自定义字段：表单中填写，勾选「列表显示」的作为列
const cf = useCustomFields('scm_customer')
const cfFields = cf.fields
const { columns: cfColumns, cell: cfCell } = useCustomColumns('scm_customer', list)
const tableColumns = computed(() => {
  const cols = visibleColumns.value
  const action = cols.findIndex((c) => c.key === 'action')
  return action < 0 ? [...cols, ...cfColumns.value] : [...cols.slice(0, action), ...cfColumns.value, ...cols.slice(action)]
})

function resetQuery() {
  Object.assign(query, { keyword: undefined, level: undefined, status: undefined, channel: undefined })
  search()
}

onMounted(load)

const rules = {
  code: [
    { required: true, message: t('scm.customer.codeRequired') },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: t('scm.customer.codePattern') }
  ],
  name: [{ required: true, whitespace: true, message: t('scm.customer.nameRequired') }]
}
const FIELDS = ['code', 'name', 'level', 'contact', 'phone', 'address', 'taxNo', 'creditLimit', 'paymentDays', 'remark', 'channel', 'region',
  'ownerId']
const drawer = useFormDrawer(
  cf.wrap({
    emptyForm: () => ({ id: undefined, ...Object.fromEntries(FIELDS.map((f) => [f, undefined])), paymentDays: 30, channel: 'DIRECT' }),
    fetch: async (record) => ({ ...(await getPartner('customer', record.id)), ownerName: record.ownerName }),
    create: (data) => addPartner('customer', data),
    update: (id, data) => updatePartner('customer', id, data),
    toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
    onSuccess: load
  })
)
const { formRef, form } = drawer

const remove = useAction((id) => deletePartner('customer', id), { successKey: 'common.msg.deleteSuccess', reload: () => load() })
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
</style>
