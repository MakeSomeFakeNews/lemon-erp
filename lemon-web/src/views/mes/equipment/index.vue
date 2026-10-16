<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="设备" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="编码或名称" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.mes_equipment_status)" />
        </a-form-item>
        <a-form-item label="工作中心" name="workCenterId">
          <WorkCenterSelect v-model:value="query.workCenterId" placeholder="全部" />
        </a-form-item>
        <a-form-item label="保养提醒" name="dueDays">
          <a-select v-model:value="query.dueDays" placeholder="全部" allow-clear :options="DUE_OPTIONS" />
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
        <a-button v-perm="'mes:equipment:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>新增设备
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1300, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div>{{ record.name }}</div>
            <div class="sub-text">{{ record.code }} {{ record.model }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="mes_equipment_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'maintain'">
            <template v-if="record.maintainCycleDays">
              <div>
                下次 {{ record.nextMaintainDate }}
                <a-tag v-if="due(record)" :color="due(record).color" :bordered="false">{{ due(record).text }}</a-tag>
              </div>
              <div class="sub-text">每 {{ record.maintainCycleDays }} 天 · 上次 {{ record.lastMaintainDate || '-' }}</div>
            </template>
            <span v-else class="sub-text">不做定期保养</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-perm="'mes:maintain:add'" type="link" size="small" @click="toMaintenance(record)">
                <template #icon><ToolOutlined /></template>维护
              </a-button>
              <a-button v-perm="'mes:equipment:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <span v-perm="'mes:equipment:remove'">
                <a-popconfirm :title="`确定删除设备「${record.name}」？`" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增设备" edit-title="编辑设备">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="设备编码" name="code">
              <a-input v-model:value="form.code" :maxlength="32" placeholder="如：EQ-SMT-01" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="设备名称" name="name">
              <a-input v-model:value="form.name" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="规格型号" name="model">
              <a-input v-model:value="form.model" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="工作中心" name="workCenterId">
              <WorkCenterSelect v-model:value="form.workCenterId" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="安装位置" name="location">
              <a-input v-model:value="form.location" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="购置日期" name="purchaseDate">
              <a-date-picker v-model:value="form.purchaseDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="状态" name="status">
              <a-select v-model:value="form.status" :options="toOptions(dict.mes_equipment_status)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="保养周期" name="maintainCycleDays" extra="不填不做定期保养">
              <a-input-number v-model:value="form.maintainCycleDays" :min="1" :max="3650" addon-after="天" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="上次保养日期" name="lastMaintainDate" extra="下次保养日期 = 上次保养（或购置）日期 + 周期">
              <a-date-picker v-model:value="form.lastMaintainDate" value-format="YYYY-MM-DD" class="full" />
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
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, PlusOutlined, ToolOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import WorkCenterSelect from '@/components/mes/WorkCenterSelect.vue'
import { addEquipment, deleteEquipment, getEquipment, pageEquipment, updateEquipment } from '@/api/mes/base'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

const router = useRouter()
const dict = useDict('mes_equipment_status')
const DUE_OPTIONS = [
  { value: 0, label: '已到期' },
  { value: 7, label: '7 天内到期' },
  { value: 30, label: '30 天内到期' }
]

const columns = [
  { title: '设备', key: 'name', width: 240 },
  { title: '工作中心', dataIndex: 'workCenterName', width: 130 },
  { title: '位置', dataIndex: 'location', width: 130 },
  { title: '状态', key: 'status', width: 90, dict: 'mes_equipment_status' },
  { title: '定期保养', key: 'maintain', width: 260 },
  { title: '购置日期', dataIndex: 'purchaseDate', width: 110 },
  { title: '备注', dataIndex: 'remark', width: 150, ellipsis: true },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'mes-equipment')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, status: undefined, workCenterId: undefined, dueDays: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageEquipment, query)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

onMounted(load)

/** 保养到期提示：已逾期 / 7 天内 */
function due(record) {
  if (!record.nextMaintainDate || record.status === 'SCRAPPED') return null
  const days = dayjs(record.nextMaintainDate).diff(dayjs().startOf('day'), 'day')
  if (days < 0) return { color: 'red', text: `逾期 ${-days} 天` }
  if (days <= 7) return { color: 'orange', text: days === 0 ? '今天到期' : `${days} 天后到期` }
  return null
}

const rules = {
  code: [
    { required: true, message: '请输入设备编码' },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: '只能包含字母、数字、下划线和短横线' }
  ],
  name: [{ required: true, whitespace: true, message: '请输入设备名称' }]
}
const FIELDS = ['code', 'name', 'model', 'workCenterId', 'location', 'purchaseDate', 'status', 'maintainCycleDays', 'lastMaintainDate', 'remark']
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, ...Object.fromEntries(FIELDS.map((f) => [f, undefined])), status: 'RUNNING' }),
  fetch: (record) => getEquipment(record.id),
  create: addEquipment,
  update: updateEquipment,
  toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: load
})
const { formRef, form } = drawer

const toMaintenance = (record) => router.push({ path: '/mes/maintenance', query: { equipmentId: record.id } })

async function remove(id) {
  await deleteEquipment(id)
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
</style>
