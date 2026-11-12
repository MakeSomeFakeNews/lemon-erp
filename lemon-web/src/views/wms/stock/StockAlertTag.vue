<template>
  <template v-if="record.alertMode === StockAlertMode.WAREHOUSE && (record.lowWarehouses || record.highWarehouses)">
    <!-- 分仓库预警：点击查看各仓库的库存与上下限 -->
    <a-popover trigger="click" :title="$t('wms.stockLimit.warehouseDetail')" @open-change="(v) => v && load()">
      <template #content>
        <a-spin :spinning="loading">
          <div class="rows">
            <div v-for="r in rows" :key="r.warehouseId" class="row">
              <span class="name">{{ r.warehouseName }}</span>
              <span :class="{ low: r.alert === StockAlert.LOW, high: r.alert === StockAlert.HIGH }">{{ formatQty(r.qty) }}</span>
              <span class="sub-text">{{ $t('wms.stockLimit.limitsText', { min: text(r.minQty), max: text(r.maxQty) }) }}</span>
            </div>
          </div>
        </a-spin>
      </template>
      <a-tag v-if="record.lowWarehouses" color="red" :bordered="false" class="tag">{{ $t('wms.stockLimit.lowWarehouses', { n: record.lowWarehouses }) }}</a-tag>
      <a-tag v-if="record.highWarehouses" color="orange" :bordered="false" class="tag">{{ $t('wms.stockLimit.highWarehouses', { n: record.highWarehouses }) }}</a-tag>
    </a-popover>
  </template>
  <a-tooltip v-else-if="record.stockAlert" :title="$t('wms.stockLimit.totalTip')">
    <a-tag v-if="record.stockAlert === StockAlert.LOW" color="red" :bordered="false" class="tag">{{ $t('wms.stockLimit.low') }}</a-tag>
    <a-tag v-else color="orange" :bordered="false" class="tag">{{ $t('wms.stockLimit.high') }}</a-tag>
  </a-tooltip>
</template>

<script setup>
import { ref } from 'vue'
import { warehouseAlerts } from '@/api/wms/stock'
import { formatQty } from '@/components/wms/wms'
import { StockAlert, StockAlertMode } from '@/constants/enums'

/**
 * 库存查询（按物料汇总）的预警标签（issue #111）：按总库存显示低于下限 / 高于上限（按所有仓库的合计）；
 * 分仓库显示低于 / 高于的仓库数，点击查看各仓库的库存与上下限
 */
const props = defineProps({ record: { type: Object, required: true } })
const rows = ref([])
const loading = ref(false)
const text = (v) => (v == null ? '-' : formatQty(v))

async function load() {
  loading.value = true
  try {
    rows.value = (await warehouseAlerts(props.record.materialId)) || []
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.tag {
  margin-left: 6px;
  margin-right: 0;
  cursor: pointer;
}
.rows {
  min-width: 240px;
}
.row {
  display: flex;
  gap: 12px;
  align-items: baseline;
  padding: 2px 0;
}
.name {
  flex: 1;
}
.low {
  color: #ff4d4f;
  font-weight: 500;
}
.high {
  color: #fa8c16;
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
