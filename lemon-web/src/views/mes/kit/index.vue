<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" class="kit-tabs">
        <!-- ---------------------------------------------------------------- 按工单 -->
        <a-tab-pane key="orders" tab="工单齐套">
          <div class="toolbar">
            <a-input-search v-model:value="orderQuery.woNo" placeholder="工单号" allow-clear class="search" @search="loadOrders" />
            <a-radio-group v-model:value="orderQuery.kitStatus" button-style="solid" size="small">
              <a-radio-button value="">全部 {{ orders.length }}</a-radio-button>
              <a-radio-button v-for="s in KIT_STATUSES" :key="s" :value="s">{{ dictLabel(dict.mes_kit_status, s) }} {{ countOf(s) }}</a-radio-button>
            </a-radio-group>
            <span class="spacer" />
            <span class="sub-text">库存按优先级分配：已下达 / 生产中的在前，草稿在后，再按计划开工日期</span>
            <a-button @click="loadOrders"><template #icon><ReloadOutlined /></template></a-button>
          </div>
          <a-table
            row-key="woId"
            size="middle"
            :columns="orderColumns"
            :data-source="filteredOrders"
            :loading="ordersLoading"
            :pagination="{ pageSize: 20, showSizeChanger: false, showTotal: (t) => `共 ${t} 个工单` }"
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
                <div class="sub-text">完工 {{ formatQty(record.finishedQty) }} / {{ formatQty(record.qty) }} {{ record.unit }}</div>
              </template>
              <template v-else-if="column.key === 'kitRate'">
                <a-progress :percent="Number(record.kitRate)" size="small" :status="Number(record.kitRate) >= 100 ? 'success' : 'exception'" />
                <div class="sub-text">缺料 {{ record.shortLineCount }} / {{ record.lineCount }} 项</div>
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
                      替代料（{{ line.substitutes.length }}）
                    </a-button>
                    <span v-else-if="Number(line.shortageQty) > 0 && line.substitutes?.length" class="sub-text">有替代料</span>
                  </template>
                </template>
              </a-table>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- ---------------------------------------------------------------- 按物料 -->
        <a-tab-pane key="materials" tab="生产用料汇总">
          <div class="toolbar">
            <a-input-search v-model:value="materialQuery.keyword" placeholder="物料编码或名称" allow-clear class="search" @search="loadMaterials" />
            <a-checkbox v-model:checked="materialQuery.onlyShort" @change="loadMaterials">只看缺料</a-checkbox>
            <span class="spacer" />
            <a-button v-perm="'mes:kit:release'" type="primary" :disabled="!selectedMaterials.length" @click="releaseOpen = true">
              <template #icon><ShoppingCartOutlined /></template>缺料下单（{{ selectedMaterials.length }}）
            </a-button>
            <a-button @click="loadMaterials"><template #icon><ReloadOutlined /></template></a-button>
          </div>
          <a-table
            row-key="materialId"
            size="middle"
            :columns="materialColumns"
            :data-source="materials"
            :loading="materialsLoading"
            :pagination="{ pageSize: 20, showSizeChanger: false, showTotal: (t) => `共 ${t} 个物料` }"
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
                <div v-if="record.requiredDate && Number(record.shortageQty) > 0" class="sub-text">最早 {{ record.requiredDate }}</div>
              </template>
              <template v-else-if="column.key === 'suggestQty'">
                <span v-if="Number(record.suggestQty) > 0" class="strong">{{ formatQty(record.suggestQty) }} {{ record.unit }}</span>
                <span v-else class="sub-text">-</span>
                <div v-if="Number(record.suggestQty) > 0" class="sub-text">
                  {{ record.supplyType === 'MAKE' ? '生成工单' : record.supplierName || '未设置默认供应商' }}
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
    <a-modal v-model:open="releaseOpen" title="缺料下单" :confirm-loading="releasing" ok-text="生成单据" @ok="submitRelease">
      <p class="sub-text">
        按建议数量（缺料 - 在途，按批量规则取整）生成草稿单据：采购 / 委外件按默认供应商合并为采购订单，自制件生成工单。生成后到对应页面确认、审核。
      </p>
      <a-table row-key="materialId" size="small" :columns="releaseColumns" :data-source="selectedMaterials" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
          <template v-else-if="column.key === 'suggestQty'">{{ formatQty(record.suggestQty) }} {{ record.unit }}</template>
          <template v-else-if="column.key === 'to'">
            {{ record.supplyType === 'MAKE' ? '工单' : record.supplierName || '未设置默认供应商' }}
          </template>
        </template>
      </a-table>
      <a-form layout="vertical" class="release-form">
        <a-form-item label="收货 / 入库仓库" required>
          <WarehouseSelect v-model:value="releaseWarehouse" :allow-clear="false" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 替代料 -->
    <a-modal v-model:open="subOpen" title="替代料切换" :confirm-loading="switching" ok-text="切换" @ok="submitSubstitute">
      <template v-if="subLine">
        <a-descriptions :column="1" size="small" class="block">
          <a-descriptions-item label="工单">{{ subWo.woNo }} {{ subWo.productCode }} {{ subWo.productName }}</a-descriptions-item>
          <a-descriptions-item label="主料">{{ subLine.materialCode }} {{ subLine.materialName }}</a-descriptions-item>
          <a-descriptions-item label="待领 / 缺料">
            <span>{{ formatQty(subLine.toIssueQty) }} / <span class="danger">{{ formatQty(subLine.shortageQty) }}</span> {{ subLine.unit }}</span>
          </a-descriptions-item>
        </a-descriptions>
        <a-form layout="vertical">
          <a-form-item label="替代料" required>
            <a-radio-group v-model:value="subForm.substituteId" class="sub-options" @change="suggestSubQty">
              <a-radio v-for="s in subLine.substitutes" :key="s.substituteId" :value="s.substituteId">
                {{ s.substituteCode }} {{ s.substituteName }}
                <span class="sub-text">1 : {{ formatQty(s.ratio) }}，库存 {{ formatQty(s.onHandQty) }} {{ s.unit }}</span>
              </a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="替代的主料数量" required :extra="subPreview">
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
import DictTag from '@/components/DictTag.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { kitMaterialLines, kitMaterials, kitOrderLines, kitOrders, kitRelease, switchSubstitute } from '@/api/mes/base'
import { formatQty } from '@/components/wms/wms'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { findDict } from '@/utils/dict'

