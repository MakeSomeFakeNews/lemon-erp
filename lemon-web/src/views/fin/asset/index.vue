<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="cards" tab="资产卡片">
          <div class="toolbar">
            <a-space wrap>
              <a-input-search v-model:value="query.keyword" placeholder="编码、名称、使用人、地点" allow-clear class="filter" @search="search" />
              <a-select v-model:value="query.category" placeholder="类别" allow-clear :options="toOptions(dict.fin_asset_category)" class="filter-s" @change="search" />
              <a-select v-model:value="query.status" placeholder="状态" allow-clear :options="toOptions(dict.fin_asset_status)" class="filter-s" @change="search" />
            </a-space>
            <a-button v-perm="'fin:asset:add'" type="primary" @click="drawer.openCreate()">
              <template #icon><PlusOutlined /></template>新增资产
            </a-button>
          </div>
          <a-table
            row-key="id"
            size="middle"
            :columns="columns"
            :data-source="list"
            :loading="loading"
            :pagination="pagination"
            :scroll="{ x: 1600 }"
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
                <div class="sub-text">残值率 {{ Number(record.residualRate) }}%</div>
              </template>
              <template v-else-if="column.key === 'depreciation'">
                <a-progress :percent="Math.round((record.depreciatedMonths / record.usefulMonths) * 100)" size="small" />
                <div class="sub-text">{{ record.depreciatedMonths }} / {{ record.usefulMonths }} 月，月折旧 {{ formatMoney(record.monthlyDepreciation) }}</div>
              </template>
              <template v-else-if="column.key === 'net'">
                <div>¥{{ formatMoney(record.netValue) }}</div>
                <div class="sub-text">累计折旧 {{ formatMoney(record.accumulated) }}</div>
              </template>
              <template v-else-if="column.key === 'status'">
                <DictTag type="fin_asset_status" :value="record.status" />
                <div v-if="record.disposeDate" class="sub-text">{{ record.disposeDate }}</div>
              </template>
              <template v-else-if="column.key === 'action'">
                <span class="table-actions">
                  <template v-if="record.status !== 'DISPOSED'">
                    <a-button v-perm="'fin:asset:edit'" type="link" size="small" @click="drawer.openEdit(record)">编辑</a-button>
                    <a-dropdown v-perm="'fin:asset:move'">
                      <a-button type="link" size="small">变动 <DownOutlined /></a-button>
                      <template #overlay>
                        <a-menu @click="({ key }) => openMove(record, key)">
                          <a-menu-item v-if="record.status === 'IDLE'" key="ASSIGN">领用</a-menu-item>
                          <a-menu-item v-if="record.status === 'IN_USE'" key="RETURN">归还</a-menu-item>
                          <a-menu-item key="TRANSFER">调拨</a-menu-item>
                        </a-menu>
                      </template>
                    </a-dropdown>
                  </template>
                  <a-button type="link" size="small" @click="openHistory(record)">履历</a-button>
                  <template v-if="record.status !== 'DISPOSED'">
                    <a-button v-perm="'fin:asset:edit'" type="link" size="small" @click="dispose(record)">处置</a-button>
                    <span v-if="record.depreciatedMonths === 0" v-perm="'fin:asset:remove'">
                      <a-popconfirm :title="`确定删除资产「${record.name}」？`" @confirm="remove(record.id)">
                        <a-button type="link" danger size="small">删除</a-button>
                      </a-popconfirm>
                    </span>
                  </template>
                </span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="depreciation" tab="折旧计提">
          <div class="toolbar">
            <a-space wrap>
              <span>期间</span>
              <PeriodPicker v-model:value="depPeriod" @update:value="records.search" />
              <a-button v-perm="'fin:asset:depreciate'" type="primary" :loading="depreciating" @click="runDepreciation">计提本期折旧</a-button>
              <span v-perm="'fin:asset:depreciate'">
                <a-popconfirm title="撤销本期折旧：删除凭证（须未审核）并恢复累计折旧，确定？" @confirm="undo">
                  <a-button>撤销计提</a-button>
                </a-popconfirm>
              </span>
            </a-space>
            <span class="sub-text">平均年限法；开始使用的次月开始计提，最后一个月计提剩余部分</span>
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

    <FormDrawer page :drawer="drawer" create-title="新增资产" edit-title="编辑资产">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="资产编码" name="code">
              <a-input v-model:value="form.code" :maxlength="32" placeholder="如：FA-2026-001" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="资产名称" name="name">
              <a-input v-model:value="form.name" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="类别" name="category">
              <a-select v-model:value="form.category" :options="toOptions(dict.fin_asset_category)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="规格型号" name="model">
              <a-input v-model:value="form.model" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="使用部门" name="deptId">
              <DeptSelect v-model:value="form.deptId" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="使用人" name="custodianId">
              <IdentityUserSelect v-model:value="form.custodianId" :label="form.custodianName" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="存放地点" name="location">
              <a-input v-model:value="form.location" :maxlength="128" placeholder="如：总部 3 楼 301" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="开始使用日期" name="useDate" extra="次月开始计提折旧">
              <a-date-picker v-model:value="form.useDate" value-format="YYYY-MM-DD" :disabled="locked" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="原值" name="originalValue">
              <a-input-number v-model:value="form.originalValue" :min="0" :precision="2" prefix="¥" :disabled="locked" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="8">
            <a-form-item label="残值率" name="residualRate">
              <a-input-number v-model:value="form.residualRate" :min="0" :max="100" :precision="2" addon-after="%" :disabled="locked" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="8">
            <a-form-item label="使用年限" name="usefulMonths">
              <a-input-number v-model:value="form.usefulMonths" :min="1" :max="1200" addon-after="月" :disabled="locked" class="full" />
            </a-form-item>
          </a-col>
          <template v-if="!form.id">
            <a-col :xs="12" :sm="12">
              <a-form-item label="期初已计提月数" name="openingMonths" extra="系统上线前已使用的资产填写">
                <a-input-number v-model:value="form.openingMonths" :min="0" :max="form.usefulMonths || 1200" addon-after="月" class="full" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :sm="12">
              <a-form-item label="期初累计折旧" name="openingAccumulated">
                <a-input-number v-model:value="form.openingAccumulated" :min="0" :precision="2" prefix="¥" class="full" />
              </a-form-item>
            </a-col>
            <template v-if="!form.openingMonths">
              <a-col :xs="24" :sm="14">
                <a-form-item label="采购供应商" name="supplierId" extra="填写后生成应付单，不填则不生成">
                  <PartnerSelect v-model:value="form.supplierId" type="supplier" allow-clear />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="10">
                <a-form-item label="进项税额" name="taxAmount">
                  <a-input-number v-model:value="form.taxAmount" :min="0" :precision="2" prefix="¥" :disabled="!form.supplierId" class="full" />
                </a-form-item>
              </a-col>
              <a-col v-if="form.supplierId" :span="24">
                <a-alert
                  type="info"
                  show-icon
                  class="supplier-tip"
                  :message="`将生成应付单 ¥${formatMoney((Number(form.originalValue) || 0) + (Number(form.taxAmount) || 0))}（借 固定资产、应交税费 - 进项税额 / 贷 应付账款），在「应付管理」中生成凭证，在「付款」中付款。`"
                />
              </a-col>
            </template>
          </template>
          <a-col :span="24">
            <a-form-item label="折旧费用科目" name="expenseAccountId" extra="管理部门用「管理费用 - 折旧费」，车间设备用「制造费用」">
              <AccountSelect v-model:value="form.expenseAccountId" :prefix="['5101', '6601', '6602']" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="状态" name="status">
              <a-radio-group v-model:value="form.status">
                <a-radio value="IN_USE">在用</a-radio>
                <a-radio value="IDLE">闲置</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-alert v-if="monthly" type="info" :message="`月折旧额约 ¥${formatMoney(monthly)}`" show-icon />
      </a-form>
    </FormDrawer>
    <a-modal v-model:open="disposeOpen" :title="`处置资产「${disposeTarget?.name || ''}」`" :confirm-loading="disposing" @ok="submitDispose">
      <a-alert
        type="info"
        show-icon
        class="dispose-tip"
        message="当月减少的资产当月照提折旧，请先计提处置月份的折旧。删除处置凭证后资产恢复为处置前的状态。"
      />
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="处置日期" required>
              <a-date-picker v-model:value="disposeForm.date" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="收付款账户" :extra="disposeForm.income || disposeForm.expense ? '' : '有收入或费用时必填'">
              <AccountSelect v-model:value="disposeForm.settleAccountId" :prefix="['1001', '1002']" placeholder="库存现金 / 银行存款" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="处置收入">
              <a-input-number v-model:value="disposeForm.income" :min="0" :precision="2" addon-after="元" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="清理费用">
              <a-input-number v-model:value="disposeForm.expense" :min="0" :precision="2" addon-after="元" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <div v-if="disposeTarget" class="sub-text">
          净值 {{ formatMoney(disposeTarget.netValue) }}，处置{{ disposeNet >= 0 ? '净损失' : '净收益' }}
          {{ formatMoney(Math.abs(disposeNet)) }}，转入营业外{{ disposeNet >= 0 ? '支出' : '收入' }}
        </div>
      </a-form>
    </a-modal>
    <a-modal
      v-model:open="moveOpen"
      :title="`${MOVE_TITLES[moveForm.moveType]}资产「${moveTarget?.name || ''}」`"
      :confirm-loading="moving"
      @ok="submitMove"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="日期" required>
              <a-date-picker v-model:value="moveForm.moveDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <template v-if="moveForm.moveType !== 'RETURN'">
            <a-col :span="12">
              <a-form-item label="使用部门" :required="moveForm.moveType === 'ASSIGN'">
                <DeptSelect v-model:value="moveForm.deptId" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="使用人">
                <IdentityUserSelect v-model:value="moveForm.userId" :label="moveForm.userName" />
              </a-form-item>
            </a-col>
          </template>
          <a-col :span="12">
            <a-form-item :label="moveForm.moveType === 'RETURN' ? '归还后存放地点' : '存放地点'">
              <a-input v-model:value="moveForm.location" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="说明">
              <a-input v-model:value="moveForm.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
        <div v-if="moveForm.moveType === 'RETURN'" class="sub-text">归还后资产转为闲置，清空使用人</div>
        <div v-else-if="moveForm.moveType === 'ASSIGN'" class="sub-text">领用后资产转为在用</div>
      </a-form>
    </a-modal>
    <a-modal v-model:open="historyOpen" :title="`资产履历 - ${historyTarget?.code || ''} ${historyTarget?.name || ''}`" width="min(1000px, 100vw)" :footer="null">
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

