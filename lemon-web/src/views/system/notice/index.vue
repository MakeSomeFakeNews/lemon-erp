<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('system.notice.titleLabel')" name="title">
          <a-input v-model:value="query.title" :placeholder="$t('system.notice.titlePh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('system.notice.type')" name="type">
          <a-select v-model:value="query.type" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_notice_type)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_notice_status)" />
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
        <a-button v-perm="'sys:notice:add'" type="primary" @click="form.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('system.notice.createTitle') }}
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <a class="title-link" @click="detail.open(record.id)">
              <PushpinFilled v-if="record.pinned" class="pin" />{{ record.title }}
            </a>
          </template>
          <template v-else-if="column.key === 'type'">
            <DictTag type="sys_notice_type" :value="record.type" />
          </template>
          <template v-else-if="column.key === 'scope'">
            <DictTag type="sys_notice_scope" :value="record.scopeType" />
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="sys_notice_status" :value="record.status" />
            <a-tag v-if="record.status === NoticeStatus.PUBLISHED && expired(record)" :bordered="false">{{ $t('system.notice.expired') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'progress'">
            <a v-if="record.totalCount" class="progress" @click="detail.open(record.id, 'readers')">
              <a-progress :percent="percent(record)" size="small" :show-info="false" class="progress-bar" />
              <span class="progress-text">{{ record.readCount }} / {{ record.totalCount }}</span>
            </a>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'publish'">
            <template v-if="record.publishTime">
              <div>{{ record.publishTime }}</div>
              <div class="sub-text">{{ record.publishByName }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'expireDate'">
            <span :class="{ 'sub-text': expired(record) }">{{ record.expireDate || $t('system.notice.longTerm') }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <template v-if="editable(record)">
                <a-button v-perm="'sys:notice:publish'" type="link" size="small" @click="publish(record)">
                  <template #icon><SendOutlined /></template>{{ $t('system.notice.publish') }}
                </a-button>
                <a-button v-perm="'sys:notice:edit'" type="link" size="small" @click="form.openEdit(record)">
                  <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
                </a-button>
                <span v-perm="'sys:notice:remove'">
                  <a-popconfirm :title="$t('system.notice.confirmDelete', { name: record.title })" @confirm="remove(record.id)">
                    <a-button type="link" danger size="small">
                      <template #icon><DeleteOutlined /></template>{{ $t('common.action.delete') }}
                    </a-button>
                  </a-popconfirm>
                </span>
              </template>
              <a-button v-else v-perm="'sys:notice:publish'" type="link" size="small" @click="withdraw(record)">
                <template #icon><RollbackOutlined /></template>{{ $t('system.notice.withdraw') }}
              </a-button>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <NoticeForm ref="form" @success="load" />
    <NoticeDetail ref="detail" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, EditOutlined, PlusOutlined, PushpinFilled, RollbackOutlined, SendOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { deleteNotice, pageNotice, publishNotice, withdrawNotice } from '@/api/system/notice'
import { useAction } from '@/composables/useAction'
import { actionWidth, useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { NoticeStatus } from '@/constants/enums'
import NoticeDetail from './NoticeDetail.vue'
import NoticeForm from './NoticeForm.vue'
import { expired } from './notice'

/**
 * 通知公告（管理端）：草稿 → 发布（按范围确定接收人并发站内消息）→ 撤回（可修改后再次发布）；
 * 阅读进度为已读 / 应读人数，点击查看已读、未读名单
 */
const { t } = useI18n()
const dict = useDict('sys_notice_type', 'sys_notice_status', 'sys_notice_scope')
const form = ref()
const detail = ref()

const columns = [
  { title: t('system.notice.titleLabel'), key: 'title', dataIndex: 'title', width: 210, ellipsis: true },
  { title: t('system.notice.type'), key: 'type', dataIndex: 'type', width: 90, dict: 'sys_notice_type' },
  { title: t('system.notice.scope'), key: 'scope', dataIndex: 'scopeType', width: 100, dict: 'sys_notice_scope' },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 140, dict: 'sys_notice_status' },
  { title: t('system.notice.progress'), key: 'progress', width: 140, exportValue: (r) => (r.totalCount ? `${r.readCount}/${r.totalCount}` : '') },
  { title: t('system.notice.publishTime'), key: 'publish', dataIndex: 'publishTime', width: 160 },
  { title: t('system.notice.expireDate'), key: 'expireDate', dataIndex: 'expireDate', width: 110, defaultHidden: true },
  { title: t('common.action.operation'), key: 'action', width: actionWidth([t('system.notice.publish'), t('common.action.edit'), t('common.action.delete')]), fixed: 'right' }
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'sys-notice')
const showSearch = ref(true)

const query = reactive({ title: undefined, type: undefined, status: undefined })
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageNotice, query)
onMounted(load)

function resetQuery() {
  Object.assign(query, { title: undefined, type: undefined, status: undefined })
  search()
}

const editable = (r) => r.status === NoticeStatus.DRAFT || r.status === NoticeStatus.WITHDRAWN
const percent = (r) => (r.totalCount ? Math.round((r.readCount * 100) / r.totalCount) : 0)

const publish = useAction((r) => publishNotice(r.id), {
  confirm: (r) => ({ title: t('system.notice.publishConfirm', { name: r.title }), content: t('system.notice.publishContent') }),
  successKey: (n) => t('system.notice.published', { n }),
  reload: () => load()
})
const withdraw = useAction((r) => withdrawNotice(r.id), {
  confirm: (r) => ({ title: t('system.notice.withdrawConfirm', { name: r.title }), content: t('system.notice.withdrawContent') }),
  successKey: 'system.notice.withdrawn',
  reload: () => load()
})
const remove = useAction(deleteNotice, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
</script>

<style scoped>
.title-link {
  font-weight: 500;
}
.pin {
  color: var(--status-critical);
  margin-right: 4px;
}
.progress {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--app-text-2);
}
.progress-bar {
  flex: 0 0 48px;
  width: 48px;
  margin: 0;
}
.progress-text {
  white-space: nowrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
