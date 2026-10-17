<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" class="eval-tabs">
        <template #rightExtra>
          <a-button v-perm="'scm:eval:add'" type="primary" @click="openEval()">
            <template #icon><PlusOutlined /></template>{{ $t('scm.eval.add') }}
          </a-button>
        </template>

        <a-tab-pane key="kpi" :tab="$t('scm.eval.kpiTab')">
          <div class="filters">
            <a-input-search v-model:value="kpiQuery.keyword" :placeholder="$t('scm.eval.supplierKeyword')" allow-clear class="filter" @search="kpiTable.search" />
            <a-range-picker v-model:value="kpiQuery.range" value-format="YYYY-MM-DD" @change="kpiTable.search" />
            <span class="sub-text">{{ $t('scm.eval.kpiTip') }}</span>
          </div>
          <a-table
            row-key="supplierId"
            size="middle"
            :columns="kpiColumns"
            :data-source="kpiTable.list.value"
            :loading="kpiTable.loading.value"
            :pagination="kpiTable.pagination"
            :scroll="{ x: 1200 }"
            @change="kpiTable.onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'supplier'">
                <div>{{ record.supplierName }}</div>
                <div class="sub-text">{{ record.supplierCode }}</div>
              </template>
              <template v-else-if="column.key === 'level'">
                <DictTag type="scm_supplier_level" :value="record.level" />
              </template>
              <template v-else-if="column.key === 'purchaseAmount'">¥{{ formatMoney(record.purchaseAmount) }}</template>
              <template v-else-if="column.key === 'onTimeRate'">
                <template v-if="record.onTimeRate != null">
                  <a-progress :percent="Number(record.onTimeRate)" size="small" :status="record.onTimeRate < 90 ? 'exception' : 'success'" />
                  <div class="sub-text">{{ $t('scm.eval.lines', { done: record.onTimeLines, total: record.inLines }) }}</div>
                </template>
                <span v-else class="sub-text">{{ $t('scm.eval.noReceipt') }}</span>
              </template>
              <template v-else-if="column.key === 'iqcPassRate'">
                <template v-if="record.iqcPassRate != null">
                  <span :class="{ danger: record.iqcPassRate < 95 }">{{ record.iqcPassRate }}%</span>
                  <span class="sub-text">{{ $t('scm.eval.batches', { pass: record.iqcPassBatches, total: record.iqcBatches }) }}</span>
                </template>
                <span v-else class="sub-text">-</span>
                <div v-if="record.claimCount" class="sub-text danger">{{ $t('scm.eval.claims', { n: record.claimCount, amount: formatMoney(record.claimAmount) }) }}</div>
              </template>
              <template v-else-if="column.key === 'returnRate'">
                <span v-if="record.returnRate != null" :class="{ danger: record.returnRate > 2 }">{{ record.returnRate }}%</span>
                <span v-else class="sub-text">-</span>
                <div class="sub-text">{{ $t('scm.eval.returnOfIn', { ret: formatQty(record.returnQty), qty: formatQty(record.inQty) }) }}</div>
              </template>
              <template v-else-if="column.key === 'last'">
                <template v-if="record.lastScore != null">
                  <b>{{ Number(record.lastScore) }}</b> {{ $t('scm.eval.points') }}
                  <DictTag type="scm_supplier_level" :value="record.lastGrade" />
                  <div class="sub-text">{{ record.lastPeriod }}</div>
                </template>
                <span v-else class="sub-text">{{ $t('scm.eval.notEvaluated') }}</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button v-perm="'scm:eval:add'" type="link" size="small" @click="openEval(record)">{{ $t('scm.eval.evaluate') }}</a-button>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="records" :tab="$t('scm.eval.recordsTab')">
          <div class="filters">
            <PartnerSelect v-model:value="recordQuery.supplierId" type="supplier" :placeholder="$t('scm.eval.allSuppliers')" allow-clear class="filter" />
            <a-input-search v-model:value="recordQuery.period" :placeholder="$t('scm.eval.period')" allow-clear class="filter" @search="recordTable.search" />
          </div>
          <a-table
            row-key="id"
            size="middle"
            :columns="recordColumns"
            :data-source="recordTable.list.value"
            :loading="recordTable.loading.value"
            :pagination="recordTable.pagination"
            :scroll="{ x: 1200 }"
            @change="recordTable.onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'supplier'">
                <div>{{ record.supplierName }}</div>
                <div class="sub-text">{{ record.supplierCode }}</div>
              </template>
              <template v-else-if="column.key === 'scores'">
                {{ $t('scm.eval.scoresText', { q: Number(record.qualityScore), d: Number(record.deliveryScore), p: Number(record.priceScore), s: Number(record.serviceScore) }) }}
              </template>
              <template v-else-if="column.key === 'total'">
                <b>{{ Number(record.totalScore) }}</b>
              </template>
              <template v-else-if="column.key === 'grade'">
                <DictTag type="scm_supplier_level" :value="record.grade" />
              </template>
              <template v-else-if="column.key === 'action'">
                <span v-perm="'scm:eval:remove'">
                  <a-popconfirm :title="$t('scm.eval.confirmDelete')" @confirm="remove(record.id)">
                    <a-button type="link" danger size="small">{{ $t('common.action.delete') }}</a-button>
                  </a-popconfirm>
                </span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal v-model:open="evalOpen" :title="$t('scm.eval.title')" :confirm-loading="saving" width="600px" @ok="submit">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="14">
            <a-form-item :label="$t('scm.eval.supplier')" name="supplierId">
              <PartnerSelect v-model:value="form.supplierId" type="supplier" :label="form.supplierLabel" />
            </a-form-item>
          </a-col>
          <a-col :span="10">
            <a-form-item :label="$t('scm.eval.period')" name="period">
              <a-input v-model:value="form.period" :maxlength="16" :placeholder="$t('scm.eval.periodPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col v-for="s in SCORES" :key="s.key" :span="12">
            <a-form-item :label="$t('scm.eval.scoreLabel', { label: s.label, weight: s.weight })" :name="s.key">
              <a-input-number v-model:value="form[s.key]" :min="0" :max="100" :precision="1" :addon-after="$t('scm.eval.points')" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <div class="total">
          {{ $t('scm.eval.totalLabel') }} <b>{{ total }}</b>{{ $t('scm.eval.gradeLabel') }} <DictTag type="scm_supplier_level" :value="grade" />
        </div>
        <a-form-item :label="$t('scm.eval.comment')" name="remark">
          <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { addEval, deleteEval, pageEval, supplierKpi } from '@/api/scm/partner'
