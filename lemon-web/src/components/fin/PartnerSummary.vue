<template>
  <!-- 往来汇总（应收管理 / 应付管理的页签）：一个往来单位一行，展开看明细，可直接对该单位收款 / 付款 -->
  <div>
    <div class="filters">
      <PartnerSelect v-model:value="query.partnerId" :type="conf.partner" allow-clear :placeholder="conf.allPartners" class="filter" />
      <a-input-search v-model:value="query.keyword" :placeholder="$t('fin.partnerSummary.keywordPh')" allow-clear class="filter" @search="search" />
      <IdentityUserSelect v-if="kind === 'ar'" v-model:value="query.ownerId" :placeholder="$t('fin.partnerSummary.owner')" class="filter-s" />
      <a-checkbox v-model:checked="query.onlyBalance" @change="search">{{ $t('fin.partnerSummary.onlyBalance') }}</a-checkbox>
      <a-tooltip v-if="canLinkAp" :title="$t('fin.linkedAp.summaryTip')">
        <a-checkbox v-model:checked="query.withAp" @change="search">{{ $t('fin.linkedAp.label') }}</a-checkbox>
      </a-tooltip>
      <a-tooltip :title="tx.formula">
        <QuestionCircleOutlined class="help" />
      </a-tooltip>
    </div>
    <div class="stats">
      <span>{{ tx.opening }} <b>¥{{ formatMoney(total.opening) }}</b></span>
      <span>{{ tx.bills }} <b>¥{{ formatMoney(total.billBalance) }}</b></span>
      <span>{{ tx.advance }} <b :class="{ neg: Number(total.advanceBalance) < 0 }">¥{{ formatMoney(total.advanceBalance) }}</b></span>
      <span v-if="query.withAp">{{ $t('fin.linkedAp.column') }} <b :class="{ neg: Number(total.linkedAp) < 0 }">¥{{ formatMoney(total.linkedAp) }}</b></span>
      <span>{{ tx.net }} <b class="warn">¥{{ formatMoney(total.net) }}</b></span>
      <span class="sub-text">{{ tx.badDebt }} ¥{{ formatMoney(total.badDebt) }}</span>
      <span class="sub-text">{{ tx.discount }} ¥{{ formatMoney(total.discount) }}</span>
    </div>
    <a-table
      row-key="partnerId"
      size="middle"
      :columns="tableColumns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: tableColumns.reduce((s, c) => s + c.width, 0) + 48 }"
      :expanded-row-keys="expanded"
      @expand="onExpand"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'partner'">
          <a @click="toggle(record)">{{ record.partnerName }}</a>
          <div class="sub-text">{{ record.partnerCode }}<template v-if="record.ownerName"> · {{ record.ownerName }}</template></div>
          <div v-if="record.linkedSupplierName" class="sub-text">{{ $t('fin.linkedAp.supplierOf', { name: record.linkedSupplierName }) }}</div>
        </template>
        <template v-else-if="column.key === 'net'">
          <b :class="{ neg: Number(record.net) < 0 }">{{ formatMoney(record.net) }}</b>
        </template>
        <template v-else-if="column.key === 'badDebt' || column.key === 'discount'">
          <span class="sub-text">{{ moneyOrBlank(record[column.key]) }}</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <TableActions :actions="rowActions(record)" />
        </template>
        <template v-else-if="column.money">
          <span :class="{ neg: Number(record[column.key]) < 0 }">{{ moneyOrBlank(record[column.key]) }}</span>
        </template>
      </template>
      <template #expandedRowRender="{ record }">
        <PartnerSummaryDetail :kind="kind" :partner-id="record.partnerId" :linked-supplier-id="query.withAp ? record.linkedSupplierId : undefined" :conf="conf" />
      </template>
    </a-table>

    <SettlementForm ref="settleRef" :kind="settleKind" :conf="settleConf" @saved="reload" />
    <NettingForm v-if="ar" ref="nettingRef" @saved="reload" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { FileSearchOutlined, InteractionOutlined, QuestionCircleOutlined, TransactionOutlined } from '@ant-design/icons-vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import SettlementForm from './SettlementForm.vue'
import PartnerSummaryDetail from './PartnerSummaryDetail.vue'
import NettingForm from './NettingForm.vue'
import { useUserStore } from '@/store/user'
import { billSummary, billSummaryTotal } from '@/api/fin'
import { useTable } from '@/composables/useTable'
import { formatMoney, moneyOrBlank } from './fin'
import { settlementConf } from './settlementConf'
import { t } from '@/locales'

/**
 * 往来汇总：一个往来单位一行（当前余额，本位币）。净欠款 = 期初余额 + 单据余额 + 预收 / 预付（负数）；
 * 坏账核销、收付款折让已从单据余额中扣除，单列参考。与往来对账单一致：对账单截至今天的期末余额 = 净欠款 + 坏账核销。
 * kind = ar | ap，conf 为 BillPage 按应收应付区分的文字
 */
