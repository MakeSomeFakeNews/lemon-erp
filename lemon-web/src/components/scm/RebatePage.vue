<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-input-search v-model:value="query.keyword" :placeholder="tx('searchPlaceholder')" allow-clear class="filter-wide" @search="search" />
        <a-select v-model:value="query.status" :placeholder="$t('scm.rebate.allStatus')" allow-clear :options="toOptions(dict.scm_rebate_status)" class="filter" @change="search" />
        <template #extra>
          <a-button v-perm="perm('add')" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>{{ tx('add') }}
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
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'no'">
            <a @click="openDetail(record.id)">{{ record.rebateNo }}</a>
            <div class="sub-text">{{ partnerName(record) }}</div>
          </template>
          <template v-else-if="column.key === 'period'">{{ record.beginDate }} ~ {{ record.endDate }}</template>
          <template v-else-if="column.key === 'mode'">
            <DictTag type="scm_rebate_calc" :value="record.calcMode" />
            <div v-if="!purchase" class="sub-text"><DictTag type="scm_rebate_settle" :value="record.settleMode" /></div>
          </template>
          <template v-else-if="column.key === 'result'">
            <template v-if="record.status === RebateStatus.SETTLED">
              ¥{{ formatMoney(record.rebateAmount) }}
              <div v-if="record.rate != null" class="sub-text">{{ tx('timesRate', { sales: formatMoney(record.salesAmount), rate: Number(record.rate) }) }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="scm_rebate_status" :value="record.status" />
            <div v-if="record.billNo" class="sub-text">{{ tx('creditNoteNo', { no: record.billNo }) }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="[{ type: 'view', onClick: () => openDetail(record.id) }]" />
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="tx('add')" :edit-title="tx('edit')" width="min(760px, 100vw)">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item v-if="purchase" :label="$t('scm.rebate.supplier')" name="supplierId">
          <PartnerSelect v-model:value="form.supplierId" type="supplier" :label="form.supplierName" />
        </a-form-item>
        <a-form-item v-else :label="$t('scm.rebate.customer')" name="customerId">
          <PartnerSelect v-model:value="form.customerId" type="customer" :label="form.customerName" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('common.field.beginDate')" name="beginDate">
              <a-date-picker v-model:value="form.beginDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('common.field.endDate')" name="endDate">
              <a-date-picker v-model:value="form.endDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('scm.rebate.settleMode')" name="settleMode">
          <a-radio-group v-if="!purchase" v-model:value="form.settleMode">
            <a-radio v-for="o in dict.scm_rebate_settle" :key="o.value" :value="o.value">{{ o.label }}</a-radio>
          </a-radio-group>
          <span v-else>{{ $t('scm.rebate.purchaseCreditNote') }}</span>
          <div class="sub-text">
            {{ purchase ? $t('scm.rebate.purchaseCreditTip') : form.settleMode === RebateSettle.CREDIT_NOTE ? $t('scm.rebate.creditNoteTip') : $t('scm.rebate.balanceTip') }}
          </div>
        </a-form-item>
        <a-form-item :label="$t('scm.rebate.calcMode')" name="calcMode">
          <a-radio-group v-model:value="form.calcMode" button-style="solid">
            <a-radio-button v-for="o in dict.scm_rebate_calc" :key="o.value" :value="o.value">{{ o.label }}</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <template v-if="form.calcMode === RebateCalcMode.ITEM">
          <div class="section-title">
            {{ $t('scm.rebate.items') }} <span class="sub-text">{{ $t('scm.rebate.itemsTip') }}</span>
            <span class="items-total">{{ $t('scm.rebate.itemsTotal', { amount: formatMoney(itemsTotal) }) }}</span>
          </div>
          <LineItemsTable :items="form.items" :columns="itemColumns" :min-rows="1">
            <template #cell="{ column, record }">
              <a-input v-if="column.key === 'itemName'" v-model:value="record.itemName" :maxlength="64" :placeholder="$t('scm.rebate.itemNamePh')" />
              <a-select v-else-if="column.key === 'brand'" v-model:value="record.brand" :options="toOptions(dict.wms_material_brand)" allow-clear class="full" />
              <MaterialCategorySelect v-else-if="column.key === 'category'" v-model="record.category" usable-only class="full" />
              <a-input-number v-else-if="column.key === 'amount'" v-model:value="record.amount" :min="0.01" :precision="2" prefix="¥" class="full" />
            </template>
          </LineItemsTable>
          <a-button type="dashed" block class="add-line" @click="items.addRow()">
            <template #icon><PlusOutlined /></template>{{ $t('scm.rebate.addItem') }}
          </a-button>
        </template>
        <template v-else>
          <div class="section-title">{{ $t('scm.rebate.tiers') }} <span class="sub-text">{{ tx('tiersTip') }}</span></div>
          <LineItemsTable :items="form.tiers" :columns="tierColumns" :min-rows="1">
            <template #cell="{ column, record }">
              <a-input-number v-if="column.key === 'minAmount'" v-model:value="record.minAmount" :min="0" :precision="2" prefix="¥" class="full" />
              <a-input-number v-else-if="column.key === 'rate'" v-model:value="record.rate" :min="0.01" :max="100" :precision="2" addon-after="%" class="full" />
            </template>
          </LineItemsTable>
          <a-button type="dashed" block class="add-line" @click="form.tiers.push(newTier())">
            <template #icon><PlusOutlined /></template>{{ $t('scm.rebate.addTier') }}
          </a-button>
        </template>
        <a-form-item :label="$t('common.field.description')" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </FormDrawer>

    <RebateDetail ref="detailRef" :kind="kind" :api="api" @edit="(d) => drawer.openEdit(d)" @changed="load" />
  </div>
</template>

<script setup>
import MaterialCategorySelect from '@/components/wms/MaterialCategorySelect.vue'
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import LineItemsTable from '@/components/common/LineItemsTable.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import RebateDetail from '@/components/scm/RebateDetail.vue'
import { formatMoney } from '@/components/scm/scm'
import { rebateApi, useRebateText } from '@/components/scm/rebate'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { rowKey, useLineItems } from '@/composables/useLineItems'
import { useTable } from '@/composables/useTable'
import { useRouteOpen } from '@/composables/useRouteOpen'
import { toOptions } from '@/utils/dict'
import { RebateCalcMode, RebateKind, RebateSettle, RebateStatus } from '@/constants/enums'

/**
 * 返利协议（销售返利 / 采购返利共用）：客户或供应商 × 期间，阶梯比例或按项目填写金额，期间结束后结算。
 * 销售返利生成红字应收或计入返利余额；采购返利生成红字应付，付款时与欠款冲抵
 */
const props = defineProps({
  kind: { type: String, required: true }
})
const { t } = useI18n()
const purchase = props.kind === RebateKind.PURCHASE
const api = rebateApi(props.kind)
const tx = useRebateText(props.kind)
const perm = (action) => `${purchase ? 'scm:purchaseRebate' : 'scm:rebate'}:${action}`
const partnerName = (r) => (purchase ? r.supplierName : r.customerName)
const dict = useDict('scm_rebate_status', 'scm_rebate_settle', 'scm_rebate_calc', 'wms_material_brand', 'wms_material_category')

const columns = [
  { title: t('scm.rebate.agreement'), key: 'no', width: 220 },
  { title: t('scm.rebate.period'), key: 'period', width: 200 },
  { title: t('scm.rebate.calcMode'), key: 'mode', width: 150 },
  { title: t('scm.rebate.rebate'), key: 'result', width: 200 },
  { title: t('common.field.status'), key: 'status', width: 160 },
  actionColumn(1, [t('common.action.view')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, purchase ? 'scm-purchase-rebate' : 'scm-rebate')
const query = reactive({ keyword: undefined, status: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(api.page, query)
onMounted(load)

const newTier = () => ({ key: rowKey(), minAmount: undefined, rate: undefined })
const newItem = () => ({ key: rowKey(), itemName: '', brand: undefined, category: undefined, amount: undefined })
const tierColumns = [
  { title: tx('reach'), key: 'minAmount' },
  { title: t('scm.rebate.rate'), key: 'rate', width: 160 },
  { title: '', key: 'action', width: 50 }
]
const itemColumns = [
  { title: t('scm.rebate.itemName'), key: 'itemName', width: 200 },
  { title: t('scm.rebate.brand'), key: 'brand', width: 130 },
  { title: t('scm.rebate.category'), key: 'category', width: 130 },
  { title: t('scm.rebate.itemAmount'), key: 'amount', width: 150 },
  { title: '', key: 'action', width: 50 }
]
const rules = {
  customerId: [{ required: !purchase, message: t('scm.rebate.ruleCustomer') }],
  supplierId: [{ required: purchase, message: t('scm.rebate.ruleSupplier') }],
  beginDate: [{ required: true, message: t('common.rule.requiredSelect', { name: t('common.field.beginDate') }) }],
  endDate: [{ required: true, message: t('common.rule.requiredSelect', { name: t('common.field.endDate') }) }],
  settleMode: [{ required: true, message: t('scm.rebate.ruleSettleMode') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, customerId: undefined, supplierId: undefined, beginDate: undefined, endDate: undefined,
    settleMode: RebateSettle.CREDIT_NOTE, calcMode: RebateCalcMode.TIER, remark: '', tiers: [newTier()], items: [newItem()] }),
  fetch: async (record) => {
    const r = await api.get(record.id)
    return {
      ...r,
      tiers: r.tiers.length ? r.tiers.map((x) => ({ key: rowKey(), minAmount: Number(x.minAmount), rate: Number(x.rate) })) : [newTier()],
      items: r.items.length ? r.items.map((x) => ({ key: rowKey(), itemName: x.itemName, brand: x.brand || undefined, category: x.category || undefined, amount: Number(x.amount) })) : [newItem()]
    }
  },
  create: async (data) => {
    const id = await api.add(data)
    detailRef.value.open(id)
  },
  update: api.update,
  toPayload: (f) => ({
    customerId: purchase ? null : f.customerId,
    supplierId: purchase ? f.supplierId : null,
    beginDate: f.beginDate,
    endDate: f.endDate,
    settleMode: purchase ? RebateSettle.CREDIT_NOTE : f.settleMode,
    calcMode: f.calcMode,
    remark: f.remark || null,
    tiers: f.calcMode === RebateCalcMode.TIER ? f.tiers.filter((x) => x.minAmount != null || x.rate != null).map((x) => ({ minAmount: x.minAmount, rate: x.rate })) : [],
    items: f.calcMode === RebateCalcMode.ITEM
      ? f.items.filter((x) => x.itemName || x.amount).map((x) => ({ itemName: x.itemName, brand: x.brand || null, category: x.category || null, amount: x.amount }))
      : []
  }),
  onSuccess: () => {
    load()
    detailRef.value.reload()
  }
})
const { formRef, form } = drawer
const items = useLineItems(() => form.value.items, { newRow: newItem })
const itemsTotal = items.total('amount')

const detailRef = ref()
const openDetail = (id) => detailRef.value.open(id)
// 从应收应付单的来源单号跳转过来（?id=返利协议ID）时打开详情
useRouteOpen({ id: (id) => openDetail(id) })
</script>

<style scoped>
.filter {
  width: 130px;
}
.filter-wide {
  width: 200px;
}
.full {
  width: 100%;
}
.add-line {
  margin: 8px 0 16px;
}
.section-title {
  margin: 12px 0 8px;
  font-weight: 600;
}
.items-total {
  float: right;
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
  font-weight: normal;
}
</style>
