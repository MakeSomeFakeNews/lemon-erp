<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.kind" :options="kindOptions" :placeholder="$t('fin.offset.allKinds')" allow-clear class="filter" @change="onKindFilter" />
        <PartnerSelect
          v-if="query.kind"
          v-model:value="query.partnerId"
          :type="query.kind === BillKind.AR ? 'customer' : 'supplier'"
          allow-clear
          :placeholder="$t('fin.offset.partner')"
          class="filter-wide"
          @update:value="search"
        />
        <a-select v-model:value="query.item" :options="toOptions(dict.fin_offset_item)" :placeholder="$t('fin.offset.allItems')" allow-clear class="filter" @change="search" />
        <a-select v-model:value="query.status" :options="toOptions(dict.fin_offset_status)" :placeholder="$t('fin.offset.allStatus')" allow-clear class="filter" @change="search" />
        <a-input-search v-model:value="query.keyword" :placeholder="$t('fin.offset.keywordPh')" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <a-button v-perm="'fin:offset:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>{{ $t('fin.offset.add') }}
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
          <template v-if="column.key === 'offsetNo'">
            <a @click="openView(record)">{{ record.offsetNo }}</a>
          </template>
          <template v-else-if="column.key === 'kind'">
            <a-tag :color="record.kind === BillKind.AR ? 'green' : 'orange'">{{ kindLabel(record.kind) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'partner'">
            {{ record.partnerName }}
            <div class="sub-text">{{ record.partnerCode }}</div>
          </template>
          <template v-else-if="column.key === 'item'"><DictTag type="fin_offset_item" :value="record.item" /></template>
          <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
          <template v-else-if="column.key === 'period'">
            <span v-if="record.beginDate || record.endDate">{{ record.beginDate || '' }} ~ {{ record.endDate || '' }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="fin_offset_status" :value="record.status" />
            <div v-if="record.billNo" class="sub-text">{{ settleText(record) }}</div>
          </template>
          <template v-else-if="column.key === 'bill'">
            <template v-if="record.billNo">
              {{ record.billNo }}
              <div class="sub-text">
                <a v-if="record.voucherId" @click="openVoucher(record.voucherId)">{{ record.voucherNo }}</a>
                <span v-else>{{ $t('fin.offset.noVoucher') }}</span>
              </div>
            </template>
          </template>
          <template v-else-if="column.key === 'review'">
            <template v-if="record.reviewByName">
              {{ record.reviewByName }}
              <div class="sub-text">{{ record.reviewTime }}</div>
            </template>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 冲账单卡片：新增 / 修改（待复核） -->
    <FormDrawer :drawer="drawer" :create-title="$t('fin.offset.add')" :edit-title="$t('fin.offset.edit')" width="min(720px, 100vw)">
      <a-alert type="info" show-icon class="form-tip" :message="$t('fin.offset.tip')" />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.offset.kind')" name="kind">
              <a-radio-group v-model:value="form.kind" button-style="solid" @change="form.partnerId = undefined">
                <a-radio-button v-for="o in kindOptions" :key="o.value" :value="o.value">{{ o.label }}</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('fin.offset.date')" name="offsetDate">
              <a-date-picker v-model:value="form.offsetDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('fin.offset.handler')" name="handlerId">
              <IdentityUserSelect v-model:value="form.handlerId" :label="form.handlerName" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="form.kind === BillKind.AR ? $t('fin.offset.customer') : $t('fin.offset.supplier')" name="partnerId">
              <PartnerSelect v-model:value="form.partnerId" :type="form.kind === BillKind.AR ? 'customer' : 'supplier'" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('fin.offset.item')" name="item">
              <a-select v-model:value="form.item" :options="toOptions(dict.fin_offset_item)" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('fin.offset.amount')" name="amount">
              <a-input-number v-model:value="form.amount" :min="0.01" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.offset.period')" name="beginDate" :extra="$t('fin.offset.periodExtra')">
              <a-range-picker v-model:value="periodRange" value-format="YYYY-MM-DD" class="full" :allow-empty="[true, true]" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.offset.ticketNo')" name="ticketNo">
              <a-input v-model:value="form.ticketNo" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('fin.offset.account')" name="accountId" :extra="form.kind === BillKind.AR ? $t('fin.offset.accountExtraAr') : $t('fin.offset.accountExtraAp')">
              <AccountSelect v-model:value="form.accountId" allow-clear :placeholder="$t('fin.offset.accountPh')" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <!-- 查看 -->
    <a-modal v-model:open="viewOpen" :title="viewing ? $t('fin.offset.viewTitle', { no: viewing.offsetNo }) : ''" :footer="null" width="min(760px, 100vw)">
      <a-descriptions v-if="viewing" :column="{ xs: 1, sm: 2 }" size="small" bordered>
        <a-descriptions-item :label="$t('fin.offset.kind')">{{ kindLabel(viewing.kind) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('common.field.status')"><DictTag type="fin_offset_status" :value="viewing.status" /></a-descriptions-item>
        <a-descriptions-item :label="$t('fin.offset.partner')">{{ viewing.partnerCode }} {{ viewing.partnerName }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.offset.date')">{{ viewing.offsetDate }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.offset.item')"><DictTag type="fin_offset_item" :value="viewing.item" /></a-descriptions-item>
        <a-descriptions-item :label="$t('fin.offset.amount')"><b>¥{{ formatMoney(viewing.amount) }}</b></a-descriptions-item>
        <a-descriptions-item :label="$t('fin.offset.period')">{{ viewing.beginDate || '' }} ~ {{ viewing.endDate || '' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.offset.ticketNo')">{{ viewing.ticketNo || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.offset.handler')">{{ viewing.handlerName || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.offset.account')">{{ viewing.accountName || $t('fin.offset.accountDefault') }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.offset.createBy')">{{ viewing.createByName }} {{ viewing.createTime }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.offset.reviewBy')">{{ viewing.reviewByName || '-' }} {{ viewing.reviewTime || '' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.offset.bill')">{{ viewing.billNo || '-' }} <span v-if="viewing.billNo" class="sub-text">{{ settleText(viewing) }}</span></a-descriptions-item>
        <a-descriptions-item :label="$t('fin.offset.voucher')">{{ viewing.voucherNo || '-' }}</a-descriptions-item>
        <a-descriptions-item v-if="viewing.remark" :label="$t('common.field.remark')" :span="2">{{ viewing.remark }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>

    <VoucherDetail v-model:open="voucherOpen" :voucher-id="voucherId" @changed="load" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import VoucherDetail from '@/components/fin/VoucherDetail.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { addOffset, cancelReviewOffset, deleteOffset, getOffset, pageOffset, reviewOffset, updateOffset } from '@/api/fin'
import { formatMoney } from '@/components/fin/fin'
import { today } from '@/components/wms/wms'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useRouteOpen } from '@/composables/useRouteOpen'
import { toOptions } from '@/utils/dict'
import { BillKind, OffsetStatus, VoucherStatus } from '@/constants/enums'

/** 冲账单：垫付运费、销售返利、三包赔偿等抵扣往来款，复核后生成红字应收 / 应付单，收付款时与欠款冲抵 */
const { t } = useI18n()
const dict = useDict('fin_offset_item', 'fin_offset_status')
const kindOptions = [
  { value: BillKind.AR, label: t('fin.offset.kindAr') },
  { value: BillKind.AP, label: t('fin.offset.kindAp') }
]
const kindLabel = (k) => kindOptions.find((o) => o.value === k)?.label || k
/** 红字单据的核销情况：已全部抵扣 / 已抵扣 x / 未抵扣 */
const settleText = (r) => {
  const used = Math.abs(Number(r.billSettledAmount) || 0)
  if (!used) return t('fin.offset.notUsed')
  return used >= Number(r.amount) ? t('fin.offset.usedAll') : t('fin.offset.usedN', { amount: formatMoney(used) })
}

const columns = [
  { title: t('fin.offset.no'), key: 'offsetNo', width: 160 },
  { title: t('fin.offset.date'), dataIndex: 'offsetDate', width: 110 },
  { title: t('fin.offset.kind'), key: 'kind', width: 110 },
  { title: t('fin.offset.partner'), key: 'partner', width: 200, ellipsis: true },
  { title: t('fin.offset.item'), key: 'item', width: 110 },
  { title: t('fin.offset.amount'), key: 'amount', width: 130, align: 'right' },
  { title: t('fin.offset.period'), key: 'period', width: 210 },
  { title: t('fin.offset.ticketNo'), dataIndex: 'ticketNo', width: 130, ellipsis: true },
  { title: t('common.field.status'), key: 'status', width: 120 },
  { title: t('fin.offset.bill'), key: 'bill', width: 150 },
  { title: t('fin.offset.handler'), dataIndex: 'handlerName', width: 100 },
  { title: t('fin.offset.createBy'), dataIndex: 'createByName', width: 100 },
  { title: t('fin.offset.reviewBy'), key: 'review', width: 150 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  // 草稿：编辑、审核、删除；已审核（未核销、凭证未过账）：取消审核
  actionColumn(3, [t('fin.offset.cancelReview')])
]
const rowActions = (record) => {
  const draft = record.status === OffsetStatus.DRAFT
  return [
    { type: 'edit', show: draft, perm: 'fin:offset:edit', onClick: () => drawer.openEdit(record) },
    { type: 'audit', label: t('fin.offset.review'), show: draft, perm: 'fin:offset:review', onClick: () => review(record) },
    {
      type: 'unaudit',
      label: t('fin.offset.cancelReview'),
      show: !draft && !Number(record.billSettledAmount) && record.voucherStatus !== VoucherStatus.POSTED,
      perm: 'fin:offset:review',
      onClick: () => cancelReview(record)
    },
    { type: 'delete', show: draft, perm: 'fin:offset:remove', confirm: t('fin.offset.confirmDelete', { no: record.offsetNo }), onClick: () => remove(record.id) }
  ]
}
const { checked, visibleColumns, scrollX } = useColumns(columns, 'fin-offset')
const query = reactive({ kind: undefined, partnerId: undefined, item: undefined, status: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageOffset, query)
onMounted(load)
function onKindFilter() {
  query.partnerId = undefined
  search()
}

// ---------------------------------------------------------------- 新增 / 修改
const rules = {
  kind: [{ required: true, message: t('fin.offset.kindRequired') }],
  offsetDate: [{ required: true, message: t('fin.offset.dateRequired') }],
  partnerId: [{ required: true, message: t('fin.offset.partnerRequired') }],
  item: [{ required: true, message: t('fin.offset.itemRequired') }],
  amount: [{ required: true, message: t('fin.offset.amountRequired') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({
    kind: BillKind.AR,
    partnerId: undefined,
    offsetDate: today(),
    handlerId: undefined,
    item: undefined,
    amount: undefined,
    beginDate: undefined,
    endDate: undefined,
    ticketNo: '',
    accountId: undefined,
    remark: ''
  }),
  fetch: (r) => getOffset(r.id),
  create: addOffset,
  update: updateOffset,
  toPayload: (f) => ({
    kind: f.kind,
    partnerId: f.partnerId,
    offsetDate: f.offsetDate,
    handlerId: f.handlerId || null,
    item: f.item,
    amount: f.amount,
    beginDate: f.beginDate || null,
    endDate: f.endDate || null,
    ticketNo: f.ticketNo || null,
    accountId: f.accountId || null,
    remark: f.remark || null
  }),
  onSuccess: load
})
const { formRef, form } = drawer
const periodRange = computed({
  get: () => (form.value.beginDate || form.value.endDate ? [form.value.beginDate, form.value.endDate] : undefined),
  set: (v) => {
    form.value.beginDate = v?.[0] || undefined
    form.value.endDate = v?.[1] || undefined
  }
})

// ---------------------------------------------------------------- 复核、删除、查看
const review = useAction((r) => reviewOffset(r.id), {
  confirm: (r) => ({ title: t('fin.offset.reviewConfirm', { no: r.offsetNo }), content: r.kind === BillKind.AR ? t('fin.offset.reviewTipAr') : t('fin.offset.reviewTipAp') }),
  successKey: 'fin.offset.reviewed',
  reload: () => load()
})
const cancelReview = useAction((r) => cancelReviewOffset(r.id), {
  confirm: (r) => ({ title: t('fin.offset.cancelConfirm', { no: r.offsetNo }), content: r.kind === BillKind.AR ? t('fin.offset.cancelTipAr') : t('fin.offset.cancelTipAp') }),
  successKey: 'fin.offset.cancelled',
  reload: () => load()
})
const remove = useAction(deleteOffset, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

const viewOpen = ref(false)
const viewing = ref()
async function openView(record) {
  viewing.value = record
  viewOpen.value = true
  viewing.value = await getOffset(record.id)
}
// 从应收应付单的来源单号跳转过来（?id=冲账单ID）时打开详情
useRouteOpen({ id: (id) => openView({ id }) })

const voucherOpen = ref(false)
const voucherId = ref()
function openVoucher(id) {
  voucherId.value = id
  voucherOpen.value = true
}
</script>

<style scoped>
.filter {
  width: 150px;
}
.filter-wide {
  width: 220px;
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
