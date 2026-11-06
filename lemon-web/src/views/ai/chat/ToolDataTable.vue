<template>
  <div class="tool-data">
    <a-alert v-if="error" type="warning" show-icon :message="$t('ai.chat.queryFailed', { msg: error })" />
    <template v-else-if="rows">
      <a-table
        :columns="columns"
        :data-source="rows"
        :row-key="(_, i) => i"
        size="small"
        :pagination="rows.length > PAGE_SIZE ? { pageSize: PAGE_SIZE, size: 'small', showSizeChanger: false } : false"
        :scroll="{ x: 'max-content' }"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <span :class="{ number: isNumber(record[column.dataIndex]) }">{{ format(record[column.dataIndex]) }}</span>
        </template>
      </a-table>
      <div class="foot sub-text">
        <span v-if="truncated">{{ $t('ai.chat.truncated', { shown: rows.length, total: total ?? rows.length }) }}</span>
        <span v-else>{{ $t('ai.chat.rows', { n: total ?? rows.length }) }}</span>
      </div>
    </template>
    <a-descriptions v-else-if="pairs" size="small" bordered :column="1">
      <a-descriptions-item v-for="p in pairs" :key="p.key" :label="p.key">{{ format(p.value) }}</a-descriptions-item>
    </a-descriptions>
    <pre v-else class="raw">{{ format(data) }}</pre>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { num } from '@/utils/number'

/**
 * 工具返回数据的表格：对象数组按字段生成列（字段名即列名），单个对象显示为键值对，其他直接显示 JSON
 */
const props = defineProps({
  data: { type: [Array, Object, String, Number, Boolean], default: null },
  total: { type: Number, default: null },
  truncated: Boolean,
  error: { type: String, default: null }
})

const PAGE_SIZE = 10
const isObject = (v) => v !== null && typeof v === 'object' && !Array.isArray(v)
const rows = computed(() => (Array.isArray(props.data) && props.data.every(isObject) ? props.data : null))
const columns = computed(() => {
  const keys = []
  for (const row of (rows.value || []).slice(0, 50)) {
    for (const k of Object.keys(row)) if (!keys.includes(k)) keys.push(k)
  }
  return keys.map((k) => ({ title: k, dataIndex: k, ellipsis: true }))
})
const pairs = computed(() => (isObject(props.data) ? Object.entries(props.data).map(([key, value]) => ({ key, value })) : null))

const isNumber = (v) => typeof v === 'number'
function format(v) {
  if (v === null || v === undefined) return '-'
  if (typeof v === 'number') return num(v, 4)
  if (typeof v === 'object') return JSON.stringify(v)
  return String(v)
}
</script>

<style scoped>
.tool-data {
  margin-top: 8px;
}
.number {
  font-variant-numeric: tabular-nums;
}
.foot {
  margin-top: 4px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.raw {
  margin: 0;
  padding: 8px;
  background: var(--app-code-bg);
  border-radius: 6px;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
}
</style>
