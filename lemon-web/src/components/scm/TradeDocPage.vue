<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="单据号" name="docNo">
          <a-input v-model:value="query.docNo" placeholder="请输入单据号" allow-clear />
        </a-form-item>
        <a-form-item label="订单号" name="orderNo">
          <a-input v-model:value="query.orderNo" placeholder="请输入订单号" allow-clear />
        </a-form-item>
        <a-form-item :label="orderConf.partnerLabel" name="partnerId">
          <PartnerSelect v-model:value="query.partnerId" :type="orderConf.partner" placeholder="全部" allow-clear />
        </a-form-item>
        <a-form-item label="仓库" name="warehouseId">
          <WarehouseSelect v-model:value="query.warehouseId" placeholder="全部" />
        </a-form-item>
        <a-form-item label="业务日期" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" style="width: 100%" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="`scm:${kind}:add`" type="primary" @click="tradeForm.openCreate()">
          <template #icon><PlusOutlined /></template>新增{{ conf.label }}
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1300, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'docNo'">
            <a @click="openDetail(record)">{{ record.docNo }}</a>
          </template>
          <template v-else-if="column.key === 'partner'">
            <div>{{ record.partnerName }}</div>
            <div class="sub-text">{{ record.partnerCode }}</div>
          </template>
          <template v-else-if="column.key === 'totalQty'">{{ formatQty(record.totalQty) }}</template>
          <template v-else-if="column.key === 'totalAmount'">¥{{ formatMoney(record.totalAmount) }}</template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">
                <template #icon><EyeOutlined /></template>查看
              </a-button>
              <a-button v-if="kind === 'salesOut'" v-perm="'scm:shipment:add'" type="link" size="small" @click="createShipment(record)">
                <template #icon><CarOutlined /></template>发运
              </a-button>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <TradeDocForm ref="tradeForm" :kind="kind" @success="load" />
    <TradeDocDetail v-model:open="detailOpen" :kind="kind" :doc-id="detailId" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CarOutlined, EyeOutlined, PlusOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import PartnerSelect from './PartnerSelect.vue'
import TradeDocDetail from './TradeDocDetail.vue'
import TradeDocForm from './TradeDocForm.vue'
import { pageTrade } from '@/api/scm/trade'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'
import { formatQty } from '@/components/wms/wms'
import { ORDER_KINDS, TRADE_KINDS, formatMoney } from './scm'

/**
 * 采购入库 / 采购退货 / 销售出库 / 销售退货列表页，四个页面共用
 */
const props = defineProps({
  kind: { type: String, required: true }
})

const route = useRoute()
const router = useRouter()
const conf = TRADE_KINDS[props.kind]
const orderConf = ORDER_KINDS[conf.orderKind]

const columns = [
  { title: '单据号', key: 'docNo', width: 170 },
  { title: '订单号', dataIndex: 'orderNo', width: 170 },
  { title: orderConf.partnerLabel, key: 'partner', width: 200 },
  { title: '仓库', dataIndex: 'warehouseName', width: 130 },
  { title: '业务日期', dataIndex: 'bizDate', width: 110 },
  { title: '数量', key: 'totalQty', width: 100, align: 'right' },
  { title: '价税合计', key: 'totalAmount', width: 130, align: 'right' },
  { title: '仓库单据', dataIndex: 'wmsDocNo', width: 170 },
  { title: '制单人', dataIndex: 'createByName', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, `scm-${props.kind}`)
const showSearch = ref(true)

const emptyQuery = () => ({ docNo: undefined, orderNo: undefined, partnerId: undefined, warehouseId: undefined, range: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange } = useTable((params) => pageTrade(props.kind, params), query, {
  params: ({ range, ...rest }) => ({ ...rest, beginDate: range?.[0], endDate: range?.[1] })
})

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

onMounted(() => {
  load()
  // 从发货通知等跳转过来时（?id=单据ID）直接打开单据详情
  const id = Number(route.query.id)
  if (id) {
    openDetail({ id })
    router.replace({ query: {} })
  }
})

const tradeForm = ref()
const detailOpen = ref(false)
const detailId = ref()
function openDetail(record) {
  detailId.value = record.id
  detailOpen.value = true
}

const createShipment = (record) => router.push({ path: '/scm/shipment', query: { tradeDocId: record.id } })
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
