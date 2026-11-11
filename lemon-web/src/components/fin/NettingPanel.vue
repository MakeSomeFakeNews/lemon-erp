<template>
  <!-- 应收应付对冲（应收管理 / 应付管理的页签，issue #95）：对冲单列表，展开看核销明细；新建、删除 -->
  <div>
    <div class="filters">
      <PartnerSelect v-model:value="query.customerId" type="customer" allow-clear :placeholder="$t('fin.netting.allCustomers')" class="filter" />
      <a-input-search v-model:value="query.keyword" :placeholder="$t('fin.netting.keywordPh')" allow-clear class="filter" @search="search" />
      <span class="sub-text">{{ $t('fin.netting.tip') }}</span>
      <span class="grow" />
      <a-button v-perm="'fin:netting:add'" type="primary" @click="formRef.open()">
        <template #icon><PlusOutlined /></template>{{ $t('fin.netting.create') }}
      </a-button>
    </div>
    <a-table
      row-key="id"
      size="middle"
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 1100 }"
      :expanded-row-keys="expanded"
      @expand="onExpand"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'customer'">
          <div>{{ record.customerName }}</div>
          <div class="sub-text">{{ record.customerCode }}</div>
        </template>
        <template v-else-if="column.key === 'supplier'">
          <div>{{ record.supplierName }}</div>
          <div class="sub-text">{{ record.supplierCode }}</div>
        </template>
        <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
        <template v-else-if="column.key === 'voucher'">
          <a v-if="record.voucherId" @click="openVoucher(record.voucherId)">{{ record.voucherNo }}</a>
        </template>
        <template v-else-if="column.key === 'action'">
          <TableActions :actions="rowActions(record)" />
        </template>
      </template>
      <template #expandedRowRender="{ record }">
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="items[record.id] || []" :pagination="false">
          <template #bodyCell="{ column, record: item }">
            <template v-if="column.key === 'kind'">
              <a-tag :color="item.kind === BillKind.AR ? 'green' : 'orange'" :bordered="false">
                {{ item.kind === BillKind.AR ? $t('fin.netting.kindAr') : $t('fin.netting.kindAp') }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'source'">
              <DictTag type="fin_bill_source" :value="item.sourceType" /> <span class="sub-text">{{ item.sourceNo }}</span>
            </template>
            <template v-else-if="column.key === 'amount'">
              <span :class="{ neg: Number(item.amount) < 0 }">¥{{ formatMoney(item.amount) }}</span>
            </template>
          </template>
        </a-table>
      </template>
    </a-table>

    <NettingForm ref="formRef" @saved="reload" />
    <VoucherDetail v-model:open="voucherOpen" :voucher-id="voucherId" @changed="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import NettingForm from './NettingForm.vue'
import VoucherDetail from './VoucherDetail.vue'
import { deleteNetting, getNetting, pageNetting } from '@/api/fin'
import { useAction } from '@/composables/useAction'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { formatMoney } from './fin'
import { t } from '@/locales'
import { BillKind } from '@/constants/enums'

useDict('fin_bill_source')
const columns = [
  { title: t('fin.netting.no'), dataIndex: 'nettingNo', width: 160 },
  { title: t('fin.netting.date'), dataIndex: 'nettingDate', width: 110 },
  { title: t('fin.netting.customer'), key: 'customer', width: 200 },
  { title: t('fin.netting.supplier'), key: 'supplier', width: 200 },
  { title: t('fin.netting.amount'), key: 'amount', width: 130, align: 'right' },
  { title: t('fin.billPage.voucher'), key: 'voucher', width: 120 },
  { title: t('fin.netting.createBy'), dataIndex: 'createByName', width: 100 },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  actionColumn(1)
]
const itemColumns = [
  { title: t('fin.netting.kind'), key: 'kind', width: 90 },
  { title: t('fin.billPage.billNo'), dataIndex: 'billNo', width: 160 },
  { title: t('common.field.date'), dataIndex: 'billDate', width: 110 },
  { title: t('fin.billPage.source'), key: 'source', width: 220 },
  { title: t('fin.netting.nettingAmount'), key: 'amount', width: 130, align: 'right' }
]

const query = reactive({ customerId: undefined, keyword: undefined })
const { list, loading, pagination, load, search, onTableChange } = useTable(pageNetting, query)
watch(() => query.customerId, () => search())
onMounted(load)

const formRef = ref()
function reload() {
  expanded.value = []
  load()
}

const remove = useAction(deleteNetting, { confirm: t('fin.netting.confirmDelete'), successKey: 'common.msg.deleteSuccess', reload: () => reload() })
const rowActions = (record) => [{ type: 'delete', perm: 'fin:netting:remove', onClick: () => remove(record.id) }]

// 展开核销明细：同时只展开一张
const expanded = ref([])
const items = ref({})
async function onExpand(open, record) {
  expanded.value = open ? [record.id] : []
  if (open && !items.value[record.id]) {
    const d = await getNetting(record.id)
    items.value = { ...items.value, [record.id]: d.items }
  }
}

const voucherOpen = ref(false)
const voucherId = ref()
function openVoucher(id) {
  voucherId.value = id
  voucherOpen.value = true
}
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.filter {
  width: 240px;
}
.grow {
  flex: 1;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.neg {
  color: #cf1322;
}
</style>
