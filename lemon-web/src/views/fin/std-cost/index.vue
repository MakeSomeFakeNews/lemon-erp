<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <span class="sub-text">期间 P 的成本差异使用生效期间不晚于 P 的最近一个已发布版本</span>
        <template #extra>
          <a-button v-perm="'fin:stdCost:edit'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>新建版本
          </a-button>
        </template>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 980, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="openDetail(record.id)">{{ record.name }}</a>
            <div v-if="record.remark" class="sub-text">{{ record.remark }}</div>
          </template>
          <template v-else-if="column.key === 'period'">{{ periodText(record.effectivePeriod) }}</template>
          <template v-else-if="column.key === 'count'">
            {{ record.itemCount }}
            <div class="sub-text">自制 {{ record.makeCount }} · 采购 {{ record.itemCount - record.makeCount }}</div>
          </template>
          <template v-else-if="column.key === 'zero'">
            <a-tag v-if="record.zeroCount" color="warning">{{ record.zeroCount }} 个采购件单价为 0</a-tag>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="fin_std_cost_status" :value="record.status" />
            <a-tag v-if="record.status === 'DRAFT' && !record.rollupTime" color="orange">需重新卷算</a-tag>
            <div v-if="record.releaseTime" class="sub-text">{{ record.releaseTime }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">查看</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新建标准成本版本" edit-title="编辑标准成本版本">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="版本名称" name="name">
          <a-input v-model:value="form.name" :maxlength="64" placeholder="如：2026 年标准成本" />
        </a-form-item>
        <a-form-item label="生效期间" name="effectivePeriod">
          <PeriodPicker v-model:value="form.effectivePeriod" class="full" />
        </a-form-item>
        <a-form-item v-if="!form.id" label="复制版本" name="copyFromId" extra="复制采购件的标准单价；不选时取最近一次成本核算的单价（没有时取参考成本价）">
          <a-select v-model:value="form.copyFromId" allow-clear placeholder="不复制" :options="versionOptions" />
        </a-form-item>
        <a-form-item label="说明" name="remark">
          <a-textarea v-model:value="form.remark" :rows="3" :maxlength="255" />
        </a-form-item>
      </a-form>
      <a-alert v-if="!form.id" type="info" show-icon message="保存后按当前的默认 BOM 和工艺路线卷算自制件的标准成本" />
    </FormDrawer>

    <SubPage :open="detailOpen" :title="detail ? `标准成本 ${detail.name}` : ''" width="min(1100px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space v-if="detail.status === 'DRAFT'" v-perm="'fin:stdCost:edit'">
          <a-popconfirm title="删除该版本？" @confirm="remove"><a-button danger>删除</a-button></a-popconfirm>
          <a-button @click="drawer.openEdit(detail)">编辑</a-button>
          <a-button :loading="rolling" @click="rollup">卷算</a-button>
          <a-popconfirm title="发布后不能再修改，差异分析从生效期间开始使用该版本，确定？" @confirm="release">
            <a-button type="primary">发布</a-button>
          </a-popconfirm>
        </a-space>
        <span v-else v-perm="'fin:stdCost:edit'">
          <a-popconfirm title="取消发布后可以修改，差异分析改用更早的版本，确定？" @confirm="unrelease">
            <a-button>取消发布</a-button>
          </a-popconfirm>
        </span>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item label="生效期间">{{ periodText(detail.effectivePeriod) }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <DictTag type="fin_std_cost_status" :value="detail.status" />
            <span v-if="detail.releaseTime" class="sub-text">{{ detail.releaseTime }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="卷算">
            <span v-if="detail.rollupTime">{{ detail.rollupTime }}</span>
            <a-tag v-else color="orange">修改了单价，需重新卷算</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="物料">自制 {{ detail.makeCount }}，采购 {{ detail.itemCount - detail.makeCount }}</a-descriptions-item>
          <a-descriptions-item label="单价为 0 的采购件">
            <a v-if="detail.zeroCount" class="warn" @click="filterZero">{{ detail.zeroCount }} 个</a>
            <span v-else>无</span>
          </a-descriptions-item>
          <a-descriptions-item label="创建">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="说明" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <div class="toolbar">
          <a-space wrap>
            <a-radio-group v-model:value="itemQuery.make" button-style="solid" @change="items.search">
              <a-radio-button value="">全部</a-radio-button>
              <a-radio-button :value="1">自制件</a-radio-button>
              <a-radio-button :value="0">采购件</a-radio-button>
            </a-radio-group>
            <a-input-search v-model:value="itemQuery.keyword" placeholder="物料编码或名称" allow-clear class="filter-wide" @search="items.search" />
          </a-space>
          <a-button v-if="editable" type="primary" ghost @click="openPrice()">
            <template #icon><PlusOutlined /></template>添加采购件
          </a-button>
        </div>
        <a-table
          row-key="id"
          size="small"
          :columns="itemColumns"
          :data-source="items.list.value"
          :loading="items.loading.value"
          :pagination="items.pagination"
          :scroll="{ x: 1000 }"
          @change="items.onTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">
              {{ record.materialCode }} {{ record.materialName }}
              <div class="sub-text">{{ record.spec }} {{ record.unit }}</div>
            </template>
            <template v-else-if="column.key === 'make'">
              <a-tag v-if="record.make" color="blue">自制</a-tag>
              <a-tag v-else>采购</a-tag>
              <div v-if="record.bomVersion" class="sub-text">BOM {{ record.bomVersion }}</div>
            </template>
            <template v-else-if="column.key === 'unitCost'">
              <b :class="{ warn: !record.make && Number(record.unitCost) === 0 }">{{ fmt(record.unitCost) }}</b>
            </template>
            <template v-else-if="column.key === 'stdHours'">{{ record.make ? fmt(record.stdHours) : '' }}</template>
            <template v-else-if="column.num">{{ record.make ? fmt(record[column.key]) : column.key === 'materialCost' ? fmt(record.materialCost) : '' }}</template>
            <template v-else-if="column.key === 'action'">
              <span class="table-actions">
                <a-button v-if="record.make" type="link" size="small" @click="openBreakdown(record)">明细</a-button>
                <template v-else-if="editable">
                  <a-button type="link" size="small" @click="openPrice(record)">改价</a-button>
                  <a-popconfirm title="从版本中删除该采购件？" @confirm="removePrice(record)">
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </template>
              </span>
            </template>
          </template>
        </a-table>
      </template>
    </SubPage>

    <a-modal v-model:open="priceOpen" :title="priceForm.label ? `标准单价 - ${priceForm.label}` : '添加采购件'" @ok="submitPrice">
      <a-form layout="vertical">
        <a-form-item v-if="!priceForm.label" label="物料" required>
          <MaterialSelect v-model:value="priceForm.materialId" />
        </a-form-item>
        <a-form-item label="标准单价" required extra="修改后须重新卷算自制件">
          <a-input-number v-model:value="priceForm.price" :min="0" :precision="4" prefix="¥" class="full" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="breakdownOpen" :title="breakdownTitle" width="min(900px, 100vw)" :footer="null">
      <template v-if="breakdown">
        <div class="section-title">材料 {{ fmt(breakdown.row.materialCost) }}</div>
        <a-table row-key="materialId" size="small" :columns="partColumns" :data-source="breakdown.items" :pagination="false" :scroll="{ x: 720 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">
              {{ record.materialCode }} {{ record.materialName }}
              <a-tag v-if="record.make" color="blue" class="ml">自制</a-tag>
            </template>
            <template v-else-if="column.key === 'qty'">
              {{ fmt(record.unitQty) }} {{ record.unit }}
              <div v-if="Number(record.lossRate)" class="sub-text">{{ fmt(record.qty) }} × (1 + {{ Number(record.lossRate) }}%)</div>
            </template>
            <template v-else-if="column.key === 'unitCost'">{{ fmt(record.unitCost) }}</template>
            <template v-else-if="column.key === 'amount'">{{ fmt(record.amount) }}</template>
          </template>
        </a-table>
        <div class="section-title">人工 {{ fmt(breakdown.row.laborCost) }}、制造费用 {{ fmt(breakdown.row.overheadCost) }}</div>
        <a-table row-key="seq" size="small" :columns="opColumns" :data-source="breakdown.ops" :pagination="false" :scroll="{ x: 720 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'op'">{{ record.seq }} {{ record.name }}<div class="sub-text">{{ record.workCenterName }}</div></template>
            <template v-else-if="column.fmt">{{ fmt(record[column.key]) }}</template>
          </template>
        </a-table>
        <div class="sub-text tip">按卷算时的 BOM 和当前的工作中心费率显示；修改 BOM、费率后请重新卷算</div>
      </template>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import {
  addStdCost, deleteStdCost, getStdCost, pageStdCost, releaseStdCost, removeStdPrice, rollupStdCost, saveStdPrice, stdCostBreakdown,
  stdCostItems, unreleaseStdCost, updateStdCost
} from '@/api/fin'
import { currentPeriod, periodText } from '@/components/fin/fin'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'

/** 标准成本：按版本维护，采购件手工维护标准单价，自制件按默认 BOM 和工艺路线逐级卷算；发布后用于成本差异分析 */
useDict('fin_std_cost_status')
const userStore = useUserStore()

const fmt = (n) => (n == null ? '' : Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 4 }))

const columns = [
  { title: '版本', key: 'name', width: 240 },
  { title: '生效期间', key: 'period', width: 120 },
  { title: '物料数', key: 'count', width: 150 },
  { title: '待补单价', key: 'zero', width: 170 },
  { title: '状态', key: 'status', width: 200 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'fin-std-cost')
const { list, loading, size, pagination, load, onTableChange } = useTable(pageStdCost, reactive({}))
onMounted(load)

const versionOptions = computed(() => list.value.map((v) => ({ value: v.id, label: `${v.name}（${periodText(v.effectivePeriod)}）` })))
const rules = {
  name: [{ required: true, whitespace: true, message: '请填写版本名称' }],
  effectivePeriod: [{ required: true, message: '请选择生效期间' }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, name: '', effectivePeriod: currentPeriod(), copyFromId: undefined, remark: '' }),
  create: async (data) => {
    const id = await addStdCost(data)
    openDetail(id)
  },
  update: updateStdCost,
  toPayload: (f) => ({ name: f.name, effectivePeriod: f.effectivePeriod, copyFromId: f.id ? null : f.copyFromId ?? null, remark: f.remark || null }),
  onSuccess: () => {
    load()
    if (detailOpen.value) refreshDetail()
  }
})
const { formRef, form } = drawer

