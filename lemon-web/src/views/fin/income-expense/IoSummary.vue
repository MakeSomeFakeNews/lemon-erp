<template>
  <div class="summary">
    <div class="filters">
      <a-range-picker v-model:value="range" :allow-clear="false" value-format="YYYY-MM-DD" />
      <a-radio-group v-model:value="direction" button-style="solid">
        <a-radio-button value="">{{ $t('fin.io.allTypes') }}</a-radio-button>
        <a-radio-button :value="FundDirection.IN">{{ $t('fin.io.typeIn') }}</a-radio-button>
        <a-radio-button :value="FundDirection.OUT">{{ $t('fin.io.typeOut') }}</a-radio-button>
      </a-radio-group>
      <span class="sub-text">{{ $t('fin.io.summaryTip') }}</span>
    </div>
    <a-row :gutter="16">
      <a-col :xs="24" :lg="10">
        <a-table
          row-key="key"
          size="small"
          bordered
          :columns="summaryColumns"
          :data-source="rows"
          :loading="loading"
          :pagination="false"
          :row-class-name="(r) => (r.key === selectedKey ? 'active-row' : '')"
          :custom-row="(r) => ({ onClick: () => selectCategory(r) })"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'category'">
              <a>{{ record.transferFee ? $t('fin.io.transferFee') : record.categoryName }}</a>
              <div class="sub-text">{{ record.accountCode }} {{ record.accountName }}</div>
            </template>
            <template v-else-if="column.key === 'direction'"><DictTag type="fin_fund_direction" :value="record.direction" /></template>
            <template v-else-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template>
          </template>
          <template v-if="rows.length" #summary>
            <a-table-summary-row class="total-row">
              <a-table-summary-cell :col-span="2">
                <span class="gap">{{ $t('fin.io.totalIn', { amount: formatMoney(totalIn) }) }}</span>{{ $t('fin.io.totalOut', { amount: formatMoney(totalOut) }) }}
              </a-table-summary-cell>
              <a-table-summary-cell align="right">{{ totalCount }}</a-table-summary-cell>
              <a-table-summary-cell align="right">{{ formatMoney(totalIn - totalOut) }}</a-table-summary-cell>
            </a-table-summary-row>
          </template>
        </a-table>
      </a-col>
      <a-col :xs="24" :lg="14">
        <div class="lines-head">
          {{ selectedName ? $t('fin.io.linesOf', { name: selectedName }) : $t('fin.io.allLines') }}
          <a-button v-if="selectedKey" type="link" size="small" @click="selectCategory()">{{ $t('fin.io.showAll') }}</a-button>
        </div>
        <a-table
          :row-key="(r) => (r.transferId ? `t${r.transferId}` : r.id)"
          size="small"
          :columns="lineColumns"
          :data-source="lines"
          :loading="linesLoading"
          :pagination="pagination"
          :scroll="{ x: 820 }"
          @change="onPage"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'docNo'">
              <!-- 调拨手续费：跳转到账户调拨打开详情（没有查看权限时只显示单号） -->
              <template v-if="record.transferId">
                <a v-if="canViewTransfer" @click="openTransfer(record.transferId)">{{ record.docNo }}</a>
                <span v-else>{{ record.docNo }}</span>
              </template>
              <a v-else @click="emit('open', record.docId)">{{ record.docNo }}</a>
            </template>
            <template v-else-if="column.key === 'category'">
              {{ record.transferId ? $t('fin.io.transferFee') : record.categoryName }}
            </template>
            <template v-else-if="column.key === 'amount'">
              <span :class="record.direction === FundDirection.OUT ? 'out' : 'in'">{{ formatMoney(record.amount) }}</span>
            </template>
          </template>
        </a-table>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import { ioSummary, pageIoLines } from '@/api/fin'
import { formatMoney } from '@/components/fin/fin'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { sumBy } from '@/utils/number'
import { FundDirection } from '@/constants/enums'

