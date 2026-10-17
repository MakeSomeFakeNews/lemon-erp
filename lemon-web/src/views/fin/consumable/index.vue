<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="uses" :tab="$t('fin.consumable.usesTab')">
          <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
            <a-select v-model:value="query.status" :placeholder="$t('fin.consumable.allStatus')" allow-clear :options="toOptions(dict.fin_consumable_status)" class="filter" @change="search" />
            <DeptSelect v-model:value="query.deptId" :placeholder="$t('fin.consumable.useDept')" class="filter" />
            <a-input-search v-model:value="query.keyword" :placeholder="$t('fin.consumable.keywordPlaceholder')" allow-clear class="filter-wide" @search="search" />
            <template #extra>
              <a-button v-perm="'fin:consumable:add'" type="primary" @click="drawer.openCreate()">
                <template #icon><PlusOutlined /></template>{{ $t('fin.consumable.addUse') }}
              </a-button>
            </template>
          </TableToolbar>
          <a-table
            row-key="id"
            :size="size"
            :columns="visibleColumns"
            :data-source="list"
            :loading="loading"
            :pagination="pagination"
            :scroll="{ x: 1100 }"
            @change="onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'useNo'">
                <a @click="openDetail(record.id)">{{ record.useNo }}</a>
                <div class="sub-text">{{ record.useDate }}</div>
              </template>
              <template v-else-if="column.key === 'dept'">
                {{ record.deptName }}
                <div v-if="record.userName" class="sub-text">{{ record.userName }}</div>
              </template>
              <template v-else-if="column.key === 'qty'">{{ formatQty(record.totalQty) }}</template>
              <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.refAmount) }}</template>
              <template v-else-if="column.key === 'status'">
                <DictTag type="fin_consumable_status" :value="record.status" />
                <div v-if="record.wmsDocNo" class="sub-text">{{ $t('fin.consumable.outbound', { no: record.wmsDocNo }) }}</div>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" size="small" @click="openDetail(record.id)">{{ $t('common.action.view') }}</a-button>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="ledger" :tab="$t('fin.consumable.ledgerTab')">
          <div class="toolbar">
            <a-space wrap>
              <a-range-picker v-model:value="ledgerRange" value-format="YYYY-MM-DD" :allow-clear="false" @change="loadLedger" />
              <DeptSelect v-model:value="ledgerDept" :placeholder="$t('fin.consumable.allDepts')" class="filter" />
            </a-space>
            <span class="sub-text">{{ $t('fin.consumable.ledgerHint') }}</span>
          </div>
          <a-table row-key="key" size="middle" :columns="ledgerColumns" :data-source="ledger" :loading="ledgerLoading" :pagination="false" :scroll="{ x: 700 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
              <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
              <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.refAmount) }}</template>
            </template>
            <template #summary>
              <a-table-summary-row v-if="ledger.length">
                <a-table-summary-cell :index="0" :col-span="3">{{ $t('common.field.total') }}</a-table-summary-cell>
                <a-table-summary-cell :index="3" align="right">¥{{ formatMoney(ledgerTotal) }}</a-table-summary-cell>
              </a-table-summary-row>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <FormDrawer page :drawer="drawer" :create-title="$t('fin.consumable.createTitle')" :edit-title="$t('fin.consumable.editTitle')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('fin.consumable.useDept')" name="deptId">
              <DeptSelect v-model:value="form.deptId" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('fin.consumable.user')" name="userId">
              <IdentityUserSelect v-model:value="form.userId" :label="form.userName" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('fin.consumable.useDate')" name="useDate">
              <a-date-picker v-model:value="form.useDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('fin.consumable.warehouse')" name="warehouseId">
              <WarehouseSelect v-model:value="form.warehouseId" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="16">
            <a-form-item :label="$t('fin.consumable.expenseAccount')" name="expenseAccountId" :extra="$t('fin.consumable.expenseExtra')">
              <AccountSelect v-model:value="form.expenseAccountId" :prefix="['5101', '6601', '6602']" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('fin.consumable.purpose')" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
        <div class="section-title">{{ $t('fin.consumable.materials') }}</div>
        <a-table row-key="key" size="small" :columns="formItemColumns" :data-source="form.items" :pagination="false" :scroll="{ x: formItemScrollX }">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'material'">
              <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" class="full" @select="(m) => onMaterial(record, m)" />
            </template>
            <template v-else-if="column.key === 'location'">
              <LocationSelect v-model:value="record.locationId" :warehouse-id="form.warehouseId" class="full" />
            </template>
            <template v-else-if="column.key === 'batchNo'">
              <a-input v-if="record.batchEnabled" v-model:value="record.batchNo" :maxlength="64" />
              <span v-else class="sub-text">-</span>
            </template>
            <template v-else-if="column.key === 'qty'">
              <a-input-number v-model:value="record.qty" :min="0" :addon-after="record.unit" class="full" />
            </template>
            <template v-else-if="column.key === 'remark'">
              <a-input v-model:value="record.remark" :maxlength="255" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" danger size="small" @click="form.items.splice(index, 1)">{{ $t('common.action.delete') }}</a-button>
            </template>
          </template>
        </a-table>
        <a-button type="dashed" block class="add-line" @click="form.items.push(newItem())">
          <template #icon><PlusOutlined /></template>{{ $t('fin.consumable.addMaterial') }}
        </a-button>
        <div class="sub-text">{{ $t('fin.consumable.materialHint') }}</div>
      </a-form>
    </FormDrawer>

    <SubPage :open="detailOpen" :title="detail ? $t('fin.consumable.detailTitle', { no: detail.useNo }) : ''" width="min(900px, 100vw)" @close="detailOpen = false">
      <template v-if="detail && detail.status === 'DRAFT'" #extra>
        <a-space>
          <span v-perm="'fin:consumable:remove'">
            <a-popconfirm :title="$t('fin.consumable.confirmDelete')" @confirm="remove"><a-button danger>{{ $t('common.action.delete') }}</a-button></a-popconfirm>
          </span>
          <a-button v-perm="'fin:consumable:add'" @click="drawer.openEdit(detail)">{{ $t('common.action.edit') }}</a-button>
          <span v-perm="'fin:consumable:confirm'">
            <a-popconfirm :title="$t('fin.consumable.confirmOutbound')" @confirm="confirm">
              <a-button type="primary">{{ $t('fin.consumable.confirmOutboundBtn') }}</a-button>
            </a-popconfirm>
          </span>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('fin.consumable.useDept')">{{ detail.deptName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.consumable.user')">{{ detail.userName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.consumable.useDate')">{{ detail.useDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.consumable.warehouse')">{{ detail.warehouseName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.consumable.expenseAccount')">{{ detail.expenseAccountName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')">
            <DictTag type="fin_consumable_status" :value="detail.status" />
            <span v-if="detail.wmsDocNo" class="sub-text">{{ $t('fin.consumable.outboundDoc', { no: detail.wmsDocNo }) }}</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('fin.consumable.purpose')" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false" :scroll="{ x: 760 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}<div class="sub-text">{{ record.spec }}</div></template>
            <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
            <template v-else-if="column.key === 'amount'">¥{{ formatMoney(Number(record.qty) * Number(record.refUnitCost || 0)) }}</template>
          </template>
        </a-table>
        <div class="sub-text total">{{ $t('fin.consumable.refTotal', { amount: formatMoney(detail.refAmount) }) }}</div>
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import DeptSelect from '@/components/fin/DeptSelect.vue'
import LocationSelect from '@/components/wms/LocationSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { addConsumable, confirmConsumable, consumableLedger, deleteConsumable, getConsumable, pageConsumable, updateConsumable } from '@/api/fin'
import { formatMoney } from '@/components/fin/fin'
import { formatQty, today } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { useI18n } from 'vue-i18n'

/** 低值易耗品领用：按部门领用出库，月末成本核算按实际成本一次计入领用部门的费用 */
const { t } = useI18n()
const dict = useDict('fin_consumable_status')
const c = 'fin.consumable.'
const tab = ref('uses')

const columns = [
  { title: t(c + 'useNo'), key: 'useNo', width: 190 },
  { title: t(c + 'useDept'), key: 'dept', width: 150 },
  { title: t(c + 'warehouse'), dataIndex: 'warehouseName', width: 150 },
  { title: t(c + 'expenseAccount'), dataIndex: 'expenseAccountName', width: 150 },
  { title: t('common.field.quantity'), key: 'qty', width: 100, align: 'right' },
  { title: t(c + 'refAmount'), key: 'amount', width: 120, align: 'right' },
  { title: t(c + 'purpose'), dataIndex: 'remark', width: 160, ellipsis: true },
  { title: t('common.field.status'), key: 'status', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: 90, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'fin-consumable')
const query = reactive({ status: undefined, deptId: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageConsumable, query)
onMounted(load)
watch(() => query.deptId, search)

let seq = 0
const newItem = () => ({ key: ++seq, materialId: undefined, locationId: undefined, batchNo: '', qty: undefined, remark: '', unit: '', batchEnabled: 0 })
function onMaterial(record, m) {
  Object.assign(record, { unit: m.unit, batchEnabled: m.batchEnabled, materialLabel: `${m.code} ${m.name}` })
  if (!m.batchEnabled) record.batchNo = ''
}

const rules = {
  deptId: [{ required: true, message: t('common.rule.requiredSelect', { name: t(c + 'useDept') }) }],
  useDate: [{ required: true, message: t('common.rule.requiredSelect', { name: t(c + 'useDate') }) }],
  warehouseId: [{ required: true, message: t('common.rule.requiredSelect', { name: t(c + 'warehouse') }) }],
  expenseAccountId: [{ required: true, message: t('common.rule.requiredSelect', { name: t(c + 'expenseAccount') }) }]
}
const formItemColumns = [
  { title: t(c + 'material'), key: 'material', width: 290 },
  { title: t(c + 'location'), key: 'location', width: 200 },
  { title: t(c + 'batch'), key: 'batchNo', width: 130 },
  { title: t('common.field.quantity'), key: 'qty', width: 150 },
  { title: t('common.field.remark'), key: 'remark', width: 140 },
  { title: t('common.action.operation'), key: 'action', width: 90 }
]
const formItemScrollX = formItemColumns.reduce((sum, c) => sum + c.width, 0)
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, deptId: undefined, userId: undefined, useDate: today(), warehouseId: undefined, expenseAccountId: undefined,
    remark: '', items: [newItem()] }),
  fetch: async (record) => {
    const d = await getConsumable(record.id)
    return {
      ...d,
      items: d.items.map((i) => ({ key: ++seq, materialId: i.materialId, materialLabel: `${i.materialCode} ${i.materialName}`, locationId: i.locationId,
        batchNo: i.batchNo || '', batchEnabled: i.batchNo ? 1 : 0, qty: Number(i.qty), remark: i.remark || '', unit: i.unit }))
    }
  },
  create: async (data) => {
    const id = await addConsumable(data)
    openDetail(id)
  },
  update: updateConsumable,
  toPayload: (f) => ({
    deptId: f.deptId, userId: f.userId ?? null, warehouseId: f.warehouseId, useDate: f.useDate, expenseAccountId: f.expenseAccountId,
    remark: f.remark || null,
    items: f.items.map((i) => ({ materialId: i.materialId, locationId: i.locationId ?? null, batchNo: i.batchNo || null, qty: i.qty, remark: i.remark || null }))
  }),
  onSuccess: () => {
    load()
    if (detailOpen.value) openDetail(detail.value.id)
  }
})
const { formRef, form } = drawer

const itemColumns = [
  { title: t(c + 'material'), key: 'material' },
  { title: t(c + 'location'), dataIndex: 'locationName', width: 120 },
  { title: t(c + 'batch'), dataIndex: 'batchNo', width: 120 },
  { title: t('common.field.quantity'), key: 'qty', width: 120, align: 'right' },
  { title: t(c + 'refAmount'), key: 'amount', width: 120, align: 'right' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 140, ellipsis: true }
]
const detailOpen = ref(false)
const detail = ref()
async function openDetail(id) {
  detail.value = await getConsumable(id)
  detailOpen.value = true
}
async function confirm() {
  await confirmConsumable(detail.value.id)
  message.success(t(c + 'outboundDone'))
  openDetail(detail.value.id)
  load()
}
async function remove() {
  await deleteConsumable(detail.value.id)
  message.success(t('common.msg.deleted'))
  detailOpen.value = false
  load()
}

// ---------------------------------------------------------------- 部门领用台账
const ledgerColumns = [
  { title: t('common.field.dept'), dataIndex: 'deptName', width: 160 },
  { title: t(c + 'material'), key: 'material' },
  { title: t('common.field.quantity'), key: 'qty', width: 140, align: 'right' },
  { title: t(c + 'refAmount'), key: 'amount', width: 140, align: 'right' }
]
const ledgerRange = ref([dayjs().startOf('month').format('YYYY-MM-DD'), today()])
const ledgerDept = ref()
const ledger = ref([])
const ledgerLoading = ref(false)
const ledgerTotal = computed(() => ledger.value.reduce((s, r) => s + Number(r.refAmount || 0), 0))
async function loadLedger() {
  ledgerLoading.value = true
  try {
    const [beginDate, endDate] = ledgerRange.value
    const rows = await consumableLedger({ beginDate, endDate, deptId: ledgerDept.value })
    ledger.value = rows.map((r) => ({ ...r, key: `${r.deptId}-${r.materialId}` }))
  } finally {
    ledgerLoading.value = false
  }
}
watch(tab, (v) => v === 'ledger' && loadLedger())
watch(ledgerDept, loadLedger)
</script>

<style scoped>
.filter {
  width: 160px;
}
.filter-wide {
  width: 220px;
}
.full {
  width: 100%;
}
.block {
  margin-bottom: 16px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.section-title {
  margin: 4px 0 8px;
  font-weight: 600;
}
.add-line {
  margin: 8px 0;
}
.total {
  margin-top: 8px;
  text-align: right;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
