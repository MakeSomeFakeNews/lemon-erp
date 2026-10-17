<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('monitor.operlog.title')" name="title">
          <a-input v-model:value="query.title" :placeholder="$t('monitor.operlog.titlePlaceholder')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('monitor.operlog.operator')" name="operator">
          <a-input v-model:value="query.operator" :placeholder="$t('common.placeholder.inputName', { name: $t('monitor.operlog.operator') })" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('monitor.operlog.operType')" name="operType">
          <a-select
            v-model:value="query.operType"
            :placeholder="$t('common.placeholder.all')"
            allow-clear
            :options="toOptions(dict.sys_oper_type, true)"
          />
        </a-form-item>
        <a-form-item :label="$t('monitor.operlog.status')" name="status">
          <a-select
            v-model:value="query.status"
            :placeholder="$t('common.placeholder.all')"
            allow-clear
            :options="toOptions(dict.sys_success_fail, true)"
          />
        </a-form-item>
        <a-form-item :label="$t('monitor.operlog.operTime')" name="range">
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
        <span v-perm="'monitor:operlog:remove'">
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
        <span v-perm="'monitor:operlog:remove'">
          <a-popconfirm :title="$t('monitor.operlog.confirmClean')" ok-type="danger" @confirm="clean">
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
        :scroll="{ x: 1200, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'operType'">
            <DictTag type="sys_oper_type" :value="record.operType" />
          </template>
          <template v-else-if="column.key === 'url'">
            <span class="request">
              <a-tag :color="METHOD_COLORS[record.requestMethod]" :bordered="false">{{ record.requestMethod }}</a-tag>
              <a-typography-text :ellipsis="{ tooltip: record.url }" :content="record.url" class="url" />
            </span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="sys_success_fail" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'costTime'">
            <span :class="{ slow: record.costTime >= 1000 }">{{ record.costTime }} ms</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">
                <template #icon><EyeOutlined /></template>{{ $t('common.action.detail') }}
              </a-button>
              <span v-perm="'monitor:operlog:remove'">
                <a-popconfirm :title="$t('monitor.loginlog.confirmDelete')" @confirm="remove([record.id])">
                  <a-button type="link" size="small" danger>
                    <template #icon><DeleteOutlined /></template>{{ $t('common.action.delete') }}
                  </a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情 -->
    <a-drawer v-model:open="detailOpen" :title="$t('monitor.operlog.detailTitle')" width="min(720px, 100vw)">
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <a-descriptions :column="{ xs: 1, sm: 2 }" bordered size="small">
            <a-descriptions-item :label="$t('monitor.operlog.title')">{{ detail.title }}</a-descriptions-item>
            <a-descriptions-item :label="$t('monitor.operlog.operType')">
              <DictTag type="sys_oper_type" :value="detail.operType" />
            </a-descriptions-item>
            <a-descriptions-item :label="$t('monitor.operlog.operator')">{{ detail.operator }}</a-descriptions-item>
            <a-descriptions-item :label="$t('monitor.operlog.ip')">
              {{ detail.ip }}
              <span v-if="detail.location" class="location">（{{ detail.location }}）</span>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('monitor.operlog.operTime')">{{ detail.operTime }}</a-descriptions-item>
            <a-descriptions-item :label="$t('monitor.operlog.cost')">{{ detail.costTime }} ms</a-descriptions-item>
            <a-descriptions-item :label="$t('common.field.status')">
              <DictTag type="sys_success_fail" :value="detail.status" />
            </a-descriptions-item>
            <a-descriptions-item :label="$t('monitor.operlog.request')">
              <a-tag :color="METHOD_COLORS[detail.requestMethod]" :bordered="false">{{ detail.requestMethod }}</a-tag>
              {{ detail.url }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('monitor.operlog.method')" :span="2">
              <a-typography-text code copyable>{{ detail.method }}</a-typography-text>
            </a-descriptions-item>
          </a-descriptions>

          <h4 class="block-title">{{ $t('monitor.operlog.params') }}</h4>
          <pre class="code-block">{{ prettyJson(detail.params) || $t('common.status.none') }}</pre>

          <h4 class="block-title">{{ $t('monitor.operlog.result') }}</h4>
          <pre class="code-block">{{ prettyJson(detail.result) || $t('common.status.none') }}</pre>

          <template v-if="detail.errorMsg">
            <h4 class="block-title">{{ $t('monitor.operlog.error') }}</h4>
            <a-alert type="error" :message="detail.errorMsg" show-icon />
          </template>
        </template>
      </a-spin>
    </a-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { ClearOutlined, DeleteOutlined, EyeOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { cleanOperLog, deleteOperLog, getOperLog, pageOperLog } from '@/api/monitor/operlog'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { prettyJson } from '@/utils/format'

const METHOD_COLORS = { GET: 'blue', POST: 'green', PUT: 'orange', DELETE: 'red' }
const { t } = useI18n()
const dict = useDict('sys_oper_type', 'sys_success_fail')

const columns = [
  { title: t('monitor.loginlog.id'), dataIndex: 'id', width: 80 },
  { title: t('monitor.operlog.title'), dataIndex: 'title', width: 130 },
  { title: t('monitor.operlog.operType'), key: 'operType', width: 100, dict: 'sys_oper_type' },
  { title: t('monitor.operlog.operator'), dataIndex: 'operator', width: 110 },
  { title: t('monitor.operlog.request'), key: 'url', width: 260 },
  { title: t('monitor.operlog.ip'), dataIndex: 'ip', width: 140 },
  { title: t('monitor.operlog.location'), dataIndex: 'location', width: 160, ellipsis: true },
  { title: t('common.field.status'), key: 'status', width: 90, dict: 'sys_success_fail' },
  { title: t('monitor.operlog.cost'), key: 'costTime', width: 100 },
  { title: t('monitor.operlog.operTime'), dataIndex: 'operTime', width: 180 },
  { title: t('common.action.operation'), key: 'action', width: 170, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns)
const showSearch = ref(true)

const emptyQuery = () => ({ title: undefined, operator: undefined, operType: undefined, status: undefined, range: [] })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(
  pageOperLog,
  query,
  {
    // 时间范围拆分为 beginTime / endTime
    params: ({ range, ...rest }) => ({ ...rest, beginTime: range?.[0], endTime: range?.[1] })
  }
)
onMounted(load)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

async function remove(ids) {
  await deleteOperLog(ids)
  message.success(t('common.msg.deleteSuccess'))
  selectedKeys.value = []
  load()
}

async function clean() {
  await cleanOperLog()
  message.success(t('monitor.loginlog.cleaned'))
  selectedKeys.value = []
  search()
}

// 详情
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

async function openDetail(record) {
  detail.value = record
  detailOpen.value = true
  detailLoading.value = true
  try {
    detail.value = await getOperLog(record.id)
  } finally {
    detailLoading.value = false
  }
}
</script>

<style scoped>
.request {
  display: flex;
  align-items: center;
  min-width: 0;
}
.url {
  max-width: 180px;
}
.slow {
  color: #d4380d;
  font-weight: 500;
}
.block-title {
  margin: 20px 0 8px;
  font-weight: 500;
  color: var(--app-text);
}
.location {
  color: var(--app-text-3);
}
</style>
