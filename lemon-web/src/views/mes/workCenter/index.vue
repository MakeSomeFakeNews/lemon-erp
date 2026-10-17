<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('mes.workCenter.title')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('mes.workCenter.codeOrName')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll"
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="'mes:wc:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('mes.workCenter.add') }}
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1220, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div>{{ record.name }}</div>
            <div class="sub-text">{{ record.code }}</div>
          </template>
          <template v-else-if="column.key === 'laborRate'">{{ $t('mes.workCenter.perHour', { v: formatMoney(record.laborRate) }) }}</template>
          <template v-else-if="column.key === 'overheadRate'">{{ $t('mes.workCenter.perHour', { v: formatMoney(record.overheadRate) }) }}</template>
          <template v-else-if="column.key === 'capacity'">
            <div>{{ $t('mes.workCenter.hoursPerDay', { v: formatQty(dailyCapacity(record)) }) }}</div>
            <div class="sub-text">{{ $t('mes.workCenter.hoursXResources', { h: formatQty(record.dailyHours ?? 8), n: record.resources ?? 1 }) }}</div>
          </template>
          <template v-else-if="column.key === 'workDays'">{{ workDaysText(record.workDays) }}</template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch
              :value="record.status"
              :name="record.name"
              :api="(s) => changeWorkCenterStatus(record.id, s)"
              :disabled="!userStore.hasPerm('mes:wc:edit')"
              @changed="(s) => (record.status = s)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-perm="'mes:wc:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <span v-perm="'mes:wc:remove'">
                <a-popconfirm :title="$t('mes.workCenter.confirmDelete', { name: record.name })" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small">
                    <template #icon><DeleteOutlined /></template>{{ $t('common.action.delete') }}
                  </a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('mes.workCenter.add')" :edit-title="$t('mes.workCenter.edit')">
      <a-alert type="info" show-icon class="tip" :message="$t('mes.workCenter.costTip')" />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.code')" name="code">
              <a-input v-model:value="form.code" :maxlength="32" :placeholder="$t('mes.workCenter.codePh')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.name')" name="name">
              <a-input v-model:value="form.name" :maxlength="64" :placeholder="$t('mes.workCenter.namePh')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.workCenter.laborRate')" name="laborRate">
              <template v-if="referenceRate != null" #extra>
                {{ $t('mes.workCenter.reference', { v: formatMoney(referenceRate) }) }}
                <a @click="form.laborRate = Number(referenceRate)">{{ $t('mes.workCenter.use') }}</a>
              </template>
              <a-input-number v-model:value="form.laborRate" :min="0" :precision="2" prefix="¥" :addon-after="$t('mes.workCenter.perHourUnit')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.workCenter.overheadRate')" name="overheadRate">
              <a-input-number v-model:value="form.overheadRate" :min="0" :precision="2" prefix="¥" :addon-after="$t('mes.workCenter.perHourUnit')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.workCenter.dailyHours')" name="dailyHours">
              <a-input-number v-model:value="form.dailyHours" :min="0" :max="24" :precision="2" :addon-after="$t('mes.workCenter.hours')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.workCenter.resources')" name="resources" :extra="$t('mes.workCenter.resourcesExtra')">
              <a-input-number v-model:value="form.resources" :min="1" :precision="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('mes.workCenter.workDays')" name="workDays" :extra="$t('mes.workCenter.workDaysExtra', { v: formatQty(dailyCapacity(form)) })">
              <a-checkbox-group v-model:value="form.workDays" :options="WEEK_DAYS" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import {
  addWorkCenter,
  changeWorkCenterStatus,
  deleteWorkCenter,
  getWorkCenter,
  laborRateReference,
  pageWorkCenter,
  updateWorkCenter
} from '@/api/mes/base'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { formatMoney } from '@/components/scm/scm'
import { formatQty } from '@/components/wms/wms'

const userStore = useUserStore()
const { t } = useI18n()
const dict = useDict('sys_common_status')

const columns = [
  { title: t('mes.workCenter.title'), key: 'name', width: 200 },
  { title: t('mes.workCenter.laborRate'), key: 'laborRate', width: 150 },
  { title: t('mes.workCenter.overheadRate'), key: 'overheadRate', width: 160 },
  { title: t('mes.workCenter.dailyCapacity'), key: 'capacity', width: 190, exportValue: (r) => dailyCapacity(r) },
  { title: t('mes.workCenter.workDays'), key: 'workDays', width: 150, exportValue: (r) => workDaysText(r.workDays) },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 180, ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 160, fixed: 'right' }
]
const WEEK_DAYS = t('mes.workCenter.weekDays').split(',').map((label, i) => ({ label, value: i + 1 }))
const parseWorkDays = (v) => (v || '').split(',').filter(Boolean).map(Number)
const workDaysText = (v) => {
  const days = parseWorkDays(v)
  return days.length ? days.map((d) => WEEK_DAYS[d - 1]?.label).join(t('mes.workCenter.sep')) : '-'
}
/** 日产能 = 每资源每日工时 × 资源数 */
const dailyCapacity = (r) => Math.round(Number(r.dailyHours ?? 8) * Number(r.resources ?? 1) * 100) / 100

const { checked, visibleColumns } = useColumns(columns, 'mes-work-center')
const showSearch = ref(true)

const query = reactive({ keyword: undefined, status: undefined })
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageWorkCenter, query)

function resetQuery() {
  Object.assign(query, { keyword: undefined, status: undefined })
  search()
}

onMounted(load)

const rules = {
  code: [
    { required: true, message: t('common.rule.required', { name: t('common.field.code') }) },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: t('mes.workCenter.codeRule') }
  ],
  name: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('common.field.name') }) }],
  laborRate: [{ required: true, message: t('mes.workCenter.laborRateRequired') }],
  overheadRate: [{ required: true, message: t('mes.workCenter.overheadRateRequired') }],
  dailyHours: [{ required: true, message: t('mes.workCenter.dailyHoursRequired') }],
  resources: [{ required: true, message: t('mes.workCenter.resourcesRequired') }],
  workDays: [{ required: true, type: 'array', min: 1, message: t('mes.workCenter.workDaysRequired') }]
}
const referenceRate = ref(null)
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    code: '',
    name: '',
    laborRate: 0,
    overheadRate: 0,
    dailyHours: 8,
    resources: 1,
    workDays: [1, 2, 3, 4, 5],
    remark: ''
  }),
  // 打开时取人力资源的人工成本参考（没有薪资档案时不显示）
  beforeOpen: async () => {
    referenceRate.value = await laborRateReference().catch(() => null)
  },
  fetch: async (record) => {
    const wc = await getWorkCenter(record.id)
    return {
      ...wc,
      dailyHours: wc.dailyHours != null ? Number(wc.dailyHours) : 8,
      resources: wc.resources ?? 1,
      workDays: wc.workDays ? parseWorkDays(wc.workDays) : [1, 2, 3, 4, 5]
    }
  },
  create: addWorkCenter,
  update: updateWorkCenter,
  toPayload: ({ code, name, laborRate, overheadRate, dailyHours, resources, workDays, remark }) => ({
    code,
    name,
    laborRate,
    overheadRate,
    dailyHours,
    resources,
    workDays: [...workDays].sort((a, b) => a - b).join(','),
    remark
  }),
  onSuccess: load
})
const { formRef, form } = drawer

async function remove(id) {
  await deleteWorkCenter(id)
  message.success(t('common.msg.deleteSuccess'))
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
.tip {
  margin-bottom: 16px;
}
</style>
