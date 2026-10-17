<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <span class="sub-text">{{ $t('dealer.report.tip') }}</span>
        <template #extra>
          <a-button v-perm="'dealer:report:edit'" type="primary" @click="openForm()">
            <template #icon><PlusOutlined /></template>{{ $t('dealer.report.report') }}
          </a-button>
        </template>
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 800, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'period'"><a @click="openForm(record)">{{ periodText(record.period) }}</a></template>
          <template v-else-if="column.key === 'sell'">{{ formatQty(record.sellTotal) }}</template>
          <template v-else-if="column.key === 'stock'">{{ formatQty(record.stockTotal) }}</template>
          <template v-else-if="column.key === 'time'">
            {{ record.reportTime }}
            <div class="sub-text">{{ record.reportByName }}</div>
          </template>
        </template>
      </a-table>
    </a-card>

    <SubPage :open="formOpen" :title="$t('dealer.report.formTitle', { period: periodText(period) })" width="min(900px, 100vw)" @close="formOpen = false">
      <template #extra>
        <a-button v-perm="'dealer:report:edit'" type="primary" :loading="saving" @click="submit">{{ $t('dealer.report.submit') }}</a-button>
      </template>
      <a-form layout="inline" class="block">
        <a-form-item :label="$t('dealer.report.month')">
          <PeriodPicker v-model:value="period" :disabled-date="(d) => d.isAfter(dayjs(), 'month')" @update:value="loadPeriod" />
        </a-form-item>
        <a-form-item :label="$t('common.field.description')">
          <a-input v-model:value="remark" :maxlength="255" class="remark" />
        </a-form-item>
      </a-form>
      <a-table row-key="materialId" size="small" :columns="lineColumns" :data-source="lines" :loading="formLoading" :pagination="false" :scroll="{ y: 460 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            {{ record.materialCode }} {{ record.materialName }}
            <div class="sub-text">{{ record.spec }}</div>
          </template>
          <template v-else-if="column.key === 'sell'">
            <a-input-number v-model:value="record.sellQty" :min="0" :addon-after="record.unit" class="full" />
          </template>
          <template v-else-if="column.key === 'stock'">
            <a-input-number v-model:value="record.stockQty" :min="0" :addon-after="record.unit" class="full" />
          </template>
        </template>
      </a-table>
      <div class="sub-text tip">{{ $t('dealer.report.formTip') }}</div>
    </SubPage>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import { dealerCatalog, getDealerReport, pageDealerReport, saveDealerReport } from '@/api/scm/dealer'
import { currentPeriod, periodText } from '@/components/fin/fin'
import { formatQty } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'

const { t } = useI18n()

/** 经销商门户：按月上报渠道库存和销量 */
const columns = [
  { title: t('dealer.report.month'), key: 'period', width: 140 },
  { title: t('dealer.report.itemCount'), dataIndex: 'itemCount', width: 100, align: 'right' },
  { title: t('dealer.report.monthSales'), key: 'sell', width: 130, align: 'right' },
  { title: t('dealer.report.monthEndStock'), key: 'stock', width: 140, align: 'right' },
  { title: t('common.field.description'), dataIndex: 'remark', ellipsis: true },
  { title: t('dealer.report.reported'), key: 'time', width: 180 }
]
const { checked, visibleColumns } = useColumns(columns, 'dealer-report')
const { list, loading, size, pagination, load, onTableChange } = useTable(pageDealerReport, reactive({}))
onMounted(load)

const lineColumns = [
  { title: t('dealer.report.product'), key: 'material' },
  { title: t('dealer.report.monthSales'), key: 'sell', width: 200 },
  { title: t('dealer.report.monthEndStock'), key: 'stock', width: 200 }
]
const formOpen = ref(false)
const formLoading = ref(false)
const saving = ref(false)
const period = ref(currentPeriod())
const remark = ref('')
const lines = ref([])
let catalog = []
/** 切换月份时带出该月已上报的数据 */
async function loadPeriod() {
  formLoading.value = true
  try {
    const existing = list.value.find((r) => r.period === period.value)
    const report = existing ? await getDealerReport(existing.id) : null
    const reported = new Map((report?.items || []).map((i) => [i.materialId, i]))
    remark.value = report?.remark || ''
    const rows = catalog.map((c) => ({ materialId: c.materialId, materialCode: c.materialCode, materialName: c.materialName, spec: c.spec,
      unit: c.unit, sellQty: undefined, stockQty: undefined }))
    // 已上报但不在当前可订货清单中的产品也保留
    reported.forEach((i) => {
      if (!rows.some((r) => r.materialId === i.materialId)) {
        rows.push({ materialId: i.materialId, materialCode: i.materialCode, materialName: i.materialName, spec: i.spec, unit: i.unit })
      }
    })
    rows.forEach((r) => {
      const i = reported.get(r.materialId)
      if (i) Object.assign(r, { sellQty: Number(i.sellQty), stockQty: Number(i.stockQty) })
    })
    lines.value = rows
  } finally {
    formLoading.value = false
  }
}
async function openForm(record) {
  period.value = record ? record.period : currentPeriod()
  formOpen.value = true
  catalog = await dealerCatalog()
  await loadPeriod()
}
async function submit() {
  const items = lines.value.filter((l) => l.sellQty != null || l.stockQty != null)
    .map((l) => ({ materialId: l.materialId, sellQty: l.sellQty || 0, stockQty: l.stockQty || 0 }))
  if (!items.length) return message.warning(t('dealer.report.msgEmpty'))
  saving.value = true
  try {
    await saveDealerReport(period.value, { remark: remark.value || null, items })
    message.success(t('dealer.report.done'))
    formOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.full {
  width: 100%;
}
.block {
  margin-bottom: 16px;
}
.remark {
  width: 320px;
}
.tip {
  margin-top: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
