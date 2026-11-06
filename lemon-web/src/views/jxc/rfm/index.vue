<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <div class="filters">
        <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" @change="load" />
        <a-input v-model:value="region" allow-clear :placeholder="$t('jxc.common.region')" class="filter-s" @press-enter="load" />
        <a-input v-model:value="keyword" allow-clear :placeholder="$t('jxc.common.customerKeyword')" class="filter" @press-enter="load" />
        <a-button type="primary" :loading="loading" @click="load">{{ $t('common.action.query') }}</a-button>
        <span v-if="data.rows.length" class="sub-text">{{ $t('jxc.rfm.avg', { r: data.avgR, f: data.avgF, m: data.avgM }) }}</span>
      </div>
      <div class="segments">
        <div class="seg" :class="{ active: !segment }" @click="segment = undefined">
          <div class="seg-name">{{ $t('jxc.rfm.all') }}</div>
          <div class="sub-text">{{ $t('jxc.rfm.count', { n: data.rows.length }) }}</div>
        </div>
        <a-tooltip v-for="s in data.segments" :key="s.segment" :title="$t(`jxc.rfm.segDesc.${s.segment}`)">
          <div class="seg" :class="{ active: segment === s.segment, empty: !s.count }" @click="s.count && (segment = s.segment)">
            <div class="seg-name"><span class="dot" :style="{ background: SEGMENT_COLORS[s.segment] }" />{{ $t(`jxc.rfm.seg.${s.segment}`) }}</div>
            <div class="sub-text">{{ $t('jxc.rfm.count', { n: s.count }) }} · ¥{{ money(s.monetary) }}</div>
          </div>
        </a-tooltip>
      </div>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <div class="head">
        <span class="sub-text">{{ $t('jxc.rfm.tip') }}</span>
        <a-button :disabled="!filtered.length" @click="exportRows">
          <template #icon><DownloadOutlined /></template>{{ $t('common.action.export') }}
        </a-button>
      </div>
      <a-table
        row-key="customerId"
        size="middle"
        :columns="columns"
        :data-source="filtered"
        :loading="loading"
        :pagination="{ pageSize: 50, showSizeChanger: false, showTotal: (n) => $t('common.table.total', { total: n }) }"
        :scroll="{ x: 1000, y: '100%' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'customer'">
            <a v-if="canDrill" @click="drill(record)">{{ record.name }}</a>
            <span v-else>{{ record.name }}</span>
            <div class="sub-text">{{ record.code }}</div>
          </template>
          <template v-else-if="column.key === 'monetary'">{{ money(record.monetary) }}</template>
          <template v-else-if="column.key === 'score'">
            <span class="score">R{{ record.r }}</span><span class="score">F{{ record.f }}</span><span class="score">M{{ record.m }}</span>
          </template>
          <template v-else-if="column.key === 'segment'">
            <a-tag :color="SEGMENT_COLORS[record.segment]" :bordered="false">{{ $t(`jxc.rfm.seg.${record.segment}`) }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-card>

    <JxcDetail ref="detailRef" report="sales" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import JxcDetail from '@/components/bi/jxc/JxcDetail.vue'
import { jxcRfm } from '@/api/bi/jxc'
import { last12Months, rangePresets } from '@/components/bi/jxc/jxc'
import { useUserStore } from '@/store/user'
import { money } from '@/utils/number'
import { writeExcel } from '@/utils/excel'
import { JxcDimension, RfmSegment } from '@/constants/enums'
import { t } from '@/locales'

const SEGMENT_COLORS = {
  [RfmSegment.IMPORTANT_VALUE]: '#f5222d',
  [RfmSegment.IMPORTANT_DEVELOP]: '#fa8c16',
  [RfmSegment.IMPORTANT_KEEP]: '#1677ff',
  [RfmSegment.IMPORTANT_RETAIN]: '#722ed1',
  [RfmSegment.GENERAL_VALUE]: '#52c41a',
  [RfmSegment.GENERAL_DEVELOP]: '#13c2c2',
  [RfmSegment.GENERAL_KEEP]: '#2f54eb',
  [RfmSegment.GENERAL_RETAIN]: '#8c8c8c'
}

const canDrill = useUserStore().hasPerm('bi:jxc:sales')
const presets = rangePresets()
/** RFM 需要一段较长的历史，默认近 12 个月 */
const range = ref(last12Months())
const region = ref('')
const keyword = ref('')
const segment = ref()
const data = ref({ rows: [], segments: [] })
const loading = ref(false)
const filtered = computed(() => (segment.value ? data.value.rows.filter((r) => r.segment === segment.value) : data.value.rows))

const columns = [
  { title: t('jxc.rfm.customer'), key: 'customer', width: 220, fixed: 'left', ellipsis: true },
  { title: t('jxc.rfm.lastDate'), dataIndex: 'lastDate', width: 110 },
  { title: t('jxc.rfm.recency'), dataIndex: 'recency', width: 100, align: 'right', sorter: (a, b) => a.recency - b.recency },
  { title: t('jxc.rfm.frequency'), dataIndex: 'frequency', width: 100, align: 'right', sorter: (a, b) => a.frequency - b.frequency },
  { title: t('jxc.rfm.monetary'), key: 'monetary', width: 130, align: 'right', sorter: (a, b) => Number(a.monetary) - Number(b.monetary) },
  { title: t('jxc.rfm.score'), key: 'score', width: 150 },
  { title: t('jxc.rfm.segment'), key: 'segment', width: 120 }
]

async function load() {
  loading.value = true
  try {
    const p = { begin: range.value[0], end: range.value[1], region: region.value?.trim() || undefined, keyword: keyword.value?.trim() || undefined }
    data.value = await jxcRfm(p)
    segment.value = undefined
  } finally {
    loading.value = false
  }
}

const detailRef = ref()
function drill(r) {
  detailRef.value.open({ params: { begin: range.value[0], end: range.value[1] }, dim: JxcDimension.CUSTOMER, key: r.customerId, label: r.name })
}

function exportRows() {
  const headers = [t('common.field.code'), t('jxc.rfm.customer'), t('jxc.rfm.lastDate'), t('jxc.rfm.recency'), t('jxc.rfm.frequency'),
    t('jxc.rfm.monetary'), 'R', 'F', 'M', t('jxc.rfm.segment')].map((title) => ({ title }))
  const rows = filtered.value.map((r) => [r.code, r.name, r.lastDate, r.recency, r.frequency, Number(r.monetary), r.r, r.f, r.m,
    t(`jxc.rfm.seg.${r.segment}`)])
  writeExcel(`RFM_${range.value[0]}_${range.value[1]}`, headers, rows, { withStamp: false })
}

onMounted(load)
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
}
.filter {
  width: 220px;
}
.filter-s {
  width: 150px;
}
.segments {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(128px, 1fr));
  gap: 8px;
  margin-top: 12px;
}
.seg {
  padding: 8px 12px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  cursor: pointer;
}
.seg.active {
  border-color: var(--ant-color-primary, #1677ff);
  background: var(--app-primary-bg);
}
.seg.empty {
  opacity: 0.5;
  cursor: default;
}
.seg-name {
  font-weight: 500;
}
.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.score {
  display: inline-block;
  min-width: 30px;
  margin-right: 4px;
  font-family: monospace;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
