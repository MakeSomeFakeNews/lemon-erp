<template>
  <a-col :span="24">
    <div class="sub-title">{{ $t('wms.stockLimit.title') }}</div>
  </a-col>
  <a-col :span="24">
    <a-form-item :label="$t('wms.stockLimit.alertMode')" name="alertMode" :extra="modeTip">
      <a-radio-group v-model:value="form.alertMode" button-style="solid" :disabled="disabled">
        <a-radio-button v-for="o in toOptions(dict.wms_stock_alert_mode)" :key="o.value" :value="o.value">{{ o.label }}</a-radio-button>
      </a-radio-group>
    </a-form-item>
  </a-col>
  <template v-if="form.alertMode !== StockAlertMode.WAREHOUSE">
    <a-col :xs="24" :sm="colSpan">
      <a-form-item :label="$t('wms.stock.safetyStock')" name="safetyStock" :extra="$t('wms.material.safetyTip')">
        <a-input-number v-model:value="form.safetyStock" :min="0" :precision="4" class="full" :placeholder="$t('wms.material.noAlert')" :disabled="disabled" />
      </a-form-item>
    </a-col>
    <a-col :xs="24" :sm="colSpan">
      <a-form-item :label="$t('wms.stockLimit.maxStock')" name="maxStock" :extra="$t('wms.stockLimit.maxTip')" :rules="maxRules">
        <a-input-number v-model:value="form.maxStock" :min="0" :precision="4" class="full" :placeholder="$t('wms.material.noAlert')" :disabled="disabled" />
      </a-form-item>
    </a-col>
  </template>
  <a-col v-else :span="24">
    <a-table
      row-key="key"
      size="small"
      :columns="columns"
      :data-source="form.warehouseLimits"
      :pagination="false"
      :scroll="{ x: 560 }"
      :locale="{ emptyText: $t('wms.stockLimit.noWarehouse') }"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'warehouse'">
          <a-form-item :name="['warehouseLimits', index, 'warehouseId']" :rules="warehouseRules(index)" class="cell-item">
            <WarehouseSelect v-model:value="record.warehouseId" :allow-clear="false" :disabled="disabled" />
          </a-form-item>
        </template>
        <template v-else-if="column.key === 'minQty'">
          <a-input-number v-model:value="record.minQty" :min="0" :precision="4" class="full" :placeholder="$t('wms.material.noAlert')" :disabled="disabled" />
        </template>
        <template v-else-if="column.key === 'maxQty'">
          <a-form-item :name="['warehouseLimits', index, 'maxQty']" :rules="rowMaxRules(record)" class="cell-item">
            <a-input-number v-model:value="record.maxQty" :min="0" :precision="4" class="full" :placeholder="$t('wms.material.noAlert')" :disabled="disabled" />
          </a-form-item>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" danger size="small" :disabled="disabled" @click="form.warehouseLimits.splice(index, 1)"><DeleteOutlined /></a-button>
        </template>
      </template>
    </a-table>
    <a-button type="dashed" block class="add-row" :disabled="disabled" @click="addRow">
      <template #icon><PlusOutlined /></template>{{ $t('wms.stockLimit.addWarehouse') }}
    </a-button>
    <div class="sum-tip">{{ $t('wms.stockLimit.sumTip', { min: sumText('minQty'), max: sumText('maxQty') }) }}</div>
  </a-col>
</template>

<script setup>
import { computed } from 'vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { formatQty, rowKey } from '@/components/wms/wms'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { StockAlertMode } from '@/constants/enums'
import { t } from '@/locales'

/**
 * 物料表单中的库存预警（issue #111）：按总库存时填写安全库存、库存上限；分仓库时逐个仓库填写下限、上限。
 * form 为物料表单（alertMode、safetyStock、maxStock、warehouseLimits: [{ key, warehouseId, minQty, maxQty }]），直接修改其字段
 */
const props = defineProps({
  form: { type: Object, required: true },
  disabled: Boolean,
  colSpan: { type: Number, default: 12 }
})

const dict = useDict('wms_stock_alert_mode')
const modeTip = computed(() =>
  props.form.alertMode === StockAlertMode.WAREHOUSE ? t('wms.stockLimit.modeWarehouseTip') : t('wms.stockLimit.modeTotalTip')
)
const columns = [
  { title: t('wms.stockLimit.warehouse'), key: 'warehouse', width: 240 },
  { title: t('wms.stockLimit.minQty'), key: 'minQty', width: 140 },
  { title: t('wms.stockLimit.maxQty'), key: 'maxQty', width: 140 },
  { title: '', key: 'action', width: 50 }
]

const greater = (min, max) => min != null && max != null && Number(min) > Number(max)
const maxRules = [{ validator: () => (greater(props.form.safetyStock, props.form.maxStock) ? Promise.reject(t('wms.stockLimit.minGreaterMax')) : Promise.resolve()) }]
const rowMaxRules = (record) => [{ validator: () => (greater(record.minQty, record.maxQty) ? Promise.reject(t('wms.stockLimit.minGreaterMax')) : Promise.resolve()) }]
const warehouseRules = (index) => [
  { required: true, message: t('wms.stockLimit.selectWarehouse') },
  {
    validator: (_, v) =>
      v != null && props.form.warehouseLimits.some((r, i) => i !== index && r.warehouseId === v) ? Promise.reject(t('wms.stockLimit.duplicateWarehouse')) : Promise.resolve()
  }
]

function addRow() {
  props.form.warehouseLimits.push({ key: rowKey(), warehouseId: undefined, minQty: null, maxQty: null })
}

/** 各仓库下限 / 上限之和（保存后物料上的安全库存、库存上限） */
function sumText(field) {
  const values = props.form.warehouseLimits.map((r) => r[field]).filter((v) => v != null && v !== '')
  return values.length ? formatQty(values.reduce((s, v) => s + Number(v), 0)) : '-'
}
</script>

<style scoped>
.sub-title {
  font-weight: 500;
  margin-bottom: 8px;
}
.full {
  width: 100%;
}
.cell-item {
  margin-bottom: 0;
}
.add-row {
  margin-top: 8px;
}
.sum-tip {
  margin: 8px 0 16px;
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
