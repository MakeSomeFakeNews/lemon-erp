<template>
  <div class="mrp-page">
    <a-card :bordered="false" class="page-card">
      <div class="head">
        <span class="section-title">运算记录</span>
        <span class="sub-text">在「生产计划」中发起 MRP 运算：勾选已确认的计划，或计入销售订单的未计划需求、销售预测</span>
      </div>
      <a-table
        row-key="id"
        size="small"
        :columns="runColumns"
        :data-source="runs.list.value"
        :loading="runs.loading.value"
        :pagination="runs.pagination"
        :row-class-name="(r) => (r.id === runId ? 'row-active' : '')"
        :custom-row="(r) => ({ onClick: () => selectRun(r.id) })"
        @change="runs.onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'runNo'">
            <a>{{ record.runNo }}</a>
          </template>
          <template v-else-if="column.key === 'planNos'">
            <span class="ellipsis">{{ record.planNos || '-' }}</span>
            <div v-if="record.sources" class="sub-text">{{ record.sources }}</div>
          </template>
          <template v-else-if="column.key === 'pending'">
            <a-badge v-if="record.pendingCount" :count="record.pendingCount" :number-style="{ backgroundColor: '#faad14' }" />
            <span v-else class="sub-text">无</span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-card :bordered="false" class="page-card">
      <div class="head">
        <span class="section-title">运算结果 {{ currentRun ? currentRun.runNo : '' }}</span>
        <a-space v-if="runId" wrap>
          <WarehouseSelect v-model:value="warehouseId" placeholder="工单完工 / 采购收货仓库（选填）" class="warehouse" />
          <a-button v-perm="'mes:mrp:release'" type="primary" :disabled="!selected.length" :loading="releasing" @click="release">
            <template #icon><SendOutlined /></template>下达所选（{{ selected.length }}）
          </a-button>
        </a-space>
      </div>
      <a-alert
        v-if="runId"
        type="info"
        show-icon
        class="tip"
        message="净需求 = 毛需求 - (库存 + 在途 - 已分配 - 安全库存)。自制件下达为草稿工单，采购件按供应商合并为草稿采购订单；下达前可调整数量和供应商"
      />
      <a-table
        row-key="id"
        size="small"
        :columns="resultColumns"
        :data-source="results"
        :loading="resultLoading"
        :pagination="false"
        :row-selection="runId ? { selectedRowKeys: selected, onChange: (k) => (selected = k), getCheckboxProps: (r) => ({ disabled: r.status !== 'NEW' }) } : undefined"
        :scroll="{ x: 1500 }"
        :locale="{ emptyText: runId ? '没有物料需求' : '请选择一条运算记录' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            <span :style="{ paddingLeft: `${record.levelNo * 12}px` }">
              <span class="level">L{{ record.levelNo }}</span>
              {{ record.materialCode }} {{ record.materialName }}
            </span>
          </template>
          <template v-else-if="column.key === 'supplyType'">
            <DictTag type="mes_supply_type" :value="record.supplyType" />
          </template>
          <template v-else-if="['grossQty', 'onHandQty', 'scheduledQty', 'allocatedQty', 'safetyQty'].includes(column.key)">
            {{ formatQty(record[column.key]) }}
          </template>
          <template v-else-if="column.key === 'netQty'">
            <b :class="{ warn: Number(record.netQty) > 0 }">{{ formatQty(record.netQty) }}</b>
          </template>
          <template v-else-if="column.key === 'suggestQty'">
            <a-input-number
              v-if="record.status === 'NEW' && canRelease"
              :value="Number(record.suggestQty)"
              :min="0"
              size="small"
              class="qty-input"
              @change="(v) => save(record, { suggestQty: v })"
            />
            <span v-else>{{ formatQty(record.suggestQty) }}</span>
            <span class="sub-text"> {{ record.unit }}</span>
          </template>
          <template v-else-if="column.key === 'dates'">
            <div>需 {{ record.requiredDate }}</div>
            <div class="sub-text">{{ record.supplyType === 'MAKE' ? '开工' : record.supplyType === 'PHANTOM' ? '虚拟件' : '下单' }} {{ record.releaseDate }}</div>
          </template>
          <template v-else-if="column.key === 'supplier'">
            <template v-if="['BUY', 'OUTSOURCE'].includes(record.supplyType)">
              <PartnerSelect
                v-if="record.status === 'NEW' && canRelease"
                :value="record.supplierId"
                :label="record.supplierName"
                type="supplier"
                placeholder="选择供应商"
                size="small"
                @update:value="(v) => save(record, { supplierId: v, price: null })"
              />
              <span v-else>{{ record.supplierName || '-' }}</span>
              <div v-if="record.price != null" class="sub-text">¥{{ formatMoney(record.price) }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag v-if="record.status === 'NEW'" color="orange" :bordered="false">待下达</a-tag>
            <template v-else-if="record.status === 'RELEASED'">
              <a-tag color="green" :bordered="false">已下达</a-tag>
              <div class="sub-text">{{ record.releaseNo }}</div>
            </template>
            <a-tooltip v-else-if="record.status === 'EXPIRED'" title="之后又运行了 MRP，本次未下达的建议已作废">
              <a-tag :bordered="false">已作废</a-tag>
            </a-tooltip>
            <a-tag v-else :bordered="false">无需处理</a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
    <ForecastPanel />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { SendOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import ForecastPanel from '@/components/mes/ForecastPanel.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { mrpResults, pageMrpRun, releaseMrp, updateMrpResult } from '@/api/mes/plan'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { formatMoney } from '@/components/scm/scm'
import { formatQty } from '@/components/wms/wms'

const route = useRoute()
const userStore = useUserStore()
const canRelease = computed(() => userStore.hasPerm('mes:mrp:release'))

const runColumns = [
  { title: '运算号', key: 'runNo', width: 170 },
  { title: '参与计划', key: 'planNos', ellipsis: true },
  { title: '结果行数', dataIndex: 'resultCount', width: 90, align: 'right' },
  { title: '待下达', key: 'pending', width: 80 },
  { title: '运算人', dataIndex: 'createByName', width: 100 },
  { title: '运算时间', dataIndex: 'createTime', width: 170 }
]
const runs = useTable(pageMrpRun, reactive({}))
runs.pagination.pageSize = 5

const resultColumns = [
  { title: '物料（低层码）', key: 'material', width: 280, fixed: 'left' },
  { title: '方式', key: 'supplyType', width: 70 },
  { title: '毛需求', key: 'grossQty', width: 90, align: 'right' },
  { title: '库存', key: 'onHandQty', width: 80, align: 'right' },
  { title: '在途', key: 'scheduledQty', width: 80, align: 'right' },
  { title: '已分配', key: 'allocatedQty', width: 80, align: 'right' },
  { title: '安全库存', key: 'safetyQty', width: 90, align: 'right' },
  { title: '净需求', key: 'netQty', width: 90, align: 'right' },
  { title: '建议数量', key: 'suggestQty', width: 150 },
  { title: '日期', key: 'dates', width: 140 },
  { title: '供应商', key: 'supplier', width: 220 },
  { title: '状态', key: 'status', width: 150 }
]

const runId = ref()
const results = ref([])
const resultLoading = ref(false)
const selected = ref([])
const warehouseId = ref()
const currentRun = computed(() => runs.list.value.find((r) => r.id === runId.value))

async function loadResults() {
  resultLoading.value = true
  try {
    results.value = await mrpResults(runId.value)
    selected.value = []
  } finally {
    resultLoading.value = false
  }
}

function selectRun(id) {
  runId.value = id
  loadResults()
}

onMounted(async () => {
  await runs.load()
  const id = Number(route.query.runId) || runs.list.value[0]?.id
  if (id) selectRun(id)
})

/** 调整建议数量、供应商（换供应商后单价取该供应商的供货价格） */
async function save(record, patch) {
  const data = {
    suggestQty: patch.suggestQty ?? Number(record.suggestQty),
    supplierId: 'supplierId' in patch ? patch.supplierId : record.supplierId,
    price: 'price' in patch ? patch.price : record.price
  }
  if (data.suggestQty == null) return
  await updateMrpResult(record.id, data)
  if ('supplierId' in patch) {
    await loadResults()
  } else {
    record.suggestQty = data.suggestQty
  }
}

const releasing = ref(false)
function release() {
  Modal.confirm({
    title: `下达 ${selected.value.length} 条建议？`,
    content: '自制件生成草稿工单，采购件按供应商合并生成草稿采购订单。',
    async onOk() {
      releasing.value = true
      try {
        const nos = await releaseMrp({ resultIds: selected.value, warehouseId: warehouseId.value })
        message.success(`已生成：${nos.join('、')}`)
        await Promise.all([loadResults(), runs.load()])
      } finally {
        releasing.value = false
      }
    }
  })
}
</script>

<style scoped>
.mrp-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.section-title {
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.tip {
  margin-bottom: 12px;
}
.warehouse {
  width: 260px;
}
.qty-input {
  width: 100px;
}
.level {
  display: inline-block;
  font-size: 11px;
  color: var(--app-text-3);
  margin-right: 4px;
}
.warn {
  color: #fa8c16;
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
:deep(.row-active) > td {
  background: var(--ant-color-primary-bg, #e6f4ff) !important;
}
:deep(.ant-table-row) {
  cursor: pointer;
}
</style>
