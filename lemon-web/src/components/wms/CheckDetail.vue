<template>
  <SubPage
    :open="open"
    :title="detail ? `盘点单 ${detail.docNo}` : '盘点单'"
    width="min(1100px, 100vw)"
    destroy-on-close
    @close="emit('update:open', false)"
  >
    <template #extra>
      <DictTag v-if="detail" type="wms_doc_status" :value="detail.status" />
    </template>

    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="doc-desc">
          <a-descriptions-item label="单据号">{{ detail.docNo }}</a-descriptions-item>
          <a-descriptions-item label="盘点仓库">{{ detail.warehouseName }}</a-descriptions-item>
          <a-descriptions-item label="盘点日期">{{ detail.bizDate }}</a-descriptions-item>
          <a-descriptions-item label="冻结仓库">
            <a-tag v-if="detail.freeze === true || detail.freeze === 1" color="orange" :bordered="false">盘点期间冻结出入库</a-tag>
            <span v-else>否</span>
          </a-descriptions-item>
          <a-descriptions-item label="制单">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.confirmTime" label="确认">
            {{ detail.confirmByName || '-' }} {{ detail.confirmTime }}
          </a-descriptions-item>
          <a-descriptions-item label="明细">{{ detail.items.length }} 行，有差异 {{ diffRows.length }} 行</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="备注">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <div class="filter-bar">
          <a-radio-group v-model:value="filter" size="small" button-style="solid">
            <a-radio-button value="all">全部</a-radio-button>
            <a-radio-button value="diff">有差异</a-radio-button>
            <a-radio-button v-if="detail.status === 'DRAFT'" value="pending">未录入</a-radio-button>
          </a-radio-group>
          <span v-if="detail.status === 'DRAFT'" class="sub-text">草稿中的账面数量为最近一次保存时的库存，确认时以当时的库存为准</span>
        </div>

        <a-table row-key="id" size="small" :columns="columns" :data-source="rows" :pagination="{ pageSize: 50 }" :scroll="{ x: 960 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">
              <div>{{ record.materialCode }} {{ record.materialName }}</div>
              <div class="sub-text">{{ record.spec }}</div>
            </template>
            <template v-else-if="column.key === 'location'">{{ locationText(record.locationCode) }}</template>
            <template v-else-if="column.key === 'batchNo'">{{ record.batchNo || '-' }}</template>
            <template v-else-if="column.key === 'bookQty'">{{ formatQty(record.bookQty) }}</template>
            <template v-else-if="column.key === 'actualQty'">
              <span v-if="record.actualQty == null" class="sub-text">未录入</span>
              <span v-else>{{ formatQty(record.actualQty) }} {{ record.unit }}</span>
            </template>
            <template v-else-if="column.key === 'diff'"><QtyDiff :value="diffOf(record)" /></template>
            <template v-else-if="column.key === 'serialNos'">
              <template v-if="record.serialNos">
                <a-tag v-for="sn in splitSerials(record.serialNos)" :key="sn" class="serial-tag">{{ sn }}</a-tag>
              </template>
              <span v-else>-</span>
            </template>
          </template>
        </a-table>
      </template>
    </a-spin>

    <template v-if="detail?.status === 'DRAFT' && (canEdit || canConfirm)" #footer>
      <div class="drawer-footer">
        <a-button v-if="canEdit" @click="emit('edit', detail)">
          <template #icon><EditOutlined /></template>编辑
        </a-button>
        <a-button v-if="canConfirm" type="primary" :loading="confirming" @click="confirm">
          <template #icon><CheckOutlined /></template>确认盘点
        </a-button>
      </div>
    </template>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, ref, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { CheckOutlined, EditOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import QtyDiff from './QtyDiff.vue'
import { confirmCheck, getCheck } from '@/api/wms/check'
import { useUserStore } from '@/store/user'
import { formatQty, locationText, splitSerials } from './wms'

const props = defineProps({
  open: Boolean,
  checkId: Number
})
const emit = defineEmits(['update:open', 'edit', 'changed'])

const userStore = useUserStore()
const canEdit = computed(() => userStore.hasPerm('wms:check:edit'))
const canConfirm = computed(() => userStore.hasPerm('wms:check:confirm'))

const columns = [
  { title: '物料', key: 'material', width: 240 },
  { title: '库位', key: 'location', width: 110 },
  { title: '批次', key: 'batchNo', width: 130 },
  { title: '账面数量', key: 'bookQty', width: 100, align: 'right' },
  { title: '实盘数量', key: 'actualQty', width: 120, align: 'right' },
  { title: '差异', key: 'diff', width: 90, align: 'right' },
  { title: '实盘序列号', key: 'serialNos', width: 240 },
  { title: '备注', dataIndex: 'remark', width: 140, ellipsis: true }
]

const detail = ref()
const loading = ref(false)
const filter = ref('all')

const diffOf = (r) => (r.actualQty == null ? null : Number(r.actualQty) - Number(r.bookQty))
const diffRows = computed(() => (detail.value?.items || []).filter((r) => (diffOf(r) ?? 0) !== 0))
const rows = computed(() => {
  const items = detail.value?.items || []
  if (filter.value === 'diff') return diffRows.value
  if (filter.value === 'pending') return items.filter((r) => r.actualQty == null)
  return items
})

async function load() {
  loading.value = true
  try {
    detail.value = await getCheck(props.checkId)
  } finally {
    loading.value = false
  }
}

watch(
  () => props.open && props.checkId,
  (v) => {
    detail.value = undefined
    filter.value = 'all'
    if (v) load()
  }
)

const confirming = ref(false)
function confirm() {
  Modal.confirm({
    title: '确认盘点？',
    content: '将以当前库存为账面数量计算差异，盘盈入库、盘亏出库，库存调整为实盘数量。确认后不能修改。',
    async onOk() {
      confirming.value = true
      try {
        const approving = await confirmCheck(props.checkId)
        message.success(approving ? '已提交盘点差异审批，审批通过后调整库存' : '盘点已确认，库存已调整')
        await load()
        emit('changed')
      } finally {
        confirming.value = false
      }
    }
  })
}

defineExpose({ reload: load })
</script>

<style scoped>
.doc-desc {
  margin-bottom: 16px;
}
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.serial-tag {
  margin: 2px 4px 2px 0;
}
</style>
