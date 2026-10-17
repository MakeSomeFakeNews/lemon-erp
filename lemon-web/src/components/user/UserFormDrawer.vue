<template>
  <FormDrawer :drawer="drawer" :create-title="$t('system.user.addUser')" :edit-title="$t('components.user.editUser')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('login.username')" name="username">
            <a-input v-model:value="form.username" :placeholder="$t('components.user.usernamePlaceholder')" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('profile.nickname')" name="nickname">
            <a-input v-model:value="form.nickname" :placeholder="$t('print.template.alias')" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('components.user.dept')" name="deptId">
            <a-tree-select
              v-model:value="form.deptId"
              :tree-data="deptOptions"
              :field-names="{ label: 'name', value: 'id', children: 'children' }"
              :placeholder="$t('common.placeholder.selectName', { name: $t('common.field.dept') })"
              tree-default-expand-all
              show-search
              tree-node-filter-prop="name"
              allow-clear
            />
          </a-form-item>
        </a-col>
        <a-col v-if="!form.id" :xs="24" :sm="12">
          <a-form-item :label="$t('login.password')" name="password">
            <a-input-group compact class="password-group">
              <a-input-password v-model:value="form.password" :placeholder="passwordHint()" autocomplete="new-password" />
              <a-tooltip :title="$t('system.user.generatePassword')">
                <a-button @click="fillPassword">
                  <template #icon><ThunderboltOutlined /></template>
                </a-button>
              </a-tooltip>
            </a-input-group>
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('common.field.phone')" name="phone">
            <a-input v-model:value="form.phone" :maxlength="32" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('common.field.email')" name="email">
            <a-input v-model:value="form.email" :maxlength="128" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('system.role.role')" name="roleIds">
            <a-select
              v-model:value="form.roleIds"
              mode="multiple"
              :options="roleOptionList"
              :placeholder="$t('common.placeholder.selectName', { name: $t('system.role.role') })"
              option-filter-prop="label"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('common.field.status')" name="status">
            <a-radio-group
              v-model:value="form.status"
              :options="toOptions(dict.sys_common_status, true)"
              :disabled="form.id === SUPER_ADMIN_ID"
            />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-textarea v-model:value="form.remark" :rows="3" :maxlength="255" show-count />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ThunderboltOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import { addUser, getUser, updateUser } from '@/api/system/user'
import { roleOptions } from '@/api/system/role'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useUserStore } from '@/store/user'
import { ADMIN_ROLE, SUPER_ADMIN_ID } from '@/utils/constants'
import { toOptions } from '@/utils/dict'
import { generateAndCopyPassword, passwordHint, passwordRules } from '@/utils/password'
import { mapTree } from '@/utils/tree'

/**
 * 新增 / 编辑用户抽屉，用户管理、部门管理共用。
 * 通过 ref 调用 openCreate(defaults) / openEdit(record)，保存成功后触发 success
 */
const props = defineProps({
  deptTree: { type: Array, default: () => [] }
})
const emit = defineEmits(['success'])

const { t } = useI18n()
const userStore = useUserStore()
const dict = useDict('sys_common_status')

// 停用的部门不能选
const deptOptions = computed(() => mapTree(props.deptTree, (n) => ({ id: n.id, name: n.name, disabled: n.status === 0 })))

// 非超级管理员不能分配超级管理员角色；每次打开时刷新，保证与角色管理一致
const roleOptionList = ref([])
async function loadRoles() {
  roleOptionList.value = (await roleOptions()).map((r) => ({
    label: r.name,
    value: r.id,
    disabled: r.code === ADMIN_ROLE && !userStore.isAdmin
  }))
}

const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    username: '',
    password: '',
    nickname: '',
    email: '',
    phone: '',
    deptId: undefined,
    roleIds: [],
    status: 1,
    remark: ''
  }),
  beforeOpen: loadRoles,
  fetch: (record) => getUser(record.id),
  create: addUser,
  update: updateUser,
  toPayload: ({ id, createTime, updateTime, avatar, deptName, ...data }) => data,
  onSuccess: () => emit('success')
})
const { form, formRef } = drawer

const rules = {
  username: [{ required: true, message: t('login.inputUsername') }],
  deptId: [{ required: true, message: t('components.user.selectDept') }],
  password: passwordRules(),
  email: [{ type: 'email', message: t('common.rule.email') }]
}

async function fillPassword() {
  form.value.password = await generateAndCopyPassword()
}

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.password-group {
  display: flex;
}
.password-group :deep(.ant-input-affix-wrapper) {
  flex: 1;
}
</style>
