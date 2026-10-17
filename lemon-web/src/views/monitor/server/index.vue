<template>
  <div class="server-page">
    <div class="page-card page-head">
      <div>
        <h3>{{ $t('monitor.server.title') }}</h3>
        <span class="sub">
          {{ info ? `${info.sys.hostname} · ${info.sys.ip}` : $t('monitor.server.collecting') }}
          <template v-if="updatedAt"> · {{ $t('monitor.server.updatedAt', { time: updatedAt }) }}</template>
        </span>
      </div>
      <a-space>
        <span class="sub">{{ $t('monitor.server.autoRefresh') }}</span>
        <a-switch v-model:checked="auto" size="small" />
        <a-button :loading="loading" @click="load">
          <template #icon><ReloadOutlined /></template>{{ $t('common.action.refresh') }}
        </a-button>
      </a-space>
    </div>

    <a-skeleton v-if="!info" active :paragraph="{ rows: 12 }" class="page-card skeleton" />
    <template v-else>
      <a-row :gutter="[16, 16]">
        <!-- CPU -->
        <a-col :xs="24" :lg="8">
          <a-card :bordered="false" class="page-card metric-card">
            <template #title><span class="card-title"><DashboardOutlined /> CPU</span></template>
            <div class="hero">
              <span class="hero-value">{{ info.cpu.usage.toFixed(1) }}<small>%</small></span>
              <span class="hero-label">{{ $t('monitor.server.usage') }}</span>
            </div>
            <UsageMeter :label="$t('monitor.server.totalUsage')" :percent="info.cpu.usage">
              {{ $t('monitor.server.cpuDetail', { user: info.cpu.user, system: info.cpu.system, iowait: info.cpu.iowait }) }}
            </UsageMeter>
            <a-descriptions :column="1" size="small" class="desc">
              <a-descriptions-item :label="$t('monitor.server.model')">
                <a-typography-text :ellipsis="{ tooltip: info.cpu.name }" :content="info.cpu.name" />
              </a-descriptions-item>
              <a-descriptions-item :label="$t('monitor.server.cores')">
                {{ $t('monitor.server.coresValue', { physical: info.cpu.physicalCores, logical: info.cpu.logicalCores }) }}
              </a-descriptions-item>
              <a-descriptions-item :label="$t('monitor.server.load')">
                {{ info.cpu.loadAverage.length ? info.cpu.loadAverage.join(' / ') : $t('monitor.server.unsupported') }}
                <span v-if="info.cpu.loadAverage.length" class="sub">{{ $t('monitor.server.loadMinutes') }}</span>
              </a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>

        <!-- 内存 -->
        <a-col :xs="24" :lg="8">
          <a-card :bordered="false" class="page-card metric-card">
            <template #title><span class="card-title"><HddOutlined /> {{ $t('monitor.server.memory') }}</span></template>
            <div class="hero">
              <span class="hero-value">{{ formatBytes(info.memory.used) }}</span>
              <span class="hero-label">{{ $t('monitor.server.usedOfTotal', { total: formatBytes(info.memory.total) }) }}</span>
            </div>
            <UsageMeter :label="$t('monitor.server.physicalMemory')" :percent="info.memory.usage">
              {{ $t('monitor.server.available', { value: formatBytes(info.memory.free) }) }}
            </UsageMeter>
            <UsageMeter
              v-if="info.memory.swapTotal > 0"
              :label="$t('monitor.server.swap')"
              :percent="(info.memory.swapUsed / info.memory.swapTotal) * 100"
            >
              {{ formatBytes(info.memory.swapUsed) }} / {{ formatBytes(info.memory.swapTotal) }}
            </UsageMeter>
          </a-card>
        </a-col>

        <!-- JVM 内存 -->
        <a-col :xs="24" :lg="8">
          <a-card :bordered="false" class="page-card metric-card">
            <template #title><span class="card-title"><CoffeeOutlined /> {{ $t('monitor.server.jvmMemory') }}</span></template>
            <div class="hero">
              <span class="hero-value">{{ formatBytes(info.jvm.heapUsed) }}</span>
              <span class="hero-label">{{ $t('monitor.server.heapUsedOfMax', { max: formatBytes(info.jvm.heapMax) }) }}</span>
            </div>
            <UsageMeter :label="$t('monitor.server.heap')" :percent="info.jvm.heapUsage">
              {{ $t('monitor.server.committed', { value: formatBytes(info.jvm.heapCommitted) }) }}
            </UsageMeter>
            <a-descriptions :column="2" size="small" class="desc">
              <a-descriptions-item :label="$t('monitor.server.nonHeap')">{{ formatBytes(info.jvm.nonHeapUsed) }}</a-descriptions-item>
              <a-descriptions-item :label="$t('monitor.server.loadedClasses')">{{ formatNumber(info.jvm.loadedClassCount) }}</a-descriptions-item>
              <a-descriptions-item :label="$t('monitor.server.threads')">{{ info.jvm.threadCount }}</a-descriptions-item>
              <a-descriptions-item :label="$t('monitor.server.peakThreads')">{{ info.jvm.peakThreadCount }}</a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>

        <!-- 服务器信息 -->
        <a-col :xs="24" :xl="12">
          <a-card :bordered="false" class="page-card" :title="$t('monitor.server.serverInfo')">
            <a-descriptions :column="1" size="small" bordered class="info-desc" :label-style="labelStyle" :content-style="contentStyle">
              <a-descriptions-item :label="$t('monitor.server.hostname')">{{ info.sys.hostname }}</a-descriptions-item>
              <a-descriptions-item :label="$t('monitor.server.serverIp')">{{ info.sys.ip }}</a-descriptions-item>
              <a-descriptions-item :label="$t('monitor.server.os')">{{ info.sys.osName }}</a-descriptions-item>
              <a-descriptions-item :label="$t('monitor.server.arch')">{{ info.sys.osArch }}</a-descriptions-item>
              <a-descriptions-item :label="$t('monitor.server.osVersion')">{{ info.sys.osVersion }}</a-descriptions-item>
              <a-descriptions-item :label="$t('monitor.server.sysUptime')">{{ formatDuration(info.sys.uptime) }}</a-descriptions-item>
              <a-descriptions-item :label="$t('monitor.server.timezone')">{{ info.sys.timezone }}</a-descriptions-item>
              <a-descriptions-item :label="$t('monitor.server.pid')">{{ info.sys.pid }}</a-descriptions-item>
              <a-descriptions-item :label="$t('monitor.server.userDir')">
                {{ info.sys.userDir }}
              </a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>

        <!-- JVM 信息 -->
        <a-col :xs="24" :xl="12">
          <a-card :bordered="false" class="page-card" :title="$t('monitor.server.jvm')">
            <a-descriptions :column="1" size="small" bordered class="info-desc" :label-style="labelStyle" :content-style="contentStyle">
              <a-descriptions-item :label="$t('common.field.name')">{{ info.jvm.name }}</a-descriptions-item>
              <a-descriptions-item :label="$t('monitor.server.version')">{{ info.jvm.version }}</a-descriptions-item>
              <a-descriptions-item :label="$t('monitor.server.vendor')">{{ info.jvm.vendor }}</a-descriptions-item>
              <a-descriptions-item :label="$t('monitor.server.startTime')">{{ info.jvm.startTime }}</a-descriptions-item>
              <a-descriptions-item :label="$t('monitor.server.uptime')">{{ formatDuration(info.jvm.uptime) }}</a-descriptions-item>
              <a-descriptions-item :label="$t('monitor.server.gc')">
                <div v-for="gc in info.jvm.gcs" :key="gc.name" class="gc">
                  {{ $t('monitor.server.gcValue', { name: gc.name, count: formatNumber(gc.count), time: formatNumber(gc.time) }) }}
                </div>
              </a-descriptions-item>
              <a-descriptions-item :label="$t('monitor.server.home')">
                {{ info.jvm.home }}
              </a-descriptions-item>
              <a-descriptions-item :label="$t('monitor.server.inputArgs')">
                <span v-if="!info.jvm.inputArgs.length" class="sub">{{ $t('common.status.none') }}</span>
                <div v-else class="args">
                  <a-tag v-for="arg in info.jvm.inputArgs" :key="arg" :bordered="false">{{ arg }}</a-tag>
                </div>
              </a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>

        <!-- 磁盘 -->
        <a-col :span="24">
          <a-card :bordered="false" class="page-card" :title="$t('monitor.server.disk')">
            <a-table
              row-key="mount"
              size="middle"
              :columns="diskColumns"
              :data-source="info.disks"
              :pagination="false"
              :scroll="{ x: 800 }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'usage'">
                  <UsageMeter :label="`${formatBytes(record.used)} / ${formatBytes(record.total)}`" :percent="record.usage" />
                </template>
                <template v-else-if="column.key === 'free'">{{ formatBytes(record.free) }}</template>
              </template>
            </a-table>
          </a-card>
        </a-col>
      </a-row>
    </template>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { useI18n } from 'vue-i18n'
