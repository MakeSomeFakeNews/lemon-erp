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
          <a-select v-model:value="query.category" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.wms_material_category)" />
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
        <a-checkbox v-model:checked="query.showZero" class="toolbar-check" @change="search">{{ $t('wms.stock.showZero') }}</a-checkbox>
      </TableToolbar>

      <a-table
        :row-key="mode === 'detail' ? 'id' : 'materialId'"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: mode === 'detail' ? 1250 : 900, y: '100%' }"
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
          <template v-else-if="column.key === 'safetyStock'">{{ formatQty(record.safetyStock) }}</template>
          <template v-else-if="column.key === 'frozenQty'">
            <a-tooltip v-if="Number(record.frozenQty)" :title="$t('wms.stock.frozenTip')">
              <span class="frozen">{{ formatQty(record.frozenQty) }}</span>
            </a-tooltip>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'qty'">
            <span :class="{ low: isLow(record) }">{{ formatQty(record.qty) }}</span>
            <a-tag v-if="isLow(record)" color="red" :bordered="false" class="low-tag">{{ $t('wms.stock.low') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button v-if="record.serialEnabled === 1" type="link" size="small" @click="showSerials(record)">
              <template #icon><BarcodeOutlined /></template>{{ $t('wms.common.serialNo') }}
            </a-button>
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { BarcodeOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import LocationSelect from '@/components/wms/LocationSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { availableSerials, pageStock, pageStockSummary } from '@/api/wms/stock'
import { expiryStatus, formatQty, locationText } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

const { t } = useI18n()

const dict = useDict('wms_material_category')

const DETAIL_COLUMNS = [
  { title: t('wms.common.material'), key: 'material', width: 200 },
  { title: t('wms.inout.spec'), dataIndex: 'spec', width: 140, ellipsis: true },
  { title: t('wms.common.warehouse'), dataIndex: 'warehouseName', width: 130 },
  { title: t('wms.common.location'), key: 'location', width: 110 },
  { title: t('wms.common.batch'), key: 'batchNo', width: 130 },
  { title: t('common.field.quantity'), key: 'qty', width: 110, align: 'right' },
  { title: t('wms.stock.frozen'), key: 'frozenQty', width: 90, align: 'right' },
  { title: t('common.field.unit'), dataIndex: 'unit', width: 70 },
  { title: t('wms.common.productionDate'), dataIndex: 'productionDate', width: 110 },
  { title: t('wms.common.expiryDate'), key: 'expiryDate', width: 190 },
  { title: t('common.action.operation'), key: 'action', width: 100, fixed: 'right' }
]
const SUMMARY_COLUMNS = [
  { title: t('wms.common.material'), key: 'material', width: 220 },
  { title: t('wms.inout.spec'), dataIndex: 'spec', width: 160, ellipsis: true },
  { title: t('wms.inout.category'), key: 'category', width: 100, dict: 'wms_material_category' },
  { title: t('common.field.unit'), dataIndex: 'unit', width: 70 },
  { title: t('wms.stock.safetyStock'), key: 'safetyStock', width: 120, align: 'right' },
  { title: t('wms.stock.qty'), key: 'qty', width: 200, align: 'right' },
  { title: t('wms.stock.frozen'), key: 'frozenQty', width: 100, align: 'right' }
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
const showSearch = ref(true)

const emptyQuery = () => ({
  warehouseId: undefined,
  keyword: undefined,
  category: undefined,
  locationId: undefined,
  batchNo: undefined,
  showZero: false,
  lowStock: false
})
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(
  (params) => (mode.value === 'detail' ? pageStock(params) : pageStockSummary(params)),
  query,
  {
    params: ({ locationId, batchNo, lowStock, ...rest }) =>
      mode.value === 'detail' ? { ...rest, locationId, batchNo } : { ...rest, lowStock }
  }
)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

function switchMode() {
  list.value = []
  search()
}

onMounted(() => {
  // 首页待办「库存低于安全库存」链接带 lowStock=1：按物料汇总并只看低于安全库存的
  if (route.query.lowStock === '1') {
    mode.value = 'summary'
    query.lowStock = true
  }
  load()
})

const isLow = (r) => mode.value === 'summary' && r.safetyStock != null && Number(r.qty) < Number(r.safetyStock)

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
.low {
  color: #ff4d4f;
  font-weight: 500;
}
.low-tag {
  margin-left: 6px;
  margin-right: 0;
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
