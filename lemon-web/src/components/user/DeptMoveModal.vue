<template>
  <a-modal
    :open="open"
    :title="`调整部门（${userIds.length} 人）`"
    :confirm-loading="saving"
    :ok-button-props="{ disabled: deptId == null }"
    destroy-on-close
    @ok="submit"
    @cancel="emit('update:open', false)"
  >
    <a-form layout="vertical">
      <a-form-item label="目标部门" required>
        <a-tree-select
          v-model:value="deptId"
          :tree-data="options"
          :field-names="{ label: 'name', value: 'id', children: 'children' }"
          placeholder="请选择部门"
          tree-default-expand-all
          show-search
          tree-node-filter-prop="name"
        />
      </a-form-item>
    </a-form>
    <a-alert type="info" show-icon message="调整后，用户的「本部门」类数据权限会按新部门立即生效。" />
  </a-modal>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { changeUserDept } from '@/api/system/user'
import { mapTree } from '@/utils/tree'

/** 批量调整用户部门 */
const props = defineProps({
  open: Boolean,
  userIds: { type: Array, default: () => [] },
  deptTree: { type: Array, default: () => [] },
  /** 默认选中的部门 */
  defaultDeptId: Number
})
const emit = defineEmits(['update:open', 'success'])

const deptId = ref()
const saving = ref(false)
// 停用的部门不能选
const options = computed(() => mapTree(props.deptTree, (n) => ({ id: n.id, name: n.name, disabled: n.status === 0 })))

watch(
  () => props.open,
  (open) => open && (deptId.value = props.defaultDeptId)
)

async function submit() {
  saving.value = true
  try {
    await changeUserDept(props.userIds, deptId.value)
    message.success('部门已调整')
    emit('update:open', false)
    emit('success', deptId.value)
  } finally {
    saving.value = false
  }
}
</script>
