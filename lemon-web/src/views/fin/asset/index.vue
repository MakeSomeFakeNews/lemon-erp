<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="cards" :tab="$t('fin.asset.tabCards')">
          <div class="toolbar">
            <a-space wrap>
              <a-input-search v-model:value="query.keyword" :placeholder="$t('fin.asset.keywordPh')" allow-clear class="filter" @search="search" />
              <a-select v-model:value="query.category" :placeholder="$t('fin.asset.category')" allow-clear :options="toOptions(dict.fin_asset_category)" class="filter-s" @change="search" />
              <a-select v-model:value="query.status" :placeholder="$t('common.field.status')" allow-clear :options="toOptions(dict.fin_asset_status)" class="filter-s" @change="search" />
            </a-space>
            <a-button v-perm="'fin:asset:add'" type="primary" @click="drawer.openCreate()">
              <template #icon><PlusOutlined /></template>{{ $t('fin.asset.add') }}
            </a-button>
          </div>
          <a-table
            row-key="id"
            size="middle"
            :columns="columns"
            :data-source="list"
            :loading="loading"
            :pagination="pagination"
            :scroll="{ x: 1720 }"
            @change="onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <div>{{ record.name }}</div>
                <div class="sub-text">{{ record.code }} {{ record.model }}</div>
              </template>
              <template v-else-if="column.key === 'user'">
                <div>{{ record.deptName || '-' }}<span v-if="record.custodianName"> · {{ record.custodianName }}</span></div>
                <div class="sub-text">{{ record.location }}</div>
              </template>
              <template v-else-if="column.key === 'category'"><DictTag type="fin_asset_category" :value="record.category" /></template>
              <template v-else-if="column.key === 'value'">
                <div>¥{{ formatMoney(record.originalValue) }}</div>
                <div class="sub-text">{{ $t('fin.asset.residualRateText', { rate: Number(record.residualRate) }) }}</div>
              </template>
              <template v-else-if="column.key === 'depreciation'">
                <a-progress :percent="Math.round((record.depreciatedMonths / record.usefulMonths) * 100)" size="small" />
                <div class="sub-text">{{ $t('fin.asset.progressText', { done: record.depreciatedMonths, total: record.usefulMonths, monthly: formatMoney(record.monthlyDepreciation) }) }}</div>
              </template>
              <template v-else-if="column.key === 'net'">
                <div>¥{{ formatMoney(record.netValue) }}</div>
                <div class="sub-text">{{ $t('fin.asset.accumulatedText', { amount: formatMoney(record.accumulated) }) }}</div>
              </template>
              <template v-else-if="column.key === 'status'">
                <DictTag type="fin_asset_status" :value="record.status" />
                <div v-if="record.disposeDate" class="sub-text">{{ record.disposeDate }}</div>
              </template>
              <template v-else-if="column.key === 'action'">
                <span class="table-actions">
                  <template v-if="record.status !== 'DISPOSED'">
                    <a-button v-perm="'fin:asset:edit'" type="link" size="small" @click="drawer.openEdit(record)">{{ $t('common.action.edit') }}</a-button>
                    <a-dropdown v-perm="'fin:asset:move'">
                      <a-button type="link" size="small">{{ $t('fin.asset.move') }} <DownOutlined /></a-button>
                      <template #overlay>
                        <a-menu @click="({ key }) => openMove(record, key)">
                          <a-menu-item v-if="record.status === 'IDLE'" key="ASSIGN">{{ $t('fin.asset.assign') }}</a-menu-item>
                          <a-menu-item v-if="record.status === 'IN_USE'" key="RETURN">{{ $t('fin.asset.return') }}</a-menu-item>
                          <a-menu-item key="TRANSFER">{{ $t('fin.asset.transfer') }}</a-menu-item>
                        </a-menu>
                      </template>
                    </a-dropdown>
                  </template>
                  <a-button type="link" size="small" @click="openHistory(record)">{{ $t('fin.asset.history') }}</a-button>
                  <template v-if="record.status !== 'DISPOSED'">
                    <a-button v-perm="'fin:asset:edit'" type="link" size="small" @click="dispose(record)">{{ $t('fin.asset.dispose') }}</a-button>
                    <span v-if="record.depreciatedMonths === 0" v-perm="'fin:asset:remove'">
                      <a-popconfirm :title="$t('fin.asset.confirmDelete', { name: record.name })" @confirm="remove(record.id)">
                        <a-button type="link" danger size="small">{{ $t('common.action.delete') }}</a-button>
                      </a-popconfirm>
                    </span>
                  </template>
                </span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="depreciation" :tab="$t('fin.asset.tabDepreciation')">
          <div class="toolbar">
            <a-space wrap>
              <span>{{ $t('fin.asset.period') }}</span>
              <PeriodPicker v-model:value="depPeriod" @update:value="records.search" />
              <a-button v-perm="'fin:asset:depreciate'" type="primary" :loading="depreciating" @click="runDepreciation">{{ $t('fin.asset.runDepreciation') }}</a-button>
              <span v-perm="'fin:asset:depreciate'">
                <a-popconfirm :title="$t('fin.asset.confirmUndo')" @confirm="undo">
                  <a-button>{{ $t('fin.asset.undo') }}</a-button>
                </a-popconfirm>
              </span>
            </a-space>
            <span class="sub-text">{{ $t('fin.asset.depHint') }}</span>
          </div>
          <a-table
            row-key="id"
            size="middle"
            :columns="recordColumns"
            :data-source="records.list.value"
            :loading="records.loading.value"
            :pagination="records.pagination"
            @change="records.onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'asset'">{{ record.assetCode }} {{ record.assetName }}</template>
              <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
              <template v-else-if="column.key === 'voucher'">{{ record.voucherNo || '-' }}</template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <FormDrawer page :drawer="drawer" :create-title="$t('fin.asset.add')" :edit-title="$t('fin.asset.edit')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.asset.code')" name="code">
              <a-input v-model:value="form.code" :maxlength="32" :placeholder="$t('fin.asset.codePh')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.asset.name')" name="name">
              <a-input v-model:value="form.name" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.asset.category')" name="category">
              <a-select v-model:value="form.category" :options="toOptions(dict.fin_asset_category)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.asset.model')" name="model">
              <a-input v-model:value="form.model" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.asset.useDept')" name="deptId">
              <DeptSelect v-model:value="form.deptId" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.asset.custodian')" name="custodianId">
              <IdentityUserSelect v-model:value="form.custodianId" :label="form.custodianName" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.asset.location')" name="location">
              <a-input v-model:value="form.location" :maxlength="128" :placeholder="$t('fin.asset.locationPh')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.asset.useDate')" name="useDate" :extra="$t('fin.asset.useDateExtra')">
              <a-date-picker v-model:value="form.useDate" value-format="YYYY-MM-DD" :disabled="locked" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('fin.asset.originalValue')" name="originalValue">
              <a-input-number v-model:value="form.originalValue" :min="0" :precision="2" prefix="¥" :disabled="locked" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="8">
            <a-form-item :label="$t('fin.asset.residualRate')" name="residualRate">
              <a-input-number v-model:value="form.residualRate" :min="0" :max="100" :precision="2" addon-after="%" :disabled="locked" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="8">
            <a-form-item :label="$t('fin.asset.usefulMonths')" name="usefulMonths">
              <a-input-number v-model:value="form.usefulMonths" :min="1" :max="1200" :addon-after="$t('fin.asset.month')" :disabled="locked" class="full" />
            </a-form-item>
          </a-col>
          <template v-if="!form.id">
            <a-col :xs="12" :sm="12">
              <a-form-item :label="$t('fin.asset.openingMonths')" name="openingMonths" :extra="$t('fin.asset.openingMonthsExtra')">
                <a-input-number v-model:value="form.openingMonths" :min="0" :max="form.usefulMonths || 1200" :addon-after="$t('fin.asset.month')" class="full" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :sm="12">
              <a-form-item :label="$t('fin.asset.openingAccumulated')" name="openingAccumulated">
                <a-input-number v-model:value="form.openingAccumulated" :min="0" :precision="2" prefix="¥" class="full" />
              </a-form-item>
            </a-col>
            <template v-if="!form.openingMonths">
              <a-col :xs="24" :sm="14">
                <a-form-item :label="$t('fin.asset.supplier')" name="supplierId" :extra="$t('fin.asset.supplierExtra')">
                  <PartnerSelect v-model:value="form.supplierId" type="supplier" allow-clear />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="10">
                <a-form-item :label="$t('fin.asset.taxAmount')" name="taxAmount">
                  <a-input-number v-model:value="form.taxAmount" :min="0" :precision="2" prefix="¥" :disabled="!form.supplierId" class="full" />
                </a-form-item>
              </a-col>
              <a-col v-if="form.supplierId" :span="24">
                <a-alert
                  type="info"
                  show-icon
                  class="supplier-tip"
                  :message="$t('fin.asset.supplierTip', { amount: formatMoney((Number(form.originalValue) || 0) + (Number(form.taxAmount) || 0)) })"
                />
              </a-col>
            </template>
          </template>
          <a-col :span="24">
            <a-form-item :label="$t('fin.asset.expenseAccount')" name="expenseAccountId" :extra="$t('fin.asset.expenseAccountExtra')">
              <AccountSelect v-model:value="form.expenseAccountId" :prefix="['5101', '6601', '6602']" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.status')" name="status">
              <a-radio-group v-model:value="form.status">
                <a-radio value="IN_USE">{{ $t('fin.asset.inUse') }}</a-radio>
                <a-radio value="IDLE">{{ $t('fin.asset.idle') }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-alert v-if="monthly" type="info" :message="$t('fin.asset.monthlyTip', { amount: formatMoney(monthly) })" show-icon />
      </a-form>
    </FormDrawer>
    <a-modal v-model:open="disposeOpen" :title="$t('fin.asset.disposeTitle', { name: disposeTarget?.name || '' })" :confirm-loading="disposing" @ok="submitDispose">
      <a-alert
        type="info"
        show-icon
        class="dispose-tip"
        :message="$t('fin.asset.disposeTip')"
      />
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('fin.asset.disposeDate')" required>
              <a-date-picker v-model:value="disposeForm.date" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('fin.asset.settleAccount')" :extra="disposeForm.income || disposeForm.expense ? '' : $t('fin.asset.settleAccountExtra')">
              <AccountSelect v-model:value="disposeForm.settleAccountId" :prefix="['1001', '1002']" :placeholder="$t('fin.asset.settleAccountPh')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('fin.asset.disposeIncome')">
              <a-input-number v-model:value="disposeForm.income" :min="0" :precision="2" :addon-after="$t('fin.asset.yuan')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('fin.asset.disposeExpense')">
              <a-input-number v-model:value="disposeForm.expense" :min="0" :precision="2" :addon-after="$t('fin.asset.yuan')" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <div v-if="disposeTarget" class="sub-text">
          {{ $t(disposeNet >= 0 ? 'fin.asset.disposeLoss' : 'fin.asset.disposeGain', { net: formatMoney(disposeTarget.netValue), amount: formatMoney(Math.abs(disposeNet)) }) }}
        </div>
      </a-form>
    </a-modal>
    <a-modal
      v-model:open="moveOpen"
      :title="$t('fin.asset.moveTitle', { type: MOVE_TITLES[moveForm.moveType], name: moveTarget?.name || '' })"
      :confirm-loading="moving"
      @ok="submitMove"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('common.field.date')" required>
              <a-date-picker v-model:value="moveForm.moveDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <template v-if="moveForm.moveType !== 'RETURN'">
            <a-col :span="12">
              <a-form-item :label="$t('fin.asset.useDept')" :required="moveForm.moveType === 'ASSIGN'">
                <DeptSelect v-model:value="moveForm.deptId" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item :label="$t('fin.asset.custodian')">
                <IdentityUserSelect v-model:value="moveForm.userId" :label="moveForm.userName" />
              </a-form-item>
            </a-col>
          </template>
          <a-col :span="12">
            <a-form-item :label="moveForm.moveType === 'RETURN' ? $t('fin.asset.returnLocation') : $t('fin.asset.location')">
              <a-input v-model:value="moveForm.location" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.description')">
              <a-input v-model:value="moveForm.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
        <div v-if="moveForm.moveType === 'RETURN'" class="sub-text">{{ $t('fin.asset.returnHint') }}</div>
        <div v-else-if="moveForm.moveType === 'ASSIGN'" class="sub-text">{{ $t('fin.asset.assignHint') }}</div>
      </a-form>
    </a-modal>
    <a-modal v-model:open="historyOpen" :title="$t('fin.asset.historyTitle', { code: historyTarget?.code || '', name: historyTarget?.name || '' })" width="min(1000px, 100vw)" :footer="null">
      <a-table row-key="id" size="small" :columns="historyColumns" :data-source="history" :pagination="false" :scroll="{ x: 980 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'"><DictTag type="fin_asset_move_type" :value="record.moveType" /></template>
          <template v-else-if="column.key === 'dept'">{{ arrow(record.fromDeptName, record.toDeptName) }}</template>
          <template v-else-if="column.key === 'user'">{{ arrow(record.fromUserName, record.toUserName) }}</template>
          <template v-else-if="column.key === 'location'">{{ arrow(record.fromLocation, record.toLocation) }}</template>
          <template v-else-if="column.key === 'by'">
            {{ record.createByName }}
            <div class="sub-text">{{ record.createTime }}</div>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { DownOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import DeptSelect from '@/components/fin/DeptSelect.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { addAsset, assetMoves, deleteAsset, moveAsset, depreciate, disposeAsset, getAsset, pageAsset, pageDepreciation, undoDepreciation, updateAsset } from '@/api/fin'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { today } from '@/components/wms/wms'
import { currentPeriod, formatMoney } from '@/components/fin/fin'

const { t } = useI18n()
const dict = useDict('fin_asset_category', 'fin_asset_status', 'fin_asset_move_type')
const tab = ref('cards')

const columns = [
  { title: t('fin.asset.asset'), key: 'name', width: 220 },
  { title: t('fin.asset.category'), key: 'category', width: 110 },
  { title: t('fin.asset.colUser'), key: 'user', width: 200 },
  { title: t('fin.asset.colUseDate'), dataIndex: 'useDate', width: 110 },
  { title: t('fin.asset.originalValue'), key: 'value', width: 150, align: 'right' },
  { title: t('fin.asset.colProgress'), key: 'depreciation', width: 240 },
  { title: t('fin.asset.netValue'), key: 'net', width: 170, align: 'right' },
  { title: t('fin.asset.colExpenseAccount'), dataIndex: 'expenseAccountName', width: 140 },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('common.action.operation'), key: 'action', width: 280, fixed: 'right' }
]
const query = reactive({ keyword: undefined, category: undefined, status: undefined })
const { list, loading, pagination, load, search, onTableChange } = useTable(pageAsset, query)
onMounted(load)

const rules = {
  code: [{ required: true, message: t('common.rule.required', { name: t('fin.asset.code') }) }],
  name: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('fin.asset.name') }) }],
  category: [{ required: true, message: t('common.rule.requiredSelect', { name: t('fin.asset.category') }) }],
  useDate: [{ required: true, message: t('common.rule.requiredSelect', { name: t('fin.asset.useDate') }) }],
  originalValue: [{ required: true, message: t('common.rule.required', { name: t('fin.asset.originalValue') }) }],
  usefulMonths: [{ required: true, message: t('common.rule.required', { name: t('fin.asset.usefulMonths') }) }],
  expenseAccountId: [{ required: true, message: t('common.rule.requiredSelect', { name: t('fin.asset.expenseAccount') }) }]
}
const FIELDS = ['code', 'name', 'category', 'model', 'deptId', 'custodianId', 'location', 'useDate', 'originalValue', 'residualRate', 'usefulMonths', 'expenseAccountId', 'status', 'remark',
  'openingMonths', 'openingAccumulated', 'supplierId', 'taxAmount']
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, code: '', name: '', category: 'ELECTRONIC', model: '', deptId: undefined, custodianId: undefined, location: '', useDate: today(), originalValue: undefined,
    residualRate: 5, usefulMonths: 36, expenseAccountId: undefined, status: 'IN_USE', remark: '', depreciatedMonths: 0,
    supplierId: undefined, taxAmount: undefined }),
  fetch: async (record) => {
    const a = await getAsset(record.id)
    return { ...a, custodianName: record.custodianName, originalValue: Number(a.originalValue), residualRate: Number(a.residualRate) }
  },
  create: addAsset,
  update: updateAsset,
  toPayload: (f) => {
    const data = Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]]))
    // 只有新增的非期初资产可以登记采购供应商
    if (f.id || f.openingMonths || !f.supplierId) Object.assign(data, { supplierId: null, taxAmount: null })
    return data
  },
  onSuccess: load
})
const { formRef, form } = drawer
const locked = computed(() => form.value.depreciatedMonths > 0)
const monthly = computed(() => {
  const f = form.value
  if (!f.originalValue || !f.usefulMonths) return 0
  return (f.originalValue * (1 - (f.residualRate || 0) / 100)) / f.usefulMonths
})

