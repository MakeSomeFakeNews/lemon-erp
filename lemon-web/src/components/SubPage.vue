<template>
  <Teleport v-if="open" to="#subpage-host">
    <section class="sub-page" :style="{ zIndex }">
      <header class="sub-page-header">
        <a-button type="text" class="back-btn" @click="close">
          <template #icon><ArrowLeftOutlined /></template>
          返回
        </a-button>
        <a-divider type="vertical" />
        <div class="sub-page-title">
          <slot name="title">{{ title }}</slot>
        </div>
        <div v-if="$slots.extra" class="sub-page-extra">
          <slot name="extra" />
        </div>
      </header>
      <div ref="bodyRef" class="sub-page-body">
        <div class="sub-page-card">
          <slot />
        </div>
      </div>
      <footer v-if="$slots.footer" class="sub-page-footer">
        <slot name="footer" />
      </footer>
    </section>
  </Teleport>
</template>

<script>
// 多个子页叠加时，后打开的在上面
let seq = 10
</script>

<script setup>
import { ref, watch } from 'vue'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'

/**
 * 子页：复杂的编辑、查看页面占满内容区显示（覆盖在列表之上），返回后列表的筛选、分页、滚动位置不变。
 * 用法与 a-drawer 一致（open / title / @close，插槽 title、extra、footer），width 等抽屉属性会被忽略。
 */
defineOptions({ inheritAttrs: false })
const props = defineProps({
  open: Boolean,
  title: { type: String, default: '' }
})
const emit = defineEmits(['close', 'update:open'])

const zIndex = ref(seq)
const bodyRef = ref()
watch(
  () => props.open,
  (v) => {
    if (v) zIndex.value = ++seq
  },
  { immediate: true }
)

function close() {
  emit('close')
  emit('update:open', false)
}
</script>

<style scoped>
.sub-page {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  background: var(--app-bg);
  pointer-events: auto;
  animation: sub-page-in 0.2s ease-out;
}
@keyframes sub-page-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
}
.sub-page-header {
  flex: none;
  display: flex;
  align-items: center;
  gap: 4px;
  min-height: 52px;
  padding: 8px 16px 8px 8px;
  background: var(--app-card);
  border-bottom: 1px solid var(--app-border);
}
.back-btn {
  color: var(--app-text-2);
}
.sub-page-title {
  flex: 1;
  min-width: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub-page-extra {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.sub-page-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 16px;
}
.sub-page-card {
  min-height: 100%;
  padding: 20px 24px;
  background: var(--app-card);
  border-radius: 8px;
  box-shadow: var(--app-shadow);
}
.sub-page-footer {
  flex: none;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 10px 24px;
  background: var(--app-card);
  border-top: 1px solid var(--app-border);
}
.sub-page-footer > :slotted(.drawer-footer) {
  flex: 1;
}
@media (max-width: 575px) {
  .sub-page-body {
    padding: 12px;
  }
  .sub-page-card {
    padding: 16px 12px;
  }
  .sub-page-footer {
    padding: 10px 12px;
  }
}
</style>
