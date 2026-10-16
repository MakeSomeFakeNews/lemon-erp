<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="单号" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="发运单号、运单号或出库单号" allow-clear />
        </a-form-item>
        <a-form-item label="客户" name="customerId">
          <PartnerSelect v-model:value="query.customerId" type="customer" placeholder="全部" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.scm_shipment_status)" />
        </a-form-item>
        <a-form-item label="承运商" name="carrier">
          <a-select v-model:value="query.carrier" placeholder="全部" allow-clear :options="toOptions(dict.scm_carrier)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll"
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="'scm:shipment:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>新增发运单
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1400, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'shipNo'">
            <a @click="openDetail(record)">{{ record.shipNo }}</a>
          </template>
          <template v-else-if="column.key === 'source'">
            <div>{{ record.tradeDocNo }}</div>
            <div class="sub-text">{{ record.orderNo }}</div>
          </template>
          <template v-else-if="column.key === 'carrier'">
            <DictTag type="scm_carrier" :value="record.carrier" />
            <div class="sub-text">{{ record.trackingNo || '未填运单号' }}</div>
          </template>
          <template v-else-if="column.key === 'receiver'">
            <div>{{ record.receiver || '-' }} {{ record.phone }}</div>
            <div class="sub-text ellipsis">{{ record.address }}</div>
          </template>
          <template v-else-if="column.key === 'freight'">{{ record.freight != null ? '¥' + formatMoney(record.freight) : '-' }}</template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="scm_shipment_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'dates'">
            <div>发 {{ record.shipDate || '-' }}</div>
            <div class="sub-text">签 {{ record.signDate || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">
                <template #icon><EyeOutlined /></template>查看
              </a-button>
              <a-button v-if="record.status !== 'SIGNED'" v-perm="'scm:shipment:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <span v-if="record.status === 'PENDING'" v-perm="'scm:shipment:remove'">
                <a-popconfirm :title="`确定删除发运单 ${record.shipNo}？`" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small">
                    <template #icon><DeleteOutlined /></template>删除
                  </a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增 / 编辑 -->
    <FormDrawer :drawer="drawer" create-title="新增发运单" edit-title="编辑发运单">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="销售出库单" name="tradeDocId">
              <a-select
                v-model:value="form.tradeDocId"
                show-search
                :filter-option="false"
                :options="docOptions"
                :loading="docLoading"
                :disabled="drawer.isEdit.value"
                placeholder="搜索出库单号"
                @search="searchDocs"
                @focus="() => !docs.length && searchDocs('')"
                @change="onDocSelect"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="承运商" name="carrier">
              <a-select v-model:value="form.carrier" :options="toOptions(dict.scm_carrier)" placeholder="请选择承运商" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="运单号" name="trackingNo">
              <a-input v-model:value="form.trackingNo" :maxlength="64" placeholder="发货时必填" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="收货人" name="receiver">
              <a-input v-model:value="form.receiver" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="收货电话" name="phone">
              <a-input v-model:value="form.phone" :maxlength="32" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="收货地址" name="address">
              <a-input v-model:value="form.address" :maxlength="255" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="运费" name="freight">
              <a-input-number v-model:value="form.freight" :min="0" :precision="2" prefix="¥" class="full" :disabled="!!form.freightBillId" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="运费结算供应商" name="carrierSupplierId" extra="发货时按运费生成该供应商的应付单（计入销售费用）">
              <PartnerSelect
                v-model:value="form.carrierSupplierId"
                type="supplier"
                :label="form.carrierSupplierName ? `${form.carrierSupplierCode} ${form.carrierSupplierName}` : undefined"
                allow-clear
                placeholder="不生成应付"
                :disabled="!!form.freightBillId"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <!-- 详情与物流跟踪 -->
    <SubPage :open="detailOpen" :title="detail ? `发运单 ${detail.shipNo}` : '发运单'" width="min(720px, 100vw)" @close="detailOpen = false">
      <template #extra>
        <DictTag v-if="detail" type="scm_shipment_status" :value="detail.status" />
      </template>
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" class="doc-desc">
            <a-descriptions-item label="出库单">{{ detail.tradeDocNo }}</a-descriptions-item>
            <a-descriptions-item label="订单号">{{ detail.orderNo }}</a-descriptions-item>
            <a-descriptions-item label="客户">{{ detail.customerName }}</a-descriptions-item>
            <a-descriptions-item label="承运商"><DictTag type="scm_carrier" :value="detail.carrier" /></a-descriptions-item>
            <a-descriptions-item label="运单号">{{ detail.trackingNo || '-' }}</a-descriptions-item>
            <a-descriptions-item label="运费">
              {{ detail.freight != null ? '¥' + formatMoney(detail.freight) : '-' }}
              <span v-if="detail.carrierSupplierName" class="sub-text">（{{ detail.carrierSupplierName }}{{ detail.freightBillId ? '，已生成应付单' : '' }}）</span>
            </a-descriptions-item>
            <a-descriptions-item label="收货人">{{ detail.receiver || '-' }} {{ detail.phone }}</a-descriptions-item>
            <a-descriptions-item label="发货 / 签收">{{ detail.shipDate || '-' }} / {{ detail.signDate || '-' }}</a-descriptions-item>
            <a-descriptions-item label="收货地址" :span="2">{{ detail.address || '-' }}</a-descriptions-item>
          </a-descriptions>
          <div class="section-title">物流动态</div>
          <a-timeline class="timeline">
            <a-timeline-item v-for="t in detail.tracks" :key="t.id" :color="trackColor(t.status)">
              <div>{{ t.content }}</div>
              <div class="sub-text">{{ t.trackTime }} · {{ t.createByName || '-' }}</div>
            </a-timeline-item>
          </a-timeline>
        </template>
      </a-spin>
      <template v-if="detail && userStore.hasPerm('scm:shipment:track') && detail.status !== 'SIGNED'" #footer>
        <div class="drawer-footer">
          <a-button v-if="['SHIPPED', 'EXCEPTION'].includes(detail.status)" @click="openAction('track')">
            <template #icon><MessageOutlined /></template>添加动态
          </a-button>
          <a-button v-if="detail.status === 'SHIPPED'" danger @click="openAction('exception')">
            <template #icon><WarningOutlined /></template>标记异常
          </a-button>
          <a-button v-if="['PENDING', 'EXCEPTION'].includes(detail.status)" type="primary" @click="openAction('ship')">
            <template #icon><CarOutlined /></template>{{ detail.status === 'PENDING' ? '发货' : '继续运输' }}
          </a-button>
          <a-button v-if="['SHIPPED', 'EXCEPTION'].includes(detail.status)" type="primary" @click="openAction('sign')">
            <template #icon><CheckOutlined /></template>签收
          </a-button>
        </div>
      </template>
    </SubPage>

    <a-modal v-model:open="action.open" :title="ACTIONS[action.type]?.title" :confirm-loading="action.saving" @ok="submitAction">
      <a-form layout="vertical">
        <a-form-item v-if="action.type === 'ship'" label="运单号" required>
          <a-input v-model:value="action.trackingNo" :maxlength="64" />
        </a-form-item>
        <a-form-item v-if="['ship', 'sign'].includes(action.type)" :label="action.type === 'ship' ? '发货日期' : '签收日期'">
          <a-date-picker v-model:value="action.date" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item :label="ACTIONS[action.type]?.contentLabel" :required="['track', 'exception'].includes(action.type)">
          <a-textarea v-model:value="action.content" :rows="3" :maxlength="255" :placeholder="ACTIONS[action.type]?.placeholder" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  CarOutlined,
  CheckOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  MessageOutlined,
  PlusOutlined,
  WarningOutlined
} from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { partnerOptions } from '@/api/scm/partner'
import { addShipment, deleteShipment, getShipment, pageShipment, shipmentAction, updateShipment } from '@/api/scm/shipment'
import { pageTrade } from '@/api/scm/trade'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { formatMoney } from '@/components/scm/scm'
import { today } from '@/components/wms/wms'

const route = useRoute()
const userStore = useUserStore()
const dict = useDict('scm_shipment_status', 'scm_carrier')

const columns = [
  { title: '发运单号', key: 'shipNo', width: 170 },
  { title: '状态', key: 'status', width: 90, dict: 'scm_shipment_status' },
  { title: '出库单 / 订单', key: 'source', width: 170 },
  { title: '客户', dataIndex: 'customerName', width: 180, ellipsis: true },
  { title: '承运商 / 运单号', key: 'carrier', width: 170, dict: 'scm_carrier' },
  { title: '收货信息', key: 'receiver', width: 220 },
  { title: '运费', key: 'freight', width: 100, align: 'right' },
  { title: '发货 / 签收', key: 'dates', width: 130 },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'scm-shipment')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, customerId: undefined, status: undefined, carrier: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageShipment, query)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

