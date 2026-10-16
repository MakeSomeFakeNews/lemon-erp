<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="项目" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="编号或名称" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.pm_project_status)" />
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
          <template #icon><CarryOutOutlined /></template>项目月结
        </a-button>
        <span class="sub-text">收入按完工百分比确认：不含税合同额 × 完工进度；成本 = 已审批工时的人工成本 + 费用（含领料）</span>
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
            <div class="sub-text">{{ record.code }} · {{ record.customerName || '内部项目' }}</div>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="pm_project_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'progress'">
            <a-progress :percent="Number(record.progress)" size="small" class="bar" />
            <div class="sub-text">计划 {{ Number(record.plannedRate) }}%</div>
          </template>
          <template v-else-if="column.key === 'contract'">
            <div>{{ formatMoney(record.contractAmount) }}</div>
            <div class="sub-text">不含税 {{ formatMoney(record.contractNet) }}</div>
          </template>
          <template v-else-if="column.key === 'revenue'">{{ formatMoney(record.revenue) }}</template>
          <template v-else-if="column.key === 'cost'">
            <div :class="{ danger: overBudget(record) }">{{ formatMoney(record.actualCost) }}</div>
            <div class="sub-text">预算 {{ Number(record.budget) ? formatMoney(record.budget) : '未编制' }}</div>
          </template>
          <template v-else-if="column.key === 'profit'">
            <div :class="{ danger: Number(record.grossProfit) < 0 }">{{ formatMoney(record.grossProfit) }}</div>
            <div class="sub-text">{{ record.grossMargin == null ? '-' : `${record.grossMargin}%` }}</div>
          </template>
          <template v-else-if="column.key === 'billing'">
            <div>{{ formatMoney(record.billed) }}</div>
            <div class="sub-text">已收 {{ formatMoney(record.received) }}</div>
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
              <template #icon><EyeOutlined /></template>核算
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <AccountingDetail v-model:open="detailOpen" :project-id="detailId" @changed="load" />
    <a-modal v-model:open="closeOpen" title="项目月结" :confirm-loading="closing" @ok="submitClose">
      <a-alert
        type="info"
        show-icon
        class="close-tip"
        message="按项目生成凭证（未审核）：已审批工时的人工成本、已审批的费用计入合同履约成本；有合同金额的项目按完工进度确认收入（合同负债 → 主营业务收入），并结转成本。每次只处理未入账的部分，删除月结凭证后可以重新月结。"
      />
      <span>月结期间：</span>
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

const dict = useDict('pm_project_status')

const columns = [
  { title: '项目', key: 'name', width: 220, fixed: 'left' },
  { title: '状态', key: 'status', width: 90, dict: 'pm_project_status' },
  { title: '完工进度', key: 'progress', width: 160 },
  { title: '合同额', key: 'contract', width: 150, align: 'right' },
  { title: '已确认收入', key: 'revenue', width: 130, align: 'right' },
  { title: '实际成本', key: 'cost', width: 150, align: 'right' },
  { title: '毛利 / 毛利率', key: 'profit', width: 140, align: 'right' },
  { title: '已开票 / 已收款', key: 'billing', width: 150, align: 'right' },
  { title: 'SPI', key: 'spi', width: 80 },
  { title: 'CPI', key: 'cpi', width: 80 },
  { title: '完工估算 EAC', key: 'eac', width: 130, align: 'right' },
  { title: '操作', key: 'action', width: 90, fixed: 'right' }
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
    message.success(count ? `已生成 ${count} 张项目凭证（未审核）` : '没有需要入账的工时、费用或收入')
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
