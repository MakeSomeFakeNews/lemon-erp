<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" placeholder="全部状态" allow-clear :options="toOptions(dict.scm_wave_status)" class="filter" @change="search" />
        <WarehouseSelect v-model:value="query.warehouseId" placeholder="全部仓库" allow-clear class="filter" @update:value="search" />
        <a-input-search v-model:value="query.keyword" placeholder="波次号、发货通知号" allow-clear class="filter-wide" @search="search" />
        <span class="sub-text">在「发货通知」中勾选同一仓库的待拣货通知生成波次</span>
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
            {{ record.deliveryCount }} 张
            <div class="sub-text">数量 {{ formatQty(record.totalQty) }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="scm_wave_status" :value="record.status" />
            <div v-if="record.pickTime" class="sub-text">{{ record.pickByName }} {{ record.pickTime }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">{{ record.status === 'PICKING' ? '拣货' : '查看' }}</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <SubPage :open="detailOpen" :title="detail ? `拣货波次 ${detail.waveNo}` : ''" width="min(1100px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space wrap>
          <a-button @click="print"><template #icon><PrinterOutlined /></template>打印拣货单</a-button>
          <template v-if="detail.status === 'PICKING'">
            <span v-perm="'scm:wave:add'">
              <a-popconfirm title="取消波次：未拣货的发货通知移出波次，确定？" @confirm="cancel"><a-button danger>取消波次</a-button></a-popconfirm>
            </span>
            <a-button v-perm="'scm:wave:add'" @click="resuggest">重新生成建议</a-button>
            <span v-perm="'scm:wave:pick'">
              <a-popconfirm :title="completeTip" @confirm="complete">
                <a-button type="primary">完成拣货</a-button>
              </a-popconfirm>
            </span>
          </template>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 4 }" size="small" bordered class="block">
          <a-descriptions-item label="仓库">{{ detail.warehouseName }}</a-descriptions-item>
          <a-descriptions-item label="状态"><DictTag type="scm_wave_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item label="发货通知">{{ detail.deliveryCount }} 张</a-descriptions-item>
          <a-descriptions-item label="创建">{{ detail.createByName }}</a-descriptions-item>
        </a-descriptions>
        <a-alert v-if="pick.shortages.length" type="warning" show-icon class="block">
          <template #message>
            库存不足（完成拣货时跳过）：
            <span v-for="(s, i) in pick.shortages" :key="i">{{ i ? '；' : '' }}{{ s.deliveryNo }} {{ s.materialCode }} 缺 {{ formatQty(s.qty - s.suggested) }} {{ s.unit }}</span>
            。补货后点「重新生成建议」，或把通知移出波次
          </template>
        </a-alert>
        <a-tabs v-model:active-key="tab">
          <a-tab-pane key="pick" :tab="`拣货单（${pick.lines.length}）`">
            <a-table row-key="key" size="small" :columns="pickColumns" :data-source="pickRows" :pagination="false" :scroll="{ x: 900 }">
              <template #bodyCell="{ column, record, index }">
                <template v-if="column.key === 'seq'">{{ index + 1 }}</template>
                <template v-else-if="column.key === 'location'">
                  <b>{{ record.locationCode || '未分库位' }}</b>
                  <div v-if="record.zone" class="sub-text">{{ record.zone }}</div>
                </template>
                <template v-else-if="column.key === 'material'">
                  {{ record.materialCode }} {{ record.materialName }}
                  <div class="sub-text">{{ record.spec }}</div>
                </template>
                <template v-else-if="column.key === 'batch'">
                  {{ record.batchNo || '-' }}
                  <div v-if="record.expiryDate" class="sub-text">有效期 {{ record.expiryDate }}</div>
                </template>
                <template v-else-if="column.key === 'qty'"><b>{{ formatQty(record.qty) }}</b> {{ record.unit }}</template>
                <template v-else-if="column.key === 'sorts'">
                  <a-space wrap :size="4">
                    <a-tag v-for="s in record.sorts" :key="s.deliveryNo">{{ s.deliveryNo }} × {{ formatQty(s.qty) }}</a-tag>
                  </a-space>
                </template>
              </template>
            </a-table>
            <div class="sub-text tip">按库位路径（区域 → 排序 → 编码）排列，先按行拣出合计数量，再按「分拣」放入各发货通知</div>
          </a-tab-pane>
          <a-tab-pane key="deliveries" :tab="`发货通知（${detail.deliveries.length}）`">
            <a-table row-key="id" size="small" :columns="deliveryColumns" :data-source="detail.deliveries" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'"><DictTag type="scm_delivery_status" :value="record.status" /></template>
                <template v-else-if="column.key === 'qty'">{{ formatQty(record.totalQty) }}</template>
                <template v-else-if="column.key === 'action'">
                  <span v-if="detail.status === 'PICKING' && record.status === 'PENDING'" v-perm="'scm:wave:add'">
                    <a-popconfirm title="把该发货通知移出波次？" @confirm="remove(record.id)">
                      <a-button type="link" size="small" danger>移出</a-button>
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

/** 波次拣货：多张发货通知合并拣货，拣货单按库位路径汇总，再按单分拣 */
const dict = useDict('scm_wave_status', 'scm_delivery_status')
const route = useRoute()

const columns = [
  { title: '波次号', key: 'waveNo', width: 200 },
  { title: '仓库', dataIndex: 'warehouseName', width: 140 },
  { title: '发货通知', key: 'count', width: 140 },
  { title: '状态', key: 'status', width: 220 },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'scm-wave')
const query = reactive({ status: undefined, warehouseId: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageWave, query)
onMounted(() => {
  load()
  if (route.query.id) openDetail(Number(route.query.id))
})

const pickColumns = [
  { title: '序号', key: 'seq', width: 60 },
  { title: '库位', key: 'location', width: 130 },
  { title: '物料', key: 'material' },
  { title: '批次', key: 'batch', width: 150 },
  { title: '合计', key: 'qty', width: 110, align: 'right' },
  { title: '分拣', key: 'sorts', width: 260 }
]
const deliveryColumns = [
  { title: '发货通知', dataIndex: 'deliveryNo', width: 170 },
  { title: '销售订单', dataIndex: 'orderNo', width: 170 },
  { title: '客户', dataIndex: 'customerName', ellipsis: true },
  { title: '数量', key: 'qty', width: 90, align: 'right' },
  { title: '状态', key: 'status', width: 90 },
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
async function resuggest() {
  await resuggestWave(detail.value.id)
  message.success('已按当前库存重新生成拣货建议')
  refresh()
}
const completeTip = computed(() => (pick.value.shortages.length
  ? '库存不足的发货通知将跳过（仍为待拣货），其余按拣货单确认拣货，确定？'
  : '按拣货单确认各发货通知的拣货，确定？'))
async function complete() {
  const res = await completeWave(detail.value.id)
  const skipped = [...res.shortage, ...res.serial]
  if (skipped.length) {
    Modal.info({
      title: `已完成 ${res.picked} 张发货通知的拣货`,
      content: `跳过：${res.shortage.length ? `${res.shortage.join('、')} 库存不足；` : ''}${res.serial.length ? `${res.serial.join('、')} 有序列号物料，请在发货通知中录入序列号拣货` : ''}`
    })
  } else {
    message.success(`已完成拣货，${res.picked} 张发货通知可以出库`)
  }
  refresh()
}
async function cancel() {
  await cancelWave(detail.value.id)
  message.success('已取消波次')
  refresh()
}
async function remove(deliveryId) {
  await removeWaveDelivery(detail.value.id, deliveryId)
  message.success('已移出波次')
  refresh()
}

function print() {
  const w = detail.value
  printDocs(
    {
      title: '波次拣货单',
      no: w.waveNo,
      fields: [
        ['仓库', w.warehouseName],
        ['发货通知', w.deliveries.map((d) => d.deliveryNo).join('、')],
        ['制单', w.createByName],
        ['状态', dict.scm_wave_status?.find((o) => o.value === w.status)?.label || w.status]
      ],
      columns: [
        { title: '序号', value: (r, i) => i + 1 },
        { title: '库位', value: (r) => r.locationCode || '未分库位' },
        { title: '物料', value: (r) => `${r.materialCode} ${r.materialName}${r.spec ? ' ' + r.spec : ''}` },
        { title: '批次', value: (r) => r.batchNo || '' },
        { title: '有效期', value: (r) => r.expiryDate || '' },
        { title: '数量', key: 'qty', value: (r) => `${formatQty(r.qty)} ${r.unit || ''}`, align: 'right' },
        { title: '分拣', value: (r) => r.sorts.map((s) => `${s.deliveryNo}×${formatQty(s.qty)}`).join(' ') }
      ],
      rows: pick.value.lines,
      note: w.remark,
      signs: [['制单', w.createByName], '拣货', '复核']
    },
    `波次拣货单 ${w.waveNo}`,
    'scm.wavePick'
  )
}
</script>

<style scoped>
.filter {
  width: 140px;
}
.filter-wide {
  width: 220px;
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