import { CoffeeOutlined, DashboardOutlined, HddOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import UsageMeter from '@/components/monitor/UsageMeter.vue'
import { getServerInfo } from '@/api/monitor/server'
import { formatBytes, formatDuration, formatNumber } from '@/utils/format'
import { isZh } from '@/locales'

const { t } = useI18n()
const REFRESH_MS = 5000

// 信息表格：标签不换行，长内容（路径、参数）可折行
const labelStyle = { width: isZh() ? '96px' : '130px', whiteSpace: 'nowrap' }
const contentStyle = { wordBreak: 'break-all' }

const info = ref(null)
const loading = ref(false)
const updatedAt = ref('')
const auto = ref(true)

const diskColumns = [
  { title: t('monitor.server.mount'), dataIndex: 'mount', width: 200, ellipsis: true },
  { title: t('common.field.name'), dataIndex: 'name', width: 200, ellipsis: true },
  { title: t('monitor.server.fsType'), dataIndex: 'type', width: 110 },
  { title: t('monitor.server.free'), key: 'free', width: 120 },
  { title: t('monitor.server.diskUsage'), key: 'usage', width: 320 }
]

async function load() {
  if (loading.value) return
  loading.value = true
  try {
    info.value = await getServerInfo()
    updatedAt.value = dayjs().format('HH:mm:ss')
  } finally {
    loading.value = false
  }
}

// 定时刷新：页面不可见时暂停，离开页面时清理
let timer = null
function schedule() {
  clearInterval(timer)
  timer = auto.value ? setInterval(() => document.visibilityState === 'visible' && load(), REFRESH_MS) : null
}
watch(auto, schedule)
onMounted(() => {
  load()
  schedule()
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding: 16px 24px;
  margin-bottom: 16px;
  background: var(--app-card);
}
.page-head h3 {
  margin: 0 0 2px;
  font-size: 16px;
}
.sub {
  color: var(--app-text-3);
  font-size: 12px;
}
.skeleton {
  padding: 24px;
  background: var(--app-card);
}
.metric-card {
  height: 100%;
}
.card-title :deep(.anticon) {
  color: var(--app-primary);
  margin-right: 4px;
}
.hero {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 16px;
}
.hero-value {
  font-size: 30px;
  font-weight: 600;
  line-height: 1.2;
  color: var(--app-text);
  font-variant-numeric: tabular-nums;
}
.hero-value small {
  font-size: 16px;
  margin-left: 2px;
}
.hero-label {
  color: var(--app-text-3);
  font-size: 12px;
}
.desc {
  margin-top: 16px;
}
/* 固定布局：标签列定宽，内容列占满剩余宽度，长路径/参数在列内折行而不是撑出卡片 */
.info-desc :deep(table) {
  table-layout: fixed;
}
.args :deep(.ant-tag) {
  white-space: normal;
  word-break: break-all;
}
.gc + .gc {
  margin-top: 2px;
}
.args {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.args :deep(.ant-tag) {
  margin: 0;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
}
</style>
