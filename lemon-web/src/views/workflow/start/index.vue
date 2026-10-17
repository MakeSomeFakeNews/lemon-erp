<template>
  <div class="start-page">
    <a-card :bordered="false" class="page-card">
      <div class="start-toolbar">
        <a-input v-model:value="keyword" :placeholder="$t('workflow.start.search')" allow-clear class="start-search">
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-button type="text" :title="$t('common.action.refresh')" @click="loadModels">
          <template #icon><ReloadOutlined /></template>
        </a-button>
      </div>

      <a-spin :spinning="loading">
        <div v-for="group in groups" :key="group.value" class="start-group">
          <div class="start-group-title">{{ group.label }}<span class="start-group-count">{{ group.items.length }}</span></div>
          <div class="start-grid">
            <div v-for="m in group.items" :key="m.modelKey" class="start-card" @click="openStart(m)">
              <span class="start-icon"><MenuIcon :name="m.icon || 'AuditOutlined'" /></span>
              <div class="start-card-text">
                <div class="start-card-name">{{ m.name }}</div>
                <div class="start-card-remark">{{ m.remark || $t('workflow.start.clickToStart') }}</div>
              </div>
            </div>
          </div>
        </div>
        <a-empty v-if="!loading && !groups.length" :description="keyword ? $t('workflow.start.noMatch') : $t('workflow.start.empty')" />
      </a-spin>
    </a-card>

    <!-- 填写申请 -->
    <a-drawer
      v-model:open="drawerOpen"
      :title="current?.name ? $t('workflow.start.titleWithName', { name: current.name }) : $t('workflow.start.title')"
      width="min(720px, 100vw)"
      destroy-on-close
    >
      <a-spin :spinning="modelLoading">
        <template v-if="current">
          <FormRender v-if="current.form?.length" ref="formRef" :fields="current.form" :model="formData" />
          <a-alert v-else type="info" show-icon :message="$t('workflow.start.noForm')" class="no-form" />
          <a-collapse ghost class="flow-preview">
            <a-collapse-panel key="flow" :header="$t('workflow.start.viewFlow')">
              <div class="flow-preview-box">
                <FlowDesigner :model-value="current.flow" :fields="current.form || []" readonly />
              </div>
            </a-collapse-panel>
          </a-collapse>
        </template>
      </a-spin>
      <template #footer>
        <div class="drawer-footer">
          <a-button @click="drawerOpen = false">{{ $t('common.action.cancel') }}</a-button>
          <a-button type="primary" :loading="submitting" :disabled="modelLoading || !current" @click="submit">{{ $t('common.action.submit') }}</a-button>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import MenuIcon from '@/components/MenuIcon.vue'
import FlowDesigner from '@/components/workflow/designer/FlowDesigner.vue'
import FormRender from '@/components/workflow/form/FormRender.vue'
import { getStartModel, listStartModels, startInstance } from '@/api/workflow/instance'
import { useDict } from '@/composables/useDict'

const { t } = useI18n()

const dict = useDict('wf_category')

const loading = ref(false)
const models = ref([])
const keyword = ref('')

async function loadModels() {
  loading.value = true
  try {
    models.value = await listStartModels()
  } finally {
    loading.value = false
  }
}
onMounted(loadModels)

/** 按分类分组，分类顺序与字典一致，未分类的放最后 */
const groups = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const filtered = models.value.filter((m) => !kw || m.name.toLowerCase().includes(kw))
  const result = dict.wf_category.map((d) => ({ value: d.value, label: d.label, items: [] }))
  const other = { value: '__other', label: t('workflow.start.other'), items: [] }
  filtered.forEach((m) => (result.find((g) => g.value === m.category) || other).items.push(m))
  return [...result, other].filter((g) => g.items.length)
})

// 发起
const drawerOpen = ref(false)
const modelLoading = ref(false)
const current = ref(null)
const formRef = ref()
const formData = reactive({})
const submitting = ref(false)

async function openStart(m) {
  current.value = null
  Object.keys(formData).forEach((k) => delete formData[k])
  drawerOpen.value = true
  modelLoading.value = true
  try {
    current.value = await getStartModel(m.modelKey)
  } catch (e) {
    drawerOpen.value = false
    throw e
  } finally {
    modelLoading.value = false
  }
}

async function submit() {
  if (current.value.form?.length) {
    try {
      await formRef.value.validate()
    } catch {
      return
    }
  }
  submitting.value = true
  try {
    await startInstance({ modelKey: current.value.modelKey, formData: { ...formData } })
    message.success(t('workflow.start.submitted'))
    drawerOpen.value = false
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.start-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.start-search {
  width: 280px;
  max-width: 100%;
}
.start-group {
  margin-top: 16px;
}
.start-group-title {
  margin-bottom: 12px;
  font-weight: 600;
}
.start-group-count {
  margin-left: 6px;
  color: var(--app-text-3);
  font-weight: normal;
  font-size: 12px;
}
.start-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
}
.start-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid var(--app-border);
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.start-card:hover {
  border-color: var(--app-primary);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}
.start-icon {
  width: 40px;
  height: 40px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  font-size: 20px;
  color: var(--app-primary);
  background: var(--app-primary-bg);
}
.start-card-text {
  min-width: 0;
}
.start-card-name {
  font-weight: 500;
}
.start-card-remark {
  margin-top: 2px;
  color: var(--app-text-3);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.no-form {
  margin-bottom: 16px;
}
.flow-preview :deep(.ant-collapse-header) {
  padding-left: 0 !important;
}
.flow-preview-box {
  height: 420px;
}
</style>
