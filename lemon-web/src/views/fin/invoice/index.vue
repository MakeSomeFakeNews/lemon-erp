<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" destroy-inactive-tab-pane>
        <template #rightExtra>
          <a-space v-if="tab !== 'report'">
            <template v-if="tab === 'INPUT'">
              <a-button v-perm="'fin:invoice:edit'" :disabled="!certifiable.length" @click="openCertify">
                <template #icon><SafetyCertificateOutlined /></template>{{ $t('fin.invoice.certifyN', { n: certifiable.length }) }}
              </a-button>
            </template>
            <a-dropdown v-perm="'fin:invoice:edit'">
              <a-button><template #icon><ImportOutlined /></template>{{ $t('common.action.import') }}</a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="xml" @click="openXmlImport">{{ $t('fin.invoice.xmlFile') }}</a-menu-item>
                  <a-menu-item key="excel" @click="excelImportOpen = true">{{ $t('fin.invoice.excelList') }}</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-button v-perm="'fin:invoice:edit'" type="primary" @click="openCreate">
              <template #icon><PlusOutlined /></template>{{ conf.registerTitle }}
            </a-button>
          </a-space>
        </template>

        <a-tab-pane v-for="k in ['OUTPUT', 'INPUT']" :key="k" :tab="KINDS[k].label">
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
            :row-selection="k === 'INPUT' ? { ...rowSelection, getCheckboxProps: (r) => ({ disabled: r.status !== 'VALID' }) } : undefined"
            :scroll="{ x: scrollX }"
            @change="onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'invoiceNo'">
                <a @click="openDetail(record)">{{ record.invoiceNo }}</a>
              </template>
              <template v-else-if="column.key === 'invoiceType'"><DictTag type="fin_invoice_type" :value="record.invoiceType" /></template>
              <template v-else-if="column.key === 'amount'">
                <span :class="{ neg: Number(record.amount) < 0, void: record.status === 'VOID' }">¥{{ formatMoney(record.amount) }}</span>
              </template>
              <template v-else-if="column.key === 'taxAmount'">
                <span :class="{ void: record.status === 'VOID' }">{{ formatMoney(record.taxAmount) }}</span>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="record.status === 'VOID' ? 'default' : 'green'" :bordered="false">{{ record.status === 'VOID' ? $t('fin.invoice.statusVoid') : $t('fin.invoice.statusValid') }}</a-tag>
              </template>
              <template v-else-if="column.key === 'matched'">
                <span v-if="record.status === 'VOID'" class="sub-text">-</span>
                <a-tag v-else-if="unmatchedAmount(record) === 0" color="green" :bordered="false">{{ $t('fin.invoice.matched') }}</a-tag>
                <a-tag v-else color="orange" :bordered="false">{{ $t('fin.invoice.unmatchedAmt', { amount: formatMoney(unmatchedAmount(record)) }) }}</a-tag>
              </template>
              <template v-else-if="column.key === 'certify'">
                <a-tag v-if="record.certifyPeriod" color="blue" :bordered="false">{{ periodText(record.certifyPeriod) }}</a-tag>
                <span v-else-if="record.status === 'VALID' && SPECIAL_TYPES.includes(record.invoiceType)" class="sub-text">{{ $t('fin.invoice.uncertified') }}</span>
                <span v-else class="sub-text">-</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <span class="table-actions">
                  <a-button type="link" size="small" @click="openDetail(record)">{{ $t('common.action.view') }}</a-button>
                  <a-button v-if="record.status === 'VALID' && unmatchedAmount(record) !== 0" v-perm="'fin:invoice:edit'" type="link" size="small" @click="openMatch(record)">
                    {{ $t('fin.invoice.match') }}
                  </a-button>
                  <span v-if="record.status === 'VALID'" v-perm="'fin:invoice:edit'">
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
          <div class="filters">
            <a-radio-group v-model:value="reportKind" button-style="solid" @change="loadReport">
              <a-radio-button value="OUTPUT">{{ $t('fin.invoice.reportOutput') }}</a-radio-button>
              <a-radio-button value="INPUT">{{ $t('fin.invoice.reportInput') }}</a-radio-button>
            </a-radio-group>
            <span>{{ $t('fin.invoice.asOf') }}</span>
            <a-date-picker v-model:value="asOf" value-format="YYYY-MM-DD" :allow-clear="false" @change="loadReport" />
            <span class="sub-text">
              {{ reportKind === 'OUTPUT' ? $t('fin.invoice.reportOutputTip') : $t('fin.invoice.reportInputTip') }}
            </span>
          </div>
          <div class="stats">
            <span>{{ reportKind === 'OUTPUT' ? $t('fin.invoice.customerCount', { n: report.length }) : $t('fin.invoice.supplierCount', { n: report.length }) }}</span>
            <span>{{ $t('fin.invoice.billAmount') }} ¥{{ formatMoney(reportTotal.amount) }}</span>
            <span>{{ reportKind === 'OUTPUT' ? $t('fin.invoice.invoicedOut') : $t('fin.invoice.invoicedIn') }} ¥{{ formatMoney(reportTotal.invoiced) }}</span>
            <span>{{ reportKind === 'OUTPUT' ? $t('fin.invoice.uninvoicedOut') : $t('fin.invoice.estimated') }} <b class="neg">¥{{ formatMoney(reportTotal.uninvoiced) }}</b></span>
            <span>{{ $t('fin.invoice.ofWhichTax') }} ¥{{ formatMoney(reportTotal.uninvoicedTax) }}</span>
          </div>
          <a-table
            row-key="partnerId"
            size="middle"
            :columns="reportColumns"
            :data-source="report"
            :loading="reportLoading"
            :pagination="report.length > 20 ? { pageSize: 20, showTotal: (total) => $t('common.table.total', { total }) } : false"
            :scroll="{ x: 900 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="['amount', 'invoiced', 'uninvoiced', 'uninvoicedTax'].includes(column.key)">
                <span :class="{ bold: column.key === 'uninvoiced' }">{{ formatMoney(record[column.key]) }}</span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 登记发票：选择往来单位后列出未全部开票的单据，勾选并填写本次勾稽金额 -->
    <SubPage :open="createOpen" :title="conf.registerTitle" width="min(1000px, 100vw)" destroy-on-close @close="createOpen = false">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="conf.partnerLabel" name="partnerId" :rules="[{ required: true, message: $t('common.rule.requiredSelect', { name: conf.partnerLabel }) }]">
              <PartnerSelect v-model:value="form.partnerId" :type="conf.partner" @update:value="loadBills" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.invoice.invoiceNo')" name="invoiceNo" :rules="[{ required: true, whitespace: true, message: $t('common.rule.required', { name: $t('fin.invoice.invoiceNo') }) }]">
              <a-input v-model:value="form.invoiceNo" :maxlength="32" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('fin.invoice.invoiceType')" name="invoiceType">
              <a-select v-model:value="form.invoiceType" :options="toOptions(dict.fin_invoice_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('fin.invoice.invoiceDate')" name="invoiceDate" :rules="[{ required: true, message: $t('common.rule.requiredSelect', { name: $t('fin.invoice.invoiceDate') }) }]">
              <a-date-picker v-model:value="form.invoiceDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('fin.invoice.amount')" name="amount" :rules="[{ required: true, message: $t('common.rule.required', { name: $t('fin.invoice.amount') }) }]" :extra="$t('fin.invoice.amountExtra')">
              <a-input-number v-model:value="form.amount" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('fin.invoice.taxAmount')" name="taxAmount" :rules="[{ required: true, message: $t('common.rule.required', { name: $t('fin.invoice.taxAmount') }) }]" :extra="$t('fin.invoice.taxExtra')">
              <a-input-number v-model:value="form.taxAmount" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <div class="bills-head">
        <span class="section-title">{{ conf.billsTitle }}</span>
        <a-space>
          <span>{{ $t('fin.invoice.matchTotal') }} <b class="total">¥{{ formatMoney(itemTotal) }}</b></span>
          <a-tag v-if="form.amount != null && Math.abs(itemTotal - Number(form.amount)) >= 0.005" color="red" :bordered="false">{{ $t('fin.invoice.mismatch') }}</a-tag>
        </a-space>
      </div>
      <a-table
        row-key="id"
        size="small"
        :columns="billColumns"
        :data-source="bills"
        :loading="billLoading"
        :pagination="false"
        :row-selection="{ selectedRowKeys: checkedBills, onChange: onCheckBills }"
        :scroll="{ x: 860 }"
        :locale="{ emptyText: form.partnerId ? conf.noBills : conf.selectPartnerFirst }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            {{ formatMoney(record.amount) }}
            <div class="sub-text">{{ $t('fin.invoice.taxAmount') }} {{ formatMoney(record.taxAmount) }}</div>
          </template>
          <template v-else-if="column.key === 'invoiced'">{{ formatMoney(record.invoicedAmount ?? 0) }}</template>
          <template v-else-if="column.key === 'remaining'">{{ formatMoney(billRemaining(record)) }}</template>
          <template v-else-if="column.key === 'apply'">
            <a-input-number
              v-model:value="record.apply"
              :precision="2"
              :min="Math.min(0, billRemaining(record))"
              :max="Math.max(0, billRemaining(record))"
              :disabled="!checkedBills.includes(record.id)"
              class="full"
              @change="recalc"
            />
          </template>
        </template>
      </a-table>
      <template #footer>
        <div class="drawer-footer">
          <a-button @click="createOpen = false">{{ $t('common.action.cancel') }}</a-button>
          <a-button type="primary" :loading="saving" @click="submit">{{ $t('common.action.save') }}</a-button>
        </div>
      </template>
    </SubPage>

    <!-- 详情 -->
    <SubPage :open="detailOpen" :title="detail ? $t('fin.invoice.detailTitle', { no: detail.invoiceNo }) : ''" width="min(820px, 100vw)" @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" class="block">
          <a-descriptions-item :label="$t('fin.invoice.kind')">{{ KINDS[detail.kind]?.label }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.invoice.invoiceType')"><DictTag type="fin_invoice_type" :value="detail.invoiceType" /></a-descriptions-item>
          <a-descriptions-item :label="KINDS[detail.kind]?.partnerLabel">{{ detail.partnerName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.invoice.invoiceDate')">{{ detail.invoiceDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.invoice.amount')"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item :label="$t('fin.invoice.taxAmount')">¥{{ formatMoney(detail.taxAmount) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')">
            <a-tag :color="detail.status === 'VOID' ? 'default' : 'green'" :bordered="false">{{ detail.status === 'VOID' ? $t('fin.invoice.statusVoid') : $t('fin.invoice.statusValid') }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.kind === 'INPUT'" :label="$t('fin.invoice.certifyPeriod')">{{ detail.certifyPeriod ? periodText(detail.certifyPeriod) : $t('fin.invoice.uncertified') }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.invoice.registeredBy')">{{ detail.createByName || '-' }} {{ detail.createTime || '' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.invoice.source')">{{ SOURCES[detail.source] || SOURCES.MANUAL }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.sellerName || detail.sellerTaxNo" :label="$t('fin.invoice.seller')">{{ detail.sellerName }} {{ detail.sellerTaxNo }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.buyerName || detail.buyerTaxNo" :label="$t('fin.invoice.buyer')">{{ detail.buyerName }} {{ detail.buyerTaxNo }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <template v-if="detail.lines?.length">
          <div class="section-title block-title">{{ $t('fin.invoice.lines') }}</div>
          <a-table row-key="id" size="small" :columns="lineColumns" :data-source="detail.lines" :pagination="false" :scroll="{ x: 760 }" class="block">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'qty'">{{ record.qty != null ? Number(record.qty) : '' }} {{ record.unit || '' }}</template>
              <template v-else-if="column.key === 'price'">{{ record.price != null ? Number(record.price) : '' }}</template>
              <template v-else-if="column.key === 'taxRate'">{{ record.taxRate != null ? `${Math.round(Number(record.taxRate) * 10000) / 100}%` : '' }}</template>
              <template v-else-if="['amount', 'taxAmount'].includes(column.key)">{{ formatMoney(record[column.key]) }}</template>
            </template>
          </a-table>
        </template>
        <div class="bills-head">
          <span class="section-title">{{ $t('fin.invoice.matchedBills', { amount: formatMoney(detail.matchedAmount) }) }}</span>
          <a-popconfirm
            v-if="detail.status === 'VALID' && detail.items?.length"
            :title="$t('fin.invoice.confirmUnmatch')"
            @confirm="unmatch(detail.id)"
          >
            <a-button v-perm="'fin:invoice:edit'" size="small">{{ $t('fin.invoice.unmatch') }}</a-button>
          </a-popconfirm>
        </div>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="['billAmount', 'amount'].includes(column.key)">{{ formatMoney(record[column.key]) }}</template>
          </template>
        </a-table>
        <div class="section-title block-title attach-title">{{ $t('fin.invoice.attachments') }}</div>
        <AttachmentPanel biz-type="fin_invoice" :biz-id="detail.id" :editable="userStore.hasPerm('fin:invoice:edit')" />
      </template>
    </SubPage>

    <!-- 勾稽导入的发票 -->
    <a-modal
      v-model:open="matchOpen"
      :title="matchInvoiceRow ? $t('fin.invoice.matchTitle', { no: matchInvoiceRow.invoiceNo }) : ''"
      width="min(900px, 100vw)"
      :confirm-loading="matching"
      :ok-text="$t('fin.invoice.match')"
      @ok="submitMatch"
    >
      <template v-if="matchInvoiceRow">
        <p>
          {{ $t('fin.invoice.matchSummary', { partner: matchInvoiceRow.partnerName, amount: formatMoney(matchInvoiceRow.amount) }) }}
          <b>¥{{ formatMoney(unmatchedAmount(matchInvoiceRow)) }}</b>{{ $t('fin.invoice.matchSummary2') }} <b class="total">¥{{ formatMoney(itemTotal) }}</b>
        </p>
        <a-table
          row-key="id"
          size="small"
          :columns="billColumns"
          :data-source="bills"
          :loading="billLoading"
          :pagination="false"
          :row-selection="{ selectedRowKeys: checkedBills, onChange: onCheckBills }"
          :scroll="{ x: 860, y: 360 }"
          :locale="{ emptyText: conf.noBills }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template>
            <template v-else-if="column.key === 'invoiced'">{{ formatMoney(record.invoicedAmount ?? 0) }}</template>
            <template v-else-if="column.key === 'remaining'">{{ formatMoney(billRemaining(record)) }}</template>
            <template v-else-if="column.key === 'apply'">
              <a-input-number
                v-model:value="record.apply"
                :precision="2"
                :min="Math.min(0, billRemaining(record))"
                :max="Math.max(0, billRemaining(record))"
                :disabled="!checkedBills.includes(record.id)"
                class="full"
              />
            </template>
          </template>
        </a-table>
      </template>
    </a-modal>

    <!-- 数电票 XML 导入 -->
    <a-modal v-model:open="xmlOpen" :title="$t('fin.invoice.xmlTitle', { label: conf.label })" :confirm-loading="xmlImporting" :ok-text="$t('common.action.import')" :ok-button-props="{ disabled: !xmlFiles.length }" @ok="submitXml">
      <a-space direction="vertical" class="full">
        <a-alert type="info" show-icon :message="xmlTip" />
        <a-upload-dragger :file-list="[]" accept=".xml" multiple :before-upload="addXml">
          <p class="ant-upload-text">{{ $t('fin.invoice.xmlDrop') }}</p>
          <p v-if="xmlFiles.length" class="ant-upload-hint">{{ $t('fin.invoice.filesSelected', { n: xmlFiles.length }) }}</p>
        </a-upload-dragger>
        <a-checkbox v-model:checked="autoMatch">{{ conf.autoMatchTip }}</a-checkbox>
        <a-alert v-if="importResult && !importResult.errors.length" type="success" show-icon :message="importResultText" />
        <a-table v-if="importResult?.errors.length" size="small" row-key="rowNum" :columns="errorColumns" :data-source="importResult.errors" :pagination="false" />
      </a-space>
    </a-modal>

    <ImportModal
      v-model:open="excelImportOpen"
      :title="conf.listTitle"
      :columns="excelColumns"
      :importer="importExcel"
      :sample="{ invoiceNo: '25322000000123456789', invoiceDate: '2026-10-08', sellerTaxNo: '91320000MA1XXXXX0X', sellerName: $t('fin.invoice.sampleSeller'), amount: 1130, taxAmount: 130 }"
      @success="load"
    />

    <!-- 进项认证 -->
    <a-modal v-model:open="certifyOpen" :title="$t('fin.invoice.certifyTitle')" :confirm-loading="certifying" :ok-text="$t('fin.invoice.certify')" @ok="certify(certifyPeriod)">
      <p>{{ $t('fin.invoice.certifyTip', { n: certifiable.length }) }}</p>
      <a-space>
        <span>{{ $t('fin.invoice.certifyPeriod') }}</span>
        <PeriodPicker v-model:value="certifyPeriod" />
        <a-popconfirm :title="$t('fin.invoice.confirmUncertify')" @confirm="certify(null)">
          <a-button :loading="certifying">{{ $t('fin.invoice.uncertify') }}</a-button>
        </a-popconfirm>
      </a-space>
    </a-modal>
  </div>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { ImportOutlined, PlusOutlined, SafetyCertificateOutlined } from '@ant-design/icons-vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import ImportModal from '@/components/ImportModal.vue'
import { useUserStore } from '@/store/user'
import DictTag from '@/components/DictTag.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import {
  addInvoice,
  cancelInvoice,
  certifyInvoice,
  getInvoice,
  importInvoiceRows,
  importInvoiceXml,
  invoiceOpenBills,
  matchInvoice,
  pageInvoice,
  unmatchInvoice,
  uninvoiced
} from '@/api/fin'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { today } from '@/components/wms/wms'
import { currentPeriod, formatMoney, periodText } from '@/components/fin/fin'

const { t } = useI18n()
const kindConf = (k, partner) => ({
  label: t(`fin.invoice.kinds.${k}.label`),
  partner,
  partnerLabel: t(`fin.invoice.kinds.${k}.partner`),
  allPartners: t(`fin.invoice.kinds.${k}.allPartners`),
  registerTitle: t(`fin.invoice.kinds.${k}.register`),
  listTitle: t(`fin.invoice.kinds.${k}.list`),
  billsTitle: t(`fin.invoice.kinds.${k}.billsTitle`),
  noBills: t(`fin.invoice.kinds.${k}.noBills`),
  selectPartnerFirst: t(`fin.invoice.kinds.${k}.selectPartnerFirst`),
  autoMatchTip: t(`fin.invoice.kinds.${k}.autoMatchTip`)
})
const KINDS = {
  OUTPUT: kindConf('OUTPUT', 'customer'),
  INPUT: kindConf('INPUT', 'supplier')
}
const STATUS_OPTIONS = [
  { value: 'VALID', label: t('fin.invoice.statusValid') },
  { value: 'VOID', label: t('fin.invoice.statusVoid') }
]
const dict = useDict('fin_invoice_type')
const userStore = useUserStore()
/** 可认证抵扣的专票（含数电票专票） */
const SPECIAL_TYPES = ['SPECIAL', 'E_SPECIAL']
const SOURCES = { MANUAL: t('fin.invoice.sources.MANUAL'), XML: t('fin.invoice.sources.XML'), EXCEL: t('fin.invoice.sources.EXCEL') }
const unmatchedAmount = (r) => round2(Number(r.amount) - Number(r.matchedAmount || 0))
const tab = ref('OUTPUT')
// 登记、列表使用的种类：报表页签时沿用上一个发票页签
const kind = ref('OUTPUT')
const conf = computed(() => KINDS[kind.value])
const round2 = (n) => Math.round(n * 100) / 100

const columns = computed(() => [
  { title: t('fin.invoice.invoiceNo'), key: 'invoiceNo', width: 160 },
  { title: t('common.field.type'), key: 'invoiceType', width: 140 },
  { title: conf.value.partnerLabel, dataIndex: 'partnerName', width: 200, ellipsis: true },
  { title: t('fin.invoice.invoiceDate'), dataIndex: 'invoiceDate', width: 120 },
  { title: t('fin.invoice.amount'), key: 'amount', width: 140, align: 'right' },
  { title: t('fin.invoice.taxAmount'), key: 'taxAmount', width: 120, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('fin.invoice.match'), key: 'matched', width: 170 },
  ...(kind.value === 'INPUT' ? [{ title: t('fin.invoice.certifyPeriod'), key: 'certify', width: 140 }] : []),
  { title: t('common.field.remark'), dataIndex: 'remark', width: 200, ellipsis: true },
  { title: t('fin.invoice.registeredBy'), dataIndex: 'createByName', width: 120 },
  { title: t('common.action.operation'), key: 'action', width: 150, fixed: 'right' }
])
// 进项发票带勾选列
const scrollX = computed(() => columns.value.reduce((sum, c) => sum + c.width, 0) + (kind.value === 'INPUT' ? 32 : 0))
const query = reactive({ kind: 'OUTPUT', partnerId: undefined, status: undefined, keyword: undefined, unmatched: false })
const { list, loading, pagination, selectedKeys, rowSelection, load, search, onTableChange } = useTable(pageInvoice, query)
onMounted(load)
watch(() => query.partnerId, () => search())
watch(tab, (t) => {
  if (t === 'report') {
    loadReport()
    return
  }
  kind.value = t
  Object.assign(query, { kind: t, partnerId: undefined, status: undefined, keyword: undefined, unmatched: false })
  selectedKeys.value = []
  search()
})

const pageTotal = computed(() =>
  list.value
    .filter((r) => r.status !== 'VOID')
    .reduce((t, r) => ({ amount: t.amount + Number(r.amount), tax: t.tax + Number(r.taxAmount) }), { amount: 0, tax: 0 })
)

async function cancel(id) {
  await cancelInvoice(id)
  message.success(t('fin.invoice.voided'))
  load()
}

// ---------------------------------------------------------------- 认证
const certifiable = computed(() => list.value.filter((r) => selectedKeys.value.includes(r.id) && r.status === 'VALID').map((r) => r.id))
const certifyOpen = ref(false)
const certifying = ref(false)
const certifyPeriod = ref(currentPeriod())
function openCertify() {
  certifyPeriod.value = currentPeriod()
  certifyOpen.value = true
}
async function certify(period) {
  certifying.value = true
  try {
    await certifyInvoice(certifiable.value, period)
    message.success(period ? t('fin.invoice.certified', { period: periodText(period) }) : t('fin.invoice.uncertified2'))
    certifyOpen.value = false
    selectedKeys.value = []
    load()
  } finally {
    certifying.value = false
  }
}

// ---------------------------------------------------------------- 登记
const billColumns = [
  { title: t('fin.invoice.billNo'), dataIndex: 'billNo', width: 150 },
  { title: t('fin.invoice.sourceNo'), dataIndex: 'sourceNo', width: 160, ellipsis: true },
  { title: t('common.field.date'), dataIndex: 'billDate', width: 100 },
  { title: t('common.field.amount'), key: 'amount', width: 120, align: 'right' },
  { title: t('fin.invoice.matched'), key: 'invoiced', width: 110, align: 'right' },
  { title: t('fin.invoice.unmatched'), key: 'remaining', width: 110, align: 'right' },
  { title: t('fin.invoice.applyNow'), key: 'apply', width: 150 }
]
const createOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const bills = ref([])
const billLoading = ref(false)
const checkedBills = ref([])
const billRemaining = (b) => round2(Number(b.amount) - Number(b.invoicedAmount || 0))
const selectedBills = computed(() => bills.value.filter((b) => checkedBills.value.includes(b.id)))
const itemTotal = computed(() => round2(selectedBills.value.reduce((s, b) => s + (Number(b.apply) || 0), 0)))

function openCreate() {
  form.value = { kind: kind.value, partnerId: undefined, invoiceNo: '', invoiceType: 'SPECIAL', invoiceDate: today(), amount: undefined, taxAmount: undefined, remark: '' }
  bills.value = []
  checkedBills.value = []
  createOpen.value = true
}

async function loadBills(partnerId) {
  bills.value = []
  checkedBills.value = []
  recalc()
  if (!partnerId) return
  billLoading.value = true
  try {
    bills.value = (await invoiceOpenBills(form.value.kind, partnerId)).map((b) => ({ ...b, apply: billRemaining(b) }))
  } finally {
    billLoading.value = false
  }
}

function onCheckBills(keys) {
  // 新勾选的单据默认勾稽全部未开票金额
  bills.value.forEach((b) => {
    if (keys.includes(b.id) && !checkedBills.value.includes(b.id) && !Number(b.apply)) b.apply = billRemaining(b)
  })
  checkedBills.value = keys
  recalc()
}

/** 按勾稽金额重新计算价税合计、税额（税额 = 单据税额 × 勾稽金额 / 单据金额） */
function recalc() {
  const rows = selectedBills.value
  if (!rows.length) {
    form.value.amount = undefined
    form.value.taxAmount = undefined
    return
  }
  form.value.amount = itemTotal.value
  form.value.taxAmount = round2(
    rows.reduce((s, b) => s + (Number(b.amount) ? (Number(b.taxAmount || 0) * (Number(b.apply) || 0)) / Number(b.amount) : 0), 0)
  )
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  const items = selectedBills.value.filter((b) => Number(b.apply)).map((b) => ({ billId: b.id, amount: b.apply }))
  if (!items.length) {
    message.warning(t('fin.invoice.selectBills'))
    return
  }
  if (Math.abs(itemTotal.value - Number(form.value.amount)) >= 0.005) {
    message.warning(t('fin.invoice.totalMismatch', { amount: formatMoney(itemTotal.value) }))
    return
  }
  saving.value = true
  try {
    await addInvoice({ ...form.value, invoiceNo: form.value.invoiceNo.trim(), remark: form.value.remark || null, items })
    message.success(t('fin.invoice.registered'))
    createOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 详情
const itemColumns = [
  { title: t('fin.invoice.bill'), dataIndex: 'billNo', width: 160 },
  { title: t('fin.invoice.sourceNo'), dataIndex: 'sourceNo', width: 170 },
  { title: t('fin.invoice.billAmount'), key: 'billAmount', width: 130, align: 'right' },
  { title: t('fin.invoice.matchAmount'), key: 'amount', width: 140, align: 'right' }
]
const detailOpen = ref(false)
const detail = ref()
async function openDetail(record) {
  detail.value = undefined
  detailOpen.value = true
  detail.value = await getInvoice(record.id)
}

const lineColumns = [
  { title: t('fin.invoice.itemName'), dataIndex: 'itemName', width: 200, ellipsis: true },
  { title: t('fin.invoice.spec'), dataIndex: 'spec', width: 110, ellipsis: true },
  { title: t('common.field.quantity'), key: 'qty', width: 100, align: 'right' },
  { title: t('common.field.price'), key: 'price', width: 100, align: 'right' },
  { title: t('common.field.amount'), key: 'amount', width: 110, align: 'right' },
  { title: t('fin.invoice.taxRate'), key: 'taxRate', width: 80, align: 'right' },
  { title: t('fin.invoice.taxAmount'), key: 'taxAmount', width: 110, align: 'right' }
]

async function unmatch(id) {
  await unmatchInvoice(id)
  message.success(t('fin.invoice.unmatched2'))
  detail.value = await getInvoice(id)
  load()
}

// ---------------------------------------------------------------- 勾稽
const matchOpen = ref(false)
const matching = ref(false)
const matchInvoiceRow = ref()
async function openMatch(record) {
  matchInvoiceRow.value = record
  bills.value = []
  checkedBills.value = []
  matchOpen.value = true
  billLoading.value = true
  try {
    const left = unmatchedAmount(record)
    bills.value = (await invoiceOpenBills(record.kind, record.partnerId)).map((b) => ({ ...b, apply: billRemaining(b) }))
    // 金额相等的单据默认勾选
    const exact = bills.value.find((b) => billRemaining(b) === left)
    if (exact) checkedBills.value = [exact.id]
  } finally {
    billLoading.value = false
  }
}
async function submitMatch() {
  const items = selectedBills.value.filter((b) => Number(b.apply)).map((b) => ({ billId: b.id, amount: b.apply }))
  if (!items.length) {
    message.warning(t('fin.invoice.selectBills'))
    return
  }
  matching.value = true
  try {
    await matchInvoice(matchInvoiceRow.value.id, items)
    message.success(t('fin.invoice.matched'))
    matchOpen.value = false
    load()
  } finally {
    matching.value = false
  }
}

// ---------------------------------------------------------------- 导入
const autoMatch = ref(true)
const importResult = ref()
const errorColumns = [
  { title: t('common.table.index'), dataIndex: 'rowNum', width: 70 },
  { title: t('fin.invoice.error'), dataIndex: 'message' }
]
const importResultText = computed(() => {
  const r = importResult.value
  if (!r) return ''
  return [
    t('fin.invoice.importedN', { n: r.imported }),
    ...(r.skipped ? [t('fin.invoice.skippedN', { n: r.skipped })] : []),
    ...(r.matched ? [t('fin.invoice.autoMatchedN', { n: r.matched })] : [])
  ].join(t('fin.invoice.sep'))
})
const xmlTip = computed(() =>
  kind.value === 'INPUT'
    ? t('fin.invoice.xmlTipInput')
    : t('fin.invoice.xmlTipOutput')
)
const xmlOpen = ref(false)
const xmlImporting = ref(false)
const xmlFiles = ref([])
function openXmlImport() {
  xmlFiles.value = []
  importResult.value = undefined
  xmlOpen.value = true
}
function addXml(file) {
  if (!xmlFiles.value.some((f) => f.name === file.name && f.size === file.size)) xmlFiles.value.push(file)
  return false
}
async function submitXml() {
  xmlImporting.value = true
  try {
    importResult.value = await importInvoiceXml(kind.value, xmlFiles.value, autoMatch.value)
    if (!importResult.value.errors.length) {
      message.success(importResultText.value)
      xmlFiles.value = []
      load()
    }
  } finally {
    xmlImporting.value = false
  }
}

const excelImportOpen = ref(false)
const excelColumns = [
  { title: t('fin.invoice.invoiceNo'), key: 'invoiceNo', required: true },
  { title: t('fin.invoice.invoiceType'), key: 'invoiceType', dict: 'fin_invoice_type' },
  { title: t('fin.invoice.invoiceDate'), key: 'invoiceDate', type: 'date', required: true },
  { title: t('fin.invoice.sellerTaxNo'), key: 'sellerTaxNo' },
  { title: t('fin.invoice.sellerName'), key: 'sellerName' },
  { title: t('fin.invoice.buyerTaxNo'), key: 'buyerTaxNo' },
  { title: t('fin.invoice.buyerName'), key: 'buyerName' },
  { title: t('fin.invoice.amount'), key: 'amount', type: 'number', required: true },
  { title: t('fin.invoice.taxAmount'), key: 'taxAmount', type: 'number', required: true }
]
async function importExcel(data) {
  const r = await importInvoiceRows(kind.value, data, true)
  if (!r.errors.length && (r.skipped || r.matched)) {
    message.info([...(r.skipped ? [t('fin.invoice.skippedN', { n: r.skipped })] : []), t('fin.invoice.autoMatchedN', { n: r.matched })].join(t('fin.invoice.sep')))
  }
  return { total: r.total, imported: r.imported, errors: r.errors }
}

// ---------------------------------------------------------------- 未开票 / 暂估
const reportKind = ref('OUTPUT')
const asOf = ref(today())
const report = ref([])
const reportLoading = ref(false)
const reportColumns = computed(() => [
  { title: reportKind.value === 'OUTPUT' ? t('fin.invoice.customer') : t('fin.invoice.supplier'), dataIndex: 'partnerName', width: 240, ellipsis: true },
  { title: t('fin.invoice.billCount'), dataIndex: 'billCount', width: 100, align: 'right' },
  { title: t('fin.invoice.billAmount'), key: 'amount', width: 140, align: 'right' },
  { title: reportKind.value === 'OUTPUT' ? t('fin.invoice.invoicedOut') : t('fin.invoice.invoicedIn'), key: 'invoiced', width: 140, align: 'right' },
  { title: reportKind.value === 'OUTPUT' ? t('fin.invoice.uninvoicedOut') : t('fin.invoice.estimatedAmount'), key: 'uninvoiced', width: 150, align: 'right' },
  { title: t('fin.invoice.ofWhichTax'), key: 'uninvoicedTax', width: 140, align: 'right' }
])
const reportTotal = computed(() =>
  report.value.reduce(
    (t, r) => ({
      amount: t.amount + Number(r.amount || 0),
      invoiced: t.invoiced + Number(r.invoiced || 0),
      uninvoiced: t.uninvoiced + Number(r.uninvoiced || 0),
      uninvoicedTax: t.uninvoicedTax + Number(r.uninvoicedTax || 0)
    }),
    { amount: 0, invoiced: 0, uninvoiced: 0, uninvoicedTax: 0 }
  )
)
async function loadReport() {
  reportLoading.value = true
  try {
    report.value = await uninvoiced(reportKind.value, asOf.value)
  } finally {
    reportLoading.value = false
  }
}
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
.bills-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 12px;
}
.section-title {
  font-weight: 500;
}
.block-title {
  margin-bottom: 8px;
}
.total {
  font-size: 16px;
  color: var(--ant-color-primary, #1677ff);
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
.bold {
  font-weight: 600;
}
.full {
  width: 100%;
}
.block {
  margin-bottom: 16px;
}
.attach-title {
  margin-top: 16px;
}
</style>
