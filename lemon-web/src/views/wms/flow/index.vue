<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('wms.flow.bizType')" name="bizType">
          <a-select v-model:value="query.bizType" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.wms_biz_type)" />
        </a-form-item>
        <a-form-item :label="$t('wms.common.material')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('wms.flow.codeOrName')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('wms.common.warehouse')" name="warehouseId">
          <WarehouseSelect v-model:value="query.warehouseId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('wms.common.docNo')" name="docNo">
          <a-input v-model:value="query.docNo" :placeholder="$t('wms.flow.fullDocNo')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('wms.common.batchNo')" name="batchNo">
          <a-input v-model:value="query.batchNo" :placeholder="$t('wms.flow.fullBatchNo')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.date')" name="range">
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
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()

const dict = useDict('wms_biz_type')

const columns = [
  { title: t('wms.flow.time'), dataIndex: 'createTime', width: 170 },
  { title: t('wms.flow.bizType'), key: 'bizType', width: 120, dict: 'wms_biz_type' },
  { title: t('wms.common.docNo'), dataIndex: 'docNo', width: 160 },
  { title: t('wms.common.material'), key: 'material', width: 220 },
  { title: t('wms.common.warehouse'), dataIndex: 'warehouseName', width: 130 },
  { title: t('wms.common.location'), key: 'location', width: 100 },
  { title: t('wms.common.batch'), key: 'batchNo', width: 130 },
  { title: t('wms.flow.changeQty'), key: 'qty', width: 120, align: 'right' },
  { title: t('wms.flow.operator'), dataIndex: 'createByName', width: 100 }
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
