<template>
  <div class="config-page">
    <a-card :bordered="false" class="page-card">
      <template #title>业务参数</template>
      <template #extra>
        <span class="sub-text">各模块的业务控制规则，只对当前企业（租户）生效；修改后立即生效</span>
      </template>
      <a-spin :spinning="loading">
        <a-tabs v-model:active-key="group">
          <a-tab-pane v-for="g in groups" :key="g.key" :tab="g.key">
            <div v-for="item in g.items" :key="item.key" class="config-item">
              <div class="config-label">
                {{ item.label }}
                <a-tag v-if="item.modified" color="blue" :bordered="false" class="ml">已修改</a-tag>
                <a-tag v-if="edits[item.key] !== item.value" color="orange" :bordered="false" class="ml">未保存</a-tag>
              </div>
              <div class="config-control">
                <a-switch
                  v-if="item.type === 'BOOL'"
                  :checked="edits[item.key] === 'true'"
                  :disabled="!canEdit"
                  @change="(v) => (edits[item.key] = String(v))"
                />
                <a-radio-group v-else-if="item.type === 'SELECT'" v-model:value="edits[item.key]" :disabled="!canEdit" button-style="solid">
                  <a-radio-button v-for="o in item.options" :key="o.value" :value="o.value">{{ o.label }}</a-radio-button>
                </a-radio-group>
                <a-input-number
                  v-else
                  :value="Number(edits[item.key])"
                  :min="item.min"
                  :max="item.max"
                  :precision="0"
                  :disabled="!canEdit"
                  class="number"
                  @change="(v) => (edits[item.key] = v == null ? '' : String(v))"
                />
                <a v-if="canEdit && edits[item.key] !== item.defaultValue" class="reset" @click="edits[item.key] = item.defaultValue">恢复默认</a>
              </div>
              <div class="sub-text">
                <span v-if="item.remark">{{ item.remark }}。</span>默认：{{ display(item, item.defaultValue) }}
              </div>
            </div>
          </a-tab-pane>
        </a-tabs>
        <div v-if="canEdit" class="config-actions">
          <a-button :disabled="!dirtyKeys.length" @click="discard">放弃修改</a-button>
          <a-button type="primary" :loading="saving" :disabled="!dirtyKeys.length" @click="save">保存（{{ dirtyKeys.length }}）</a-button>
        </div>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { listBizRules, saveBizRules } from '@/api/system/customField'
import { useUserStore } from '@/store/user'

/** 业务参数：按模块分组显示各模块登记的业务控制规则（按租户），只提交修改过的 */
const userStore = useUserStore()
const canEdit = computed(() => userStore.hasPerm('sys:bizRule:edit'))
const items = ref([])
const edits = reactive({})
const loading = ref(false)
const saving = ref(false)
const group = ref()

const groups = computed(() => {
  const map = new Map()
  items.value.forEach((i) => {
    if (!map.has(i.module)) map.set(i.module, { key: i.module, items: [] })
    map.get(i.module).items.push(i)
  })
  return [...map.values()]
})
const dirtyKeys = computed(() => items.value.filter((i) => edits[i.key] !== i.value).map((i) => i.key))

function display(item, value) {
  if (item.type === 'BOOL') return value === 'true' ? '开启' : '关闭'
  if (item.type === 'SELECT') return item.options.find((o) => o.value === value)?.label || value
  return value
}
function discard() {
  items.value.forEach((i) => (edits[i.key] = i.value))
}
async function load() {
  loading.value = true
  try {
    items.value = await listBizRules()
    discard()
    group.value ??= items.value[0]?.module
  } finally {
    loading.value = false
  }
}
onMounted(load)

async function save() {
  const data = {}
  dirtyKeys.value.forEach((k) => (data[k] = edits[k]))
  saving.value = true
  try {
    await saveBizRules(data)
    message.success('已保存')
    load()
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
