<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" class="kit-tabs">
        <!-- ---------------------------------------------------------------- 按工单 -->
        <a-tab-pane key="orders" :tab="$t('mes.kit.ordersTab')">
          <div class="toolbar">
            <a-input-search v-model:value="orderQuery.woNo" :placeholder="$t('mes.wo.woNo')" allow-clear class="search" @search="loadOrders" />
            <a-radio-group v-model:value="orderQuery.kitStatus" button-style="solid" size="small">
              <a-radio-button value="">{{ $t('common.status.all') }} {{ orders.length }}</a-radio-button>
              <a-radio-button v-for="s in KIT_STATUSES" :key="s" :value="s">{{ dictLabel(dict.mes_kit_status, s) }} {{ countOf(s) }}</a-radio-button>
            </a-radio-group>
            <span class="spacer" />
            <span class="sub-text">{{ $t('mes.kit.priorityTip') }}</span>
            <a-button @click="loadOrders"><template #icon><ReloadOutlined /></template></a-button>
          </div>
          <a-table
            row-key="woId"
            size="middle"
            :columns="orderColumns"
            :data-source="filteredOrders"
            :loading="ordersLoading"
            :pagination="{ pageSize: 20, showSizeChanger: false, showTotal: (n) => $t('mes.kit.totalWo', { n }) }"
            :scroll="{ x: 1150 }"
            :expanded-row-keys="expandedOrders"
            @expand="onExpandOrder"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'wo'">
                <div>{{ record.woNo }}</div>
                <DictTag type="mes_wo_status" :value="record.status" />
              </template>
              <template v-else-if="column.key === 'product'">
                <div>{{ record.productCode }} {{ record.productName }}</div>
                <div class="sub-text">{{ $t('mes.kit.finished', { done: formatQty(record.finishedQty), qty: formatQty(record.qty), unit: record.unit || '' }) }}</div>
              </template>
              <template v-else-if="column.key === 'kitRate'">
                <a-progress :percent="Number(record.kitRate)" size="small" :status="Number(record.kitRate) >= 100 ? 'success' : 'exception'" />
                <div class="sub-text">{{ $t('mes.kit.shortLines', { short: record.shortLineCount, total: record.lineCount }) }}</div>
              </template>
              <template v-else-if="column.key === 'buildableQty'">
                {{ formatQty(record.buildableQty) }} <span class="sub-text">/ {{ formatQty(record.qty) }}</span>
              </template>
              <template v-else-if="column.key === 'kitStatus'">
                <DictTag type="mes_kit_status" :value="record.kitStatus" />
              </template>
            </template>
            <template #expandedRowRender="{ record }">
              <a-table
                row-key="woMaterialId"
                size="small"
                :columns="lineColumns"
                :data-source="orderLines[record.woId] || []"
                :loading="!orderLines[record.woId]"
                :pagination="false"
                :scroll="{ x: 1000 }"
              >
                <template #bodyCell="{ column, record: line }">
                  <template v-if="column.key === 'material'">
                    <div>{{ line.materialCode }} {{ line.materialName }}</div>
                    <div class="sub-text">{{ line.spec || '' }}</div>
                  </template>
                  <template v-else-if="column.key === 'shortageQty'">
                    <span :class="{ danger: Number(line.shortageQty) > 0 }">{{ formatQty(line.shortageQty) }}</span>
                  </template>
                  <template v-else-if="QTY_KEYS.includes(column.key)">{{ formatQty(line[column.key]) }}</template>
                  <template v-else-if="column.key === 'action'">
                    <a-button
                      v-if="Number(line.shortageQty) > 0 && line.substitutes?.length && canSubstitute"
                      type="link"
                      size="small"
                      @click="openSubstitute(record, line)"
                    >
                      {{ $t('mes.kit.substitutesN', { n: line.substitutes.length }) }}
                    </a-button>
                    <span v-else-if="Number(line.shortageQty) > 0 && line.substitutes?.length" class="sub-text">{{ $t('mes.kit.hasSubstitute') }}</span>
                  </template>
                </template>
              </a-table>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- ---------------------------------------------------------------- 按物料 -->
        <a-tab-pane key="materials" :tab="$t('mes.kit.materialsTab')">
          <div class="toolbar">
            <a-input-search v-model:value="materialQuery.keyword" :placeholder="$t('mes.bom.materialKeyword')" allow-clear class="search" @search="loadMaterials" />
            <a-checkbox v-model:checked="materialQuery.onlyShort" @change="loadMaterials">{{ $t('mes.kit.onlyShort') }}</a-checkbox>
            <span class="spacer" />
            <a-button v-perm="'mes:kit:release'" type="primary" :disabled="!selectedMaterials.length" @click="releaseOpen = true">
              <template #icon><ShoppingCartOutlined /></template>{{ $t('mes.kit.releaseN', { n: selectedMaterials.length }) }}
            </a-button>
            <a-button @click="loadMaterials"><template #icon><ReloadOutlined /></template></a-button>
          </div>
          <a-table
            row-key="materialId"
            size="middle"
            :columns="materialColumns"
            :data-source="materials"
            :loading="materialsLoading"
            :pagination="{ pageSize: 20, showSizeChanger: false, showTotal: (n) => $t('mes.kit.totalMaterial', { n }) }"
            :scroll="{ x: 1110 }"
            :row-selection="canRelease ? materialSelection : undefined"
            :expanded-row-keys="expandedMaterials"
            @expand="onExpandMaterial"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'material'">
                <div>{{ record.materialCode }} {{ record.materialName }}</div>
                <div class="sub-text">{{ [record.spec, record.unit].filter(Boolean).join(' · ') }}</div>
              </template>
              <template v-else-if="column.key === 'supplyType'">
                <DictTag type="mes_supply_type" :value="record.supplyType" />
              </template>
              <template v-else-if="column.key === 'shortageQty'">
                <span :class="{ danger: Number(record.shortageQty) > 0 }">{{ formatQty(record.shortageQty) }}</span>
                <div v-if="record.requiredDate && Number(record.shortageQty) > 0" class="sub-text">{{ $t('mes.kit.earliest', { d: record.requiredDate }) }}</div>
              </template>
              <template v-else-if="column.key === 'suggestQty'">
                <span v-if="Number(record.suggestQty) > 0" class="strong">{{ formatQty(record.suggestQty) }} {{ record.unit }}</span>
                <span v-else class="sub-text">-</span>
                <div v-if="Number(record.suggestQty) > 0" class="sub-text">
                  {{ record.supplyType === 'MAKE' ? $t('mes.kit.genWo') : record.supplierName || $t('mes.kit.noSupplier') }}
                </div>
              </template>
              <template v-else-if="QTY_KEYS.includes(column.key)">{{ formatQty(record[column.key]) }}</template>
            </template>
            <template #expandedRowRender="{ record }">
              <a-table
                row-key="woMaterialId"
                size="small"
                :columns="materialLineColumns"
                :data-source="materialLines[record.materialId] || []"
                :loading="!materialLines[record.materialId]"
                :pagination="false"
                :scroll="{ x: 900 }"
              >
                <template #bodyCell="{ column, record: line }">
                  <template v-if="column.key === 'wo'">
                    {{ line.woNo }} <DictTag type="mes_wo_status" :value="line.woStatus" />
                  </template>
                  <template v-else-if="column.key === 'product'">{{ line.productCode }} {{ line.productName }}</template>
                  <template v-else-if="column.key === 'shortageQty'">
                    <span :class="{ danger: Number(line.shortageQty) > 0 }">{{ formatQty(line.shortageQty) }}</span>
                  </template>
                  <template v-else-if="QTY_KEYS.includes(column.key)">{{ formatQty(line[column.key]) }}</template>
                </template>
              </a-table>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 缺料下单 -->
    <a-modal v-model:open="releaseOpen" :title="$t('mes.kit.release')" :confirm-loading="releasing" :ok-text="$t('mes.kit.genDocs')" @ok="submitRelease">
      <p class="sub-text">
        {{ $t('mes.kit.releaseTip') }}
      </p>
      <a-table row-key="materialId" size="small" :columns="releaseColumns" :data-source="selectedMaterials" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
          <template v-else-if="column.key === 'suggestQty'">{{ formatQty(record.suggestQty) }} {{ record.unit }}</template>
          <template v-else-if="column.key === 'to'">
            {{ record.supplyType === 'MAKE' ? $t('mes.wo.wo') : record.supplierName || $t('mes.kit.noSupplier') }}
          </template>
        </template>
      </a-table>
      <a-form layout="vertical" class="release-form">
        <a-form-item :label="$t('mes.kit.warehouse')" required>
          <WarehouseSelect v-model:value="releaseWarehouse" :allow-clear="false" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 替代料 -->
    <a-modal v-model:open="subOpen" :title="$t('mes.kit.subTitle')" :confirm-loading="switching" :ok-text="$t('mes.kit.switch')" @ok="submitSubstitute">
      <template v-if="subLine">
        <a-descriptions :column="1" size="small" class="block">
          <a-descriptions-item :label="$t('mes.wo.wo')">{{ subWo.woNo }} {{ subWo.productCode }} {{ subWo.productName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('mes.kit.mainMaterial')">{{ subLine.materialCode }} {{ subLine.materialName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('mes.kit.toIssueShort')">
            <span>{{ formatQty(subLine.toIssueQty) }} / <span class="danger">{{ formatQty(subLine.shortageQty) }}</span> {{ subLine.unit }}</span>
          </a-descriptions-item>
        </a-descriptions>
        <a-form layout="vertical">
          <a-form-item :label="$t('mes.kit.substitute')" required>
            <a-radio-group v-model:value="subForm.substituteId" class="sub-options" @change="suggestSubQty">
              <a-radio v-for="s in subLine.substitutes" :key="s.substituteId" :value="s.substituteId">
                {{ s.substituteCode }} {{ s.substituteName }}
                <span class="sub-text">{{ $t('mes.kit.ratioStock', { ratio: formatQty(s.ratio), stock: formatQty(s.onHandQty), unit: s.unit || '' }) }}</span>
              </a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item :label="$t('mes.kit.subQty')" required :extra="subPreview">
            <a-input-number
              v-model:value="subForm.qty"
              :min="0"
              :max="Number(subLine.toIssueQty)"
              :precision="subLine.serialEnabled === 1 ? 0 : 4"
              :addon-after="subLine.unit || undefined"
              class="full"
            />
          </a-form-item>
        </a-form>
      </template>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { ReloadOutlined, ShoppingCartOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { kitMaterialLines, kitMaterials, kitOrderLines, kitOrders, kitRelease, switchSubstitute } from '@/api/mes/base'
import { formatQty } from '@/components/wms/wms'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { findDict } from '@/utils/dict'

const userStore = useUserStore()
const dict = useDict('mes_kit_status', 'mes_wo_status', 'mes_supply_type')
const { t } = useI18n()
const dictLabel = (list, value) => findDict(list, value)?.label || value
const KIT_STATUSES = ['SHORT', 'PARTIAL', 'COMPLETE', 'ISSUED']
const QTY_KEYS = ['requiredQty', 'supplementQty', 'issuedQty', 'toIssueQty', 'onHandQty', 'allocatedQty', 'onOrderQty', 'unitQty']
const canSubstitute = computed(() => userStore.hasPerm('mes:kit:substitute'))
const canRelease = computed(() => userStore.hasPerm('mes:kit:release'))

const tab = ref('orders')

// ---------------------------------------------------------------- 工单齐套
const orderColumns = [
  { title: t('mes.wo.wo'), key: 'wo', width: 180 },
  { title: t('mes.bom.product'), key: 'product', width: 260 },
  { title: t('mes.wo.planStart'), dataIndex: 'planStart', width: 120 },
  { title: t('mes.kit.kitRate'), key: 'kitRate', width: 200 },
  { title: t('mes.kit.buildable'), key: 'buildableQty', width: 140, align: 'right' },
  { title: t('mes.kit.kitStatus'), key: 'kitStatus', width: 110 }
]
const lineColumns = [
  { title: t('mes.wo.material'), key: 'material', width: 220 },
  { title: t('mes.kit.required'), key: 'requiredQty', width: 80, align: 'right' },
  { title: t('mes.kit.issued'), key: 'issuedQty', width: 80, align: 'right' },
  { title: t('mes.kit.toIssue'), key: 'toIssueQty', width: 80, align: 'right' },
  { title: t('mes.mrp.onHand'), key: 'onHandQty', width: 80, align: 'right' },
  { title: t('mes.kit.allocated'), key: 'allocatedQty', width: 90, align: 'right' },
  { title: t('mes.kit.shortage'), key: 'shortageQty', width: 90, align: 'right' },
  { title: t('mes.mrp.scheduled'), key: 'onOrderQty', width: 90, align: 'right' },
  { title: t('common.field.unit'), dataIndex: 'unit', width: 60 },
  { title: '', key: 'action', width: 120 }
]
const orderQuery = reactive({ woNo: '', kitStatus: '' })
const orders = ref([])
const ordersLoading = ref(false)
const orderLines = ref({})
const expandedOrders = ref([])
const countOf = (s) => orders.value.filter((o) => o.kitStatus === s).length
const filteredOrders = computed(() => orders.value.filter((o) => !orderQuery.kitStatus || o.kitStatus === orderQuery.kitStatus))

async function loadOrders() {
  ordersLoading.value = true
  try {
    orders.value = await kitOrders({ woNo: orderQuery.woNo || undefined })
    orderLines.value = {}
    expandedOrders.value = []
  } finally {
    ordersLoading.value = false
  }
}

async function loadOrderLines(woId) {
  orderLines.value = { ...orderLines.value, [woId]: undefined }
  const lines = await kitOrderLines(woId)
  orderLines.value = { ...orderLines.value, [woId]: lines }
}

function onExpandOrder(expanded, record) {
  expandedOrders.value = expanded ? [...expandedOrders.value, record.woId] : expandedOrders.value.filter((k) => k !== record.woId)
  if (expanded && !orderLines.value[record.woId]) loadOrderLines(record.woId)
}

// ---------------------------------------------------------------- 生产用料汇总
const materialColumns = [
  { title: t('mes.wo.material'), key: 'material', width: 210 },
  { title: t('mes.kit.supplyType'), key: 'supplyType', width: 90 },
  { title: t('mes.kit.woCount'), dataIndex: 'woCount', width: 80, align: 'right' },
  { title: t('mes.kit.required'), key: 'requiredQty', width: 80, align: 'right' },
  { title: t('mes.kit.issued'), key: 'issuedQty', width: 80, align: 'right' },
  { title: t('mes.kit.toIssue'), key: 'toIssueQty', width: 80, align: 'right' },
  { title: t('mes.mrp.onHand'), key: 'onHandQty', width: 80, align: 'right' },
  { title: t('mes.kit.shortage'), key: 'shortageQty', width: 110, align: 'right' },
  { title: t('mes.mrp.scheduled'), key: 'onOrderQty', width: 90, align: 'right' },
  { title: t('mes.kit.suggest'), key: 'suggestQty', width: 150, align: 'right' }
]
const materialLineColumns = [
  { title: t('mes.wo.wo'), key: 'wo', width: 200 },
  { title: t('mes.bom.product'), key: 'product', width: 220 },
  { title: t('mes.wo.planStart'), dataIndex: 'planStart', width: 120 },
  { title: t('mes.kit.toIssue'), key: 'toIssueQty', width: 90, align: 'right' },
  { title: t('mes.kit.allocated'), key: 'allocatedQty', width: 90, align: 'right' },
  { title: t('mes.kit.shortage'), key: 'shortageQty', width: 90, align: 'right' }
]
const materialQuery = reactive({ keyword: '', onlyShort: true })
const materials = ref([])
const materialsLoading = ref(false)
const materialLines = ref({})
const expandedMaterials = ref([])
const selectedKeys = ref([])
const selectedMaterials = computed(() => materials.value.filter((m) => selectedKeys.value.includes(m.materialId)))
const materialSelection = computed(() => ({
  selectedRowKeys: selectedKeys.value,
  onChange: (keys) => (selectedKeys.value = keys),
  getCheckboxProps: (r) => ({ disabled: !(Number(r.suggestQty) > 0) })
}))

async function loadMaterials() {
  materialsLoading.value = true
  try {
    materials.value = await kitMaterials({ keyword: materialQuery.keyword || undefined, onlyShort: materialQuery.onlyShort })
    materialLines.value = {}
    expandedMaterials.value = []
    selectedKeys.value = []
  } finally {
    materialsLoading.value = false
  }
}

async function onExpandMaterial(expanded, record) {
  expandedMaterials.value = expanded
    ? [...expandedMaterials.value, record.materialId]
    : expandedMaterials.value.filter((k) => k !== record.materialId)
  if (expanded && !materialLines.value[record.materialId]) {
    const lines = await kitMaterialLines(record.materialId)
    materialLines.value = { ...materialLines.value, [record.materialId]: lines }
  }
}

watch(tab, (v) => {
  if (v === 'materials' && !materials.value.length) loadMaterials()
})

onMounted(loadOrders)

// ---------------------------------------------------------------- 缺料下单
const releaseOpen = ref(false)
const releasing = ref(false)
const releaseWarehouse = ref()
const releaseColumns = [
  { title: t('mes.wo.material'), key: 'material' },
  { title: t('mes.mrp.suggest'), key: 'suggestQty', width: 120, align: 'right' },
  { title: t('mes.kit.generate'), key: 'to', width: 160 }
]

async function submitRelease() {
  if (!releaseWarehouse.value) {
    message.warning(t('mes.kit.warehouseRequired'))
    return
  }
  releasing.value = true
  try {
    const docNos = await kitRelease({ materialIds: selectedKeys.value, warehouseId: releaseWarehouse.value })
    releaseOpen.value = false
    Modal.success({ title: t('mes.kit.generated'), content: docNos.join(t('mes.workCenter.sep')) })
    loadMaterials()
    loadOrders()
  } finally {
    releasing.value = false
  }
}

// ---------------------------------------------------------------- 替代料
const subOpen = ref(false)
const switching = ref(false)
const subWo = ref()
const subLine = ref()
const subForm = reactive({ substituteId: undefined, qty: undefined })
const currentSub = computed(() => subLine.value?.substitutes.find((s) => s.substituteId === subForm.substituteId))
const subPreview = computed(() =>
  currentSub.value && subForm.qty
    ? t('mes.kit.subNeed', { qty: formatQty(subForm.qty * Number(currentSub.value.ratio)), unit: currentSub.value.unit || '' })
    : t('mes.kit.subPreviewDefault')
)

function openSubstitute(wo, line) {
  subWo.value = wo
  subLine.value = line
  subForm.substituteId = line.substitutes[0].substituteId
  suggestSubQty()
  subOpen.value = true
}

/** 默认替代缺料部分，不超过替代料库存能覆盖的数量 */
function suggestSubQty() {
  const s = currentSub.value
  if (!s) return
  const coverable = Number(s.onHandQty) / Number(s.ratio)
  const qty = Math.min(Number(subLine.value.shortageQty), coverable)
  subForm.qty = subLine.value.serialEnabled === 1 ? Math.floor(qty) : Math.floor(qty * 10000) / 10000
}

async function submitSubstitute() {
  if (!subForm.substituteId || !(subForm.qty > 0)) {
    message.warning(t('mes.kit.subRequired'))
    return
  }
  switching.value = true
  try {
    await switchSubstitute({ woId: subWo.value.woId, materialId: subLine.value.materialId, substituteId: subForm.substituteId, qty: subForm.qty })
    message.success(t('mes.kit.switched'))
    subOpen.value = false
    await loadOrders()
  } finally {
    switching.value = false
  }
}
</script>

<style scoped>
.kit-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 12px;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.search {
  width: 220px;
}
.spacer {
  flex: 1;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.danger {
  color: var(--status-critical);
  font-weight: 500;
}
.strong {
  font-weight: 600;
}
.full {
  width: 100%;
}
.block {
  margin-bottom: 12px;
}
.sub-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.release-form {
  margin-top: 16px;
}
</style>