const dict = useDict('fin_asset_category', 'fin_asset_status', 'fin_asset_move_type')
const tab = ref('cards')

const columns = [
  { title: '资产', key: 'name', width: 220 },
  { title: '类别', key: 'category', width: 110 },
  { title: '使用部门 / 人 / 地点', key: 'user', width: 180 },
  { title: '开始使用', dataIndex: 'useDate', width: 110 },
  { title: '原值', key: 'value', width: 140, align: 'right' },
  { title: '折旧进度', key: 'depreciation', width: 220 },
  { title: '净值', key: 'net', width: 150, align: 'right' },
  { title: '折旧科目', dataIndex: 'expenseAccountName', width: 110 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 250, fixed: 'right' }
]
const query = reactive({ keyword: undefined, category: undefined, status: undefined })
const { list, loading, pagination, load, search, onTableChange } = useTable(pageAsset, query)
onMounted(load)

const rules = {
  code: [{ required: true, message: '请填写资产编码' }],
  name: [{ required: true, whitespace: true, message: '请填写资产名称' }],
  category: [{ required: true, message: '请选择类别' }],
  useDate: [{ required: true, message: '请选择开始使用日期' }],
  originalValue: [{ required: true, message: '请填写原值' }],
  usefulMonths: [{ required: true, message: '请填写使用年限' }],
  expenseAccountId: [{ required: true, message: '请选择折旧费用科目' }]
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
    message.warning('有处置收入或清理费用时请选择收付款账户')
    return
  }
  disposing.value = true
  try {
    await disposeAsset(disposeTarget.value.id, { ...disposeForm })
    message.success('已处置，处置凭证已生成')
    disposeOpen.value = false
    load()
  } finally {
    disposing.value = false
  }
}

