<template>
  <SubPage
    :open="open"
    :title="supplier ? `供货价格 - ${supplier.name}` : '供货价格'"
    width="min(960px, 100vw)"
    destroy-on-close
    @close="emit('update:open', false)"
  >
    <div class="toolbar">
      <a-input-search v-model:value="keyword" placeholder="物料编码或名称" allow-clear class="search" @search="search" />
      <a-button v-perm="'scm:supplier:edit'" type="primary" @click="openForm()">
        <template #icon><PlusOutlined /></template>添加物料
      </a-button>
    </div>
    <a-table row-key="id" size="small" :columns="columns" :data-source="list" :loading="loading" :pagination="pagination" @change="onTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'material'">
          <div>
            {{ record.materialCode }} {{ record.materialName }}
            <a-tag v-if="record.isDefault === 1" color="gold" :bordered="false">默认</a-tag>
          </div>
          <div class="sub-text">{{ record.spec }}</div>
        </template>
        <template v-else-if="column.key === 'price'">
          <div>¥{{ Number(record.price).toLocaleString('zh-CN', { maximumFractionDigits: 4 }) }} / {{ record.unit || '单位' }}</div>
          <div class="sub-text">
            税率 {{ Number(record.taxRate) }}%
            <a-tooltip v-if="record.tiers?.length" :title="tierText(record.tiers)">
              <a-tag color="blue" :bordered="false" class="tier-tag">阶梯价</a-tag>
            </a-tooltip>
          </div>
        </template>
        <template v-else-if="column.key === 'validity'">
          <template v-if="record.validFrom || record.validTo">
            <div>{{ record.validFrom || '不限' }} ~ {{ record.validTo || '不限' }}</div>
            <a-tag v-if="record.validTo && record.validTo < today()" color="red" :bordered="false">已过期</a-tag>
            <a-tag v-else-if="record.validFrom && record.validFrom > today()" color="orange" :bordered="false">未生效</a-tag>
          </template>
          <span v-else class="sub-text">长期有效</span>
        </template>
        <template v-else-if="column.key === 'leadDays'">{{ record.leadDays }} 天</template>
        <template v-else-if="column.key === 'minQty'">{{ formatQty(record.minQty) }}</template>
        <template v-else-if="column.key === 'action'">
          <span class="table-actions">
            <a-button type="link" size="small" @click="openHistory(record)">价格历史</a-button>
          </span>
          <span v-perm="'scm:supplier:edit'" class="table-actions">
            <a-button type="link" size="small" @click="openForm(record)">编辑</a-button>
            <a-popconfirm title="确定删除该供货价格？" @confirm="remove(record.id)">
              <a-button type="link" danger size="small">删除</a-button>
            </a-popconfirm>
          </span>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="formOpen" :title="form.id ? '编辑供货价格' : '添加供货价格'" :confirm-loading="saving" @ok="submit">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="物料" name="materialId">
          <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" :disabled="!!form.id" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="含税单价" name="price">
              <a-input-number v-model:value="form.price" :min="0" :precision="4" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="税率" name="taxRate">
              <a-input-number v-model:value="form.taxRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="采购提前期" name="leadDays">
              <a-input-number v-model:value="form.leadDays" :min="0" :max="365" addon-after="天" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="最小起订量" name="minQty">
              <a-input-number v-model:value="form.minQty" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="有效期起" name="validFrom">
              <a-date-picker v-model:value="form.validFrom" value-format="YYYY-MM-DD" placeholder="不限" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="有效期止" name="validTo">
              <a-date-picker v-model:value="form.validTo" value-format="YYYY-MM-DD" placeholder="不限" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="阶梯价" class="tier-item">
          <div v-for="(t, i) in form.tiers" :key="t.key" class="tier-row">
            <a-input-number v-model:value="t.minQty" :min="0" :precision="4" addon-before="数量 ≥" class="tier-input" />
            <a-input-number v-model:value="t.price" :min="0" :precision="4" prefix="¥" placeholder="含税单价" class="tier-input" />
            <a-button type="link" danger size="small" @click="form.tiers.splice(i, 1)">
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </div>
          <a-button v-if="(form.tiers?.length || 0) < 10" type="dashed" size="small" @click="addTier">
            <template #icon><PlusOutlined /></template>添加阶梯
          </a-button>
          <div class="sub-text">采购数量达到阶梯数量时按阶梯单价，最多 10 档</div>
        </a-form-item>
        <a-form-item name="isDefault">
          <a-checkbox v-model:checked="form.isDefault">设为该物料的默认供应商（MRP 按默认供应商生成采购订单）</a-checkbox>
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="historyOpen" :title="historyTitle" width="min(760px, 100vw)" :footer="null">
      <a-radio-group v-model:value="historyScope" size="small" class="history-scope" @change="loadHistory">
        <a-radio-button value="supplier">本供应商</a-radio-button>
        <a-radio-button value="all">全部供应商</a-radio-button>
      </a-radio-group>
      <a-table
        row-key="key"
        size="small"
        :columns="historyColumns"
        :data-source="history"
        :loading="historyLoading"
        :pagination="{ pageSize: 10, size: 'small' }"
        :locale="{ emptyText: '没有采购入库记录' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'qty'">{{ formatQty(record.qty) }}</template>
          <template v-else-if="column.key === 'price'">¥{{ formatPrice(record.price) }}</template>
          <template v-else-if="column.key === 'taxRate'">{{ Number(record.taxRate || 0) }}%</template>
        </template>
      </a-table>
    </a-modal>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { addPrice, deletePrice, pagePrice, priceHistory, priceTiers, updatePrice } from '@/api/scm/partner'