const disposeOpen = ref(false)
const disposing = ref(false)
const disposeTarget = ref()
const disposeForm = reactive({ date: undefined, income: undefined, expense: undefined, settleAccountId: undefined, remark: '' })
const disposeNet = computed(() => {
  const r = disposeTarget.value
  if (!r) return 0
  return Number(r.netValue) + (Number(disposeForm.expense) || 0) - (Number(disposeForm.income) || 0)
})
function dispose(record) {
  disposeTarget.value = record
  Object.assign(disposeForm, { date: today(), income: undefined, expense: undefined, settleAccountId: undefined, remark: '' })
  disposeOpen.value = true
}
async function submitDispose() {
  if ((disposeForm.income || disposeForm.expense) && !disposeForm.settleAccountId) {
    message.warning(t('fin.asset.needSettleAccount'))
    return
  }
  disposing.value = true
  try {
    await disposeAsset(disposeTarget.value.id, { ...disposeForm })
    message.success(t('fin.asset.disposed'))
    disposeOpen.value = false
    load()
  } finally {
    disposing.value = false
  }
}

// ---------------------------------------------------------------- 领用 / 归还 / 调拨、履历
const MOVE_TITLES = { ASSIGN: t('fin.asset.assign'), RETURN: t('fin.asset.return'), TRANSFER: t('fin.asset.transfer') }
const moveOpen = ref(false)
const moving = ref(false)
const moveTarget = ref()
const moveForm = reactive({ moveType: 'TRANSFER', moveDate: undefined, deptId: undefined, userId: undefined, location: '', remark: '' })
function openMove(record, type) {
  moveTarget.value = record
  // 调拨默认带出当前值，只改变化的部分；领用填写新的使用部门和使用人；归还清空使用人
  const keep = type === 'TRANSFER'
  Object.assign(moveForm, { moveType: type, moveDate: today(), deptId: keep || type === 'ASSIGN' ? record.deptId : undefined,
    userId: keep ? record.custodianId : undefined, userName: keep ? record.custodianName : undefined,
    location: type === 'RETURN' ? '' : record.location || '', remark: '' })
  moveOpen.value = true
}
async function submitMove() {
  if (moveForm.moveType === 'ASSIGN' && !moveForm.deptId) return message.warning(t('common.rule.requiredSelect', { name: t('fin.asset.useDept') }))
  moving.value = true
  try {
    const { userName, ...data } = moveForm
    await moveAsset(moveTarget.value.id, { ...data, location: data.location || null, remark: data.remark || null })
    message.success(t('fin.asset.moveDone', { type: MOVE_TITLES[moveForm.moveType] }))
    moveOpen.value = false
    load()
  } finally {
    moving.value = false
  }
}
const historyColumns = [
  { title: t('common.field.date'), dataIndex: 'moveDate', width: 110 },
  { title: t('common.field.type'), key: 'type', width: 90 },
  { title: t('common.field.dept'), key: 'dept', width: 180 },
  { title: t('fin.asset.custodian'), key: 'user', width: 160 },
  { title: t('fin.asset.location'), key: 'location', width: 200 },
  { title: t('common.field.description'), dataIndex: 'remark', ellipsis: true },
  { title: t('fin.asset.handledBy'), key: 'by', width: 150 }
]
const historyOpen = ref(false)
const historyTarget = ref()
const history = ref([])
async function openHistory(record) {
  historyTarget.value = record
  history.value = await assetMoves(record.id)
  historyOpen.value = true
}
const arrow = (from, to) => (from === to ? from || '-' : `${from || '-'} → ${to || '-'}`)

