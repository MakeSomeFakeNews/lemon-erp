<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="单号" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="申请单号或工单号" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.mes_supplement_status)" />
        </a-form-item>
        <a-form-item label="原因" name="reason">
          <a-select v-model:value="query.reason" placeholder="全部" allow-clear :options="toOptions(dict.mes_supplement_reason)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll"
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="'mes:supplement:add'" type="primary" @click="formOpen = true">
          <template #icon><PlusOutlined /></template>补料申请
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1200, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'suppNo'">
            <a @click="openDetail(record.id)">{{ record.suppNo }}</a>
          </template>
          <template v-else-if="column.key === 'wo'">
            <div>{{ record.woNo }}</div>
            <div class="sub-text">{{ record.productCode }} {{ record.productName }}</div>
          </template>
          <template v-else-if="column.key === 'reason'">
            <DictTag type="mes_supplement_reason" :value="record.reason" />
          </template>
          <template v-else-if="column.key === 'totalQty'">{{ formatQty(record.totalQty) }}</template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="mes_supplement_status" :value="record.status" />
            <a-tooltip v-if="record.flowInstanceId" title="走审批流程">
              <ApartmentOutlined class="flow-icon" />
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'creator'">
            <div>{{ record.createByName }}</div>
            <div class="sub-text">{{ record.createTime }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record.id)">
                <template #icon><EyeOutlined /></template>查看
              </a-button>
              <a-button v-if="record.status === 'APPROVED'" v-perm="'mes:supplement:issue'" type="link" size="small" @click="openIssue(record.id)">
                发料
              </a-button>
              <span v-if="canCancel(record)" v-perm="'mes:supplement:add'">
                <a-popconfirm :title="`撤销补料申请 ${record.suppNo}？`" @confirm="cancel(record.id)">
                  <a-button type="link" danger size="small">撤销</a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <SupplementForm v-model:open="formOpen" @success="load" />
    <WoDocForm v-model:open="issueOpen" kind="supplement" :supplement="issueTarget" @success="onIssued" />

    <SubPage :open="detailOpen" :title="detail ? `补料申请 ${detail.suppNo}` : '补料申请'" width="min(800px, 100vw)" destroy-on-close @close="detailOpen = false">
      <template #extra>
        <DictTag v-if="detail" type="mes_supplement_status" :value="detail.status" />
      </template>
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" class="block">
            <a-descriptions-item label="工单">{{ detail.woNo }}</a-descriptions-item>
            <a-descriptions-item label="产品">{{ detail.productCode }} {{ detail.productName }}</a-descriptions-item>
            <a-descriptions-item label="原因"><DictTag type="mes_supplement_reason" :value="detail.reason" /></a-descriptions-item>
            <a-descriptions-item label="申请">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
            <a-descriptions-item label="审批">
              {{ detail.flowInstanceId ? '审批流程' : '未配置审批流程，提交即通过' }}
              <span v-if="detail.approveTime" class="sub-text">{{ detail.approveTime }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="补料单">{{ detail.docNo || '-' }}</a-descriptions-item>
            <a-descriptions-item v-if="detail.remark" label="说明" :span="2">{{ detail.remark }}</a-descriptions-item>
          </a-descriptions>
          <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
              <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
              <template v-else-if="column.key === 'issuedQty'">
                <span v-if="Number(record.issuedQty) > 0 || detail.status === 'DONE'">{{ formatQty(record.issuedQty) }} {{ record.unit }}</span>
                <span v-else class="sub-text">-</span>
              </template>
            </template>
          </a-table>
        </template>
      </a-spin>
      <template v-if="detail && detail.status === 'APPROVED' && userStore.hasPerm('mes:supplement:issue')" #footer>
        <div class="drawer-footer">
          <a-button type="primary" @click="openIssue(detail.id)">发料</a-button>
        </div>
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ApartmentOutlined, EyeOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import SupplementForm from '@/components/mes/SupplementForm.vue'
import WoDocForm from '@/components/mes/WoDocForm.vue'
import { cancelSupplement, getSupplement, pageSupplement } from '@/api/mes/wo'
import { formatQty } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'

const userStore = useUserStore()
const dict = useDict('mes_supplement_status', 'mes_supplement_reason')

const columns = [
  { title: '申请单号', key: 'suppNo', width: 170 },
  { title: '工单 / 产品', key: 'wo', width: 240 },
  { title: '原因', key: 'reason', width: 100, dict: 'mes_supplement_reason' },
  { title: '数量', key: 'totalQty', width: 90, align: 'right' },
  { title: '状态', key: 'status', width: 110, dict: 'mes_supplement_status' },
  { title: '申请人', key: 'creator', width: 170 },
  { title: '补料单', dataIndex: 'docNo', width: 170 },
  { title: '说明', dataIndex: 'remark', ellipsis: true, width: 180 },
  { title: '操作', key: 'action', width: 190, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'mes-supplement')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, status: undefined, reason: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageSupplement, query)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

onMounted(load)

const canCancel = (r) => ['APPROVING', 'APPROVED'].includes(r.status) && r.createBy === userStore.user?.id

async function cancel(id) {
  await cancelSupplement(id)
  message.success('已撤销')
  load()
}

// ---------------------------------------------------------------- 新增、发料
const formOpen = ref(false)
const issueOpen = ref(false)
const issueTarget = ref()

async function openIssue(id) {
  issueTarget.value = await getSupplement(id)
  issueOpen.value = true
}

function onIssued() {
  load()
  if (detailOpen.value && detail.value) openDetail(detail.value.id)
}

// ---------------------------------------------------------------- 详情
const itemColumns = [
  { title: '物料', key: 'material', width: 260 },
  { title: '规格', dataIndex: 'spec', width: 140, ellipsis: true },
  { title: '申请数量', key: 'qty', width: 120, align: 'right' },
  { title: '已发数量', key: 'issuedQty', width: 120, align: 'right' }
]
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref()

async function openDetail(id) {
  detailOpen.value = true
  detailLoading.value = true
  try {
    detail.value = await getSupplement(id)
  } finally {
    detailLoading.value = false
  }
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.block {
  margin-bottom: 16px;
}
.flow-icon {
  margin-left: 4px;
  color: var(--app-text-3);
}
</style>