import { useTable } from '@/composables/useTable'
import { formatQty, rowKey, today } from '@/components/wms/wms'

/** 供应商的供货价格：可供应的物料、含税单价、提前期、默认供应商 */
const props = defineProps({
  open: Boolean,
  supplier: Object
})
const emit = defineEmits(['update:open'])

const columns = [
  { title: '物料', key: 'material', width: 260 },
  { title: '含税单价', key: 'price', width: 150 },
  { title: '有效期', key: 'validity', width: 190 },
  { title: '提前期', key: 'leadDays', width: 90 },
  { title: '最小起订量', key: 'minQty', width: 110, align: 'right' },
  { title: '备注', dataIndex: 'remark', width: 140, ellipsis: true },
  { title: '操作', key: 'action', width: 190 }
]

const keyword = ref('')
const query = reactive({})
const { list, loading, pagination, load, search, onTableChange } = useTable(
  (p) => pagePrice({ ...p, supplierId: props.supplier?.id, keyword: keyword.value || undefined }),
  query
)

watch(
  () => props.open && props.supplier?.id,
  (v) => {
    keyword.value = ''
    if (v) search()
  }
)

const formOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const rules = {
  materialId: [{ required: true, message: '请选择物料' }],
  price: [{ required: true, message: '请填写单价' }]
}

const formatPrice = (n) => Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 4 })
const tierText = (tiers) => tiers.map((t) => `≥ ${formatQty(t.minQty)}：¥${formatPrice(t.price)}`).join('；')
const toTier = (t) => ({ key: rowKey(), minQty: t?.minQty != null ? Number(t.minQty) : undefined, price: t?.price != null ? Number(t.price) : undefined })

function addTier() {
  form.value.tiers.push(toTier())
}

async function openForm(record) {
  form.value = record
    ? {
        ...record,
        materialLabel: `${record.materialCode} ${record.materialName}`,
        price: Number(record.price),
        taxRate: Number(record.taxRate),
        minQty: record.minQty != null ? Number(record.minQty) : undefined,
        isDefault: record.isDefault === 1,
        tiers: []
      }
    : { materialId: undefined, price: undefined, taxRate: 13, leadDays: 7, minQty: undefined, isDefault: false, remark: '', tiers: [] }
  formOpen.value = true
  if (record) {
    const tiers = await priceTiers(record.id)
    if (form.value.id === record.id) form.value.tiers = (tiers || []).map(toTier)
  }
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const f = form.value
    const data = {
      supplierId: props.supplier.id,
      materialId: f.materialId,
      price: f.price,
      taxRate: f.taxRate,
      leadDays: f.leadDays ?? 0,
      minQty: f.minQty ?? null,
      isDefault: f.isDefault ? 1 : 0,
      remark: f.remark || null,
      validFrom: f.validFrom || null,
      validTo: f.validTo || null,
      tiers: f.tiers.filter((t) => t.minQty != null || t.price != null).map((t) => ({ minQty: t.minQty, price: t.price }))
    }
    f.id ? await updatePrice(f.id, data) : await addPrice(data)
    message.success('保存成功')
    formOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

// 价格历史：采购入库的实际成交价
const historyOpen = ref(false)
const historyLoading = ref(false)
const historyScope = ref('supplier')
const historyRecord = ref()
const history = ref([])
const historyTitle = computed(() =>
  historyRecord.value ? `价格历史 - ${historyRecord.value.materialCode} ${historyRecord.value.materialName}` : '价格历史'
)
const historyColumns = [
  { title: '入库日期', dataIndex: 'bizDate', width: 110 },
  { title: '入库单号', dataIndex: 'docNo', width: 170 },
  { title: '供应商', dataIndex: 'supplierName', width: 160, ellipsis: true },
  { title: '数量', key: 'qty', width: 100, align: 'right' },
  { title: '含税单价', key: 'price', width: 120, align: 'right' },
  { title: '税率', key: 'taxRate', width: 80, align: 'right' }
]

function openHistory(record) {
  historyRecord.value = record
  historyScope.value = 'supplier'
  history.value = []
  historyOpen.value = true
  loadHistory()
}

async function loadHistory() {
  const r = historyRecord.value
  historyLoading.value = true
  try {
    const list = await priceHistory({
      materialId: r.materialId,
      supplierId: historyScope.value === 'supplier' ? props.supplier?.id : undefined
    })
    history.value = list.map((h, i) => ({ ...h, key: i }))
  } finally {
    historyLoading.value = false
  }
}

async function remove(id) {
  await deletePrice(id)
  message.success('删除成功')
  load()
}
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.search {
  max-width: 280px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
.tier-tag {
  margin-left: 4px;
}
.tier-item {
  margin-bottom: 12px;
}
.tier-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.tier-input {
  flex: 1;
}
.history-scope {
  margin-bottom: 12px;
}
</style>