const props = defineProps({
  kind: { type: String, required: true },
  conf: { type: Object, required: true }
})

const ar = props.kind === 'ar'
const settleKind = ar ? 'receipt' : 'payment'
const settleConf = settlementConf(settleKind)
const tx = {
  opening: t(ar ? 'fin.partnerSummary.ar.opening' : 'fin.partnerSummary.ap.opening'),
  bills: t(ar ? 'fin.partnerSummary.ar.bills' : 'fin.partnerSummary.ap.bills'),
  advance: t(ar ? 'fin.partnerSummary.ar.advance' : 'fin.partnerSummary.ap.advance'),
  badDebt: t(ar ? 'fin.partnerSummary.ar.badDebt' : 'fin.partnerSummary.ap.badDebt'),
  discount: t(ar ? 'fin.partnerSummary.ar.discount' : 'fin.partnerSummary.ap.discount'),
  net: t(ar ? 'fin.partnerSummary.ar.net' : 'fin.partnerSummary.ap.net'),
  settle: t(ar ? 'fin.partnerSummary.ar.settle' : 'fin.partnerSummary.ap.settle'),
  formula: t(ar ? 'fin.partnerSummary.ar.formula' : 'fin.partnerSummary.ap.formula')
}

// 挂接应付（issue #95）：只用于应收，需要应付的查看权限；挂接后另列关联供应商的应付净额（负数），净欠款 = 应收 − 应付
const canLinkAp = ar && useUserStore().hasPerm('fin:ap:list')
const columns = [
  { title: props.conf.partnerLabel, key: 'partner', width: 220 },
  { title: tx.opening, key: 'opening', width: 140, align: 'right', money: true },
  { title: tx.bills, key: 'billBalance', width: 150, align: 'right', money: true },
  { title: tx.advance, key: 'advanceBalance', width: 150, align: 'right', money: true },
  { title: tx.badDebt, key: 'badDebt', width: 120, align: 'right' },
  { title: tx.discount, key: 'discount', width: 120, align: 'right' },
  { title: tx.net, key: 'net', width: 150, align: 'right' },
  actionColumn(ar ? 3 : 2, [tx.settle, t('fin.partnerSummary.statement')])
]
const linkedColumn = { title: t('fin.linkedAp.column'), key: 'linkedAp', width: 140, align: 'right', money: true }
const tableColumns = computed(() => {
  if (!query.withAp) return columns
  const i = columns.findIndex((c) => c.key === 'net')
  return [...columns.slice(0, i), linkedColumn, ...columns.slice(i)]
})
// 行操作：收款 / 付款、往来对账单
const rowActions = (record) => [
  { key: 'settle', label: tx.settle, icon: TransactionOutlined, perm: `fin:${settleKind}:add`, onClick: () => openSettle(record) },
  { key: 'statement', label: t('fin.partnerSummary.statement'), icon: FileSearchOutlined, perm: 'fin:partnerStatement:list', onClick: () => toStatement(record) },
  // 有关联供应商的客户：应收应付对冲
  { key: 'netting', label: t('fin.netting.create'), icon: InteractionOutlined, perm: 'fin:netting:add', more: true, show: !!record.linkedSupplierId, onClick: () => nettingRef.value.open(record.partnerId) }
]

const query = reactive({ partnerId: undefined, keyword: undefined, ownerId: undefined, onlyBalance: true, withAp: false })
const nettingRef = ref()
const total = ref({})
const { list, loading, pagination, load, search, onTableChange } = useTable((p) => billSummary(props.kind, p), query)
async function loadTotal() {
  total.value = await billSummaryTotal(props.kind, { ...query })
}
watch(() => [query.partnerId, query.ownerId], () => search())
// 查询条件变化（包括「查询」按钮）时同时刷新合计
watch(loading, (v, old) => {
  if (old && !v) loadTotal()
})
onMounted(load)

function reload() {
  expanded.value = []
  load()
}

// 展开明细：同时只展开一个单位
const expanded = ref([])
function onExpand(open, record) {
  expanded.value = open ? [record.partnerId] : []
}
const toggle = (record) => onExpand(!expanded.value.includes(record.partnerId), record)

const settleRef = ref()
const openSettle = (record) => settleRef.value.open({ partnerId: record.partnerId, partnerName: `${record.partnerCode} ${record.partnerName}` })

const router = useRouter()
const toStatement = (record) =>
  router.push({
    path: '/fin/partner-statement',
    query: { kind: props.kind.toUpperCase(), partnerId: record.partnerId, partnerName: `${record.partnerCode} ${record.partnerName}`, withAp: query.withAp ? '1' : undefined }
  })
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.filter {
  width: 240px;
}
.filter-s {
  width: 180px;
}
.help {
  color: var(--app-text-3);
  cursor: help;
}
.stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 24px;
  margin-bottom: 8px;
  color: var(--app-text-2);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.neg,
.warn {
  color: #cf1322;
}
</style>
