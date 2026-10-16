<template>
  <a-tooltip :title="`搜索菜单（${shortcut} K）`">
    <button type="button" class="search-trigger" @click="show">
      <SearchOutlined />
      <span class="trigger-text hide-mobile">搜索菜单</span>
      <kbd class="trigger-kbd hide-mobile">{{ shortcut }} K</kbd>
    </button>
  </a-tooltip>

  <a-modal
    v-model:open="open"
    :footer="null"
    :closable="false"
    :width="560"
    :style="{ top: '80px' }"
    wrap-class-name="menu-search-modal"
    destroy-on-close
  >
    <a-input
      ref="inputRef"
      v-model:value="keyword"
      size="large"
      placeholder="输入页面名称或所属模块，如：入库、凭证、财务"
      allow-clear
      :bordered="false"
      class="search-input"
      @keydown="onKeydown"
    >
      <template #prefix><SearchOutlined class="input-icon" /></template>
    </a-input>

    <div class="result-title">{{ keyword.trim() ? `${results.length} 个结果` : '最近访问' }}</div>
    <ul ref="listRef" class="result-list scroll-hover">
      <li
        v-for="(item, i) in results"
        :key="item.path"
        :class="['result-item', { active: i === activeIndex }]"
        @mouseenter="activeIndex = i"
        @click="go(item)"
      >
        <span class="item-icon"><MenuIcon :name="item.icon || 'FileOutlined'" /></span>
        <span class="item-main">
          <span class="item-title">
            <template v-for="(part, j) in highlight(item.title)" :key="j">
              <mark v-if="part.hit">{{ part.text }}</mark>
              <template v-else>{{ part.text }}</template>
            </template>
          </span>
          <span class="item-chain">{{ item.chain }}</span>
        </span>
        <EnterOutlined v-if="i === activeIndex" class="item-enter" />
      </li>
      <li v-if="!results.length" class="empty">{{ keyword.trim() ? '没有匹配的页面' : '还没有访问记录，输入关键字搜索' }}</li>
    </ul>

    <div class="footer-tip">
      <span><kbd>↑</kbd><kbd>↓</kbd> 选择</span>
      <span><kbd>Enter</kbd> 打开</span>
      <span><kbd>Esc</kbd> 关闭</span>
    </div>
  </a-modal>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { EnterOutlined, SearchOutlined } from '@ant-design/icons-vue'
import MenuIcon from '@/components/MenuIcon.vue'
import { useUserStore } from '@/store/user'
import { buildBreadcrumbIndex, flattenPages } from '@/utils/menu'
import { load, save } from '@/utils/storage'

/**
 * 菜单搜索：按页面名称、所属模块和分组搜索当前用户可访问的页面，⌘/Ctrl + K 打开；
 * 没有输入时显示最近访问的页面（按用户保存在本地）
 */
const RECENT_MAX = 8
const RESULT_MAX = 50

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const shortcut = /Mac|iPhone|iPad/.test(navigator.platform) ? '⌘' : 'Ctrl'

const open = ref(false)
const keyword = ref('')
const activeIndex = ref(0)
const inputRef = ref()
const listRef = ref()

/** 可访问的页面，附带「模块 / 分组」路径用于展示和匹配 */
const pages = computed(() => {
  const chains = buildBreadcrumbIndex(userStore.menus)
  return flattenPages(userStore.menus).map((p) => {
    const parents = (chains.get(p.path) || []).slice(0, -1).map((c) => c.title)
    return { ...p, chain: parents.join(' / '), search: [p.title, ...parents].join(' ').toLowerCase() }
  })
})
const pageByPath = computed(() => new Map(pages.value.map((p) => [p.path, p])))

const recentKey = computed(() => `lemon_erp_recent_pages_${userStore.user?.id ?? ''}`)
const recent = ref([])
watch(recentKey, (k) => (recent.value = load(k, [])), { immediate: true })
watch(
  () => route.path,
  (path) => {
    if (!pageByPath.value.has(path)) return
    recent.value = [path, ...recent.value.filter((p) => p !== path)].slice(0, RECENT_MAX)
    save(recentKey.value, recent.value)
  },
  { immediate: true }
)

/** 匹配度：名称开头 > 名称包含 > 模块或分组包含；多个关键字（空格分隔）需全部命中 */
const results = computed(() => {
  const words = keyword.value.trim().toLowerCase().split(/\s+/).filter(Boolean)
  if (!words.length) return recent.value.map((p) => pageByPath.value.get(p)).filter(Boolean)
  return pages.value
    .filter((p) => words.every((w) => p.search.includes(w)))
    .map((p) => {
      const title = p.title.toLowerCase()
      const score = words.reduce((s, w) => s + (title.startsWith(w) ? 3 : title.includes(w) ? 2 : 0), 0)
      return { p, score }
    })
    .sort((a, b) => b.score - a.score || a.p.title.length - b.p.title.length)
    .slice(0, RESULT_MAX)
    .map((r) => r.p)
})
watch(results, () => (activeIndex.value = 0))

