<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" :placeholder="$t('dealer.statement.allStatus')" allow-clear :options="statusOptions" class="filter" @change="search" />
        <span class="sub-text">{{ $t('dealer.statement.tip') }}</span>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'statementNo'"><a @click="openDetail(record.id)">{{ record.statementNo }}</a></template>
          <template v-else-if="column.key === 'period'">{{ record.beginDate }} ~ {{ record.endDate }}</template>
          <template v-else-if="['opening', 'billAmount', 'receiptAmount', 'closing'].includes(column.key)">
            <span :class="{ bold: column.key === 'closing' }">{{ formatMoney(record[column.key]) }}</span>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="fin_statement_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[
                record.status === StatementStatus.SENT
                  ? { type: 'confirm', label: $t('dealer.statement.confirmAction'), onClick: () => openDetail(record.id) }
                  : { type: 'view', onClick: () => openDetail(record.id) }
              ]"
            />
          </template>
        </template>
      </a-table>
    </a-card>

    <SubPage :open="detailOpen" :title="detail ? $t('dealer.statement.detailTitle', { no: detail.statementNo }) : ''" width="min(1100px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <span v-perm="'dealer:statement:confirm'">
          <a-space v-if="detail.status === StatementStatus.SENT">
            <a-button danger @click="respondRef.open(false)">{{ $t('dealer.statement.dispute') }}</a-button>
            <a-button type="primary" @click="respondRef.open(true)">{{ $t('dealer.statement.confirmStatement') }}</a-button>
          </a-space>
        </span>
      </template>
      <CustomerStatementDetail v-if="detail" :key="detail.id" portal :statement="detail" :lines-api="dealerStatementLines"
        :attachment-editable="userStore.hasPerm('dealer:statement:confirm')" />
    </SubPage>

    <StatementRespondModal ref="respondRef" :tip="confirmTip" :submitter="(data) => confirmDealerStatement(detail.id, data)" @done="refresh" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import TableToolbar from '@/components/TableToolbar.vue'
import CustomerStatementDetail from '@/components/fin/CustomerStatementDetail.vue'
import StatementRespondModal from '@/components/fin/StatementRespondModal.vue'
import { confirmDealerStatement, dealerStatementLines, getDealerStatement, pageDealerStatement } from '@/api/scm/dealer'
import { formatMoney } from '@/components/fin/fin'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { useUserStore } from '@/store/user'

import { StatementStatus } from '@/constants/enums'
/** 经销商门户 - 对账单（issue #134）：查看公司发来的对账单和明细，确认欠款或提出异议，可导出、上传签字盖章的扫描件 */
const { t } = useI18n()
const dict = useDict('fin_statement_status')
const userStore = useUserStore()
const statusOptions = computed(() => toOptions(dict.fin_statement_status).filter((o) => o.value !== StatementStatus.DRAFT))
const columns = [
  { title: t('dealer.statement.statementNo'), key: 'statementNo', width: 180 },
  { title: t('dealer.statement.period'), key: 'period', width: 210 },
  { title: t('fin.partnerStatement.opening'), key: 'opening', width: 130, align: 'right' },
  { title: t('dealer.statement.billAmount'), key: 'billAmount', width: 130, align: 'right' },
  { title: t('dealer.statement.receiptAmount'), key: 'receiptAmount', width: 130, align: 'right' },
  { title: t('dealer.statement.closing'), key: 'closing', width: 130, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 100 },
  actionColumn(1, [t('common.action.view'), t('dealer.statement.confirmAction')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'dealer-statement')
const query = reactive({ status: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageDealerStatement, query)
onMounted(load)

const detailOpen = ref(false)
const detail = ref()
const respondRef = ref()
const confirmTip = computed(() => (detail.value ? t('dealer.statement.confirmTip', { amount: formatMoney(detail.value.closing), end: detail.value.endDate }) : ''))
async function openDetail(id) {
  detail.value = await getDealerStatement(id)
  detailOpen.value = true
}
async function refresh() {
  detail.value = await getDealerStatement(detail.value.id)
  load()
}
</script>

<style scoped>
.filter {
  width: 140px;
}
.bold {
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
