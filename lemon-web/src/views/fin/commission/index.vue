<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="sheet" tab="提成表">
          <div class="toolbar">
            <a-space wrap>
              <span>期间</span>
              <PeriodPicker v-model:value="period" @update:value="loadSheet" />
              <template v-if="sheet">
                <a-tag :color="sheet.status === 'CONFIRMED' ? 'green' : 'default'" :bordered="false">
                  {{ sheet.status === 'CONFIRMED' ? '已确认' : '草稿' }}
                </a-tag>
                <span>提成合计 <b>¥{{ formatMoney(sheet.totalAmount) }}</b></span>
              </template>
            </a-space>
            <a-space v-perm="'fin:commission:edit'">
              <a-button v-if="!sheet || sheet.status === 'DRAFT'" :loading="generating" @click="generate">
                <template #icon><CalculatorOutlined /></template>{{ sheet ? '重新计算' : '计算提成' }}
              </a-button>
              <a-popconfirm v-if="sheet?.status === 'DRAFT'" title="确认提成表？确认后生成的工资表计入销售提成。" @confirm="confirm">
                <a-button type="primary">确认</a-button>
              </a-popconfirm>
              <a-popconfirm v-if="sheet?.status === 'CONFIRMED'" title="取消确认？已生成的工资表不会自动更新，需重新生成草稿工资表。" @confirm="unconfirm">
                <a-button>取消确认</a-button>
              </a-popconfirm>
            </a-space>
          </div>
          <a-alert
            type="info"
            show-icon
            class="tip"
            message="按销售订单的业务员统计当月业绩：销售额为销售出库的不含税金额（扣除退货），毛利 = 销售额 - 出库数量 × 当期成本单价（未核算时取参考成本价），回款为收款核销的销售应收。重新计算时保留手工调整。"
          />
          <a-table
            row-key="id"
            size="middle"
            :columns="sheetColumns"
            :data-source="sheet?.lines || []"
            :loading="loading"
            :pagination="false"
            :scroll="{ x: 1100, y: '100%' }"
            :locale="{ emptyText: sheet ? '本期没有销售业绩' : '本期还没有计算提成' }"
          >
            <template #expandedRowRender="{ record }">
              <a-table row-key="key" size="small" :columns="detailColumns" :data-source="detailRows(record)" :pagination="false">
                <template #bodyCell="{ column, record: d }">
                  <template v-if="column.key === 'basis'"><DictTag type="fin_commission_basis" :value="d.basis" /></template>
                  <template v-else-if="column.key === 'category'">
                    <DictTag v-if="d.category" type="wms_material_category" :value="d.category" /><span v-else class="sub-text">全部</span>
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
                <a-button v-if="sheet.status === 'DRAFT'" v-perm="'fin:commission:edit'" type="link" size="small" @click="openAdjust(record)">调整</a-button>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="rule" tab="提成规则">
          <div class="toolbar">
            <span class="sub-text">同一业务员、物料分类、依据取最具体（指定业务员 → 指定分类 → 通用）且门槛最高的已达成规则；不同依据的提成相加</span>
            <a-button v-perm="'fin:commission:rule'" type="primary" @click="drawer.openCreate()">
              <template #icon><PlusOutlined /></template>新增规则
            </a-button>
          </div>
          <a-table row-key="id" size="middle" :columns="ruleColumns" :data-source="rules" :loading="rulesLoading" :pagination="false" :scroll="{ x: 1000 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'basis'"><DictTag type="fin_commission_basis" :value="record.basis" /></template>
              <template v-else-if="column.key === 'salesman'">{{ record.salesmanName || '全部业务员' }}</template>
              <template v-else-if="column.key === 'category'">
                <DictTag v-if="record.materialCategory" type="wms_material_category" :value="record.materialCategory" /><span v-else class="sub-text">全部</span>
              </template>
              <template v-else-if="column.key === 'threshold'">{{ Number(record.threshold) ? `≥ ${formatMoney(record.threshold)}` : '-' }}</template>
              <template v-else-if="column.key === 'rate'"><b>{{ Number(record.rate) }}%</b></template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="record.status === 0 ? 'default' : 'green'" :bordered="false">{{ record.status === 0 ? '停用' : '启用' }}</a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <span v-perm="'fin:commission:rule'" class="table-actions">
                  <a-button type="link" size="small" @click="drawer.openEdit(record)">编辑</a-button>
                  <a-popconfirm :title="`删除规则「${record.name}」？`" @confirm="removeRule(record.id)">
                    <a-button type="link" danger size="small">删除</a-button>
                  </a-popconfirm>
                </span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal v-model:open="adjustOpen" :title="adjustRow ? `调整提成：${adjustRow.salesmanName}` : ''" :confirm-loading="adjusting" @ok="submitAdjust">
      <a-form layout="vertical">
        <a-form-item label="调整金额" extra="正数增加、负数扣减；应发提成 = 规则计算的提成 + 调整">
          <a-input-number v-model:value="adjustForm.adjustAmount" :precision="2" prefix="¥" class="full" />
        </a-form-item>
        <a-form-item label="调整原因">
          <a-input v-model:value="adjustForm.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>

    <FormDrawer :drawer="drawer" create-title="新增提成规则" edit-title="编辑提成规则">
      <a-form ref="formRef" :model="form" :rules="formRules" layout="vertical">
        <a-form-item label="规则名称" name="name">
          <a-input v-model:value="form.name" :maxlength="64" placeholder="如：销售额基础提成" />
        </a-form-item>
        <a-form-item label="提成依据" name="basis">
          <a-radio-group v-model:value="form.basis" button-style="solid" @change="form.basis === 'RECEIPT' && (form.materialCategory = undefined)">
            <a-radio-button v-for="d in dict.fin_commission_basis" :key="d.value" :value="d.value">{{ d.label }}</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="业务员" name="salesmanId" extra="为空适用全部业务员">
          <IdentityUserSelect v-model:value="form.salesmanId" :label="form.salesmanName" placeholder="全部业务员" />
        </a-form-item>
        <a-form-item label="物料分类" name="materialCategory" :extra="form.basis === 'RECEIPT' ? '回款不区分物料分类' : '为空适用全部分类'">
          <a-select v-model:value="form.materialCategory" :options="toOptions(dict.wms_material_category)" allow-clear placeholder="全部分类" :disabled="form.basis === 'RECEIPT'" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="业绩门槛" name="threshold" extra="当月该依据业绩达到才适用，用于阶梯提成">
              <a-input-number v-model:value="form.threshold" :min="0" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="提成比例" name="rate">
              <a-input-number v-model:value="form.rate" :min="0" :max="100" :precision="3" addon-after="%" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="状态" name="status">
          <a-switch :checked="form.status !== 0" checked-children="启用" un-checked-children="停用" @change="(v) => (form.status = v ? 1 : 0)" />
        </a-form-item>
        <a-form-item label="备注" name="remark">
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

