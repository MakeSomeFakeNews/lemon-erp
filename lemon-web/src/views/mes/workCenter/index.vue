<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="工作中心" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="编码或名称" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.sys_common_status, true)" />
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
          <template #icon><PlusOutlined /></template>新增工作中心
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
          <template v-else-if="column.key === 'laborRate'">¥{{ formatMoney(record.laborRate) }} / 工时</template>
          <template v-else-if="column.key === 'overheadRate'">¥{{ formatMoney(record.overheadRate) }} / 工时</template>
          <template v-else-if="column.key === 'capacity'">
            <div>{{ formatQty(dailyCapacity(record)) }} 工时 / 天</div>
            <div class="sub-text">{{ formatQty(record.dailyHours ?? 8) }} 工时 × {{ record.resources ?? 1 }} 个资源</div>
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
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <span v-perm="'mes:wc:remove'">
                <a-popconfirm :title="`确定删除工作中心「${record.name}」？`" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small">
                    <template #icon><DeleteOutlined /></template>删除
                  </a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增工作中心" edit-title="编辑工作中心">
      <a-alert type="info" show-icon class="tip" message="成本核算按报工工时 × 费率计算工单的直接人工和制造费用" />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="编码" name="code">
              <a-input v-model:value="form.code" :maxlength="32" placeholder="如：WC01" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="名称" name="name">
              <a-input v-model:value="form.name" :maxlength="64" placeholder="如：SMT 贴片线" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="人工费率" name="laborRate">
              <template v-if="referenceRate != null" #extra>
                参考：生产岗位平均人工成本 ¥{{ formatMoney(referenceRate) }} / 小时
                <a @click="form.laborRate = Number(referenceRate)">使用</a>
              </template>
              <a-input-number v-model:value="form.laborRate" :min="0" :precision="2" prefix="¥" addon-after="/ 工时" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="制造费用率" name="overheadRate">
              <a-input-number v-model:value="form.overheadRate" :min="0" :precision="2" prefix="¥" addon-after="/ 工时" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="每资源每日工时" name="dailyHours">
              <a-input-number v-model:value="form.dailyHours" :min="0" :max="24" :precision="2" addon-after="小时" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="资源数" name="resources" extra="设备台数或班组人数">
              <a-input-number v-model:value="form.resources" :min="1" :precision="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="工作日" name="workDays" :extra="`日产能 ${formatQty(dailyCapacity(form))} 工时；节假日、加班可在「产能排程 - 工厂日历」中单独设置`">
              <a-checkbox-group v-model:value="form.workDays" :options="WEEK_DAYS" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
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
const dict = useDict('sys_common_status')

const columns = [
  { title: '工作中心', key: 'name', width: 200 },
  { title: '人工费率', key: 'laborRate', width: 150 },
  { title: '制造费用率', key: 'overheadRate', width: 150 },
  { title: '日产能', key: 'capacity', width: 170, exportValue: (r) => dailyCapacity(r) },
  { title: '工作日', key: 'workDays', width: 150, exportValue: (r) => workDaysText(r.workDays) },
  { title: '状态', key: 'status', width: 90 },
  { title: '备注', dataIndex: 'remark', width: 180, ellipsis: true },
  { title: '操作', key: 'action', width: 160, fixed: 'right' }
]
const WEEK_DAYS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'].map((label, i) => ({ label, value: i + 1 }))
const parseWorkDays = (v) => (v || '').split(',').filter(Boolean).map(Number)
const workDaysText = (v) => {
  const days = parseWorkDays(v)
  return days.length ? days.map((d) => WEEK_DAYS[d - 1]?.label).join('、') : '-'
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
    { required: true, message: '请输入编码' },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: '只能包含字母、数字、下划线和短横线' }
  ],
  name: [{ required: true, whitespace: true, message: '请输入名称' }],
  laborRate: [{ required: true, message: '请填写人工费率' }],
  overheadRate: [{ required: true, message: '请填写制造费用率' }],
  dailyHours: [{ required: true, message: '请填写每日工时' }],
  resources: [{ required: true, message: '请填写资源数' }],
  workDays: [{ required: true, type: 'array', min: 1, message: '请选择工作日' }]
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
  message.success('删除成功')
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
