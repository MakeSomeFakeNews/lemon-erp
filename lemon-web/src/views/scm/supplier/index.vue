<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('scm.supplier.supplier')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('scm.supplier.codeOrName')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('scm.supplier.level')" name="level">
          <a-select v-model:value="query.level" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.scm_supplier_level)" />
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
        <a-button v-perm="'scm:supplier:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('scm.supplier.add') }}
        </a-button>
        <a-button v-perm="'scm:supplier:add'" @click="importOpen = true">
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
        :scroll="{ x: 1370, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div>{{ record.name }}</div>
            <div class="sub-text">{{ record.code }}</div>
          </template>
          <template v-else-if="column.key === 'level'">
            <DictTag type="scm_supplier_level" :value="record.level" />
          </template>
          <template v-else-if="column.key === 'contact'">
            <div>{{ record.contact || '-' }}</div>
            <div v-if="record.phone" class="sub-text">{{ record.phone }}</div>
          </template>
          <template v-else-if="column.key === 'paymentDays'">{{ record.paymentDays ? $t('scm.supplier.days', { n: record.paymentDays }) : $t('scm.supplier.cash') }}</template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch
              :value="record.status"
              :name="record.name"
              :api="(s) => changePartnerStatus('supplier', record.id, s)"
              :disabled="!userStore.hasPerm('scm:supplier:edit')"
              @changed="(s) => (record.status = s)"
            />
          </template>
          <template v-else-if="column.custom">{{ cfCell(record, column) }}</template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openExtras(record)">
                <template #icon><PaperClipOutlined /></template>{{ $t('scm.supplier.extras') }}
              </a-button>
              <a-button type="link" size="small" @click="openPrices(record)">
                <template #icon><DollarOutlined /></template>{{ $t('scm.supplier.prices') }}
              </a-button>
              <a-button v-perm="'scm:supplier:portal'" type="link" size="small" @click="openPortal(record)">
                <template #icon><UserOutlined /></template>{{ $t('scm.supplier.portal') }}
              </a-button>
              <a-button v-perm="'scm:supplier:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <span v-perm="'scm:supplier:remove'">
                <a-popconfirm :title="$t('scm.supplier.confirmDelete', { name: record.name })" @confirm="remove(record.id)">
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

    <FormDrawer :drawer="drawer" :create-title="$t('scm.supplier.add')" :edit-title="$t('scm.supplier.edit')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.supplier.code')" name="code">
              <a-input v-model:value="form.code" :maxlength="32" :placeholder="$t('scm.supplier.codePlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.supplier.name')" name="name">
              <a-input v-model:value="form.name" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.supplier.level')" name="level" :extra="$t('scm.supplier.levelTip')">
              <a-select v-model:value="form.level" allow-clear :options="toOptions(dict.scm_supplier_level)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.supplier.paymentDays')" name="paymentDays" :extra="$t('scm.supplier.paymentDaysTip')">
              <a-input-number v-model:value="form.paymentDays" :min="0" :max="365" :addon-after="$t('scm.supplier.day')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.supplier.contact')" name="contact">
              <a-input v-model:value="form.contact" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.supplier.phone')" name="phone">
              <a-input v-model:value="form.phone" :maxlength="32" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.email')" name="email">
              <a-input v-model:value="form.email" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.supplier.taxNo')" name="taxNo">
              <a-input v-model:value="form.taxNo" :maxlength="32" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.supplier.bankName')" name="bankName">
              <a-input v-model:value="form.bankName" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.supplier.bankAccount')" name="bankAccount">
              <a-input v-model:value="form.bankAccount" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('scm.supplier.address')" name="address">
              <a-input v-model:value="form.address" :maxlength="255" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" show-count />
            </a-form-item>
          </a-col>
        </a-row>
        <CustomFieldsForm :fields="cfFields" :values="form.cf || {}" :title="$t('scm.supplier.extInfo')" />
      </a-form>
    </FormDrawer>

    <SupplierPriceDrawer v-model:open="priceOpen" :supplier="priceSupplier" />
    <PortalAccountDrawer v-model:open="portalOpen" kind="supplier" :partner="portalSupplier" />
    <ImportModal
      v-model:open="importOpen"
      :title="$t('scm.supplier.supplier')"
      :columns="importColumns"
      :importer="(data) => importPartner('supplier', data)"
      :sample="{ code: 'S0001', name: $t('scm.supplier.sampleName'), contact: $t('scm.supplier.sampleContact'), phone: '13900000000', paymentDays: 30 }"
      @success="search"
    />
    <RecordExtrasModal
      v-model:open="extrasOpen"
      :title="extrasTitle"
      biz-type="scm_supplier"
      :biz-id="extrasId"
      :editable="userStore.hasPerm('scm:supplier:edit')"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, DollarOutlined, EditOutlined, ImportOutlined, PaperClipOutlined, PlusOutlined, UserOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import ImportModal from '@/components/ImportModal.vue'
