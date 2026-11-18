<template>
  <a-drawer v-model:open="visible" :title="$t('fin.cashAccount.openingTitle', { name: account?.name || '' })" width="min(860px, 100vw)" destroy-on-close>
    <a-spin :spinning="loading">
      <template v-if="account">
        <a-alert v-if="!account.enableDate" type="info" show-icon class="tip" :message="$t('fin.cashAccount.noEnableDate')" />
        <template v-else>
          <a-alert v-if="enabled" type="info" show-icon class="tip" :message="$t('fin.cashAccount.openingLocked')" />
          <div class="section-title">{{ $t('fin.cashAccount.openingCheck', { date: account.enableDate }) }}</div>
          <table v-if="check" class="rec-table">
            <tbody>
              <tr>
                <td>{{ $t('fin.cashAccount.journalOpening') }}</td>
                <td class="num">{{ formatMoney(check.journalOpening) }}</td>
                <td>{{ $t('fin.cashAccount.statementOpening') }}</td>
                <td class="num">{{ formatMoney(check.statementOpening) }}</td>
              </tr>
              <tr>
                <td>{{ $t('fin.bank.bankIn') }}</td>
                <td class="num">{{ formatMoney(check.bankIn) }}</td>
                <td>{{ $t('fin.bank.bookIn') }}</td>
                <td class="num">{{ formatMoney(check.bookIn) }}</td>
              </tr>
              <tr>
                <td>{{ $t('fin.bank.bankOut') }}</td>
                <td class="num">{{ formatMoney(check.bankOut) }}</td>
                <td>{{ $t('fin.bank.bookOut') }}</td>
                <td class="num">{{ formatMoney(check.bookOut) }}</td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td>{{ $t('fin.bank.adjusted') }}</td>
                <td :class="['num', { neg: !check.balanced }]">{{ formatMoney(check.adjustedBook) }}</td>
                <td>{{ $t('fin.bank.adjusted') }}</td>
                <td :class="['num', { neg: !check.balanced }]">{{ formatMoney(check.adjustedBank) }}</td>
              </tr>
            </tfoot>
          </table>
          <div v-if="check" class="check-line">
            <a-tag :color="check.balanced ? 'green' : 'red'" :bordered="false">{{ check.balanced ? $t('fin.bank.balanced') : $t('fin.bank.unbalanced') }}</a-tag>
            <span v-if="check.ledgerBalance != null" class="sub-text">{{ $t('fin.cashAccount.ledgerBalance', { amount: formatMoney(check.ledgerBalance) }) }}</span>
          </div>

          <div class="items-head">
            <span class="section-title">{{ $t('fin.cashAccount.openingItems') }}</span>
            <span class="sub-text">{{ $t('fin.cashAccount.openingItemsTip') }}</span>
          </div>
          <a-form v-if="!enabled" v-perm="'fin:cashAccount:edit'" layout="inline" class="item-form">
            <a-form-item>
              <a-select v-model:value="item.side" :options="toOptions(dict.fin_bank_opening_side)" class="side" />
            </a-form-item>
            <a-form-item>
              <a-date-picker v-model:value="item.txnDate" value-format="YYYY-MM-DD" :disabled-date="(d) => !d.isBefore(account.enableDate, 'day')" class="date" />
            </a-form-item>
            <a-form-item>
              <a-input-number v-model:value="item.amount" :precision="2" :placeholder="$t('fin.cashAccount.itemAmountPh')" class="amount" />
            </a-form-item>
            <a-form-item>
              <a-input v-model:value="item.summary" :maxlength="255" :placeholder="$t('fin.bank.summary')" />
            </a-form-item>
            <a-form-item>
              <a-input v-model:value="item.refNo" :maxlength="64" :placeholder="$t('fin.bank.refNo')" class="ref" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" :loading="adding" @click="addItem"><template #icon><PlusOutlined /></template>{{ $t('common.action.add') }}</a-button>
            </a-form-item>
          </a-form>
          <a-table row-key="id" size="small" :columns="columns" :data-source="items" :pagination="false" :scroll="{ x: 700 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'side'"><DictTag type="fin_bank_opening_side" :value="record.side" /></template>
              <template v-else-if="column.key === 'amount'">
                <span :class="['num', { neg: Number(record.amount) < 0 }]">{{ formatMoney(record.amount) }}</span>
              </template>
              <template v-else-if="column.key === 'matched'">
                <template v-if="record.entryId || record.statementId">
                  <a-tag color="green" :bordered="false">{{ $t('fin.bank.matched') }}</a-tag>
                  <span class="sub-text">{{ record.voucherNo || record.statementDate }}</span>
                </template>
                <a-tag v-else :bordered="false">{{ $t('fin.bank.unmatched') }}</a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <TableActions
                  :actions="[
                    {
                      type: 'delete',
                      show: !enabled && !record.entryId && !record.statementId,
                      perm: 'fin:cashAccount:edit',
                      confirm: $t('fin.cashAccount.confirmDeleteItem'),
                      onClick: () => removeItem(record.id)
                    }
                  ]"
                />
              </template>
            </template>
          </a-table>
        </template>
      </template>
    </a-spin>
    <template #footer>
      <div class="footer">
        <a-button @click="visible = false">{{ $t('common.action.close') }}</a-button>
        <a-button v-if="account && !enabled" v-perm="'fin:cashAccount:edit'" type="primary" :disabled="check && !check.balanced" @click="enable">
          {{ $t('fin.cashAccount.enableAccount') }}
        </a-button>
      </div>
    </template>
  </a-drawer>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import {
  addCashAccountOpeningItem,
  cashAccountOpeningCheck,
  cashAccountOpeningItems,
  changeCashAccountStatus,
  deleteCashAccountOpeningItem
} from '@/api/fin'
import { formatMoney } from '@/components/fin/fin'
import { useAction } from '@/composables/useAction'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { STATUS_ENABLED } from '@/utils/constants'
import { BankOpeningSide } from '@/constants/enums'

