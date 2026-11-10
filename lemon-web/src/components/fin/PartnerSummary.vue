<template>
  <!-- 往来汇总（应收管理 / 应付管理的页签）：一个往来单位一行，展开看明细，可直接对该单位收款 / 付款 -->
  <div>
    <div class="filters">
      <PartnerSelect v-model:value="query.partnerId" :type="conf.partner" allow-clear :placeholder="conf.allPartners" class="filter" />
      <a-input-search v-model:value="query.keyword" :placeholder="$t('fin.partnerSummary.keywordPh')" allow-clear class="filter" @search="search" />
      <IdentityUserSelect v-if="kind === 'ar'" v-model:value="query.ownerId" :placeholder="$t('fin.partnerSummary.owner')" class="filter-s" />
      <a-checkbox v-model:checked="query.onlyBalance" @change="search">{{ $t('fin.partnerSummary.onlyBalance') }}</a-checkbox>
      <a-tooltip :title="tx.formula">
        <QuestionCircleOutlined class="help" />
      </a-tooltip>
    </div>
    <div class="stats">
      <span>{{ tx.opening }} <b>¥{{ formatMoney(total.opening) }}</b></span>
      <span>{{ tx.bills }} <b>¥{{ formatMoney(total.billBalance) }}</b></span>
      <span>{{ tx.advance }} <b :class="{ neg: Number(total.advanceBalance) < 0 }">¥{{ formatMoney(total.advanceBalance) }}</b></span>
      <span>{{ tx.net }} <b class="warn">¥{{ formatMoney(total.net) }}</b></span>
      <span class="sub-text">{{ tx.badDebt }} ¥{{ formatMoney(total.badDebt) }}</span>
    </div>
    <a-table
      row-key="partnerId"
      size="middle"
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: scrollX }"
      :expanded-row-keys="expanded"
      @expand="onExpand"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'partner'">
          <a @click="toggle(record)">{{ record.partnerName }}</a>
          <div class="sub-text">{{ record.partnerCode }}<template v-if="record.ownerName"> · {{ record.ownerName }}</template></div>
        </template>
        <template v-else-if="column.key === 'net'">
          <b :class="{ neg: Number(record.net) < 0 }">{{ formatMoney(record.net) }}</b>
        </template>
        <template v-else-if="column.key === 'badDebt'">
          <span class="sub-text">{{ moneyOrBlank(record.badDebt) }}</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <span class="table-actions">
            <a-button v-perm="`fin:${settleKind}:add`" type="link" size="small" @click="openSettle(record)">{{ tx.settle }}</a-button>
            <a-button v-perm="'fin:partnerStatement:list'" type="link" size="small" @click="toStatement(record)">{{ $t('fin.partnerSummary.statement') }}</a-button>
          </span>
        </template>
        <template v-else-if="column.money">
          <span :class="{ neg: Number(record[column.key]) < 0 }">{{ moneyOrBlank(record[column.key]) }}</span>
        </template>
      </template>
      <template #expandedRowRender="{ record }">
        <PartnerSummaryDetail :kind="kind" :partner-id="record.partnerId" :conf="conf" />
      </template>
    </a-table>

    <SettlementForm ref="settleRef" :kind="settleKind" :conf="settleConf" @saved="reload" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { QuestionCircleOutlined } from '@ant-design/icons-vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import SettlementForm from './SettlementForm.vue'
import PartnerSummaryDetail from './PartnerSummaryDetail.vue'
import { billSummary, billSummaryTotal } from '@/api/fin'
import { useTable } from '@/composables/useTable'
import { formatMoney, moneyOrBlank } from './fin'
import { settlementConf } from './settlementConf'
import { t } from '@/locales'

/**
 * 往来汇总：一个往来单位一行（当前余额，本位币）。净欠款 = 期初余额 + 单据余额 + 预收 / 预付（负数）；
 * 坏账核销已从单据余额中扣除，单列参考。与往来对账单一致：对账单截至今天的期末余额 = 净欠款 + 坏账核销。
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
  net: t(ar ? 'fin.partnerSummary.ar.net' : 'fin.partnerSummary.ap.net'),
  settle: t(ar ? 'fin.partnerSummary.ar.settle' : 'fin.partnerSummary.ap.settle'),
  formula: t(ar ? 'fin.partnerSummary.ar.formula' : 'fin.partnerSummary.ap.formula')
}

const columns = [
  { title: props.conf.partnerLabel, key: 'partner', width: 220 },
  { title: tx.opening, key: 'opening', width: 140, align: 'right', money: true },
  { title: tx.bills, key: 'billBalance', width: 150, align: 'right', money: true },
  { title: tx.advance, key: 'advanceBalance', width: 150, align: 'right', money: true },
  { title: tx.badDebt, key: 'badDebt', width: 120, align: 'right' },
  { title: tx.net, key: 'net', width: 150, align: 'right' },
  { title: t('common.action.operation'), key: 'action', width: 150, fixed: 'right' }
]
const scrollX = columns.reduce((s, c) => s + c.width, 0) + 48

const query = reactive({ partnerId: undefined, keyword: undefined, ownerId: undefined, onlyBalance: true })
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
  router.push({ path: '/fin/partner-statement', query: { kind: props.kind.toUpperCase(), partnerId: record.partnerId, partnerName: `${record.partnerCode} ${record.partnerName}` } })
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
