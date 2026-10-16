<template>
  <SubPage :open="open" title="记账凭证" width="min(980px, 100vw)" destroy-on-close @close="emit('update:open', false)">
    <template #extra>
      <a-space>
        <a-button v-if="v" size="small" @click="print"><template #icon><PrinterOutlined /></template>打印</a-button>
        <a-tag v-if="v?.reversalOf" color="red" :bordered="false">红字</a-tag>
        <a-tag v-if="v?.reversed === 1" color="orange" :bordered="false">已冲销</a-tag>
        <DictTag v-if="v" type="fin_voucher_status" :value="v.status" />
      </a-space>
    </template>
    <a-spin :spinning="loading">
      <div v-if="v" class="voucher">
        <div class="voucher-title">记 账 凭 证</div>
        <div class="voucher-head">
          <span>凭证字号：<b>{{ v.voucherNo }}</b></span>
          <span>日期：{{ v.voucherDate }}</span>
          <span>期间：{{ periodText(v.period) }}</span>
          <span>附单据 {{ v.attachmentCount }} 张</span>
        </div>
        <table class="voucher-table">
          <thead>
            <tr>
              <th style="width: 30%">摘要</th>
              <th style="width: 32%">会计科目</th>
              <th style="width: 12%">部门 / 辅助</th>
              <th style="width: 13%">借方金额</th>
              <th style="width: 13%">贷方金额</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="e in v.entries" :key="e.id">
              <td>{{ e.summary }}</td>
              <td>{{ e.accountCode }} {{ e.accountName }}</td>
              <td>
                {{ e.deptName || '' }}
                <div v-if="e.projectName || e.partnerName" class="aux">{{ [e.projectName, e.partnerName].filter(Boolean).join(' / ') }}</div>
              </td>
              <td :class="['num', { neg: Number(e.debit) < 0 }]">{{ moneyOrBlank(e.debit) }}</td>
              <td :class="['num', { neg: Number(e.credit) < 0 }]">{{ moneyOrBlank(e.credit) }}</td>
            </tr>
          </tbody>
          <tfoot>
            <tr>
              <td colspan="3">合计：{{ moneyUpper(v.totalDebit) }}</td>
              <td :class="['num', { neg: Number(v.totalDebit) < 0 }]">{{ formatMoney(v.totalDebit) }}</td>
              <td :class="['num', { neg: Number(v.totalCredit) < 0 }]">{{ formatMoney(v.totalCredit) }}</td>
            </tr>
          </tfoot>
        </table>
        <div class="voucher-foot">
          <span>来源：<DictTag type="fin_voucher_source" :value="v.sourceType" /> {{ v.sourceNo || '' }}</span>
          <span>制单：{{ v.createByName || '-' }}</span>
          <span>审核：{{ v.auditByName || '-' }}</span>
          <span>记账：{{ v.postByName || '-' }}</span>
        </div>
        <div class="attachment-head">附件（原始凭证）</div>
        <AttachmentPanel biz-type="fin_voucher" :biz-id="v.id" :editable="userStore.hasPerm('fin:voucher:edit')" />
        <div class="attachment-head">变更记录</div>
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
import { canDeleteVoucher, canReverse, formatMoney, moneyOrBlank, moneyUpper, periodText } from './fin'
import { printDocs } from '@/utils/print'

/** 凭证详情（凭证样式），按状态提供编辑、审核、记账等操作；触发 edit(voucher)、changed */
const props = defineProps({
  open: Boolean,
  voucherId: Number
})
const emit = defineEmits(['update:open', 'edit', 'changed', 'reversed'])

const userStore = useUserStore()
const v = ref()
const loading = ref(false)

function print() {
  const d = v.value
  printDocs({
    title: '记账凭证',
    no: d.voucherNo,
    fields: [
      ['日期', d.voucherDate],
      ['期间', periodText(d.period)],
      ['附单据', `${d.attachmentCount ?? 0} 张`]
    ],
    columns: [
      { title: '摘要', key: 'summary' },
      { title: '会计科目', value: (e) => `${e.accountCode} ${e.accountName}` },
      { title: '部门 / 辅助', key: 'deptName', value: (e) => [e.deptName, e.projectName, e.partnerName].filter(Boolean).join(' / ') },
      { title: '借方金额', key: 'debit', value: (e) => moneyOrBlank(e.debit), align: 'right' },
      { title: '贷方金额', key: 'credit', value: (e) => moneyOrBlank(e.credit), align: 'right' }
    ],
    rows: d.entries,
    totals: { summary: '合计', deptName: moneyUpper(d.totalDebit), debit: formatMoney(d.totalDebit), credit: formatMoney(d.totalCredit) },
    signs: [['制单', d.createByName], ['审核', d.auditByName], ['记账', d.postByName], '财务主管']
  }, `记账凭证 ${d.voucherNo}`, 'fin.voucher')
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
  message.success(`${label}成功`)
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
        label: '删除',
        danger: true,
        run: () =>
          Modal.confirm({
            title: `删除凭证 ${d.voucherNo}？`,
            content: d.sourceType !== 'MANUAL' ? '业务生成的凭证删除后，可以在来源单据中重新生成。' : undefined,
            okType: 'danger',
            async onOk() {
              await deleteVoucher(d.id)
              message.success('删除成功')
              emit('update:open', false)
              emit('changed')
            }
          })
      })
    if (d.sourceType === 'MANUAL' && can('edit')) list.push({ key: 'edit', label: '编辑', run: () => emit('edit', d) })
    if (can('add')) list.push({ key: 'submit', label: '提交审批', run: () => act('submit', '提交审批') })
    if (can('audit')) list.push({ key: 'audit', label: '审核', primary: true, run: () => act('audit', '审核') })
  }
  if (d.status === 'AUDITED') {
    if (can('audit')) list.push({ key: 'unaudit', label: '反审核', run: () => act('unaudit', '反审核') })
    if (can('post')) list.push({ key: 'post', label: '记账', primary: true, run: () => act('post', '记账') })
  }
  if (d.status === 'POSTED' && d.sourceType !== 'CLOSE' && can('post')) {
    list.push({ key: 'unpost', label: '反记账', run: () => act('unpost', '反记账') })
  }
  if (canReverse(d) && can('reverse')) {
    list.push({ key: 'reverse', label: '红字冲销', danger: true, run: () => (reverseOpen.value = true) })
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
  color: var(--ant-color-text-tertiary, #888);
  font-size: 12px;
}
</style>