// ---------------------------------------------------------------- 新增 / 编辑
const rules = {
  tradeDocId: [{ required: true, message: '请选择销售出库单' }],
  carrier: [{ required: true, message: '请选择承运商' }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    tradeDocId: undefined,
    carrier: undefined,
    trackingNo: '',
    freight: undefined,
    carrierSupplierId: undefined,
    receiver: '',
    phone: '',
    address: '',
    remark: ''
  }),
  fetch: async (record) => {
    const s = await getShipment(record.id)
    docs.value = [{ id: s.tradeDocId, docNo: s.tradeDocNo, partnerName: s.customerName }]
    return s
  },
  create: addShipment,
  update: updateShipment,
  toPayload: ({ tradeDocId, carrier, trackingNo, freight, carrierSupplierId, receiver, phone, address, remark }) => ({
    tradeDocId,
    carrier,
    trackingNo,
    freight: freight ?? null,
    carrierSupplierId: carrierSupplierId ?? null,
    receiver,
    phone,
    address,
    remark
  }),
  onSuccess: load
})
const { formRef, form } = drawer

const docs = ref([])
const docLoading = ref(false)
const docOptions = computed(() => docs.value.map((d) => ({ value: d.id, label: `${d.docNo}　${d.partnerName}` })))
let timer
function searchDocs(docNo) {
  clearTimeout(timer)
  timer = setTimeout(async () => {
    docLoading.value = true
    try {
      docs.value = (await pageTrade('salesOut', { docNo: docNo || undefined, current: 1, size: 50 })).records
    } finally {
      docLoading.value = false
    }
  }, 200)
}

