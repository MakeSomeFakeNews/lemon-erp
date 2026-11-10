<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" class="io-tabs">
        <a-tab-pane key="docs" :tab="$t('fin.io.docsTab')">
          <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
            <a-select v-model:value="query.direction" :placeholder="$t('fin.io.allTypes')" allow-clear class="filter" @change="search">
              <a-select-option :value="FundDirection.IN">{{ $t('fin.io.typeIn') }}</a-select-option>
              <a-select-option :value="FundDirection.OUT">{{ $t('fin.io.typeOut') }}</a-select-option>
            </a-select>
            <a-select v-model:value="query.status" :options="toOptions(dict.fin_io_status)" :placeholder="$t('fin.io.allStatus')" allow-clear class="filter" @change="search" />
            <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" @change="search" />
            <a-input-search v-model:value="query.keyword" :placeholder="$t('fin.io.keywordPh')" allow-clear class="filter-wide" @search="search" />
            <template #extra>
              <a-space v-perm="'fin:io:add'">
                <a-button type="primary" @click="formRef.openCreate(FundDirection.IN)"><template #icon><PlusOutlined /></template>{{ $t('fin.io.addIn') }}</a-button>
                <a-button type="primary" @click="formRef.openCreate(FundDirection.OUT)"><template #icon><PlusOutlined /></template>{{ $t('fin.io.addOut') }}</a-button>
              </a-space>
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
              <template v-if="column.key === 'docNo'">
                <a @click="detailRef.open(record.id)">{{ record.docNo }}</a>
              </template>
              <template v-else-if="column.key === 'direction'"><DictTag type="fin_fund_direction" :value="record.direction" /></template>
              <template v-else-if="column.key === 'amount'">
                <span :class="record.direction === FundDirection.OUT ? 'out' : 'in'">¥{{ formatMoney(record.amount) }}</span>
              </template>
              <template v-else-if="column.key === 'settleMethod'"><DictTag v-if="record.settleMethod" type="fin_settle_method" :value="record.settleMethod" /></template>
              <template v-else-if="column.key === 'status'">
                <DictTag type="fin_io_status" :value="record.status" />
                <div v-if="record.voucherNo" class="sub-text">
                  <a @click="openVoucher(record.voucherId)">{{ record.voucherNo }}</a>
                </div>
              </template>
              <template v-else-if="column.key === 'action'">
                <TableActions :actions="rowActions(record)" />
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="summary" :tab="$t('fin.io.summaryTab')">
          <IoSummary v-if="tab === 'summary'" @open="(id) => detailRef.open(id)" />
        </a-tab-pane>
        <a-tab-pane key="category" :tab="$t('fin.io.categoryTab')">
          <IoCategory v-if="tab === 'category'" />
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <IoForm ref="formRef" @saved="onSaved" />
    <IoDetail ref="detailRef" @edit="(d) => formRef.openEdit(d)" @changed="load" />
    <VoucherDetail v-model:open="voucherOpen" :voucher-id="voucherId" @changed="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import VoucherDetail from '@/components/fin/VoucherDetail.vue'
import { deleteIo, pageIo } from '@/api/fin'
import { formatMoney } from '@/components/fin/fin'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { FundDirection, IoStatus, VoucherStatus } from '@/constants/enums'
import IoCategory from './IoCategory.vue'
import IoDetail from './IoDetail.vue'
import IoForm from './IoForm.vue'
import IoSummary from './IoSummary.vue'
import { useIoActions } from './useIoActions'

/**
 * 其他收支（现金银行）：其他收入单（废料销售、佣金收入等）、费用支出单（房租、水电、差旅等），确认后生成凭证并进入出纳日记账、资金日报；
 * 按收支类别汇总与明细；收支类别维护
 */
const { t } = useI18n()
const dict = useDict('fin_fund_direction', 'fin_io_status', 'fin_settle_method')
const tab = ref('docs')

const columns = [
  { title: t('fin.io.docNo'), key: 'docNo', width: 150 },
  { title: t('fin.io.date'), dataIndex: 'docDate', width: 105 },
  { title: t('fin.io.type'), key: 'direction', width: 75 },
  { title: t('fin.io.categories'), dataIndex: 'categoryNames', width: 170, ellipsis: true },
  { title: t('fin.io.amount'), key: 'amount', width: 120, align: 'right' },
  { title: t('fin.io.account'), dataIndex: 'accountName', width: 160, ellipsis: true },
  { title: t('fin.io.partner'), dataIndex: 'partnerLabel', width: 150, ellipsis: true },
  { title: t('fin.io.settleMethod'), key: 'settleMethod', width: 110 },
  { title: t('fin.io.ticketNo'), dataIndex: 'ticketNo', width: 130, ellipsis: true },
  { title: t('fin.io.dept'), dataIndex: 'deptName', width: 120 },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('fin.io.createBy'), dataIndex: 'createByName', width: 100 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  // 草稿：编辑、确认、删除；已确认（凭证未过账）：取消确认
  actionColumn(3, [t('fin.io.cancelConfirm')])
]
const rowActions = (record) => {
  const draft = record.status === IoStatus.DRAFT
  return [
    { type: 'edit', show: draft, perm: 'fin:io:edit', onClick: () => formRef.value.openEdit(record) },
    { type: 'confirm', label: t('fin.io.confirm'), show: draft, perm: 'fin:io:confirm', onClick: () => confirm(record) },
    {
      type: 'unaudit',
      label: t('fin.io.cancelConfirm'),
      show: !draft && record.voucherStatus !== VoucherStatus.POSTED,
      perm: 'fin:io:confirm',
      onClick: () => cancelConfirm(record)
    },
    { type: 'delete', show: draft, perm: 'fin:io:remove', confirm: t('fin.io.confirmDelete', { no: record.docNo }), onClick: () => remove(record.id) }
  ]
}
const { checked, visibleColumns, scrollX } = useColumns(columns, 'fin-income-expense')
const range = ref()
const query = reactive({ direction: undefined, status: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(
  (p) => pageIo({ ...p, beginDate: range.value?.[0], endDate: range.value?.[1] }),
  query
)
onMounted(load)

const formRef = ref()
const detailRef = ref()
const { confirm, cancelConfirm } = useIoActions(() => load())
const remove = useAction(deleteIo, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

/** 保存后刷新列表；在详情中修改的刷新详情，新增的打开详情以便确认 */
function onSaved(id) {
  load()
  detailRef.value.open(id)
}

const voucherOpen = ref(false)
const voucherId = ref()
function openVoucher(id) {
  voucherId.value = id
  voucherOpen.value = true
}
</script>

<style scoped>
.filter {
  width: 130px;
}
.filter-wide {
  width: 220px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.in {
  color: var(--status-positive, #389e0d);
}
.out {
  color: var(--status-critical, #cf1322);
}
</style>
