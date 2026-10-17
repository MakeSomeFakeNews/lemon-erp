<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('crm.followup.target')" name="targetType">
          <a-select v-model:value="query.targetType" :placeholder="$t('crm.common.all')" allow-clear :options="TARGET_TYPES" />
        </a-form-item>
        <a-form-item :label="$t('crm.followupModal.method')" name="method">
          <a-select v-model:value="query.method" :placeholder="$t('crm.common.all')" allow-clear :options="toOptions(dict.crm_follow_method)" />
        </a-form-item>
        <a-form-item :label="$t('crm.followupModal.followTime')" name="dateRange">
          <a-range-picker v-model:value="query.dateRange" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item :label="$t('crm.followup.followBy')" name="mine">
          <a-checkbox v-model:checked="query.mine">{{ $t('crm.common.onlyMine') }}</a-checkbox>
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <span class="sub-text">{{ $t('crm.followup.tip') }}</span>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1250, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'target'">
            <a-tag :color="targetType(record.targetType).color" :bordered="false">{{ targetType(record.targetType).label }}</a-tag>
            {{ record.targetName }}
          </template>
          <template v-else-if="column.key === 'method'">
            <DictTag type="crm_follow_method" :value="record.method" />
          </template>
          <template v-else-if="column.key === 'content'">
            <a-tooltip :title="record.content">
              <span class="ellipsis">{{ record.content }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'next'">
            <template v-if="record.nextTime">
              <div>{{ record.nextTime }}</div>
              <div v-if="record.nextPlan" class="sub-text">{{ record.nextPlan }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span v-if="record.createBy === userStore.user?.id || userStore.hasPerm('crm:followup:remove')">
              <a-popconfirm :title="$t('crm.followup.confirmDelete')" @confirm="remove(record.id)">
                <a-button type="link" danger size="small"><template #icon><DeleteOutlined /></template>{{ $t('common.action.delete') }}</a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { deleteFollowup, pageFollowup } from '@/api/crm'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { TARGET_TYPES, targetType } from '@/components/crm/crm'
import { useAction } from '@/composables/useAction'

const userStore = useUserStore()
const { t } = useI18n()
const dict = useDict('crm_follow_method')

const columns = [
  { title: t('crm.followupModal.followTime'), dataIndex: 'followTime', width: 160 },
  { title: t('crm.followup.target'), key: 'target', width: 240 },
  { title: t('crm.common.f.customer'), dataIndex: 'customerName', width: 180, ellipsis: true },
  { title: t('crm.followup.method'), key: 'method', width: 90, dict: 'crm_follow_method' },
  { title: t('crm.followup.content'), key: 'content', width: 300 },
  { title: t('crm.common.f.nextFollow'), key: 'next', width: 200 },
  { title: t('crm.followup.followBy'), dataIndex: 'createByName', width: 100 },
  { title: t('common.action.operation'), key: 'action', width: 100, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'crm-followup')
const showSearch = ref(true)

const emptyQuery = () => ({ targetType: undefined, method: undefined, dateRange: undefined, mine: false })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageFollowup, query, {
  params: ({ dateRange, ...q }) => ({ ...q, beginDate: dateRange?.[0], endDate: dateRange?.[1] })
})

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

onMounted(load)

const remove = useAction(deleteFollowup, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
.ellipsis {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
