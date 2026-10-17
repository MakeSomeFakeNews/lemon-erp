<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm v-if="tab === 'batch'" :model="batchQuery" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('wms.common.batchNo')" name="batchNo">
          <a-input v-model:value="batchQuery.batchNo" :placeholder="$t('common.placeholder.input')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('wms.common.material')" name="keyword">
          <a-input v-model:value="batchQuery.keyword" :placeholder="$t('wms.flow.codeOrName')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('wms.common.expiryDate')" name="expireBefore" :tooltip="$t('wms.trace.expireBeforeTip')">
          <a-date-picker v-model:value="batchQuery.expireBefore" value-format="YYYY-MM-DD" :placeholder="$t('wms.trace.any')" style="width: 100%" />
        </a-form-item>
      </SearchForm>
      <SearchForm v-else :model="serialQuery" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('wms.common.serialNo')" name="serialNo">
          <a-input v-model:value="serialQuery.serialNo" :placeholder="$t('wms.doc.serialInputTip')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('wms.common.material')" name="keyword">
          <a-input v-model:value="serialQuery.keyword" :placeholder="$t('wms.flow.codeOrName')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="serialQuery.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.wms_serial_status)" />
        </a-form-item>
        <a-form-item :label="$t('wms.common.warehouse')" name="warehouseId">
          <WarehouseSelect v-model:value="serialQuery.warehouseId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll" v-model:size="size" v-model:show-search="showSearch" @refresh="load">
        <a-radio-group v-model:value="tab" button-style="solid" @change="switchTab">
          <a-radio-button value="batch">{{ $t('wms.trace.batchTrace') }}</a-radio-button>
          <a-radio-button value="serial">{{ $t('wms.trace.serialTrace') }}</a-radio-button>
        </a-radio-group>
        <a-checkbox v-if="tab === 'batch'" v-model:checked="batchQuery.inStock" class="toolbar-check" @change="search">
          {{ $t('wms.trace.inStockOnly') }}
        </a-checkbox>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="tab === 'batch' ? batchColumns : serialColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1100, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            <div>{{ record.materialName }}</div>
            <div class="sub-text">{{ record.materialCode }} {{ record.spec }}</div>
          </template>
          <template v-else-if="column.key === 'expiryDate'">
            <template v-if="record.expiryDate">
              {{ record.expiryDate }}
              <a-tag v-if="expiryStatus(record.expiryDate)" :color="expiryStatus(record.expiryDate).color" :bordered="false">
                {{ expiryStatus(record.expiryDate).text }}
              </a-tag>
            </template>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="wms_serial_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'position'">
            {{ record.warehouseName }} / {{ locationText(record.locationCode) }}
          </template>
          <template v-else-if="column.key === 'batchNo'">{{ record.batchNo || '-' }}</template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openTrace(record)">
              <template #icon><NodeIndexOutlined /></template>{{ $t('wms.trace.trace') }}
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 批次追踪：库存分布 + 流水 -->
    <SubPage v-model:open="batchOpen" :title="batchTitle" width="min(960px, 100vw)" destroy-on-close>
      <a-spin :spinning="traceLoading">
        <template v-if="batch">
          <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
            <a-descriptions-item :label="$t('wms.common.material')">{{ batch.materialCode }} {{ batch.materialName }}</a-descriptions-item>
            <a-descriptions-item :label="$t('wms.common.batchNo')">{{ batch.batchNo }}</a-descriptions-item>
            <a-descriptions-item :label="$t('wms.trace.inStockQty')">{{ formatQty(batch.qty) }} {{ batch.unit }}</a-descriptions-item>
            <a-descriptions-item :label="$t('wms.common.productionDate')">{{ batch.productionDate || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('wms.common.expiryDate')">{{ batch.expiryDate || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('wms.trace.firstIn')">{{ batch.createTime }}</a-descriptions-item>
          </a-descriptions>
          <div class="section-title">{{ $t('wms.trace.distribution') }}</div>
          <a-table row-key="id" size="small" :columns="distColumns" :data-source="distribution" :pagination="false" class="block">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'location'">{{ locationText(record.locationCode) }}</template>
              <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }}</template>
            </template>
          </a-table>
          <div class="section-title">{{ $t('wms.trace.flows') }}</div>
          <a-table row-key="id" size="small" :columns="flowColumns" :data-source="flows" :pagination="false" :scroll="{ x: 700 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'bizType'"><DictTag type="wms_biz_type" :value="record.bizType" /></template>
              <template v-else-if="column.key === 'position'">
                {{ record.warehouseName }} / {{ locationText(record.locationCode) }}
              </template>
              <template v-else-if="column.key === 'qty'"><QtyDiff :value="Number(record.qty)" /></template>
            </template>
          </a-table>
        </template>
      </a-spin>
    </SubPage>

    <!-- 序列号轨迹 -->
    <a-drawer v-model:open="serialOpen" :title="serialTitle" width="min(560px, 100vw)" destroy-on-close>
      <a-spin :spinning="traceLoading">
        <template v-if="serial">
          <a-descriptions :column="1" size="small" class="block">
            <a-descriptions-item :label="$t('wms.common.material')">{{ serial.materialCode }} {{ serial.materialName }}</a-descriptions-item>
            <a-descriptions-item :label="$t('wms.trace.currentStatus')"><DictTag type="wms_serial_status" :value="serial.status" /></a-descriptions-item>
            <a-descriptions-item :label="serial.status === 'IN_STOCK' ? $t('wms.trace.currentPosition') : $t('wms.trace.lastPosition')">
              {{ serial.warehouseName }} / {{ locationText(serial.locationCode) }}
            </a-descriptions-item>
            <a-descriptions-item v-if="serial.batchNo" :label="$t('wms.common.batch')">{{ serial.batchNo }}</a-descriptions-item>
          </a-descriptions>
          <a-timeline>
            <a-timeline-item v-for="log in logs" :key="log.id" :color="log.direction > 0 ? 'green' : 'orange'">
              <div class="log-head">
                <DictTag type="wms_biz_type" :value="log.bizType" />
                <span>{{ log.direction > 0 ? $t('wms.trace.in') : $t('wms.trace.out') }} {{ log.warehouseName }} / {{ locationText(log.locationCode) }}</span>
              </div>
              <div class="sub-text">{{ log.createTime }} · {{ log.docNo }} · {{ log.createByName || '-' }}</div>
            </a-timeline-item>
          </a-timeline>
        </template>
      </a-spin>
    </a-drawer>
  </div>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { NodeIndexOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import QtyDiff from '@/components/wms/QtyDiff.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { getBatch, pageBatch, pageFlow, pageSerial, pageStock, serialLogs } from '@/api/wms/stock'
import { expiryStatus, formatQty, locationText } from '@/components/wms/wms'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

const { t } = useI18n()

const dict = useDict('wms_serial_status')

const batchColumns = [
  { title: t('wms.common.batchNo'), dataIndex: 'batchNo', width: 150 },
  { title: t('wms.common.material'), key: 'material', width: 240 },
  { title: t('wms.common.productionDate'), dataIndex: 'productionDate', width: 110 },
  { title: t('wms.common.expiryDate'), key: 'expiryDate', width: 190 },
  { title: t('wms.trace.inStockQty'), key: 'qty', width: 120, align: 'right' },
  { title: t('wms.trace.firstIn'), dataIndex: 'createTime', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: 90, fixed: 'right' }
]
const serialColumns = [
  { title: t('wms.common.serialNo'), dataIndex: 'serialNo', width: 180 },
  { title: t('wms.common.material'), key: 'material', width: 240 },
  { title: t('common.field.status'), key: 'status', width: 90, dict: 'wms_serial_status' },
  { title: t('wms.trace.position'), key: 'position', width: 200 },
  { title: t('wms.common.batch'), key: 'batchNo', width: 120 },
  { title: t('wms.trace.firstIn'), dataIndex: 'createTime', width: 170 },
  { title: t('wms.trace.lastChange'), dataIndex: 'updateTime', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: 90, fixed: 'right' }
]
const distColumns = [
  { title: t('wms.common.warehouse'), dataIndex: 'warehouseName' },
  { title: t('wms.common.location'), key: 'location' },
  { title: t('common.field.quantity'), key: 'qty', align: 'right' }
]
const flowColumns = [
  { title: t('wms.flow.time'), dataIndex: 'createTime', width: 170 },
  { title: t('wms.trace.biz'), key: 'bizType', width: 110, dict: 'wms_biz_type' },
  { title: t('wms.common.docNo'), dataIndex: 'docNo', width: 160 },
  { title: t('wms.trace.position'), key: 'position', width: 180 },
  { title: t('common.field.quantity'), key: 'qty', width: 100, align: 'right' },
  { title: t('wms.flow.operator'), dataIndex: 'createByName', width: 100 }
]

const tab = ref('batch')
const showSearch = ref(true)
const emptyBatchQuery = () => ({ batchNo: undefined, keyword: undefined, expireBefore: undefined, inStock: false })
const emptySerialQuery = () => ({ serialNo: undefined, keyword: undefined, status: undefined, warehouseId: undefined })
const batchQuery = reactive(emptyBatchQuery())
const serialQuery = reactive(emptySerialQuery())

const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(
  (params) => (tab.value === 'batch' ? pageBatch(params) : pageSerial(params)),
  {},
  { params: () => (tab.value === 'batch' ? { ...batchQuery } : { ...serialQuery }) }
)

function resetQuery() {
  Object.assign(batchQuery, emptyBatchQuery())
  Object.assign(serialQuery, emptySerialQuery())
  search()
}

function switchTab() {
  list.value = []
  search()
}

onMounted(load)

// 追踪详情
const traceLoading = ref(false)
const batchOpen = ref(false)
const batchTitle = ref('')
const batch = ref()
const distribution = ref([])
const flows = ref([])
const serialOpen = ref(false)
const serialTitle = ref('')
const serial = ref()
const logs = ref([])

async function openTrace(record) {
  traceLoading.value = true
  try {
    if (tab.value === 'batch') {
      batchTitle.value = t('wms.trace.batchTitle', { no: record.batchNo })
      batch.value = undefined
      batchOpen.value = true
      const params = { materialId: record.materialId, batchNo: record.batchNo }
      const [detail, stock, flow] = await Promise.all([
        getBatch(record.id),
        pageStock({ ...params, size: 500 }),
        pageFlow({ ...params, size: 100 })
      ])
      batch.value = detail
      // 库存查询的批次号为模糊匹配，这里只保留当前批次
      distribution.value = stock.records.filter((s) => s.batchNo === record.batchNo)
      flows.value = flow.records
    } else {
      serialTitle.value = t('wms.trace.serialTitle', { no: record.serialNo })
      serial.value = undefined
      serialOpen.value = true
      logs.value = await serialLogs(record.id)
      serial.value = record
    }
  } finally {
    traceLoading.value = false
  }
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.toolbar-check {
  align-self: center;
  margin-left: 8px;
}
.block {
  margin-bottom: 16px;
}
.section-title {
  font-weight: 500;
  margin-bottom: 8px;
}
.log-head {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
