<template>
  <a-modal :open="open" title="按规格录入" width="min(900px, 100vw)" ok-text="添加到明细" :ok-button-props="{ disabled: !total }" @ok="confirm" @cancel="close">
    <a-space direction="vertical" class="full">
      <a-select
        :value="templateId"
        show-search
        :filter-option="false"
        :options="templateList"
        :loading="searching"
        placeholder="搜索款式编码或名称"
        class="full"
        @search="search"
        @focus="!templateList.length && search('')"
        @change="pick"
      />
      <a-spin :spinning="loading">
        <template v-if="matrix">
          <div v-if="!matrix.variants.length" class="sub-text">该款式还没有生成 SKU</div>
          <!-- 两个属性：行 × 列矩阵 -->
          <div v-else-if="matrix.attributes.length === 2" class="matrix-wrap">
            <table class="matrix">
              <thead>
                <tr>
                  <th>{{ matrix.attributes[0].name }} \ {{ matrix.attributes[1].name }}</th>
                  <th v-for="c in matrix.attributes[1].values" :key="c.id">{{ c.value }}</th>
                  <th>合计</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="r in matrix.attributes[0].values" :key="r.id">
                  <th>{{ r.value }}</th>
                  <td v-for="c in matrix.attributes[1].values" :key="c.id">
                    <a-input-number v-if="cell(r.id, c.id)" v-model:value="qty[cell(r.id, c.id).id]" :min="0" :precision="precision(cell(r.id, c.id))" size="small" class="cell" />
                    <span v-else class="sub-text">-</span>
                  </td>
                  <td class="total">{{ formatQty(rowTotal(r.id)) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <!-- 一个或三个属性：逐行录入 -->
          <a-table v-else row-key="id" size="small" :columns="columns" :data-source="matrix.variants" :pagination="false" :scroll="{ y: 400 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'qty'">
                <a-input-number v-model:value="qty[record.id]" :min="0" :precision="precision(record)" size="small" class="full" />
              </template>
            </template>
          </a-table>
          <div v-if="matrix.variants.length" class="summary">合计数量 <b>{{ formatQty(total) }}</b> {{ matrix.template.unit || '' }}</div>
        </template>
      </a-spin>
    </a-space>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { materialVariants, templateOptions } from '@/api/wms/material'
import { formatQty } from '@/components/wms/wms'

/**
 * 按规格录入：选择款式后，在规格矩阵（如 颜色 × 尺码）中填写各 SKU 的数量，确认后返回
 * [{ material: { id, code, name, spec, unit, serialEnabled, label }, qty }]
 */
defineProps({ open: Boolean })
const emit = defineEmits(['update:open', 'confirm'])

const templateId = ref()
const templateList = ref([])
const searching = ref(false)
const loading = ref(false)
const matrix = ref(null)
const qty = reactive({})
let timer

const columns = [
  { title: 'SKU 编码', dataIndex: 'code', width: 200 },
  { title: '规格', dataIndex: 'variantText' },
  { title: '数量', key: 'qty', width: 160 }
]

const precision = (v) => (v.serialEnabled === 1 ? 0 : 4)
const valueIds = (v) => Object.values(v.values).map(Number)
const cell = (r, c) => matrix.value.variants.find((v) => valueIds(v).includes(r) && valueIds(v).includes(c))
const rowTotal = (r) => matrix.value.attributes[1].values.reduce((s, c) => s + (Number(qty[cell(r, c.id)?.id]) || 0), 0)
const total = computed(() => (matrix.value?.variants || []).reduce((s, v) => s + (Number(qty[v.id]) || 0), 0))

function search(keyword) {
  clearTimeout(timer)
  timer = setTimeout(async () => {
    searching.value = true
    try {
      const list = await templateOptions(keyword || undefined)
      templateList.value = list.map((m) => ({ value: m.id, label: `${m.code} ${m.name}` }))
    } finally {
      searching.value = false
    }
  }, 250)
}

async function pick(id) {
  templateId.value = id
  Object.keys(qty).forEach((k) => delete qty[k])
  loading.value = true
  try {
    matrix.value = await materialVariants(id)
  } finally {
    loading.value = false
  }
}

function close() {
  emit('update:open', false)
}

function confirm() {
  const lines = matrix.value.variants
    .filter((v) => Number(qty[v.id]) > 0 && v.status !== 0)
    .map((v) => ({
      material: { id: v.id, code: v.code, name: v.name, spec: v.spec, unit: v.unit, serialEnabled: v.serialEnabled, label: `${v.code} ${v.name}${v.spec ? `（${v.spec}）` : ''}` },
      qty: Number(qty[v.id])
    }))
  emit('confirm', lines)
  Object.keys(qty).forEach((k) => delete qty[k])
  close()
}
</script>

<style scoped>
.full {
  width: 100%;
}
.matrix-wrap {
  overflow-x: auto;
}
.matrix {
  border-collapse: collapse;
  min-width: 100%;
}
.matrix th,
.matrix td {
  border: 1px solid var(--app-border);
  padding: 4px 6px;
  text-align: center;
  white-space: nowrap;
}
.matrix th {
  background: var(--app-bg-2, #fafafa);
  font-weight: 500;
}
.cell {
  width: 90px;
}
.total {
  background: var(--app-bg-2, #fafafa);
  font-variant-numeric: tabular-nums;
}
.summary {
  margin-top: 8px;
  text-align: right;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
