<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <div class="toolbar">
        <span class="sub-text">{{ $t('fin.cashAccount.tip') }}</span>
        <a-space wrap>
          <a-button v-perm="'fin:account:list'" @click="router.push('/fin/account')">{{ $t('fin.cashAccount.designate') }}</a-button>
          <a-button v-perm="'fin:cashAccount:add'" type="primary" @click="formRef.openCreate()">
            <template #icon><PlusOutlined /></template>{{ $t('fin.cashAccount.add') }}
          </a-button>
        </a-space>
      </div>
      <a-table row-key="id" size="middle" :columns="columns" :data-source="list" :loading="loading" :pagination="false" :scroll="{ x: scrollX, y: '100%' }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <b>{{ record.name }}</b>
            <div v-if="record.bankName || record.bankAccountNo" class="sub-text">{{ [record.bankName, record.bankAccountNo].filter(Boolean).join(' ') }}</div>
          </template>
          <template v-else-if="column.key === 'type'"><DictTag type="fin_cash_account_type" :value="record.type" /></template>
          <template v-else-if="column.key === 'account'">
            <span v-if="record.accountCode">{{ record.accountCode }} {{ record.accountName }}</span>
          </template>
          <template v-else-if="column.key === 'enableDate'">
            <span v-if="record.enableDate">{{ record.enableDate }}</span>
            <span v-else class="sub-text">{{ $t('fin.cashAccount.fromStart') }}</span>
          </template>
          <template v-else-if="column.key === 'journalOpening'">
            <span v-if="record.enableDate" class="num">{{ formatMoney(record.journalOpening) }}</span>
            <span v-else class="sub-text">{{ $t('fin.cashAccount.subjectOpening') }}</span>
          </template>
          <template v-else-if="column.key === 'statementOpening'">
            <template v-if="record.enableDate && record.type !== CashAccountType.CASH">
              <span class="num">{{ formatMoney(record.statementOpening) }}</span>
              <div v-if="record.openingItemCount" class="sub-text">{{ $t('fin.cashAccount.openingItemCount', { n: record.openingItemCount }) }}</div>
            </template>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="sys_common_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <CashAccountForm ref="formRef" @saved="load" />
    <OpeningDrawer ref="openingRef" @changed="load" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { changeCashAccountStatus, deleteCashAccount, listCashAccount } from '@/api/fin'
import { formatMoney } from '@/components/fin/fin'
import { useAction } from '@/composables/useAction'
import { useDict } from '@/composables/useDict'
import { STATUS_DISABLED, STATUS_ENABLED } from '@/utils/constants'
import { CashAccountType } from '@/constants/enums'
import CashAccountForm from './CashAccountForm.vue'
import OpeningDrawer from './OpeningDrawer.vue'

/**
 * 资金账户：出纳的现金、银行、微信、支付宝等账户，绑定入账科目（指定科目下的末级科目），单据的账户下拉只列出启用的账户。
 * 有启用日期的账户录入日记账期初，银行类账户另录对账单期初和期初未达账，调平后启用
 */
const { t } = useI18n()
const router = useRouter()
useDict('fin_cash_account_type', 'sys_common_status')
const formRef = ref()
const openingRef = ref()

const columns = [
  { title: t('fin.cashAccount.name'), key: 'name', width: 200 },
  { title: t('fin.cashAccount.type'), key: 'type', width: 90 },
  { title: t('fin.cashAccount.subject'), key: 'account', width: 200 },
  { title: t('fin.cashAccount.enableDate'), key: 'enableDate', width: 150 },
  { title: t('fin.cashAccount.journalOpening'), key: 'journalOpening', width: 140, align: 'right' },
  { title: t('fin.cashAccount.statementOpening'), key: 'statementOpening', width: 140, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 80 },
  { title: t('common.field.sort'), dataIndex: 'sort', width: 70, align: 'right' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  actionColumn(4)
]
const scrollX = columns.reduce((sum, c) => sum + (c.width || 0), 0)

const enabled = (r) => r.status === STATUS_ENABLED
const rowActions = (record) => [
  { type: 'edit', perm: 'fin:cashAccount:edit', onClick: () => formRef.value.openEdit(record) },
  {
    type: 'setting',
    key: 'opening',
    label: t('fin.cashAccount.opening'),
    show: record.type !== CashAccountType.CASH,
    onClick: () => openingRef.value.open(record)
  },
  { type: 'enable', show: !enabled(record), perm: 'fin:cashAccount:edit', onClick: () => changeStatus(record.id, STATUS_ENABLED) },
  {
    type: 'disable',
    show: enabled(record),
    perm: 'fin:cashAccount:edit',
    confirm: t('fin.cashAccount.confirmDisable', { name: record.name }),
    onClick: () => changeStatus(record.id, STATUS_DISABLED)
  },
  {
    type: 'delete',
    show: !record.used,
    perm: 'fin:cashAccount:remove',
    confirm: t('fin.cashAccount.confirmDelete', { name: record.name }),
    onClick: () => remove(record.id)
  }
]

const list = ref([])
const loading = ref(false)
async function load() {
  loading.value = true
  try {
    list.value = await listCashAccount()
  } finally {
    loading.value = false
  }
}
onMounted(load)

const changeStatus = useAction(changeCashAccountStatus, { successKey: 'common.msg.operationSuccess', reload: () => load() })
const remove = useAction(deleteCashAccount, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.num {
  font-variant-numeric: tabular-nums;
}
</style>
