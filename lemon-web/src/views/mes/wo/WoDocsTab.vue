<template>
  <a-table row-key="id" size="small" :columns="columns" :data-source="docs" :pagination="false" :scroll="{ x: scrollX }">
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'kind'"><DictTag type="mes_wo_doc_kind" :value="record.kind" /></template>
      <template v-else-if="column.key === 'totalQty'">{{ formatQty(record.totalQty) }}</template>
      <template v-else-if="column.key === 'cost'">
        <template v-if="record.kind === WoDocKind.FINISH && record.costBasis">
          <a-tooltip :title="$t('mes.wo.cost.breakdown', { m: money(record.materialCost), l: money(record.laborCost), o: money(record.overheadCost) })">
            <span>{{ money(finishTotal(record)) }}</span>
          </a-tooltip>
          <a-tag :bordered="false" class="basis">{{ $t('mes.wo.cost.basis.' + record.costBasis) }}</a-tag>
        </template>
        <!-- 升级到实时库存成本之前的完工入库：没有成本口径，材料成本是按产品流水回填的近似成本 -->
        <template v-else-if="record.kind === WoDocKind.FINISH && record.materialCost != null">
          <span>{{ money(finishTotal(record)) }}</span>
          <a-tooltip :title="$t('mes.wo.cost.estimatedTip')">
            <a-tag :bordered="false" color="orange" class="basis">{{ $t('mes.wo.cost.estimated') }}</a-tag>
          </a-tooltip>
        </template>
        <a-tooltip v-else-if="record.kind === WoDocKind.RETURN && record.materialCost != null" :title="$t('mes.wo.cost.returnAdjustStock', { v: money(record.stockCostAdjust) })">
          <span class="sub-text">{{ $t('mes.wo.cost.returnAdjust', { v: money(record.materialCost) }) }}</span>
        </a-tooltip>
        <span v-else class="sub-text">-</span>
      </template>
    </template>
  </a-table>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import { formatQty } from '@/components/wms/wms'
import { WoDocKind } from '@/constants/enums'
import { useUserStore } from '@/store/user'
import { money, nz, round2 } from '@/utils/number'

/** 工单详情 - 单据（领料、退料、补料、报废、完工入库）；有「查看成本金额」权限时显示转入产品的成本（料工费、退料冲减） */
defineProps({ docs: { type: Array, default: () => [] } })
const { t } = useI18n()
const userStore = useUserStore()

const finishTotal = (r) => round2(nz(r.materialCost) + nz(r.laborCost) + nz(r.overheadCost))

const columns = computed(() => [
  { title: t('mes.wo.docNo'), dataIndex: 'docNo', width: 170 },
  { title: t('common.field.type'), key: 'kind', width: 130, dict: 'mes_wo_doc_kind' },
  { title: t('mes.wo.warehouse'), dataIndex: 'warehouseName', width: 130 },
  { title: t('mes.wo.bizDate'), dataIndex: 'bizDate', width: 120 },
  { title: t('common.field.quantity'), key: 'totalQty', width: 90, align: 'right' },
  ...(userStore.hasPerm('wms:stock:cost') ? [{ title: t('mes.wo.cost.title'), key: 'cost', width: 190, align: 'right' }] : []),
  { title: t('mes.wo.wmsDoc'), dataIndex: 'wmsDocNo', width: 170 },
  { title: t('mes.wo.preparedByCol'), dataIndex: 'createByName', width: 110 }
])
const scrollX = computed(() => columns.value.reduce((sum, c) => sum + c.width, 0))
</script>

<style scoped>
.basis {
  margin-left: 6px;
  margin-right: 0;
}
.sub-text {
  color: var(--app-text-3);
}
</style>
