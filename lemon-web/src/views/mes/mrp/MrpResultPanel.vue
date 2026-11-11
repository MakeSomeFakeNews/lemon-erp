<template>
  <!-- 运算结果：运算信息栏 → 按状态汇总（点击筛选）→ 筛选行 → 结果表 → 勾选后出现的下达栏 -->
  <div class="mrp-result">
    <MrpRunBar ref="runBarRef" v-model="runId" @loaded="(has) => (hasRuns = has)" @refresh="refresh" />

    <a-empty v-if="hasRuns === false" class="no-run" :description="$t('mes.mrp.noRun')">
      <div class="sub-text">{{ $t('mes.mrp.runsTip') }}</div>
      <a-button v-if="userStore.hasPerm('mes:plan:list')" type="primary" class="to-plan" @click="router.push('/mes/plan')">
        {{ $t('mes.mrp.toPlan') }}
      </a-button>
    </a-empty>

    <template v-else>
      <div class="summary">
        <div
          v-for="c in cards"
          :key="c.key"
          class="summary-card"
          :class="[c.count ? `is-${c.key.toLowerCase()}` : '', { active: status === c.key }]"
          @click="toggleStatus(c.key)"
        >
          <div class="summary-label">{{ c.label }}</div>
          <div class="summary-value">{{ c.count }}</div>
          <div class="summary-sub">{{ c.sub || ' ' }}</div>
        </div>
      </div>

      <div class="filters">
        <a-segmented v-model:value="status" :options="statusOptions" />
        <a-segmented v-model:value="supplyType" :options="typeOptions" />
        <a-input-search v-model:value="keyword" :placeholder="$t('mes.mrp.keywordPh')" allow-clear class="keyword" />
        <span class="sub-text total">{{ $t('mes.mrp.total', { n: filtered.length }) }}</span>
      </div>

      <MrpResultTable
        v-model:selected="selected"
        :results="filtered"
        :loading="loading"
        :empty-text="results.length ? $t('mes.mrp.noMatch') : $t('mes.mrp.noDemand')"
        :can-release="canRelease"
        @reload="loadResults"
      />

      <transition name="batch">
        <div v-if="selected.length" class="batch-bar">
          <span class="batch-count">
            <b>{{ $t('mes.mrp.selectedN', { n: selected.length }) }}</b>
            <span v-if="selectedBreakdown" class="breakdown">{{ selectedBreakdown }}</span>
          </span>
          <a @click="selected = []">{{ $t('common.action.clear') }}</a>
          <span class="batch-right">
            <span class="batch-label">{{ $t('mes.mrp.warehouse') }}</span>
            <WarehouseSelect v-model:value="warehouseId" :placeholder="$t('mes.mrp.warehousePh')" class="warehouse" />
            <a-tooltip :title="$t('mes.mrp.releaseContent')">
              <QuestionCircleOutlined class="help" />
            </a-tooltip>
            <a-button type="primary" :loading="releasing" @click="release">
              <template #icon><SendOutlined /></template>{{ $t('mes.mrp.release') }}
            </a-button>
          </span>
        </div>
      </transition>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { QuestionCircleOutlined, SendOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { mrpResults, releaseMrp } from '@/api/mes/plan'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { MrpResultStatus, SupplyType } from '@/constants/enums'
import MrpRunBar from './MrpRunBar.vue'
import MrpResultTable from './MrpResultTable.vue'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const dict = useDict('mes_supply_type')
const canRelease = computed(() => userStore.hasPerm('mes:mrp:release'))

/** 筛选「全部」的取值 */
const ALL = 'ALL'
const runBarRef = ref()
const hasRuns = ref()
const runId = ref()
const results = ref([])
const loading = ref(false)
const selected = ref([])
const warehouseId = ref()

const status = ref(MrpResultStatus.NEW)
const supplyType = ref(ALL)
const keyword = ref('')

async function loadResults() {
  if (!runId.value) return
  loading.value = true
  try {
    results.value = await mrpResults(runId.value)
    const pending = new Set(results.value.filter((r) => r.status === MrpResultStatus.NEW).map((r) => r.id))
    selected.value = selected.value.filter((id) => pending.has(id))
  } finally {
    loading.value = false
  }
}

// 切换运算：有待下达的建议时默认只看待下达，否则看全部
watch(runId, async () => {
  selected.value = []
  supplyType.value = ALL
  keyword.value = ''
  await loadResults()
  status.value = results.value.some((r) => r.status === MrpResultStatus.NEW) ? MrpResultStatus.NEW : ALL
})

function refresh() {
  runBarRef.value?.reload()
  loadResults()
}

const typeLabel = (v) => dict.mes_supply_type.find((d) => d.value === v)?.label || v
const countBy = (list, key) => list.reduce((m, r) => ((m[r[key]] = (m[r[key]] || 0) + 1), m), {})
const statusCount = computed(() => countBy(results.value, 'status'))
/** 「自制 6 · 采购 24」：按供应方式统计，0 项的不显示 */
const breakdown = (list, sep) => {
  const c = countBy(list, 'supplyType')
  return Object.values(SupplyType).filter((v) => c[v]).map((v) => `${typeLabel(v)} ${c[v]}`).join(sep)
}

const cards = computed(() =>
  [MrpResultStatus.NEW, MrpResultStatus.RELEASED, MrpResultStatus.NONE, MrpResultStatus.EXPIRED].map((key) => ({
    key,
    label: t(`mes.mrp.status.${key}`),
    count: statusCount.value[key] || 0,
    sub: key === MrpResultStatus.NEW ? breakdown(results.value.filter((r) => r.status === key), ' · ') : ''
  }))
)
/** 点击汇总卡片筛选该状态，再次点击恢复全部 */
function toggleStatus(key) {
  status.value = status.value === key ? ALL : key
}

// 状态分段：待下达 / 已下达 / 全部；从汇总卡片选了无需处理、已作废时临时加上该项
const statusOptions = computed(() => {
  const keys = [MrpResultStatus.NEW, MrpResultStatus.RELEASED]
  if (![...keys, ALL].includes(status.value)) keys.push(status.value)
  return [...keys.map((k) => ({ value: k, label: t(`mes.mrp.status.${k}`) })), { value: ALL, label: t('mes.mrp.all') }]
})
const typeOptions = computed(() => [
  { value: ALL, label: t('mes.mrp.all') },
  ...Object.values(SupplyType).map((v) => ({ value: v, label: typeLabel(v) }))
])

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return results.value.filter(
    (r) =>
      (status.value === ALL || r.status === status.value) &&
      (supplyType.value === ALL || r.supplyType === supplyType.value) &&
      (!kw || `${r.materialCode} ${r.materialName}`.toLowerCase().includes(kw))
  )
})

