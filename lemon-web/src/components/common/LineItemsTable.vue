<template>
  <a-table
    row-key="key"
    size="small"
    :columns="columns"
    :data-source="items"
    :pagination="pagination"
    :scroll="{ x: scrollX ?? widthSum }"
    :locale="emptyText ? { emptyText } : undefined"
  >
    <template #bodyCell="slotProps">
      <template v-if="slotProps.column.key === 'action' && !$slots.action">
        <a-button type="link" danger size="small" :disabled="items.length <= minRows" @click="remove(slotProps.record)">
          <template #icon><DeleteOutlined /></template>
        </a-button>
      </template>
      <slot v-else-if="slotProps.column.key === 'action'" name="action" v-bind="slotProps" />
      <slot v-else name="cell" v-bind="slotProps" />
    </template>
  </a-table>
</template>

<script setup>
import { computed } from 'vue'
import { Modal } from 'ant-design-vue'
import { DeleteOutlined } from '@ant-design/icons-vue'

/**
 * 单据明细行表格：行 key 为 key（见 useLineItems），横向滚动宽度默认为各列宽之和；
 * key 为 action 的列默认是删除按钮（少于等于 minRows 行时不可删，confirmRemove 返回标题时先确认），可用 #action 插槽替换；
 * 其余单元格用 #cell 插槽，参数同 a-table 的 bodyCell（column、record、index）。
 */
const props = defineProps({
  items: { type: Array, required: true },
  columns: { type: Array, required: true },
  emptyText: { type: String, default: undefined },
  pagination: { type: [Object, Boolean], default: false },
  scrollX: { type: Number, default: undefined },
  minRows: { type: Number, default: 0 },
  /** (行) => 确认框标题，返回空时直接删除 */
  confirmRemove: { type: Function, default: undefined }
})
const emit = defineEmits(['remove'])

const widthSum = computed(() => props.columns.reduce((sum, c) => sum + (c.width || 0), 0))

function remove(record) {
  const doRemove = () => {
    const i = props.items.indexOf(record)
    if (i >= 0) props.items.splice(i, 1)
    emit('remove', record)
  }
  const title = props.confirmRemove?.(record)
  if (!title) return doRemove()
  Modal.confirm({ title, okType: 'danger', onOk: doRemove })
}
</script>
