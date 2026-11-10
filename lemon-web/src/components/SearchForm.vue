<template>
  <a-form :model="model" class="search-form" :class="{ 'search-inline': !expanded }" @finish="emit('search')">
    <a-row :gutter="[16, 16]">
      <a-col v-for="(node, i) in visibleItems" :key="i" v-bind="colProps" :class="{ 'search-wide': isWide(node) }">
        <component :is="node" />
      </a-col>
      <a-col v-bind="colProps" class="search-actions" :class="{ 'push-right': true }">
        <a-space>
          <a-button type="primary" html-type="submit">
            <template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}
          </a-button>
          <a-button @click="emit('reset')">
            <template #icon><UndoOutlined /></template>{{ $t('common.action.reset') }}
          </a-button>
          <a v-if="items.length > collapsedCount" class="toggle" @click="expanded = !expanded">
            {{ expanded ? $t('common.action.collapse') : $t('common.action.expand') }}
            <DownOutlined :class="['toggle-icon', { up: expanded }]" />
          </a>
        </a-space>
      </a-col>
    </a-row>
  </a-form>
</template>

<script setup>
import { Comment, Fragment, computed, ref, useSlots } from 'vue'
import { DownOutlined, SearchOutlined, UndoOutlined } from '@ant-design/icons-vue'

/**
 * 可折叠搜索区：默认插槽中每个 a-form-item 占一格，超过 collapsedCount 个时可展开/收起。
 * 收起（或条件不超过 collapsedCount 个）时，1200 宽及以上条件与「查询」「重置」排在同一行：按钮区按内容宽度，
 * 条件平分剩余宽度（每个最多三分之一行），1440 宽下 3 个条件时按钮不会被挤到第二行；放不下时按钮整体换行。
 * 日期范围等较宽的条件在 a-form-item 上写 data-wide，同一行中按两倍宽度分配。展开后按栅格每行 3 个（1600 宽以上 4 个）
 */
const props = defineProps({
  model: { type: Object, required: true },
  collapsedCount: { type: Number, default: 3 }
})
const emit = defineEmits(['search', 'reset'])
const slots = useSlots()
const expanded = ref(false)

const colProps = { xs: 24, sm: 12, lg: 8, xxl: 6 }
const isWide = (node) => node.props?.['data-wide'] !== undefined

function flatten(nodes) {
  return nodes.flatMap((n) => (n.type === Fragment ? flatten(n.children) : n.type === Comment ? [] : [n]))
}

const items = computed(() => flatten(slots.default?.() || []))
const visibleItems = computed(() => (expanded.value ? items.value : items.value.slice(0, props.collapsedCount)))
</script>

<style scoped>
.search-form :deep(.ant-form-item) {
  margin-bottom: 0;
}
/* 按钮区按内容宽度占据行尾剩余空间，放不下（如左侧有部门树的页面、英文按钮）时整体换到下一行，而不是溢出 */
.search-form .search-actions.ant-col {
  flex: 1 0 auto;
  max-width: 100%;
}
@media (min-width: 1200px) {
  .search-inline .ant-row > .ant-col:not(.search-actions) {
    flex: 1 1 0;
    min-width: 220px;
    max-width: 33.33%;
  }
  .search-inline .ant-row > .ant-col.search-wide {
    flex-grow: 2;
    min-width: 320px;
    max-width: 50%;
  }
  .search-inline .ant-row > .search-actions.ant-col {
    flex: 0 0 auto;
  }
}
.search-actions {
  margin-left: auto;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}
.toggle {
  white-space: nowrap;
}
.toggle-icon {
  font-size: 12px;
  transition: transform 0.2s;
}
.toggle-icon.up {
  transform: rotate(180deg);
}
</style>
