<template>
  <div class="table-page">
    <a-alert v-if="status && !status.features?.REPORT" type="warning" show-icon class="page-card" :message="$t('ai.report.notAvailable')">
      <template v-if="canConfig" #action>
        <a-button size="small" @click="router.push('/ai/settings')">{{ $t('ai.report.goSettings') }}</a-button>
      </template>
    </a-alert>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" @refresh="load">
        <a-radio-group v-model:value="query.periodType" button-style="solid" @change="search">
          <a-radio-button :value="undefined">{{ $t('ai.report.all') }}</a-radio-button>
          <a-radio-button :value="AiReportPeriodType.WEEK">{{ $t('ai.report.type.WEEK') }}</a-radio-button>
          <a-radio-button :value="AiReportPeriodType.MONTH">{{ $t('ai.report.type.MONTH') }}</a-radio-button>
        </a-radio-group>
        <a-button v-perm="'ai:report:generate'" type="primary" :disabled="!status?.features?.REPORT" @click="generateRef.open()">
          <template #icon><ThunderboltOutlined /></template>{{ $t('ai.report.generate') }}
        </a-button>
        <template #extra>
          <a-tooltip :title="$t('ai.report.autoNotice')" placement="bottomRight">
            <QuestionCircleOutlined class="help" />
          </a-tooltip>
        </template>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1100, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <a @click="detailRef.open(record.id)">{{ record.title }}</a>
          </template>
          <template v-else-if="column.key === 'periodType'">
            <a-tag :color="record.periodType === AiReportPeriodType.MONTH ? 'purple' : 'blue'">{{ $t(`ai.report.type.${record.periodType}`) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'period'">{{ record.periodStart }} ~ {{ record.periodEnd }}</template>
          <template v-else-if="column.key === 'generatedBy'">
            <a-tooltip v-if="record.auto" :title="$t('ai.report.autoTip')">
              <a-tag color="green">{{ $t('ai.report.auto') }}</a-tag>
            </a-tooltip>
            <span v-else>{{ record.userName || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'sources'">
            <span class="sources" :title="(record.sourceLabels || []).join(' / ')">{{ (record.sourceLabels || []).join(' / ') }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag v-if="record.status === AiReportStatus.GENERATING" color="processing">
              <template #icon><SyncOutlined spin /></template>{{ $t('ai.report.status.GENERATING') }}
            </a-tag>
            <a-tooltip v-else-if="record.status === AiReportStatus.FAILED" :title="record.error">
              <a-tag color="error">{{ $t('ai.report.status.FAILED') }}</a-tag>
            </a-tooltip>
            <a-tag v-else color="success">{{ $t('ai.report.status.DONE') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'tokens'">
            <span v-if="record.promptTokens != null">{{ formatNumber(record.promptTokens + (record.completionTokens || 0)) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a @click="detailRef.open(record.id)"><EyeOutlined /> {{ $t('common.action.view') }}</a>
              <a-popconfirm
                v-if="record.status !== AiReportStatus.GENERATING"
                v-perm="'ai:report:generate'"
                :title="$t('ai.report.deleteConfirm')"
                @confirm="remove(record.id)"
              >
                <a class="danger"><DeleteOutlined /> {{ $t('common.action.delete') }}</a>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <GenerateModal ref="generateRef" @success="onGenerated" />
    <ReportDetail ref="detailRef" @changed="load" />
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, EyeOutlined, QuestionCircleOutlined, SyncOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import TableToolbar from '@/components/TableToolbar.vue'
import GenerateModal from './GenerateModal.vue'
import ReportDetail from './ReportDetail.vue'
import { getAiStatus } from '@/api/ai/config'
import { deleteReport, pageReports } from '@/api/ai/report'
import { useAction } from '@/composables/useAction'
import { useTable } from '@/composables/useTable'
import { AiReportPeriodType, AiReportStatus } from '@/constants/enums'
import { useUserStore } from '@/store/user'
import { formatNumber } from '@/utils/format'

/** 有报告在生成中时刷新列表的间隔 */
const POLL_MS = 5000

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const canConfig = userStore.hasPerm('ai:config:list')

const columns = [
  { title: t('ai.report.colTitle'), key: 'title', width: 260, ellipsis: true },
  { title: t('ai.report.colType'), key: 'periodType', width: 80 },
  { title: t('ai.report.colPeriod'), key: 'period', width: 200 },
  { title: t('ai.report.colGeneratedBy'), key: 'generatedBy', width: 110 },
  { title: t('ai.report.colSources'), key: 'sources', width: 220, ellipsis: true },
  { title: t('ai.report.colStatus'), key: 'status', width: 100 },
  { title: t('ai.report.colTokens'), key: 'tokens', width: 90, align: 'right' },
  { title: t('ai.report.colTime'), dataIndex: 'createTime', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: 140, fixed: 'right' }
]

const status = ref(null)
const generateRef = ref()
const detailRef = ref()
const query = reactive({ periodType: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageReports, query)

const remove = useAction((id) => deleteReport(id), { successKey: 'common.msg.deleteSuccess', reload: () => load() })

function onGenerated(id) {
  load()
  detailRef.value.open(id)
}

// 有生成中的报告时定时刷新
let timer = null
watch(list, (rows) => {
  clearTimeout(timer)
  if (rows.some((r) => r.status === AiReportStatus.GENERATING)) {
    timer = setTimeout(() => load(), POLL_MS)
  }
})
onBeforeUnmount(() => clearTimeout(timer))

onMounted(async () => {
  load()
  const id = Number(route.query.id)
  if (id) detailRef.value.open(id)
  status.value = await getAiStatus()
})
</script>

<style scoped>
.help {
  color: var(--app-text-2);
  cursor: help;
}
.sources {
  color: var(--app-text-2);
}
.danger {
  color: var(--ant-color-error, #ff4d4f);
}
</style>
