<template>
  <a-modal
    :open="open"
    :footer="null"
    :closable="false"
    :mask-closable="false"
    :keyboard="false"
    width="100%"
    wrap-class-name="model-designer-wrap"
    destroy-on-close
  >
    <div class="designer">
      <div class="designer-header">
        <div class="designer-title">
          <a-button type="text" @click="close">
            <template #icon><ArrowLeftOutlined /></template>
          </a-button>
          <span class="designer-name">{{ model?.name }}</span>
          <a-tag v-if="model?.version" color="green" :bordered="false">{{ $t('workflow.model.publishedVersion', { v: model.version }) }}</a-tag>
          <a-tag v-else :bordered="false">{{ $t('workflow.model.unpublished') }}</a-tag>
          <a-tag v-if="dirty" color="orange" :bordered="false">{{ $t('workflow.model.dirty') }}</a-tag>
        </div>
        <a-segmented v-model:value="tab" :options="TABS" class="designer-tabs" />
        <a-space class="designer-actions">
          <a-button v-perm="'wf:model:edit'" :loading="saving === 'save'" :disabled="loading || !!saving" @click="save(false)">
            <template #icon><SaveOutlined /></template>{{ $t('common.action.save') }}
          </a-button>
          <a-button
            v-perm="'wf:model:deploy'"
            type="primary"
            :loading="saving === 'deploy'"
            :disabled="loading || !!saving"
            @click="save(true)"
          >
            <template #icon><CloudUploadOutlined /></template>{{ $t('workflow.model.saveAndDeploy') }}
          </a-button>
        </a-space>
      </div>

      <a-alert v-if="errors.length" type="error" show-icon closable class="designer-errors" @close="errors = []">
        <template #message>{{ $t('workflow.model.fixErrors') }}</template>
        <template #description>
          <ul class="error-list">
            <li v-for="(e, i) in errors" :key="i">{{ e }}</li>
          </ul>
        </template>
      </a-alert>

      <a-spin :spinning="loading" wrapper-class-name="designer-spin">
        <div v-show="tab === 'form'" class="designer-body designer-form">
          <FormDesigner :fields="form" />
        </div>
        <div v-show="tab === 'flow'" class="designer-body">
          <a-alert v-if="model?.business" type="info" show-icon class="business-tip">
            <template #message>
              {{ $t('workflow.model.businessTip', { name: model.business.name }) }}
              <a-tag v-for="v in model.business.variables" :key="v.field" :bordered="false" class="var-tag">{{ v.label }}</a-tag>
            </template>
          </a-alert>
          <FlowDesigner v-if="flow" :model-value="flow" :fields="conditionFields" :show-validation="showValidation" />
        </div>
      </a-spin>
    </div>
  </a-modal>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { computed, ref, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { ArrowLeftOutlined, CloudUploadOutlined, SaveOutlined } from '@ant-design/icons-vue'
import FlowDesigner from '@/components/workflow/designer/FlowDesigner.vue'
import FormDesigner from '@/components/workflow/form/FormDesigner.vue'
import { NODE_TYPES, createNode, createStartNode, validateFlow } from '@/components/workflow/designer/flow'
import { normalizeFields, validateFields } from '@/components/workflow/form/form'
import { deployModel, getModel, saveModelDesign } from '@/api/workflow/model'
import { useDictStore } from '@/store/dict'

const { t } = useI18n()

/** 流程模型设计：表单设计 + 流程设计，全屏弹窗 */
const props = defineProps({
  open: Boolean,
  modelId: Number
})
const emit = defineEmits(['update:open', 'saved'])

const TABS = [
  { label: t('workflow.model.formDesign'), value: 'form' },
  { label: t('workflow.model.flowDesign'), value: 'flow' }
]

const tab = ref('flow')
const loading = ref(false)
const saving = ref('')
const model = ref(null)
const form = ref([])
const flow = ref(null)
const errors = ref([])
const showValidation = ref(false)
const snapshot = ref('')

const serialize = () => JSON.stringify({ form: form.value, flow: flow.value })
const dirty = computed(() => !loading.value && !!model.value && serialize() !== snapshot.value)

function defaultFlow() {
  const start = createStartNode()
  start.next = createNode(NODE_TYPES.APPROVER)
  return start
}

// 条件分支可选的字段：表单字段 + 业务单据传入的流程变量（字典类型的变量以字典标签显示可选值）
const dictStore = useDictStore()
const conditionFields = computed(() => [
  ...form.value,
  ...(model.value?.business?.variables || []).map((v) => ({
    field: v.field,
    label: t('workflow.model.businessVar', { label: v.label }),
    type: v.type,
    // 字典类型的变量：字典取值（含租户新增的）+ 字典之外的固定值（如加班时假期类型为 OVERTIME）
    options: v.dictType
      ? [
          ...(dictStore.cache[v.dictType] || []).map((d) => ({ label: d.label, value: d.value })),
          ...(v.options || []).map((o) => ({ label: o, value: o }))
        ]
      : v.options || []
  }))
])

async function load() {
  loading.value = true
  errors.value = []
  showValidation.value = false
  try {
    const data = await getModel(props.modelId)
    model.value = data
    data.business?.variables?.forEach((v) => v.dictType && dictStore.load(v.dictType).catch(() => {}))
    form.value = (data.form || []).map((f) => ({ ...f, options: f.options || [] }))
    flow.value = data.flow || defaultFlow()
    tab.value = 'flow'
    snapshot.value = serialize()
  } catch (e) {
    emit('update:open', false)
    throw e
  } finally {
    loading.value = false
  }
}

watch(
  () => props.open,
  (open) => open && props.modelId && load(),
  { immediate: true }
)

function validate() {
  const formErrors = validateFields(form.value).map((msg) => t('workflow.model.formError', { msg }))
  const flowErrors = validateFlow(flow.value, conditionFields.value).errors.map((msg) => t('workflow.model.flowError', { msg }))
  return { formErrors, flowErrors }
}

async function save(deploy) {
  showValidation.value = true
  const { formErrors, flowErrors } = validate()
  errors.value = [...formErrors, ...flowErrors]
  if (errors.value.length) {
    tab.value = formErrors.length ? 'form' : 'flow'
    return
  }
  saving.value = deploy ? 'deploy' : 'save'
  try {
    await saveModelDesign(props.modelId, { form: normalizeFields(form.value), flow: flow.value })
    snapshot.value = serialize()
    if (deploy) {
      const version = await deployModel(props.modelId)
      model.value.version = version
      message.success(t('workflow.model.deployed', { v: version }))
    } else {
      message.success(t('common.msg.saveSuccess'))
    }
    emit('saved')
  } finally {
    saving.value = ''
  }
}

function close() {
  if (!dirty.value) {
    emit('update:open', false)
    return
  }
  Modal.confirm({
    title: t('workflow.model.discardTitle'),
    content: t('workflow.model.discardContent'),
    okText: t('common.action.discard'),
    okType: 'danger',
    cancelText: t('workflow.model.keepEditing'),
    onOk: () => emit('update:open', false)
  })
}
</script>

<style scoped>
.designer {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--app-bg);
}
.designer-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 16px;
  background: var(--app-card);
  border-bottom: 1px solid var(--app-border);
  flex-wrap: wrap;
}
.designer-title {
  flex: 1;
  min-width: 200px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.designer-name {
  font-size: 16px;
  font-weight: 600;
}
.designer-actions {
  flex: 1;
  justify-content: flex-end;
  display: flex;
}
.designer-errors {
  margin: 12px 16px 0;
}
.error-list {
  margin: 0;
  padding-left: 18px;
  max-height: 120px;
  overflow: auto;
}
.designer-spin {
  flex: 1;
  min-height: 0;
}
.designer-spin :deep(.ant-spin-container) {
  height: 100%;
}
.designer-body {
  height: 100%;
}
.business-tip {
  margin: 12px 16px 0;
}
.var-tag {
  margin: 2px 4px 2px 0;
}
.designer-form {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px;
  overflow: auto;
}
@media (max-width: 767px) {
  .designer-tabs {
    order: 3;
  }
}
</style>

<style>
.model-designer-wrap .ant-modal {
  top: 0;
  max-width: 100%;
  margin: 0;
  padding-bottom: 0;
}
.model-designer-wrap .ant-modal-content {
  padding: 0;
  border-radius: 0;
  overflow: hidden;
}
.model-designer-wrap .ant-modal-body {
  padding: 0;
}
</style>
