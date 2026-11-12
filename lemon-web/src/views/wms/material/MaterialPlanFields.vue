<template>
  <!-- 物料表单的生产计划属性与替代料（在父组件的 a-form 中，直接修改 form） -->
  <div class="section-title">{{ $t('wms.material.plan') }}</div>
  <a-row :gutter="16">
    <a-col :xs="24" :sm="8">
      <a-form-item :label="$t('wms.material.supplyType')" name="supplyType" :extra="SUPPLY_HINTS[form.supplyType || 'AUTO']">
        <a-select v-model:value="form.supplyType" :options="supplyOptions" />
      </a-form-item>
    </a-col>
    <a-col :xs="24" :sm="8">
      <a-form-item :label="$t('wms.material.leadDays')" name="leadDays" :extra="$t('wms.material.leadDaysTip')">
        <a-input-number v-model:value="form.leadDays" :min="0" :max="365" :precision="0" :addon-after="$t('common.unit.day')" class="full" :placeholder="$t('wms.material.auto')" />
      </a-form-item>
    </a-col>
    <a-col :xs="24" :sm="8">
      <a-form-item :label="$t('wms.material.planner')" name="planner">
        <a-input v-model:value="form.planner" :maxlength="64" :placeholder="$t('wms.common.optional')" />
      </a-form-item>
    </a-col>
    <a-col :xs="24" :sm="8">
      <a-form-item :label="$t('wms.material.minLot')" name="minLot" :extra="$t('wms.material.minLotTip')">
        <a-input-number v-model:value="form.minLot" :min="0" :precision="4" class="full" :placeholder="$t('wms.material.noLimit')" />
      </a-form-item>
    </a-col>
    <a-col :xs="24" :sm="8">
      <a-form-item :label="$t('wms.material.lotMultiple')" name="lotMultiple" :extra="$t('wms.material.lotMultipleTip')">
        <a-input-number v-model:value="form.lotMultiple" :min="0" :precision="4" class="full" :placeholder="$t('wms.material.noRounding')" />
      </a-form-item>
    </a-col>
    <a-col :xs="24" :sm="8">
      <a-form-item :label="$t('wms.material.overIssue')" name="overIssueRate" :extra="$t('wms.material.overIssueTip')">
        <a-input-number v-model:value="form.overIssueRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
      </a-form-item>
    </a-col>
    <a-col :span="24">
      <a-checkbox :checked="form.fqcRequired === 1" @change="(e) => (form.fqcRequired = e.target.checked ? 1 : 0)">
        {{ $t('wms.material.fqcRequired') }}<span class="option-desc">{{ $t('wms.material.fqcDesc') }}</span>
      </a-checkbox>
    </a-col>
  </a-row>

  <div class="sub-title">
    {{ $t('wms.material.substitute') }}
    <span class="option-desc">{{ $t('wms.material.substituteTip') }}</span>
  </div>
  <a-table
    row-key="key"
    size="small"
    :columns="subColumns"
    :data-source="form.substitutes"
    :pagination="false"
    :scroll="{ x: subScrollX }"
    :locale="{ emptyText: $t('wms.material.noSubstitute') }"
  >
    <template #bodyCell="{ column, record, index }">
      <template v-if="column.key === 'substitute'">
        <a-form-item :name="['substitutes', index, 'substituteId']" :rules="[{ required: true, message: $t('wms.material.selectSubstitute') }]" class="cell-item">
          <MaterialSelect
            :value="record.substituteId"
            :label="record.label"
            @update:value="(v) => (record.substituteId = v)"
            @select="(m) => (record.label = m.label)"
          />
        </a-form-item>
      </template>
      <template v-else-if="column.key === 'ratio'">
        <a-form-item :name="['substitutes', index, 'ratio']" :rules="[{ required: true, message: $t('wms.material.inputRatio') }]" class="cell-item">
          <a-input-number v-model:value="record.ratio" :min="0" :step="0.5" class="full" />
        </a-form-item>
      </template>
      <template v-else-if="column.key === 'priority'">
        <a-input-number v-model:value="record.priority" :min="1" :max="99" :precision="0" class="full" />
      </template>
      <template v-else-if="column.key === 'remark'">
        <a-input v-model:value="record.remark" :maxlength="255" :placeholder="$t('wms.material.condition')" />
      </template>
      <template v-else-if="column.key === 'action'">
        <a-button type="link" danger size="small" @click="form.substitutes.splice(index, 1)"><DeleteOutlined /></a-button>
      </template>
    </template>
  </a-table>
  <a-button type="dashed" block class="add-row" :disabled="form.supplyType === SupplyType.PHANTOM" @click="addSubstitute">
    <template #icon><PlusOutlined /></template>{{ $t('wms.material.addSubstitute') }}
  </a-button>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { rowKey } from '@/components/wms/wms'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { SupplyType } from '@/constants/enums'

/** 生产计划属性（供应方式、提前期、批量规则、超领、完工检验、计划员）和替代料，需要 mes:material:edit */
const props = defineProps({
  /** 表单（直接修改） */
  form: { type: Object, required: true }
})
const { t } = useI18n()
const dict = useDict('mes_supply_type')
const SUPPLY_HINTS = {
  AUTO: t('wms.material.supplyHint.AUTO'),
  MAKE: t('wms.material.supplyHint.MAKE'),
  BUY: t('wms.material.supplyHint.BUY'),
  OUTSOURCE: t('wms.material.supplyHint.OUTSOURCE'),
  PHANTOM: t('wms.material.supplyHint.PHANTOM')
}
const supplyOptions = computed(() => [{ value: '', label: t('wms.material.auto') }, ...toOptions(dict.mes_supply_type)])

const subColumns = [
  { title: t('wms.material.substitute'), key: 'substitute', width: 290 },
  { title: t('wms.material.ratio'), key: 'ratio', width: 120 },
  { title: t('wms.material.priority'), key: 'priority', width: 90 },
  { title: t('common.field.remark'), key: 'remark', width: 180 },
  { title: '', key: 'action', width: 50 }
]
const subScrollX = subColumns.reduce((sum, c) => sum + c.width, 0)

function addSubstitute() {
  props.form.substitutes.push({ key: rowKey(), substituteId: undefined, label: undefined, ratio: 1, priority: props.form.substitutes.length + 1, remark: '' })
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
.section-title {
  font-weight: 600;
  margin: 4px 0 12px;
  padding-left: 8px;
  border-left: 3px solid var(--app-primary);
}
.sub-title {
  font-weight: 500;
  margin-bottom: 8px;
}
.cell-item {
  margin-bottom: 0;
}
.add-row {
  margin-top: 8px;
}
</style>
