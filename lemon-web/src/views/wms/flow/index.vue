<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="业务类型" name="bizType">
          <a-select v-model:value="query.bizType" placeholder="全部" allow-clear :options="toOptions(dict.wms_biz_type)" />
        </a-form-item>
        <a-form-item label="物料" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="编码或名称" allow-clear />
        </a-form-item>
        <a-form-item label="仓库" name="warehouseId">
          <WarehouseSelect v-model:value="query.warehouseId" placeholder="全部" />
        </a-form-item>
        <a-form-item label="单据号" name="docNo">
          <a-input v-model:value="query.docNo" placeholder="完整单据号" allow-clear />
        </a-form-item>
        <a-form-item label="批次号" name="batchNo">
          <a-input v-model:value="query.batchNo" placeholder="完整批次号" allow-clear />
        </a-form-item>
        <a-form-item label="日期" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" style="width: 100%" />
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
      />
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1200, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'bizType'">
            <DictTag type="wms_biz_type" :value="record.bizType" />
          </template>
          <template v-else-if="column.key === 'material'">
            <div>{{ record.materialName }}</div>
            <div class="sub-text">{{ record.materialCode }} {{ record.spec }}</div>
          </template>
          <template v-else-if="column.key === 'location'">{{ locationText(record.locationCode) }}</template>
          <template v-else-if="column.key === 'batchNo'">{{ record.batchNo || '-' }}</template>
          <template v-else-if="column.key === 'qty'">
            <QtyDiff :value="Number(record.qty)" /> <span class="sub-text">{{ record.unit }}</span>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import QtyDiff from '@/components/wms/QtyDiff.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { pageFlow } from '@/api/wms/stock'
import { locationText } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

const dict = useDict('wms_biz_type')

const columns = [
  { title: '时间', dataIndex: 'createTime', width: 170 },
  { title: '业务类型', key: 'bizType', width: 90, dict: 'wms_biz_type' },
  { title: '单据号', dataIndex: 'docNo', width: 160 },
  { title: '物料', key: 'material', width: 220 },
  { title: '仓库', dataIndex: 'warehouseName', width: 130 },
  { title: '库位', key: 'location', width: 100 },
  { title: '批次', key: 'batchNo', width: 130 },
  { title: '变动数量', key: 'qty', width: 120, align: 'right' },
  { title: '操作人', dataIndex: 'createByName', width: 100 }
]
const { checked, visibleColumns } = useColumns(columns, 'wms-flow')
const showSearch = ref(true)

const emptyQuery = () => ({
  bizType: undefined,
  keyword: undefined,
  warehouseId: undefined,
  docNo: undefined,
  batchNo: undefined,
  range: undefined
})
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageFlow, query, {
  params: ({ range, ...rest }) => ({ ...rest, beginDate: range?.[0], endDate: range?.[1] })
})

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

onMounted(load)
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
