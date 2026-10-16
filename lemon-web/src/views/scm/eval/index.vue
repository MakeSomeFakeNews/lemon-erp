<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" class="eval-tabs">
        <template #rightExtra>
          <a-button v-perm="'scm:eval:add'" type="primary" @click="openEval()">
            <template #icon><PlusOutlined /></template>新增评估
          </a-button>
        </template>

        <a-tab-pane key="kpi" tab="供应商绩效">
          <div class="filters">
            <a-input-search v-model:value="kpiQuery.keyword" placeholder="供应商编码或名称" allow-clear class="filter" @search="kpiTable.search" />
            <a-range-picker v-model:value="kpiQuery.range" value-format="YYYY-MM-DD" @change="kpiTable.search" />
            <span class="sub-text">按入库业务日期统计；准时 = 入库日期不晚于订单明细交货日期</span>
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
                  <div class="sub-text">{{ record.onTimeLines }} / {{ record.inLines }} 行</div>
                </template>
                <span v-else class="sub-text">无入库</span>
              </template>
              <template v-else-if="column.key === 'iqcPassRate'">
                <template v-if="record.iqcPassRate != null">
                  <span :class="{ danger: record.iqcPassRate < 95 }">{{ record.iqcPassRate }}%</span>
                  <span class="sub-text">（{{ record.iqcPassBatches }}/{{ record.iqcBatches }} 批）</span>
                </template>
                <span v-else class="sub-text">-</span>
                <div v-if="record.claimCount" class="sub-text danger">质量索赔 {{ record.claimCount }} 次 ¥{{ formatMoney(record.claimAmount) }}</div>
              </template>
              <template v-else-if="column.key === 'returnRate'">
                <span v-if="record.returnRate != null" :class="{ danger: record.returnRate > 2 }">{{ record.returnRate }}%</span>
                <span v-else class="sub-text">-</span>
                <div class="sub-text">退 {{ formatQty(record.returnQty) }} / 入 {{ formatQty(record.inQty) }}</div>
              </template>
              <template v-else-if="column.key === 'last'">
                <template v-if="record.lastScore != null">
                  <b>{{ Number(record.lastScore) }}</b> 分
                  <DictTag type="scm_supplier_level" :value="record.lastGrade" />
                  <div class="sub-text">{{ record.lastPeriod }}</div>
                </template>
                <span v-else class="sub-text">未评估</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button v-perm="'scm:eval:add'" type="link" size="small" @click="openEval(record)">评估</a-button>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="records" tab="评估记录">
          <div class="filters">
            <PartnerSelect v-model:value="recordQuery.supplierId" type="supplier" placeholder="全部供应商" allow-clear class="filter" />
            <a-input-search v-model:value="recordQuery.period" placeholder="考核期间" allow-clear class="filter" @search="recordTable.search" />
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
                质量 {{ Number(record.qualityScore) }} · 交期 {{ Number(record.deliveryScore) }} · 价格 {{ Number(record.priceScore) }} · 服务
                {{ Number(record.serviceScore) }}
              </template>
              <template v-else-if="column.key === 'total'">
                <b>{{ Number(record.totalScore) }}</b>
              </template>
              <template v-else-if="column.key === 'grade'">
                <DictTag type="scm_supplier_level" :value="record.grade" />
              </template>
              <template v-else-if="column.key === 'action'">
                <span v-perm="'scm:eval:remove'">
                  <a-popconfirm title="确定删除该评估记录？" @confirm="remove(record.id)">
                    <a-button type="link" danger size="small">删除</a-button>
                  </a-popconfirm>
                </span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal v-model:open="evalOpen" title="供应商评估" :confirm-loading="saving" width="560px" @ok="submit">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="14">
            <a-form-item label="供应商" name="supplierId">
              <PartnerSelect v-model:value="form.supplierId" type="supplier" :label="form.supplierLabel" />
            </a-form-item>
          </a-col>
          <a-col :span="10">
            <a-form-item label="考核期间" name="period">
              <a-input v-model:value="form.period" :maxlength="16" placeholder="如 2026-Q3" />
            </a-form-item>
          </a-col>
          <a-col v-for="s in SCORES" :key="s.key" :span="12">
            <a-form-item :label="`${s.label}（权重 ${s.weight}%）`" :name="s.key">
              <a-input-number v-model:value="form[s.key]" :min="0" :max="100" :precision="1" addon-after="分" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <div class="total">
          综合得分 <b>{{ total }}</b>，评定等级 <DictTag type="scm_supplier_level" :value="grade" />
        </div>
        <a-form-item label="评语" name="remark">
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
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { addEval, deleteEval, pageEval, supplierKpi } from '@/api/scm/partner'
import { useTable } from '@/composables/useTable'
import { formatMoney } from '@/components/scm/scm'
import { formatQty } from '@/components/wms/wms'

const tab = ref('kpi')

const kpiColumns = [
  { title: '供应商', key: 'supplier', width: 220 },
  { title: '等级', key: 'level', width: 90 },
  { title: '订单数', dataIndex: 'orderCount', width: 80, align: 'right' },
  { title: '入库金额', key: 'purchaseAmount', width: 140, align: 'right' },
  { title: '准时交货率', key: 'onTimeRate', width: 180 },
  { title: '来料合格率', key: 'iqcPassRate', width: 170 },
  { title: '退货率', key: 'returnRate', width: 150 },
  { title: '最近评估', key: 'last', width: 160 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]
const kpiQuery = reactive({ keyword: undefined, range: undefined })
const kpiTable = useTable(supplierKpi, kpiQuery, {
  params: ({ range, ...rest }) => ({ ...rest, beginDate: range?.[0], endDate: range?.[1] })
})

const recordColumns = [
  { title: '供应商', key: 'supplier', width: 220 },
  { title: '考核期间', dataIndex: 'period', width: 100 },
  { title: '各项得分', key: 'scores', width: 300 },
  { title: '综合得分', key: 'total', width: 90, align: 'right' },
  { title: '等级', key: 'grade', width: 90 },
  { title: '评语', dataIndex: 'remark', width: 180, ellipsis: true },
  { title: '评估人', dataIndex: 'createByName', width: 100 },
  { title: '评估时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
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
  { key: 'qualityScore', label: '质量', weight: 40 },
  { key: 'deliveryScore', label: '交期', weight: 30 },
  { key: 'priceScore', label: '价格', weight: 20 },
  { key: 'serviceScore', label: '服务', weight: 10 }
]
const evalOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const required = (msg) => [{ required: true, message: msg }]
const rules = {
  supplierId: required('请选择供应商'),
  period: required('请填写考核期间'),
  qualityScore: required('请打分'),
  deliveryScore: required('请打分'),
  priceScore: required('请打分'),
  serviceScore: required('请打分')
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
    message.success(`评估完成，综合得分 ${total.value}`)
    evalOpen.value = false
    kpiTable.load()
    recordTable.load()
  } finally {
    saving.value = false
  }
}

async function remove(id) {
  await deleteEval(id)
  message.success('删除成功')
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
