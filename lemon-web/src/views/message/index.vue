<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <div class="toolbar">
        <a-radio-group v-model:value="query.unread" button-style="solid" size="small" @change="search">
          <a-radio-button :value="undefined">全部</a-radio-button>
          <a-radio-button :value="true">未读</a-radio-button>
        </a-radio-group>
        <a-select
          v-model:value="query.category"
          placeholder="全部类别"
          allow-clear
          size="small"
          class="category"
          :options="toOptions(dict.sys_message_category)"
          @change="search"
        />
        <span class="spacer" />
        <a-button size="small" :disabled="!selectedKeys.length" @click="read(selectedKeys)">标为已读</a-button>
        <a-button size="small" @click="read([])">全部已读</a-button>
        <a-popconfirm :title="`删除选中的 ${selectedKeys.length} 条消息？`" :disabled="!selectedKeys.length" @confirm="remove(selectedKeys)">
          <a-button size="small" danger :disabled="!selectedKeys.length">删除</a-button>
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
            <a-badge :status="record.readFlag ? 'default' : 'processing'" :text="record.readFlag ? '已读' : '未读'" />
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
import DictTag from '@/components/DictTag.vue'
import { deleteMessage, pageMessage, readMessage } from '@/api/system/message'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

const router = useRouter()
const dict = useDict('sys_message_category')
const columns = [
  { title: '消息', key: 'title' },
  { title: '类别', key: 'category', width: 90 },
  { title: '状态', key: 'read', width: 80 },
  { title: '时间', dataIndex: 'createTime', width: 170 }
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
  message.success('已标为已读')
  selectedKeys.value = []
  load()
}

async function remove(ids) {
  await deleteMessage(ids)
  message.success('已删除')
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
