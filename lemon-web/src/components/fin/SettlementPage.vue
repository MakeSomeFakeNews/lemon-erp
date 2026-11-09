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
            :scroll="{ x: 1260 }"
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
              </template>
              <template v-else-if="column.key === 'method'"><DictTag type="fin_settle_method" :value="record.method" /></template>
              <template v-else-if="column.key === 'voucher'">
                <a v-if="record.voucherId" @click="openVoucher(record.voucherId)">{{ record.voucherNo }}</a>
                <DictTag v-else-if="record.status && record.status !== ApproveStatus.APPROVED" type="sys_approve_status" :value="record.status" />
                <span v-else class="sub-text">{{ $t('fin.settlementPage.notGenerated') }}</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <span class="table-actions">
                  <a-button type="link" size="small" @click="openDetail(record)">{{ $t('common.action.view') }}</a-button>
                  <span v-if="!record.voucherId && record.status !== ApproveStatus.APPROVING" v-perm="`fin:${kind}:remove`">
                    <a-popconfirm :title="record.status === ApproveStatus.APPROVED ? $t('fin.settlementPage.confirmDeleteApproved', { no: record.settleNo }) : $t('fin.settlementPage.confirmDelete', { no: record.settleNo })" @confirm="remove(record.id)">
                      <a-button type="link" danger size="small">{{ $t('common.action.delete') }}</a-button>
                    </a-popconfirm>
                  </span>
                </span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="discounts" :tab="$t('fin.settlementPage.discountTab')">
          <SettlementDiscounts v-if="tab === 'discounts'" :kind="kind" :conf="conf" @open="openDetail({ id: $event })" />
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <SettlementForm ref="formRef" :kind="kind" :conf="conf" @saved="load" />

    <!-- 详情 -->
    <SubPage :open="detailOpen" :title="detail ? conf.detailTitle(detail.settleNo) : ''" width="min(820px, 100vw)" @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" class="block">
          <a-descriptions-item :label="conf.partnerLabel">{{ detail.partnerName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.date')">{{ detail.settleDate }}</a-descriptions-item>
          <a-descriptions-item :label="conf.paidLabel"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item v-if="Number(detail.discountAmount)" :label="$t('fin.settlementForm.discountTotal')">
            ¥{{ formatMoney(detail.discountAmount) }}
            <span v-if="isForeign(detail.currency)" class="sub-text">{{ $t('fin.settlementPage.originalN', { amount: `${detail.currency} ${formatMoney(detail.discountFc)}` }) }}</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="Number(detail.advanceAmount)" :label="conf.toAdvance">
            {{ isForeign(detail.currency) ? `${detail.currency} ` : '¥' }}{{ formatMoney(detail.advanceAmount) }} <span class="sub-text">{{ detail.advanceNo || '' }}</span>
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
            <template v-else-if="column.key === 'source'"><DictTag type="fin_bill_source" :value="record.sourceType" /> <span class="sub-text">{{ record.sourceNo || '' }}</span></template>
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
import { deleteSettlement, getSettlement, pageSettlement, settlementVoucher } from '@/api/fin'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { formatMoney } from './fin'
import { fcText, isForeign } from './currency'
import { t } from '@/locales'
import { useAction } from '@/composables/useAction'

import { ApproveStatus } from '@/constants/enums'
/** 收款单 / 付款单，kind = receipt | payment */
const props = defineProps({
  kind: { type: String, required: true }
})

const CONF = {
  receipt: {
    partner: 'customer',
    billKind: 'ar',
    partnerLabel: t('fin.settlementPage.receipt.partner'),
    allPartners: t('fin.settlementPage.receipt.allPartners'),
    newTitle: t('fin.settlementPage.receipt.newTitle'),
    dateLabel: t('fin.settlementPage.receipt.date'),
    billsTitle: t('fin.settlementPage.receipt.billsTitle'),
    thisTotal: t('fin.settlementPage.receipt.thisTotal'),
    confirm: t('fin.settlementPage.receipt.confirm'),
    generateTip: t('fin.settlementPage.receipt.generateTip'),
    submitted: t('fin.settlementPage.receipt.submitted'),
    success: t('fin.settlementPage.receipt.success'),
    foreignTip: (currency) => t('fin.settlementPage.receipt.foreignTip', { currency }),
    multiCurrency: (currencies) => t('fin.settlementPage.receipt.multiCurrency', { currencies }),
    foreignTotal: (currency, amount) => t('fin.settlementPage.receipt.foreignTotal', { currency, amount }),
    detailTitle: (no) => t('fin.settlementPage.receipt.detailTitle', { no }),
    generateConfirm: (n) => t('fin.settlementPage.receipt.generateConfirm', { n }),
    filledTip: (currency) => t('fin.settlementPage.receipt.filledTip', { currency }),
    toAdvance: t('fin.settlementPage.receipt.toAdvance'),
    paidLabel: t('fin.settlementPage.receipt.paidLabel'),
    advanceN: (amount) => t('fin.settlementPage.receipt.advanceN', { amount }),
    tab: t('fin.settlementPage.receipt.tab'),
    discountExport: t('fin.settlementPage.receipt.discountExport')
  },
  payment: {
    partner: 'supplier',
    billKind: 'ap',
    partnerLabel: t('fin.settlementPage.payment.partner'),
    allPartners: t('fin.settlementPage.payment.allPartners'),
    newTitle: t('fin.settlementPage.payment.newTitle'),
    dateLabel: t('fin.settlementPage.payment.date'),
    billsTitle: t('fin.settlementPage.payment.billsTitle'),
    thisTotal: t('fin.settlementPage.payment.thisTotal'),
    confirm: t('fin.settlementPage.payment.confirm'),
    generateTip: t('fin.settlementPage.payment.generateTip'),
    submitted: t('fin.settlementPage.payment.submitted'),
    success: t('fin.settlementPage.payment.success'),
    foreignTip: (currency) => t('fin.settlementPage.payment.foreignTip', { currency }),
    multiCurrency: (currencies) => t('fin.settlementPage.payment.multiCurrency', { currencies }),
    foreignTotal: (currency, amount) => t('fin.settlementPage.payment.foreignTotal', { currency, amount }),
    detailTitle: (no) => t('fin.settlementPage.payment.detailTitle', { no }),
    generateConfirm: (n) => t('fin.settlementPage.payment.generateConfirm', { n }),
    filledTip: (currency) => t('fin.settlementPage.payment.filledTip', { currency }),
    toAdvance: t('fin.settlementPage.payment.toAdvance'),
    paidLabel: t('fin.settlementPage.payment.paidLabel'),
    advanceN: (amount) => t('fin.settlementPage.payment.advanceN', { amount }),
    tab: t('fin.settlementPage.payment.tab'),
    discountExport: t('fin.settlementPage.payment.discountExport')
  }
}
const conf = CONF[props.kind]
useDict('fin_settle_method', 'fin_bill_source', 'sys_approve_status')

const columns = [
  { title: t('fin.settlementPage.no'), key: 'settleNo', width: 170 },
  { title: conf.partnerLabel, dataIndex: 'partnerName', width: 220, ellipsis: true },
  { title: t('common.field.date'), dataIndex: 'settleDate', width: 110 },
  { title: t('common.field.amount'), key: 'amount', width: 170, align: 'right' },
  { title: t('fin.settlementPage.account'), dataIndex: 'accountName', width: 150 },
  { title: t('fin.settlementPage.methodShort'), key: 'method', width: 110 },
  { title: t('fin.settlementPage.voucher'), key: 'voucher', width: 120 },
  { title: t('fin.settlementPage.createBy'), dataIndex: 'createByName', width: 120 },
  { title: t('common.action.operation'), key: 'action', width: 140, fixed: 'right' }
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

// ---------------------------------------------------------------- 新增
const formRef = ref()
const openCreate = () => formRef.value.open()

// ---------------------------------------------------------------- 详情
const itemColumns = [
  { title: t('fin.settlementPage.settledBill'), dataIndex: 'billNo', width: 160 },
  { title: t('fin.settlementPage.source'), key: 'source', width: 200 },
  { title: t('fin.settlementPage.billDate'), dataIndex: 'billDate', width: 110 },
  { title: t('fin.settlementPage.billAmount'), key: 'billAmount', width: 130, align: 'right' },
  { title: t('fin.settlementPage.settle'), key: 'amount', width: 130, align: 'right' },
  { title: t('fin.settlementForm.discount'), key: 'discount', width: 110, align: 'right' }
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
</style>
