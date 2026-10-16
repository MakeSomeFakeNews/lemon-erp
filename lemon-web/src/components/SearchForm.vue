<template>
  <a-form :model="model" class="search-form" @finish="emit('search')">
    <a-row :gutter="[16, 16]">
      <a-col v-for="(node, i) in visibleItems" :key="i" v-bind="colProps">
        <component :is="node" />
      </a-col>
      <a-col v-bind="colProps" class="search-actions" :class="{ 'push-right': true }">
        <a-space>
          <a-button type="primary" html-type="submit">
            <template #icon><SearchOutlined /></template>查询
          </a-button>
          <a-button @click="emit('reset')">
            <template #icon><UndoOutlined /></template>重置
          </a-button>
          <a v-if="items.length > collapsedCount" class="toggle" @click="expanded = !expanded">
            {{ expanded ? '收起' : '展开' }}
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
 * 可折叠搜索区：默认插槽中每个 a-form-item 占一格，超过 collapsedCount 个时可展开/收起
 */
const props = defineProps({
  model: { type: Object, required: true },
  collapsedCount: { type: Number, default: 3 }
})
const emit = defineEmits(['search', 'reset'])
const slots = useSlots()
const expanded = ref(false)

const colProps = { xs: 24, sm: 12, lg: 8, xxl: 6 }

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
