<template>
  <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" :disabled="readonly" class="basic-form">
    <a-row :gutter="16">
      <a-col :xs="24" :md="12">
        <a-form-item label="角色名称" name="name">
          <a-input v-model:value="form.name" :maxlength="64" />
        </a-form-item>
      </a-col>
      <a-col :xs="24" :md="12">
        <a-form-item label="角色编码" name="code">
          <a-input v-model:value="form.code" :disabled="isAdminRole" :maxlength="64" />
        </a-form-item>
      </a-col>
      <a-col :xs="24" :md="12">
        <a-form-item label="排序" name="sort">
          <a-input-number v-model:value="form.sort" :min="0" style="width: 100%" />
        </a-form-item>
      </a-col>
      <a-col :xs="24" :md="12">
        <a-form-item label="状态" name="status">
          <a-radio-group
            v-model:value="form.status"
            :options="toOptions(dict.sys_common_status, true)"
            :disabled="isAdminRole"
          />
        </a-form-item>
      </a-col>
      <a-col :span="24">
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="form.remark" :rows="3" :maxlength="255" show-count />
        </a-form-item>
      </a-col>
    </a-row>
  </a-form>
  <PanelActions v-if="!readonly" :dirty="dirty" :saving="saving" @save="save" @reset="reset" />
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import PanelActions from './PanelActions.vue'
import { updateRole } from '@/api/system/role'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { ADMIN_ROLE, BASIC_FIELDS, ROLE_CODE_RULES, pick } from './role'

/** 角色基本信息 */
const props = defineProps({
  detail: { type: Object, required: true },
  readonly: Boolean
})
const emit = defineEmits(['saved', 'update:dirty'])
const dict = useDict('sys_common_status')

const formRef = ref()
const form = ref({})
const saving = ref(false)
const isAdminRole = computed(() => props.detail.code === ADMIN_ROLE)
const rules = {
  name: [{ required: true, message: '请输入角色名称' }],
  code: ROLE_CODE_RULES
}

const reset = () => (form.value = pick(props.detail, BASIC_FIELDS))
watch(() => props.detail, reset, { immediate: true })

const dirty = computed(() => JSON.stringify(form.value) !== JSON.stringify(pick(props.detail, BASIC_FIELDS)))
watch(dirty, (v) => emit('update:dirty', v), { immediate: true })

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    // 不传 menuIds，后端保持菜单权限不变
    await updateRole(props.detail.id, { ...form.value })
    message.success('基本信息已保存')
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.basic-form {
  max-width: 760px;
}
</style>
