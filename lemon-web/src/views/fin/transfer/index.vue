<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" @change="search" />
        <a-input-search v-model:value="query.keyword" :placeholder="$t('fin.transfer.keywordPh')" allow-clear class="filter" @search="search" />
        <template #extra>
          <a-button v-perm="'fin:transfer:edit'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>{{ $t('fin.transfer.add') }}
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
          <template v-if="column.key === 'transferNo'">
            <a @click="openView(record)">{{ record.transferNo }}</a>
          </template>
          <template v-else-if="column.key === 'flow'">
            <span>{{ record.fromAccountName }}</span>
            <ArrowRightOutlined class="arrow" />
            <span>{{ record.toAccountName }}</span>
          </template>
          <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
          <template v-else-if="column.key === 'fee'">
            <span v-if="Number(record.fee)">¥{{ formatMoney(record.fee) }}</span>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'feeBearer'">
            <DictTag v-if="Number(record.fee)" type="fin_fee_bearer" :value="record.feeBearer" />
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'voucher'">
            <a v-if="record.voucherId" @click="openVoucher(record.voucherId)">{{ record.voucherNo }}</a>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[{ type: 'delete', perm: 'fin:transfer:edit', confirm: $t('fin.transfer.confirmDelete', { no: record.transferNo }), onClick: () => remove(record.id) }]"
            />
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('fin.transfer.createTitle')" width="min(640px, 100vw)">
      <a-alert type="info" show-icon class="form-tip" :message="$t('fin.transfer.tip')" />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.transfer.date')" name="transferDate">
              <a-date-picker v-model:value="form.transferDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.amount')" name="amount">
              <a-input-number v-model:value="form.amount" :min="0.01" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.transfer.from')" name="fromAccountId">
              <AccountSelect v-model:value="form.fromAccountId" :roles="['CASH', 'BANK']" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.transfer.to')" name="toAccountId">
              <AccountSelect v-model:value="form.toAccountId" :roles="['CASH', 'BANK']" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.transfer.fee')" name="fee" :extra="$t('fin.transfer.feeExtra')">
              <a-input-number v-model:value="form.fee" :min="0" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <!-- 手续费支付账户始终显示（默认转出账户支付），手续费为 0 时不起作用 -->
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.transfer.feeBearer')" name="feeBearer" :extra="feeBearerText">
              <a-select v-model:value="form.feeBearer" :options="toOptions(dict.fin_fee_bearer)" :disabled="!hasFee" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.transfer.handler')" name="handlerId">
              <IdentityUserSelect v-model:value="form.handlerId" :placeholder="$t('fin.transfer.optional')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.transfer.ticketNo')" name="ticketNo">
              <a-input v-model:value="form.ticketNo" :maxlength="64" :placeholder="$t('fin.transfer.ticketNoPh')" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('common.field.remark')" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" :placeholder="$t('fin.transfer.remarkPh')" />
        </a-form-item>
      </a-form>
    </FormDrawer>

    <!-- 查看 -->
    <a-modal v-model:open="viewOpen" :title="viewing ? $t('fin.transfer.viewTitle', { no: viewing.transferNo }) : ''" :footer="null" width="min(760px, 100vw)">
      <a-descriptions v-if="viewing" :column="{ xs: 1, sm: 2 }" size="small" bordered>
        <a-descriptions-item :label="$t('fin.transfer.transferNo')">{{ viewing.transferNo }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.transfer.date')">{{ viewing.transferDate }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.transfer.from')">{{ viewing.fromAccountName }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.transfer.to')">{{ viewing.toAccountName }}</a-descriptions-item>
        <a-descriptions-item :label="$t('common.field.amount')"><b>¥{{ formatMoney(viewing.amount) }}</b></a-descriptions-item>
        <a-descriptions-item :label="$t('fin.transfer.fee')">¥{{ formatMoney(viewing.fee) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.transfer.feeBearer')">
          <template v-if="Number(viewing.fee)">
            <DictTag type="fin_fee_bearer" :value="viewing.feeBearer" />
            <span class="sub-text">{{ bearerText(viewing) }}</span>
          </template>
          <span v-else class="sub-text">{{ $t('fin.transfer.noFee') }}</span>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('fin.transfer.handler')">{{ viewing.handlerName || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.transfer.ticketNo')">{{ viewing.ticketNo || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.transfer.voucher')">
          <a v-if="viewing.voucherId" @click="openVoucher(viewing.voucherId)">{{ viewing.voucherNo }}</a>
          <DictTag v-if="viewing.voucherStatus" type="fin_voucher_status" :value="viewing.voucherStatus" class="voucher-status" />
        </a-descriptions-item>
        <a-descriptions-item :label="$t('fin.transfer.createBy')">{{ viewing.createByName }} {{ viewing.createTime }}</a-descriptions-item>
        <a-descriptions-item :label="$t('common.field.remark')">{{ viewing.remark || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>

    <VoucherDetail v-model:open="voucherOpen" :voucher-id="voucherId" @changed="onVoucherChanged" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowRightOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import VoucherDetail from '@/components/fin/VoucherDetail.vue'
import { addFundTransfer, deleteFundTransfer, getFundTransfer, pageFundTransfer } from '@/api/fin'
import { formatMoney } from '@/components/fin/fin'
import { today } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { FeeBearer } from '@/constants/enums'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useAction } from '@/composables/useAction'
import { useRouteOpen } from '@/composables/useRouteOpen'

/** 账户调拨：银行账户之间、银行与现金之间转账；手续费由所选的支付账户支出（出纳日记账中单独一笔），计入财务费用 */
const { t } = useI18n()
const dict = useDict('fin_fee_bearer', 'fin_voucher_status')
const columns = [
  { title: t('fin.transfer.transferNo'), key: 'transferNo', width: 170 },
  { title: t('common.field.date'), dataIndex: 'transferDate', width: 110 },
  { title: t('fin.transfer.flow'), key: 'flow', width: 270 },
  { title: t('common.field.amount'), key: 'amount', width: 130, align: 'right' },
  { title: t('fin.transfer.fee'), key: 'fee', width: 100, align: 'right' },
  { title: t('fin.transfer.feeBearer'), key: 'feeBearer', width: 140 },
  { title: t('fin.transfer.ticketNo'), dataIndex: 'ticketNo', width: 140, ellipsis: true },
  { title: t('fin.transfer.handler'), dataIndex: 'handlerName', width: 100 },
  { title: t('fin.transfer.voucher'), key: 'voucher', width: 120 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  { title: t('fin.transfer.createBy'), dataIndex: 'createByName', width: 100 },
  actionColumn(1, [t('common.action.delete')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'fin-transfer')
const range = ref()
const query = reactive({ keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(
  (p) => pageFundTransfer({ ...p, begin: range.value?.[0], end: range.value?.[1] }),
  query
)
onMounted(load)

const rules = {
  transferDate: [{ required: true, message: t('fin.transfer.dateRequired') }],
  fromAccountId: [{ required: true, message: t('fin.transfer.fromRequired') }],
  toAccountId: [
    { required: true, message: t('fin.transfer.toRequired') },
    { validator: (_, v) => (v && v === form.value.fromAccountId ? Promise.reject(t('fin.transfer.sameAccount')) : Promise.resolve()) }
  ],
  amount: [{ required: true, message: t('fin.transfer.amountRequired') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({
    transferDate: today(),
    fromAccountId: undefined,
    toAccountId: undefined,
    amount: undefined,
    fee: 0,
    feeBearer: FeeBearer.FROM,
    handlerId: undefined,
    ticketNo: '',
    remark: ''
  }),
  create: addFundTransfer,
  update: () => Promise.resolve(),
  toPayload: (f) => ({ ...f, fee: f.fee || 0, handlerId: f.handlerId || null, ticketNo: f.ticketNo || null, remark: f.remark || null }),
  successMessage: t('fin.transfer.success'),
  onSuccess: load
})
const { formRef, form } = drawer
const hasFee = computed(() => Number(form.value.fee) > 0)
/** 手续费支付账户的效果：转出账户多付出手续费，或转入账户少收到手续费；手续费为 0 时不起作用 */
function bearerText(f) {
  const amount = Number(f.amount) || 0
  const fee = Number(f.fee) || 0
  return f.feeBearer === FeeBearer.TO
    ? t('fin.transfer.bearerToText', { out: formatMoney(amount), in: formatMoney(amount - fee) })
    : t('fin.transfer.bearerFromText', { out: formatMoney(amount + fee), in: formatMoney(amount) })
}
const feeBearerText = computed(() => (hasFee.value ? bearerText(form.value) : t('fin.transfer.noFeeExtra')))

const remove = useAction(deleteFundTransfer, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

const viewOpen = ref(false)
const viewing = ref()
async function openView(record) {
  viewing.value = record
  viewOpen.value = true
  viewing.value = await getFundTransfer(record.id)
}
// 从收支汇总的调拨手续费明细跳转过来（?id=调拨ID）时打开详情
useRouteOpen({ id: (id) => openView({ id }) })

const voucherOpen = ref(false)
const voucherId = ref()
function openVoucher(id) {
  voucherId.value = id
  voucherOpen.value = true
}
function onVoucherChanged() {
  load()
  if (viewOpen.value && viewing.value?.id) openView(viewing.value)
}
</script>

<style scoped>
.filter {
  width: 220px;
}
.arrow {
  margin: 0 8px;
  color: var(--app-text-3);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.voucher-status {
  margin-left: 8px;
}
.form-tip {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
</style>
