<template>
  <!-- 物料表单的基础信息（在父组件的 a-form 中，直接修改 form） -->
  <a-row :gutter="16">
    <a-col :xs="24" :sm="colSpan">
      <a-form-item :label="$t('wms.material.code')" name="code" :class="ai.cls('code')">
        <a-input v-model:value="form.code" :maxlength="64" :placeholder="$t('wms.material.codePh')" :disabled="readonly" />
      </a-form-item>
    </a-col>
    <a-col :xs="24" :sm="colSpan">
      <a-form-item :label="$t('wms.material.name')" name="name" :class="ai.cls('name')">
        <a-input v-model:value="form.name" :maxlength="128" :disabled="readonly" />
      </a-form-item>
    </a-col>
    <a-col :xs="24" :sm="colSpan">
      <a-form-item :label="$t('wms.inout.spec')" name="spec">
        <a-input v-model:value="form.spec" :maxlength="128" :disabled="readonly" />
      </a-form-item>
    </a-col>
    <a-col :xs="24" :sm="colSpan">
      <a-form-item :label="$t('wms.material.unit')" name="unit">
        <a-auto-complete v-model:value="form.unit" :options="UNITS" :maxlength="16" :placeholder="$t('wms.material.unitPh')" :disabled="readonly" />
      </a-form-item>
    </a-col>
    <a-col :xs="24" :sm="colSpan">
      <a-form-item :label="$t('wms.inout.category')" name="category" :class="ai.cls('category')">
        <MaterialCategorySelect v-model="form.category" usable-only :placeholder="$t('common.placeholder.select')" :disabled="readonly" />
      </a-form-item>
    </a-col>
    <a-col :xs="24" :sm="colSpan">
      <a-form-item :label="$t('pricing.goods.brand')" name="brand">
        <a-select
          v-model:value="form.brand"
          :placeholder="$t('wms.common.optional')"
          allow-clear
          show-search
          option-filter-prop="label"
          :options="toOptions(dict.wms_material_brand)"
          :disabled="readonly"
        />
      </a-form-item>
    </a-col>
    <a-col :span="24">
      <!-- 虚拟商品（服务项目）：只用于销售开单和收入统计，不管库存，库存、批次、计划等属性隐藏 -->
      <a-form-item :label="$t('wms.material.goodsType')" name="isVirtual" :extra="isVirtual ? $t('wms.material.virtualTip') : undefined">
        <a-radio-group v-model:value="form.isVirtual" :disabled="readonly" @change="onVirtualChange">
          <a-radio :value="0">{{ $t('wms.material.physical') }}</a-radio>
          <a-radio :value="1">{{ $t('wms.material.virtual') }}</a-radio>
        </a-radio-group>
      </a-form-item>
    </a-col>
    <a-col v-if="!isVirtual" :xs="24" :sm="colSpan">
      <a-form-item :label="$t('wms.stock.safetyStock')" name="safetyStock" :extra="$t('wms.material.safetyTip')">
        <a-input-number v-model:value="form.safetyStock" :min="0" :precision="4" class="full" :placeholder="$t('wms.material.noAlert')" :disabled="readonly" />
      </a-form-item>
    </a-col>
    <a-col v-if="!isVirtual" :xs="24" :sm="colSpan">
      <a-form-item name="reorderPoint" :extra="$t('wms.material.reorderPointExtra')">
        <template #label>
          {{ $t('wms.material.reorderPoint') }}
          <a-tooltip :title="$t('wms.material.reorderPointTip')">
            <QuestionCircleOutlined class="label-help" />
          </a-tooltip>
        </template>
        <a-input-number v-model:value="form.reorderPoint" :min="0" :precision="4" class="full" :placeholder="$t('wms.material.notInvolved')" :disabled="readonly" />
      </a-form-item>
    </a-col>
    <a-col v-if="!isVirtual" :xs="24" :sm="colSpan">
      <a-form-item :label="$t('wms.material.reorderQty')" name="reorderQty" :extra="$t('wms.material.reorderQtyTip')">
        <a-input-number v-model:value="form.reorderQty" :min="0" :precision="4" class="full" :placeholder="$t('wms.common.optional')" :disabled="readonly" />
      </a-form-item>
    </a-col>
    <a-col :xs="24" :sm="colSpan">
      <a-form-item :label="$t('wms.units.barcode')" name="barcode" :extra="$t('wms.material.barcodeTip')">
        <a-input v-model:value="form.barcode" :maxlength="64" :placeholder="$t('wms.common.optional')" allow-clear :disabled="readonly" />
      </a-form-item>
    </a-col>
    <a-col v-if="!isVirtual" :xs="24" :sm="colSpan">
      <a-form-item :label="$t('wms.material.refCost')" name="refCost" :extra="$t('wms.material.refCostTip')">
        <a-input-number v-model:value="form.refCost" :min="0" :precision="4" class="full" :placeholder="$t('wms.common.optional')" :disabled="readonly" />
      </a-form-item>
    </a-col>
    <a-col v-if="!isVirtual" :xs="24" :sm="colSpan">
      <a-form-item :label="$t('wms.material.costMethod')" name="costMethod" :extra="$t('wms.material.costMethodTip')">
        <a-select v-model:value="form.costMethod" :options="toOptions(dict.wms_cost_method)" :disabled="readonly" />
      </a-form-item>
    </a-col>
    <a-col v-if="!isVirtual" :xs="24" :sm="colSpan">
      <a-form-item :label="$t('wms.material.defaultWarehouse')" name="defaultWarehouseId">
        <WarehouseSelect v-model:value="form.defaultWarehouseId" :placeholder="$t('wms.common.optional')" :disabled="readonly" @update:value="form.defaultLocationId = null" />
      </a-form-item>
    </a-col>
    <a-col v-if="!isVirtual" :xs="24" :sm="colSpan">
      <a-form-item :label="$t('wms.material.defaultLocation')" name="defaultLocationId" :extra="$t('wms.material.defaultLocationTip')">
        <LocationSelect v-model:value="form.defaultLocationId" :warehouse-id="form.defaultWarehouseId" :disabled="readonly" />
      </a-form-item>
    </a-col>
    <a-col v-if="enabledLevels.length" :span="24">
      <div class="sub-title">{{ $t('pricing.level.prices') }}</div>
      <a-row :gutter="16">
        <a-col v-for="l in enabledLevels" :key="l.level" :xs="12" :sm="6">
          <a-form-item :label="l.name" :name="l.field">
            <a-input-number v-model:value="form[l.field]" :min="0" :precision="4" class="full" :disabled="readonly" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-col>
    <a-col v-if="!isVirtual" :span="24">
      <a-form-item :label="$t('wms.material.tracking')" :extra="$t('wms.material.trackingTip')">
        <a-space direction="vertical">
          <a-checkbox :checked="form.batchEnabled === 1" :disabled="readonly" @change="(e) => (form.batchEnabled = e.target.checked ? 1 : 0)">
            {{ $t('wms.material.batchManaged') }}<span class="option-desc">{{ $t('wms.material.batchManagedDesc') }}</span>
          </a-checkbox>
          <a-checkbox :checked="form.serialEnabled === 1" :disabled="readonly" @change="(e) => (form.serialEnabled = e.target.checked ? 1 : 0)">
            {{ $t('wms.material.serialManaged') }}<span class="option-desc">{{ $t('wms.material.serialManagedDesc') }}</span>
          </a-checkbox>
          <a-checkbox :checked="form.iqcRequired === 1" :disabled="readonly" @change="(e) => (form.iqcRequired = e.target.checked ? 1 : 0)">
            {{ $t('wms.material.iqc') }}<span class="option-desc">{{ $t('wms.material.iqcDesc') }}</span>
          </a-checkbox>
        </a-space>
      </a-form-item>
    </a-col>
    <a-col :span="24">
      <a-form-item :label="$t('common.field.remark')" name="remark">
        <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" show-count :disabled="readonly" />
      </a-form-item>
    </a-col>
  </a-row>
