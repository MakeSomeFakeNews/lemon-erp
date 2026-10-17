<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" :placeholder="$t('fin.statement.allStatus')" allow-clear :options="toOptions(dict.fin_statement_status)" class="filter" @change="search" />
        <PartnerSelect v-model:value="query.supplierId" type="supplier" allow-clear :placeholder="$t('fin.statement.supplier')" class="filter-wide" @update:value="search" />
        <a-input-search v-model:value="query.keyword" :placeholder="$t('fin.statement.keywordPh')" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <a-button v-perm="'fin:statement:add'" type="primary" @click="openGenerate">
            <template #icon><PlusOutlined /></template>{{ $t('fin.statement.generate') }}
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
        :scroll="{ x: 1100, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'statementNo'">
            <a @click="openDetail(record.id)">{{ record.statementNo }}</a>
          </template>
          <template v-else-if="column.key === 'period'">{{ record.beginDate }} ~ {{ record.endDate }}</template>
          <template v-else-if="column.key === 'amount'">
            ¥{{ formatMoney(record.amount) }}
            <div class="sub-text">{{ $t('fin.statement.billCount', { n: record.billCount }) }}</div>
          </template>
          <template v-else-if="column.key === 'invoice'">
            <template v-if="record.invoiceNo">
              {{ record.invoiceNo }}
              <div class="sub-text">¥{{ formatMoney(record.invoiceAmount) }} · {{ record.invoiceDate }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="fin_statement_status" :value="record.status" />
            <a-tooltip v-if="record.supplierRemark" :title="record.supplierRemark"><div class="sub-text ellipsis">{{ record.supplierRemark }}</div></a-tooltip>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">{{ $t('common.action.view') }}</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="generateOpen" :title="$t('fin.statement.generate')" :confirm-loading="saving" @ok="submitGenerate">
      <a-alert type="info" show-icon class="block" :message="$t('fin.statement.generateTip')" />
      <a-form layout="vertical">
        <a-form-item :label="$t('fin.statement.supplier')" required>
          <PartnerSelect v-model:value="gen.supplierId" type="supplier" />
        </a-form-item>
        <a-form-item :label="$t('fin.statement.period')" required>
          <a-range-picker v-model:value="gen.range" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item :label="$t('common.field.remark')">
          <a-input v-model:value="gen.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>

    <SubPage :open="detailOpen" :title="detail ? $t('fin.statement.detailTitle', { no: detail.statementNo }) : ''" width="min(980px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space v-if="['DRAFT', 'DISPUTED'].includes(detail.status)">
          <span v-perm="'fin:statement:remove'">
            <a-popconfirm :title="$t('fin.statement.confirmDelete')" @confirm="remove"><a-button danger>{{ $t('common.action.delete') }}</a-button></a-popconfirm>
          </span>
          <span v-perm="'fin:statement:edit'">
            <a-popconfirm :title="$t('fin.statement.confirmSend')" @confirm="send"><a-button type="primary">{{ $t('fin.statement.send') }}</a-button></a-popconfirm>
          </span>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('fin.statement.supplier')">{{ detail.supplierCode }} {{ detail.supplierName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.statement.period')">{{ detail.beginDate }} ~ {{ detail.endDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="fin_statement_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('fin.statement.amount')"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item :label="$t('fin.statement.confirmTime')">{{ detail.confirmTime || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.statement.invoice')">
            <template v-if="detail.invoiceNo">{{ detail.invoiceNo }} ¥{{ formatMoney(detail.invoiceAmount) }} {{ detail.invoiceDate }}</template>
            <span v-else>-</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.supplierRemark" :label="$t('fin.statement.supplierRemark')" :span="3">{{ detail.supplierRemark }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <StatementLines :lines="detail.lines" />
        <div class="section-title">{{ $t('fin.statement.attachments') }}</div>
        <AttachmentPanel biz-type="fin_statement" :biz-id="detail.id" :editable="userStore.hasPerm('fin:statement:edit')" />
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { PlusOutlined } from '@ant-design/icons-vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import StatementLines from '@/components/fin/StatementLines.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { deleteStatement, generateStatement, getStatement, pageStatement, sendStatement } from '@/api/fin'
import { formatMoney } from '@/components/fin/fin'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { useUserStore } from '@/store/user'

/** 供应商对账：汇总期间内的应付单生成对账单，发送后供应商在门户确认并填写发票 */
const { t } = useI18n()
const dict = useDict('fin_statement_status')
const userStore = useUserStore()

const columns = [
  { title: t('fin.statement.statementNo'), key: 'statementNo', width: 180 },
  { title: t('fin.statement.supplier'), dataIndex: 'supplierName', width: 200, ellipsis: true },
  { title: t('fin.statement.period'), key: 'period', width: 200 },
  { title: t('fin.statement.amount'), key: 'amount', width: 160, align: 'right' },
  { title: t('fin.statement.invoice'), key: 'invoice', width: 180 },
  { title: t('common.field.status'), key: 'status', width: 130 },
  { title: t('common.action.operation'), key: 'action', width: 100, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'fin-statement')
const query = reactive({ status: undefined, supplierId: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageStatement, query)
onMounted(load)

const saving = ref(false)
const generateOpen = ref(false)
const gen = ref({})
function openGenerate() {
  const last = dayjs().subtract(1, 'month')
  gen.value = { supplierId: undefined, range: [last.startOf('month').format('YYYY-MM-DD'), last.endOf('month').format('YYYY-MM-DD')], remark: '' }
  generateOpen.value = true
}
async function submitGenerate() {
  if (!gen.value.supplierId || !gen.value.range) return message.warning(t('fin.statement.selectRequired'))
  saving.value = true
  try {
    const id = await generateStatement({ supplierId: gen.value.supplierId, beginDate: gen.value.range[0], endDate: gen.value.range[1], remark: gen.value.remark || null })
    message.success(t('fin.statement.generated'))
    generateOpen.value = false
    load()
    openDetail(id)
  } finally {
    saving.value = false
  }
}

const detailOpen = ref(false)
const detail = ref()
async function openDetail(id) {
  detail.value = await getStatement(id)
  detailOpen.value = true
}
async function send() {
  await sendStatement(detail.value.id)
  message.success(t('fin.statement.sent'))
  detail.value = await getStatement(detail.value.id)
  load()
}
async function remove() {
  await deleteStatement(detail.value.id)
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
.section-title {
  margin: 16px 0 8px;
  font-weight: 600;
}
</style>
