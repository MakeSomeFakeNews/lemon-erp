<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" :placeholder="$t('scm.wave.allStatus')" allow-clear :options="toOptions(dict.scm_wave_status)" class="filter" @change="search" />
        <WarehouseSelect v-model:value="query.warehouseId" :placeholder="$t('scm.wave.allWarehouse')" allow-clear class="filter" @update:value="search" />
        <a-input-search v-model:value="query.keyword" :placeholder="$t('scm.wave.keywordPh')" allow-clear class="filter-wide" @search="search" />
        <span class="sub-text">{{ $t('scm.wave.toolbarTip') }}</span>
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1000, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'waveNo'">
            <a @click="openDetail(record.id)">{{ record.waveNo }}</a>
            <div class="sub-text">{{ record.createTime }}</div>
          </template>
          <template v-else-if="column.key === 'count'">
            {{ $t('scm.wave.docCount', { n: record.deliveryCount }) }}
            <div class="sub-text">{{ $t('scm.wave.qtyN', { qty: formatQty(record.totalQty) }) }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="scm_wave_status" :value="record.status" />
            <div v-if="record.pickTime" class="sub-text">{{ record.pickByName }} {{ record.pickTime }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">{{ record.status === WaveStatus.PICKING ? $t('scm.wave.pick') : $t('common.action.view') }}</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <SubPage :open="detailOpen" :title="detail ? `${$t('scm.wave.title')} ${detail.waveNo}` : ''" width="min(1100px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space wrap>
          <a-button @click="print"><template #icon><PrinterOutlined /></template>{{ $t('scm.wave.printPickList') }}</a-button>
          <template v-if="detail.status === WaveStatus.PICKING">
            <span v-perm="'scm:wave:add'">
              <a-popconfirm :title="$t('scm.wave.cancelConfirm')" @confirm="cancel"><a-button danger>{{ $t('scm.wave.cancelWave') }}</a-button></a-popconfirm>
            </span>
            <a-button v-perm="'scm:wave:add'" @click="resuggest">{{ $t('scm.wave.resuggest') }}</a-button>
            <span v-perm="'scm:wave:pick'">
              <a-popconfirm :title="completeTip" @confirm="complete">
                <a-button type="primary">{{ $t('scm.wave.completePick') }}</a-button>
              </a-popconfirm>
            </span>
          </template>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 4 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('scm.wave.warehouse')">{{ detail.warehouseName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="scm_wave_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('scm.wave.delivery')">{{ $t('scm.wave.docCount', { n: detail.deliveryCount }) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.wave.createdBy')">{{ detail.createByName }}</a-descriptions-item>
        </a-descriptions>
        <a-alert v-if="pick.shortages.length" type="warning" show-icon class="block">
          <template #message>
            {{ $t('scm.wave.shortageHead') }}
            <span v-for="(s, i) in pick.shortages" :key="i">{{ i ? $t('scm.wave.shortSep') : '' }}{{ $t('scm.wave.shortItem', { no: s.deliveryNo, code: s.materialCode, qty: formatQty(s.qty - s.suggested), unit: s.unit || '' }) }}</span>
            {{ $t('scm.wave.shortageTail') }}
          </template>
        </a-alert>
        <a-tabs v-model:active-key="tab">
          <a-tab-pane key="pick" :tab="$t('scm.wave.pickTab', { n: pick.lines.length })">
            <a-table row-key="key" size="small" :columns="pickColumns" :data-source="pickRows" :pagination="false" :scroll="{ x: 900 }">
              <template #bodyCell="{ column, record, index }">
                <template v-if="column.key === 'seq'">{{ index + 1 }}</template>
                <template v-else-if="column.key === 'location'">
                  <b>{{ record.locationCode || $t('scm.wave.noLocation') }}</b>
                  <div v-if="record.zone" class="sub-text">{{ record.zone }}</div>
                </template>
                <template v-else-if="column.key === 'material'">
                  {{ record.materialCode }} {{ record.materialName }}
                  <div class="sub-text">{{ record.spec }}</div>
                </template>
                <template v-else-if="column.key === 'batch'">
                  {{ record.batchNo || '-' }}
                  <div v-if="record.expiryDate" class="sub-text">{{ $t('scm.wave.expiryN', { date: record.expiryDate }) }}</div>
                </template>
                <template v-else-if="column.key === 'qty'"><b>{{ formatQty(record.qty) }}</b> {{ record.unit }}</template>
                <template v-else-if="column.key === 'sorts'">
                  <a-space wrap :size="4">
                    <a-tag v-for="s in record.sorts" :key="s.deliveryNo">{{ s.deliveryNo }} × {{ formatQty(s.qty) }}</a-tag>
                  </a-space>
                </template>
              </template>
            </a-table>
            <div class="sub-text tip">{{ $t('scm.wave.pickTip') }}</div>
          </a-tab-pane>
          <a-tab-pane key="deliveries" :tab="$t('scm.wave.deliveryTab', { n: detail.deliveries.length })">
            <a-table row-key="id" size="small" :columns="deliveryColumns" :data-source="detail.deliveries" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'"><DictTag type="scm_delivery_status" :value="record.status" /></template>
                <template v-else-if="column.key === 'qty'">{{ formatQty(record.totalQty) }}</template>
                <template v-else-if="column.key === 'action'">
                  <span v-if="detail.status === WaveStatus.PICKING && record.status === DeliveryStatus.PENDING" v-perm="'scm:wave:add'">
                    <a-popconfirm :title="$t('scm.wave.removeConfirm')" @confirm="remove(record.id)">
                      <a-button type="link" size="small" danger>{{ $t('scm.wave.remove') }}</a-button>
                    </a-popconfirm>
                  </span>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
        </a-tabs>
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { PrinterOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { cancelWave, completeWave, getWave, pageWave, removeWaveDelivery, resuggestWave, wavePickList } from '@/api/scm/shipment'
import { formatQty } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { printDocs } from '@/utils/print'
import { useI18n } from 'vue-i18n'
import { useAction } from '@/composables/useAction'

import { DeliveryStatus, WaveStatus } from '@/constants/enums'
/** 波次拣货：多张发货通知合并拣货，拣货单按库位路径汇总，再按单分拣 */
const { t } = useI18n()
const dict = useDict('scm_wave_status', 'scm_delivery_status')
const route = useRoute()

const columns = [
  { title: t('scm.wave.waveNo'), key: 'waveNo', width: 200 },
  { title: t('scm.wave.warehouse'), dataIndex: 'warehouseName', width: 140 },
  { title: t('scm.wave.delivery'), key: 'count', width: 150 },
  { title: t('common.field.status'), key: 'status', width: 220 },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 100, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'scm-wave')
const query = reactive({ status: undefined, warehouseId: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageWave, query)
onMounted(() => {
  load()
  if (route.query.id) openDetail(Number(route.query.id))
})

const pickColumns = [
  { title: t('common.table.index'), key: 'seq', width: 60 },
  { title: t('scm.wave.location'), key: 'location', width: 130 },
  { title: t('scm.wave.material'), key: 'material' },
  { title: t('scm.wave.batch'), key: 'batch', width: 150 },
  { title: t('common.field.total'), key: 'qty', width: 110, align: 'right' },
  { title: t('scm.wave.sort'), key: 'sorts', width: 260 }
]
const deliveryColumns = [
  { title: t('scm.wave.delivery'), dataIndex: 'deliveryNo', width: 170 },
  { title: t('scm.wave.salesOrder'), dataIndex: 'orderNo', width: 170 },
  { title: t('scm.wave.customer'), dataIndex: 'customerName', ellipsis: true },
  { title: t('common.field.quantity'), key: 'qty', width: 90, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: '', key: 'action', width: 70 }
]
const detailOpen = ref(false)
const detail = ref()
const tab = ref('pick')
const pick = ref({ lines: [], shortages: [] })
const pickRows = computed(() => pick.value.lines.map((l, i) => ({ ...l, key: i })))
async function openDetail(id) {
  const [w, p] = await Promise.all([getWave(id), wavePickList(id)])
  detail.value = w
  pick.value = p
  detailOpen.value = true
}
const refresh = () => {
  openDetail(detail.value.id)
  load()
}
const resuggest = useAction(() => resuggestWave(detail.value.id), { successKey: 'scm.wave.resuggested', reload: () => refresh() })
const completeTip = computed(() => (pick.value.shortages.length
  ? t('scm.wave.completeTipShort')
  : t('scm.wave.completeTip')))
async function complete() {
  const res = await completeWave(detail.value.id)
  const skipped = [...res.shortage, ...res.serial]
  if (skipped.length) {
    Modal.info({
      title: t('scm.wave.completedN', { n: res.picked }),
      content: t('scm.wave.skipped', {
        detail: [
          res.shortage.length ? t('scm.wave.skipShortage', { list: res.shortage.join(t('scm.wave.listSep')) }) : '',
          res.serial.length ? t('scm.wave.skipSerial', { list: res.serial.join(t('scm.wave.listSep')) }) : ''
        ].join('')
      })
    })
  } else {
    message.success(t('scm.wave.completedAll', { n: res.picked }))
  }
  refresh()
}
const cancel = useAction(() => cancelWave(detail.value.id), { successKey: 'scm.wave.cancelled', reload: () => refresh() })
const remove = useAction((deliveryId) => removeWaveDelivery(detail.value.id, deliveryId), { successKey: 'scm.wave.removed', reload: () => refresh() })

function print() {
  const w = detail.value
  printDocs(
    {
      title: t('scm.wave.wavePickList'),
      no: w.waveNo,
      fields: [
        [t('scm.wave.warehouse'), w.warehouseName],
        [t('scm.wave.delivery'), w.deliveries.map((d) => d.deliveryNo).join(t('scm.wave.listSep'))],
        [t('scm.wave.prepared'), w.createByName],
        [t('common.field.status'), dict.scm_wave_status?.find((o) => o.value === w.status)?.label || w.status]
      ],
      columns: [
        { title: t('common.table.index'), value: (r, i) => i + 1 },
        { title: t('scm.wave.location'), value: (r) => r.locationCode || t('scm.wave.noLocation') },
        { title: t('scm.wave.material'), value: (r) => `${r.materialCode} ${r.materialName}${r.spec ? ' ' + r.spec : ''}` },
        { title: t('scm.wave.batch'), value: (r) => r.batchNo || '' },
        { title: t('scm.wave.expiryDate'), value: (r) => r.expiryDate || '' },
        { title: t('common.field.quantity'), key: 'qty', value: (r) => `${formatQty(r.qty)} ${r.unit || ''}`, align: 'right' },
        { title: t('scm.wave.sort'), value: (r) => r.sorts.map((s) => `${s.deliveryNo}×${formatQty(s.qty)}`).join(' ') }
      ],
      rows: pick.value.lines,
      note: w.remark,
      signs: [[t('scm.wave.prepared'), w.createByName], t('scm.wave.picker'), t('scm.wave.reviewer')]
    },
    `${t('scm.wave.wavePickList')} ${w.waveNo}`,
    'scm.wavePick'
  )
}
</script>

<style scoped>
.filter {
  width: 160px;
}
.filter-wide {
  width: 240px;
}
.block {
  margin-bottom: 16px;
}
.tip {
  margin-top: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
