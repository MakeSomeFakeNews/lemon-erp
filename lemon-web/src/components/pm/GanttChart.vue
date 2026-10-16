<template>
  <div class="gantt" :style="{ maxHeight }">
    <a-empty v-if="!rows.length" description="还没有任务" class="empty" />
    <div v-else class="gantt-body">
      <!-- 左侧：任务、负责人、进度 -->
      <div class="left">
        <div class="head left-head">
          <span class="col-name">任务</span>
          <span class="col-user">负责人</span>
          <span class="col-progress">进度</span>
        </div>
        <div v-for="t in rows" :key="t.id" class="row left-row" :class="{ parent: t.parent }">
          <span class="col-name" :style="{ paddingLeft: `${8 + t.level * 16}px` }" :title="t.name">
            <FlagFilled v-if="t.milestone === 1" class="milestone-icon" />{{ t.name }}
          </span>
          <span class="col-user">{{ t.assigneeName || '-' }}</span>
          <span class="col-progress" :class="{ late: t.overdue }">{{ t.progress }}%</span>
        </div>
      </div>

      <!-- 右侧：时间轴 -->
      <div ref="rightRef" class="right">
        <div class="timeline" :style="{ width: `${days.length * DAY}px` }">
          <div class="head">
            <div class="months">
              <span v-for="m in months" :key="m.key" class="month" :style="{ width: `${m.days * DAY}px` }">{{ m.label }}</span>
            </div>
            <div class="days">
              <span
                v-for="d in days"
                :key="d.key"
                class="day"
                :class="{ weekend: d.weekend, today: d.today }"
                :style="{ width: `${DAY}px` }"
              >{{ d.date }}</span>
            </div>
          </div>
          <div class="grid">
            <div
              v-for="d in days"
              :key="d.key"
              class="grid-col"
              :class="{ weekend: d.weekend }"
              :style="{ left: `${d.index * DAY}px`, width: `${DAY}px` }"
            />
            <div v-if="todayIndex >= 0" class="today-line" :style="{ left: `${todayIndex * DAY + DAY / 2}px` }" />
            <div v-for="t in rows" :key="t.id" class="row bar-row">
              <a-tooltip :title="tip(t)">
                <div v-if="t.milestone === 1 && !t.parent" class="diamond" :class="{ done: t.status === 'DONE', late: t.overdue }"
                     :style="{ left: `${offset(t.planEnd) * DAY + DAY / 2 - 7}px` }" />
                <div
                  v-else
                  class="bar"
                  :class="{ parent: t.parent, late: t.overdue, done: t.status === 'DONE' }"
                  :style="barStyle(t)"
                >
                  <div class="fill" :style="{ width: `${t.progress}%` }" />
                </div>
              </a-tooltip>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-if="rows.length" class="legend">
      <span><i class="lg bar-sample" />计划</span>
      <span><i class="lg fill-sample" />已完成</span>
      <span><i class="lg late-sample" />逾期</span>
      <span><i class="lg parent-sample" />汇总任务</span>
      <span><i class="lg diamond-sample" />里程碑</span>
      <span><i class="lg today-sample" />今天</span>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { FlagFilled } from '@ant-design/icons-vue'
import { flattenTasks } from './pm'

/**
 * 甘特图（只读）：左侧任务树，右侧按天的时间轴；上级任务显示为汇总条，里程碑显示为菱形，逾期任务为红色，竖线为今天
 * - tasks：任务树（PmTask，含 children）
 */
const props = defineProps({
  tasks: { type: Array, default: () => [] },
  maxHeight: { type: String, default: '560px' }
})

const DAY = 24
const rightRef = ref()

const rows = computed(() => flattenTasks(props.tasks).map((t) => ({ ...t, parent: !!t.children?.length })))

const range = computed(() => {
  if (!rows.value.length) return null
  let start = dayjs(rows.value[0].planStart)
  let end = dayjs(rows.value[0].planEnd)
  rows.value.forEach((t) => {
    if (dayjs(t.planStart).isBefore(start)) start = dayjs(t.planStart)
    if (dayjs(t.planEnd).isAfter(end)) end = dayjs(t.planEnd)
  })
  return { start: start.subtract(2, 'day'), end: end.add(5, 'day') }
})

const days = computed(() => {
  if (!range.value) return []
  const list = []
  const todayStr = dayjs().format('YYYY-MM-DD')
  for (let d = range.value.start, i = 0; !d.isAfter(range.value.end); d = d.add(1, 'day'), i++) {
    list.push({ key: d.format('YYYY-MM-DD'), index: i, date: d.date(), weekend: d.day() === 0 || d.day() === 6, today: d.format('YYYY-MM-DD') === todayStr })
  }
  return list
})

const months = computed(() => {
  const list = []
  days.value.forEach((d) => {
    const key = d.key.slice(0, 7)
    const last = list[list.length - 1]
    if (last && last.key === key) last.days++
    else list.push({ key, label: dayjs(d.key).format('YYYY 年 M 月'), days: 1 })
  })
  return list
})

