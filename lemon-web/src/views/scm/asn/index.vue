<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" :placeholder="$t('scm.asn.allStatus')" allow-clear :options="toOptions(dict.scm_asn_status)" class="filter" @change="search" />
        <a-input-search v-model:value="query.keyword" :placeholder="$t('scm.asn.keywordPh')" allow-clear class="filter-wide" @search="search" />
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1100, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'asnNo'">
            <a @click="openDetail(record.id)">{{ record.asnNo }}</a>
            <div class="sub-text">{{ $t('scm.asn.orderN', { no: record.orderNo }) }}</div>
          </template>
          <template v-else-if="column.key === 'dates'">
            <div>{{ $t('scm.asn.shipN', { date: record.shipDate }) }}</div>
            <div class="sub-text">{{ $t('scm.asn.expectedN', { date: record.expectedDate || '-' }) }}</div>
          </template>
          <template v-else-if="column.key === 'logistics'">
            {{ record.carrier || '-' }}
            <div class="sub-text">{{ record.trackingNo }}</div>
          </template>
          <template v-else-if="column.key === 'qty'">{{ formatQty(record.totalQty) }}</template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="scm_asn_status" :value="record.status" />
            <div v-if="record.tradeDocNo" class="sub-text">{{ $t('scm.asn.inboundN', { no: record.tradeDocNo }) }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record.id)">{{ $t('common.action.view') }}</a-button>
              <a-button v-if="record.status === AsnStatus.SUBMITTED" v-perm="'scm:asn:receive'" type="link" size="small" @click="openReceive(record.id)">{{ $t('scm.asn.receive') }}</a-button>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <SubPage :open="detailOpen" :title="detail ? `${$t('scm.asn.title')} ${detail.asnNo}` : ''" width="min(900px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-button v-if="detail.status === AsnStatus.SUBMITTED" v-perm="'scm:asn:receive'" type="primary" @click="openReceive(detail.id)">{{ $t('scm.asn.receive') }}</a-button>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('scm.asn.supplier')">{{ detail.supplierName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.asn.purchaseOrder')">{{ detail.orderNo }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="scm_asn_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('scm.asn.shipDate')">{{ detail.shipDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.asn.expectedDate')">{{ detail.expectedDate || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.asn.logistics')">{{ detail.carrier || '-' }} {{ detail.trackingNo || '' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.tradeDocNo" :label="$t('scm.asn.purchaseIn')">{{ detail.tradeDocNo }} {{ detail.receivedTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="2">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}<div class="sub-text">{{ record.spec }}</div></template>
            <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
          </template>
        </a-table>
      </template>
    </SubPage>

    <a-modal v-model:open="receiveOpen" :title="`${$t('scm.asn.receive')} - ${receiving?.asnNo || ''}`" width="760px" :confirm-loading="saving" @ok="submitReceive">
      <a-alert type="info" show-icon class="block" :message="$t('scm.asn.receiveTip')" />
      <a-form layout="vertical">
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item :label="$t('scm.asn.receiveWarehouse')" required>
              <WarehouseSelect v-model:value="receive.warehouseId" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('scm.asn.receiveDate')" required>
              <a-date-picker v-model:value="receive.bizDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <a-table row-key="asnItemId" size="small" :columns="receiveColumns" :data-source="receive.lines" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">{{ record.label }}<div v-if="record.batchNo" class="sub-text">{{ $t('scm.asn.batchN', { batch: record.batchNo }) }}</div></template>
          <template v-else-if="column.key === 'shipped'">{{ formatQty(record.shipped) }}</template>
          <template v-else-if="column.key === 'qty'">
            <a-input-number v-model:value="record.qty" :min="0" class="full" />
          </template>
          <template v-else-if="column.key === 'location'">
            <LocationSelect v-model:value="record.locationId" :warehouse-id="receive.warehouseId" class="full" />
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import LocationSelect from '@/components/wms/LocationSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { getAsn, pageAsn, receiveAsn } from '@/api/scm/srm'
import { formatQty, today } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { useI18n } from 'vue-i18n'

import { AsnStatus } from '@/constants/enums'
/** 送货通知：供应商在门户按采购订单发起，仓库按送货通知收货生成采购入库 */
const { t } = useI18n()
const dict = useDict('scm_asn_status')

const columns = [
  { title: t('scm.asn.asnNo'), key: 'asnNo', width: 190 },
  { title: t('scm.asn.supplier'), dataIndex: 'supplierName', width: 200, ellipsis: true },
  { title: t('common.field.date'), key: 'dates', width: 190 },
  { title: t('scm.asn.logistics'), key: 'logistics', width: 160 },
  { title: t('scm.asn.shipQty'), key: 'qty', width: 110, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 160 },
  { title: t('common.action.operation'), key: 'action', width: 130, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'scm-asn')
const query = reactive({ status: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageAsn, query)
onMounted(load)

const itemColumns = [
  { title: t('scm.asn.material'), key: 'material' },
  { title: t('scm.asn.batch'), dataIndex: 'batchNo', width: 140 },
  { title: t('scm.asn.shipQty'), key: 'qty', width: 140 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true }
]
const detailOpen = ref(false)
const detail = ref()
async function openDetail(id) {
  detail.value = await getAsn(id)
  detailOpen.value = true
}

const receiveColumns = [
  { title: t('scm.asn.material'), key: 'material' },
  { title: t('scm.asn.shipQty'), key: 'shipped', width: 110 },
  { title: t('scm.asn.receivedQty'), key: 'qty', width: 130 },
  { title: t('scm.asn.location'), key: 'location', width: 180 }
]
const receiveOpen = ref(false)
const receiving = ref()
const receive = ref({})
const saving = ref(false)
async function openReceive(id) {
  const a = await getAsn(id)
  receiving.value = a
  receive.value = {
    warehouseId: a.warehouseId, bizDate: today(),
    lines: a.items.map((i) => ({ asnItemId: i.id, label: `${i.materialCode} ${i.materialName}`, batchNo: i.batchNo, shipped: Number(i.qty),
      qty: Number(i.qty), locationId: undefined }))
  }
  receiveOpen.value = true
}
async function submitReceive() {
  if (!receive.value.warehouseId) return message.warning(t('scm.asn.selectWarehouse'))
  saving.value = true
  try {
    await receiveAsn(receiving.value.id, {
      warehouseId: receive.value.warehouseId,
      bizDate: receive.value.bizDate,
      lines: receive.value.lines.map((l) => ({ asnItemId: l.asnItemId, qty: l.qty ?? 0, locationId: l.locationId || null }))
    })
    message.success(t('scm.asn.received'))
    receiveOpen.value = false
    load()
    if (detailOpen.value) detail.value = await getAsn(receiving.value.id)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.filter {
  width: 160px;
}
.filter-wide {
  width: 300px;
}
.full {
  width: 100%;
}
.block {
  margin-bottom: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