const selectedBreakdown = computed(() => {
  const ids = new Set(selected.value)
  return breakdown(results.value.filter((r) => ids.has(r.id)), t('mes.workCenter.sep'))
})

const releasing = ref(false)
function release() {
  Modal.confirm({
    title: t('mes.mrp.releaseConfirm', { n: selected.value.length }),
    content: t('mes.mrp.releaseContent'),
    async onOk() {
      releasing.value = true
      try {
        const nos = await releaseMrp({ resultIds: selected.value, warehouseId: warehouseId.value })
        message.success(t('mes.mrp.generated', { nos: nos.join(t('mes.workCenter.sep')) }))
        selected.value = []
        refresh()
      } finally {
        releasing.value = false
      }
    }
  })
}
</script>

<style scoped>
.mrp-result {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}
.summary-card {
  padding: 10px 14px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}
.summary-card:hover {
  border-color: var(--app-primary);
}
.summary-card.active {
  border-color: var(--app-primary);
  background: var(--app-primary-bg);
}
.summary-label {
  color: var(--app-text-2);
  font-size: 13px;
}
.summary-value {
  font-size: 22px;
  font-weight: 600;
  line-height: 1.4;
  font-variant-numeric: tabular-nums;
}
.breakdown {
  margin-left: 8px;
  color: var(--app-text-2);
}
.is-new .summary-value {
  color: #fa8c16;
}
.is-released .summary-value {
  color: #52c41a;
}
.summary-sub {
  color: var(--app-text-3);
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-bottom: 12px;
}
.keyword {
  width: 220px;
}
.total {
  margin-left: auto;
}
.batch-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-top: 12px;
  padding: 10px 16px;
  border-radius: 8px;
  background: var(--app-primary-bg);
  border: 1px solid var(--app-primary);
}
.batch-right {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.batch-label {
  color: var(--app-text-2);
}
.warehouse {
  width: 220px;
}
.help {
  color: var(--app-text-3);
  cursor: help;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.no-run {
  margin: 48px 0;
}
.to-plan {
  margin-top: 16px;
}
.batch-enter-active,
.batch-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}
.batch-enter-from,
.batch-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
@media (max-width: 767px) {
  .summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
