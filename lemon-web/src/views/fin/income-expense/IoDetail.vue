<template>
  <SubPage :open="open" :title="detail ? $t('fin.io.detailTitle', { type: typeLabel(detail.direction), no: detail.docNo }) : ''" width="min(900px, 100vw)" @close="open = false">
    <template v-if="detail" #extra>
      <a-space>
        <template v-if="detail.status === IoStatus.DRAFT">
          <span v-perm="'fin:io:remove'">
            <a-popconfirm :title="$t('fin.io.confirmDelete', { no: detail.docNo })" @confirm="remove">
              <a-button danger>{{ $t('common.action.delete') }}</a-button>
            </a-popconfirm>
          </span>
          <a-button v-perm="'fin:io:edit'" @click="emit('edit', detail)">{{ $t('common.action.edit') }}</a-button>
          <a-button v-perm="'fin:io:confirm'" type="primary" @click="confirm(detail)">{{ $t('fin.io.confirm') }}</a-button>
        </template>
        <a-button v-else-if="detail.voucherStatus !== VoucherStatus.POSTED" v-perm="'fin:io:confirm'" @click="cancelConfirm(detail)">
          {{ $t('fin.io.cancelConfirm') }}
        </a-button>
      </a-space>
    </template>
    <template v-if="detail">
      <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
        <a-descriptions-item :label="$t('fin.io.type')"><DictTag type="fin_fund_direction" :value="detail.direction" /></a-descriptions-item>
        <a-descriptions-item :label="$t('fin.io.date')">{{ detail.docDate }}</a-descriptions-item>
        <a-descriptions-item :label="$t('common.field.status')"><DictTag type="fin_io_status" :value="detail.status" /></a-descriptions-item>
        <a-descriptions-item :label="$t('fin.io.amount')"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
        <a-descriptions-item :label="$t('fin.io.account')">{{ detail.accountName }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.io.settleMethod')"><DictTag v-if="detail.settleMethod" type="fin_settle_method" :value="detail.settleMethod" /><span v-else>-</span></a-descriptions-item>
        <a-descriptions-item :label="$t('fin.io.partner')">
          <template v-if="detail.partnerType"><DictTag type="fin_io_partner_type" :value="detail.partnerType" /> {{ detail.partnerLabel }}</template>
          <span v-else>-</span>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('fin.io.ticketNo')">{{ detail.ticketNo || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.io.dept')">{{ detail.deptName || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.io.voucher')">
          <a v-if="detail.voucherId" @click="voucherOpen = true">{{ detail.voucherNo }}</a>
          <DictTag v-if="detail.voucherId && detail.voucherStatus !== VoucherStatus.POSTED" type="fin_voucher_status" :value="detail.voucherStatus" class="ml" />
          <span v-if="!detail.voucherId">-</span>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('fin.io.createBy')">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.io.confirmBy')">{{ detail.confirmByName || '-' }} {{ detail.confirmTime || '' }}</a-descriptions-item>
        <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="3">{{ detail.remark }}</a-descriptions-item>
      </a-descriptions>
      <div class="section-title">{{ $t('fin.io.items') }}</div>
      <a-table row-key="id" size="small" :columns="columns" :data-source="detail.items" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template>
        </template>
      </a-table>
    </template>
    <VoucherDetail v-model:open="voucherOpen" :voucher-id="detail?.voucherId" @changed="reload" />
  </SubPage>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import VoucherDetail from '@/components/fin/VoucherDetail.vue'
import { deleteIo, getIo } from '@/api/fin'
import { formatMoney } from '@/components/fin/fin'
import { useDict } from '@/composables/useDict'
import { FundDirection, IoStatus, VoucherStatus } from '@/constants/enums'
import { useIoActions } from './useIoActions'

/** 收支单详情：待确认时可修改、确认、删除；已确认、凭证未记账时可取消确认 */
const emit = defineEmits(['edit', 'changed'])
const { t } = useI18n()
useDict('fin_fund_direction', 'fin_io_status', 'fin_io_partner_type', 'fin_settle_method', 'fin_voucher_status')
const typeLabel = (d) => (d === FundDirection.IN ? t('fin.io.typeIn') : t('fin.io.typeOut'))
const columns = [
  { title: t('fin.io.category'), dataIndex: 'categoryName', width: 180 },
  { title: t('fin.io.categoryAccount'), dataIndex: 'accountName', width: 200 },
  { title: t('common.field.amount'), key: 'amount', width: 140, align: 'right' },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true }
]

const open = ref(false)
const detail = ref()
const voucherOpen = ref(false)
async function show(id) {
  detail.value = await getIo(id)
  open.value = true
}
const reload = () => {
  emit('changed')
  return detail.value ? show(detail.value.id) : undefined
}
const { confirm, cancelConfirm } = useIoActions(reload)
async function remove() {
  await deleteIo(detail.value.id)
  message.success(t('common.msg.deleteSuccess'))
  open.value = false
  emit('changed')
}

defineExpose({ open: show, reload })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.section-title {
  margin: 12px 0 8px;
  font-weight: 600;
}
.ml {
  margin-left: 4px;
}
</style>
