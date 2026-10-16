<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="reload" @reset="resetQuery">
        <a-form-item label="商机" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="编号、名称或客户" allow-clear />
        </a-form-item>
        <a-form-item v-if="view === 'list'" label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="STATUS_OPTIONS" />
        </a-form-item>
        <a-form-item v-if="view === 'list'" label="阶段" name="stage">
          <a-select v-model:value="query.stage" placeholder="全部" allow-clear :options="toOptions(dict.crm_opp_stage)" />
        </a-form-item>
        <a-form-item label="负责人" name="mine">
          <a-checkbox v-model:checked="query.mine">只看我的</a-checkbox>
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="view === 'list' ? columns : undefined" @refresh="reload">
        <a-radio-group v-model:value="view" button-style="solid" @change="reload">
          <a-radio-button value="board"><AppstoreOutlined /> 看板</a-radio-button>
          <a-radio-button value="list"><UnorderedListOutlined /> 列表</a-radio-button>
        </a-radio-group>
        <a-button v-perm="'crm:opp:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>新增商机
        </a-button>
      </TableToolbar>

      <!-- 看板：进行中的各阶段 -->
      <div v-if="view === 'board'" class="board-wrap">
        <a-spin :spinning="boardLoading">
          <div class="board-summary">
            <span>进行中 <b>{{ boardList.length }}</b> 个，金额 <b>¥{{ formatMoney(sum(boardList, 'amount')) }}</b></span>
            <span>加权金额 <b>¥{{ formatMoney(weighted) }}</b></span>
            <span class="sub-text">赢单、输单的商机在「列表」中查看</span>
          </div>
          <div class="board">
            <div v-for="stage in OPEN_STAGES" :key="stage" class="column">
              <div class="column-head">
                <DictTag type="crm_opp_stage" :value="stage" />
                <span class="sub-text">{{ byStage(stage).length }} 个 · ¥{{ formatMoney(sum(byStage(stage), 'amount')) }}</span>
              </div>
              <div class="column-body">
                <div v-for="o in byStage(stage)" :key="o.id" class="opp-card" @click="openDetail(o)">
                  <div class="opp-name">{{ o.name }}</div>
                  <div class="sub-text">{{ o.customerName }}</div>
                  <div class="opp-amount">¥{{ formatMoney(o.amount) }}</div>
                  <div class="opp-foot">
                    <span :class="{ danger: isOverdue(o) }">{{ o.expectedDate ? `预计 ${o.expectedDate}` : '未定成交日期' }}</span>
                    <span>{{ o.ownerName }} · {{ o.probability }}%</span>
                  </div>
                </div>
                <a-empty v-if="!byStage(stage).length" :image="simpleImage" description="暂无商机" />
              </div>
            </div>
          </div>
        </a-spin>
      </div>

      <a-table
        v-else
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1350, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="openDetail(record)">{{ record.name }}</a>
            <div class="sub-text">{{ record.oppNo }}</div>
          </template>
          <template v-else-if="column.key === 'customer'">
            <div>{{ record.customerName }}</div>
            <div v-if="record.contactName" class="sub-text">{{ record.contactName }}</div>
          </template>
          <template v-else-if="column.key === 'stage'">
            <DictTag type="crm_opp_stage" :value="record.stage" />
            <div class="sub-text">{{ record.probability }}%</div>
          </template>
          <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
          <template v-else-if="column.key === 'expectedDate'">
            <span :class="{ danger: isOverdue(record) }">{{ record.expectedDate || '-' }}</span>
            <div v-if="record.closeDate" class="sub-text">成交 / 输单 {{ record.closeDate }}</div>
          </template>
          <template v-else-if="column.key === 'source'">
            <DictTag v-if="record.source" type="crm_source" :value="record.source" />
            <div v-if="record.campaignName" class="sub-text">{{ record.campaignName }}</div>
          </template>
          <template v-else-if="column.key === 'order'">
            <span v-if="record.salesOrderNo">{{ record.salesOrderNo }}</span>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">
                <template #icon><EyeOutlined /></template>详情
              </a-button>
              <a-button v-if="isOpen(record)" v-perm="'crm:opp:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <span v-if="!record.salesOrderId" v-perm="'crm:opp:remove'">
                <a-popconfirm :title="`确定删除商机「${record.name}」？`" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer page :drawer="drawer" create-title="新增商机" edit-title="编辑商机" width="min(960px, 100vw)">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="商机名称" name="name">
              <a-input v-model:value="form.name" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="客户" name="customerId">
              <PartnerSelect v-model:value="form.customerId" type="customer" :label="form.customerName" @update:value="onCustomer" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="联系人" name="contactId">
              <a-select v-model:value="form.contactId" allow-clear :options="contactOpts" placeholder="先选择客户" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="负责人" name="ownerId" extra="不选择时为你本人">
              <IdentityUserSelect v-model:value="form.ownerId" :label="form.ownerName" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="预计成交日期" name="expectedDate">
              <a-date-picker v-model:value="form.expectedDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col v-if="!drawer.isEdit.value" :xs="24" :sm="8">
            <a-form-item label="销售阶段" name="stage">
              <a-select v-model:value="form.stage" :options="openStageOptions" @change="(s) => (form.probability = STAGE_PROBABILITY[s])" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="赢单概率" name="probability">
              <a-input-number v-model:value="form.probability" :min="0" :max="100" addon-after="%" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="预计金额" name="amount" :extra="form.items.length ? '按产品明细自动合计' : ''">
              <a-input-number
                :value="form.items.length ? itemsTotal : form.amount"
                :disabled="form.items.length > 0"
                :min="0"
                :precision="2"
                prefix="¥"
                class="full"
                @update:value="(v) => (form.amount = v)"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="来源" name="source">
              <a-select v-model:value="form.source" allow-clear :options="toOptions(dict.crm_source)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="市场活动" name="campaignId">
              <a-select v-model:value="form.campaignId" allow-clear show-search option-filter-prop="label" :options="campaignOpts" />
            </a-form-item>
          </a-col>
        </a-row>

        <div class="items-head">
          <span>产品明细</span>
          <a-button size="small" @click="addItem"><template #icon><PlusOutlined /></template>添加产品</a-button>
        </div>
        <a-table row-key="key" size="small" :columns="itemColumns" :data-source="form.items" :pagination="false" :scroll="{ x: 800 }" class="items">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'material'">
              <a-form-item :name="['items', index, 'materialId']" :rules="[{ required: true, message: '请选择产品' }]" class="cell">
                <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'qty'">
              <a-form-item :name="['items', index, 'qty']" :rules="[{ required: true, message: '请填写数量' }]" class="cell">
                <a-input-number v-model:value="record.qty" :min="0.0001" :precision="4" class="full" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'price'">
              <a-form-item :name="['items', index, 'price']" :rules="[{ required: true, message: '请填写单价' }]" class="cell">
                <a-input-number v-model:value="record.price" :min="0" :precision="4" prefix="¥" class="full" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'taxRate'">
              <a-input-number v-model:value="record.taxRate" :min="0" :max="100" addon-after="%" class="full" />
            </template>
            <template v-else-if="column.key === 'amount'">¥{{ formatMoney(lineAmount(record.qty, record.price)) }}</template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" danger size="small" @click="form.items.splice(index, 1)"><DeleteOutlined /></a-button>
            </template>
          </template>
        </a-table>

        <a-form-item label="备注" name="remark" class="remark">
          <a-textarea v-model:value="form.remark" :rows="2" :maxlength="500" show-count />
        </a-form-item>
      </a-form>
    </FormDrawer>

    <OpportunityDetail v-model:open="detailOpen" :opportunity-id="detailId" @changed="reload" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { Empty, message } from 'ant-design-vue'
