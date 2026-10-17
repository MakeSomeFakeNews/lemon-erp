<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" :placeholder="$t('mes.doc.allStatus')" allow-clear :options="toOptions(dict.qms_claim_status)" class="filter" @change="search" />
        <PartnerSelect v-model:value="query.supplierId" type="supplier" allow-clear :placeholder="$t('mes.mrp.supplier')" class="filter-wide" @update:value="search" />
        <a-input-search v-model:value="query.keyword" :placeholder="$t('qms.claim.keywordPh')" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <a-button v-perm="'qms:claim:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>{{ $t('qms.claim.add') }}
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
        :scroll="{ x: 1120, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'claimNo'">
            {{ record.claimNo }}
            <div v-if="record.ncrNo" class="sub-text">{{ $t('qms.claim.ncrNo', { no: record.ncrNo }) }}</div>
          </template>
          <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="qms_claim_status" :value="record.status" />
            <div v-if="record.billNo" class="sub-text">{{ $t('qms.claim.billNo', { no: record.billNo }) }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <template v-if="record.status === 'DRAFT'">
                <a-button v-perm="'qms:claim:add'" type="link" size="small" @click="drawer.openEdit(record)">{{ $t('common.action.edit') }}</a-button>
                <span v-perm="'qms:claim:confirm'">
                  <a-popconfirm :title="$t('qms.claim.confirmConfirm')" @confirm="confirm(record.id)">
                    <a-button type="link" size="small">{{ $t('mes.plan.confirm') }}</a-button>
                  </a-popconfirm>
                </span>
                <span v-perm="'qms:claim:remove'">
                  <a-popconfirm :title="$t('qms.claim.deleteConfirm')" @confirm="remove(record.id)">
                    <a-button type="link" danger size="small">{{ $t('common.action.delete') }}</a-button>
                  </a-popconfirm>
                </span>
              </template>
              <span v-else-if="record.status === 'CONFIRMED'" v-perm="'qms:claim:confirm'">
                <a-popconfirm :title="$t('qms.claim.cancelConfirm')" @confirm="cancel(record.id)">
                  <a-button type="link" danger size="small">{{ $t('common.action.cancel') }}</a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('qms.claim.addTitle')" :edit-title="$t('qms.claim.editTitle')">
      <a-alert type="info" show-icon class="block" :message="$t('qms.claim.tip')" />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('mes.mrp.supplier')" name="supplierId">
          <PartnerSelect v-model:value="form.supplierId" type="supplier" :label="form.supplierName" />
        </a-form-item>
        <a-form-item :label="$t('qms.claim.ncr')" name="ncrId">
          <a-select
            v-model:value="form.ncrId"
            show-search
            allow-clear
            :filter-option="false"
            :options="ncrOptions"
            :placeholder="$t('qms.claim.ncrPh')"
            @search="searchNcr"
            @focus="searchNcr('')"
            @change="onNcrChange"
          />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('qms.claim.claimDate')" name="claimDate">
              <a-date-picker v-model:value="form.claimDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('qms.claim.amount')" name="amount">
              <a-input-number v-model:value="form.amount" :min="0.01" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('qms.claim.reason')" name="reason">
          <a-textarea v-model:value="form.reason" :rows="3" :maxlength="500" :placeholder="$t('qms.claim.reasonPh')" />
        </a-form-item>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { addClaim, cancelClaim, confirmClaim, deleteClaim, getClaim, getNcr, pageClaim, pageNcr, updateClaim } from '@/api/qms'
import { formatMoney } from '@/components/fin/fin'
import { today } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

/** 供应商质量索赔：确认后冲减应付 */
const dict = useDict('qms_claim_status')
const { t } = useI18n()
const route = useRoute()

const columns = [
  { title: t('qms.claim.claimNo'), key: 'claimNo', width: 190 },
  { title: t('mes.mrp.supplier'), dataIndex: 'supplierName', width: 180, ellipsis: true },
  { title: t('qms.claim.claimDate'), dataIndex: 'claimDate', width: 110 },
  { title: t('qms.claim.amount'), key: 'amount', width: 130, align: 'right' },
  { title: t('qms.claim.reason'), dataIndex: 'reason', width: 220, ellipsis: true },
  { title: t('qms.claim.statusOffset'), key: 'status', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: 170, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'qms-claim')
const query = reactive({ status: undefined, supplierId: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageClaim, query)

const rules = {
  supplierId: [{ required: true, message: t('qms.claim.supplierRequired') }],
  claimDate: [{ required: true, message: t('qms.claim.claimDateRequired') }],
  amount: [{ required: true, message: t('qms.claim.amountRequired') }],
  reason: [{ required: true, whitespace: true, message: t('qms.claim.reasonRequired') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, supplierId: undefined, ncrId: undefined, claimDate: today(), amount: undefined, reason: '' }),
  fetch: async (record) => {
    const c = await getClaim(record.id)
    if (c.ncrId) ncrOptions.value = [{ value: c.ncrId, label: c.ncrNo }]
    return { ...c, amount: Number(c.amount) }
  },
  create: addClaim,
  update: updateClaim,
  toPayload: (f) => ({ supplierId: f.supplierId, ncrId: f.ncrId ?? null, claimDate: f.claimDate, amount: f.amount, reason: f.reason }),
  onSuccess: load
})
const { formRef, form } = drawer

const ncrOptions = ref([])
const ncrMap = new Map()
async function searchNcr(keyword) {
  const page = await pageNcr({ keyword: keyword || undefined, size: 20 })
  page.records.forEach((n) => ncrMap.set(n.id, n))
  ncrOptions.value = page.records
    .filter((n) => n.supplierId && n.status !== 'CANCELLED')
    .map((n) => ({ value: n.id, label: `${n.ncrNo} ${n.supplierName} ${n.materialName}` }))
}
function onNcrChange(id) {
  const n = ncrMap.get(id)
  if (n && !form.value.supplierId) {
    form.value.supplierId = n.supplierId
    form.value.supplierName = n.supplierName
  }
}

onMounted(async () => {
  await load()
  // 从不合格品处理单「供应商索赔」进入
  const ncrId = Number(route.query.ncrId)
  if (ncrId) {
    const n = await getNcr(ncrId)
    ncrMap.set(n.id, n)
    ncrOptions.value = [{ value: n.id, label: `${n.ncrNo} ${n.supplierName || ''} ${n.materialName}` }]
    drawer.openCreate({ ncrId, supplierId: n.supplierId, supplierName: n.supplierName, reason: n.description })
  }
})

async function confirm(id) {
  await confirmClaim(id)
  message.success(t('qms.claim.confirmed'))
  load()
}
async function cancel(id) {
  await cancelClaim(id)
  message.success(t('mes.ecn.cancelled'))
  load()
}
async function remove(id) {
  await deleteClaim(id)
  message.success(t('common.msg.deleteSuccess'))
  load()
}
</script>

<style scoped>
.filter {
  width: 140px;
}
.filter-wide {
  width: 220px;
}
.block {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
