<template>
  <a-table
    row-key="id"
    size="small"
    :columns="columns"
    :data-source="list"
    :loading="loading"
    :pagination="total > size ? { current, pageSize: size, total, size: 'small', onChange: (p) => ((current = p), load()) } : false"
    :locale="{ emptyText: '暂无变更记录' }"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'field'">{{ record.fieldLabel || record.fieldName }}</template>
      <template v-else-if="column.key === 'change'">
        <span class="old">{{ record.oldValue ?? '（空）' }}</span>
        <ArrowRightOutlined class="arrow" />
        <span>{{ record.newValue ?? '（空）' }}</span>
      </template>
    </template>
  </a-table>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ArrowRightOutlined } from '@ant-design/icons-vue'
import { pageChangeLog } from '@/api/system/changeLog'

/**
 * 字段变更历史：bizType 为后端 RecordType.code（如 wms_material）
 */
const props = defineProps({
  bizType: { type: String, required: true },
  bizId: [Number, String]
})

const columns = [
  { title: '时间', dataIndex: 'createTime', width: 170 },
  { title: '修改人', dataIndex: 'createByName', width: 100 },
  { title: '字段', key: 'field', width: 120 },
  { title: '变更', key: 'change' }
]
const list = ref([])
const loading = ref(false)
const current = ref(1)
const size = 20
const total = ref(0)

async function load() {
  if (!props.bizId) {
    list.value = []
    return
  }
  loading.value = true
  try {
    const res = await pageChangeLog({ bizType: props.bizType, bizId: props.bizId, current: current.value, size })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}
watch(
  () => [props.bizType, props.bizId],
  () => {
    current.value = 1
    load()
  },
  { immediate: true }
)
</script>

<style scoped>
.old {
  color: var(--ant-color-text-tertiary, #999);
  text-decoration: line-through;
}
.arrow {
  margin: 0 6px;
  color: var(--ant-color-text-tertiary, #999);
}
</style>
