<template>
  <div class="format-bar">
    <div class="format-head">
      <span class="format-title">{{ $t('print.format.title') }}</span>
      <a-tooltip :title="$t('print.format.hint')"><QuestionCircleOutlined class="format-help" /></a-tooltip>
      <span class="spacer" />
      <span v-perm="'sys:printTemplate:edit'" class="format-actions">
        <a-button type="link" size="small" @click="openAdd"><template #icon><PlusOutlined /></template>{{ $t('print.format.add') }}</a-button>
        <template v-if="current">
          <a-divider type="vertical" />
          <a-tooltip :title="$t('print.format.settings')">
            <a-button type="text" size="small" @click="drawer.openEdit(current)"><SettingOutlined /></a-button>
          </a-tooltip>
          <a-tooltip v-if="!current.isDefault" :title="$t('print.format.setDefault')">
            <a-button type="text" size="small" @click="setDefault(current)"><StarOutlined /></a-button>
          </a-tooltip>
          <a-popconfirm v-if="!current.isDefault || formats.length === 1" :title="$t('print.format.deleteConfirm', { name: formatLabel(current.name) })" @confirm="remove(current.id)">
            <a-tooltip :title="$t('common.action.delete')">
              <a-button type="text" size="small" danger><DeleteOutlined /></a-button>
            </a-tooltip>
          </a-popconfirm>
        </template>
      </span>
    </div>
    <div class="format-list">
      <!-- 还没有保存过格式：按默认版式，相当于一份标准格式 -->
      <div v-if="!formats.length" class="format-chip active">
        {{ $t('print.format.standard') }}
        <a-tag color="blue" :bordered="false" class="format-tag">{{ $t('print.format.isDefault') }}</a-tag>
      </div>
      <div
        v-for="f in formats"
        :key="f.id"
        :class="['format-chip', { active: f.id === current?.id, off: f.status !== STATUS_ENABLED }]"
        @click="emit('update:modelValue', f.id)"
      >
        {{ formatLabel(f.name) }}
        <a-tag v-if="f.isDefault" color="blue" :bordered="false" class="format-tag">{{ $t('print.format.isDefault') }}</a-tag>
        <a-tag v-if="f.status !== STATUS_ENABLED" :bordered="false" class="format-tag">{{ $t('print.format.disabled') }}</a-tag>
      </div>
    </div>

    <FormDrawer :drawer="drawer" :create-title="$t('print.format.add')" :edit-title="$t('print.format.settings')" width="min(440px, 100vw)">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('print.format.name')" name="name">
          <a-input v-model:value="form.name" :maxlength="32" />
        </a-form-item>
        <a-form-item v-if="!isEdit" :label="$t('print.format.copyFrom')" :extra="$t('print.format.copyTip')">
          <a-select v-model:value="form.sourceId" :options="sourceOptions" />
        </a-form-item>
        <a-form-item :label="$t('common.field.sort')">
          <a-input-number v-model:value="form.sort" :min="0" :max="9999" :precision="0" class="full" />
        </a-form-item>
        <a-form-item :extra="$t('print.format.defaultTip')">
          <a-space :size="24">
            <a-checkbox v-model:checked="form.isDefault" :disabled="isEdit && editingDefault">{{ $t('print.format.isDefault') }}</a-checkbox>
            <a-checkbox
              :checked="form.status === STATUS_ENABLED"
              :disabled="form.isDefault"
              @update:checked="(v) => (form.status = v ? STATUS_ENABLED : STATUS_DISABLED)"
            >
              {{ $t('print.format.enabled') }}
            </a-checkbox>
          </a-space>
        </a-form-item>
      </a-form>
    </FormDrawer>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, PlusOutlined, QuestionCircleOutlined, SettingOutlined, StarOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import { useAction } from '@/composables/useAction'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { createPrintFormat, deletePrintFormat, updatePrintFormat } from '@/api/system/printTemplate'
import { STATUS_DISABLED, STATUS_ENABLED } from '@/utils/constants'
import { formatLabel } from '@/utils/printTemplates'

