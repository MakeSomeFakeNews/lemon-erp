<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-input-search v-model:value="query.keyword" placeholder="经销商、区域" allow-clear class="filter-wide" @search="search" />
        <PeriodPicker v-model:value="query.period" placeholder="全部月份" allow-clear @update:value="search" />
        <span class="sub-text">经销商在门户按月上报的当月销量和月末渠道库存</span>
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1000, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'dealer'">
            <a @click="openDetail(record.id)">{{ record.customerName }}</a>
            <div class="sub-text">{{ record.region || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'period'">{{ periodText(record.period) }}</template>
          <template v-else-if="column.key === 'sell'">{{ formatQty(record.sellTotal) }}</template>
          <template v-else-if="column.key === 'stock'">{{ formatQty(record.stockTotal) }}</template>
          <template v-else-if="column.key === 'time'">
            {{ record.reportTime }}
            <div class="sub-text">{{ record.reportByName }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">查看</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <SubPage :open="detailOpen" :title="detail ? `${detail.customerName} ${periodText(detail.period)} 渠道上报` : ''" width="min(900px, 100vw)"
      @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item label="经销商">{{ detail.customerName }}</a-descriptions-item>
          <a-descriptions-item label="授权区域">{{ detail.region || '-' }}</a-descriptions-item>
          <a-descriptions-item label="上报">{{ detail.reportTime }} {{ detail.reportByName }}</a-descriptions-item>
          <a-descriptions-item label="当月销量">{{ formatQty(detail.sellTotal) }}</a-descriptions-item>
          <a-descriptions-item label="月末库存">{{ formatQty(detail.stockTotal) }}</a-descriptions-item>
          <a-descriptions-item label="说明">{{ detail.remark || '-' }}</a-descriptions-item>
        </a-descriptions>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}<div class="sub-text">{{ record.spec }}</div></template>
            <template v-else-if="column.key === 'sell'">{{ formatQty(record.sellQty) }} {{ record.unit }}</template>
            <template v-else-if="column.key === 'stock'">{{ formatQty(record.stockQty) }} {{ record.unit }}</template>
            <template v-else-if="column.key === 'cover'">
              <span v-if="Number(record.sellQty) > 0">{{ (Number(record.stockQty) / Number(record.sellQty)).toFixed(1) }} 个月</span>
              <span v-else class="sub-text">-</span>
            </template>
          </template>
        </a-table>
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import { getChannelReport, pageChannelReport } from '@/api/scm/dealer'
import { periodText } from '@/components/fin/fin'
import { formatQty } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'

/** 渠道库存：经销商按月上报的销量和库存，库存可售月数 = 月末库存 / 当月销量 */
const columns = [
  { title: '经销商', key: 'dealer', width: 240 },
  { title: '月份', key: 'period', width: 120 },
  { title: '产品数', dataIndex: 'itemCount', width: 90, align: 'right' },
  { title: '当月销量', key: 'sell', width: 120, align: 'right' },
  { title: '月末库存', key: 'stock', width: 120, align: 'right' },
  { title: '上报', key: 'time', width: 180 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'scm-channel')
const query = reactive({ keyword: undefined, period: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageChannelReport, query)
onMounted(load)

const itemColumns = [
  { title: '产品', key: 'material' },
  { title: '当月销量', key: 'sell', width: 140, align: 'right' },
  { title: '月末库存', key: 'stock', width: 140, align: 'right' },
  { title: '库存可售', key: 'cover', width: 110, align: 'right' }
]
const detailOpen = ref(false)
const detail = ref()
async function openDetail(id) {
  detail.value = await getChannelReport(id)
  detailOpen.value = true
}
</script>

<style scoped>
.filter-wide {
  width: 220px;
}
.block {
  margin-bottom: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
