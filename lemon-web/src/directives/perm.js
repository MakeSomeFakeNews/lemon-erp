import { useUserStore } from '@/store/user'

/**
 * 按钮级权限：v-perm="'sys:user:add'" 或 v-perm="['a', 'b']"（满足其一即可）
 */
export default {
  mounted(el, binding) {
    if (!useUserStore().hasPerm(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  }
}