/** 收支汇总：期间内已确认的收支单按类别汇总（左），账户调拨的手续费单列一行（支出）；点击类别查看明细（右） */
const emit = defineEmits(['open'])
const { t } = useI18n()
useDict('fin_fund_direction')
const range = ref([dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
const direction = ref('')
const params = () => ({ begin: range.value[0], end: range.value[1], direction: direction.value || undefined })

const summaryColumns = [
  { title: t('fin.io.category'), key: 'category' },
  { title: t('fin.io.type'), key: 'direction', width: 80 },
  { title: t('fin.io.count'), dataIndex: 'count', width: 70, align: 'right' },
  { title: t('common.field.amount'), key: 'amount', width: 130, align: 'right' }
]
const rows = ref([])
const loading = ref(false)
const totalIn = computed(() => sumBy(rows.value.filter((r) => r.direction === FundDirection.IN), 'amount'))
const totalOut = computed(() => sumBy(rows.value.filter((r) => r.direction === FundDirection.OUT), 'amount'))
const totalCount = computed(() => rows.value.reduce((n, r) => n + Number(r.count), 0))
async function loadSummary() {
  loading.value = true
  try {
    rows.value = (await ioSummary(params())).map((r) => ({ ...r, key: r.transferFee ? 'transfer-fee' : `${r.direction}-${r.categoryId}` }))
  } finally {
    loading.value = false
  }
}

const lineColumns = [
  { title: t('fin.io.date'), dataIndex: 'docDate', width: 105 },
  { title: t('fin.io.docNo'), key: 'docNo', width: 150 },
  { title: t('fin.io.category'), key: 'category', width: 120, ellipsis: true },
  { title: t('common.field.amount'), key: 'amount', width: 110, align: 'right' },
  { title: t('fin.io.partner'), dataIndex: 'partnerLabel', width: 130, ellipsis: true },
  { title: t('fin.io.account'), dataIndex: 'accountName', width: 130, ellipsis: true },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true }
]
/** 选中的汇总行：收支类别或调拨手续费 */
const selected = ref()
const selectedKey = computed(() => selected.value?.key)
const selectedName = computed(() => (selected.value?.transferFee ? t('fin.io.transferFee') : selected.value?.categoryName))
const lines = ref([])
const linesLoading = ref(false)
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (n) => t('common.table.total', { total: n }) })
async function loadLines() {
  linesLoading.value = true
  try {
    const s = selected.value
    const res = await pageIoLines({
      ...params(),
      categoryId: s?.transferFee ? undefined : s?.categoryId,
      transferFee: s?.transferFee || undefined,
      current: pagination.current,
      size: pagination.pageSize
    })
    lines.value = res.records
    pagination.total = res.total
  } finally {
    linesLoading.value = false
  }
}
function onPage(p) {
  pagination.current = p.current
  pagination.pageSize = p.pageSize
  loadLines()
}
function selectCategory(r) {
  selected.value = r
  pagination.current = 1
  loadLines()
}

function reload() {
  pagination.current = 1
  loadSummary()
  loadLines()
}
watch([range, direction], () => {
  selected.value = undefined
  reload()
})
onMounted(reload)
defineExpose({ reload })

const router = useRouter()
const userStore = useUserStore()
const canViewTransfer = computed(() => userStore.hasPerm('fin:transfer:list'))
const openTransfer = (id) => router.push({ path: '/fin/transfer', query: { id } })
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  margin-bottom: 12px;
}
.gap {
  margin-right: 16px;
}
.lines-head {
  font-weight: 500;
  margin-bottom: 8px;
  min-height: 24px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
  font-weight: normal;
}
.in {
  color: var(--status-positive, #389e0d);
}
.out {
  color: var(--status-critical, #cf1322);
}
:deep(.active-row) td {
  background: var(--ant-color-primary-bg, #e6f4ff);
}
:deep(.total-row) td {
  font-weight: 500;
}
:deep(tbody tr) {
  cursor: pointer;
}
</style>
