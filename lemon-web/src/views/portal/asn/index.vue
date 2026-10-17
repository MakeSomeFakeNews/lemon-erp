<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" :placeholder="$t('portal.asn.allStatus')" allow-clear :options="toOptions(dict.scm_asn_status)" class="filter" @change="search" />
        <a-input-search v-model:value="query.keyword" :placeholder="$t('portal.asn.searchPlaceholder')" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <span class="sub-text">{{ $t('portal.asn.tip') }}</span>
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
            <div class="sub-text">{{ $t('portal.asn.orderNo', { no: record.orderNo }) }}</div>
          </template>
          <template v-else-if="column.key === 'logistics'">{{ record.carrier || '-' }}<div class="sub-text">{{ record.trackingNo }}</div></template>
          <template v-else-if="column.key === 'qty'">{{ formatQty(record.totalQty) }}</template>
          <template v-else-if="column.key === 'status'"><DictTag type="scm_asn_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record.id)">{{ $t('common.action.view') }}</a-button>
              <span v-if="record.status === AsnStatus.SUBMITTED" v-perm="'portal:asn:add'">
                <a-popconfirm :title="$t('portal.asn.confirmCancel')" @confirm="cancel(record.id)">
                  <a-button type="link" danger size="small">{{ $t('common.action.cancel') }}</a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <SubPage :open="detailOpen" :title="detail ? $t('portal.asn.detailTitle', { no: detail.asnNo }) : ''" width="min(860px, 100vw)" @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('portal.asn.purchaseOrder')">{{ detail.orderNo }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="scm_asn_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('portal.asn.receivedTime')">{{ detail.receivedTime || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('portal.asn.shipDate')">{{ detail.shipDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('portal.asn.expectedDate')">{{ detail.expectedDate || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('portal.asn.logistics')">{{ detail.carrier || '-' }} {{ detail.trackingNo || '' }}</a-descriptions-item>
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
import { useI18n } from 'vue-i18n'
import { onMounted, reactive, ref } from 'vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { cancelPortalAsn, getPortalAsn, pagePortalAsn } from '@/api/portal'
import { formatQty } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { useAction } from '@/composables/useAction'

import { AsnStatus } from '@/constants/enums'
const { t } = useI18n()

/** 供应商门户 - 送货通知 */
const dict = useDict('scm_asn_status')
const columns = [
  { title: t('portal.asn.asnNo'), key: 'asnNo', width: 190 },
  { title: t('portal.asn.shipDate'), dataIndex: 'shipDate', width: 110 },
  { title: t('portal.asn.expectedDate'), dataIndex: 'expectedDate', width: 130 },
  { title: t('portal.asn.logistics'), key: 'logistics', width: 160 },
  { title: t('common.field.quantity'), key: 'qty', width: 100, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('common.action.operation'), key: 'action', width: 130, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'portal-asn')
const query = reactive({ status: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pagePortalAsn, query)
onMounted(load)

const itemColumns = [
  { title: t('portal.asn.material'), key: 'material' },
  { title: t('portal.asn.batch'), dataIndex: 'batchNo', width: 140 },
  { title: t('portal.asn.qty'), key: 'qty', width: 140 }
]
const detailOpen = ref(false)
const detail = ref()
async function openDetail(id) {
  detail.value = await getPortalAsn(id)
  detailOpen.value = true
}
const cancel = useAction(cancelPortalAsn, { successKey: 'portal.asn.canceled', reload: () => load() })
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
