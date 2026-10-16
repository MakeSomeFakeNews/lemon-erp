<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="关键字" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="通知单号、订单号或客户" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.scm_delivery_status)" />
        </a-form-item>
        <a-form-item label="仓库" name="warehouseId">
          <WarehouseSelect v-model:value="query.warehouseId" placeholder="全部" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :exporter="exportAll"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="'scm:delivery:add'" type="primary" @click="form.openCreate()">
          <template #icon><PlusOutlined /></template>新增发货通知
        </a-button>
        <a-button v-perm="'scm:wave:add'" :disabled="!selectedKeys.length" @click="createWave">
          <template #icon><MergeCellsOutlined /></template>生成波次{{ selectedKeys.length ? `（${selectedKeys.length}）` : '' }}
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="{ ...rowSelection, getCheckboxProps: (r) => ({ disabled: r.status !== 'PENDING' || !!r.waveNo }) }"
        :scroll="{ x: 1400, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'deliveryNo'">
            <a @click="openDetail(record)">{{ record.deliveryNo }}</a>
          </template>
          <template v-else-if="column.key === 'orderNo'">
            <a @click="toOrder(record)">{{ record.orderNo }}</a>
          </template>
          <template v-else-if="column.key === 'totalQty'">{{ formatQty(record.totalQty) }}</template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="scm_delivery_status" :value="record.status" />
            <div v-if="record.waveNo" class="sub-text">波次 {{ record.waveNo }}</div>
          </template>
          <template v-else-if="column.key === 'pick'">
            <template v-if="record.pickTime">{{ record.pickByName || '-' }} {{ record.pickTime }}</template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'tradeDocNo'">
            <a v-if="record.tradeDocId" @click="toTradeDoc(record)">{{ record.tradeDocNo }}</a>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record)">
              <template #icon><EyeOutlined /></template>查看
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <DeliveryForm ref="form" @success="onSaved" />
    <DeliveryDetail v-model:open="detailOpen" :delivery-id="detailId" @changed="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { EyeOutlined, MergeCellsOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import DeliveryDetail from '@/components/scm/DeliveryDetail.vue'
import DeliveryForm from '@/components/scm/DeliveryForm.vue'
import { pageDelivery } from '@/api/scm/planning'
import { addWave } from '@/api/scm/shipment'
import { formatQty } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

const dict = useDict('scm_delivery_status')

const columns = [
  { title: '通知单号', key: 'deliveryNo', dataIndex: 'deliveryNo', width: 180 },
  { title: '销售订单', key: 'orderNo', dataIndex: 'orderNo', width: 180 },
  { title: '客户', dataIndex: 'customerName', width: 180, ellipsis: true },
  { title: '发货仓库', dataIndex: 'warehouseName', width: 130 },
  { title: '发货日期', dataIndex: 'shipDate', width: 110 },
  { title: '数量', key: 'totalQty', dataIndex: 'totalQty', width: 100, align: 'right' },
  { title: '状态', key: 'status', dataIndex: 'status', width: 90, dict: 'scm_delivery_status' },
  { title: '拣货', key: 'pick', width: 200, exportValue: (r) => (r.pickTime ? `${r.pickByName || ''} ${r.pickTime}` : '') },
  { title: '销售出库单', key: 'tradeDocNo', dataIndex: 'tradeDocNo', width: 170 },
  { title: '制单人', dataIndex: 'createByName', width: 100 },
  { title: '备注', dataIndex: 'remark', width: 160, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 90, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'scm-delivery')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, status: undefined, warehouseId: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(pageDelivery, query)

/** 选中的待拣货通知（同一仓库）生成拣货波次 */
async function createWave() {
  const id = await addWave({ deliveryIds: selectedKeys.value })
  message.success('已生成波次')
  selectedKeys.value = []
  router.push({ path: '/scm/wave', query: { id } })
}

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const route = useRoute()
const router = useRouter()
const form = ref()
const detailOpen = ref(false)
const detailId = ref()

onMounted(() => {
  load()
  // 从销售订单「发货通知」跳转过来时（?orderId=&create=1）直接打开新增表单
  const orderId = Number(route.query.orderId)
  if (orderId && route.query.create) form.value.openCreate(orderId)
  else if (Number(route.query.id)) openDetail({ id: Number(route.query.id) })
  if (Object.keys(route.query).length) router.replace({ query: {} })
})

function openDetail(record) {
  detailId.value = record.id
  detailOpen.value = true
}

/** 保存后打开详情，查看拣货建议 */
function onSaved(id) {
  load()
  if (id) openDetail({ id })
}

const toOrder = (r) => router.push({ path: '/scm/sales', query: { id: r.orderId } })
const toTradeDoc = (r) => router.push({ path: '/scm/sales-out', query: { id: r.tradeDocId } })
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