/**
 * 银行类资金账户的期初：日记账期初 + 银行已收企业未收 − 银行已付企业未付 = 对账单期初 + 企业已收银行未收 − 企业已付银行未付，
 * 录入期初未达账调平后启用；启用后只能查看（未达账在银行对账中勾对）
 */
const emit = defineEmits(['changed'])
const { t } = useI18n()
const dict = useDict('fin_bank_opening_side')
const visible = ref(false)
const loading = ref(false)
const account = ref()
const check = ref()
const items = ref([])
const enabled = computed(() => account.value?.status === STATUS_ENABLED)

const columns = [
  { title: t('fin.cashAccount.itemSide'), key: 'side', width: 140 },
  { title: t('common.field.date'), dataIndex: 'txnDate', width: 110 },
  { title: t('common.field.amount'), key: 'amount', width: 120, align: 'right' },
  { title: t('fin.bank.summary'), dataIndex: 'summary', ellipsis: true },
  { title: t('fin.bank.refNo'), dataIndex: 'refNo', width: 130, ellipsis: true },
  { title: t('fin.bank.match'), key: 'matched', width: 150 },
  actionColumn(1, [t('common.action.delete')], { fixed: undefined })
]

const emptyItem = () => ({ side: BankOpeningSide.BOOK, txnDate: undefined, amount: undefined, summary: '', refNo: '' })
const item = reactive(emptyItem())

async function load() {
  loading.value = true
  try {
    if (account.value.enableDate) {
      ;[check.value, items.value] = await Promise.all([cashAccountOpeningCheck(account.value.id), cashAccountOpeningItems(account.value.id)])
    }
  } finally {
    loading.value = false
  }
}

const adding = ref(false)
async function addItem() {
  if (!item.txnDate || !item.amount) {
    message.warning(t('fin.cashAccount.itemRequired'))
    return
  }
  adding.value = true
  try {
    await addCashAccountOpeningItem(account.value.id, { ...item, summary: item.summary || null, refNo: item.refNo || null })
    Object.assign(item, emptyItem(), { side: item.side })
    load()
  } finally {
    adding.value = false
  }
}
const removeItem = useAction(deleteCashAccountOpeningItem, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

async function enable() {
  await changeCashAccountStatus(account.value.id, STATUS_ENABLED)
  message.success(t('fin.cashAccount.enabledMsg'))
  account.value = { ...account.value, status: STATUS_ENABLED }
  emit('changed')
}

defineExpose({
  open(record) {
    account.value = { ...record }
    check.value = undefined
    items.value = []
    Object.assign(item, emptyItem())
    visible.value = true
    load()
  }
})
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.section-title {
  font-weight: 500;
  margin-bottom: 8px;
}
.rec-table {
  width: 100%;
  border-collapse: collapse;
}
.rec-table td {
  border: 1px solid var(--app-border, #d9d9d9);
  padding: 6px 10px;
}
.rec-table tfoot td {
  font-weight: 600;
}
.check-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 8px 0 16px;
}
.items-head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}
.item-form {
  margin-bottom: 8px;
  row-gap: 8px;
}
.side {
  width: 150px;
}
.date {
  width: 140px;
}
.amount {
  width: 140px;
}
.ref {
  width: 140px;
}
.num {
  font-variant-numeric: tabular-nums;
  text-align: right;
}
.neg {
  color: var(--status-critical, #cf1322);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
