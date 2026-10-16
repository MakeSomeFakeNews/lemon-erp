<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="仓库" name="warehouseId">
          <WarehouseSelect v-model:value="query.warehouseId" placeholder="全部" />
        </a-form-item>
        <a-form-item label="物料" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="物料编码或名称" allow-clear />
        </a-form-item>
        <a-form-item label="来源单号" name="sourceNo">
          <a-input v-model:value="query.sourceNo" placeholder="销售订单号" allow-clear />
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
        <a-alert
          type="info"
          show-icon
          message="预留不锁定库位批次；其他出库、调拨不能占用已预留的库存；销售出库时核销本订单的预留，订单完成、关闭或反审核时自动释放。"
        />
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
          <template v-if="column.key === 'sourceNo'">
            <a v-if="record.sourceType === 'SALES'" @click="toOrder(record)">{{ record.sourceNo }}</a>
            <span v-else>{{ record.sourceNo }}</span>
          </template>
          <template v-else-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ record.spec }}</div>
          </template>
          <template v-else-if="column.key === 'qty'">
            <b>{{ formatQty(record.qty) }}</b> <span class="sub-text">{{ record.unit }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span v-perm="'wms:reservation:release'">
              <a-popconfirm title="确定释放该预留？释放后库存可被其他订单或出库占用" @confirm="release(record)">
                <a-button type="link" danger size="small">
                  <template #icon><UnlockOutlined /></template>释放
                </a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UnlockOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { pageReservation, releaseReservation } from '@/api/wms/reservation'
import { formatQty } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'

const router = useRouter()

const columns = [
  { title: '销售订单', key: 'sourceNo', dataIndex: 'sourceNo', width: 180 },
  { title: '仓库', dataIndex: 'warehouseName', width: 140 },
  { title: '物料', key: 'material', width: 240, exportValue: (r) => `${r.materialCode} ${r.materialName}` },
  { title: '规格', dataIndex: 'spec', width: 140, ellipsis: true },
  { title: '预留数量', key: 'qty', dataIndex: 'qty', width: 130, align: 'right' },
  { title: '单位', dataIndex: 'unit', width: 70 },
  { title: '说明', dataIndex: 'remark', width: 180, ellipsis: true },
  { title: '预留人', dataIndex: 'createByName', width: 100 },
  { title: '预留时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 90, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'wms-reservation')
const showSearch = ref(true)

const emptyQuery = () => ({ warehouseId: undefined, keyword: undefined, sourceNo: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageReservation, query)
onMounted(load)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const toOrder = (record) => router.push({ path: '/scm/sales', query: { id: record.sourceId } })

async function release(record) {
  await releaseReservation(record.id)
  message.success('已释放')
  load()
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
