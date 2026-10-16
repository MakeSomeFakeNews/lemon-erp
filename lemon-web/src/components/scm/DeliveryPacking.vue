<template>
  <div class="packing">
    <div class="packing-head">
      <span class="section-title">装箱（{{ packages.length }} 箱{{ totalWeight ? `，毛重 ${formatQty(totalWeight)} kg` : '' }}）</span>
      <a-space wrap>
        <span v-for="s in summary" :key="s.id" :class="['sub-text', { warn: s.packed < s.qty }]">
          {{ s.materialCode }} 已装 {{ formatQty(s.packed) }} / {{ formatQty(s.qty) }}
        </span>
        <a-button size="small" :disabled="!packages.length" @click="printList"><template #icon><PrinterOutlined /></template>装箱单</a-button>
        <a-button size="small" :disabled="!packages.length" @click="labelOpen = true"><template #icon><BarcodeOutlined /></template>箱标签</a-button>
        <a-button v-if="editable" v-perm="'scm:delivery:pack'" size="small" type="primary" ghost :disabled="!remainingLines.length" @click="openBox()">
          <template #icon><PlusOutlined /></template>装一箱
        </a-button>
      </a-space>
    </div>
    <a-table v-if="packages.length" row-key="id" size="small" :columns="columns" :data-source="packages" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'box'"><b>{{ record.boxNo }}</b><div v-if="record.remark" class="sub-text">{{ record.remark }}</div></template>
        <template v-else-if="column.key === 'items'">
          <div v-for="i in record.items" :key="i.id">{{ i.materialCode }} {{ i.materialName }} × {{ formatQty(i.qty) }} {{ i.unit }}</div>
        </template>
        <template v-else-if="column.key === 'size'">
          {{ record.weight != null ? `${formatQty(record.weight)} kg` : '-' }}
          <div v-if="record.volume != null" class="sub-text">{{ formatQty(record.volume) }} m³</div>
        </template>
        <template v-else-if="column.key === 'action'">
          <span v-if="editable" v-perm="'scm:delivery:pack'" class="table-actions">
            <a-button type="link" size="small" @click="openBox(record)">编辑</a-button>
            <a-popconfirm title="删除该箱？" @confirm="remove(record)"><a-button type="link" size="small" danger>删除</a-button></a-popconfirm>
          </span>
        </template>
      </template>
    </a-table>
    <div v-else class="sub-text">还没有装箱{{ editable ? '，点「装一箱」录入箱内物料、重量和体积' : '' }}</div>

    <a-modal v-model:open="boxOpen" :title="box.id ? `编辑 ${box.boxNo}` : '装一箱'" :confirm-loading="saving" @ok="submit">
      <a-table row-key="deliveryItemId" size="small" :columns="lineColumns" :data-source="box.lines" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
          <template v-else-if="column.key === 'left'">{{ formatQty(record.max) }} {{ record.unit }}</template>
          <template v-else-if="column.key === 'qty'">
            <a-input-number v-model:value="record.qty" :min="0" :max="record.max" class="full" />
          </template>
        </template>
      </a-table>
      <a-row :gutter="12" class="box-form">
        <a-col :span="8">
          <a-form-item label="毛重">
            <a-input-number v-model:value="box.weight" :min="0" :precision="3" addon-after="kg" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="体积">
            <a-input-number v-model:value="box.volume" :min="0" :precision="4" addon-after="m³" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="备注">
            <a-input v-model:value="box.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-modal>
    <LabelPrintModal v-model:open="labelOpen" type-key="label.box" :items="boxLabels" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { BarcodeOutlined, PlusOutlined, PrinterOutlined } from '@ant-design/icons-vue'
import LabelPrintModal from '@/components/label/LabelPrintModal.vue'
import { addPackage, deletePackage, deliveryPackages, updatePackage } from '@/api/scm/planning'
import { formatQty } from '@/components/wms/wms'
import { printDocs } from '@/utils/print'

/** 发货通知的装箱：已拣货后按箱录入箱内物料、毛重、体积，打印装箱单和箱标签 */
const props = defineProps({
  delivery: { type: Object, required: true },
  editable: Boolean
})

const columns = [
  { title: '箱号', key: 'box', width: 200 },
  { title: '箱内物料', key: 'items' },
  { title: '毛重 / 体积', key: 'size', width: 130 },
  { title: '', key: 'action', width: 110 }
]
const lineColumns = [
  { title: '物料', key: 'material' },
  { title: '可装', key: 'left', width: 100 },
  { title: '装入数量', key: 'qty', width: 140 }
]
const packages = ref([])
async function load() {
  packages.value = await deliveryPackages(props.delivery.id)
}
watch(() => props.delivery?.id, (id) => id && load(), { immediate: true })