/**
 * 打印模板页面中单据的打印格式：切换要编辑的格式，新增（可复制已有格式）、修改名称 / 排序 / 状态、设为默认、删除。
 * 格式的内容（纸张、字段、列等）仍在页面中编辑；变更后触发 changed（参数为要选中的格式 ID），由页面重新加载
 */
const props = defineProps({
  /** 模板标识 */
  typeKey: { type: String, required: true },
  /** 该单据的打印格式（已排序），没有保存过时为空 */
  formats: { type: Array, default: () => [] },
  /** 当前编辑的格式 ID，为空时为默认格式 */
  modelValue: Number
})
const emit = defineEmits(['update:modelValue', 'changed'])
const { t } = useI18n()

const current = computed(() => props.formats.find((f) => f.id === props.modelValue) || props.formats.find((f) => f.isDefault))
/** 复制来源：已有格式，或不复制（默认版式） */
const sourceOptions = computed(() => [
  { value: 0, label: t('print.format.copyNone') },
  ...props.formats.map((f) => ({ value: f.id, label: formatLabel(f.name) }))
])

const nextSort = () => props.formats.reduce((max, f) => Math.max(max, f.sort ?? 0), 0) + 1
const emptyForm = () => ({ id: undefined, name: '', sourceId: 0, sort: 1, isDefault: false, status: STATUS_ENABLED })
let createdId
const drawer = useFormDrawer({
  emptyForm,
  fetch: (f) => ({ id: f.id, name: f.name, sort: f.sort, isDefault: !!f.isDefault, status: f.status }),
  create: async (p) => (createdId = await createPrintFormat(p)),
  update: (id, p) => updatePrintFormat(id, p),
  toPayload: (f) => ({
    templateKey: props.typeKey,
    name: f.name.trim(),
    ...(drawer.isEdit.value ? {} : { sourceId: f.sourceId || undefined }),
    sort: f.sort ?? 0,
    isDefault: f.isDefault,
    status: f.isDefault ? STATUS_ENABLED : f.status
  }),
  successMessage: () => t(drawer.isEdit.value ? 'print.format.updated' : 'print.format.created'),
  onSuccess: () => emit('changed', drawer.isEdit.value ? form.value.id : createdId)
})
const { formRef, form, isEdit } = drawer
/** 编辑的是默认格式：不能取消默认（把其他格式设为默认即可） */
const editingDefault = computed(() => !!props.formats.find((f) => f.id === form.value.id)?.isDefault)
// 设为默认时必须启用
watch(
  () => form.value.isDefault,
  (v) => v && (form.value.status = STATUS_ENABLED)
)

const rules = {
  name: [{ required: true, whitespace: true, message: t('print.format.inputName') }]
}

function openAdd() {
  // 默认复制当前编辑的格式，排在最后
  drawer.openCreate({ sourceId: current.value?.id || 0, sort: nextSort() })
}

const setDefault = useAction(
  (f) => updatePrintFormat(f.id, { name: f.name, sort: f.sort, status: STATUS_ENABLED, isDefault: true }),
  { successKey: 'print.format.defaultSet', reload: (_, f) => emit('changed', f.id) }
)
const remove = useAction(deletePrintFormat, { successKey: 'print.format.deleted', reload: () => emit('changed', undefined) })
</script>

<style scoped>
.format-bar {
  margin-bottom: 4px;
}
.format-head {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 8px 0 6px;
}
.format-title {
  font-weight: 600;
  color: var(--app-text);
}
.format-help {
  color: var(--app-text-3);
  cursor: help;
}
.spacer {
  flex: 1;
}
.format-actions {
  display: inline-flex;
  align-items: center;
}
.format-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.format-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border: 1px solid var(--app-border);
  border-radius: 6px;
  cursor: pointer;
  color: var(--app-text-2);
}
.format-chip:hover {
  background: var(--app-hover);
}
.format-chip.active {
  color: var(--app-primary);
  border-color: var(--app-primary);
  background: var(--app-primary-bg);
}
.format-chip.off {
  color: var(--app-text-3);
}
.format-tag {
  margin: 0;
  font-size: 11px;
  line-height: 18px;
}
.full {
  width: 100%;
}
</style>
