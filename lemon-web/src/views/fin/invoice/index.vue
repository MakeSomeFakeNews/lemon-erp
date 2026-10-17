<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" destroy-inactive-tab-pane>
        <template #rightExtra>
          <a-space v-if="tab !== 'report'">
            <template v-if="tab === InvoiceKind.INPUT">
              <a-button v-perm="'fin:invoice:edit'" :disabled="!certifiable.length" @click="certifyRef.open()">
                <template #icon><SafetyCertificateOutlined /></template>{{ $t('fin.invoice.certifyN', { n: certifiable.length }) }}
              </a-button>
            </template>
            <a-dropdown v-perm="'fin:invoice:edit'">
              <a-button><template #icon><ImportOutlined /></template>{{ $t('common.action.import') }}</a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="xml" @click="xmlRef.open()">{{ $t('fin.invoice.xmlFile') }}</a-menu-item>
                  <a-menu-item key="excel" @click="excelImportOpen = true">{{ $t('fin.invoice.excelList') }}</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-button v-perm="'fin:invoice:edit'" type="primary" @click="createRef.open()">
              <template #icon><PlusOutlined /></template>{{ conf.registerTitle }}
            </a-button>
          </a-space>
        </template>

        <a-tab-pane v-for="k in [InvoiceKind.OUTPUT, InvoiceKind.INPUT]" :key="k" :tab="KINDS[k].label">
          <div class="filters">
            <PartnerSelect v-model:value="query.partnerId" :type="KINDS[k].partner" allow-clear :placeholder="KINDS[k].allPartners" class="filter" />
            <a-select v-model:value="query.status" :placeholder="$t('common.field.status')" allow-clear :options="STATUS_OPTIONS" class="filter-s" @change="search" />
            <a-input-search v-model:value="query.keyword" :placeholder="$t('fin.invoice.keywordPlaceholder')" allow-clear class="filter" @search="search" />
            <a-checkbox v-model:checked="query.unmatched" @change="search">{{ $t('fin.invoice.onlyUnmatched') }}</a-checkbox>
          </div>
          <div class="stats">
            <span>{{ $t('fin.invoice.pageTotal') }} <b>¥{{ formatMoney(pageTotal.amount) }}</b></span>
            <span>{{ $t('fin.invoice.taxAmount') }} ¥{{ formatMoney(pageTotal.tax) }}</span>
            <span class="sub-text">{{ $t('fin.invoice.voidExcluded') }}</span>
          </div>
          <a-table
            row-key="id"
            size="middle"
            :columns="columns"
            :data-source="list"
            :loading="loading"
            :pagination="pagination"
            :row-selection="k === InvoiceKind.INPUT ? { ...rowSelection, getCheckboxProps: (r) => ({ disabled: r.status !== InvoiceStatus.VALID }) } : undefined"
            :scroll="{ x: scrollX }"
            @change="onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'invoiceNo'">
                <a @click="detailRef.open(record)">{{ record.invoiceNo }}</a>
              </template>
              <template v-else-if="column.key === 'invoiceType'"><DictTag type="fin_invoice_type" :value="record.invoiceType" /></template>
              <template v-else-if="column.key === 'amount'">
                <span :class="{ neg: Number(record.amount) < 0, void: record.status === InvoiceStatus.VOID }">¥{{ formatMoney(record.amount) }}</span>
              </template>
              <template v-else-if="column.key === 'taxAmount'">
                <span :class="{ void: record.status === InvoiceStatus.VOID }">{{ formatMoney(record.taxAmount) }}</span>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="record.status === InvoiceStatus.VOID ? 'default' : 'green'" :bordered="false">{{ record.status === InvoiceStatus.VOID ? $t('fin.invoice.statusVoid') : $t('fin.invoice.statusValid') }}</a-tag>
              </template>
              <template v-else-if="column.key === 'matched'">
                <span v-if="record.status === InvoiceStatus.VOID" class="sub-text">-</span>
                <a-tag v-else-if="unmatchedAmount(record) === 0" color="green" :bordered="false">{{ $t('fin.invoice.matched') }}</a-tag>
                <a-tag v-else color="orange" :bordered="false">{{ $t('fin.invoice.unmatchedAmt', { amount: formatMoney(unmatchedAmount(record)) }) }}</a-tag>
              </template>
              <template v-else-if="column.key === 'certify'">
                <a-tag v-if="record.certifyPeriod" color="blue" :bordered="false">{{ periodText(record.certifyPeriod) }}</a-tag>
                <span v-else-if="record.status === InvoiceStatus.VALID && SPECIAL_TYPES.includes(record.invoiceType)" class="sub-text">{{ $t('fin.invoice.uncertified') }}</span>
                <span v-else class="sub-text">-</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <span class="table-actions">
                  <a-button type="link" size="small" @click="detailRef.open(record)">{{ $t('common.action.view') }}</a-button>
                  <a-button v-if="record.status === InvoiceStatus.VALID && unmatchedAmount(record) !== 0" v-perm="'fin:invoice:edit'" type="link" size="small" @click="matchRef.open(record)">
                    {{ $t('fin.invoice.match') }}
                  </a-button>
                  <span v-if="record.status === InvoiceStatus.VALID" v-perm="'fin:invoice:edit'">
                    <a-popconfirm :title="$t('fin.invoice.confirmVoid', { no: record.invoiceNo })" @confirm="cancel(record.id)">
                      <a-button type="link" danger size="small">{{ $t('fin.invoice.void') }}</a-button>
                    </a-popconfirm>
                  </span>
                </span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="report" :tab="$t('fin.invoice.reportTab')">
          <InvoiceReportTab v-model:report-kind="reportKind" v-model:as-of="asOf" />
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 登记发票 -->
    <InvoiceCreate ref="createRef" :kind="kind" @success="load" />

    <!-- 详情 -->
    <InvoiceDetail ref="detailRef" @changed="load" />

    <!-- 勾稽导入的发票 -->
    <InvoiceMatchModal ref="matchRef" :kind="kind" @success="load" />

    <!-- 数电票 XML 导入 -->
    <InvoiceXmlImport ref="xmlRef" :kind="kind" @success="load" />

    <InvoiceExcelImport v-model:open="excelImportOpen" :kind="kind" @success="load" />

    <!-- 进项认证 -->
    <InvoiceCertifyModal ref="certifyRef" :certifiable="certifiable" @success="onCertified" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ImportOutlined, PlusOutlined, SafetyCertificateOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { cancelInvoice, pageInvoice } from '@/api/fin'
