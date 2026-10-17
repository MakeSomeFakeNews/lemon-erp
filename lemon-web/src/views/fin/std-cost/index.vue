<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <span class="sub-text">{{ $t('fin.stdCost.versionTip') }}</span>
        <template #extra>
          <a-button v-perm="'fin:stdCost:edit'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>{{ $t('fin.stdCost.newVersion') }}
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
        :scroll="{ x: 980, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="openDetail(record.id)">{{ record.name }}</a>
            <div v-if="record.remark" class="sub-text">{{ record.remark }}</div>
          </template>
          <template v-else-if="column.key === 'period'">{{ periodText(record.effectivePeriod) }}</template>
          <template v-else-if="column.key === 'count'">
            {{ record.itemCount }}
            <div class="sub-text">{{ $t('fin.stdCost.makeBuyCount', { make: record.makeCount, buy: record.itemCount - record.makeCount }) }}</div>
          </template>
          <template v-else-if="column.key === 'zero'">
            <a-tag v-if="record.zeroCount" color="warning">{{ $t('fin.stdCost.zeroCountTag', { n: record.zeroCount }) }}</a-tag>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="fin_std_cost_status" :value="record.status" />
            <a-tag v-if="record.status === 'DRAFT' && !record.rollupTime" color="orange">{{ $t('fin.stdCost.needRollup') }}</a-tag>
            <div v-if="record.releaseTime" class="sub-text">{{ record.releaseTime }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">{{ $t('common.action.view') }}</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('fin.stdCost.createTitle')" :edit-title="$t('fin.stdCost.editTitle')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('fin.stdCost.versionName')" name="name">
          <a-input v-model:value="form.name" :maxlength="64" :placeholder="$t('fin.stdCost.versionNamePh')" />
        </a-form-item>
        <a-form-item :label="$t('fin.stdCost.effectivePeriod')" name="effectivePeriod">
          <PeriodPicker v-model:value="form.effectivePeriod" class="full" />
        </a-form-item>
        <a-form-item v-if="!form.id" :label="$t('fin.stdCost.copyFrom')" name="copyFromId" :extra="$t('fin.stdCost.copyFromExtra')">
          <a-select v-model:value="form.copyFromId" allow-clear :placeholder="$t('fin.stdCost.noCopy')" :options="versionOptions" />
        </a-form-item>
        <a-form-item :label="$t('common.field.description')" name="remark">
          <a-textarea v-model:value="form.remark" :rows="3" :maxlength="255" />
        </a-form-item>
      </a-form>
      <a-alert v-if="!form.id" type="info" show-icon :message="$t('fin.stdCost.saveTip')" />
    </FormDrawer>

    <SubPage :open="detailOpen" :title="detail ? $t('fin.stdCost.detailTitle', { name: detail.name }) : ''" width="min(1100px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space v-if="detail.status === 'DRAFT'" v-perm="'fin:stdCost:edit'">
          <a-popconfirm :title="$t('fin.stdCost.confirmDeleteVersion')" @confirm="remove"><a-button danger>{{ $t('common.action.delete') }}</a-button></a-popconfirm>
          <a-button @click="drawer.openEdit(detail)">{{ $t('common.action.edit') }}</a-button>
          <a-button :loading="rolling" @click="rollup">{{ $t('fin.stdCost.rollup') }}</a-button>
          <a-popconfirm :title="$t('fin.stdCost.confirmRelease')" @confirm="release">
            <a-button type="primary">{{ $t('fin.stdCost.release') }}</a-button>
          </a-popconfirm>
        </a-space>
        <span v-else v-perm="'fin:stdCost:edit'">
          <a-popconfirm :title="$t('fin.stdCost.confirmUnrelease')" @confirm="unrelease">
            <a-button>{{ $t('fin.stdCost.unrelease') }}</a-button>
          </a-popconfirm>
        </span>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('fin.stdCost.effectivePeriod')">{{ periodText(detail.effectivePeriod) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')">
            <DictTag type="fin_std_cost_status" :value="detail.status" />
            <span v-if="detail.releaseTime" class="sub-text">{{ detail.releaseTime }}</span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('fin.stdCost.rollup')">
            <span v-if="detail.rollupTime">{{ detail.rollupTime }}</span>
            <a-tag v-else color="orange">{{ $t('fin.stdCost.priceChangedRollup') }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('fin.stdCost.material')">{{ $t('fin.stdCost.makeBuyDetail', { make: detail.makeCount, buy: detail.itemCount - detail.makeCount }) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.stdCost.zeroPriceParts')">
            <a v-if="detail.zeroCount" class="warn" @click="filterZero">{{ $t('fin.stdCost.nItems', { n: detail.zeroCount }) }}</a>
            <span v-else>{{ $t('common.status.none') }}</span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('fin.stdCost.created')">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.description')" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <div class="toolbar">
          <a-space wrap>
            <a-radio-group v-model:value="itemQuery.make" button-style="solid" @change="items.search">
              <a-radio-button value="">{{ $t('common.status.all') }}</a-radio-button>
              <a-radio-button :value="1">{{ $t('fin.stdCost.makePart') }}</a-radio-button>
              <a-radio-button :value="0">{{ $t('fin.stdCost.buyPart') }}</a-radio-button>
            </a-radio-group>
            <a-input-search v-model:value="itemQuery.keyword" :placeholder="$t('fin.stdCost.materialKeyword')" allow-clear class="filter-wide" @search="items.search" />
          </a-space>
          <a-button v-if="editable" type="primary" ghost @click="openPrice()">
            <template #icon><PlusOutlined /></template>{{ $t('fin.stdCost.addBuyPart') }}
          </a-button>
        </div>
        <a-table
          row-key="id"
          size="small"
          :columns="itemColumns"
          :data-source="items.list.value"
          :loading="items.loading.value"
          :pagination="items.pagination"
          :scroll="{ x: 1000 }"
          @change="items.onTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">
              {{ record.materialCode }} {{ record.materialName }}
              <div class="sub-text">{{ record.spec }} {{ record.unit }}</div>
            </template>
            <template v-else-if="column.key === 'make'">
              <a-tag v-if="record.make" color="blue">{{ $t('fin.stdCost.make') }}</a-tag>
              <a-tag v-else>{{ $t('fin.stdCost.buy') }}</a-tag>
              <div v-if="record.bomVersion" class="sub-text">BOM {{ record.bomVersion }}</div>
            </template>
            <template v-else-if="column.key === 'unitCost'">
              <b :class="{ warn: !record.make && Number(record.unitCost) === 0 }">{{ fmt(record.unitCost) }}</b>
            </template>
            <template v-else-if="column.key === 'stdHours'">{{ record.make ? fmt(record.stdHours) : '' }}</template>
            <template v-else-if="column.num">{{ record.make ? fmt(record[column.key]) : column.key === 'materialCost' ? fmt(record.materialCost) : '' }}</template>
            <template v-else-if="column.key === 'action'">
              <span class="table-actions">
                <a-button v-if="record.make" type="link" size="small" @click="openBreakdown(record)">{{ $t('fin.stdCost.breakdown') }}</a-button>
                <template v-else-if="editable">
                  <a-button type="link" size="small" @click="openPrice(record)">{{ $t('fin.stdCost.changePrice') }}</a-button>
                  <a-popconfirm :title="$t('fin.stdCost.confirmRemovePart')" @confirm="removePrice(record)">
                    <a-button type="link" size="small" danger>{{ $t('common.action.delete') }}</a-button>
                  </a-popconfirm>
                </template>
              </span>
            </template>
          </template>
        </a-table>
      </template>
    </SubPage>

    <a-modal v-model:open="priceOpen" :title="priceForm.label ? $t('fin.stdCost.priceTitle', { label: priceForm.label }) : $t('fin.stdCost.addBuyPart')" @ok="submitPrice">
      <a-form layout="vertical">
        <a-form-item v-if="!priceForm.label" :label="$t('fin.stdCost.material')" required>
          <MaterialSelect v-model:value="priceForm.materialId" />
        </a-form-item>
        <a-form-item :label="$t('fin.stdCost.stdPrice')" required :extra="$t('fin.stdCost.stdPriceExtra')">
          <a-input-number v-model:value="priceForm.price" :min="0" :precision="4" prefix="¥" class="full" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="breakdownOpen" :title="breakdownTitle" width="min(900px, 100vw)" :footer="null">
      <template v-if="breakdown">
        <div class="section-title">{{ $t('fin.stdCost.materialSection', { amount: fmt(breakdown.row.materialCost) }) }}</div>
        <a-table row-key="materialId" size="small" :columns="partColumns" :data-source="breakdown.items" :pagination="false" :scroll="{ x: 720 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">
              {{ record.materialCode }} {{ record.materialName }}
              <a-tag v-if="record.make" color="blue" class="ml">{{ $t('fin.stdCost.make') }}</a-tag>
            </template>
            <template v-else-if="column.key === 'qty'">
              {{ fmt(record.unitQty) }} {{ record.unit }}
              <div v-if="Number(record.lossRate)" class="sub-text">{{ fmt(record.qty) }} × (1 + {{ Number(record.lossRate) }}%)</div>
            </template>
            <template v-else-if="column.key === 'unitCost'">{{ fmt(record.unitCost) }}</template>
            <template v-else-if="column.key === 'amount'">{{ fmt(record.amount) }}</template>
          </template>
        </a-table>
        <div class="section-title">{{ $t('fin.stdCost.laborSection', { labor: fmt(breakdown.row.laborCost), overhead: fmt(breakdown.row.overheadCost) }) }}</div>
        <a-table row-key="seq" size="small" :columns="opColumns" :data-source="breakdown.ops" :pagination="false" :scroll="{ x: 720 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'op'">{{ record.seq }} {{ record.name }}<div class="sub-text">{{ record.workCenterName }}</div></template>
            <template v-else-if="column.fmt">{{ fmt(record[column.key]) }}</template>
          </template>
        </a-table>
        <div class="sub-text tip">{{ $t('fin.stdCost.breakdownTip') }}</div>
      </template>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import {
  addStdCost, deleteStdCost, getStdCost, pageStdCost, releaseStdCost, removeStdPrice, rollupStdCost, saveStdPrice, stdCostBreakdown,
  stdCostItems, unreleaseStdCost, updateStdCost
} from '@/api/fin'
import { currentPeriod, periodText } from '@/components/fin/fin'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { useI18n } from 'vue-i18n'

/** 标准成本：按版本维护，采购件手工维护标准单价，自制件按默认 BOM 和工艺路线逐级卷算；发布后用于成本差异分析 */
useDict('fin_std_cost_status')
const userStore = useUserStore()
const { t } = useI18n()

const fmt = (n) => (n == null ? '' : Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 4 }))