// ---------------------------------------------------------------- 领用 / 归还 / 调拨、履历
const MOVE_TITLES = { ASSIGN: '领用', RETURN: '归还', TRANSFER: '调拨' }
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
  if (moveForm.moveType === 'ASSIGN' && !moveForm.deptId) return message.warning('请选择使用部门')
  moving.value = true
  try {
    const { userName, ...data } = moveForm
    await moveAsset(moveTarget.value.id, { ...data, location: data.location || null, remark: data.remark || null })
    message.success(`已${MOVE_TITLES[moveForm.moveType]}`)
    moveOpen.value = false
    load()
  } finally {
    moving.value = false
  }
}
const historyColumns = [
  { title: '日期', dataIndex: 'moveDate', width: 110 },
  { title: '类型', key: 'type', width: 80 },
  { title: '部门', key: 'dept', width: 180 },
  { title: '使用人', key: 'user', width: 160 },
  { title: '存放地点', key: 'location', width: 200 },
  { title: '说明', dataIndex: 'remark', ellipsis: true },
  { title: '经办', key: 'by', width: 150 }
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
  message.success('删除成功')
  load()
}

// ---------------------------------------------------------------- 折旧
const depPeriod = ref(currentPeriod())
const recordColumns = [
  { title: '期间', dataIndex: 'period', width: 100 },
  { title: '资产', key: 'asset', width: 260 },
  { title: '使用部门', dataIndex: 'deptName', width: 120 },
  { title: '折旧额', key: 'amount', width: 140, align: 'right' },
  { title: '凭证', key: 'voucher', width: 120 }
]
const records = useTable((p) => pageDepreciation({ ...p, period: depPeriod.value }), reactive({}))
watch(tab, (t) => t === 'depreciation' && records.load())
const depreciating = ref(false)
async function runDepreciation() {
  depreciating.value = true
  try {
    const count = await depreciate(depPeriod.value)
    message.success(`已计提 ${count} 项资产的折旧，凭证已生成（未审核）`)
    records.load()
    load()
  } finally {
    depreciating.value = false
  }
}
async function undo() {
  await undoDepreciation(depPeriod.value)
  message.success('已撤销')
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
