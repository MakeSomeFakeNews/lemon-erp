<template>
  <!-- 运算信息栏：选择运算记录（最近的在前，下拉到底自动加载更多），显示运算人、时间、需求来源和参与计划 -->
  <div class="run-bar">
    <a-select
      :value="modelValue"
      show-search
      option-label-prop="label"
      :filter-option="filterRun"
      :options="options"
      :loading="loading"
      :placeholder="$t('mes.mrp.runPh')"
      :not-found-content="loading ? undefined : $t('mes.mrp.noRun')"
      popup-class-name="mrp-run-popup"
      :dropdown-match-select-width="440"
      class="run-select"
      @change="(v) => emit('update:modelValue', v)"
      @popup-scroll="onPopupScroll"
    >
      <template #option="{ run }">
        <div class="run-option">
          <span class="run-no">{{ run.runNo }}</span>
          <span class="sub-text">{{ shortTime(run.createTime) }}</span>
          <span v-if="run.remark" class="sub-text ellipsis remark">{{ run.remark }}</span>
          <span v-if="run.pendingCount" class="pending">{{ $t('mes.mrp.pendingN', { n: run.pendingCount }) }}</span>
          <span v-else class="sub-text pending">{{ $t('mes.mrp.noPending') }}</span>
        </div>
      </template>
    </a-select>

    <template v-if="current">
      <span class="meta">
        <UserOutlined />{{ current.createByName || '-' }}
        <ClockCircleOutlined class="gap" />{{ current.createTime }}
      </span>
      <span v-if="sourceTags.length" class="meta">
        <a-tag v-for="s in sourceTags" :key="s.text" :color="s.color" :bordered="false">{{ s.text }}</a-tag>
      </span>
      <span v-if="planNos.length" class="meta">
        <span class="label">{{ $t('mes.mrp.planNos') }}</span>
        <a-tag v-for="no in planNos.slice(0, PLAN_SHOWN)" :key="no" :bordered="false" class="plan-tag">{{ no }}</a-tag>
        <a-popover v-if="planNos.length > PLAN_SHOWN" :title="$t('mes.mrp.planNosTitle', { n: planNos.length })" placement="bottom">
          <template #content>
            <div class="plan-all">
              <a-tag v-for="no in planNos" :key="no" :bordered="false">{{ no }}</a-tag>
            </div>
          </template>
          <a-tag :bordered="false" class="more-tag">+{{ planNos.length - PLAN_SHOWN }}</a-tag>
        </a-popover>
      </span>
    </template>

    <span class="actions">
      <a-tooltip :title="$t('mes.mrp.runsTip')" placement="bottomRight">
        <QuestionCircleOutlined class="help" />
      </a-tooltip>
      <a-tooltip :title="$t('common.action.refresh')">
        <a-button type="text" size="small" @click="emit('refresh')">
          <template #icon><ReloadOutlined /></template>
        </a-button>
      </a-tooltip>
    </span>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ClockCircleOutlined, QuestionCircleOutlined, ReloadOutlined, UserOutlined } from '@ant-design/icons-vue'
import { pageMrpRun } from '@/api/mes/plan'

const props = defineProps({ modelValue: Number })
const emit = defineEmits(['update:modelValue', 'refresh', 'loaded'])
const { t } = useI18n()
const route = useRoute()

/** 每次加载的运算记录数、信息栏直接显示的计划数（其余收进 +N） */
const PAGE_SIZE = 20
const PLAN_SHOWN = 3
/** 打开 ?runId= 指定的运算时最多向后查找的页数 */
const MAX_SEEK_PAGES = 10

const runs = ref([])
const total = ref(0)
const page = ref(0)
const loading = ref(false)
const hasMore = computed(() => runs.value.length < total.value)

async function loadPage(n) {
  loading.value = true
  try {
    const res = await pageMrpRun({ current: n, size: PAGE_SIZE })
    runs.value = n === 1 ? res.records : [...runs.value, ...res.records.filter((r) => !runs.value.some((x) => x.id === r.id))]
    total.value = res.total
    page.value = n
  } finally {
    loading.value = false
  }
}

