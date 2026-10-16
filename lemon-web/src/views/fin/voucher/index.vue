<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="期间" name="period">
          <PeriodPicker v-model:value="query.period" style="width: 100%" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.fin_voucher_status)" />
        </a-form-item>
        <a-form-item label="来源" name="sourceType">
          <a-select v-model:value="query.sourceType" placeholder="全部" allow-clear :options="toOptions(dict.fin_voucher_source)" />
        </a-form-item>
        <a-form-item label="关键字" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="凭证号、摘要或来源单号" allow-clear />
        </a-form-item>
        <a-form-item label="科目" name="accountId">
          <AccountSelect v-model:value="query.accountId" all allow-clear placeholder="包含该科目的凭证" />
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
        <a-button v-perm="'fin:voucher:add'" type="primary" @click="form.openCreate()">
          <template #icon><PlusOutlined /></template>新增凭证
        </a-button>
        <a-button v-perm="'fin:voucher:audit'" :disabled="!byStatus('DRAFT').length" @click="batch('audit', '审核', byStatus('DRAFT'))">
          审核（{{ byStatus('DRAFT').length }}）
        </a-button>
        <a-button v-perm="'fin:voucher:post'" :disabled="!byStatus('AUDITED').length" @click="batch('post', '记账', byStatus('AUDITED'))">
          记账（{{ byStatus('AUDITED').length }}）
        </a-button>
        <a-dropdown>
          <a-button>更多 <DownOutlined /></a-button>
          <template #overlay>
            <a-menu>
              <a-menu-item v-if="userStore.hasPerm('fin:voucher:audit')" :disabled="!byStatus('AUDITED').length" @click="batch('unaudit', '反审核', byStatus('AUDITED'))">
                反审核（{{ byStatus('AUDITED').length }}）
              </a-menu-item>
              <a-menu-item v-if="userStore.hasPerm('fin:voucher:post')" :disabled="!byStatus('POSTED').length" @click="batch('unpost', '反记账', byStatus('POSTED'))">
                反记账（{{ byStatus('POSTED').length }}）
              </a-menu-item>
              <a-menu-item v-if="userStore.hasPerm('fin:voucher:remove')" danger :disabled="!deletable.length" @click="removeSelected">
                删除（{{ deletable.length }}）
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        :scroll="{ x: 1250, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'voucherNo'">
            <a @click="openDetail(record)">{{ record.voucherNo }}</a>
            <div class="sub-text">{{ periodText(record.period) }}</div>
          </template>
          <template v-else-if="column.key === 'amount'">
            <span :class="{ neg: Number(record.totalDebit) < 0 }">¥{{ formatMoney(record.totalDebit) }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="fin_voucher_status" :value="record.status" />
            <a-tag v-if="record.reversalOf" color="red" :bordered="false">红字</a-tag>
            <a-tag v-if="record.reversed === 1" color="orange" :bordered="false">已冲销</a-tag>
          </template>
          <template v-else-if="column.key === 'source'">
            <DictTag type="fin_voucher_source" :value="record.sourceType" />
            <div v-if="record.sourceNo" class="sub-text">{{ record.sourceNo }}</div>
          </template>
          <template v-else-if="column.key === 'people'">
            <div>{{ record.createByName || '-' }}</div>
            <div class="sub-text">{{ record.postByName ? '记账 ' + record.postByName : record.auditByName ? '审核 ' + record.auditByName : '未审核' }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">查看</a-button>
              <a-button
                v-if="record.status === 'DRAFT' && record.sourceType === 'MANUAL'"
                v-perm="'fin:voucher:edit'"
                type="link"
                size="small"
                @click="form.openEdit(record)"
              >
                编辑
              </a-button>
              <a-button v-if="canReverse(record)" v-perm="'fin:voucher:reverse'" type="link" size="small" danger @click="openReverse(record)">
                冲销
              </a-button>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <VoucherForm ref="form" @success="onSaved" />
    <VoucherDetail ref="detailRef" v-model:open="detailOpen" :voucher-id="detailId" @edit="onEdit" @changed="load" @reversed="(id) => openDetail({ id })" />
    <ReverseModal v-model:open="reverseOpen" :voucher="reverseTarget" @success="onReversed" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { DownOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import VoucherDetail from '@/components/fin/VoucherDetail.vue'
import VoucherForm from '@/components/fin/VoucherForm.vue'
import ReverseModal from '@/components/fin/ReverseModal.vue'
import { deleteVoucher, pageVoucher, voucherAction } from '@/api/fin'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { canDeleteVoucher, canReverse, currentPeriod, formatMoney, periodText } from '@/components/fin/fin'

const userStore = useUserStore()
const dict = useDict('fin_voucher_status', 'fin_voucher_source')

const columns = [
  { title: '凭证号', key: 'voucherNo', width: 130 },
  { title: '日期', dataIndex: 'voucherDate', width: 110 },
  { title: '摘要', dataIndex: 'summary', width: 260, ellipsis: true },
  { title: '金额', key: 'amount', width: 140, align: 'right' },
  { title: '状态', key: 'status', width: 150, dict: 'fin_voucher_status' },
  { title: '来源', key: 'source', width: 160, dict: 'fin_voucher_source' },
  { title: '制单 / 记账', key: 'people', width: 150 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'fin-voucher')
const showSearch = ref(true)

const emptyQuery = () => ({ period: currentPeriod(), status: undefined, sourceType: undefined, keyword: undefined, accountId: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(pageVoucher, query)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

onMounted(load)

const byStatus = (status) => list.value.filter((r) => selectedKeys.value.includes(r.id) && r.status === status).map((r) => r.id)

function batch(action, label, ids) {
  Modal.confirm({
    title: `${label} ${ids.length} 张凭证？`,
    async onOk() {
      await voucherAction(action, ids)
      message.success(`${label}成功`)
      selectedKeys.value = []
      load()
    }
  })
}

const deletable = computed(() => list.value.filter((r) => selectedKeys.value.includes(r.id) && canDeleteVoucher(r)).map((r) => r.id))

function removeSelected() {
  const ids = deletable.value
  Modal.confirm({
    title: `删除 ${ids.length} 张未审核凭证？`,
    okType: 'danger',
    async onOk() {
      await deleteVoucher(ids)
      message.success('删除成功')
      selectedKeys.value = []
      load()
    }
  })
}

const form = ref()
const detailRef = ref()
const detailOpen = ref(false)
const detailId = ref()
function openDetail(record) {
  detailId.value = record.id
  detailOpen.value = true
}
function onEdit(record) {
  form.value.openEdit(record)
}
const reverseOpen = ref(false)
const reverseTarget = ref()
function openReverse(record) {
  reverseTarget.value = record
  reverseOpen.value = true
}
function onReversed(id) {
  load()
  if (id) openDetail({ id })
}
function onSaved(id) {
  load()
  // 从详情进入的编辑：返回详情并刷新
  if (detailOpen.value && (!id || id === detailId.value)) detailRef.value?.reload()
  else if (id) openDetail({ id })
}
</script>

<style scoped>
.neg {
  color: #cf1322;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
