<template>
  <!-- 详情：生产计划、被 BOM 使用、在制工单用料 -->
  <SubPage :open="open" :title="detail ? `${detail.code} ${detail.name}` : $t('wms.common.material')" width="min(960px, 100vw)" destroy-on-close @close="open = false">
    <template #extra>
      <a-button v-if="detail && (canEdit || canEditPlan)" type="primary" @click="emit('edit', detail.id)">{{ $t('common.action.edit') }}</a-button>
    </template>
    <a-spin :spinning="loading">
      <template v-if="detail">
        <MaterialImages :material-id="detail.id" :editable="canEdit" :size="88" class="block" @changed="emit('changed')" />
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
          <a-descriptions-item :label="$t('wms.material.specUnit')">{{ detail.spec || '-' }} / {{ detail.unit || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('wms.inout.category')"><DictTag type="wms_material_category" :value="detail.category" /></a-descriptions-item>
          <a-descriptions-item v-if="detail.barcode" :label="$t('wms.units.barcode')">{{ detail.barcode }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.isVirtual === 1" :label="$t('wms.material.goodsType')">
            <a-tag color="cyan" :bordered="false">{{ $t('wms.material.virtual') }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item v-else :label="$t('wms.material.stockSafety')">
            <span>
              <span :class="{ danger: belowSafety(detail) }">{{ formatQty(detail.onHandQty) }}</span> /
              {{ detail.safetyStock != null ? formatQty(detail.safetyStock) : '-' }} {{ detail.unit }}
            </span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('wms.material.supplyType')">
            <span>
              <DictTag type="mes_supply_type" :value="detail.effectiveSupplyType" />
              <span v-if="!detail.supplyType" class="sub-text">{{ $t('wms.material.auto') }}</span>
            </span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('wms.material.defaultBom')">{{ detail.bomVersion ? $t('wms.material.bomLead', { version: detail.bomVersion, days: detail.bomLeadDays }) : '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('wms.material.defaultSupplier')">
            {{ detail.supplierName ? $t('wms.material.supplierLead', { name: detail.supplierName, days: detail.supplierLeadDays }) : '-' }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('wms.material.leadDays')">{{ leadText(detail) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('wms.material.lotRule')">{{ planRule(detail).join($t('wms.material.listSep')) || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('wms.material.overIssue')">{{ formatQty(detail.overIssueRate) }}%</a-descriptions-item>
          <a-descriptions-item v-if="detail.fqcRequired === 1" :label="$t('wms.material.fqc')">{{ $t('wms.material.fqcYes') }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.planner" :label="$t('wms.material.planner')">{{ detail.planner }}</a-descriptions-item>
        </a-descriptions>
        <a-tabs size="small">
          <a-tab-pane v-if="canSeeKit" key="usage" :tab="$t('wms.material.usageTab', { n: usage.length })">
            <a-table row-key="woMaterialId" size="small" :columns="usageColumns" :data-source="usage" :pagination="false" :scroll="{ x: 820 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'wo'">
                  <div>{{ record.woNo }} <DictTag type="mes_wo_status" :value="record.woStatus" /></div>
                  <div class="sub-text">{{ record.productCode }} {{ record.productName }}</div>
                </template>
                <template v-else-if="column.key === 'shortageQty'">
                  <span :class="{ danger: Number(record.shortageQty) > 0 }">{{ formatQty(record.shortageQty) }}</span>
                </template>
                <template v-else-if="QTY_KEYS.includes(column.key)">{{ formatQty(record[column.key]) }}</template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="whereUsed" :tab="$t('wms.material.whereUsedTab', { n: detail.whereUsed.length })">
            <a-table row-key="bomId" size="small" :columns="whereUsedColumns" :data-source="detail.whereUsed" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'product'">{{ record.productCode }} {{ record.productName }}</template>
                <template v-else-if="column.key === 'version'">
                  {{ record.version }}
                  <a-tag v-if="record.isDefault === 1" color="blue" :bordered="false">{{ $t('wms.material.default') }}</a-tag>
                  <a-tag v-if="record.bomStatus !== 1" :bordered="false">{{ $t('common.status.disabled') }}</a-tag>
                </template>
                <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }}</template>
                <template v-else-if="column.key === 'lossRate'">{{ formatQty(record.lossRate) }}%</template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="substitutes" :tab="$t('wms.material.substituteTab', { n: detail.substitutes.length })">
            <a-table row-key="id" size="small" :columns="subViewColumns" :data-source="detail.substitutes" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'substitute'">{{ record.substituteCode }} {{ record.substituteName }}</template>
                <template v-else-if="column.key === 'ratio'">1 : {{ formatQty(record.ratio) }}</template>
                <template v-else-if="column.key === 'onHandQty'">{{ formatQty(record.onHandQty) }} {{ record.unit }}</template>
              </template>
            </a-table>
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-spin>
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import MaterialImages from '@/components/wms/MaterialImages.vue'
import { getMaterialPlan, kitMaterialLines } from '@/api/mes/base'
import { formatQty } from '@/components/wms/wms'
import { useUserStore } from '@/store/user'
import { leadText, planRule } from './material'

/**
 * 物料详情（子页）：计划属性、被 BOM 使用、在制工单用料、替代料。
 * open(id) 打开，reload() 在打开时重新加载；edit(id) 请求编辑，changed 图片有变化
 */
defineProps({
  canEdit: Boolean,
  canEditPlan: Boolean
})
const emit = defineEmits(['edit', 'changed'])
const { t } = useI18n()
const userStore = useUserStore()
const canSeeKit = computed(() => userStore.hasPerm('mes:kit:list'))

const QTY_KEYS = ['requiredQty', 'issuedQty', 'toIssueQty', 'allocatedQty']
const belowSafety = (r) => r.safetyStock != null && Number(r.onHandQty) < Number(r.safetyStock)

const usageColumns = [
  { title: t('wms.material.woProduct'), key: 'wo', width: 260 },
  { title: t('wms.material.planStart'), dataIndex: 'planStart', width: 110 },
  { title: t('wms.material.required'), key: 'requiredQty', width: 90, align: 'right' },
  { title: t('wms.material.issued'), key: 'issuedQty', width: 90, align: 'right' },
  { title: t('wms.material.toIssue'), key: 'toIssueQty', width: 90, align: 'right' },
  { title: t('wms.material.allocated'), key: 'allocatedQty', width: 100, align: 'right' },
  { title: t('wms.material.shortage'), key: 'shortageQty', width: 90, align: 'right' }
]
const whereUsedColumns = [
  { title: t('wms.material.product'), key: 'product', width: 260 },
  { title: t('wms.material.bomVersion'), key: 'version', width: 160 },
  { title: t('wms.material.usageQty'), key: 'qty', width: 100, align: 'right' },
  { title: t('wms.material.lossRate'), key: 'lossRate', width: 100, align: 'right' }
]
const subViewColumns = [
  { title: t('wms.material.priority'), dataIndex: 'priority', width: 80 },
  { title: t('wms.material.substitute'), key: 'substitute', width: 260 },
  { title: t('wms.material.mainToSub'), key: 'ratio', width: 140 },
  { title: t('wms.variant.stock'), key: 'onHandQty', width: 120, align: 'right' },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true }
]

const open = ref(false)
const loading = ref(false)
const detail = ref()
const usage = ref([])

async function load(id) {
  open.value = true
  loading.value = true
  try {
    const [d, lines] = await Promise.all([getMaterialPlan(id), canSeeKit.value ? kitMaterialLines(id) : []])
    detail.value = d
    usage.value = lines
  } finally {
    loading.value = false
  }
}

/** 编辑保存后：详情打开时重新加载 */
function reload() {
  if (open.value && detail.value) load(detail.value.id)
}

defineExpose({ open: load, reload })
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.block {
  margin-bottom: 16px;
}
.danger {
  color: var(--status-critical);
  font-weight: 500;
}
</style>
