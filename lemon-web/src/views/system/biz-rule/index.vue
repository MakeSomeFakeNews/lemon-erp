<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <template #title>{{ $t('system.bizRule.title') }}</template>
      <template #extra>
        <span class="sub-text">{{ $t('system.bizRule.subtitle') }}</span>
      </template>
      <div v-if="loading && !groups.length" class="page-spin"><a-spin /></div>
      <a-tabs v-model:active-key="group">
        <a-tab-pane v-for="g in groups" :key="g.key" :tab="g.key">
          <div v-for="item in g.items" :key="item.key" class="config-item">
            <div class="config-label">
              {{ item.label }}
              <a-tag v-if="item.modified" color="blue" :bordered="false" class="ml">{{ $t('system.config.modified') }}</a-tag>
              <a-tag v-if="edits[item.key] !== item.value" color="orange" :bordered="false" class="ml">{{ $t('system.config.unsaved') }}</a-tag>
            </div>
            <div class="config-control">
              <a-switch
                v-if="item.type === SettingType.BOOL"
                :checked="edits[item.key] === 'true'"
                :disabled="!canEdit"
                @change="(v) => (edits[item.key] = String(v))"
              />
              <a-radio-group v-else-if="item.type === SettingType.SELECT" v-model:value="edits[item.key]" :disabled="!canEdit" button-style="solid">
                <a-radio-button v-for="o in item.options" :key="o.value" :value="o.value">{{ o.label }}</a-radio-button>
              </a-radio-group>
              <div v-else-if="item.type === SettingType.TABLE" class="rule-table-wrap">
                <table class="rule-table">
                  <thead>
                    <tr>
                      <th v-for="c in item.columns" :key="c.label">{{ c.label }}</th>
                      <th v-if="canEdit" class="op" />
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, i) in tableRows(edits[item.key])" :key="i">
                      <td v-for="(c, j) in item.columns" :key="j">
                        <a-input-number
                          :value="row[j] === '' ? null : Number(row[j])"
                          :min="c.min"
                          :max="c.max"
                          :precision="c.scale"
                          :placeholder="c.nullable ? $t('system.bizRule.unlimited') : ''"
                          :disabled="!canEdit"
                          size="small"
                          class="cell"
                          @change="(v) => setCell(item, i, j, v)"
                        />
                      </td>
                      <td v-if="canEdit" class="op">
                        <a @click="insertRow(item, i)">{{ $t('system.bizRule.insertRow') }}</a>
                        <a v-if="tableRows(edits[item.key]).length > 1" class="danger" @click="removeRow(item, i)">{{ $t('common.action.delete') }}</a>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <a-input-number
                v-else
                :value="Number(edits[item.key])"
                :min="item.min"
                :max="item.max"
                :precision="item.type === SettingType.DECIMAL ? item.scale : 0"
                :disabled="!canEdit"
                class="number"
                @change="(v) => (edits[item.key] = v == null ? '' : String(v))"
              />
              <a v-if="canEdit && edits[item.key] !== item.defaultValue" class="reset" @click="edits[item.key] = item.defaultValue">{{ $t('system.config.restoreDefault') }}</a>
            </div>
            <div class="sub-text">
              <span v-if="item.remark">{{ $t('system.config.remark', { remark: item.remark }) }}</span><template v-if="item.type !== SettingType.TABLE">{{ $t('system.config.default', { value: display(item, item.defaultValue) }) }}</template>
            </div>
          </div>
        </a-tab-pane>
      </a-tabs>
      <div v-if="canEdit" class="config-actions">
        <a-button :disabled="!dirtyKeys.length" @click="discard">{{ $t('common.action.discard') }}</a-button>
        <a-button type="primary" :loading="saving" :disabled="!dirtyKeys.length" @click="save">{{ $t('system.config.saveN', { n: dirtyKeys.length }) }}</a-button>
      </div>
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { listBizRules, saveBizRules } from '@/api/system/customField'
import { useUserStore } from '@/store/user'

import { SettingType } from '@/constants/enums'
/** 业务参数：按模块分组显示各模块登记的业务控制规则（按租户），只提交修改过的 */
const { t } = useI18n()
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
  if (item.type === SettingType.BOOL) return value === 'true' ? t('system.config.on') : t('system.config.off')
  if (item.type === SettingType.SELECT) return item.options.find((o) => o.value === value)?.label || value
  return value
}
function discard() {
  items.value.forEach((i) => (edits[i.key] = i.value))
}

// 表格型参数：行之间用 ; 分隔、单元格之间用 , 分隔，留空表示不限（与后端 BizRule 一致）
function tableRows(value) {
  return (value || '').split(';').map((r) => r.split(','))
}
function setTable(item, rows) {
  edits[item.key] = rows.map((r) => r.join(',')).join(';')
}
function setCell(item, i, j, v) {
  const rows = tableRows(edits[item.key])
  rows[i][j] = v == null ? '' : String(v)
  setTable(item, rows)
}
function insertRow(item, i) {
  const rows = tableRows(edits[item.key])
  rows.splice(i + 1, 0, item.columns.map(() => ''))
  setTable(item, rows)
}
function removeRow(item, i) {
  const rows = tableRows(edits[item.key])
  rows.splice(i, 1)
  setTable(item, rows)
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
    message.success(t('common.msg.saved'))
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
.rule-table-wrap {
  max-width: 100%;
  overflow-x: auto;
}
.rule-table {
  border-collapse: collapse;
}
.rule-table th,
.rule-table td {
  padding: 4px 8px;
  border: 1px solid var(--app-border);
  text-align: left;
  white-space: nowrap;
}
.rule-table th {
  font-weight: 500;
  font-size: 12px;
  color: var(--app-text-2);
  background: var(--app-hover);
}
.rule-table .cell {
  width: 150px;
}
.rule-table .op a + a {
  margin-left: 8px;
}
.rule-table .danger {
  color: #ff4d4f;
}
.reset {
  font-size: 12px;
}
.ml {
  margin-left: 6px;
}
.config-actions {
  flex: none;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  max-width: 760px;
  padding-top: 12px;
}
.page-spin {
  padding: 48px 0;
  text-align: center;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
