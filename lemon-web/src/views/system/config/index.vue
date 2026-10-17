<template>
  <div class="config-page">
    <a-card :bordered="false" class="page-card">
      <template #title>{{ $t('system.config.title') }}</template>
      <template #extra>
        <span class="sub-text">{{ $t('system.config.subtitle') }}</span>
      </template>
      <a-spin :spinning="loading">
        <a-tabs v-model:active-key="group">
          <a-tab-pane v-for="g in groups" :key="g.key" :tab="g.label">
            <div v-for="item in g.items" :key="item.key" class="config-item">
              <div class="config-label">
                {{ item.label }}
                <a-tag v-if="isModified(item)" color="blue" :bordered="false" class="ml">{{ $t('system.config.modified') }}</a-tag>
                <a-tag v-if="isDirty(item)" color="orange" :bordered="false" class="ml">{{ $t('system.config.unsaved') }}</a-tag>
              </div>
              <div class="config-control">
                <a-switch v-if="item.type === 'BOOL'" :checked="edits[item.key] === 'true'" :disabled="!canEdit"
                  @change="(v) => (edits[item.key] = String(v))" />
                <a-radio-group v-else-if="item.type === 'SELECT'" v-model:value="edits[item.key]" :disabled="!canEdit" button-style="solid">
                  <a-radio-button v-for="o in item.options" :key="o.value" :value="o.value">{{ o.label }}</a-radio-button>
                </a-radio-group>
                <a-input-number v-else-if="item.type === 'NUMBER'" :value="Number(edits[item.key])" :min="item.min" :max="item.max" :precision="0"
                  :disabled="!canEdit" class="number" @change="(v) => (edits[item.key] = v == null ? '' : String(v))" />
                <a-input v-else v-model:value="edits[item.key]" :maxlength="item.max" :disabled="!canEdit" class="text" />
                <a v-if="canEdit && edits[item.key] !== item.defaultValue" class="reset" @click="edits[item.key] = item.defaultValue">{{ $t('system.config.restoreDefault') }}</a>
              </div>
              <div class="sub-text">
                <span v-if="item.remark">{{ $t('system.config.remark', { remark: item.remark }) }}</span>{{ $t('system.config.default', { value: display(item, item.defaultValue) }) }}
              </div>
            </div>
          </a-tab-pane>
        </a-tabs>
        <div v-if="canEdit" class="config-actions">
          <a-button :disabled="!dirtyKeys.length" @click="discard">{{ $t('common.action.discard') }}</a-button>
          <a-button type="primary" :loading="saving" :disabled="!dirtyKeys.length" @click="save">{{ $t('system.config.saveN', { n: dirtyKeys.length }) }}</a-button>
        </div>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { listConfig, saveConfig } from '@/api/system/config'
import { useConfigStore } from '@/store/config'
import { useUserStore } from '@/store/user'

/** 参数设置：按分组显示系统参数（平台级），只提交修改过的参数；值等于默认值时恢复默认 */
const { t } = useI18n()
const userStore = useUserStore()
const configStore = useConfigStore()
const canEdit = computed(() => userStore.hasPerm('sys:config:edit'))

const items = ref([])
const edits = reactive({})
const loading = ref(false)
const saving = ref(false)
const group = ref()

const groups = computed(() => {
  const map = new Map()
  items.value.forEach((i) => {
    if (!map.has(i.group)) map.set(i.group, { key: i.group, label: i.groupLabel, items: [] })
    map.get(i.group).items.push(i)
  })
  return [...map.values()]
})

async function load() {
  loading.value = true
  try {
    items.value = await listConfig()
    discard()
    group.value ??= items.value[0]?.group
  } finally {
    loading.value = false
  }
}
onMounted(load)

function discard() {
  items.value.forEach((i) => (edits[i.key] = i.value))
}

const isDirty = (item) => edits[item.key] !== item.value
const isModified = (item) => item.modified
const dirtyKeys = computed(() => items.value.filter(isDirty).map((i) => i.key))

function display(item, value) {
  if (item.type === 'BOOL') return value === 'true' ? t('system.config.on') : t('system.config.off')
  if (item.type === 'SELECT') return item.options.find((o) => o.value === value)?.label || value
  return value === '' || value == null ? t('system.config.empty') : value
}

async function save() {
  const data = {}
  dirtyKeys.value.forEach((k) => (data[k] = edits[k]))
  saving.value = true
  try {
    await saveConfig(data)
    message.success(t('common.msg.saved'))
    await Promise.all([load(), configStore.load(true)])
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.config-item {
  padding: 14px 0;
  border-bottom: 1px solid var(--app-border);
  max-width: 760px;
}
.config-label {
  font-weight: 500;
  margin-bottom: 8px;
}
.config-control {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 6px;
}
.number {
  width: 160px;
}
.text {
  width: 320px;
  max-width: 100%;
}
.reset {
  font-size: 12px;
}
.ml {
  margin-left: 6px;
}
.config-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  max-width: 760px;
  margin-top: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
