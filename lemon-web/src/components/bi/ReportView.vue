<template>
  <div class="report-view">
    <a-spin :spinning="loading">
      <a-empty v-if="!loading && empty" :image="simpleImage" description="没有数据，请先同步数据仓库或调整日期范围" />
      <template v-else-if="cube && result">
        <PivotTable v-if="type === 'table'" :cube="cube" :config="config" :result="result" :page-size="10" />
        <EChart v-else-if="option" :option="option" :height="height" />
      </template>
      <div v-else :style="{ height }" />
    </a-spin>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { Empty } from 'ant-design-vue'
import EChart from '@/components/charts/EChart.vue'
import PivotTable from './PivotTable.vue'
import { olapPivot } from '@/api/bi'
import { loadCubes, pivotChartOption } from './bi'

/**
 * 按报表配置执行多维分析并展示为图表或透视表
 * - config：报表配置（PivotRequest）
 * - chart：图表类型，不传使用配置中的类型（bar / line / pie / table）
 */
const props = defineProps({
  config: { type: Object, required: true },
  chart: String,
  height: { type: String, default: '300px' }
})

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const cube = ref(null)
const result = ref(null)
const loading = ref(false)

const type = computed(() => props.chart || props.config.chart || 'bar')
const empty = computed(() => !result.value?.rowTotals?.length)
const option = computed(() => (cube.value && result.value ? pivotChartOption(cube.value, props.config, result.value, type.value) : null))

async function load() {
  loading.value = true
  try {
    const cubes = await loadCubes()
    cube.value = cubes.find((c) => c.key === props.config.cube) || null
    result.value = await olapPivot(props.config)
  } catch {
    result.value = null
  } finally {
    loading.value = false
  }
}

watch(() => JSON.stringify(props.config), load, { immediate: true })
</script>

<style scoped>
.report-view {
  min-height: 120px;
}
</style>
