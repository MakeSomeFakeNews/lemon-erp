<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="query.username" placeholder="请输入用户名" allow-clear />
        </a-form-item>
        <a-form-item label="登录 IP" name="ip">
          <a-input v-model:value="query.ip" placeholder="请输入 IP" allow-clear />
        </a-form-item>
        <a-form-item label="登录地点" name="location">
          <a-input v-model:value="query.location" placeholder="如 广东、深圳、内网IP" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select
            v-model:value="query.status"
            placeholder="全部"
            allow-clear
            :options="toOptions(dict.sys_success_fail, true)"
          />
        </a-form-item>
        <a-form-item label="登录时间" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" style="width: 100%" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll"
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :columns="columns"
        @refresh="load"
      >
        <span v-perm="'monitor:loginlog:remove'">
          <a-popconfirm
            :title="`确定删除选中的 ${selectedKeys.length} 条日志？`"
            :disabled="!selectedKeys.length"
            @confirm="remove(selectedKeys)"
          >
            <a-button danger :disabled="!selectedKeys.length">
              <template #icon><DeleteOutlined /></template>删除
            </a-button>
          </a-popconfirm>
        </span>
        <span v-perm="'monitor:loginlog:remove'">
          <a-popconfirm title="确定清空全部登录日志？此操作不可恢复" ok-type="danger" @confirm="clean">
            <a-button danger>
              <template #icon><ClearOutlined /></template>清空
            </a-button>
          </a-popconfirm>
        </span>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        :scroll="{ x: 1000, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <DictTag type="sys_success_fail" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <span v-perm="'monitor:loginlog:remove'">
              <a-popconfirm title="确定删除该日志？" @confirm="remove([record.id])">
                <a-button type="link" size="small" danger>
                  <template #icon><DeleteOutlined /></template>删除
                </a-button>
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
import { ClearOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { cleanLoginLog, deleteLoginLog, pageLoginLog } from '@/api/monitor/loginlog'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

const dict = useDict('sys_success_fail')

const columns = [
  { title: '编号', dataIndex: 'id', width: 80 },
  { title: '用户名', dataIndex: 'username', width: 130 },
  { title: '登录 IP', dataIndex: 'ip', width: 150 },
  { title: '登录地点', dataIndex: 'location', width: 180, ellipsis: true },
  { title: '浏览器', dataIndex: 'browser', ellipsis: true },
  { title: '操作系统', dataIndex: 'os', ellipsis: true },
  { title: '状态', key: 'status', width: 90, dict: 'sys_success_fail' },
  { title: '提示信息', dataIndex: 'msg', ellipsis: true },
  { title: '登录时间', dataIndex: 'loginTime', width: 180 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns)
const showSearch = ref(true)

const emptyQuery = () => ({ username: undefined, ip: undefined, location: undefined, status: undefined, range: [] })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(
  pageLoginLog,
  query,
  {
    params: ({ range, ...rest }) => ({ ...rest, beginTime: range?.[0], endTime: range?.[1] })
  }
)
onMounted(load)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

async function remove(ids) {
  await deleteLoginLog(ids)
  message.success('删除成功')
  selectedKeys.value = []
  load()
}

async function clean() {
  await cleanLoginLog()
  message.success('已清空')
  selectedKeys.value = []
  search()
}
</script>
