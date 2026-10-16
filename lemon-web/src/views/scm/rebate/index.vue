<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-input-search v-model:value="query.keyword" placeholder="协议号、客户" allow-clear class="filter-wide" @search="search" />
        <a-select v-model:value="query.status" placeholder="全部状态" allow-clear :options="toOptions(dict.scm_rebate_status)" class="filter" @change="search" />
        <template #extra>
          <a-button v-perm="'scm:rebate:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>新增返利协议
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
              <div class="sub-text">销售额 ¥{{ formatMoney(record.salesAmount) }} × {{ Number(record.rate) }}%</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="scm_rebate_status" :value="record.status" />
            <div v-if="record.billNo" class="sub-text">红字应收 {{ record.billNo }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">查看</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增返利协议" edit-title="编辑返利协议">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="客户" name="customerId">
          <PartnerSelect v-model:value="form.customerId" type="customer" :label="form.customerName" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="开始日期" name="beginDate">
              <a-date-picker v-model:value="form.beginDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="结束日期" name="endDate">
              <a-date-picker v-model:value="form.endDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="兑付方式" name="settleMode">
          <a-radio-group v-model:value="form.settleMode">
            <a-radio v-for="o in dict.scm_rebate_settle" :key="o.value" :value="o.value">{{ o.label }}</a-radio>
          </a-radio-group>
          <div class="sub-text">
            {{ form.settleMode === 'CREDIT_NOTE' ? '结算后生成红字应收单冲减客户应收（借 主营业务收入、销项税额 / 贷 应收账款）'
              : '结算后计入客户的返利余额，新建销售订单时抵扣' }}
          </div>
        </a-form-item>
        <div class="section-title">返利阶梯 <span class="sub-text">期间销售额（出库 - 退货，含税）达到的最高一档返利率 × 全部销售额</span></div>
        <a-table row-key="key" size="small" :columns="tierColumns" :data-source="form.tiers" :pagination="false">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'minAmount'">
              <a-input-number v-model:value="record.minAmount" :min="0" :precision="2" prefix="¥" class="full" />
            </template>
            <template v-else-if="column.key === 'rate'">
              <a-input-number v-model:value="record.rate" :min="0.01" :max="100" :precision="2" addon-after="%" class="full" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" danger size="small" :disabled="form.tiers.length === 1" @click="form.tiers.splice(index, 1)">删除</a-button>
            </template>
          </template>
        </a-table>
        <a-button type="dashed" block class="add-line" @click="form.tiers.push(newTier())">
          <template #icon><PlusOutlined /></template>添加一档
        </a-button>
        <a-form-item label="说明" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </FormDrawer>

    <SubPage :open="detailOpen" :title="detail ? `返利协议 ${detail.rebateNo}` : ''" width="min(900px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space>
          <template v-if="detail.status === 'ACTIVE'">
            <span v-perm="'scm:rebate:add'">
              <a-popconfirm title="删除该返利协议？" @confirm="remove"><a-button danger>删除</a-button></a-popconfirm>
            </span>
            <a-button v-perm="'scm:rebate:add'" @click="drawer.openEdit(detail)">编辑</a-button>
            <span v-perm="'scm:rebate:settle'">
              <a-popconfirm :title="settleTip" :disabled="!ended" @confirm="settle">
                <a-tooltip :title="ended ? '' : '期间结束后才能结算'">
                  <a-button type="primary" :disabled="!ended">结算</a-button>
                </a-tooltip>
              </a-popconfirm>
            </span>
          </template>
          <span v-else v-perm="'scm:rebate:settle'">
            <a-popconfirm title="撤销结算：删除红字应收单（须未核销、未生成凭证）或收回未使用的返利余额，确定？" @confirm="unsettle">
              <a-button>撤销结算</a-button>
            </a-popconfirm>
          </span>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item label="客户">{{ detail.customerName }}</a-descriptions-item>
          <a-descriptions-item label="期间">{{ detail.beginDate }} ~ {{ detail.endDate }}</a-descriptions-item>
          <a-descriptions-item label="状态"><DictTag type="scm_rebate_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item label="兑付方式"><DictTag type="scm_rebate_settle" :value="detail.settleMode" /></a-descriptions-item>
          <template v-if="detail.status === 'SETTLED'">
            <a-descriptions-item label="结算">{{ detail.settleDate }}</a-descriptions-item>
            <a-descriptions-item label="返利">¥{{ formatMoney(detail.rebateAmount) }}（¥{{ formatMoney(detail.salesAmount) }} × {{ Number(detail.rate) }}%）</a-descriptions-item>
            <a-descriptions-item v-if="detail.billNo" label="红字应收">{{ detail.billNo }}</a-descriptions-item>
          </template>
          <template v-else-if="calc">
            <a-descriptions-item :label="ended ? '应返利' : '预估返利'">
              ¥{{ formatMoney(calc.rebateAmount) }}（¥{{ formatMoney(calc.salesAmount) }} × {{ Number(calc.rate) }}%）
            </a-descriptions-item>
            <a-descriptions-item label="下一档">{{ nextTierText }}</a-descriptions-item>
          </template>
          <a-descriptions-item label="创建">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="说明" :span="remarkSpan">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <div class="section-title">返利阶梯</div>
        <a-table row-key="id" size="small" :columns="tierViewColumns" :data-source="detail.tiers" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'minAmount'">¥{{ formatMoney(record.minAmount) }}</template>
            <template v-else-if="column.key === 'rate'">{{ Number(record.rate) }}%</template>
            <template v-else-if="column.key === 'reached'">
              <a-tag v-if="reachedTier && reachedTier.id === record.id" color="green">当前</a-tag>
            </template>
          </template>
        </a-table>
      </template>
    </SubPage>
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
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { addRebate, calcRebate, deleteRebate, getRebate, pageRebate, settleRebate, unsettleRebate, updateRebate } from '@/api/scm/sourcing'
import { formatMoney } from '@/components/scm/scm'
import { today } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

/** 销售返利：客户 × 期间的阶梯返利协议，期间结束后结算，生成红字应收或计入返利余额在下次订单抵扣 */
const dict = useDict('scm_rebate_status', 'scm_rebate_settle')

const columns = [
  { title: '协议', key: 'no', width: 220 },
  { title: '期间', key: 'period', width: 200 },
  { title: '兑付方式', key: 'mode', width: 120 },
  { title: '返利', key: 'result', width: 200 },
  { title: '状态', key: 'status', width: 160 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'scm-rebate')
const query = reactive({ keyword: undefined, status: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageRebate, query)
onMounted(load)

let seq = 0
const newTier = () => ({ key: ++seq, minAmount: undefined, rate: undefined })
const tierColumns = [
  { title: '销售额达到', key: 'minAmount' },
  { title: '返利率', key: 'rate', width: 160 },
  { title: '', key: 'action', width: 70 }
]
const rules = {
  customerId: [{ required: true, message: '请选择客户' }],
  beginDate: [{ required: true, message: '请选择开始日期' }],
  endDate: [{ required: true, message: '请选择结束日期' }],
  settleMode: [{ required: true, message: '请选择兑付方式' }]
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
  { title: '销售额达到', key: 'minAmount' },
  { title: '返利率', key: 'rate', width: 120 },
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
  return next ? `再销售 ¥${formatMoney(Number(next.minAmount) - salesNow.value)} 达到 ${Number(next.rate)}%` : '已达到最高一档'
})
/** 说明占满最后一行：前面的项数为 客户、期间、状态、兑付方式、创建 + 结算信息（已结算 2~3 项，执行中 2 项） */
const remarkSpan = computed(() => {
  const d = detail.value
  const n = 5 + (d?.status === 'SETTLED' ? (d.billNo ? 3 : 2) : calc.value ? 2 : 0)
  return 3 - (n % 3)
})
const settleTip = computed(() => calc.value
  ? `按期间销售额 ¥${formatMoney(calc.value.salesAmount)} 结算返利 ¥${formatMoney(calc.value.rebateAmount)}，确定？` : '确定结算？')
async function openDetail(id) {
  const r = await getRebate(id)
  detail.value = r
  calc.value = r.status === 'ACTIVE' ? await calcRebate(id) : undefined
  detailOpen.value = true
}
async function settle() {
  const res = await settleRebate(detail.value.id)
  message.success(`已结算，返利 ¥${formatMoney(res.rebateAmount)}`)
  openDetail(detail.value.id)
  load()
}
async function unsettle() {
  await unsettleRebate(detail.value.id)
  message.success('已撤销结算')
  openDetail(detail.value.id)
  load()
}
async function remove() {
  await deleteRebate(detail.value.id)
  message.success('已删除')
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
