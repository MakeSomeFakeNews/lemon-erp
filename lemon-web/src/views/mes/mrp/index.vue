<template>
  <div class="mrp-page">
    <a-card :bordered="false" class="page-card">
      <div class="head">
        <span class="section-title">{{ $t('mes.mrp.runs') }}</span>
        <span class="sub-text">{{ $t('mes.mrp.runsTip') }}</span>
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
            <span class="ellipsis" :title="record.planNos">{{ record.planNos || '-' }}</span>
            <div v-if="record.sources" class="sub-text">{{ record.sources }}</div>
          </template>
          <template v-else-if="column.key === 'pending'">
            <a-badge v-if="record.pendingCount" :count="record.pendingCount" :number-style="{ backgroundColor: '#faad14' }" />
            <span v-else class="sub-text">{{ $t('common.status.none') }}</span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-card :bordered="false" class="page-card">
      <div class="head">
        <span class="section-title">{{ $t('mes.mrp.results', { no: currentRun ? currentRun.runNo : '' }) }}</span>
        <a-space v-if="runId" wrap>
          <WarehouseSelect v-model:value="warehouseId" :placeholder="$t('mes.mrp.warehousePh')" class="warehouse" />
          <a-button v-perm="'mes:mrp:release'" type="primary" :disabled="!selected.length" :loading="releasing" @click="release">
            <template #icon><SendOutlined /></template>{{ $t('mes.mrp.releaseSelected', { n: selected.length }) }}
          </a-button>
        </a-space>
      </div>
      <a-alert
        v-if="runId"
        type="info"
        show-icon
        class="tip"
        :message="$t('mes.mrp.tip')"
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
        :locale="{ emptyText: runId ? $t('mes.mrp.noDemand') : $t('mes.mrp.selectRun') }"
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
            <div>{{ $t('mes.mrp.required', { d: record.requiredDate }) }}</div>
            <div class="sub-text">{{ record.supplyType === 'MAKE' ? $t('mes.mrp.start') : record.supplyType === 'PHANTOM' ? $t('mes.mrp.phantom') : $t('mes.mrp.order') }} {{ record.releaseDate }}</div>
          </template>
          <template v-else-if="column.key === 'supplier'">
            <template v-if="['BUY', 'OUTSOURCE'].includes(record.supplyType)">
              <PartnerSelect
                v-if="record.status === 'NEW' && canRelease"
                :value="record.supplierId"
                :label="record.supplierName"
                type="supplier"
                :placeholder="$t('mes.mrp.selectSupplier')"
                size="small"
                @update:value="(v) => save(record, { supplierId: v, price: null })"
              />
              <span v-else>{{ record.supplierName || '-' }}</span>
              <div v-if="record.price != null" class="sub-text">¥{{ formatMoney(record.price) }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag v-if="record.status === 'NEW'" color="orange" :bordered="false">{{ $t('mes.mrp.status.NEW') }}</a-tag>
            <template v-else-if="record.status === 'RELEASED'">
              <a-tag color="green" :bordered="false">{{ $t('mes.mrp.status.RELEASED') }}</a-tag>
              <div class="sub-text">{{ record.releaseNo }}</div>
            </template>
            <a-tooltip v-else-if="record.status === 'EXPIRED'" :title="$t('mes.mrp.expiredTip')">
              <a-tag :bordered="false">{{ $t('mes.mrp.status.EXPIRED') }}</a-tag>
            </a-tooltip>
            <a-tag v-else :bordered="false">{{ $t('mes.mrp.status.NONE') }}</a-tag>
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
import { useI18n } from 'vue-i18n'
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
const { t } = useI18n()
const canRelease = computed(() => userStore.hasPerm('mes:mrp:release'))

const runColumns = [
  { title: t('mes.mrp.runNo'), key: 'runNo', width: 170 },
  { title: t('mes.mrp.planNos'), key: 'planNos', ellipsis: true },
  { title: t('mes.mrp.resultCount'), dataIndex: 'resultCount', width: 100, align: 'right' },
  { title: t('mes.mrp.status.NEW'), key: 'pending', width: 100 },
  { title: t('mes.mrp.runBy'), dataIndex: 'createByName', width: 100 },
  { title: t('mes.mrp.runTime'), dataIndex: 'createTime', width: 170 }
]
const runs = useTable(pageMrpRun, reactive({}))
runs.pagination.pageSize = 5

const resultColumns = [
  { title: t('mes.mrp.material'), key: 'material', width: 280, fixed: 'left' },
  { title: t('mes.mrp.supplyType'), key: 'supplyType', width: 90 },
  { title: t('mes.mrp.gross'), key: 'grossQty', width: 100, align: 'right' },
  { title: t('mes.mrp.onHand'), key: 'onHandQty', width: 90, align: 'right' },
  { title: t('mes.mrp.scheduled'), key: 'scheduledQty', width: 90, align: 'right' },
  { title: t('mes.mrp.allocated'), key: 'allocatedQty', width: 90, align: 'right' },
  { title: t('mes.mrp.safety'), key: 'safetyQty', width: 100, align: 'right' },
  { title: t('mes.mrp.net'), key: 'netQty', width: 90, align: 'right' },
  { title: t('mes.mrp.suggest'), key: 'suggestQty', width: 150 },
  { title: t('common.field.date'), key: 'dates', width: 150 },
  { title: t('mes.mrp.supplier'), key: 'supplier', width: 220 },
  { title: t('common.field.status'), key: 'status', width: 150 }
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
    title: t('mes.mrp.releaseConfirm', { n: selected.value.length }),
    content: t('mes.mrp.releaseContent'),
    async onOk() {
      releasing.value = true
      try {
        const nos = await releaseMrp({ resultIds: selected.value, warehouseId: warehouseId.value })
        message.success(t('mes.mrp.generated', { nos: nos.join(t('mes.workCenter.sep')) }))
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
