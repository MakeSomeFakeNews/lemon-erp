<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab">
        <template #rightExtra>
          <a-button v-if="tab === 'currency'" v-perm="'fin:currency:edit'" type="primary" @click="openCurrency()">
            <template #icon><PlusOutlined /></template>新增币种
          </a-button>
        </template>

        <!-- 币种 -->
        <a-tab-pane key="currency" tab="币种">
          <a-table row-key="code" size="middle" :columns="currencyColumns" :data-source="currencyList" :loading="currencyLoading" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'code'">
                <b>{{ record.code }}</b>
                <a-tag v-if="record.isBase === 1 || record.isBase === true" color="blue" :bordered="false" class="ml">本位币</a-tag>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="record.status === 0 ? 'default' : 'green'" :bordered="false">{{ record.status === 0 ? '停用' : '启用' }}</a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button v-perm="'fin:currency:edit'" type="link" size="small" @click="openCurrency(record)">
                  <template #icon><EditOutlined /></template>编辑
                </a-button>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 汇率 -->
        <a-tab-pane key="rate" tab="汇率">
          <a-alert
            type="info"
            show-icon
            class="tip"
            message="汇率为 1 外币折合多少本位币（人民币），按月维护；下单、收付款时按业务日期所在月份的汇率带出，可在单据上修改。"
          />
          <div class="toolbar">
            <a-space wrap>
              <span>期间</span>
              <PeriodPicker v-model:value="ratePeriod" @update:value="loadPeriodRates" />
            </a-space>
            <a-button v-perm="'fin:currency:edit'" type="primary" :loading="rateSaving" @click="saveRates">
              <template #icon><SaveOutlined /></template>保存本月汇率
            </a-button>
          </div>
          <a-table
            row-key="code"
            size="middle"
            :columns="periodColumns"
            :data-source="foreignCurrencies"
            :loading="rateLoading"
            :pagination="false"
            :locale="{ emptyText: '没有启用的外币，请先在「币种」中添加' }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'currency'">{{ record.code }} {{ record.name }}</template>
              <template v-else-if="column.key === 'rate'">
                <a-input-number
                  v-model:value="rateInputs[record.code]"
                  :min="0"
                  :precision="6"
                  :disabled="!canEdit"
                  placeholder="未维护"
                  class="rate-input"
                />
              </template>
              <template v-else-if="column.key === 'hint'">
                <span v-if="rateInputs[record.code]" class="sub-text">1 {{ record.code }} = ¥{{ Number(rateInputs[record.code]) }}</span>
              </template>
            </template>
          </a-table>

          <div class="section-head">
            <span class="section-title">历史汇率</span>
            <a-select v-model:value="historyCurrency" :options="historyOptions" placeholder="全部外币" allow-clear class="filter-s" @change="loadHistory" />
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
        <a-tab-pane key="revalue" tab="期末调汇">
          <a-alert
            type="info"
            show-icon
            class="tip"
            message="外币应收应付未核销的原币按期末汇率重估，差额计入财务费用（汇兑损益），下月 1 日自动冲回。执行前请先维护该月的汇率。"
          />
          <div class="toolbar">
            <a-space wrap>
              <span>调汇期间</span>
              <PeriodPicker v-model:value="revaluePeriod" @update:value="revalueResult = null" />
              <a-button v-perm="'fin:currency:edit'" type="primary" :loading="revaluing" @click="runRevalue">
                <template #icon><SwapOutlined /></template>执行期末调汇
              </a-button>
            </a-space>
          </div>
          <template v-if="revalueResult">
            <div class="result-head">
              <span>{{ periodText(revaluePeriod) }} 调汇结果：汇兑损益合计 <b :class="{ neg: revalueTotal < 0 }">¥{{ formatMoney(revalueTotal) }}</b></span>
              <a-space>
                <span v-if="revalueResult.voucherId">
                  调汇凭证 <a @click="openVoucher(revalueResult.voucherId)">#{{ revalueResult.voucherId }}</a>
                </span>
                <span v-if="revalueResult.reverseVoucherId">
                  冲回凭证 <a @click="openVoucher(revalueResult.reverseVoucherId)">#{{ revalueResult.reverseVoucherId }}</a>
                </span>
                <span v-if="!revalueResult.voucherId" class="sub-text">没有差额，未生成凭证</span>
              </a-space>
            </div>
            <a-table
              row-key="key"
              size="middle"
              :columns="revalueColumns"
              :data-source="revalueLines"
              :pagination="false"
              :locale="{ emptyText: '没有未核销的外币应收应付' }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'kind'">
                  <a-tag :color="record.kind === 'AR' ? 'blue' : 'orange'" :bordered="false">{{ record.kind === 'AR' ? '应收' : '应付' }}</a-tag>
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

    <a-modal v-model:open="currencyOpen" :title="currencyForm.isNew ? '新增币种' : '编辑币种'" :confirm-loading="currencySaving" @ok="submitCurrency">
      <a-form ref="currencyFormRef" :model="currencyForm" :rules="currencyRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="币种编码" name="code" extra="ISO 4217 三位字母，如 USD">
              <a-input v-model:value="currencyForm.code" :maxlength="8" :disabled="!currencyForm.isNew" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="名称" name="name">
              <a-input v-model:value="currencyForm.name" :maxlength="32" placeholder="如：美元" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="符号" name="symbol">
              <a-input v-model:value="currencyForm.symbol" :maxlength="8" placeholder="如：$" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="排序" name="sort">
              <a-input-number v-model:value="currencyForm.sort" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态" name="status">
              <a-radio-group v-model:value="currencyForm.status" :disabled="currencyForm.isBase">
                <a-radio :value="1">启用</a-radio>
                <a-radio :value="0">停用</a-radio>
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

