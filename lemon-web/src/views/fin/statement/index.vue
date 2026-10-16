<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" placeholder="全部状态" allow-clear :options="toOptions(dict.fin_statement_status)" class="filter" @change="search" />
        <PartnerSelect v-model:value="query.supplierId" type="supplier" allow-clear placeholder="供应商" class="filter-wide" @update:value="search" />
        <a-input-search v-model:value="query.keyword" placeholder="对账单号、供应商" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <a-button v-perm="'fin:statement:add'" type="primary" @click="openGenerate">
            <template #icon><PlusOutlined /></template>生成对账单
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
            <div class="sub-text">{{ record.billCount }} 张应付单</div>
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
            <a-button type="link" size="small" @click="openDetail(record.id)">查看</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="generateOpen" title="生成对账单" :confirm-loading="saving" @ok="submitGenerate">
      <a-alert type="info" show-icon class="block" message="汇总供应商在期间内还没有对过账的应付单（采购入库、退货、质量索赔等），生成后发送给供应商在门户确认" />
      <a-form layout="vertical">
        <a-form-item label="供应商" required>
          <PartnerSelect v-model:value="gen.supplierId" type="supplier" />
        </a-form-item>
        <a-form-item label="对账期间" required>
          <a-range-picker v-model:value="gen.range" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="gen.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>

    <SubPage :open="detailOpen" :title="detail ? `对账单 ${detail.statementNo}` : ''" width="min(980px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space v-if="['DRAFT', 'DISPUTED'].includes(detail.status)">
          <span v-perm="'fin:statement:remove'">
            <a-popconfirm title="删除对账单？其中的应付单可以重新对账" @confirm="remove"><a-button danger>删除</a-button></a-popconfirm>
          </span>
          <span v-perm="'fin:statement:edit'">
            <a-popconfirm title="发送给供应商在门户确认？" @confirm="send"><a-button type="primary">发送供应商</a-button></a-popconfirm>
          </span>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item label="供应商">{{ detail.supplierCode }} {{ detail.supplierName }}</a-descriptions-item>
          <a-descriptions-item label="对账期间">{{ detail.beginDate }} ~ {{ detail.endDate }}</a-descriptions-item>
          <a-descriptions-item label="状态"><DictTag type="fin_statement_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item label="对账金额"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item label="供应商确认">{{ detail.confirmTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="发票">
            <template v-if="detail.invoiceNo">{{ detail.invoiceNo }} ¥{{ formatMoney(detail.invoiceAmount) }} {{ detail.invoiceDate }}</template>
            <span v-else>-</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.supplierRemark" label="供应商意见" :span="3">{{ detail.supplierRemark }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="备注" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <StatementLines :lines="detail.lines" />
        <div class="section-title">发票及附件</div>
        <AttachmentPanel biz-type="fin_statement" :biz-id="detail.id" :editable="userStore.hasPerm('fin:statement:edit')" />
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
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
const dict = useDict('fin_statement_status')
const userStore = useUserStore()

const columns = [
  { title: '对账单号', key: 'statementNo', width: 180 },
  { title: '供应商', dataIndex: 'supplierName', width: 200, ellipsis: true },
  { title: '对账期间', key: 'period', width: 200 },
  { title: '对账金额', key: 'amount', width: 150, align: 'right' },
  { title: '发票', key: 'invoice', width: 180 },
  { title: '状态', key: 'status', width: 120 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
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
  if (!gen.value.supplierId || !gen.value.range) return message.warning('请选择供应商和对账期间')
  saving.value = true
  try {
    const id = await generateStatement({ supplierId: gen.value.supplierId, beginDate: gen.value.range[0], endDate: gen.value.range[1], remark: gen.value.remark || null })
    message.success('已生成对账单')
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
  message.success('已发送，供应商可在门户确认')
  detail.value = await getStatement(detail.value.id)
  load()
}
async function remove() {
  await deleteStatement(detail.value.id)
  message.success('已删除')
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
