<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" :placeholder="$t('fin.customerStatement.allStatus')" allow-clear :options="toOptions(dict.fin_statement_status)" class="filter" @change="search" />
        <PartnerSelect v-model:value="query.customerId" type="customer" allow-clear :placeholder="$t('fin.customerStatement.customer')" class="filter-wide" @update:value="search" />
        <a-input-search v-model:value="query.keyword" :placeholder="$t('fin.customerStatement.keywordPh')" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <a-button v-perm="'fin:customerStatement:add'" type="primary" @click="openGenerate">
            <template #icon><PlusOutlined /></template>{{ $t('fin.customerStatement.generate') }}
          </a-button>
        </template>
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
          <template v-if="column.key === 'statementNo'">
            <a @click="openDetail(record.id)">{{ record.statementNo }}</a>
          </template>
          <template v-else-if="column.key === 'customer'">
            {{ record.customerName }}
            <div class="sub-text">{{ record.customerCode }}</div>
          </template>
          <template v-else-if="column.key === 'period'">{{ record.beginDate }} ~ {{ record.endDate }}</template>
          <template v-else-if="['opening', 'billAmount', 'receiptAmount', 'closing'].includes(column.key)">
            <span :class="{ bold: column.key === 'closing' }">{{ formatMoney(record[column.key]) }}</span>
            <div v-if="column.key === 'receiptAmount' && Number(record.discountAmount)" class="sub-text">
              {{ $t('fin.customerStatement.discountShort', { amount: formatMoney(record.discountAmount) }) }}
            </div>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="fin_statement_status" :value="record.status" />
            <a-tooltip v-if="record.customerRemark" :title="record.customerRemark"><div class="sub-text ellipsis">{{ record.customerRemark }}</div></a-tooltip>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="[{ type: 'view', onClick: () => openDetail(record.id) }]" />
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="generateOpen" :title="$t('fin.customerStatement.generate')" :confirm-loading="saving" @ok="submitGenerate">
      <a-alert type="info" show-icon class="block" :message="$t('fin.customerStatement.generateTip')" />
      <a-form layout="vertical">
        <a-form-item :label="$t('fin.customerStatement.customer')" required>
          <PartnerSelect v-model:value="gen.customerId" type="customer" />
        </a-form-item>
        <a-form-item :label="$t('fin.customerStatement.period')" required>
          <a-range-picker v-model:value="gen.range" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item :label="$t('common.field.remark')">
          <a-input v-model:value="gen.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>

    <SubPage :open="detailOpen" :title="detail ? $t('fin.customerStatement.detailTitle', { no: detail.statementNo }) : ''" width="min(1100px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space>
          <template v-if="editable">
            <span v-perm="'fin:customerStatement:remove'">
              <a-popconfirm :title="$t('fin.customerStatement.confirmDelete')" @confirm="remove"><a-button danger>{{ $t('common.action.delete') }}</a-button></a-popconfirm>
            </span>
            <span v-perm="'fin:customerStatement:edit'">
              <a-popconfirm :title="$t('fin.customerStatement.confirmSend')" @confirm="send"><a-button type="primary">{{ $t('fin.customerStatement.send') }}</a-button></a-popconfirm>
            </span>
          </template>
          <span v-else-if="detail.status === StatementStatus.SENT" v-perm="'fin:customerStatement:edit'">
            <a-dropdown>
              <a-button>{{ $t('fin.customerStatement.register') }} <DownOutlined /></a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="agree" @click="respondRef.open(true)">{{ $t('fin.customerStatement.registerAgree') }}</a-menu-item>
                  <a-menu-item key="dispute" @click="respondRef.open(false)">{{ $t('fin.customerStatement.registerDispute') }}</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </span>
        </a-space>
      </template>
      <CustomerStatementDetail v-if="detail" :key="detail.id" :statement="detail" :lines-api="customerStatementLines"
        :attachment-editable="userStore.hasPerm('fin:customerStatement:edit')" />
    </SubPage>

    <StatementRespondModal ref="respondRef" :tip="$t('fin.customerStatement.registerTip')" :submitter="(data) => registerCustomerStatement(detail.id, data)" @done="refresh" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { DownOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import TableToolbar from '@/components/TableToolbar.vue'
import CustomerStatementDetail from '@/components/fin/CustomerStatementDetail.vue'
import StatementRespondModal from '@/components/fin/StatementRespondModal.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import {
  customerStatementLines, deleteCustomerStatement, generateCustomerStatement, getCustomerStatement, pageCustomerStatement,
  registerCustomerStatement, sendCustomerStatement
} from '@/api/fin'
import { formatMoney } from '@/components/fin/fin'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { useUserStore } from '@/store/user'

