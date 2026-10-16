<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" placeholder="全部状态" allow-clear :options="toOptions(dict.scm_tender_status)" class="filter" @change="search" />
        <a-input-search v-model:value="query.keyword" placeholder="招标编号、标题" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <a-button v-perm="'scm:tender:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>新增招标
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
        :scroll="{ x: 1100, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'tenderNo'">
            <a @click="openDetail(record.id)">{{ record.tenderNo }}</a>
            <div class="sub-text">{{ record.title }}</div>
          </template>
          <template v-else-if="column.key === 'deadline'">
            <div>{{ record.deadline }}</div>
            <div v-if="record.status === 'BIDDING'" :class="['sub-text', { danger: passed(record) }]">{{ passed(record) ? '已截止，可开标' : remain(record) }}</div>
          </template>
          <template v-else-if="column.key === 'bids'">{{ record.bidCount }} / {{ record.supplierCount }} 家</template>
          <template v-else-if="column.key === 'award'">
            <template v-if="record.awardSupplierName">
              {{ record.awardSupplierName }}
              <div class="sub-text">{{ record.contractNo ? '合同 ' + record.contractNo : record.orderNo ? '订单 ' + record.orderNo : '' }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="scm_tender_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">查看</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增招标" edit-title="编辑招标" width="min(1000px, 100vw)" page>
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="14">
            <a-form-item label="招标标题" name="title">
              <a-input v-model:value="form.title" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="10">
            <a-form-item label="投标截止时间" name="deadline">
              <a-date-picker v-model:value="form.deadline" show-time value-format="YYYY-MM-DDTHH:mm:ss" format="YYYY-MM-DD HH:mm" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="邀请供应商" name="supplierIds" extra="发布后通知供应商的门户账号；没有门户账号的供应商可由采购代录线下标书">
              <a-select
                v-model:value="form.supplierIds"
                mode="multiple"
                :filter-option="false"
                :options="supplierOptions"
                placeholder="搜索供应商"
                @search="searchSuppliers"
                @focus="searchSuppliers('')"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="招标说明" name="remark">
              <a-textarea v-model:value="form.remark" :rows="2" :maxlength="500" placeholder="技术要求、资质要求、付款与交货条件等" />
            </a-form-item>
          </a-col>
        </a-row>
        <div class="items-head">
          <span class="section-title">招标物料</span>
          <a-button type="primary" ghost size="small" @click="addItem">
            <template #icon><PlusOutlined /></template>添加物料
          </a-button>
        </div>
        <a-table row-key="key" size="small" :columns="itemColumns" :data-source="form.items" :pagination="false" :scroll="{ x: 800 }">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'material'">
              <a-form-item :name="['items', index, 'materialId']" :rules="[{ required: true, message: '请选择物料' }]" class="cell-item">
                <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'qty'">
              <a-form-item :name="['items', index, 'qty']" :rules="[{ required: true, message: '请填写数量' }]" class="cell-item">
                <a-input-number v-model:value="record.qty" :min="0" class="full" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'requiredDate'">
              <a-date-picker v-model:value="record.requiredDate" value-format="YYYY-MM-DD" class="full" />
            </template>
            <template v-else-if="column.key === 'remark'">
              <a-input v-model:value="record.remark" :maxlength="255" placeholder="规格、技术要求" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" danger size="small" @click="form.items.splice(index, 1)"><DeleteOutlined /></a-button>
            </template>
          </template>
        </a-table>
      </a-form>
    </FormDrawer>

    <SubPage :open="detailOpen" :title="detail ? `招标 ${detail.tenderNo} ${detail.title}` : ''" width="min(1100px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space>
          <template v-if="detail.status === 'DRAFT'">
            <a-button v-perm="'scm:tender:edit'" @click="drawer.openEdit(detail)">编辑</a-button>
            <span v-perm="'scm:tender:remove'">
              <a-popconfirm title="删除该招标？" @confirm="remove"><a-button danger>删除</a-button></a-popconfirm>
            </span>
            <span v-perm="'scm:tender:edit'">
              <a-popconfirm title="发布后通知邀请的供应商投标，不能再修改，确定？" @confirm="publish"><a-button type="primary">发布</a-button></a-popconfirm>
            </span>
          </template>
          <template v-if="detail.status === 'BIDDING'">
            <a-button v-perm="'scm:tender:edit'" @click="openBid">代录标书</a-button>
            <span v-perm="'scm:tender:award'">
              <a-popconfirm title="开标后各家报价公开，供应商不能再修改，确定？" :disabled="!canOpen" @confirm="open">
                <a-tooltip :title="canOpen ? '' : '投标截止或邀请的供应商都已投标后才能开标'">
                  <a-button type="primary" :disabled="!canOpen">开标</a-button>
                </a-tooltip>
              </a-popconfirm>
            </span>
          </template>
          <a-button v-if="detail.status === 'OPENED' && detail.bids.length" v-perm="'scm:tender:award'" type="primary" @click="openAward()">定标</a-button>
          <span v-if="['DRAFT', 'BIDDING', 'OPENED'].includes(detail.status)" v-perm="'scm:tender:edit'">
            <a-popconfirm title="取消该招标？" @confirm="cancel"><a-button>取消招标</a-button></a-popconfirm>
          </span>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item label="状态"><DictTag type="scm_tender_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item label="投标截止">{{ detail.deadline }}</a-descriptions-item>
          <a-descriptions-item label="开标时间">{{ detail.openTime || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.awardSupplierName" label="中标供应商">{{ detail.awardSupplierName }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.contractNo || detail.orderNo" label="生成单据">
            {{ detail.contractNo ? '采购合同 ' + detail.contractNo : '采购订单 ' + detail.orderNo }}
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.awardReason" label="定标说明">{{ detail.awardReason }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="招标说明" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <div class="section-title">邀请的供应商</div>
        <div class="supplier-tags block">
          <a-tag v-for="s in detail.suppliers" :key="s.id" :color="s.bid ? 'green' : 'default'">
            {{ s.supplierName }} {{ s.bid ? '已投标' : '未投标' }}
          </a-tag>
        </div>

        <template v-if="opened">
          <div class="section-title">比价（开标后）</div>
          <a-table row-key="id" size="small" bordered :columns="compareColumns" :data-source="detail.items" :pagination="false" :scroll="{ x: 300 + detail.bids.length * 180 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'material'">
                {{ record.materialCode }} {{ record.materialName }}
                <div class="sub-text">{{ formatQty(record.qty) }} {{ record.unit }}</div>
              </template>
              <template v-else-if="column.bid">
                <template v-if="priceOf(column.bid, record.id)">
                  <span :class="{ best: isBest(column.bid, record.id) }">¥{{ Number(priceOf(column.bid, record.id).price) }}</span>
                  <div class="sub-text">小计 ¥{{ formatMoney(Number(priceOf(column.bid, record.id).price) * Number(record.qty)) }}</div>
                </template>
              </template>
            </template>
            <template #summary>
              <a-table-summary-row>
                <a-table-summary-cell>投标总价 / 交货期</a-table-summary-cell>
                <a-table-summary-cell v-for="b in detail.bids" :key="b.id">
                  <b :class="{ best: b.id === lowestBidId }">¥{{ formatMoney(b.totalAmount) }}</b>
                  <div class="sub-text">{{ b.deliveryDays != null ? `${b.deliveryDays} 天` : '-' }}{{ b.remark ? ' · ' + b.remark : '' }}</div>
                  <a-button
                    v-if="detail.status === 'OPENED'"
                    v-perm="'scm:tender:award'"
                    type="link"
                    size="small"
                    class="award-link"
                    @click="openAward(b.supplierId)"
                  >
                    定标给该供应商
                  </a-button>
                </a-table-summary-cell>
              </a-table-summary-row>
            </template>
          </a-table>
        </template>
        <template v-else>
          <div class="section-title">招标物料</div>
          <a-table row-key="id" size="small" :columns="itemViewColumns" :data-source="detail.items" :pagination="false" class="block">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
              <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
            </template>
          </a-table>
          <a-alert v-if="detail.bids.length" type="info" show-icon :message="`已收到 ${detail.bids.length} 份标书，报价在开标后可见`" />
        </template>
        <div class="section-title files-title">附件</div>
        <AttachmentPanel biz-type="scm_tender" :biz-id="detail.id" :editable="userStore.hasPerm('scm:tender:edit')" />
      </template>
    </SubPage>

    <!-- 代录标书 -->
    <a-modal v-model:open="bidOpen" title="代录标书" width="680px" :confirm-loading="saving" @ok="submitBid">
      <a-form layout="vertical">
        <a-row :gutter="12">
          <a-col :span="14">
            <a-form-item label="投标供应商" required>
              <a-select v-model:value="bid.supplierId" :options="invitedOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="10">
            <a-form-item label="交货期">
              <a-input-number v-model:value="bid.deliveryDays" :min="0" addon-after="天" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-table row-key="tenderItemId" size="small" :columns="bidColumns" :data-source="bid.items" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">{{ record.label }}</template>
            <template v-else-if="column.key === 'price'">
              <a-input-number v-model:value="record.price" :min="0" :precision="4" prefix="¥" class="full" />
            </template>
            <template v-else-if="column.key === 'taxRate'">
              <a-input-number v-model:value="record.taxRate" :min="0" :max="100" addon-after="%" class="full" />
            </template>
          </template>
        </a-table>
        <a-form-item label="投标说明" class="bid-remark">
          <a-input v-model:value="bid.remark" :maxlength="500" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 定标 -->
    <a-modal v-model:open="awardOpen" title="定标" :confirm-loading="saving" @ok="submitAward">
      <a-form layout="vertical">
        <a-form-item label="中标供应商" required>
          <a-select v-model:value="award.supplierId" :options="bidderOptions" />
        </a-form-item>
        <a-form-item label="定标后生成" required>
          <a-radio-group v-model:value="award.target">
            <a-radio value="CONTRACT">采购合同</a-radio>
            <a-radio value="ORDER">采购订单</a-radio>
            <a-radio value="NONE">不生成</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="award.target === 'ORDER'" label="收货仓库" required>
          <WarehouseSelect v-model:value="award.warehouseId" />
        </a-form-item>
        <a-row v-if="award.target === 'CONTRACT'" :gutter="12">
          <a-col :span="12">
            <a-form-item label="合同生效日期">
              <a-date-picker v-model:value="award.startDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="合同到期日期">
              <a-date-picker v-model:value="award.endDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="定标说明">
          <a-input v-model:value="award.reason" :maxlength="255" placeholder="如：综合价格与交期最优" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { partnerOptions } from '@/api/scm/partner'
import { addTender, awardTender, cancelTender, deleteTender, getTender, openTender, pageTender, proxyBid, publishTender, updateTender } from '@/api/scm/srm'
import { formatMoney } from '@/components/scm/scm'
import { formatQty, rowKey } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { useUserStore } from '@/store/user'

/** 招标采购：邀请供应商投标（门户或代录），截止后开标比价，定标生成采购合同或订单 */
const dict = useDict('scm_tender_status')
const userStore = useUserStore()

const columns = [
  { title: '招标编号 / 标题', key: 'tenderNo', width: 240 },
  { title: '投标截止', key: 'deadline', width: 180 },
  { title: '投标', key: 'bids', width: 100 },
  { title: '中标', key: 'award', width: 220 },
  { title: '状态', key: 'status', width: 90 },
  { title: '创建人', dataIndex: 'createByName', width: 100 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'scm-tender')
const query = reactive({ status: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageTender, query)
onMounted(load)

const passed = (t) => !dayjs(t.deadline).isAfter(dayjs())
function remain(t) {
  const mins = dayjs(t.deadline).diff(dayjs(), 'minute')
  return mins >= 1440 ? `剩余 ${Math.floor(mins / 1440)} 天` : mins >= 60 ? `剩余 ${Math.floor(mins / 60)} 小时` : `剩余 ${mins} 分钟`
}

// ---------------------------------------------------------------- 表单
const newItem = () => ({ key: rowKey(), materialId: undefined, qty: undefined, requiredDate: undefined, remark: '' })
const rules = {
  title: [{ required: true, whitespace: true, message: '请填写招标标题' }],
  deadline: [{ required: true, message: '请选择投标截止时间' }],
  supplierIds: [{ required: true, type: 'array', min: 1, message: '请选择邀请的供应商' }]
}
const supplierOptions = ref([])
async function searchSuppliers(keyword) {
  const list = await partnerOptions('supplier', keyword || undefined)
  const selected = supplierOptions.value.filter((o) => form.value.supplierIds?.includes(o.value))
  const map = new Map(selected.map((o) => [o.value, o]))
  list.forEach((s) => map.set(s.id, { value: s.id, label: `${s.code} ${s.name}` }))
  supplierOptions.value = [...map.values()]
}
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined, title: '', deadline: dayjs().add(7, 'day').hour(17).minute(0).second(0).format('YYYY-MM-DDTHH:mm:ss'),
    remark: '', supplierIds: [], items: [newItem()]
  }),
  fetch: async (record) => {
    const t = await getTender(record.id)
    supplierOptions.value = t.suppliers.map((s) => ({ value: s.supplierId, label: `${s.supplierCode} ${s.supplierName}` }))
    return {
      ...t,
      deadline: t.deadline.replace(' ', 'T'),
      supplierIds: t.suppliers.map((s) => s.supplierId),
      items: t.items.map((i) => ({ key: rowKey(), materialId: i.materialId, materialLabel: `${i.materialCode} ${i.materialName}`,
        qty: Number(i.qty), requiredDate: i.requiredDate, remark: i.remark }))
    }
  },
  beforeOpen: (record) => {
    if (!record) supplierOptions.value = []
  },
  create: addTender,
  update: updateTender,
  toPayload: (f) => ({
    title: f.title, deadline: f.deadline, remark: f.remark || null, supplierIds: f.supplierIds,
    items: f.items.map((i) => ({ materialId: i.materialId, qty: i.qty, requiredDate: i.requiredDate || null, remark: i.remark || null }))
  }),
  onSuccess: () => {
    load()
    if (detailOpen.value) refreshDetail()
  }
})
const { formRef, form } = drawer
const addItem = () => form.value.items.push(newItem())
const itemColumns = [
  { title: '物料', key: 'material', width: 280 },
  { title: '数量', key: 'qty', width: 130 },
  { title: '需求日期', key: 'requiredDate', width: 150 },
  { title: '要求', key: 'remark', width: 200 },
  { title: '', key: 'action', width: 50 }
]
const itemViewColumns = [
  { title: '物料', key: 'material' },
  { title: '数量', key: 'qty', width: 140 },
  { title: '需求日期', dataIndex: 'requiredDate', width: 120 },
  { title: '要求', dataIndex: 'remark', ellipsis: true }
]

// ---------------------------------------------------------------- 详情
const detailOpen = ref(false)
const detail = ref()
async function openDetail(id) {
  detail.value = await getTender(id)
  detailOpen.value = true
}
async function refreshDetail() {
  if (detail.value) detail.value = await getTender(detail.value.id)
}
const opened = computed(() => ['OPENED', 'AWARDED'].includes(detail.value?.status))
const canOpen = computed(() => detail.value && (passed(detail.value) || detail.value.suppliers.every((s) => s.bid)))
const compareColumns = computed(() => [
  { title: '物料', key: 'material', width: 240, fixed: 'left' },
  ...(detail.value?.bids || []).map((b) => ({ title: b.supplierName, key: `bid${b.id}`, bid: b, width: 180 }))
])
const priceOf = (bid, itemId) => bid.items?.find((i) => i.tenderItemId === itemId)
function isBest(bid, itemId) {
  const prices = detail.value.bids.map((b) => Number(priceOf(b, itemId)?.price ?? Infinity))
  return detail.value.bids.length > 1 && Number(priceOf(bid, itemId)?.price) === Math.min(...prices)
}
const lowestBidId = computed(() => {
  const bids = detail.value?.bids || []
  if (bids.length < 2) return null
  return bids.reduce((min, b) => (Number(b.totalAmount) < Number(min.totalAmount) ? b : min)).id
})
async function publish() {
  await publishTender(detail.value.id)
  message.success('已发布，已通知邀请的供应商')
  refreshDetail()
  load()
}
async function open() {
  await openTender(detail.value.id)
  message.success('已开标')
  refreshDetail()
  load()
}
async function cancel() {
  await cancelTender(detail.value.id)
  message.success('已取消')
  refreshDetail()
  load()
}
async function remove() {
  await deleteTender(detail.value.id)
  message.success('已删除')
  detailOpen.value = false
  load()
}

// 代录标书
const saving = ref(false)
const bidOpen = ref(false)
const bid = ref({})
const invitedOptions = computed(() => (detail.value?.suppliers || []).map((s) => ({ value: s.supplierId, label: s.supplierName })))
const bidColumns = [
  { title: '物料', key: 'material' },
  { title: '含税单价', key: 'price', width: 170 },
  { title: '税率', key: 'taxRate', width: 120 }
]
function openBid() {
  bid.value = {
    supplierId: undefined, deliveryDays: undefined, remark: '',
    items: detail.value.items.map((i) => ({ tenderItemId: i.id, label: `${i.materialCode} ${i.materialName} × ${formatQty(i.qty)}`, price: undefined, taxRate: 13 }))
  }
  bidOpen.value = true
}
async function submitBid() {
  if (!bid.value.supplierId) return message.warning('请选择投标供应商')
  if (bid.value.items.some((i) => i.price == null)) return message.warning('请为每个物料报价')
  saving.value = true
  try {
    await proxyBid(detail.value.id, bid.value)
    message.success('已保存标书')
    bidOpen.value = false
    refreshDetail()
    load()
  } finally {
    saving.value = false
  }
}

// 定标
const awardOpen = ref(false)
const award = ref({})
const bidderOptions = computed(() => (detail.value?.bids || []).map((b) => ({ value: b.supplierId, label: `${b.supplierName}（¥${formatMoney(b.totalAmount)}）` })))
function openAward(supplierId) {
  award.value = {
    supplierId: supplierId ?? detail.value.bids.find((b) => b.id === lowestBidId.value)?.supplierId ?? detail.value.bids[0]?.supplierId,
    target: 'CONTRACT', warehouseId: undefined, startDate: undefined, endDate: undefined, reason: ''
  }
  awardOpen.value = true
}
async function submitAward() {
  if (award.value.target === 'ORDER' && !award.value.warehouseId) return message.warning('请选择收货仓库')
  saving.value = true
  try {
    await awardTender(detail.value.id, award.value)
    message.success(award.value.target === 'CONTRACT' ? '已定标，已生成采购合同（草稿）' : award.value.target === 'ORDER' ? '已定标，已生成采购订单（草稿）' : '已定标')
    awardOpen.value = false
    refreshDetail()
    load()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.filter {
  width: 140px;
}
.filter-wide {
  width: 220px;
}
.full {
  width: 100%;
}
.block {
  margin-bottom: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.danger {
  color: var(--status-critical);
}
.best {
  color: var(--status-success, #52c41a);
  font-weight: 600;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0;
}
.section-title {
  margin: 8px 0;
  font-weight: 600;
}
.files-title {
  margin-top: 16px;
}
.supplier-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.cell-item {
  margin-bottom: 0;
}
.award-link {
  padding: 0;
}
.bid-remark {
  margin-top: 12px;
}
</style>
