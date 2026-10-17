<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('fin.voucher.period')" name="period">
          <PeriodPicker v-model:value="query.period" style="width: 100%" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.fin_voucher_status)" />
        </a-form-item>
        <a-form-item :label="$t('fin.voucher.source')" name="sourceType">
          <a-select v-model:value="query.sourceType" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.fin_voucher_source)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('fin.voucher.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('fin.voucher.account')" name="accountId">
          <AccountSelect v-model:value="query.accountId" all allow-clear :placeholder="$t('fin.voucher.accountPh')" />
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
          <template #icon><PlusOutlined /></template>{{ $t('fin.voucher.add') }}
        </a-button>
        <a-button v-perm="'fin:voucher:audit'" :disabled="!byStatus('DRAFT').length" @click="batch('audit', $t('common.action.audit'), byStatus('DRAFT'))">
          {{ $t('fin.voucher.withCount', { action: $t('common.action.audit'), n: byStatus('DRAFT').length }) }}
        </a-button>
        <a-button v-perm="'fin:voucher:post'" :disabled="!byStatus('AUDITED').length" @click="batch('post', $t('fin.voucher.post'), byStatus('AUDITED'))">
          {{ $t('fin.voucher.withCount', { action: $t('fin.voucher.post'), n: byStatus('AUDITED').length }) }}
        </a-button>
        <a-dropdown>
          <a-button>{{ $t('common.action.more') }} <DownOutlined /></a-button>
          <template #overlay>
            <a-menu>
              <a-menu-item v-if="userStore.hasPerm('fin:voucher:audit')" :disabled="!byStatus('AUDITED').length" @click="batch('unaudit', $t('common.action.unaudit'), byStatus('AUDITED'))">
                {{ $t('fin.voucher.withCount', { action: $t('common.action.unaudit'), n: byStatus('AUDITED').length }) }}
              </a-menu-item>
              <a-menu-item v-if="userStore.hasPerm('fin:voucher:post')" :disabled="!byStatus('POSTED').length" @click="batch('unpost', $t('fin.voucher.unpost'), byStatus('POSTED'))">
                {{ $t('fin.voucher.withCount', { action: $t('fin.voucher.unpost'), n: byStatus('POSTED').length }) }}
              </a-menu-item>
              <a-menu-item v-if="userStore.hasPerm('fin:voucher:remove')" danger :disabled="!deletable.length" @click="removeSelected">
                {{ $t('fin.voucher.withCount', { action: $t('common.action.delete'), n: deletable.length }) }}
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
            <a-tag v-if="record.reversalOf" color="red" :bordered="false">{{ $t('fin.voucher.red') }}</a-tag>
            <a-tag v-if="record.reversed === 1" color="orange" :bordered="false">{{ $t('fin.voucher.reversed') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'source'">
            <DictTag type="fin_voucher_source" :value="record.sourceType" />
            <div v-if="record.sourceNo" class="sub-text">{{ record.sourceNo }}</div>
          </template>
          <template v-else-if="column.key === 'people'">
            <div>{{ record.createByName || '-' }}</div>
            <div class="sub-text">{{ record.postByName ? $t('fin.voucher.postedBy', { name: record.postByName }) : record.auditByName ? $t('fin.voucher.auditedBy', { name: record.auditByName }) : $t('fin.voucher.unaudited') }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">{{ $t('common.action.view') }}</a-button>
              <a-button
                v-if="record.status === 'DRAFT' && record.sourceType === 'MANUAL'"
                v-perm="'fin:voucher:edit'"
                type="link"
                size="small"
                @click="form.openEdit(record)"
              >
                {{ $t('common.action.edit') }}
              </a-button>
              <a-button v-if="canReverse(record)" v-perm="'fin:voucher:reverse'" type="link" size="small" danger @click="openReverse(record)">
                {{ $t('fin.voucher.reverse') }}
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
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()
const userStore = useUserStore()
const dict = useDict('fin_voucher_status', 'fin_voucher_source')

const columns = [
  { title: t('fin.voucher.voucherNo'), key: 'voucherNo', width: 140 },
  { title: t('common.field.date'), dataIndex: 'voucherDate', width: 110 },
  { title: t('fin.voucher.summary'), dataIndex: 'summary', width: 260, ellipsis: true },
  { title: t('common.field.amount'), key: 'amount', width: 140, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 150, dict: 'fin_voucher_status' },
  { title: t('fin.voucher.source'), key: 'source', width: 160, dict: 'fin_voucher_source' },
  { title: t('fin.voucher.people'), key: 'people', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: 180, fixed: 'right' }
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
    title: t('fin.voucher.batchConfirm', { action: label, n: ids.length }),
    async onOk() {
      await voucherAction(action, ids)
      message.success(t('fin.voucher.actionSuccess', { action: label }))
      selectedKeys.value = []
      load()
    }
  })
}

const deletable = computed(() => list.value.filter((r) => selectedKeys.value.includes(r.id) && canDeleteVoucher(r)).map((r) => r.id))

function removeSelected() {
  const ids = deletable.value
  Modal.confirm({
    title: t('fin.voucher.deleteConfirm', { n: ids.length }),
    okType: 'danger',
    async onOk() {
      await deleteVoucher(ids)
      message.success(t('common.msg.deleteSuccess'))
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