const columns = [
  { title: t('fin.stdCost.version'), key: 'name', width: 240 },
  { title: t('fin.stdCost.effectivePeriod'), key: 'period', width: 130 },
  { title: t('fin.stdCost.materialCount'), key: 'count', width: 160 },
  { title: t('fin.stdCost.missingPrice'), key: 'zero', width: 200 },
  { title: t('common.field.status'), key: 'status', width: 200 },
  { title: t('common.action.operation'), key: 'action', width: 90, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'fin-std-cost')
const { list, loading, size, pagination, load, onTableChange } = useTable(pageStdCost, reactive({}))
onMounted(load)

const versionOptions = computed(() => list.value.map((v) => ({ value: v.id, label: `${v.name}（${periodText(v.effectivePeriod)}）` })))
const rules = {
  name: [{ required: true, whitespace: true, message: t('fin.stdCost.versionNameRequired') }],
  effectivePeriod: [{ required: true, message: t('fin.stdCost.effectivePeriodRequired') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, name: '', effectivePeriod: currentPeriod(), copyFromId: undefined, remark: '' }),
  create: async (data) => {
    const id = await addStdCost(data)
    openDetail(id)
  },
  update: updateStdCost,
  toPayload: (f) => ({ name: f.name, effectivePeriod: f.effectivePeriod, copyFromId: f.id ? null : f.copyFromId ?? null, remark: f.remark || null }),
  onSuccess: () => {
    load()
    if (detailOpen.value) refreshDetail()
  }
})
const { formRef, form } = drawer

