<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-segmented v-model:value="tab" :options="TABS" />
        <a-input-search v-model:value="query.keyword" placeholder="订单号、物料" allow-clear class="filter-wide" @search="search" />
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1000, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'orderNo'">
            <a @click="openDetail(record.id)">{{ record.orderNo }}</a>
            <div v-if="record.contractNo" class="sub-text">合同 {{ record.contractNo }}</div>
          </template>
          <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.totalAmount) }}</template>
          <template v-else-if="column.key === 'progress'">
            <a-progress :percent="pct(record.doneQty, record.totalQty)" size="small" />
            <div class="sub-text">已收货 {{ formatQty(record.doneQty) }} / {{ formatQty(record.totalQty) }}</div>
          </template>
          <template v-else-if="column.key === 'confirm'">
            <template v-if="record.supplierConfirm">
              <DictTag type="scm_supplier_confirm" :value="record.supplierConfirm" />
              <div v-if="record.promisedDate" class="sub-text">承诺 {{ record.promisedDate }}</div>
            </template>
            <a-tag v-else color="orange" :bordered="false">待确认</a-tag>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="scm_order_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">{{ !record.supplierConfirm && executable(record) ? '确认' : '查看' }}</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <SubPage :open="detailOpen" :title="detail ? `采购订单 ${detail.orderNo}` : ''" width="min(1000px, 100vw)" @close="detailOpen = false">
      <template v-if="detail && executable(detail)" #extra>
        <a-space>
          <a-button v-if="detail.supplierConfirm !== 'REJECTED'" v-perm="'portal:asn:add'" @click="openAsn">发起送货</a-button>
          <span v-perm="'portal:order:confirm'">
            <a-space>
              <a-button danger @click="openConfirm(false)">拒绝</a-button>
              <a-button type="primary" @click="openConfirm(true)">{{ detail.supplierConfirm ? '重新确认' : '接受订单' }}</a-button>
            </a-space>
          </span>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item label="订单日期">{{ detail.orderDate }}</a-descriptions-item>
          <a-descriptions-item label="要求交货">{{ detail.deliveryDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="状态"><DictTag type="scm_order_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item label="收货仓库">{{ detail.warehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="价税合计"><b>¥{{ formatMoney(detail.totalAmount) }}</b></a-descriptions-item>
          <a-descriptions-item label="采购员">{{ detail.createByName }}</a-descriptions-item>
          <a-descriptions-item label="确认">
            <template v-if="detail.supplierConfirm">
              <DictTag type="scm_supplier_confirm" :value="detail.supplierConfirm" />
              {{ detail.promisedDate ? '承诺交期 ' + detail.promisedDate : '' }} {{ detail.confirmRemark || '' }}
            </template>
            <span v-else>待确认</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.contractNo" label="采购合同">{{ detail.contractNo }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="备注">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false" :scroll="{ x: 800 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}<div class="sub-text">{{ record.spec }}</div></template>
            <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
            <template v-else-if="column.key === 'done'">{{ formatQty(Number(record.doneQty) - Number(record.returnQty || 0)) }}</template>
            <template v-else-if="column.key === 'price'">¥{{ Number(record.price) }}</template>
            <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
          </template>
        </a-table>
      </template>
    </SubPage>

    <a-modal v-model:open="confirmOpen" :title="confirm.accept ? '接受订单' : '拒绝订单'" :confirm-loading="saving" @ok="submitConfirm">
      <a-form layout="vertical">
        <a-form-item v-if="confirm.accept" label="承诺交期" required>
          <a-date-picker v-model:value="confirm.promisedDate" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item :label="confirm.accept ? '说明' : '拒绝原因'" :required="!confirm.accept">
          <a-textarea v-model:value="confirm.remark" :rows="3" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>

    <SubPage :open="asnOpen" :title="`发起送货 - ${detail?.orderNo || ''}`" width="min(900px, 100vw)" @close="asnOpen = false">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="12" :sm="6">
            <a-form-item label="发货日期" required>
              <a-date-picker v-model:value="asn.shipDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="预计到货">
              <a-date-picker v-model:value="asn.expectedDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="承运商"><a-input v-model:value="asn.carrier" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="运单号"><a-input v-model:value="asn.trackingNo" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注"><a-input v-model:value="asn.remark" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <a-table row-key="orderItemId" size="small" :columns="asnColumns" :data-source="asn.lines" :pagination="false" :scroll="{ x: 700 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">{{ record.label }}</template>
          <template v-else-if="column.key === 'open'">{{ formatQty(record.openQty) }} {{ record.unit }}</template>
          <template v-else-if="column.key === 'qty'">
            <a-input-number v-model:value="record.qty" :min="0" :max="record.openQty" class="full" />
          </template>
          <template v-else-if="column.key === 'batchNo'">
            <a-input v-model:value="record.batchNo" :maxlength="64" :placeholder="record.batchEnabled ? '批次管理物料必填' : '选填'" />
          </template>
        </template>
      </a-table>
      <template #footer>
        <div class="drawer-footer">
          <a-button @click="asnOpen = false">取消</a-button>
          <a-button type="primary" :loading="saving" @click="submitAsn">提交送货通知</a-button>
        </div>
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { addPortalAsn, confirmPortalOrder, getPortalOrder, orderOpenLines, pagePortalOrder } from '@/api/portal'
import { formatMoney } from '@/components/scm/scm'
import { formatQty, today } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'

/** 供应商门户 - 我的订单：确认订单（承诺交期）、发起送货 */
const TABS = [
  { label: '待确认', value: 'pending' },
  { label: '全部订单', value: 'all' }
]
const tab = ref('pending')
const columns = [
  { title: '订单号', key: 'orderNo', width: 180 },
  { title: '订单日期', dataIndex: 'orderDate', width: 110 },
  { title: '要求交货', dataIndex: 'deliveryDate', width: 110 },
  { title: '金额', key: 'amount', width: 130, align: 'right' },
  { title: '收货进度', key: 'progress', width: 180 },
  { title: '确认', key: 'confirm', width: 130 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'portal-order')
const query = reactive({ pending: true, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pagePortalOrder, query)
watch(tab, (t) => {
  query.pending = t === 'pending' ? true : undefined
  search()
})
onMounted(load)

const executable = (o) => ['APPROVED', 'PARTIAL'].includes(o.status)
const pct = (a, b) => (Number(b) ? Math.min(100, Math.round((Number(a) / Number(b)) * 100)) : 0)
const itemColumns = [
  { title: '物料', key: 'material', width: 240 },
  { title: '数量', key: 'qty', width: 120 },
  { title: '已收货', key: 'done', width: 100 },
  { title: '单价（含税）', key: 'price', width: 120, align: 'right' },
  { title: '金额', key: 'amount', width: 120, align: 'right' },
  { title: '交货日期', dataIndex: 'deliveryDate', width: 110 }
]

const detailOpen = ref(false)
const detail = ref()
async function openDetail(id) {
  detail.value = await getPortalOrder(id)
  detailOpen.value = true
}

const saving = ref(false)
const confirmOpen = ref(false)
const confirm = ref({})
function openConfirm(accept) {
  confirm.value = { accept, promisedDate: detail.value.promisedDate || detail.value.deliveryDate, remark: '' }
  confirmOpen.value = true
}
async function submitConfirm() {
  if (confirm.value.accept && !confirm.value.promisedDate) return message.warning('请填写承诺交期')
  if (!confirm.value.accept && !confirm.value.remark?.trim()) return message.warning('请填写拒绝原因')
  saving.value = true
  try {
    await confirmPortalOrder(detail.value.id, confirm.value)
    message.success(confirm.value.accept ? '已接受订单' : '已拒绝订单')
    confirmOpen.value = false
    detail.value = await getPortalOrder(detail.value.id)
    load()
  } finally {
    saving.value = false
  }
}

const asnOpen = ref(false)
const asn = ref({})
const asnColumns = [
  { title: '物料', key: 'material', width: 240 },
  { title: '可送货', key: 'open', width: 120 },
  { title: '本次送货', key: 'qty', width: 140 },
  { title: '批次号', key: 'batchNo', width: 160 }
]
async function openAsn() {
  const open = new Map((await orderOpenLines(detail.value.id)).map((l) => [l.orderItemId, Number(l.openQty)]))
  asn.value = {
    shipDate: today(), expectedDate: undefined, carrier: '', trackingNo: '', remark: '',
    lines: detail.value.items
      .map((i) => ({ orderItemId: i.id, label: `${i.materialCode} ${i.materialName}`, unit: i.unit, batchEnabled: i.batchEnabled === 1,
        openQty: open.get(i.id) || 0, qty: open.get(i.id) || 0, batchNo: '' }))
      .filter((l) => l.openQty > 0)
  }
  if (!asn.value.lines.length) return message.info('订单已全部送货或收货')
  asnOpen.value = true
}
async function submitAsn() {
  const items = asn.value.lines.filter((l) => Number(l.qty) > 0)
  if (!items.length) return message.warning('请填写送货数量')
  saving.value = true
  try {
    await addPortalAsn({
      orderId: detail.value.id, shipDate: asn.value.shipDate, expectedDate: asn.value.expectedDate || null, carrier: asn.value.carrier || null,
      trackingNo: asn.value.trackingNo || null, remark: asn.value.remark || null,
      items: items.map((l) => ({ orderItemId: l.orderItemId, qty: l.qty, batchNo: l.batchNo || null }))
    })
    message.success('已提交送货通知，仓库将按通知收货')
    asnOpen.value = false
  } finally {
    saving.value = false
  }
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
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