/** 销售提成：按业务员统计当月业绩，按规则计算提成，确认后计入工资表 */
const dict = useDict('fin_commission_basis', 'wms_material_category')
const tab = ref('sheet')
const period = ref(currentPeriod())

// ---------------------------------------------------------------- 提成表
const sheetColumns = [
  { title: '业务员', dataIndex: 'salesmanName', width: 140 },
  { title: '销售额', key: 'salesAmount', width: 130, align: 'right' },
  { title: '毛利', key: 'profitAmount', width: 120, align: 'right' },
  { title: '回款', key: 'receiptAmount', width: 130, align: 'right' },
  { title: '规则提成', key: 'commissionAmount', width: 120, align: 'right' },
  { title: '调整', key: 'adjustAmount', width: 110, align: 'right' },
  { title: '应发提成', key: 'finalAmount', width: 130, align: 'right' },
  { title: '调整原因', dataIndex: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]
const detailColumns = [
  { title: '依据', key: 'basis', width: 90 },
  { title: '物料分类', key: 'category', width: 120 },
  { title: '业绩', key: 'base', width: 130, align: 'right' },
  { title: '规则', dataIndex: 'ruleName' },
  { title: '比例', key: 'rate', width: 90, align: 'right' },
  { title: '提成', key: 'amount', width: 120, align: 'right' }
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
    message.success('已计算')
    loadSheet()
  } finally {
    generating.value = false
  }
}
async function confirm() {
  await confirmCommission(period.value)
  message.success('已确认')
  loadSheet()
}
async function unconfirm() {
  await unconfirmCommission(period.value)
  message.success('已取消确认')
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
    message.success('已调整')
    adjustOpen.value = false
    loadSheet()
  } finally {
    adjusting.value = false
  }
}

// ---------------------------------------------------------------- 规则
const ruleColumns = [
  { title: '规则名称', dataIndex: 'name', width: 180 },
  { title: '依据', key: 'basis', width: 90 },
  { title: '业务员', key: 'salesman', width: 130 },
  { title: '物料分类', key: 'category', width: 110 },
  { title: '业绩门槛', key: 'threshold', width: 130, align: 'right' },
  { title: '比例', key: 'rate', width: 90, align: 'right' },
  { title: '状态', key: 'status', width: 80 },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 130, fixed: 'right' }
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
  name: [{ required: true, whitespace: true, message: '请填写规则名称' }],
  basis: [{ required: true, message: '请选择提成依据' }],
  rate: [{ required: true, message: '请填写提成比例' }]
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
  message.success('删除成功')
  loadRules()
}

watch(tab, (t) => t === 'rule' && loadRules())
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