import { useAction } from '@/composables/useAction'
import { useTable } from '@/composables/useTable'
import { today } from '@/components/wms/wms'
import { formatMoney, periodText } from '@/components/fin/fin'
import InvoiceCertifyModal from './InvoiceCertifyModal.vue'
import InvoiceCreate from './InvoiceCreate.vue'
import InvoiceDetail from './InvoiceDetail.vue'
import InvoiceExcelImport from './InvoiceExcelImport.vue'
import InvoiceMatchModal from './InvoiceMatchModal.vue'
import InvoiceReportTab from './InvoiceReportTab.vue'
import InvoiceXmlImport from './InvoiceXmlImport.vue'
import { KINDS, SPECIAL_TYPES, STATUS_OPTIONS, unmatchedAmount } from './invoice'
import { InvoiceKind, InvoiceStatus } from '@/constants/enums'

const { t } = useI18n()
const tab = ref(InvoiceKind.OUTPUT)
// 登记、列表使用的种类：报表页签时沿用上一个发票页签
const kind = ref(InvoiceKind.OUTPUT)
const conf = computed(() => KINDS[kind.value])

const columns = computed(() => [
  { title: t('fin.invoice.invoiceNo'), key: 'invoiceNo', width: 160 },
  { title: t('common.field.type'), key: 'invoiceType', width: 140 },
  { title: conf.value.partnerLabel, dataIndex: 'partnerName', width: 200, ellipsis: true },
  { title: t('fin.invoice.invoiceDate'), dataIndex: 'invoiceDate', width: 120 },
  { title: t('fin.invoice.amount'), key: 'amount', width: 140, align: 'right' },
  { title: t('fin.invoice.taxAmount'), key: 'taxAmount', width: 120, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('fin.invoice.match'), key: 'matched', width: 170 },
  ...(kind.value === InvoiceKind.INPUT ? [{ title: t('fin.invoice.certifyPeriod'), key: 'certify', width: 140 }] : []),
  { title: t('common.field.remark'), dataIndex: 'remark', width: 200, ellipsis: true },
  { title: t('fin.invoice.registeredBy'), dataIndex: 'createByName', width: 120 },
  { title: t('common.action.operation'), key: 'action', width: 150, fixed: 'right' }
])
// 进项发票带勾选列
const scrollX = computed(() => columns.value.reduce((sum, c) => sum + c.width, 0) + (kind.value === InvoiceKind.INPUT ? 32 : 0))
const query = reactive({ kind: InvoiceKind.OUTPUT, partnerId: undefined, status: undefined, keyword: undefined, unmatched: false })
const { list, loading, pagination, selectedKeys, rowSelection, load, search, onTableChange } = useTable(pageInvoice, query)
onMounted(load)
watch(() => query.partnerId, () => search())
watch(tab, (t) => {
  if (t === 'report') return // 未开票 / 暂估页签打开时自行查询
  kind.value = t
  Object.assign(query, { kind: t, partnerId: undefined, status: undefined, keyword: undefined, unmatched: false })
  selectedKeys.value = []
  search()
})

const pageTotal = computed(() =>
  list.value
    .filter((r) => r.status !== InvoiceStatus.VOID)
    .reduce((t, r) => ({ amount: t.amount + Number(r.amount), tax: t.tax + Number(r.taxAmount) }), { amount: 0, tax: 0 })
)

const cancel = useAction(cancelInvoice, { successKey: 'fin.invoice.voided', reload: () => load() })

// 认证：勾选的有效发票
const certifiable = computed(() => list.value.filter((r) => selectedKeys.value.includes(r.id) && r.status === InvoiceStatus.VALID).map((r) => r.id))
function onCertified() {
  selectedKeys.value = []
  load()
}

const createRef = ref()
const detailRef = ref()
const matchRef = ref()
const xmlRef = ref()
const certifyRef = ref()
const excelImportOpen = ref(false)

// 未开票 / 暂估的查询条件，切换页签后保持
const reportKind = ref(InvoiceKind.OUTPUT)
const asOf = ref(today())
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
  width: 130px;
}
.stats {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin-bottom: 8px;
  color: var(--app-text-2);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.neg {
  color: #cf1322;
}
.void {
  color: var(--app-text-3);
  text-decoration: line-through;
}
</style>
