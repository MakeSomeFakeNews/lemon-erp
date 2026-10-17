<template>
  <a-modal
    :open="open"
    :title="title"
    width="min(760px, 100vw)"
    :ok-text="multiple && selectedKeys.length ? $t('components.user.confirmN', { n: selectedKeys.length }) : $t('common.action.confirm')"
    :ok-button-props="{ disabled: !selectedKeys.length }"
    :confirm-loading="submitting"
    destroy-on-close
    @ok="confirm"
    @cancel="emit('update:open', false)"
  >
    <div class="picker-toolbar">
      <a-input-search
        v-model:value="query.username"
        :placeholder="$t('components.user.searchPlaceholder')"
        allow-clear
        class="picker-search"
        @search="search"
      />
      <span v-if="selectedKeys.length" class="picker-selected">
        {{ multiple ? $t('components.user.selectedN', { n: selectedKeys.length }) : $t('components.user.selectedOne', { name: selectedRows[0]?.nickname || selectedRows[0]?.username || '' }) }}
        <a @click="selectedKeys = []">{{ $t('common.action.clear') }}</a>
      </span>
    </div>
    <a-table
      row-key="id"
      size="small"
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :row-selection="selection"
      :scroll="{ y: 360 }"
      :custom-row="(record) => ({ onClick: () => toggleRow(record) })"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'username'">
          <UserCell :user="record" />
        </template>
        <template v-else-if="column.key === 'dept'">
          {{ record.deptName || '-' }}
          <a-tag v-if="disabledRow?.(record)" :bordered="false" class="picker-tag">{{ disabledText }}</a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <DictTag type="sys_common_status" :value="record.status" />
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { t } from '@/locales'
import UserCell from './UserCell.vue'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'

/**
 * 选择用户弹窗：搜索、分页、跨页多选，点击整行即可勾选
 * - fetcher(params)：返回 { records, total }，params 含 current、size、username
 * - disabledRow(record)：返回 true 的行不可选（例如已经在当前部门的用户）
 * - submit(ids, rows)：确定时调用，成功后自动关闭；rows 为选中用户的行数据
 * - multiple：false 时为单选
 */
const props = defineProps({
  open: Boolean,
  title: { type: String, default: () => t('components.user.pickTitle') },
  fetcher: { type: Function, required: true },
  disabledRow: Function,
  disabledText: { type: String, default: () => t('components.user.added') },
  submit: { type: Function, required: true },
  multiple: { type: Boolean, default: true }
})
const emit = defineEmits(['update:open'])
useDict('sys_common_status')

const columns = [
  { title: t('common.field.user'), key: 'username' },
  { title: t('common.field.dept'), key: 'dept', width: 200 },
  { title: t('common.field.status'), key: 'status', width: 90 }
]

const query = reactive({ username: undefined })
const { list, loading, pagination, selectedKeys, load, search, onTableChange } = useTable((params) => props.fetcher(params), query)

// 跨页选择时记住选中行的数据，确定时一并返回
const rowCache = new Map()
watch(list, (rows) => rows.forEach((r) => rowCache.set(r.id, r)))
const selectedRows = computed(() => selectedKeys.value.map((k) => rowCache.get(k)).filter(Boolean))

const selection = computed(() => ({
  type: props.multiple ? 'checkbox' : 'radio',
  selectedRowKeys: selectedKeys.value,
  preserveSelectedRowKeys: true,
  onChange: (keys) => (selectedKeys.value = keys),
  getCheckboxProps: (record) => ({ disabled: !!props.disabledRow?.(record) })
}))

function toggleRow(record) {
  if (props.disabledRow?.(record)) return
  if (!props.multiple) {
    selectedKeys.value = [record.id]
    return
  }
  const keys = new Set(selectedKeys.value)
  keys.has(record.id) ? keys.delete(record.id) : keys.add(record.id)
  selectedKeys.value = [...keys]
}

watch(
  () => props.open,
  (open) => {
    if (!open) return
    query.username = undefined
    selectedKeys.value = []
    pagination.current = 1
    load()
  },
  { immediate: true }
)

const submitting = ref(false)
async function confirm() {
  submitting.value = true
  try {
    await props.submit([...selectedKeys.value], selectedRows.value)
    emit('update:open', false)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.picker-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.picker-search {
  width: 260px;
  max-width: 100%;
}
.picker-selected {
  color: var(--app-text-2);
}
.picker-tag {
  margin-left: 4px;
}
:deep(.ant-table-row) {
  cursor: pointer;
}
</style>
