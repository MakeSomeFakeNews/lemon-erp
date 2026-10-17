<template>
  <a-modal
    :open="open"
    :title="material ? `${$t('wms.units.title')} - ${material.code} ${material.name}` : $t('wms.units.title')"
    width="min(640px, 100vw)"
    destroy-on-close
    :confirm-loading="saving"
    :ok-button-props="{ disabled: !editable || loading }"
    :ok-text="$t('common.action.save')"
    @ok="save"
    @cancel="emit('update:open', false)"
  >
    <a-alert
      type="info"
      show-icon
      class="tip"
      :message="$t('wms.units.tip', { unit: material?.unit || '-' })"
    />
    <a-spin :spinning="loading">
      <a-form ref="formRef" :model="form">
        <a-table row-key="key" size="small" :columns="columns" :data-source="form.rows" :pagination="false" :locale="{ emptyText: $t('wms.units.empty') }">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'unit'">
              <a-form-item :name="['rows', index, 'unit']" :rules="unitRules" class="cell-item">
                <a-input v-model:value="record.unit" :maxlength="16" :placeholder="$t('wms.units.unitPlaceholder')" :disabled="!editable" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'ratio'">
              <a-form-item :name="['rows', index, 'ratio']" :rules="[{ required: true, message: $t('wms.units.inputRatio') }]" class="cell-item">
                <a-input-number
                  v-model:value="record.ratio"
                  :min="0.0001"
                  :precision="4"
                  class="full"
                  :addon-after="material?.unit || undefined"
                  :disabled="!editable"
                />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'barcode'">
              <a-input v-model:value="record.barcode" :maxlength="64" :placeholder="$t('wms.common.optional')" :disabled="!editable" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button v-if="editable" type="link" danger size="small" @click="form.rows.splice(index, 1)"><DeleteOutlined /></a-button>
            </template>
          </template>
        </a-table>
      </a-form>
      <a-button v-if="editable" type="dashed" block class="add-row" @click="addRow">
        <template #icon><PlusOutlined /></template>{{ $t('wms.units.add') }}
      </a-button>
    </a-spin>
  </a-modal>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { materialUnits, saveMaterialUnits } from '@/api/wms/material'
import { rowKey } from '@/components/wms/wms'
import { t } from '@/locales'

/** 物料的辅助单位维护：整体保存 */
const props = defineProps({
  open: Boolean,
  /** { id, code, name, unit } */
  material: Object,
  editable: Boolean
})
const emit = defineEmits(['update:open', 'saved'])

const columns = [
  { title: t('wms.units.unit'), key: 'unit', width: 140 },
  { title: t('wms.units.ratio'), key: 'ratio', width: 180 },
  { title: t('wms.units.barcode'), key: 'barcode' },
  { title: '', key: 'action', width: 50 }
]
const unitRules = [
  { required: true, whitespace: true, message: t('wms.units.inputUnit') },
  {
    validator: (_, v) => {
      const name = v?.trim()
      if (!name) return Promise.resolve()
      if (name === props.material?.unit) return Promise.reject(t('wms.units.sameAsBase'))
      if (form.rows.filter((r) => r.unit?.trim() === name).length > 1) return Promise.reject(t('wms.units.duplicate'))
      return Promise.resolve()
    }
  }
]

const formRef = ref()
const form = reactive({ rows: [] })
const loading = ref(false)
const saving = ref(false)

watch(
  () => props.open,
  async (open) => {
    if (!open || !props.material) return
    form.rows = []
    loading.value = true
    try {
      const list = await materialUnits(props.material.id)
      form.rows = list.map((u) => ({ key: rowKey(), unit: u.unit, ratio: Number(u.ratio), barcode: u.barcode || '' }))
    } finally {
      loading.value = false
    }
  }
)

function addRow() {
  form.rows.push({ key: rowKey(), unit: '', ratio: null, barcode: '' })
}

async function save() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    await saveMaterialUnits(
      props.material.id,
      form.rows.map((r) => ({ unit: r.unit.trim(), ratio: r.ratio, barcode: r.barcode?.trim() || null }))
    )
    message.success(t('common.msg.saveSuccess'))
    emit('update:open', false)
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
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
</style>
