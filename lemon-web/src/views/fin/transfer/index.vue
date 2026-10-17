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
        :scroll="{ x: 1100, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'flow'">
            <span>{{ record.fromAccountName }}</span>
            <ArrowRightOutlined class="arrow" />
            <span>{{ record.toAccountName }}</span>
          </template>
          <template v-else-if="column.key === 'amount'">
            ¥{{ formatMoney(record.amount) }}
            <div v-if="Number(record.fee)" class="sub-text">{{ $t('fin.transfer.feeText', { amount: formatMoney(record.fee) }) }}</div>
          </template>
          <template v-else-if="column.key === 'voucher'">
            <a v-if="record.voucherId" @click="openVoucher(record.voucherId)">{{ record.voucherNo }}</a>
          </template>
          <template v-else-if="column.key === 'action'">
            <span v-perm="'fin:transfer:edit'">
              <a-popconfirm :title="$t('fin.transfer.confirmDelete', { no: record.transferNo })" @confirm="remove(record.id)">
                <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('fin.transfer.createTitle')">
      <a-alert type="info" show-icon class="form-tip" :message="$t('fin.transfer.tip')" />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('fin.transfer.date')" name="transferDate">
          <a-date-picker v-model:value="form.transferDate" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.transfer.from')" name="fromAccountId">
              <AccountSelect v-model:value="form.fromAccountId" :prefix="['1001', '1002']" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.transfer.to')" name="toAccountId">
              <AccountSelect v-model:value="form.toAccountId" :prefix="['1001', '1002']" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.amount')" name="amount">
              <a-input-number v-model:value="form.amount" :min="0.01" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.transfer.fee')" name="fee" :extra="$t('fin.transfer.feeExtra')">
              <a-input-number v-model:value="form.fee" :min="0" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('common.field.remark')" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" :placeholder="$t('fin.transfer.remarkPh')" />
        </a-form-item>
      </a-form>
    </FormDrawer>

    <VoucherDetail v-model:open="voucherOpen" :voucher-id="voucherId" @changed="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { ArrowRightOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import VoucherDetail from '@/components/fin/VoucherDetail.vue'
import { addFundTransfer, deleteFundTransfer, pageFundTransfer } from '@/api/fin'
import { formatMoney } from '@/components/fin/fin'
import { today } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'

const { t } = useI18n()
const columns = [
  { title: t('fin.transfer.transferNo'), dataIndex: 'transferNo', width: 170 },
  { title: t('common.field.date'), dataIndex: 'transferDate', width: 110 },
  { title: t('fin.transfer.flow'), key: 'flow', width: 340 },
  { title: t('common.field.amount'), key: 'amount', width: 150, align: 'right' },
  { title: t('fin.transfer.voucher'), key: 'voucher', width: 120 },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  { title: t('fin.transfer.createBy'), dataIndex: 'createByName', width: 120 },
  { title: t('common.action.operation'), key: 'action', width: 90, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'fin-transfer')
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
  emptyForm: () => ({ transferDate: today(), fromAccountId: undefined, toAccountId: undefined, amount: undefined, fee: 0, remark: '' }),
  create: addFundTransfer,
  update: () => Promise.resolve(),
  successMessage: t('fin.transfer.success'),
  onSuccess: load
})
const { formRef, form } = drawer

async function remove(id) {
  await deleteFundTransfer(id)
  message.success(t('common.msg.deleteSuccess'))
  load()
}

const voucherOpen = ref(false)
const voucherId = ref()
function openVoucher(id) {
  voucherId.value = id
  voucherOpen.value = true
}
</script>

<style scoped>
.filter {
  width: 200px;
}
.arrow {
  margin: 0 8px;
  color: var(--app-text-3);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.form-tip {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
</style>
