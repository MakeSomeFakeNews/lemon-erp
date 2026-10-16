<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="关键字" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="维护单号、设备编码或名称" allow-clear />
        </a-form-item>
        <a-form-item label="类型" name="type">
          <a-select v-model:value="query.type" placeholder="全部" allow-clear :options="toOptions(dict.mes_maintain_type)" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.mes_maintain_status)" />
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
          <template #icon><PlusOutlined /></template>报修 / 保养
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
            <div>开始 {{ record.startTime || '-' }}</div>
            <div class="sub-text">完成 {{ record.finishTime || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'result'">
            <div>{{ record.result || '-' }}</div>
            <div class="sub-text">
              <span v-if="record.downtime != null">停机 {{ record.downtime }} 小时</span>
              <span v-if="record.cost != null"> · 费用 ¥{{ formatMoney(record.cost) }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-if="record.status === 'PENDING'" v-perm="'mes:maintain:handle'" type="link" size="small" @click="start(record)">开始</a-button>
              <a-button v-if="record.status === 'IN_PROGRESS'" v-perm="'mes:maintain:handle'" type="link" size="small" @click="openFinish(record)">
                完成
              </a-button>
              <span v-if="record.status === 'PENDING'" v-perm="'mes:maintain:remove'">
                <a-popconfirm title="确定删除该维护单？" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small">删除</a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="createOpen" title="报修 / 保养" :confirm-loading="saving" @ok="submitCreate">
      <a-form ref="createRef" :model="createForm" :rules="createRules" layout="vertical">
        <a-form-item label="设备" name="equipmentId">
          <a-select v-model:value="createForm.equipmentId" show-search option-filter-prop="label" :options="equipmentOptionList" />
        </a-form-item>
        <a-form-item label="类型" name="type">
          <a-radio-group v-model:value="createForm.type" :options="toOptions(dict.mes_maintain_type)" />
        </a-form-item>
        <a-form-item :label="createForm.type === 'REPAIR' ? '故障现象' : '保养内容'" name="content">
          <a-textarea v-model:value="createForm.content" :rows="3" :maxlength="255" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="计划日期" name="planDate">
              <a-date-picker v-model:value="createForm.planDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="处理人" name="handler">
              <a-input v-model:value="createForm.handler" :maxlength="64" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="finishOpen" title="完成维护" :confirm-loading="saving" @ok="submitFinish">
      <a-form layout="vertical">
        <a-form-item label="处理结果" required>
          <a-textarea v-model:value="finishForm.result" :rows="3" :maxlength="255" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="费用">
              <a-input-number v-model:value="finishForm.cost" :min="0" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="处理人">
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

const route = useRoute()
const dict = useDict('mes_maintain_type', 'mes_maintain_status')

const columns = [
  { title: '维护单号', dataIndex: 'orderNo', width: 170 },
  { title: '设备', key: 'equipment', width: 180 },
  { title: '类型', key: 'type', width: 100, dict: 'mes_maintain_type' },
  { title: '内容', dataIndex: 'content', width: 220, ellipsis: true },
  { title: '计划日期', dataIndex: 'planDate', width: 110 },
  { title: '状态', key: 'status', width: 90, dict: 'mes_maintain_status' },
  { title: '处理人', dataIndex: 'handler', width: 100 },
  { title: '处理时间', key: 'time', width: 200 },
  { title: '处理结果', key: 'result', width: 220 },
  { title: '操作', key: 'action', width: 130, fixed: 'right' }
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
  equipmentId: [{ required: true, message: '请选择设备' }],
  type: [{ required: true, message: '请选择类型' }],
  content: [{ required: true, whitespace: true, message: '请填写内容' }]
}
function openCreate(equipmentId) {
  createForm.value = { equipmentId, type: 'REPAIR', content: '', planDate: today(), handler: '' }
  createOpen.value = true
}
async function submitCreate() {
  await createRef.value.validate()
  saving.value = true
  try {
    await addMaintenance(createForm.value)
    message.success('已创建维护单')
    createOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 处理
function start(record) {
  Modal.confirm({
    title: `开始处理 ${record.orderNo}？`,
    content: record.type === 'REPAIR' ? '故障维修开始后，设备状态变为「维修中」。' : undefined,
    async onOk() {
      await startMaintenance(record.id, record.handler)
      message.success('已开始')
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
  if (!finishForm.result.trim()) return message.warning('请填写处理结果')
  saving.value = true
  try {
    const { id, ...data } = finishForm
    await finishMaintenance(id, data)
    message.success('已完成')
    finishOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(id) {
  await deleteMaintenance(id)
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
