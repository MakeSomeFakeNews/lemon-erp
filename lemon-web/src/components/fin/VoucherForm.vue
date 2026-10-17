<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('fin.voucherForm.createTitle')" :edit-title="$t('fin.voucherForm.editTitle')" width="min(1200px, 100vw)">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('fin.voucherForm.voucherDate')" name="voucherDate" :rules="[{ required: true, message: $t('fin.voucherForm.dateRequired') }]">
            <a-date-picker v-model:value="form.voucherDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="4">
          <a-form-item :label="$t('fin.voucherForm.attachmentCount')" name="attachmentCount">
            <a-input-number v-model:value="form.attachmentCount" :min="0" :max="999" :addon-after="$t('fin.voucherForm.sheets')" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="14" class="balance">
          <span>{{ $t('fin.voucherForm.totalDebit') }} <b>{{ formatMoney(totalDebit) }}</b></span>
          <span>{{ $t('fin.voucherForm.totalCredit') }} <b>{{ formatMoney(totalCredit) }}</b></span>
          <a-tag v-if="diff === 0 && totalDebit > 0" color="green" :bordered="false">{{ $t('fin.voucherForm.balanced') }}</a-tag>
          <a-tag v-else-if="diff !== 0" color="red" :bordered="false">{{ $t('fin.voucherForm.diff', { amount: formatMoney(Math.abs(diff)) }) }}</a-tag>
        </a-col>
      </a-row>

      <a-table row-key="key" size="small" class="entries" :columns="columns" :data-source="form.entries" :pagination="false" :scroll="{ x: 1340 }">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'seq'">{{ index + 1 }}</template>
          <template v-else-if="column.key === 'summary'">
            <a-form-item :name="['entries', index, 'summary']" :rules="[{ required: true, whitespace: true, message: $t('fin.voucherForm.summaryRequired') }]" class="cell">
              <a-input v-model:value="record.summary" :maxlength="255" :placeholder="$t('fin.voucherForm.summary')" @focus="copySummary(index)" />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'accountId'">
            <a-form-item :name="['entries', index, 'accountId']" :rules="[{ required: true, message: $t('fin.voucherForm.accountRequired') }]" class="cell">
              <AccountSelect :value="record.accountId" @update:value="(v) => changeAccount(record, v)" />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'aux'">
            <div class="aux-list">
              <a-form-item
                v-for="a in visibleAux(record)"
                :key="a.value"
                :name="['entries', index, a.key]"
                :rules="[{ required: isRequired(record, a.value), message: $t('common.placeholder.selectName', { name: a.label }) }]"
                class="cell aux-item"
              >
                <div class="aux-row">
                  <span :class="['aux-label', { required: isRequired(record, a.value) }]">{{ a.label }}</span>
                  <DeptSelect v-if="a.value === AuxItem.DEPT" v-model:value="record.deptId" :placeholder="isRequired(record, 'DEPT') ? $t('fin.voucherForm.required') : $t('fin.voucherForm.optional')" class="aux-input" />
                  <PartnerSelect
                    v-else-if="a.value === AuxItem.CUSTOMER || a.value === AuxItem.SUPPLIER"
                    v-model:value="record[a.key]"
                    :type="a.value === AuxItem.CUSTOMER ? 'customer' : 'supplier'"
                    :label="record[a.name]"
                    class="aux-input"
                  />
                  <ProjectSelect v-else-if="a.value === AuxItem.PROJECT" v-model:value="record.projectId" class="aux-input" />
                  <MaterialSelect v-else v-model:value="record.materialId" :label="record.materialName" :placeholder="$t('fin.voucherForm.materialPlaceholder')" class="aux-input" />
                  <a-button v-if="a.value !== AuxItem.DEPT && !isRequired(record, a.value)" type="text" size="small" :title="$t('fin.voucherForm.remove')" @click="removeAux(record, a)">
                    <CloseOutlined />
                  </a-button>
                </div>
              </a-form-item>
              <a-dropdown v-if="addableAux(record).length" :trigger="['click']">
                <a class="aux-add"><PlusOutlined /> {{ $t('fin.voucherForm.aux') }}</a>
                <template #overlay>
                  <a-menu @click="({ key }) => record.extraAux.push(key)">
                    <a-menu-item v-for="a in addableAux(record)" :key="a.value">{{ a.label }}</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
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
          <template #icon><PlusOutlined /></template>{{ $t('fin.voucherForm.addEntry') }}
        </a-button>
        <span class="sub-text">{{ $t('fin.voucherForm.tip') }}</span>
      </div>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { CloseOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import AccountSelect from './AccountSelect.vue'
import DeptSelect from './DeptSelect.vue'
import ProjectSelect from '@/components/pm/ProjectSelect.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { accountOptions, addVoucher, getVoucher, updateVoucher } from '@/api/fin'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { rowKey, today } from '@/components/wms/wms'
import { AUX_ITEMS, auxItemsOf, cached, formatMoney } from './fin'
import { t } from '@/locales'

import { AuxItem } from '@/constants/enums'
/** 新增 / 编辑凭证（未审核的手工凭证）。通过 ref 调用 openCreate() / openEdit(record)，保存后触发 success */
const emit = defineEmits(['success'])

const columns = [
  { title: '#', key: 'seq', width: 40 },
  { title: t('fin.voucherForm.summary'), key: 'summary', width: 260 },
  { title: t('fin.voucherForm.account'), key: 'accountId', width: 280 },
  { title: t('fin.voucherForm.aux'), key: 'aux', width: 400 },
  { title: t('fin.voucherForm.debit'), key: 'debit', width: 150 },
  { title: t('fin.voucherForm.credit'), key: 'credit', width: 150 },
  { title: '', key: 'action', width: 50 }
]

const emptyRow = () => ({
  key: rowKey(),
  summary: '',
  accountId: undefined,
  ...Object.fromEntries(AUX_ITEMS.map((a) => [a.key, undefined])),
  extraAux: [],
  debit: undefined,
  credit: undefined
})

// ---------------------------------------------------------------- 辅助核算

/** 末级科目：ID → 科目（含辅助核算设置） */
const accounts = ref(new Map())
onMounted(async () => {
  accounts.value = new Map((await cached('accounts-leaf', accountOptions)).map((a) => [a.id, a]))
})

const requiredAux = (record) => auxItemsOf(accounts.value.get(record.accountId))
const isRequired = (record, item) => requiredAux(record).includes(item)
const hasValue = (record, a) => record[a.key] !== undefined && record[a.key] !== null

/** 显示的辅助核算：部门（始终可填）、科目设置的、手工添加的、已有值的 */
const visibleAux = (record) =>
  AUX_ITEMS.filter((a) => a.value === AuxItem.DEPT || isRequired(record, a.value) || record.extraAux.includes(a.value) || hasValue(record, a))
const addableAux = (record) => {
  const shown = new Set(visibleAux(record).map((a) => a.value))
  return AUX_ITEMS.filter((a) => !shown.has(a.value))
}

function removeAux(record, a) {
  record[a.key] = undefined
  record[a.name] = undefined
  record.extraAux = record.extraAux.filter((v) => v !== a.value)
}

/** 换科目：清掉新科目不需要、也不是手工添加的辅助核算（部门保留） */
function changeAccount(record, accountId) {
  record.accountId = accountId
  const required = requiredAux(record)
  AUX_ITEMS.filter((a) => a.value !== AuxItem.DEPT && !required.includes(a.value) && !record.extraAux.includes(a.value)).forEach((a) => {
    record[a.key] = undefined
    record[a.name] = undefined
  })
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
        ...Object.fromEntries(AUX_ITEMS.flatMap((a) => [[a.key, e[a.key] || undefined], [a.name, e[a.name]]])),
        extraAux: [],
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
      message.error(t('fin.voucherForm.unbalanced'))
      throw new Error('unbalanced')
    }
    return {
      voucherDate: f.voucherDate,
      attachmentCount: f.attachmentCount ?? 0,
      entries: f.entries.map((e) => ({
        summary: e.summary,
        accountId: e.accountId,
        ...Object.fromEntries(AUX_ITEMS.map((a) => [a.key, e[a.key] || null])),
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
.aux-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.aux-row {
  display: flex;
  align-items: center;
  gap: 4px;
}
.aux-label {
  width: 72px;
  flex: none;
  font-size: 12px;
  color: var(--app-text-2);
}
.aux-label.required::before {
  content: '*';
  margin-right: 2px;
  color: #ff4d4f;
}
.aux-input {
  flex: 1;
  min-width: 0;
}
.aux-add {
  font-size: 12px;
  align-self: flex-start;
}
</style>