/** 把名称按关键字切分，命中的部分高亮 */
function highlight(title) {
  const words = keyword.value.trim().toLowerCase().split(/\s+/).filter(Boolean)
  if (!words.length) return [{ text: title, hit: false }]
  const hits = new Array(title.length).fill(false)
  const lower = title.toLowerCase()
  for (const w of words) {
    let i = lower.indexOf(w)
    while (i >= 0) {
      hits.fill(true, i, i + w.length)
      i = lower.indexOf(w, i + w.length)
    }
  }
  const parts = []
  for (let i = 0; i < title.length; i++) {
    const last = parts[parts.length - 1]
    if (last && last.hit === hits[i]) last.text += title[i]
    else parts.push({ text: title[i], hit: hits[i] })
  }
  return parts
}

function show() {
  keyword.value = ''
  activeIndex.value = 0
  open.value = true
}
// 弹窗打开后（等动画开始、默认焦点设置完）再聚焦输入框
watch(open, (v) => v && setTimeout(() => inputRef.value?.focus(), 50))

function go(item) {
  open.value = false
  if (item.path !== route.path) router.push(item.path)
}

function onKeydown(e) {
  const n = results.value.length
  if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
    e.preventDefault()
    if (!n) return
    activeIndex.value = (activeIndex.value + (e.key === 'ArrowDown' ? 1 : n - 1)) % n
    nextTick(() => listRef.value?.querySelector('.result-item.active')?.scrollIntoView({ block: 'nearest' }))
  } else if (e.key === 'Enter' && !e.isComposing && n) {
    e.preventDefault()
    go(results.value[activeIndex.value])
  }
}

function onGlobalKeydown(e) {
  if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    open.value ? (open.value = false) : show()
  }
}
onMounted(() => window.addEventListener('keydown', onGlobalKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onGlobalKeydown))
</script>

<style scoped>
.search-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  padding: 0 8px 0 10px;
  margin-right: 4px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: var(--app-hover);
  color: var(--app-text-3);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.search-trigger:hover {
  color: var(--app-primary);
  border-color: var(--app-primary);
}
.trigger-kbd {
  padding: 0 5px;
  border-radius: 4px;
  font-family: inherit;
  font-size: 11px;
  line-height: 18px;
  color: var(--app-text-3);
  background: var(--app-card);
  border: 1px solid var(--app-border);
}
@media (max-width: 575px) {
  .hide-mobile {
    display: none !important;
  }
  .search-trigger {
    width: 32px;
    padding: 0;
    justify-content: center;
    border-color: transparent;
    background: transparent;
    font-size: 16px;
  }
}
</style>

<style>
/* 弹窗渲染在 body 下，不能用 scoped */
.menu-search-modal .ant-modal-content {
  padding: 8px 8px 0;
  overflow: hidden;
}
.menu-search-modal .search-input {
  padding: 8px 12px;
  font-size: 16px;
}
.menu-search-modal .input-icon {
  color: var(--app-text-3);
  font-size: 16px;
  margin-right: 4px;
}
.menu-search-modal .result-title {
  padding: 8px 12px 4px;
  border-top: 1px solid var(--app-border);
  font-size: 12px;
  color: var(--app-text-3);
}
.menu-search-modal .result-list {
  max-height: min(420px, 60vh);
  margin: 0;
  padding: 4px;
  list-style: none;
}
.menu-search-modal .result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
}
.menu-search-modal .result-item.active {
  background: var(--app-primary-bg);
}
.menu-search-modal .item-icon {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 16px;
  color: var(--app-text-2);
  background: var(--app-hover);
}
.menu-search-modal .result-item.active .item-icon {
  color: var(--app-primary);
  background: var(--app-card);
}
.menu-search-modal .item-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.menu-search-modal .item-title {
  color: var(--app-text);
}
.menu-search-modal .item-title mark {
  padding: 0;
  color: var(--app-primary);
  background: transparent;
  font-weight: 600;
}
.menu-search-modal .item-chain {
  font-size: 12px;
  color: var(--app-text-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.menu-search-modal .item-enter {
  color: var(--app-primary);
}
.menu-search-modal .empty {
  padding: 24px 12px;
  text-align: center;
  color: var(--app-text-3);
}
.menu-search-modal .footer-tip {
  display: flex;
  gap: 16px;
  padding: 8px 12px;
  border-top: 1px solid var(--app-border);
  font-size: 12px;
  color: var(--app-text-3);
}
.menu-search-modal kbd {
  display: inline-block;
  min-width: 18px;
  margin-right: 3px;
  padding: 0 4px;
  border-radius: 4px;
  font-family: inherit;
  font-size: 11px;
  line-height: 16px;
  text-align: center;
  border: 1px solid var(--app-border);
  background: var(--app-hover);
}
@media (max-width: 575px) {
  .menu-search-modal .footer-tip {
    display: none;
  }
}
</style>
