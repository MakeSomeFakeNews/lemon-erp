<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <div class="toolbar">
        <a-radio-group v-model:value="query.unread" button-style="solid" size="small" @change="search">
          <a-radio-button :value="undefined">{{ $t('common.status.all') }}</a-radio-button>
          <a-radio-button :value="true">{{ $t('message.unread') }}</a-radio-button>
        </a-radio-group>
        <a-select
          v-model:value="query.category"
          :placeholder="$t('message.allCategories')"
          allow-clear
          size="small"
          class="category"
          :options="toOptions(dict.sys_message_category)"
          @change="search"
        />
        <span class="spacer" />
        <a-button size="small" :disabled="!selectedKeys.length" @click="read(selectedKeys)">{{ $t('message.markRead') }}</a-button>
        <a-button size="small" @click="read([])">{{ $t('message.markAllRead') }}</a-button>
        <a-popconfirm :title="$t('message.confirmDeleteN', { n: selectedKeys.length })" :disabled="!selectedKeys.length" @confirm="remove(selectedKeys)">
          <a-button size="small" danger :disabled="!selectedKeys.length">{{ $t('common.action.delete') }}</a-button>
        </a-popconfirm>
      </div>
      <a-table
        row-key="id"
        size="middle"
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        :scroll="{ x: 900, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <a :class="{ unread: record.readFlag === 0 }" @click="open(record)">{{ record.title }}</a>
            <div v-if="record.content && record.content !== record.title" class="sub-text">{{ record.content }}</div>
          </template>
          <template v-else-if="column.key === 'category'">
            <DictTag type="sys_message_category" :value="record.category" />
          </template>
          <template v-else-if="column.key === 'read'">
            <a-badge :status="record.readFlag ? 'default' : 'processing'" :text="record.readFlag ? $t('message.read') : $t('message.unread')" />
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import { deleteMessage, pageMessage, readMessage } from '@/api/system/message'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

const { t } = useI18n()
const router = useRouter()
const dict = useDict('sys_message_category')
const columns = [
  { title: t('message.message'), key: 'title' },
  { title: t('message.category'), key: 'category', width: 100 },
  { title: t('common.field.status'), key: 'read', width: 90 },
  { title: t('message.time'), dataIndex: 'createTime', width: 170 }
]
const query = reactive({ unread: undefined, category: undefined })
const { list, loading, pagination, selectedKeys, rowSelection, load, search, onTableChange } = useTable(pageMessage, query)
onMounted(load)

async function open(record) {
  if (!record.readFlag) await readMessage([record.id])
  if (record.link) router.push(record.link).catch(() => {})
  else load()
}

async function read(ids) {
  await readMessage(ids)
  message.success(t('message.markedRead'))
  selectedKeys.value = []
  load()
}

async function remove(ids) {
  await deleteMessage(ids)
  message.success(t('common.msg.deleted'))
  selectedKeys.value = []
  load()
}
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.category {
  width: 140px;
}
.spacer {
  flex: 1;
}
.unread {
  font-weight: 600;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