/** 各明细的装箱进度 */
const packedOf = (itemId, exclude) => packages.value.filter((p) => p.id !== exclude)
  .flatMap((p) => p.items).filter((i) => i.deliveryItemId === itemId).reduce((s, i) => s + Number(i.qty), 0)
const summary = computed(() => props.delivery.items.map((i) => ({ id: i.id, materialCode: i.materialCode, qty: Number(i.qty), packed: packedOf(i.id) })))
const remainingLines = computed(() => summary.value.filter((s) => s.packed < s.qty))
const totalWeight = computed(() => packages.value.reduce((s, p) => s + Number(p.weight || 0), 0))

const boxOpen = ref(false)
const saving = ref(false)
const box = ref({ lines: [] })
function openBox(record) {
  const lines = props.delivery.items.map((i) => {
    const max = Number(i.qty) - packedOf(i.id, record?.id)
    const current = record?.items.find((x) => x.deliveryItemId === i.id)
    return { deliveryItemId: i.id, materialCode: i.materialCode, materialName: i.materialName, unit: i.unit, max,
      qty: current ? Number(current.qty) : record ? undefined : max }
  }).filter((l) => l.max > 0 || l.qty)
  box.value = { id: record?.id, boxNo: record?.boxNo, weight: record?.weight != null ? Number(record.weight) : undefined,
    volume: record?.volume != null ? Number(record.volume) : undefined, remark: record?.remark || '', lines }
  boxOpen.value = true
}
async function submit() {
  const items = box.value.lines.filter((l) => Number(l.qty) > 0).map((l) => ({ deliveryItemId: l.deliveryItemId, qty: l.qty }))
  if (!items.length) return message.warning('请填写装入数量')
  saving.value = true
  try {
    const data = { weight: box.value.weight ?? null, volume: box.value.volume ?? null, remark: box.value.remark || null, items }
    if (box.value.id) await updatePackage(props.delivery.id, box.value.id, data)
    else await addPackage(props.delivery.id, data)
    message.success('已保存')
    boxOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function remove(record) {
  await deletePackage(props.delivery.id, record.id)
  message.success('已删除')
  load()
}

const boxLabels = computed(() => packages.value.map((p) => ({
  boxNo: p.boxNo, customer: props.delivery.customerName, deliveryNo: props.delivery.deliveryNo, seq: `${p.seq} / ${packages.value.length}`,
  contents: p.items.map((i) => `${i.materialCode} × ${formatQty(i.qty)}`).join('、'), weight: p.weight != null ? `${formatQty(p.weight)} kg` : ''
})))
const labelOpen = ref(false)

function printList() {
  const d = props.delivery
  const rows = packages.value.flatMap((p) => p.items.map((i, idx) => ({ ...i, boxNo: idx === 0 ? p.boxNo : '', weight: idx === 0 ? p.weight : null,
    volume: idx === 0 ? p.volume : null })))
  const totalVolume = packages.value.reduce((s, p) => s + Number(p.volume || 0), 0)
  printDocs(
    {
      title: '装箱单',
      no: d.deliveryNo,
      fields: [
        ['销售订单', d.orderNo],
        ['客户', d.customerName],
        ['发货仓库', d.warehouseName],
        ['发货日期', d.shipDate],
        ['箱数', String(packages.value.length)],
        ['总毛重', totalWeight.value ? `${formatQty(totalWeight.value)} kg` : '-'],
        ['总体积', totalVolume ? `${formatQty(totalVolume)} m³` : '-']
      ],
      columns: [
        { title: '箱号', value: (r) => r.boxNo },
        { title: '物料', value: (r) => `${r.materialCode} ${r.materialName}${r.spec ? ' ' + r.spec : ''}` },
        { title: '数量', value: (r) => `${formatQty(r.qty)} ${r.unit || ''}`, align: 'right' },
        { title: '毛重', value: (r) => (r.weight != null ? `${formatQty(r.weight)} kg` : ''), align: 'right' },
        { title: '体积', value: (r) => (r.volume != null ? `${formatQty(r.volume)} m³` : ''), align: 'right' }
      ],
      rows,
      note: d.remark,
      signs: ['装箱', '复核', '客户签收']
    },
    `装箱单 ${d.deliveryNo}`,
    'scm.packingList'
  )
}
</script>

<style scoped>
.packing {
  margin-top: 16px;
}
.packing-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.section-title {
  font-weight: 600;
}
.box-form {
  margin-top: 12px;
}
.full {
  width: 100%;
}
.warn {
  color: var(--ant-color-warning, #faad14);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