import { StatementStatus } from '@/constants/enums'
/**
 * 客户对账（issue #134）：按客户和期间生成对账单（往来对账快照：期初、本期应收、收款、折让、对冲、期末和逐笔明细），
 * 发送后客户在经销商门户确认或提出异议；没有门户账号的客户打印给对方签字盖章后，在这里登记确认结果
 */
const { t } = useI18n()
const dict = useDict('fin_statement_status')
const userStore = useUserStore()

const columns = [
  { title: t('fin.customerStatement.statementNo'), key: 'statementNo', width: 180 },
  { title: t('fin.customerStatement.customer'), key: 'customer', width: 200, ellipsis: true },
  { title: t('fin.customerStatement.period'), key: 'period', width: 210 },
  { title: t('fin.partnerStatement.opening'), key: 'opening', width: 130, align: 'right' },
  { title: t('fin.customerStatement.billAmount'), key: 'billAmount', width: 130, align: 'right' },
  { title: t('fin.customerStatement.receiptAmount'), key: 'receiptAmount', width: 140, align: 'right' },
  { title: t('fin.customerStatement.closing'), key: 'closing', width: 130, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 130 },
  actionColumn(1, [t('common.action.view')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'fin-customer-statement')
const query = reactive({ status: undefined, customerId: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageCustomerStatement, query)
onMounted(load)

const saving = ref(false)
const generateOpen = ref(false)
const gen = ref({})
function openGenerate() {
  const last = dayjs().subtract(1, 'month')
  gen.value = { customerId: undefined, range: [last.startOf('month').format('YYYY-MM-DD'), last.endOf('month').format('YYYY-MM-DD')], remark: '' }
  generateOpen.value = true
}
async function submitGenerate() {
  if (!gen.value.customerId || !gen.value.range) return message.warning(t('fin.customerStatement.selectRequired'))
  saving.value = true
  try {
    const id = await generateCustomerStatement({ customerId: gen.value.customerId, beginDate: gen.value.range[0], endDate: gen.value.range[1], remark: gen.value.remark || null })
    message.success(t('fin.customerStatement.generated'))
    generateOpen.value = false
    load()
    openDetail(id)
  } finally {
    saving.value = false
  }
}

const detailOpen = ref(false)
const detail = ref()
const respondRef = ref()
const editable = computed(() => [StatementStatus.DRAFT, StatementStatus.DISPUTED].includes(detail.value?.status))
async function openDetail(id) {
  detail.value = await getCustomerStatement(id)
  detailOpen.value = true
}
async function refresh() {
  detail.value = await getCustomerStatement(detail.value.id)
  load()
}
async function send() {
  await sendCustomerStatement(detail.value.id)
  message.success(t('fin.customerStatement.sent'))
  refresh()
}
async function remove() {
  await deleteCustomerStatement(detail.value.id)
  message.success(t('common.msg.deleted'))
  detailOpen.value = false
  load()
}
</script>

<style scoped>
.filter {
  width: 140px;
}
.filter-wide {
  width: 220px;
}
.full {
  width: 100%;
}
.block {
  margin-bottom: 16px;
}
.bold {
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.ellipsis {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
