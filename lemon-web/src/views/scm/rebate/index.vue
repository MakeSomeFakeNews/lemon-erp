<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-input-search v-model:value="query.keyword" :placeholder="$t('scm.rebate.searchPlaceholder')" allow-clear class="filter-wide" @search="search" />
        <a-select v-model:value="query.status" :placeholder="$t('scm.rebate.allStatus')" allow-clear :options="toOptions(dict.scm_rebate_status)" class="filter" @change="search" />
        <template #extra>
          <a-button v-perm="'scm:rebate:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>{{ $t('scm.rebate.add') }}
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
          <template v-if="column.key === 'no'">
            <a @click="openDetail(record.id)">{{ record.rebateNo }}</a>
            <div class="sub-text">{{ record.customerName }}</div>
          </template>
          <template v-else-if="column.key === 'period'">{{ record.beginDate }} ~ {{ record.endDate }}</template>
          <template v-else-if="column.key === 'mode'"><DictTag type="scm_rebate_settle" :value="record.settleMode" /></template>
          <template v-else-if="column.key === 'result'">
            <template v-if="record.status === 'SETTLED'">
              ¥{{ formatMoney(record.rebateAmount) }}
              <div class="sub-text">{{ $t('scm.rebate.salesTimesRate', { sales: formatMoney(record.salesAmount), rate: Number(record.rate) }) }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="scm_rebate_status" :value="record.status" />
            <div v-if="record.billNo" class="sub-text">{{ $t('scm.rebate.creditNoteNo', { no: record.billNo }) }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">{{ $t('common.action.view') }}</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('scm.rebate.add')" :edit-title="$t('scm.rebate.edit')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('scm.rebate.customer')" name="customerId">
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
          <a-radio-group v-model:value="form.settleMode">
            <a-radio v-for="o in dict.scm_rebate_settle" :key="o.value" :value="o.value">{{ o.label }}</a-radio>
          </a-radio-group>
          <div class="sub-text">
            {{ form.settleMode === 'CREDIT_NOTE' ? $t('scm.rebate.creditNoteTip') : $t('scm.rebate.balanceTip') }}
          </div>
        </a-form-item>
        <div class="section-title">{{ $t('scm.rebate.tiers') }} <span class="sub-text">{{ $t('scm.rebate.tiersTip') }}</span></div>
        <a-table row-key="key" size="small" :columns="tierColumns" :data-source="form.tiers" :pagination="false">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'minAmount'">
              <a-input-number v-model:value="record.minAmount" :min="0" :precision="2" prefix="¥" class="full" />
            </template>
            <template v-else-if="column.key === 'rate'">
              <a-input-number v-model:value="record.rate" :min="0.01" :max="100" :precision="2" addon-after="%" class="full" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" danger size="small" :disabled="form.tiers.length === 1" @click="form.tiers.splice(index, 1)">{{ $t('common.action.delete') }}</a-button>
            </template>
          </template>
        </a-table>
        <a-button type="dashed" block class="add-line" @click="form.tiers.push(newTier())">
          <template #icon><PlusOutlined /></template>{{ $t('scm.rebate.addTier') }}
        </a-button>
        <a-form-item :label="$t('common.field.description')" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </FormDrawer>

    <SubPage :open="detailOpen" :title="detail ? $t('scm.rebate.detailTitle', { no: detail.rebateNo }) : ''" width="min(900px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space>
          <template v-if="detail.status === 'ACTIVE'">
            <span v-perm="'scm:rebate:add'">
              <a-popconfirm :title="$t('scm.rebate.confirmDelete')" @confirm="remove"><a-button danger>{{ $t('common.action.delete') }}</a-button></a-popconfirm>
            </span>
            <a-button v-perm="'scm:rebate:add'" @click="drawer.openEdit(detail)">{{ $t('common.action.edit') }}</a-button>
            <span v-perm="'scm:rebate:settle'">
              <a-popconfirm :title="settleTip" :disabled="!ended" @confirm="settle">
                <a-tooltip :title="ended ? '' : $t('scm.rebate.settleAfterEnd')">
                  <a-button type="primary" :disabled="!ended">{{ $t('scm.rebate.settle') }}</a-button>
                </a-tooltip>
              </a-popconfirm>
            </span>
          </template>
          <span v-else v-perm="'scm:rebate:settle'">
            <a-popconfirm :title="$t('scm.rebate.unsettleConfirm')" @confirm="unsettle">
              <a-button>{{ $t('scm.rebate.unsettle') }}</a-button>
            </a-popconfirm>
          </span>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('scm.rebate.customer')">{{ detail.customerName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.rebate.period')">{{ detail.beginDate }} ~ {{ detail.endDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="scm_rebate_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('scm.rebate.settleMode')"><DictTag type="scm_rebate_settle" :value="detail.settleMode" /></a-descriptions-item>
          <template v-if="detail.status === 'SETTLED'">
            <a-descriptions-item :label="$t('scm.rebate.settled')">{{ detail.settleDate }}</a-descriptions-item>
            <a-descriptions-item :label="$t('scm.rebate.rebate')">{{ $t('scm.rebate.rebateCalc', { rebate: formatMoney(detail.rebateAmount), sales: formatMoney(detail.salesAmount), rate: Number(detail.rate) }) }}</a-descriptions-item>
            <a-descriptions-item v-if="detail.billNo" :label="$t('scm.rebate.creditNote')">{{ detail.billNo }}</a-descriptions-item>
          </template>
          <template v-else-if="calc">
            <a-descriptions-item :label="ended ? $t('scm.rebate.due') : $t('scm.rebate.estimated')">
              {{ $t('scm.rebate.rebateCalc', { rebate: formatMoney(calc.rebateAmount), sales: formatMoney(calc.salesAmount), rate: Number(calc.rate) }) }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('scm.rebate.nextTier')">{{ nextTierText }}</a-descriptions-item>
          </template>
          <a-descriptions-item :label="$t('scm.rebate.created')">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.description')" :span="remarkSpan">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <div class="section-title">{{ $t('scm.rebate.tiers') }}</div>
        <a-table row-key="id" size="small" :columns="tierViewColumns" :data-source="detail.tiers" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'minAmount'">¥{{ formatMoney(record.minAmount) }}</template>
            <template v-else-if="column.key === 'rate'">{{ Number(record.rate) }}%</template>
            <template v-else-if="column.key === 'reached'">
              <a-tag v-if="reachedTier && reachedTier.id === record.id" color="green">{{ $t('scm.rebate.current') }}</a-tag>
            </template>
          </template>
        </a-table>
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { addRebate, calcRebate, deleteRebate, getRebate, pageRebate, settleRebate, unsettleRebate, updateRebate } from '@/api/scm/sourcing'
import { formatMoney } from '@/components/scm/scm'
import { today } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

const { t } = useI18n()

/** 销售返利：客户 × 期间的阶梯返利协议，期间结束后结算，生成红字应收或计入返利余额在下次订单抵扣 */
const dict = useDict('scm_rebate_status', 'scm_rebate_settle')

const columns = [
  { title: t('scm.rebate.agreement'), key: 'no', width: 220 },
  { title: t('scm.rebate.period'), key: 'period', width: 200 },
  { title: t('scm.rebate.settleMode'), key: 'mode', width: 140 },
  { title: t('scm.rebate.rebate'), key: 'result', width: 200 },
  { title: t('common.field.status'), key: 'status', width: 160 },
  { title: t('common.action.operation'), key: 'action', width: 90, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'scm-rebate')
const query = reactive({ keyword: undefined, status: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageRebate, query)
onMounted(load)

let seq = 0
const newTier = () => ({ key: ++seq, minAmount: undefined, rate: undefined })
const tierColumns = [
  { title: t('scm.rebate.salesReach'), key: 'minAmount' },
  { title: t('scm.rebate.rate'), key: 'rate', width: 160 },
  { title: '', key: 'action', width: 70 }
]
const rules = {
  customerId: [{ required: true, message: t('scm.rebate.ruleCustomer') }],
  beginDate: [{ required: true, message: t('common.rule.requiredSelect', { name: t('common.field.beginDate') }) }],
  endDate: [{ required: true, message: t('common.rule.requiredSelect', { name: t('common.field.endDate') }) }],
  settleMode: [{ required: true, message: t('scm.rebate.ruleSettleMode') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, customerId: undefined, beginDate: undefined, endDate: undefined, settleMode: 'CREDIT_NOTE', remark: '',
    tiers: [newTier()] }),
  fetch: async (record) => {
    const r = await getRebate(record.id)
    return { ...r, tiers: r.tiers.map((t) => ({ key: ++seq, minAmount: Number(t.minAmount), rate: Number(t.rate) })) }
  },
  create: async (data) => {
    const id = await addRebate(data)
    openDetail(id)
  },
  update: updateRebate,
  toPayload: (f) => ({ customerId: f.customerId, beginDate: f.beginDate, endDate: f.endDate, settleMode: f.settleMode, remark: f.remark || null,
    tiers: f.tiers.filter((t) => t.minAmount != null || t.rate != null).map((t) => ({ minAmount: t.minAmount, rate: t.rate })) }),
  onSuccess: () => {
    load()
    if (detailOpen.value) openDetail(detail.value.id)
  }
})
const { formRef, form } = drawer

const tierViewColumns = [
  { title: t('scm.rebate.salesReach'), key: 'minAmount' },
  { title: t('scm.rebate.rate'), key: 'rate', width: 120 },
  { title: '', key: 'reached', width: 80 }
]
const detailOpen = ref(false)
const detail = ref()
const calc = ref()
const ended = computed(() => detail.value && detail.value.endDate < today())
const salesNow = computed(() => Number(detail.value?.status === 'SETTLED' ? detail.value.salesAmount : calc.value?.salesAmount) || 0)
const reachedTier = computed(() => [...(detail.value?.tiers || [])].filter((t) => salesNow.value >= Number(t.minAmount))
  .sort((a, b) => Number(b.minAmount) - Number(a.minAmount))[0])
const nextTierText = computed(() => {
  const next = [...(detail.value?.tiers || [])].filter((t) => salesNow.value < Number(t.minAmount))
    .sort((a, b) => Number(a.minAmount) - Number(b.minAmount))[0]
  return next ? t('scm.rebate.nextTierText', { amount: formatMoney(Number(next.minAmount) - salesNow.value), rate: Number(next.rate) }) : t('scm.rebate.topTier')
})
/** 说明占满最后一行：前面的项数为 客户、期间、状态、兑付方式、创建 + 结算信息（已结算 2~3 项，执行中 2 项） */
const remarkSpan = computed(() => {
  const d = detail.value
  const n = 5 + (d?.status === 'SETTLED' ? (d.billNo ? 3 : 2) : calc.value ? 2 : 0)
  return 3 - (n % 3)
})
const settleTip = computed(() => calc.value
  ? t('scm.rebate.settleConfirm', { sales: formatMoney(calc.value.salesAmount), rebate: formatMoney(calc.value.rebateAmount) }) : t('scm.rebate.settleConfirmPlain'))
async function openDetail(id) {
  const r = await getRebate(id)
  detail.value = r
  calc.value = r.status === 'ACTIVE' ? await calcRebate(id) : undefined
  detailOpen.value = true
}
async function settle() {
  const res = await settleRebate(detail.value.id)
  message.success(t('scm.rebate.settledMsg', { amount: formatMoney(res.rebateAmount) }))
  openDetail(detail.value.id)
  load()
}
async function unsettle() {
  await unsettleRebate(detail.value.id)
  message.success(t('scm.rebate.unsettledMsg'))
  openDetail(detail.value.id)
  load()
}
async function remove() {
  await deleteRebate(detail.value.id)
  message.success(t('common.msg.deleted'))
  detailOpen.value = false
  load()
}
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
.block {
  margin-bottom: 16px;
}
.add-line {
  margin: 8px 0 16px;
}
.section-title {
  margin: 12px 0 8px;
  font-weight: 600;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
  font-weight: normal;
}
</style>
