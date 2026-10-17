<template>
  <a-alert v-if="isAdminRole" type="info" show-icon :message="$t('system.role.adminAllMenu')" />
  <template v-else>
    <a-alert
      type="info"
      show-icon
      class="panel-tip"
      :message="$t('system.role.menuTip')"
    />
    <CheckTree v-model="checked" :tree="menuTree" :disabled="readonly" check-ancestors height="calc(100vh - 480px)">
      <template #extra="{ type }">
        <DictTag type="sys_menu_type" :value="type" class="tree-tag" />
      </template>
    </CheckTree>
    <PanelActions v-if="!readonly" :dirty="dirty" :saving="saving" @save="save" @reset="reset" />
  </template>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import CheckTree from '@/components/tree/CheckTree.vue'
import PanelActions from './PanelActions.vue'
import { updateRole } from '@/api/system/role'
import { useDict } from '@/composables/useDict'
import { ADMIN_ROLE, BASIC_FIELDS, pick, sameIds } from './role'

/** 角色菜单权限 */
const props = defineProps({
  detail: { type: Object, required: true },
  menuTree: { type: Array, default: () => [] },
  readonly: Boolean
})
const emit = defineEmits(['saved', 'update:dirty'])
const { t } = useI18n()
useDict('sys_menu_type')

const checked = ref([])
const saving = ref(false)
const isAdminRole = computed(() => props.detail.code === ADMIN_ROLE)

const reset = () => (checked.value = [...(props.detail.menuIds || [])])
watch(() => props.detail, reset, { immediate: true })

const dirty = computed(() => !isAdminRole.value && !sameIds(checked.value, props.detail.menuIds || []))
watch(dirty, (v) => emit('update:dirty', v), { immediate: true })

async function save() {
  saving.value = true
  try {
    await updateRole(props.detail.id, { ...pick(props.detail, BASIC_FIELDS), menuIds: [...checked.value] })
    message.success(t('system.role.menuSaved'))
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.panel-tip {
  margin-bottom: 12px;
}
.tree-tag {
  margin-left: 6px;
  font-size: 11px;
  line-height: 16px;
  padding: 0 4px;
}
</style>
