<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <span class="sub-text">每月上报各产品的当月销量（卖给终端客户）和月末库存，同一月份重复上报会覆盖</span>
        <template #extra>
          <a-button v-perm="'dealer:report:edit'" type="primary" @click="openForm()">
            <template #icon><PlusOutlined /></template>上报
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

    <SubPage :open="formOpen" :title="`库存销量上报 ${periodText(period)}`" width="min(900px, 100vw)" @close="formOpen = false">
      <template #extra>
        <a-button v-perm="'dealer:report:edit'" type="primary" :loading="saving" @click="submit">提交上报</a-button>
      </template>
      <a-form layout="inline" class="block">
        <a-form-item label="月份">
          <PeriodPicker v-model:value="period" :disabled-date="(d) => d.isAfter(dayjs(), 'month')" @update:value="loadPeriod" />
        </a-form-item>
        <a-form-item label="说明">
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
      <div class="sub-text tip">只上报填写了销量或库存的产品；产品清单为可订货的产品</div>
    </SubPage>
  </div>
</template>

<script setup>
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

/** 经销商门户：按月上报渠道库存和销量 */
const columns = [
  { title: '月份', key: 'period', width: 140 },
  { title: '产品数', dataIndex: 'itemCount', width: 100, align: 'right' },
  { title: '当月销量', key: 'sell', width: 120, align: 'right' },
  { title: '月末库存', key: 'stock', width: 120, align: 'right' },
  { title: '说明', dataIndex: 'remark', ellipsis: true },
  { title: '上报', key: 'time', width: 180 }
]
const { checked, visibleColumns } = useColumns(columns, 'dealer-report')
const { list, loading, size, pagination, load, onTableChange } = useTable(pageDealerReport, reactive({}))
onMounted(load)

const lineColumns = [
  { title: '产品', key: 'material' },
  { title: '当月销量', key: 'sell', width: 200 },
  { title: '月末库存', key: 'stock', width: 200 }
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
  if (!items.length) return message.warning('请填写至少一个产品的销量或库存')
  saving.value = true
  try {
    await saveDealerReport(period.value, { remark: remark.value || null, items })
    message.success('已上报')
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
