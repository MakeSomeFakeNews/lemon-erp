<template>
  <FormDrawer page :drawer="drawer" :create-title="title" :edit-title="editTitle" width="min(1000px, 100vw)">
    <a-alert type="info" show-icon class="form-tip" :message="income ? $t('fin.io.formTipIn') : $t('fin.io.formTipOut')" />
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('fin.io.date')" name="docDate">
            <a-date-picker v-model:value="form.docDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('fin.io.ticketNo')" name="ticketNo">
            <a-input v-model:value="form.ticketNo" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('fin.io.settleMethod')" name="settleMethod">
            <a-select v-model:value="form.settleMethod" :options="settleOptions" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('fin.io.account')" name="accountId">
            <AccountSelect v-model:value="form.accountId" :roles="['CASH', 'BANK']" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('fin.io.partner')" name="partnerId" :extra="$t('fin.io.partnerExtra')">
            <div class="partner">
              <a-select v-model:value="form.partnerType" :options="toOptions(dict.fin_io_partner_type)" allow-clear :placeholder="$t('fin.io.noPartner')" class="partner-type" @change="onPartnerType" />
              <PartnerSelect
                v-if="form.partnerType === IoPartnerType.CUSTOMER || form.partnerType === IoPartnerType.SUPPLIER"
                v-model:value="form.partnerId"
                :type="form.partnerType === IoPartnerType.CUSTOMER ? 'customer' : 'supplier'"
                :label="form.partnerLabel"
                class="partner-value"
              />
              <a-input v-else-if="form.partnerType === IoPartnerType.OTHER" v-model:value="form.partnerName" :maxlength="128" :placeholder="$t('fin.io.partnerNamePh')" class="partner-value" />
            </div>
          </a-form-item>
        </a-col>
        <a-col v-if="!income" :xs="24" :sm="12">
          <a-form-item :label="$t('fin.io.dept')" name="deptId" :extra="$t('fin.io.deptExtra')">
            <DeptSelect v-model:value="form.deptId" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
    <div class="items-head">
      <span class="section-title">{{ $t('fin.io.items') }}</span>
      <a-space>
        <span>{{ $t('common.field.total') }} <b class="total">¥{{ formatMoney(itemTotal) }}</b></span>
        <a-button size="small" @click="lines.addRow()"><template #icon><PlusOutlined /></template>{{ $t('common.action.addRow') }}</a-button>
      </a-space>
    </div>
    <LineItemsTable :items="form.items" :columns="itemColumns" :min-rows="1">
      <template #cell="{ column, record }">
        <template v-if="column.key === 'categoryId'">
          <a-select
            v-model:value="record.categoryId"
            :options="categoryOptions"
            show-search
            option-filter-prop="label"
            :placeholder="$t('fin.io.categoryPh')"
            class="full"
          />
        </template>
        <template v-else-if="column.key === 'account'">
          <span class="sub-text">{{ categoryAccount(record.categoryId) }}</span>
        </template>
        <template v-else-if="column.key === 'amount'">
          <a-input-number v-model:value="record.amount" :min="0.01" :precision="2" class="full" />
        </template>
        <template v-else-if="column.key === 'remark'">
          <a-input v-model:value="record.remark" :maxlength="255" :placeholder="$t('fin.io.itemRemarkPh')" />
        </template>
      </template>
    </LineItemsTable>
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import LineItemsTable from '@/components/common/LineItemsTable.vue'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import DeptSelect from '@/components/fin/DeptSelect.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { addIo, getIo, listIoCategory, updateIo } from '@/api/fin'
import { formatMoney } from '@/components/fin/fin'
import { today } from '@/components/wms/wms'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { rowKey, useLineItems } from '@/composables/useLineItems'
import { optionsWithCurrent, toOptions } from '@/utils/dict'
import { FundDirection, IoPartnerType } from '@/constants/enums'
import { SETTLE_METHOD_NOTE, STATUS_ENABLED } from '@/utils/constants'