// ---------------------------------------------------------------- 详情
const itemColumns = [
  { title: t('fin.stdCost.material'), key: 'material', width: 260 },
  { title: t('common.field.type'), key: 'make', width: 110 },
  { title: t('fin.stdCost.materialCost'), key: 'materialCost', num: true, width: 120, align: 'right' },
  { title: t('fin.stdCost.labor'), key: 'laborCost', num: true, width: 110, align: 'right' },
  { title: t('fin.stdCost.overhead'), key: 'overheadCost', num: true, width: 120, align: 'right' },
  { title: t('fin.stdCost.unitStdCost'), key: 'unitCost', width: 150, align: 'right' },
  { title: t('fin.stdCost.stdHours'), key: 'stdHours', width: 110, align: 'right' },
  { title: t('common.action.operation'), key: 'action', width: 140, fixed: 'right' }
]
const detailOpen = ref(false)
const detail = ref()
const editable = computed(() => detail.value?.status === 'DRAFT' && userStore.hasPerm('fin:stdCost:edit'))
const itemQuery = reactive({ make: '', keyword: undefined })
const items = useTable((p) => stdCostItems(detail.value.id, p), itemQuery, { params: (q) => ({ ...q, make: q.make === '' ? undefined : q.make }) })
async function openDetail(id) {
  detail.value = await getStdCost(id)
  Object.assign(itemQuery, { make: '', keyword: undefined })
  detailOpen.value = true
  items.search()
}
async function refreshDetail() {
  detail.value = await getStdCost(detail.value.id)
  items.load()
}
function filterZero() {
  Object.assign(itemQuery, { make: 0, keyword: undefined })
  items.search()
}
const rolling = ref(false)
async function rollup() {
  rolling.value = true
  try {
    const zero = await rollupStdCost(detail.value.id)
    if (zero) message.warning(t('fin.stdCost.rollupDoneZero', { n: zero }))
    else message.success(t('fin.stdCost.rollupDone'))
    refreshDetail()
    load()
  } finally {
    rolling.value = false
  }
}
async function release() {
  await releaseStdCost(detail.value.id)
  message.success(t('fin.stdCost.released'))
  refreshDetail()
  load()
}
async function unrelease() {
  await unreleaseStdCost(detail.value.id)
  message.success(t('fin.stdCost.unreleased'))
  refreshDetail()
  load()
}
async function remove() {
  await deleteStdCost(detail.value.id)
  message.success(t('common.msg.deleted'))
  detailOpen.value = false
  load()
}