const userStore = useUserStore()
const dict = useDict('mes_kit_status', 'mes_wo_status', 'mes_supply_type')
const dictLabel = (list, value) => findDict(list, value)?.label || value
const KIT_STATUSES = ['SHORT', 'PARTIAL', 'COMPLETE', 'ISSUED']
const QTY_KEYS = ['requiredQty', 'supplementQty', 'issuedQty', 'toIssueQty', 'onHandQty', 'allocatedQty', 'onOrderQty', 'unitQty']
const canSubstitute = computed(() => userStore.hasPerm('mes:kit:substitute'))
const canRelease = computed(() => userStore.hasPerm('mes:kit:release'))

const tab = ref('orders')

// ---------------------------------------------------------------- 工单齐套
const orderColumns = [
  { title: '工单', key: 'wo', width: 180 },
  { title: '产品', key: 'product', width: 260 },
  { title: '计划开工', dataIndex: 'planStart', width: 110 },
  { title: '齐套率', key: 'kitRate', width: 200 },
  { title: '可生产数量', key: 'buildableQty', width: 130, align: 'right' },
  { title: '齐套状态', key: 'kitStatus', width: 110 }
]
const lineColumns = [
  { title: '物料', key: 'material', width: 220 },
  { title: '需求', key: 'requiredQty', width: 80, align: 'right' },
  { title: '已领', key: 'issuedQty', width: 80, align: 'right' },
  { title: '待领', key: 'toIssueQty', width: 80, align: 'right' },
  { title: '库存', key: 'onHandQty', width: 80, align: 'right' },
  { title: '分配', key: 'allocatedQty', width: 80, align: 'right' },
  { title: '缺料', key: 'shortageQty', width: 80, align: 'right' },
  { title: '在途', key: 'onOrderQty', width: 80, align: 'right' },
  { title: '单位', dataIndex: 'unit', width: 60 },
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
  { title: '物料', key: 'material', width: 210 },
  { title: '属性', key: 'supplyType', width: 72 },
  { title: '工单数', dataIndex: 'woCount', width: 66, align: 'right' },
  { title: '需求', key: 'requiredQty', width: 80, align: 'right' },
  { title: '已领', key: 'issuedQty', width: 80, align: 'right' },
  { title: '待领', key: 'toIssueQty', width: 80, align: 'right' },
  { title: '库存', key: 'onHandQty', width: 80, align: 'right' },
  { title: '缺料', key: 'shortageQty', width: 110, align: 'right' },
  { title: '在途', key: 'onOrderQty', width: 80, align: 'right' },
  { title: '建议下单', key: 'suggestQty', width: 150, align: 'right' }
]
const materialLineColumns = [
  { title: '工单', key: 'wo', width: 200 },
  { title: '产品', key: 'product', width: 220 },
  { title: '计划开工', dataIndex: 'planStart', width: 110 },
  { title: '待领', key: 'toIssueQty', width: 90, align: 'right' },
  { title: '分配', key: 'allocatedQty', width: 90, align: 'right' },
  { title: '缺料', key: 'shortageQty', width: 90, align: 'right' }
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

watch(tab, (t) => {
  if (t === 'materials' && !materials.value.length) loadMaterials()
})

onMounted(loadOrders)

// ---------------------------------------------------------------- 缺料下单
const releaseOpen = ref(false)
const releasing = ref(false)
const releaseWarehouse = ref()
const releaseColumns = [
  { title: '物料', key: 'material' },
  { title: '建议数量', key: 'suggestQty', width: 120, align: 'right' },
  { title: '生成', key: 'to', width: 160 }
]

async function submitRelease() {
  if (!releaseWarehouse.value) {
    message.warning('请选择收货 / 入库仓库')
    return
  }
  releasing.value = true
  try {
    const docNos = await kitRelease({ materialIds: selectedKeys.value, warehouseId: releaseWarehouse.value })
    releaseOpen.value = false
    Modal.success({ title: '已生成草稿单据', content: docNos.join('、') })
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
    ? `需要替代料 ${formatQty(subForm.qty * Number(currentSub.value.ratio))} ${currentSub.value.unit || ''}`
    : '主料需求相应减少，替代料按比例加入工单用料'
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
    message.warning('请选择替代料并填写数量')
    return
  }
  switching.value = true
  try {
    await switchSubstitute({ woId: subWo.value.woId, materialId: subLine.value.materialId, substituteId: subForm.substituteId, qty: subForm.qty })
    message.success('已切换替代料')
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
