<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('mes.plan.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.mes_plan_status)" />
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
        <a-button v-perm="'mes:plan:add'" type="primary" @click="openForm()">
          <template #icon><PlusOutlined /></template>{{ $t('mes.plan.add') }}
        </a-button>
        <a-button v-perm="'mes:plan:add'" @click="salesOpen = true">
          <template #icon><ShoppingOutlined /></template>{{ $t('mes.plan.fromSales') }}
        </a-button>
        <a-button v-perm="'mes:plan:confirm'" :disabled="!draftSelected.length" @click="confirmSelected">
          <template #icon><CheckOutlined /></template>{{ $t('mes.plan.confirmN', { n: draftSelected.length }) }}
        </a-button>
        <a-button v-perm="'mes:mrp:run'" type="primary" ghost :loading="running" @click="openMrp">
          <template #icon><CalculatorOutlined /></template>{{ $t('mes.plan.mrpRun') }}<template v-if="confirmedSelected.length"> ({{ confirmedSelected.length }})</template>
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="{ ...rowSelection, getCheckboxProps: (r) => ({ disabled: r.status === PlanStatus.CLOSED }) }"
        :scroll="{ x: 1200, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'product'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ record.spec }}</div>
          </template>
          <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
          <template v-else-if="column.key === 'progress'">
            <template v-if="Number(record.woQty)">
              <div>{{ $t('mes.plan.woQty', { v: formatQty(record.woQty) }) }}</div>
              <div class="sub-text">{{ $t('mes.plan.finishedQty', { v: formatQty(record.finishedQty) }) }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'source'">
            <span v-if="record.source === PlanSource.SALES">{{ $t('mes.plan.salesOrderNo', { no: record.salesOrderNo }) }}</span>
            <span v-else class="sub-text">{{ $t('mes.plan.manual') }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="mes_plan_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <template v-if="record.status === PlanStatus.DRAFT">
                <a-button v-perm="'mes:plan:edit'" type="link" size="small" @click="openForm(record)">{{ $t('common.action.edit') }}</a-button>
                <a-button v-perm="'mes:plan:confirm'" type="link" size="small" @click="confirmOne(record)">{{ $t('mes.plan.confirm') }}</a-button>
                <span v-perm="'mes:plan:remove'">
                  <a-popconfirm :title="$t('mes.plan.confirmDelete', { no: record.planNo })" @confirm="remove(record.id)">
                    <a-button type="link" danger size="small">{{ $t('common.action.delete') }}</a-button>
                  </a-popconfirm>
                </span>
              </template>
              <template v-else-if="record.status === PlanStatus.CONFIRMED">
                <a-button
                  v-if="Number(record.woQty) < Number(record.qty)"
                  v-perm="'mes:wo:add'"
                  type="link"
                  size="small"
                  @click="openToWo(record)"
                >
                  {{ $t('mes.plan.toWo') }}
                </a-button>
                <span v-perm="'mes:plan:confirm'">
                  <a-popconfirm :title="$t('mes.plan.closeConfirm')" @confirm="close(record.id)">
                    <a-button type="link" size="small">{{ $t('common.action.close') }}</a-button>
                  </a-popconfirm>
                </span>
              </template>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="formOpen" :title="form.id ? $t('mes.plan.edit') : $t('mes.plan.add')" :confirm-loading="saving" @ok="submit">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('mes.plan.productLabel')" name="materialId">
          <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('common.field.quantity')" name="qty">
              <a-input-number v-model:value="form.qty" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('mes.plan.dueDate')" name="dueDate">
              <a-date-picker v-model:value="form.dueDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('common.field.remark')" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="toWoOpen" :title="$t('mes.plan.toWoTitle', { no: toWoPlan?.planNo || '' })" :confirm-loading="saving" @ok="submitToWo">
      <a-form layout="vertical">
        <a-form-item :label="$t('mes.plan.woQtyLabel')" :extra="toWoPlan ? $t('mes.plan.toWoExtra', { qty: formatQty(toWoPlan.qty), wo: formatQty(toWoPlan.woQty) }) : ''">
          <a-input-number v-model:value="toWo.qty" :min="0" class="full" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('mes.wo.planStart')">
              <a-date-picker v-model:value="toWo.planStart" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('mes.wo.planEnd')">
              <a-date-picker v-model:value="toWo.planEnd" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('mes.wo.finishWarehouse')">
          <WarehouseSelect v-model:value="toWo.warehouseId" :placeholder="$t('mes.woDocForm.optional')" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="salesOpen" :title="$t('mes.plan.fromSalesTitle')" :confirm-loading="saving" @ok="fromSales">
      <p class="sub-text">{{ $t('mes.plan.fromSalesTip') }}</p>
      <a-select
        v-model:value="salesOrderId"
        show-search
        :filter-option="false"
        :options="salesOptions"
        :placeholder="$t('mes.plan.salesSearchPh')"
        class="full"
        @search="searchSales"
        @focus="() => !salesOrders.length && searchSales('')"
      />
    </a-modal>
    <a-modal v-model:open="mrpOpen" :title="$t('mes.plan.mrpRun')" :confirm-loading="running" @ok="runSelected">
      <a-form layout="vertical">
        <a-form-item :label="$t('mes.plan.plans')">
          <span v-if="confirmedSelected.length">{{ $t('mes.plan.selectedN', { n: confirmedSelected.length }) }}</span>
          <span v-else class="sub-text">{{ $t('mes.plan.noneSelected') }}</span>
        </a-form-item>
        <a-form-item :extra="$t('mes.plan.includeSalesExtra')">
          <a-checkbox v-model:checked="mrpForm.includeSales">{{ $t('mes.plan.includeSales') }}</a-checkbox>
        </a-form-item>
        <a-form-item :label="$t('mes.plan.includeForecast')" :extra="$t('mes.plan.includeForecastExtra')">
          <a-input-number v-model:value="mrpForm.forecastMonths" :min="0" :max="12" :addon-after="$t('mes.plan.months')" style="width: 180px" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { CalculatorOutlined, CheckOutlined, PlusOutlined, ShoppingOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { addPlan, closePlan, confirmPlan, deletePlan, pagePlan, planFromSales, planToWo, runMrp, updatePlan } from '@/api/mes/plan'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { pageOrder } from '@/api/scm/order'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { formatQty, today } from '@/components/wms/wms'
import { useAction } from '@/composables/useAction'

import { OrderStatus, PlanSource, PlanStatus } from '@/constants/enums'
const router = useRouter()
const { t } = useI18n()
const dict = useDict('mes_plan_status')

const columns = [
  { title: t('mes.plan.planNo'), dataIndex: 'planNo', width: 170 },
  { title: t('mes.bom.product'), key: 'product', width: 240 },
  { title: t('common.field.quantity'), key: 'qty', width: 110, align: 'right' },
  { title: t('mes.plan.progress'), key: 'progress', width: 120, align: 'right' },
  { title: t('mes.plan.dueDate'), dataIndex: 'dueDate', width: 110 },
  { title: t('mes.plan.source'), key: 'source', width: 200 },
  { title: t('common.field.status'), key: 'status', width: 100, dict: 'mes_plan_status' },
  { title: t('mes.wo.preparedByCol'), dataIndex: 'createByName', width: 110 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 190, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'mes-plan')
const showSearch = ref(true)

const query = reactive({ keyword: undefined, status: undefined })
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(pagePlan, query)

function resetQuery() {
  Object.assign(query, { keyword: undefined, status: undefined })
  search()
}

onMounted(load)

const selectedRows = computed(() => list.value.filter((r) => selectedKeys.value.includes(r.id)))
const draftSelected = computed(() => selectedRows.value.filter((r) => r.status === PlanStatus.DRAFT).map((r) => r.id))
const confirmedSelected = computed(() => selectedRows.value.filter((r) => r.status === PlanStatus.CONFIRMED).map((r) => r.id))

// ---------------------------------------------------------------- 转工单
const toWoOpen = ref(false)
const toWoPlan = ref()
const toWo = reactive({ qty: undefined, planStart: undefined, planEnd: undefined, warehouseId: undefined })
function openToWo(record) {
  toWoPlan.value = record
  Object.assign(toWo, { qty: Number(record.qty) - Number(record.woQty), planStart: today(), planEnd: record.dueDate, warehouseId: undefined })
  toWoOpen.value = true
}
async function submitToWo() {
  saving.value = true
  try {
    await planToWo(toWoPlan.value.id, { ...toWo })
    message.success(t('mes.plan.woCreated'))
    toWoOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 新增 / 编辑
const formOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const rules = {
  materialId: [{ required: true, message: t('mes.wo.productRequired') }],
  qty: [{ required: true, message: t('mes.wo.qtyRequired') }],
  dueDate: [{ required: true, message: t('mes.plan.dueDateRequired') }]
}

function openForm(record) {
  form.value = record
    ? { ...record, qty: Number(record.qty), materialLabel: `${record.materialCode} ${record.materialName}` }
    : { materialId: undefined, qty: undefined, dueDate: today(), remark: '' }
  formOpen.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const { id, materialId, qty, dueDate, remark } = form.value
    id ? await updatePlan(id, { materialId, qty, dueDate, remark }) : await addPlan({ materialId, qty, dueDate, remark })
    message.success(t('common.msg.saveSuccess'))
    formOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

const confirmOne = useAction((record) => confirmPlan(record.id), { successKey: 'mes.plan.confirmed', reload: () => load() })

async function confirmSelected() {
  await confirmPlan(draftSelected.value)
  message.success(t('mes.plan.confirmedN', { n: draftSelected.value.length }))
  load()
}

const close = useAction(closePlan, { successKey: 'mes.wo.closed', reload: () => load() })

const remove = useAction(deletePlan, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

// ---------------------------------------------------------------- MRP
const running = ref(false)
const mrpOpen = ref(false)
const mrpForm = reactive({ includeSales: false, forecastMonths: 0 })
function openMrp() {
  Object.assign(mrpForm, { includeSales: !confirmedSelected.value.length, forecastMonths: 0 })
  mrpOpen.value = true
}
async function runSelected() {
  if (!confirmedSelected.value.length && !mrpForm.includeSales && !mrpForm.forecastMonths) {
    message.warning(t('mes.plan.mrpNothing'))
    return
  }
  running.value = true
  try {
    const runId = await runMrp({
      planIds: confirmedSelected.value,
      includeSales: mrpForm.includeSales,
      forecastMonths: mrpForm.forecastMonths || 0
    })
    message.success(t('mes.plan.mrpDone'))
    mrpOpen.value = false
    router.push({ path: '/mes/mrp', query: { runId } })
  } finally {
    running.value = false
  }
}

// ---------------------------------------------------------------- 由销售订单生成
const salesOpen = ref(false)
const salesOrderId = ref()
const salesOrders = ref([])
const salesOptions = computed(() =>
  salesOrders.value.map((o) => ({ value: o.id, label: t('mes.plan.salesOption', { no: o.orderNo, partner: o.partnerName, date: o.deliveryDate || '-' }) }))
)
let timer
function searchSales(orderNo) {
  clearTimeout(timer)
  timer = setTimeout(async () => {
    const res = await pageOrder('sales', { orderNo: orderNo || undefined, current: 1, size: 100 })
    salesOrders.value = res.records.filter((o) => [OrderStatus.APPROVED, OrderStatus.PARTIAL].includes(o.status))
  }, 200)
}
async function fromSales() {
  if (!salesOrderId.value) return message.warning(t('mes.plan.salesRequired'))
  saving.value = true
  try {
    const count = await planFromSales(salesOrderId.value)
    message.success(t('mes.plan.generatedN', { n: count }))
    salesOpen.value = false
    salesOrderId.value = undefined
    load()
  } finally {
    saving.value = false
  }
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
