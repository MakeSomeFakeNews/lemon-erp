<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <template #title>{{ $t('fin.accountRule.title') }}</template>
      <template #extra>
        <span class="sub-text">{{ $t('fin.accountRule.tip') }}</span>
      </template>
      <div v-if="loading && !groups.length" class="page-spin"><a-spin /></div>
      <a-tabs v-model:active-key="group">
        <a-tab-pane v-for="g in groups" :key="g.key" :tab="g.key">
          <div v-for="role in g.roles" :key="role.role" class="rule-item">
            <div class="rule-label">
              {{ role.label }}
              <a-tag v-if="isModified(role)" color="blue" :bordered="false" class="ml">{{ $t('fin.accountRule.modified') }}</a-tag>
              <a-tag v-if="isDirty(role)" color="orange" :bordered="false" class="ml">{{ $t('fin.accountRule.unsaved') }}</a-tag>
            </div>
            <div class="rule-control">
              <AccountSelect
                v-model:value="edits[role.role].accountId"
                allow-clear
                :disabled="!canEdit"
                :placeholder="rolePlaceholder(role)"
                class="account"
              />
            </div>
            <template v-if="role.dimension !== 'NONE'">
              <div class="sub-text dim-tip">
                {{ $t('fin.accountRule.byDimension', { dim: $t(DIMENSION_LABELS[role.dimension]) }) }}
              </div>
              <div v-for="d in dict[role.dictType]" :key="d.value" class="dim-row">
                <span class="dim-label">{{ d.label }}</span>
                <AccountSelect
                  v-model:value="edits[role.role].items[d.value]"
                  allow-clear
                  :disabled="!canEdit"
                  :placeholder="dimPlaceholder(role, d.value)"
                  class="account"
                />
              </div>
            </template>
          </div>
        </a-tab-pane>
      </a-tabs>
      <div v-if="canEdit" class="rule-actions">
        <a-button :disabled="!dirtyRoles.length" @click="discard">{{ $t('common.action.discard') }}</a-button>
        <a-button type="primary" :loading="saving" :disabled="!dirtyRoles.length" @click="save">{{ $t('fin.accountRule.saveN', { n: dirtyRoles.length }) }}</a-button>
      </div>
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { accountOptions, accountRules, saveAccountRule } from '@/api/fin'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import { cached } from '@/components/fin/fin'

/**
 * 凭证科目设置：业务单据自动生成凭证时各用途使用的科目。存货、收入、成本可按物料分类，工资费用可按职位类别，其他出入库的对方科目可按收发类别分别设置；
 * 优先级为 分类设置 → 用途设置 → 预置科目
 */
const { t } = useI18n()
const userStore = useUserStore()
const canEdit = computed(() => userStore.hasPerm('fin:accountRule:edit'))
const dict = useDict('wms_material_category', 'hr_position_category', 'wms_in_category', 'wms_out_category')
/** 维度 → 名称（与后端 AccountRole.Dimension 一致） */
const DIMENSION_LABELS = {
  MATERIAL_CATEGORY: 'fin.accountRule.materialCategory',
  POSITION_CATEGORY: 'fin.accountRule.positionCategory',
  IN_CATEGORY: 'fin.accountRule.inCategory',
  OUT_CATEGORY: 'fin.accountRule.outCategory'
}

const roles = ref([])
const accounts = ref([])
const edits = reactive({})
const loading = ref(false)
const saving = ref(false)
const group = ref()

const groups = computed(() => {
  const map = new Map()
  roles.value.forEach((r) => {
    if (!map.has(r.group)) map.set(r.group, { key: r.group, roles: [] })
    map.get(r.group).roles.push(r)
  })
  return [...map.values()]
})

const accountByCode = computed(() => new Map(accounts.value.map((a) => [a.code, a])))
const accountById = computed(() => new Map(accounts.value.map((a) => [a.id, a])))

function accountText(code) {
  const a = accountByCode.value.get(code)
  return a ? `${a.code} ${a.name}` : t('fin.accountRule.invalidAccount', { code })
}

const roleByName = computed(() => new Map(roles.value.map((r) => [r.role, r])))

function idText(id) {
  const a = accountById.value.get(id)
  return a ? `${a.code} ${a.name}` : id
}

/** 用途实际使用的科目：用途设置 → 后备用途（如其他出入库对方科目沿用待处理财产损溢）→ 预置科目 */
function effectiveText(role) {
  const id = edits[role.role]?.accountId
  if (id) return idText(id)
  const fallback = role.fallback && roleByName.value.get(role.fallback)
  return fallback ? effectiveText(fallback) : accountText(role.defaultCode)
}

/** 用途未设置时的提示：有后备用途的显示「同 xx」，否则显示预置科目 */
function rolePlaceholder(role) {
  const fallback = role.fallback && roleByName.value.get(role.fallback)
  if (fallback) return t('fin.accountRule.sameAsRole', { role: fallback.label, account: effectiveText(fallback) })
  return t('fin.accountRule.preset', { account: accountText(role.defaultCode) })
}

/** 分类未设置时实际使用的科目：用途设置 → 后备用途 → 该分类的预置科目 */
function dimPlaceholder(role, value) {
  const id = edits[role.role].accountId
  if (id) return t('fin.accountRule.sameAsAbove', { account: idText(id) })
  if (role.fallback) return rolePlaceholder(role)
  return t('fin.accountRule.preset', { account: accountText(role.dimensionDefaults?.[value] || role.defaultCode) })
}

async function load() {
  loading.value = true
  try {
    const [list, leaves] = await Promise.all([accountRules(), cached('accounts-leaf', accountOptions)])
    roles.value = list
    accounts.value = leaves
    discard()
    group.value ??= list[0]?.group
  } finally {
    loading.value = false
  }
}
onMounted(load)

function discard() {
  roles.value.forEach((r) => (edits[r.role] = { accountId: r.accountId ?? null, items: { ...(r.items || {}) } }))
}

/** 去掉未填写的分类，便于比较 */
const cleanItems = (items) => Object.fromEntries(Object.entries(items || {}).filter(([, v]) => v != null))
const sameItems = (a, b) => {
  const x = cleanItems(a)
  const y = cleanItems(b)
  return Object.keys(x).length === Object.keys(y).length && Object.keys(x).every((k) => x[k] === y[k])
}
const isDirty = (r) => edits[r.role] && ((edits[r.role].accountId ?? null) !== (r.accountId ?? null) || !sameItems(edits[r.role].items, r.items))
const isModified = (r) => r.accountId != null || Object.keys(r.items || {}).length > 0
const dirtyRoles = computed(() => roles.value.filter(isDirty))

async function save() {
  saving.value = true
  try {
    for (const r of dirtyRoles.value) {
      await saveAccountRule(r.role, { accountId: edits[r.role].accountId ?? null, items: cleanItems(edits[r.role].items) })
    }
    message.success(t('common.msg.saved'))
    await load()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.rule-item {
  padding: 14px 0;
  border-bottom: 1px solid var(--app-border);
  max-width: 760px;
}
.rule-label {
  font-weight: 500;
  margin-bottom: 8px;
}
.rule-control {
  display: flex;
  align-items: center;
  gap: 12px;
}
.account {
  width: 360px;
  max-width: 100%;
}
.dim-tip {
  margin: 10px 0 6px;
}
.dim-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}
.dim-label {
  width: 140px;
  flex-shrink: 0;
  color: var(--app-text-2);
}
.ml {
  margin-left: 6px;
}
.rule-actions {
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