const priceOpen = ref(false)
const priceForm = reactive({ materialId: undefined, label: '', price: undefined })
function openPrice(record) {
  Object.assign(priceForm, record
    ? { materialId: record.materialId, label: `${record.materialCode} ${record.materialName}`, price: Number(record.materialCost) }
    : { materialId: undefined, label: '', price: undefined })
  priceOpen.value = true
}
async function submitPrice() {
  if (!priceForm.materialId) return message.warning(t('fin.stdCost.selectMaterial'))
  if (priceForm.price == null) return message.warning(t('fin.stdCost.inputStdPrice'))
  await saveStdPrice(detail.value.id, { materialId: priceForm.materialId, price: priceForm.price })
  message.success(t('fin.stdCost.savedRollup'))
  priceOpen.value = false
  refreshDetail()
  load()
}
async function removePrice(record) {
  await removeStdPrice(detail.value.id, record.materialId)
  message.success(t('common.msg.deleted'))
  refreshDetail()
  load()
}

const partColumns = [
  { title: t('fin.stdCost.component'), key: 'material' },
  { title: t('fin.stdCost.unitQty'), key: 'qty', width: 190, align: 'right' },
  { title: t('fin.stdCost.stdCost'), key: 'unitCost', width: 120, align: 'right' },
  { title: t('common.field.amount'), key: 'amount', width: 120, align: 'right' }
]
const opColumns = [
  { title: t('fin.stdCost.operation'), key: 'op' },
  { title: t('fin.stdCost.stdHours'), key: 'stdHours', fmt: true, width: 110, align: 'right' },
  { title: t('fin.stdCost.laborRate'), key: 'laborRate', fmt: true, width: 110, align: 'right' },
  { title: t('fin.stdCost.labor'), key: 'labor', fmt: true, width: 100, align: 'right' },
  { title: t('fin.stdCost.overheadRate'), key: 'overheadRate', fmt: true, width: 130, align: 'right' },
  { title: t('fin.stdCost.overhead'), key: 'overhead', fmt: true, width: 110, align: 'right' }
]
const breakdownOpen = ref(false)
const breakdown = ref()
const breakdownTitle = ref('')
async function openBreakdown(record) {
  breakdown.value = await stdCostBreakdown(detail.value.id, record.materialId)
  breakdownTitle.value = t('fin.stdCost.breakdownTitle', { material: `${record.materialCode} ${record.materialName}`, cost: fmt(record.unitCost) })
  breakdownOpen.value = true
}
</script>

<style scoped>
.filter-wide {
  width: 220px;
}
.full {
  width: 100%;
}
.block {
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
.section-title {
  margin: 12px 0 8px;
  font-weight: 600;
}
.warn {
  color: var(--ant-color-warning, #faad14);
}
.ml {
  margin-left: 6px;
}
.tip {
  margin-top: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
