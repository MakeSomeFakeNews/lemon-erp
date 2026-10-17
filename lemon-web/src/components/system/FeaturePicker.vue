<template>
  <div class="feature-picker">
    <div class="section-label">行业模板 <span class="sub-text">选择后自动勾选该行业常用的功能模块，可再调整</span></div>
    <div class="industries">
      <div
        v-for="i in catalog.industries"
        :key="i.key"
        :class="['industry', { active: industry === i.key }]"
        @click="applyIndustry(i)"
      >
        <div class="industry-name">{{ i.name }}</div>
        <div class="industry-desc">{{ i.description }}</div>
        <div class="industry-count">{{ i.features.length }} 个模块</div>
      </div>
    </div>

    <div class="section-label">
      功能模块
      <span class="sub-text">已选 {{ checkedKeys.length }} / {{ catalog.features.length }}；勾选时自动加上依赖的模块，取消时一并取消依赖它的模块</span>
    </div>
    <div v-for="g in groups" :key="g.name" class="group">
      <div class="group-name">{{ g.name }}</div>
      <div class="features">
        <label
          v-for="f in g.features"
          :key="f.key"
          :class="['feature', { checked: state[f.key] === 'all', partial: state[f.key] === 'part', disabled: f.required }]"
        >
          <a-checkbox
            :checked="state[f.key] === 'all'"
            :indeterminate="state[f.key] === 'part'"
            :disabled="f.required"
            @change="(e) => toggle(f, e.target.checked)"
          />
          <span class="feature-text">
            <span class="feature-name">{{ f.name }}<a-tag v-if="f.required" :bordered="false" class="req">必选</a-tag></span>
            <span class="feature-desc">{{ f.description }}</span>
            <span v-if="f.requires.length" class="feature-req">依赖：{{ f.requires.map(nameOf).join('、') }}</span>
          </span>
        </label>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'

/**
 * 按功能模块选择菜单：行业模板一键勾选，模块勾选 / 取消时同步菜单（v-model 为菜单 ID 列表）。
 * - catalog：/system/tenant-package/features 的结果 { features, industries }
 * - tree：菜单树（可选）。传入时补齐上级目录并去掉没有下级的目录（套餐编辑页与菜单树同步）；
 *   不传时只输出模块中的菜单，上级目录由后端补齐（新建租户）
 * - 模块状态由菜单计算：全部包含为勾选，部分包含为半选（在菜单树中单独调整过）
 */
const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  catalog: { type: Object, required: true },
  tree: { type: Array, default: null }
})
const emit = defineEmits(['update:modelValue', 'industry'])

const industry = ref()
const byKey = computed(() => Object.fromEntries(props.catalog.features.map((f) => [f.key, f])))
const nameOf = (key) => byKey.value[key]?.name || key
const groups = computed(() => {
  const map = new Map()
  props.catalog.features.forEach((f) => {
    if (!map.has(f.group)) map.set(f.group, { name: f.group, features: [] })
    map.get(f.group).features.push(f)
  })
  return [...map.values()]
})

const selected = computed(() => new Set(props.modelValue.map(Number)))
/** 模块状态：all 全部包含、part 部分包含、none */
const state = computed(() => {
  const s = {}
  props.catalog.features.forEach((f) => {
    const hit = f.menuIds.filter((id) => selected.value.has(id)).length
    s[f.key] = f.required || (hit && hit === f.menuIds.length) ? 'all' : hit ? 'part' : 'none'
  })
  return s
})
const checkedKeys = computed(() => props.catalog.features.filter((f) => state.value[f.key] === 'all').map((f) => f.key))

