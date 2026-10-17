<template>
  <a-popover v-model:open="open" trigger="click" placement="bottomLeft" overlay-class-name="icon-picker-popover">
    <template #content>
      <!-- 弹层内的搜索框不参与外层表单项的值收集 -->
      <a-form-item-rest>
        <a-input v-model:value="keyword" :placeholder="$t('components.icon.search')" allow-clear class="icon-search">
          <template #prefix><MenuIcon name="SearchOutlined" /></template>
        </a-input>
      </a-form-item-rest>
      <a-spin :spinning="!iconModule">
        <div class="icon-grid">
          <div
            v-for="name in filtered"
            :key="name"
            :class="['icon-cell', { active: name === modelValue }]"
            :title="name"
            @click="select(name)"
          >
            <component :is="iconModule[name]" />
          </div>
          <a-empty v-if="iconModule && !filtered.length" :image="simpleImage" :description="$t('components.icon.noMatch')" />
        </div>
      </a-spin>
    </template>
    <a-input :value="modelValue" :placeholder="$t('components.icon.select')" readonly allow-clear class="icon-trigger" @change="clear">
      <template #prefix>
        <span class="icon-preview"><MenuIcon v-if="modelValue" :name="modelValue" /></span>
      </template>
    </a-input>
  </a-popover>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { Empty } from 'ant-design-vue'
import MenuIcon from './MenuIcon.vue'
import { iconModule, loadIcons, outlinedIconNames } from '@/utils/icons'

const props = defineProps({ modelValue: String })
const emit = defineEmits(['update:modelValue'])

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const open = ref(false)
const keyword = ref('')

watch(open, (v) => v && loadIcons())

const names = computed(() => (iconModule.value ? outlinedIconNames(iconModule.value) : []))
const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  return k ? names.value.filter((n) => n.toLowerCase().includes(k)) : names.value
})

function select(name) {
  emit('update:modelValue', name)
  open.value = false
}

function clear(e) {
  // allow-clear 点击清空时触发
  if (!e.target.value) emit('update:modelValue', '')
}
</script>

<style scoped>
.icon-search {
  margin-bottom: 8px;
}
.icon-grid {
  width: min(420px, 80vw);
  height: 280px;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(40px, 1fr));
  gap: 4px;
  align-content: start;
}
.icon-cell {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  border-radius: 6px;
  cursor: pointer;
  color: var(--app-text-2);
  transition: all 0.15s;
}
.icon-cell:hover {
  background: var(--app-hover);
  color: var(--app-primary);
}
.icon-cell.active {
  background: var(--app-primary-bg);
  color: var(--app-primary);
}
.icon-preview {
  display: inline-flex;
  width: 16px;
  justify-content: center;
}
.icon-trigger :deep(input) {
  cursor: pointer;
}
.icon-grid :deep(.ant-empty) {
  grid-column: 1 / -1;
}
</style>
