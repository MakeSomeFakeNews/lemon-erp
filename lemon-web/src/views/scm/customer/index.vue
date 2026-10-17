<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="客户" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="编码或名称" allow-clear />
        </a-form-item>
        <a-form-item label="等级" name="level">
          <a-select v-model:value="query.level" placeholder="全部" allow-clear :options="toOptions(dict.scm_customer_level)" />
        </a-form-item>
        <a-form-item label="渠道" name="channel">
          <a-select v-model:value="query.channel" placeholder="全部" allow-clear :options="toOptions(dict.scm_channel)" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.sys_common_status, true)" />
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
          <template #icon><PlusOutlined /></template>新增客户
        </a-button>
        <a-button v-perm="'scm:customer:add'" @click="importOpen = true">
          <template #icon><ImportOutlined /></template>导入
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="tableColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1520, y: '100%' }"
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
            <div v-if="record.ownerName" class="sub-text">负责 {{ record.ownerName }}</div>
          </template>
          <template v-else-if="column.key === 'contact'">
            <div>{{ record.contact || '-' }}</div>
            <div v-if="record.phone" class="sub-text">{{ record.phone }}</div>
          </template>
          <template v-else-if="column.key === 'creditLimit'">{{ record.creditLimit != null ? '¥' + formatMoney(record.creditLimit) : '不限' }}</template>
          <template v-else-if="column.key === 'paymentDays'">{{ record.paymentDays ? `${record.paymentDays} 天` : '现结' }}</template>
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
                <template #icon><PaperClipOutlined /></template>资料
              </a-button>
              <a-button v-perm="'scm:customer:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <a-button v-if="record.channel === 'DEALER'" v-perm="'scm:customer:portal'" type="link" size="small" @click="openPortal(record)">
                门户账号
              </a-button>
              <span v-perm="'scm:customer:remove'">
                <a-popconfirm :title="`确定删除客户「${record.name}」？`" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small">
                    <template #icon><DeleteOutlined /></template>删除
                  </a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增客户" edit-title="编辑客户">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="客户编码" name="code">
              <a-input v-model:value="form.code" :maxlength="32" placeholder="如：C001" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="客户名称" name="name">
              <a-input v-model:value="form.name" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="等级" name="level" >
              <a-select v-model:value="form.level" allow-clear :options="toOptions(dict.scm_customer_level)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="销售渠道" name="channel" extra="经销商可开通经销商门户，在门户订货、上报渠道库存">
              <a-radio-group v-model:value="form.channel">
                <a-radio v-for="o in dict.scm_channel" :key="o.value" :value="o.value">{{ o.label }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="负责销售" name="ownerId" extra="经销商门户的订单记到其名下审核">
              <IdentityUserSelect v-model:value="form.ownerId" :label="form.ownerName" />
            </a-form-item>
          </a-col>
          <a-col v-if="form.channel === 'DEALER'" :span="24">
            <a-form-item label="授权区域" name="region">
              <a-input v-model:value="form.region" :maxlength="128" placeholder="如：华南（广东、广西、海南）" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="账期" name="paymentDays" extra="应收单到期日 = 出库日期 + 账期">
              <a-input-number v-model:value="form.paymentDays" :min="0" :max="365" addon-after="天" class="full" />
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
            <a-form-item label="信用额度" name="creditLimit" extra="审核销售订单时校验未完成订单金额，不填不限制">
              <a-input-number v-model:value="form.creditLimit" :min="0" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="纳税人识别号" name="taxNo">
              <a-input v-model:value="form.taxNo" :maxlength="32" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="收货地址" name="address">
              <a-input v-model:value="form.address" :maxlength="255" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" show-count />
            </a-form-item>
          </a-col>
        </a-row>
        <CustomFieldsForm :fields="cfFields" :values="form.cf || {}" title="扩展信息" />
      </a-form>
    </FormDrawer>

    <ImportModal
      v-model:open="importOpen"
      title="客户"
      :columns="importColumns"
      :importer="(data) => importPartner('customer', data)"
      :sample="{ code: 'C0001', name: '示例客户', contact: '张三', phone: '13800000000', paymentDays: 30 }"
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
import { message } from 'ant-design-vue'
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
  { title: '客户编码', key: 'code', required: true },
  { title: '客户名称', key: 'name', required: true },
  { title: '客户等级', key: 'level', dict: 'scm_customer_level' },
  { title: '联系人', key: 'contact' },
  { title: '联系电话', key: 'phone' },
  { title: '收货地址', key: 'address' },
  { title: '纳税人识别号', key: 'taxNo' },
  { title: '信用额度', key: 'creditLimit', type: 'number' },
  { title: '账期(天)', key: 'paymentDays', type: 'int' },
  { title: '备注', key: 'remark' }
]

const columns = [
  { title: '客户', key: 'name', width: 240 },
  { title: '等级', key: 'level', width: 100, dict: 'scm_customer_level' },
  { title: '渠道', key: 'channel', width: 150, dict: 'scm_channel' },
  { title: '联系人', key: 'contact', width: 150 },
  { title: '信用额度', key: 'creditLimit', width: 130, align: 'right' },
  { title: '账期', key: 'paymentDays', width: 90 },
  { title: '地址', dataIndex: 'address', width: 220, ellipsis: true },
  { title: '状态', key: 'status', width: 90 },
  { title: '备注', dataIndex: 'remark', width: 150, ellipsis: true },
  { title: '操作', key: 'action', width: 290, fixed: 'right' }
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
    { required: true, message: '请输入客户编码' },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: '只能包含字母、数字、下划线和短横线' }
  ],
  name: [{ required: true, whitespace: true, message: '请输入客户名称' }]
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

async function remove(id) {
  await deletePartner('customer', id)
  message.success('删除成功')
  load()
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
