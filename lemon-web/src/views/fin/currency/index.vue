<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab">
        <template #rightExtra>
          <a-button v-if="tab === 'currency'" v-perm="'fin:currency:edit'" type="primary" @click="openCurrency()">
            <template #icon><PlusOutlined /></template>{{ $t('fin.currencyPage.addCurrency') }}
          </a-button>
        </template>

        <!-- 币种 -->
        <a-tab-pane key="currency" :tab="$t('fin.currencyPage.currency')">
          <a-table row-key="code" size="middle" :columns="currencyColumns" :data-source="currencyList" :loading="currencyLoading" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'code'">
                <b>{{ record.code }}</b>
                <a-tag v-if="record.isBase === 1 || record.isBase === true" color="blue" :bordered="false" class="ml">{{ $t('fin.currencyPage.baseCurrency') }}</a-tag>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="record.status === 0 ? 'default' : 'green'" :bordered="false">{{ record.status === 0 ? $t('common.status.disabled') : $t('common.status.enabled') }}</a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button v-perm="'fin:currency:edit'" type="link" size="small" @click="openCurrency(record)">
                  <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
                </a-button>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 汇率 -->
        <a-tab-pane key="rate" :tab="$t('fin.currencyPage.rate')">
          <a-alert
            type="info"
            show-icon
            class="tip"
            :message="$t('fin.currencyPage.rateTip')"
          />
          <div class="toolbar">
            <a-space wrap>
              <span>{{ $t('fin.currencyPage.period') }}</span>
              <PeriodPicker v-model:value="ratePeriod" @update:value="loadPeriodRates" />
            </a-space>
            <a-button v-perm="'fin:currency:edit'" type="primary" :loading="rateSaving" @click="saveRates">
              <template #icon><SaveOutlined /></template>{{ $t('fin.currencyPage.saveRates') }}
            </a-button>
          </div>
          <a-table
            row-key="code"
            size="middle"
            :columns="periodColumns"
            :data-source="foreignCurrencies"
            :loading="rateLoading"
            :pagination="false"
            :locale="{ emptyText: $t('fin.currencyPage.noForeign') }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'currency'">{{ record.code }} {{ record.name }}</template>
              <template v-else-if="column.key === 'rate'">
                <a-input-number
                  v-model:value="rateInputs[record.code]"
                  :min="0"
                  :precision="6"
                  :disabled="!canEdit"
                  :placeholder="$t('fin.currencyPage.notMaintained')"
                  class="rate-input"
                />
              </template>
              <template v-else-if="column.key === 'hint'">
                <span v-if="rateInputs[record.code]" class="sub-text">1 {{ record.code }} = ¥{{ Number(rateInputs[record.code]) }}</span>
              </template>
            </template>
          </a-table>

          <div class="section-head">
            <span class="section-title">{{ $t('fin.currencyPage.history') }}</span>
            <a-select v-model:value="historyCurrency" :options="historyOptions" :placeholder="$t('fin.currencyPage.allForeign')" allow-clear class="filter-s" @change="loadHistory" />
          </div>
          <a-table
            row-key="id"
            size="small"
            :columns="historyColumns"
            :data-source="history"
            :loading="historyLoading"
            :pagination="{ pageSize: 12, showSizeChanger: false }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'period'">{{ periodText(record.period) }}</template>
              <template v-else-if="column.key === 'rate'">{{ Number(record.rate) }}</template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 期末调汇 -->
        <a-tab-pane key="revalue" :tab="$t('fin.currencyPage.revalue')">
          <a-alert
            type="info"
            show-icon
            class="tip"
            :message="$t('fin.currencyPage.revalueTip')"
          />
          <div class="toolbar">
            <a-space wrap>
              <span>{{ $t('fin.currencyPage.revaluePeriod') }}</span>
              <PeriodPicker v-model:value="revaluePeriod" @update:value="revalueResult = null" />
              <a-button v-perm="'fin:currency:edit'" type="primary" :loading="revaluing" @click="runRevalue">
                <template #icon><SwapOutlined /></template>{{ $t('fin.currencyPage.runRevalue') }}
              </a-button>
            </a-space>
          </div>
          <template v-if="revalueResult">
            <div class="result-head">
              <span>{{ $t('fin.currencyPage.resultHead', { period: periodText(revaluePeriod) }) }} <b :class="{ neg: revalueTotal < 0 }">¥{{ formatMoney(revalueTotal) }}</b></span>
              <a-space>
                <span v-if="revalueResult.voucherId">
                  {{ $t('fin.currencyPage.revalueVoucher') }} <a @click="openVoucher(revalueResult.voucherId)">#{{ revalueResult.voucherId }}</a>
                </span>
                <span v-if="revalueResult.reverseVoucherId">
                  {{ $t('fin.currencyPage.reverseVoucher') }} <a @click="openVoucher(revalueResult.reverseVoucherId)">#{{ revalueResult.reverseVoucherId }}</a>
                </span>
                <span v-if="!revalueResult.voucherId" class="sub-text">{{ $t('fin.currencyPage.noDiff') }}</span>
              </a-space>
            </div>
            <a-table
              row-key="key"
              size="middle"
              :columns="revalueColumns"
              :data-source="revalueLines"
              :pagination="false"
              :locale="{ emptyText: $t('fin.currencyPage.noOpenItems') }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'kind'">
                  <a-tag :color="record.kind === BillKind.AR ? 'blue' : 'orange'" :bordered="false">{{ record.kind === BillKind.AR ? $t('fin.currencyPage.ar') : $t('fin.currencyPage.ap') }}</a-tag>
                </template>
                <template v-else-if="column.key === 'rate'">{{ Number(record.rate) }}</template>
                <template v-else-if="column.key === 'remainingFc'">{{ record.currency }} {{ formatMoney(record.remainingFc) }}</template>
                <template v-else-if="['bookBase', 'newBase'].includes(column.key)">¥{{ formatMoney(record[column.key]) }}</template>
                <template v-else-if="column.key === 'diff'">
                  <span :class="{ neg: Number(record.diff) < 0 }">{{ formatMoney(record.diff) }}</span>
                </template>
              </template>
            </a-table>
          </template>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal v-model:open="currencyOpen" :title="currencyForm.isNew ? $t('fin.currencyPage.addCurrency') : $t('fin.currencyPage.editCurrency')" :confirm-loading="currencySaving" @ok="submitCurrency">
      <a-form ref="currencyFormRef" :model="currencyForm" :rules="currencyRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('fin.currencyPage.code')" name="code" :extra="$t('fin.currencyPage.codeExtra')">
              <a-input v-model:value="currencyForm.code" :maxlength="8" :disabled="!currencyForm.isNew" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('common.field.name')" name="name">
              <a-input v-model:value="currencyForm.name" :maxlength="32" :placeholder="$t('fin.currencyPage.namePh')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('fin.currencyPage.symbol')" name="symbol">
              <a-input v-model:value="currencyForm.symbol" :maxlength="8" :placeholder="$t('fin.currencyPage.symbolPh')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('common.field.sort')" name="sort">
              <a-input-number v-model:value="currencyForm.sort" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('common.field.status')" name="status">
              <a-radio-group v-model:value="currencyForm.status" :disabled="currencyForm.isBase">
                <a-radio :value="1">{{ $t('common.status.enabled') }}</a-radio>
                <a-radio :value="0">{{ $t('common.status.disabled') }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <VoucherDetail v-model:open="voucherOpen" :voucher-id="voucherId" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { EditOutlined, PlusOutlined, SaveOutlined, SwapOutlined } from '@ant-design/icons-vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import VoucherDetail from '@/components/fin/VoucherDetail.vue'
import { currencies, exchangeRates, revalue, saveCurrency, saveRate } from '@/api/fin'
import { useUserStore } from '@/store/user'
import { currentPeriod, formatMoney, periodText } from '@/components/fin/fin'
import { useCurrencies } from '@/components/fin/currency'
import { useI18n } from 'vue-i18n'

import { BillKind } from '@/constants/enums'
/** 币种与汇率：币种维护、按月汇率、期末调汇 */
const userStore = useUserStore()
const { t } = useI18n()
const canEdit = computed(() => userStore.hasPerm('fin:currency:edit'))
const tab = ref('currency')
const isBase = (c) => c.isBase === 1 || c.isBase === true

// ---------------------------------------------------------------- 币种
const currencyColumns = [
  { title: t('common.field.code'), key: 'code', width: 180 },
  { title: t('common.field.name'), dataIndex: 'name', width: 160 },
  { title: t('fin.currencyPage.symbol'), dataIndex: 'symbol', width: 100 },
  { title: t('common.field.sort'), dataIndex: 'sort', width: 80 },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('common.action.operation'), key: 'action', width: 110 }
]
const currencyList = ref([])
const currencyLoading = ref(false)
async function loadCurrencies() {
  currencyLoading.value = true
  try {
    currencyList.value = await currencies()
    // 刷新全局缓存，下单、收付款页面使用最新的币种
    useCurrencies(true)
  } finally {
    currencyLoading.value = false
  }
}

