<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.date')" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" :allow-clear="false" />
        </a-form-item>
        <a-form-item :label="$t('wms.common.warehouse')" name="warehouseId">
          <WarehouseSelect v-model:value="query.warehouseId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('wms.common.material')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('wms.flow.codeOrName')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('wms.inout.category')" name="category">
          <a-select v-model:value="query.category" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.wms_material_category)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll"
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :columns="COLUMNS"
        @refresh="load"
      >
        <span class="sub-text">{{ $t('wms.inout.tip') }}</span>
      </TableToolbar>

      <a-table
        row-key="materialId"
        :size="size"
        bordered
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1950, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            <div>{{ record.materialName }}</div>
            <div class="sub-text">{{ record.materialCode }}</div>
          </template>
          <template v-else-if="column.key === 'category'">
            <DictTag type="wms_material_category" :value="record.category" />
          </template>
          <template v-else-if="column.qty">
            <span :class="{ zero: !Number(record[column.key]), bold: column.bold, neg: Number(record[column.key]) < 0 }">
              {{ Number(record[column.key]) ? formatQty(record[column.key]) : '-' }}
            </span>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { pageInOut } from '@/api/wms/stock'
import { formatQty } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

const { t } = useI18n()

/** 收发存汇总：日期范围内各物料的期初、各类入库、各类出库和期末数量 */
const dict = useDict('wms_material_category')

const q = (title, key, extra = {}) => ({ title, key, dataIndex: key, width: 120, align: 'right', qty: true, ...extra })
const COLUMNS = [
  { title: t('wms.common.material'), key: 'material', width: 200, fixed: 'left' },
  { title: t('wms.inout.spec'), dataIndex: 'spec', width: 130, ellipsis: true },
  { title: t('wms.inout.category'), key: 'category', width: 100, dict: 'wms_material_category' },
  { title: t('common.field.unit'), dataIndex: 'unit', width: 60 },
  q(t('wms.inout.openQty'), 'openQty', { bold: true }),
  q(t('wms.inout.purchaseIn'), 'purchaseIn'),
  q(t('wms.inout.productionIn'), 'productionIn'),
  q(t('wms.inout.salesReturn'), 'salesReturn'),
  q(t('wms.inout.otherIn'), 'otherIn'),
  q(t('wms.inout.salesOut'), 'salesOut'),
  q(t('wms.inout.productionOut'), 'productionOut'),
  q(t('wms.inout.purchaseReturn'), 'purchaseReturn'),
  q(t('wms.inout.otherOut'), 'otherOut'),
  q(t('wms.inout.closeQty'), 'closeQty', { bold: true, fixed: 'right' })
]
const { checked, visibleColumns } = useColumns(COLUMNS, 'wms-inout')
const showSearch = ref(true)

const emptyQuery = () => ({
  range: [dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')],
  warehouseId: undefined,
  keyword: undefined,
  category: undefined
})
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageInOut, query, {
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
.zero {
  color: var(--app-text-3);
}
.bold {
  font-weight: 500;
}
.neg {
  color: var(--status-critical);
}
</style>
