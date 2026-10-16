<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" placeholder="全部状态" allow-clear :options="toOptions(dict.scm_contract_status)" class="filter" @change="search" />
        <PartnerSelect v-model:value="query.supplierId" type="supplier" allow-clear placeholder="供应商" class="filter-wide" @update:value="search" />
        <a-input-search v-model:value="query.keyword" placeholder="合同编号、名称、供应商" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <a-button v-perm="'scm:contract:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>新增合同
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
        :scroll="{ x: 1150, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'contractNo'">
            <a @click="openDetail(record.id)">{{ record.contractNo }}</a>
            <div class="sub-text">{{ record.name }}</div>
          </template>
          <template v-else-if="column.key === 'type'"><DictTag type="scm_contract_type" :value="record.contractType" /></template>
          <template v-else-if="column.key === 'period'">
            <div>{{ record.startDate }} ~ {{ record.endDate }}</div>
            <div v-if="record.status === 'ACTIVE' && expired(record)" class="sub-text danger">已到期</div>
          </template>
          <template v-else-if="column.key === 'amount'">
            <div>已下单 ¥{{ formatMoney(record.orderedAmount) }}</div>
            <div class="sub-text">{{ record.amountLimit != null ? `上限 ¥${formatMoney(record.amountLimit)}` : '金额不限' }}</div>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="scm_contract_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">查看</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增采购合同" edit-title="编辑采购合同" width="min(1100px, 100vw)" page>
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="合同名称" name="name">
              <a-input v-model:value="form.name" :maxlength="128" placeholder="如：2026 年度包装材料框架协议" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="供应商" name="supplierId">
              <PartnerSelect v-model:value="form.supplierId" type="supplier" :label="form.supplierName" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="合同类型" name="contractType" :extra="TYPE_TIPS[form.contractType]">
              <a-radio-group v-model:value="form.contractType" :options="toOptions(dict.scm_contract_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="签订日期" name="signDate">
              <a-date-picker v-model:value="form.signDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="金额上限" name="amountLimit" extra="价税合计，为空不限">
              <a-input-number v-model:value="form.amountLimit" :min="0.01" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="生效日期" name="startDate">
              <a-date-picker v-model:value="form.startDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="到期日期" name="endDate">
              <a-date-picker v-model:value="form.endDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="4">
            <a-form-item label="付款天数" name="paymentDays" extra="应付单按此计算到期日">
              <a-input-number v-model:value="form.paymentDays" :min="0" :precision="0" addon-after="天" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="8">
            <a-form-item label="付款条款" name="paymentTerms">
              <a-input v-model:value="form.paymentTerms" :maxlength="255" placeholder="如：月结 30 天，承兑汇票" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-input v-model:value="form.remark" :maxlength="500" />
            </a-form-item>
          </a-col>
        </a-row>

        <div class="items-head">
          <span class="section-title">合同物料（采购订单引用合同时按合同价下单）</span>
          <a-button type="primary" ghost size="small" @click="addItem">
            <template #icon><PlusOutlined /></template>添加物料
          </a-button>
        </div>
        <a-table row-key="key" size="small" :columns="itemColumns" :data-source="form.items" :pagination="false" :scroll="{ x: 900 }">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'material'">
              <a-form-item :name="['items', index, 'materialId']" :rules="[{ required: true, message: '请选择物料' }]" class="cell-item">
                <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'price'">
              <a-form-item :name="['items', index, 'price']" :rules="[{ required: true, message: '请填写单价' }]" class="cell-item">
                <a-input-number v-model:value="record.price" :min="0" :precision="4" prefix="¥" class="full" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'taxRate'">
              <a-input-number v-model:value="record.taxRate" :min="0" :max="100" addon-after="%" class="full" />
            </template>
            <template v-else-if="column.key === 'qtyLimit'">
              <a-form-item
                :name="['items', index, 'qtyLimit']"
                :rules="form.contractType === 'FIXED' ? [{ required: true, message: '请填写数量' }] : []"
                class="cell-item"
              >
                <a-input-number v-model:value="record.qtyLimit" :min="0" :placeholder="form.contractType === 'FIXED' ? '' : '不限'" class="full" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'remark'">
              <a-input v-model:value="record.remark" :maxlength="255" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" danger size="small" @click="form.items.splice(index, 1)"><DeleteOutlined /></a-button>
            </template>
          </template>
        </a-table>
      </a-form>
    </FormDrawer>

    <SubPage :open="detailOpen" :title="detail ? `采购合同 ${detail.contractNo}` : ''" width="min(1000px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space>
          <a-button v-if="detail.flowInstanceId" @click="flowOpen = true">审批记录</a-button>
          <template v-if="detail.status === 'DRAFT'">
            <a-button v-perm="'scm:contract:edit'" @click="drawer.openEdit(detail)">编辑</a-button>
            <span v-perm="'scm:contract:remove'">
              <a-popconfirm title="删除该合同？" @confirm="remove">
                <a-button danger>删除</a-button>
              </a-popconfirm>
            </span>
            <span v-perm="'scm:contract:approve'">
              <a-popconfirm title="提交合同？发布了审批流程时进入审批，否则直接生效" @confirm="submit">
                <a-button type="primary">提交</a-button>
              </a-popconfirm>
            </span>
          </template>
          <a-button v-if="detail.status === 'ACTIVE'" v-perm="'scm:contract:close'" @click="openClose">关闭合同</a-button>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item label="合同名称" :span="2">{{ detail.name }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <DictTag type="scm_contract_status" :value="detail.status" />
            <span v-if="detail.status === 'ACTIVE' && expired(detail)" class="danger">已到期</span>
          </a-descriptions-item>
          <a-descriptions-item label="供应商">{{ detail.supplierCode }} {{ detail.supplierName }}</a-descriptions-item>
          <a-descriptions-item label="类型"><DictTag type="scm_contract_type" :value="detail.contractType" /></a-descriptions-item>
          <a-descriptions-item label="签订日期">{{ detail.signDate }}</a-descriptions-item>
          <a-descriptions-item label="期限">{{ detail.startDate }} ~ {{ detail.endDate }}</a-descriptions-item>
          <a-descriptions-item label="金额">
            已下单 ¥{{ formatMoney(detail.orderedAmount) }}
            <span class="sub-text">{{ detail.amountLimit != null ? ` / 上限 ¥${formatMoney(detail.amountLimit)}` : '（不限）' }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="付款">{{ detail.paymentDays != null ? `${detail.paymentDays} 天` : '按供应商账期' }} {{ detail.paymentTerms || '' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.tenderNo" label="来源招标">{{ detail.tenderNo }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.closeReason" label="关闭原因">{{ detail.closeReason }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="备注" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <a-tabs>
          <a-tab-pane key="items" tab="物料与执行">
            <a-table row-key="id" size="small" :columns="usageColumns" :data-source="detail.items" :pagination="false" :scroll="{ x: 860 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'material'">
                  {{ record.materialCode }} {{ record.materialName }}
                  <div class="sub-text">{{ record.spec }}</div>
                </template>
                <template v-else-if="column.key === 'price'">¥{{ Number(record.price) }}<div class="sub-text">税率 {{ Number(record.taxRate) }}%</div></template>
                <template v-else-if="column.key === 'progress'">
                  <template v-if="record.qtyLimit != null">
                    <a-progress :percent="pct(record.orderedQty, record.qtyLimit)" size="small" />
                    <div class="sub-text">已下单 {{ formatQty(record.orderedQty) }} / {{ formatQty(record.qtyLimit) }} {{ record.unit }}</div>
                  </template>
                  <span v-else>已下单 {{ formatQty(record.orderedQty) }} {{ record.unit }}<span class="sub-text">（不限）</span></span>
                </template>
                <template v-else-if="column.key === 'received'">{{ formatQty(record.receivedQty) }}</template>
                <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.orderedAmount) }}</template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="orders" :tab="`采购订单（${orders.length}）`">
            <a-table row-key="id" size="small" :columns="orderColumns" :data-source="orders" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'"><DictTag type="scm_order_status" :value="record.status" /></template>
                <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.totalAmount) }}</template>
                <template v-else-if="column.key === 'qty'">{{ formatQty(record.doneQty) }} / {{ formatQty(record.totalQty) }}</template>
                <template v-else-if="column.key === 'confirm'">
                  <DictTag v-if="record.supplierConfirm" type="scm_supplier_confirm" :value="record.supplierConfirm" />
                  <span v-else class="sub-text">-</span>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="files" tab="附件">
            <AttachmentPanel biz-type="scm_contract" :biz-id="detail.id" :editable="userStore.hasPerm('scm:contract:edit')" />
          </a-tab-pane>
        </a-tabs>
      </template>
    </SubPage>

    <a-modal v-model:open="closeOpen" title="关闭合同" @ok="submitClose">
      <a-alert type="info" show-icon class="block" message="关闭后采购订单不能再引用该合同，已下单的订单不受影响" />
      <a-textarea v-model:value="closeReason" :rows="3" :maxlength="255" placeholder="关闭原因" />
    </a-modal>

    <InstanceDetailDrawer v-model:open="flowOpen" :instance-id="detail?.flowInstanceId" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import InstanceDetailDrawer from '@/components/workflow/InstanceDetailDrawer.vue'
