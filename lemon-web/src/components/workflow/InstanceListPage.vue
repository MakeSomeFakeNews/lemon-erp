<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('workflow.list.title')" name="name">
          <a-input v-model:value="query.name" :placeholder="$t('workflow.list.titlePh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.wf_status)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :columns="columns"
        @refresh="load"
      />

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1150, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="openDetail(record)">{{ record.name || record.processName }}</a>
          </template>
          <template v-else-if="column.key === 'category'">
            <DictTag type="wf_category" :value="record.category" />
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="wf_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'currentNodes'">
            <span v-if="record.status === FlowStatus.RUNNING && record.currentNodes">{{ record.currentNodes }}</span>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">
                <template #icon><EyeOutlined /></template>{{ $t('common.action.view') }}
              </a-button>
              <template v-if="manage && record.status === FlowStatus.RUNNING">
                <a-button v-perm="'wf:instance:cancel'" type="link" size="small" danger @click="openTerminate(record)">
                  <template #icon><StopOutlined /></template>{{ $t('workflow.list.terminate') }}
                </a-button>
              </template>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <InstanceDetailDrawer v-model:open="detailOpen" :instance-id="detailId" @changed="load" />

    <!-- 终止流程 -->
    <a-modal
      v-model:open="terminateOpen"
      :title="$t('workflow.list.terminateTitle', { name: terminateTarget?.name || '' })"
      :ok-text="$t('workflow.list.terminate')"
      :ok-button-props="{ danger: true }"
      :confirm-loading="terminating"
      destroy-on-close
      @ok="submitTerminate"
    >
      <a-form ref="terminateRef" :model="terminateForm" layout="vertical">
        <a-form-item :label="$t('workflow.list.terminateReason')" name="reason" :rules="[{ required: true, whitespace: true, message: $t('workflow.list.terminateReasonRequired') }]">
          <a-textarea v-model:value="terminateForm.reason" :rows="3" :maxlength="500" show-count />
        </a-form-item>
      </a-form>
      <a-alert type="warning" show-icon :message="$t('workflow.list.terminateTip')" />
    </a-modal>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { EyeOutlined, StopOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import InstanceDetailDrawer from './InstanceDetailDrawer.vue'
import { terminateInstance } from '@/api/workflow/instance'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

import { FlowStatus } from '@/constants/enums'
const { t } = useI18n()

/**
 * 流程实例列表：「我的申请」与「流程实例」共用
 * - fetcher：分页接口
 * - manage：管理视图，显示发起人列和终止操作
 */
const props = defineProps({
  fetcher: { type: Function, required: true },
  storageKey: { type: String, required: true },
  manage: Boolean
})
const dict = useDict('wf_status', 'wf_category')

const columns = [
  { title: t('workflow.list.title'), key: 'name', width: 220, ellipsis: true },
  { title: t('workflow.list.process'), dataIndex: 'processName', width: 140, ellipsis: true },
  { title: t('workflow.list.category'), key: 'category', width: 100 },
  ...(props.manage ? [{ title: t('workflow.list.initiator'), dataIndex: 'startUserName', width: 120 }] : []),
  { title: t('common.field.status'), key: 'status', width: 110 },
  { title: t('workflow.list.currentNode'), key: 'currentNodes', width: 140, ellipsis: true },
  { title: t('workflow.list.startTime'), dataIndex: 'startTime', width: 170 },
  { title: t('workflow.list.endTime'), dataIndex: 'endTime', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: props.manage ? 170 : 100, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, props.storageKey)
const showSearch = ref(true)

const query = reactive({ name: undefined, status: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable((p) => props.fetcher(p), query)

function resetQuery() {
  Object.assign(query, { name: undefined, status: undefined })
  search()
}

const detailOpen = ref(false)
const detailId = ref()
function openDetail(record) {
  detailId.value = record.id
  detailOpen.value = true
}

// 终止
const terminateOpen = ref(false)
const terminating = ref(false)
const terminateTarget = ref()
const terminateRef = ref()
const terminateForm = reactive({ reason: '' })

function openTerminate(record) {
  terminateTarget.value = record
  terminateForm.reason = ''
  terminateOpen.value = true
}

async function submitTerminate() {
  await terminateRef.value.validate()
  terminating.value = true
  try {
    await terminateInstance(terminateTarget.value.id, terminateForm.reason.trim())
    message.success(t('workflow.list.terminated'))
    terminateOpen.value = false
    load()
  } finally {
    terminating.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
}
</style>
