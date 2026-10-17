<template>
  <div class="resource-page">
    <a-card :bordered="false" class="page-card">
      <div class="filters">
        <DeptSelect v-model:value="deptId" :placeholder="$t('pm.resource.allDepts')" class="dept" @update:value="load" />
        <span class="sub-text">{{ $t('pm.resource.tip') }}</span>
        <a-button type="text" class="refresh" @click="load"><template #icon><ReloadOutlined /></template></a-button>
      </div>
      <EChart v-if="rows.length" :option="chartOption" :height="`${Math.max(220, rows.length * 32 + 60)}px`" />
    </a-card>

    <a-card :bordered="false" class="page-card">
      <a-table row-key="userId" size="middle" :columns="columns" :data-source="rows" :loading="loading" :pagination="false"
               :scroll="{ x: 1000 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'user'">
            <div>{{ record.userName }}</div>
            <div class="sub-text">{{ record.deptName || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'allocation'">
            <a-progress :percent="Math.min(record.allocation, 100)" size="small" :status="record.allocation > 100 ? 'exception' : 'normal'"
                        :format="() => `${record.allocation}%`" class="alloc" />
            <a-tag v-if="record.allocation > 100" color="red" :bordered="false">{{ $t('pm.resource.overloaded') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'hours'">{{ $t('pm.common.nHours', { n: Number(record.openTaskHours) }) }}</template>
          <template v-else-if="column.key === 'projects'">
            <a-tag v-for="m in record.projects" :key="m.id" :bordered="false" class="project-tag"
                   :class="{ left: m.leaveDate && m.leaveDate < todayStr }">
              {{ m.projectCode }} {{ m.projectName }} · {{ roleLabel(m.role) }} {{ m.allocation }}%
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import EChart from '@/components/charts/EChart.vue'
import DeptSelect from '@/components/fin/DeptSelect.vue'
import { listResources } from '@/api/pm'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { today } from '@/components/pm/pm'
import { t } from '@/locales'

const dict = useDict('pm_member_role')
const deptId = ref()
const rows = ref([])
const loading = ref(false)
const todayStr = today()

const columns = [
  { title: t('pm.common.member'), key: 'user', width: 160, fixed: 'left' },
  { title: t('pm.detail.allocation'), key: 'allocation', width: 240 },
  { title: t('pm.resource.remainingHours'), key: 'hours', width: 170 },
  { title: t('pm.resource.projects'), key: 'projects' }
]

const roleLabel = (v) => findDict(dict.pm_member_role, v)?.label || v

async function load() {
  loading.value = true
  try {
    rows.value = await listResources(deptId.value || undefined)
  } finally {
    loading.value = false
  }
}
onMounted(load)

const chartOption = computed(() => {
  const list = [...rows.value].reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v) => `${v}%` },
    grid: { left: 90, right: 40, top: 20, bottom: 30 },
    xAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
    yAxis: { type: 'category', data: list.map((r) => r.userName) },
    series: [
      {
        type: 'bar',
        name: t('pm.detail.allocation'),
        barMaxWidth: 18,
        data: list.map((r) => ({ value: r.allocation, itemStyle: { color: r.allocation > 100 ? '#f5222d' : '#1677ff' } })),
        label: { show: true, position: 'right', formatter: '{c}%' },
        markLine: { symbol: 'none', data: [{ xAxis: 100 }], lineStyle: { color: '#faad14', type: 'dashed' }, label: { formatter: '100%' } }
      }
    ]
  }
})
</script>

<style scoped>
.resource-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.resource-page > .page-card + .page-card {
  margin-top: 0;
}
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.dept {
  width: 220px;
}
.refresh {
  margin-left: auto;
}
.alloc {
  display: inline-block;
  width: 150px;
  margin-right: 8px;
}
.project-tag {
  margin-bottom: 4px;
}
.project-tag.left {
  opacity: 0.5;
  text-decoration: line-through;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
