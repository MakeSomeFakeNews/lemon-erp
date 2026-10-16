<template>
  <component
    :is="page ? SubPage : Drawer"
    :open="state.open"
    :title="state.isEdit ? editTitle : createTitle"
    :width="width"
    destroy-on-close
    @close="state.close"
  >
    <a-spin :spinning="state.loading">
      <slot />
    </a-spin>
    <template #footer>
      <div class="drawer-footer">
        <span class="shortcut-hint">{{ shortcut }} + Enter 保存</span>
        <a-button @click="state.close">取消</a-button>
        <a-button type="primary" :loading="state.saving" :disabled="state.loading" @click="state.submit">保存</a-button>
      </div>
    </template>
  </component>
</template>

<script setup>
import { reactive } from 'vue'
import { Drawer } from 'ant-design-vue'
import SubPage from './SubPage.vue'

/**
 * 新增 / 编辑抽屉，drawer 传入 useFormDrawer() 的返回值；表单内容放在默认插槽。
 * 复杂表单（明细行多、字段多）加 page，以子页的形式占满内容区显示
 */
const props = defineProps({
  drawer: { type: Object, required: true },
  createTitle: { type: String, default: '新增' },
  editTitle: { type: String, default: '编辑' },
  width: { type: String, default: 'min(640px, 100vw)' },
  page: Boolean
})

// 解包 useFormDrawer 返回的 ref，模板中直接使用
const state = reactive(props.drawer)
const shortcut = /Mac|iPhone|iPad/.test(navigator.platform) ? '⌘' : 'Ctrl'
</script>

<style scoped>
.shortcut-hint {
  margin-right: auto;
  align-self: center;
  color: var(--app-text-3);
  font-size: 12px;
}
@media (max-width: 767px) {
  .shortcut-hint {
    display: none;
  }
}
</style>