const currencyOpen = ref(false)
const currencySaving = ref(false)
const currencyFormRef = ref()
const currencyForm = ref({})
const currencyRules = {
  code: [
    { required: true, message: t('fin.currencyPage.codeRequired') },
    { pattern: /^[A-Za-z]{3,8}$/, message: t('fin.currencyPage.codeLetters') }
  ],
  name: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('common.field.name') }) }]
}
function openCurrency(record) {
  currencyForm.value = record
    ? { ...record, isNew: false, isBase: isBase(record), status: record.status ?? 1 }
    : { code: '', name: '', symbol: '', status: 1, sort: currencyList.value.length + 1, isNew: true, isBase: false }
  currencyOpen.value = true
}
async function submitCurrency() {
  await currencyFormRef.value.validate()
  const { code, name, symbol, status, sort } = currencyForm.value
  currencySaving.value = true
  try {
    await saveCurrency({ code: code.toUpperCase(), name, symbol: symbol || null, status, sort })
    message.success(t('common.msg.saveSuccess'))
    currencyOpen.value = false
    loadCurrencies()
  } finally {
    currencySaving.value = false
  }
}

// ---------------------------------------------------------------- 汇率
const foreignCurrencies = computed(() => currencyList.value.filter((c) => !isBase(c) && c.status !== 0))
const periodColumns = [
  { title: t('fin.currencyPage.currency'), key: 'currency', width: 200 },
  { title: t('fin.currencyPage.rateColumn'), key: 'rate', width: 260 },
  { title: '', key: 'hint' }
]
const ratePeriod = ref(currentPeriod())
const rateInputs = reactive({})
const rateLoaded = ref({})
const rateLoading = ref(false)
const rateSaving = ref(false)

