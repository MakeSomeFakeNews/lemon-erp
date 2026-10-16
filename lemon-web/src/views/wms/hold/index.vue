<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.wms_hold_status)" />
        </a-form-item>
        <a-form-item label="关键字" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="物料编码、名称或入库单号" allow-clear />
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
          message="需要来料检验的物料采购入库后冻结为待检；来料检验合格或让步接收后自动放行，不合格的保持冻结，采购退货时优先退冻结的库存。"
        />
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1400, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ record.spec }}</div>
          </template>
          <template v-else-if="column.key === 'slot'">
            {{ record.warehouseName }} · {{ locationText(record.locationCode) }}
            <div v-if="record.batchNo" class="sub-text">批次 {{ record.batchNo }}</div>
          </template>
          <template v-else-if="column.key === 'qty'">
            <b v-if="Number(record.qty)">{{ formatQty(record.qty) }}</b>
            <span v-else class="sub-text">0</span>
            <span class="sub-text"> / {{ formatQty(record.holdQty) }} {{ record.unit }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="wms_hold_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'release'">
            <template v-if="record.releaseTime">{{ record.releaseByName || '-' }} {{ record.releaseTime }}</template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button
              v-if="record.status === 'PENDING' || record.status === 'REJECTED'"
              v-perm="'wms:hold:release'"
              type="link"
              size="small"
              @click="openRelease(record)"
            >
              <template #icon><UnlockOutlined /></template>放行
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="releaseOpen" title="放行冻结库存" :confirm-loading="releasing" @ok="submitRelease">
      <p v-if="current">
        {{ current.materialCode }} {{ current.materialName }}：{{ formatQty(current.qty) }} {{ current.unit }}，
        <DictTag type="wms_hold_status" :value="current.status" />
      </p>
      <a-textarea v-model:value="releaseRemark" :rows="3" :maxlength="255" placeholder="放行原因，如：让步接收、复检合格" />
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { UnlockOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { pageHold, releaseHold } from '@/api/wms/hold'
import { formatQty, locationText } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

const dict = useDict('wms_hold_status')

const columns = [
  { title: '采购入库单', dataIndex: 'sourceNo', width: 170 },
  { title: '供应商', dataIndex: 'partnerName', width: 160, ellipsis: true },
  { title: '物料', key: 'material', width: 220 },
  { title: '仓库 / 库位', key: 'slot', width: 180 },
  { title: '冻结 / 入库', key: 'qty', width: 150, align: 'right' },
  { title: '状态', key: 'status', width: 110, dict: 'wms_hold_status' },
  { title: '检验单', dataIndex: 'inspNo', width: 150 },
  { title: '放行', key: 'release', width: 200 },
  { title: '说明', dataIndex: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 90, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns)
const showSearch = ref(true)

const emptyQuery = () => ({ status: undefined, keyword: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageHold, query)
onMounted(load)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const releaseOpen = ref(false)
const releasing = ref(false)
const releaseRemark = ref('')
const current = ref()
function openRelease(record) {
  current.value = record
  releaseRemark.value = record.status === 'REJECTED' ? '让步接收' : ''
  releaseOpen.value = true
}
async function submitRelease() {
  releasing.value = true
  try {
    await releaseHold(current.value.id, releaseRemark.value || null)
    message.success('已放行，库存可以出库')
    releaseOpen.value = false
    load()
  } finally {
    releasing.value = false
  }
}
</script>
