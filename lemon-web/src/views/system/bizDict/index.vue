<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-alert type="info" show-icon class="tip" :message="$t('system.bizDict.tip')" />
      <div class="toolbar">
        <a-input-search v-model:value="keyword" :placeholder="$t('system.bizDict.search')" allow-clear class="search" />
      </div>
      <a-table row-key="id" size="middle" :columns="columns" :data-source="filtered" :loading="loading" :pagination="false"
        :scroll="{ x: 760, y: '100%' }" :custom-row="(r) => ({ onClick: () => openData(r) })" class="clickable">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a>{{ typeName(record) }}</a>
          </template>
          <template v-else-if="column.key === 'type'">
            <a-typography-text code>{{ record.type }}</a-typography-text>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click.stop="openData(record)">
              <template #icon><UnorderedListOutlined /></template>{{ $t('system.bizDict.maintain') }}
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <DataDrawer v-model:open="dataOpen" :dict-type="current" biz />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { UnorderedListOutlined } from '@ant-design/icons-vue'
import { listBizDictTypes } from '@/api/system/dict'
import { t, te } from '@/locales'
import DataDrawer from '../dict/DataDrawer.vue'

/** 业务字典：当前租户维护自己的取值（新增、改名、停用）；系统字典由平台维护，这里不显示 */
const loading = ref(false)
const types = ref([])
const keyword = ref('')

/** 字典名称按界面语言翻译（locales/<语言>/system.js 的 dictTypes），没有译文时用数据库中的名称 */
const typeName = (r) => (te(`system.bizDict.types.${r.type}`) ? t(`system.bizDict.types.${r.type}`) : r.name)
const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return kw ? types.value.filter((r) => [typeName(r), r.type, r.remark].some((v) => v && v.toLowerCase().includes(kw))) : types.value
})

const columns = [
  { title: t('system.dict.name'), key: 'name', width: 180 },
  { title: t('system.dict.type'), key: 'type', width: 220 },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 120, fixed: 'right' }
]

async function load() {
  loading.value = true
  try {
    types.value = await listBizDictTypes()
  } finally {
    loading.value = false
  }
}
onMounted(load)

const dataOpen = ref(false)
const current = ref(null)
function openData(record) {
  current.value = { ...record, name: typeName(record) }
  dataOpen.value = true
}
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.toolbar {
  margin-bottom: 12px;
}
.search {
  width: 260px;
}
.clickable :deep(.ant-table-row) {
  cursor: pointer;
}
</style>
