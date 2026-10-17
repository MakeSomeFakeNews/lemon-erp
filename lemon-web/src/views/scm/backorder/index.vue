<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="partnerLabel" name="partnerId">
          <PartnerSelect
            :key="query.kind"
            v-model:value="query.partnerId"
            :type="query.kind === 'SALES' ? 'customer' : 'supplier'"
            :placeholder="$t('common.placeholder.all')"
            allow-clear
          />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('scm.backorder.keywordPh')" allow-clear />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :exporter="(cols) => exportAll(cols, query.kind === 'SALES' ? $t('scm.backorder.sales') : $t('scm.backorder.purchase'))"
        :columns="columns"
        @refresh="load"
      >
        <a-radio-group v-model:value="query.kind" button-style="solid" @change="switchKind">
          <a-radio-button value="SALES">{{ $t('scm.backorder.sales') }}</a-radio-button>
          <a-radio-button value="PURCHASE">{{ $t('scm.backorder.purchase') }}</a-radio-button>
        </a-radio-group>
        <a-checkbox v-model:checked="query.overdueOnly" class="toolbar-check" @change="search">{{ $t('scm.backorder.overdueOnly') }}</a-checkbox>
      </TableToolbar>
      <a-table
        row-key="orderItemId"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1500, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'orderNo'">
            <a @click="toOrder(record)">{{ record.orderNo }}</a>
          </template>
          <template v-else-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ record.spec }}</div>
          </template>
          <template v-else-if="column.key === 'qty'">
            {{ formatQty(record.qty) }}
            <div class="sub-text">{{ query.kind === 'SALES' ? $t('scm.backorder.shippedN', { qty: formatQty(record.doneQty) }) : $t('scm.backorder.receivedN', { qty: formatQty(record.doneQty) }) }}</div>
          </template>
          <template v-else-if="column.key === 'openQty'">
            <b>{{ formatQty(record.openQty) }}</b> <span class="sub-text">{{ record.unit }}</span>
          </template>
          <template v-else-if="column.key === 'overdue'">
            <span v-if="overdueDays(record) > 0" class="overdue">{{ $t('scm.backorder.overdueDays', { n: overdueDays(record) }) }}</span>
            <span v-else-if="record.deliveryDate" class="sub-text">{{ $t('scm.backorder.dueInDays', { n: -overdueDays(record) }) }}</span>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'reservedQty'">{{ formatQty(record.reservedQty) }}</template>
          <template v-else-if="column.key === 'availableQty'">
            {{ formatQty(record.availableQty) }}
            <a-tag v-if="shortage(record)" color="red" :bordered="false" class="short-tag">{{ $t('scm.backorder.shortage') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openAtp(record)">
              <template #icon><FieldTimeOutlined /></template>{{ $t('scm.backorder.atp') }}
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <AtpModal
      v-model:open="atpOpen"
      :material-id="atpRow?.materialId"
      :material-label="atpRow ? `${atpRow.materialCode} ${atpRow.materialName}` : ''"
      :qty="atpRow ? Number(atpRow.openQty) - Number(atpRow.reservedQty || 0) : undefined"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { FieldTimeOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import AtpModal from '@/components/scm/AtpModal.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { pageBackorder } from '@/api/scm/planning'
import { formatQty } from '@/components/wms/wms'
import { columnKey, useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const router = useRouter()

const overdueDays = (r) => (r.deliveryDate ? dayjs().startOf('day').diff(dayjs(r.deliveryDate), 'day') : 0)
/** 销售欠交：可用库存不够发未预留的部分 */
const shortage = (r) => Number(r.availableQty || 0) < Number(r.openQty || 0) - Number(r.reservedQty || 0)

const emptyQuery = () => ({ kind: 'SALES', overdueOnly: false, keyword: undefined, partnerId: undefined })
const query = reactive(emptyQuery())
const partnerLabel = computed(() => (query.kind === 'SALES' ? t('scm.backorder.customer') : t('scm.backorder.supplier')))

/** 列按欠交类型生成：销售多出预留、可用库存和可承诺量 */
function buildColumns(kind) {
  const sales = kind === 'SALES'
  return [
    { title: t('scm.backorder.orderNo'), key: 'orderNo', dataIndex: 'orderNo', width: 180 },
    { title: t('scm.backorder.orderDate'), dataIndex: 'orderDate', width: 120 },
    { title: sales ? t('scm.backorder.customer') : t('scm.backorder.supplier'), dataIndex: 'partnerName', width: 180, ellipsis: true },
    { title: t('scm.backorder.material'), key: 'material', width: 240, exportValue: (r) => `${r.materialCode} ${r.materialName}` },
    { title: t('common.field.unit'), dataIndex: 'unit', width: 70 },
    { title: t('scm.backorder.orderQty'), key: 'qty', dataIndex: 'qty', width: 120, align: 'right' },
    { title: sales ? t('scm.backorder.unshipped') : t('scm.backorder.unreceived'), key: 'openQty', dataIndex: 'openQty', width: 120, align: 'right' },
    { title: t('scm.backorder.deliveryDate'), dataIndex: 'deliveryDate', width: 120 },
    { title: t('scm.backorder.overdue'), key: 'overdue', width: 120, exportValue: (r) => (overdueDays(r) > 0 ? overdueDays(r) : '') },
    ...(sales
      ? [
          { title: t('scm.backorder.reserved'), key: 'reservedQty', dataIndex: 'reservedQty', width: 100, align: 'right' },
          { title: t('scm.backorder.available'), key: 'availableQty', dataIndex: 'availableQty', width: 140, align: 'right' }
        ]
      : []),
    { title: t('scm.backorder.createBy'), dataIndex: 'createByName', width: 120 },
    ...(sales ? [{ title: t('common.action.operation'), key: 'action', width: 150, fixed: 'right' }] : [])
  ]
}
const columns = computed(() => buildColumns(query.kind))
const { checked } = useColumns(buildColumns('SALES'), 'scm-backorder')
const visibleColumns = computed(() => columns.value.filter((c) => checked.value.includes(columnKey(c))))
const showSearch = ref(true)

const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageBackorder, query)
onMounted(load)

function switchKind() {
  query.partnerId = undefined
  list.value = []
  search()
}

function resetQuery() {
  Object.assign(query, { ...emptyQuery(), kind: query.kind })
  search()
}

const toOrder = (r) => router.push({ path: r.kind === 'PURCHASE' || query.kind === 'PURCHASE' ? '/scm/purchase' : '/scm/sales', query: { id: r.orderId } })

const atpOpen = ref(false)
const atpRow = ref()
function openAtp(record) {
  atpRow.value = record
  atpOpen.value = true
}
</script>

<style scoped>
.overdue {
  color: #ff4d4f;
  font-weight: 500;
}
.short-tag {
  margin-left: 4px;
}
.toolbar-check {
  margin-left: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
