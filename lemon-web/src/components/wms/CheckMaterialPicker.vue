<template>
  <a-modal
    :open="open"
    :title="$t('wms.checkPicker.title')"
    width="min(1280px, calc(100vw - 32px))"
    :style="{ top: '24px' }"
    destroy-on-close
    @cancel="close"
  >
    <SearchForm :model="query" class="picker-search" @search="search" @reset="reset">
      <a-form-item :label="$t('wms.checkPicker.scope')">
        <a-radio-group v-model:value="query.warehouseGoods" button-style="solid" @change="search">
          <a-radio-button :value="true">
            <a-tooltip :title="$t('wms.checkPicker.scopeWarehouseTip')">{{ $t('wms.checkPicker.scopeWarehouse') }}</a-tooltip>
          </a-radio-button>
          <a-radio-button :value="false">{{ $t('wms.checkPicker.scopeAll') }}</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item :label="$t('wms.checkPicker.keyword')">
        <a-input v-model:value="query.keyword" :placeholder="$t('wms.checkPicker.keywordPh')" allow-clear />
      </a-form-item>
      <a-form-item data-wide>
        <template #label>
          <a-tooltip :title="$t('wms.checkPicker.changedTip')">
            <a-checkbox v-model:checked="query.changed">{{ $t('wms.checkPicker.changed') }}</a-checkbox>
          </a-tooltip>
        </template>
        <a-range-picker
          v-model:value="query.changeRange"
          value-format="YYYY-MM-DD"
          :presets="presets"
          :disabled="!query.changed"
          :placeholder="[$t('wms.checkPicker.anyDate'), $t('wms.checkPicker.anyDate')]"
          :allow-empty="[true, true]"
          class="full"
        />
      </a-form-item>
      <a-form-item :label="$t('wms.inout.category')">
        <MaterialCategorySelect v-model="query.category" :placeholder="$t('common.placeholder.all')" />
      </a-form-item>
      <a-form-item :label="$t('pricing.goods.brand')">
        <a-select v-model:value="query.brand" :placeholder="$t('common.placeholder.all')" allow-clear show-search option-filter-prop="label" :options="toOptions(dict.wms_material_brand)" />
      </a-form-item>
      <a-form-item :label="$t('wms.checkPicker.location')">
        <LocationSelect v-model:value="query.locationId" :warehouse-id="warehouseId" :placeholder="$t('common.placeholder.all')" />
      </a-form-item>
      <a-form-item :label="$t('wms.checkPicker.stock')">
        <a-select v-model:value="query.stock" :options="stockOptions" />
      </a-form-item>
      <a-form-item :label="$t('common.field.status')">
        <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" />
      </a-form-item>
      <a-form-item :label="$t('wms.checkPicker.lastPurchase')" data-wide>
        <a-range-picker v-model:value="query.purchaseRange" value-format="YYYY-MM-DD" :allow-empty="[true, true]" class="full" />
      </a-form-item>
      <a-form-item :label="$t('wms.checkPicker.lastSales')" data-wide>
        <a-range-picker v-model:value="query.salesRange" value-format="YYYY-MM-DD" :allow-empty="[true, true]" class="full" />
      </a-form-item>
    </SearchForm>

    <a-table
      row-key="id"
      size="small"
      class="picker-table"
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :row-selection="rowSelection"
      :custom-row="customRow"
      :scroll="{ x: 1420, y: 'max(240px, calc(100vh - 470px))' }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <span>{{ record.name }}</span>
          <a-tag v-if="existing.has(record.id)" color="blue" :bordered="false" class="name-tag">{{ $t('wms.checkPicker.inItems') }}</a-tag>
          <a-tag v-if="record.status === STATUS_DISABLED" :bordered="false" class="name-tag">{{ $t('common.status.disabled') }}</a-tag>
        </template>
        <template v-else-if="column.key === 'category'">
          <DictTag type="wms_material_category" :value="record.category" />
        </template>
        <template v-else-if="column.key === 'brand'">
          <DictTag type="wms_material_brand" :value="record.brand" />
        </template>
        <template v-else-if="column.key === 'qty'">
          <span :class="Number(record.qty) < 0 ? 'qty-negative' : Number(record.qty) ? '' : 'sub-text'">{{ formatQty(record.qty) }}</span>
        </template>
        <template v-else-if="['spec', 'barcode', 'locations', 'lastChangeTime', 'lastPurchaseDate', 'lastSalesDate'].includes(column.key)">
          {{ record[column.key] || '-' }}
        </template>
      </template>
    </a-table>

    <template #footer>
      <div class="picker-footer">
        <span class="picker-tip">
          {{ $t('wms.checkPicker.selectedN', { n: selected.size }) }}
          <a v-if="selected.size" class="clear-link" @click="selected = new Map()">{{ $t('common.action.clear') }}</a>
        </span>
        <a-button @click="close">{{ $t('common.action.cancel') }}</a-button>
        <a-tooltip :title="$t('wms.checkPicker.addAllTip', { max: MAX_CHECK_LINES })">
          <a-button :disabled="!pagination.total" :loading="addingAll" @click="addAll">
            {{ $t('wms.checkPicker.addAll', { n: pagination.total }) }}
          </a-button>
        </a-tooltip>
        <a-button :disabled="!selected.size" @click="submit([...selected.keys()], true)">
          {{ $t('wms.checkPicker.addContinue') }}
        </a-button>
        <a-button type="primary" :disabled="!selected.size" @click="submit([...selected.keys()])">
          {{ $t('common.action.confirm') }}
        </a-button>
      </div>
    </template>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import LocationSelect from './LocationSelect.vue'
