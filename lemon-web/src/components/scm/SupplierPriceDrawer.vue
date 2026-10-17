<template>
  <SubPage
    :open="open"
    :title="supplier ? $t('scm.supplierPrice.titleOf', { name: supplier.name }) : $t('scm.supplierPrice.title')"
    width="min(960px, 100vw)"
    destroy-on-close
    @close="emit('update:open', false)"
  >
    <div class="toolbar">
      <a-input-search v-model:value="keyword" :placeholder="$t('scm.supplierPrice.materialKeyword')" allow-clear class="search" @search="search" />
      <a-button v-perm="'scm:supplier:edit'" type="primary" @click="openForm()">
        <template #icon><PlusOutlined /></template>{{ $t('scm.supplierPrice.addMaterial') }}
      </a-button>
    </div>
    <a-table row-key="id" size="small" :columns="columns" :data-source="list" :loading="loading" :pagination="pagination" @change="onTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'material'">
          <div>
            {{ record.materialCode }} {{ record.materialName }}
            <a-tag v-if="record.isDefault === 1" color="gold" :bordered="false">{{ $t('scm.supplierPrice.default') }}</a-tag>
          </div>
          <div class="sub-text">{{ record.spec }}</div>
        </template>
        <template v-else-if="column.key === 'price'">
          <div>¥{{ Number(record.price).toLocaleString('zh-CN', { maximumFractionDigits: 4 }) }} / {{ record.unit || $t('common.field.unit') }}</div>
          <div class="sub-text">
            {{ $t('scm.supplierPrice.taxRateOf', { rate: Number(record.taxRate) }) }}
            <a-tooltip v-if="record.tiers?.length" :title="tierText(record.tiers)">
              <a-tag color="blue" :bordered="false" class="tier-tag">{{ $t('scm.supplierPrice.tierPrice') }}</a-tag>
            </a-tooltip>
          </div>
        </template>
        <template v-else-if="column.key === 'validity'">
          <template v-if="record.validFrom || record.validTo">
            <div>{{ record.validFrom || $t('scm.supplierPrice.unlimited') }} ~ {{ record.validTo || $t('scm.supplierPrice.unlimited') }}</div>
            <a-tag v-if="record.validTo && record.validTo < today()" color="red" :bordered="false">{{ $t('scm.supplierPrice.expired') }}</a-tag>
            <a-tag v-else-if="record.validFrom && record.validFrom > today()" color="orange" :bordered="false">{{ $t('scm.supplierPrice.notEffective') }}</a-tag>
          </template>
          <span v-else class="sub-text">{{ $t('scm.supplierPrice.longTerm') }}</span>
        </template>
        <template v-else-if="column.key === 'leadDays'">{{ $t('scm.supplierPrice.days', { n: record.leadDays }) }}</template>
        <template v-else-if="column.key === 'minQty'">{{ formatQty(record.minQty) }}</template>
        <template v-else-if="column.key === 'action'">
          <span class="table-actions">
            <a-button type="link" size="small" @click="openHistory(record)">{{ $t('scm.supplierPrice.history') }}</a-button>
          </span>
          <span v-perm="'scm:supplier:edit'" class="table-actions">
            <a-button type="link" size="small" @click="openForm(record)">{{ $t('common.action.edit') }}</a-button>
            <a-popconfirm :title="$t('scm.supplierPrice.confirmDelete')" @confirm="remove(record.id)">
              <a-button type="link" danger size="small">{{ $t('common.action.delete') }}</a-button>
            </a-popconfirm>
          </span>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="formOpen" :title="form.id ? $t('scm.supplierPrice.editTitle') : $t('scm.supplierPrice.addTitle')" :confirm-loading="saving" @ok="submit">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('scm.supplierPrice.material')" name="materialId">
          <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" :disabled="!!form.id" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('scm.supplierPrice.taxPrice')" name="price">
              <a-input-number v-model:value="form.price" :min="0" :precision="4" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('scm.supplierPrice.taxRate')" name="taxRate">
              <a-input-number v-model:value="form.taxRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('scm.supplierPrice.leadDays')" name="leadDays">
              <a-input-number v-model:value="form.leadDays" :min="0" :max="365" :addon-after="$t('scm.supplierPrice.day')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('scm.supplierPrice.minQty')" name="minQty">
              <a-input-number v-model:value="form.minQty" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('scm.supplierPrice.validFrom')" name="validFrom">
              <a-date-picker v-model:value="form.validFrom" value-format="YYYY-MM-DD" :placeholder="$t('scm.supplierPrice.unlimited')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('scm.supplierPrice.validTo')" name="validTo">
              <a-date-picker v-model:value="form.validTo" value-format="YYYY-MM-DD" :placeholder="$t('scm.supplierPrice.unlimited')" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('scm.supplierPrice.tierPrice')" class="tier-item">
          <div v-for="(t, i) in form.tiers" :key="t.key" class="tier-row">
            <a-input-number v-model:value="t.minQty" :min="0" :precision="4" :addon-before="$t('scm.supplierPrice.qtyGte')" class="tier-input" />
            <a-input-number v-model:value="t.price" :min="0" :precision="4" prefix="¥" :placeholder="$t('scm.supplierPrice.taxPrice')" class="tier-input" />
            <a-button type="link" danger size="small" @click="form.tiers.splice(i, 1)">
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </div>
          <a-button v-if="(form.tiers?.length || 0) < 10" type="dashed" size="small" @click="addTier">
            <template #icon><PlusOutlined /></template>{{ $t('scm.supplierPrice.addTier') }}
          </a-button>
          <div class="sub-text">{{ $t('scm.supplierPrice.tierTip') }}</div>
        </a-form-item>
        <a-form-item name="isDefault">
          <a-checkbox v-model:checked="form.isDefault">{{ $t('scm.supplierPrice.setDefault') }}</a-checkbox>
        </a-form-item>
        <a-form-item :label="$t('common.field.remark')" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="historyOpen" :title="historyTitle" width="min(760px, 100vw)" :footer="null">
      <a-radio-group v-model:value="historyScope" size="small" class="history-scope" @change="loadHistory">
        <a-radio-button value="supplier">{{ $t('scm.supplierPrice.thisSupplier') }}</a-radio-button>
        <a-radio-button value="all">{{ $t('scm.supplierPrice.allSuppliers') }}</a-radio-button>
      </a-radio-group>
      <a-table
        row-key="key"
        size="small"
        :columns="historyColumns"
        :data-source="history"
        :loading="historyLoading"
        :pagination="{ pageSize: 10, size: 'small' }"
        :locale="{ emptyText: $t('scm.supplierPrice.noHistory') }"
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
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { addPrice, deletePrice, pagePrice, priceHistory, priceTiers, updatePrice } from '@/api/scm/partner'
import { useTable } from '@/composables/useTable'
import { formatQty, rowKey, today } from '@/components/wms/wms'
import { price as formatPrice } from '@/utils/number'
import { useAction } from '@/composables/useAction'

