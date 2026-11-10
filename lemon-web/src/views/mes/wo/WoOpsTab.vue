<template>
  <a-table row-key="id" size="small" :columns="columns" :data-source="detail.ops" :pagination="false">
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'name'">
        {{ record.name }}
        <a-tooltip v-if="record.firstArticle === 1" :title="$t('mes.wo.firstArticleTip')">
          <a-tag color="purple" :bordered="false">{{ $t('mes.wo.firstArticle') }}</a-tag>
        </a-tooltip>
      </template>
      <template v-else-if="column.key === 'progress'">
        <a-progress :percent="pct(record.goodQty, detail.qty)" size="small" />
        <div class="sub-text">{{ $t('mes.wo.goodScrap', { good: formatQty(record.goodQty), scrap: formatQty(record.scrapQty) }) }}</div>
      </template>
      <template v-else-if="column.key === 'hours'">
        {{ $t('mes.wo.hoursOf', { actual: formatQty(record.hours), std: formatQty(Number(record.stdHours) * Number(detail.qty)) }) }}
      </template>
      <template v-else-if="column.key === 'plan'">
        <template v-if="record.planStart">
          <div>{{ record.planStart }}</div>
          <div class="sub-text">{{ $t('mes.wo.to', { v: record.planEnd || '-' }) }}</div>
        </template>
        <span v-else class="sub-text">{{ $t('mes.wo.unscheduled') }}</span>
      </template>
      <template v-else-if="column.key === 'action'">
        <TableActions
          :actions="[
            {
              key: 'report',
              label: $t('mes.wo.report'),
              icon: FormOutlined,
              show: [WorkOrderStatus.RELEASED, WorkOrderStatus.STARTED].includes(detail.status),
              perm: 'mes:wo:report',
              onClick: () => emit('report', record)
            }
          ]"
        />
      </template>
    </template>
  </a-table>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { FormOutlined } from '@ant-design/icons-vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { formatQty } from '@/components/wms/wms'
import { pct } from './wo'
import { WorkOrderStatus } from '@/constants/enums'

/** 工单详情 - 工序，已下达、已开工的工单可以报工（触发 report(工序)） */
defineProps({ detail: { type: Object, required: true } })
const emit = defineEmits(['report'])
const { t } = useI18n()

const columns = [
  { title: t('mes.wo.opSeq'), dataIndex: 'seq', width: 80 },
  { title: t('mes.wo.op'), key: 'name', width: 160 },
  { title: t('mes.wo.workCenter'), dataIndex: 'workCenterName', width: 140 },
  { title: t('mes.wo.opProgress'), key: 'progress', width: 220 },
  { title: t('mes.wo.actualStdHours'), key: 'hours', width: 170 },
  { title: t('mes.wo.planDate'), key: 'plan', width: 120 },
  actionColumn(1, [t('mes.wo.report')], { title: '', fixed: undefined })
]
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
