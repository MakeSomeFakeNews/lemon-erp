<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.wms_hold_status)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('wms.hold.keywordPh')" allow-clear />
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
          :message="$t('wms.hold.tip')"
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
            <div v-if="record.batchNo" class="sub-text">{{ $t('wms.hold.batchN', { no: record.batchNo }) }}</div>
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
              v-if="record.status === HoldStatus.PENDING || record.status === HoldStatus.REJECTED"
              v-perm="'wms:hold:release'"
              type="link"
              size="small"
              @click="openRelease(record)"
            >
              <template #icon><UnlockOutlined /></template>{{ $t('wms.hold.release') }}
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="releaseOpen" :title="$t('wms.hold.releaseTitle')" :confirm-loading="releasing" @ok="submitRelease">
      <p v-if="current">
        {{ current.materialCode }} {{ current.materialName }}：{{ formatQty(current.qty) }} {{ current.unit }}，
        <DictTag type="wms_hold_status" :value="current.status" />
      </p>
      <a-textarea v-model:value="releaseRemark" :rows="3" :maxlength="255" :placeholder="$t('wms.hold.reasonPh')" />
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
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

import { HoldStatus } from '@/constants/enums'
const { t } = useI18n()

const dict = useDict('wms_hold_status')

const columns = [
  { title: t('wms.hold.purchaseIn'), dataIndex: 'sourceNo', width: 170 },
  { title: t('wms.hold.supplier'), dataIndex: 'partnerName', width: 160, ellipsis: true },
  { title: t('wms.common.material'), key: 'material', width: 220 },
  { title: t('wms.hold.slot'), key: 'slot', width: 190 },
  { title: t('wms.hold.qty'), key: 'qty', width: 150, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 110, dict: 'wms_hold_status' },
  { title: t('wms.hold.inspNo'), dataIndex: 'inspNo', width: 150 },
  { title: t('wms.hold.release'), key: 'release', width: 200 },
  { title: t('common.field.description'), dataIndex: 'remark', ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 90, fixed: 'right' }
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
  releaseRemark.value = record.status === HoldStatus.REJECTED ? t('wms.hold.concession') : ''
  releaseOpen.value = true
}
async function submitRelease() {
  releasing.value = true
  try {
    await releaseHold(current.value.id, releaseRemark.value || null)
    message.success(t('wms.hold.released'))
    releaseOpen.value = false
    load()
  } finally {
    releasing.value = false
  }
}
</script>
