<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('monitor.online.username')" name="username">
          <a-input v-model:value="query.username" :placeholder="$t('common.placeholder.inputName', { name: $t('monitor.online.username') })" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('monitor.online.ip')" name="ip">
          <a-input v-model:value="query.ip" :placeholder="$t('common.placeholder.inputName', { name: 'IP' })" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('monitor.online.location')" name="location">
          <a-input v-model:value="query.location" :placeholder="$t('monitor.loginlog.locationPlaceholder')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select
            v-model:value="query.status"
            :placeholder="$t('common.placeholder.all')"
            allow-clear
            :options="toOptions(dict.sys_success_fail, true)"
          />
        </a-form-item>
        <a-form-item :label="$t('monitor.online.loginTime')" name="range">
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
            :title="$t('monitor.loginlog.confirmDeleteN', { n: selectedKeys.length })"
            :disabled="!selectedKeys.length"
            @confirm="remove(selectedKeys)"
          >
            <a-button danger :disabled="!selectedKeys.length">
              <template #icon><DeleteOutlined /></template>{{ $t('common.action.delete') }}
            </a-button>
          </a-popconfirm>
        </span>
        <span v-perm="'monitor:loginlog:remove'">
          <a-popconfirm :title="$t('monitor.loginlog.confirmClean')" ok-type="danger" @confirm="clean">
            <a-button danger>
              <template #icon><ClearOutlined /></template>{{ $t('common.action.clear') }}
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
              <a-popconfirm :title="$t('monitor.loginlog.confirmDelete')" @confirm="remove([record.id])">
                <a-button type="link" size="small" danger>
                  <template #icon><DeleteOutlined /></template>{{ $t('common.action.delete') }}
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
import { useI18n } from 'vue-i18n'
import { ClearOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { cleanLoginLog, deleteLoginLog, pageLoginLog } from '@/api/monitor/loginlog'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

const { t } = useI18n()
const dict = useDict('sys_success_fail')

const columns = [
  { title: t('monitor.loginlog.id'), dataIndex: 'id', width: 80 },
  { title: t('monitor.online.username'), dataIndex: 'username', width: 130 },
  { title: t('monitor.online.ip'), dataIndex: 'ip', width: 150 },
  { title: t('monitor.online.location'), dataIndex: 'location', width: 180, ellipsis: true },
  { title: t('monitor.online.browser'), dataIndex: 'browser', ellipsis: true },
  { title: t('monitor.online.os'), dataIndex: 'os', ellipsis: true },
  { title: t('common.field.status'), key: 'status', width: 90, dict: 'sys_success_fail' },
  { title: t('monitor.loginlog.msg'), dataIndex: 'msg', ellipsis: true },
  { title: t('monitor.online.loginTime'), dataIndex: 'loginTime', width: 180 },
  { title: t('common.action.operation'), key: 'action', width: 110, fixed: 'right' }
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
  message.success(t('common.msg.deleteSuccess'))
  selectedKeys.value = []
  load()
}

async function clean() {
  await cleanLoginLog()
  message.success(t('monitor.loginlog.cleaned'))
  selectedKeys.value = []
  search()
}
</script>