import { addContract, closeContract, contractOrders, deleteContract, getContract, pageContract, submitContract, updateContract } from '@/api/scm/srm'
import { formatMoney } from '@/components/scm/scm'
import { formatQty, rowKey, today } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { useUserStore } from '@/store/user'

/** 采购合同 / 框架协议：生效后采购订单可引用，按合同价下单并校验数量、金额上限 */
const dict = useDict('scm_contract_type', 'scm_contract_status')
const userStore = useUserStore()
const TYPE_TIPS = {
  FRAMEWORK: '约定价格与期限，数量按订单执行（可设数量上限、金额上限）',
  FIXED: '每个物料约定数量，订单累计不超过合同数量'
}

const columns = [
  { title: '合同编号 / 名称', key: 'contractNo', width: 240 },
  { title: '供应商', dataIndex: 'supplierName', width: 200, ellipsis: true },
  { title: '类型', key: 'type', width: 120 },
  { title: '期限', key: 'period', width: 200 },
  { title: '执行金额', key: 'amount', width: 170 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'scm-contract')
const query = reactive({ status: undefined, supplierId: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageContract, query)
onMounted(load)

const expired = (c) => dayjs(c.endDate).isBefore(dayjs(), 'day')
const pct = (a, b) => (Number(b) ? Math.min(100, Math.round((Number(a) / Number(b)) * 100)) : 0)

// ---------------------------------------------------------------- 表单
const newItem = () => ({ key: rowKey(), materialId: undefined, price: undefined, taxRate: 13, qtyLimit: undefined, remark: '' })
const rules = {
  name: [{ required: true, whitespace: true, message: '请填写合同名称' }],
  supplierId: [{ required: true, message: '请选择供应商' }],
  contractType: [{ required: true, message: '请选择合同类型' }],
  signDate: [{ required: true, message: '请选择签订日期' }],
  startDate: [{ required: true, message: '请选择生效日期' }],
  endDate: [{ required: true, message: '请选择到期日期' }]
}
const num = (v) => (v == null ? undefined : Number(v))
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined, name: '', supplierId: undefined, contractType: 'FRAMEWORK', signDate: today(), startDate: today(),
    endDate: dayjs().add(1, 'year').subtract(1, 'day').format('YYYY-MM-DD'), amountLimit: undefined, paymentDays: undefined,
    paymentTerms: '', remark: '', items: [newItem()]
  }),
  fetch: async (record) => {
    const c = await getContract(record.id)
    return {
      ...c,
      supplierName: `${c.supplierCode} ${c.supplierName}`,
      amountLimit: num(c.amountLimit),
      items: c.items.map((i) => ({
        key: rowKey(), materialId: i.materialId, materialLabel: `${i.materialCode} ${i.materialName}`, price: num(i.price),
        taxRate: num(i.taxRate), qtyLimit: num(i.qtyLimit), remark: i.remark
      }))
    }
  },
  create: addContract,
  update: updateContract,
  toPayload: (f) => ({
    name: f.name, supplierId: f.supplierId, contractType: f.contractType, signDate: f.signDate, startDate: f.startDate, endDate: f.endDate,
    amountLimit: f.amountLimit ?? null, paymentDays: f.paymentDays ?? null, paymentTerms: f.paymentTerms || null, remark: f.remark || null,
    items: f.items.map((i) => ({ materialId: i.materialId, price: i.price, taxRate: i.taxRate ?? 13, qtyLimit: i.qtyLimit ?? null, remark: i.remark || null }))
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
  { title: '合同单价（含税）', key: 'price', width: 170 },
  { title: '税率', key: 'taxRate', width: 110 },
  { title: '数量上限', key: 'qtyLimit', width: 140 },
  { title: '备注', key: 'remark', width: 160 },
  { title: '', key: 'action', width: 50 }
]

// ---------------------------------------------------------------- 详情
const usageColumns = [
  { title: '物料', key: 'material', width: 240 },
  { title: '合同价', key: 'price', width: 130 },
  { title: '执行进度', key: 'progress', width: 240 },
  { title: '已入库', key: 'received', width: 100, align: 'right' },
  { title: '已下单金额', key: 'amount', width: 130, align: 'right' }
]
const orderColumns = [
  { title: '订单号', dataIndex: 'orderNo', width: 170 },
  { title: '订单日期', dataIndex: 'orderDate', width: 110 },
  { title: '金额', key: 'amount', width: 130, align: 'right' },
  { title: '入库 / 订单数量', key: 'qty', width: 150 },
  { title: '供应商确认', key: 'confirm', width: 110 },
  { title: '状态', key: 'status', width: 100 }
]
const detailOpen = ref(false)
const detail = ref()
const orders = ref([])
const flowOpen = ref(false)
async function openDetail(id) {
  const [c, o] = await Promise.all([getContract(id), contractOrders(id)])
  detail.value = c
  orders.value = o
  detailOpen.value = true
}
async function refreshDetail() {
  if (detail.value) await openDetail(detail.value.id)
}
async function submit() {
  const approving = await submitContract(detail.value.id)
  message.success(approving ? '已提交审批' : '合同已生效')
  refreshDetail()
  load()
}
async function remove() {
  await deleteContract(detail.value.id)
  message.success('已删除')
  detailOpen.value = false
  load()
}
const closeOpen = ref(false)
const closeReason = ref('')
function openClose() {
  closeReason.value = ''
  closeOpen.value = true
}
async function submitClose() {
  if (!closeReason.value.trim()) return message.warning('请填写关闭原因')
  await closeContract(detail.value.id, closeReason.value)
  message.success('已关闭')
  closeOpen.value = false
  refreshDetail()
  load()
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
  margin-left: 4px;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0;
}
.section-title {
  font-weight: 600;
}
.cell-item {
  margin-bottom: 0;
}
</style>
