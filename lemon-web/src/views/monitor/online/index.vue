<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-input-search
          v-model:value="username"
          placeholder="按用户名搜索"
          allow-clear
          style="width: 220px"
          @search="load"
        />
        <a-tag color="processing" :bordered="false" class="count-tag">
          当前在线 {{ list.length }} 个会话
        </a-tag>
      </TableToolbar>

      <a-table
        row-key="tokenId"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="{ pageSize: 20, showTotal: (t) => `共 ${t} 条`, hideOnSinglePage: true }"
        :scroll="{ x: 900, y: '100%' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'username'">
            <div class="user-cell">
              <UserAvatar :name="record.username" size="small" />
              <span>{{ record.username }}</span>
              <a-tag v-if="record.userId === userStore.user?.id" color="green" :bordered="false">我</a-tag>
            </div>
          </template>
          <template v-else-if="column.key === 'tokenId'">
            <a-typography-text
              type="secondary"
              :ellipsis="{ tooltip: record.tokenId }"
              :content="record.tokenId"
              style="max-width: 140px"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <span v-perm="'monitor:online:kick'">
              <a-popconfirm :title="`确定将「${record.username}」强制下线？`" @confirm="kick(record)">
                <a-button type="link" size="small" danger>
                  <template #icon><DisconnectOutlined /></template>强退
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
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DisconnectOutlined } from '@ant-design/icons-vue'
import TableToolbar from '@/components/TableToolbar.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { kickOnline, listOnline } from '@/api/monitor/online'
import { useColumns } from '@/composables/useColumns'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const columns = [
  { title: '用户名', key: 'username', width: 180 },
  { title: '会话编号', key: 'tokenId', width: 170 },
  { title: '登录 IP', dataIndex: 'ip', width: 150 },
  { title: '登录地点', dataIndex: 'location', width: 180, ellipsis: true },
  { title: '浏览器', dataIndex: 'browser', ellipsis: true },
  { title: '操作系统', dataIndex: 'os', ellipsis: true },
  { title: '登录时间', dataIndex: 'loginTime', width: 180 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns)
const size = ref('middle')

const username = ref('')
const list = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    list.value = await listOnline({ username: username.value || undefined })
  } finally {
    loading.value = false
  }
}
onMounted(load)

async function kick(record) {
  await kickOnline(record.tokenId)
  message.success(`已将「${record.username}」强制下线`)
  load()
}
</script>

<style scoped>
.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.count-tag {
  align-self: center;
}
</style>
