<template>
  <a-drawer :open="open" :title="$t('system.notice.detailTitle')" width="min(820px, 100vw)" destroy-on-close @close="open = false">
    <template #extra>
      <a-space v-if="notice">
        <DictTag type="sys_notice_status" :value="notice.status" />
        <a-tag v-if="expired(notice)" :bordered="false">{{ $t('system.notice.expired') }}</a-tag>
      </a-space>
    </template>
    <a-spin :spinning="loading">
      <template v-if="notice">
        <div class="summary">
          <div>
            <span class="label">{{ $t('system.notice.scope') }}</span>
            <DictTag type="sys_notice_scope" :value="notice.scopeType" />
            <span v-if="notice.targetNames?.length">{{ notice.targetNames.join('、') }}</span>
          </div>
          <div v-if="notice.totalCount">
            <span class="label">{{ $t('system.notice.progress') }}</span>
            {{ $t('system.notice.readOfTotal', { read: notice.readCount, total: notice.totalCount }) }}
          </div>
          <div v-if="notice.createByName">
            <span class="label">{{ $t('common.field.createBy') }}</span>{{ notice.createByName }} · {{ notice.createTime }}
          </div>
        </div>
        <a-tabs v-model:active-key="tab">
          <a-tab-pane key="content" :tab="$t('system.notice.content')">
            <NoticeView :notice="notice" />
          </a-tab-pane>
          <a-tab-pane key="readers" :tab="$t('system.notice.readers')" :disabled="!notice.totalCount">
            <div class="reader-bar">
              <a-radio-group v-model:value="readFilter" button-style="solid" size="small" @change="search">
                <a-radio-button :value="undefined">{{ $t('system.notice.allReaders', { n: notice.totalCount }) }}</a-radio-button>
                <a-radio-button :value="true">{{ $t('system.notice.readN', { n: notice.readCount }) }}</a-radio-button>
                <a-radio-button :value="false">{{ $t('system.notice.unreadN', { n: notice.totalCount - notice.readCount }) }}</a-radio-button>
              </a-radio-group>
              <a-input-search
                v-model:value="keyword"
                :placeholder="$t('system.user.usernameOrNickname')"
                allow-clear
                size="small"
                class="reader-search"
                @search="search"
              />
            </div>
            <a-table
              row-key="userId"
              size="small"
              :columns="columns"
              :data-source="list"
              :loading="tableLoading"
              :pagination="pagination"
              :scroll="{ x: 560 }"
              @change="onTableChange"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'user'">
                  <div>{{ record.nickname || record.username }}</div>
                  <div class="sub-text">{{ record.username }}</div>
                </template>
                <template v-else-if="column.key === 'readTime'">
                  <span v-if="record.readTime">{{ record.readTime }}</span>
                  <a-badge v-else status="default" :text="$t('system.notice.unread')" />
                </template>
              </template>
            </a-table>
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-spin>
  </a-drawer>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import NoticeView from '@/components/system/NoticeView.vue'
import { getNotice, noticeReaders } from '@/api/system/notice'
import { expired } from './notice'

/** 管理端公告详情：内容预览与阅读情况（已读 / 未读名单）；open(id, 'readers') 直接打开阅读情况 */
const { t } = useI18n()
const open = ref(false)
const loading = ref(false)
const notice = ref(null)
const tab = ref('content')

const columns = [
  { title: t('common.field.user'), key: 'user', width: 180 },
  { title: t('common.field.dept'), dataIndex: 'deptName', width: 160, ellipsis: true },
  { title: t('system.notice.readTime'), key: 'readTime', width: 170 }
]
const list = ref([])
const tableLoading = ref(false)
const readFilter = ref(undefined)
const keyword = ref('')
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: false, showTotal: (n) => t('common.table.total', { total: n }) })

async function loadReaders() {
  tableLoading.value = true
  try {
    const res = await noticeReaders(notice.value.id, {
      read: readFilter.value,
      keyword: keyword.value || undefined,
      current: pagination.current,
      size: pagination.pageSize
    })
    list.value = res.records
    pagination.total = res.total
  } finally {
    tableLoading.value = false
  }
}

function search() {
  pagination.current = 1
  loadReaders()
}

function onTableChange(p) {
  pagination.current = p.current
  loadReaders()
}

async function show(id, initialTab = 'content') {
  notice.value = null
  list.value = []
  readFilter.value = undefined
  keyword.value = ''
  pagination.current = 1
  open.value = true
  loading.value = true
  try {
    notice.value = await getNotice(id)
  } catch {
    open.value = false
    return
  } finally {
    loading.value = false
  }
  tab.value = initialTab === 'readers' && notice.value.totalCount ? 'readers' : 'content'
  if (notice.value.totalCount) loadReaders()
}

defineExpose({ open: show })
</script>

<style scoped>
.summary {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 24px;
  padding: 10px 14px;
  border-radius: 6px;
  background: var(--app-hover);
  color: var(--app-text-2);
}
.label {
  margin-right: 8px;
  color: var(--app-text-3);
}
.reader-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
}
.reader-search {
  width: 220px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
