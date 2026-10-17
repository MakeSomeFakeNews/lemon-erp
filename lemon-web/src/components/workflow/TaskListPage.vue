<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('workflow.list.processName')" name="name">
          <a-input v-model:value="query.name" :placeholder="$t('workflow.list.processNamePh')" allow-clear />
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
      >
        <slot name="title" :total="pagination.total" />
      </TableToolbar>

      <a-table
        :row-key="rowKey"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'processName'">
            <a @click="openDetail(record)">{{ record.processName }}</a>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="wf_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">
                <template #icon><component :is="actionIcon" /></template>{{ actionText || $t('common.action.view') }}
              </a-button>
            </span>
          </template>
          <slot v-else name="cell" :column="column" :record="record" />
        </template>
      </a-table>
    </a-card>

    <InstanceDetailDrawer v-model:open="detailOpen" :instance-id="detailId" @changed="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { EyeOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import InstanceDetailDrawer from '@/components/workflow/InstanceDetailDrawer.vue'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'

/**
 * 待办 / 已办 / 抄送列表：按流程名称搜索，点击查看审批详情。
 * 内置 processName、status、action 列，其它需要自定义渲染的列通过 cell 插槽提供
 */
const props = defineProps({
  fetcher: { type: Function, required: true },
  columns: { type: Array, required: true },
  storageKey: { type: String, required: true },
  rowKey: { type: String, default: 'taskId' },
  scrollX: { type: Number, default: 900 },
  actionText: { type: String, default: '' },
  // 组件是函数，Object 类型的默认值要用工厂函数返回，否则会被当作工厂调用
  actionIcon: { type: [Object, Function], default: () => EyeOutlined }
})

useDict('wf_status')

const { checked, visibleColumns } = useColumns(props.columns, props.storageKey)
const showSearch = ref(true)

const query = reactive({ name: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(props.fetcher, query)

function resetQuery() {
  query.name = undefined
  search()
}

const detailOpen = ref(false)
const detailId = ref()
function openDetail(record) {
  detailId.value = record.processInstanceId
  detailOpen.value = true
}

onMounted(load)
</script>