/** 其他收入单 / 费用支出单的新增、修改（待确认时）；保存后由列表或详情确认生成凭证 */
const emit = defineEmits(['saved'])
const { t } = useI18n()
const dict = useDict('fin_io_partner_type', 'fin_settle_method')
const direction = ref(FundDirection.IN)
const income = computed(() => direction.value === FundDirection.IN)
const title = computed(() => (income.value ? t('fin.io.createIn') : t('fin.io.createOut')))
const editTitle = computed(() => (income.value ? t('fin.io.editIn') : t('fin.io.editOut')))
/** 票据结算由票据管理办理，不能选择 */
const settleOptions = computed(() => optionsWithCurrent(dict.fin_settle_method, drawer.form.value.settleMethod).filter((o) => o.value !== SETTLE_METHOD_NOTE))

// 收支类别：启用的、与单据方向一致的（修改时补上已停用的原类别）
const categories = ref([])
const categoryOptions = computed(() => categories.value
  .filter((c) => c.direction === direction.value && (c.status === STATUS_ENABLED || drawer.form.value.items.some((i) => i.categoryId === c.id)))
  .map((c) => ({ value: c.id, label: c.name })))
const categoryAccount = (id) => {
  const c = categories.value.find((x) => x.id === id)
  return c ? (c.accountCode ? `${c.accountCode} ${c.accountName}` : t('fin.io.noAccount')) : ''
}

const emptyItem = () => ({ key: rowKey(), categoryId: undefined, amount: undefined, remark: '' })
const itemColumns = [
  { title: t('fin.io.category'), key: 'categoryId', width: 220 },
  { title: t('fin.io.categoryAccount'), key: 'account', width: 200 },
  { title: t('common.field.amount'), key: 'amount', width: 150 },
  { title: t('common.field.remark'), key: 'remark' },
  { title: '', key: 'action', width: 50 }
]
const rules = {
  docDate: [{ required: true, message: t('fin.io.dateRequired') }],
  accountId: [{ required: true, message: t('fin.io.accountRequired') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined, docDate: today(), ticketNo: '', partnerType: undefined, partnerId: undefined, partnerName: '', partnerLabel: undefined,
    settleMethod: undefined, accountId: undefined, deptId: undefined, remark: '', items: [emptyItem()]
  }),
  beforeOpen: async () => {
    categories.value = await listIoCategory()
  },
  fetch: async (record) => {
    const d = await getIo(record.id)
    direction.value = d.direction
    return {
      id: d.id,
      docDate: d.docDate,
      ticketNo: d.ticketNo || '',
      partnerType: d.partnerType || undefined,
      partnerId: d.partnerId || undefined,
      partnerName: d.partnerName || '',
      partnerLabel: d.partnerLabel,
      settleMethod: d.settleMethod || undefined,
      accountId: d.accountId,
      deptId: d.deptId || undefined,
      remark: d.remark || '',
      items: d.items.map((i) => ({ key: rowKey(), categoryId: i.categoryId, amount: Number(i.amount), remark: i.remark || '' }))
    }
  },
  create: async (data) => emit('saved', await addIo(data)),
  update: async (id, data) => {
    await updateIo(id, data)
    emit('saved', id)
  },
  toPayload: (f) => ({
    direction: direction.value,
    docDate: f.docDate,
    ticketNo: f.ticketNo || null,
    partnerType: f.partnerType || null,
    partnerId: f.partnerId || null,
    partnerName: f.partnerName || null,
    settleMethod: f.settleMethod || null,
    accountId: f.accountId,
    deptId: income.value ? null : f.deptId || null,
    remark: f.remark || null,
    items: f.items.filter((i) => i.categoryId || i.amount).map((i) => ({ categoryId: i.categoryId, amount: i.amount, remark: i.remark || null }))
  })
})
const { formRef, form } = drawer
const lines = useLineItems(() => form.value.items, { newRow: emptyItem })
const itemTotal = lines.total('amount')

function onPartnerType() {
  form.value.partnerId = undefined
  form.value.partnerLabel = undefined
}

defineExpose({
  /** 新增：dir 为 IN 其他收入 / OUT 费用支出 */
  openCreate: (dir) => {
    direction.value = dir
    return drawer.openCreate()
  },
  openEdit: (record) => {
    direction.value = record.direction
    return drawer.openEdit(record)
  }
})
</script>

<style scoped>
.form-tip {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
.partner {
  display: flex;
  gap: 8px;
}
.partner-type {
  width: 120px;
  flex: none;
}
.partner-value {
  flex: 1;
  min-width: 0;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 12px;
}
.section-title {
  font-weight: 500;
}
.total {
  font-size: 16px;
  color: var(--ant-color-primary, #1677ff);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
