<template>
  <FormDrawer :drawer="drawer" :create-title="$t('system.dept.addDept')" :edit-title="$t('system.dept.editDept')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="24">
          <a-form-item :label="$t('system.dept.parent')" name="parentId">
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
          <a-form-item :label="$t('system.dept.name')" name="name">
            <a-input v-model:value="form.name" :maxlength="64" :placeholder="$t('system.dept.namePlaceholder')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('common.field.sort')" name="sort">
            <a-input-number v-model:value="form.sort" :min="0" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('system.dept.leader')" name="leaderUserId" :extra="$t('system.dept.leaderTip')">
            <UserSelect v-model:value="form.leaderUserId" :label="form.leaderName" :placeholder="$t('system.dept.leaderPlaceholder')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('system.dept.contactPhone')" name="phone">
            <a-input v-model:value="form.phone" :maxlength="32" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('common.field.email')" name="email">
            <a-input v-model:value="form.email" :maxlength="128" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('common.field.status')" name="status">
            <a-radio-group v-model:value="form.status" :options="toOptions(dict.sys_common_status, true)" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
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
const { t } = useI18n()
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
  parentId: [{ required: true, message: t('system.dept.selectParent') }],
  name: [{ required: true, message: t('system.dept.inputName') }],
  email: [{ type: 'email', message: t('common.rule.email') }]
}

// 上级部门：编辑时不能选择自己及下级部门
const parentOptions = computed(() => [
  {
    id: 0,
    name: t('system.dept.root'),
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
