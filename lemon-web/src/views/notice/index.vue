<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <div class="toolbar">
        <a-radio-group v-model:value="query.unread" button-style="solid" size="small" @change="search">
          <a-radio-button :value="false">{{ $t('common.status.all') }}</a-radio-button>
          <a-radio-button :value="true">{{ $t('system.notice.unread') }}</a-radio-button>
        </a-radio-group>
        <a-select
          v-model:value="query.type"
          :placeholder="$t('system.notice.allTypes')"
          allow-clear
          size="small"
          class="type-select"
          :options="toOptions(dict.sys_notice_type)"
          @change="search"
        />
        <a-input-search
          v-model:value="query.keyword"
          :placeholder="$t('system.notice.titlePh')"
          allow-clear
          size="small"
          class="keyword"
          @search="search"
        />
      </div>
      <a-table
        row-key="id"
        size="middle"
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 760, y: '100%' }"
        :custom-row="(record) => ({ onClick: () => openNotice(record.id), class: 'clickable-row' })"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <span :class="['title', { unread: !record.read }]">
              <span v-if="!record.read" class="dot" />
              <PushpinFilled v-if="record.pinned" class="pin" />
              <a>{{ record.title }}</a>
            </span>
          </template>
          <template v-else-if="column.key === 'type'">
            <DictTag type="sys_notice_type" :value="record.type" />
          </template>
          <template v-else-if="column.key === 'expireDate'">
            {{ record.expireDate || $t('system.notice.longTerm') }}
          </template>
        </template>
      </a-table>
    </a-card>

    <NoticeDrawer ref="drawer" @read="onRead" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { PushpinFilled } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import NoticeDrawer from '@/components/system/NoticeDrawer.vue'
import { pageMyNotice } from '@/api/system/notice'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

/**
 * 全部公告（所有登录用户）：只显示发给自己、已发布且未过期的公告，置顶在前；点击打开详情并标记已读。
 * 站内消息的链接 /notices?id= 进入时直接打开该公告
 */
const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const dict = useDict('sys_notice_type')
const drawer = ref()

const columns = [
  { title: t('system.notice.titleLabel'), key: 'title' },
  { title: t('system.notice.type'), key: 'type', width: 100 },
  { title: t('system.notice.publisher'), dataIndex: 'publishByName', width: 120, ellipsis: true },
  { title: t('system.notice.publishTime'), dataIndex: 'publishTime', width: 170 },
  { title: t('system.notice.expireDate'), key: 'expireDate', width: 110 }
]
const query = reactive({ unread: false, type: undefined, keyword: undefined })
const { list, loading, pagination, load, search, onTableChange } = useTable(pageMyNotice, query)

const openNotice = (id) => drawer.value?.open(id)

function onRead(id) {
  const row = list.value.find((n) => n.id === id)
  if (row) row.read = true
}

/** 从消息链接进入：打开后去掉地址中的 id，刷新页面时不再重复打开 */
function openFromRoute() {
  const id = Number(route.query.id)
  if (!id) return
  openNotice(id)
  router.replace({ query: { ...route.query, id: undefined } })
}

onMounted(() => {
  load()
  openFromRoute()
})
watch(() => route.query.id, openFromRoute)
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.type-select {
  width: 140px;
}
.keyword {
  width: 220px;
}
.title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.title.unread a {
  font-weight: 600;
}
.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--app-primary);
  flex-shrink: 0;
}
.pin {
  color: var(--status-critical);
}
:deep(.clickable-row) {
  cursor: pointer;
}
</style>
