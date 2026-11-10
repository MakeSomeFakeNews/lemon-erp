<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('mes.wo.woNo')" name="woNo">
          <a-input v-model:value="query.woNo" :placeholder="$t('mes.wo.woNoPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('mes.wo.product')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('mes.wo.productPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.mes_wo_status)" />
        </a-form-item>
        <a-form-item :label="$t('mes.wo.planStart')" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" style="width: 100%" />
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
        <a-button v-perm="'mes:wo:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('mes.wo.add') }}
        </a-button>
        <a-button v-perm="'mes:wo:add'" @click="openRework()">
          <template #icon><RedoOutlined /></template>{{ $t('mes.wo.addRework') }}
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'woNo'">
            <a @click="openDetail(record.id)">{{ record.woNo }}</a>
            <a-tag v-if="record.source === WorkOrderSource.MRP" color="purple" :bordered="false" class="source-tag">MRP</a-tag>
            <a-tag v-else-if="record.source === WorkOrderSource.KIT" color="orange" :bordered="false" class="source-tag">{{ $t('mes.wo.source.kit') }}</a-tag>
            <a-tag v-else-if="record.source === WorkOrderSource.PLAN" color="blue" :bordered="false" class="source-tag">{{ $t('mes.wo.source.plan') }}</a-tag>
            <a-tag v-else-if="record.source === WorkOrderSource.SPLIT" color="cyan" :bordered="false" class="source-tag">{{ $t('mes.wo.source.split') }}</a-tag>
            <a-tag v-if="record.woType === WorkOrderType.REWORK" color="orange" :bordered="false" class="source-tag">{{ $t('mes.wo.source.rework') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'product'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">BOM {{ record.bomVersion }}</div>
          </template>
          <template v-else-if="column.key === 'progress'">
            <a-progress
              :percent="pct(record.finishedQty, record.qty)"
              size="small"
              :show-info="false"
              :status="record.status === WorkOrderStatus.COMPLETED || pct(record.finishedQty, record.qty) >= 100 ? 'success' : 'normal'"
            />
            <div class="sub-text">
              {{ formatQty(record.finishedQty) }} / {{ formatQty(record.qty) }} · {{ pct(record.finishedQty, record.qty) }}%
              <span v-if="Number(record.scrapQty) > 0" class="scrap">{{ $t('mes.wo.scrapShort', { n: formatQty(record.scrapQty) }) }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'plan'">
            <div>{{ record.planStart || '-' }}</div>
            <div class="sub-text">{{ $t('mes.wo.to', { v: record.planEnd || '-' }) }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="mes_wo_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增 / 编辑 -->
    <WoForm ref="formRef" @success="load" />

    <!-- 详情：用料、工序、单据、报工 -->
    <WoDetail ref="detailRef" @changed="load" @release="release" @rework="openRework" />

    <!-- 返工工单 -->
    <WoReworkModal ref="reworkRef" @success="onReworkCreated" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { PlusOutlined, RedoOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { pageWo, releaseWo } from '@/api/mes/wo'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { formatQty } from '@/components/wms/wms'
import WoDetail from './WoDetail.vue'
import WoForm from './WoForm.vue'
import WoReworkModal from './WoReworkModal.vue'
import { offerOpen, pct } from './wo'
import { WorkOrderSource, WorkOrderStatus, WorkOrderType } from '@/constants/enums'

const { t } = useI18n()
const dict = useDict('mes_wo_status', 'mes_wo_type')

const columns = [
  { title: t('mes.wo.woNo'), key: 'woNo', width: 190 },
  { title: t('mes.wo.product'), key: 'product', width: 240 },
  { title: t('mes.wo.progress'), key: 'progress', width: 180, exportColumns: [
    { title: t('mes.wo.finishedQty'), exportValue: (r) => Number(r.finishedQty) },
    { title: t('mes.wo.planQty'), exportValue: (r) => Number(r.qty) },
    { title: t('mes.wo.scrapQty'), exportValue: (r) => Number(r.scrapQty || 0) }
  ] },
  { title: t('mes.wo.planDate'), key: 'plan', width: 120 },
  { title: t('common.field.status'), key: 'status', width: 100, dict: 'mes_wo_status' },
  { title: t('mes.wo.finishWarehouseShort'), dataIndex: 'warehouseName', width: 130 },
  { title: t('mes.wo.preparedByCol'), dataIndex: 'createByName', width: 110 },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 },
  actionColumn(3, [t('common.action.view')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'mes-wo')
const showSearch = ref(true)

const emptyQuery = () => ({ woNo: undefined, keyword: undefined, status: undefined, range: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageWo, query, {
  params: ({ range, ...rest }) => ({ ...rest, beginDate: range?.[0], endDate: range?.[1] })
})

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const route = useRoute()
const router = useRouter()
onMounted(() => {
  load()
  // 从产能排程等页面跳转过来时（?id=工单ID）直接打开详情
  const id = Number(route.query.id)
  if (id) {
    openDetail(id)
    router.replace({ query: {} })
  }
})

const formRef = ref()

// 行操作：查看；草稿可编辑、下达
const rowActions = (record) => {
  const draft = record.status === WorkOrderStatus.DRAFT
  return [
    { type: 'view', onClick: () => openDetail(record.id) },
    { type: 'edit', show: draft, perm: 'mes:wo:edit', onClick: () => formRef.value.openEdit(record) },
    { type: 'submit', label: t('mes.wo.release'), show: draft, perm: 'mes:wo:release', onClick: () => release(record) }
  ]
}
const detailRef = ref()
const reworkRef = ref()
const openDetail = (id) => detailRef.value.open(id)
const openRework = (wo) => reworkRef.value.open(wo)

const release = useAction((record) => releaseWo(record.id), {
  confirm: (record) => ({ title: t('mes.wo.releaseConfirm', { no: record.woNo }), content: t('mes.wo.releaseContent') }),
  successKey: 'mes.wo.released',
  reload: () => {
    load()
    detailRef.value.refreshIfOpen()
  }
})

function onReworkCreated(id) {
  load()
  offerOpen(t('mes.wo.reworkCreated'), t('mes.wo.reworkCreatedContent'), () => openDetail(id))
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.source-tag {
  margin-left: 4px;
}
.scrap {
  margin-left: 4px;
  color: var(--status-critical, #cf1322);
}
</style>
