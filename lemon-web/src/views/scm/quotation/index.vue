<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="关键字" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="报价单号或备注" allow-clear />
        </a-form-item>
        <a-form-item label="客户" name="customerId">
          <PartnerSelect v-model:value="query.customerId" type="customer" placeholder="全部" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.scm_quotation_status)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :exporter="exportAll"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="'scm:quotation:add'" type="primary" @click="form.openCreate()">
          <template #icon><PlusOutlined /></template>新增报价单
        </a-button>
        <span v-perm="'scm:quotation:remove'">
          <a-popconfirm
            :title="`确定删除选中的 ${selectedKeys.length} 张草稿？`"
            :disabled="!selectedKeys.length"
            @confirm="remove(selectedKeys)"
          >
            <a-button danger :disabled="!selectedKeys.length">
              <template #icon><DeleteOutlined /></template>批量删除
            </a-button>
          </a-popconfirm>
        </span>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="{ ...rowSelection, getCheckboxProps: (r) => ({ disabled: r.status !== 'DRAFT' }) }"
        :scroll="{ x: 1500, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'quoteNo'">
            <a @click="openDetail(record)">{{ record.quoteNo }}</a>
          </template>
          <template v-else-if="column.key === 'validTo'">
            {{ record.validTo || '不限' }}
            <a-tag v-if="isExpired(record)" color="red" :bordered="false" class="tag-gap">已过期</a-tag>
          </template>
          <template v-else-if="column.key === 'totalAmount'">¥{{ formatMoney(record.totalAmount) }}</template>
          <template v-else-if="column.key === 'orderNo'">
            <a v-if="record.orderId" @click="router.push({ path: '/scm/sales', query: { id: record.orderId } })">{{ record.orderNo }}</a>
            <span v-else-if="record.lostReason" class="sub-text">{{ record.lostReason }}</span>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="scm_quotation_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">
                <template #icon><EyeOutlined /></template>查看
              </a-button>
              <a-button v-if="record.status === 'DRAFT'" v-perm="'scm:quotation:edit'" type="link" size="small" @click="form.openEdit(record)">
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <span v-if="record.status === 'DRAFT'" v-perm="'scm:quotation:remove'">
                <a-popconfirm title="确定删除该报价单？" @confirm="remove([record.id])">
                  <a-button type="link" danger size="small">
                    <template #icon><DeleteOutlined /></template>删除
                  </a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <QuotationForm ref="form" @success="onSaved" />
    <QuotationDetail ref="detailRef" v-model:open="detailOpen" :quotation-id="detailId" @edit="onEditFromDetail" @changed="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, EyeOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import QuotationDetail from '@/components/scm/QuotationDetail.vue'
import QuotationForm from '@/components/scm/QuotationForm.vue'
import { deleteQuotation, pageQuotation } from '@/api/scm/sourcing'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { formatMoney } from '@/components/scm/scm'
import { today } from '@/components/wms/wms'

const dict = useDict('scm_quotation_status')

const columns = [
  { title: '报价单号', key: 'quoteNo', dataIndex: 'quoteNo', width: 180 },
  { title: '客户', dataIndex: 'customerName', width: 180, ellipsis: true },
  { title: '报价日期', dataIndex: 'quoteDate', width: 110 },
  { title: '有效期至', key: 'validTo', dataIndex: 'validTo', width: 160 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100, dict: 'scm_quotation_status' },
  { title: '价税合计', key: 'totalAmount', dataIndex: 'totalAmount', width: 130, align: 'right' },
  { title: '销售订单 / 失效原因', key: 'orderNo', dataIndex: 'orderNo', width: 180, ellipsis: true },
  { title: '制单人', dataIndex: 'createByName', width: 100 },
  { title: '备注', dataIndex: 'remark', width: 180, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'scm-quotation')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, customerId: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(
  pageQuotation,
  query
)
const route = useRoute()
const router = useRouter()
onMounted(() => {
  load()
  // 从 CRM 客户、商机「新建报价」跳转过来时带入客户和商机
  const q = route.query
  if (q.create) {
    const num = (v) => (v == null || v === '' ? undefined : Number(v))
    form.value.openCreate({
      customerId: num(q.customerId),
      customerName: q.customerName || undefined,
      opportunityId: num(q.opportunityId)
    })
    router.replace({ query: {} })
  }
})

const isExpired = (r) => !!r.validTo && r.validTo < today() && ['DRAFT', 'APPROVING', 'APPROVED'].includes(r.status)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const form = ref()
const detailRef = ref()
const detailOpen = ref(false)
const detailId = ref()

function openDetail(record) {
  detailId.value = record.id
  detailOpen.value = true
}

function onEditFromDetail(record) {
  form.value.openEdit(record)
}

/** 保存后打开详情，方便接着提交审批 */
function onSaved(id) {
  load()
  // 从详情进入的编辑：返回详情并刷新
  if (detailOpen.value && (!id || id === detailId.value)) detailRef.value?.reload()
  else if (id) openDetail({ id })
}

async function remove(ids) {
  await deleteQuotation(ids)
  message.success('删除成功')
  selectedKeys.value = selectedKeys.value.filter((k) => !ids.includes(k))
  load()
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.tag-gap {
  margin-left: 4px;
}
</style>