import { AppstoreOutlined, DeleteOutlined, EditOutlined, EyeOutlined, PlusOutlined, UnorderedListOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import OpportunityDetail from '@/components/crm/OpportunityDetail.vue'
import { addOpportunity, campaignOptions, deleteOpportunity, getOpportunity, listContacts, pageOpportunity, updateOpportunity } from '@/api/crm'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { formatMoney, lineAmount } from '@/components/scm/scm'
import { rowKey } from '@/components/wms/wms'
import { OPEN_STAGES, STAGE_PROBABILITY, sum } from '@/components/crm/crm'

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const dict = useDict('crm_opp_stage', 'crm_source')
const STATUS_OPTIONS = [
  { value: 'OPEN', label: '进行中' },
  { value: 'WON', label: '赢单' },
  { value: 'LOST', label: '输单' }
]
const openStageOptions = computed(() => toOptions(dict.crm_opp_stage).filter((o) => OPEN_STAGES.includes(o.value)))

const columns = [
  { title: '商机', key: 'name', width: 220 },
  { title: '客户', key: 'customer', width: 200 },
  { title: '阶段', key: 'stage', width: 110, dict: 'crm_opp_stage' },
  { title: '预计金额', key: 'amount', width: 140, align: 'right' },
  { title: '预计成交', key: 'expectedDate', width: 160 },
  { title: '负责人', dataIndex: 'ownerName', width: 100 },
  { title: '来源', key: 'source', width: 140, dict: 'crm_source' },
  { title: '销售订单', key: 'order', width: 150 },
  { title: '操作', key: 'action', width: 190, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'crm-opportunity')
const showSearch = ref(true)
const view = ref('board')

const emptyQuery = () => ({ keyword: undefined, status: undefined, stage: undefined, mine: false })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageOpportunity, query)

// 看板：进行中的商机（最多 500 个）
const boardList = ref([])
const boardLoading = ref(false)
async function loadBoard() {
  boardLoading.value = true
  try {
    const res = await pageOpportunity({ current: 1, size: 500, status: 'OPEN', keyword: query.keyword, mine: query.mine })
    boardList.value = res.records
  } finally {
    boardLoading.value = false
  }
}
const byStage = (stage) => boardList.value.filter((o) => o.stage === stage)
const weighted = computed(() => boardList.value.reduce((s, o) => s + (Number(o.amount) * o.probability) / 100, 0))

function reload() {
  view.value === 'board' ? loadBoard() : search()
}
function resetQuery() {
  Object.assign(query, emptyQuery())
  reload()
}

const isOpen = (r) => r.stage !== 'WON' && r.stage !== 'LOST'
const isOverdue = (r) => isOpen(r) && r.expectedDate && dayjs(r.expectedDate).isBefore(dayjs(), 'day')

const campaignOpts = ref([])
onMounted(async () => {
  reload()
  campaignOpts.value = (await campaignOptions()).map((c) => ({ value: c.id, label: c.name }))
})

// ---------------------------------------------------------------- 新增 / 编辑

const contactOpts = ref([])
async function loadContacts(customerId) {
  contactOpts.value = customerId ? (await listContacts(customerId)).map((c) => ({ value: c.id, label: c.title ? `${c.name}（${c.title}）` : c.name })) : []
}
function onCustomer(id) {
  form.value.contactId = undefined
  loadContacts(id)
}

const newItem = () => ({ key: rowKey(), materialId: undefined, qty: 1, price: undefined, taxRate: 13 })
function addItem() {
  form.value.items.push(newItem())
}
const itemsTotal = computed(() => form.value.items.reduce((s, i) => s + lineAmount(i.qty, i.price), 0))

const itemColumns = [
  { title: '产品', key: 'material', width: 260 },
  { title: '数量', key: 'qty', width: 120 },
  { title: '含税单价', key: 'price', width: 140 },
  { title: '税率', key: 'taxRate', width: 110 },
  { title: '金额', key: 'amount', width: 120, align: 'right' },
  { title: '', key: 'action', width: 50 }
]

const rules = {
  name: [{ required: true, whitespace: true, message: '请输入商机名称' }],
  customerId: [{ required: true, message: '请选择客户' }]
}
const FIELDS = ['name', 'customerId', 'contactId', 'amount', 'stage', 'probability', 'expectedDate', 'source', 'campaignId', 'ownerId', 'remark']
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    ...Object.fromEntries(FIELDS.map((f) => [f, undefined])),
    stage: 'INITIAL',
    probability: 10,
    customerName: undefined,
    ownerName: undefined,
    items: []
  }),
  beforeOpen: () => {
    contactOpts.value = []
  },
  fetch: async (record) => {
    const d = await getOpportunity(record.id)
    loadContacts(d.customerId)
    return {
      ...d,
      items: d.items.map((i) => ({
        key: rowKey(),
        materialId: i.materialId,
        materialLabel: `${i.materialCode} ${i.materialName}`,
        qty: Number(i.qty),
        price: Number(i.price),
        taxRate: Number(i.taxRate)
      }))
    }
  },
  create: addOpportunity,
  update: updateOpportunity,
  toPayload: (f) => ({
    ...Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
    items: f.items.map((i) => ({ materialId: i.materialId, qty: i.qty, price: i.price, taxRate: i.taxRate }))
  }),
  onSuccess: reload
})
const { formRef, form } = drawer

