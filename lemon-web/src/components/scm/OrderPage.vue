<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('scm.order.orderNo')" name="orderNo">
          <a-input v-model:value="query.orderNo" :placeholder="$t('common.placeholder.inputName', { name: $t('scm.order.orderNo') })" allow-clear />
        </a-form-item>
        <a-form-item :label="conf.partnerLabel" name="partnerId">
          <PartnerSelect v-model:value="query.partnerId" :type="conf.partner" :placeholder="$t('common.placeholder.all')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.scm_order_status)" />
        </a-form-item>
        <a-form-item :label="$t('scm.order.material')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('scm.order.materialKeyword')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('scm.order.orderDate')" name="range">
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
        <a-button v-perm="`scm:${kind}:add`" type="primary" @click="form.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('scm.order.addOrder', { kind: conf.label }) }}
        </a-button>
        <a-button v-if="kind === 'sales'" v-perm="'scm:sales:add'" @click="importOpen = true">
          <template #icon><ImportOutlined /></template>{{ $t('scm.order.importExternal') }}
        </a-button>
        <span v-perm="`scm:${kind}:remove`">
          <a-popconfirm
            :title="$t('scm.order.confirmDeleteDrafts', { n: selectedKeys.length })"
            :disabled="!selectedKeys.length"
            @confirm="remove(selectedKeys)"
          >
            <a-button danger :disabled="!selectedKeys.length">
              <template #icon><DeleteOutlined /></template>{{ $t('common.action.batchDelete') }}
            </a-button>
          </a-popconfirm>
        </span>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="{ ...rowSelection, getCheckboxProps: (r) => ({ disabled: r.status !== 'DRAFT' }) }"
        :scroll="{ x: 1500, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'orderNo'">
            <a @click="openDetail(record)">{{ record.orderNo }}</a>
            <a-tag v-if="record.version > 1" color="blue" :bordered="false" class="source-tag">V{{ record.version }}</a-tag>
            <a-tag v-if="record.source === 'MRP'" color="purple" :bordered="false" class="source-tag">MRP</a-tag>
            <a-tag v-else-if="record.source === 'CONSIGN'" color="cyan" :bordered="false" class="source-tag">{{ $t('scm.order.sourceConsign') }}</a-tag>
            <a-tag v-else-if="record.source === 'DEALER'" color="purple" :bordered="false" class="source-tag">{{ $t('scm.order.sourceDealer') }}</a-tag>
            <a-tag v-else-if="record.source === 'IMPORT'" color="lime" :bordered="false" class="source-tag">{{ $t('scm.order.sourceImport') }}</a-tag>
            <div v-if="record.externalNo" class="sub-text">{{ $t('scm.order.externalNoValue', { no: record.externalNo }) }}</div>
          </template>
          <template v-else-if="column.key === 'partner'">
            <div>{{ record.partnerName }}</div>
            <div class="sub-text">{{ record.partnerCode }}</div>
          </template>
          <template v-else-if="column.key === 'dates'">
            <div>{{ record.orderDate }}</div>
            <div class="sub-text">{{ $t('scm.order.deliveryValue', { date: record.deliveryDate || '-' }) }}</div>
          </template>
          <template v-else-if="column.key === 'totalAmount'">
            {{ currencySymbol(record.currency) }}{{ formatMoney(record.totalAmount) }}
            <div v-if="isForeign(record.currency)" class="sub-text">{{ record.currency }} @{{ Number(record.exchangeRate) }}</div>
          </template>
          <template v-else-if="column.key === 'progress'">
            <a-progress :percent="progress(record)" size="small" :status="progress(record) >= 100 ? 'success' : 'normal'" />
            <div class="sub-text">{{ formatQty(record.doneQty) }} / {{ formatQty(record.totalQty) }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="scm_order_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">
                <template #icon><EyeOutlined /></template>{{ $t('common.action.view') }}
              </a-button>
              <a-button v-if="record.status === 'DRAFT'" v-perm="`scm:${kind}:edit`" type="link" size="small" @click="form.openEdit(record)">
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <a-button
                v-else-if="['APPROVED', 'PARTIAL'].includes(record.status)"
                v-perm="`scm:${conf.trades[0]}:add`"
                type="link"
                size="small"
                @click="openTrade(conf.trades[0], record)"
              >
                <template #icon><component :is="kind === 'purchase' ? ImportOutlined : ExportOutlined" /></template>
                {{ kind === 'purchase' ? $t('scm.order.inbound') : $t('scm.order.outbound') }}
              </a-button>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <OrderForm ref="form" :kind="kind" @success="onSaved" />
    <ImportModal
      v-if="kind === 'sales'"
      v-model:open="importOpen"
      :title="$t('scm.order.externalOrder')"
      :columns="importColumns"
      :importer="importOrders"
      :sample="{ externalNo: 'TB20261015001', customerCode: 'C001', orderDate: '2026-10-15', materialCode: 'M0001', qty: 2, price: 99 }"
      @success="search"
    />
    <OrderDetail
      ref="detailRef"
      v-model:open="detailOpen"
      :kind="kind"
      :order-id="detailId"
      @edit="onEditFromDetail"
      @trade="openTrade"
      @changed="load"
    />
    <TradeDocForm v-for="t in conf.trades" :key="t" :ref="(el) => (tradeForms[t] = el)" :kind="t" @success="onTradeSaved" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, ExportOutlined, EyeOutlined, ImportOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import ImportModal from '@/components/ImportModal.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import OrderDetail from './OrderDetail.vue'
import OrderForm from './OrderForm.vue'
import PartnerSelect from './PartnerSelect.vue'
import TradeDocForm from './TradeDocForm.vue'
import { deleteOrder, pageOrder } from '@/api/scm/order'
import { importSalesOrders } from '@/api/scm/dealer'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { formatQty } from '@/components/wms/wms'
import { ORDER_KINDS, formatMoney } from './scm'
import { currencySymbol, isForeign, useCurrencies } from '@/components/fin/currency'

/**
 * 采购订单 / 销售订单列表页，两个页面共用
 */
const props = defineProps({
  kind: { type: String, required: true }
})

const { t } = useI18n()
const conf = ORDER_KINDS[props.kind]

// 外部订单导入（电商 / 门店）：同一外部单号的多行为一张订单，单价为空按价目表，已导入的外部单号跳过
const importOpen = ref(false)
const importColumns = [
  { title: t('scm.order.externalNo'), key: 'externalNo', required: true },
  { title: t('scm.order.customerCode'), key: 'customerCode', required: true },
  { title: t('scm.order.placedDate'), key: 'orderDate', type: 'date', required: true },
  { title: t('scm.order.deliveryDate'), key: 'deliveryDate', type: 'date' },
  { title: t('scm.order.materialCode'), key: 'materialCode', required: true },
  { title: t('common.field.quantity'), key: 'qty', type: 'number', required: true },
  { title: t('scm.order.taxPrice'), key: 'price', type: 'number' },
  { title: t('common.field.remark'), key: 'remark' }
]
async function importOrders(req) {
  const res = await importSalesOrders(req)
  if (!res.errors.length) {
    message.success(
      res.skipped.length
        ? t('scm.order.importDoneSkipped', { n: res.orders, skipped: res.skipped.length, list: res.skipped.join(t('scm.order.listSep')) })
        : t('scm.order.importDone', { n: res.orders })
    )
  }
  return res
}
const dict = useDict('scm_order_status')
useCurrencies()

const columns = [
  { title: t('scm.order.orderNo'), key: 'orderNo', width: 190 },
  { title: conf.partnerLabel, key: 'partner', width: 200 },
  { title: t('scm.order.orderDeliveryDate'), key: 'dates', width: 140 },
  { title: t('scm.order.totalAmount'), key: 'totalAmount', width: 130, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: props.kind === 'purchase' ? t('scm.order.inProgress') : t('scm.order.outProgress'), key: 'progress', width: 190 },
  { title: conf.warehouseLabel, dataIndex: 'warehouseName', width: 120 },
  { title: t('scm.order.preparedBy'), dataIndex: 'createByName', width: 110 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 150, ellipsis: true },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: 180, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, `scm-${props.kind}`)
const showSearch = ref(true)

const emptyQuery = () => ({ orderNo: undefined, partnerId: undefined, status: undefined, keyword: undefined, range: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange } = useTable(
  (params) => pageOrder(props.kind, params),
  query,
  { params: ({ range, ...rest }) => ({ ...rest, beginDate: range?.[0], endDate: range?.[1] }) }
)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const route = useRoute()
const router = useRouter()
onMounted(() => {
  load()
  // 从请购单、询价单、报价单等跳转过来时（?id=订单ID）直接打开订单详情
  const id = Number(route.query.id)
  if (id) {
    openDetail({ id })
    router.replace({ query: {} })
  }
})

const progress = (r) => (Number(r.totalQty) ? Math.min(Math.round((Number(r.doneQty) / Number(r.totalQty)) * 100), 100) : 0)

const form = ref()
const detailRef = ref()
const detailOpen = ref(false)
const detailId = ref()
const tradeForms = reactive({})

function openDetail(record) {
  detailId.value = record.id
  detailOpen.value = true
}

function onEditFromDetail(record) {
  form.value.openEdit(record)
}

/** 保存后打开详情，方便接着审核 */
function onSaved(id) {
  load()
  // 从详情进入的编辑：返回详情并刷新
  if (detailOpen.value && (!id || id === detailId.value)) detailRef.value?.reload()
  else if (id) openDetail({ id })
}

function openTrade(tradeKind, order) {
  tradeForms[tradeKind]?.openCreate(order)
}

function onTradeSaved() {
  load()
  if (detailOpen.value) detailRef.value?.reload()
}

async function remove(ids) {
  await deleteOrder(props.kind, ids)
  message.success(t('common.msg.deleteSuccess'))
  selectedKeys.value = selectedKeys.value.filter((k) => !ids.includes(k))
  load()
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.source-tag {
  margin-left: 4px;
}
</style>