</template>

<script setup>
import { computed } from 'vue'
import { QuestionCircleOutlined } from '@ant-design/icons-vue'
import { isZh } from '@/locales'
import LocationSelect from '@/components/wms/LocationSelect.vue'
import MaterialCategorySelect from '@/components/wms/MaterialCategorySelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'

/**
 * 物料基础信息：编码、名称、规格、单位、分类、品牌、商品类型（实物 / 虚拟商品）、库存属性、价格列、批次 / 序列号 / 来料检验、备注。
 * 虚拟商品隐藏库存、成本、批次等属性
 */
const props = defineProps({
  /** 表单（直接修改） */
  form: { type: Object, required: true },
  colSpan: { type: Number, default: 12 },
  readonly: Boolean,
  /** 智能录入的预填提示（useAiPrefill） */
  ai: { type: Object, required: true },
  /** 启用的价格列 */
  enabledLevels: { type: Array, default: () => [] }
})
const dict = useDict('wms_material_brand', 'wms_cost_method')
const UNITS = (isZh() ? ['个', '件', '台', '箱', '包', '桶', '瓶', '袋', 'kg', 'g', 'L', 'm'] : ['pcs', 'set', 'box', 'pack', 'bag', 'bottle', 'drum', 'kg', 'g', 'L', 'm']).map((value) => ({ value })) // i18n-ignore

const isVirtual = computed(() => props.form.isVirtual === 1)
/** 设为虚拟商品：清除库存、批次等无关属性（保存时后端同样不保存） */
function onVirtualChange() {
  if (!isVirtual.value) return
  Object.assign(props.form, {
    batchEnabled: 0, serialEnabled: 0, iqcRequired: 0, safetyStock: null, reorderPoint: null, reorderQty: null,
    defaultWarehouseId: null, defaultLocationId: null, refCost: null
  })
}
</script>

<style scoped>
.full {
  width: 100%;
}
.option-desc {
  color: var(--app-text-3);
  font-weight: normal;
  font-size: 12px;
}
.label-help {
  margin-left: 4px;
  color: var(--app-text-3);
  cursor: help;
}
.sub-title {
  font-weight: 500;
  margin-bottom: 8px;
}
</style>
