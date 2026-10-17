<template>
  <a-modal
    :open="open"
    :title="$t('components.user.moveTitle', { n: userIds.length })"
    :confirm-loading="saving"
    :ok-button-props="{ disabled: deptId == null }"
    destroy-on-close
    @ok="submit"
    @cancel="emit('update:open', false)"
  >
    <a-form layout="vertical">
      <a-form-item :label="$t('components.user.targetDept')" required>
        <a-tree-select
          v-model:value="deptId"
          :tree-data="options"
          :field-names="{ label: 'name', value: 'id', children: 'children' }"
          :placeholder="$t('common.placeholder.selectName', { name: $t('common.field.dept') })"
          tree-default-expand-all
          show-search
          tree-node-filter-prop="name"
        />
      </a-form-item>
    </a-form>
    <a-alert type="info" show-icon :message="$t('components.user.moveTip')" />
  </a-modal>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
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
const { t } = useI18n()

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
    message.success(t('components.user.moved'))
    emit('update:open', false)
    emit('success', deptId.value)
  } finally {
    saving.value = false
  }
}
</script>
