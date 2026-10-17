<template>
  <a-table row-key="id" size="small" :columns="trainingColumns" :data-source="trainings" :pagination="{ pageSize: 10 }">
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'category'"><DictTag type="hr_training_category" :value="record.category" /></template>
      <template v-else-if="column.key === 'status'"><DictTag type="hr_training_status" :value="record.trainingStatus" /></template>
      <template v-else-if="column.key === 'result'">
        <template v-if="record.trainingStatus === TrainingStatus.FINISHED">
          {{ record.attended ? $t('hr.employee.attended') : $t('hr.employee.absent') }}
          <span v-if="record.score != null"> · {{ $t('hr.recruit.scoreN', { n: record.score }) }}</span>
          <a-tag v-if="record.passed === 1" color="green" :bordered="false">{{ $t('hr.employee.passed') }}</a-tag>
          <a-tag v-else-if="record.passed === 0" color="red" :bordered="false">{{ $t('hr.employee.failed') }}</a-tag>
        </template>
        <span v-else class="sub-text">-</span>
      </template>
    </template>
  </a-table>
</template>

<script setup>
import DictTag from '@/components/DictTag.vue'
import { t } from '@/locales'
import { TrainingStatus } from '@/constants/enums'

/** 员工自助 - 我的培训 */
defineProps({ trainings: { type: Array, default: () => [] } })

const trainingColumns = [
  { title: t('hr.employee.training'), dataIndex: 'trainingTitle' },
  { title: t('hr.position.category'), key: 'category', width: 120 },
  { title: t('common.field.date'), dataIndex: 'startDate', width: 110 },
  { title: t('hr.employee.hours'), dataIndex: 'hours', width: 80 },
  { title: t('common.field.status'), key: 'status', width: 110 },
  { title: t('hr.employee.result'), key: 'result', width: 200 }
]
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
