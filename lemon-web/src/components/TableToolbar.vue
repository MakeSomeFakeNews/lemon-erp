<template>
  <div class="table-toolbar">
    <div class="toolbar-left">
      <slot />
    </div>
    <div class="toolbar-right">
      <slot name="extra" />
      <a-tooltip v-if="exporter" title="导出 Excel（当前查询条件）">
        <a-button type="text" :loading="exporting" @click="doExport">
          <template #icon><DownloadOutlined /></template>
        </a-button>
      </a-tooltip>
      <a-tooltip v-if="showSearch !== undefined" :title="showSearch ? '隐藏搜索' : '显示搜索'">
        <a-button type="text" @click="emit('update:showSearch', !showSearch)">
          <template #icon><SearchOutlined /></template>
        </a-button>
      </a-tooltip>
      <a-tooltip title="刷新">
        <a-button type="text" @click="emit('refresh')">
          <template #icon><ReloadOutlined /></template>
        </a-button>
      </a-tooltip>
      <a-dropdown v-if="size" trigger="click">
        <a-tooltip title="密度">
          <a-button type="text">
            <template #icon><ColumnHeightOutlined /></template>
          </a-button>
        </a-tooltip>
        <template #overlay>
          <a-menu :selected-keys="[size]" @click="({ key }) => emit('update:size', key)">
            <a-menu-item key="large">宽松</a-menu-item>
            <a-menu-item key="middle">默认</a-menu-item>
            <a-menu-item key="small">紧凑</a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
      <a-popover v-if="columns" trigger="click" placement="bottomRight">
        <template #title>
          <div class="column-title">
            <a-checkbox :checked="allChecked" :indeterminate="indeterminate" @change="toggleAll">列展示</a-checkbox>
            <a @click="reset">重置</a>
          </div>
        </template>
        <template #content>
          <a-checkbox-group :value="checked" class="column-list" @change="(v) => emit('update:checked', v)">
            <a-checkbox v-for="c in columns" :key="columnKey(c)" :value="columnKey(c)">{{ c.title }}</a-checkbox>
          </a-checkbox-group>
        </template>
        <a-tooltip title="列设置">
          <a-button type="text">
            <template #icon><SettingOutlined /></template>
          </a-button>
        </a-tooltip>
      </a-popover>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ColumnHeightOutlined, DownloadOutlined, ReloadOutlined, SearchOutlined, SettingOutlined } from '@ant-design/icons-vue'
import { columnKey } from '@/composables/useColumns'

/**
 * 表格工具栏：默认插槽放操作按钮；右侧为搜索显隐、刷新、密度、列设置。
 * 不传 size / columns / showSearch 时对应功能不显示；传 exporter（useTable 的 exportAll）时显示导出，导出当前显示的列
 */
const props = defineProps({
  size: String,
  columns: Array,
  checked: Array,
  showSearch: { type: Boolean, default: undefined },
  exporter: Function
})
const emit = defineEmits(['refresh', 'update:size', 'update:checked', 'update:showSearch'])

const allKeys = computed(() => (props.columns || []).map(columnKey))
const allChecked = computed(() => props.checked?.length === allKeys.value.length)
const indeterminate = computed(() => !!props.checked?.length && !allChecked.value)

const toggleAll = (e) => emit('update:checked', e.target.checked ? allKeys.value : [])
const reset = () => emit('update:checked', allKeys.value)

const exporting = ref(false)
async function doExport() {
  exporting.value = true
  try {
    const cols = (props.columns || []).filter((c) => !props.checked || props.checked.includes(columnKey(c)))
    await props.exporter(cols)
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.table-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 16px;
}
.toolbar-left {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-left: auto;
}
.column-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-width: 160px;
}
.column-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
</style>
