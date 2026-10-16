<template>
  <FormDrawer page :drawer="drawer" create-title="新增凭证" edit-title="编辑凭证" width="min(1200px, 100vw)">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="12" :sm="6">
          <a-form-item label="凭证日期" name="voucherDate" :rules="[{ required: true, message: '请选择日期' }]">
            <a-date-picker v-model:value="form.voucherDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="4">
          <a-form-item label="附单据数" name="attachmentCount">
            <a-input-number v-model:value="form.attachmentCount" :min="0" :max="999" addon-after="张" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="14" class="balance">
          <span>借方合计 <b>{{ formatMoney(totalDebit) }}</b></span>
          <span>贷方合计 <b>{{ formatMoney(totalCredit) }}</b></span>
          <a-tag v-if="diff === 0 && totalDebit > 0" color="green" :bordered="false">借贷平衡</a-tag>
          <a-tag v-else-if="diff !== 0" color="red" :bordered="false">差额 {{ formatMoney(Math.abs(diff)) }}</a-tag>
        </a-col>
      </a-row>

      <a-table row-key="key" size="small" class="entries" :columns="columns" :data-source="form.entries" :pagination="false" :scroll="{ x: 1400 }">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'seq'">{{ index + 1 }}</template>
          <template v-else-if="column.key === 'summary'">
            <a-form-item :name="['entries', index, 'summary']" :rules="[{ required: true, whitespace: true, message: '请填写摘要' }]" class="cell">
              <a-input v-model:value="record.summary" :maxlength="255" placeholder="摘要" @focus="copySummary(index)" />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'accountId'">
            <a-form-item :name="['entries', index, 'accountId']" :rules="[{ required: true, message: '请选择科目' }]" class="cell">
              <AccountSelect v-model:value="record.accountId" />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'deptId'">
            <DeptSelect v-model:value="record.deptId" placeholder="选填" />
          </template>
          <template v-else-if="column.key === 'aux'">
            <div class="aux-cell">
              <a-select v-model:value="record.auxType" :options="AUX_TYPES" placeholder="无" allow-clear class="aux-type" @change="clearAux(record)" />
              <ProjectSelect v-if="record.auxType === 'PROJECT'" v-model:value="record.projectId" class="aux-value" />
              <PartnerSelect
                v-else-if="record.auxType"
                v-model:value="record.partnerId"
                :type="record.auxType === 'CUSTOMER' ? 'customer' : 'supplier'"
                :label="record.partnerName"
                class="aux-value"
              />
            </div>
          </template>
          <template v-else-if="column.key === 'debit'">
            <a-input-number
              v-model:value="record.debit"
              :min="0"
              :precision="2"
              class="full amount"
              @change="(v) => v && (record.credit = undefined)"
              @dblclick="fill(record, 'debit')"
            />
          </template>
          <template v-else-if="column.key === 'credit'">
            <a-input-number
              v-model:value="record.credit"
              :min="0"
              :precision="2"
              class="full amount"
              @change="(v) => v && (record.debit = undefined)"
              @dblclick="fill(record, 'credit')"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" :disabled="form.entries.length <= 2" @click="form.entries.splice(index, 1)">
              <DeleteOutlined />
            </a-button>
          </template>
        </template>
      </a-table>
      <div class="entries-foot">
        <a-button type="dashed" @click="addRow">
          <template #icon><PlusOutlined /></template>添加分录
        </a-button>
        <span class="sub-text">摘要获得焦点时自动带出上一行；双击借方 / 贷方金额自动补平差额。只能选择末级科目。</span>
      </div>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import AccountSelect from './AccountSelect.vue'
import DeptSelect from './DeptSelect.vue'
import ProjectSelect from '@/components/pm/ProjectSelect.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { addVoucher, getVoucher, updateVoucher } from '@/api/fin'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { rowKey, today } from '@/components/wms/wms'
import { formatMoney } from './fin'

/** 新增 / 编辑凭证（未审核的手工凭证）。通过 ref 调用 openCreate() / openEdit(record)，保存后触发 success */
const emit = defineEmits(['success'])