async function remove(id) {
  await deleteOpportunity(id)
  message.success('删除成功')
  reload()
}

const detailOpen = ref(false)
const detailId = ref()
function openDetail(record) {
  detailId.value = record.id
  detailOpen.value = true
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.danger {
  color: #f5222d;
}
.full {
  width: 100%;
}
.board-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
}
.board-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  align-items: center;
  margin-bottom: 12px;
}
.board {
  display: grid;
  grid-template-columns: repeat(4, minmax(240px, 1fr));
  gap: 12px;
  overflow-x: auto;
}
.column {
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  background: var(--app-hover);
  min-height: 300px;
}
.column-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
}
.column-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0 8px 8px;
}
.opp-card {
  padding: 10px 12px;
  border-radius: 6px;
  background: var(--app-card);
  box-shadow: var(--app-shadow);
  cursor: pointer;
  border: 1px solid var(--app-border);
  transition: border-color 0.2s;
}
.opp-card:hover {
  border-color: var(--app-primary);
}
.opp-name {
  font-weight: 500;
}
.opp-amount {
  margin: 6px 0;
  font-size: 16px;
  font-weight: 600;
}
.opp-foot {
  display: flex;
  justify-content: space-between;
  color: var(--app-text-3);
  font-size: 12px;
}
.opp-foot .danger {
  color: #f5222d;
}
.items-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 4px 0 8px;
  font-weight: 500;
}
.items :deep(.ant-table-cell) {
  vertical-align: top;
}
.cell {
  margin-bottom: 0;
}
.remark {
  margin-top: 16px;
}
</style>
