<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-input-search
          v-model:value="username"
          :placeholder="$t('monitor.online.searchUser')"
          allow-clear
          style="width: 220px"
          @search="load"
        />
        <a-tag color="processing" :bordered="false" class="count-tag">
          {{ $t('monitor.online.count', { n: list.length }) }}
        </a-tag>
      </TableToolbar>

      <a-table
        row-key="tokenId"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="{ pageSize: 20, showTotal: (total) => $t('common.table.total', { total }), hideOnSinglePage: true }"
        :scroll="{ x: 900, y: '100%' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'username'">
            <div class="user-cell">
              <UserAvatar :name="record.username" size="small" />
              <span>{{ record.username }}</span>
              <a-tag v-if="record.userId === userStore.user?.id" color="green" :bordered="false">{{ $t('monitor.online.me') }}</a-tag>
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
              <a-popconfirm :title="$t('monitor.online.confirmKick', { name: record.username })" @confirm="kick(record)">
                <a-button type="link" size="small" danger>
                  <template #icon><DisconnectOutlined /></template>{{ $t('monitor.online.kick') }}
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
import { useI18n } from 'vue-i18n'
import { DisconnectOutlined } from '@ant-design/icons-vue'
import TableToolbar from '@/components/TableToolbar.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { kickOnline, listOnline } from '@/api/monitor/online'
import { useColumns } from '@/composables/useColumns'
import { useUserStore } from '@/store/user'

const { t } = useI18n()
const userStore = useUserStore()

const columns = [
  { title: t('monitor.online.username'), key: 'username', width: 180 },
  { title: t('monitor.online.tokenId'), key: 'tokenId', width: 170 },
  { title: t('monitor.online.ip'), dataIndex: 'ip', width: 150 },
  { title: t('monitor.online.location'), dataIndex: 'location', width: 180, ellipsis: true },
  { title: t('monitor.online.browser'), dataIndex: 'browser', ellipsis: true },
  { title: t('monitor.online.os'), dataIndex: 'os', ellipsis: true },
  { title: t('monitor.online.loginTime'), dataIndex: 'loginTime', width: 180 },
  { title: t('common.action.operation'), key: 'action', width: 120, fixed: 'right' }
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
  message.success(t('monitor.online.kicked', { name: record.username }))
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
