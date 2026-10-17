<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('mes.maintenance.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.type')" name="type">
          <a-select v-model:value="query.type" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.mes_maintain_type)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.mes_maintain_status)" />
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
        <a-button v-perm="'mes:maintain:add'" type="primary" @click="openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('mes.maintenance.add') }}
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1400, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'equipment'">
            <div>{{ record.equipmentName }}</div>
            <div class="sub-text">{{ record.equipmentCode }}</div>
          </template>
          <template v-else-if="column.key === 'type'"><DictTag type="mes_maintain_type" :value="record.type" /></template>
          <template v-else-if="column.key === 'status'"><DictTag type="mes_maintain_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'time'">
            <div>{{ $t('mes.maintenance.started', { t: record.startTime || '-' }) }}</div>
            <div class="sub-text">{{ $t('mes.maintenance.finished', { t: record.finishTime || '-' }) }}</div>
          </template>
          <template v-else-if="column.key === 'result'">
            <div>{{ record.result || '-' }}</div>
            <div class="sub-text">
              <span v-if="record.downtime != null">{{ $t('mes.maintenance.downtime', { n: record.downtime }) }}</span>
              <span v-if="record.cost != null">{{ $t('mes.maintenance.cost', { v: formatMoney(record.cost) }) }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-if="record.status === MaintenanceStatus.PENDING" v-perm="'mes:maintain:handle'" type="link" size="small" @click="start(record)">{{ $t('mes.maintenance.start') }}</a-button>
              <a-button v-if="record.status === MaintenanceStatus.IN_PROGRESS" v-perm="'mes:maintain:handle'" type="link" size="small" @click="openFinish(record)">
                {{ $t('mes.maintenance.finish') }}
              </a-button>
              <span v-if="record.status === MaintenanceStatus.PENDING" v-perm="'mes:maintain:remove'">
                <a-popconfirm :title="$t('mes.maintenance.confirmDelete')" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small">{{ $t('common.action.delete') }}</a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="createOpen" :title="$t('mes.maintenance.add')" :confirm-loading="saving" @ok="submitCreate">
      <a-form ref="createRef" :model="createForm" :rules="createRules" layout="vertical">
        <a-form-item :label="$t('mes.oee.equipment')" name="equipmentId">
          <a-select v-model:value="createForm.equipmentId" show-search option-filter-prop="label" :options="equipmentOptionList" />
        </a-form-item>
        <a-form-item :label="$t('common.field.type')" name="type">
          <a-radio-group v-model:value="createForm.type" :options="toOptions(dict.mes_maintain_type)" />
        </a-form-item>
        <a-form-item :label="createForm.type === MaintenanceType.REPAIR ? $t('mes.maintenance.symptom') : $t('mes.maintenance.maintainContent')" name="content">
          <a-textarea v-model:value="createForm.content" :rows="3" :maxlength="255" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('mes.wo.planDate')" name="planDate">
              <a-date-picker v-model:value="createForm.planDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('mes.maintenance.handler')" name="handler">
              <a-input v-model:value="createForm.handler" :maxlength="64" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="finishOpen" :title="$t('mes.maintenance.finishTitle')" :confirm-loading="saving" @ok="submitFinish">
      <a-form layout="vertical">
        <a-form-item :label="$t('mes.maintenance.result')" required>
          <a-textarea v-model:value="finishForm.result" :rows="3" :maxlength="255" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('mes.maintenance.costLabel')">
              <a-input-number v-model:value="finishForm.cost" :min="0" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('mes.maintenance.handler')">
              <a-input v-model:value="finishForm.handler" :maxlength="64" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { addMaintenance, deleteMaintenance, equipmentOptions, finishMaintenance, pageMaintenance, startMaintenance } from '@/api/mes/base'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { formatMoney } from '@/components/scm/scm'
import { today } from '@/components/wms/wms'
import { useAction } from '@/composables/useAction'

import { MaintenanceStatus, MaintenanceType } from '@/constants/enums'
const route = useRoute()
const dict = useDict('mes_maintain_type', 'mes_maintain_status')
const { t } = useI18n()

const columns = [
  { title: t('mes.maintenance.orderNo'), dataIndex: 'orderNo', width: 170 },
  { title: t('mes.oee.equipment'), key: 'equipment', width: 180 },
  { title: t('common.field.type'), key: 'type', width: 110, dict: 'mes_maintain_type' },
  { title: t('mes.maintenance.content'), dataIndex: 'content', width: 220, ellipsis: true },
  { title: t('mes.wo.planDate'), dataIndex: 'planDate', width: 120 },
  { title: t('common.field.status'), key: 'status', width: 100, dict: 'mes_maintain_status' },
  { title: t('mes.maintenance.handler'), dataIndex: 'handler', width: 100 },
  { title: t('mes.maintenance.handleTime'), key: 'time', width: 200 },
  { title: t('mes.maintenance.result'), key: 'result', width: 220 },
  { title: t('common.action.operation'), key: 'action', width: 140, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'mes-maintenance')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, type: undefined, status: undefined, equipmentId: Number(route.query.equipmentId) || undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageMaintenance, query)

function resetQuery() {
  Object.assign(query, emptyQuery(), { equipmentId: undefined })
  search()
}

const equipments = ref([])
const equipmentOptionList = computed(() => equipments.value.map((e) => ({ value: e.id, label: `${e.code} ${e.name}` })))

onMounted(async () => {
  load()
  equipments.value = await equipmentOptions()
  if (query.equipmentId) openCreate(query.equipmentId)
})

// ---------------------------------------------------------------- 新建
const saving = ref(false)
const createOpen = ref(false)
const createRef = ref()
const createForm = ref({})
const createRules = {
  equipmentId: [{ required: true, message: t('mes.maintenance.equipmentRequired') }],
  type: [{ required: true, message: t('mes.doc.typeRequired') }],
  content: [{ required: true, whitespace: true, message: t('mes.maintenance.contentRequired') }]
}
function openCreate(equipmentId) {
  createForm.value = { equipmentId, type: MaintenanceType.REPAIR, content: '', planDate: today(), handler: '' }
  createOpen.value = true
}
async function submitCreate() {
  await createRef.value.validate()
  saving.value = true
  try {
    await addMaintenance(createForm.value)
    message.success(t('mes.maintenance.created'))
    createOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 处理
function start(record) {
  Modal.confirm({
    title: t('mes.maintenance.startConfirm', { no: record.orderNo }),
    content: record.type === MaintenanceType.REPAIR ? t('mes.maintenance.startRepairContent') : undefined,
    async onOk() {
      await startMaintenance(record.id, record.handler)
      message.success(t('mes.maintenance.startedMsg'))
      load()
    }
  })
}

const finishOpen = ref(false)
const finishForm = reactive({ id: undefined, result: '', cost: undefined, handler: '' })
function openFinish(record) {
  Object.assign(finishForm, { id: record.id, result: '', cost: undefined, handler: record.handler || '' })
  finishOpen.value = true
}
async function submitFinish() {
  if (!finishForm.result.trim()) return message.warning(t('mes.maintenance.resultRequired'))
  saving.value = true
  try {
    const { id, ...data } = finishForm
    await finishMaintenance(id, data)
    message.success(t('mes.maintenance.finishedMsg'))
    finishOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

const remove = useAction(deleteMaintenance, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
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
