<template>
  <div class="packing">
    <div class="packing-head">
      <span class="section-title">{{ totalWeight ? $t('scm.deliveryPacking.headWeight', { n: packages.length, weight: formatQty(totalWeight) }) : $t('scm.deliveryPacking.head', { n: packages.length }) }}</span>
      <a-space wrap>
        <span v-for="s in summary" :key="s.id" :class="['sub-text', { warn: s.packed < s.qty }]">
          {{ $t('scm.deliveryPacking.packedOf', { code: s.materialCode, packed: formatQty(s.packed), qty: formatQty(s.qty) }) }}
        </span>
        <a-button size="small" :disabled="!packages.length" @click="printList"><template #icon><PrinterOutlined /></template>{{ $t('scm.deliveryPacking.packingList') }}</a-button>
        <a-button size="small" :disabled="!packages.length" @click="labelOpen = true"><template #icon><BarcodeOutlined /></template>{{ $t('scm.deliveryPacking.boxLabel') }}</a-button>
        <a-button v-if="editable" v-perm="'scm:delivery:pack'" size="small" type="primary" ghost :disabled="!remainingLines.length" @click="openBox()">
          <template #icon><PlusOutlined /></template>{{ $t('scm.deliveryPacking.packBox') }}
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
            <a-button type="link" size="small" @click="openBox(record)">{{ $t('common.action.edit') }}</a-button>
            <a-popconfirm :title="$t('scm.deliveryPacking.confirmDelete')" @confirm="remove(record)"><a-button type="link" size="small" danger>{{ $t('common.action.delete') }}</a-button></a-popconfirm>
          </span>
        </template>
      </template>
    </a-table>
    <div v-else class="sub-text">{{ editable ? $t('scm.deliveryPacking.emptyEditable') : $t('scm.deliveryPacking.empty') }}</div>

    <a-modal v-model:open="boxOpen" :title="box.id ? $t('scm.deliveryPacking.editBox', { no: box.boxNo }) : $t('scm.deliveryPacking.packBox')" :confirm-loading="saving" @ok="submit">
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
          <a-form-item :label="$t('scm.deliveryPacking.grossWeight')">
            <a-input-number v-model:value="box.weight" :min="0" :precision="3" addon-after="kg" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item :label="$t('scm.deliveryPacking.volume')">
            <a-input-number v-model:value="box.volume" :min="0" :precision="4" addon-after="m³" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item :label="$t('common.field.remark')">
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
import { useI18n } from 'vue-i18n'
import { useAction } from '@/composables/useAction'

/** 发货通知的装箱：已拣货后按箱录入箱内物料、毛重、体积，打印装箱单和箱标签 */
const props = defineProps({
  delivery: { type: Object, required: true },
  editable: Boolean
})
const { t } = useI18n()

const columns = [
  { title: t('scm.deliveryPacking.boxNo'), key: 'box', width: 200 },
  { title: t('scm.deliveryPacking.boxItems'), key: 'items' },
  { title: t('scm.deliveryPacking.weightVolume'), key: 'size', width: 160 },
  { title: '', key: 'action', width: 110 }
]
const lineColumns = [
  { title: t('scm.deliveryPacking.material'), key: 'material' },
  { title: t('scm.deliveryPacking.left'), key: 'left', width: 110 },
  { title: t('scm.deliveryPacking.packQty'), key: 'qty', width: 140 }
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
  if (!items.length) return message.warning(t('scm.deliveryPacking.inputPackQty'))
  saving.value = true
  try {
    const data = { weight: box.value.weight ?? null, volume: box.value.volume ?? null, remark: box.value.remark || null, items }
    if (box.value.id) await updatePackage(props.delivery.id, box.value.id, data)
    else await addPackage(props.delivery.id, data)
    message.success(t('common.msg.saved'))
    boxOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}
const remove = useAction((record) => deletePackage(props.delivery.id, record.id), { successKey: 'common.msg.deleted', reload: () => load() })

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
      title: t('scm.deliveryPacking.packingList'),
      no: d.deliveryNo,
      fields: [
        [t('scm.deliveryPacking.salesOrder'), d.orderNo],
        [t('scm.deliveryPacking.customer'), d.customerName],
        [t('scm.deliveryPacking.shipWarehouse'), d.warehouseName],
        [t('scm.deliveryPacking.shipDate'), d.shipDate],
        [t('scm.deliveryPacking.boxCount'), String(packages.value.length)],
        [t('scm.deliveryPacking.totalWeight'), totalWeight.value ? `${formatQty(totalWeight.value)} kg` : '-'],
        [t('scm.deliveryPacking.totalVolume'), totalVolume ? `${formatQty(totalVolume)} m³` : '-']
      ],
      columns: [
        { title: t('scm.deliveryPacking.boxNo'), value: (r) => r.boxNo },
        { title: t('scm.deliveryPacking.material'), value: (r) => `${r.materialCode} ${r.materialName}${r.spec ? ' ' + r.spec : ''}` },
        { title: t('common.field.quantity'), value: (r) => `${formatQty(r.qty)} ${r.unit || ''}`, align: 'right' },
        { title: t('scm.deliveryPacking.grossWeight'), value: (r) => (r.weight != null ? `${formatQty(r.weight)} kg` : ''), align: 'right' },
        { title: t('scm.deliveryPacking.volume'), value: (r) => (r.volume != null ? `${formatQty(r.volume)} m³` : ''), align: 'right' }
      ],
      rows,
      note: d.remark,
      signs: [t('scm.deliveryPacking.packer'), t('scm.deliveryPacking.reviewer'), t('scm.deliveryPacking.customerSign')]
    },
    `${t('scm.deliveryPacking.packingList')} ${d.deliveryNo}`,
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