import MaterialCategorySelect from './MaterialCategorySelect.vue'
import { pageCheckMaterials } from '@/api/wms/check'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { StockSign } from '@/constants/enums'
import { STATUS_DISABLED } from '@/utils/constants'
import { t } from '@/locales'
import { toOptions } from '@/utils/dict'
import { changePresets, changeRanges, fetchAllMaterialIds, MAX_CHECK_LINES } from './checkMaterials'
import { formatQty } from './wms'

/**
 * 盘点单「选择商品」（issue #115、#123）：顶部查询条件、下面物料列表，勾选后「确定」加入盘点明细。
 * 数据来源是物料资料（不是库存），显示盘点仓库的当前库存（为 0 也列出）。按仓库、按变动商品都只是查询条件：
 * - 仓库范围「本仓库货品」：在盘点仓库有库存记录（含已出完的）、有过出入库，或默认库位在该仓库的物料；「全部物料」不限
 * - 「变动日期」：只列出该日期范围内在盘点仓库有过库存变动（出入库、调拨、组装拆卸等，不含盘点）的物料，即动态盘点
 * 可跨页勾选、点击行勾选，或添加全部符合条件的；「加入并继续」不关闭窗口。发出 pick(物料 ID 列表)
 */
const props = defineProps({
  open: Boolean,
  warehouseId: Number,
  /** 已在盘点明细中的物料 ID */
  existingIds: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:open', 'pick'])

const dict = useDict('sys_common_status', 'wms_material_brand')
const presets = changePresets()
const existing = computed(() => new Set(props.existingIds))

const stockOptions = [
  { value: '', label: t('wms.checkPicker.stockAll') },
  { value: StockSign.POSITIVE, label: t('wms.checkPicker.stockPositive') },
  { value: StockSign.ZERO, label: t('wms.checkPicker.stockZero') },
  { value: StockSign.NEGATIVE, label: t('wms.checkPicker.stockNegative') }
]

const columns = [
  { title: t('wms.checkPicker.code'), dataIndex: 'code', key: 'code', width: 130, fixed: 'left', ellipsis: true },
  { title: t('wms.checkPicker.name'), key: 'name', width: 220, ellipsis: true },
  { title: t('wms.checkPicker.spec'), key: 'spec', width: 160, ellipsis: true },
  { title: t('wms.checkPicker.unit'), dataIndex: 'unit', key: 'unit', width: 60 },
  { title: t('wms.inout.category'), key: 'category', width: 110 },
  { title: t('pricing.goods.brand'), key: 'brand', width: 100 },
  { title: t('wms.checkPicker.location'), key: 'locations', width: 120, ellipsis: true },
  { title: t('wms.checkPicker.qty'), key: 'qty', width: 100, align: 'right' },
  { title: t('wms.checkPicker.lastChange'), key: 'lastChangeTime', width: 150 },
  { title: t('wms.checkPicker.lastPurchase'), key: 'lastPurchaseDate', width: 100 },
  { title: t('wms.checkPicker.lastSales'), key: 'lastSalesDate', width: 100 },
  { title: t('wms.checkPicker.barcode'), key: 'barcode', width: 130, ellipsis: true }
]

const emptyQuery = () => ({
  warehouseGoods: true,
  keyword: undefined,
  changed: false,
  changeRange: changeRanges()[0].range,
  category: undefined,
  brand: undefined,
  locationId: undefined,
  stock: '',
  status: undefined,
  purchaseRange: undefined,
  salesRange: undefined
})
const query = reactive(emptyQuery())

/** 查询参数：日期范围拆分为起止，未勾选「变动日期」时不按变动筛选 */
function toParams(q) {
  const { changeRange, purchaseRange, salesRange, changed, stock, ...rest } = q
  return {
    ...rest,
    warehouseId: props.warehouseId,
    changed: changed || undefined,
    changeBegin: changed ? changeRange?.[0] || undefined : undefined,
    changeEnd: changed ? changeRange?.[1] || undefined : undefined,
    purchaseBegin: purchaseRange?.[0] || undefined,
    purchaseEnd: purchaseRange?.[1] || undefined,
    salesBegin: salesRange?.[0] || undefined,
    salesEnd: salesRange?.[1] || undefined,
    stock: stock || undefined
  }
}

const { list, loading, pagination, search, onTableChange } = useTable(pageCheckMaterials, query, { params: toParams })
pagination.pageSize = 20
pagination.pageSizeOptions = ['20', '50', '100', '200']

// 跨页保留勾选
const selected = ref(new Map())
const rowSelection = computed(() => ({
  selectedRowKeys: [...selected.value.keys()],
  preserveSelectedRowKeys: true,
  onSelect: (record, checked) => toggle([record], checked),
  onSelectAll: (checked, _rows, changed) => toggle(changed, checked)
}))
function toggle(rows, checked) {
  const map = new Map(selected.value)
  rows.forEach((r) => (checked ? map.set(r.id, r) : map.delete(r.id)))
  selected.value = map
}
/** 点击行勾选 / 取消 */
const customRow = (record) => ({
  class: 'picker-row',
  onClick: (e) => {
    if (e.target.closest?.('.ant-checkbox-wrapper, .ant-table-selection-column')) return
    toggle([record], !selected.value.has(record.id))
  }
})

function reset() {
  Object.assign(query, emptyQuery())
  search()
}

watch(
  () => props.open,
  (open) => {
    if (!open) return
    selected.value = new Map()
    search()
  }
)

const addingAll = ref(false)
async function addAll() {
  addingAll.value = true
  try {
    const { ids, total } = await fetchAllMaterialIds(toParams(query))
    if (total > MAX_CHECK_LINES) {
      message.warning(t('wms.checkPicker.tooMany', { n: total, max: MAX_CHECK_LINES }))
      return
    }
    submit(ids)
  } finally {
    addingAll.value = false
  }
}

/** keepOpen：加入明细后不关闭窗口，清空勾选继续选择 */
function submit(ids, keepOpen = false) {
  emit('pick', ids)
  if (keepOpen) selected.value = new Map()
  else close()
}

function close() {
  emit('update:open', false)
}
</script>

<style scoped>
.full {
  width: 100%;
}
.picker-search {
  margin-bottom: 12px;
}
.picker-table :deep(.picker-row) {
  cursor: pointer;
}
.name-tag {
  margin-left: 6px;
}
.picker-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}
.picker-tip {
  margin-right: auto;
  color: var(--app-text-2);
}
.clear-link {
  margin-left: 8px;
}
.qty-negative {
  color: #ff4d4f;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