// ---------------------------------------------------------------- 详情
const itemColumns = [
  { title: '物料', key: 'material', width: 260 },
  { title: '类型', key: 'make', width: 100 },
  { title: '材料', key: 'materialCost', num: true, width: 120, align: 'right' },
  { title: '人工', key: 'laborCost', num: true, width: 110, align: 'right' },
  { title: '制造费用', key: 'overheadCost', num: true, width: 110, align: 'right' },
  { title: '单位标准成本', key: 'unitCost', width: 130, align: 'right' },
  { title: '标准工时', key: 'stdHours', width: 100, align: 'right' },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
]
const detailOpen = ref(false)
const detail = ref()
const editable = computed(() => detail.value?.status === 'DRAFT' && userStore.hasPerm('fin:stdCost:edit'))
const itemQuery = reactive({ make: '', keyword: undefined })
const items = useTable((p) => stdCostItems(detail.value.id, p), itemQuery, { params: (q) => ({ ...q, make: q.make === '' ? undefined : q.make }) })
async function openDetail(id) {
  detail.value = await getStdCost(id)
  Object.assign(itemQuery, { make: '', keyword: undefined })
  detailOpen.value = true
  items.search()
}
async function refreshDetail() {
  detail.value = await getStdCost(detail.value.id)
  items.load()
}
function filterZero() {
  Object.assign(itemQuery, { make: 0, keyword: undefined })
  items.search()
}
const rolling = ref(false)
async function rollup() {
  rolling.value = true
  try {
    const zero = await rollupStdCost(detail.value.id)
    if (zero) message.warning(`卷算完成，有 ${zero} 个采购件单价为 0，请补录后重新卷算`)
    else message.success('卷算完成')
    refreshDetail()
    load()
  } finally {
    rolling.value = false
  }
}
async function release() {
  await releaseStdCost(detail.value.id)
  message.success('已发布')
  refreshDetail()
  load()
}
async function unrelease() {
  await unreleaseStdCost(detail.value.id)
  message.success('已取消发布')
  refreshDetail()
  load()
}
async function remove() {
  await deleteStdCost(detail.value.id)
  message.success('已删除')
  detailOpen.value = false
  load()
}

