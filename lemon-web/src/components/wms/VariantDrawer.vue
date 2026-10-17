<template>
  <a-drawer :open="open" :title="material ? `${$t('wms.variant.title')} · ${material.code} ${material.name}` : ''" width="min(860px, 100vw)" destroy-on-close @close="emit('update:open', false)">
    <a-spin :spinning="loading">
      <a-alert
        v-if="!isTemplate"
        type="info"
        show-icon
        class="tip"
        :message="$t('wms.variant.tip')"
      />
      <div class="section-title">{{ $t('wms.variant.attributes') }}</div>
      <a-select
        v-if="!isTemplate"
        v-model:value="selectedAttrs"
        mode="multiple"
        :options="attrOptions"
        :max-tag-count="3"
        :placeholder="$t('wms.variant.attrPlaceholder')"
        class="full attr-select"
        @change="onAttrsChange"
      />
      <div v-for="a in chosenAttrs" :key="a.id" class="attr-block">
        <div class="attr-name">{{ a.name }}</div>
        <a-checkbox-group v-model:value="checked[a.id]">
          <a-checkbox v-for="v in a.values" :key="v.id" :value="v.id" :disabled="usedValues.has(v.id)">{{ v.value }}</a-checkbox>
        </a-checkbox-group>
        <a-button v-if="!isTemplate || a.values.some((v) => !usedValues.has(v.id))" type="link" size="small" @click="checkAll(a)">{{ $t('wms.variant.selectAll') }}</a-button>
      </div>
      <div v-if="canGenerate" class="generate">
        <i18n-t keypath="wms.variant.willAdd" tag="span"><template #n><b>{{ newCount }}</b></template></i18n-t>
        <a-button v-perm="'wms:material:add'" type="primary" :disabled="!newCount" :loading="generating" @click="generate">{{ $t('wms.variant.generate') }}</a-button>
        <span class="sub-text">{{ $t('wms.variant.attrTip') }}</span>
      </div>

      <template v-if="isTemplate && matrix">
        <div class="section-title matrix-title">
          {{ $t('wms.variant.skuCount', { n: matrix.variants.length }) }}<span v-if="stock" class="sub-text">{{ $t('wms.variant.stockTip') }}</span>
        </div>
        <!-- 两个属性：二维矩阵 -->
        <div v-if="matrix.attributes.length === 2" class="matrix-wrap">
          <table class="matrix">
            <thead>
              <tr>
                <th>{{ matrix.attributes[0].name }} \ {{ matrix.attributes[1].name }}</th>
                <th v-for="c in matrix.attributes[1].values" :key="c.id">{{ c.value }}</th>
                <th v-if="stock">{{ $t('common.field.total') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in matrix.attributes[0].values" :key="r.id">
                <th>{{ r.value }}</th>
                <td v-for="c in matrix.attributes[1].values" :key="c.id">
                  <template v-if="cell(r.id, c.id)">
                    <div class="code">{{ cell(r.id, c.id).code }}</div>
                    <div v-if="stock" class="qty">{{ formatQty(stock[cell(r.id, c.id).id] || 0) }}</div>
                  </template>
                  <span v-else class="sub-text">-</span>
                </td>
                <td v-if="stock" class="qty total">{{ formatQty(rowTotal(r.id)) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <a-table v-else row-key="id" size="small" :columns="variantColumns" :data-source="matrix.variants" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'stock'">{{ formatQty(stock?.[record.id] || 0) }}</template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="record.status === 0 ? 'default' : 'green'" :bordered="false">{{ record.status === 0 ? $t('common.status.disabled') : $t('common.status.enabled') }}</a-tag>
            </template>
          </template>
        </a-table>
      </template>
    </a-spin>
  </a-drawer>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { attributes, generateVariants, materialVariants, variantStock } from '@/api/wms/material'
import { formatQty } from '@/components/wms/wms'
import { t } from '@/locales'
import { useUserStore } from '@/store/user'

/**
 * 多规格：为物料选择规格属性和属性值生成 SKU；已是款式时可补充属性值生成新的 SKU，并查看各 SKU 的库存矩阵
 */
const props = defineProps({ open: Boolean, material: Object })
const emit = defineEmits(['update:open', 'generated'])
const userStore = useUserStore()

const loading = ref(false)
const generating = ref(false)
const allAttrs = ref([])
const matrix = ref(null)
const stock = ref(null)
const selectedAttrs = ref([])
const checked = reactive({})

const isTemplate = computed(() => props.material?.isTemplate === 1)
const attrOptions = computed(() => allAttrs.value.filter((a) => a.status !== 0).map((a) => ({ value: a.id, label: a.name })))
const chosenAttrs = computed(() => selectedAttrs.value.map((id) => allAttrs.value.find((a) => a.id === id)).filter(Boolean))
const usedValues = computed(() => new Set((matrix.value?.variants || []).flatMap((v) => Object.values(v.values).map(Number))))
const canGenerate = computed(() => chosenAttrs.value.length > 0)
const existingKeys = computed(() => new Set((matrix.value?.variants || []).map((v) => Object.values(v.values).map(Number).sort((a, b) => a - b).join(','))))

/** 所选属性值组合中尚未生成的数量 */
const newCount = computed(() => {
  const lists = chosenAttrs.value.map((a) => checked[a.id] || [])
  if (!lists.length || lists.some((l) => !l.length)) return 0
  let combos = [[]]
  lists.forEach((l) => {
    combos = combos.flatMap((c) => l.map((v) => [...c, v]))
  })
  return combos.filter((c) => !existingKeys.value.has([...c].sort((a, b) => a - b).join(','))).length
})

const variantColumns = [
  { title: t('wms.variant.skuCode'), dataIndex: 'code', width: 200 },
  { title: t('common.field.spec'), dataIndex: 'variantText' },
  { title: t('wms.variant.stock'), key: 'stock', width: 110, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 90 }
]

const cell = (r, c) =>
  matrix.value.variants.find((v) => Object.values(v.values).map(Number).includes(r) && Object.values(v.values).map(Number).includes(c))
const rowTotal = (r) =>
  matrix.value.attributes[1].values.reduce((s, c) => s + Number((cell(r, c.id) && stock.value?.[cell(r, c.id).id]) || 0), 0)

function onAttrsChange(ids) {
  if (ids.length > 3) selectedAttrs.value = ids.slice(0, 3)
  selectedAttrs.value.forEach((id) => (checked[id] ??= []))
}

function checkAll(a) {
  checked[a.id] = a.values.map((v) => v.id)
}

async function load() {
  loading.value = true
  matrix.value = null
  stock.value = null
  try {
    allAttrs.value = await attributes()
    if (isTemplate.value) {
      matrix.value = await materialVariants(props.material.id)
      selectedAttrs.value = matrix.value.attributes.map((a) => a.id)
      matrix.value.attributes.forEach((a) => (checked[a.id] = a.values.map((v) => v.id)))
      if (userStore.hasPerm('wms:stock:list')) stock.value = await variantStock(props.material.id)
    } else {
      selectedAttrs.value = []
      Object.keys(checked).forEach((k) => delete checked[k])
    }
  } finally {
    loading.value = false
  }
}

watch(
  () => props.open,
  (v) => v && props.material && load()
)

async function generate() {
  const valueIds = Object.fromEntries(chosenAttrs.value.map((a) => [a.id, checked[a.id]]))
  generating.value = true
  try {
    const n = await generateVariants(props.material.id, valueIds)
    message.success(t('wms.variant.generated', { n }))
    emit('generated')
    // 成为款式后重新加载矩阵
    props.material.isTemplate = 1
    await load()
  } finally {
    generating.value = false
  }
}
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.section-title {
  font-weight: 500;
  margin-bottom: 8px;
}
.matrix-title {
  margin-top: 20px;
}
.attr-select {
  margin-bottom: 12px;
}
.attr-block {
  padding: 8px 0;
  border-bottom: 1px dashed var(--app-border);
}
.attr-name {
  font-size: 13px;
  color: var(--app-text-2);
  margin-bottom: 4px;
}
.generate {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
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
  padding: 6px 10px;
  text-align: center;
  white-space: nowrap;
}
.matrix th {
  background: var(--app-bg-2, #fafafa);
  font-weight: 500;
}
.code {
  font-size: 12px;
  color: var(--app-text-3);
}
.qty {
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}
.total {
  background: var(--app-bg-2, #fafafa);
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