/** 选择出库单后带出客户的联系人、电话、地址 */
async function onDocSelect(id) {
  const doc = docs.value.find((d) => d.id === id)
  if (!doc) return
  const customers = await partnerOptions('customer', doc.partnerCode)
  const c = customers.find((p) => p.code === doc.partnerCode)
  if (c) Object.assign(form.value, { receiver: c.contact || '', phone: c.phone || '', address: c.address || '' })
}

onMounted(async () => {
  await load()
  // 从销售出库单跳转过来：直接新建该出库单的发运单
  const tradeDocId = Number(route.query.tradeDocId)
  if (tradeDocId) {
    await drawer.openCreate()
    docs.value = (await pageTrade('salesOut', { current: 1, size: 50 })).records
    if (docs.value.some((d) => d.id === tradeDocId)) {
      form.value.tradeDocId = tradeDocId
      await onDocSelect(tradeDocId)
    }
  }
})

async function remove(id) {
  await deleteShipment(id)
  message.success('删除成功')
  load()
}

// ---------------------------------------------------------------- 详情与跟踪
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref()
async function loadDetail(id) {
  detailLoading.value = true
  try {
    detail.value = await getShipment(id)
  } finally {
    detailLoading.value = false
  }
}
function openDetail(record) {
  detail.value = undefined
  detailOpen.value = true
  loadDetail(record.id)
}
const trackColor = (status) => ({ SIGNED: 'green', EXCEPTION: 'red', PENDING: 'gray' })[status] || 'blue'

const ACTIONS = {
  ship: { title: '发货', contentLabel: '备注', placeholder: '选填，默认「已发货，运单号 …」' },
  track: { title: '添加物流动态', contentLabel: '物流动态', placeholder: '如：已到达北京转运中心' },
  exception: { title: '标记异常', contentLabel: '异常说明', placeholder: '如：包裹破损、地址错误' },
  sign: { title: '签收', contentLabel: '备注', placeholder: '选填，默认「客户已签收」' }
}
const action = reactive({ open: false, saving: false, type: 'ship', date: today(), trackingNo: '', content: '' })
function openAction(type) {
  Object.assign(action, { open: true, type, date: today(), trackingNo: detail.value.trackingNo || '', content: '' })
}
async function submitAction() {
  action.saving = true
  try {
    await shipmentAction(detail.value.id, action.type, {
      date: action.date,
      trackingNo: action.trackingNo || null,
      content: action.content || null
    })
    message.success('操作成功')
    action.open = false
    await loadDetail(detail.value.id)
    load()
  } finally {
    action.saving = false
  }
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.ellipsis {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.full {
  width: 100%;
}
.doc-desc {
  margin-bottom: 16px;
}
.section-title {
  font-weight: 500;
  margin-bottom: 16px;
}
.timeline {
  padding-left: 4px;
}
</style>
