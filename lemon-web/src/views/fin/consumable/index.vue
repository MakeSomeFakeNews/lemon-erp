<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="uses" tab="领用单">
          <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
            <a-select v-model:value="query.status" placeholder="全部状态" allow-clear :options="toOptions(dict.fin_consumable_status)" class="filter" @change="search" />
            <DeptSelect v-model:value="query.deptId" placeholder="领用部门" class="filter" />
            <a-input-search v-model:value="query.keyword" placeholder="领用单号、用途" allow-clear class="filter-wide" @search="search" />
            <template #extra>
              <a-button v-perm="'fin:consumable:add'" type="primary" @click="drawer.openCreate()">
                <template #icon><PlusOutlined /></template>新增领用
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
                <div v-if="record.wmsDocNo" class="sub-text">出库 {{ record.wmsDocNo }}</div>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" size="small" @click="openDetail(record.id)">查看</a-button>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="ledger" tab="部门领用台账">
          <div class="toolbar">
            <a-space wrap>
              <a-range-picker v-model:value="ledgerRange" value-format="YYYY-MM-DD" :allow-clear="false" @change="loadLedger" />
              <DeptSelect v-model:value="ledgerDept" placeholder="全部部门" class="filter" />
            </a-space>
            <span class="sub-text">已出库的领用；金额按最近一次成本核算单价参考，实际金额以月末成本核算为准</span>
          </div>
          <a-table row-key="key" size="middle" :columns="ledgerColumns" :data-source="ledger" :loading="ledgerLoading" :pagination="false" :scroll="{ x: 700 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
              <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
              <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.refAmount) }}</template>
            </template>
            <template #summary>
              <a-table-summary-row v-if="ledger.length">
                <a-table-summary-cell :index="0" :col-span="3">合计</a-table-summary-cell>
                <a-table-summary-cell :index="3" align="right">¥{{ formatMoney(ledgerTotal) }}</a-table-summary-cell>
              </a-table-summary-row>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <FormDrawer page :drawer="drawer" create-title="新增低值易耗品领用" edit-title="编辑低值易耗品领用">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="8">
            <a-form-item label="领用部门" name="deptId">
              <DeptSelect v-model:value="form.deptId" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="领用人" name="userId">
              <IdentityUserSelect v-model:value="form.userId" :label="form.userName" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="领用日期" name="useDate">
              <a-date-picker v-model:value="form.useDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="出库仓库" name="warehouseId">
              <WarehouseSelect v-model:value="form.warehouseId" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="16">
            <a-form-item label="费用科目" name="expenseAccountId" extra="月末成本核算时按实际成本：借 费用科目（领用部门）/ 贷 周转材料">
              <AccountSelect v-model:value="form.expenseAccountId" :prefix="['5101', '6601', '6602']" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="用途" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
        <div class="section-title">领用物料</div>
        <a-table row-key="key" size="small" :columns="formItemColumns" :data-source="form.items" :pagination="false" :scroll="{ x: 860 }">
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
              <a-button type="link" danger size="small" @click="form.items.splice(index, 1)">删除</a-button>
            </template>
          </template>
        </a-table>
        <a-button type="dashed" block class="add-line" @click="form.items.push(newItem())">
          <template #icon><PlusOutlined /></template>添加物料
        </a-button>
        <div class="sub-text">只能领用类别为「低值易耗品」的物料，出库后计入领用部门的费用</div>
      </a-form>
    </FormDrawer>

    <SubPage :open="detailOpen" :title="detail ? `低值易耗品领用 ${detail.useNo}` : ''" width="min(900px, 100vw)" @close="detailOpen = false">
      <template v-if="detail && detail.status === 'DRAFT'" #extra>
        <a-space>
          <span v-perm="'fin:consumable:remove'">
            <a-popconfirm title="删除该领用单？" @confirm="remove"><a-button danger>删除</a-button></a-popconfirm>
          </span>
          <a-button v-perm="'fin:consumable:add'" @click="drawer.openEdit(detail)">编辑</a-button>
          <span v-perm="'fin:consumable:confirm'">
            <a-popconfirm title="确认后生成部门领用出库单并扣减库存，确定？" @confirm="confirm">
              <a-button type="primary">确认出库</a-button>
            </a-popconfirm>
          </span>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item label="领用部门">{{ detail.deptName }}</a-descriptions-item>
          <a-descriptions-item label="领用人">{{ detail.userName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="领用日期">{{ detail.useDate }}</a-descriptions-item>
          <a-descriptions-item label="出库仓库">{{ detail.warehouseName }}</a-descriptions-item>
          <a-descriptions-item label="费用科目">{{ detail.expenseAccountName }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <DictTag type="fin_consumable_status" :value="detail.status" />
            <span v-if="detail.wmsDocNo" class="sub-text">出库单 {{ detail.wmsDocNo }}</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="用途" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false" :scroll="{ x: 760 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}<div class="sub-text">{{ record.spec }}</div></template>
            <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
            <template v-else-if="column.key === 'amount'">¥{{ formatMoney(Number(record.qty) * Number(record.refUnitCost || 0)) }}</template>
          </template>
        </a-table>
        <div class="sub-text total">参考金额 ¥{{ formatMoney(detail.refAmount) }}，实际金额以月末成本核算为准</div>
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

/** 低值易耗品领用：按部门领用出库，月末成本核算按实际成本一次计入领用部门的费用 */
const dict = useDict('fin_consumable_status')
const tab = ref('uses')

const columns = [
  { title: '领用单号', key: 'useNo', width: 190 },
  { title: '领用部门', key: 'dept', width: 150 },
  { title: '出库仓库', dataIndex: 'warehouseName', width: 130 },
  { title: '费用科目', dataIndex: 'expenseAccountName', width: 140 },
  { title: '数量', key: 'qty', width: 100, align: 'right' },
  { title: '参考金额', key: 'amount', width: 120, align: 'right' },
  { title: '用途', dataIndex: 'remark', width: 160, ellipsis: true },
  { title: '状态', key: 'status', width: 170 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
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
  deptId: [{ required: true, message: '请选择领用部门' }],
  useDate: [{ required: true, message: '请选择领用日期' }],
  warehouseId: [{ required: true, message: '请选择仓库' }],
  expenseAccountId: [{ required: true, message: '请选择费用科目' }]
}
const formItemColumns = [
  { title: '物料', key: 'material', width: 260 },
  { title: '库位', key: 'location', width: 150 },
  { title: '批次', key: 'batchNo', width: 130 },
  { title: '数量', key: 'qty', width: 150 },
  { title: '备注', key: 'remark', width: 140 },
  { title: '操作', key: 'action', width: 70 }
]
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
  { title: '物料', key: 'material' },
  { title: '库位', dataIndex: 'locationName', width: 120 },
  { title: '批次', dataIndex: 'batchNo', width: 120 },
  { title: '数量', key: 'qty', width: 120, align: 'right' },
  { title: '参考金额', key: 'amount', width: 120, align: 'right' },
  { title: '备注', dataIndex: 'remark', width: 140, ellipsis: true }
]
const detailOpen = ref(false)
const detail = ref()
async function openDetail(id) {
  detail.value = await getConsumable(id)
  detailOpen.value = true
}
async function confirm() {
  await confirmConsumable(detail.value.id)
  message.success('已出库')
  openDetail(detail.value.id)
  load()
}
async function remove() {
  await deleteConsumable(detail.value.id)
  message.success('已删除')
  detailOpen.value = false
  load()
}

// ---------------------------------------------------------------- 部门领用台账
const ledgerColumns = [
  { title: '部门', dataIndex: 'deptName', width: 160 },
  { title: '物料', key: 'material' },
  { title: '数量', key: 'qty', width: 140, align: 'right' },
  { title: '参考金额', key: 'amount', width: 140, align: 'right' }
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
watch(tab, (t) => t === 'ledger' && loadLedger())
watch(ledgerDept, loadLedger)
</script>

<style scoped>
.filter {
  width: 140px;
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