/** 模块及其依赖（递归） */
function withRequires(keys) {
  const result = new Set()
  const queue = [...keys]
  while (queue.length) {
    const f = byKey.value[queue.shift()]
    if (f && !result.has(f.key)) {
      result.add(f.key)
      queue.push(...f.requires)
    }
  }
  props.catalog.features.filter((f) => f.required).forEach((f) => result.add(f.key))
  return result
}
/** 依赖 key 的已勾选模块（递归） */
function dependents(key) {
  const result = new Set([key])
  let changed = true
  while (changed) {
    changed = false
    for (const f of props.catalog.features) {
      if (!result.has(f.key) && state.value[f.key] !== 'none' && f.requires.some((r) => result.has(r))) {
        result.add(f.key)
        changed = true
      }
    }
  }
  return result
}

// ---------------------------------------------------------------- 菜单树：补齐上级、去掉空目录
const nodes = computed(() => {
  const map = new Map()
  const walk = (list, parent) => list?.forEach((n) => {
    map.set(n.id, { parent, type: n.type })
    walk(n.children, n.id)
  })
  walk(props.tree || [], null)
  return map
})
function normalize(ids) {
  if (!props.tree) return [...ids]
  const result = new Set()
  for (const id of ids) {
    const node = nodes.value.get(id)
    if (!node || node.type === 'M') continue
    let cur = id
    while (cur != null && !result.has(cur)) {
      result.add(cur)
      cur = nodes.value.get(cur)?.parent ?? null
    }
  }
  return [...result]
}

function toggle(feature, on) {
  const ids = new Set(selected.value)
  if (on) {
    const keys = withRequires([feature.key])
    const added = [...keys].filter((k) => k !== feature.key && state.value[k] !== 'all').map(nameOf)
    keys.forEach((k) => byKey.value[k].menuIds.forEach((id) => ids.add(id)))
    if (added.length) message.info(`同时勾选了依赖的模块：${added.join('、')}`)
  } else {
    const keys = dependents(feature.key)
    const removed = [...keys].filter((k) => k !== feature.key).map(nameOf)
    keys.forEach((k) => byKey.value[k].menuIds.forEach((id) => ids.delete(id)))
    if (removed.length) message.info(`同时取消了依赖它的模块：${removed.join('、')}`)
  }
  industry.value = undefined
  emit('industry', null)
  emit('update:modelValue', normalize(ids))
}

function applyIndustry(item) {
  industry.value = item.key
  const ids = new Set()
  withRequires(item.features).forEach((k) => byKey.value[k].menuIds.forEach((id) => ids.add(id)))
  emit('industry', item)
  emit('update:modelValue', normalize(ids))
}

/** 外部清空（新建表单）时取消行业选择 */
watch(() => props.modelValue.length, (n) => {
  if (!n) industry.value = undefined
})
defineExpose({ applyIndustry })
</script>

<style scoped>
.section-label {
  margin: 4px 0 8px;
  font-weight: 500;
}
.sub-text {
  margin-left: 6px;
  color: var(--app-text-3);
  font-size: 12px;
  font-weight: 400;
}
.industries {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 8px;
  margin-bottom: 16px;
}
.industry {
  padding: 8px 10px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}
.industry:hover {
  border-color: var(--app-primary);
}
.industry.active {
  border-color: var(--app-primary);
  background: var(--app-primary-bg, #e6f4ff);
}
.industry-name {
  font-weight: 500;
}
.industry-desc {
  margin: 2px 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--app-text-3);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.industry-count {
  font-size: 12px;
  color: var(--app-primary);
}
.group {
  margin-bottom: 12px;
}
.group-name {
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--app-text-2);
}
.features {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 8px;
}
.feature {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  padding: 8px 10px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  cursor: pointer;
}
.feature.checked {
  border-color: var(--app-primary);
  background: var(--app-primary-bg, #e6f4ff);
}
.feature.partial {
  border-style: dashed;
  border-color: var(--app-primary);
}
.feature.disabled {
  cursor: default;
}
.feature-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.feature-name {
  font-weight: 500;
}
.req {
  margin-left: 6px;
  font-size: 11px;
  line-height: 16px;
}
.feature-desc,
.feature-req {
  font-size: 12px;
  line-height: 1.5;
  color: var(--app-text-3);
}
.feature-req {
  color: var(--app-text-2);
}
</style>
