<template>
  <a-alert v-if="isAdminRole" type="info" show-icon :message="$t('system.role.adminAllData')" />
  <template v-else>
    <a-radio-group v-model:value="dataScope" :disabled="readonly" class="scope-options">
      <a-radio v-for="o in options" :key="o.value" :value="o.value" class="scope-option">
        <div class="scope-label">{{ o.label }}</div>
        <div class="scope-desc">{{ o.desc }}</div>
      </a-radio>
    </a-radio-group>
    <div v-if="dataScope === DATA_SCOPE_CUSTOM" class="scope-depts">
      <div class="scope-depts-title">{{ $t('system.role.accessibleDepts') }}</div>
      <CheckTree v-model="deptIds" :tree="deptTree" :disabled="readonly" :default-expand-level="2" height="320px" />
    </div>
    <PanelActions v-if="!readonly" :dirty="dirty" :saving="saving" @save="save" @reset="reset" />
  </template>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import CheckTree from '@/components/tree/CheckTree.vue'
import PanelActions from './PanelActions.vue'
import { assignRoleDataScope } from '@/api/system/role'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { ADMIN_ROLE, DATA_SCOPE_CUSTOM, DATA_SCOPE_DESC, sameIds } from './role'

/** 角色数据权限 */
const props = defineProps({
  detail: { type: Object, required: true },
  deptTree: { type: Array, default: () => [] },
  readonly: Boolean
})
const emit = defineEmits(['saved', 'update:dirty'])
const { t } = useI18n()
const dict = useDict('sys_data_scope')

const dataScope = ref()
const deptIds = ref([])
const saving = ref(false)
const isAdminRole = computed(() => props.detail.code === ADMIN_ROLE)
const options = computed(() =>
  toOptions(dict.sys_data_scope, true).map((o) => ({ ...o, desc: DATA_SCOPE_DESC[o.value] }))
)

function reset() {
  dataScope.value = props.detail.dataScope
  deptIds.value = [...(props.detail.deptIds || [])]
}
watch(() => props.detail, reset, { immediate: true })

const dirty = computed(
  () =>
    !isAdminRole.value &&
    (dataScope.value !== props.detail.dataScope ||
      (dataScope.value === DATA_SCOPE_CUSTOM && !sameIds(deptIds.value, props.detail.deptIds || [])))
)
watch(dirty, (v) => emit('update:dirty', v), { immediate: true })

async function save() {
  if (dataScope.value === DATA_SCOPE_CUSTOM && !deptIds.value.length) {
    message.warning(t('system.role.selectOneDept'))
    return
  }
  saving.value = true
  try {
    await assignRoleDataScope(props.detail.id, {
      dataScope: dataScope.value,
      deptIds: dataScope.value === DATA_SCOPE_CUSTOM ? deptIds.value : []
    })
    message.success(t('system.role.dataScopeSaved'))
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.scope-options {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 8px;
  width: 100%;
}
.scope-option {
  display: flex;
  align-items: flex-start;
  margin: 0;
  padding: 10px 12px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  transition: border-color 0.2s, background 0.2s;
}
.scope-option:hover {
  border-color: var(--app-primary);
}
.scope-option.ant-radio-wrapper-checked {
  border-color: var(--app-primary);
  background: var(--app-primary-bg);
}
.scope-label {
  font-weight: 500;
}
.scope-desc {
  margin-top: 2px;
  font-size: 12px;
  color: var(--app-text-3);
}
.scope-depts {
  margin-top: 16px;
}
.scope-depts-title {
  margin-bottom: 8px;
  font-weight: 500;
}
</style>
