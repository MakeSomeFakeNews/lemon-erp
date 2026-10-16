<template>
  <div class="server-page">
    <div class="page-card page-head">
      <div>
        <h3>服务监控</h3>
        <span class="sub">
          {{ info ? `${info.sys.hostname} · ${info.sys.ip}` : '正在采集…' }}
          <template v-if="updatedAt"> · 更新于 {{ updatedAt }}</template>
        </span>
      </div>
      <a-space>
        <span class="sub">自动刷新</span>
        <a-switch v-model:checked="auto" size="small" />
        <a-button :loading="loading" @click="load">
          <template #icon><ReloadOutlined /></template>刷新
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
              <span class="hero-label">使用率</span>
            </div>
            <UsageMeter label="总使用率" :percent="info.cpu.usage">
              用户 {{ info.cpu.user }}% · 系统 {{ info.cpu.system }}% · IO 等待 {{ info.cpu.iowait }}%
            </UsageMeter>
            <a-descriptions :column="1" size="small" class="desc">
              <a-descriptions-item label="型号">
                <a-typography-text :ellipsis="{ tooltip: info.cpu.name }" :content="info.cpu.name" />
              </a-descriptions-item>
              <a-descriptions-item label="核心">
                {{ info.cpu.physicalCores }} 物理 / {{ info.cpu.logicalCores }} 逻辑
              </a-descriptions-item>
              <a-descriptions-item label="负载">
                {{ info.cpu.loadAverage.length ? info.cpu.loadAverage.join(' / ') : '不支持' }}
                <span v-if="info.cpu.loadAverage.length" class="sub">（1 / 5 / 15 分钟）</span>
              </a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>

        <!-- 内存 -->
        <a-col :xs="24" :lg="8">
          <a-card :bordered="false" class="page-card metric-card">
            <template #title><span class="card-title"><HddOutlined /> 内存</span></template>
            <div class="hero">
              <span class="hero-value">{{ formatBytes(info.memory.used) }}</span>
              <span class="hero-label">已用 / 共 {{ formatBytes(info.memory.total) }}</span>
            </div>
            <UsageMeter label="物理内存" :percent="info.memory.usage">
              可用 {{ formatBytes(info.memory.free) }}
            </UsageMeter>
            <UsageMeter
              v-if="info.memory.swapTotal > 0"
              label="交换空间"
              :percent="(info.memory.swapUsed / info.memory.swapTotal) * 100"
            >
              {{ formatBytes(info.memory.swapUsed) }} / {{ formatBytes(info.memory.swapTotal) }}
            </UsageMeter>
          </a-card>
        </a-col>

        <!-- JVM 内存 -->
        <a-col :xs="24" :lg="8">
          <a-card :bordered="false" class="page-card metric-card">
            <template #title><span class="card-title"><CoffeeOutlined /> JVM 内存</span></template>
            <div class="hero">
              <span class="hero-value">{{ formatBytes(info.jvm.heapUsed) }}</span>
              <span class="hero-label">堆已用 / 最大 {{ formatBytes(info.jvm.heapMax) }}</span>
            </div>
            <UsageMeter label="堆内存" :percent="info.jvm.heapUsage">
              已提交 {{ formatBytes(info.jvm.heapCommitted) }}
            </UsageMeter>
            <a-descriptions :column="2" size="small" class="desc">
              <a-descriptions-item label="非堆">{{ formatBytes(info.jvm.nonHeapUsed) }}</a-descriptions-item>
              <a-descriptions-item label="已加载类">{{ formatNumber(info.jvm.loadedClassCount) }}</a-descriptions-item>
              <a-descriptions-item label="线程">{{ info.jvm.threadCount }}</a-descriptions-item>
              <a-descriptions-item label="峰值线程">{{ info.jvm.peakThreadCount }}</a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>

        <!-- 服务器信息 -->
        <a-col :xs="24" :xl="12">
          <a-card :bordered="false" class="page-card" title="服务器信息">
            <a-descriptions :column="1" size="small" bordered class="info-desc" :label-style="labelStyle" :content-style="contentStyle">
              <a-descriptions-item label="主机名">{{ info.sys.hostname }}</a-descriptions-item>
              <a-descriptions-item label="服务器 IP">{{ info.sys.ip }}</a-descriptions-item>
              <a-descriptions-item label="操作系统">{{ info.sys.osName }}</a-descriptions-item>
              <a-descriptions-item label="系统架构">{{ info.sys.osArch }}</a-descriptions-item>
              <a-descriptions-item label="系统版本">{{ info.sys.osVersion }}</a-descriptions-item>
              <a-descriptions-item label="系统运行">{{ formatDuration(info.sys.uptime) }}</a-descriptions-item>
              <a-descriptions-item label="时区">{{ info.sys.timezone }}</a-descriptions-item>
              <a-descriptions-item label="进程 ID">{{ info.sys.pid }}</a-descriptions-item>
              <a-descriptions-item label="项目路径">
                {{ info.sys.userDir }}
              </a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>

        <!-- JVM 信息 -->
        <a-col :xs="24" :xl="12">
          <a-card :bordered="false" class="page-card" title="Java 虚拟机">
            <a-descriptions :column="1" size="small" bordered class="info-desc" :label-style="labelStyle" :content-style="contentStyle">
              <a-descriptions-item label="名称">{{ info.jvm.name }}</a-descriptions-item>
              <a-descriptions-item label="版本">{{ info.jvm.version }}</a-descriptions-item>
              <a-descriptions-item label="厂商">{{ info.jvm.vendor }}</a-descriptions-item>
              <a-descriptions-item label="启动时间">{{ info.jvm.startTime }}</a-descriptions-item>
              <a-descriptions-item label="运行时长">{{ formatDuration(info.jvm.uptime) }}</a-descriptions-item>
              <a-descriptions-item label="垃圾回收">
                <div v-for="gc in info.jvm.gcs" :key="gc.name" class="gc">
                  {{ gc.name }}：{{ formatNumber(gc.count) }} 次 / {{ formatNumber(gc.time) }} ms
                </div>
              </a-descriptions-item>
              <a-descriptions-item label="安装路径">
                {{ info.jvm.home }}
              </a-descriptions-item>
              <a-descriptions-item label="启动参数">
                <span v-if="!info.jvm.inputArgs.length" class="sub">无</span>
                <div v-else class="args">
                  <a-tag v-for="arg in info.jvm.inputArgs" :key="arg" :bordered="false">{{ arg }}</a-tag>
                </div>
              </a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>

        <!-- 磁盘 -->
        <a-col :span="24">
          <a-card :bordered="false" class="page-card" title="磁盘">
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
import { CoffeeOutlined, DashboardOutlined, HddOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import UsageMeter from '@/components/monitor/UsageMeter.vue'
import { getServerInfo } from '@/api/monitor/server'
import { formatBytes, formatDuration, formatNumber } from '@/utils/format'

const REFRESH_MS = 5000

// 信息表格：标签不换行，长内容（路径、参数）可折行
const labelStyle = { width: '96px', whiteSpace: 'nowrap' }
const contentStyle = { wordBreak: 'break-all' }

const info = ref(null)
const loading = ref(false)
const updatedAt = ref('')
const auto = ref(true)

const diskColumns = [
  { title: '挂载点', dataIndex: 'mount', width: 200, ellipsis: true },
  { title: '名称', dataIndex: 'name', width: 200, ellipsis: true },
  { title: '文件系统', dataIndex: 'type', width: 110 },
  { title: '可用', key: 'free', width: 120 },
  { title: '使用情况', key: 'usage', width: 320 }
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
