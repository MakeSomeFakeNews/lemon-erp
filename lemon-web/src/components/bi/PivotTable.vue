<template>
  <a-table
    class="pivot-table"
    row-key="key"
    size="small"
    bordered
    :columns="columns"
    :data-source="rows"
    :pagination="rows.length > pageSize ? { pageSize, showSizeChanger: false, size: 'small' } : false"
    :scroll="{ x: scrollX }"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === '__row'">
        <a v-if="drillable && record.r !== null && record.r !== '-'" class="row-link" title="钻取" @click="emit('drill', record.r)">
          {{ record.label }}
        </a>
        <span v-else>{{ record.label }}</span>
      </template>
      <template v-else>
        <span class="num">{{ formatMeasure(record[column.dataIndex], column.format) }}</span>
      </template>
    </template>
    <template #summary>
      <a-table-summary fixed="bottom">
        <a-table-summary-row class="total-row">
          <a-table-summary-cell :index="0">合计</a-table-summary-cell>
          <a-table-summary-cell v-for="(c, i) in leafColumns" :key="c.dataIndex" :index="i + 1" align="right">
            <span class="num">{{ formatMeasure(totals[c.dataIndex], c.format) }}</span>
          </a-table-summary-cell>
        </a-table-summary-row>
      </a-table-summary>
    </template>
  </a-table>
</template>

<script setup>
import { computed } from 'vue'
import { activeMeasures, dimLabelFn, formatMeasure } from './bi'

/**
 * 透视表：行为行维度的值，列为（列维度值 × 指标），有列维度时最后一组为行合计；底部为合计行。
 * - cube：数据集元数据；config：{ row, col, measures }；result：透视结果
 * - drillable：行维度值可点击，@drill(value)
 */
const props = defineProps({
  cube: { type: Object, required: true },
  config: { type: Object, required: true },
  result: { type: Object, default: null },
  drillable: Boolean,
  pageSize: { type: Number, default: 50 }
})
const emit = defineEmits(['drill'])

const measures = computed(() => activeMeasures(props.cube, props.config))
const rowLabel = computed(() => dimLabelFn(props.cube, props.config.row, props.result?.labels?.rows))
const colLabel = computed(() => dimLabelFn(props.cube, props.config.col, props.result?.labels?.cols))
const hasCol = computed(() => !!props.config.col && (props.result?.colTotals?.length ?? 0) > 0)
const colValues = computed(() => (hasCol.value ? props.result.colTotals.map((c) => c.c) : []))

const measureColumn = (prefix, m) => ({
  title: m.label,
  dataIndex: `${prefix}__${m.key}`,
  key: `${prefix}__${m.key}`,
  format: m.format,
  align: 'right',
  width: m.format === 'MONEY' ? 130 : 100
})

const columns = computed(() => {
  const dim = props.cube.dims.find((d) => d.key === props.config.row)
  const first = { title: dim?.label || '汇总', key: '__row', fixed: 'left', width: 180, ellipsis: true }
  if (!hasCol.value) return [first, ...measures.value.map((m) => measureColumn('T', m))]
  const groups = colValues.value.map((c) => ({
    title: colLabel.value(c),
    key: `g-${c}`,
    children: measures.value.map((m) => measureColumn(`C${c}`, m))
  }))
  groups.push({ title: '合计', key: 'g-total', children: measures.value.map((m) => measureColumn('T', m)) })
  return [first, ...groups]
})

const leafColumns = computed(() => columns.value.slice(1).flatMap((c) => c.children || [c]))
const scrollX = computed(() => 180 + leafColumns.value.reduce((s, c) => s + c.width, 0))

const rows = computed(() => {
  const res = props.result
  if (!res) return []
  const byRow = new Map()
  for (const t of res.rowTotals || []) {
    const row = { key: `r-${t.r}`, r: t.r, label: rowLabel.value(t.r) }
    measures.value.forEach((m) => (row[`T__${m.key}`] = t[m.key]))
    byRow.set(t.r, row)
  }
  if (hasCol.value) {
    for (const cell of res.cells || []) {
      const row = byRow.get(cell.r)
      if (row) measures.value.forEach((m) => (row[`C${cell.c}__${m.key}`] = cell[m.key]))
    }
  }
  return [...byRow.values()]
})

const totals = computed(() => {
  const res = props.result
  const t = {}
  if (!res) return t
  measures.value.forEach((m) => (t[`T__${m.key}`] = res.total?.[m.key]))
  if (hasCol.value) {
    for (const ct of res.colTotals) measures.value.forEach((m) => (t[`C${ct.c}__${m.key}`] = ct[m.key]))
  }
  return t
})
</script>

<style scoped>
.num {
  font-variant-numeric: tabular-nums;
}
.row-link {
  cursor: pointer;
}
.pivot-table :deep(.total-row td) {
  font-weight: 600;
  background: var(--app-hover);
}
</style>