import { useTable } from '@/composables/useTable'
import { formatMoney } from '@/components/scm/scm'
import { formatQty } from '@/components/wms/wms'

const { t } = useI18n()
const tab = ref('kpi')

const kpiColumns = [
  { title: t('scm.eval.supplier'), key: 'supplier', width: 220 },
  { title: t('scm.eval.level'), key: 'level', width: 90 },
  { title: t('scm.eval.orderCount'), dataIndex: 'orderCount', width: 90, align: 'right' },
  { title: t('scm.eval.purchaseAmount'), key: 'purchaseAmount', width: 140, align: 'right' },
  { title: t('scm.eval.onTimeRate'), key: 'onTimeRate', width: 180 },
  { title: t('scm.eval.iqcPassRate'), key: 'iqcPassRate', width: 170 },
  { title: t('scm.eval.returnRate'), key: 'returnRate', width: 170 },
  { title: t('scm.eval.last'), key: 'last', width: 160 },
  { title: t('common.action.operation'), key: 'action', width: 100, fixed: 'right' }
]
const kpiQuery = reactive({ keyword: undefined, range: undefined })
const kpiTable = useTable(supplierKpi, kpiQuery, {
  params: ({ range, ...rest }) => ({ ...rest, beginDate: range?.[0], endDate: range?.[1] })
})