async function remove(id) {
  await deleteAsset(id)
  message.success(t('common.msg.deleteSuccess'))
  load()
}

// ---------------------------------------------------------------- 折旧
const depPeriod = ref(currentPeriod())
const recordColumns = [
  { title: t('fin.asset.period'), dataIndex: 'period', width: 100 },
  { title: t('fin.asset.asset'), key: 'asset', width: 260 },
  { title: t('fin.asset.useDept'), dataIndex: 'deptName', width: 140 },
  { title: t('fin.asset.depAmount'), key: 'amount', width: 150, align: 'right' },
  { title: t('fin.asset.voucher'), key: 'voucher', width: 120 }
]
const records = useTable((p) => pageDepreciation({ ...p, period: depPeriod.value }), reactive({}))
watch(tab, (v) => v === 'depreciation' && records.load())
const depreciating = ref(false)
async function runDepreciation() {
  depreciating.value = true
  try {
    const count = await depreciate(depPeriod.value)
    message.success(t('fin.asset.depreciated', { count }))
    records.load()
    load()
  } finally {
    depreciating.value = false
  }
}
async function undo() {
  await undoDepreciation(depPeriod.value)
  message.success(t('fin.asset.undone'))
  records.load()
  load()
}
</script>

<style scoped>
.supplier-tip {
  margin-bottom: 16px;
}
.dispose-tip {
  margin-bottom: 16px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.filter {
  width: 220px;
}
.filter-s {
  width: 130px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
</style>
