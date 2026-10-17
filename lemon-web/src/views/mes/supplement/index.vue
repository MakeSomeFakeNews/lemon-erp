<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('mes.supplement.no')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('mes.supplement.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.mes_supplement_status)" />
        </a-form-item>
        <a-form-item :label="$t('mes.supplement.reason')" name="reason">
          <a-select v-model:value="query.reason" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.mes_supplement_reason)" />
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
          <template #icon><PlusOutlined /></template>{{ $t('mes.supplementForm.title') }}
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
            <a-tooltip v-if="record.flowInstanceId" :title="$t('mes.supplement.viaFlow')">
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
                <template #icon><EyeOutlined /></template>{{ $t('common.action.view') }}
              </a-button>
              <a-button v-if="record.status === SupplementStatus.APPROVED" v-perm="'mes:supplement:issue'" type="link" size="small" @click="openIssue(record.id)">
                {{ $t('mes.supplement.issue') }}
              </a-button>
              <span v-if="canCancel(record)" v-perm="'mes:supplement:add'">
                <a-popconfirm :title="$t('mes.supplement.cancelConfirm', { no: record.suppNo })" @confirm="cancel(record.id)">
                  <a-button type="link" danger size="small">{{ $t('mes.supplement.cancel') }}</a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <SupplementForm v-model:open="formOpen" @success="load" />
    <WoDocForm v-model:open="issueOpen" kind="supplement" :supplement="issueTarget" @success="onIssued" />

    <SubPage :open="detailOpen" :title="detail ? $t('mes.supplement.detailTitle', { no: detail.suppNo }) : $t('mes.supplementForm.title')" width="min(800px, 100vw)" destroy-on-close @close="detailOpen = false">
      <template #extra>
        <DictTag v-if="detail" type="mes_supplement_status" :value="detail.status" />
      </template>
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" class="block">
            <a-descriptions-item :label="$t('mes.supplementForm.wo')">{{ detail.woNo }}</a-descriptions-item>
            <a-descriptions-item :label="$t('mes.wo.product')">{{ detail.productCode }} {{ detail.productName }}</a-descriptions-item>
            <a-descriptions-item :label="$t('mes.supplement.reason')"><DictTag type="mes_supplement_reason" :value="detail.reason" /></a-descriptions-item>
            <a-descriptions-item :label="$t('mes.supplement.applied')">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
            <a-descriptions-item :label="$t('mes.supplement.approval')">
              {{ detail.flowInstanceId ? $t('mes.supplement.approvalFlow') : $t('mes.supplement.noFlow') }}
              <span v-if="detail.approveTime" class="sub-text">{{ detail.approveTime }}</span>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('mes.supplement.docNo')">{{ detail.docNo || '-' }}</a-descriptions-item>
            <a-descriptions-item v-if="detail.remark" :label="$t('common.field.description')" :span="2">{{ detail.remark }}</a-descriptions-item>
          </a-descriptions>
          <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
              <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
              <template v-else-if="column.key === 'issuedQty'">
                <span v-if="Number(record.issuedQty) > 0 || detail.status === SupplementStatus.DONE">{{ formatQty(record.issuedQty) }} {{ record.unit }}</span>
                <span v-else class="sub-text">-</span>
              </template>
            </template>
          </a-table>
        </template>
      </a-spin>
      <template v-if="detail && detail.status === SupplementStatus.APPROVED && userStore.hasPerm('mes:supplement:issue')" #footer>
        <div class="drawer-footer">
          <a-button type="primary" @click="openIssue(detail.id)">{{ $t('mes.supplement.issue') }}</a-button>
        </div>
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { onMounted, reactive, ref } from 'vue'
import { ApartmentOutlined, EyeOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
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
import { useAction } from '@/composables/useAction'

import { SupplementStatus } from '@/constants/enums'
const userStore = useUserStore()
const { t } = useI18n()
const dict = useDict('mes_supplement_status', 'mes_supplement_reason')

const columns = [
  { title: t('mes.supplement.suppNo'), key: 'suppNo', width: 170 },
  { title: t('mes.supplement.woProduct'), key: 'wo', width: 240 },
  { title: t('mes.supplement.reason'), key: 'reason', width: 110, dict: 'mes_supplement_reason' },
  { title: t('common.field.quantity'), key: 'totalQty', width: 90, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 120, dict: 'mes_supplement_status' },
  { title: t('mes.supplement.applicant'), key: 'creator', width: 170 },
  { title: t('mes.supplement.docNo'), dataIndex: 'docNo', width: 170 },
  { title: t('common.field.description'), dataIndex: 'remark', ellipsis: true, width: 180 },
  { title: t('common.action.operation'), key: 'action', width: 190, fixed: 'right' }
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

const canCancel = (r) => [SupplementStatus.APPROVING, SupplementStatus.APPROVED].includes(r.status) && r.createBy === userStore.user?.id

const cancel = useAction(cancelSupplement, { successKey: 'mes.supplement.cancelled', reload: () => load() })

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
  { title: t('mes.supplementForm.material'), key: 'material', width: 260 },
  { title: t('common.field.spec'), dataIndex: 'spec', width: 140, ellipsis: true },
  { title: t('mes.supplement.applyQty'), key: 'qty', width: 120, align: 'right' },
  { title: t('mes.supplement.issuedQty'), key: 'issuedQty', width: 120, align: 'right' }
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
