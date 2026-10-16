<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="跟进对象" name="targetType">
          <a-select v-model:value="query.targetType" placeholder="全部" allow-clear :options="TARGET_TYPES" />
        </a-form-item>
        <a-form-item label="跟进方式" name="method">
          <a-select v-model:value="query.method" placeholder="全部" allow-clear :options="toOptions(dict.crm_follow_method)" />
        </a-form-item>
        <a-form-item label="跟进时间" name="dateRange">
          <a-range-picker v-model:value="query.dateRange" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item label="跟进人" name="mine">
          <a-checkbox v-model:checked="query.mine">只看我的</a-checkbox>
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <span class="sub-text">在客户、线索、商机中写跟进；这里汇总查看全部跟进记录</span>
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
              <a-popconfirm title="确定删除这条跟进记录？" @confirm="remove(record.id)">
                <a-button type="link" danger size="small"><template #icon><DeleteOutlined /></template>删除</a-button>
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
import { message } from 'ant-design-vue'
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

const userStore = useUserStore()
const dict = useDict('crm_follow_method')

const columns = [
  { title: '跟进时间', dataIndex: 'followTime', width: 160 },
  { title: '跟进对象', key: 'target', width: 240 },
  { title: '客户', dataIndex: 'customerName', width: 180, ellipsis: true },
  { title: '方式', key: 'method', width: 80, dict: 'crm_follow_method' },
  { title: '内容', key: 'content', width: 300 },
  { title: '下次跟进', key: 'next', width: 200 },
  { title: '跟进人', dataIndex: 'createByName', width: 100 },
  { title: '操作', key: 'action', width: 90, fixed: 'right' }
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

async function remove(id) {
  await deleteFollowup(id)
  message.success('删除成功')
  load()
}
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
