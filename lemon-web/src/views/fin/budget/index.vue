<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <div class="toolbar">
        <a-space wrap>
          <span>年度</span>
          <a-date-picker v-model:value="yearValue" picker="year" value-format="YYYY" :allow-clear="false" @change="load" />
          <DeptSelect v-model:value="deptId" placeholder="全部部门" class="dept" />
          <a-radio-group v-model:value="tab" button-style="solid">
            <a-radio-button value="execution">执行分析</a-radio-button>
            <a-radio-button value="budget">预算编制</a-radio-button>
          </a-radio-group>
        </a-space>
        <a-button v-perm="'fin:budget:edit'" type="primary" @click="openForm()">
          <template #icon><PlusOutlined /></template>编制预算
        </a-button>
      </div>

      <a-table
        v-if="tab === 'execution'"
        row-key="budgetId"
        size="middle"
        :columns="execColumns"
        :data-source="execution"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1100 }"
        :locale="{ emptyText: '本年度还没有编制预算' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'account'">
            <div>{{ record.accountCode }} {{ record.accountName }}</div>
            <div class="sub-text">{{ record.deptName }}</div>
          </template>
          <template v-else-if="column.key === 'totalBudget'">¥{{ formatMoney(record.totalBudget) }}</template>
          <template v-else-if="column.key === 'totalActual'">
            <span :class="{ over: overBudget(record) }">¥{{ formatMoney(record.totalActual) }}</span>
          </template>
          <template v-else-if="column.key === 'rate'">
            <a-progress
              v-if="record.rate != null"
              :percent="Math.min(Number(record.rate), 100)"
              :status="Number(record.rate) > 100 ? 'exception' : Number(record.rate) > 90 ? 'active' : 'normal'"
              :format="() => `${record.rate}%`"
              size="small"
            />
          </template>
          <template v-else-if="column.key === 'months'">
            <div class="months">
              <div v-for="(b, i) in record.budgets" :key="i" class="month" :class="{ over: Number(record.actuals[i]) > Number(b) && Number(b) > 0 }">
                <div class="m">{{ i + 1 }}月</div>
                <div class="a">{{ short(record.actuals[i]) }}</div>
                <div class="b">/{{ short(b) }}</div>
              </div>
            </div>
          </template>
        </template>
      </a-table>

      <a-table
        v-else
        row-key="id"
        size="middle"
        :columns="budgetColumns"
        :data-source="budgets"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1100 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'account'">
            <div>{{ record.accountCode }} {{ record.accountName }}</div>
            <div class="sub-text">{{ record.deptId === 0 ? '全公司' : record.deptName }}</div>
          </template>
          <template v-else-if="column.key === 'total'">¥{{ formatMoney(record.total) }}</template>
          <template v-else-if="column.key === 'controlMode'"><DictTag type="fin_budget_control" :value="record.controlMode || 'NONE'" /></template>
          <template v-else-if="column.key === 'months'">
            <span class="sub-text">{{ MONTH_KEYS.map((k) => short(record[k])).join(' · ') }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span v-perm="'fin:budget:edit'" class="table-actions">
              <a-button type="link" size="small" @click="openForm(record)">编辑</a-button>
              <a-popconfirm title="确定删除该预算？" @confirm="remove(record.id)">
                <a-button type="link" danger size="small">删除</a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="formOpen" :title="form.id ? '编辑预算' : '编制预算'" width="720px" :confirm-loading="saving" @ok="submit">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-form-item label="年度" required>
              <a-input-number v-model:value="form.budgetYear" :min="2000" :max="2999" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="部门" extra="不选为全公司">
              <DeptSelect v-model:value="form.deptId" />
            </a-form-item>
          </a-col>
          <a-col :span="10">
            <a-form-item label="科目" name="accountId" :rules="[{ required: true, message: '请选择科目' }]" extra="可选上级科目，执行数按下级汇总">
              <AccountSelect v-model:value="form.accountId" all :prefix="['5', '6']" />
            </a-form-item>
          </a-col>
        </a-row>
        <div class="months-head">
          <span>各月预算（合计 ¥{{ formatMoney(form.months?.reduce((s, v) => s + (Number(v) || 0), 0) || 0) }}）</span>
          <a-space>
            <a-input-number v-model:value="spread" :min="0" :precision="2" placeholder="全年金额" size="small" />
            <a-button size="small" @click="spreadEvenly">平均分摊到 12 个月</a-button>
          </a-space>
        </div>
        <a-row :gutter="[12, 8]">
          <a-col v-for="(m, i) in form.months" :key="i" :span="6">
            <a-input-number v-model:value="form.months[i]" :min="0" :precision="2" :addon-before="`${i + 1}月`" class="full" />
          </a-col>
        </a-row>
        <a-row :gutter="16" class="remark">
          <a-col :xs="24" :sm="10">
            <a-form-item label="预算控制" extra="费用报销提交时检查预算执行情况">
              <a-select v-model:value="form.controlMode" :options="toOptions(dict.fin_budget_control)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="14">
            <a-form-item label="备注">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import DeptSelect from '@/components/fin/DeptSelect.vue'
import { addBudget, budgetExecution, deleteBudget, listBudget, updateBudget } from '@/api/fin'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { formatMoney } from '@/components/fin/fin'

const dict = useDict('fin_budget_control')

const MONTH_KEYS = ['m01', 'm02', 'm03', 'm04', 'm05', 'm06', 'm07', 'm08', 'm09', 'm10', 'm11', 'm12']
const yearValue = ref(String(dayjs().year()))
const year = computed(() => Number(yearValue.value))
const deptId = ref()
const tab = ref('execution')
const loading = ref(false)
const execution = ref([])
const budgets = ref([])

const short = (n) => {
  const v = Number(n) || 0
  if (v === 0) return '0'
  return Math.abs(v) >= 10000 ? `${(v / 10000).toFixed(1)}万` : v.toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}
const overBudget = (r) => r.rate != null && Number(r.rate) > 100

const execColumns = [
  { title: '科目 / 部门', key: 'account', width: 220, fixed: 'left' },
  { title: '全年预算', key: 'totalBudget', width: 130, align: 'right' },
  { title: '执行数', key: 'totalActual', width: 130, align: 'right' },
  { title: '执行率', key: 'rate', width: 160 },
  { title: '各月执行 / 预算', key: 'months', width: 400 }
]
const budgetColumns = [
  { title: '科目 / 部门', key: 'account', width: 240 },
  { title: '全年预算', key: 'total', width: 140, align: 'right' },
  { title: '预算控制', key: 'controlMode', width: 120 },
  { title: '1-12 月', key: 'months' },
  { title: '备注', dataIndex: 'remark', width: 150, ellipsis: true },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
]

async function load() {
  loading.value = true
  try {
    const params = { year: year.value, deptId: deptId.value ?? undefined }
    ;[execution.value, budgets.value] = await Promise.all([budgetExecution(params), listBudget(params)])
  } finally {
    loading.value = false
  }
}
watch(deptId, load)
onMounted(load)

const formOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const spread = ref()
function openForm(record) {
  form.value = record
    ? { id: record.id, budgetYear: record.budgetYear, deptId: record.deptId || undefined, accountId: record.accountId,
        months: MONTH_KEYS.map((k) => Number(record[k])), controlMode: record.controlMode || 'NONE', remark: record.remark }
    : { budgetYear: year.value, deptId: deptId.value, accountId: undefined, months: Array(12).fill(0), controlMode: 'NONE', remark: '' }
  spread.value = undefined
  formOpen.value = true
}
function spreadEvenly() {
  if (!spread.value) return
  const each = Math.floor((spread.value / 12) * 100) / 100
  form.value.months = Array(12).fill(each)
  form.value.months[11] = Math.round((spread.value - each * 11) * 100) / 100
}
async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const { id, ...data } = form.value
    data.deptId = data.deptId || null
    id ? await updateBudget(id, data) : await addBudget(data)
    message.success('保存成功')
    formOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function remove(id) {
  await deleteBudget(id)
  message.success('删除成功')
  load()
}
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.dept {
  width: 200px;
}
.months {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 4px;
}
.month {
  min-width: 52px;
  text-align: center;
  padding: 2px 4px;
  border-radius: 4px;
  background: var(--app-bg-2, #fafafa);
  font-size: 12px;
}
.month .m {
  color: var(--app-text-3);
}
.month .b {
  color: var(--app-text-3);
}
.month.over,
.over {
  color: #cf1322;
}
.months-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.remark {
  margin-top: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
</style>