const offset = (date) => dayjs(date).diff(range.value.start, 'day')
const todayIndex = computed(() => (range.value ? days.value.findIndex((d) => d.today) : -1))

function barStyle(t) {
  const left = offset(t.planStart) * DAY
  const width = (offset(t.planEnd) - offset(t.planStart) + 1) * DAY
  return { left: `${left + 2}px`, width: `${Math.max(width - 4, 6)}px` }
}

function tip(t) {
  return `${t.name}：${t.planStart} ~ ${t.planEnd}，进度 ${t.progress}%${t.overdue ? '（逾期）' : ''}${t.predecessorName ? `，前置：${t.predecessorName}` : ''}`
}

// 打开时滚动到今天附近
watch(
  todayIndex,
  async (i) => {
    await nextTick()
    if (rightRef.value && i > 0) rightRef.value.scrollLeft = Math.max(i * DAY - 200, 0)
  },
  { immediate: true }
)
</script>

<style scoped>
.gantt {
  overflow-y: auto;
  border: 1px solid var(--app-border);
  border-radius: 8px;
}
.empty {
  padding: 32px 0;
}
.gantt-body {
  display: flex;
}
.left {
  flex: none;
  width: 360px;
  border-right: 1px solid var(--app-border);
  background: var(--app-card);
}
.right {
  flex: 1;
  overflow-x: auto;
  min-width: 0;
}
.head {
  height: 48px;
  border-bottom: 1px solid var(--app-border);
  background: var(--app-hover);
}
.left-head {
  display: flex;
  align-items: center;
  font-weight: 500;
}
.row {
  height: 32px;
  border-bottom: 1px solid var(--app-border);
}
.left-row {
  display: flex;
  align-items: center;
}
.left-row.parent {
  font-weight: 600;
}
.col-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  padding-left: 8px;
}
.col-user {
  width: 80px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: var(--app-text-2);
}
.col-progress {
  width: 52px;
  text-align: right;
  padding-right: 8px;
  color: var(--app-text-2);
}
.col-progress.late {
  color: #f5222d;
}
.milestone-icon {
  color: #faad14;
  margin-right: 4px;
}
.timeline {
  position: relative;
}
.months,
.days {
  display: flex;
  height: 24px;
}
.month {
  flex: none;
  padding-left: 6px;
  line-height: 24px;
  font-size: 12px;
  color: var(--app-text-2);
  border-right: 1px solid var(--app-border);
  white-space: nowrap;
  overflow: hidden;
}
.day {
  flex: none;
  line-height: 24px;
  text-align: center;
  font-size: 11px;
  color: var(--app-text-3);
}
.day.weekend {
  color: var(--app-text-3);
  opacity: 0.6;
}
.day.today {
  color: #fff;
  background: var(--app-primary);
  border-radius: 4px;
}
.grid {
  position: relative;
}
.grid-col {
  position: absolute;
  top: 0;
  bottom: 0;
}
.grid-col.weekend {
  background: var(--app-hover);
}
.today-line {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 2px;
  margin-left: -1px;
  background: #f5222d;
  opacity: 0.6;
  z-index: 1;
}
.bar-row {
  position: relative;
}
.bar {
  position: absolute;
  top: 8px;
  height: 16px;
  border-radius: 4px;
  background: rgba(22, 119, 255, 0.25);
  overflow: hidden;
  z-index: 1;
}
.bar .fill {
  height: 100%;
  background: #1677ff;
}
.bar.done .fill {
  background: #52c41a;
}
.bar.done {
  background: rgba(82, 196, 26, 0.25);
}
.bar.late {
  background: rgba(245, 34, 45, 0.25);
}
.bar.late .fill {
  background: #f5222d;
}
.bar.parent {
  top: 11px;
  height: 10px;
  border-radius: 2px;
  background: rgba(114, 46, 209, 0.25);
}
.bar.parent .fill {
  background: #722ed1;
}
.diamond {
  position: absolute;
  top: 9px;
  width: 14px;
  height: 14px;
  transform: rotate(45deg);
  background: #faad14;
  z-index: 1;
}
.diamond.done {
  background: #52c41a;
}
.diamond.late {
  background: #f5222d;
}
.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  padding: 8px 12px;
  font-size: 12px;
  color: var(--app-text-3);
  border-top: 1px solid var(--app-border);
}
.lg {
  display: inline-block;
  width: 16px;
  height: 8px;
  border-radius: 2px;
  margin-right: 4px;
  vertical-align: middle;
}
.bar-sample {
  background: rgba(22, 119, 255, 0.25);
}
.fill-sample {
  background: #1677ff;
}
.late-sample {
  background: #f5222d;
}
.parent-sample {
  background: #722ed1;
}
.diamond-sample {
  width: 8px;
  height: 8px;
  transform: rotate(45deg);
  background: #faad14;
}
.today-sample {
  width: 2px;
  height: 12px;
  background: #f5222d;
}
</style>
