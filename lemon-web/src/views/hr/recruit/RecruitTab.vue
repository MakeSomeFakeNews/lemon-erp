<template>
  <TableToolbar v-model:size="size" :columns="recruitColumns" @refresh="load">
    <a-button v-perm="'hr:recruit:add'" type="primary" @click="formRef.openCreate()">
      <template #icon><PlusOutlined /></template>{{ $t('hr.recruit.addRecruit') }}
    </a-button>
  </TableToolbar>
  <a-table
    row-key="id"
    :size="size"
    :columns="recruitColumns"
    :data-source="list"
    :loading="loading"
    :pagination="pagination"
    :scroll="{ x: recruitScrollX, y: '100%' }"
    @change="onTableChange"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'title'">
        <div>{{ record.title }}</div>
        <div class="sub-text">{{ record.recruitNo }}</div>
      </template>
      <template v-else-if="column.key === 'dept'">
        <div>{{ record.deptName }}</div>
        <div class="sub-text">{{ record.positionName || '-' }}</div>
      </template>
      <template v-else-if="column.key === 'progress'">
        <a-progress :percent="Math.round((record.hiredCount / record.headcount) * 100)" size="small" :format="() => `${record.hiredCount}/${record.headcount}`" />
      </template>
      <template v-else-if="column.key === 'candidates'">
        <a @click="emit('view-candidates', record)">{{ $t('hr.recruit.peopleN', { n: record.candidateCount }) }}</a>
      </template>
      <template v-else-if="column.key === 'status'">
        <DictTag type="hr_recruit_status" :value="record.status" />
      </template>
      <template v-else-if="column.key === 'action'">
        <TableActions :actions="rowActions(record)" />
      </template>
    </template>
  </a-table>
  <RecruitForm ref="formRef" @success="changed" />
</template>

<script setup>
import { ref } from 'vue'
import { CloseCircleOutlined, PlusOutlined, RollbackOutlined, UserAddOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { approveRecruit, closeRecruit, deleteRecruit, pageRecruit, withdrawRecruit } from '@/api/hr'
import { sumWidth } from '@/composables/useColumns'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { useAction } from '@/composables/useAction'
import { useTable } from '@/composables/useTable'
import { t } from '@/locales'
import RecruitForm from './RecruitForm.vue'
import { RecruitStatus } from '@/constants/enums'

/**
 * 招聘需求列表（放在 table-card 中）：query 为页面上的筛选条件，通过 ref 调用 load() / search()。
 * 查看应聘者触发 view-candidates(需求)，录入简历触发 add-candidate(需求ID)，需求状态变化后触发 options-changed（刷新需求下拉）
 */
const props = defineProps({ query: { type: Object, required: true } })
const emit = defineEmits(['view-candidates', 'add-candidate', 'options-changed'])

const recruitColumns = [
  { title: t('hr.recruit.position'), key: 'title', width: 220, fixed: 'left' },
  { title: t('hr.recruit.deptPosition'), key: 'dept', width: 190 },
  { title: t('hr.recruit.progress'), key: 'progress', width: 150 },
  { title: t('hr.recruit.candidate'), key: 'candidates', width: 100 },
  { title: t('hr.recruit.salaryRange'), dataIndex: 'salaryRange', width: 120 },
  { title: t('hr.recruit.deadline'), dataIndex: 'deadline', width: 130 },
  { title: t('common.field.status'), key: 'status', width: 110 },
  { title: t('hr.recruit.applicant'), dataIndex: 'createByName', width: 100 },
  actionColumn(3, [t('hr.recruit.withdraw')])
]
const recruitScrollX = sumWidth(recruitColumns)
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageRecruit, props.query)
const formRef = ref()

/** 刷新列表和需求下拉 */
function changed() {
  load()
  emit('options-changed')
}

const approve = useAction((record) => approveRecruit(record.id), {
  successKey: (flow) => (flow ? t('hr.recruit.submittedFlow') : t('hr.recruit.approved')),
  reload: changed
})
const withdraw = useAction(withdrawRecruit, { successKey: 'hr.recruit.withdrawn', reload: () => load() })
const close = useAction(closeRecruit, { successKey: 'hr.recruit.closed', reload: changed })
const removeRecruit = useAction(deleteRecruit, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

// 行操作：草稿可编辑、审核、删除；审批中可撤回；招聘中可添加候选人、关闭
const rowActions = (record) => {
  const draft = record.status === RecruitStatus.DRAFT
  const open = record.status === RecruitStatus.OPEN
  return [
    { type: 'edit', show: draft, perm: 'hr:recruit:edit', onClick: () => formRef.value.openEdit(record) },
    { type: 'audit', show: draft, perm: 'hr:recruit:approve', onClick: () => approve(record) },
    { type: 'delete', show: draft, perm: 'hr:recruit:remove', confirm: t('hr.recruit.confirmDelete', { name: record.title }), onClick: () => removeRecruit(record.id) },
    {
      key: 'withdraw',
      label: t('hr.recruit.withdraw'),
      icon: RollbackOutlined,
      show: record.status === RecruitStatus.APPROVING,
      perm: 'hr:recruit:approve',
      confirm: t('hr.recruit.confirmWithdraw'),
      onClick: () => withdraw(record.id)
    },
    { key: 'addCandidate', label: t('hr.recruit.addCandidate'), icon: UserAddOutlined, show: open, perm: 'hr:recruit:candidate', onClick: () => emit('add-candidate', record.id) },
    { key: 'close', label: t('common.action.close'), icon: CloseCircleOutlined, show: open, perm: 'hr:recruit:edit', confirm: t('hr.recruit.confirmClose'), onClick: () => close(record.id) }
  ]
}

defineExpose({ load, search })
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
