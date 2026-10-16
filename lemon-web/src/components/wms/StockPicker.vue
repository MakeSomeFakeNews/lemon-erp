<template>
  <a-modal
    :open="open"
    :title="title"
    width="min(960px, 100vw)"
    ok-text="添加到明细"
    :ok-button-props="{ disabled: !selected.size }"
    destroy-on-close
    @ok="submit"
    @cancel="emit('update:open', false)"
  >
    <div class="picker-search">
      <a-input-search
        v-model:value="query.keyword"
        placeholder="物料编码或名称"
        allow-clear
        class="picker-input"
        @search="search"
      />
      <a-input-search v-model:value="query.batchNo" placeholder="批次号" allow-clear class="picker-input" @search="search" />
      <LocationSelect v-model:value="query.locationId" :warehouse-id="warehouseId" placeholder="全部库位" class="picker-input" />
      <span class="picker-tip">已选 {{ selected.size }} 项</span>
    </div>
    <a-table
      row-key="id"
      size="small"
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :row-selection="rowSelection"
      :scroll="{ x: 820, y: 400 }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'material'">
          <div>{{ record.materialCode }} {{ record.materialName }}</div>
          <div class="sub-text">{{ record.spec }}</div>
        </template>
        <template v-else-if="column.key === 'location'">{{ locationText(record.locationCode) }}</template>
        <template v-else-if="column.key === 'batchNo'">{{ record.batchNo || '-' }}</template>
        <template v-else-if="column.key === 'qty'">
          {{ formatQty(availableQty(record)) }} {{ record.unit }}
          <div v-if="Number(record.frozenQty)" class="sub-text">冻结 {{ formatQty(record.frozenQty) }}</div>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import LocationSelect from './LocationSelect.vue'
import { pageStock } from '@/api/wms/stock'
import { useTable } from '@/composables/useTable'
import { availableQty, formatQty, locationText } from './wms'

/**
 * 从仓库现有库存中选择（出库、调拨），可跨页多选；确认后返回选中的库存行
 */
const props = defineProps({
  open: Boolean,
  warehouseId: Number,
  title: { type: String, default: '选择库存' }
})
const emit = defineEmits(['update:open', 'pick'])

const columns = [
  { title: '物料', key: 'material', width: 260 },
  { title: '库位', key: 'location', width: 120 },
  { title: '批次', key: 'batchNo', width: 140 },
  { title: '有效期至', dataIndex: 'expiryDate', width: 120 },
  { title: '可用数量', key: 'qty', width: 130, align: 'right' }
]

const query = reactive({ keyword: undefined, batchNo: undefined, locationId: undefined })
const { list, loading, pagination, load, search, onTableChange } = useTable(pageStock, query, {
  params: (q) => ({ ...q, warehouseId: props.warehouseId })
})

// 跨页保留选中行
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

watch(
  () => props.open,
  (open) => {
    if (!open) return
    selected.value = new Map()
    Object.assign(query, { keyword: undefined, batchNo: undefined, locationId: undefined })
    search()
  }
)

watch(() => query.locationId, () => props.open && search())

function submit() {
  emit('pick', [...selected.value.values()])
  emit('update:open', false)
}

defineExpose({ load })
</script>

<style scoped>
.picker-search {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.picker-input {
  width: 200px;
}
.picker-tip {
  margin-left: auto;
  color: var(--app-text-2);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