const columns = [
  { title: '#', key: 'seq', width: 40 },
  { title: '摘要', key: 'summary', width: 260 },
  { title: '会计科目', key: 'accountId', width: 280 },
  { title: '部门', key: 'deptId', width: 170 },
  { title: '辅助核算', key: 'aux', width: 300 },
  { title: '借方金额', key: 'debit', width: 150 },
  { title: '贷方金额', key: 'credit', width: 150 },
  { title: '', key: 'action', width: 50 }
]

const AUX_TYPES = [
  { label: '项目', value: 'PROJECT' },
  { label: '客户', value: 'CUSTOMER' },
  { label: '供应商', value: 'SUPPLIER' }
]
const emptyRow = () => ({
  key: rowKey(),
  summary: '',
  accountId: undefined,
  deptId: undefined,
  auxType: undefined,
  projectId: undefined,
  partnerId: undefined,
  debit: undefined,
  credit: undefined
})
function clearAux(record) {
  record.projectId = undefined
  record.partnerId = undefined
  record.partnerName = undefined
}

const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, voucherDate: today(), attachmentCount: 0, entries: [emptyRow(), emptyRow()] }),
  fetch: async (record) => {
    const v = await getVoucher(record.id)
    return {
      ...v,
      entries: v.entries.map((e) => ({
        key: rowKey(),
        summary: e.summary,
        accountId: e.accountId,
        deptId: e.deptId,
        auxType: e.projectId ? 'PROJECT' : e.partnerType || undefined,
        projectId: e.projectId || undefined,
        partnerId: e.partnerId || undefined,
        partnerName: e.partnerName,
        debit: Number(e.debit) || undefined,
        credit: Number(e.credit) || undefined
      }))
    }
  },
  create: async (data) => emit('success', await addVoucher(data)),
  update: async (id, data) => {
    await updateVoucher(id, data)
    emit('success', id)
  },
  toPayload: (f) => {
    if (diff.value !== 0) {
      message.error('借贷不平衡')
      throw new Error('借贷不平衡')
    }
    return {
      voucherDate: f.voucherDate,
      attachmentCount: f.attachmentCount ?? 0,
      entries: f.entries.map((e) => ({
        summary: e.summary,
        accountId: e.accountId,
        deptId: e.deptId || null,
        projectId: e.auxType === 'PROJECT' ? e.projectId || null : null,
        partnerType: e.auxType && e.auxType !== 'PROJECT' && e.partnerId ? e.auxType : null,
        partnerId: e.auxType && e.auxType !== 'PROJECT' ? e.partnerId || null : null,
        debit: e.debit || 0,
        credit: e.credit || 0
      }))
    }
  }
})
const { form, formRef } = drawer

const sum = (field) => Math.round(form.value.entries.reduce((s, e) => s + (Number(e[field]) || 0), 0) * 100) / 100
const totalDebit = computed(() => sum('debit'))
const totalCredit = computed(() => sum('credit'))
const diff = computed(() => Math.round((totalDebit.value - totalCredit.value) * 100) / 100)

function addRow() {
  form.value.entries.push(emptyRow())
}

function copySummary(index) {
  const row = form.value.entries[index]
  if (!row.summary && index > 0) row.summary = form.value.entries[index - 1].summary
}

/** 双击补平：把差额填到该行的借方或贷方 */
function fill(row, side) {
  const other = side === 'debit' ? 'credit' : 'debit'
  const current = Number(row[side]) || 0
  const rest = side === 'debit' ? -diff.value + current : diff.value + current
  if (rest > 0) {
    row[side] = rest
    row[other] = undefined
  }
}

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
.balance {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  padding-top: 30px;
}
.entries :deep(.ant-table-cell) {
  vertical-align: top;
}
.cell {
  margin-bottom: 0;
}
.amount :deep(input) {
  text-align: right;
}
.entries-foot {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.aux-cell {
  display: flex;
  gap: 4px;
}
.aux-type {
  width: 90px;
  flex: none;
}
.aux-value {
  flex: 1;
  min-width: 0;
}
</style>
