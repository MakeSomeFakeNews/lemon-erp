<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" :placeholder="$t('portal.statement.allStatus')" allow-clear :options="statusOptions" class="filter" @change="search" />
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 900, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'statementNo'"><a @click="openDetail(record.id)">{{ record.statementNo }}</a></template>
          <template v-else-if="column.key === 'period'">{{ record.beginDate }} ~ {{ record.endDate }}</template>
          <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}<div class="sub-text">{{ $t('portal.statement.billCount', { n: record.billCount }) }}</div></template>
          <template v-else-if="column.key === 'invoice'">
            <span v-if="record.invoiceNo">{{ record.invoiceNo }}</span>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="fin_statement_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">{{ record.status === StatementStatus.SENT ? $t('portal.statement.confirmAction') : $t('common.action.view') }}</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <SubPage :open="detailOpen" :title="detail ? $t('portal.statement.detailTitle', { no: detail.statementNo }) : ''" width="min(960px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <span v-perm="'portal:statement:confirm'">
          <a-space v-if="detail.status === StatementStatus.SENT">
            <a-button danger @click="openConfirm(false)">{{ $t('portal.statement.dispute') }}</a-button>
            <a-button type="primary" @click="openConfirm(true)">{{ $t('portal.statement.confirmStatement') }}</a-button>
          </a-space>
          <a-button v-else-if="detail.status === StatementStatus.CONFIRMED" @click="openInvoice">{{ detail.invoiceNo ? $t('portal.statement.editInvoice') : $t('portal.statement.fillInvoice') }}</a-button>
        </span>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('portal.statement.period')">{{ detail.beginDate }} ~ {{ detail.endDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('portal.statement.amount')"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="fin_statement_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('portal.statement.invoice')" :span="2">
            <template v-if="detail.invoiceNo">{{ detail.invoiceNo }} ¥{{ formatMoney(detail.invoiceAmount) }} {{ detail.invoiceDate }}</template>
            <span v-else>-</span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('portal.statement.confirmTime')">{{ detail.confirmTime || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.supplierRemark" :label="$t('portal.statement.ourRemark')" :span="3">{{ detail.supplierRemark }}</a-descriptions-item>
        </a-descriptions>
        <StatementLines :lines="detail.lines" />
        <div class="section-title">{{ $t('portal.statement.attachments') }}</div>
        <AttachmentPanel biz-type="fin_statement" :biz-id="detail.id" :editable="userStore.hasPerm('portal:statement:confirm')" />
      </template>
    </SubPage>

    <a-modal v-model:open="confirmOpen" :title="confirm.agree ? $t('portal.statement.confirmStatement') : $t('portal.statement.raiseDispute')" :confirm-loading="saving" @ok="submitConfirm">
      <a-form layout="vertical">
        <template v-if="confirm.agree">
          <a-alert type="info" show-icon class="block" :message="$t('portal.statement.confirmTip')" />
          <a-row :gutter="12">
            <a-col :span="12"><a-form-item :label="$t('portal.statement.invoiceNo')"><a-input v-model:value="confirm.invoiceNo" :maxlength="64" /></a-form-item></a-col>
            <a-col :span="12">
              <a-form-item :label="$t('portal.statement.invoiceAmount')"><a-input-number v-model:value="confirm.invoiceAmount" :min="0" :precision="2" prefix="¥" class="full" /></a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item :label="$t('portal.statement.invoiceDate')"><a-date-picker v-model:value="confirm.invoiceDate" value-format="YYYY-MM-DD" class="full" /></a-form-item>
            </a-col>
          </a-row>
        </template>
        <a-form-item :label="confirm.agree ? $t('common.field.description') : $t('portal.statement.disputeRemark')" :required="!confirm.agree">
          <a-textarea v-model:value="confirm.remark" :rows="3" :maxlength="500" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="invoiceOpen" :title="$t('portal.statement.invoiceInfo')" :confirm-loading="saving" @ok="submitInvoice">
      <a-form layout="vertical">
        <a-form-item :label="$t('portal.statement.invoiceNo')" required><a-input v-model:value="invoice.invoiceNo" :maxlength="64" /></a-form-item>
        <a-form-item :label="$t('portal.statement.invoiceAmount')" required><a-input-number v-model:value="invoice.invoiceAmount" :min="0" :precision="2" prefix="¥" class="full" /></a-form-item>
        <a-form-item :label="$t('portal.statement.invoiceDate')" required><a-date-picker v-model:value="invoice.invoiceDate" value-format="YYYY-MM-DD" class="full" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import StatementLines from '@/components/fin/StatementLines.vue'
