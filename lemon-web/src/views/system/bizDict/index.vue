<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-alert type="info" show-icon class="tip" :message="$t('system.bizDict.tip')" />
      <div class="toolbar">
        <a-input-search v-model:value="query.keyword" :placeholder="$t('system.bizDict.search')" allow-clear class="search" />
      </div>
      <a-table row-key="id" size="middle" :columns="visibleColumns" :data-source="list" :loading="loading" :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }" :custom-row="(r) => ({ onClick: () => openData(r) })" class="clickable"
        @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a>{{ typeName(record) }}</a>
          </template>
          <template v-else-if="column.key === 'type'">
            <a-typography-text code>{{ record.type }}</a-typography-text>
          </template>
          <template v-else-if="column.key === 'action'">
            <!-- 整行可点击，按钮点击不再冒泡到行 -->
            <span @click.stop>
              <TableActions :actions="[{ type: 'list', label: $t('system.bizDict.maintain'), onClick: () => openData(record) }]" />
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <DataDrawer v-model:open="dataOpen" :dict-type="current" biz />
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { pageBizDictTypes } from '@/api/system/dict'
import { currentLocale, i18n, t, te } from '@/locales'
import DataDrawer from '../dict/DataDrawer.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'

/**
 * 业务字典：当前租户维护自己的取值（新增、改名、停用）；系统字典由平台维护，这里不显示。
 * 字典类型分页查询（issue #135），可按类型编码排序
 */

/** 字典名称按界面语言翻译（locales/<语言>/system.js 的 bizDict.types），没有译文时用数据库中的名称 */
const typeName = (r) => (te(`system.bizDict.types.${r.type}`) ? t(`system.bizDict.types.${r.type}`) : r.name)
/** 名称译文包含关键字的类型编码：数据库中只有中文名称，英文界面按英文名称搜索时由后端按编码匹配 */
function translatedMatches(kw) {
  const names = i18n.global.getLocaleMessage(currentLocale())?.system?.bizDict?.types || {}
  return Object.entries(names)
    .filter(([, name]) => typeof name === 'string' && name.toLowerCase().includes(kw))
    .map(([type]) => type)
}

const query = reactive({ keyword: undefined })
const { list, loading, pagination, load, search, onTableChange, sorter } = useTable(pageBizDictTypes, query, {
  params: ({ keyword }) => {
    const kw = keyword?.trim()
    if (!kw) return {}
    const types = translatedMatches(kw.toLowerCase())
    return types.length ? { keyword: kw, types: types.join(',') } : { keyword: kw }
  }
})

const columns = [
  { title: t('system.dict.name'), key: 'name', width: 180 },
  { title: t('system.dict.type'), key: 'type', width: 220, sortable: true },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  actionColumn(1, [t('system.bizDict.maintain')])
]
const { visibleColumns, scrollX } = useColumns(columns, null, { sorter })
onMounted(load)
// 输入时自动查询（与旧版的即时筛选一致），连续输入只查询一次
let searchTimer
watch(() => query.keyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(search, 300)
})
onBeforeUnmount(() => clearTimeout(searchTimer))

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
