<template>
  <SubPage v-model:open="visible" :title="title">
    <a-form layout="inline" class="history-filter">
      <a-form-item :label="$t('wms.common.warehouse')">
        <WarehouseSelect v-model:value="query.warehouseId" :placeholder="$t('wms.history.allWarehouses')" style="width: 220px" />
      </a-form-item>
      <a-form-item :label="$t('wms.history.dateRange')">
        <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" :allow-clear="false" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" :loading="loading" @click="load">
          <template #icon><SearchOutlined /></template>{{ $t('common.action.search') }}
        </a-button>
      </a-form-item>
    </a-form>
    <div class="history-tip">{{ query.warehouseId ? $t('wms.history.warehouseTip') : $t('wms.history.allTip') }}</div>

    <a-table
      row-key="key"
      size="small"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="false"
      :scroll="{ x: scrollX }"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'seq'">{{ index + 1 }}</template>
        <template v-else-if="column.key === 'docNo'">
          <a v-if="record.docId" @click="openDoc(record)">{{ record.docNo }}</a>
          <span v-else>-</span>
          <div v-if="record.sourceNo" class="sub-text">{{ record.sourceNo }}</div>
        </template>
        <template v-else-if="column.key === 'bizType'">
          <template v-if="record.opening">
            <span>{{ $t('wms.history.opening') }}</span>
            <a-tooltip v-if="costVisible && record.estimated" :title="$t('wms.history.openingEstimated')">
              <a-tag color="orange" :bordered="false" class="estimated-tag">{{ $t('wms.history.estimated') }}</a-tag>
            </a-tooltip>
          </template>
          <DictTag v-else-if="record.docBizType" :type="record.bizType === StockBizType.IN ? 'wms_in_type' : 'wms_out_type'" :value="record.docBizType" />
          <DictTag v-else type="wms_biz_type" :value="record.bizType" />
        </template>
        <template v-else-if="column.key === 'bizPrice'">{{ record.bizPrice == null ? '' : price(record.bizPrice) }}</template>
        <template v-else-if="column.key === 'costPrice'">
          <a-tooltip v-if="record.costEstimated === 1" :title="$t('wms.history.estimatedTip')">
            <a-tag color="orange" :bordered="false" class="estimated-tag">{{ $t('wms.history.estimated') }}</a-tag>
          </a-tooltip>
          {{ record.costPrice == null ? '' : price(record.costPrice) }}
        </template>
        <template v-else-if="column.key === 'inQty'">{{ record.qty > 0 ? formatQty(record.qty) : '' }}</template>
        <template v-else-if="column.key === 'outQty'">{{ record.qty < 0 ? formatQty(-record.qty) : '' }}</template>
        <template v-else-if="column.key === 'balanceQty'"><b>{{ formatQty(record.balanceQty) }}</b></template>
        <template v-else-if="column.key === 'costAmount'">
          <span :class="{ negative: record.costAmount < 0 }">{{ record.costAmount == null ? '' : money(record.costAmount) }}</span>
        </template>
        <template v-else-if="column.key === 'balanceAmount'">{{ record.balanceAmount == null ? '' : money(record.balanceAmount) }}</template>
      </template>
    </a-table>

    <StockDocDetail v-if="docKind" v-model:open="docOpen" :kind="docKind" :doc-id="docId" />
    <CheckDetail v-model:open="checkOpen" :check-id="checkId" />
  </SubPage>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { SearchOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import CheckDetail from './CheckDetail.vue'
import StockDocDetail from './StockDocDetail.vue'
import WarehouseSelect from './WarehouseSelect.vue'
import { stockHistory } from '@/api/wms/stock'
import { formatQty } from './wms'
import { money, price } from '@/utils/number'
import { StockBizType } from '@/constants/enums'

/**
 * 经营历史：物料的期初余额与每条库存流水（业务单价、成本价、入库 / 出库数量、结余数量与金额、往来单位），
 * 点击单号查看单据。通过 ref 调用 open({ materialId, materialCode, materialName, warehouseId? })
 */
const { t } = useI18n()

const visible = ref(false)
const loading = ref(false)
const material = ref({})
const query = reactive({ warehouseId: undefined, range: [] })
const data = ref(null)

const title = computed(() => t('wms.history.title', { name: `${material.value.materialCode || ''} ${material.value.materialName || ''}`.trim() }))
const costVisible = computed(() => !!data.value?.costVisible)

const columns = computed(() =>
  [
    { title: '#', key: 'seq', width: 50 },
    { title: t('common.field.date'), dataIndex: 'bizDate', width: 110 },
    { title: t('wms.common.docNo'), key: 'docNo', width: 170 },
    { title: t('wms.history.bizType'), key: 'bizType', width: 130 },
    { title: t('wms.history.partner'), dataIndex: 'partnerName', width: 160, ellipsis: true },
    query.warehouseId ? null : { title: t('wms.common.warehouse'), dataIndex: 'warehouseName', width: 120, ellipsis: true },
    costVisible.value ? { title: t('wms.history.bizPrice'), key: 'bizPrice', width: 100, align: 'right' } : null,
    costVisible.value ? { title: t('wms.history.costPrice'), key: 'costPrice', width: 130, align: 'right' } : null,
    { title: t('wms.history.inQty'), key: 'inQty', width: 100, align: 'right' },
    { title: t('wms.history.outQty'), key: 'outQty', width: 100, align: 'right' },
    { title: t('wms.history.balanceQty'), key: 'balanceQty', width: 110, align: 'right' },
    costVisible.value ? { title: t('wms.history.costAmount'), key: 'costAmount', width: 120, align: 'right' } : null,
    costVisible.value ? { title: t('wms.history.balanceAmount'), key: 'balanceAmount', width: 130, align: 'right' } : null
  ].filter(Boolean)
)
const scrollX = computed(() => columns.value.reduce((sum, c) => sum + c.width, 0))

/** 第一行为期初余额 */
const rows = computed(() => {
  const d = data.value
  if (!d) return []
  const opening = {
    key: 'opening',
    opening: true,
    bizDate: d.beginDate,
    qty: 0,
    balanceQty: d.openingQty,
    balanceAmount: d.openingAmount,
    estimated: d.openingEstimated
  }
  return [opening, ...d.rows.map((r) => ({ ...r, key: r.id, qty: Number(r.qty), costAmount: r.costAmount == null ? null : Number(r.costAmount) }))]
})

async function load() {
  loading.value = true
  try {
    data.value = await stockHistory({
      materialId: material.value.materialId,
      warehouseId: query.warehouseId || undefined,
      beginDate: query.range?.[0],
      endDate: query.range?.[1]
    })
  } finally {
    loading.value = false
  }
}

function open(m) {
  material.value = m
  query.warehouseId = m.warehouseId || undefined
  query.range = [dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')]
  data.value = null
  visible.value = true
  load()
}

// 单据：出入库单 / 调拨单 / 盘点单
const docOpen = ref(false)
const docKind = ref('')
const docId = ref()
const checkOpen = ref(false)
const checkId = ref()
function openDoc(record) {
  if (record.bizType === StockBizType.CHECK) {
    checkId.value = record.docId
    checkOpen.value = true
    return
  }
  docKind.value = record.bizType.toLowerCase()
  docId.value = record.docId
  docOpen.value = true
}

defineExpose({ open })
</script>

<style scoped>
.history-filter {
  row-gap: 8px;
  margin-bottom: 8px;
}
.history-tip {
  color: var(--app-text-3);
  font-size: 12px;
  margin-bottom: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.estimated-tag {
  margin: 0 4px;
}
.negative {
  color: #cf1322;
}
</style>