import RecordExtrasModal from '@/components/RecordExtrasModal.vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import PortalAccountDrawer from '@/components/scm/PortalAccountDrawer.vue'
import SupplierPriceDrawer from '@/components/scm/SupplierPriceDrawer.vue'
import { addPartner, changePartnerStatus, deletePartner, getPartner, pagePartner, updatePartner, importPartner } from '@/api/scm/partner'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import CustomFieldsForm from '@/components/common/CustomFieldsForm.vue'
import { useCustomColumns, useCustomFields } from '@/composables/useCustomFields'

const { t } = useI18n()
const userStore = useUserStore()
const dict = useDict('sys_common_status', 'scm_supplier_level')

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
  { title: t('scm.supplier.code'), key: 'code', required: true },
  { title: t('scm.supplier.name'), key: 'name', required: true },
  { title: t('scm.supplier.level'), key: 'level', dict: 'scm_supplier_level' },
  { title: t('scm.supplier.contact'), key: 'contact' },
  { title: t('scm.supplier.phone'), key: 'phone' },
  { title: t('common.field.email'), key: 'email' },
  { title: t('scm.supplier.address'), key: 'address' },
  { title: t('scm.supplier.taxNo'), key: 'taxNo' },
  { title: t('scm.supplier.bankName'), key: 'bankName' },
  { title: t('scm.supplier.bankAccount'), key: 'bankAccount' },
  { title: t('scm.supplier.paymentDaysCol'), key: 'paymentDays', type: 'int' },
  { title: t('common.field.remark'), key: 'remark' }
]

const columns = [
  { title: t('scm.supplier.supplier'), key: 'name', width: 240 },
  { title: t('scm.supplier.level'), key: 'level', width: 100, dict: 'scm_supplier_level' },
  { title: t('scm.supplier.contact'), key: 'contact', width: 150 },
  { title: t('scm.supplier.paymentDays'), key: 'paymentDays', width: 110 },
  { title: t('scm.supplier.address'), dataIndex: 'address', width: 220, ellipsis: true },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 150, ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 470, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'scm-supplier')
const showSearch = ref(true)

const query = reactive({ keyword: undefined, level: undefined, status: undefined })
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable((p) => pagePartner('supplier', p), query)
// 自定义字段：表单中填写，勾选「列表显示」的作为列
const cf = useCustomFields('scm_supplier')
const cfFields = cf.fields
const { columns: cfColumns, cell: cfCell } = useCustomColumns('scm_supplier', list)
const tableColumns = computed(() => {
  const cols = visibleColumns.value
  const action = cols.findIndex((c) => c.key === 'action')
  return action < 0 ? [...cols, ...cfColumns.value] : [...cols.slice(0, action), ...cfColumns.value, ...cols.slice(action)]
})

function resetQuery() {
  Object.assign(query, { keyword: undefined, level: undefined, status: undefined })
  search()
}

onMounted(load)

const rules = {
  code: [
    { required: true, message: t('scm.supplier.codeRequired') },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: t('scm.supplier.codePattern') }
  ],
  name: [{ required: true, whitespace: true, message: t('scm.supplier.nameRequired') }],
  email: [{ type: 'email', message: t('common.rule.email') }]
}
const FIELDS = ['code', 'name', 'level', 'contact', 'phone', 'email', 'address', 'taxNo', 'bankName', 'bankAccount', 'paymentDays', 'remark']
const drawer = useFormDrawer(
  cf.wrap({
    emptyForm: () => ({ id: undefined, ...Object.fromEntries(FIELDS.map((f) => [f, undefined])), paymentDays: 30 }),
    fetch: (record) => getPartner('supplier', record.id),
    create: (data) => addPartner('supplier', data),
    update: (id, data) => updatePartner('supplier', id, data),
    toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' ? null : f[k]])),
    onSuccess: load
  })
)
const { formRef, form } = drawer

async function remove(id) {
  await deletePartner('supplier', id)
  message.success(t('common.msg.deleteSuccess'))
  load()
}

const portalOpen = ref(false)
const portalSupplier = ref()
function openPortal(record) {
  portalSupplier.value = record
  portalOpen.value = true
}

const priceOpen = ref(false)
const priceSupplier = ref()
function openPrices(record) {
  priceSupplier.value = record
  priceOpen.value = true
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
</style>
