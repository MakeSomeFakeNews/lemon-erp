<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-alert
        type="info"
        show-icon
        class="tip"
        message="结账前本期凭证必须全部记账，之前的期间必须已结账；结账时自动生成并记账「结转损益」凭证，把损益类科目余额转入本年利润。反结账只能从最后一个已结账的期间开始。"
      />
      <div class="toolbar">
        <a-space wrap>
          <span>结账期间</span>
          <PeriodPicker v-model:value="target" @update:value="checkItems = null" />
          <a-button :loading="checking" @click="runCheck">
            <template #icon><SafetyCertificateOutlined /></template>结账检查
          </a-button>
          <a-button v-perm="'fin:period:tax'" @click="openVat">
            <template #icon><CalculatorOutlined /></template>增值税结转
          </a-button>
          <a-button v-perm="'fin:period:close'" type="primary" :loading="saving" @click="close(target)">结账</a-button>
        </a-space>
      </div>
      <div v-if="checkItems" class="check-panel">
        <div class="check-head">
          <span>
            {{ periodText(checkedPeriod) }} 结账检查：
            <span v-if="counts.ERROR" class="error">{{ counts.ERROR }} 项错误</span>
            <span v-if="counts.WARN" class="warn">{{ counts.ERROR ? '，' : '' }}{{ counts.WARN }} 项提示</span>
            <span v-if="!counts.ERROR && !counts.WARN" class="ok">全部通过</span>
          </span>
          <a @click="checkItems = null">收起</a>
        </div>
        <CloseCheckList :items="checkItems" />
      </div>
      <a-table row-key="period" size="middle" :columns="columns" :data-source="list" :loading="loading" :pagination="pagination" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'period'">{{ periodText(record.period) }}</template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 'CLOSED' ? 'default' : 'green'" :bordered="false">{{ record.status === 'CLOSED' ? '已结账' : '未结账' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'vouchers'">
            {{ record.voucherCount }} 张
            <span v-if="record.unpostedCount" class="warn">（{{ record.unpostedCount }} 张未记账）</span>
          </template>
          <template v-else-if="column.key === 'close'">
            <template v-if="record.closeTime">{{ record.closeByName }} {{ record.closeTime }}</template>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span v-perm="'fin:period:close'">
              <a-button v-if="record.status === 'OPEN'" type="link" size="small" @click="close(record.period)">结账</a-button>
              <a-popconfirm v-else title="反结账将删除本期结转损益凭证，确定？" @confirm="reopen(record.period)">
                <a-button type="link" size="small">反结账</a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 结账：先显示检查结果，有错误时不能结账，有提示时须确认 -->
    <a-modal
      v-model:open="closeOpen"
      :title="`结账 ${periodText(closeTarget)}`"
      width="min(640px, 100vw)"
      ok-text="结账"
      :confirm-loading="saving"
      :ok-button-props="{ disabled: !!closeCounts.ERROR || (!!closeCounts.WARN && !warnConfirmed) }"
      @ok="doClose"
    >
      <a-spin :spinning="checking">
        <CloseCheckList :items="closeItems" />
        <a-alert v-if="closeCounts.ERROR" type="error" show-icon class="mt" message="存在错误项，请处理后再结账。" />
        <template v-else>
          <a-checkbox v-if="closeCounts.WARN" v-model:checked="warnConfirmed" class="mt">我已了解以上 {{ closeCounts.WARN }} 项提示，仍然结账</a-checkbox>
          <div class="mt sub-text">结账后本期不能再录入、修改凭证。</div>
        </template>
      </a-spin>
    </a-modal>

    <!-- 增值税结转 -->
    <a-modal v-model:open="vatOpen" :title="`增值税结转 ${periodText(target)}`" width="min(560px, 100vw)" :footer="vatResult ? null : undefined"
      ok-text="结转" :confirm-loading="vatSaving" @ok="runVat">
      <template v-if="!vatResult">
        <a-alert
          type="info"
          show-icon
          class="tip"
          message="按本期销项税额减进项税额计算应交增值税，并按税率计提城建税、教育费附加、地方教育附加，生成结转凭证。"
        />
        <a-form layout="vertical">
          <a-row :gutter="12">
            <a-col :span="8">
              <a-form-item label="城建税税率">
                <a-input-number v-model:value="vatForm.cityRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="教育费附加">
                <a-input-number v-model:value="vatForm.eduRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="地方教育附加">
                <a-input-number v-model:value="vatForm.localRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </template>
      <template v-else>
        <a-result status="success" title="增值税已结转" :sub-title="vatResult.voucherId ? '已生成结转凭证，审核记账后结账' : '本期没有需要结转的税额'">
          <template #extra>
            <a-descriptions :column="1" size="small" bordered class="vat-result">
              <a-descriptions-item label="销项税额">¥{{ formatMoney(vatResult.output) }}</a-descriptions-item>
              <a-descriptions-item label="进项税额">¥{{ formatMoney(vatResult.input) }}</a-descriptions-item>
              <a-descriptions-item label="应交增值税"><b>¥{{ formatMoney(vatResult.payable) }}</b></a-descriptions-item>
              <a-descriptions-item label="附加税费">¥{{ formatMoney(vatResult.surcharge) }}</a-descriptions-item>
            </a-descriptions>
            <a-space class="mt">
              <a-button v-if="vatResult.voucherId" @click="openVoucher(vatResult.voucherId)">查看凭证</a-button>
              <a-button type="primary" @click="vatOpen = false">完成</a-button>
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

const columns = [
  { title: '会计期间', key: 'period', width: 160 },
  { title: '状态', key: 'status', width: 100 },
  { title: '凭证', key: 'vouchers', width: 200 },
  { title: '结账', key: 'close', width: 260 },
  { title: '操作', key: 'action', width: 120 }
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
    message.success('结账成功')
    closeOpen.value = false
    if (checkedPeriod.value === closeTarget.value) checkItems.value = null
    load()
  } finally {
    saving.value = false
  }
}

async function reopen(period) {
  await reopenPeriod(period)
  message.success('已反结账')
  load()
}

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
    message.success('增值税结转完成')
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
