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
    :scroll="{ x: 1200, y: '100%' }"
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
        <span class="table-actions">
          <template v-if="record.status === RecruitStatus.DRAFT">
            <a-button v-perm="'hr:recruit:approve'" type="link" size="small" @click="approve(record)">{{ $t('common.action.audit') }}</a-button>
            <a-button v-perm="'hr:recruit:edit'" type="link" size="small" @click="formRef.openEdit(record)">{{ $t('common.action.edit') }}</a-button>
            <span v-perm="'hr:recruit:remove'">
              <a-popconfirm :title="$t('hr.recruit.confirmDelete', { name: record.title })" @confirm="removeRecruit(record.id)">
                <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
              </a-popconfirm>
            </span>
          </template>
          <span v-else-if="record.status === RecruitStatus.APPROVING" v-perm="'hr:recruit:approve'">
            <a-popconfirm :title="$t('hr.recruit.confirmWithdraw')" @confirm="withdraw(record.id)">
              <a-button type="link" size="small">{{ $t('hr.recruit.withdraw') }}</a-button>
            </a-popconfirm>
          </span>
          <template v-else-if="record.status === RecruitStatus.OPEN">
            <a-button v-perm="'hr:recruit:candidate'" type="link" size="small" @click="emit('add-candidate', record.id)">
              {{ $t('hr.recruit.addCandidate') }}
            </a-button>
            <span v-perm="'hr:recruit:edit'">
              <a-popconfirm :title="$t('hr.recruit.confirmClose')" @confirm="close(record.id)">
                <a-button type="link" size="small">{{ $t('common.action.close') }}</a-button>
              </a-popconfirm>
            </span>
          </template>
        </span>
      </template>
    </template>
  </a-table>
  <RecruitForm ref="formRef" @success="changed" />
</template>

<script setup>
import { ref } from 'vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { approveRecruit, closeRecruit, deleteRecruit, pageRecruit, withdrawRecruit } from '@/api/hr'
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
  { title: t('common.action.operation'), key: 'action', width: 230, fixed: 'right' }
]
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

defineExpose({ load, search })
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
