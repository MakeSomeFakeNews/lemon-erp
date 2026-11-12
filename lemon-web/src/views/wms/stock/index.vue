<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('wms.common.warehouse')" name="warehouseId">
          <WarehouseSelect v-model:value="query.warehouseId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('wms.common.material')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('wms.flow.codeOrName')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('wms.inout.category')" name="category">
          <MaterialCategorySelect v-model="query.category" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item v-if="mode === 'detail'" :label="$t('wms.common.location')" name="locationId">
          <LocationSelect v-model:value="query.locationId" :warehouse-id="query.warehouseId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item v-if="mode === 'detail'" :label="$t('wms.common.batchNo')" name="batchNo">
          <a-input v-model:value="query.batchNo" :placeholder="$t('common.placeholder.input')" allow-clear />
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
        <a-radio-group v-model:value="mode" button-style="solid" @change="switchMode">
          <a-radio-button value="detail">{{ $t('wms.stock.detail') }}</a-radio-button>
          <a-radio-button value="summary">{{ $t('wms.stock.summary') }}</a-radio-button>
        </a-radio-group>
        <a-checkbox v-if="mode === 'summary'" v-model:checked="query.lowStock" class="toolbar-check" @change="search">
          {{ $t('wms.stock.lowOnly') }}
        </a-checkbox>
        <a-checkbox v-if="mode === 'summary'" v-model:checked="query.highStock" class="toolbar-check" @change="search">
          {{ $t('wms.stockLimit.highOnly') }}
        </a-checkbox>
        <a-checkbox v-model:checked="query.showZero" class="toolbar-check" @change="search">{{ $t('wms.stock.showZero') }}</a-checkbox>
        <span v-if="total" class="toolbar-total">
          {{ $t('wms.stock.totalQty', { n: formatQty(total.qty) }) }}
          <template v-if="canCost && total.amount != null">{{ $t('wms.stock.totalAmount', { n: money(total.amount) }) }}</template>
        </span>
      </TableToolbar>

      <a-table
        :row-key="mode === 'detail' ? 'id' : 'materialId'"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            <div>{{ record.materialName }}</div>
            <div class="sub-text">{{ record.materialCode }}</div>
          </template>
          <template v-else-if="column.key === 'location'">{{ locationText(record.locationCode) }}</template>
          <template v-else-if="column.key === 'batchNo'">{{ record.batchNo || '-' }}</template>
          <template v-else-if="column.key === 'expiryDate'">
            <template v-if="record.expiryDate">
              {{ record.expiryDate }}
              <a-tag v-if="expiryStatus(record.expiryDate)" :color="expiryStatus(record.expiryDate).color" :bordered="false">
                {{ expiryStatus(record.expiryDate).text }}
              </a-tag>
            </template>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'category'">
            <DictTag type="wms_material_category" :value="record.category" />
          </template>
          <template v-else-if="column.key === 'safetyStock' || column.key === 'maxStock'">
            <a-tooltip v-if="record[column.key] != null && record.alertMode === StockAlertMode.WAREHOUSE && !query.warehouseId" :title="$t('wms.stockLimit.sumOfWarehouses')">
              <span class="dotted">{{ formatQty(record[column.key]) }}</span>
            </a-tooltip>
            <span v-else>{{ record[column.key] == null ? '-' : formatQty(record[column.key]) }}</span>
          </template>
          <template v-else-if="column.key === 'frozenQty'">
            <a-tooltip v-if="Number(record.frozenQty)" :title="$t('wms.stock.frozenTip')">
              <span class="frozen">{{ formatQty(record.frozenQty) }}</span>
            </a-tooltip>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'qty'">
            <span :class="{ low: isLow(record), high: isHigh(record) }">{{ formatQty(record.qty) }}</span>
            <StockAlertTag v-if="mode === 'summary'" :record="record" />
          </template>
          <template v-else-if="column.key === 'unitCost'">{{ record.unitCost == null ? '-' : price(record.unitCost) }}</template>
          <template v-else-if="column.key === 'amount'">{{ record.amount == null ? '-' : money(record.amount) }}</template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[
                { key: 'history', type: 'history', label: $t('wms.history.action'), onClick: () => showHistory(record) },
                { key: 'serial', label: $t('wms.common.serialNo'), icon: BarcodeOutlined, show: record.serialEnabled === 1, onClick: () => showSerials(record) }
              ]"
            />
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="serialOpen" :title="serialTitle" :footer="null" width="min(640px, 100vw)">
      <a-spin :spinning="serialLoading">
        <div class="serial-list">
          <a-tag v-for="sn in serials" :key="sn">{{ sn }}</a-tag>
          <a-empty v-if="!serialLoading && !serials.length" :description="$t('wms.stock.noSerial')" />
        </div>
      </a-spin>
    </a-modal>

    <StockHistory ref="historyRef" />
  </div>
