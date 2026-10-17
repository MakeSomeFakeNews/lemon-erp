<template>
  <SubPage :open="open" :title="$t('mes.supplementForm.title')" width="min(820px, 100vw)" destroy-on-close @close="emit('update:open', false)">
    <a-alert
      type="info"
      show-icon
      class="tip"
      :message="$t('mes.supplementForm.tip')"
    />
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('mes.supplementForm.wo')" name="woId" :rules="[{ required: true, message: $t('mes.supplementForm.woRequired') }]">
            <a-input v-if="wo" :value="`${wo.woNo}  ${wo.materialCode} ${wo.materialName}`" disabled />
            <a-select
              v-else
              v-model:value="form.woId"
              show-search
              :filter-option="false"
              :options="woOptions"
              :loading="woLoading"
              :placeholder="$t('mes.supplementForm.woPh')"
              :not-found-content="$t('mes.supplementForm.woNotFound')"
              @search="searchWo"
              @focus="() => !woOptions.length && searchWo('')"
              @change="onWoChange"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('mes.supplementForm.reason')" name="reason" :rules="[{ required: true, message: $t('mes.supplementForm.reasonRequired') }]">
            <a-select v-model:value="form.reason" :placeholder="$t('common.placeholder.select')" :options="toOptions(dict.mes_supplement_reason)" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.description')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" :placeholder="$t('mes.supplementForm.remarkPh')" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-table row-key="key" size="small" :columns="columns" :data-source="form.items" :pagination="false" :scroll="{ x: 640 }">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'material'">
            <a-form-item :name="['items', index, 'materialId']" :rules="[{ required: true, message: $t('mes.supplementForm.materialRequired') }]" class="cell-item">
              <a-select v-model:value="record.materialId" :options="materialOptions" :placeholder="$t('mes.supplementForm.woMaterial')" />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'progress'">
            <template v-if="lineOf(record.materialId)">
              <div>{{ $t('mes.supplementForm.progressText', { req: formatQty(lineOf(record.materialId).requiredQty), issued: formatQty(lineOf(record.materialId).issuedQty) }) }}</div>
              <div v-if="Number(lineOf(record.materialId).supplementQty)" class="sub-text">{{ $t('mes.supplementForm.supplemented', { v: formatQty(lineOf(record.materialId).supplementQty) }) }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-form-item :name="['items', index, 'qty']" :rules="[{ required: true, message: $t('mes.supplementForm.qtyRequired') }]" class="cell-item">
              <a-input-number
                v-model:value="record.qty"
                :min="0"
                :precision="lineOf(record.materialId)?.serialEnabled === 1 ? 0 : 4"
                :addon-after="lineOf(record.materialId)?.unit || undefined"
                class="full"
              />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" :disabled="form.items.length <= 1" @click="form.items.splice(index, 1)">
              <DeleteOutlined />
            </a-button>
          </template>
        </template>
      </a-table>
      <a-button type="dashed" block class="add-row" :disabled="!lines.length" @click="addRow()">
        <template #icon><PlusOutlined /></template>{{ $t('mes.supplementForm.addMaterial') }}
      </a-button>
    </a-form>
    <template #footer>
      <div class="drawer-footer">
        <a-button @click="emit('update:open', false)">{{ $t('common.action.cancel') }}</a-button>
        <a-button type="primary" :loading="saving" @click="submit">{{ $t('mes.supplementForm.submit') }}</a-button>
      </div>
    </template>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import { addSupplement, getWo, pageWo } from '@/api/mes/wo'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { formatQty, rowKey } from '@/components/wms/wms'

/**
 * 补料申请表单
 * - wo：工单详情（含 materials），不传时在表单中选择工单
 * - materialId：默认补料的物料
 */
const props = defineProps({
  open: Boolean,
  wo: Object,
  materialId: Number
})
const emit = defineEmits(['update:open', 'success'])
const { t } = useI18n()

const dict = useDict('mes_supplement_reason')
const columns = [
  { title: t('mes.supplementForm.material'), key: 'material', width: 260 },
  { title: t('mes.supplementForm.progress'), key: 'progress', width: 220 },
  { title: t('mes.supplementForm.qty'), key: 'qty', width: 170 },
  { title: '', key: 'action', width: 50 }
]

const formRef = ref()
const form = ref({ woId: undefined, reason: undefined, remark: '', items: [] })
const saving = ref(false)
const lines = ref([])
const materialOptions = computed(() => lines.value.map((m) => ({ value: m.materialId, label: `${m.materialCode} ${m.materialName}` })))
const lineOf = (materialId) => lines.value.find((m) => m.materialId === materialId)

function addRow(materialId) {
  form.value.items.push({ key: rowKey(), materialId, qty: undefined })
}

watch(
  () => props.open,
  (open) => {
    if (!open) return
    form.value = { woId: props.wo?.id, reason: undefined, remark: '', items: [] }
    lines.value = props.wo?.materials || []
    woOptions.value = []
    addRow(props.materialId)
  }
)

// ---------------------------------------------------------------- 选择工单
const woOptions = ref([])
const woLoading = ref(false)
let seq = 0
async function searchWo(keyword) {
  const current = ++seq
  woLoading.value = true
  try {
    const [released, started] = await Promise.all(
      ['RELEASED', 'STARTED'].map((status) => pageWo({ current: 1, size: 20, status, woNo: keyword || undefined }))
    )
    if (current !== seq) return
    woOptions.value = [...started.records, ...released.records].map((w) => ({
      value: w.id,
      label: `${w.woNo}  ${w.materialCode} ${w.materialName}`
    }))
  } finally {
    if (current === seq) woLoading.value = false
  }
}
async function onWoChange(id) {
  lines.value = []
  form.value.items = []
  addRow()
  if (id) lines.value = (await getWo(id)).materials
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    await addSupplement({
      woId: form.value.woId,
      reason: form.value.reason,
      remark: form.value.remark || null,
      items: form.value.items.map((i) => ({ materialId: i.materialId, qty: i.qty }))
    })
    message.success(t('mes.supplementForm.submitted'))
    emit('update:open', false)
    emit('success')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.tip {
  margin-bottom: 16px;
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
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