const recordColumns = [
  { title: t('scm.eval.supplier'), key: 'supplier', width: 220 },
  { title: t('scm.eval.period'), dataIndex: 'period', width: 100 },
  { title: t('scm.eval.scores'), key: 'scores', width: 360 },
  { title: t('scm.eval.total'), key: 'total', width: 110, align: 'right' },
  { title: t('scm.eval.level'), key: 'grade', width: 90 },
  { title: t('scm.eval.comment'), dataIndex: 'remark', width: 180, ellipsis: true },
  { title: t('scm.eval.evaluator'), dataIndex: 'createByName', width: 110 },
  { title: t('scm.eval.evalTime'), dataIndex: 'createTime', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: 100, fixed: 'right' }
]
const recordQuery = reactive({ supplierId: undefined, period: undefined })
const recordTable = useTable(pageEval, recordQuery)
watch(() => recordQuery.supplierId, () => recordTable.search())

onMounted(() => {
  kpiTable.load()
  recordTable.load()
})

// ---------------------------------------------------------------- 评估
const SCORES = [
  { key: 'qualityScore', label: t('scm.eval.quality'), weight: 40 },
  { key: 'deliveryScore', label: t('scm.eval.delivery'), weight: 30 },
  { key: 'priceScore', label: t('scm.eval.price'), weight: 20 },
  { key: 'serviceScore', label: t('scm.eval.service'), weight: 10 }
]
const evalOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const required = (msg) => [{ required: true, message: msg }]
const rules = {
  supplierId: required(t('scm.eval.supplierRequired')),
  period: required(t('scm.eval.periodRequired')),
  qualityScore: required(t('scm.eval.scoreRequired')),
  deliveryScore: required(t('scm.eval.scoreRequired')),
  priceScore: required(t('scm.eval.scoreRequired')),
  serviceScore: required(t('scm.eval.scoreRequired'))
}

const total = computed(() => {
  const f = form.value
  const sum = SCORES.reduce((s, x) => s + (Number(f[x.key]) || 0) * x.weight, 0) / 100
  return Math.round(sum * 10) / 10
})
const grade = computed(() => (total.value >= 90 ? 'A' : total.value >= 75 ? 'B' : total.value >= 60 ? 'C' : 'D'))

function qualityScore(kpi) {
  const base = kpi?.iqcPassRate != null
    ? Number(kpi.iqcPassRate)
    : kpi?.returnRate != null
      ? Math.max(0, Math.round(100 - Number(kpi.returnRate) * 5))
      : undefined
  return base == null ? undefined : Math.max(0, base - Number(kpi.claimCount || 0) * 5)
}

/** 从绩效发起评估时，按准时交货率、退货率给出交期、质量的建议分 */
function openEval(kpi) {
  const quarter = `${dayjs().year()}-Q${Math.floor(dayjs().month() / 3) + 1}`
  form.value = {
    supplierId: kpi?.supplierId,
    supplierLabel: kpi ? `${kpi.supplierCode} ${kpi.supplierName}` : undefined,
    period: quarter,
    // 质量得分：优先取来料检验批次合格率，没有检验记录时按退货率折算；每次质量索赔扣 5 分
    qualityScore: qualityScore(kpi),
    deliveryScore: kpi?.onTimeRate != null ? Number(kpi.onTimeRate) : undefined,
    priceScore: undefined,
    serviceScore: undefined,
    remark: ''
  }
  evalOpen.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const { supplierLabel, ...data } = form.value
    await addEval(data)
    message.success(t('scm.eval.done', { score: total.value }))
    evalOpen.value = false
    kpiTable.load()
    recordTable.load()
  } finally {
    saving.value = false
  }
}

async function remove(id) {
  await deleteEval(id)
  message.success(t('common.msg.deleteSuccess'))
  recordTable.load()
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
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.danger {
  color: #ff4d4f;
}
.full {
  width: 100%;
}
.total {
  margin-bottom: 16px;
}
</style>
