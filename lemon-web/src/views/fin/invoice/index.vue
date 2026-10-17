<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" destroy-inactive-tab-pane>
        <template #rightExtra>
          <a-space v-if="tab !== 'report'">
            <template v-if="tab === 'INPUT'">
              <a-button v-perm="'fin:invoice:edit'" :disabled="!certifiable.length" @click="openCertify">
                <template #icon><SafetyCertificateOutlined /></template>认证（{{ certifiable.length }}）
              </a-button>
            </template>
            <a-dropdown v-perm="'fin:invoice:edit'">
              <a-button><template #icon><ImportOutlined /></template>导入</a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="xml" @click="openXmlImport">数电票 XML 文件</a-menu-item>
                  <a-menu-item key="excel" @click="excelImportOpen = true">发票清单（Excel）</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-button v-perm="'fin:invoice:edit'" type="primary" @click="openCreate">
              <template #icon><PlusOutlined /></template>登记{{ conf.label }}
            </a-button>
          </a-space>
        </template>

        <a-tab-pane v-for="k in ['OUTPUT', 'INPUT']" :key="k" :tab="KINDS[k].label">
          <div class="filters">
            <PartnerSelect v-model:value="query.partnerId" :type="KINDS[k].partner" allow-clear :placeholder="`全部${KINDS[k].partnerLabel}`" class="filter" />
            <a-select v-model:value="query.status" placeholder="状态" allow-clear :options="STATUS_OPTIONS" class="filter-s" @change="search" />
            <a-input-search v-model:value="query.keyword" placeholder="发票号码或备注" allow-clear class="filter" @search="search" />
            <a-checkbox v-model:checked="query.unmatched" @change="search">只看待勾稽</a-checkbox>
          </div>
          <div class="stats">
            <span>本页价税合计 <b>¥{{ formatMoney(pageTotal.amount) }}</b></span>
            <span>税额 ¥{{ formatMoney(pageTotal.tax) }}</span>
            <span class="sub-text">已作废的不计入</span>
          </div>
          <a-table
            row-key="id"
            size="middle"
            :columns="columns"
            :data-source="list"
            :loading="loading"
            :pagination="pagination"
            :row-selection="k === 'INPUT' ? { ...rowSelection, getCheckboxProps: (r) => ({ disabled: r.status !== 'VALID' }) } : undefined"
            :scroll="{ x: 1200 }"
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
                <a-tag :color="record.status === 'VOID' ? 'default' : 'green'" :bordered="false">{{ record.status === 'VOID' ? '已作废' : '有效' }}</a-tag>
              </template>
              <template v-else-if="column.key === 'matched'">
                <span v-if="record.status === 'VOID'" class="sub-text">-</span>
                <a-tag v-else-if="unmatchedAmount(record) === 0" color="green" :bordered="false">已勾稽</a-tag>
                <a-tag v-else color="orange" :bordered="false">待勾稽 {{ formatMoney(unmatchedAmount(record)) }}</a-tag>
              </template>
              <template v-else-if="column.key === 'certify'">
                <a-tag v-if="record.certifyPeriod" color="blue" :bordered="false">{{ periodText(record.certifyPeriod) }}</a-tag>
                <span v-else-if="record.status === 'VALID' && SPECIAL_TYPES.includes(record.invoiceType)" class="sub-text">未认证</span>
                <span v-else class="sub-text">-</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <span class="table-actions">
                  <a-button type="link" size="small" @click="openDetail(record)">查看</a-button>
                  <a-button v-if="record.status === 'VALID' && unmatchedAmount(record) !== 0" v-perm="'fin:invoice:edit'" type="link" size="small" @click="openMatch(record)">
                    勾稽
                  </a-button>
                  <span v-if="record.status === 'VALID'" v-perm="'fin:invoice:edit'">
                    <a-popconfirm :title="`作废发票 ${record.invoiceNo}？作废后释放勾稽的单据开票金额。`" @confirm="cancel(record.id)">
                      <a-button type="link" danger size="small">作废</a-button>
                    </a-popconfirm>
                  </span>
                </span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="report" tab="未开票 · 暂估">
          <div class="filters">
            <a-radio-group v-model:value="reportKind" button-style="solid" @change="loadReport">
              <a-radio-button value="OUTPUT">未开票收入（销项）</a-radio-button>
              <a-radio-button value="INPUT">暂估应付（进项）</a-radio-button>
            </a-radio-group>
            <span>截止日期</span>
            <a-date-picker v-model:value="asOf" value-format="YYYY-MM-DD" :allow-clear="false" @change="loadReport" />
            <span class="sub-text">
              {{ reportKind === 'OUTPUT' ? '已确认应收但尚未开具销项发票的金额' : '已入库形成应付但尚未收到进项发票的金额（月末暂估）' }}
            </span>
          </div>
          <div class="stats">
            <span>{{ reportKind === 'OUTPUT' ? '客户' : '供应商' }} {{ report.length }} 家</span>
            <span>单据金额 ¥{{ formatMoney(reportTotal.amount) }}</span>
            <span>{{ reportKind === 'OUTPUT' ? '已开票' : '已收票' }} ¥{{ formatMoney(reportTotal.invoiced) }}</span>
            <span>{{ reportKind === 'OUTPUT' ? '未开票' : '暂估' }} <b class="neg">¥{{ formatMoney(reportTotal.uninvoiced) }}</b></span>
            <span>其中税额 ¥{{ formatMoney(reportTotal.uninvoicedTax) }}</span>
          </div>
          <a-table
            row-key="partnerId"
            size="middle"
            :columns="reportColumns"
            :data-source="report"
            :loading="reportLoading"
            :pagination="report.length > 20 ? { pageSize: 20, showTotal: (t) => `共 ${t} 条` } : false"
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
    <SubPage :open="createOpen" :title="`登记${conf.label}`" width="min(1000px, 100vw)" destroy-on-close @close="createOpen = false">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="conf.partnerLabel" name="partnerId" :rules="[{ required: true, message: `请选择${conf.partnerLabel}` }]">
              <PartnerSelect v-model:value="form.partnerId" :type="conf.partner" @update:value="loadBills" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="发票号码" name="invoiceNo" :rules="[{ required: true, whitespace: true, message: '请填写发票号码' }]">
              <a-input v-model:value="form.invoiceNo" :maxlength="32" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="发票类型" name="invoiceType">
              <a-select v-model:value="form.invoiceType" :options="toOptions(dict.fin_invoice_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="开票日期" name="invoiceDate" :rules="[{ required: true, message: '请选择开票日期' }]">
              <a-date-picker v-model:value="form.invoiceDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="价税合计" name="amount" :rules="[{ required: true, message: '请填写价税合计' }]" extra="默认为勾稽金额合计">
              <a-input-number v-model:value="form.amount" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="税额" name="taxAmount" :rules="[{ required: true, message: '请填写税额' }]" extra="按单据税额比例计算，可修改">
              <a-input-number v-model:value="form.taxAmount" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <div class="bills-head">
        <span class="section-title">勾稽{{ conf.billLabel }}（未全部{{ conf.invoiceVerb }}的单据）</span>
        <a-space>
          <span>勾稽合计 <b class="total">¥{{ formatMoney(itemTotal) }}</b></span>
          <a-tag v-if="form.amount != null && Math.abs(itemTotal - Number(form.amount)) >= 0.005" color="red" :bordered="false">与价税合计不一致</a-tag>
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
        :locale="{ emptyText: form.partnerId ? `没有未${conf.invoiceVerb}的单据` : `请先选择${conf.partnerLabel}` }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            {{ formatMoney(record.amount) }}
            <div class="sub-text">税额 {{ formatMoney(record.taxAmount) }}</div>
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
          <a-button @click="createOpen = false">取消</a-button>
          <a-button type="primary" :loading="saving" @click="submit">保存</a-button>
        </div>
      </template>
    </SubPage>

    <!-- 详情 -->
    <SubPage :open="detailOpen" :title="detail ? `发票 ${detail.invoiceNo}` : ''" width="min(820px, 100vw)" @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" class="block">
          <a-descriptions-item label="种类">{{ KINDS[detail.kind]?.label }}</a-descriptions-item>
          <a-descriptions-item label="发票类型"><DictTag type="fin_invoice_type" :value="detail.invoiceType" /></a-descriptions-item>
          <a-descriptions-item :label="KINDS[detail.kind]?.partnerLabel">{{ detail.partnerName }}</a-descriptions-item>
          <a-descriptions-item label="开票日期">{{ detail.invoiceDate }}</a-descriptions-item>
          <a-descriptions-item label="价税合计"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item label="税额">¥{{ formatMoney(detail.taxAmount) }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="detail.status === 'VOID' ? 'default' : 'green'" :bordered="false">{{ detail.status === 'VOID' ? '已作废' : '有效' }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.kind === 'INPUT'" label="认证期间">{{ detail.certifyPeriod ? periodText(detail.certifyPeriod) : '未认证' }}</a-descriptions-item>
          <a-descriptions-item label="登记人">{{ detail.createByName || '-' }} {{ detail.createTime || '' }}</a-descriptions-item>
          <a-descriptions-item label="来源">{{ SOURCES[detail.source] || '手工登记' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.sellerName || detail.sellerTaxNo" label="销售方">{{ detail.sellerName }} {{ detail.sellerTaxNo }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.buyerName || detail.buyerTaxNo" label="购买方">{{ detail.buyerName }} {{ detail.buyerTaxNo }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="备注">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <template v-if="detail.lines?.length">
          <div class="section-title block-title">发票明细</div>
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
          <span class="section-title">勾稽单据（已勾稽 ¥{{ formatMoney(detail.matchedAmount) }}）</span>
          <a-popconfirm
            v-if="detail.status === 'VALID' && detail.items?.length"
            title="取消全部勾稽？单据的已开票金额会释放，之后可以重新勾稽。"
            @confirm="unmatch(detail.id)"
          >
            <a-button v-perm="'fin:invoice:edit'" size="small">取消勾稽</a-button>
          </a-popconfirm>
        </div>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="['billAmount', 'amount'].includes(column.key)">{{ formatMoney(record[column.key]) }}</template>
          </template>
        </a-table>
        <div class="section-title block-title attach-title">版式文件（PDF / OFD / XML）</div>
        <AttachmentPanel biz-type="fin_invoice" :biz-id="detail.id" :editable="userStore.hasPerm('fin:invoice:edit')" />
      </template>
    </SubPage>

    <!-- 勾稽导入的发票 -->
    <a-modal
      v-model:open="matchOpen"
      :title="matchInvoiceRow ? `勾稽发票 ${matchInvoiceRow.invoiceNo}` : ''"
      width="min(900px, 100vw)"
      :confirm-loading="matching"
      ok-text="勾稽"
      @ok="submitMatch"
    >
      <template v-if="matchInvoiceRow">
        <p>
          {{ matchInvoiceRow.partnerName }}，价税合计 ¥{{ formatMoney(matchInvoiceRow.amount) }}，待勾稽
          <b>¥{{ formatMoney(unmatchedAmount(matchInvoiceRow)) }}</b>；本次勾稽合计 <b class="total">¥{{ formatMoney(itemTotal) }}</b>
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
          :locale="{ emptyText: `没有未${conf.invoiceVerb}的单据` }"
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
    <a-modal v-model:open="xmlOpen" :title="`导入数电票 XML（${conf.label}）`" :confirm-loading="xmlImporting" ok-text="导入" :ok-button-props="{ disabled: !xmlFiles.length }" @ok="submitXml">
      <a-space direction="vertical" class="full">
        <a-alert type="info" show-icon :message="xmlTip" />
        <a-upload-dragger :file-list="[]" accept=".xml" multiple :before-upload="addXml">
          <p class="ant-upload-text">点击或拖拽数电票 XML 文件到这里（可多选，最多 200 个）</p>
          <p v-if="xmlFiles.length" class="ant-upload-hint">已选择 {{ xmlFiles.length }} 个文件</p>
        </a-upload-dragger>
        <a-checkbox v-model:checked="autoMatch">自动勾稽：往来单位只有一张未{{ conf.invoiceVerb }}金额与发票相等的单据时自动勾稽</a-checkbox>
        <a-alert v-if="importResult && !importResult.errors.length" type="success" show-icon :message="importResultText" />
        <a-table v-if="importResult?.errors.length" size="small" row-key="rowNum" :columns="errorColumns" :data-source="importResult.errors" :pagination="false" />
      </a-space>
    </a-modal>

    <ImportModal
      v-model:open="excelImportOpen"
      :title="`${conf.label}清单`"
      :columns="excelColumns"
      :importer="importExcel"
      :sample="{ invoiceNo: '25322000000123456789', invoiceDate: '2026-10-08', sellerTaxNo: '91320000MA1XXXXX0X', sellerName: '示例供应商有限公司', amount: 1130, taxAmount: 130 }"
      @success="load"
    />

    <!-- 进项认证 -->
    <a-modal v-model:open="certifyOpen" title="进项发票认证" :confirm-loading="certifying" ok-text="认证" @ok="certify(certifyPeriod)">
      <p>已选择 {{ certifiable.length }} 张有效的进项发票，认证（勾选抵扣）后记入所选期间的进项税额抵扣。</p>
      <a-space>
        <span>认证期间</span>
        <PeriodPicker v-model:value="certifyPeriod" />
        <a-popconfirm title="取消所选发票的认证？" @confirm="certify(null)">
          <a-button :loading="certifying">取消认证</a-button>
        </a-popconfirm>
      </a-space>
    </a-modal>
  </div>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
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

const KINDS = {
  OUTPUT: { label: '销项发票', partner: 'customer', partnerLabel: '客户', billLabel: '应收单', invoiceVerb: '开票' },
  INPUT: { label: '进项发票', partner: 'supplier', partnerLabel: '供应商', billLabel: '应付单', invoiceVerb: '收票' }
}
const STATUS_OPTIONS = [
  { value: 'VALID', label: '有效' },
  { value: 'VOID', label: '已作废' }
]
const dict = useDict('fin_invoice_type')
const userStore = useUserStore()
/** 可认证抵扣的专票（含数电票专票） */
const SPECIAL_TYPES = ['SPECIAL', 'E_SPECIAL']
const SOURCES = { MANUAL: '手工登记', XML: '数电票 XML 导入', EXCEL: '发票清单导入' }
const unmatchedAmount = (r) => round2(Number(r.amount) - Number(r.matchedAmount || 0))
const tab = ref('OUTPUT')
// 登记、列表使用的种类：报表页签时沿用上一个发票页签
const kind = ref('OUTPUT')
const conf = computed(() => KINDS[kind.value])
const round2 = (n) => Math.round(n * 100) / 100

const columns = computed(() => [
  { title: '发票号码', key: 'invoiceNo', width: 160 },
  { title: '类型', key: 'invoiceType', width: 130 },
  { title: conf.value.partnerLabel, dataIndex: 'partnerName', width: 200, ellipsis: true },
  { title: '开票日期', dataIndex: 'invoiceDate', width: 110 },
  { title: '价税合计', key: 'amount', width: 130, align: 'right' },
  { title: '税额', key: 'taxAmount', width: 110, align: 'right' },
  { title: '状态', key: 'status', width: 90 },
  { title: '勾稽', key: 'matched', width: 150 },
  ...(kind.value === 'INPUT' ? [{ title: '认证期间', key: 'certify', width: 130 }] : []),
  { title: '备注', dataIndex: 'remark', ellipsis: true },
  { title: '登记人', dataIndex: 'createByName', width: 100 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
])
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
  message.success('已作废')
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
    message.success(period ? `已认证到 ${periodText(period)}` : '已取消认证')
    certifyOpen.value = false
    selectedKeys.value = []
    load()
  } finally {
    certifying.value = false
  }
}

// ---------------------------------------------------------------- 登记
const billColumns = [
  { title: '单号', dataIndex: 'billNo', width: 150 },
  { title: '来源单号', dataIndex: 'sourceNo', width: 160, ellipsis: true },
  { title: '日期', dataIndex: 'billDate', width: 100 },
  { title: '金额', key: 'amount', width: 120, align: 'right' },
  { title: '已勾稽', key: 'invoiced', width: 110, align: 'right' },
  { title: '未勾稽', key: 'remaining', width: 110, align: 'right' },
  { title: '本次勾稽', key: 'apply', width: 150 }
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
    message.warning('请勾选勾稽的单据并填写金额')
    return
  }
  if (Math.abs(itemTotal.value - Number(form.value.amount)) >= 0.005) {
    message.warning(`勾稽合计 ¥${formatMoney(itemTotal.value)} 与价税合计不一致`)
    return
  }
  saving.value = true
  try {
    await addInvoice({ ...form.value, invoiceNo: form.value.invoiceNo.trim(), remark: form.value.remark || null, items })
    message.success('发票已登记')
    createOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 详情
const itemColumns = [
  { title: '单据', dataIndex: 'billNo', width: 160 },
  { title: '来源单号', dataIndex: 'sourceNo', width: 170 },
  { title: '单据金额', key: 'billAmount', width: 130, align: 'right' },
  { title: '勾稽金额', key: 'amount', width: 130, align: 'right' }
]
const detailOpen = ref(false)
const detail = ref()
async function openDetail(record) {
  detail.value = undefined
  detailOpen.value = true
  detail.value = await getInvoice(record.id)
}

const lineColumns = [
  { title: '项目名称', dataIndex: 'itemName', width: 200, ellipsis: true },
  { title: '规格型号', dataIndex: 'spec', width: 110, ellipsis: true },
  { title: '数量', key: 'qty', width: 100, align: 'right' },
  { title: '单价', key: 'price', width: 100, align: 'right' },
  { title: '金额', key: 'amount', width: 110, align: 'right' },
  { title: '税率', key: 'taxRate', width: 70, align: 'right' },
  { title: '税额', key: 'taxAmount', width: 100, align: 'right' }
]

async function unmatch(id) {
  await unmatchInvoice(id)
  message.success('已取消勾稽')
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
    message.warning('请勾选勾稽的单据并填写金额')
    return
  }
  matching.value = true
  try {
    await matchInvoice(matchInvoiceRow.value.id, items)
    message.success('已勾稽')
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
  { title: '序号', dataIndex: 'rowNum', width: 70 },
  { title: '错误', dataIndex: 'message' }
]
const importResultText = computed(() => {
  const r = importResult.value
  return r ? `导入 ${r.imported} 张${r.skipped ? `，${r.skipped} 张已登记过跳过` : ''}${r.matched ? `，自动勾稽 ${r.matched} 张` : ''}` : ''
})
const xmlTip = computed(() =>
  kind.value === 'INPUT'
    ? '从税务数字账户下载的数电票 XML，按销售方纳税人识别号（没有时按名称）匹配供应商；已登记的发票号码自动跳过。导入后可在列表中勾稽应付单。'
    : '在税务数字账户开具后下载的数电票 XML，按购买方纳税人识别号（没有时按名称）匹配客户；已登记的发票号码自动跳过。导入后可在列表中勾稽应收单。'
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
  { title: '发票号码', key: 'invoiceNo', required: true },
  { title: '发票类型', key: 'invoiceType', dict: 'fin_invoice_type' },
  { title: '开票日期', key: 'invoiceDate', type: 'date', required: true },
  { title: '销方识别号', key: 'sellerTaxNo' },
  { title: '销方名称', key: 'sellerName' },
  { title: '购方识别号', key: 'buyerTaxNo' },
  { title: '购方名称', key: 'buyerName' },
  { title: '价税合计', key: 'amount', type: 'number', required: true },
  { title: '税额', key: 'taxAmount', type: 'number', required: true }
]
async function importExcel(data) {
  const r = await importInvoiceRows(kind.value, data, true)
  if (!r.errors.length && (r.skipped || r.matched)) message.info(`${r.skipped ? `${r.skipped} 张已登记过跳过；` : ''}自动勾稽 ${r.matched} 张`)
  return { total: r.total, imported: r.imported, errors: r.errors }
}

// ---------------------------------------------------------------- 未开票 / 暂估
const reportKind = ref('OUTPUT')
const asOf = ref(today())
const report = ref([])
const reportLoading = ref(false)
const reportColumns = computed(() => [
  { title: reportKind.value === 'OUTPUT' ? '客户' : '供应商', dataIndex: 'partnerName', width: 240, ellipsis: true },
  { title: '单据数', dataIndex: 'billCount', width: 90, align: 'right' },
  { title: '单据金额', key: 'amount', width: 140, align: 'right' },
  { title: reportKind.value === 'OUTPUT' ? '已开票' : '已收票', key: 'invoiced', width: 140, align: 'right' },
  { title: reportKind.value === 'OUTPUT' ? '未开票' : '暂估金额', key: 'uninvoiced', width: 140, align: 'right' },
  { title: '其中税额', key: 'uninvoicedTax', width: 130, align: 'right' }
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