import { confirmPortalStatement, getPortalStatement, pagePortalStatement, savePortalInvoice } from '@/api/portal'
import { formatMoney } from '@/components/fin/fin'
import { today } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { useUserStore } from '@/store/user'

import { StatementStatus } from '@/constants/enums'
const { t } = useI18n()

/** 供应商门户 - 对账单：确认或提出异议，填写发票并上传扫描件 */
const dict = useDict('fin_statement_status')
const userStore = useUserStore()
const statusOptions = computed(() => toOptions(dict.fin_statement_status).filter((o) => o.value !== StatementStatus.DRAFT))
const columns = [
  { title: t('portal.statement.statementNo'), key: 'statementNo', width: 180 },
  { title: t('portal.statement.period'), key: 'period', width: 200 },
  { title: t('portal.statement.amount'), key: 'amount', width: 160, align: 'right' },
  { title: t('portal.statement.invoice'), key: 'invoice', width: 150 },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('common.action.operation'), key: 'action', width: 90, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'portal-statement')
const query = reactive({ status: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pagePortalStatement, query)
onMounted(load)

const detailOpen = ref(false)
const detail = ref()
async function openDetail(id) {
  detail.value = await getPortalStatement(id)
  detailOpen.value = true
}
async function refresh() {
  detail.value = await getPortalStatement(detail.value.id)
  load()
}

const saving = ref(false)
const confirmOpen = ref(false)
const confirm = ref({})
function openConfirm(agree) {
  confirm.value = { agree, remark: '', invoiceNo: '', invoiceAmount: agree ? Number(detail.value.amount) : undefined, invoiceDate: today() }
  confirmOpen.value = true
}
async function submitConfirm() {
  if (!confirm.value.agree && !confirm.value.remark?.trim()) return message.warning(t('portal.statement.msgDispute'))
  const data = { ...confirm.value }
  if (!data.agree || !data.invoiceNo) Object.assign(data, { invoiceNo: null, invoiceAmount: null, invoiceDate: null })
  saving.value = true
  try {
    await confirmPortalStatement(detail.value.id, data)
    message.success(data.agree ? t('portal.statement.confirmed') : t('portal.statement.disputed'))
    confirmOpen.value = false
    refresh()
  } finally {
    saving.value = false
  }
}
const invoiceOpen = ref(false)
const invoice = ref({})
function openInvoice() {
  invoice.value = {
    invoiceNo: detail.value.invoiceNo || '',
    invoiceAmount: detail.value.invoiceAmount != null ? Number(detail.value.invoiceAmount) : Number(detail.value.amount),
    invoiceDate: detail.value.invoiceDate || today()
  }
  invoiceOpen.value = true
}
async function submitInvoice() {
  if (!invoice.value.invoiceNo || invoice.value.invoiceAmount == null || !invoice.value.invoiceDate) return message.warning(t('portal.statement.msgInvoice'))
  saving.value = true
  try {
    await savePortalInvoice(detail.value.id, invoice.value)
    message.success(t('common.msg.saved'))
    invoiceOpen.value = false
    refresh()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.filter {
  width: 140px;
}
.full {
  width: 100%;
}
.block {
  margin-bottom: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.section-title {
  margin: 16px 0 8px;
  font-weight: 600;
}
</style>
