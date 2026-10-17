<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('scm.shipment.no')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('scm.shipment.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('scm.shipment.customer')" name="customerId">
          <PartnerSelect v-model:value="query.customerId" type="customer" :placeholder="$t('common.placeholder.all')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.scm_shipment_status)" />
        </a-form-item>
        <a-form-item :label="$t('scm.shipment.carrier')" name="carrier">
          <a-select v-model:value="query.carrier" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.scm_carrier)" />
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
          <template #icon><PlusOutlined /></template>{{ $t('scm.shipment.add') }}
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
            <div class="sub-text">{{ record.trackingNo || $t('scm.shipment.noTrackingNo') }}</div>
          </template>
          <template v-else-if="column.key === 'receiver'">
            <div>{{ record.receiver || '-' }} {{ record.phone }}</div>
            <div class="sub-text ellipsis" :title="record.address">{{ record.address }}</div>
          </template>
          <template v-else-if="column.key === 'freight'">{{ record.freight != null ? '¥' + formatMoney(record.freight) : '-' }}</template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="scm_shipment_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'dates'">
            <div>{{ $t('scm.shipment.shipShort', { date: record.shipDate || '-' }) }}</div>
            <div class="sub-text">{{ $t('scm.shipment.signShort', { date: record.signDate || '-' }) }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">
                <template #icon><EyeOutlined /></template>{{ $t('common.action.view') }}
              </a-button>
              <a-button v-if="record.status !== ShipmentStatus.SIGNED" v-perm="'scm:shipment:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <span v-if="record.status === ShipmentStatus.PENDING" v-perm="'scm:shipment:remove'">
                <a-popconfirm :title="$t('scm.shipment.confirmDelete', { no: record.shipNo })" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small">
                    <template #icon><DeleteOutlined /></template>{{ $t('common.action.delete') }}
                  </a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增 / 编辑 -->
    <FormDrawer :drawer="drawer" :create-title="$t('scm.shipment.add')" :edit-title="$t('scm.shipment.edit')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item :label="$t('scm.shipment.salesOutDoc')" name="tradeDocId">
              <a-select
                v-model:value="form.tradeDocId"
                show-search
                :filter-option="false"
                :options="docOptions"
                :loading="docLoading"
                :disabled="drawer.isEdit.value"
                :placeholder="$t('scm.shipment.searchDocNo')"
                @search="searchDocs"
                @focus="() => !docs.length && searchDocs('')"
                @change="onDocSelect"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.shipment.carrier')" name="carrier">
              <a-select v-model:value="form.carrier" :options="toOptions(dict.scm_carrier)" :placeholder="$t('scm.shipment.selectCarrier')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.shipment.trackingNo')" name="trackingNo">
              <a-input v-model:value="form.trackingNo" :maxlength="64" :placeholder="$t('scm.shipment.requiredOnShip')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.shipment.receiver')" name="receiver">
              <a-input v-model:value="form.receiver" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.shipment.phone')" name="phone">
              <a-input v-model:value="form.phone" :maxlength="32" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('scm.shipment.address')" name="address">
              <a-input v-model:value="form.address" :maxlength="255" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.shipment.freight')" name="freight">
              <a-input-number v-model:value="form.freight" :min="0" :precision="2" prefix="¥" class="full" :disabled="!!form.freightBillId" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.shipment.carrierSupplier')" name="carrierSupplierId" :extra="$t('scm.shipment.carrierSupplierExtra')">
              <PartnerSelect
                v-model:value="form.carrierSupplierId"
                type="supplier"
                :label="form.carrierSupplierName ? `${form.carrierSupplierCode} ${form.carrierSupplierName}` : undefined"
                allow-clear
                :placeholder="$t('scm.shipment.noPayable')"
                :disabled="!!form.freightBillId"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <!-- 详情与物流跟踪 -->
    <SubPage :open="detailOpen" :title="detail ? `${$t('scm.shipment.title')} ${detail.shipNo}` : $t('scm.shipment.title')" width="min(720px, 100vw)" @close="detailOpen = false">
      <template #extra>
        <DictTag v-if="detail" type="scm_shipment_status" :value="detail.status" />
      </template>
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" class="doc-desc">
            <a-descriptions-item :label="$t('scm.shipment.outDoc')">{{ detail.tradeDocNo }}</a-descriptions-item>
            <a-descriptions-item :label="$t('scm.shipment.orderNo')">{{ detail.orderNo }}</a-descriptions-item>
            <a-descriptions-item :label="$t('scm.shipment.customer')">{{ detail.customerName }}</a-descriptions-item>
            <a-descriptions-item :label="$t('scm.shipment.carrier')"><DictTag type="scm_carrier" :value="detail.carrier" /></a-descriptions-item>
            <a-descriptions-item :label="$t('scm.shipment.trackingNo')">{{ detail.trackingNo || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('scm.shipment.freight')">
              {{ detail.freight != null ? '¥' + formatMoney(detail.freight) : '-' }}
              <span v-if="detail.carrierSupplierName" class="sub-text">{{ detail.freightBillId ? $t('scm.shipment.supplierBilled', { name: detail.carrierSupplierName }) : $t('scm.shipment.supplierParen', { name: detail.carrierSupplierName }) }}</span>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('scm.shipment.receiver')">{{ detail.receiver || '-' }} {{ detail.phone }}</a-descriptions-item>
            <a-descriptions-item :label="$t('scm.shipment.shipSign')">{{ detail.shipDate || '-' }} / {{ detail.signDate || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('scm.shipment.address')" :span="2">{{ detail.address || '-' }}</a-descriptions-item>
          </a-descriptions>
          <div class="section-title">{{ $t('scm.shipment.tracks') }}</div>
          <a-timeline class="timeline">
            <a-timeline-item v-for="t in detail.tracks" :key="t.id" :color="trackColor(t.status)">
              <div>{{ t.content }}</div>
              <div class="sub-text">{{ t.trackTime }} · {{ t.createByName || '-' }}</div>
            </a-timeline-item>
          </a-timeline>
        </template>
      </a-spin>
      <template v-if="detail && userStore.hasPerm('scm:shipment:track') && detail.status !== ShipmentStatus.SIGNED" #footer>
        <div class="drawer-footer">
          <a-button v-if="[ShipmentStatus.SHIPPED, ShipmentStatus.EXCEPTION].includes(detail.status)" @click="openAction('track')">
            <template #icon><MessageOutlined /></template>{{ $t('scm.shipment.addTrack') }}
          </a-button>
          <a-button v-if="detail.status === ShipmentStatus.SHIPPED" danger @click="openAction('exception')">
            <template #icon><WarningOutlined /></template>{{ $t('scm.shipment.markException') }}
          </a-button>
          <a-button v-if="[ShipmentStatus.PENDING, ShipmentStatus.EXCEPTION].includes(detail.status)" type="primary" @click="openAction('ship')">
            <template #icon><CarOutlined /></template>{{ detail.status === ShipmentStatus.PENDING ? $t('scm.shipment.ship') : $t('scm.shipment.continue') }}
          </a-button>
          <a-button v-if="[ShipmentStatus.SHIPPED, ShipmentStatus.EXCEPTION].includes(detail.status)" type="primary" @click="openAction('sign')">
            <template #icon><CheckOutlined /></template>{{ $t('scm.shipment.sign') }}
          </a-button>
        </div>
      </template>
    </SubPage>

    <a-modal v-model:open="action.open" :title="ACTIONS[action.type]?.title" :confirm-loading="action.saving" @ok="submitAction">
      <a-form layout="vertical">
        <a-form-item v-if="action.type === 'ship'" :label="$t('scm.shipment.trackingNo')" required>
          <a-input v-model:value="action.trackingNo" :maxlength="64" />
        </a-form-item>
        <a-form-item v-if="['ship', 'sign'].includes(action.type)" :label="action.type === 'ship' ? $t('scm.shipment.shipDate') : $t('scm.shipment.signDate')">
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
import { useI18n } from 'vue-i18n'
import { useAction } from '@/composables/useAction'

import { ShipmentStatus } from '@/constants/enums'
const { t } = useI18n()
const route = useRoute()
const userStore = useUserStore()
const dict = useDict('scm_shipment_status', 'scm_carrier')

const columns = [
  { title: t('scm.shipment.shipNo'), key: 'shipNo', width: 170 },
  { title: t('common.field.status'), key: 'status', width: 100, dict: 'scm_shipment_status' },
  { title: t('scm.shipment.source'), key: 'source', width: 180 },
  { title: t('scm.shipment.customer'), dataIndex: 'customerName', width: 180, ellipsis: true },
  { title: t('scm.shipment.carrierTracking'), key: 'carrier', width: 180, dict: 'scm_carrier' },
  { title: t('scm.shipment.receiverInfo'), key: 'receiver', width: 220 },
  { title: t('scm.shipment.freight'), key: 'freight', width: 100, align: 'right' },
  { title: t('scm.shipment.shipSign'), key: 'dates', width: 150 },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: 220, fixed: 'right' }
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
  tradeDocId: [{ required: true, message: t('scm.shipment.selectDoc') }],
  carrier: [{ required: true, message: t('scm.shipment.selectCarrier') }]
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

const remove = useAction(deleteShipment, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

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
  ship: { title: t('scm.shipment.ship'), contentLabel: t('common.field.remark'), placeholder: t('scm.shipment.shipPh') },
  track: { title: t('scm.shipment.addTrackTitle'), contentLabel: t('scm.shipment.tracks'), placeholder: t('scm.shipment.trackPh') },
  exception: { title: t('scm.shipment.markException'), contentLabel: t('scm.shipment.exceptionDesc'), placeholder: t('scm.shipment.exceptionPh') },
  sign: { title: t('scm.shipment.sign'), contentLabel: t('common.field.remark'), placeholder: t('scm.shipment.signPh') }
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
    message.success(t('common.msg.operationSuccess'))
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
