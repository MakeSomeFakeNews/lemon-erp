<template>
  <SubPage :open="open" title="补料申请" width="min(820px, 100vw)" destroy-on-close @close="emit('update:open', false)">
    <a-alert
      type="info"
      show-icon
      class="tip"
      message="领料达到上限（需求 × (1 + 允许超领比例) + 已补料）后，超出部分提交补料申请。流程中心发布了「mes_supplement」流程时走审批，否则提交即可发料。"
    />
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item label="工单" name="woId" :rules="[{ required: true, message: '请选择工单' }]">
            <a-input v-if="wo" :value="`${wo.woNo}  ${wo.materialCode} ${wo.materialName}`" disabled />
            <a-select
              v-else
              v-model:value="form.woId"
              show-search
              :filter-option="false"
              :options="woOptions"
              :loading="woLoading"
              placeholder="搜索已下达 / 生产中的工单号"
              not-found-content="没有可补料的工单"
              @search="searchWo"
              @focus="() => !woOptions.length && searchWo('')"
              @change="onWoChange"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item label="补料原因" name="reason" :rules="[{ required: true, message: '请选择补料原因' }]">
            <a-select v-model:value="form.reason" placeholder="请选择" :options="toOptions(dict.mes_supplement_reason)" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="说明" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" placeholder="如：焊接不良报废 3 件" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-table row-key="key" size="small" :columns="columns" :data-source="form.items" :pagination="false" :scroll="{ x: 640 }">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'material'">
            <a-form-item :name="['items', index, 'materialId']" :rules="[{ required: true, message: '请选择物料' }]" class="cell-item">
              <a-select v-model:value="record.materialId" :options="materialOptions" placeholder="工单用料" />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'progress'">
            <template v-if="lineOf(record.materialId)">
              <div>需求 {{ formatQty(lineOf(record.materialId).requiredQty) }}，已领 {{ formatQty(lineOf(record.materialId).issuedQty) }}</div>
              <div v-if="Number(lineOf(record.materialId).supplementQty)" class="sub-text">已补料 {{ formatQty(lineOf(record.materialId).supplementQty) }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-form-item :name="['items', index, 'qty']" :rules="[{ required: true, message: '请填写数量' }]" class="cell-item">
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
        <template #icon><PlusOutlined /></template>添加物料
      </a-button>
    </a-form>
    <template #footer>
      <div class="drawer-footer">
        <a-button @click="emit('update:open', false)">取消</a-button>
        <a-button type="primary" :loading="saving" @click="submit">提交申请</a-button>
      </div>
    </template>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
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

const dict = useDict('mes_supplement_reason')
const columns = [
  { title: '物料', key: 'material', width: 260 },
  { title: '领料情况', key: 'progress', width: 200 },
  { title: '补料数量', key: 'qty', width: 160 },
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
    message.success('已提交补料申请')
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
