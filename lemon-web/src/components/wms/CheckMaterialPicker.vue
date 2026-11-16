<template>
  <a-modal :open="open" :title="$t('wms.checkPicker.title')" width="min(1180px, 100vw)" destroy-on-close @cancel="close">
    <a-form :model="query" layout="vertical" class="picker-form">
      <a-row :gutter="12">
        <a-col :xs="24" :md="12" :lg="8">
          <a-form-item>
            <template #label>
              <a-checkbox v-model:checked="query.changed">{{ $t('wms.checkPicker.changed') }}</a-checkbox>
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
        </a-col>
        <a-col :xs="24" :md="12" :lg="8">
          <a-form-item :label="$t('wms.checkPicker.keyword')">
            <a-input v-model:value="query.keyword" :placeholder="$t('wms.checkPicker.keywordPh')" allow-clear @press-enter="search" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :lg="4">
          <a-form-item :label="$t('wms.inout.category')">
            <MaterialCategorySelect v-model="query.category" :placeholder="$t('common.placeholder.all')" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :lg="4">
          <a-form-item :label="$t('pricing.goods.brand')">
            <a-select v-model:value="query.brand" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.wms_material_brand)" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :lg="4">
          <a-form-item :label="$t('wms.checkPicker.location')">
            <LocationSelect v-model:value="query.locationId" :warehouse-id="warehouseId" :placeholder="$t('common.placeholder.all')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :md="12" :lg="5">
          <a-form-item :label="$t('wms.checkPicker.lastPurchase')">
            <a-range-picker v-model:value="query.purchaseRange" value-format="YYYY-MM-DD" :allow-empty="[true, true]" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :md="12" :lg="5">
          <a-form-item :label="$t('wms.checkPicker.lastSales')">
            <a-range-picker v-model:value="query.salesRange" value-format="YYYY-MM-DD" :allow-empty="[true, true]" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :lg="3">
          <a-form-item :label="$t('common.field.status')">
            <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :lg="7">
          <a-form-item label=" ">
            <a-space wrap>
              <a-checkbox v-model:checked="query.inStock">{{ $t('wms.checkPicker.inStock') }}</a-checkbox>
              <a-button type="primary" @click="search">{{ $t('common.action.query') }}</a-button>
              <a-button @click="reset">{{ $t('common.action.reset') }}</a-button>
            </a-space>
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>

    <a-table
      row-key="id"
      size="small"
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :row-selection="rowSelection"
      :scroll="{ x: 1080, y: 360 }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'material'">
          <div>
            {{ record.code }} {{ record.name }}
            <a-tag v-if="record.status === STATUS_DISABLED" :bordered="false">{{ $t('common.status.disabled') }}</a-tag>
            <a-tag v-if="existing.has(record.id)" color="blue" :bordered="false">{{ $t('wms.checkPicker.inItems') }}</a-tag>
          </div>
          <div class="sub-text">{{ record.spec }}</div>
        </template>
        <template v-else-if="column.key === 'category'">
          <DictTag type="wms_material_category" :value="record.category" />
        </template>
        <template v-else-if="column.key === 'brand'">
          <DictTag type="wms_material_brand" :value="record.brand" />
        </template>
        <template v-else-if="column.key === 'qty'">
          <span :class="{ 'sub-text': !Number(record.qty) }">{{ formatQty(record.qty) }} {{ record.unit }}</span>
        </template>
        <template v-else-if="column.key === 'lastChangeTime'">{{ record.lastChangeTime || '-' }}</template>
        <template v-else-if="column.key === 'lastPurchaseDate'">{{ record.lastPurchaseDate || '-' }}</template>
        <template v-else-if="column.key === 'lastSalesDate'">{{ record.lastSalesDate || '-' }}</template>
      </template>
    </a-table>

    <template #footer>
      <div class="picker-footer">
        <span class="picker-tip">{{ $t('common.table.selected', { n: selected.size }) }}</span>
        <a-button @click="close">{{ $t('common.action.cancel') }}</a-button>
        <a-button :disabled="!pagination.total" :loading="addingAll" @click="addAll">
          {{ $t('wms.checkPicker.addAll', { n: pagination.total }) }}
        </a-button>
        <a-button type="primary" :disabled="!selected.size" @click="submit([...selected.keys()])">
          {{ $t('wms.checkPicker.addSelected', { n: selected.size }) }}
        </a-button>
      </div>
    </template>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import LocationSelect from './LocationSelect.vue'
import MaterialCategorySelect from './MaterialCategorySelect.vue'
import { pageCheckMaterials } from '@/api/wms/check'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { STATUS_DISABLED } from '@/utils/constants'
import { t } from '@/locales'
import { toOptions } from '@/utils/dict'
import { changePresets, changeRanges, fetchAllMaterialIds, MAX_CHECK_LINES } from './checkMaterials'
import { formatQty } from './wms'

/**
 * 盘点单「选择商品」：数据来源是物料资料（不是库存），显示盘点仓库的当前库存（为 0 也列出）；
 * 勾选「变动日期」时只列出该日期范围内在盘点仓库有过库存变动（出入库、调拨、组装拆卸等，不含盘点）的物料，即动态盘点。
 * 可跨页勾选，或添加全部符合条件的物料；确认后发出 pick(物料 ID 列表)
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

const columns = [
  { title: t('wms.common.material'), key: 'material', width: 280, fixed: 'left' },
  { title: t('wms.inout.category'), key: 'category', width: 130 },
  { title: t('pricing.goods.brand'), key: 'brand', width: 100 },
  { title: t('wms.checkPicker.qty'), key: 'qty', width: 120, align: 'right' },
  { title: t('wms.checkPicker.lastChange'), key: 'lastChangeTime', width: 160 },
  { title: t('wms.checkPicker.lastPurchase'), key: 'lastPurchaseDate', width: 110 },
  { title: t('wms.checkPicker.lastSales'), key: 'lastSalesDate', width: 110 }
]

const emptyQuery = () => ({
  changed: false,
  changeRange: changeRanges()[0].range,
  keyword: undefined,
  category: undefined,
  brand: undefined,
  locationId: undefined,
  purchaseRange: undefined,
  salesRange: undefined,
  status: undefined,
  inStock: false
})
const query = reactive(emptyQuery())

/** 查询参数：日期范围拆分为起止，未勾选「变动日期」时不按变动筛选 */
function toParams(q) {
  const { changeRange, purchaseRange, salesRange, changed, inStock, ...rest } = q
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
    inStock: inStock || undefined
  }
}

const { list, loading, pagination, search, onTableChange } = useTable(pageCheckMaterials, query, { params: toParams })

// 跨页保留勾选
const selected = ref(new Map())
const rowSelection = computed(() => ({
  selectedRowKeys: [...selected.value.keys()],
  onSelect: (record, checked) => toggle([record], checked),
  onSelectAll: (checked, _rows, changed) => toggle(changed, checked)
}))
function toggle(rows, checked) {
  const map = new Map(selected.value)
  rows.forEach((r) => (checked ? map.set(r.id, r) : map.delete(r.id)))
  selected.value = map
}

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

function submit(ids) {
  emit('pick', ids)
  close()
}

function close() {
  emit('update:open', false)
}
</script>

<style scoped>
.full {
  width: 100%;
}
.picker-form :deep(.ant-form-item) {
  margin-bottom: 8px;
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
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
