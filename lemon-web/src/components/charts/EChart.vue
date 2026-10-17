<template>
  <div ref="el" class="echart" :style="{ height }" />
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, FunnelChart, GaugeChart, LineChart, PieChart, ScatterChart } from 'echarts/charts'
import { DatasetComponent, GridComponent, LegendComponent, MarkLineComponent, TitleComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { useAppStore } from '@/store/app'

echarts.use([BarChart, LineChart, PieChart, FunnelChart, ScatterChart, GaugeChart, GridComponent, TooltipComponent, LegendComponent,
  TitleComponent, DatasetComponent, MarkLineComponent, CanvasRenderer])

/**
 * ECharts 图表：option 变化时更新，随容器大小自适应，跟随系统亮色 / 暗色主题。
 * 只注册了柱状图、折线图、饼图、漏斗图、散点图、仪表盘，需要其他图表时在这里添加
 * - option：ECharts 配置
 * - height：高度，默认 300px
 * - dark：强制暗色（数据大屏），不传时跟随系统主题
 * - @click(params)：点击图形（钻取）
 */
const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '300px' },
  dark: { type: Boolean, default: undefined }
})
const emit = defineEmits(['click'])

const el = ref()
const chart = shallowRef()
const app = useAppStore()
let observer

/** 主题色：文字、轴线随暗色模式切换 */
function themed(option) {
  const dark = props.dark ?? app.dark
  const text = dark ? 'rgba(255,255,255,0.65)' : 'rgba(0,0,0,0.65)'
  const line = dark ? (props.dark ? 'rgba(80, 160, 255, 0.15)' : '#303033') : '#e8e8e8'
  const axis = (a) => (a ? [].concat(a).map((x) => ({ axisLabel: { color: text }, axisLine: { lineStyle: { color: line } },
    splitLine: { lineStyle: { color: line } }, ...x })) : a)
  return {
    color: ['#1677ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96', '#fa8c16', '#2f54eb', '#a0d911'],
    backgroundColor: 'transparent',
    textStyle: { color: text },
    tooltip: { confine: true },
    ...option,
    legend: option.legend ? { textStyle: { color: text }, ...option.legend } : undefined,
    xAxis: axis(option.xAxis),
    yAxis: axis(option.yAxis)
  }
}

function render() {
  if (!chart.value) return
  chart.value.setOption(themed(props.option), true)
}

onMounted(() => {
  chart.value = echarts.init(el.value)
  chart.value.on('click', (p) => emit('click', p))
  render()
  observer = new ResizeObserver(() => chart.value?.resize())
  observer.observe(el.value)
})
onBeforeUnmount(() => {
  observer?.disconnect()
  chart.value?.dispose()
})
watch(() => props.option, render, { deep: true })
watch(() => app.dark, render)
</script>

<style scoped>
.echart {
  width: 100%;
}
</style>
