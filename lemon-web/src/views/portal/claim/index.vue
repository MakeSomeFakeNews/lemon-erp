<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load" />
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 800, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
          <template v-else-if="column.key === 'status'"><DictTag type="qms_claim_status" :value="record.status" /></template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import DictTag from '@/components/DictTag.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { pagePortalClaim } from '@/api/portal'
import { formatMoney } from '@/components/fin/fin'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'

/** 供应商门户 - 质量索赔：已确认的索赔冲减应付款，体现在对账单中 */
const columns = [
  { title: '索赔单号', dataIndex: 'claimNo', width: 180 },
  { title: '索赔日期', dataIndex: 'claimDate', width: 110 },
  { title: '索赔金额', key: 'amount', width: 130, align: 'right' },
  { title: '索赔原因', dataIndex: 'reason', ellipsis: true },
  { title: '状态', key: 'status', width: 100 }
]
const { checked, visibleColumns } = useColumns(columns, 'portal-claim')
const { list, loading, size, pagination, load, onTableChange } = useTable(pagePortalClaim, reactive({}))
onMounted(load)
</script>
