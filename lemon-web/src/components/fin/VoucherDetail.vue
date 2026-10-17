<template>
  <SubPage :open="open" :title="$t('fin.voucherDetail.title')" width="min(980px, 100vw)" destroy-on-close @close="emit('update:open', false)">
    <template #extra>
      <a-space>
        <a-button v-if="v" size="small" @click="print"><template #icon><PrinterOutlined /></template>{{ $t('common.action.print') }}</a-button>
        <a-tag v-if="v?.reversalOf" color="red" :bordered="false">{{ $t('fin.voucherDetail.red') }}</a-tag>
        <a-tag v-if="v?.reversed === 1" color="orange" :bordered="false">{{ $t('fin.voucherDetail.reversed') }}</a-tag>
        <DictTag v-if="v" type="fin_voucher_status" :value="v.status" />
      </a-space>
    </template>
    <a-spin :spinning="loading">
      <div v-if="v" class="voucher">
        <div class="voucher-title">{{ $t('fin.voucherDetail.heading') }}</div>
        <div class="voucher-head">
          <span>{{ $t('fin.voucherDetail.voucherNo') }}<b>{{ v.voucherNo }}</b></span>
          <span>{{ $t('fin.voucherDetail.date', { date: v.voucherDate }) }}</span>
          <span>{{ $t('fin.voucherDetail.period', { period: periodText(v.period) }) }}</span>
          <span>{{ $t('fin.voucherDetail.attachmentCount', { n: v.attachmentCount }) }}</span>
        </div>
        <table class="voucher-table">
          <thead>
            <tr>
              <th style="width: 27%">{{ $t('fin.voucherDetail.summary') }}</th>
              <th style="width: 27%">{{ $t('fin.voucherDetail.account') }}</th>
              <th style="width: 20%">{{ $t('fin.voucherDetail.aux') }}</th>
              <th style="width: 13%">{{ $t('fin.voucherDetail.debit') }}</th>
              <th style="width: 13%">{{ $t('fin.voucherDetail.credit') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="e in v.entries" :key="e.id">
              <td>{{ e.summary }}</td>
              <td>{{ e.accountCode }} {{ e.accountName }}</td>
              <td>
                <template v-for="a in AUX_ITEMS" :key="a.value">
                  <div v-if="e[a.name]" class="aux"><span class="aux-label">{{ a.label }}</span>{{ e[a.name] }}</div>
                </template>
              </td>
              <td :class="['num', { neg: Number(e.debit) < 0 }]">{{ moneyOrBlank(e.debit) }}</td>
              <td :class="['num', { neg: Number(e.credit) < 0 }]">{{ moneyOrBlank(e.credit) }}</td>
            </tr>
          </tbody>
          <tfoot>
            <tr>
              <td colspan="3">{{ isZh() ? $t('fin.voucherDetail.totalUpper', { amount: moneyUpper(v.totalDebit) }) : $t('common.field.total') }}</td>
              <td :class="['num', { neg: Number(v.totalDebit) < 0 }]">{{ formatMoney(v.totalDebit) }}</td>
              <td :class="['num', { neg: Number(v.totalCredit) < 0 }]">{{ formatMoney(v.totalCredit) }}</td>
            </tr>
          </tfoot>
        </table>
        <div class="voucher-foot">
          <span>{{ $t('fin.voucherDetail.source') }}<DictTag type="fin_voucher_source" :value="v.sourceType" /> {{ v.sourceNo || '' }}</span>
          <span>{{ $t('fin.voucherDetail.preparedBy', { name: v.createByName || '-' }) }}</span>
          <span>{{ $t('fin.voucherDetail.approvedBy', { name: v.auditByName || '-' }) }}</span>
          <span>{{ $t('fin.voucherDetail.postedBy', { name: v.postByName || '-' }) }}</span>
        </div>
        <div class="attachment-head">{{ $t('fin.voucherDetail.attachmentHead') }}</div>
        <AttachmentPanel biz-type="fin_voucher" :biz-id="v.id" :editable="userStore.hasPerm('fin:voucher:edit')" />
        <div class="attachment-head">{{ $t('fin.voucherDetail.changeLog') }}</div>
        <ChangeLogPanel biz-type="fin_voucher" :biz-id="v.id" />
      </div>
    </a-spin>
    <template v-if="v && actions.length" #footer>
      <div class="drawer-footer">
        <a-button v-for="a in actions" :key="a.key" :type="a.primary ? 'primary' : 'default'" :danger="a.danger" @click="a.run">
          {{ a.label }}
        </a-button>
      </div>
    </template>
    <ReverseModal v-model:open="reverseOpen" :voucher="v" @success="onReversed" />
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, ref, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { PrinterOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import ChangeLogPanel from '@/components/ChangeLogPanel.vue'
import ReverseModal from './ReverseModal.vue'
import { deleteVoucher, getVoucher, voucherAction } from '@/api/fin'
import { useUserStore } from '@/store/user'
import { AUX_ITEMS, auxText, canDeleteVoucher, canReverse, formatMoney, moneyOrBlank, moneyUpper, periodText } from './fin'
import { printDocs } from '@/utils/print'
import { useI18n } from 'vue-i18n'
import { isZh } from '@/locales'

/** 凭证详情（凭证样式），按状态提供编辑、审核、记账等操作；触发 edit(voucher)、changed */
const props = defineProps({
  open: Boolean,
  voucherId: Number
})
const emit = defineEmits(['update:open', 'edit', 'changed', 'reversed'])

const { t } = useI18n()
const userStore = useUserStore()
const v = ref()
const loading = ref(false)

function print() {
  const d = v.value
  printDocs({
    // 打印模板（utils/printTemplates.js）按名称匹配，非中文界面下按译文名称打印
    title: t('fin.voucherDetail.title'),
    no: d.voucherNo,
    fields: [
      [t('common.field.date'), d.voucherDate],
      [t('fin.voucherDetail.printPeriod'), periodText(d.period)],
      [t('fin.voucherDetail.printAttachment'), t('fin.voucherDetail.attachmentN', { n: d.attachmentCount ?? 0 })]
    ],
    columns: [
      { title: t('fin.voucherDetail.summary'), key: 'summary' },
      { title: t('fin.voucherDetail.account'), value: (e) => `${e.accountCode} ${e.accountName}` },
      { title: t('fin.voucherDetail.deptAux'), key: 'deptName', value: auxText },
      { title: t('fin.voucherDetail.debit'), key: 'debit', value: (e) => moneyOrBlank(e.debit), align: 'right' },
      { title: t('fin.voucherDetail.credit'), key: 'credit', value: (e) => moneyOrBlank(e.credit), align: 'right' }
    ],
    rows: d.entries,
    totals: { summary: t('common.field.total'), deptName: isZh() ? moneyUpper(d.totalDebit) : '', debit: formatMoney(d.totalDebit), credit: formatMoney(d.totalCredit) },
    signs: [
      [t('fin.voucherDetail.signPrepared'), d.createByName],
      [t('fin.voucherDetail.signApproved'), d.auditByName],
      [t('fin.voucherDetail.signPosted'), d.postByName],
      t('fin.voucherDetail.signManager')
    ]
  }, t('fin.voucherDetail.printTitle', { no: d.voucherNo }), 'fin.voucher')
}

async function load() {
  loading.value = true
  try {
    v.value = await getVoucher(props.voucherId)
  } finally {
    loading.value = false
  }
}

watch(
  () => props.open && props.voucherId,
  (id) => {
    v.value = undefined
    if (id) load()
  }
)

const reverseOpen = ref(false)
async function onReversed(id) {
  emit('changed')
  emit('reversed', id)
  await load()
}

async function act(action, label) {
  await voucherAction(action, v.value.id)
  message.success(t('fin.voucherDetail.actionSuccess', { action: label }))
  await load()
  emit('changed')
}

const actions = computed(() => {
  const d = v.value
  if (!d) return []
  const can = (p) => userStore.hasPerm(`fin:voucher:${p}`)
  const list = []
  if (d.status === 'DRAFT') {
    if (can('remove') && canDeleteVoucher(d))
      list.push({
        key: 'remove',
        label: t('common.action.delete'),
        danger: true,
        run: () =>
          Modal.confirm({
            title: t('fin.voucherDetail.confirmDelete', { no: d.voucherNo }),
            content: d.sourceType !== 'MANUAL' ? t('fin.voucherDetail.deleteTip') : undefined,
            okType: 'danger',
            async onOk() {
              await deleteVoucher(d.id)
              message.success(t('common.msg.deleteSuccess'))
              emit('update:open', false)
              emit('changed')
            }
          })
      })
    if (d.sourceType === 'MANUAL' && can('edit')) list.push({ key: 'edit', label: t('common.action.edit'), run: () => emit('edit', d) })
    if (can('add')) list.push({ key: 'submit', label: t('fin.voucherDetail.submit'), run: () => act('submit', t('fin.voucherDetail.submit')) })
    if (can('audit')) list.push({ key: 'audit', label: t('common.action.audit'), primary: true, run: () => act('audit', t('common.action.audit')) })
  }
  if (d.status === 'AUDITED') {
    if (can('audit')) list.push({ key: 'unaudit', label: t('common.action.unaudit'), run: () => act('unaudit', t('common.action.unaudit')) })
    if (can('post')) list.push({ key: 'post', label: t('fin.voucherDetail.post'), primary: true, run: () => act('post', t('fin.voucherDetail.post')) })
  }
  if (d.status === 'POSTED' && d.sourceType !== 'CLOSE' && can('post')) {
    list.push({ key: 'unpost', label: t('fin.voucherDetail.unpost'), run: () => act('unpost', t('fin.voucherDetail.unpost')) })
  }
  if (canReverse(d) && can('reverse')) {
    list.push({ key: 'reverse', label: t('fin.voucherDetail.reverse'), danger: true, run: () => (reverseOpen.value = true) })
  }
  return list
})

defineExpose({ reload: load })
</script>

<style scoped>
.voucher {
  padding: 8px 4px;
}
.voucher-title {
  text-align: center;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 4px;
  margin-bottom: 12px;
  border-bottom: 3px double var(--app-border, #d9d9d9);
  padding-bottom: 8px;
}
.voucher-head,
.voucher-foot {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 8px;
  margin: 8px 0;
  color: var(--app-text-2);
}
.voucher-table {
  width: 100%;
  border-collapse: collapse;
}
.voucher-table th,
.voucher-table td {
  border: 1px solid var(--app-border, #d9d9d9);
  padding: 8px 10px;
}
.voucher-table th {
  background: var(--app-bg-2, #fafafa);
  font-weight: 500;
}
.voucher-table tfoot td {
  font-weight: 500;
}
.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.attachment-head {
  margin: 16px 0 4px;
  font-weight: 500;
}
.neg {
  color: #cf1322;
}
.aux {
  font-size: 12px;
  line-height: 1.6;
}
.aux-label {
  margin-right: 4px;
  color: var(--ant-color-text-tertiary, #888);
}
</style>