/** 供应商的供货价格：可供应的物料、含税单价、提前期、默认供应商 */
const props = defineProps({
  open: Boolean,
  supplier: Object
})
const emit = defineEmits(['update:open'])
const { t } = useI18n()

const columns = [
  { title: t('scm.supplierPrice.material'), key: 'material', width: 260 },
  { title: t('scm.supplierPrice.taxPrice'), key: 'price', width: 160 },
  { title: t('scm.supplierPrice.validity'), key: 'validity', width: 190 },
  { title: t('scm.supplierPrice.leadTime'), key: 'leadDays', width: 100 },
  { title: t('scm.supplierPrice.minQty'), key: 'minQty', width: 120, align: 'right' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 140, ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 220 }
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
  materialId: [{ required: true, message: t('scm.supplierPrice.materialRequired') }],
  price: [{ required: true, message: t('scm.supplierPrice.priceRequired') }]
}

const tierText = (tiers) => tiers.map((t) => `≥ ${formatQty(t.minQty)}：¥${formatPrice(t.price)}`).join(t('scm.supplierPrice.sep'))
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
    message.success(t('common.msg.saveSuccess'))
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
  historyRecord.value ? t('scm.supplierPrice.historyOf', { name: `${historyRecord.value.materialCode} ${historyRecord.value.materialName}` }) : t('scm.supplierPrice.history')
)
const historyColumns = [
  { title: t('scm.supplierPrice.inDate'), dataIndex: 'bizDate', width: 120 },
  { title: t('scm.supplierPrice.inDocNo'), dataIndex: 'docNo', width: 170 },
  { title: t('scm.supplierPrice.supplier'), dataIndex: 'supplierName', width: 160, ellipsis: true },
  { title: t('common.field.quantity'), key: 'qty', width: 100, align: 'right' },
  { title: t('scm.supplierPrice.taxPrice'), key: 'price', width: 130, align: 'right' },
  { title: t('scm.supplierPrice.taxRate'), key: 'taxRate', width: 80, align: 'right' }
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

const remove = useAction(deletePrice, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
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
