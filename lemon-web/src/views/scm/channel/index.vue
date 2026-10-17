<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-input-search v-model:value="query.keyword" :placeholder="$t('scm.channel.searchPlaceholder')" allow-clear class="filter-wide" @search="search" />
        <PeriodPicker v-model:value="query.period" :placeholder="$t('scm.channel.allMonths')" allow-clear @update:value="search" />
        <span class="sub-text">{{ $t('scm.channel.tip') }}</span>
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
            <a-button type="link" size="small" @click="openDetail(record.id)">{{ $t('common.action.view') }}</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <SubPage :open="detailOpen" :title="detail ? $t('scm.channel.detailTitle', { name: detail.customerName, period: periodText(detail.period) }) : ''" width="min(900px, 100vw)"
      @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('scm.channel.dealer')">{{ detail.customerName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.channel.region')">{{ detail.region || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.channel.reported')">{{ detail.reportTime }} {{ detail.reportByName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.channel.monthSales')">{{ formatQty(detail.sellTotal) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.channel.monthEndStock')">{{ formatQty(detail.stockTotal) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.description')">{{ detail.remark || '-' }}</a-descriptions-item>
        </a-descriptions>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}<div class="sub-text">{{ record.spec }}</div></template>
            <template v-else-if="column.key === 'sell'">{{ formatQty(record.sellQty) }} {{ record.unit }}</template>
            <template v-else-if="column.key === 'stock'">{{ formatQty(record.stockQty) }} {{ record.unit }}</template>
            <template v-else-if="column.key === 'cover'">
              <span v-if="Number(record.sellQty) > 0">{{ $t('scm.channel.months', { n: (Number(record.stockQty) / Number(record.sellQty)).toFixed(1) }) }}</span>
              <span v-else class="sub-text">-</span>
            </template>
          </template>
        </a-table>
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { onMounted, reactive, ref } from 'vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import { getChannelReport, pageChannelReport } from '@/api/scm/dealer'
import { periodText } from '@/components/fin/fin'
import { formatQty } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'

const { t } = useI18n()

/** 渠道库存：经销商按月上报的销量和库存，库存可售月数 = 月末库存 / 当月销量 */
const columns = [
  { title: t('scm.channel.dealer'), key: 'dealer', width: 240 },
  { title: t('scm.channel.month'), key: 'period', width: 120 },
  { title: t('scm.channel.itemCount'), dataIndex: 'itemCount', width: 100, align: 'right' },
  { title: t('scm.channel.monthSales'), key: 'sell', width: 130, align: 'right' },
  { title: t('scm.channel.monthEndStock'), key: 'stock', width: 140, align: 'right' },
  { title: t('scm.channel.reported'), key: 'time', width: 180 },
  { title: t('common.action.operation'), key: 'action', width: 90, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'scm-channel')
const query = reactive({ keyword: undefined, period: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageChannelReport, query)
onMounted(load)

const itemColumns = [
  { title: t('scm.channel.product'), key: 'material' },
  { title: t('scm.channel.monthSales'), key: 'sell', width: 140, align: 'right' },
  { title: t('scm.channel.monthEndStock'), key: 'stock', width: 150, align: 'right' },
  { title: t('scm.channel.cover'), key: 'cover', width: 120, align: 'right' }
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
