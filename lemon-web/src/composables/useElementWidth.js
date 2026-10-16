import { onBeforeUnmount, ref, watch } from 'vue'

/**
 * 元素的当前宽度（ResizeObserver），用于按容器宽度而不是窗口宽度排版（侧边栏展开收起时也能正确响应）。
 * 元素可以晚于组件挂载才出现（如 v-if 中的元素）
 *
 * @param {import('vue').Ref<HTMLElement>} target 模板 ref
 */
export function useElementWidth(target) {
  const width = ref(0)
  const observer = new ResizeObserver(([entry]) => (width.value = entry.contentRect.width))
  watch(
    target,
    (el, old) => {
      if (old) observer.unobserve(old)
      if (el) observer.observe(el)
    },
    { immediate: true, flush: 'post' }
  )
  onBeforeUnmount(() => observer.disconnect())
  return width
}