const priceOpen = ref(false)
const priceForm = reactive({ materialId: undefined, label: '', price: undefined })
function openPrice(record) {
  Object.assign(priceForm, record
    ? { materialId: record.materialId, label: `${record.materialCode} ${record.materialName}`, price: Number(record.materialCost) }
    : { materialId: undefined, label: '', price: undefined })
  priceOpen.value = true
}
async function submitPrice() {
  if (!priceForm.materialId) return message.warning('请选择物料')
  if (priceForm.price == null) return message.warning('请填写标准单价')
  await saveStdPrice(detail.value.id, { materialId: priceForm.materialId, price: priceForm.price })
  message.success('已保存，请重新卷算')
  priceOpen.value = false
  refreshDetail()
  load()
}
async function removePrice(record) {
  await removeStdPrice(detail.value.id, record.materialId)
  message.success('已删除')
  refreshDetail()
  load()
}

const partColumns = [
  { title: '子件', key: 'material' },
  { title: '单位用量（含损耗）', key: 'qty', width: 170, align: 'right' },
  { title: '标准成本', key: 'unitCost', width: 120, align: 'right' },
  { title: '金额', key: 'amount', width: 120, align: 'right' }
]
const opColumns = [
  { title: '工序', key: 'op' },
  { title: '标准工时', key: 'stdHours', fmt: true, width: 100, align: 'right' },
  { title: '人工费率', key: 'laborRate', fmt: true, width: 100, align: 'right' },
  { title: '人工', key: 'labor', fmt: true, width: 100, align: 'right' },
  { title: '制造费用率', key: 'overheadRate', fmt: true, width: 110, align: 'right' },
  { title: '制造费用', key: 'overhead', fmt: true, width: 100, align: 'right' }
]
const breakdownOpen = ref(false)
const breakdown = ref()
const breakdownTitle = ref('')
async function openBreakdown(record) {
  breakdown.value = await stdCostBreakdown(detail.value.id, record.materialId)
  breakdownTitle.value = `卷算明细 - ${record.materialCode} ${record.materialName}：${fmt(record.unitCost)}`
  breakdownOpen.value = true
}
</script>

<style scoped>
.filter-wide {
  width: 220px;
}
.full {
  width: 100%;
}
.block {
  margin-bottom: 16px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.section-title {
  margin: 12px 0 8px;
  font-weight: 600;
}
.warn {
  color: var(--ant-color-warning, #faad14);
}
.ml {
  margin-left: 6px;
}
.tip {
  margin-top: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
