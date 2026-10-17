<template>
  <div class="tab-head">
    <a-button type="primary" @click="emit('apply', LeaveKind.LEAVE)">{{ $t('hr.self.applyLeave') }}</a-button>
    <a-button @click="emit('apply', LeaveKind.OVERTIME)">{{ $t('hr.self.applyOvertime') }}</a-button>
  </div>
  <a-table row-key="id" size="small" :columns="leaveColumns" :data-source="leaves" :pagination="{ pageSize: 10 }" :scroll="{ x: leaveScrollX }">
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'kind'">
        <DictTag type="hr_leave_kind" :value="record.kind" />
        <DictTag v-if="record.type" type="hr_leave_type" :value="record.type" />
      </template>
      <template v-else-if="column.key === 'time'">{{ $t('hr.self.timeRange', { start: fmt(record.startTime), end: fmt(record.endTime) }) }}</template>
      <template v-else-if="column.key === 'hours'">{{ hoursText(record.hours) }}</template>
      <template v-else-if="column.key === 'status'">
        <DictTag type="hr_leave_status" :value="record.status" />
        <div v-if="record.approveComment" class="sub-text">{{ record.approveComment }}</div>
      </template>
      <template v-else-if="column.key === 'action'">
        <a-popconfirm v-if="canCancel(record)" :title="$t('hr.self.confirmCancel')" @confirm="cancel(record.id)">
          <a-button type="link" size="small">{{ $t('hr.self.cancel') }}</a-button>
        </a-popconfirm>
      </template>
    </template>
  </a-table>
</template>

<script setup>
import dayjs from 'dayjs'
import DictTag from '@/components/DictTag.vue'
import { hoursText } from '@/components/hr/hr'
import { cancelLeave } from '@/api/hr'
import { useAction } from '@/composables/useAction'
import { t } from '@/locales'
import { LeaveKind, LeaveStatus } from '@/constants/enums'

/** 员工自助 - 请假加班：申请触发 apply(LEAVE / OVERTIME)，撤销后触发 changed */
defineProps({ leaves: { type: Array, default: () => [] } })
const emit = defineEmits(['apply', 'changed'])

const fmt = (v) => (v ? dayjs(v).format('MM-DD HH:mm') : '')
const leaveColumns = [
  { title: t('hr.self.docNo'), dataIndex: 'leaveNo', width: 150 },
  { title: t('hr.self.kind'), key: 'kind', width: 150 },
  { title: t('hr.self.time'), key: 'time', width: 220 },
  { title: t('hr.self.duration'), key: 'hours', width: 140 },
  { title: t('hr.self.reason'), dataIndex: 'reason', width: 200, ellipsis: true },
  { title: t('common.field.status'), key: 'status', width: 140 },
  { title: '', key: 'action', width: 80 }
]
const leaveScrollX = leaveColumns.reduce((sum, c) => sum + c.width, 0)
const canCancel = (r) => r.status === LeaveStatus.APPROVING || (r.status === LeaveStatus.APPROVED && dayjs(r.startTime).isAfter(dayjs()))
const cancel = useAction(cancelLeave, { successKey: 'hr.self.cancelled', reload: () => emit('changed') })
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.tab-head {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}
</style>
