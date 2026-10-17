<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="sheet" :tab="$t('fin.commission.sheetTab')">
          <div class="toolbar">
            <a-space wrap>
              <span>{{ $t('fin.commission.period') }}</span>
              <PeriodPicker v-model:value="period" @update:value="loadSheet" />
              <template v-if="sheet">
                <a-tag :color="sheet.status === 'CONFIRMED' ? 'green' : 'default'" :bordered="false">
                  {{ sheet.status === 'CONFIRMED' ? $t('fin.commission.confirmed') : $t('fin.commission.draft') }}
                </a-tag>
                <span>{{ $t('fin.commission.totalLabel') }} <b>¥{{ formatMoney(sheet.totalAmount) }}</b></span>
              </template>
            </a-space>
            <a-space v-perm="'fin:commission:edit'">
              <a-button v-if="!sheet || sheet.status === 'DRAFT'" :loading="generating" @click="generate">
                <template #icon><CalculatorOutlined /></template>{{ sheet ? $t('fin.commission.recalc') : $t('fin.commission.calc') }}
              </a-button>
              <a-popconfirm v-if="sheet?.status === 'DRAFT'" :title="$t('fin.commission.confirmSheet')" @confirm="confirm">
                <a-button type="primary">{{ $t('common.action.confirm') }}</a-button>
              </a-popconfirm>
              <a-popconfirm v-if="sheet?.status === 'CONFIRMED'" :title="$t('fin.commission.confirmUnconfirm')" @confirm="unconfirm">
                <a-button>{{ $t('fin.commission.unconfirm') }}</a-button>
              </a-popconfirm>
            </a-space>
          </div>
          <a-alert
            type="info"
            show-icon
            class="tip"
            :message="$t('fin.commission.sheetTip')"
          />
          <a-table
            row-key="id"
            size="middle"
            :columns="sheetColumns"
            :data-source="sheet?.lines || []"
            :loading="loading"
            :pagination="false"
            :scroll="{ x: 1100, y: '100%' }"
            :locale="{ emptyText: sheet ? $t('fin.commission.noSales') : $t('fin.commission.notCalculated') }"
          >
            <template #expandedRowRender="{ record }">
              <a-table row-key="key" size="small" :columns="detailColumns" :data-source="detailRows(record)" :pagination="false">
                <template #bodyCell="{ column, record: d }">
                  <template v-if="column.key === 'basis'"><DictTag type="fin_commission_basis" :value="d.basis" /></template>
                  <template v-else-if="column.key === 'category'">
                    <DictTag v-if="d.category" type="wms_material_category" :value="d.category" /><span v-else class="sub-text">{{ $t('common.status.all') }}</span>
                  </template>
                  <template v-else-if="column.key === 'rate'">{{ Number(d.rate) }}%</template>
                  <template v-else-if="['base', 'amount'].includes(column.key)">{{ formatMoney(d[column.key]) }}</template>
                </template>
              </a-table>
            </template>
            <template #bodyCell="{ column, record }">
              <template v-if="['salesAmount', 'profitAmount', 'receiptAmount', 'commissionAmount', 'adjustAmount'].includes(column.key)">
                <span :class="{ neg: Number(record[column.key]) < 0 }">{{ formatMoney(record[column.key]) }}</span>
              </template>
              <template v-else-if="column.key === 'finalAmount'"><b>{{ formatMoney(record.finalAmount) }}</b></template>
              <template v-else-if="column.key === 'action'">
                <a-button v-if="sheet.status === 'DRAFT'" v-perm="'fin:commission:edit'" type="link" size="small" @click="openAdjust(record)">{{ $t('fin.commission.adjust') }}</a-button>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="rule" :tab="$t('fin.commission.ruleTab')">
          <div class="toolbar">
            <span class="sub-text">{{ $t('fin.commission.ruleHint') }}</span>
            <a-button v-perm="'fin:commission:rule'" type="primary" @click="drawer.openCreate()">
              <template #icon><PlusOutlined /></template>{{ $t('fin.commission.addRule') }}
            </a-button>
          </div>
          <a-table row-key="id" size="middle" :columns="ruleColumns" :data-source="rules" :loading="rulesLoading" :pagination="false" :scroll="{ x: 1000 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'basis'"><DictTag type="fin_commission_basis" :value="record.basis" /></template>
              <template v-else-if="column.key === 'salesman'">{{ record.salesmanName || $t('fin.commission.allSalesmen') }}</template>
              <template v-else-if="column.key === 'category'">
                <DictTag v-if="record.materialCategory" type="wms_material_category" :value="record.materialCategory" /><span v-else class="sub-text">{{ $t('common.status.all') }}</span>
              </template>
              <template v-else-if="column.key === 'threshold'">{{ Number(record.threshold) ? `≥ ${formatMoney(record.threshold)}` : '-' }}</template>
              <template v-else-if="column.key === 'rate'"><b>{{ Number(record.rate) }}%</b></template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="record.status === 0 ? 'default' : 'green'" :bordered="false">{{ record.status === 0 ? $t('common.status.disabled') : $t('common.status.enabled') }}</a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <span v-perm="'fin:commission:rule'" class="table-actions">
                  <a-button type="link" size="small" @click="drawer.openEdit(record)">{{ $t('common.action.edit') }}</a-button>
                  <a-popconfirm :title="$t('fin.commission.confirmDeleteRule', { name: record.name })" @confirm="removeRule(record.id)">
                    <a-button type="link" danger size="small">{{ $t('common.action.delete') }}</a-button>
                  </a-popconfirm>
                </span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal v-model:open="adjustOpen" :title="adjustRow ? $t('fin.commission.adjustTitle', { name: adjustRow.salesmanName }) : ''" :confirm-loading="adjusting" @ok="submitAdjust">
      <a-form layout="vertical">
        <a-form-item :label="$t('fin.commission.adjustAmount')" :extra="$t('fin.commission.adjustExtra')">
          <a-input-number v-model:value="adjustForm.adjustAmount" :precision="2" prefix="¥" class="full" />
        </a-form-item>
        <a-form-item :label="$t('fin.commission.adjustReason')">
          <a-input v-model:value="adjustForm.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>

    <FormDrawer :drawer="drawer" :create-title="$t('fin.commission.createRule')" :edit-title="$t('fin.commission.editRule')">
      <a-form ref="formRef" :model="form" :rules="formRules" layout="vertical">
        <a-form-item :label="$t('fin.commission.ruleName')" name="name">
          <a-input v-model:value="form.name" :maxlength="64" :placeholder="$t('fin.commission.ruleNamePlaceholder')" />
        </a-form-item>
        <a-form-item :label="$t('fin.commission.basis')" name="basis">
          <a-radio-group v-model:value="form.basis" button-style="solid" @change="form.basis === 'RECEIPT' && (form.materialCategory = undefined)">
            <a-radio-button v-for="d in dict.fin_commission_basis" :key="d.value" :value="d.value">{{ d.label }}</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('fin.commission.salesman')" name="salesmanId" :extra="$t('fin.commission.emptyAllSalesmen')">
          <IdentityUserSelect v-model:value="form.salesmanId" :label="form.salesmanName" :placeholder="$t('fin.commission.allSalesmen')" />
        </a-form-item>
        <a-form-item :label="$t('fin.commission.category')" name="materialCategory" :extra="form.basis === 'RECEIPT' ? $t('fin.commission.receiptNoCategory') : $t('fin.commission.emptyAllCategories')">
          <a-select v-model:value="form.materialCategory" :options="toOptions(dict.wms_material_category)" allow-clear :placeholder="$t('fin.commission.allCategories')" :disabled="form.basis === 'RECEIPT'" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('fin.commission.threshold')" name="threshold" :extra="$t('fin.commission.thresholdExtra')">
              <a-input-number v-model:value="form.threshold" :min="0" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('fin.commission.rateLabel')" name="rate">
              <a-input-number v-model:value="form.rate" :min="0" :max="100" :precision="3" addon-after="%" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-switch :checked="form.status !== 0" :checked-children="$t('common.status.enabled')" :un-checked-children="$t('common.status.disabled')" @change="(v) => (form.status = v ? 1 : 0)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.remark')" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { CalculatorOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import {
  addCommissionRule,
  adjustCommission,
  commissionRules,
  commissionSheet,
  confirmCommission,
  deleteCommissionRule,
  generateCommission,
  unconfirmCommission,
  updateCommissionRule
} from '@/api/fin'
import { currentPeriod, formatMoney } from '@/components/fin/fin'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'
import { useI18n } from 'vue-i18n'

/** 销售提成：按业务员统计当月业绩，按规则计算提成，确认后计入工资表 */
const { t } = useI18n()
const dict = useDict('fin_commission_basis', 'wms_material_category')
const tab = ref('sheet')
const period = ref(currentPeriod())

// ---------------------------------------------------------------- 提成表
const sheetColumns = [
  { title: t('fin.commission.salesman'), dataIndex: 'salesmanName', width: 140 },
  { title: t('fin.commission.sales'), key: 'salesAmount', width: 130, align: 'right' },
  { title: t('fin.commission.profit'), key: 'profitAmount', width: 120, align: 'right' },
  { title: t('fin.commission.receipt'), key: 'receiptAmount', width: 130, align: 'right' },
  { title: t('fin.commission.ruleCommission'), key: 'commissionAmount', width: 120, align: 'right' },
  { title: t('fin.commission.adjust'), key: 'adjustAmount', width: 110, align: 'right' },
  { title: t('fin.commission.finalCommission'), key: 'finalAmount', width: 130, align: 'right' },
  { title: t('fin.commission.adjustReason'), dataIndex: 'remark', ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 90, fixed: 'right' }
]
const detailColumns = [
  { title: t('fin.commission.basisShort'), key: 'basis', width: 110 },
  { title: t('fin.commission.category'), key: 'category', width: 150 },
  { title: t('fin.commission.performance'), key: 'base', width: 130, align: 'right' },
  { title: t('fin.commission.rule'), dataIndex: 'ruleName' },
  { title: t('fin.commission.rate'), key: 'rate', width: 90, align: 'right' },
  { title: t('fin.commission.commission'), key: 'amount', width: 120, align: 'right' }
]
const detailRows = (line) => {
  try {
    return JSON.parse(line.detail || '[]').map((d, i) => ({ ...d, key: i }))
  } catch {
    return []
  }
}
const sheet = ref(null)
const loading = ref(false)
const generating = ref(false)
async function loadSheet() {
  loading.value = true
  try {
    sheet.value = await commissionSheet(period.value)
  } finally {
    loading.value = false
  }
}
async function generate() {
  generating.value = true
  try {
    await generateCommission(period.value)
    message.success(t('fin.commission.calculated'))
    loadSheet()
  } finally {
    generating.value = false
  }
}
async function confirm() {
  await confirmCommission(period.value)
  message.success(t('fin.commission.confirmed'))
  loadSheet()
}
async function unconfirm() {
  await unconfirmCommission(period.value)
  message.success(t('fin.commission.unconfirmed'))
  loadSheet()
}

const adjustOpen = ref(false)
const adjusting = ref(false)
const adjustRow = ref()
const adjustForm = reactive({ adjustAmount: 0, remark: '' })
function openAdjust(row) {
  adjustRow.value = row
  Object.assign(adjustForm, { adjustAmount: Number(row.adjustAmount) || 0, remark: row.remark || '' })
  adjustOpen.value = true
}
async function submitAdjust() {
  adjusting.value = true
  try {
    await adjustCommission(adjustRow.value.id, { adjustAmount: adjustForm.adjustAmount || 0, remark: adjustForm.remark || null })
    message.success(t('fin.commission.adjusted'))
    adjustOpen.value = false
    loadSheet()
  } finally {
    adjusting.value = false
  }
}

// ---------------------------------------------------------------- 规则
const ruleColumns = [
  { title: t('fin.commission.ruleName'), dataIndex: 'name', width: 180 },
  { title: t('fin.commission.basisShort'), key: 'basis', width: 110 },
  { title: t('fin.commission.salesman'), key: 'salesman', width: 130 },
  { title: t('fin.commission.category'), key: 'category', width: 150 },
  { title: t('fin.commission.threshold'), key: 'threshold', width: 130, align: 'right' },
  { title: t('fin.commission.rate'), key: 'rate', width: 90, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 130, fixed: 'right' }
]
const rules = ref([])
const rulesLoading = ref(false)
async function loadRules() {
  rulesLoading.value = true
  try {
    rules.value = await commissionRules()
  } finally {
    rulesLoading.value = false
  }
}
const formRules = {
  name: [{ required: true, whitespace: true, message: t('fin.commission.ruleNameRequired') }],
  basis: [{ required: true, message: t('fin.commission.basisRequired') }],
  rate: [{ required: true, message: t('fin.commission.rateRequired') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, name: '', basis: 'SALES', salesmanId: null, materialCategory: undefined, threshold: 0, rate: undefined, status: 1, remark: '' }),
  fetch: (r) => ({ ...r, threshold: Number(r.threshold), rate: Number(r.rate), materialCategory: r.materialCategory || undefined }),
  create: addCommissionRule,
  update: updateCommissionRule,
  toPayload: (f) => ({
    name: f.name,
    basis: f.basis,
    salesmanId: f.salesmanId || null,
    materialCategory: f.materialCategory || null,
    threshold: f.threshold || 0,
    rate: f.rate,
    status: f.status,
    remark: f.remark || null
  }),
  onSuccess: loadRules
})
const { formRef, form } = drawer
async function removeRule(id) {
  await deleteCommissionRule(id)
  message.success(t('common.msg.deleteSuccess'))
  loadRules()
}

watch(tab, (v) => v === 'rule' && loadRules())
onMounted(loadSheet)
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 12px;
  margin-bottom: 12px;
}
.tip {
  margin-bottom: 12px;
}
.neg {
  color: var(--status-critical);
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
