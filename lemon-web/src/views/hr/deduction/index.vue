<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('hr.deduction.employee')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('hr.deduction.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('hr.deduction.type')" name="type">
          <a-select v-model:value="query.type" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.hr_deduction_type)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :exporter="exportAll"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="'hr:deduction:edit'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('hr.deduction.add') }}
        </a-button>
        <span v-perm="'hr:deduction:edit'">
          <a-popconfirm :title="$t('hr.deduction.confirmDeleteN', { n: selectedKeys.length })" :disabled="!selectedKeys.length" @confirm="remove(selectedKeys)">
            <a-button danger :disabled="!selectedKeys.length">
              <template #icon><DeleteOutlined /></template>{{ $t('common.action.batchDelete') }}
            </a-button>
          </a-popconfirm>
        </span>
        <a-alert
          type="info"
          show-icon
          :message="$t('hr.deduction.tip')"
        />
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        :scroll="{ x: 1100, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'employee'">
            <div>{{ record.employeeName }}</div>
            <div class="sub-text">{{ record.empNo }} · {{ record.deptName || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'type'">
            <DictTag type="hr_deduction_type" :value="record.type" />
          </template>
          <template v-else-if="column.key === 'monthlyAmount'">¥{{ formatMoney(record.monthlyAmount) }}</template>
          <template v-else-if="column.key === 'period'">
            {{ periodText(record.startPeriod) }} ~ {{ record.endPeriod ? periodText(record.endPeriod) : $t('hr.deduction.longTerm') }}
            <a-tag v-if="record.endPeriod && record.endPeriod < thisPeriod" :bordered="false" class="tag-gap">{{ $t('hr.deduction.ended') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <span v-perm="'hr:deduction:edit'" class="table-actions">
              <a-button type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <a-popconfirm :title="$t('hr.deduction.confirmDelete')" @confirm="remove([record.id])">
                <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('hr.deduction.createTitle')" :edit-title="$t('hr.deduction.editTitle')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('hr.deduction.employee')" name="employeeId">
          <EmployeeSelect v-model:value="form.employeeId" :label="form.employeeLabel" :disabled="isEdit" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.deduction.type')" name="type">
              <a-select v-model:value="form.type" :options="toOptions(dict.hr_deduction_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.deduction.monthlyAmountLabel')" name="monthlyAmount">
              <a-input-number v-model:value="form.monthlyAmount" :min="0" :precision="2" :addon-after="$t('hr.deduction.perMonth')" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-alert v-if="standard" type="info" show-icon class="mb" :message="$t('hr.deduction.standard', { hint: standard.hint })" />
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.deduction.startPeriod')" name="startPeriod">
              <a-date-picker
                picker="month"
                :value="form.startPeriod ? String(form.startPeriod) : undefined"
                value-format="YYYYMM"
                format="YYYY-MM"
                class="full"
                @change="(v) => (form.startPeriod = v ? Number(v) : undefined)"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.deduction.endPeriod')" name="endPeriod" :extra="$t('hr.deduction.endPeriodExtra')">
              <a-date-picker
                picker="month"
                :value="form.endPeriod ? String(form.endPeriod) : undefined"
                value-format="YYYYMM"
                format="YYYY-MM"
                class="full"
                @change="(v) => (form.endPeriod = v ? Number(v) : undefined)"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('common.field.remark')" name="remark">
          <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" :placeholder="$t('hr.deduction.remarkPh')" />
        </a-form-item>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import EmployeeSelect from '@/components/hr/EmployeeSelect.vue'
import { DEDUCTION_STANDARD, periodText } from '@/components/hr/hr'
import { formatMoney } from '@/components/scm/scm'
import { addTaxDeduction, deleteTaxDeduction, pageTaxDeduction, updateTaxDeduction } from '@/api/hr'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

const { t } = useI18n()
const dict = useDict('hr_deduction_type')
const thisPeriod = Number(dayjs().format('YYYYMM'))

const columns = [
  { title: t('hr.deduction.employee'), key: 'employee', width: 200, exportValue: (r) => `${r.empNo} ${r.employeeName}` },
  { title: t('hr.deduction.type'), key: 'type', dataIndex: 'type', width: 140, dict: 'hr_deduction_type' },
  { title: t('hr.deduction.monthlyAmount'), key: 'monthlyAmount', dataIndex: 'monthlyAmount', width: 120, align: 'right' },
  {
    title: t('hr.deduction.period'),
    key: 'period',
    width: 260,
    exportValue: (r) => `${periodText(r.startPeriod)} ~ ${r.endPeriod ? periodText(r.endPeriod) : t('hr.deduction.longTerm')}`
  },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 130, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'hr-deduction')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, type: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(pageTaxDeduction, query)
onMounted(load)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const FIELDS = ['employeeId', 'type', 'monthlyAmount', 'startPeriod', 'endPeriod', 'remark']
const rules = {
  employeeId: [{ required: true, message: t('hr.deduction.rule.employee') }],
  type: [{ required: true, message: t('hr.deduction.rule.type') }],
  monthlyAmount: [{ required: true, message: t('hr.deduction.rule.monthlyAmount') }],
  startPeriod: [{ required: true, message: t('hr.deduction.rule.startPeriod') }],
  endPeriod: [
    {
      validator: (_, v) =>
        v && drawer.form.value.startPeriod && v < drawer.form.value.startPeriod ? Promise.reject(new Error(t('hr.deduction.rule.endBeforeStart'))) : Promise.resolve()
    }
  ]
}
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    employeeId: undefined,
    employeeLabel: undefined,
    type: undefined,
    monthlyAmount: undefined,
    startPeriod: Number(dayjs().startOf('year').format('YYYYMM')),
    endPeriod: undefined,
    remark: undefined
  }),
  fetch: (record) => ({ ...record, employeeLabel: `${record.empNo} ${record.employeeName}` }),
  create: addTaxDeduction,
  update: updateTaxDeduction,
  toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: load
})
const { formRef, form, isEdit } = drawer

const standard = computed(() => DEDUCTION_STANDARD[form.value.type])
/** 选择类型时带出标准扣除额（金额为空或仍是上一个类型的标准额时） */
watch(
  () => form.value.type,
  (type, prev) => {
    const amount = form.value.monthlyAmount
    if (type && (amount == null || amount === DEDUCTION_STANDARD[prev]?.amount)) form.value.monthlyAmount = DEDUCTION_STANDARD[type]?.amount
  }
)

async function remove(ids) {
  await deleteTaxDeduction(ids)
  message.success(t('common.msg.deleteSuccess'))
  selectedKeys.value = selectedKeys.value.filter((k) => !ids.includes(k))
  load()
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
.mb {
  margin-bottom: 16px;
}
.tag-gap {
  margin-left: 4px;
}
</style>
