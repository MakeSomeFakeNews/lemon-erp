<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="单据号" name="docNo">
          <a-input v-model:value="query.docNo" placeholder="请输入单据号" allow-clear />
        </a-form-item>
        <a-form-item v-if="conf.typeDict" :label="conf.typeLabel" name="bizType">
          <a-select v-model:value="query.bizType" placeholder="全部" allow-clear :options="toOptions(dict[conf.typeDict])" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.wms_doc_status)" />
        </a-form-item>
        <a-form-item label="仓库" name="warehouseId">
          <WarehouseSelect v-model:value="query.warehouseId" placeholder="全部" />
        </a-form-item>
        <a-form-item label="业务日期" name="range">
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
          <template #icon><PlusOutlined /></template>新增{{ conf.label }}
        </a-button>
        <span v-perm="`wms:${kind}:remove`">
          <a-popconfirm
            :title="`确定删除选中的 ${selectedKeys.length} 张草稿？`"
            :disabled="!selectedKeys.length"
            @confirm="remove(selectedKeys)"
          >
            <a-button danger :disabled="!selectedKeys.length">
              <template #icon><DeleteOutlined /></template>批量删除
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
        :row-selection="{ ...rowSelection, getCheckboxProps: (r) => ({ disabled: r.status !== 'DRAFT' }) }"
        :scroll="{ x: 1350, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'docNo'">
            <a @click="openDetail(record)">{{ record.docNo }}</a>
            <a-tag v-if="record.redOf" color="red" :bordered="false" class="doc-tag">红字</a-tag>
            <a-tag v-if="record.reversed === 1" :bordered="false" class="doc-tag">已冲销</a-tag>
            <div v-if="record.sourceNo" class="sub-text">来源 {{ record.sourceNo }}</div>
            <div v-if="record.redOf" class="sub-text">冲销 {{ record.redOfNo || record.redOf }}</div>
          </template>
          <template v-else-if="column.key === 'bizType'">
            <DictTag :type="conf.typeDict" :value="record.bizType" />
          </template>
          <template v-else-if="column.key === 'warehouse'">
            <span v-if="kind === 'transfer'">{{ record.warehouseName }} <ArrowRightOutlined class="arrow" /> {{ record.toWarehouseName }}</span>
            <span v-else>{{ record.warehouseName }}</span>
            <div v-if="kind === 'transfer' && isTrue(record.inTransit)" class="sub-text">在途调拨</div>
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
                <template #icon><EyeOutlined /></template>查看
              </a-button>
              <template v-if="record.status === 'DRAFT'">
                <a-button v-perm="`wms:${kind}:edit`" type="link" size="small" @click="form.openEdit(record)">
                  <template #icon><EditOutlined /></template>编辑
                </a-button>
                <a-dropdown v-if="moreActions.length">
                  <a-button type="link" size="small">更多<DownOutlined /></a-button>
                  <template #overlay>
                    <a-menu @click="({ key }) => onMoreAction(key, record)">
                      <a-menu-item v-for="a in moreActions" :key="a.key" :danger="a.danger">
                        <component :is="a.icon" /> {{ a.label }}
                      </a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </template>
              <span v-if="kind === 'transfer' && record.status === 'TRANSIT'" v-perm="'wms:transfer:receive'">
                <a-popconfirm
                  :title="`确认调入仓库「${record.toWarehouseName}」已收货？`"
                  description="收货后按明细增加调入仓库的库存"
                  @confirm="receive(record)"
                >
                  <a-button type="link" size="small">
                    <template #icon><InboxOutlined /></template>收货
                  </a-button>
                </a-popconfirm>
              </span>
              <span v-if="canReverse(record)" v-perm="`wms:${kind}:reverse`">
                <a-popconfirm
                  :title="`确定冲销${conf.label} ${record.docNo}？`"
                  description="生成红字单据，库存反向变动"
                  ok-type="danger"
                  @confirm="reverse(record)"
                >
                  <a-button type="link" danger size="small">
                    <template #icon><RollbackOutlined /></template>冲销
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

/**
 * 入库单 / 出库单 / 调拨单列表页，三个页面共用
 */
const props = defineProps({
  kind: { type: String, required: true }
})

const userStore = useUserStore()
const conf = DOC_KINDS[props.kind]
const dict = useDict('wms_doc_status', ...(conf.typeDict ? [conf.typeDict] : []))

const columns = [
  { title: '单据号', key: 'docNo', width: 170 },
  conf.typeDict ? { title: conf.typeLabel, key: 'bizType', width: 110 } : null,
  { title: props.kind === 'transfer' ? '调出 → 调入' : '仓库', key: 'warehouse', width: props.kind === 'transfer' ? 240 : 140 },
  { title: '业务日期', dataIndex: 'bizDate', width: 110 },
  { title: '合计数量', key: 'totalQty', width: 110, align: 'right' },
  { title: '状态', key: 'status', width: 90 },
  { title: '制单人', dataIndex: 'createByName', width: 100 },
  { title: '确认', key: 'confirm', width: 160 },
  { title: '备注', dataIndex: 'remark', width: 160, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 260, fixed: 'right' }
].filter(Boolean)
const { checked, visibleColumns } = useColumns(columns, `wms-${props.kind}`)
const showSearch = ref(true)

const emptyQuery = () => ({ docNo: undefined, bizType: undefined, status: undefined, warehouseId: undefined, range: undefined })
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
  if (userStore.hasPerm(`wms:${props.kind}:confirm`)) actions.push({ key: 'confirm', label: '确认', icon: CheckOutlined })
  if (userStore.hasPerm(`wms:${props.kind}:remove`)) actions.push({ key: 'remove', label: '删除', icon: DeleteOutlined, danger: true })
  return actions
})

function onMoreAction(key, record) {
  if (key === 'confirm') {
    Modal.confirm({
      title: `确认${conf.label} ${record.docNo}？`,
      content: '确认后按明细更新库存，单据不能再修改；发布了出入库审批流程时先提交审批。',
      async onOk() {
        const flow = await confirmDoc(props.kind, record.id)
        message.success(flow ? '已提交出入库审批，审批通过后更新库存' : '确认成功')
        load()
      }
    })
  }
  if (key === 'remove') {
    Modal.confirm({
      title: `确定删除${conf.label} ${record.docNo}？`,
      okType: 'danger',
      onOk: () => remove([record.id])
    })
  }
}

const isTrue = (v) => v === true || v === 1

async function receive(record) {
  await receiveTransfer(record.id)
  message.success('收货成功')
  load()
}

async function reverse(record) {
  await reverseDoc(props.kind, record.id)
  message.success('冲销成功，已生成红字单据')
  load()
}

async function remove(ids) {
  await deleteDoc(props.kind, ids)
  message.success('删除成功')
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
