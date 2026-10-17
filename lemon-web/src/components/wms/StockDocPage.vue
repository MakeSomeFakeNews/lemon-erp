<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('wms.common.docNo')" name="docNo">
          <a-input v-model:value="query.docNo" :placeholder="$t('common.placeholder.input')" allow-clear />
        </a-form-item>
        <a-form-item v-if="conf.typeDict" :label="conf.typeLabel" name="bizType">
          <a-select v-model:value="query.bizType" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict[conf.typeDict])" />
        </a-form-item>
        <a-form-item v-if="conf.categoryDict" :label="conf.categoryLabel" name="ioCategory">
          <a-select v-model:value="query.ioCategory" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict[conf.categoryDict])" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.wms_doc_status)" />
        </a-form-item>
        <a-form-item :label="$t('wms.common.warehouse')" name="warehouseId">
          <WarehouseSelect v-model:value="query.warehouseId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('wms.common.bizDate')" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" style="width: 100%" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="`wms:${kind}:add`" type="primary" @click="form.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('wms.doc.createTitle', { kind: conf.label }) }}
        </a-button>
        <span v-perm="`wms:${kind}:remove`">
          <a-popconfirm
            :title="$t('wms.doc.confirmDeleteDrafts', { n: selectedKeys.length })"
            :disabled="!selectedKeys.length"
            @confirm="remove(selectedKeys)"
          >
            <a-button danger :disabled="!selectedKeys.length">
              <template #icon><DeleteOutlined /></template>{{ $t('common.action.batchDelete') }}
            </a-button>
          </a-popconfirm>
        </span>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="{ ...rowSelection, getCheckboxProps: (r) => ({ disabled: r.status !== WmsDocStatus.DRAFT }) }"
        :scroll="{ x: 1460, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'docNo'">
            <a @click="openDetail(record)">{{ record.docNo }}</a>
            <a-tag v-if="record.redOf" color="red" :bordered="false" class="doc-tag">{{ $t('wms.common.red') }}</a-tag>
            <a-tag v-if="record.reversed === 1" :bordered="false" class="doc-tag">{{ $t('wms.common.reversed') }}</a-tag>
            <div v-if="record.sourceNo" class="sub-text">{{ $t('wms.doc.sourceN', { no: record.sourceNo }) }}</div>
            <div v-if="record.redOf" class="sub-text">{{ $t('wms.doc.reverseOfN', { no: record.redOfNo || record.redOf }) }}</div>
          </template>
          <template v-else-if="column.key === 'bizType'">
            <DictTag :type="conf.typeDict" :value="record.bizType" />
          </template>
          <template v-else-if="column.key === 'ioCategory'">
            <DictTag v-if="record.ioCategory" :type="conf.categoryDict" :value="record.ioCategory" />
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'warehouse'">
            <span v-if="kind === 'transfer'">{{ record.warehouseName }} <ArrowRightOutlined class="arrow" /> {{ record.toWarehouseName }}</span>
            <span v-else>{{ record.warehouseName }}</span>
            <div v-if="kind === 'transfer' && isTrue(record.inTransit)" class="sub-text">{{ $t('wms.doc.inTransitTransfer') }}</div>
          </template>
          <template v-else-if="column.key === 'totalQty'">
            <span :class="negativeClass(record.totalQty)">{{ formatQty(record.totalQty) }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="wms_doc_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'confirm'">
            <template v-if="record.confirmTime">
              <div>{{ record.confirmByName || '-' }}</div>
              <div class="sub-text">{{ record.confirmTime }}</div>
            </template>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">
                <template #icon><EyeOutlined /></template>{{ $t('common.action.view') }}
              </a-button>
              <template v-if="record.status === WmsDocStatus.DRAFT">
                <a-button v-perm="`wms:${kind}:edit`" type="link" size="small" @click="form.openEdit(record)">
                  <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
                </a-button>
                <a-dropdown v-if="moreActions.length">
                  <a-button type="link" size="small">{{ $t('common.action.more') }}<DownOutlined /></a-button>
                  <template #overlay>
                    <a-menu @click="({ key }) => onMoreAction(key, record)">
                      <a-menu-item v-for="a in moreActions" :key="a.key" :danger="a.danger">
                        <component :is="a.icon" /> {{ a.label }}
                      </a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </template>
              <span v-if="kind === 'transfer' && record.status === WmsDocStatus.TRANSIT" v-perm="'wms:transfer:receive'">
                <a-popconfirm
                  :title="$t('wms.doc.confirmReceive', { name: record.toWarehouseName })"
                  :description="$t('wms.doc.receiveTip')"
                  @confirm="receive(record)"
                >
                  <a-button type="link" size="small">
                    <template #icon><InboxOutlined /></template>{{ $t('wms.common.receive') }}
                  </a-button>
                </a-popconfirm>
              </span>
              <span v-if="canReverse(record)" v-perm="`wms:${kind}:reverse`">
                <a-popconfirm
                  :title="$t('wms.doc.confirmReverse', { kind: conf.label, no: record.docNo })"
                  :description="$t('wms.doc.reverseTip')"
                  ok-type="danger"
                  @confirm="reverse(record)"
                >
                  <a-button type="link" danger size="small">
                    <template #icon><RollbackOutlined /></template>{{ $t('wms.common.reverse') }}
                  </a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <StockDocForm ref="form" :kind="kind" @success="onSaved" />
    <StockDocDetail ref="detailRef" v-model:open="detailOpen" :kind="kind" :doc-id="detailId" @edit="onEditFromDetail" @changed="load" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import {
  ArrowRightOutlined,
  CheckOutlined,
  DeleteOutlined,
  DownOutlined,
  EditOutlined,
  EyeOutlined,
  InboxOutlined,
  PlusOutlined,
  RollbackOutlined
} from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import StockDocDetail from './StockDocDetail.vue'
import StockDocForm from './StockDocForm.vue'
import WarehouseSelect from './WarehouseSelect.vue'
import { confirmDoc, deleteDoc, pageDoc, receiveTransfer, reverseDoc } from '@/api/wms/doc'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { DOC_KINDS, canReverse, formatQty, negativeClass } from './wms'
import { useAction } from '@/composables/useAction'

import { WmsDocStatus } from '@/constants/enums'
/**
 * 入库单 / 出库单 / 调拨单列表页，三个页面共用
 */
const props = defineProps({
  kind: { type: String, required: true }
})

const { t } = useI18n()
const userStore = useUserStore()
const conf = DOC_KINDS[props.kind]
const dict = useDict('wms_doc_status', ...(conf.typeDict ? [conf.typeDict, conf.categoryDict] : []))

const columns = [
  { title: t('wms.common.docNo'), key: 'docNo', width: 170 },
  conf.typeDict ? { title: conf.typeLabel, key: 'bizType', width: 120 } : null,
  conf.categoryDict ? { title: conf.categoryLabel, key: 'ioCategory', width: 110 } : null,
  { title: props.kind === 'transfer' ? t('wms.doc.fromTo') : t('wms.common.warehouse'), key: 'warehouse', width: props.kind === 'transfer' ? 240 : 140 },
  { title: t('wms.common.bizDate'), dataIndex: 'bizDate', width: 120 },
  { title: t('wms.common.totalQty'), key: 'totalQty', width: 110, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('wms.common.createBy'), dataIndex: 'createByName', width: 110 },
  { title: t('wms.doc.confirmed'), key: 'confirm', width: 160 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: 280, fixed: 'right' }
].filter(Boolean)
const { checked, visibleColumns } = useColumns(columns, `wms-${props.kind}`)
const showSearch = ref(true)

const emptyQuery = () => ({ docNo: undefined, bizType: undefined, ioCategory: undefined, status: undefined, warehouseId: undefined, range: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange } = useTable(
  (params) => pageDoc(props.kind, params),
  query,
  { params: ({ range, ...rest }) => ({ ...rest, beginDate: range?.[0], endDate: range?.[1] }) }
)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

onMounted(load)

const form = ref()

// 详情
const detailRef = ref()
const detailOpen = ref(false)
const detailId = ref()
function openDetail(record) {
  detailId.value = record.id
  detailOpen.value = true
}
function onSaved() {
  load()
  if (detailOpen.value) detailRef.value?.reload()
}
function onEditFromDetail(record) {
  form.value.openEdit(record)
}

const moreActions = computed(() => {
  const actions = []
  if (userStore.hasPerm(`wms:${props.kind}:confirm`)) actions.push({ key: 'confirm', label: t('wms.doc.confirm'), icon: CheckOutlined })
  if (userStore.hasPerm(`wms:${props.kind}:remove`)) actions.push({ key: 'remove', label: t('common.action.delete'), icon: DeleteOutlined, danger: true })
  return actions
})

function onMoreAction(key, record) {
  if (key === 'confirm') {
    Modal.confirm({
      title: t('wms.doc.confirmDoc', { kind: conf.label, no: record.docNo }),
      content: t('wms.doc.confirmContent'),
      async onOk() {
        const flow = await confirmDoc(props.kind, record.id)
        message.success(flow ? t('wms.doc.submittedApproval') : t('wms.doc.confirmOk'))
        load()
      }
    })
  }
  if (key === 'remove') {
    Modal.confirm({
      title: t('wms.doc.confirmDeleteDoc', { kind: conf.label, no: record.docNo }),
      okType: 'danger',
      onOk: () => remove([record.id])
    })
  }
}

const isTrue = (v) => v === true || v === 1

const receive = useAction((record) => receiveTransfer(record.id), { successKey: 'wms.doc.receiveSuccess', reload: () => load() })

const reverse = useAction((record) => reverseDoc(props.kind, record.id), { successKey: 'wms.doc.reverseSuccess', reload: () => load() })

async function remove(ids) {
  await deleteDoc(props.kind, ids)
  message.success(t('common.msg.deleteSuccess'))
  selectedKeys.value = selectedKeys.value.filter((k) => !ids.includes(k))
  load()
}
</script>

<style scoped>
.doc-tag {
  margin-left: 6px;
}
.qty-negative {
  color: var(--status-critical);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.arrow {
  color: var(--app-text-3);
  font-size: 12px;
  margin: 0 2px;
}
</style>
