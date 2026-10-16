<template>
  <div class="cache-page">
    <div class="page-card page-head">
      <div>
        <h3>缓存监控</h3>
        <span class="sub">
          {{ isRedis ? `Redis ${redis.redis_version || ''} · ${redis.redis_mode || 'standalone'}` : '内存缓存' }}
          <template v-if="updatedAt"> · 更新于 {{ updatedAt }}</template>
        </span>
      </div>
      <a-button :loading="loading" @click="loadAll">
        <template #icon><ReloadOutlined /></template>刷新
      </a-button>
    </div>

    <a-alert
      v-if="info && !isRedis"
      type="info"
      show-icon
      class="mode-alert"
      message="当前使用内存缓存（lemon.cache.type = memory），Redis 运行指标不可用"
      description="内存缓存仅适用于本地体验与测试。生产环境请使用 Redis，下方的缓存管理在两种模式下都可以使用。"
    />

    <!-- 指标 -->
    <a-row v-if="isRedis" :gutter="[16, 16]" class="stats">
      <a-col v-for="s in stats" :key="s.label" :xs="12" :md="8" :xl="4">
        <div class="page-card stat">
          <div class="stat-label">{{ s.label }}</div>
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-sub">{{ s.sub }}</div>
        </div>
      </a-col>
    </a-row>

    <a-row v-if="isRedis" :gutter="[16, 16]" class="section">
      <a-col :xs="24" :xl="12">
        <a-card :bordered="false" class="page-card full" title="基本信息">
          <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" bordered :label-style="{ width: '96px', whiteSpace: 'nowrap' }">
            <a-descriptions-item label="版本">{{ redis.redis_version }}</a-descriptions-item>
            <a-descriptions-item label="运行模式">{{ redis.redis_mode }}</a-descriptions-item>
            <a-descriptions-item label="端口">{{ redis.tcp_port }}</a-descriptions-item>
            <a-descriptions-item label="角色">{{ redis.role }}</a-descriptions-item>
            <a-descriptions-item label="操作系统">{{ redis.os }}</a-descriptions-item>
            <a-descriptions-item label="运行时长">{{ formatDuration(Number(redis.uptime_in_seconds)) }}</a-descriptions-item>
            <a-descriptions-item label="已用内存">{{ redis.used_memory_human }}</a-descriptions-item>
            <a-descriptions-item label="内存峰值">{{ redis.used_memory_peak_human }}</a-descriptions-item>
            <a-descriptions-item label="内存上限">
              {{ redis.maxmemory_human === '0B' ? '不限制' : redis.maxmemory_human }}
            </a-descriptions-item>
            <a-descriptions-item label="碎片率">{{ redis.mem_fragmentation_ratio }}</a-descriptions-item>
            <a-descriptions-item label="淘汰策略">{{ redis.maxmemory_policy }}</a-descriptions-item>
            <a-descriptions-item label="AOF">{{ redis.aof_enabled === '1' ? '开启' : '关闭' }}</a-descriptions-item>
            <a-descriptions-item label="RDB 最近保存">{{ redis.rdb_last_bgsave_status }}</a-descriptions-item>
            <a-descriptions-item label="网络流量">
              入 {{ formatBytes(Number(redis.total_net_input_bytes)) }} / 出 {{ formatBytes(Number(redis.total_net_output_bytes)) }}
            </a-descriptions-item>
            <a-descriptions-item label="过期 Key">{{ formatNumber(Number(redis.expired_keys)) }}</a-descriptions-item>
            <a-descriptions-item label="淘汰 Key">{{ formatNumber(Number(redis.evicted_keys)) }}</a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="12">
        <a-card :bordered="false" class="page-card full" title="命令统计">
          <template #extra>
            <a-segmented v-model:value="cmdView" :options="['图表', '表格']" size="small" />
          </template>
          <CommandStatsChart v-if="cmdView === '图表'" :data="info.commandStats" />
          <a-table
            v-else
            size="small"
            row-key="name"
            :columns="cmdColumns"
            :data-source="info.commandStats"
            :pagination="{ pageSize: 8, size: 'small', hideOnSinglePage: true }"
          />
        </a-card>
      </a-col>
    </a-row>

    <!-- 缓存管理 -->
    <a-row :gutter="[16, 16]" class="section">
      <a-col :xs="24" :lg="7">
        <a-card :bordered="false" class="page-card manage-card" title="缓存列表">
          <div class="list">
            <div
              v-for="c in names"
              :key="c.prefix"
              class="list-item"
              :class="{ active: c.prefix === currentPrefix }"
              tabindex="0"
              @click="selectName(c.prefix)"
              @keydown.enter="selectName(c.prefix)"
            >
              <div class="item-main">
                <div class="item-title">
                  {{ c.name }}
                  <a-badge :count="c.count" :show-zero="true" :overflow-count="9999" class="count" />
                </div>
                <div class="item-sub"><code>{{ c.prefix }}*</code></div>
                <div class="item-sub">{{ c.remark }}</div>
              </div>
              <span v-perm="'monitor:cache:remove'" @click.stop>
                <a-popconfirm :title="`确定清空「${c.name}」的全部缓存？`" @confirm="clear(c)">
                  <a-tooltip title="清空">
                    <a-button type="text" size="small" danger><template #icon><ClearOutlined /></template></a-button>
                  </a-tooltip>
                </a-popconfirm>
              </span>
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="7">
        <a-card :bordered="false" class="page-card manage-card" :title="`键名列表${keys.length ? `（${keys.length}）` : ''}`">
          <template #extra>
            <a-tooltip title="刷新">
              <a-button type="text" size="small" :disabled="!currentPrefix" @click="loadKeys">
                <template #icon><ReloadOutlined /></template>
              </a-button>
            </a-tooltip>
          </template>
          <a-input
            v-if="keys.length"
            v-model:value="keyFilter"
            placeholder="筛选键名"
            allow-clear
            size="small"
            class="key-filter"
          />
          <a-empty v-if="!filteredKeys.length" :image="simpleImage" :description="currentPrefix ? '没有缓存' : '请选择左侧缓存'" />
          <div v-else class="list">
            <div
              v-for="k in filteredKeys"
              :key="k"
              class="list-item key-item"
              :class="{ active: k === currentKey }"
              tabindex="0"
              @click="selectKey(k)"
              @keydown.enter="selectKey(k)"
            >
              <span class="key-text" :title="k">{{ k.slice(currentPrefix.length) || k }}</span>
              <span v-perm="'monitor:cache:remove'" @click.stop>
                <a-popconfirm title="确定删除该缓存？" @confirm="removeKey(k)">
                  <a-button type="text" size="small" danger><template #icon><DeleteOutlined /></template></a-button>
                </a-popconfirm>
              </span>
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="10">
        <a-card :bordered="false" class="page-card manage-card" title="缓存内容">
          <a-empty v-if="!value" :image="simpleImage" description="请选择键名查看内容" />
          <template v-else>
            <a-descriptions :column="1" size="small" class="value-desc">
              <a-descriptions-item label="键名">
                <a-typography-text copyable :content="value.key" code />
              </a-descriptions-item>
              <a-descriptions-item label="过期时间">
                {{ value.ttl < 0 ? '永不过期' : `${formatDuration(value.ttl)}后过期` }}
              </a-descriptions-item>
            </a-descriptions>
            <pre class="code-block value-json">{{ prettyJson(value.value) }}</pre>
          </template>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { Empty, message } from 'ant-design-vue'
import { ClearOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import CommandStatsChart from '@/components/monitor/CommandStatsChart.vue'
import {
  clearCache,
  deleteCacheKey,
  getCacheInfo,
  getCacheValue,
  listCacheKeys,
  listCacheNames
} from '@/api/monitor/cache'
import { formatBytes, formatDuration, formatNumber, prettyJson } from '@/utils/format'

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE

const info = ref(null)
const loading = ref(false)
const updatedAt = ref('')
const cmdView = ref('图表')

const isRedis = computed(() => info.value?.type === 'redis')
const redis = computed(() => info.value?.info || {})

const stats = computed(() => {
  const r = redis.value
  const hits = Number(r.keyspace_hits || 0)
  const misses = Number(r.keyspace_misses || 0)
  const hitRate = hits + misses ? ((hits / (hits + misses)) * 100).toFixed(1) + '%' : '-'
  return [
    { label: 'Key 总数', value: formatNumber(info.value.dbSize), sub: `过期 ${formatNumber(Number(r.expired_keys || 0))}` },
    { label: '已用内存', value: r.used_memory_human, sub: `峰值 ${r.used_memory_peak_human}` },
    { label: '客户端连接', value: formatNumber(Number(r.connected_clients || 0)), sub: `拒绝 ${formatNumber(Number(r.rejected_connections || 0))}` },
    { label: '每秒操作', value: formatNumber(Number(r.instantaneous_ops_per_sec || 0)), sub: `累计 ${formatNumber(Number(r.total_commands_processed || 0))}` },
    { label: '命中率', value: hitRate, sub: `命中 ${formatNumber(hits)} / 未命中 ${formatNumber(misses)}` },
    { label: '运行时长', value: formatDuration(Number(r.uptime_in_seconds)).replace(/ \d+ 分$/, ''), sub: `端口 ${r.tcp_port}` }
  ]
})

const cmdColumns = [
  { title: '命令', dataIndex: 'name' },
  { title: '调用次数', dataIndex: 'calls', align: 'right', customRender: ({ text }) => formatNumber(text) },
  { title: '累计耗时 (μs)', dataIndex: 'usec', align: 'right', customRender: ({ text }) => formatNumber(text) },
  { title: '平均耗时 (μs)', dataIndex: 'usecPerCall', align: 'right', customRender: ({ text }) => text.toFixed(2) }
]

// 缓存管理
const names = ref([])
const currentPrefix = ref('')
const keys = ref([])
const keyFilter = ref('')
const currentKey = ref('')
const value = ref(null)
const filteredKeys = computed(() => (keyFilter.value ? keys.value.filter((k) => k.includes(keyFilter.value)) : keys.value))

async function loadAll() {
  loading.value = true
  try {
    const [i, n] = await Promise.all([getCacheInfo(), listCacheNames()])
    info.value = i
    names.value = n
    updatedAt.value = dayjs().format('HH:mm:ss')
    if (currentPrefix.value) await loadKeys()
  } finally {
    loading.value = false
  }
}

async function selectName(prefix) {
  currentPrefix.value = prefix
  keyFilter.value = ''
  value.value = null
  currentKey.value = ''
  await loadKeys()
}

async function loadKeys() {
  keys.value = await listCacheKeys(currentPrefix.value)
}

async function selectKey(key) {
  currentKey.value = key
  try {
    value.value = await getCacheValue(key)
  } catch (e) {
    // 已过期：从列表移除
    value.value = null
    keys.value = keys.value.filter((k) => k !== key)
    throw e
  }
}

async function clear(c) {
  await clearCache(c.prefix)
  message.success(`已清空「${c.name}」`)
  if (c.prefix === currentPrefix.value) value.value = null
  await loadAll()
}

async function removeKey(key) {
  await deleteCacheKey(key)
  message.success('已删除')
  if (key === currentKey.value) value.value = null
  keys.value = keys.value.filter((k) => k !== key)
  names.value = await listCacheNames()
}

onMounted(loadAll)
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding: 16px 24px;
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
.mode-alert,
.stats,
.section {
  margin-top: 16px;
}
.stat {
  padding: 16px 20px;
  background: var(--app-card);
  height: 100%;
}
.stat-label {
  color: var(--app-text-3);
  font-size: 12px;
}
.stat-value {
  margin: 4px 0 2px;
  font-size: 22px;
  font-weight: 600;
  color: var(--app-text);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.stat-sub {
  color: var(--app-text-3);
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.full {
  height: 100%;
}
.manage-card {
  height: 100%;
}
.manage-card :deep(.ant-card-body) {
  padding: 12px;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 420px;
  overflow: auto;
}
.list-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid transparent;
  cursor: pointer;
  outline: none;
  transition: background 0.2s;
}
.list-item:hover,
.list-item:focus-visible {
  background: var(--app-hover);
}
.list-item.active {
  background: var(--app-primary-bg);
  border-color: var(--app-primary);
}
.item-main {
  min-width: 0;
}
.item-title {
  font-weight: 500;
  color: var(--app-text);
  display: flex;
  align-items: center;
  gap: 8px;
}
.count :deep(.ant-badge-count) {
  background: var(--app-hover);
  color: var(--app-text-2);
  box-shadow: none;
}
.item-sub {
  margin-top: 2px;
  color: var(--app-text-3);
  font-size: 12px;
}
.item-sub code {
  font-size: 12px;
}
.key-filter {
  margin-bottom: 8px;
}
.key-item {
  align-items: center;
  padding: 6px 8px 6px 12px;
}
.key-text {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.value-desc {
  margin-bottom: 8px;
}
.value-json {
  max-height: 340px;
  overflow: auto;
  margin: 0;
}
</style>
