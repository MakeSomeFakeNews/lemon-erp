<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('pm.common.project')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('pm.common.codeOrName')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.pm_project_status)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :exporter="exportAll"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="'pm:accounting:close'" type="primary" @click="closeOpen = true">
          <template #icon><CarryOutOutlined /></template>{{ $t('pm.accounting.close') }}
        </a-button>
        <span class="sub-text">{{ $t('pm.accounting.tip') }}</span>
      </TableToolbar>
      <a-table
        row-key="projectId"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1700, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="openDetail(record)">{{ record.name }}</a>
            <div class="sub-text">{{ record.code }} · {{ record.customerName || $t('pm.common.internal') }}</div>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="pm_project_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'progress'">
            <a-progress :percent="Number(record.progress)" size="small" class="bar" />
            <div class="sub-text">{{ $t('pm.detail.planPercent', { p: Number(record.plannedRate) }) }}</div>
          </template>
          <template v-else-if="column.key === 'contract'">
            <div>{{ formatMoney(record.contractAmount) }}</div>
            <div class="sub-text">{{ $t('pm.accounting.exclTax') }} {{ formatMoney(record.contractNet) }}</div>
          </template>
          <template v-else-if="column.key === 'revenue'">{{ formatMoney(record.revenue) }}</template>
          <template v-else-if="column.key === 'cost'">
            <div :class="{ danger: overBudget(record) }">{{ formatMoney(record.actualCost) }}</div>
            <div class="sub-text">{{ $t('pm.common.budget') }} {{ Number(record.budget) ? formatMoney(record.budget) : $t('pm.project.noBudget') }}</div>
          </template>
          <template v-else-if="column.key === 'profit'">
            <div :class="{ danger: Number(record.grossProfit) < 0 }">{{ formatMoney(record.grossProfit) }}</div>
            <div class="sub-text">{{ record.grossMargin == null ? '-' : `${record.grossMargin}%` }}</div>
          </template>
          <template v-else-if="column.key === 'billing'">
            <div>{{ formatMoney(record.billed) }}</div>
            <div class="sub-text">{{ $t('pm.common.received') }} {{ formatMoney(record.received) }}</div>
          </template>
          <template v-else-if="column.key === 'spi'">
            <a-tag :color="indexTag(record.spi).color" :bordered="false">{{ indexTag(record.spi).text }}</a-tag>
          </template>
          <template v-else-if="column.key === 'cpi'">
            <a-tag :color="indexTag(record.cpi).color" :bordered="false">{{ indexTag(record.cpi).text }}</a-tag>
          </template>
          <template v-else-if="column.key === 'eac'">{{ record.eac == null ? '-' : formatMoney(record.eac) }}</template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record)">
              <template #icon><EyeOutlined /></template>{{ $t('pm.accounting.view') }}
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <AccountingDetail v-model:open="detailOpen" :project-id="detailId" @changed="load" />
    <a-modal v-model:open="closeOpen" :title="$t('pm.accounting.close')" :confirm-loading="closing" @ok="submitClose">
      <a-alert
        type="info"
        show-icon
        class="close-tip"
        :message="$t('pm.accounting.closeTip')"
      />
      <span>{{ $t('pm.accounting.closePeriod') }}</span>
      <PeriodPicker v-model:value="closePeriod" />
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { CarryOutOutlined, EyeOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import { currentPeriod } from '@/components/fin/fin'
import AccountingDetail from '@/components/pm/AccountingDetail.vue'
import { closeAccounting, pageAccounting } from '@/api/pm'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { formatMoney } from '@/components/scm/scm'
import { indexTag, num } from '@/components/pm/pm'
import { t } from '@/locales'

const dict = useDict('pm_project_status')

const columns = [
  { title: t('pm.common.project'), key: 'name', width: 220, fixed: 'left' },
  { title: t('common.field.status'), key: 'status', width: 100, dict: 'pm_project_status' },
  { title: t('pm.acc.completion'), key: 'progress', width: 160 },
  { title: t('pm.project.contract'), key: 'contract', width: 150, align: 'right' },
  { title: t('pm.acc.revenue'), key: 'revenue', width: 160, align: 'right' },
  { title: t('pm.acc.actualCost'), key: 'cost', width: 150, align: 'right' },
  { title: t('pm.acc.profitMargin'), key: 'profit', width: 170, align: 'right' },
  { title: t('pm.accounting.billedReceived'), key: 'billing', width: 160, align: 'right' },
  { title: 'SPI', key: 'spi', width: 80 },
  { title: 'CPI', key: 'cpi', width: 80 },
  { title: t('pm.acc.eac'), key: 'eac', width: 130, align: 'right' },
  { title: t('common.action.operation'), key: 'action', width: 110, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'pm-accounting')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageAccounting, query)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
onMounted(load)

const overBudget = (r) => num(r.budget) > 0 && num(r.actualCost) > num(r.budget)

const detailOpen = ref(false)
const detailId = ref()
function openDetail(record) {
  detailId.value = record.projectId
  detailOpen.value = true
}

// 项目月结
const closeOpen = ref(false)
const closing = ref(false)
const closePeriod = ref(currentPeriod())
async function submitClose() {
  closing.value = true
  try {
    const count = await closeAccounting(closePeriod.value)
    message.success(count ? t('pm.accounting.closeDone', { count }) : t('pm.accounting.closeNothing'))
    closeOpen.value = false
    load()
  } finally {
    closing.value = false
  }
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.danger {
  color: #f5222d;
}
.bar {
  margin: 0;
}
.close-tip {
  margin-bottom: 12px;
}
</style>
