<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('scm.trade.docNo')" name="docNo">
          <a-input v-model:value="query.docNo" :placeholder="$t('scm.trade.inputDocNo')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('scm.trade.orderNo')" name="orderNo">
          <a-input v-model:value="query.orderNo" :placeholder="$t('scm.trade.inputOrderNo')" allow-clear />
        </a-form-item>
        <a-form-item :label="orderConf.partnerLabel" name="partnerId">
          <PartnerSelect v-model:value="query.partnerId" :type="orderConf.partner" :placeholder="$t('common.placeholder.all')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('scm.trade.warehouse')" name="warehouseId">
          <WarehouseSelect v-model:value="query.warehouseId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('scm.trade.bizDate')" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" style="width: 100%" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll"
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="`scm:${kind}:add`" type="primary" @click="tradeForm.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('scm.trade.addKind', { kind: conf.label }) }}
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'docNo'">
            <a @click="openDetail(record)">{{ record.docNo }}</a>
          </template>
          <template v-else-if="column.key === 'orderNo'">
            <a-tag v-if="record.orderSource === OrderSource.DIRECT" color="blue" :bordered="false">{{ $t('scm.trade.direct') }}</a-tag>
            <span v-else>{{ record.orderNo }}</span>
          </template>
          <template v-else-if="column.key === 'warehouse'">
            <WarehouseNames :name="record.warehouseName" :names="record.warehouseNames" />
          </template>
          <template v-else-if="column.key === 'partner'">
            <div>{{ record.partnerName }}</div>
            <div class="sub-text">{{ record.partnerCode }}</div>
          </template>
          <template v-else-if="column.key === 'totalQty'">{{ formatQty(record.totalQty) }}</template>
          <template v-else-if="column.key === 'totalAmount'">
            ¥{{ formatMoney(record.totalAmount) }}
            <div v-if="Number(record.discountAmount)" class="sub-text">{{ $t('scm.wholeDiscount.value', { amount: '¥' + formatMoney(record.discountAmount) }) }}</div>
            <div v-if="Number(record.freight)" class="sub-text">{{ $t('scm.freight.value', { amount: '¥' + formatMoney(record.freight) }) }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[
                { type: 'view', onClick: () => openDetail(record) },
                { key: 'ship', label: $t('scm.trade.ship'), icon: CarOutlined, show: kind === 'salesOut', perm: 'scm:shipment:add', onClick: () => createShipment(record) }
              ]"
            />
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
import { CarOutlined, PlusOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import WarehouseNames from '@/components/wms/WarehouseNames.vue'
import PartnerSelect from './PartnerSelect.vue'
import TradeDocDetail from './TradeDocDetail.vue'
import TradeDocForm from './TradeDocForm.vue'
import { pageTrade } from '@/api/scm/trade'
import { useColumns } from '@/composables/useColumns'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { useTable } from '@/composables/useTable'
import { formatQty, warehouseText } from '@/components/wms/wms'
import { ORDER_KINDS, TRADE_KINDS, formatMoney } from './scm'
import { useI18n } from 'vue-i18n'
import { OrderSource } from '@/constants/enums'

/**
 * 采购入库 / 采购退货 / 销售出库 / 销售退货列表页，四个页面共用
 */
const props = defineProps({
  kind: { type: String, required: true }
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const conf = TRADE_KINDS[props.kind]
const orderConf = ORDER_KINDS[conf.orderKind]

// 导出（exportValue）：往来单位为「编码 名称」，金额、数量为原始数值，见 useTable
const columns = [
  { title: t('scm.trade.docNo'), key: 'docNo', width: 170 },
  {
    title: t('scm.trade.orderNo'),
    key: 'orderNo',
    width: 170,
    exportValue: (r) => (r.orderSource === OrderSource.DIRECT ? t('scm.trade.direct') : r.orderNo || '')
  },
  { title: orderConf.partnerLabel, key: 'partner', width: 200, exportValue: (r) => [r.partnerCode, r.partnerName].filter(Boolean).join(' ') },
  { title: t('scm.trade.warehouse'), key: 'warehouse', width: 130, exportValue: warehouseText },
  { title: t('scm.trade.bizDate'), dataIndex: 'bizDate', width: 120 },
  { title: t('common.field.quantity'), key: 'totalQty', width: 100, align: 'right' },
  {
    title: t('scm.trade.totalAmount'),
    key: 'totalAmount',
    width: 140,
    align: 'right',
    exportColumns: [
      { title: t('scm.trade.totalAmount'), dataIndex: 'totalAmount' },
      { title: t('scm.wholeDiscount.label'), exportValue: (r) => (Number(r.discountAmount) ? Number(r.discountAmount) : '') },
      ...(props.kind === 'purchaseIn' ? [{ title: t('scm.freight.label'), exportValue: (r) => (Number(r.freight) ? Number(r.freight) : '') }] : [])
    ]
  },
  { title: t('scm.trade.wmsDocNo'), dataIndex: 'wmsDocNo', width: 170 },
  { title: t('scm.trade.createBy'), dataIndex: 'createByName', width: 120 },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 },
  actionColumn(props.kind === 'salesOut' ? 2 : 1, [t('common.action.view')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, `scm-${props.kind}`)
const showSearch = ref(true)

const emptyQuery = () => ({ docNo: undefined, orderNo: undefined, partnerId: undefined, warehouseId: undefined, range: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable((params) => pageTrade(props.kind, params), query, {
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
