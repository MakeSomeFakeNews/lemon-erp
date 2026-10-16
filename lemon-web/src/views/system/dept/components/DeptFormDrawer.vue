<template>
  <FormDrawer :drawer="drawer" create-title="新增部门" edit-title="编辑部门">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="24">
          <a-form-item label="上级部门" name="parentId">
            <a-tree-select
              v-model:value="form.parentId"
              :tree-data="parentOptions"
              :field-names="{ label: 'name', value: 'id', children: 'children' }"
              tree-default-expand-all
              show-search
              tree-node-filter-prop="name"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item label="部门名称" name="name">
            <a-input v-model:value="form.name" :maxlength="64" placeholder="如：研发部" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item label="排序" name="sort">
            <a-input-number v-model:value="form.sort" :min="0" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item label="负责人" name="leaderUserId" extra="审批流程中的「部门主管」即部门负责人">
            <UserSelect v-model:value="form.leaderUserId" :label="form.leaderName" placeholder="搜索并选择负责人" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item label="联系电话" name="phone">
            <a-input v-model:value="form.phone" :maxlength="32" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item label="邮箱" name="email">
            <a-input v-model:value="form.email" :maxlength="128" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item label="状态" name="status">
            <a-radio-group v-model:value="form.status" :options="toOptions(dict.sys_common_status, true)" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed } from 'vue'
import FormDrawer from '@/components/FormDrawer.vue'
import UserSelect from '@/components/user/UserSelect.vue'
import { addDept, updateDept } from '@/api/system/dept'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { STATUS_ENABLED } from '@/utils/constants'
import { toOptions } from '@/utils/dict'
import { mapTree } from '@/utils/tree'

/**
 * 新增 / 编辑部门抽屉。通过 ref 调用：
 * - openCreate(parent)：在 parent（默认第一个根部门）下新增，排在最后
 * - openEdit(dept)
 */
const props = defineProps({
  deptTree: { type: Array, default: () => [] }
})
const emit = defineEmits(['success'])
const dict = useDict('sys_common_status')

const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    parentId: 0,
    name: '',
    sort: 0,
    leaderUserId: null,
    phone: '',
    email: '',
    status: STATUS_ENABLED
  }),
  // leaderName 只用于回显
  toPayload: ({ id, leaderName, createTime, updateTime, children, ...rest }) => rest,
  create: addDept,
  update: updateDept,
  onSuccess: () => emit('success')
})
const { form, formRef } = drawer
const rules = {
  parentId: [{ required: true, message: '请选择上级部门' }],
  name: [{ required: true, message: '请输入部门名称' }],
  email: [{ type: 'email', message: '邮箱格式不正确' }]
}

// 上级部门：编辑时不能选择自己及下级部门
const parentOptions = computed(() => [
  {
    id: 0,
    name: '顶级部门',
    children: mapTree(props.deptTree, (n, parent) => ({
      id: n.id,
      name: n.name,
      disabled: parent?.disabled || n.id === form.value.id
    }))
  }
])

function openCreate(parent) {
  const target = parent ?? props.deptTree[0]
  return drawer.openCreate({ parentId: target?.id ?? 0, sort: target?.children?.length ?? 0 })
}

defineExpose({ openCreate, openEdit: drawer.openEdit })
</script>
