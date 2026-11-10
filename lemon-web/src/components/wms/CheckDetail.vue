<template>
  <SubPage
    :open="open"
    :title="detail ? `${$t('wms.checkDoc.title')} ${detail.docNo}` : $t('wms.checkDoc.title')"
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
          <a-descriptions-item :label="$t('wms.common.docNo')">{{ detail.docNo }}</a-descriptions-item>
          <a-descriptions-item :label="$t('wms.checkDoc.warehouse')">{{ detail.warehouseName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('wms.checkDoc.date')">{{ detail.bizDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('wms.checkDoc.freeze')">
            <a-tag v-if="detail.freeze === true || detail.freeze === 1" color="orange" :bordered="false">{{ $t('wms.checkDoc.frozenTag') }}</a-tag>
            <span v-else>{{ $t('common.status.no') }}</span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('wms.doc.prepared')">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.confirmTime" :label="$t('wms.doc.confirmed')">
            {{ detail.confirmByName || '-' }} {{ detail.confirmTime }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('wms.checkDoc.items')">{{ $t('wms.checkDoc.itemsSummary', { n: detail.items.length, diff: diffRows.length }) }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <div class="filter-bar">
          <a-radio-group v-model:value="filter" size="small" button-style="solid">
            <a-radio-button value="all">{{ $t('common.status.all') }}</a-radio-button>
            <a-radio-button value="diff">{{ $t('wms.checkDoc.hasDiff') }}</a-radio-button>
            <a-radio-button v-if="detail.status === WmsDocStatus.DRAFT" value="pending">{{ $t('wms.checkDoc.notCounted') }}</a-radio-button>
          </a-radio-group>
          <span v-if="detail.status === WmsDocStatus.DRAFT" class="sub-text">{{ $t('wms.checkDoc.draftBookTip') }}</span>
        </div>

        <a-table row-key="id" size="small" :columns="columns" :data-source="rows" :pagination="{ pageSize: 50 }" :scroll="{ x: scrollX }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">
              <div>{{ record.materialCode }} {{ record.materialName }}</div>
              <div class="sub-text">{{ record.spec }}</div>
            </template>
            <template v-else-if="column.key === 'location'">{{ locationText(record.locationCode) }}</template>
            <template v-else-if="column.key === 'batchNo'">{{ record.batchNo || '-' }}</template>
            <template v-else-if="column.key === 'bookQty'">{{ formatQty(record.bookQty) }}</template>
            <template v-else-if="column.key === 'actualQty'">
              <span v-if="record.actualQty == null" class="sub-text">{{ $t('wms.checkDoc.notCounted') }}</span>
              <span v-else>{{ formatQty(record.actualQty) }} {{ record.unit }}</span>
            </template>
            <template v-else-if="column.key === 'diff'"><QtyDiff :value="diffOf(record)" /></template>
            <template v-else-if="column.key === 'costPrice'">
              <template v-if="(diffOf(record) ?? 0) > 0">
                <span v-if="record.costPrice != null">{{ price(record.costPrice) }}</span>
                <!-- 已完成：没填成本价的按确认时的均价计价，显示实际单价 -->
                <span v-else-if="record.diffAmount != null">{{ price(record.diffAmount / diffOf(record)) }}</span>
                <span v-else-if="record.avgCost != null" class="sub-text">{{ $t('wms.checkDoc.avgCostTag', { v: price(record.avgCost) }) }}</span>
                <span v-else class="sub-text">{{ $t('wms.checkDoc.costPriceDefault') }}</span>
              </template>
              <span v-else class="sub-text">-</span>
            </template>
            <template v-else-if="column.key === 'diffAmount'">
              <span v-if="record.diffAmount != null" :class="{ negative: record.diffAmount < 0 }">{{ money(record.diffAmount) }}</span>
              <a-tooltip v-else-if="estimateOf(record) != null" :title="$t('wms.checkDoc.diffAmountEstimated')">
                <span class="sub-text">≈ {{ money(estimateOf(record)) }}</span>
              </a-tooltip>
              <span v-else class="sub-text">-</span>
            </template>
            <template v-else-if="column.key === 'serialNos'">
              <SerialTags :value="record.serialNos" />
            </template>
          </template>
        </a-table>
      </template>
    </a-spin>

    <template v-if="detail?.status === WmsDocStatus.DRAFT && (canEdit || canConfirm)" #footer>
      <div class="drawer-footer">
        <a-button v-if="canEdit" @click="emit('edit', detail)">
          <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
        </a-button>
        <a-button v-if="canConfirm" type="primary" :loading="confirming" @click="confirm">
          <template #icon><CheckOutlined /></template>{{ $t('wms.checkDoc.confirm') }}
        </a-button>
      </div>
    </template>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import SerialTags from '@/components/wms/SerialTags.vue'
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import { CheckOutlined, EditOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import QtyDiff from './QtyDiff.vue'
import { confirmCheck, getCheck } from '@/api/wms/check'
import { useUserStore } from '@/store/user'
import { money, price, round2 } from '@/utils/number'
import { formatQty, locationText } from './wms'

import { WmsDocStatus } from '@/constants/enums'
const props = defineProps({
  open: Boolean,
  checkId: Number
})
const emit = defineEmits(['update:open', 'edit', 'changed'])
const { t } = useI18n()

const userStore = useUserStore()
const canEdit = computed(() => userStore.hasPerm('wms:check:edit'))
const canConfirm = computed(() => userStore.hasPerm('wms:check:confirm'))

/** 盘盈成本价：后端按权限返回（costVisible：有成本权限，或确认前能编辑盘点单）；差异金额只给有「查看成本金额」权限的人 */
const columns = computed(() =>
  [
    { title: t('wms.common.material'), key: 'material', width: 240 },
    { title: t('wms.common.location'), key: 'location', width: 110 },
    { title: t('wms.common.batch'), key: 'batchNo', width: 130 },
    { title: t('wms.checkDoc.bookQty'), key: 'bookQty', width: 110, align: 'right' },
    { title: t('wms.checkDoc.actualQty'), key: 'actualQty', width: 120, align: 'right' },
    { title: t('wms.checkDoc.diff'), key: 'diff', width: 100, align: 'right' },
    detail.value?.costVisible ? { title: t('wms.checkDoc.costPrice'), key: 'costPrice', width: 120, align: 'right' } : null,
    userStore.hasPerm('wms:stock:cost') ? { title: t('wms.checkDoc.diffAmount'), key: 'diffAmount', width: 120, align: 'right' } : null,
    { title: t('wms.checkDoc.actualSerials'), key: 'serialNos', width: 240 },
    { title: t('common.field.remark'), dataIndex: 'remark', width: 140, ellipsis: true }
  ].filter(Boolean)
)
const scrollX = computed(() => columns.value.reduce((sum, c) => sum + c.width, 0))

const detail = ref()
const loading = ref(false)
const filter = ref('all')

const diffOf = (r) => (r.actualQty == null ? null : Number(r.actualQty) - Number(r.bookQty))
/** 未确认时估算差异金额：盘盈按填写的成本价（默认均价），盘亏按均价 */
function estimateOf(r) {
  const diff = diffOf(r)
  if (!diff || r.avgCost == null) return null
  const unit = diff > 0 && r.costPrice != null ? Number(r.costPrice) : Number(r.avgCost)
  return round2(diff * unit)
}
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
    title: t('wms.checkDoc.confirmTitle'),
    content: t('wms.checkDoc.confirmContent'),
    async onOk() {
      confirming.value = true
      try {
        const approving = await confirmCheck(props.checkId)
        message.success(approving ? t('wms.checkDoc.submittedApproval') : t('wms.checkDoc.confirmed'))
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
.negative {
  color: #cf1322;
}
</style>
