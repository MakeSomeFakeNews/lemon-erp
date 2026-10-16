<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" placeholder="全部状态" allow-clear :options="toOptions(dict.scm_asn_status)" class="filter" @change="search" />
        <a-input-search v-model:value="query.keyword" placeholder="送货单号、订单号、运单号" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <span class="sub-text">在「我的订单」中按订单发起送货</span>
        </template>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 900, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'asnNo'">
            <a @click="openDetail(record.id)">{{ record.asnNo }}</a>
            <div class="sub-text">订单 {{ record.orderNo }}</div>
          </template>
          <template v-else-if="column.key === 'logistics'">{{ record.carrier || '-' }}<div class="sub-text">{{ record.trackingNo }}</div></template>
          <template v-else-if="column.key === 'qty'">{{ formatQty(record.totalQty) }}</template>
          <template v-else-if="column.key === 'status'"><DictTag type="scm_asn_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record.id)">查看</a-button>
              <span v-if="record.status === 'SUBMITTED'" v-perm="'portal:asn:add'">
                <a-popconfirm title="取消该送货通知？" @confirm="cancel(record.id)">
                  <a-button type="link" danger size="small">取消</a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <SubPage :open="detailOpen" :title="detail ? `送货通知 ${detail.asnNo}` : ''" width="min(860px, 100vw)" @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item label="采购订单">{{ detail.orderNo }}</a-descriptions-item>
          <a-descriptions-item label="状态"><DictTag type="scm_asn_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item label="收货时间">{{ detail.receivedTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="发货日期">{{ detail.shipDate }}</a-descriptions-item>
          <a-descriptions-item label="预计到货">{{ detail.expectedDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="物流">{{ detail.carrier || '-' }} {{ detail.trackingNo || '' }}</a-descriptions-item>
        </a-descriptions>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
            <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
          </template>
        </a-table>
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { cancelPortalAsn, getPortalAsn, pagePortalAsn } from '@/api/portal'
import { formatQty } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

/** 供应商门户 - 送货通知 */
const dict = useDict('scm_asn_status')
const columns = [
  { title: '送货单号', key: 'asnNo', width: 190 },
  { title: '发货日期', dataIndex: 'shipDate', width: 110 },
  { title: '预计到货', dataIndex: 'expectedDate', width: 110 },
  { title: '物流', key: 'logistics', width: 160 },
  { title: '数量', key: 'qty', width: 100, align: 'right' },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'portal-asn')
const query = reactive({ status: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pagePortalAsn, query)
onMounted(load)

const itemColumns = [
  { title: '物料', key: 'material' },
  { title: '批次', dataIndex: 'batchNo', width: 140 },
  { title: '送货数量', key: 'qty', width: 140 }
]
const detailOpen = ref(false)
const detail = ref()
async function openDetail(id) {
  detail.value = await getPortalAsn(id)
  detailOpen.value = true
}
async function cancel(id) {
  await cancelPortalAsn(id)
  message.success('已取消')
  load()
}
</script>

<style scoped>
.filter {
  width: 140px;
}
.filter-wide {
  width: 240px;
}
.block {
  margin-bottom: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
