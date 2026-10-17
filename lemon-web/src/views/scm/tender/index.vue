<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" :placeholder="$t('scm.tender.allStatus')" allow-clear :options="toOptions(dict.scm_tender_status)" class="filter" @change="search" />
        <a-input-search v-model:value="query.keyword" :placeholder="$t('scm.tender.keywordPh')" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <a-button v-perm="'scm:tender:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>{{ $t('scm.tender.add') }}
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
            <div v-if="record.status === TenderStatus.BIDDING" :class="['sub-text', { danger: passed(record) }]">{{ passed(record) ? $t('scm.tender.closedCanOpen') : remain(record) }}</div>
          </template>
          <template v-else-if="column.key === 'bids'">{{ $t('scm.tender.bidCount', { bids: record.bidCount, total: record.supplierCount }) }}</template>
          <template v-else-if="column.key === 'award'">
            <template v-if="record.awardSupplierName">
              {{ record.awardSupplierName }}
              <div class="sub-text">{{ record.contractNo ? $t('scm.tender.contractNo', { no: record.contractNo }) : record.orderNo ? $t('scm.tender.orderNo', { no: record.orderNo }) : '' }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="scm_tender_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">{{ $t('common.action.view') }}</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('scm.tender.add')" :edit-title="$t('scm.tender.edit')" width="min(1000px, 100vw)" page>
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="14">
            <a-form-item :label="$t('scm.tender.title')" name="title">
              <a-input v-model:value="form.title" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="10">
            <a-form-item :label="$t('scm.tender.deadline')" name="deadline">
              <a-date-picker v-model:value="form.deadline" show-time value-format="YYYY-MM-DDTHH:mm:ss" format="YYYY-MM-DD HH:mm" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('scm.tender.suppliers')" name="supplierIds" :extra="$t('scm.tender.suppliersExtra')">
              <a-select
                v-model:value="form.supplierIds"
                mode="multiple"
                :filter-option="false"
                :options="supplierOptions"
                :placeholder="$t('scm.tender.searchSupplier')"
                @search="searchSuppliers"
                @focus="searchSuppliers('')"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('scm.tender.remark')" name="remark">
              <a-textarea v-model:value="form.remark" :rows="2" :maxlength="500" :placeholder="$t('scm.tender.remarkPh')" />
            </a-form-item>
          </a-col>
        </a-row>
        <div class="items-head">
          <span class="section-title">{{ $t('scm.tender.materials') }}</span>
          <a-button type="primary" ghost size="small" @click="addItem">
            <template #icon><PlusOutlined /></template>{{ $t('scm.tender.addMaterial') }}
          </a-button>
        </div>
        <a-table row-key="key" size="small" :columns="itemColumns" :data-source="form.items" :pagination="false" :scroll="{ x: 800 }">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'material'">
              <a-form-item :name="['items', index, 'materialId']" :rules="[{ required: true, message: $t('scm.tender.selectMaterial') }]" class="cell-item">
                <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'qty'">
              <a-form-item :name="['items', index, 'qty']" :rules="[{ required: true, message: $t('scm.tender.inputQty') }]" class="cell-item">
                <a-input-number v-model:value="record.qty" :min="0" class="full" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'requiredDate'">
              <a-date-picker v-model:value="record.requiredDate" value-format="YYYY-MM-DD" class="full" />
            </template>
            <template v-else-if="column.key === 'remark'">
              <a-input v-model:value="record.remark" :maxlength="255" :placeholder="$t('scm.tender.requirementPh')" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" danger size="small" @click="form.items.splice(index, 1)"><DeleteOutlined /></a-button>
            </template>
          </template>
        </a-table>
      </a-form>
    </FormDrawer>

    <SubPage :open="detailOpen" :title="detail ? $t('scm.tender.detailTitle', { no: detail.tenderNo, title: detail.title }) : ''" width="min(1100px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space>
          <template v-if="detail.status === TenderStatus.DRAFT">
            <a-button v-perm="'scm:tender:edit'" @click="drawer.openEdit(detail)">{{ $t('common.action.edit') }}</a-button>
            <span v-perm="'scm:tender:remove'">
              <a-popconfirm :title="$t('scm.tender.confirmDelete')" @confirm="remove"><a-button danger>{{ $t('common.action.delete') }}</a-button></a-popconfirm>
            </span>
            <span v-perm="'scm:tender:edit'">
              <a-popconfirm :title="$t('scm.tender.confirmPublish')" @confirm="publish"><a-button type="primary">{{ $t('scm.tender.publish') }}</a-button></a-popconfirm>
            </span>
          </template>
          <template v-if="detail.status === TenderStatus.BIDDING">
            <a-button v-perm="'scm:tender:edit'" @click="openBid">{{ $t('scm.tender.proxyBid') }}</a-button>
            <span v-perm="'scm:tender:award'">
              <a-popconfirm :title="$t('scm.tender.confirmOpen')" :disabled="!canOpen" @confirm="open">
                <a-tooltip :title="canOpen ? '' : $t('scm.tender.openTip')">
                  <a-button type="primary" :disabled="!canOpen">{{ $t('scm.tender.open') }}</a-button>
                </a-tooltip>
              </a-popconfirm>
            </span>
          </template>
          <a-button v-if="detail.status === TenderStatus.OPENED && detail.bids.length" v-perm="'scm:tender:award'" type="primary" @click="openAward()">{{ $t('scm.tender.award') }}</a-button>
          <span v-if="[TenderStatus.DRAFT, TenderStatus.BIDDING, TenderStatus.OPENED].includes(detail.status)" v-perm="'scm:tender:edit'">
            <a-popconfirm :title="$t('scm.tender.confirmCancel')" @confirm="cancel"><a-button>{{ $t('scm.tender.cancelTender') }}</a-button></a-popconfirm>
          </span>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="scm_tender_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('scm.tender.bidDeadline')">{{ detail.deadline }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.tender.openTime')">{{ detail.openTime || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.awardSupplierName" :label="$t('scm.tender.awardSupplier')">{{ detail.awardSupplierName }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.contractNo || detail.orderNo" :label="$t('scm.tender.generatedDoc')">
            {{ detail.contractNo ? $t('scm.tender.genContract', { no: detail.contractNo }) : $t('scm.tender.genOrder', { no: detail.orderNo }) }}
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.awardReason" :label="$t('scm.tender.awardReason')">{{ detail.awardReason }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('scm.tender.remark')" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <div class="section-title">{{ $t('scm.tender.invited') }}</div>
        <div class="supplier-tags block">
          <a-tag v-for="s in detail.suppliers" :key="s.id" :color="s.bid ? 'green' : 'default'">
            {{ s.supplierName }} {{ s.bid ? $t('scm.tender.bidded') : $t('scm.tender.notBidded') }}
          </a-tag>
        </div>

        <template v-if="opened">
          <div class="section-title">{{ $t('scm.tender.compare') }}</div>
          <a-table row-key="id" size="small" bordered :columns="compareColumns" :data-source="detail.items" :pagination="false" :scroll="{ x: 300 + detail.bids.length * 180 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'material'">
                {{ record.materialCode }} {{ record.materialName }}
                <div class="sub-text">{{ formatQty(record.qty) }} {{ record.unit }}</div>
              </template>
              <template v-else-if="column.bid">
                <template v-if="priceOf(column.bid, record.id)">
                  <span :class="{ best: isBest(column.bid, record.id) }">¥{{ Number(priceOf(column.bid, record.id).price) }}</span>
                  <div class="sub-text">{{ $t('scm.tender.subtotal', { amount: formatMoney(Number(priceOf(column.bid, record.id).price) * Number(record.qty)) }) }}</div>
                </template>
              </template>
            </template>
            <template #summary>
              <a-table-summary-row>
                <a-table-summary-cell>{{ $t('scm.tender.totalAndDays') }}</a-table-summary-cell>
                <a-table-summary-cell v-for="b in detail.bids" :key="b.id">
                  <b :class="{ best: b.id === lowestBidId }">¥{{ formatMoney(b.totalAmount) }}</b>
                  <div class="sub-text">{{ b.deliveryDays != null ? $t('scm.tender.days', { n: b.deliveryDays }) : '-' }}{{ b.remark ? ' · ' + b.remark : '' }}</div>
                  <a-button
                    v-if="detail.status === TenderStatus.OPENED"
                    v-perm="'scm:tender:award'"
                    type="link"
                    size="small"
                    class="award-link"
                    @click="openAward(b.supplierId)"
                  >
                    {{ $t('scm.tender.awardTo') }}
                  </a-button>
                </a-table-summary-cell>
              </a-table-summary-row>
            </template>
          </a-table>
        </template>
        <template v-else>
          <div class="section-title">{{ $t('scm.tender.materials') }}</div>
          <a-table row-key="id" size="small" :columns="itemViewColumns" :data-source="detail.items" :pagination="false" class="block">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
              <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
            </template>
          </a-table>
          <a-alert v-if="detail.bids.length" type="info" show-icon :message="$t('scm.tender.bidsReceived', { n: detail.bids.length })" />
        </template>
        <div class="section-title files-title">{{ $t('scm.tender.attachments') }}</div>
        <AttachmentPanel biz-type="scm_tender" :biz-id="detail.id" :editable="userStore.hasPerm('scm:tender:edit')" />
      </template>
    </SubPage>

    <!-- 代录标书 -->
    <a-modal v-model:open="bidOpen" :title="$t('scm.tender.proxyBid')" width="720px" :confirm-loading="saving" @ok="submitBid">
      <a-form layout="vertical">
        <a-row :gutter="12">
          <a-col :span="14">
            <a-form-item :label="$t('scm.tender.bidSupplier')" required>
              <a-select v-model:value="bid.supplierId" :options="invitedOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="10">
            <a-form-item :label="$t('scm.tender.deliveryDays')">
              <a-input-number v-model:value="bid.deliveryDays" :min="0" :addon-after="$t('scm.tender.daysUnit')" class="full" />
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
        <a-form-item :label="$t('scm.tender.bidRemark')" class="bid-remark">
          <a-input v-model:value="bid.remark" :maxlength="500" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 定标 -->
    <a-modal v-model:open="awardOpen" :title="$t('scm.tender.award')" :confirm-loading="saving" @ok="submitAward">
      <a-form layout="vertical">
        <a-form-item :label="$t('scm.tender.awardSupplier')" required>
          <a-select v-model:value="award.supplierId" :options="bidderOptions" />
        </a-form-item>
        <a-form-item :label="$t('scm.tender.awardTarget')" required>
          <a-radio-group v-model:value="award.target">
            <a-radio value="CONTRACT">{{ $t('scm.tender.purchaseContract') }}</a-radio>
            <a-radio value="ORDER">{{ $t('scm.tender.purchaseOrder') }}</a-radio>
            <a-radio value="NONE">{{ $t('scm.tender.none') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="award.target === AwardTarget.ORDER" :label="$t('scm.tender.warehouse')" required>
          <WarehouseSelect v-model:value="award.warehouseId" />
        </a-form-item>
        <a-row v-if="award.target === AwardTarget.CONTRACT" :gutter="12">
          <a-col :span="12">
            <a-form-item :label="$t('scm.tender.contractStart')">
              <a-date-picker v-model:value="award.startDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('scm.tender.contractEnd')">
              <a-date-picker v-model:value="award.endDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('scm.tender.awardReason')">
          <a-input v-model:value="award.reason" :maxlength="255" :placeholder="$t('scm.tender.awardReasonPh')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
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

import { AwardTarget, TenderStatus } from '@/constants/enums'
const { t } = useI18n()

/** 招标采购：邀请供应商投标（门户或代录），截止后开标比价，定标生成采购合同或订单 */
const dict = useDict('scm_tender_status')
const userStore = useUserStore()

const columns = [
  { title: t('scm.tender.colTenderNo'), key: 'tenderNo', width: 240 },
  { title: t('scm.tender.bidDeadline'), key: 'deadline', width: 180 },
  { title: t('scm.tender.colBids'), key: 'bids', width: 100 },
  { title: t('scm.tender.colAward'), key: 'award', width: 220 },
  { title: t('common.field.status'), key: 'status', width: 120 },
  { title: t('scm.tender.creator'), dataIndex: 'createByName', width: 110 },
  { title: t('common.action.operation'), key: 'action', width: 90, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'scm-tender')
const query = reactive({ status: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageTender, query)
onMounted(load)

const passed = (r) => !dayjs(r.deadline).isAfter(dayjs())
function remain(r) {
  const mins = dayjs(r.deadline).diff(dayjs(), 'minute')
  return mins >= 1440
    ? t('scm.tender.remainDays', { n: Math.floor(mins / 1440) })
    : mins >= 60
      ? t('scm.tender.remainHours', { n: Math.floor(mins / 60) })
      : t('scm.tender.remainMinutes', { n: mins })
}

// ---------------------------------------------------------------- 表单
const newItem = () => ({ key: rowKey(), materialId: undefined, qty: undefined, requiredDate: undefined, remark: '' })
const rules = {
  title: [{ required: true, whitespace: true, message: t('scm.tender.inputTitle') }],
  deadline: [{ required: true, message: t('scm.tender.selectDeadline') }],
  supplierIds: [{ required: true, type: 'array', min: 1, message: t('scm.tender.selectSuppliers') }]
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
    const tender = await getTender(record.id)
    supplierOptions.value = tender.suppliers.map((s) => ({ value: s.supplierId, label: `${s.supplierCode} ${s.supplierName}` }))
    return {
      ...tender,
      deadline: tender.deadline.replace(' ', 'T'),
      supplierIds: tender.suppliers.map((s) => s.supplierId),
      items: tender.items.map((i) => ({ key: rowKey(), materialId: i.materialId, materialLabel: `${i.materialCode} ${i.materialName}`,
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
  { title: t('scm.tender.material'), key: 'material', width: 280 },
  { title: t('common.field.quantity'), key: 'qty', width: 130 },
  { title: t('scm.tender.requiredDate'), key: 'requiredDate', width: 150 },
  { title: t('scm.tender.requirement'), key: 'remark', width: 200 },
  { title: '', key: 'action', width: 50 }
]
const itemViewColumns = [
  { title: t('scm.tender.material'), key: 'material' },
  { title: t('common.field.quantity'), key: 'qty', width: 140 },
  { title: t('scm.tender.requiredDate'), dataIndex: 'requiredDate', width: 130 },
  { title: t('scm.tender.requirement'), dataIndex: 'remark', ellipsis: true }
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
const opened = computed(() => [TenderStatus.OPENED, TenderStatus.AWARDED].includes(detail.value?.status))
const canOpen = computed(() => detail.value && (passed(detail.value) || detail.value.suppliers.every((s) => s.bid)))
const compareColumns = computed(() => [
  { title: t('scm.tender.material'), key: 'material', width: 240, fixed: 'left' },
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
  message.success(t('scm.tender.published'))
  refreshDetail()
  load()
}
async function open() {
  await openTender(detail.value.id)
  message.success(t('scm.tender.opened'))
  refreshDetail()
  load()
}
async function cancel() {
  await cancelTender(detail.value.id)
  message.success(t('scm.tender.canceled'))
  refreshDetail()
  load()
}
async function remove() {
  await deleteTender(detail.value.id)
  message.success(t('scm.tender.deleted'))
  detailOpen.value = false
  load()
}

// 代录标书
const saving = ref(false)
const bidOpen = ref(false)
const bid = ref({})
const invitedOptions = computed(() => (detail.value?.suppliers || []).map((s) => ({ value: s.supplierId, label: s.supplierName })))
const bidColumns = [
  { title: t('scm.tender.material'), key: 'material' },
  { title: t('scm.tender.taxPrice'), key: 'price', width: 170 },
  { title: t('scm.tender.taxRate'), key: 'taxRate', width: 120 }
]
function openBid() {
  bid.value = {
    supplierId: undefined, deliveryDays: undefined, remark: '',
    items: detail.value.items.map((i) => ({ tenderItemId: i.id, label: `${i.materialCode} ${i.materialName} × ${formatQty(i.qty)}`, price: undefined, taxRate: 13 }))
  }
  bidOpen.value = true
}
async function submitBid() {
  if (!bid.value.supplierId) return message.warning(t('scm.tender.selectBidSupplier'))
  if (bid.value.items.some((i) => i.price == null)) return message.warning(t('scm.tender.quoteEach'))
  saving.value = true
  try {
    await proxyBid(detail.value.id, bid.value)
    message.success(t('scm.tender.bidSaved'))
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
  if (award.value.target === AwardTarget.ORDER && !award.value.warehouseId) return message.warning(t('scm.tender.selectWarehouse'))
  saving.value = true
  try {
    await awardTender(detail.value.id, award.value)
    message.success(
      award.value.target === AwardTarget.CONTRACT
        ? t('scm.tender.awardedContract')
        : award.value.target === AwardTarget.ORDER
          ? t('scm.tender.awardedOrder')
          : t('scm.tender.awarded')
    )
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
