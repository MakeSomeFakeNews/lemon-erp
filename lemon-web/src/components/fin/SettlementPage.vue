<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="docs" :tab="conf.tab">
          <div class="toolbar">
            <a-space wrap>
              <PartnerSelect v-model:value="query.partnerId" :type="conf.partner" allow-clear :placeholder="conf.allPartners" class="filter" />
              <a-input-search v-model:value="query.keyword" :placeholder="$t('fin.settlementPage.no')" allow-clear class="filter" @search="search" />
            </a-space>
            <a-space>
              <a-button v-perm="`fin:${kind}:voucher`" :disabled="!selectedKeys.length" @click="generate">
                <template #icon><FileAddOutlined /></template>{{ $t('fin.settlementPage.generateN', { n: selectedKeys.length }) }}
              </a-button>
              <a-button v-perm="`fin:${kind}:add`" type="primary" @click="openCreate">
                <template #icon><PlusOutlined /></template>{{ conf.newTitle }}
              </a-button>
            </a-space>
          </div>
          <a-table
            row-key="id"
            size="middle"
            :columns="columns"
            :data-source="list"
            :loading="loading"
            :pagination="pagination"
            :row-selection="{ ...rowSelection, getCheckboxProps: (r) => ({ disabled: !!r.voucherId || r.status !== ApproveStatus.APPROVED }) }"
            :scroll="{ x: scrollX }"
            @change="onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'settleNo'">
                <a @click="openDetail(record)">{{ record.settleNo }}</a>
              </template>
              <template v-else-if="column.key === 'amount'">
                ¥{{ formatMoney(record.amount) }}
                <div v-if="isForeign(record.currency)" class="sub-text">{{ fcText(record.currency, record.amountFc, record.exchangeRate) }}</div>
                <div v-if="Number(record.discountAmount)" class="sub-text">{{ $t('fin.settlementPage.discountN', { amount: formatMoney(record.discountAmount) }) }}</div>
                <div v-if="Number(record.advanceAmount)" class="sub-text">{{ conf.advanceN((isForeign(record.currency) ? `${record.currency} ` : '') + formatMoney(record.advanceAmount)) }}</div>
                <div v-if="Number(record.advanceOffset)" class="sub-text">{{ conf.advanceOffsetLabel }} {{ (isForeign(record.currency) ? `${record.currency} ` : '') + formatMoney(record.advanceOffset) }}</div>
              </template>
              <template v-else-if="column.key === 'method'"><DictTag type="fin_settle_method" :value="record.method" /></template>
              <template v-else-if="column.key === 'voucher'">
                <a v-if="record.voucherId" @click="openVoucher(record.voucherId)">{{ record.voucherNo }}</a>
                <DictTag v-else-if="record.status && record.status !== ApproveStatus.APPROVED" type="sys_approve_status" :value="record.status" />
                <span v-else class="sub-text">{{ $t('fin.settlementPage.notGenerated') }}</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <TableActions :actions="rowActions(record)" />
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="discounts" :tab="$t('fin.settlementPage.discountTab')">
          <SettlementDiscounts v-if="tab === 'discounts'" :kind="kind" :conf="conf" @open="openDetail({ id: $event })" />
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <SettlementForm ref="formRef" :kind="kind" :conf="conf" @saved="onSaved" />

    <!-- 详情 -->
    <SubPage :open="detailOpen" :title="detail ? conf.detailTitle(detail.settleNo) : ''" width="min(820px, 100vw)" @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" class="block">
          <a-descriptions-item :label="conf.partnerLabel">{{ detail.partnerName }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.status !== ApproveStatus.APPROVED" :label="$t('common.field.status')">
            <DictTag type="sys_approve_status" :value="detail.status" />
          </a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.date')">{{ detail.settleDate }}</a-descriptions-item>
          <a-descriptions-item :label="conf.paidLabel"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item v-if="Number(detail.discountAmount)" :label="$t('fin.settlementForm.discountTotal')">
            ¥{{ formatMoney(detail.discountAmount) }}
            <span v-if="isForeign(detail.currency)" class="sub-text">{{ $t('fin.settlementPage.originalN', { amount: `${detail.currency} ${formatMoney(detail.discountFc)}` }) }}</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="Number(detail.advanceAmount)" :label="conf.toAdvance">
            {{ isForeign(detail.currency) ? `${detail.currency} ` : '¥' }}{{ formatMoney(detail.advanceAmount) }} <span class="sub-text">{{ detail.advanceNo || '' }}</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="Number(detail.advanceOffset)" :label="conf.advanceOffsetLabel">
            {{ isForeign(detail.currency) ? `${detail.currency} ` : '¥' }}{{ formatMoney(detail.advanceOffset) }}
            <span class="sub-text inline-gap">{{ (detail.advances || []).map((a) => a.advanceNo).join('、') }}</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="isForeign(detail.currency)" :label="$t('fin.settlementPage.fcRate')">
            {{ fcText(detail.currency, detail.amountFc, detail.exchangeRate) }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('fin.settlementPage.account')">{{ detail.accountName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.settlementPage.method')"><DictTag type="fin_settle_method" :value="detail.method" /></a-descriptions-item>
          <a-descriptions-item :label="$t('fin.settlementPage.voucher')">{{ detail.voucherNo || $t('fin.settlementPage.notGenerated') }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.settlementPage.preparedBy')">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'billAmount'">{{ formatMoney(record.billAmount) }}</template>
            <template v-else-if="column.key === 'source'">
              <DictTag type="fin_bill_source" :value="record.sourceType" />
              <SourceDocLink class="sub-text" :type="record.sourceType" :id="record.sourceId" :no="record.sourceNo" :bill-kind="record.billKind" :project-id="record.projectId" />
            </template>
            <template v-else-if="column.key === 'advanceOffset'">{{ Number(record.advanceOffset) ? formatMoney(record.advanceOffset) : '' }}</template>
            <template v-else-if="column.key === 'discount'">
              {{ Number(record.discount) ? formatMoney(record.discount) : '' }}
              <div v-if="Number(record.discount) && isForeign(detail.currency)" class="sub-text">{{ $t('fin.settlementPage.originalN', { amount: `${detail.currency} ${formatMoney(record.discountFc)}` }) }}</div>
            </template>
            <template v-else-if="column.key === 'amount'">
              {{ formatMoney(record.amount) }}
              <div v-if="isForeign(detail.currency) && record.amountFc != null" class="sub-text">{{ $t('fin.settlementPage.originalN', { amount: `${detail.currency} ${formatMoney(record.amountFc)}` }) }}</div>
            </template>
          </template>
        </a-table>
      </template>
      <template v-if="detail && editable(detail)" #footer>
        <div class="drawer-footer">
          <a-button v-perm="`fin:${kind}:add`" @click="openEdit(detail)">{{ $t('common.action.edit') }}</a-button>
          <a-button v-if="detail.status === ApproveStatus.DRAFT" v-perm="`fin:${kind}:confirm`" type="primary" @click="confirm(detail)">
            {{ $t('fin.settlementPage.confirmAction') }}
          </a-button>
        </div>
      </template>
    </SubPage>

    <VoucherDetail v-model:open="voucherOpen" :voucher-id="voucherId" @changed="load" />
  </div>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { onMounted, reactive, ref, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { FileAddOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import VoucherDetail from './VoucherDetail.vue'
import SettlementForm from './SettlementForm.vue'
import SettlementDiscounts from './SettlementDiscounts.vue'
import SourceDocLink from '@/components/common/SourceDocLink.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { confirmSettlement, deleteSettlement, getSettlement, pageSettlement, settlementVoucher } from '@/api/fin'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { formatMoney } from './fin'
import { fcText, isForeign } from './currency'
import { t } from '@/locales'
import { settlementConf } from './settlementConf'
import { useAction } from '@/composables/useAction'

import { ApproveStatus } from '@/constants/enums'
/** 收款单 / 付款单，kind = receipt | payment */
const props = defineProps({
  kind: { type: String, required: true }
})

const conf = settlementConf(props.kind)
useDict('fin_settle_method', 'fin_bill_source', 'sys_approve_status')

// 列宽合计 + 勾选列不超过 1440 宽屏幕的内容区（约 1140px），右侧固定的操作列不遮挡其他列
const columns = [
  { title: t('fin.settlementPage.no'), key: 'settleNo', width: 160 },
  { title: conf.partnerLabel, dataIndex: 'partnerName', width: 180, ellipsis: true },
  { title: t('common.field.date'), dataIndex: 'settleDate', width: 100 },
  { title: t('common.field.amount'), key: 'amount', width: 150, align: 'right' },
  { title: t('fin.settlementPage.account'), dataIndex: 'accountName', width: 120, ellipsis: true },
  { title: t('fin.settlementPage.methodShort'), key: 'method', width: 90 },
  { title: t('fin.settlementPage.voucher'), key: 'voucher', width: 100 },
  { title: t('fin.settlementPage.createBy'), dataIndex: 'createByName', width: 80, ellipsis: true },
  // 草稿：查看、修改、确认、删除
  actionColumn(4, [t('common.action.view')])
]
const scrollX = columns.reduce((sum, c) => sum + c.width, 0) + 32
// 行操作：查看；草稿 / 已拒绝可修改，草稿可确认；未生成凭证且不在审批中的可删除
const rowActions = (record) => [
  { type: 'view', onClick: () => openDetail(record) },
  { type: 'edit', show: editable(record), perm: `fin:${props.kind}:add`, onClick: () => openEdit(record) },
  {
    type: 'confirm',
    label: t('fin.settlementPage.confirmAction'),
    show: record.status === ApproveStatus.DRAFT,
    perm: `fin:${props.kind}:confirm`,
    onClick: () => confirm(record)
  },
  {
    type: 'delete',
    show: !record.voucherId && record.status !== ApproveStatus.APPROVING,
    perm: `fin:${props.kind}:remove`,
    confirm:
      record.status === ApproveStatus.APPROVED
        ? t('fin.settlementPage.confirmDeleteApproved', { no: record.settleNo })
        : t('fin.settlementPage.confirmDelete', { no: record.settleNo }),
    onClick: () => remove(record.id)
  }
]
const tab = ref('docs')
const query = reactive({ partnerId: undefined, keyword: undefined })
const { list, loading, pagination, selectedKeys, rowSelection, load, search, onTableChange } = useTable(
  (p) => pageSettlement(props.kind, p),
  query
)
watch(() => query.partnerId, () => search())
onMounted(load)

function generate() {
  Modal.confirm({
    title: conf.generateConfirm(selectedKeys.value.length),
    content: conf.generateTip,
    async onOk() {
      const count = await settlementVoucher(props.kind, selectedKeys.value)
      message.success(t('fin.settlementPage.generated', { n: count }))
      selectedKeys.value = []
      load()
    }
  })
}

const remove = useAction((id) => deleteSettlement(props.kind, id), { successKey: 'common.msg.deleteSuccess', reload: () => load() })

// ---------------------------------------------------------------- 新增、修改草稿、确认
const formRef = ref()
const openCreate = () => formRef.value.open()
/** 草稿、已拒绝的付款单可以修改（票据业务生成的除外） */
const editable = (r) => (r.status === ApproveStatus.DRAFT || r.status === ApproveStatus.REJECTED) && r.method !== 'NOTE'
const openEdit = (r) => formRef.value.open({ id: r.id })
function onSaved() {
  load()
  if (detailOpen.value && detail.value) openDetail(detail.value)
}
const confirm = useAction((r) => confirmSettlement(props.kind, r.id), {
  confirm: (r) => ({ title: t('fin.settlementPage.confirmTitle', { no: r.settleNo }), content: conf.confirmTip }),
  successKey: 'fin.settlementPage.confirmed',
  reload: () => onSaved()
})

// ---------------------------------------------------------------- 详情
const itemColumns = [
  { title: t('fin.settlementPage.settledBill'), dataIndex: 'billNo', width: 160 },
  { title: t('fin.settlementPage.source'), key: 'source', width: 200 },
  { title: t('fin.settlementPage.billDate'), dataIndex: 'billDate', width: 110 },
  { title: t('fin.settlementPage.billAmount'), key: 'billAmount', width: 130, align: 'right' },
  { title: t('fin.settlementPage.settle'), key: 'amount', width: 130, align: 'right' },
  { title: t('fin.settlementForm.discount'), key: 'discount', width: 110, align: 'right' },
  { title: conf.advanceOffsetLabel, key: 'advanceOffset', width: 110, align: 'right' }
]
const detailOpen = ref(false)
const detail = ref()
async function openDetail(record) {
  detail.value = undefined
  detailOpen.value = true
  detail.value = await getSettlement(props.kind, record.id)
}

const voucherOpen = ref(false)
const voucherId = ref()
function openVoucher(id) {
  voucherId.value = id
  voucherOpen.value = true
}
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.filter {
  width: 240px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.block {
  margin-bottom: 16px;
}
.inline-gap {
  margin-left: 8px;
}
</style>
