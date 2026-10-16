<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="load" @reset="resetQuery">
        <a-form-item label="物料" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="物料编码或名称" allow-clear />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <a-alert
        type="info"
        show-icon
        class="tip"
        message="预计库存 = 可用库存 + 采购在途 + 在制净额（在制产出 − 工单待领料）− 销售待发；预计库存低于再订货点（未设置时取安全库存）时建议补货，补货数量取再订货量与补足到再订货点所需数量中的较大者。"
      />
      <TableToolbar
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :exporter="exportList"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="'scm:requisition:add'" type="primary" :disabled="!selectedKeys.length" :loading="creating" @click="createRequisition">
          <template #icon><FileAddOutlined /></template>生成请购单<template v-if="selectedKeys.length">（{{ selectedKeys.length }}）</template>
        </a-button>
        <a-date-picker v-model:value="requiredDate" value-format="YYYY-MM-DD" placeholder="需求日期（选填）" />
        <span class="toolbar-switch">
          <a-switch v-model:checked="query.onlyNeeded" size="small" @change="load" /> 只看需要补货
        </span>
      </TableToolbar>

      <a-table
        row-key="materialId"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        :scroll="{ x: 1700, y: '100%' }"
        @change="(p) => Object.assign(pagination, { current: p.current, pageSize: p.pageSize })"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            <div>{{ record.code }} {{ record.name }}</div>
            <div class="sub-text">{{ record.spec }}</div>
          </template>
          <template v-else-if="column.key === 'supplyType'">
            <DictTag type="mes_supply_type" :value="record.supplyType" />
          </template>
          <template v-else-if="column.key === 'point'">
            <div>{{ formatQty(record.reorderPoint) }}</div>
            <div class="sub-text">安全库存 {{ formatQty(record.safetyStock) }}</div>
          </template>
          <template v-else-if="['available', 'poOpen', 'soOpen', 'woNet', 'reorderQty'].includes(column.key)">
            {{ formatQty(record[column.key]) }}
          </template>
          <template v-else-if="column.key === 'projected'">
            <span :class="{ short: isShort(record) }">{{ formatQty(record.projected) }}</span>
          </template>
          <template v-else-if="column.key === 'suggestQty'">
            <b v-if="Number(record.suggestQty) > 0">{{ formatQty(record.suggestQty) }}</b>
            <span v-else class="sub-text">0</span>
            <span class="sub-text"> {{ record.unit }}</span>
          </template>
          <template v-else-if="column.key === 'orderQty'">
            <span v-if="record.supplyType === 'MAKE'" class="sub-text">自制件请在生产计划 / MRP 中安排</span>
            <a-input-number
              v-else-if="selectedKeys.includes(record.materialId)"
              v-model:value="record.orderQty"
              :min="0"
              :precision="4"
              size="small"
              class="full"
            />
            <span v-else class="sub-text">勾选后可修改</span>
          </template>
          <template v-else-if="column.key === 'supplier'">
            <PartnerSelect
              v-if="selectedKeys.includes(record.materialId)"
              v-model:value="record.orderSupplierId"
              type="supplier"
              :label="record.supplierName"
              placeholder="选填"
              allow-clear
              class="full"
            />
            <template v-else>
              <div>{{ record.supplierName || '-' }}</div>
              <div v-if="record.price != null" class="sub-text">参考价 ¥{{ formatMoney(record.price) }}</div>
            </template>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { computed, h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { FileAddOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { listReorder, reorderToRequisition } from '@/api/scm/planning'
import { formatMoney } from '@/components/scm/scm'
import { formatQty } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { writeExcel } from '@/utils/excel'

const router = useRouter()
const dict = useDict('mes_supply_type')

/** 预计库存低于再订货点（未设置时取安全库存） */
const isShort = (r) => Number(r.projected) < Number(Number(r.reorderPoint) > 0 ? r.reorderPoint : r.safetyStock || 0)
/** 可以生成请购单：有建议数量且不是自制件 */
const selectable = (r) => Number(r.suggestQty) > 0 && r.supplyType !== 'MAKE'

const columns = [
  { title: '物料', key: 'material', width: 240, exportValue: (r) => `${r.code} ${r.name}` },
  { title: '规格', dataIndex: 'spec', width: 120, ellipsis: true },
  { title: '单位', dataIndex: 'unit', width: 70 },
  { title: '供应方式', key: 'supplyType', width: 90, exportValue: (r) => findDict(dict.mes_supply_type, r.supplyType)?.label ?? r.supplyType ?? '' },
  { title: '再订货点', key: 'point', width: 130, align: 'right', exportValue: (r) => r.reorderPoint },
  { title: '再订货量', key: 'reorderQty', width: 100, align: 'right' },
  { title: '可用库存', key: 'available', width: 100, align: 'right' },
  { title: '采购在途', key: 'poOpen', width: 100, align: 'right' },
  { title: '在制净额', key: 'woNet', width: 100, align: 'right' },
  { title: '销售待发', key: 'soOpen', width: 100, align: 'right' },
  { title: '预计库存', key: 'projected', width: 100, align: 'right' },
  { title: '建议补货', key: 'suggestQty', width: 120, align: 'right' },
  { title: '补货数量', key: 'orderQty', width: 170, exportValue: (r) => (selectable(r) ? r.orderQty : '') },
  { title: '供应商', key: 'supplier', width: 200, exportValue: (r) => r.supplierName || '' }
]
const { checked, visibleColumns } = useColumns(columns, 'scm-reorder')
const showSearch = ref(true)
const size = ref('middle')

const emptyQuery = () => ({ keyword: undefined, onlyNeeded: true })
const query = reactive(emptyQuery())
const list = ref([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 50,
  showSizeChanger: true,
  pageSizeOptions: ['20', '50', '100', '200'],
  showTotal: (t) => `共 ${t} 条`
})

const selectedKeys = ref([])
const rowSelection = computed(() => ({
  selectedRowKeys: selectedKeys.value,
  onChange: (keys) => (selectedKeys.value = keys),
  getCheckboxProps: (r) => ({ disabled: !selectable(r) })
}))

async function load() {
  loading.value = true
  try {
    const rows = await listReorder({ keyword: query.keyword || undefined, onlyNeeded: query.onlyNeeded })
    list.value = rows.map((r) => ({ ...r, orderQty: Number(r.suggestQty) || undefined, orderSupplierId: r.supplierId ?? undefined }))
    selectedKeys.value = []
    pagination.current = 1
  } finally {
    loading.value = false
  }
}
onMounted(load)

function resetQuery() {
  Object.assign(query, emptyQuery())
  load()
}

const requiredDate = ref()
const creating = ref(false)
async function createRequisition() {
  const rows = list.value.filter((r) => selectedKeys.value.includes(r.materialId))
  const invalid = rows.find((r) => !(Number(r.orderQty) > 0))
  if (invalid) {
    message.warning(`请填写「${invalid.code} ${invalid.name}」的补货数量`)
    return
  }
  creating.value = true
  try {
    const id = await reorderToRequisition({
      items: rows.map((r) => ({
        materialId: r.materialId,
        qty: r.orderQty,
        supplierId: r.orderSupplierId ?? null,
        requiredDate: requiredDate.value || null
      }))
    })
    load()
    Modal.confirm({
      title: '已生成请购单',
      content: h('div', `已按 ${rows.length} 个物料生成请购单（草稿），提交审批后可转采购订单。`),
      okText: '查看请购单',
      cancelText: '留在本页',
      onOk: () => router.push({ path: '/scm/requisition', query: { id } })
    })
  } finally {
    creating.value = false
  }
}

/** 列表不分页，直接按当前数据导出 */
async function exportList(cols) {
  const fields = cols.filter((c) => c.key !== 'action')
  const valueOf = (c, r) => {
    const v = c.exportValue ? c.exportValue(r) : r[c.dataIndex || c.key]
    return v === null || v === undefined ? '' : v
  }
  await writeExcel(
    '补货建议',
    fields.map((c) => ({ title: c.title, width: c.width ? Math.round(c.width / 7) : 16 })),
    list.value.map((r) => fields.map((c) => valueOf(c, r)))
  )
}
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.toolbar-switch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: 8px;
}
.short {
  color: #ff4d4f;
  font-weight: 500;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
