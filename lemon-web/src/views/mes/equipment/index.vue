<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('mes.oee.equipment')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('mes.workCenter.codeOrName')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.mes_equipment_status)" />
        </a-form-item>
        <a-form-item :label="$t('mes.wo.workCenter')" name="workCenterId">
          <WorkCenterSelect v-model:value="query.workCenterId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('mes.equipment.dueRemind')" name="dueDays">
          <a-select v-model:value="query.dueDays" :placeholder="$t('common.placeholder.all')" allow-clear :options="DUE_OPTIONS" />
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
          <template #icon><PlusOutlined /></template>{{ $t('mes.equipment.add') }}
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
                {{ $t('mes.equipment.next', { d: record.nextMaintainDate }) }}
                <a-tag v-if="due(record)" :color="due(record).color" :bordered="false">{{ due(record).text }}</a-tag>
              </div>
              <div class="sub-text">{{ $t('mes.equipment.cycle', { n: record.maintainCycleDays, last: record.lastMaintainDate || '-' }) }}</div>
            </template>
            <span v-else class="sub-text">{{ $t('mes.equipment.noMaintain') }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-perm="'mes:maintain:add'" type="link" size="small" @click="toMaintenance(record)">
                <template #icon><ToolOutlined /></template>{{ $t('mes.equipment.maintain') }}
              </a-button>
              <a-button v-perm="'mes:equipment:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <span v-perm="'mes:equipment:remove'">
                <a-popconfirm :title="$t('mes.equipment.confirmDelete', { name: record.name })" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('mes.equipment.add')" :edit-title="$t('mes.equipment.edit')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.equipment.code')" name="code">
              <a-input v-model:value="form.code" :maxlength="32" :placeholder="$t('mes.equipment.codePh')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.equipment.name')" name="name">
              <a-input v-model:value="form.name" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.equipment.model')" name="model">
              <a-input v-model:value="form.model" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.wo.workCenter')" name="workCenterId">
              <WorkCenterSelect v-model:value="form.workCenterId" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.equipment.location')" name="location">
              <a-input v-model:value="form.location" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.equipment.purchaseDate')" name="purchaseDate">
              <a-date-picker v-model:value="form.purchaseDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.status')" name="status">
              <a-select v-model:value="form.status" :options="toOptions(dict.mes_equipment_status)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.equipment.maintainCycle')" name="maintainCycleDays" :extra="$t('mes.equipment.maintainCycleExtra')">
              <a-input-number v-model:value="form.maintainCycleDays" :min="1" :max="3650" :addon-after="$t('mes.bom.dayUnit')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.equipment.lastMaintain')" name="lastMaintainDate" :extra="$t('mes.equipment.lastMaintainExtra')">
              <a-date-picker v-model:value="form.lastMaintainDate" value-format="YYYY-MM-DD" class="full" />
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
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { DeleteOutlined, EditOutlined, PlusOutlined, ToolOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
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
import { useAction } from '@/composables/useAction'

import { EquipmentStatus } from '@/constants/enums'
const router = useRouter()
const { t } = useI18n()
const dict = useDict('mes_equipment_status')
const DUE_OPTIONS = [
  { value: 0, label: t('mes.equipment.due0') },
  { value: 7, label: t('mes.equipment.dueIn', { n: 7 }) },
  { value: 30, label: t('mes.equipment.dueIn', { n: 30 }) }
]

const columns = [
  { title: t('mes.oee.equipment'), key: 'name', width: 240 },
  { title: t('mes.wo.workCenter'), dataIndex: 'workCenterName', width: 130 },
  { title: t('mes.equipment.locationShort'), dataIndex: 'location', width: 130 },
  { title: t('common.field.status'), key: 'status', width: 100, dict: 'mes_equipment_status' },
  { title: t('mes.equipment.periodic'), key: 'maintain', width: 260 },
  { title: t('mes.equipment.purchaseDate'), dataIndex: 'purchaseDate', width: 120 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 150, ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 220, fixed: 'right' }
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
  if (!record.nextMaintainDate || record.status === EquipmentStatus.SCRAPPED) return null
  const days = dayjs(record.nextMaintainDate).diff(dayjs().startOf('day'), 'day')
  if (days < 0) return { color: 'red', text: t('mes.equipment.overdue', { n: -days }) }
  if (days <= 7) return { color: 'orange', text: days === 0 ? t('mes.equipment.dueToday') : t('mes.equipment.dueAfter', { n: days }) }
  return null
}

const rules = {
  code: [
    { required: true, message: t('mes.equipment.codeRequired') },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: t('mes.workCenter.codeRule') }
  ],
  name: [{ required: true, whitespace: true, message: t('mes.equipment.nameRequired') }]
}
const FIELDS = ['code', 'name', 'model', 'workCenterId', 'location', 'purchaseDate', 'status', 'maintainCycleDays', 'lastMaintainDate', 'remark']
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, ...Object.fromEntries(FIELDS.map((f) => [f, undefined])), status: EquipmentStatus.RUNNING }),
  fetch: (record) => getEquipment(record.id),
  create: addEquipment,
  update: updateEquipment,
  toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: load
})
const { formRef, form } = drawer

const toMaintenance = (record) => router.push({ path: '/mes/maintenance', query: { equipmentId: record.id } })

const remove = useAction(deleteEquipment, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
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
