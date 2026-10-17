<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-alert
        type="info"
        show-icon
        class="tip"
        :message="$t('fin.period.tip')"
      />
      <div class="toolbar">
        <a-space wrap>
          <span>{{ $t('fin.period.closePeriod') }}</span>
          <PeriodPicker v-model:value="target" @update:value="checkItems = null" />
          <a-button :loading="checking" @click="runCheck">
            <template #icon><SafetyCertificateOutlined /></template>{{ $t('fin.period.closeCheck') }}
          </a-button>
          <a-button v-perm="'fin:period:tax'" @click="openVat">
            <template #icon><CalculatorOutlined /></template>{{ $t('fin.period.vatTransfer') }}
          </a-button>
          <a-button v-perm="'fin:period:close'" type="primary" :loading="saving" @click="close(target)">{{ $t('fin.period.close') }}</a-button>
        </a-space>
      </div>
      <div v-if="checkItems" class="check-panel">
        <div class="check-head">
          <span>
            {{ $t('fin.period.checkHead', { period: periodText(checkedPeriod) }) }}
            <span v-if="counts.ERROR" class="error">{{ $t('fin.period.errorN', { n: counts.ERROR }) }}</span>
            <span v-if="counts.WARN" class="warn">{{ counts.ERROR ? $t('fin.period.sep') : '' }}{{ $t('fin.period.warnN', { n: counts.WARN }) }}</span>
            <span v-if="!counts.ERROR && !counts.WARN" class="ok">{{ $t('fin.period.allPassed') }}</span>
          </span>
          <a @click="checkItems = null">{{ $t('common.action.collapse') }}</a>
        </div>
        <CloseCheckList :items="checkItems" />
      </div>
      <a-table row-key="period" size="middle" :columns="columns" :data-source="list" :loading="loading" :pagination="pagination" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'period'">{{ periodText(record.period) }}</template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === PeriodStatus.CLOSED ? 'default' : 'green'" :bordered="false">{{ record.status === PeriodStatus.CLOSED ? $t('fin.period.closed') : $t('fin.period.open') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'vouchers'">
            {{ $t('fin.period.voucherCount', { n: record.voucherCount }) }}
            <span v-if="record.unpostedCount" class="warn">{{ $t('fin.period.unposted', { n: record.unpostedCount }) }}</span>
          </template>
          <template v-else-if="column.key === 'close'">
            <template v-if="record.closeTime">{{ record.closeByName }} {{ record.closeTime }}</template>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span v-perm="'fin:period:close'">
              <a-button v-if="record.status === PeriodStatus.OPEN" type="link" size="small" @click="close(record.period)">{{ $t('fin.period.close') }}</a-button>
              <a-popconfirm v-else :title="$t('fin.period.confirmReopen')" @confirm="reopen(record.period)">
                <a-button type="link" size="small">{{ $t('fin.period.reopen') }}</a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 结账：先显示检查结果，有错误时不能结账，有提示时须确认 -->
    <a-modal
      v-model:open="closeOpen"
      :title="$t('fin.period.closeTitle', { period: periodText(closeTarget) })"
      width="min(640px, 100vw)"
      :ok-text="$t('fin.period.close')"
      :confirm-loading="saving"
      :ok-button-props="{ disabled: !!closeCounts.ERROR || (!!closeCounts.WARN && !warnConfirmed) }"
      @ok="doClose"
    >
      <a-spin :spinning="checking">
        <CloseCheckList :items="closeItems" />
        <a-alert v-if="closeCounts.ERROR" type="error" show-icon class="mt" :message="$t('fin.period.hasErrors')" />
        <template v-else>
          <a-checkbox v-if="closeCounts.WARN" v-model:checked="warnConfirmed" class="mt">{{ $t('fin.period.warnConfirm', { n: closeCounts.WARN }) }}</a-checkbox>
          <div class="mt sub-text">{{ $t('fin.period.closeNote') }}</div>
        </template>
      </a-spin>
    </a-modal>

    <!-- 增值税结转 -->
    <a-modal v-model:open="vatOpen" :title="$t('fin.period.vatTitle', { period: periodText(target) })" width="min(560px, 100vw)" :footer="vatResult ? null : undefined"
      :ok-text="$t('fin.period.transfer')" :confirm-loading="vatSaving" @ok="runVat">
      <template v-if="!vatResult">
        <a-alert
          type="info"
          show-icon
          class="tip"
          :message="$t('fin.period.vatTip')"
        />
        <a-form layout="vertical">
          <a-row :gutter="12">
            <a-col :span="8">
              <a-form-item :label="$t('fin.period.cityRate')">
                <a-input-number v-model:value="vatForm.cityRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item :label="$t('fin.period.eduRate')">
                <a-input-number v-model:value="vatForm.eduRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item :label="$t('fin.period.localRate')">
                <a-input-number v-model:value="vatForm.localRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </template>
      <template v-else>
        <a-result status="success" :title="$t('fin.period.vatDone')" :sub-title="vatResult.voucherId ? $t('fin.period.vatVoucherGenerated') : $t('fin.period.vatNothing')">
          <template #extra>
            <a-descriptions :column="1" size="small" bordered class="vat-result">
              <a-descriptions-item :label="$t('fin.period.outputTax')">¥{{ formatMoney(vatResult.output) }}</a-descriptions-item>
              <a-descriptions-item :label="$t('fin.period.inputTax')">¥{{ formatMoney(vatResult.input) }}</a-descriptions-item>
              <a-descriptions-item :label="$t('fin.period.vatPayable')"><b>¥{{ formatMoney(vatResult.payable) }}</b></a-descriptions-item>
              <a-descriptions-item :label="$t('fin.period.surcharge')">¥{{ formatMoney(vatResult.surcharge) }}</a-descriptions-item>
            </a-descriptions>
            <a-space class="mt">
              <a-button v-if="vatResult.voucherId" @click="openVoucher(vatResult.voucherId)">{{ $t('fin.period.viewVoucher') }}</a-button>
              <a-button type="primary" @click="vatOpen = false">{{ $t('fin.period.done') }}</a-button>
            </a-space>
          </template>
        </a-result>
      </template>
    </a-modal>

    <VoucherDetail v-model:open="voucherOpen" :voucher-id="voucherId" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { CalculatorOutlined, SafetyCertificateOutlined } from '@ant-design/icons-vue'
import CloseCheckList from '@/components/fin/CloseCheckList.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import VoucherDetail from '@/components/fin/VoucherDetail.vue'
import { closeCheck, closePeriod, pagePeriod, reopenPeriod, transferVat } from '@/api/fin'
import { useTable } from '@/composables/useTable'
import { currentPeriod, formatMoney, periodText } from '@/components/fin/fin'
import { useI18n } from 'vue-i18n'
import { useAction } from '@/composables/useAction'

import { PeriodStatus } from '@/constants/enums'
const { t } = useI18n()

const columns = [
  { title: t('fin.period.period'), key: 'period', width: 160 },
  { title: t('common.field.status'), key: 'status', width: 110 },
  { title: t('fin.period.vouchers'), key: 'vouchers', width: 200 },
  { title: t('fin.period.close'), key: 'close', width: 260 },
  { title: t('common.action.operation'), key: 'action', width: 130 }
]
const { list, loading, pagination, load, onTableChange } = useTable(pagePeriod, reactive({}))
const target = ref(currentPeriod())
const saving = ref(false)
onMounted(load)

const countLevels = (items) => (items || []).reduce((c, i) => ({ ...c, [i.level]: (c[i.level] || 0) + 1 }), {})

// ---------------------------------------------------------------- 结账检查
const checking = ref(false)
const checkItems = ref(null)
const checkedPeriod = ref()
const counts = computed(() => countLevels(checkItems.value))
async function runCheck() {
  checking.value = true
  try {
    checkedPeriod.value = target.value
    checkItems.value = await closeCheck(target.value)
  } finally {
    checking.value = false
  }
}

// ---------------------------------------------------------------- 结账
const closeOpen = ref(false)
const closeTarget = ref()
const closeItems = ref([])
const warnConfirmed = ref(false)
const closeCounts = computed(() => countLevels(closeItems.value))
async function close(period) {
  closeTarget.value = period
  closeItems.value = []
  warnConfirmed.value = false
  closeOpen.value = true
  checking.value = true
  try {
    closeItems.value = await closeCheck(period)
  } catch (e) {
    closeOpen.value = false
    throw e
  } finally {
    checking.value = false
  }
}
async function doClose() {
  saving.value = true
  try {
    await closePeriod(closeTarget.value)
    message.success(t('fin.period.closeSuccess'))
    closeOpen.value = false
    if (checkedPeriod.value === closeTarget.value) checkItems.value = null
    load()
  } finally {
    saving.value = false
  }
}

const reopen = useAction(reopenPeriod, { successKey: 'fin.period.reopened', reload: () => load() })

// ---------------------------------------------------------------- 增值税结转
const vatOpen = ref(false)
const vatSaving = ref(false)
const vatResult = ref()
const vatForm = reactive({ cityRate: 7, eduRate: 3, localRate: 2 })
function openVat() {
  vatResult.value = undefined
  vatOpen.value = true
}
async function runVat() {
  vatSaving.value = true
  try {
    vatResult.value = await transferVat(target.value, { ...vatForm })
    message.success(t('fin.period.vatSuccess'))
  } finally {
    vatSaving.value = false
  }
}

const voucherOpen = ref(false)
const voucherId = ref()
function openVoucher(id) {
  voucherId.value = id
  voucherOpen.value = true
}
</script>

<style scoped>
.tip {
  margin-bottom: 16px;
}
.toolbar {
  margin-bottom: 12px;
}
.check-panel {
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid var(--app-border, #f0f0f0);
  border-radius: 8px;
}
.check-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-weight: 500;
}
.ok {
  color: #52c41a;
}
.warn {
  color: #fa8c16;
}
.error {
  color: #cf1322;
}
.mt {
  margin-top: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
.vat-result {
  text-align: left;
}
</style>