/** 币种与汇率：币种维护、按月汇率、期末调汇 */
const userStore = useUserStore()
const canEdit = computed(() => userStore.hasPerm('fin:currency:edit'))
const tab = ref('currency')
const isBase = (c) => c.isBase === 1 || c.isBase === true

// ---------------------------------------------------------------- 币种
const currencyColumns = [
  { title: '编码', key: 'code', width: 160 },
  { title: '名称', dataIndex: 'name', width: 160 },
  { title: '符号', dataIndex: 'symbol', width: 100 },
  { title: '排序', dataIndex: 'sort', width: 80 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 100 }
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
    { required: true, message: '请输入币种编码' },
    { pattern: /^[A-Za-z]{3,8}$/, message: '币种编码为字母' }
  ],
  name: [{ required: true, whitespace: true, message: '请输入名称' }]
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
    message.success('保存成功')
    currencyOpen.value = false
    loadCurrencies()
  } finally {
    currencySaving.value = false
  }
}

// ---------------------------------------------------------------- 汇率
const foreignCurrencies = computed(() => currencyList.value.filter((c) => !isBase(c) && c.status !== 0))
const periodColumns = [
  { title: '币种', key: 'currency', width: 200 },
  { title: '汇率（1 外币 = ? 人民币）', key: 'rate', width: 220 },
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
    message.warning('汇率必须大于 0')
    return
  }
  if (!changed.length) {
    message.info('汇率没有修改')
    return
  }
  rateSaving.value = true
  try {
    for (const code of changed) {
      await saveRate({ currency: code, period: ratePeriod.value, rate: rateInputs[code] })
    }
    message.success(`已保存 ${changed.length} 个币种的汇率`)
    loadPeriodRates()
    loadHistory()
  } finally {
    rateSaving.value = false
  }
}

const historyColumns = [
  { title: '期间', key: 'period', width: 140 },
  { title: '币种', dataIndex: 'currency', width: 100 },
  { title: '汇率', key: 'rate', width: 140, align: 'right' }
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
  { title: '币种', dataIndex: 'currency', width: 90 },
  { title: '类型', key: 'kind', width: 80 },
  { title: '期末汇率', key: 'rate', width: 110, align: 'right' },
  { title: '未核销原币', key: 'remainingFc', width: 150, align: 'right' },
  { title: '账面本位币', key: 'bookBase', width: 140, align: 'right' },
  { title: '重估本位币', key: 'newBase', width: 140, align: 'right' },
  { title: '汇兑损益', key: 'diff', width: 130, align: 'right' }
]
const revalueLines = computed(() => (revalueResult.value?.lines || []).map((l, i) => ({ ...l, key: i })))
const revalueTotal = computed(() => Math.round(revalueLines.value.reduce((s, l) => s + Number(l.diff || 0), 0) * 100) / 100)

function runRevalue() {
  Modal.confirm({
    title: `执行 ${periodText(revaluePeriod.value)} 期末调汇？`,
    content: '按期末汇率重估外币应收应付余额，生成调汇凭证，并于下月 1 日生成冲回凭证。',
    async onOk() {
      revaluing.value = true
      try {
        revalueResult.value = await revalue(revaluePeriod.value)
        message.success(revalueResult.value?.voucherId ? '调汇完成，已生成凭证' : '调汇完成')
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
