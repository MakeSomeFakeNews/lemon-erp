<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" placeholder="全部状态" allow-clear :options="toOptions(dict.qms_claim_status)" class="filter" @change="search" />
        <PartnerSelect v-model:value="query.supplierId" type="supplier" allow-clear placeholder="供应商" class="filter-wide" @update:value="search" />
        <a-input-search v-model:value="query.keyword" placeholder="索赔单号、供应商" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <a-button v-perm="'qms:claim:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>新增索赔
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
            <div v-if="record.ncrNo" class="sub-text">不合格品 {{ record.ncrNo }}</div>
          </template>
          <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="qms_claim_status" :value="record.status" />
            <div v-if="record.billNo" class="sub-text">应付单 {{ record.billNo }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <template v-if="record.status === 'DRAFT'">
                <a-button v-perm="'qms:claim:add'" type="link" size="small" @click="drawer.openEdit(record)">编辑</a-button>
                <span v-perm="'qms:claim:confirm'">
                  <a-popconfirm title="确认索赔？将生成红字应付单冲减该供应商的应付款" @confirm="confirm(record.id)">
                    <a-button type="link" size="small">确认</a-button>
                  </a-popconfirm>
                </span>
                <span v-perm="'qms:claim:remove'">
                  <a-popconfirm title="删除该索赔单？" @confirm="remove(record.id)">
                    <a-button type="link" danger size="small">删除</a-button>
                  </a-popconfirm>
                </span>
              </template>
              <span v-else-if="record.status === 'CONFIRMED'" v-perm="'qms:claim:confirm'">
                <a-popconfirm title="取消索赔？红字应付单一并删除（已核销或已生成凭证的不能取消）" @confirm="cancel(record.id)">
                  <a-button type="link" danger size="small">取消</a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增供应商索赔" edit-title="编辑供应商索赔">
      <a-alert type="info" show-icon class="block" message="确认后生成红字应付单冲减应付款，财务可生成凭证：借 应付账款，贷 营业外收入。" />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="供应商" name="supplierId">
          <PartnerSelect v-model:value="form.supplierId" type="supplier" :label="form.supplierName" />
        </a-form-item>
        <a-form-item label="关联不合格品处理单" name="ncrId">
          <a-select
            v-model:value="form.ncrId"
            show-search
            allow-clear
            :filter-option="false"
            :options="ncrOptions"
            placeholder="选填，搜索处理单号、供应商"
            @search="searchNcr"
            @focus="searchNcr('')"
            @change="onNcrChange"
          />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="索赔日期" name="claimDate">
              <a-date-picker v-model:value="form.claimDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="索赔金额" name="amount">
              <a-input-number v-model:value="form.amount" :min="0.01" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="索赔原因" name="reason">
          <a-textarea v-model:value="form.reason" :rows="3" :maxlength="500" placeholder="如：来料尺寸超差报废，按合同索赔材料费与停线损失" />
        </a-form-item>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
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
const route = useRoute()

const columns = [
  { title: '索赔单号', key: 'claimNo', width: 190 },
  { title: '供应商', dataIndex: 'supplierName', width: 180, ellipsis: true },
  { title: '索赔日期', dataIndex: 'claimDate', width: 110 },
  { title: '索赔金额', key: 'amount', width: 130, align: 'right' },
  { title: '索赔原因', dataIndex: 'reason', width: 220, ellipsis: true },
  { title: '状态 / 冲减应付', key: 'status', width: 150 },
  { title: '操作', key: 'action', width: 170, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'qms-claim')
const query = reactive({ status: undefined, supplierId: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageClaim, query)

const rules = {
  supplierId: [{ required: true, message: '请选择供应商' }],
  claimDate: [{ required: true, message: '请选择索赔日期' }],
  amount: [{ required: true, message: '请填写索赔金额' }],
  reason: [{ required: true, whitespace: true, message: '请填写索赔原因' }]
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
  message.success('已确认，已生成红字应付单')
  load()
}
async function cancel(id) {
  await cancelClaim(id)
  message.success('已取消')
  load()
}
async function remove(id) {
  await deleteClaim(id)
  message.success('删除成功')
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