function onPopupScroll(e) {
  const el = e.target
  if (!loading.value && hasMore.value && el.scrollTop + el.clientHeight >= el.scrollHeight - 24) loadPage(page.value + 1)
}

const options = computed(() => runs.value.map((r) => ({ value: r.id, label: r.runNo, run: r })))
const filterRun = (input, option) => {
  const kw = input.trim().toLowerCase()
  return option.run.runNo.toLowerCase().includes(kw) || (option.run.remark || '').toLowerCase().includes(kw)
}
const shortTime = (s) => (s ? s.slice(0, 16) : '')

const current = computed(() => runs.value.find((r) => r.id === props.modelValue))
const planNos = computed(() => (current.value?.planNos ? current.value.planNos.split(',').filter(Boolean) : []))

/**
 * 需求来源：后端以中文名称保存（如「生产计划、销售预测 3 个月」），按中文语言包识别后显示为当前语言，识别不了的原样显示
 */
const SOURCE_COLORS = { plan: 'blue', order: 'green', forecast: 'purple' }
const zh = (key, named = {}) => t(key, named, { locale: 'zh-CN' })
const sourceTags = computed(() => {
  const text = current.value?.sources
  if (!text) return []
  return text.split(zh('mes.workCenter.sep')).map((s) => s.trim()).filter(Boolean).map((s) => {
    for (const kind of Object.keys(SOURCE_COLORS)) {
      const n = s.match(/\d+/)?.[0]
      if (s === zh(`mes.mrp.source.${kind}`, { n })) return { text: t(`mes.mrp.source.${kind}`, { n }), color: SOURCE_COLORS[kind] }
    }
    return { text: s }
  })
})

/** 刷新运算记录（下达后待下达数变化）；保留已加载的页数 */
async function reload() {
  const pages = Math.max(page.value, 1)
  await loadPage(1)
  for (let n = 2; n <= pages && hasMore.value; n++) await loadPage(n)
}

onMounted(async () => {
  await loadPage(1)
  const wanted = Number(route.query.runId)
  // ?runId= 指定的运算不在第一页时向后查找
  while (wanted && !runs.value.some((r) => r.id === wanted) && hasMore.value && page.value < MAX_SEEK_PAGES) await loadPage(page.value + 1)
  const id = runs.value.some((r) => r.id === wanted) ? wanted : runs.value[0]?.id
  emit('loaded', runs.value.length > 0)
  if (id) emit('update:modelValue', id)
})

defineExpose({ reload, current })
</script>

<style scoped>
.run-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 16px;
  margin-bottom: 12px;
}
.run-select {
  width: 220px;
}
.run-select :deep(.ant-select-selection-item) {
  font-weight: 600;
}
.meta {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  color: var(--app-text-2);
}
.meta .anticon {
  color: var(--app-text-3);
  margin-right: 2px;
}
.meta .gap {
  margin-left: 10px;
}
.meta .ant-tag {
  margin-inline-end: 0;
}
.label {
  color: var(--app-text-3);
  margin-right: 2px;
}
.plan-tag,
.more-tag {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
}
.more-tag {
  cursor: pointer;
  color: var(--app-primary);
}
.plan-all {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-width: 420px;
}
.plan-all .ant-tag {
  margin-inline-end: 0;
}
.actions {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.help {
  color: var(--app-text-3);
  cursor: help;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

<style>
.mrp-run-popup .run-option {
  display: flex;
  align-items: center;
  gap: 8px;
}
.mrp-run-popup .run-option .run-no {
  font-weight: 500;
}
.mrp-run-popup .run-option .remark {
  min-width: 0;
  flex: 1;
}
.mrp-run-popup .run-option .pending {
  margin-left: auto;
  color: #fa8c16;
  font-size: 12px;
}
.mrp-run-popup .run-option .sub-text.pending {
  color: var(--app-text-3);
}
.mrp-run-popup .sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.mrp-run-popup .ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
