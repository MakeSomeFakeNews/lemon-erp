<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="关键字" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="计划号、产品编码或名称" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.mes_plan_status)" />
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
          <template #icon><PlusOutlined /></template>新增计划
        </a-button>
        <a-button v-perm="'mes:plan:add'" @click="salesOpen = true">
          <template #icon><ShoppingOutlined /></template>由销售订单生成
        </a-button>
        <a-button v-perm="'mes:plan:confirm'" :disabled="!draftSelected.length" @click="confirmSelected">
          <template #icon><CheckOutlined /></template>确认（{{ draftSelected.length }}）
        </a-button>
        <a-button v-perm="'mes:mrp:run'" type="primary" ghost :loading="running" @click="openMrp">
          <template #icon><CalculatorOutlined /></template>MRP 运算<template v-if="confirmedSelected.length">（{{ confirmedSelected.length }}）</template>
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="{ ...rowSelection, getCheckboxProps: (r) => ({ disabled: r.status === 'CLOSED' }) }"
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
              <div>工单 {{ formatQty(record.woQty) }}</div>
              <div class="sub-text">完工 {{ formatQty(record.finishedQty) }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'source'">
            <span v-if="record.source === 'SALES'">销售订单 {{ record.salesOrderNo }}</span>
            <span v-else class="sub-text">手工</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="mes_plan_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <template v-if="record.status === 'DRAFT'">
                <a-button v-perm="'mes:plan:edit'" type="link" size="small" @click="openForm(record)">编辑</a-button>
                <a-button v-perm="'mes:plan:confirm'" type="link" size="small" @click="confirmOne(record)">确认</a-button>
                <span v-perm="'mes:plan:remove'">
                  <a-popconfirm :title="`确定删除计划 ${record.planNo}？`" @confirm="remove(record.id)">
                    <a-button type="link" danger size="small">删除</a-button>
                  </a-popconfirm>
                </span>
              </template>
              <template v-else-if="record.status === 'CONFIRMED'">
                <a-button
                  v-if="Number(record.woQty) < Number(record.qty)"
                  v-perm="'mes:wo:add'"
                  type="link"
                  size="small"
                  @click="openToWo(record)"
                >
                  转工单
                </a-button>
                <span v-perm="'mes:plan:confirm'">
                  <a-popconfirm title="关闭后不再参与 MRP 运算，确定关闭？" @confirm="close(record.id)">
                    <a-button type="link" size="small">关闭</a-button>
                  </a-popconfirm>
                </span>
              </template>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="formOpen" :title="form.id ? '编辑计划' : '新增计划'" :confirm-loading="saving" @ok="submit">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="产品（需有默认 BOM）" name="materialId">
          <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="数量" name="qty">
              <a-input-number v-model:value="form.qty" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="需求日期" name="dueDate">
              <a-date-picker v-model:value="form.dueDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="toWoOpen" :title="`计划 ${toWoPlan?.planNo || ''} 转工单`" :confirm-loading="saving" @ok="submitToWo">
      <a-form layout="vertical">
        <a-form-item label="工单数量" :extra="toWoPlan ? `计划 ${formatQty(toWoPlan.qty)}，已转工单 ${formatQty(toWoPlan.woQty)}` : ''">
          <a-input-number v-model:value="toWo.qty" :min="0" class="full" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="计划开工">
              <a-date-picker v-model:value="toWo.planStart" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="计划完工">
              <a-date-picker v-model:value="toWo.planEnd" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="完工入库仓库">
          <WarehouseSelect v-model:value="toWo.warehouseId" placeholder="选填" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="salesOpen" title="由销售订单生成计划" :confirm-loading="saving" @ok="fromSales">
      <p class="sub-text">订单中的自制、委外件按「未出库数量 - 已生成计划的数量」生成草稿计划，需求日期取交货日期；重复生成不会重复计划。</p>
      <a-select
        v-model:value="salesOrderId"
        show-search
        :filter-option="false"
        :options="salesOptions"
        placeholder="搜索已审核的销售订单号"
        class="full"
        @search="searchSales"
        @focus="() => !salesOrders.length && searchSales('')"
      />
    </a-modal>
    <a-modal v-model:open="mrpOpen" title="MRP 运算" :confirm-loading="running" @ok="runSelected">
      <a-form layout="vertical">
        <a-form-item label="生产计划">
          <span v-if="confirmedSelected.length">已勾选 {{ confirmedSelected.length }} 个已确认的计划</span>
          <span v-else class="sub-text">没有勾选计划（可在列表中勾选已确认的计划）</span>
        </a-form-item>
        <a-form-item extra="已审核、未完成的销售订单中还没有生成生产计划的未出库数量">
          <a-checkbox v-model:checked="mrpForm.includeSales">计入销售订单的未计划需求</a-checkbox>
        </a-form-item>
        <a-form-item label="计入销售预测" extra="从本月起的月数，0 不计入；预测扣减同月已有的销售订单（在「MRP 运算」页面维护预测）">
          <a-input-number v-model:value="mrpForm.forecastMonths" :min="0" :max="12" addon-after="个月" style="width: 160px" />
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

const router = useRouter()
const dict = useDict('mes_plan_status')

const columns = [
  { title: '计划号', dataIndex: 'planNo', width: 170 },
  { title: '产品', key: 'product', width: 240 },
  { title: '数量', key: 'qty', width: 110, align: 'right' },
  { title: '工单进度', key: 'progress', width: 110, align: 'right' },
  { title: '需求日期', dataIndex: 'dueDate', width: 110 },
  { title: '来源', key: 'source', width: 200 },
  { title: '状态', key: 'status', width: 90, dict: 'mes_plan_status' },
  { title: '制单人', dataIndex: 'createByName', width: 100 },
  { title: '备注', dataIndex: 'remark', width: 160, ellipsis: true },
  { title: '操作', key: 'action', width: 170, fixed: 'right' }
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
const draftSelected = computed(() => selectedRows.value.filter((r) => r.status === 'DRAFT').map((r) => r.id))
const confirmedSelected = computed(() => selectedRows.value.filter((r) => r.status === 'CONFIRMED').map((r) => r.id))

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
    message.success('已生成草稿工单，请到「生产工单」中下达')
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
  materialId: [{ required: true, message: '请选择产品' }],
  qty: [{ required: true, message: '请填写数量' }],
  dueDate: [{ required: true, message: '请选择需求日期' }]
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
    message.success('保存成功')
    formOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function confirmOne(record) {
  await confirmPlan(record.id)
  message.success('已确认')
  load()
}

async function confirmSelected() {
  await confirmPlan(draftSelected.value)
  message.success(`已确认 ${draftSelected.value.length} 个计划`)
  load()
}

async function close(id) {
  await closePlan(id)
  message.success('已关闭')
  load()
}

async function remove(id) {
  await deletePlan(id)
  message.success('删除成功')
  load()
}

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
    message.warning('请勾选已确认的计划，或计入销售订单、销售预测')
    return
  }
  running.value = true
  try {
    const runId = await runMrp({
      planIds: confirmedSelected.value,
      includeSales: mrpForm.includeSales,
      forecastMonths: mrpForm.forecastMonths || 0
    })
    message.success('运算完成')
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
  salesOrders.value.map((o) => ({ value: o.id, label: `${o.orderNo}　${o.partnerName}　交货 ${o.deliveryDate || '-'}` }))
)
let timer
function searchSales(orderNo) {
  clearTimeout(timer)
  timer = setTimeout(async () => {
    const res = await pageOrder('sales', { orderNo: orderNo || undefined, current: 1, size: 100 })
    salesOrders.value = res.records.filter((o) => ['APPROVED', 'PARTIAL'].includes(o.status))
  }, 200)
}
async function fromSales() {
  if (!salesOrderId.value) return message.warning('请选择销售订单')
  saving.value = true
  try {
    const count = await planFromSales(salesOrderId.value)
    message.success(`已生成 ${count} 个草稿计划`)
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