async function loadPeriodRates() {
  rateLoading.value = true
  try {
    const list = await exchangeRates({ period: ratePeriod.value })
    const map = Object.fromEntries(list.map((r) => [r.currency, Number(r.rate)]))
    Object.keys(rateInputs).forEach((k) => delete rateInputs[k])
    Object.assign(rateInputs, map)
    rateLoaded.value = map
  } finally {
    rateLoading.value = false
  }
}

async function saveRates() {
  const changed = foreignCurrencies.value
    .map((c) => c.code)
    .filter((code) => rateInputs[code] != null && rateInputs[code] !== '' && Number(rateInputs[code]) !== rateLoaded.value[code])
  if (changed.some((code) => !(Number(rateInputs[code]) > 0))) {
    message.warning(t('fin.currencyPage.ratePositive'))
    return
  }
  if (!changed.length) {
    message.info(t('fin.currencyPage.rateUnchanged'))
    return
  }
  rateSaving.value = true
  try {
    for (const code of changed) {
      await saveRate({ currency: code, period: ratePeriod.value, rate: rateInputs[code] })
    }
    message.success(t('fin.currencyPage.ratesSaved', { n: changed.length }))
    loadPeriodRates()
    loadHistory()
  } finally {
    rateSaving.value = false
  }
}

const historyColumns = [
  { title: t('fin.currencyPage.period'), key: 'period', width: 140 },
  { title: t('fin.currencyPage.currency'), dataIndex: 'currency', width: 100 },
  { title: t('fin.currencyPage.rate'), key: 'rate', width: 140, align: 'right' }
]
const historyCurrency = ref()
const historyOptions = computed(() => currencyList.value.filter((c) => !isBase(c)).map((c) => ({ value: c.code, label: `${c.code} ${c.name || ''}` })))
const history = ref([])
const historyLoading = ref(false)
async function loadHistory() {
  historyLoading.value = true
  try {
    const list = await exchangeRates({ currency: historyCurrency.value || undefined })
    history.value = [...list].sort((a, b) => b.period - a.period || String(a.currency).localeCompare(b.currency))
  } finally {
    historyLoading.value = false
  }
}

watch(tab, (t) => {
  if (t === 'rate' && !history.value.length) {
    loadPeriodRates()
    loadHistory()
  }
})

// ---------------------------------------------------------------- 期末调汇
const revaluePeriod = ref(currentPeriod())
const revaluing = ref(false)
const revalueResult = ref(null)
const revalueColumns = [
  { title: t('fin.currencyPage.currency'), dataIndex: 'currency', width: 100 },
  { title: t('common.field.type'), key: 'kind', width: 110 },
  { title: t('fin.currencyPage.closingRate'), key: 'rate', width: 120, align: 'right' },
  { title: t('fin.currencyPage.remainingFc'), key: 'remainingFc', width: 170, align: 'right' },
  { title: t('fin.currencyPage.bookBase'), key: 'bookBase', width: 160, align: 'right' },
  { title: t('fin.currencyPage.newBase'), key: 'newBase', width: 170, align: 'right' },
  { title: t('fin.currencyPage.fxGainLoss'), key: 'diff', width: 150, align: 'right' }
]
const revalueLines = computed(() => (revalueResult.value?.lines || []).map((l, i) => ({ ...l, key: i })))
const revalueTotal = computed(() => Math.round(revalueLines.value.reduce((s, l) => s + Number(l.diff || 0), 0) * 100) / 100)

function runRevalue() {
  Modal.confirm({
    title: t('fin.currencyPage.confirmRevalue', { period: periodText(revaluePeriod.value) }),
    content: t('fin.currencyPage.confirmRevalueContent'),
    async onOk() {
      revaluing.value = true
      try {
        revalueResult.value = await revalue(revaluePeriod.value)
        message.success(revalueResult.value?.voucherId ? t('fin.currencyPage.revalueDoneVoucher') : t('fin.currencyPage.revalueDone'))
      } finally {
        revaluing.value = false
      }
    }
  })
}

const voucherOpen = ref(false)
const voucherId = ref()
function openVoucher(id) {
  voucherId.value = id
  voucherOpen.value = true
}

onMounted(loadCurrencies)
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.toolbar,
.result-head,
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.section-head {
  margin-top: 24px;
}
.section-title {
  font-weight: 500;
}
.rate-input {
  width: 180px;
}
.filter-s {
  width: 160px;
}
.full {
  width: 100%;
}
.ml {
  margin-left: 6px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.neg {
  color: #cf1322;
}
</style>
