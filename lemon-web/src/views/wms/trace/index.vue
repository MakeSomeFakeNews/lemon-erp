<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm v-if="tab === 'batch'" :model="batchQuery" @search="search" @reset="resetQuery">
        <a-form-item label="批次号" name="batchNo">
          <a-input v-model:value="batchQuery.batchNo" placeholder="请输入批次号" allow-clear />
        </a-form-item>
        <a-form-item label="物料" name="keyword">
          <a-input v-model:value="batchQuery.keyword" placeholder="编码或名称" allow-clear />
        </a-form-item>
        <a-form-item label="有效期至" name="expireBefore" tooltip="有效期在该日期（含）之前的批次，用于临期、过期预警">
          <a-date-picker v-model:value="batchQuery.expireBefore" value-format="YYYY-MM-DD" placeholder="不限" style="width: 100%" />
        </a-form-item>
      </SearchForm>
      <SearchForm v-else :model="serialQuery" @search="search" @reset="resetQuery">
        <a-form-item label="序列号" name="serialNo">
          <a-input v-model:value="serialQuery.serialNo" placeholder="扫码或输入序列号" allow-clear />
        </a-form-item>
        <a-form-item label="物料" name="keyword">
          <a-input v-model:value="serialQuery.keyword" placeholder="编码或名称" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="serialQuery.status" placeholder="全部" allow-clear :options="toOptions(dict.wms_serial_status)" />
        </a-form-item>
        <a-form-item label="仓库" name="warehouseId">
          <WarehouseSelect v-model:value="serialQuery.warehouseId" placeholder="全部" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll" v-model:size="size" v-model:show-search="showSearch" @refresh="load">
        <a-radio-group v-model:value="tab" button-style="solid" @change="switchTab">
          <a-radio-button value="batch">批次追踪</a-radio-button>
          <a-radio-button value="serial">序列号追踪</a-radio-button>
        </a-radio-group>
        <a-checkbox v-if="tab === 'batch'" v-model:checked="batchQuery.inStock" class="toolbar-check" @change="search">
          只看有库存的批次
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
              <template #icon><NodeIndexOutlined /></template>追踪
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
            <a-descriptions-item label="物料">{{ batch.materialCode }} {{ batch.materialName }}</a-descriptions-item>
            <a-descriptions-item label="批次号">{{ batch.batchNo }}</a-descriptions-item>
            <a-descriptions-item label="在库数量">{{ formatQty(batch.qty) }} {{ batch.unit }}</a-descriptions-item>
            <a-descriptions-item label="生产日期">{{ batch.productionDate || '-' }}</a-descriptions-item>
            <a-descriptions-item label="有效期至">{{ batch.expiryDate || '-' }}</a-descriptions-item>
            <a-descriptions-item label="首次入库">{{ batch.createTime }}</a-descriptions-item>
          </a-descriptions>
          <div class="section-title">库存分布</div>
          <a-table row-key="id" size="small" :columns="distColumns" :data-source="distribution" :pagination="false" class="block">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'location'">{{ locationText(record.locationCode) }}</template>
              <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }}</template>
            </template>
          </a-table>
          <div class="section-title">出入库记录（最近 100 条）</div>
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
            <a-descriptions-item label="物料">{{ serial.materialCode }} {{ serial.materialName }}</a-descriptions-item>
            <a-descriptions-item label="当前状态"><DictTag type="wms_serial_status" :value="serial.status" /></a-descriptions-item>
            <a-descriptions-item :label="serial.status === 'IN_STOCK' ? '当前位置' : '最后所在'">
              {{ serial.warehouseName }} / {{ locationText(serial.locationCode) }}
            </a-descriptions-item>
            <a-descriptions-item v-if="serial.batchNo" label="批次">{{ serial.batchNo }}</a-descriptions-item>
          </a-descriptions>
          <a-timeline>
            <a-timeline-item v-for="log in logs" :key="log.id" :color="log.direction > 0 ? 'green' : 'orange'">
              <div class="log-head">
                <DictTag type="wms_biz_type" :value="log.bizType" />
                <span>{{ log.direction > 0 ? '入' : '出' }} {{ log.warehouseName }} / {{ locationText(log.locationCode) }}</span>
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

const dict = useDict('wms_serial_status')

const batchColumns = [
  { title: '批次号', dataIndex: 'batchNo', width: 150 },
  { title: '物料', key: 'material', width: 240 },
  { title: '生产日期', dataIndex: 'productionDate', width: 110 },
  { title: '有效期至', key: 'expiryDate', width: 190 },
  { title: '在库数量', key: 'qty', width: 120, align: 'right' },
  { title: '首次入库', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 90, fixed: 'right' }
]
const serialColumns = [
  { title: '序列号', dataIndex: 'serialNo', width: 180 },
  { title: '物料', key: 'material', width: 240 },
  { title: '状态', key: 'status', width: 90, dict: 'wms_serial_status' },
  { title: '位置', key: 'position', width: 200 },
  { title: '批次', key: 'batchNo', width: 120 },
  { title: '首次入库', dataIndex: 'createTime', width: 170 },
  { title: '最近变动', dataIndex: 'updateTime', width: 170 },
  { title: '操作', key: 'action', width: 90, fixed: 'right' }
]
const distColumns = [
  { title: '仓库', dataIndex: 'warehouseName' },
  { title: '库位', key: 'location' },
  { title: '数量', key: 'qty', align: 'right' }
]
const flowColumns = [
  { title: '时间', dataIndex: 'createTime', width: 170 },
  { title: '业务', key: 'bizType', width: 80, dict: 'wms_biz_type' },
  { title: '单据号', dataIndex: 'docNo', width: 160 },
  { title: '位置', key: 'position', width: 180 },
  { title: '数量', key: 'qty', width: 100, align: 'right' },
  { title: '操作人', dataIndex: 'createByName', width: 100 }
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
      batchTitle.value = `批次追踪：${record.batchNo}`
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
      serialTitle.value = `序列号追踪：${record.serialNo}`
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