</template>

<script setup>
import MaterialCategorySelect from '@/components/wms/MaterialCategorySelect.vue'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { BarcodeOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import LocationSelect from '@/components/wms/LocationSelect.vue'
import StockHistory from '@/components/wms/StockHistory.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import StockAlertTag from './StockAlertTag.vue'
import { StockAlert, StockAlertMode } from '@/constants/enums'
import { availableSerials, pageStock, pageStockSummary, stockTotal } from '@/api/wms/stock'
import { expiryStatus, formatQty, locationText } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { money, price } from '@/utils/number'
import { useUserStore } from '@/store/user'

const { t } = useI18n()

const dict = useDict('wms_material_category')
/** 查看成本金额：均价、库存金额（实时移动加权平均，供应商寄售仓不计价） */
const canCost = useUserStore().hasPerm('wms:stock:cost')
const COST_COLUMNS = canCost
  ? [
      { title: t('wms.stock.unitCost'), key: 'unitCost', width: 110, align: 'right' },
      { title: t('wms.stock.amount'), key: 'amount', width: 130, align: 'right' }
    ]
  : []

/** 操作列：流水、序列号（启用序列号的物料） */
const actionCol = () => actionColumn(2, [t('wms.history.action')])
const DETAIL_COLUMNS = [
  { title: t('wms.common.material'), key: 'material', width: 200 },
  { title: t('wms.inout.spec'), dataIndex: 'spec', width: 140, ellipsis: true },
  { title: t('wms.common.warehouse'), dataIndex: 'warehouseName', width: 130 },
  { title: t('wms.common.location'), key: 'location', width: 110 },
  { title: t('wms.common.batch'), key: 'batchNo', width: 130 },
  { title: t('common.field.quantity'), key: 'qty', width: 110, align: 'right' },
  { title: t('wms.stock.frozen'), key: 'frozenQty', width: 90, align: 'right' },
  { title: t('common.field.unit'), dataIndex: 'unit', width: 70 },
  ...COST_COLUMNS,
  { title: t('wms.common.productionDate'), dataIndex: 'productionDate', width: 110 },
  { title: t('wms.common.expiryDate'), key: 'expiryDate', width: 190 },
  actionCol()
]
const SUMMARY_COLUMNS = [
  { title: t('wms.common.material'), key: 'material', width: 220 },
  { title: t('wms.inout.spec'), dataIndex: 'spec', width: 160, ellipsis: true },
  { title: t('wms.inout.category'), key: 'category', width: 160, dict: 'wms_material_category' },
  { title: t('common.field.unit'), dataIndex: 'unit', width: 70 },
  { title: t('wms.stock.safetyStock'), key: 'safetyStock', width: 110, align: 'right' },
  { title: t('wms.stockLimit.maxStock'), key: 'maxStock', width: 110, align: 'right' },
  { title: t('wms.stock.qty'), key: 'qty', width: 260, align: 'right' },
  { title: t('wms.stock.frozen'), key: 'frozenQty', width: 100, align: 'right' },
  ...COST_COLUMNS,
  actionCol()
]

const route = useRoute()
const mode = ref('detail')
const detailCols = useColumns(DETAIL_COLUMNS, 'wms-stock')
const summaryCols = useColumns(SUMMARY_COLUMNS, 'wms-stock-summary')
const current = computed(() => (mode.value === 'detail' ? detailCols : summaryCols))
const columns = computed(() => (mode.value === 'detail' ? DETAIL_COLUMNS : SUMMARY_COLUMNS))
const checked = computed({
  get: () => current.value.checked.value,
  set: (v) => (current.value.checked.value = v)
})
const visibleColumns = computed(() => current.value.visibleColumns.value)
const scrollX = computed(() => current.value.scrollX.value)
const showSearch = ref(true)

const emptyQuery = () => ({
  warehouseId: undefined,
  keyword: undefined,
  category: undefined,
  locationId: undefined,
  batchNo: undefined,
  showZero: false,
  lowStock: false,
  highStock: false
})
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(
  (params) => (mode.value === 'detail' ? pageStock(params) : pageStockSummary(params)),
  query,
  {
    params: ({ locationId, batchNo, lowStock, highStock, ...rest }) =>
      mode.value === 'detail' ? { ...rest, locationId, batchNo } : { ...rest, lowStock, highStock }
  }
)

// 合计（全部页）：列表刷新后按相同条件查询
const total = ref(null)
watch(list, async () => {
  const { locationId, batchNo, lowStock, highStock, ...rest } = query
  const summary = mode.value !== 'detail'
  total.value = await stockTotal(summary ? { ...rest, lowStock, highStock, summary } : { ...rest, locationId, batchNo })
})

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

function switchMode() {
  list.value = []
  total.value = null
  search()
}

// 经营历史：库存明细带出所在仓库
const historyRef = ref()
function showHistory(record) {
  historyRef.value.open({
    materialId: record.materialId,
    materialCode: record.materialCode,
    materialName: record.materialName,
    warehouseId: mode.value === 'detail' ? record.warehouseId : undefined
  })
}

onMounted(() => {
  // 首页待办「库存低于安全库存」「库存高于库存上限」链接带 lowStock=1 / highStock=1：按物料汇总并只看对应的物料
  if (route.query.lowStock === '1' || route.query.highStock === '1') {
    mode.value = 'summary'
    query.lowStock = route.query.lowStock === '1'
    query.highStock = route.query.highStock === '1'
  }
  load()
})

// 预警（issue #111）：按物料的预警方式由后端判断，按总库存比较所有仓库的合计，分仓库按仓库判断
const isLow = (r) => mode.value === 'summary' && r.stockAlert === StockAlert.LOW
const isHigh = (r) => mode.value === 'summary' && r.stockAlert === StockAlert.HIGH

// 在库序列号
const serialOpen = ref(false)
const serialLoading = ref(false)
const serialTitle = ref('')
const serials = ref([])
async function showSerials(record) {
  serialTitle.value = t('wms.stock.serialTitle', { name: record.materialName, slot: `${record.warehouseName} / ${locationText(record.locationCode)}` })
  serials.value = []
  serialOpen.value = true
  serialLoading.value = true
  try {
    serials.value = await availableSerials({
      warehouseId: record.warehouseId,
      locationId: record.locationId || undefined,
      materialId: record.materialId,
      batchNo: record.batchNo || undefined
    })
  } finally {
    serialLoading.value = false
  }
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.toolbar-check {
  align-self: center;
  margin-left: 8px;
}
.toolbar-total {
  align-self: center;
  margin-left: 12px;
  color: var(--app-text-2);
}
.low {
  color: #ff4d4f;
  font-weight: 500;
}
.high {
  color: #fa8c16;
  font-weight: 500;
}
.dotted {
  border-bottom: 1px dashed var(--app-text-3);
}
.serial-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 60vh;
  overflow: auto;
}
.frozen {
  color: #fa8c16;
}
</style>
