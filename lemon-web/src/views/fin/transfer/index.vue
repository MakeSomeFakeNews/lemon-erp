<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" @change="search" />
        <a-input-search v-model:value="query.keyword" placeholder="单号、备注" allow-clear class="filter" @search="search" />
        <template #extra>
          <a-button v-perm="'fin:transfer:edit'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>新增调拨
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
            <div v-if="Number(record.fee)" class="sub-text">手续费 ¥{{ formatMoney(record.fee) }}</div>
          </template>
          <template v-else-if="column.key === 'voucher'">
            <a v-if="record.voucherId" @click="openVoucher(record.voucherId)">{{ record.voucherNo }}</a>
          </template>
          <template v-else-if="column.key === 'action'">
            <span v-perm="'fin:transfer:edit'">
              <a-popconfirm :title="`删除 ${record.transferNo}？生成的凭证（须未审核）一并删除`" @confirm="remove(record.id)">
                <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增账户调拨">
      <a-alert type="info" show-icon class="form-tip" message="银行账户之间、银行与现金之间转账，保存后生成凭证（借 转入账户、财务费用，贷 转出账户）。" />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="调拨日期" name="transferDate">
          <a-date-picker v-model:value="form.transferDate" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="转出账户" name="fromAccountId">
              <AccountSelect v-model:value="form.fromAccountId" :prefix="['1001', '1002']" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="转入账户" name="toAccountId">
              <AccountSelect v-model:value="form.toAccountId" :prefix="['1001', '1002']" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="金额" name="amount">
              <a-input-number v-model:value="form.amount" :min="0.01" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="手续费" name="fee" extra="由转出账户承担，计入财务费用">
              <a-input-number v-model:value="form.fee" :min="0" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" placeholder="如：归集资金、备付工资" />
        </a-form-item>
      </a-form>
    </FormDrawer>

    <VoucherDetail v-model:open="voucherOpen" :voucher-id="voucherId" @changed="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
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

const columns = [
  { title: '调拨单号', dataIndex: 'transferNo', width: 170 },
  { title: '日期', dataIndex: 'transferDate', width: 110 },
  { title: '转出 → 转入', key: 'flow', width: 340 },
  { title: '金额', key: 'amount', width: 150, align: 'right' },
  { title: '凭证', key: 'voucher', width: 110 },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
  { title: '制单人', dataIndex: 'createByName', width: 100 },
  { title: '操作', key: 'action', width: 70, fixed: 'right' }
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
  transferDate: [{ required: true, message: '请选择日期' }],
  fromAccountId: [{ required: true, message: '请选择转出账户' }],
  toAccountId: [
    { required: true, message: '请选择转入账户' },
    { validator: (_, v) => (v && v === form.value.fromAccountId ? Promise.reject('转出、转入账户不能相同') : Promise.resolve()) }
  ],
  amount: [{ required: true, message: '请填写金额' }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ transferDate: today(), fromAccountId: undefined, toAccountId: undefined, amount: undefined, fee: 0, remark: '' }),
  create: addFundTransfer,
  update: () => Promise.resolve(),
  successMessage: '调拨成功，凭证已生成（未审核）',
  onSuccess: load
})
const { formRef, form } = drawer

async function remove(id) {
  await deleteFundTransfer(id)
  message.success('删除成功')
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
