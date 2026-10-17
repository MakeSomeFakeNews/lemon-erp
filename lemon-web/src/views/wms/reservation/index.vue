<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('wms.common.warehouse')" name="warehouseId">
          <WarehouseSelect v-model:value="query.warehouseId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('wms.common.material')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('wms.common.materialKeyword')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('wms.common.sourceNo')" name="sourceNo">
          <a-input v-model:value="query.sourceNo" :placeholder="$t('wms.reservation.orderNoPh')" allow-clear />
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
          :message="$t('wms.reservation.tip')"
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
              <a-popconfirm :title="$t('wms.reservation.confirmRelease')" @confirm="release(record)">
                <a-button type="link" danger size="small">
                  <template #icon><UnlockOutlined /></template>{{ $t('wms.reservation.release') }}
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
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()

const router = useRouter()

const columns = [
  { title: t('wms.reservation.salesOrder'), key: 'sourceNo', dataIndex: 'sourceNo', width: 180 },
  { title: t('wms.common.warehouse'), dataIndex: 'warehouseName', width: 140 },
  { title: t('wms.common.material'), key: 'material', width: 240, exportValue: (r) => `${r.materialCode} ${r.materialName}` },
  { title: t('common.field.spec'), dataIndex: 'spec', width: 140, ellipsis: true },
  { title: t('wms.reservation.qty'), key: 'qty', dataIndex: 'qty', width: 130, align: 'right' },
  { title: t('common.field.unit'), dataIndex: 'unit', width: 70 },
  { title: t('common.field.description'), dataIndex: 'remark', width: 180, ellipsis: true },
  { title: t('wms.reservation.by'), dataIndex: 'createByName', width: 110 },
  { title: t('wms.reservation.time'), dataIndex: 'createTime', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: 90, fixed: 'right' }
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
  message.success(t('wms.reservation.released'))
  load()
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
